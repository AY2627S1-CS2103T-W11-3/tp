package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import seedu.address.commons.core.index.Index;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;

/**
 * Views the full details of a person identified by the index in the displayed person list.
 */
public class ViewCommand extends Command {

    public static final String COMMAND_WORD = "view";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Views the person identified by the index number used in the displayed person list.\n"
            + "Parameters: INDEX (must be a positive integer)\n"
            + "Example: " + COMMAND_WORD + " 1";

    public static final String MESSAGE_VIEW_PERSON_SUCCESS = "Viewed person: %1$s";
    public static final String MESSAGE_INDEX_OUT_OF_RANGE =
            "Invalid index. Please select an index shown in the contact list.";
    public static final String MESSAGE_NO_CONTACTS = "No contacts available to view.";

    private final BigInteger targetIndex;

    /**
     * Creates a command using an existing application index.
     */
    public ViewCommand(Index targetIndex) {
        this(BigInteger.valueOf(requireNonNull(targetIndex).getOneBased()));
    }

    /**
     * Creates a command with a positive one-based index, including indices too large for an int.
     */
    public ViewCommand(BigInteger targetIndex) {
        requireNonNull(targetIndex);
        if (targetIndex.signum() <= 0) {
            throw new IllegalArgumentException("Contact index must be positive");
        }
        this.targetIndex = targetIndex;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> lastShownList = model.getFilteredPersonList();

        if (lastShownList.isEmpty()) {
            throw new CommandException(MESSAGE_NO_CONTACTS);
        }

        if (targetIndex.compareTo(BigInteger.valueOf(lastShownList.size())) > 0) {
            throw new CommandException(MESSAGE_INDEX_OUT_OF_RANGE);
        }

        Person personToView = lastShownList.get(targetIndex.intValueExact() - 1);
        return new CommandResult(formatDetails(personToView));
    }

    /**
     * Returns the full details of {@code person} for display in the result box.
     */
    private String formatDetails(Person person) {
        String tags = person.getTags().isEmpty()
                ? "Not specified"
                : person.getTags().stream()
                .map(tag -> tag.tagName)
                .sorted()
                .collect(Collectors.joining(", "));
        String note = person.getNote().isEmpty() ? "" : "Note: " + person.getNote() + "\n";

        return "Name: " + person.getName() + "\n"
                + "Phone: " + person.getPhone() + "\n"
                + "Email: " + person.getEmail() + "\n"
                + "Address: " + person.getAddress() + "\n"
                + note
                + "Tags: " + tags + "\n"
                + "\n"
                + "Showing details for " + person.getName() + ".";
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof ViewCommand otherViewCommand)) {
            return false;
        }

        return targetIndex.equals(otherViewCommand.targetIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetIndex", targetIndex)
                .toString();
    }
}
