package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.BOB;
import static seedu.address.testutil.TypicalPersons.DANIEL;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.FavouriteCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.RecentCommand;
import seedu.address.logic.commands.UndoCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ContactSection;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Note;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonContainsKeywordsPredicate;
import seedu.address.model.tag.Tag;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.PersonUtil;

public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy IO exception");
    private static final IOException DUMMY_AD_EXCEPTION = new AccessDeniedException("dummy access denied exception");

    @TempDir
    public Path temporaryFolder;

    private Model model = new ModelManager();
    private Logic logic;

    @BeforeEach
    public void setUp() {
        JsonAddressBookStorage addressBookStorage =
                new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        StorageManager storage = new StorageManager(addressBookStorage, userPrefsStorage);
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_invalidCommandFormat_throwsParseException() {
        String invalidCommand = "uicfhmowqewca";
        assertParseException(invalidCommand, MESSAGE_UNKNOWN_COMMAND);
    }

    @Test
    public void execute_deleteFromEmptyList_throwsEmptyListError() {
        String deleteCommand = "delete 9";
        assertCommandException(deleteCommand, DeleteCommand.MESSAGE_EMPTY_LIST);
    }

    @Test
    public void execute_validCommand_success() throws Exception {
        String listCommand = ListCommand.COMMAND_WORD;
        assertCommandSuccess(listCommand, ListCommand.MESSAGE_SUCCESS, model);
    }

    @Test
    public void execute_favouriteFilteredContact_copiesIndependentlyAndKeepsContext() throws Exception {
        Person target = new PersonBuilder(BOB).withNote("Met in class").build();
        model.addPerson(ALICE);
        model.addPerson(target);
        logic.execute("find Bob");

        assertEquals("Added to Favourites: Bob Choo (index 1).",
                logic.execute("favourite 1").getFeedbackToUser());

        assertEquals(ContactSection.CONTACTS, logic.getActiveSection());
        assertEquals(List.of(target), logic.getFilteredPersonList());
        assertEquals(List.of(ALICE, target), model.getAddressBook().getPersonList());
        assertFalse(Files.exists(temporaryFolder.resolve("addressBook.json")));

        logic.execute("edit 1 note/Updated original");
        assertEquals(new Note("Updated original"), model.getAddressBook().getPersonList().get(1).getNote());
        assertEquals(List.of(target), model.getAddressBook(ContactSection.FAVOURITES).getPersonList());
        assertEquals(List.of(target), new JsonAddressBookStorage(temporaryFolder.resolve("favourites.json"))
                .readAddressBook().orElseThrow().getPersonList());
    }

    @Test
    public void execute_favouriteInvalidSelectionDuplicateAndWrongSection_reportDistinctErrors() throws Exception {
        assertThrows(CommandException.class, FavouriteCommand.MESSAGE_EMPTY_LIST, () -> logic.execute("favourite 1"));
        model.addPerson(ALICE);
        assertThrows(CommandException.class,
                String.format(FavouriteCommand.MESSAGE_INDEX_OUT_OF_RANGE, 1), () -> logic.execute("favourite 2"));
        logic.execute("favourite 1");
        logic.execute("edit 1 p/88888888");
        assertThrows(CommandException.class, FavouriteCommand.MESSAGE_DUPLICATE, () -> logic.execute("favourite 1"));
        logic.execute("tab favourites");
        assertThrows(CommandException.class, FavouriteCommand.MESSAGE_WRONG_SECTION, ()
            -> logic.execute("favourite 1"));
        assertEquals(List.of(ALICE), logic.getFilteredPersonList());
    }

    @Test
    public void execute_tab_switchesContextAndScopesContactChanges() throws Exception {
        model.addPerson(ALICE);
        logic.execute("tab favourites");
        assertEquals(ContactSection.FAVOURITES, logic.getActiveSection());
        assertTrue(logic.getFilteredPersonList().isEmpty());
        logic.execute(PersonUtil.getAddCommand(BOB));
        logic.execute("find Nobody");
        logic.execute("tab favourites");
        assertTrue(logic.getFilteredPersonList().isEmpty());
        logic.execute("tab contacts");
        assertEquals(List.of(ALICE), logic.getFilteredPersonList());
        logic.execute("tab favourites");
        assertEquals(List.of(BOB), logic.getFilteredPersonList());
        assertEquals(temporaryFolder.resolve("favourites.json"), logic.getActiveDataFilePath());
        logic.execute("delete 1");
        assertTrue(new JsonAddressBookStorage(temporaryFolder.resolve("favourites.json"))
                .readAddressBook().orElseThrow().getPersonList().isEmpty());
        assertEquals(List.of(ALICE), model.getAddressBook(ContactSection.CONTACTS).getPersonList());
    }

    @Test
    public void execute_favouriteSaveFailure_preservesContextAndContacts() {
        model.addPerson(ALICE);
        StorageManager failing = new StorageManager(
                new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"))) {
            @Override
            public void saveFavourites(ReadOnlyAddressBook favourites) throws IOException {
                throw DUMMY_IO_EXCEPTION;
            }
        };
        logic = new LogicManager(model, failing);

        assertThrows(CommandException.class, LogicManager.MESSAGE_SAVE_ERROR, () -> logic.execute("favourite 1"));

        assertEquals(ContactSection.CONTACTS, logic.getActiveSection());
        assertEquals(List.of(ALICE), logic.getFilteredPersonList());
        assertTrue(model.getAddressBook(ContactSection.FAVOURITES).getPersonList().isEmpty());
    }

    @Test
    public void execute_recentCommand_updatesDisplayedList() throws Exception {
        model.addPerson(ALICE);
        model.addPerson(BOB);

        CommandResult result = logic.execute(RecentCommand.COMMAND_WORD);

        assertEquals(RecentCommand.MESSAGE_SUCCESS, result.getFeedbackToUser());
        assertEquals(List.of(BOB, ALICE), logic.getFilteredPersonList());
        assertEquals(List.of(ALICE, BOB), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_favouritesChanges_preserveContactsUndoHistory() throws Exception {
        model.addPerson(ALICE);
        logic.execute(PersonUtil.getAddCommand(BOB));
        logic.execute("favourite 1");
        logic.execute("tab favourites");
        logic.execute("edit 1 note/Edited favourite");
        logic.execute("delete 1");
        assertThrows(CommandException.class, UndoCommand.MESSAGE_WRONG_SECTION, () -> logic.execute("undo"));
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertEquals(List.of(ALICE, BOB), model.getAddressBook(ContactSection.CONTACTS).getPersonList());

        logic.execute("tab contacts");
        logic.execute("undo");
        assertEquals(List.of(ALICE), model.getAddressBook().getPersonList());
        assertEquals(List.of(ALICE), new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"))
                .readAddressBook().orElseThrow().getPersonList());
        assertTrue(new JsonAddressBookStorage(temporaryFolder.resolve("favourites.json"))
                .readAddressBook().orElseThrow().getPersonList().isEmpty());
    }

    @Test
    public void execute_favouriteAfterRecent_copiesDisplayedContact() throws Exception {
        model.addPerson(ALICE);
        model.addPerson(BOB);
        logic.execute("recent");

        logic.execute("favourite 1");

        assertEquals(List.of(BOB), model.getAddressBook(ContactSection.FAVOURITES).getPersonList());
        assertEquals(List.of(BOB, ALICE), logic.getFilteredPersonList());
        logic.execute("tab favourites");
        logic.execute("recent");
        assertEquals(List.of(BOB), logic.getFilteredPersonList());
        logic.execute("tab contacts");
        assertEquals(List.of(ALICE, BOB), logic.getFilteredPersonList());
    }

    @Test
    public void execute_editFromTagFilter_preservesMatchingContacts() throws Exception {
        model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        JsonAddressBookStorage addressBookStorage =
                new JsonAddressBookStorage(temporaryFolder.resolve("filter-edit.json"));
        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("filter-edit-prefs.json"));
        logic = new LogicManager(model, new StorageManager(addressBookStorage, userPrefsStorage));

        logic.execute("filter t/friends");
        assertEquals(List.of(ALICE, BENSON, DANIEL), logic.getFilteredPersonList());

        logic.execute("edit 1 p/80001111");
        Person editedAlice = new PersonBuilder(ALICE).withPhone("80001111").build();
        assertEquals(List.of(editedAlice, BENSON, DANIEL), logic.getFilteredPersonList());

        logic.execute("edit 1 t/family");
        assertEquals(List.of(BENSON, DANIEL), logic.getFilteredPersonList());
    }

    @Test
    public void execute_viewAfterFilter_keepsActiveFilter() throws Exception {
        model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        JsonAddressBookStorage addressBookStorage =
                new JsonAddressBookStorage(temporaryFolder.resolve("filter-view.json"));
        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("filter-view-prefs.json"));
        logic = new LogicManager(model, new StorageManager(addressBookStorage, userPrefsStorage));

        logic.execute("filter t/owesMoney t/friends");
        assertEquals("friends, owesMoney", logic.getActiveFilter());

        logic.execute("view 1");
        assertEquals("friends, owesMoney", logic.getActiveFilter());
        assertEquals(List.of(BENSON), logic.getFilteredPersonList());

        logic.execute("list");
        assertEquals("", logic.getActiveFilter());
    }

    @Test
    public void execute_storageThrowsIoException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_IO_EXCEPTION, LogicManager.MESSAGE_SAVE_ERROR);
    }

    @Test
    public void execute_storageThrowsAdException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_AD_EXCEPTION, LogicManager.MESSAGE_SAVE_ERROR);
    }

    @Test
    public void execute_deleteSaveFailure_preservesDiskAndObservableState() throws Exception {
        for (boolean existingFile : new boolean[] {true, false}) {
            for (int failureMode : new int[] {0, 1, 2}) {
                Path folder = Files.createTempDirectory(temporaryFolder, "delete-failure-");
                Path destination = folder.resolve("contacts.json");
                model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
                model.updateFilteredPersonList(new PersonContainsKeywordsPredicate(List.of("Meier")));
                AddressBook original = new AddressBook(model.getAddressBook());
                List<Person> displayed = List.copyOf(model.getFilteredPersonList());
                byte[] originalBytes = new byte[0];
                if (existingFile) {
                    new JsonAddressBookStorage(destination).saveAddressBook(original);
                    originalBytes = Files.readAllBytes(destination);
                }
                AtomicInteger changes = new AtomicInteger();
                model.getFilteredPersonList().addListener(
                        (ListChangeListener<Person>) change -> changes.incrementAndGet());
                model.getAddressBook().getPersonList().addListener(
                        (ListChangeListener<Person>) change -> changes.incrementAndGet());
                JsonAddressBookStorage failingStorage = new JsonAddressBookStorage(destination) {
                    @Override
                    protected void writeAddressBook(ReadOnlyAddressBook data, Path temporaryFile) throws IOException {
                        if (failureMode == 0) {
                            Files.writeString(temporaryFile, "partial JSON");
                            throw new IOException("Simulated partial write");
                        }
                        super.writeAddressBook(data, temporaryFile);
                    }

                    @Override
                    protected void replaceAddressBook(Path temporaryFile, Path target) throws IOException {
                        if (failureMode == 2) {
                            throw new AccessDeniedException(target.toString());
                        }
                        throw new AtomicMoveNotSupportedException(temporaryFile.toString(), target.toString(),
                                "Simulated unsupported atomic replacement");
                    }
                };
                logic = new LogicManager(model, new StorageManager(failingStorage,
                        new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));

                assertThrows(CommandException.class, DeleteCommand.MESSAGE_SAVE_ERROR, () -> logic.execute("delete 2"));

                assertEquals(original, model.getAddressBook());
                assertEquals(displayed, model.getFilteredPersonList());
                assertEquals(0, changes.get());
                if (existingFile) {
                    assertArrayEquals(originalBytes, Files.readAllBytes(destination));
                } else {
                    assertFalse(Files.exists(destination));
                }
                try (var files = Files.list(folder)) {
                    assertEquals(existingFile ? 1L : 0L, files.count());
                }
            }
        }
    }

    @Test
    public void execute_deleteSuccess_savesOnceBeforePublishingAndReloads() throws Exception {
        Path destination = temporaryFolder.resolve("delete-success.json");
        model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.updateFilteredPersonList(new PersonContainsKeywordsPredicate(List.of("Meier")));
        Person target = model.getFilteredPersonList().get(1);
        AddressBook expected = new AddressBook(model.getAddressBook());
        expected.removePerson(target);
        List<Person> remainingResults = List.of(model.getFilteredPersonList().get(0));
        AtomicInteger saves = new AtomicInteger();
        JsonAddressBookStorage savingStorage = new JsonAddressBookStorage(destination) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook data) throws IOException {
                assertEquals(2, model.getFilteredPersonList().size());
                assertEquals(expected, data);
                saves.incrementAndGet();
                super.saveAddressBook(data);
            }
        };
        logic = new LogicManager(model, new StorageManager(savingStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));

        assertEquals("Deleted contact: Daniel Meier (index 2).", logic.execute("delete 2").getFeedbackToUser());

        assertEquals(1, saves.get());
        assertEquals(expected, model.getAddressBook());
        assertEquals(remainingResults, model.getFilteredPersonList());
        assertEquals(expected, new AddressBook(
                new JsonAddressBookStorage(destination).readAddressBook().orElseThrow()));
    }

    @Test
    public void execute_deleteSearchResult_preservesOtherNotesAndSharedTagsAfterReload() throws Exception {
        logic.execute("add n/Sarah Lim p/91234567 e/sarah@example.com a/NUS t/project note/Met at workshop");
        logic.execute("add n/Alex Tan p/98765432 e/alex@example.com a/NUS t/project note/Project teammate");
        Person remaining = model.getAddressBook().getPersonList().get(1);
        logic.execute("find Sarah");

        assertEquals("Deleted contact: Sarah Lim (index 1).", logic.execute("delete 1").getFeedbackToUser());
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(List.of(remaining), model.getAddressBook().getPersonList());
        JsonAddressBookStorage saved = new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"));
        assertEquals(List.of(remaining), saved.readAddressBook().orElseThrow().getPersonList());
    }

    @Test
    public void execute_invalidDelete_precedencePreservesSavedData() throws Exception {
        logic.execute("add n/Sarah Lim p/91234567 e/sarah@example.com a/NUS note/Workshop");
        Path saved = temporaryFolder.resolve("addressBook.json");
        byte[] before = Files.readAllBytes(saved);
        AddressBook original = new AddressBook(model.getAddressBook());
        String[][] cases = {
            {"delete", DeleteCommand.MESSAGE_MISSING_INDEX},
            {"delete abc 2", DeleteCommand.MESSAGE_MULTIPLE_INDICES},
            {"delete 01", DeleteCommand.MESSAGE_INVALID_INDEX},
            {"delete +1", DeleteCommand.MESSAGE_INVALID_INDEX}
        };
        for (boolean empty : new boolean[] {false, true}) {
            if (empty) {
                logic.execute("find nobody");
            }
            List<Person> displayed = List.copyOf(model.getFilteredPersonList());
            for (String[] input : cases) {
                assertThrows(ParseException.class, input[1], () -> logic.execute(input[0]));
            }
            String rangeMessage = empty ? DeleteCommand.MESSAGE_EMPTY_LIST
                    : "Contact index out of range. Choose an index from 1 to 1.";
            assertThrows(CommandException.class, rangeMessage, () -> logic.execute("delete " + "9".repeat(1000)));
            assertEquals(original, model.getAddressBook());
            assertEquals(displayed, model.getFilteredPersonList());
            assertArrayEquals(before, Files.readAllBytes(saved));
        }
    }

    @Test
    public void getFilteredPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> logic.getFilteredPersonList().remove(0));
    }

    @Test
    public void execute_contactChanges_restoreAllFieldsAfterEachSave() throws Exception {
        JsonAddressBookStorage saved = new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"));
        logic.execute("add n/John Tan p/91234567 e/john@example.com a/NUS t/CS2103T t/backend"
                + " note/Met during project discussion");
        assertEquals(model.getAddressBook(), saved.readAddressBook().orElseThrow());
        logic.execute("edit 1 NOTE/Discussed frontend implementation");
        Person restored = saved.readAddressBook().orElseThrow().getPersonList().getFirst();
        assertEquals(new Note("Discussed frontend implementation"), restored.getNote());
        assertTrue(restored.getTags().containsAll(List.of(new Tag("CS2103T"), new Tag("backend"))));
        logic.execute("edit 1 p/98765432");
        assertEquals(restored.getNote(), saved.readAddressBook().orElseThrow().getPersonList().getFirst().getNote());

        model.addPerson(ALICE);
        logic.execute("find John");
        logic.execute("delete 1");
        assertEquals(List.of(ALICE), saved.readAddressBook().orElseThrow().getPersonList());
        assertTrue(model.getFilteredPersonList().isEmpty());
        logic.execute("clear");
        assertTrue(saved.readAddressBook().orElseThrow().getPersonList().isEmpty());
    }

    @Test
    public void execute_clearAfterCorruptedFile_savesEmptyContactsThatReloadWithoutError() throws Exception {
        Path dataPath = temporaryFolder.resolve("addressBook.json");
        Files.writeString(dataPath, "invalid JSON");
        JsonAddressBookStorage saved = new JsonAddressBookStorage(dataPath);
        assertThrows(DataLoadingException.class, saved::readAddressBook);
        // The empty model matches the startup fallback after a loading error.
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertEquals(ClearCommand.MESSAGE_SUCCESS, logic.execute("clear").getFeedbackToUser());

        ReadOnlyAddressBook reloaded = new JsonAddressBookStorage(dataPath).readAddressBook().orElseThrow();
        Model restarted = new ModelManager(reloaded, new UserPrefs());
        assertEquals(new AddressBook(), restarted.getAddressBook());
        assertThrows(CommandException.class, UndoCommand.MESSAGE_NO_HISTORY, () -> logic.execute("undo"));
    }

    @Test
    public void execute_saveFailure_preservesDataAndFilterUntilRetry() throws Exception {
        Path dataPath = temporaryFolder.resolve("retry.json");
        AtomicBoolean failSave = new AtomicBoolean(false);
        JsonAddressBookStorage saved = new JsonAddressBookStorage(dataPath) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                if (failSave.get()) {
                    throw DUMMY_IO_EXCEPTION;
                }
                super.saveAddressBook(addressBook);
            }
        };
        model.addPerson(ALICE);
        model.addPerson(BOB);
        saved.saveAddressBook(model.getAddressBook());
        String previousFile = Files.readString(dataPath);
        logic = new LogicManager(model, new StorageManager(saved,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
        logic.execute("find " + BOB.getName().fullName);
        AddressBook previousContacts = new AddressBook(model.getAddressBook());
        List<Person> previousVisible = List.copyOf(model.getFilteredPersonList());
        AtomicInteger notifications = new AtomicInteger();
        model.getFilteredPersonList().addListener(
                (ListChangeListener<Person>) change -> notifications.incrementAndGet());
        failSave.set(true);
        for (String command : List.of("delete 1", "edit 1 note/Changed note", "clear")) {
            String message = command.startsWith("delete")
                    ? DeleteCommand.MESSAGE_SAVE_ERROR : LogicManager.MESSAGE_SAVE_ERROR;
            assertThrows(CommandException.class, message, () -> logic.execute(command));
            assertEquals(previousContacts, model.getAddressBook());
            assertEquals(previousVisible, model.getFilteredPersonList());
            assertEquals(previousFile, Files.readString(dataPath));
            assertEquals(0, notifications.get());
        }
        // Reading and viewing remain available even when storage cannot be written.
        logic.execute("view 1");
        logic.execute("list");
        logic.execute("find " + BOB.getName().fullName);
        failSave.set(false);
        logic.execute("delete 1");
        assertEquals(List.of(ALICE), saved.readAddressBook().orElseThrow().getPersonList());
        assertTrue(model.getFilteredPersonList().isEmpty());
    }

    /**
     * Executes the command and confirms that
     * - no exceptions are thrown <br>
     * - the feedback message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandSuccess(String inputCommand, String expectedMessage,
            Model expectedModel) throws CommandException, ParseException {
        CommandResult result = logic.execute(inputCommand);
        assertEquals(expectedMessage, result.getFeedbackToUser());
        assertEquals(expectedModel, model);
    }

    /**
     * Executes the command, confirms that a ParseException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertParseException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, ParseException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that a CommandException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, CommandException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that the exception is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage) {
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        assertCommandFailure(inputCommand, expectedException, expectedMessage, expectedModel);
    }

    /**
     * Executes the command and confirms that
     * - the {@code expectedException} is thrown <br>
     * - the resulting error message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandSuccess(String, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage, Model expectedModel) {
        assertThrows(expectedException, expectedMessage, () -> logic.execute(inputCommand));
        assertEquals(expectedModel, model);
    }

    /**
     * Tests the Logic component's handling of an {@code IOException} thrown by the Storage component.
     *
     * @param e the exception to be thrown by the Storage component
     * @param expectedMessage the message expected inside exception thrown by the Logic component
     */
    private void assertCommandFailureForExceptionFromStorage(IOException e, String expectedMessage) {
        Path prefPath = temporaryFolder.resolve("ExceptionUserPrefs.json");

        // Inject LogicManager with a JsonAddressBookStorage that throws the IOException e when saving
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(prefPath) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                throw e;
            }
        };

        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("ExceptionUserPrefs.json"));
        StorageManager storage = new StorageManager(addressBookStorage, userPrefsStorage);

        logic = new LogicManager(model, storage);

        // Triggers the saveAddressBook method by executing an add command
        String addCommand = AddCommand.COMMAND_WORD + NAME_DESC_AMY + PHONE_DESC_AMY
                + EMAIL_DESC_AMY + ADDRESS_DESC_AMY;
        ModelManager expectedModel = new ModelManager();
        assertCommandFailure(addCommand, CommandException.class, expectedMessage, expectedModel);
    }
}
