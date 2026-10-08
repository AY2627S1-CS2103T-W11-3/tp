package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.ReadOnlyAddressBook;

/**
 * A class to access AddressBook data stored as a JSON file on the hard disk.
 */
public class JsonAddressBookStorage {

    private static final Logger logger = LogsCenter.getLogger(JsonAddressBookStorage.class);

    private Path filePath;

    public JsonAddressBookStorage(Path filePath) {
        this.filePath = filePath;
    }

    public Path getAddressBookFilePath() {
        return filePath;
    }

    /**
     * Returns AddressBook data as a {@link ReadOnlyAddressBook}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyAddressBook> readAddressBook() throws DataLoadingException {
        return readAddressBook(filePath);
    }

    /**
     * Similar to {@link #readAddressBook()}.
     *
     * @param filePath location of the data. Cannot be null.
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyAddressBook> readAddressBook(Path filePath) throws DataLoadingException {
        requireNonNull(filePath);

        String content;
        try {
            content = Files.readString(filePath);
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (CharacterCodingException e) {
            throw new ContactDataLoadingException(ContactDataLoadingException.MESSAGE_MALFORMED, e);
        } catch (IOException e) {
            throw new ContactDataLoadingException(ContactDataLoadingException.MESSAGE_UNREADABLE, e);
        }

        JsonSerializableAddressBook jsonAddressBook;
        try {
            jsonAddressBook = JsonUtil.fromJsonString(content, JsonSerializableAddressBook.class);
        } catch (IOException e) {
            throw new ContactDataLoadingException(ContactDataLoadingException.MESSAGE_MALFORMED, e);
        }
        if (jsonAddressBook == null) {
            throw new ContactDataLoadingException(ContactDataLoadingException.MESSAGE_MALFORMED,
                    new IllegalValueException("Expected a contact collection, but found null."));
        }

        try {
            return Optional.of(jsonAddressBook.toModelType());
        } catch (IllegalValueException ive) {
            logger.info("Illegal values found in " + filePath + ": " + ive.getMessage());
            throw new ContactDataLoadingException(ContactDataLoadingException.MESSAGE_INVALID, ive);
        }
    }

    /**
     * Saves the given {@link ReadOnlyAddressBook} to the storage.
     * @param addressBook cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
        saveAddressBook(addressBook, filePath);
    }

    /**
     * Similar to {@link #saveAddressBook(ReadOnlyAddressBook)}.
     *
     * @param filePath location of the data. Cannot be null.
     */
    public void saveAddressBook(ReadOnlyAddressBook addressBook, Path filePath) throws IOException {
        requireNonNull(addressBook);
        requireNonNull(filePath);

        Path destination = filePath.toAbsolutePath();
        Files.createDirectories(destination.getParent());
        if (Files.exists(destination) && !Files.isWritable(destination)) {
            throw new AccessDeniedException(destination.toString());
        }
        Path temporaryFile = Files.createTempFile(destination.getParent(), "addressbook-", ".tmp");
        try {
            writeAddressBook(addressBook, temporaryFile);
            replaceAddressBook(temporaryFile, destination);
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw e;
        }
    }

    /**
     * Writes a complete candidate to a temporary file without touching the saved address book.
     */
    protected void writeAddressBook(ReadOnlyAddressBook addressBook, Path temporaryFile) throws IOException {
        JsonUtil.saveJsonFile(new JsonSerializableAddressBook(addressBook), temporaryFile);
    }

    /**
     * Atomically publishes the candidate. Unsupported atomic moves fail without a non-atomic fallback.
     */
    protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
        Files.move(temporaryFile, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
