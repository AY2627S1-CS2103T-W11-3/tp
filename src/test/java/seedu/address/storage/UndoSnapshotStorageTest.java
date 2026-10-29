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
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.AddressBook;
import seedu.address.model.ContactSection;
import seedu.address.model.ContactSnapshot;
import seedu.address.model.group.Group;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

class UndoSnapshotStorageTest {
    @TempDir
    private Path testFolder;
    private JsonAddressBookStorage contacts;
    private JsonAddressBookStorage favourites;
    private UndoSnapshotStorage history;

    @BeforeEach
    public void setUp() {
        contacts = new JsonAddressBookStorage(testFolder.resolve("contacts.json"));
        favourites = new JsonAddressBookStorage(testFolder.resolve("favourites.json"));
        history = new UndoSnapshotStorage(contacts, favourites);
    }

    @Test
    public void readLatestSnapshot_emptyHistory_doesNotCreateFiles() throws Exception {
        assertTrue(history.readLatestSnapshot().isEmpty());
        assertFalse(Files.exists(contacts.getAddressBookFilePath()));
        assertFalse(Files.exists(indexFile()));
    }

    @Test
    public void saveChange_restoresBothCollectionsAndIdsAfterRestart() throws Exception {
        AddressBook original = book(ALICE);
        original.addGroup(new Group("Team", Set.of(ALICE.getId())));
        ContactSnapshot before = new ContactSnapshot(original, book(BOB));
        history.saveChange(before, new ContactSnapshot(book(BOB), book(ALICE)),
                "Undid edit in Contacts.", ContactSection.CONTACTS);
        original.removePerson(ALICE);
        history.close();
        history = new UndoSnapshotStorage(contacts, favourites);
        ContactSnapshot restored = history.readLatestSnapshot().orElseThrow();
        assertEquals(before, restored);
        assertEquals(ALICE.getId(),
                restored.getAddressBook(ContactSection.CONTACTS).getPersonList().getFirst().getId());
        assertEquals("Undid edit in Contacts.", history.getLatestUndoDescription());

        history.saveRestoredSnapshot(restored);
        history.discardLatestSnapshot();
        history.close();
        history = new UndoSnapshotStorage(contacts, favourites);

        assertEquals(before.getAddressBook(ContactSection.CONTACTS), contacts.readAddressBook().orElseThrow());
        assertEquals(before.getAddressBook(ContactSection.FAVOURITES), favourites.readAddressBook().orElseThrow());
        assertTrue(history.readLatestSnapshot().isEmpty());
    }

    @Test
    public void saveChange_moreThanFiveChanges_preservesGlobalOrderAndDescriptions() throws Exception {
        ContactSnapshot previous = new ContactSnapshot(book(ALICE), book(BOB));
        for (int i = 1; i <= 6; i++) {
            ContactSnapshot next = new ContactSnapshot(
                    book(new PersonBuilder(ALICE).withNote("Contact " + i).build()),
                    book(new PersonBuilder(BOB).withNote("Favourite " + i).build()));
            history.saveChange(previous, next, "Undid operation " + i + ".", ContactSection.CONTACTS);
            previous = next;
        }
        for (int i = 6; i >= 2; i--) {
            assertEquals("Undid operation " + i + ".", history.getLatestUndoDescription());
            ContactSnapshot restored = history.readLatestSnapshot().orElseThrow();
            history.saveRestoredSnapshot(restored);
            history.discardLatestSnapshot();
        }
        assertTrue(history.readLatestSnapshot().isEmpty());
    }

    @Test
    public void saveChange_identicalData_skipsHistoryButGroupChangesAreRecorded() throws Exception {
        ContactSnapshot empty = new ContactSnapshot(book(), book());
        AddressBook grouped = book();
        grouped.addGroup(new Group("Team", Set.of()));
        ContactSnapshot changed = new ContactSnapshot(grouped, book());
        history.saveChange(empty, empty, "No change.", ContactSection.CONTACTS);
        assertTrue(history.readLatestSnapshot().isEmpty());
        history.saveChange(empty, changed, "Undid group creation.", ContactSection.CONTACTS);
        history.saveChange(changed, changed, "No change.", ContactSection.CONTACTS);
        assertEquals("Undid group creation.", history.getLatestUndoDescription());
        assertEquals(empty, history.readLatestSnapshot().orElseThrow());
    }

    @Test
    public void saveChange_secondFileFails_preservesBothFilesAndHistory() throws Exception {
        ContactSnapshot original = new ContactSnapshot(book(ALICE), book(BOB));
        history.saveChange(new ContactSnapshot(book(), book()), original,
                "Undid first operation.", ContactSection.CONTACTS);
        byte[] savedContacts = Files.readAllBytes(contacts.getAddressBookFilePath());
        byte[] savedFavourites = Files.readAllBytes(favourites.getAddressBookFilePath());
        byte[] savedHistory = Files.readAllBytes(indexFile());
        JsonAddressBookStorage failing = new JsonAddressBookStorage(favourites.getAddressBookFilePath()) {
            @Override
            protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
                throw new IOException("Replacement failed");
            }
        };
        history = new UndoSnapshotStorage(contacts, failing);
        assertThrows(IOException.class, () -> history.saveChange(original,
                new ContactSnapshot(book(BOB), book(ALICE)), "Undid second operation.", ContactSection.CONTACTS));
        assertArrayEquals(savedContacts, Files.readAllBytes(contacts.getAddressBookFilePath()));
        assertArrayEquals(savedFavourites, Files.readAllBytes(favourites.getAddressBookFilePath()));
        assertArrayEquals(savedHistory, Files.readAllBytes(indexFile()));
        assertEquals("Undid first operation.", history.getLatestUndoDescription());
    }

    @Test
    public void newSession_interruptedUndo_restoresDataAndRetainsUnconsumedHistory() throws Exception {
        ContactSnapshot before = new ContactSnapshot(book(ALICE), book(BOB));
        ContactSnapshot after = new ContactSnapshot(book(BOB), book(ALICE));
        history.saveChange(before, after, "Undid edit.", ContactSection.CONTACTS);
        JsonAddressBookStorage interrupted = new JsonAddressBookStorage(favourites.getAddressBookFilePath()) {
            @Override
            protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
                super.replaceAddressBook(temporaryFile, destination);
                throw new AssertionError("Interrupted after replacing both files");
            }
        };
        history = new UndoSnapshotStorage(contacts, interrupted);
        ContactSnapshot restored = history.readLatestSnapshot().orElseThrow();
        assertThrows(AssertionError.class, () -> history.saveRestoredSnapshot(restored));

        history = new UndoSnapshotStorage(contacts, favourites);

        assertEquals(after.getAddressBook(ContactSection.CONTACTS), contacts.readAddressBook().orElseThrow());
        assertEquals(after.getAddressBook(ContactSection.FAVOURITES), favourites.readAddressBook().orElseThrow());
        assertEquals(before, history.readLatestSnapshot().orElseThrow());
    }

    @Test
    public void readLatestSnapshot_missingOrInvalidHistory_preservesData() throws Exception {
        history.saveChange(new ContactSnapshot(book(), book()),
                new ContactSnapshot(book(ALICE), book(BOB)), "Undid add.", ContactSection.CONTACTS);
        byte[] saved = Files.readAllBytes(contacts.getAddressBookFilePath());
        for (String invalid : List.of("invalid JSON", "null", "{}")) {
            Files.writeString(indexFile(), invalid);
            assertThrows(DataLoadingException.class, history::readLatestSnapshot);
        }
        Files.delete(indexFile());
        assertThrows(DataLoadingException.class, history::readLatestSnapshot);
        assertArrayEquals(saved, Files.readAllBytes(contacts.getAddressBookFilePath()));
    }

    @Test
    public void saveChange_historyReplacementFails_blocksAccessUntilRecoverySucceeds() throws Exception {
        ContactSnapshot before = new ContactSnapshot(book(ALICE), book(BOB));
        history.saveChange(new ContactSnapshot(book(), book()), before,
                "Undid first operation.", ContactSection.CONTACTS);
        byte[] originalHistory = Files.readAllBytes(indexFile());
        Path obstruction = indexFile().resolve("obstruction");
        JsonAddressBookStorage blocked = new JsonAddressBookStorage(favourites.getAddressBookFilePath()) {
            @Override
            protected void replaceAddressBook(Path temporaryFile, Path destination) throws IOException {
                super.replaceAddressBook(temporaryFile, destination);
                Files.delete(indexFile());
                Files.createDirectory(indexFile());
                Files.writeString(obstruction, "Prevent publishing or restoring the history index");
            }
        };
        history = new UndoSnapshotStorage(contacts, blocked);
        assertThrows(IOException.class, () -> history.saveChange(before,
                new ContactSnapshot(book(BOB), book(ALICE)), "Undid second operation.", ContactSection.CONTACTS));
        assertThrows(DataLoadingException.class, history::readLatestSnapshot);
        StorageManager restarting = new StorageManager(contacts, favourites,
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
        assertThrows(DataLoadingException.class, restarting::readAddressBook);
        assertThrows(DataLoadingException.class, restarting::readFavourites);

        Files.delete(obstruction);
        Files.delete(indexFile());
        history = new UndoSnapshotStorage(contacts, favourites);

        assertArrayEquals(originalHistory, Files.readAllBytes(indexFile()));
        assertEquals(before.getAddressBook(ContactSection.CONTACTS), contacts.readAddressBook().orElseThrow());
        assertEquals(before.getAddressBook(ContactSection.FAVOURITES), favourites.readAddressBook().orElseThrow());
        assertEquals("Undid first operation.", history.getLatestUndoDescription());
    }

    @Test
    public void newSession_eitherCollectionChanged_discardsStaleHistory() throws Exception {
        history.saveChange(new ContactSnapshot(book(), book()),
                new ContactSnapshot(book(ALICE), book(BOB)), "Undid add.", ContactSection.CONTACTS);
        history.close();
        favourites.saveAddressBook(book());
        history = new UndoSnapshotStorage(contacts, favourites);
        assertTrue(history.readLatestSnapshot().isEmpty());
        history.close();
        assertThrows(IllegalStateException.class, history::readLatestSnapshot);
    }

    private Path indexFile() {
        return testFolder.resolve(".contacts.json-shared-undo.json");
    }

    private static AddressBook book(Person... persons) {
        AddressBook book = new AddressBook();
        book.setPersons(List.of(persons));
        return book;
    }
}
