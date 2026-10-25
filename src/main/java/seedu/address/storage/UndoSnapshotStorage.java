package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UndoHistory;

/**
 * Saves contact changes with file-backed undo snapshots for one application session.
 * Callers must publish changes to the live model only after saving or restoring succeeds.
 * Instances are used synchronously and must be closed when their session ends.
 */
class UndoSnapshotStorage implements UndoHistory {
    public static final int MAX_HISTORY_SIZE = 5;

    private static final Logger logger = LogsCenter.getLogger(UndoSnapshotStorage.class);

    private final JsonAddressBookStorage addressBookStorage;
    private final Deque<Path> snapshots = new ArrayDeque<>();
    private Path sessionDirectory;
    private boolean isClosed;

    /**
     * Creates an empty undo session using the given address-book storage for JSON reads and atomic saves.
     */
    public UndoSnapshotStorage(JsonAddressBookStorage addressBookStorage) {
        this.addressBookStorage = requireNonNull(addressBookStorage);
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
        if (previous.getPersonList().equals(candidate.getPersonList())
                && previous.getGroupList().equals(candidate.getGroupList())) {
            addressBookStorage.saveAddressBook(candidate);
            return;
        }

        Path snapshot = createSnapshotFile();
        try {
            addressBookStorage.saveAddressBook(previous, snapshot);
            addressBookStorage.saveAddressBook(candidate);
        } catch (IOException | RuntimeException e) {
            deleteSafely(snapshot);
            deleteEmptySessionDirectory();
            throw e;
        }

        snapshots.addLast(snapshot);
        if (snapshots.size() > MAX_HISTORY_SIZE) {
            deleteSafely(snapshots.removeFirst());
        }
        assert snapshots.size() <= MAX_HISTORY_SIZE : "Undo history exceeds its capacity";
        logger.fine("Saved contact change with " + snapshots.size() + " undo snapshot(s).");
    }

    @Override
    public Optional<ReadOnlyAddressBook> readLatestSnapshot() throws DataLoadingException {
        requireOpen();
        if (snapshots.isEmpty()) {
            return Optional.empty();
        }

        Path snapshot = snapshots.getLast();
        return Optional.of(addressBookStorage.readAddressBook(snapshot)
                .orElseThrow(() -> new DataLoadingException(new NoSuchFileException(snapshot.toString()))));
    }

    @Override
    public void discardLatestSnapshot() {
        requireOpen();
        if (snapshots.isEmpty()) {
            throw new IllegalStateException("No undo snapshot is available to consume.");
        }
        deleteSafely(snapshots.removeLast());
        logger.fine("Restored contact snapshot; " + snapshots.size() + " undo snapshot(s) remain.");
    }

    /**
     * Creates a backup file in a lazily allocated directory owned exclusively by this session.
     */
    private Path createSnapshotFile() throws IOException {
        if (sessionDirectory == null) {
            Path parent = addressBookStorage.getAddressBookFilePath().toAbsolutePath().getParent();
            Files.createDirectories(parent);
            sessionDirectory = Files.createTempDirectory(parent, ".addressbook-undo-");
        }
        try {
            return Files.createTempFile(sessionDirectory, "undo-", ".json");
        } catch (IOException e) {
            deleteEmptySessionDirectory();
            throw e;
        }
    }

    /**
     * Removes an unused session directory after a failed first backup or save.
     */
    private void deleteEmptySessionDirectory() {
        if (!snapshots.isEmpty() || sessionDirectory == null) {
            return;
        }
        try {
            Files.deleteIfExists(sessionDirectory);
            sessionDirectory = null;
        } catch (IOException e) {
            logger.warning("Could not remove unused undo directory: " + e.getMessage());
        }
    }

    /**
     * Ends the session and attempts to remove its backups, including any previously failed cleanup.
     */
    public void close() {
        isClosed = true;
        snapshots.clear();
        if (sessionDirectory == null || Files.notExists(sessionDirectory)) {
            return;
        }
        try (Stream<Path> files = Files.list(sessionDirectory)) {
            files.forEach(this::deleteSafely);
        } catch (IOException e) {
            logger.warning("Could not list undo backups for cleanup: " + e.getMessage());
        }
        deleteSafely(sessionDirectory);
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
