package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ReadOnlyAddressBook;

/**
 * Represents a contact-changing command whose previous data can be restored from a snapshot.
 * Logic saves the snapshot and candidate together before publishing the command's changes.
 */
public abstract class UndoableCommand extends Command {

    /**
     * Copies the contact data before execution so later model changes cannot alter the undo snapshot.
     */
    public final ReadOnlyAddressBook createSnapshot(Model model) {
        requireNonNull(model);
        return new AddressBook(model.getAddressBook());
    }
}
