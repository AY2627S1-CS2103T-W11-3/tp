package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.AddressBook;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.TypicalPersons;

public class JsonSerializableAddressBookTest {

    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonSerializableAddressBookTest");
    private static final Path TYPICAL_PERSONS_FILE = TEST_DATA_FOLDER.resolve("typicalPersonsAddressBook.json");
    private static final Path INVALID_PERSON_FILE = TEST_DATA_FOLDER.resolve("invalidPersonAddressBook.json");
    private static final Path DUPLICATE_PERSON_FILE = TEST_DATA_FOLDER.resolve("duplicatePersonAddressBook.json");

    @Test
    public void toModelType_typicalPersonsFile_success() throws Exception {
        JsonSerializableAddressBook dataFromFile = JsonUtil.readJsonFile(TYPICAL_PERSONS_FILE,
                JsonSerializableAddressBook.class).get();
        AddressBook addressBookFromFile = dataFromFile.toModelType();
        AddressBook typicalPersonsAddressBook = TypicalPersons.getTypicalAddressBook();
        assertEquals(addressBookFromFile, typicalPersonsAddressBook);
    }

    @Test
    public void toModelType_invalidPersonFile_throwsIllegalValueException() throws Exception {
        JsonSerializableAddressBook dataFromFile = JsonUtil.readJsonFile(INVALID_PERSON_FILE,
                JsonSerializableAddressBook.class).get();
        assertThrows(IllegalValueException.class, dataFromFile::toModelType);
    }

    @Test
    public void toModelType_duplicatePersons_throwsIllegalValueException() throws Exception {
        JsonSerializableAddressBook dataFromFile = JsonUtil.readJsonFile(DUPLICATE_PERSON_FILE,
                JsonSerializableAddressBook.class).get();
        assertThrows(IllegalValueException.class, JsonSerializableAddressBook.MESSAGE_DUPLICATE_PERSON,
                dataFromFile::toModelType);
    }

    @Test
    public void toModelType_duplicateIds_throwsIllegalValueException() {
        JsonSerializableAddressBook data = new JsonSerializableAddressBook(List.of(
                new JsonAdaptedPerson(ALICE),
                new JsonAdaptedPerson(new PersonBuilder(ALICE).withName(BOB.getName().fullName)
                        .withPhone(BOB.getPhone().value).withEmail(BOB.getEmail().value).build())));

        assertThrows(IllegalValueException.class, JsonSerializableAddressBook.MESSAGE_DUPLICATE_ID, data::toModelType);
    }

    @Test
    public void toModelType_invalidGroups_throwsIllegalValueException() {
        JsonAdaptedGroup group = new JsonAdaptedGroup("Team", List.of());
        JsonSerializableAddressBook nullGroup = new JsonSerializableAddressBook(
                List.of(), Arrays.asList((JsonAdaptedGroup) null));
        JsonSerializableAddressBook duplicateGroups = new JsonSerializableAddressBook(List.of(), List.of(group, group));
        JsonSerializableAddressBook missingMember = new JsonSerializableAddressBook(
                List.of(), List.of(new JsonAdaptedGroup("Team", List.of("Missing Contact"))));
        JsonSerializableAddressBook invalidName = new JsonSerializableAddressBook(
                List.of(), List.of(new JsonAdaptedGroup(" ", List.of())));

        assertThrows(IllegalValueException.class, nullGroup::toModelType);
        assertThrows(IllegalValueException.class, duplicateGroups::toModelType);
        assertThrows(IllegalValueException.class, missingMember::toModelType);
        assertThrows(IllegalValueException.class, invalidName::toModelType);
    }

    @Test
    public void toModelType_invalidGroupMembers_throwsIllegalValueException() {
        JsonAdaptedGroup nullMembers = new JsonAdaptedGroup("Team", null);
        JsonAdaptedGroup nullMember = new JsonAdaptedGroup("Team", Arrays.asList((String) null));
        JsonAdaptedGroup duplicateMembers = new JsonAdaptedGroup("Team", List.of("Alice Pauline", "Alice Pauline"));
        JsonAdaptedGroup invalidMember = new JsonAdaptedGroup("Team", List.of("invalid/name"));

        assertThrows(IllegalValueException.class, () -> nullMembers.toModelType(List.of(ALICE)));
        assertThrows(IllegalValueException.class, () -> nullMember.toModelType(List.of(ALICE)));
        assertThrows(IllegalValueException.class, () -> duplicateMembers.toModelType(List.of(ALICE)));
        assertThrows(IllegalValueException.class, () -> invalidMember.toModelType(List.of(ALICE)));
    }

    @Test
    public void toModelType_legacyNameMembership_migratesSameNameContactsToIds() throws Exception {
        Person otherAlice = new PersonBuilder().withName("Alice Pauline").withPhone("91234567")
                .withEmail("second-alice@example.com").build();
        JsonSerializableAddressBook legacy = new JsonSerializableAddressBook(
                List.of(new JsonAdaptedPerson(ALICE), new JsonAdaptedPerson(otherAlice)),
                List.of(new JsonAdaptedGroup("Team", List.of("Alice Pauline"))));

        AddressBook migrated = legacy.toModelType();
        assertEquals(Set.of(ALICE.getId(), otherAlice.getId()), migrated.getGroup("Team").getMembers());

        String savedJson = JsonUtil.toJsonString(new JsonSerializableAddressBook(migrated));
        assertTrue(savedJson.contains(ALICE.getId().toString()));
        assertTrue(savedJson.contains(otherAlice.getId().toString()));
        assertEquals(migrated, JsonUtil.fromJsonString(savedJson, JsonSerializableAddressBook.class).toModelType());
    }

    @Test
    public void toModelType_unknownMemberId_rejectsGroup() {
        JsonSerializableAddressBook data = new JsonSerializableAddressBook(
                List.of(new JsonAdaptedPerson(ALICE)),
                List.of(new JsonAdaptedGroup("Team", List.of(UUID.randomUUID().toString()))));

        assertThrows(IllegalValueException.class, data::toModelType);
    }

    @Test
    public void toModelType_legacyConstructor_defaultsToNoGroups() throws Exception {
        JsonSerializableAddressBook legacyBook = new JsonSerializableAddressBook(List.of());

        assertEquals(List.of(), legacyBook.toModelType().getGroupList());
    }

}
