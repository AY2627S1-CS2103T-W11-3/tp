package seedu.address.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.UndoHistory;
import seedu.address.model.UserPrefs;

/**
 * API of the Storage component
 */
public interface Storage extends UndoHistory {

    /**
     * Returns the file path of the UserPrefs data file.
     */
    Path getUserPrefsFilePath();

    /**
     * Returns UserPrefs data from storage.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if the loading of data from preference file failed.
     */
    Optional<UserPrefs> readUserPrefs() throws DataLoadingException;

    /**
     * Saves the given {@link seedu.address.model.ReadOnlyUserPrefs} to the storage.
     * @param userPrefs cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    void saveUserPrefs(ReadOnlyUserPrefs userPrefs) throws IOException;

    /**
     * Returns the file path of the AddressBook data file.
     */
    Path getAddressBookFilePath();

    /**
     * Returns AddressBook data as a {@link ReadOnlyAddressBook}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    Optional<ReadOnlyAddressBook> readAddressBook() throws DataLoadingException;

    /**
     * Saves the given {@link ReadOnlyAddressBook} to the storage.
     * If saving fails, the previously saved address book must remain unchanged.
     * @param addressBook cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException;

    /**
     * Saves a Contacts change with its previous data in session undo history.
     * Identical data is saved without adding history. A failed backup or save preserves saved data and history.
     */
    void saveUndoableChange(ReadOnlyAddressBook previous, ReadOnlyAddressBook candidate) throws IOException;

    /**
     * Ends the undo session and attempts to remove its backup files without throwing cleanup errors.
     */
    void closeUndoHistory();

    /** Returns the file path of the independent favorites collection. */
    Path getFavouritesFilePath();

    /** Reads favorites, returning an empty optional if the file does not exist. */
    Optional<ReadOnlyAddressBook> readFavourites() throws DataLoadingException;

    /** Saves favorites atomically, preserving the previous file if saving fails. */
    void saveFavourites(ReadOnlyAddressBook favourites) throws IOException;

}
