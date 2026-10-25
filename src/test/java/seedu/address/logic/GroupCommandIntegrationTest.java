package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;

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

/** Checks group commands through parsing, saving, and model updates. */
public class GroupCommandIntegrationTest {
    @TempDir
    public Path temporaryFolder;

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
        logic.execute("group add n/Team");
        logic.execute("group member g/Team add/Alice Pauline");
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
