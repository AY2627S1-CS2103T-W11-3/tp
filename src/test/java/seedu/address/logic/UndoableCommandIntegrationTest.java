package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.UndoCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/**
 * Verifies contact commands and undo through the full parser, logic, model, and storage flow.
 */
public class UndoableCommandIntegrationTest {
    private static final String ADD_COMMAND = "add n/Charlie Tan p/91234567 e/charlie@example.com a/NUS"
            + " t/project note/Met at orientation";

    @TempDir
    private Path testFolder;

    private Model model;
    private Logic logic;
    private StorageManager storage;
    private FailingAddressBookStorage dataStorage;

    @BeforeEach
    public void setUp() throws Exception {
        AddressBook contacts = new AddressBook();
        contacts.setPersons(List.of(ALICE, BOB));
        model = new ModelManager(contacts, new UserPrefs());
        dataStorage = new FailingAddressBookStorage(testFolder.resolve("contacts.json"));
        storage = new StorageManager(dataStorage, new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
        storage.saveAddressBook(contacts);
        logic = new LogicManager(model, storage);
    }

    @AfterEach
    public void tearDown() {
        storage.closeUndoHistory();
    }

    @Test
    public void execute_mixedContactChanges_undoRestoresEachSnapshotAndShowsAllContacts() throws Exception {
        List<AddressBook> snapshots = new ArrayList<>();
        for (String command : List.of(ADD_COMMAND, "edit 1 note/Updated note", "delete 2", "clear")) {
            snapshots.add(new AddressBook(model.getAddressBook()));
            logic.execute(command);
            assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());
        }
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        for (int i = snapshots.size() - 1; i >= 0; i--) {
            logic.execute("find NoMatch");
            assertEquals(UndoCommand.MESSAGE_SUCCESS, logic.execute("undo").getFeedbackToUser());
            assertEquals(snapshots.get(i), model.getAddressBook());
            assertEquals(snapshots.get(i).getPersonList(), model.getFilteredPersonList());
            assertEquals(snapshots.get(i), storage.readAddressBook().orElseThrow());
        }
        assertThrows(CommandException.class, UndoCommand.MESSAGE_NO_HISTORY, () -> logic.execute("undo"));
    }

    @Test
    public void execute_nonContactCommands_doesNotRecordHistory() throws Exception {
        AddressBook original = new AddressBook(model.getAddressBook());
        logic.execute("find Alice");
        assertEquals(original, model.getAddressBook());
        assertThrows(CommandException.class, UndoCommand.MESSAGE_NO_HISTORY, () -> logic.execute("undo"));
        assertEquals(List.of(ALICE), model.getFilteredPersonList());
        assertEquals(original, storage.readAddressBook().orElseThrow());
    }

    @Test
    public void execute_identicalEditAndEmptyClear_doNotRecordHistory() throws Exception {
        logic.execute("edit 1 n/" + ALICE.getName().fullName);
        assertThrows(CommandException.class, UndoCommand.MESSAGE_NO_HISTORY, () -> logic.execute("undo"));

        logic.execute("clear");
        logic.execute("clear");
        logic.execute("undo");
        assertEquals(List.of(ALICE, BOB), model.getAddressBook().getPersonList());
        assertThrows(CommandException.class, UndoCommand.MESSAGE_NO_HISTORY, () -> logic.execute("undo"));
    }

    @Test
    public void execute_failedCommands_doNotRecordHistoryOrChangeData() throws Exception {
        AddressBook original = new AddressBook(model.getAddressBook());
        byte[] saved = Files.readAllBytes(dataStorage.getAddressBookFilePath());
        assertThrows(ParseException.class, () -> logic.execute("add"));
        assertThrows(CommandException.class, () -> logic.execute("delete 9"));
        assertThrows(CommandException.class, () -> logic.execute("edit 9 n/Changed"));

        assertEquals(original, model.getAddressBook());
        assertArrayEquals(saved, Files.readAllBytes(dataStorage.getAddressBookFilePath()));
        assertThrows(CommandException.class, UndoCommand.MESSAGE_NO_HISTORY, () -> logic.execute("undo"));
    }

    @Test
    public void execute_backupFailure_preservesModelDisplayDiskAndExistingHistory() throws Exception {
        assertFailedSavePreservesState(true);
    }

    @Test
    public void execute_candidateSaveFailure_preservesModelDisplayDiskAndExistingHistory() throws Exception {
        assertFailedSavePreservesState(false);
    }

    @Test
    public void execute_undoSaveFailure_preservesStateAndAllowsRetry() throws Exception {
        AddressBook original = new AddressBook(model.getAddressBook());
        logic.execute(ADD_COMMAND);
        logic.execute("find Bob");
        AddressBook beforeFailure = new AddressBook(model.getAddressBook());
        byte[] saved = Files.readAllBytes(dataStorage.getAddressBookFilePath());
        AtomicInteger notifications = new AtomicInteger();
        model.getAddressBook().getPersonList().addListener(
                (ListChangeListener<Person>) change -> notifications.incrementAndGet());
        model.getFilteredPersonList().addListener(
                (ListChangeListener<Person>) change -> notifications.incrementAndGet());
        dataStorage.shouldFailSave = true;

        assertThrows(CommandException.class, UndoCommand.MESSAGE_SAVE_ERROR, () -> logic.execute("undo"));
        assertEquals(beforeFailure, model.getAddressBook());
        assertEquals(List.of(BOB), model.getFilteredPersonList());
        assertEquals(0, notifications.get());
        assertArrayEquals(saved, Files.readAllBytes(dataStorage.getAddressBookFilePath()));

        dataStorage.shouldFailSave = false;
        logic.execute("undo");
        assertEquals(original, model.getAddressBook());
        assertEquals(original.getPersonList(), model.getFilteredPersonList());
        assertEquals(original, storage.readAddressBook().orElseThrow());
        assertThrows(CommandException.class, UndoCommand.MESSAGE_NO_HISTORY, () -> logic.execute("undo"));
    }

    @Test
    public void execute_unreadableUndoSnapshot_preservesStateAndAllowsRetry() throws Exception {
        AddressBook original = new AddressBook(model.getAddressBook());
        logic.execute(ADD_COMMAND);
        logic.execute("find Bob");
        AddressBook beforeFailure = new AddressBook(model.getAddressBook());
        byte[] saved = Files.readAllBytes(dataStorage.getAddressBookFilePath());
        Path snapshot;
        try (var files = Files.walk(testFolder)) {
            snapshot = files.filter(path -> path.getFileName().toString().startsWith("undo-"))
                    .findFirst().orElseThrow();
        }
        byte[] backup = Files.readAllBytes(snapshot);
        Files.writeString(snapshot, "invalid JSON");

        assertThrows(CommandException.class, UndoCommand.MESSAGE_LOAD_ERROR, () -> logic.execute("undo"));
        assertEquals(beforeFailure, model.getAddressBook());
        assertEquals(List.of(BOB), model.getFilteredPersonList());
        assertArrayEquals(saved, Files.readAllBytes(dataStorage.getAddressBookFilePath()));

        Files.write(snapshot, backup);
        logic.execute("undo");
        assertEquals(original, model.getAddressBook());
        assertThrows(CommandException.class, UndoCommand.MESSAGE_NO_HISTORY, () -> logic.execute("undo"));
    }

    @Test
    public void closeUndoHistory_removesBackupsPreservesContactsAndRejectsFurtherChanges() throws Exception {
        logic.execute(ADD_COMMAND);
        AddressBook saved = new AddressBook(model.getAddressBook());
        storage.closeUndoHistory();
        try (var files = Files.list(testFolder)) {
            assertEquals(List.of(dataStorage.getAddressBookFilePath()), files.toList());
        }
        assertEquals(saved, storage.readAddressBook().orElseThrow());
        assertThrows(IllegalStateException.class, storage::readLatestSnapshot);
        assertThrows(IllegalStateException.class, storage::discardLatestSnapshot);
        assertThrows(IllegalStateException.class, () -> storage.saveUndoableChange(saved, new AddressBook()));
    }

    /**
     * Exercises both execution paths while a previous successful command already occupies history.
     */
    private void assertFailedSavePreservesState(boolean shouldFailBackup) throws Exception {
        AddressBook original = new AddressBook(model.getAddressBook());
        logic.execute("edit 1 note/Keep this successful change");
        logic.execute("find Bob");
        AddressBook beforeFailure = new AddressBook(model.getAddressBook());
        List<Person> displayed = List.copyOf(model.getFilteredPersonList());
        byte[] saved = Files.readAllBytes(dataStorage.getAddressBookFilePath());
        AtomicInteger notifications = new AtomicInteger();
        model.getAddressBook().getPersonList().addListener(
                (ListChangeListener<Person>) change -> notifications.incrementAndGet());
        model.getFilteredPersonList().addListener(
                (ListChangeListener<Person>) change -> notifications.incrementAndGet());
        dataStorage.shouldFailBackup = shouldFailBackup;
        dataStorage.shouldFailSave = !shouldFailBackup;

        assertThrows(CommandException.class, LogicManager.MESSAGE_SAVE_ERROR, () -> logic.execute(ADD_COMMAND));
        assertThrows(CommandException.class, DeleteCommand.MESSAGE_SAVE_ERROR, () -> logic.execute("delete 1"));
        assertEquals(beforeFailure, model.getAddressBook());
        assertEquals(displayed, model.getFilteredPersonList());
        assertEquals(0, notifications.get());
        assertArrayEquals(saved, Files.readAllBytes(dataStorage.getAddressBookFilePath()));

        dataStorage.shouldFailBackup = false;
        dataStorage.shouldFailSave = false;
        logic.execute("undo");
        assertEquals(original, model.getAddressBook());
        assertThrows(CommandException.class, UndoCommand.MESSAGE_NO_HISTORY, () -> logic.execute("undo"));
    }

    /**
     * Injects failures at atomic replacement to distinguish backup failures from candidate-save failures.
     */
    private static class FailingAddressBookStorage extends JsonAddressBookStorage {
        private boolean shouldFailBackup;
        private boolean shouldFailSave;

        FailingAddressBookStorage(Path path) {
            super(path);
        }

        @Override
        protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
            boolean isMainFile = destination.equals(getAddressBookFilePath().toAbsolutePath());
            if ((isMainFile && shouldFailSave) || (!isMainFile && shouldFailBackup)) {
                throw new IOException("Injected replacement failure");
            }
            super.replaceAddressBook(temporaryFile, destination);
        }
    }
}
