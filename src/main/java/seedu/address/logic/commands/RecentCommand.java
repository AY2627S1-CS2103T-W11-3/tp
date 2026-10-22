package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.model.Model;

/**
 * Lists all persons in reverse addition order.
 */
public class RecentCommand extends Command {

    public static final String COMMAND_WORD = "recent";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Lists all contacts with the most recently added contact first.\n"
            + "Example: " + COMMAND_WORD;

    public static final String MESSAGE_SUCCESS = "Listed all contacts by most recent addition.";
    public static final String MESSAGE_NO_CONTACTS = "No contacts available.";

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredPersonListToShowRecent();
        String message = model.getFilteredPersonList().isEmpty() ? MESSAGE_NO_CONTACTS : MESSAGE_SUCCESS;
        return new CommandResult(message);
    }
}
