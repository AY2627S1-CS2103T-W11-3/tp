package seedu.address.storage;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.ReadOnlyAddressBook;

/**
 * Saves Contacts and Favourites together, retaining their original bytes until both replacements succeed.
 * A pending journal restores both originals after a failed or interrupted save.
 * Callers must recover before loading collections and publish live-model changes only after saving succeeds.
 */
class CollectionSaveTransaction {
    private static final Logger logger = LogsCenter.getLogger(CollectionSaveTransaction.class);

    private final JsonAddressBookStorage contactsStorage;
    private final JsonAddressBookStorage favouritesStorage;
    private final Path journalFile;

    /**
     * Creates a coordinated saver for two distinct collection files without changing either file.
     */
    CollectionSaveTransaction(JsonAddressBookStorage contactsStorage, JsonAddressBookStorage favouritesStorage) {
        requireAllNonNull(contactsStorage, favouritesStorage);
        this.contactsStorage = contactsStorage;
        this.favouritesStorage = favouritesStorage;
        Path contactsFile = contactsStorage.getAddressBookFilePath().toAbsolutePath().normalize();
        if (contactsFile.equals(favouritesStorage.getAddressBookFilePath().toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Collection files must have different paths");
        }
        journalFile = contactsFile.resolveSibling("." + contactsFile.getFileName() + "-pending-save.json");
    }

    /**
     * Prepares both candidates before publishing either, then saves them as one recoverable operation.
     * A failed save restores the original bytes or retains the journal if restoration also fails.
     *
     * @throws IOException If preparation, publication, or recovery fails.
     */
    void save(ReadOnlyAddressBook contacts, ReadOnlyAddressBook favourites) throws IOException {
        save(contacts, favourites, null);
    }

    /**
     * Saves both collections and the shared undo index in the same recoverable operation.
     * A null index leaves existing history untouched for callers saving only collection data.
     *
     * @throws IOException If preparation, publication, or recovery fails.
     */
    void save(ReadOnlyAddressBook contacts, ReadOnlyAddressBook favourites, byte[] history) throws IOException {
        requireAllNonNull(contacts, favourites);
        recover();
        Path contactsFile = contactsStorage.getAddressBookFilePath().toAbsolutePath();
        Path favouritesFile = favouritesStorage.getAddressBookFilePath().toAbsolutePath();
        Path contactsCandidate = createTemporaryFile(contactsFile);
        Path favouritesCandidate = null;
        try {
            favouritesCandidate = createTemporaryFile(favouritesFile);
            contactsStorage.writeAddressBook(contacts, contactsCandidate);
            favouritesStorage.writeAddressBook(favourites, favouritesCandidate);
            byte[][] originals = {readOriginal(contactsFile), readOriginal(favouritesFile)};
            if (history != null) {
                originals = new byte[][] {originals[0], originals[1], readOriginal(getHistoryFile())};
            }
            Backup backup = new Backup(originals, favouritesFile);
            writeAtomically(journalFile, JsonUtil.toJsonString(backup).getBytes(StandardCharsets.UTF_8));
            try {
                contactsStorage.replaceAddressBook(contactsCandidate, contactsFile);
                favouritesStorage.replaceAddressBook(favouritesCandidate, favouritesFile);
                if (history != null) {
                    writeAtomically(getHistoryFile(), history);
                }
                Files.delete(journalFile);
            } catch (IOException | RuntimeException e) {
                try {
                    recover();
                } catch (IOException recoveryError) {
                    e.addSuppressed(recoveryError);
                }
                throw e;
            }
        } finally {
            deleteTemporaryFile(contactsCandidate);
            if (favouritesCandidate != null) {
                deleteTemporaryFile(favouritesCandidate);
            }
        }
    }

    /**
     * Restores both original files when a pending save exists, including their original absence.
     * Retains the journal until both restorations succeed, allowing recovery to be retried safely.
     *
     * @throws IOException If the journal is invalid or either file cannot be restored.
     */
    void recover() throws IOException {
        byte[] journal = readOriginal(journalFile);
        if (journal == null) {
            return;
        }
        Backup backup = JsonUtil.fromJsonString(new String(journal, StandardCharsets.UTF_8), Backup.class);
        if (backup == null || backup.originals == null || (backup.originals.length != 2 && backup.originals.length != 3)
                || !favouritesStorage.getAddressBookFilePath().toAbsolutePath().normalize().toString()
                        .equals(backup.favouritesPath)) {
            throw new IOException("Invalid pending collection save");
        }
        restore(contactsStorage.getAddressBookFilePath(), backup.originals[0]);
        restore(favouritesStorage.getAddressBookFilePath(), backup.originals[1]);
        if (backup.originals.length == 3) {
            restore(getHistoryFile(), backup.originals[2]);
        }
        Files.delete(journalFile);
    }

    Path getHistoryFile() {
        Path contactsFile = contactsStorage.getAddressBookFilePath().toAbsolutePath().normalize();
        return contactsFile.resolveSibling("." + contactsFile.getFileName() + "-shared-undo.json");
    }

    /**
     * Returns exact original bytes, distinguishing an absent file from an unreadable one.
     */
    private byte[] readOriginal(Path file) throws IOException {
        try {
            return Files.readAllBytes(file);
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    /**
     * Creates a temporary file beside its destination so atomic replacement stays on the same filesystem.
     */
    private Path createTemporaryFile(Path destination) throws IOException {
        Path parent = destination.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        return Files.createTempFile(parent, "collections-", ".tmp");
    }

    /**
     * Restores a file's original contents or removes a file that did not exist before the save.
     */
    private void restore(Path destination, byte[] original) throws IOException {
        if (original == null) {
            Files.deleteIfExists(destination);
        } else {
            writeAtomically(destination, original);
        }
    }

    /**
     * Replaces one file atomically without falling back to a partial in-place write.
     */
    private void writeAtomically(Path destination, byte[] contents) throws IOException {
        Path temporaryFile = createTemporaryFile(destination);
        try {
            Files.write(temporaryFile, contents);
            Files.move(temporaryFile, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            deleteTemporaryFile(temporaryFile);
        }
    }

    /**
     * Removes a temporary file without reporting a completed save as failed when cleanup is unavailable.
     */
    private void deleteTemporaryFile(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            logger.warning("Could not remove collection save temporary file: " + e.getMessage());
        }
    }

    /**
     * Stores exact original bytes, including null entries for missing files, and the paired destination.
     */
    private static class Backup {
        private final byte[][] originals;
        private final String favouritesPath;

        private Backup(byte[][] originals, Path favouritesFile) {
            this.originals = originals;
            favouritesPath = favouritesFile.toAbsolutePath().normalize().toString();
        }

        private Backup() {
            originals = null;
            favouritesPath = null;
        }
    }
}
