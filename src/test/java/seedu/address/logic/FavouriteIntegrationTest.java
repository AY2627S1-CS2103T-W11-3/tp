package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import seedu.address.logic.commands.FavouriteCommand;
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
import seedu.address.testutil.PersonBuilder;

public class FavouriteIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Logic logic;
    private StorageManager storage;
    private Person bobWithNote;

    @BeforeEach
    public void setUp() throws Exception {
        bobWithNote = new PersonBuilder(BOB).withNote("Met in class").build();
        AddressBook contacts = new AddressBookBuilder().withPerson(ALICE).withPerson(bobWithNote).build();
        model = new ModelManager(contacts, new UserPrefs());
        storage = new StorageManager(new JsonAddressBookStorage(temporaryFolder.resolve("contacts.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));
        storage.saveAddressBook(contacts);
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_filteredIndex_appendsCompleteCopyWithoutChangingSourceOrFilter() throws Exception {
        byte[] contactsBefore = Files.readAllBytes(storage.getAddressBookFilePath());
        logic.execute("favourite 1");
        logic.execute("find Bob");

        assertEquals("Added to Favourites: Bob Choo (index 1).",
                logic.execute("favourite 1").getFeedbackToUser());

        assertEquals(ContactSection.CONTACTS, logic.getActiveSection());
        assertEquals(List.of(bobWithNote), logic.getFilteredPersonList());
        assertEquals(List.of(ALICE, bobWithNote), storage.readFavourites().orElseThrow().getPersonList());
        assertArrayEquals(contactsBefore, Files.readAllBytes(storage.getAddressBookFilePath()));
        Model restarted = new ModelManager(storage.readAddressBook().orElseThrow(),
                storage.readFavourites().orElseThrow(), new UserPrefs());
        assertEquals(List.of(ALICE, bobWithNote), restarted.getAddressBook().getPersonList());
        assertEquals(List.of(ALICE, bobWithNote), restarted.getAddressBook(ContactSection.FAVOURITES).getPersonList());

        logic.execute("edit 1 note/Updated original");
        logic.execute("find Bob");
        logic.execute("delete 1");
        logic.execute("tab favourites");
        assertEquals(List.of(ALICE, bobWithNote), logic.getFilteredPersonList());
    }

    @Test
    public void execute_duplicateIdentity_rejectsDifferentDetailsButAllowsDifferentCase() throws Exception {
        logic.execute("favourite 1");
        logic.execute("edit 1 p/88888888");
        byte[] favouritesBefore = Files.readAllBytes(storage.getFavouritesFilePath());

        assertEquals(FavouriteCommand.MESSAGE_DUPLICATE,
                assertThrows(CommandException.class, () -> logic.execute("favourite 1")).getMessage());

        assertArrayEquals(favouritesBefore, Files.readAllBytes(storage.getFavouritesFilePath()));
        logic.execute("edit 1 n/alice pauline");
        logic.execute("favourite 1");
        assertEquals(2, storage.readFavourites().orElseThrow().getPersonList().size());
    }

    @Test
    public void execute_fromEmptyOrPopulatedFavourites_reportsWrongSectionWithoutChanges() throws Exception {
        logic.execute("tab favourites");
        assertEquals(FavouriteCommand.MESSAGE_WRONG_SECTION,
                assertThrows(CommandException.class, () -> logic.execute("favourite 1")).getMessage());
        assertFalse(Files.exists(storage.getFavouritesFilePath()));

        logic.execute("tab contacts");
        logic.execute("favourite 1");
        logic.execute("tab favourites");
        byte[] favouritesBefore = Files.readAllBytes(storage.getFavouritesFilePath());
        assertEquals(FavouriteCommand.MESSAGE_WRONG_SECTION,
                assertThrows(CommandException.class, () -> logic.execute("favourite 999")).getMessage());
        assertArrayEquals(favouritesBefore, Files.readAllBytes(storage.getFavouritesFilePath()));
        assertEquals(List.of(ALICE), logic.getFilteredPersonList());
    }

    @Test
    public void execute_outOfRangeAndEmptySearch_preserveDataWithoutCreatingFavouritesFile() throws Exception {
        byte[] contactsBefore = Files.readAllBytes(storage.getAddressBookFilePath());
        for (String index : List.of("3", "9".repeat(1000))) {
            assertEquals(String.format(FavouriteCommand.MESSAGE_INDEX_OUT_OF_RANGE, 2),
                    assertThrows(CommandException.class, () -> logic.execute("favourite " + index)).getMessage());
        }
        logic.execute("find Nobody");
        assertEquals(FavouriteCommand.MESSAGE_EMPTY_LIST,
                assertThrows(CommandException.class, () -> logic.execute("favourite 1")).getMessage());
        assertFalse(Files.exists(storage.getFavouritesFilePath()));
        assertTrue(model.getAddressBook(ContactSection.FAVOURITES).getPersonList().isEmpty());
        assertArrayEquals(contactsBefore, Files.readAllBytes(storage.getAddressBookFilePath()));
    }

    @Test
    public void execute_writeFailure_preservesAbsentFileAndLiveState() throws Exception {
        useFailingFavouritesStorage(false);
        assertFailedFavouritePreservesLiveState("Alice");
        assertFalse(Files.exists(storage.getFavouritesFilePath()));
        assertTrue(model.getAddressBook(ContactSection.FAVOURITES).getPersonList().isEmpty());
    }

    @Test
    public void execute_replacementFailure_preservesExistingFileAndLiveState() throws Exception {
        logic.execute("favourite 1");
        byte[] favouritesBefore = Files.readAllBytes(storage.getFavouritesFilePath());
        useFailingFavouritesStorage(true);

        assertFailedFavouritePreservesLiveState("Bob");

        assertArrayEquals(favouritesBefore, Files.readAllBytes(storage.getFavouritesFilePath()));
        assertEquals(List.of(ALICE), model.getAddressBook(ContactSection.FAVOURITES).getPersonList());
    }

    private void assertFailedFavouritePreservesLiveState(String keyword) throws Exception {
        logic.execute("find " + keyword);
        List<Person> displayedBefore = List.copyOf(logic.getFilteredPersonList());
        AtomicInteger notifications = new AtomicInteger();
        logic.getFilteredPersonList().addListener((ListChangeListener<Person>) change
            -> notifications.incrementAndGet());
        byte[] contactsBefore = Files.readAllBytes(storage.getAddressBookFilePath());

        assertEquals(LogicManager.MESSAGE_SAVE_ERROR,
                assertThrows(CommandException.class, () -> logic.execute("favourite 1")).getMessage());

        assertEquals(ContactSection.CONTACTS, logic.getActiveSection());
        assertEquals(displayedBefore, logic.getFilteredPersonList());
        assertEquals(0, notifications.get());
        assertArrayEquals(contactsBefore, Files.readAllBytes(storage.getAddressBookFilePath()));
    }

    private void useFailingFavouritesStorage(boolean replacementFailure) {
        JsonAddressBookStorage failing = new JsonAddressBookStorage(storage.getFavouritesFilePath()) {
            @Override
            protected void writeAddressBook(ReadOnlyAddressBook addressBook, Path temporaryFile) throws IOException {
                super.writeAddressBook(addressBook, temporaryFile);
                if (!replacementFailure) {
                    throw new IOException("Simulated write failure");
                }
            }

            @Override
            protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
                throw new IOException("Simulated replacement failure");
            }
        };
        storage = new StorageManager(new JsonAddressBookStorage(storage.getAddressBookFilePath()), failing,
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));
        logic = new LogicManager(model, storage);
    }
}
