package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.ContactSection;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.AddressBookBuilder;

public class TabIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Logic logic;
    private StorageManager storage;

    @BeforeEach
    public void setUp() throws Exception {
        AddressBook contacts = new AddressBookBuilder().withPerson(ALICE).build();
        AddressBook favourites = new AddressBookBuilder().withPerson(BOB).build();
        model = new ModelManager(contacts, favourites, new UserPrefs());
        storage = new StorageManager(new JsonAddressBookStorage(temporaryFolder.resolve("contacts.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));
        storage.saveAddressBook(contacts);
        storage.saveFavourites(favourites);
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_contactCommandsInFavourites_updateOnlyFavouritesFile() throws Exception {
        byte[] contactsBefore = Files.readAllBytes(storage.getAddressBookFilePath());
        assertEquals("Showing Favourites.", logic.execute("tab favourites").getFeedbackToUser());
        assertEquals(ContactSection.FAVOURITES, logic.getActiveSection());
        assertEquals(storage.getFavouritesFilePath(), logic.getActiveDataFilePath());
        logic.execute("add n/Charlie p/12345678 e/charlie@example.com a/NUS note/New friend");
        logic.execute("find Charlie");
        assertEquals(1, logic.getFilteredPersonList().size());
        assertTrue(logic.execute("view 1").getFeedbackToUser().contains("Note: New friend"));
        logic.execute("edit 1 note/Updated note");
        logic.execute("find Charlie");
        assertTrue(logic.execute("view 1").getFeedbackToUser().contains("Note: Updated note"));
        logic.execute("delete 1");
        logic.execute("list");
        assertEquals(List.of(BOB), logic.getFilteredPersonList());
        assertEquals(List.of(BOB), storage.readFavourites().orElseThrow().getPersonList());
        logic.execute("clear");
        assertTrue(storage.readFavourites().orElseThrow().getPersonList().isEmpty());
        assertArrayEquals(contactsBefore, Files.readAllBytes(storage.getAddressBookFilePath()));

        assertEquals("Showing Contacts.", logic.execute("tab contacts").getFeedbackToUser());
        assertEquals(List.of(ALICE), logic.getFilteredPersonList());
        assertEquals(storage.getAddressBookFilePath(), logic.getActiveDataFilePath());
        logic.execute("delete 1");
        assertTrue(storage.readAddressBook().orElseThrow().getPersonList().isEmpty());
    }

    @Test
    public void execute_navigationAndReadOnlyCommands_doNotSaveEitherFile() throws Exception {
        StorageManager readOnlyStorage = new StorageManager(
                new JsonAddressBookStorage(storage.getAddressBookFilePath()),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) {
                throw new AssertionError("Navigation must not save contacts");
            }

            @Override
            public void saveFavourites(ReadOnlyAddressBook favourites) {
                throw new AssertionError("Navigation must not save favorites");
            }
        };
        logic = new LogicManager(model, readOnlyStorage);
        logic.execute("tab favourites");
        logic.execute("find Bob");
        logic.execute("view 1");
        logic.execute("list");
        logic.execute("tab contacts");
        assertEquals(List.of(ALICE), logic.getFilteredPersonList());
    }

    @Test
    public void execute_favouritesSaveFailures_preserveBothFilesFilterAndObservableState() throws Exception {
        byte[] contactsBefore = Files.readAllBytes(storage.getAddressBookFilePath());
        byte[] favouritesBefore = Files.readAllBytes(storage.getFavouritesFilePath());
        JsonAddressBookStorage failing = new JsonAddressBookStorage(storage.getFavouritesFilePath()) {
            @Override
            protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
                throw new IOException("Simulated replacement failure");
            }
        };
        storage = new StorageManager(new JsonAddressBookStorage(storage.getAddressBookFilePath()), failing,
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));
        logic = new LogicManager(model, storage);
        logic.execute("tab favourites");
        logic.execute("find Bob");
        AtomicInteger notifications = new AtomicInteger();
        logic.getFilteredPersonList().addListener((ListChangeListener<Person>) change
            -> notifications.incrementAndGet());

        assertEquals(LogicManager.MESSAGE_SAVE_ERROR,
                assertThrows(CommandException.class, () -> logic.execute("edit 1 p/88888888")).getMessage());
        assertEquals(DeleteCommand.MESSAGE_SAVE_ERROR,
                assertThrows(CommandException.class, () -> logic.execute("delete 1")).getMessage());

        assertEquals(ContactSection.FAVOURITES, logic.getActiveSection());
        assertEquals(List.of(BOB), logic.getFilteredPersonList());
        assertEquals(0, notifications.get());
        assertArrayEquals(contactsBefore, Files.readAllBytes(storage.getAddressBookFilePath()));
        assertArrayEquals(favouritesBefore, Files.readAllBytes(storage.getFavouritesFilePath()));
        logic.execute("tab contacts");
        assertEquals(List.of(ALICE), logic.getFilteredPersonList());
    }
}
