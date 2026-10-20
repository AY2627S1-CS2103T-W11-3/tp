package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.testutil.PersonBuilder;

public class StorageManagerTest {

    @TempDir
    public Path testFolder;

    private StorageManager storageManager;

    @BeforeEach
    public void setUp() {
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(getTempFilePath("ab"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(getTempFilePath("prefs"));
        storageManager = new StorageManager(addressBookStorage, userPrefsStorage);
    }

    private Path getTempFilePath(String fileName) {
        return testFolder.resolve(fileName);
    }

    @Test
    public void prefsReadSave() throws Exception {
        /*
         * Note: This is an integration test that verifies the StorageManager is properly wired to the
         * {@link JsonUserPrefsStorage} class.
         * More extensive testing of UserPref saving/reading is done in {@link JsonUserPrefsStorageTest} class.
         */
        UserPrefs original = new UserPrefs();
        original.setGuiSettings(new GuiSettings(300, 600, 4, 6));
        storageManager.saveUserPrefs(original);
        UserPrefs retrieved = storageManager.readUserPrefs().get();
        assertEquals(original, retrieved);
    }

    @Test
    public void addressBookReadSave() throws Exception {
        /*
         * Note: This is an integration test that verifies the StorageManager is properly wired to the
         * {@link JsonAddressBookStorage} class.
         * More extensive testing of UserPref saving/reading is done in {@link JsonAddressBookStorageTest} class.
         */
        AddressBook original = getTypicalAddressBook();
        storageManager.saveAddressBook(original);
        ReadOnlyAddressBook retrieved = storageManager.readAddressBook().get();
        assertEquals(original, new AddressBook(retrieved));
    }

    @Test
    public void getAddressBookFilePath() {
        assertNotNull(storageManager.getAddressBookFilePath());
    }

    @Test
    public void readFavourites_missingFile_returnsEmptyWithoutCreatingFiles() throws Exception {
        assertEquals(getTempFilePath("favourites.json"), storageManager.getFavouritesFilePath());
        assertTrue(storageManager.readFavourites().isEmpty());
        assertFalse(Files.exists(storageManager.getFavouritesFilePath()));
        assertFalse(Files.exists(storageManager.getAddressBookFilePath()));
    }

    @Test
    public void saveFavourites_contactWithTagsAndNote_roundTripsIndependently() throws Exception {
        AddressBook contacts = getTypicalAddressBook();
        storageManager.saveAddressBook(contacts);
        byte[] contactsBefore = Files.readAllBytes(storageManager.getAddressBookFilePath());
        AddressBook favourites = new AddressBook();
        favourites.addPerson(new PersonBuilder(ALICE).withNote("Met in class").build());

        storageManager.saveFavourites(favourites);

        assertEquals(favourites, new AddressBook(storageManager.readFavourites().orElseThrow()));
        assertArrayEquals(contactsBefore, Files.readAllBytes(storageManager.getAddressBookFilePath()));
        byte[] favouritesBefore = Files.readAllBytes(storageManager.getFavouritesFilePath());

        storageManager.saveAddressBook(new AddressBook());

        assertArrayEquals(favouritesBefore, Files.readAllBytes(storageManager.getFavouritesFilePath()));
        assertEquals(favourites, new AddressBook(storageManager.readFavourites().orElseThrow()));

        storageManager.saveFavourites(new AddressBook());
        assertTrue(storageManager.readFavourites().orElseThrow().getPersonList().isEmpty());
    }

    @Test
    public void readFavourites_invalidFile_preservesFileAndReadableContacts() throws Exception {
        AddressBook contacts = getTypicalAddressBook();
        storageManager.saveAddressBook(contacts);
        Files.writeString(storageManager.getFavouritesFilePath(), "invalid favourites");

        assertThrows(DataLoadingException.class, storageManager::readFavourites);

        assertEquals("invalid favourites", Files.readString(storageManager.getFavouritesFilePath()));
        assertEquals(contacts, new AddressBook(storageManager.readAddressBook().orElseThrow()));
    }

    @Test
    public void saveFavourites_customPath_usesConfiguredStorage() throws Exception {
        Path favouritesPath = getTempFilePath("custom-favourites.json");
        StorageManager customStorage = new StorageManager(
                new JsonAddressBookStorage(storageManager.getAddressBookFilePath()),
                new JsonAddressBookStorage(favouritesPath),
                new JsonUserPrefsStorage(getTempFilePath("prefs")));
        AddressBook favourites = getTypicalAddressBook();

        customStorage.saveFavourites(favourites);

        assertEquals(favouritesPath, customStorage.getFavouritesFilePath());
        assertEquals(favourites, new AddressBook(customStorage.readFavourites().orElseThrow()));
        assertFalse(Files.exists(storageManager.getFavouritesFilePath()));
        assertFalse(Files.exists(storageManager.getAddressBookFilePath()));
    }

    @Test
    public void saveFavourites_replacementFailure_preservesBothFiles() throws Exception {
        AddressBook original = getTypicalAddressBook();
        storageManager.saveAddressBook(original);
        storageManager.saveFavourites(original);
        byte[] contactsBefore = Files.readAllBytes(storageManager.getAddressBookFilePath());
        byte[] favouritesBefore = Files.readAllBytes(storageManager.getFavouritesFilePath());
        JsonAddressBookStorage failingStorage = new JsonAddressBookStorage(storageManager.getFavouritesFilePath()) {
            @Override
            protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
                throw new IOException("Simulated replacement failure");
            }
        };
        StorageManager storage = new StorageManager(
                new JsonAddressBookStorage(storageManager.getAddressBookFilePath()), failingStorage,
                new JsonUserPrefsStorage(getTempFilePath("prefs")));

        assertThrows(IOException.class, () -> storage.saveFavourites(new AddressBook()));

        assertArrayEquals(contactsBefore, Files.readAllBytes(storage.getAddressBookFilePath()));
        assertArrayEquals(favouritesBefore, Files.readAllBytes(storage.getFavouritesFilePath()));
        assertEquals(original, new AddressBook(storage.readFavourites().orElseThrow()));
    }

}
