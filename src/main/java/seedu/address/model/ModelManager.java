package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.model.group.Group;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonHasTagsPredicate;
import seedu.address.model.person.exceptions.DuplicatePersonException;

/**
 * Represents the in-memory model of the address book data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final Map<ContactSection, AddressBook> addressBooks = new EnumMap<>(ContactSection.class);
    private final Map<ContactSection, FilteredList<Person>> filteredPersonLists = new EnumMap<>(ContactSection.class);
    private final Map<ContactSection, SortedList<Person>> displayedPersonLists = new EnumMap<>(ContactSection.class);
    private final UserPrefs userPrefs;
    private ContactSection activeSection = ContactSection.CONTACTS;
    private FilteredList<Person> filteredPersons;
    private SortedList<Person> displayedPersons;
    private ObservableList<Person> visiblePersons;
    private boolean isRecentPersonListActive;
    private String activeFilter = "";

    /**
     * Initializes a ModelManager with the given addressBook and userPrefs.
     */
    public ModelManager(ReadOnlyAddressBook addressBook, ReadOnlyUserPrefs userPrefs) {
        this(addressBook, new AddressBook(), userPrefs);
    }

    /** Initializes independent contacts and favorites with shared application preferences. */
    public ModelManager(ReadOnlyAddressBook contacts, ReadOnlyAddressBook favourites, ReadOnlyUserPrefs userPrefs) {
        requireAllNonNull(contacts, favourites, userPrefs);

        logger.fine("Initializing contact collections and user prefs " + userPrefs);
        addressBooks.put(ContactSection.CONTACTS, new AddressBook(contacts));
        addressBooks.put(ContactSection.FAVOURITES, new AddressBook(favourites));
        this.userPrefs = new UserPrefs(userPrefs);
        initializeFilteredPersons();
    }

    /** Initializes empty contacts and favorites with default preferences. */
    public ModelManager() {
        this(new AddressBook(), new UserPrefs());
    }

    //=========== UserPrefs ==================================================================================

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    //=========== AddressBook ================================================================================

    @Override
    public ContactSection getActiveSection() {
        return activeSection;
    }

    @Override
    public void setActiveSection(ContactSection section) {
        requireNonNull(section);
        if (activeSection != section) {
            activeSection = section;
            initializeFilteredPersons();
        }
    }

    @Override
    public void setAddressBook(ReadOnlyAddressBook addressBook) {
        AddressBook replacement = new AddressBook(requireNonNull(addressBook));
        if (activeSection == ContactSection.CONTACTS) {
            Set<UUID> retainedIds = replacement.getPersonList().stream()
                    .map(Person::getId).collect(Collectors.toSet());
            for (Person person : List.copyOf(getAddressBook().getPersonList())) {
                if (!retainedIds.contains(person.getId())) {
                    removeMatchingFavourite(person);
                }
            }
        }
        addressBooks.get(activeSection).resetData(replacement);
    }

    @Override
    public ReadOnlyAddressBook getAddressBook() {
        return getAddressBook(activeSection);
    }

    @Override
    public ReadOnlyAddressBook getAddressBook(ContactSection section) {
        return addressBooks.get(requireNonNull(section));
    }

    @Override
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return addressBooks.get(activeSection).hasPerson(person);
    }

    @Override
    public boolean hasPersonOtherThan(Person person, Person personToExclude) {
        requireAllNonNull(person, personToExclude);
        if (addressBooks.get(activeSection).hasPersonOtherThan(person, personToExclude)) {
            return true;
        }
        ContactSection otherSection = activeSection == ContactSection.CONTACTS
                ? ContactSection.FAVOURITES : ContactSection.CONTACTS;
        Person other = findById(otherSection, personToExclude);
        return other != null && addressBooks.get(otherSection).hasPersonOtherThan(person, other);
    }

    @Override
    public void deletePerson(Person target) {
        addressBooks.get(activeSection).removePerson(target);
        if (activeSection == ContactSection.CONTACTS) {
            removeMatchingFavourite(target);
        }
    }

    @Override
    public void addPerson(Person person) {
        addressBooks.get(activeSection).addPerson(person);
        updatePersonListAfterAdd();
    }

    @Override
    public void addFavourite(Person person) {
        addressBooks.get(ContactSection.FAVOURITES).addPerson(person);
    }

    @Override
    public void setPerson(Person target, Person editedPerson) {
        requireAllNonNull(target, editedPerson);
        if (hasPersonOtherThan(editedPerson, target)) {
            throw new DuplicatePersonException();
        }
        addressBooks.get(activeSection).setPerson(target, editedPerson);
        ContactSection otherSection = activeSection == ContactSection.CONTACTS
                ? ContactSection.FAVOURITES : ContactSection.CONTACTS;
        Person other = findById(otherSection, target);
        if (other != null) {
            addressBooks.get(otherSection).setPerson(other, editedPerson);
        }
    }

    /**
     * Returns the counterpart identified by its persistent ID, ignoring editable contact details.
     */
    private Person findById(ContactSection section, Person person) {
        return addressBooks.get(section).getPersonList().stream()
                .filter(candidate -> candidate.getId().equals(person.getId())).findFirst().orElse(null);
    }

    /**
     * Removes a favorite counterpart and its local group memberships when its original is deleted.
     */
    private void removeMatchingFavourite(Person person) {
        Person favourite = findById(ContactSection.FAVOURITES, person);
        if (favourite != null) {
            addressBooks.get(ContactSection.FAVOURITES).removePerson(favourite);
        }
    }

    @Override
    public Group getGroup(String name) {
        return addressBooks.get(activeSection).getGroup(name);
    }

    @Override
    public void addGroup(Group group) {
        addressBooks.get(activeSection).addGroup(group);
    }

    @Override
    public void deleteGroup(String name) {
        addressBooks.get(activeSection).removeGroup(name);
    }

    @Override
    public void setGroup(Group group) {
        addressBooks.get(activeSection).setGroup(group);
    }

    //=========== Filtered Person List Accessors =============================================================

    /** Selects the active collection's view, retaining its previous filter and display order. */
    private void initializeFilteredPersons() {
        filteredPersons = filteredPersonLists.computeIfAbsent(activeSection,
                section -> new FilteredList<>(addressBooks.get(section).getPersonList()));
        displayedPersons = displayedPersonLists.computeIfAbsent(activeSection,
                section -> new SortedList<>(filteredPersons));
        visiblePersons = FXCollections.unmodifiableObservableList(displayedPersons);
        isRecentPersonListActive = displayedPersons.getComparator() != null;
        activeFilter = filteredPersons.getPredicate() instanceof PersonHasTagsPredicate tagPredicate
                ? tagPredicate.getDisplayNames()
                : "";
    }

    /**
     * Returns an unmodifiable view of the list of {@code Person} backed by the internal list of
     * the active collection.
     */
    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return visiblePersons;
    }

    @Override
    public String getActiveFilter() {
        return activeFilter;
    }

    @Override
    public void updateFilteredPersonList(Predicate<Person> predicate) {
        requireNonNull(predicate);
        displayedPersons.setComparator(null);
        filteredPersons.setPredicate(predicate);
        isRecentPersonListActive = false;
        activeFilter = predicate instanceof PersonHasTagsPredicate tagPredicate
                ? tagPredicate.getDisplayNames()
                : "";
    }

    /** Copies the visible membership and order used to resolve indices in a staged command. */
    void copyPersonListView(Model source) {
        List<Person> displayed = List.copyOf(source.getFilteredPersonList());
        Set<Person> visible = Set.copyOf(displayed);
        updateFilteredPersonList(visible::contains);
        displayedPersons.setComparator(Comparator.comparingInt(displayed::indexOf));
    }

    @Override
    public void updateFilteredPersonListToShowRecent() {
        ObservableList<Person> persons = getAddressBook().getPersonList();
        activeFilter = "";
        filteredPersons.setPredicate(PREDICATE_SHOW_ALL_PERSONS);
        displayedPersons.setComparator((first, second) -> Integer.compare(
                persons.indexOf(second), persons.indexOf(first)));
        isRecentPersonListActive = true;
    }

    /** Shows all persons after an add unless the recent view is active. */
    private void updatePersonListAfterAdd() {
        if (!isRecentPersonListActive) {
            updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ModelManager otherModelManager)) {
            return false;
        }

        return addressBooks.equals(otherModelManager.addressBooks)
                && activeSection == otherModelManager.activeSection
                && userPrefs.equals(otherModelManager.userPrefs)
                && isRecentPersonListActive == otherModelManager.isRecentPersonListActive
                && activeFilter.equals(otherModelManager.activeFilter)
                && displayedPersons.equals(otherModelManager.displayedPersons);
    }

}
