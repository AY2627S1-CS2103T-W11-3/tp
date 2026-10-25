package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.model.AddressBook;
import seedu.address.model.ContactSection;
import seedu.address.model.Model;
import seedu.address.model.ReadOnlyAddressBook;

/**
 * Represents a contact-changing command whose previous data can be restored from a snapshot.
 * Logic saves the snapshot and candidate together before publishing the command's changes.
 */
public abstract class UndoableCommand extends Command {

    /**
     * Copies the changed section's original data before publishing a command's changes to the live model.
     */
    public final ReadOnlyAddressBook createSnapshot(Model model, ContactSection section) {
        requireNonNull(model);
        return new AddressBook(model.getAddressBook(requireNonNull(section)));
    }
}
