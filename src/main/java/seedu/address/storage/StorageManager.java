package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.ContactSnapshot;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.UserPrefs;

/**
 * Manages storage of AddressBook data in local storage.
 */
public class StorageManager implements Storage {

    private static final Logger logger = LogsCenter.getLogger(StorageManager.class);
    private JsonAddressBookStorage addressBookStorage;
    private JsonAddressBookStorage favouritesStorage;
    private JsonUserPrefsStorage userPrefsStorage;
    private final UndoSnapshotStorage undoHistory;

    /**
     * Creates a {@code StorageManager} with the given address book and user prefs storage.
     */
    public StorageManager(JsonAddressBookStorage addressBookStorage, JsonUserPrefsStorage userPrefsStorage) {
        this(addressBookStorage, new JsonAddressBookStorage(
                requireNonNull(addressBookStorage).getAddressBookFilePath().resolveSibling("favourites.json")),
                userPrefsStorage);
    }

    /**
     * Creates storage for both contact collections, shared preferences, and one persistent undo history.
     */
    public StorageManager(JsonAddressBookStorage addressBookStorage, JsonAddressBookStorage favouritesStorage,
            JsonUserPrefsStorage userPrefsStorage) {
        this.addressBookStorage = requireNonNull(addressBookStorage);
        this.favouritesStorage = requireNonNull(favouritesStorage);
        this.userPrefsStorage = requireNonNull(userPrefsStorage);
        undoHistory = new UndoSnapshotStorage(addressBookStorage, favouritesStorage);
    }

    // ================ UserPrefs methods ==============================

    @Override
    public Path getUserPrefsFilePath() {
        return userPrefsStorage.getUserPrefsFilePath();
    }

    @Override
    public Optional<UserPrefs> readUserPrefs() throws DataLoadingException {
        return userPrefsStorage.readUserPrefs();
    }

    @Override
    public void saveUserPrefs(ReadOnlyUserPrefs userPrefs) throws IOException {
        userPrefsStorage.saveUserPrefs(userPrefs);
    }


    // ================ AddressBook methods ==============================

    @Override
    public Path getAddressBookFilePath() {
        return addressBookStorage.getAddressBookFilePath();
    }

    @Override
    public Optional<ReadOnlyAddressBook> readAddressBook() throws DataLoadingException {
        logger.fine("Attempting to read data from file: " + addressBookStorage.getAddressBookFilePath());
        try {
            undoHistory.requireAvailable();
        } catch (IOException e) {
            throw new DataLoadingException(e);
        }
        return addressBookStorage.readAddressBook();
    }

    @Override
    public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
        logger.fine("Attempting to write to data file: " + addressBookStorage.getAddressBookFilePath());
        undoHistory.invalidatePersistedHistory();
        addressBookStorage.saveAddressBook(addressBook);
    }

    @Override
    public void saveUndoableChange(ContactSnapshot previous, ContactSnapshot candidate,
            String undoDescription) throws IOException {
        logger.fine("Saving a shared undoable change");
        undoHistory.saveChange(previous, candidate, undoDescription);
    }

    @Override
    public Optional<ContactSnapshot> readLatestSnapshot() throws DataLoadingException {
        return undoHistory.readLatestSnapshot();
    }

    @Override
    public String getLatestUndoDescription() {
        return undoHistory.getLatestUndoDescription();
    }

    @Override
    public void saveRestoredSnapshot(ContactSnapshot snapshot) throws IOException {
        undoHistory.saveRestoredSnapshot(snapshot);
    }

    @Override
    public void discardLatestSnapshot() {
        undoHistory.discardLatestSnapshot();
    }

    @Override
    public void closeUndoHistory() {
        undoHistory.close();
    }

    @Override
    public Path getFavouritesFilePath() {
        return favouritesStorage.getAddressBookFilePath();
    }

    @Override
    public Optional<ReadOnlyAddressBook> readFavourites() throws DataLoadingException {
        try {
            undoHistory.requireAvailable();
        } catch (IOException e) {
            throw new DataLoadingException(e);
        }
        return favouritesStorage.readAddressBook();
    }

    @Override
    public void saveFavourites(ReadOnlyAddressBook favourites) throws IOException {
        undoHistory.invalidatePersistedHistory();
        favouritesStorage.saveAddressBook(favourites);
    }

}
