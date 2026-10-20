package seedu.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.AddressBook;
import seedu.address.model.ContactSection;
import seedu.address.model.Model;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.AddressBookBuilder;

public class MainAppTest {
    @TempDir
    public Path temporaryFolder;

    private StorageManager storage;
    private final MainApp app = new MainApp();

    @BeforeEach
    public void setUp() {
        storage = new StorageManager(new JsonAddressBookStorage(temporaryFolder.resolve("contacts.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));
    }

    @Test
    public void initModelManager_missingFiles_usesSampleContactsAndEmptyFavouritesWithoutWriting() {
        Model model = app.initModelManager(storage, new UserPrefs());
        assertFalse(model.getAddressBook().getPersonList().isEmpty());
        assertTrue(model.getAddressBook(ContactSection.FAVOURITES).getPersonList().isEmpty());
        assertEquals(ContactSection.CONTACTS, model.getActiveSection());
        assertFalse(Files.exists(storage.getAddressBookFilePath()));
        assertFalse(Files.exists(storage.getFavouritesFilePath()));
    }

    @Test
    public void initModelManager_savedCollections_loadsBothAndStartsOnContacts() throws Exception {
        AddressBook saved = new AddressBookBuilder().withPerson(ALICE).build();
        storage.saveAddressBook(new AddressBook());
        storage.saveFavourites(saved);

        Model model = app.initModelManager(storage, new UserPrefs());

        assertEquals(ContactSection.CONTACTS, model.getActiveSection());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertEquals(List.of(ALICE), model.getAddressBook(ContactSection.FAVOURITES).getPersonList());
    }

    @Test
    public void initModelManager_invalidCollection_retainsOtherCollectionAndOriginalFiles() throws Exception {
        AddressBook saved = new AddressBookBuilder().withPerson(ALICE).build();
        storage.saveAddressBook(saved);
        Files.writeString(storage.getFavouritesFilePath(), "invalid favourites");

        Model model = app.initModelManager(storage, new UserPrefs());
        assertEquals(List.of(ALICE), model.getAddressBook().getPersonList());
        assertTrue(model.getAddressBook(ContactSection.FAVOURITES).getPersonList().isEmpty());
        assertEquals("invalid favourites", Files.readString(storage.getFavouritesFilePath()));

        storage.saveFavourites(saved);
        Files.writeString(storage.getAddressBookFilePath(), "invalid contacts");
        model = app.initModelManager(storage, new UserPrefs());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertEquals(List.of(ALICE), model.getAddressBook(ContactSection.FAVOURITES).getPersonList());
        assertEquals("invalid contacts", Files.readString(storage.getAddressBookFilePath()));
    }
}
