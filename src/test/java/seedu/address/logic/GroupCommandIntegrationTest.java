package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
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
    public void invalidGroupOperation_keepsModelUnchanged() throws Exception {
        ModelManager model = new ModelManager();
        StorageManager storage = new StorageManager(
                new JsonAddressBookStorage(temporaryFolder.resolve("contacts.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json")));
        LogicManager logic = new LogicManager(model, storage);

        logic.execute("group add n/Team");
        assertThrows(CommandException.class, () -> logic.execute("group add n/Team"));
        assertThrows(CommandException.class, () -> logic.execute("group member g/Team add/Unknown Person"));
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
