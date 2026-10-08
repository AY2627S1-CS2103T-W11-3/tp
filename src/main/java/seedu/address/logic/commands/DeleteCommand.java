package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.util.List;

import seedu.address.commons.core.index.Index;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.person.Person;

/** Deletes a person identified using its displayed index from the address book. */
public class DeleteCommand extends Command implements StagedCommand {

    public static final String COMMAND_WORD = "delete";

    public static final String MESSAGE_USAGE =
            COMMAND_WORD
                    + ": Deletes the person identified by the index number used in the displayed"
                    + " person list.\n"
                    + "Parameters: <INDEX> (must be a positive integer)\n"
                    + "Example: "
                    + COMMAND_WORD
                    + " 1";

    public static final String MESSAGE_DELETE_PERSON_SUCCESS = "Deleted contact: %1$s (index %2$d).";

    public static final String MESSAGE_MISSING_INDEX = "Missing contact index. Usage: delete <INDEX>";
    public static final String MESSAGE_MULTIPLE_INDICES =
            "Expected exactly one contact index. Usage: delete <INDEX>";
    public static final String MESSAGE_INVALID_INDEX =
            "Invalid contact index. Enter a positive whole number without leading zeros. Usage:"
                    + " delete <INDEX>";
    public static final String MESSAGE_EMPTY_LIST = "No contacts are displayed. Nothing to delete.";
    public static final String MESSAGE_INDEX_OUT_OF_RANGE =
            "Contact index out of range. Choose an index from 1 to %1$d.";

    public static final String MESSAGE_SAVE_ERROR =
            "Could not save changes. Contact was not deleted. Please try again.";

    private final BigInteger targetIndex;

    /** Creates a command using an existing application index. */
    public DeleteCommand(Index targetIndex) {
        this(BigInteger.valueOf(requireNonNull(targetIndex).getZeroBased()).add(BigInteger.ONE));
    }

    /**
     * Creates a command with a positive one-based index, including indices too large for an int.
     */
    public DeleteCommand(BigInteger targetIndex) {
        requireNonNull(targetIndex);
        if (targetIndex.signum() <= 0) {
            throw new IllegalArgumentException("Contact index must be positive");
        }
        this.targetIndex = targetIndex;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        return prepare(model).apply();
    }

    @Override
    public PreparedChange prepare(Model model) throws CommandException {
        Person target = getTargetPerson(model);
        AddressBook candidate = new AddressBook(model.getAddressBook());
        candidate.removePerson(target);
        CommandResult result = new CommandResult(
                String.format(MESSAGE_DELETE_PERSON_SUCCESS, target.getName().fullName, targetIndex));
        return new PreparedChange(candidate, () -> model.deletePerson(target), result, MESSAGE_SAVE_ERROR);
    }

    /**
     * Validates the displayed index and returns its contact without changing the model.
     */
    private Person getTargetPerson(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> lastShownList = model.getFilteredPersonList();

        if (lastShownList.isEmpty()) {
            throw new CommandException(MESSAGE_EMPTY_LIST);
        }
        if (targetIndex.compareTo(BigInteger.valueOf(lastShownList.size())) > 0) {
            throw new CommandException(
                    String.format(MESSAGE_INDEX_OUT_OF_RANGE, lastShownList.size()));
        }

        return lastShownList.get(targetIndex.intValueExact() - 1);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof DeleteCommand otherDeleteCommand)) {
            return false;
        }

        return targetIndex.equals(otherDeleteCommand.targetIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("targetIndex", targetIndex).toString();
    }
}
