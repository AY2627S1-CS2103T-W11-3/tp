package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.person.PersonHasTagsPredicate;

/**
 * Filters and lists all persons in the address book who have all of the specified tags.
 * Tag matching is case-insensitive.
 */
public class FilterCommand extends Command {

    public static final String COMMAND_WORD = "filter";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Filters all persons who have all of the specified tags "
            + "(case-insensitive) and displays them as a list with index numbers.\n"
            + "Parameters: t/TAG [t/TAG]...\n"
            + "Example: " + COMMAND_WORD + " t/friends t/owesMoney";

    public static final String MESSAGE_NO_MATCH = "No contacts match the specified tag(s).";

    private final PersonHasTagsPredicate predicate;

    public FilterCommand(PersonHasTagsPredicate predicate) {
        this.predicate = predicate;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredPersonList(predicate);
        int matchCount = model.getFilteredPersonList().size();
        String message = matchCount == 0
                ? MESSAGE_NO_MATCH
                : String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, matchCount);
        return new CommandResult(message);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof FilterCommand otherFilterCommand)) {
            return false;
        }

        return predicate.equals(otherFilterCommand.predicate);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("predicate", predicate)
                .toString();
    }
}
