package seedu.address.model;

import java.util.function.Predicate;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.model.group.Group;
import seedu.address.model.person.Person;

/**
 * The API of the Model component.
 */
public interface Model {
    /** {@code Predicate} that always evaluates to true */
    Predicate<Person> PREDICATE_SHOW_ALL_PERSONS = unused -> true;

    /** Returns the collection used by contact operations. */
    ContactSection getActiveSection();

    /** Switches collections, retaining each section's filter and display order for the current session. */
    void setActiveSection(ContactSection section);

    /**
     * Returns the user prefs.
     */
    ReadOnlyUserPrefs getUserPrefs();

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Sets the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);

    /**
     * Replaces the active collection with the data in {@code addressBook}.
     * Removing Contacts also removes matching favorites by ID; replacing Favourites leaves Contacts unchanged.
     */
    void setAddressBook(ReadOnlyAddressBook addressBook);

    /** Returns the complete active collection. */
    ReadOnlyAddressBook getAddressBook();

    /** Returns a complete collection regardless of the active section. */
    ReadOnlyAddressBook getAddressBook(ContactSection section);

    /**
     * Returns true if a person with the same identity as {@code person} exists in the address book.
     */
    boolean hasPerson(Person person);

    /**
     * Returns true if a person with the same identity as {@code person} exists in the address book, excluding
     * {@code personToExclude}. Also checks the counterpart's collection when an ID-linked copy exists.
     */
    boolean hasPersonOtherThan(Person person, Person personToExclude);

    /**
     * Deletes the given person.
     * The person must exist in the active collection. Deleting from Contacts also removes its favorite by ID.
     * Deleting from Favourites leaves the original contact unchanged.
     */
    void deletePerson(Person target);

    /**
     * Adds the given person.
     * {@code person} must not already exist in the address book.
     */
    void addPerson(Person person);

    /**
     * Adds a favorite with the original contact's ID without changing the active section or its search filter.
     */
    void addFavourite(Person person);

    /**
     * Replaces the given person {@code target} with {@code editedPerson}.
     * Updates any counterpart with the same ID and preserves each collection's local group memberships.
     * {@code target} must exist in the active collection.
     * The person identity of {@code editedPerson} must not be the same as another existing person in the address book.
     */
    void setPerson(Person target, Person editedPerson);

    /** Returns the group with this name, or null if absent. */
    Group getGroup(String name);

    /** Adds a new group. */
    void addGroup(Group group);

    /** Deletes an existing group. */
    void deleteGroup(String name);

    /** Replaces an existing group. */
    void setGroup(Group group);

    /** Returns an unmodifiable view of the filtered person list */
    ObservableList<Person> getFilteredPersonList();

    /**
     * Returns the tag names of the active tag filter, or an empty string when the list is not filtered by tag.
     */
    String getActiveFilter();

    /**
     * Updates the filter of the filtered person list to filter by the given {@code predicate}.
     * @throws NullPointerException if {@code predicate} is null.
     */
    void updateFilteredPersonList(Predicate<Person> predicate);

    /**
     * Updates the filtered person list to show all persons in reverse addition order.
     */
    void updateFilteredPersonListToShowRecent();
}
