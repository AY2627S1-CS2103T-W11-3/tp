package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.GuiSettings;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;

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
    public void saveCollections_success_savesBothAndRemovesJournal() throws Exception {
        JsonAddressBookStorage contacts = new JsonAddressBookStorage(getTempFilePath("contacts.json"));
        JsonAddressBookStorage favourites = new JsonAddressBookStorage(getTempFilePath("favourites.json"));
        CollectionSaveTransaction transaction = new CollectionSaveTransaction(contacts, favourites);

        transaction.save(getTypicalAddressBook(), new AddressBook());
        transaction.recover();

        assertEquals(getTypicalAddressBook(), contacts.readAddressBook().orElseThrow());
        assertEquals(new AddressBook(), favourites.readAddressBook().orElseThrow());
        assertFalse(Files.exists(getTempFilePath(".contacts.json-pending-save.json")));
    }

    @Test
    public void saveCollections_secondReplacementFails_restoresExactOriginals() throws Exception {
        for (boolean contactsExisted : new boolean[] {false, true}) {
            for (boolean favouritesExisted : new boolean[] {false, true}) {
                Path folder = Files.createTempDirectory(testFolder, "rollback-");
                Path contactsFile = folder.resolve("contacts.json");
                Path favouritesFile = folder.resolve("favourites.json");
                byte[] original = "original bytes, including invalid JSON".getBytes(StandardCharsets.UTF_8);
                if (contactsExisted) {
                    Files.write(contactsFile, original);
                }
                if (favouritesExisted) {
                    Files.write(favouritesFile, original);
                }
                JsonAddressBookStorage failing = new JsonAddressBookStorage(favouritesFile) {
                    @Override
                    protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
                        throw new IOException("Simulated replacement failure");
                    }
                };
                CollectionSaveTransaction transaction = new CollectionSaveTransaction(
                        new JsonAddressBookStorage(contactsFile), failing);

                assertThrows(IOException.class, () -> transaction.save(getTypicalAddressBook(), new AddressBook()));

                assertEquals(contactsExisted, Files.exists(contactsFile));
                assertEquals(favouritesExisted, Files.exists(favouritesFile));
                if (contactsExisted) {
                    assertArrayEquals(original, Files.readAllBytes(contactsFile));
                }
                if (favouritesExisted) {
                    assertArrayEquals(original, Files.readAllBytes(favouritesFile));
                }
                assertFalse(Files.exists(folder.resolve(".contacts.json-pending-save.json")));
            }
        }
    }

    @Test
    public void recover_interruptedSave_restoresBothBeforeRetry() throws Exception {
        Path contactsFile = getTempFilePath("contacts.json");
        Path favouritesFile = getTempFilePath("favourites.json");
        JsonAddressBookStorage contacts = new JsonAddressBookStorage(contactsFile);
        contacts.saveAddressBook(new AddressBook());
        byte[] original = Files.readAllBytes(contactsFile);
        JsonAddressBookStorage interrupted = new JsonAddressBookStorage(favouritesFile) {
            @Override
            protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
                super.replaceAddressBook(temporaryFile, destination);
                throw new AssertionError("Simulated process interruption");
            }
        };
        CollectionSaveTransaction transaction = new CollectionSaveTransaction(contacts, interrupted);
        assertThrows(AssertionError.class, () -> transaction.save(getTypicalAddressBook(), new AddressBook()));

        CollectionSaveTransaction restarted = new CollectionSaveTransaction(
                contacts, new JsonAddressBookStorage(favouritesFile));
        Files.delete(contactsFile);
        Files.createDirectory(contactsFile);
        Path obstruction = contactsFile.resolve("obstruction");
        Files.writeString(obstruction, "Prevent replacement during recovery");
        assertThrows(IOException.class, restarted::recover);
        assertTrue(Files.exists(getTempFilePath(".contacts.json-pending-save.json")));
        Files.delete(obstruction);
        Files.delete(contactsFile);
        restarted.recover();
        restarted.recover();

        assertArrayEquals(original, Files.readAllBytes(contactsFile));
        assertFalse(Files.exists(favouritesFile));
        restarted.save(getTypicalAddressBook(), new AddressBook());
        assertEquals(getTypicalAddressBook(), contacts.readAddressBook().orElseThrow());
    }

    @Test
    public void recover_invalidJournal_preservesSavedData() throws Exception {
        Path contactsFile = getTempFilePath("contacts.json");
        byte[] original = "Original contact data".getBytes(StandardCharsets.UTF_8);
        Files.write(contactsFile, original);
        CollectionSaveTransaction transaction = new CollectionSaveTransaction(
                new JsonAddressBookStorage(contactsFile),
                new JsonAddressBookStorage(getTempFilePath("favourites.json")));

        for (String journal : List.of("null", "{}", "{\"originals\":[]}")) {
            Files.writeString(getTempFilePath(".contacts.json-pending-save.json"), journal);
            assertThrows(IOException.class, transaction::recover);
            assertArrayEquals(original, Files.readAllBytes(contactsFile));
        }
    }
}
