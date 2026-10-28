package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.ContactSection;
import seedu.address.model.ContactSnapshot;

/**
 * Persists one bounded undo history for Contacts and Favourites in chronological order.
 * Data and history are published together; callers update the live model only after saving succeeds.
 */
class UndoSnapshotStorage {
    public static final int MAX_HISTORY_SIZE = 5;
    private static final Logger logger = LogsCenter.getLogger(UndoSnapshotStorage.class);

    private final JsonAddressBookStorage contactsStorage;
    private final JsonAddressBookStorage favouritesStorage;
    private final CollectionSaveTransaction transaction;
    private List<Entry> entries = new ArrayList<>();
    private ContactSnapshot latestReadSnapshot;
    private IOException recoveryFailure;
    private boolean isClosed;

    /**
     * Recovers an interrupted operation before loading history matching both saved collection files.
     * Unrecoverable saves block subsequent data access rather than exposing partially restored collections.
     */
    UndoSnapshotStorage(JsonAddressBookStorage contactsStorage, JsonAddressBookStorage favouritesStorage) {
        this.contactsStorage = requireNonNull(contactsStorage);
        this.favouritesStorage = requireNonNull(favouritesStorage);
        transaction = new CollectionSaveTransaction(contactsStorage, favouritesStorage);
        try {
            transaction.recover();
        } catch (IOException e) {
            recoveryFailure = e;
            return;
        }
        try {
            if (Files.exists(transaction.getHistoryFile())) {
                Journal journal = readJournal();
                if (checksum(contactsStorage.getAddressBookFilePath()).equals(journal.contactsChecksum)
                        && checksum(favouritesStorage.getAddressBookFilePath()).equals(journal.favouritesChecksum)) {
                    entries = new ArrayList<>(journal.entries);
                } else {
                    logger.warning("Ignoring shared undo history because saved contacts changed externally");
                }
            }
        } catch (IOException e) {
            logger.warning("Could not load shared undo history: " + e.getMessage());
        }
    }

    /**
     * Blocks access after failed startup recovery or after this storage instance has closed.
     *
     * @throws IOException If an interrupted operation could not be recovered.
     */
    void requireAvailable() throws IOException {
        if (isClosed) {
            throw new IllegalStateException("Undo storage session is closed");
        }
        if (recoveryFailure != null) {
            throw new IOException("Could not recover the pending collection save", recoveryFailure);
        }
    }

    /**
     * Saves both candidate collections and records one before-state when their data changes.
     * Identical data is still saved without consuming undo capacity.
     *
     * @throws IOException If data or history cannot be saved consistently.
     */
    void saveChange(ContactSnapshot previous, ContactSnapshot candidate, String description) throws IOException {
        requireAvailable();
        requireNonNull(previous);
        requireNonNull(candidate);
        requireNonNull(description);
        if (description.isBlank()) {
            throw new IllegalArgumentException("Undo description must not be blank");
        }
        List<Entry> next = new ArrayList<>(entries);
        if (!previous.equals(candidate)) {
            next.add(new Entry(previous, description));
            if (next.size() > MAX_HISTORY_SIZE) {
                next.removeFirst();
            }
        }
        save(candidate, next);
        entries = next;
        latestReadSnapshot = null;
    }

    /**
     * Reads the latest shared snapshot without consuming history or changing either collection.
     *
     * @throws DataLoadingException If the stored snapshot is missing or invalid.
     */
    Optional<ContactSnapshot> readLatestSnapshot() throws DataLoadingException {
        try {
            requireAvailable();
            if (entries.isEmpty()) {
                return Optional.empty();
            }
            Journal journal = readJournal();
            if (journal.entries.size() != entries.size()) {
                throw new IOException("Shared undo history changed during the session");
            }
            latestReadSnapshot = journal.entries.getLast().toModelType();
            return Optional.of(latestReadSnapshot);
        } catch (IOException | IllegalValueException e) {
            throw new DataLoadingException(e);
        }
    }

    /**
     * Returns feedback identifying the latest operation, independent of the active section.
     */
    String getLatestUndoDescription() {
        if (entries.isEmpty()) {
            throw new IllegalStateException("No changes to undo");
        }
        return entries.getLast().description;
    }

    /**
     * Restores both collections and removes the latest persisted history entry as one save operation.
     * The caller consumes the in-memory entry only after applying the restored data to the live model.
     *
     * @throws IOException If either collection or the remaining history cannot be saved.
     */
    void saveRestoredSnapshot(ContactSnapshot snapshot) throws IOException {
        requireAvailable();
        if (latestReadSnapshot == null || !latestReadSnapshot.equals(snapshot)) {
            throw new IllegalStateException("Read the latest undo snapshot before restoring it");
        }
        List<Entry> remaining = new ArrayList<>(entries);
        remaining.removeLast();
        save(snapshot, remaining);
    }

    /**
     * Consumes the latest in-memory entry after a successful restoration and live-model update.
     */
    void discardLatestSnapshot() {
        if (latestReadSnapshot == null || entries.isEmpty()) {
            throw new IllegalStateException("No prepared undo snapshot to consume");
        }
        entries.removeLast();
        latestReadSnapshot = null;
    }

    /**
     * Invalidates shared history before a collection save outside the undoable command path.
     *
     * @throws IOException If history cannot be invalidated safely.
     */
    void invalidatePersistedHistory() throws IOException {
        requireAvailable();
        Files.deleteIfExists(transaction.getHistoryFile());
        entries.clear();
        latestReadSnapshot = null;
    }

    /**
     * Closes this instance without deleting history, which is persisted with every successful save.
     */
    void close() {
        isClosed = true;
    }

    /**
     * Publishes the complete next state with checksums binding its history to the saved JSON bytes.
     */
    private void save(ContactSnapshot candidate, List<Entry> next) throws IOException {
        JsonSerializableAddressBook contacts =
                new JsonSerializableAddressBook(candidate.getAddressBook(ContactSection.CONTACTS));
        JsonSerializableAddressBook favourites =
                new JsonSerializableAddressBook(candidate.getAddressBook(ContactSection.FAVOURITES));
        Journal journal = new Journal(digest(JsonUtil.toJsonString(contacts).getBytes(StandardCharsets.UTF_8)),
                digest(JsonUtil.toJsonString(favourites).getBytes(StandardCharsets.UTF_8)), next);
        try {
            transaction.save(candidate.getAddressBook(ContactSection.CONTACTS),
                    candidate.getAddressBook(ContactSection.FAVOURITES),
                    JsonUtil.toJsonString(journal).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            try {
                transaction.recover();
            } catch (IOException recoveryError) {
                recoveryFailure = recoveryError;
                e.addSuppressed(recoveryError);
            }
            throw e;
        }
    }

    /**
     * Reads and validates bounded history before exposing any snapshot for restoration.
     */
    private Journal readJournal() throws IOException {
        Journal journal = JsonUtil.fromJsonString(Files.readString(transaction.getHistoryFile()), Journal.class);
        if (journal == null || journal.entries == null || journal.entries.size() > MAX_HISTORY_SIZE
                || journal.contactsChecksum == null || journal.favouritesChecksum == null) {
            throw new IOException("Invalid shared undo history");
        }
        for (Entry entry : journal.entries) {
            if (entry == null || entry.description == null || entry.description.isBlank()
                    || entry.contacts == null || entry.favourites == null) {
                throw new IOException("Invalid shared undo snapshot");
            }
        }
        return journal;
    }

    /**
     * Returns the checksum of a saved file, distinguishing absent files from read failures.
     */
    private static String checksum(Path file) throws IOException {
        try {
            return digest(Files.readAllBytes(file));
        } catch (NoSuchFileException e) {
            return "missing";
        }
    }

    /**
     * Calculates the fingerprint used to reject history belonging to externally changed data.
     */
    private static String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for undo history", e);
        }
    }

    /**
     * Stores both complete collections before one command, together with its undo feedback.
     */
    private static class Entry {
        private JsonSerializableAddressBook contacts;
        private JsonSerializableAddressBook favourites;
        private String description;

        private Entry(ContactSnapshot snapshot, String description) {
            contacts = new JsonSerializableAddressBook(snapshot.getAddressBook(ContactSection.CONTACTS));
            favourites = new JsonSerializableAddressBook(snapshot.getAddressBook(ContactSection.FAVOURITES));
            this.description = description;
        }

        private Entry() {}

        private ContactSnapshot toModelType() throws IllegalValueException {
            return new ContactSnapshot(contacts.toModelType(), favourites.toModelType());
        }
    }

    /**
     * Binds a chronological history to the exact bytes saved in both collection files.
     */
    private static class Journal {
        private String contactsChecksum;
        private String favouritesChecksum;
        private List<Entry> entries;

        private Journal(String contactsChecksum, String favouritesChecksum, List<Entry> entries) {
            this.contactsChecksum = contactsChecksum;
            this.favouritesChecksum = favouritesChecksum;
            this.entries = List.copyOf(entries);
        }

        private Journal() {}
    }
}
