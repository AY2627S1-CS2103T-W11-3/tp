package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.ContactSection;
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
    private final Map<ContactSection, UndoSnapshotStorage> undoHistories = new EnumMap<>(ContactSection.class);

    /**
     * Creates a {@code StorageManager} with the given address book and user prefs storage.
     */
    public StorageManager(JsonAddressBookStorage addressBookStorage, JsonUserPrefsStorage userPrefsStorage) {
        this(addressBookStorage, new JsonAddressBookStorage(
                requireNonNull(addressBookStorage).getAddressBookFilePath().resolveSibling("favourites.json")),
                userPrefsStorage);
    }

    /** Creates storage for independent contact collections and shared preferences. */
    public StorageManager(JsonAddressBookStorage addressBookStorage, JsonAddressBookStorage favouritesStorage,
            JsonUserPrefsStorage userPrefsStorage) {
        this.addressBookStorage = requireNonNull(addressBookStorage);
        this.favouritesStorage = requireNonNull(favouritesStorage);
        this.userPrefsStorage = requireNonNull(userPrefsStorage);
        undoHistories.put(ContactSection.CONTACTS, new UndoSnapshotStorage(addressBookStorage));
        undoHistories.put(ContactSection.FAVOURITES, new UndoSnapshotStorage(favouritesStorage));
        assert undoHistories.size() == ContactSection.values().length : "Every section must have undo storage";
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
        return addressBookStorage.readAddressBook();
    }

    @Override
    public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
        logger.fine("Attempting to write to data file: " + addressBookStorage.getAddressBookFilePath());
        addressBookStorage.saveAddressBook(addressBook);
    }

    @Override
    public void saveUndoableChange(ContactSection section, ReadOnlyAddressBook previous, ReadOnlyAddressBook candidate)
            throws IOException {
        logger.fine("Saving undoable change in " + section);
        undoHistories.get(requireNonNull(section)).saveChange(previous, candidate);
    }

    @Override
    public Optional<ReadOnlyAddressBook> readLatestSnapshot(ContactSection section) throws DataLoadingException {
        return undoHistories.get(requireNonNull(section)).readLatestSnapshot();
    }

    @Override
    public void discardLatestSnapshot(ContactSection section) {
        undoHistories.get(requireNonNull(section)).discardLatestSnapshot();
    }

    @Override
    public void closeUndoHistory() {
        undoHistories.values().forEach(UndoSnapshotStorage::close);
    }

    @Override
    public Path getFavouritesFilePath() {
        return favouritesStorage.getAddressBookFilePath();
    }

    @Override
    public Optional<ReadOnlyAddressBook> readFavourites() throws DataLoadingException {
        return favouritesStorage.readAddressBook();
    }

    @Override
    public void saveFavourites(ReadOnlyAddressBook favourites) throws IOException {
        favouritesStorage.saveAddressBook(favourites);
    }

}
