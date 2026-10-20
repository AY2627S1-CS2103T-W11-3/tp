package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.AddressBook;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class UndoSnapshotStorageTest {
    @TempDir
    private Path testFolder;

    private FailingAddressBookStorage dataStorage;
    private Path dataFile;
    private UndoSnapshotStorage history;

    @BeforeEach
    public void setUp() {
        dataFile = testFolder.resolve("contacts.json");
        dataStorage = new FailingAddressBookStorage(dataFile);
        history = new UndoSnapshotStorage(dataStorage);
    }

    @AfterEach
    public void tearDown() {
        history.close();
    }

    @Test
    public void readLatestSnapshot_emptyHistory_doesNotCreateFiles() throws Exception {
        assertTrue(history.readLatestSnapshot().isEmpty());
        assertEquals(List.of(), snapshotFiles());
        assertFalse(Files.exists(dataFile));
    }

    @Test
    public void saveChange_missingDataFile_backsUpFullModelWithoutChangingOnRead() throws Exception {
        Person person = new PersonBuilder(ALICE).withNote("Met at orientation").withTags("friends", "NUS").build();
        AddressBook original = book(BOB, person);
        history.saveChange(original, new AddressBook());
        assertEquals(new AddressBook(), dataStorage.readAddressBook().orElseThrow());
        assertEquals(1, snapshotFiles().size());

        // Later changes to the source cannot alter the file-backed snapshot.
        original.removePerson(BOB);
        assertEquals(book(BOB, person), history.readLatestSnapshot().orElseThrow());
        assertEquals(book(BOB, person), history.readLatestSnapshot().orElseThrow());
        assertEquals(new AddressBook(), dataStorage.readAddressBook().orElseThrow());
        assertEquals(1, snapshotFiles().size());
        history.discardLatestSnapshot();
        assertEquals(List.of(), snapshotFiles());
        assertTrue(history.readLatestSnapshot().isEmpty());
    }

    @Test
    public void saveChange_moreThanFiveChanges_retainsLatestFiveInReverseOrder() throws Exception {
        List<AddressBook> states = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            states.add(book(new PersonBuilder(ALICE).withNote("State " + i).build()));
        }
        for (int i = 1; i < states.size(); i++) {
            history.saveChange(states.get(i - 1), states.get(i));
        }
        assertEquals(5, snapshotFiles().size());
        for (int i = 5; i >= 1; i--) {
            assertEquals(states.get(i), history.readLatestSnapshot().orElseThrow());
            history.discardLatestSnapshot();
        }
        assertTrue(history.readLatestSnapshot().isEmpty());
    }

    @Test
    public void saveChange_firstSaveFails_keepsDataFileAbsentAndHistoryEmpty() throws Exception {
        dataStorage.shouldFailSave = true;
        assertThrows(IOException.class, () -> history.saveChange(book(ALICE), book(BOB)));
        assertFalse(Files.exists(dataFile));
        assertEquals(List.of(), snapshotFiles());
        assertTrue(history.readLatestSnapshot().isEmpty());
    }

    @Test
    public void saveChange_identicalData_doesNotSaveOrConsumeHistory() throws Exception {
        history.saveChange(new AddressBook(), book(ALICE));
        dataStorage.shouldFailBackup = true;
        dataStorage.shouldFailSave = true;
        history.saveChange(book(ALICE), book(ALICE));
        assertEquals(new AddressBook(), history.readLatestSnapshot().orElseThrow());
        assertEquals(1, snapshotFiles().size());
    }

    @Test
    public void saveChange_backupOrSaveFailure_preservesSavedDataAndHistory() throws Exception {
        for (boolean shouldFailBackup : new boolean[] {true, false}) {
            history.saveChange(book(ALICE), book(BOB));
            byte[] saved = Files.readAllBytes(dataFile);
            dataStorage.shouldFailBackup = shouldFailBackup;
            dataStorage.shouldFailSave = !shouldFailBackup;
            assertThrows(IOException.class, () -> history.saveChange(book(BOB), new AddressBook()));
            assertArrayEquals(saved, Files.readAllBytes(dataFile));
            assertEquals(book(ALICE), history.readLatestSnapshot().orElseThrow());
            assertEquals(1, snapshotFiles().size());
            dataStorage.shouldFailBackup = false;
            dataStorage.shouldFailSave = false;
            history.discardLatestSnapshot();
        }
    }

    @Test
    public void readLatestSnapshot_invalidOrMissingSnapshot_preservesDataAndHistory() throws Exception {
        history.saveChange(book(ALICE), book(BOB));
        Path snapshot = snapshotFiles().getFirst();
        byte[] backup = Files.readAllBytes(snapshot);
        byte[] saved = Files.readAllBytes(dataFile);
        Files.writeString(snapshot, "invalid JSON");
        assertThrows(DataLoadingException.class, history::readLatestSnapshot);
        assertArrayEquals(saved, Files.readAllBytes(dataFile));
        Files.delete(snapshot);
        assertThrows(DataLoadingException.class, history::readLatestSnapshot);
        assertArrayEquals(saved, Files.readAllBytes(dataFile));

        Files.write(snapshot, backup);
        assertEquals(book(ALICE), history.readLatestSnapshot().orElseThrow());
    }

    @Test
    public void saveChange_afterDiscard_preservesNewAndEarlierSnapshots() throws Exception {
        history.saveChange(new AddressBook(), book(ALICE));
        history.saveChange(book(ALICE), book(ALICE, BOB));
        history.discardLatestSnapshot();
        history.saveChange(book(ALICE), book(BOB));
        assertEquals(book(ALICE), history.readLatestSnapshot().orElseThrow());
        history.discardLatestSnapshot();
        assertEquals(new AddressBook(), history.readLatestSnapshot().orElseThrow());
        history.discardLatestSnapshot();
        assertTrue(history.readLatestSnapshot().isEmpty());
    }

    @Test
    public void newSession_doesNotReusePreviousSessionHistory() throws Exception {
        UndoSnapshotStorage second = new UndoSnapshotStorage(dataStorage);
        try {
            history.saveChange(book(ALICE), book(BOB));
            assertTrue(second.readLatestSnapshot().isEmpty());
            assertEquals(book(BOB), dataStorage.readAddressBook().orElseThrow());
            assertEquals(book(ALICE), history.readLatestSnapshot().orElseThrow());
        } finally {
            second.close();
        }
    }

    @Test
    public void close_removesBackupsPreservesContactsAndRejectsFurtherChanges() throws Exception {
        history.saveChange(book(ALICE), book(BOB));
        Path session = snapshotFiles().getFirst().getParent();
        history.close();
        assertFalse(Files.exists(session));
        assertEquals(book(BOB), dataStorage.readAddressBook().orElseThrow());
        assertThrows(IllegalStateException.class, history::readLatestSnapshot);
        assertThrows(IllegalStateException.class, () -> history.saveChange(book(BOB), book(ALICE)));
    }

    private List<Path> snapshotFiles() throws IOException {
        try (Stream<Path> files = Files.walk(testFolder)) {
            return files.filter(path -> path.getFileName().toString().startsWith("undo-"))
                    .filter(path -> path.toString().endsWith(".json"))
                    .toList();
        }
    }

    private static AddressBook book(Person... persons) {
        AddressBook result = new AddressBook();
        result.setPersons(List.of(persons));
        return result;
    }

    /** Injects failures before publishing either a snapshot or the main data file. */
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
