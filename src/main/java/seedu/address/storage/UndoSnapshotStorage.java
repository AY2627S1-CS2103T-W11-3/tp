package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.ReadOnlyAddressBook;

/**
 * Saves contact changes with file-backed undo snapshots for one application session.
 * Callers must publish changes to the live model only after saving or restoring succeeds.
 * Instances are used synchronously and must be closed when their session ends.
 */
public class UndoSnapshotStorage {
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
     * Identical contact data is ignored. A failed backup or save leaves existing history untouched.
     * The previous data comes from the model, including when the main data file does not yet exist.
     */
    public void saveChange(ReadOnlyAddressBook previous, ReadOnlyAddressBook candidate) throws IOException {
        requireOpen();
        requireNonNull(previous);
        requireNonNull(candidate);
        if (previous.getPersonList().equals(candidate.getPersonList())) {
            return;
        }

        Path snapshot = createSnapshotFile();
        try {
            addressBookStorage.saveAddressBook(previous, snapshot);
            addressBookStorage.saveAddressBook(candidate);
        } catch (IOException | RuntimeException e) {
            deleteSafely(snapshot);
            throw e;
        }

        snapshots.addLast(snapshot);
        if (snapshots.size() > MAX_HISTORY_SIZE) {
            deleteSafely(snapshots.removeFirst());
        }
    }

    /**
     * Saves the latest snapshot as the current data and returns it for publication to the live model.
     * Returns empty when no changes can be undone. Read/save failures retain the entry for retry.
     */
    public Optional<ReadOnlyAddressBook> restoreLatest() throws IOException, DataLoadingException {
        requireOpen();
        if (snapshots.isEmpty()) {
            return Optional.empty();
        }

        Path snapshot = snapshots.getLast();
        ReadOnlyAddressBook restored = addressBookStorage.readAddressBook(snapshot)
                .orElseThrow(() -> new IOException("Undo snapshot is missing."));
        addressBookStorage.saveAddressBook(restored);
        snapshots.removeLast();
        deleteSafely(snapshot);
        return Optional.of(restored);
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
        return Files.createTempFile(sessionDirectory, "undo-", ".json");
    }

    /**
     * Ends the session and attempts to remove its backups, including any previously failed cleanup.
     */
    public void close() {
        isClosed = true;
        snapshots.clear();
        if (sessionDirectory == null) {
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
