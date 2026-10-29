package seedu.address.model;

import static java.util.Objects.requireNonNull;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.group.Group;
import seedu.address.model.person.Person;
import seedu.address.model.person.UniquePersonList;

/**
 * Wraps all data at the address-book level.
 * Duplicates are not allowed (by .isSamePerson comparison).
 */
public class AddressBook implements ReadOnlyAddressBook {

    private final UniquePersonList persons = new UniquePersonList();
    private final ObservableList<Group> groups = FXCollections.observableArrayList();

    public AddressBook() {}

    /**
     * Creates an AddressBook using the Persons in the {@code toBeCopied}
     */
    public AddressBook(ReadOnlyAddressBook toBeCopied) {
        this();
        resetData(toBeCopied);
    }

    //// list overwrite operations

    /**
     * Replaces the contents of the person list with {@code persons}.
     * {@code persons} must not contain duplicate persons.
     */
    public void setPersons(List<Person> persons) {
        this.persons.setPersons(persons);
    }

    /**
     * Resets the existing data of this {@code AddressBook} with {@code newData}.
     */
    public void resetData(ReadOnlyAddressBook newData) {
        requireNonNull(newData);

        setPersons(newData.getPersonList());
        groups.setAll(newData.getGroupList());
    }

    //// person-level operations

    /**
     * Returns true if a person with the same identity as {@code person} exists in the address book.
     */
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return persons.contains(person);
    }

    /**
     * Returns true if a person with the same identity as {@code person} exists in the address book, excluding
     * {@code personToExclude}.
     */
    public boolean hasPersonOtherThan(Person person, Person personToExclude) {
        requireNonNull(person);
        requireNonNull(personToExclude);
        return persons.containsOtherThan(person, personToExclude);
    }

    /**
     * Adds a person to the address book.
     * The person must not already exist in the address book.
     */
    public void addPerson(Person p) {
        persons.add(p);
    }

    /**
     * Replaces the given person {@code target} in the list with {@code editedPerson}.
     * {@code target} must exist in the address book.
     * The person identity of {@code editedPerson} must not be the same as another existing person in the address book.
     */
    public void setPerson(Person target, Person editedPerson) {
        requireNonNull(editedPerson);

        persons.setPerson(target, editedPerson);
        if (!target.getId().equals(editedPerson.getId())) {
            for (int i = 0; i < groups.size(); i++) {
                Group group = groups.get(i);
                if (group.getMembers().contains(target.getId())) {
                    groups.set(i, group.withoutMember(target.getId()).withMember(editedPerson.getId()));
                }
            }
        }
    }

    /**
     * Removes {@code key} from this {@code AddressBook}.
     * {@code key} must exist in the address book.
     */
    public void removePerson(Person key) {
        persons.remove(key);
        for (int i = 0; i < groups.size(); i++) {
            Group group = groups.get(i);
            if (group.getMembers().contains(key.getId())) {
                groups.set(i, group.withoutMember(key.getId()));
            }
        }
    }

    /** Adds a group whose name is unique. */
    public void addGroup(Group group) {
        requireNonNull(group);
        if (getGroup(group.getName()) != null) {
            throw new IllegalArgumentException("Group already exists");
        }
        groups.add(group);
    }

    /** Removes the group with the given name. */
    public void removeGroup(String name) {
        requireNonNull(name);
        if (!groups.removeIf(group -> group.getName().equals(name))) {
            throw new IllegalArgumentException("Group does not exist");
        }
    }

    /** Returns the group with this name, or null if absent. */
    public Group getGroup(String name) {
        return groups.stream().filter(group -> group.getName().equals(name)).findFirst().orElse(null);
    }

    /** Replaces an existing group. */
    public void setGroup(Group group) {
        requireNonNull(group);
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).getName().equals(group.getName())) {
                groups.set(i, group);
                return;
            }
        }
        throw new IllegalArgumentException("Group does not exist");
    }

    //// util methods

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("persons", persons)
                .add("groups", groups)
                .toString();
    }

    @Override
    public ObservableList<Person> getPersonList() {
        return persons.asUnmodifiableObservableList();
    }

    @Override
    public ObservableList<Group> getGroupList() {
        return FXCollections.unmodifiableObservableList(groups);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof AddressBook otherAddressBook)) {
            return false;
        }

        return persons.equals(otherAddressBook.persons) && groups.equals(otherAddressBook.groups);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(persons, groups);
    }
}
