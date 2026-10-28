package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Set;

import seedu.address.model.AddressBook;
import seedu.address.model.ContactSection;
import seedu.address.model.Model;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;

/**
 * Represents a contact-changing command whose previous data can be restored from a snapshot.
 * Logic saves the snapshot and candidate together before publishing the command's changes.
 */
public abstract class UndoableCommand extends Command {

    /**
     * Describes the operation using the original and prepared collections before publishing changes.
     */
    public abstract String describeUndo(ReadOnlyAddressBook previous, ReadOnlyAddressBook candidate,
            ContactSection section);

    /**
     * Identifies the changed contact without reusing an index after filtering or sorting has changed.
     */
    protected final String describePersonChange(String operation, ReadOnlyAddressBook source,
            ReadOnlyAddressBook comparison, ContactSection section) {
        requireNonNull(operation);
        requireNonNull(source);
        requireNonNull(comparison);
        requireNonNull(section);
        Set<Person> unchangedPersons = Set.copyOf(comparison.getPersonList());
        return source.getPersonList().stream().filter(person -> !unchangedPersons.contains(person))
                .findFirst().map(person -> "Undid " + operation + " of " + person.getName().fullName + ".")
                .orElse("Undid " + operation + " in " + section.getDisplayName() + ".");
    }

    /**
     * Copies the changed section's original data before publishing a command's changes to the live model.
     */
    public final ReadOnlyAddressBook createSnapshot(Model model, ContactSection section) {
        requireNonNull(model);
        return new AddressBook(model.getAddressBook(requireNonNull(section)));
    }
}
