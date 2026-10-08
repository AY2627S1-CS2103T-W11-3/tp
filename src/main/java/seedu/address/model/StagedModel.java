package seedu.address.model;

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
    private boolean hasContactChanges;

    /**
     * Copies all contacts and the currently visible contacts used to resolve command indices.
     * The live model's filter remains untouched unless the command explicitly changes it.
     */
    public StagedModel(Model source) {
        candidate = new ModelManager(source.getAddressBook(), source.getUserPrefs());
        Set<Person> visiblePersons = Set.copyOf(source.getFilteredPersonList());
        candidate.updateFilteredPersonList(visiblePersons::contains);
    }

    public boolean hasContactChanges() {
        return hasContactChanges;
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
        candidate.setAddressBook(snapshot);
        changes.add(model -> model.setAddressBook(snapshot));
        hasContactChanges = true;
    }

    @Override
    public ReadOnlyAddressBook getAddressBook() {
        return candidate.getAddressBook();
    }

    @Override
    public boolean hasPerson(Person person) {
        return candidate.hasPerson(person);
    }

    @Override
    public void deletePerson(Person target) {
        candidate.deletePerson(target);
        changes.add(model -> model.deletePerson(target));
        hasContactChanges = true;
    }

    @Override
    public void addPerson(Person person) {
        candidate.addPerson(person);
        changes.add(model -> model.addPerson(person));
        hasContactChanges = true;
    }

    @Override
    public void setPerson(Person target, Person editedPerson) {
        candidate.setPerson(target, editedPerson);
        changes.add(model -> model.setPerson(target, editedPerson));
        hasContactChanges = true;
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
