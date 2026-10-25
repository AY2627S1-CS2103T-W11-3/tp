package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;

/**
 * Saves contact changes with file-backed undo snapshots that survive normal application shutdown.
 * Callers must publish changes to the live model only after saving or restoring succeeds.
 * Instances are used synchronously and must be closed when their session ends.
 */
class UndoSnapshotStorage {
    public static final int MAX_HISTORY_SIZE = 5;

    private static final Logger logger = LogsCenter.getLogger(UndoSnapshotStorage.class);

    private final JsonAddressBookStorage addressBookStorage;
    private final Deque<Path> snapshots = new ArrayDeque<>();
    private final Path historyDirectory;
    private final Path indexFile;
    private ReadOnlyAddressBook expectedData;
    private ReadOnlyAddressBook latestReadSnapshot;
    private boolean isClosed;

    /**
     * Loads a cleanly closed history belonging to the given address-book storage, if available.
     */
    public UndoSnapshotStorage(JsonAddressBookStorage addressBookStorage) {
        this.addressBookStorage = requireNonNull(addressBookStorage);
        Path dataFile = addressBookStorage.getAddressBookFilePath().toAbsolutePath();
        historyDirectory = dataFile.resolveSibling("." + dataFile.getFileName() + "-undo");
        indexFile = historyDirectory.resolve("history.json");
        loadHistory();
    }

    /**
     * Backs up the previous contacts, then saves the candidate before registering its undo entry.
     * Identical contact data is saved without adding history, allowing recovery from an invalid data file.
     * A failed backup or save leaves existing history untouched.
     * The previous data comes from the model, including when the main data file does not yet exist.
     */
    public void saveChange(ReadOnlyAddressBook previous, ReadOnlyAddressBook candidate) throws IOException {
        requireOpen();
        requireNonNull(previous);
        requireNonNull(candidate);
        invalidatePersistedHistory();
        if (previous.getPersonList().equals(candidate.getPersonList())
                && previous.getGroupList().equals(candidate.getGroupList())) {
            addressBookStorage.saveAddressBook(candidate);
            recordSavedData(candidate);
            return;
        }

        Path snapshot = createSnapshotFile();
        try {
            addressBookStorage.saveAddressBook(previous, snapshot);
            addressBookStorage.saveAddressBook(candidate);
        } catch (IOException | RuntimeException e) {
            deleteSafely(snapshot);
            deleteEmptyHistoryDirectory();
            throw e;
        }

        recordSavedData(candidate);
        snapshots.addLast(snapshot);
        if (snapshots.size() > MAX_HISTORY_SIZE) {
            deleteSafely(snapshots.removeFirst());
        }
        assert snapshots.size() <= MAX_HISTORY_SIZE : "Undo history exceeds its capacity";
        logger.fine("Saved contact change with " + snapshots.size() + " undo snapshot(s).");
    }

    /** Reads this collection's latest snapshot without consuming it. */
    public Optional<ReadOnlyAddressBook> readLatestSnapshot() throws DataLoadingException {
        requireOpen();
        if (snapshots.isEmpty()) {
            return Optional.empty();
        }

        Path snapshot = snapshots.getLast();
        ReadOnlyAddressBook restored = addressBookStorage.readAddressBook(snapshot)
                .orElseThrow(() -> new DataLoadingException(new NoSuchFileException(snapshot.toString())));
        latestReadSnapshot = restored;
        return Optional.of(restored);
    }

    /** Consumes this collection's latest snapshot after a successful save. */
    public void discardLatestSnapshot() {
        requireOpen();
        if (snapshots.isEmpty()) {
            throw new IllegalStateException("No undo snapshot is available to consume.");
        }
        if (latestReadSnapshot != null) {
            expectedData = latestReadSnapshot;
            latestReadSnapshot = null;
        }
        deleteSafely(snapshots.removeLast());
        logger.fine("Restored contact snapshot; " + snapshots.size() + " undo snapshot(s) remain.");
    }

    /**
     * Creates a backup file in a lazily allocated directory specific to this collection's file.
     */
    private Path createSnapshotFile() throws IOException {
        Files.createDirectories(historyDirectory);
        try {
            return Files.createTempFile(historyDirectory, "undo-", ".json");
        } catch (IOException e) {
            deleteEmptyHistoryDirectory();
            throw e;
        }
    }

    /**
     * Removes an unused history directory after a failed first backup or save.
     */
    private void deleteEmptyHistoryDirectory() {
        if (!snapshots.isEmpty()) {
            return;
        }
        try {
            Files.deleteIfExists(historyDirectory);
        } catch (IOException e) {
            logger.warning("Could not remove unused undo directory: " + e.getMessage());
        }
    }

    /**
     * Persists the history index on normal shutdown and releases this instance without deleting snapshots.
     * Persistence failures are logged; the collection data is never changed during shutdown.
     */
    public void close() {
        if (isClosed) {
            return;
        }
        try {
            persistHistory();
        } catch (IOException e) {
            logger.warning("Could not persist undo history in " + historyDirectory + ": " + e.getMessage());
        }
        isClosed = true;
        snapshots.clear();
    }

    /** Reloads only validated history whose checksum matches the current collection file. */
    private void loadHistory() {
        try {
            if (Files.notExists(indexFile)) {
                return;
            }
            JsonUndoHistory metadata = JsonUtil.fromJsonString(Files.readString(indexFile), JsonUndoHistory.class);
            if (metadata == null) {
                throw new IOException("Expected undo history metadata, but found null");
            }
            List<Path> restored = metadata.resolveSnapshots(historyDirectory);
            if (!metadata.getDataChecksum().equals(dataChecksum())) {
                logger.warning("Ignoring undo history for changed data file: "
                        + addressBookStorage.getAddressBookFilePath());
                return;
            }
            expectedData = addressBookStorage.readAddressBook().orElseThrow(() -> new IOException("Missing data file"));
            snapshots.addAll(restored);
            assert snapshots.size() <= MAX_HISTORY_SIZE : "Loaded undo history exceeds its capacity";
            logger.fine("Loaded " + snapshots.size() + " undo snapshot(s) from " + historyDirectory);
        } catch (DataLoadingException | IOException e) {
            logger.warning("Could not load undo history from " + indexFile + ": " + e.getMessage());
        }
    }

    /** Invalidates an earlier clean-shutdown index before the collection file may change. */
    void invalidatePersistedHistory() throws IOException {
        requireOpen();
        Files.deleteIfExists(indexFile);
    }

    /** Atomically publishes the snapshot order only after all collection operations have finished. */
    private void persistHistory() throws IOException {
        if (snapshots.isEmpty()) {
            invalidatePersistedHistory();
            deleteEmptyHistoryDirectory();
            return;
        }
        try {
            ReadOnlyAddressBook savedData = addressBookStorage.readAddressBook()
                    .orElseThrow(() -> new IOException("Missing data file"));
            if (!new AddressBook(savedData).equals(expectedData)) {
                invalidatePersistedHistory();
                logger.warning("Not persisting undo history because the saved collection changed externally");
                return;
            }
        } catch (DataLoadingException e) {
            throw new IOException("Could not validate the saved collection for undo history", e);
        }
        JsonUndoHistory metadata = new JsonUndoHistory(dataChecksum(), snapshots.stream()
                .map(path -> path.getFileName().toString()).toList());
        Path temporaryFile = Files.createTempFile(historyDirectory, "history-", ".tmp");
        try {
            JsonUtil.saveJsonFile(metadata, temporaryFile);
            Files.move(temporaryFile, indexFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            deleteSafely(temporaryFile);
        }
    }

    /** Remembers the complete saved state so external edits cannot be attached to stale history on shutdown. */
    private void recordSavedData(ReadOnlyAddressBook data) {
        expectedData = new AddressBook(data);
        latestReadSnapshot = null;
    }

    /** Binds persisted history to the exact saved collection without changing that collection. */
    private String dataChecksum() throws IOException {
        try {
            byte[] data = Files.readAllBytes(addressBookStorage.getAddressBookFilePath());
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for undo history checksums", e);
        }
    }

    /**
     * Removes an obsolete backup without turning an already committed change into a reported failure.
     */
    private void deleteSafely(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warning("Could not remove undo backup: " + e.getMessage());
        }
    }

    private void requireOpen() {
        if (isClosed) {
            throw new IllegalStateException("Undo storage session is closed.");
        }
    }
}
