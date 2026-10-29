package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.ContactSection;
import seedu.address.model.ModelManager;
import seedu.address.model.group.Group;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Checks group commands through parsing, saving, and model updates. */
public class GroupCommandIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void listAndView_showGroupsWithoutSavingOrChangingFilter() throws Exception {
        Path dataFile = temporaryFolder.resolve("contacts.json");
        ModelManager model = new ModelManager();
        model.addPerson(ALICE);
        StorageManager storage = new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
        LogicManager logic = new LogicManager(model, storage);

        assertEquals("No groups exist.", logic.execute("group list").getFeedbackToUser());
        assertFalse(Files.exists(dataFile));
        logic.execute("group add n/Empty Team");
        logic.execute("group add n/CS2103T Team");
        logic.execute("group member g/CS2103T Team add/Alice Pauline");
        logic.execute("find Nobody");
        byte[] savedBytes = Files.readAllBytes(dataFile);

        assertEquals("Groups:\n1. Empty Team (0 members)\n2. CS2103T Team (1 member)",
                logic.execute("group list").getFeedbackToUser());
        assertEquals("Group Empty Team has no members.",
                logic.execute("group view g/Empty Team").getFeedbackToUser());
        assertEquals("Members of CS2103T Team:\n1. Alice Pauline (Phone: 94351253)",
                logic.execute("group view g/CS2103T Team").getFeedbackToUser());
        assertEquals(0, logic.getFilteredPersonList().size());
        assertArrayEquals(savedBytes, Files.readAllBytes(dataFile));
        CommandException error = assertThrows(CommandException.class, () ->
                logic.execute("group view g/Missing Team"));
        assertEquals("Group does not exist: Missing Team", error.getMessage());
    }

    @Test
    public void listAndView_preserveUndoHistory() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(ALICE);
        JsonAddressBookStorage dataStorage = new JsonAddressBookStorage(temporaryFolder.resolve("contacts.json"));
        StorageManager storage = new StorageManager(dataStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
        LogicManager logic = new LogicManager(model, storage);
        try {
            logic.execute("group add n/Team");
            logic.execute("group member g/Team add/Alice Pauline");
            logic.execute("group list");
            logic.execute("group view g/Team");

            assertEquals("Undid addition of Alice Pauline to group Team.",
                    logic.execute("undo").getFeedbackToUser());
            assertEquals(0, model.getGroup("Team").getMembers().size());
            assertEquals(model.getAddressBook(), dataStorage.readAddressBook().orElseThrow());

            logic.execute("group list");
            logic.execute("group view g/Team");

            assertEquals("Undid creation of group Team.", logic.execute("undo").getFeedbackToUser());
            assertEquals(0, model.getAddressBook().getGroupList().size());
            assertEquals(model.getAddressBook(), dataStorage.readAddressBook().orElseThrow());
            assertThrows(CommandException.class, () -> logic.execute("undo"));
        } finally {
            storage.closeUndoHistory();
        }
    }

    @Test
    public void view_sameNameContacts_requiresIndexAndShowsOnlySelectedMember() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(ALICE);
        model.addPerson(new PersonBuilder().withName("Alice Pauline").withPhone("91234567")
                .withEmail("other-alice@example.com").build());
        StorageManager storage = new StorageManager(
                new JsonAddressBookStorage(temporaryFolder.resolve("contacts.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
        LogicManager logic = new LogicManager(model, storage);

        logic.execute("group add n/Team");
        CommandException error = assertThrows(CommandException.class, () ->
                logic.execute("group member g/Team add/Alice Pauline"));
        assertEquals("Multiple contacts named Alice Pauline. Use add/i/INDEX or remove/i/INDEX "
                + "with a displayed contact index.", error.getMessage());
        logic.execute("filter t/friends");
        assertEquals(1, logic.getFilteredPersonList().size());
        logic.execute("group member g/Team add/i/1");
        logic.execute("list");

        assertEquals(Set.of(ALICE.getId()), model.getGroup("Team").getMembers());
        assertEquals("Groups:\n1. Team (1 member)", logic.execute("group list").getFeedbackToUser());
        assertEquals("Members of Team:\n1. Alice Pauline (Phone: 94351253)",
                logic.execute("group view g/Team").getFeedbackToUser());
        assertThrows(CommandException.class, () -> logic.execute("group member g/Team remove/Alice Pauline"));
        assertThrows(CommandException.class, () -> logic.execute("group member g/Team remove/i/3"));
        assertEquals(Set.of(ALICE.getId()), model.getGroup("Team").getMembers());

        logic.execute("group member g/Team add/i/2");
        assertEquals("Groups:\n1. Team (2 members)", logic.execute("group list").getFeedbackToUser());
        logic.execute("group member g/Team remove/i/1");
        assertEquals("Members of Team:\n1. Alice Pauline (Phone: 91234567)",
                logic.execute("group view g/Team").getFeedbackToUser());
        assertEquals("Undid removal of Alice Pauline (Phone: 94351253) from group Team.",
                logic.execute("undo").getFeedbackToUser());
        assertEquals("Groups:\n1. Team (2 members)", logic.execute("group list").getFeedbackToUser());
    }

    @Test
    public void delete_sameNameContact_preservesOtherMembershipAfterSaveAndReload() throws Exception {
        Path dataFile = temporaryFolder.resolve("contacts.json");
        StorageManager storage = new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
        ModelManager model = new ModelManager();
        LogicManager logic = new LogicManager(model, storage);
        try {
            logic.execute("add n/Alex Tan p/91234567 e/alex1@example.com a/NUS");
            logic.execute("group add n/Project Team");
            logic.execute("group member g/Project Team add/Alex Tan");
            logic.execute("add n/Alex Tan p/92345678 e/alex2@example.com a/NUS");
            logic.execute("delete 2");

            assertEquals("Members of Project Team:\n1. Alex Tan (Phone: 91234567)",
                    logic.execute("group view g/Project Team").getFeedbackToUser());
            assertEquals(model.getGroup("Project Team"),
                    new JsonAddressBookStorage(dataFile).readAddressBook().orElseThrow().getGroupList().getFirst());

            logic.execute("group member g/Project Team remove/Alex Tan");
            assertEquals(Set.of(), model.getGroup("Project Team").getMembers());
        } finally {
            storage.closeUndoHistory();
        }
    }

    @Test
    public void createAddRemove_persistsEmptyGroupAndMembership() throws Exception {
        Path dataFile = temporaryFolder.resolve("contacts.json");
        ModelManager model = new ModelManager();
        model.addPerson(ALICE);
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(dataFile);
        StorageManager storage = new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
        LogicManager logic = new LogicManager(model, storage);

        logic.execute("group add n/CS2103T Team");
        assertEquals(0, addressBookStorage.readAddressBook().orElseThrow().getGroupList().get(0)
                .getMembers().size());

        logic.execute("group member g/CS2103T Team add/Alice Pauline");
        Group savedGroup = addressBookStorage.readAddressBook().orElseThrow().getGroupList().get(0);
        assertEquals(1, savedGroup.getMembers().size());
        assertEquals(savedGroup, model.getGroup("CS2103T Team"));

        assertThrows(CommandException.class, () -> logic.execute("group member g/CS2103T Team add/Alice Pauline"));
        logic.execute("group member g/CS2103T Team remove/Alice Pauline");
        assertEquals(0, model.getGroup("CS2103T Team").getMembers().size());
        assertEquals(model.getGroup("CS2103T Team"),
                addressBookStorage.readAddressBook().orElseThrow().getGroupList().get(0));
    }

    @Test
    public void execute_favouritesGroupChanges_preserveContactsGroups() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(ALICE);
        model.addFavourite(ALICE);
        JsonAddressBookStorage contactsStorage = new JsonAddressBookStorage(temporaryFolder.resolve("contacts.json"));
        JsonAddressBookStorage favouritesStorage =
                new JsonAddressBookStorage(temporaryFolder.resolve("favourites.json"));
        StorageManager storage = new StorageManager(contactsStorage, favouritesStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
        LogicManager logic = new LogicManager(model, storage);

        logic.execute("group add n/Team");
        logic.execute("tab favourites");
        assertEquals("No groups exist.", logic.execute("group list").getFeedbackToUser());
        logic.execute("group add n/Team");
        logic.execute("group member g/Team add/Alice Pauline");
        assertEquals("Groups:\n1. Team (1 member)", logic.execute("group list").getFeedbackToUser());
        assertEquals(1, model.getGroup("Team").getMembers().size());
        assertEquals(model.getGroup("Team"),
                favouritesStorage.readAddressBook().orElseThrow().getGroupList().get(0));

        logic.execute("group delete g/Team");
        assertEquals(0, favouritesStorage.readAddressBook().orElseThrow().getGroupList().size());
        assertEquals(1, model.getAddressBook(ContactSection.CONTACTS).getGroupList().size());
        assertEquals(0, contactsStorage.readAddressBook().orElseThrow().getGroupList().get(0).getMembers().size());
    }

    @Test
    public void execute_groupChangesAndClear_undoRestoresSavedStates() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(ALICE);
        JsonAddressBookStorage dataStorage = new JsonAddressBookStorage(temporaryFolder.resolve("contacts.json"));
        StorageManager storage = new StorageManager(dataStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
        LogicManager logic = new LogicManager(model, storage);
        List<AddressBook> states = new ArrayList<>();
        try {
            for (String command : List.of("group add n/Team", "group member g/Team add/Alice Pauline",
                    "group member g/Team remove/Alice Pauline", "group delete g/Team")) {
                states.add(new AddressBook(model.getAddressBook()));
                logic.execute(command);
            }
            assertThrows(CommandException.class, () -> logic.execute("group delete g/Team"));
            for (int i = states.size() - 1; i >= 0; i--) {
                logic.execute("undo");
                assertEquals(states.get(i), model.getAddressBook());
                assertEquals(states.get(i), dataStorage.readAddressBook().orElseThrow());
            }
            assertThrows(CommandException.class, () -> logic.execute("undo"));

            model.addGroup(new Group("Empty Team", Set.of()));
            logic.execute("delete 1");
            AddressBook groupsOnly = new AddressBook(model.getAddressBook());
            logic.execute("clear");
            logic.execute("undo");
            assertEquals(groupsOnly, model.getAddressBook());
            assertEquals(groupsOnly, dataStorage.readAddressBook().orElseThrow());
        } finally {
            storage.closeUndoHistory();
        }
    }

    @Test
    public void invalidGroupOperation_keepsModelUnchanged() throws Exception {
        ModelManager model = new ModelManager();
        StorageManager storage = new StorageManager(
                new JsonAddressBookStorage(temporaryFolder.resolve("contacts.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
        LogicManager logic = new LogicManager(model, storage);

        logic.execute("group add n/Team");
        assertThrows(CommandException.class, () -> logic.execute("group add n/Team"));
        assertThrows(CommandException.class, () -> logic.execute("group member g/Team add/Unknown Person"));
        model.addPerson(ALICE);
        assertThrows(CommandException.class, () -> logic.execute("group member g/Team remove/Alice Pauline"));
        assertEquals(1, model.getAddressBook().getGroupList().size());
        assertEquals(0, model.getGroup("Team").getMembers().size());
    }

    @Test
    public void deleteGroup_removesSavedGroupButKeepsContacts() throws Exception {
        Path dataFile = temporaryFolder.resolve("contacts.json");
        ModelManager model = new ModelManager();
        model.addPerson(ALICE);
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(dataFile);
        StorageManager storage = new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
        LogicManager logic = new LogicManager(model, storage);

        logic.execute("group add n/CS2103T Team");
        logic.execute("group member g/CS2103T Team add/Alice Pauline");
        logic.execute("group delete g/CS2103T Team");

        assertEquals(0, model.getAddressBook().getGroupList().size());
        assertEquals(0, addressBookStorage.readAddressBook().orElseThrow().getGroupList().size());
        assertEquals(1, model.getAddressBook().getPersonList().size());
        assertEquals(1, addressBookStorage.readAddressBook().orElseThrow().getPersonList().size());
        assertThrows(CommandException.class, () -> logic.execute("group delete g/CS2103T Team"));
    }
}
