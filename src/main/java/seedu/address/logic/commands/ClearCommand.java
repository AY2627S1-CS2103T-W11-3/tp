package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.model.AddressBook;
import seedu.address.model.ContactSection;
import seedu.address.model.Model;
import seedu.address.model.ReadOnlyAddressBook;

/**
 * Clears the address book.
 */
public class ClearCommand extends UndoableCommand {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_SUCCESS = "Address book has been cleared!";


    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.setAddressBook(new AddressBook());
        return new CommandResult(MESSAGE_SUCCESS);
    }

    @Override
    public String describeUndo(ReadOnlyAddressBook previous, ReadOnlyAddressBook candidate, ContactSection section) {
        return "Undid clear of " + requireNonNull(section).getDisplayName() + ".";
    }
}
