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
import static seedu.address.testutil.TypicalPersons.AMY;
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
import seedu.address.logic.commands.EditCommand;
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
    public void execute_editAndUndo_preserveContactId() throws Exception {
        model.addPerson(ALICE);
        logic.execute("edit 1 p/80000000");

        assertEquals(ALICE.getId(), model.getAddressBook().getPersonList().getFirst().getId());
        JsonAddressBookStorage saved = new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"));
        assertEquals(ALICE.getId(), saved.readAddressBook().orElseThrow().getPersonList().getFirst().getId());

        logic.execute("undo");

        Person restored = model.getAddressBook().getPersonList().getFirst();
        assertEquals(ALICE, restored);
        assertEquals(ALICE.getId(), restored.getId());
        assertEquals(ALICE.getId(), saved.readAddressBook().orElseThrow().getPersonList().getFirst().getId());
    }

    @Test
    public void execute_favouriteFilteredContact_mirrorsEditsAndKeepsContext() throws Exception {
        Person target = new PersonBuilder(BOB).withNote("Met in class").build();
        model.addPerson(ALICE);
        model.addPerson(target);
        logic.execute("find Bob");

        assertEquals("Added to Favourites: Bob Choo (index 1).",
                logic.execute("favourite 1").getFeedbackToUser());

        assertEquals(ContactSection.CONTACTS, logic.getActiveSection());
        assertEquals(List.of(target), logic.getFilteredPersonList());
        assertEquals(List.of(ALICE, target), model.getAddressBook().getPersonList());
        assertTrue(Files.exists(temporaryFolder.resolve("addressBook.json")));

        logic.execute("edit 1 note/Updated original");
        assertEquals(new Note("Updated original"), model.getAddressBook().getPersonList().get(1).getNote());
        Person edited = model.getAddressBook().getPersonList().get(1);
        assertEquals(target.getId(), edited.getId());
        assertEquals(List.of(edited), model.getAddressBook(ContactSection.FAVOURITES).getPersonList());
        assertEquals(List.of(edited), new JsonAddressBookStorage(temporaryFolder.resolve("favourites.json"))
                .readAddressBook().orElseThrow().getPersonList());
    }

    @Test
    public void isFavourite_matchesContactIdentityAndUpdatesAfterRemoval() throws Exception {
        model.addPerson(ALICE);
        assertFalse(logic.isFavourite(ALICE));
        logic.execute("favourite 1");
        logic.execute("tab favourites");
        logic.execute("find Nobody");

        assertTrue(logic.isFavourite(ALICE));
        assertTrue(logic.isFavourite(new PersonBuilder(ALICE).withPhone(BOB.getPhone().value).build()));
        assertTrue(logic.isFavourite(new PersonBuilder(ALICE).withEmail(BOB.getEmail().value).build()));
        assertFalse(logic.isFavourite(BOB));

        logic.execute("list");
        logic.execute("delete 1");
        assertFalse(logic.isFavourite(ALICE));
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
        assertEquals("88888888", logic.getFilteredPersonList().getFirst().getPhone().value);
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
        assertTrue(logic.getFilteredPersonList().isEmpty());
        logic.execute("list");
        assertEquals(List.of(BOB), logic.getFilteredPersonList());
        assertEquals(temporaryFolder.resolve("favourites.json"), logic.getActiveDataFilePath());
        logic.execute("delete 1");
        assertTrue(new JsonAddressBookStorage(temporaryFolder.resolve("favourites.json"))
                .readAddressBook().orElseThrow().getPersonList().isEmpty());
        assertEquals(List.of(ALICE), model.getAddressBook(ContactSection.CONTACTS).getPersonList());
    }

    @Test
    public void execute_tab_restoresEachSectionsFilter() throws Exception {
        model.addPerson(ALICE);
        model.addPerson(BOB);
        logic.execute("favourite 1");
        logic.execute("favourite 2");

        for (String command : List.of("find Alice", "filter t/friends")) {
            logic.execute("tab contacts");
            logic.execute(command);
            logic.execute("tab favourites");
            logic.execute("find Bob");
            assertEquals("", logic.getActiveFilter());
            logic.execute("tab contacts");
            assertEquals(List.of(ALICE), logic.getFilteredPersonList());
            assertEquals(command.startsWith("filter") ? "friends" : "", logic.getActiveFilter());

            logic.execute("list");
            assertEquals(List.of(ALICE, BOB), logic.getFilteredPersonList());
            assertEquals("", logic.getActiveFilter());
            logic.execute("tab favourites");
            assertEquals(List.of(BOB), logic.getFilteredPersonList());
        }
    }

    @Test
    public void execute_addAfterSectionSwitch_preservesEachSectionsRecentView() throws Exception {
        model.addPerson(ALICE);
        model.addPerson(BOB);
        logic.execute("recent");
        logic.execute("tab favourites");
        logic.execute("recent");
        logic.execute("tab contacts");

        logic.execute(PersonUtil.getAddCommand(AMY));

        assertEquals(List.of(AMY, BOB, ALICE), logic.getFilteredPersonList());
        logic.execute("tab favourites");
        logic.execute(PersonUtil.getAddCommand(ALICE));
        logic.execute(PersonUtil.getAddCommand(BOB));
        assertEquals(List.of(BOB, ALICE), logic.getFilteredPersonList());
        logic.execute("tab contacts");
        assertEquals(List.of(AMY, BOB, ALICE), logic.getFilteredPersonList());
    }

    @Test
    public void execute_favouriteSaveAndUndoFailure_preservesContextDataAndHistory() throws Exception {
        model.addPerson(ALICE);
        AtomicBoolean shouldFail = new AtomicBoolean(true);
        Path favouritesFile = temporaryFolder.resolve("favourites.json");
        JsonAddressBookStorage favouritesStorage = new JsonAddressBookStorage(favouritesFile) {
            @Override
            protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
                if (shouldFail.get()) {
                    throw DUMMY_IO_EXCEPTION;
                }
                super.replaceAddressBook(temporaryFile, destination);
            }
        };
        StorageManager failing = new StorageManager(
                new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json")), favouritesStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")));
        logic = new LogicManager(model, failing);

        assertThrows(CommandException.class, LogicManager.MESSAGE_SAVE_ERROR, () -> logic.execute("favourite 1"));

        assertEquals(ContactSection.CONTACTS, logic.getActiveSection());
        assertEquals(List.of(ALICE), logic.getFilteredPersonList());
        assertTrue(model.getAddressBook(ContactSection.FAVOURITES).getPersonList().isEmpty());

        shouldFail.set(false);
        logic.execute("favourite 1");
        shouldFail.set(true);
        assertThrows(CommandException.class, LogicManager.MESSAGE_SAVE_ERROR, () ->
                logic.execute("edit 1 note/Unsaved edit"));
        assertEquals(List.of(ALICE), model.getAddressBook().getPersonList());
        assertEquals(List.of(ALICE), model.getAddressBook(ContactSection.FAVOURITES).getPersonList());
        shouldFail.set(false);
        logic.execute("tab favourites");
        logic.execute("find Nobody");
        byte[] saved = Files.readAllBytes(favouritesFile);
        shouldFail.set(true);
        assertThrows(CommandException.class, UndoCommand.MESSAGE_SAVE_ERROR, () -> logic.execute("undo"));
        assertEquals(List.of(ALICE), model.getAddressBook().getPersonList());
        assertTrue(logic.getFilteredPersonList().isEmpty());
        assertArrayEquals(saved, Files.readAllBytes(favouritesFile));

        shouldFail.set(false);
        logic.execute("undo");
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertTrue(favouritesStorage.readAddressBook().orElseThrow().getPersonList().isEmpty());
        assertEquals(List.of(ALICE), model.getAddressBook(ContactSection.CONTACTS).getPersonList());
        assertThrows(CommandException.class, UndoCommand.MESSAGE_NO_HISTORY, () -> logic.execute("undo"));
    }

    @Test
    public void execute_mirroredEditAndDelete_preserveGroupsAndUndoBothCopies() throws Exception {
        model.addPerson(ALICE);
        logic.execute("favourite 1");
        logic.execute("group add n/Team");
        logic.execute("group member g/Team add/Alice Pauline");
        logic.execute("tab favourites");
        logic.execute("group add n/Favourites Team");
        logic.execute("group member g/Favourites Team add/Alice Pauline");
        logic.execute("edit 1 n/Renamed Contact");
        for (ContactSection section : ContactSection.values()) {
            assertEquals("Renamed Contact",
                    model.getAddressBook(section).getPersonList().getFirst().getName().fullName);
            assertEquals("Renamed Contact",
                    model.getAddressBook(section).getGroupList().getFirst().getMembers().iterator().next().fullName);
        }
        logic.execute("tab contacts");
        logic.execute("delete 1");
        assertTrue(model.getAddressBook(ContactSection.FAVOURITES).getPersonList().isEmpty());
        assertTrue(model.getAddressBook(ContactSection.FAVOURITES).getGroupList().getFirst().getMembers().isEmpty());
        logic.execute("undo");
        assertEquals(1, model.getAddressBook(ContactSection.FAVOURITES).getPersonList().size());
        logic.execute("tab favourites");
        logic.execute("delete 1");
        assertEquals(1, model.getAddressBook(ContactSection.CONTACTS).getPersonList().size());
        logic.execute("undo");
        logic.execute("clear");
        assertEquals(1, model.getAddressBook(ContactSection.CONTACTS).getPersonList().size());
        logic.execute("undo");
        logic.execute("tab contacts");
        logic.execute("clear");
        assertTrue(model.getAddressBook(ContactSection.FAVOURITES).getPersonList().isEmpty());
        logic.execute("undo");
        assertEquals(1, model.getAddressBook(ContactSection.FAVOURITES).getPersonList().size());
    }

    @Test
    public void execute_reusedPhone_canFavouriteDifferentContact() throws Exception {
        model.addPerson(ALICE);
        logic.execute("favourite 1");
        logic.execute("edit 1 p/80000000");
        Person bob = new PersonBuilder(BOB).withPhone(ALICE.getPhone().value).build();
        logic.execute(PersonUtil.getAddCommand(bob));
        logic.execute("favourite 2");
        assertEquals(2, model.getAddressBook(ContactSection.FAVOURITES).getPersonList().size());
        assertEquals("80000000", model.getAddressBook(ContactSection.FAVOURITES)
                .getPersonList().getFirst().getPhone().value);
    }

    @Test
    public void execute_mirroredEdit_conflictingFavouriteRejectsBothChanges() throws Exception {
        model.addPerson(ALICE);
        logic.execute("favourite 1");
        logic.execute("tab favourites");
        logic.execute(PersonUtil.getAddCommand(BOB));
        logic.execute("tab contacts");
        assertThrows(CommandException.class, EditCommand.MESSAGE_DUPLICATE_PERSON, () ->
                logic.execute("edit 1 p/" + BOB.getPhone().value));
        assertEquals(List.of(ALICE), model.getAddressBook().getPersonList());
        assertEquals(List.of(ALICE, BOB), model.getAddressBook(ContactSection.FAVOURITES).getPersonList());
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
    public void execute_undo_identifiesOperationAndContact() throws Exception {
        model.addPerson(ALICE);
        String[][] cases = {
            {"delete 1", "Undid delete of Alice Pauline."},
            {"edit 1 n/Renamed Contact", "Undid edit of Alice Pauline."},
            {"clear", "Undid clear of Contacts."},
            {PersonUtil.getAddCommand(BOB), "Undid add of Bob Choo."}
        };
        for (String[] input : cases) {
            logic.execute(input[0]);
            assertEquals(input[1], logic.execute("undo").getFeedbackToUser());
            assertEquals(List.of(ALICE), model.getAddressBook().getPersonList());
        }
        logic.execute("favourite 1");
        logic.execute("tab favourites");
        logic.execute("clear");
        assertEquals("Undid clear of Favourites.", logic.execute("undo").getFeedbackToUser());
        assertEquals("Undid favourite of Alice Pauline.", logic.execute("undo").getFeedbackToUser());
    }

    @Test
    public void execute_addAfterRecent_keepsRecentViewWithAddedContactFirst() throws Exception {
        model.addPerson(ALICE);
        model.addPerson(BOB);
        logic.execute("recent");

        logic.execute(PersonUtil.getAddCommand(AMY));

        assertEquals(List.of(AMY, BOB, ALICE), logic.getFilteredPersonList());
        assertEquals(List.of(ALICE, BOB, AMY), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_editAfterRecent_keepsRecentViewAndOriginalInsertionPosition() throws Exception {
        model.addPerson(ALICE);
        model.addPerson(BOB);
        logic.execute("recent");
        Person editedAlice = new PersonBuilder(ALICE).withPhone("91234567").build();

        logic.execute("edit 2 p/91234567");

        assertEquals(List.of(BOB, editedAlice), logic.getFilteredPersonList());
        assertEquals(List.of(editedAlice, BOB), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_interleavedSections_undoLatestOperationGlobally() throws Exception {
        model.addPerson(ALICE);
        logic.execute(PersonUtil.getAddCommand(BOB));
        logic.execute("favourite 1");
        logic.execute("tab favourites");
        logic.execute("edit 1 note/Edited favourite");
        logic.execute("delete 1");
        logic.execute("undo");
        assertEquals(new Note("Edited favourite"), model.getAddressBook().getPersonList().getFirst().getNote());
        assertEquals(new Note("Edited favourite"),
                model.getAddressBook(ContactSection.CONTACTS).getPersonList().getFirst().getNote());

        logic.execute("tab contacts");
        logic.execute("undo");
        assertEquals(List.of(ALICE, BOB), model.getAddressBook().getPersonList());
        assertEquals(List.of(ALICE), model.getAddressBook(ContactSection.FAVOURITES).getPersonList());
        assertEquals(ContactSection.CONTACTS, model.getActiveSection());
        logic.execute("undo");
        assertTrue(model.getAddressBook(ContactSection.FAVOURITES).getPersonList().isEmpty());
        logic.execute("tab favourites");
        logic.execute("undo");
        assertEquals(List.of(ALICE), model.getAddressBook(ContactSection.CONTACTS).getPersonList());
        assertEquals(ContactSection.FAVOURITES, model.getActiveSection());
        assertThrows(CommandException.class, UndoCommand.MESSAGE_NO_HISTORY, () -> logic.execute("undo"));
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
        assertEquals(List.of(BOB, ALICE), logic.getFilteredPersonList());
        logic.execute("favourite 2");
        logic.execute("tab favourites");
        assertEquals(List.of(ALICE, BOB), logic.getFilteredPersonList());
        logic.execute("tab contacts");
        logic.execute("tab favourites");
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
            protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
                assertEquals(2, model.getFilteredPersonList().size());
                saves.incrementAndGet();
                super.replaceAddressBook(temporaryFile, destination);
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
    public void execute_otherSectionFailedToLoad_preservesFileAcrossUndoRestartAndClear() throws Exception {
        for (ContactSection failedSection : ContactSection.values()) {
            Path folder = Files.createTempDirectory(temporaryFolder, "failed-load-");
            JsonAddressBookStorage contacts = new JsonAddressBookStorage(folder.resolve("contacts.json"));
            JsonAddressBookStorage favourites = new JsonAddressBookStorage(folder.resolve("favourites.json"));
            JsonAddressBookStorage failed = failedSection == ContactSection.CONTACTS ? contacts : favourites;
            JsonAddressBookStorage healthy = failedSection == ContactSection.CONTACTS ? favourites : contacts;
            healthy.saveAddressBook(getTypicalAddressBook());
            Files.writeString(failed.getAddressBookFilePath(), "Invalid JSON to preserve\n");
            byte[] original = Files.readAllBytes(failed.getAddressBookFilePath());
            StorageManager storage = new StorageManager(contacts, favourites,
                    new JsonUserPrefsStorage(folder.resolve("prefs.json")));
            model = loadModel(storage);
            ContactSection healthySection = failedSection == ContactSection.CONTACTS
                    ? ContactSection.FAVOURITES : ContactSection.CONTACTS;
            model.setActiveSection(healthySection);
            logic = new LogicManager(model, storage);

            logic.execute("edit 1 note/Updated healthy section");
            assertArrayEquals(original, Files.readAllBytes(failed.getAddressBookFilePath()));
            storage.closeUndoHistory();
            storage = new StorageManager(contacts, favourites,
                    new JsonUserPrefsStorage(folder.resolve("prefs.json")));
            model = loadModel(storage);
            model.setActiveSection(healthySection);
            logic = new LogicManager(model, storage);
            logic.execute("undo");
            assertEquals(getTypicalAddressBook(), healthy.readAddressBook().orElseThrow());
            assertArrayEquals(original, Files.readAllBytes(failed.getAddressBookFilePath()));

            logic.execute("clear");
            logic.execute("clear");
            assertArrayEquals(original, Files.readAllBytes(failed.getAddressBookFilePath()));
            logic.execute("undo");
            assertEquals(getTypicalAddressBook(), healthy.readAddressBook().orElseThrow());
            assertArrayEquals(original, Files.readAllBytes(failed.getAddressBookFilePath()));
            model.setActiveSection(failedSection);
            logic.execute("clear");
            assertEquals(new AddressBook(), failed.readAddressBook().orElseThrow());
            assertThrows(CommandException.class, UndoCommand.MESSAGE_NO_HISTORY, () -> logic.execute("undo"));
        }
    }

    @Test
    public void execute_failedRepair_keepsFailedFileProtectedUntilSuccessfulRetry() throws Exception {
        Path contactsFile = temporaryFolder.resolve("repair-contacts.json");
        Path favouritesFile = temporaryFolder.resolve("repair-favourites.json");
        AtomicBoolean shouldFailContacts = new AtomicBoolean(false);
        JsonAddressBookStorage contacts = new JsonAddressBookStorage(contactsFile) {
            @Override
            protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
                if (shouldFailContacts.get()) {
                    throw DUMMY_IO_EXCEPTION;
                }
                super.replaceAddressBook(temporaryFile, destination);
            }
        };
        contacts.saveAddressBook(getTypicalAddressBook());
        Files.writeString(favouritesFile, "Invalid favourites JSON");
        byte[] original = Files.readAllBytes(favouritesFile);
        AtomicBoolean shouldFail = new AtomicBoolean(true);
        JsonAddressBookStorage favourites = new JsonAddressBookStorage(favouritesFile) {
            @Override
            protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
                if (shouldFail.get()) {
                    throw DUMMY_IO_EXCEPTION;
                }
                super.replaceAddressBook(temporaryFile, destination);
            }
        };
        StorageManager storage = new StorageManager(contacts, favourites,
                new JsonUserPrefsStorage(temporaryFolder.resolve("repair-prefs.json")));
        model = loadModel(storage);
        logic = new LogicManager(model, storage);
        logic.execute("tab favourites");
        assertThrows(CommandException.class, LogicManager.MESSAGE_SAVE_ERROR, () -> logic.execute("clear"));
        assertArrayEquals(original, Files.readAllBytes(favouritesFile));
        logic.execute("tab contacts");
        logic.execute("edit 1 note/Healthy change after failed repair");
        byte[] savedContacts = Files.readAllBytes(contactsFile);
        shouldFailContacts.set(true);
        assertThrows(CommandException.class, UndoCommand.MESSAGE_SAVE_ERROR, () -> logic.execute("undo"));
        assertArrayEquals(savedContacts, Files.readAllBytes(contactsFile));
        assertArrayEquals(original, Files.readAllBytes(favouritesFile));
        assertEquals(new Note("Healthy change after failed repair"),
                model.getAddressBook().getPersonList().getFirst().getNote());
        shouldFailContacts.set(false);
        logic.execute("undo");
        assertArrayEquals(original, Files.readAllBytes(favouritesFile));
        assertEquals(getTypicalAddressBook(), contacts.readAddressBook().orElseThrow());

        shouldFail.set(false);
        logic.execute("favourite 1");
        assertEquals(List.of(ALICE), favourites.readAddressBook().orElseThrow().getPersonList());
        logic.execute("edit 1 note/Linked after repair");
        assertEquals(new Note("Linked after repair"),
                favourites.readAddressBook().orElseThrow().getPersonList().getFirst().getNote());
        logic.execute("undo");
        logic.execute("undo");
        assertEquals(new AddressBook(), favourites.readAddressBook().orElseThrow());
    }

    private Model loadModel(StorageManager storage) {
        ReadOnlyAddressBook contacts;
        ReadOnlyAddressBook favourites;
        try {
            contacts = storage.readAddressBook().orElseGet(AddressBook::new);
        } catch (DataLoadingException e) {
            contacts = new AddressBook();
        }
        try {
            favourites = storage.readFavourites().orElseGet(AddressBook::new);
        } catch (DataLoadingException e) {
            favourites = new AddressBook();
        }
        return new ModelManager(contacts, favourites, new UserPrefs());
    }

    @Test
    public void execute_saveFailure_preservesDataAndFilterUntilRetry() throws Exception {
        Path dataPath = temporaryFolder.resolve("retry.json");
        AtomicBoolean failSave = new AtomicBoolean(false);
        JsonAddressBookStorage saved = new JsonAddressBookStorage(dataPath) {
            @Override
            protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
                if (failSave.get()) {
                    throw DUMMY_IO_EXCEPTION;
                }
                super.replaceAddressBook(temporaryFile, destination);
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
            protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
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
