package seedu.address.model;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.model.person.Person;

/**
 * Prepares a command on an isolated model and records changes for application after a successful save.
 * Commands must use the Model API for all changes. Persons are immutable and can be shared safely.
 */
public class StagedModel implements Model {
    private final Model candidate;
    private final List<Consumer<Model>> changes = new ArrayList<>();
    private ContactSection changedSection;

    /**
     * Copies all contacts and the currently visible contacts used to resolve command indices.
     * The live model's filter remains untouched unless the command explicitly changes it.
     */
    public StagedModel(Model source) {
        candidate = new ModelManager(source.getAddressBook(ContactSection.CONTACTS),
                source.getAddressBook(ContactSection.FAVOURITES), source.getUserPrefs());
        candidate.setActiveSection(source.getActiveSection());
        Set<Person> visiblePersons = Set.copyOf(source.getFilteredPersonList());
        candidate.updateFilteredPersonList(visiblePersons::contains);
    }

    public boolean hasContactChanges() {
        return changedSection != null;
    }

    /** Returns the collection changed by this command, after a contact change has been recorded. */
    public ContactSection getChangedSection() {
        return requireNonNull(changedSection);
    }

    /** Each command saves one collection, so reject changes spanning multiple files. */
    private void recordContactChange(ContactSection section) {
        if (changedSection != null && changedSection != section) {
            throw new IllegalStateException("A command cannot change both contact collections");
        }
        changedSection = section;
    }

    @Override
    public ContactSection getActiveSection() {
        return candidate.getActiveSection();
    }

    @Override
    public void setActiveSection(ContactSection section) {
        candidate.setActiveSection(section);
        changes.add(model -> model.setActiveSection(section));
    }

    /**
     * Applies the validated operations to the unchanged source model, once, after saving succeeds.
     * Replaying model operations preserves incremental list notifications and the live search filter.
     */
    public void commitTo(Model target) {
        changes.forEach(change -> change.accept(target));
        changes.clear();
    }

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return candidate.getUserPrefs();
    }

    @Override
    public GuiSettings getGuiSettings() {
        return candidate.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        candidate.setGuiSettings(guiSettings);
        changes.add(model -> model.setGuiSettings(guiSettings));
    }

    @Override
    public void setAddressBook(ReadOnlyAddressBook addressBook) {
        AddressBook snapshot = new AddressBook(addressBook);
        recordContactChange(candidate.getActiveSection());
        candidate.setAddressBook(snapshot);
        changes.add(model -> model.setAddressBook(snapshot));
    }

    @Override
    public ReadOnlyAddressBook getAddressBook() {
        return candidate.getAddressBook();
    }

    @Override
    public ReadOnlyAddressBook getAddressBook(ContactSection section) {
        return candidate.getAddressBook(section);
    }

    @Override
    public boolean hasPerson(Person person) {
        return candidate.hasPerson(person);
    }

    @Override
    public void deletePerson(Person target) {
        recordContactChange(candidate.getActiveSection());
        candidate.deletePerson(target);
        changes.add(model -> model.deletePerson(target));
    }

    @Override
    public void addPerson(Person person) {
        recordContactChange(candidate.getActiveSection());
        candidate.addPerson(person);
        changes.add(model -> model.addPerson(person));
    }

    @Override
    public void addFavourite(Person person) {
        recordContactChange(ContactSection.FAVOURITES);
        candidate.addFavourite(person);
        changes.add(model -> model.addFavourite(person));
    }

    @Override
    public void setPerson(Person target, Person editedPerson) {
        recordContactChange(candidate.getActiveSection());
        candidate.setPerson(target, editedPerson);
        changes.add(model -> model.setPerson(target, editedPerson));
    }

    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return candidate.getFilteredPersonList();
    }

    @Override
    public void updateFilteredPersonList(Predicate<Person> predicate) {
        candidate.updateFilteredPersonList(predicate);
        changes.add(model -> model.updateFilteredPersonList(predicate));
    }
}
