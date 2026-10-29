package seedu.address.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.group.Group;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;

/**
 * An Immutable AddressBook that is serializable to JSON format.
 */
@JsonRootName(value = "addressbook")
class JsonSerializableAddressBook {

    public static final String MESSAGE_DUPLICATE_PERSON = "Persons list contains duplicate person(s).";
    public static final String MESSAGE_DUPLICATE_ID = "Persons list contains duplicate contact IDs.";

    private final List<JsonAdaptedPerson> persons = new ArrayList<>();
    private final List<JsonAdaptedGroup> groups = new ArrayList<>();

    /**
     * Constructs a {@code JsonSerializableAddressBook} with the given persons.
     */
    @JsonCreator
    public JsonSerializableAddressBook(@JsonProperty("persons") List<JsonAdaptedPerson> persons,
            @JsonProperty("groups") List<JsonAdaptedGroup> groups) {
        if (persons == null) {
            throw new IllegalArgumentException("The persons field must be a list.");
        }
        this.persons.addAll(persons);
        if (groups != null) {
            this.groups.addAll(groups);
        }
    }

    /** Creates an address book without groups for existing callers. */
    public JsonSerializableAddressBook(List<JsonAdaptedPerson> persons) {
        this(persons, null);
    }

    /**
     * Converts a given {@code ReadOnlyAddressBook} into this class for Jackson use.
     *
     * @param source future changes to this will not affect the created {@code JsonSerializableAddressBook}.
     */
    public JsonSerializableAddressBook(ReadOnlyAddressBook source) {
        persons.addAll(source.getPersonList().stream().map(JsonAdaptedPerson::new).collect(Collectors.toList()));
        groups.addAll(source.getGroupList().stream().map(JsonAdaptedGroup::new).collect(Collectors.toList()));
    }

    /**
     * Converts this address book into the model's {@code AddressBook} object.
     *
     * @throws IllegalValueException if there were any data constraints violated.
     */
    public AddressBook toModelType() throws IllegalValueException {
        AddressBook addressBook = new AddressBook();
        Set<UUID> contactIds = new HashSet<>();
        for (JsonAdaptedPerson jsonAdaptedPerson : persons) {
            if (jsonAdaptedPerson == null) {
                throw new IllegalValueException("A contact must not be null.");
            }
            Person person = jsonAdaptedPerson.toModelType();
            if (!contactIds.add(person.getId())) {
                throw new IllegalValueException(MESSAGE_DUPLICATE_ID);
            }
            if (addressBook.hasPerson(person)) {
                throw new IllegalValueException(MESSAGE_DUPLICATE_PERSON);
            }
            addressBook.addPerson(person);
        }
        for (JsonAdaptedGroup jsonAdaptedGroup : groups) {
            if (jsonAdaptedGroup == null) {
                throw new IllegalValueException("A group must not be null.");
            }
            Group group = jsonAdaptedGroup.toModelType();
            if (addressBook.getGroup(group.getName()) != null) {
                throw new IllegalValueException("Groups list contains duplicate group names.");
            }
            for (Name member : group.getMembers()) {
                if (addressBook.getPersonList().stream().noneMatch(person -> person.getName().equals(member))) {
                    throw new IllegalValueException("Group contains a contact that does not exist.");
                }
            }
            addressBook.addGroup(group);
        }
        return addressBook;
    }

}
