package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
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
    public void view_sameNameContacts_showsPhoneForEachMatch() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(ALICE);
        model.addPerson(new PersonBuilder(ALICE).withPhone("91234567")
                .withEmail("other-alice@example.com").build());
        StorageManager storage = new StorageManager(
                new JsonAddressBookStorage(temporaryFolder.resolve("contacts.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
        LogicManager logic = new LogicManager(model, storage);

        logic.execute("group add n/Team");
        logic.execute("group member g/Team add/Alice Pauline");

        assertEquals("Groups:\n1. Team (2 members)", logic.execute("group list").getFeedbackToUser());
        assertEquals("Members of Team:\n1. Alice Pauline (Phone: 94351253)"
                        + "\n2. Alice Pauline (Phone: 91234567)",
                logic.execute("group view g/Team").getFeedbackToUser());
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
