package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.model.ReadOnlyAddressBook;

/**
 * A candidate collection and its validated live-model change.
 * Save the candidate successfully before applying the change, exactly once.
 */
public final class PreparedChange {
    private final ReadOnlyAddressBook candidate;
    private final Runnable applyChange;
    private final CommandResult result;
    private final String saveErrorMessage;

    /** Creates a change whose application requires no further validation or file access. */
    public PreparedChange(ReadOnlyAddressBook candidate, Runnable applyChange,
            CommandResult result, String saveErrorMessage) {
        this.candidate = requireNonNull(candidate);
        this.applyChange = requireNonNull(applyChange);
        this.result = requireNonNull(result);
        this.saveErrorMessage = requireNonNull(saveErrorMessage);
    }

    public ReadOnlyAddressBook getCandidate() {
        return candidate;
    }

    public String getSaveErrorMessage() {
        return saveErrorMessage;
    }

    /** Applies the prepared change after saving and returns its success feedback. */
    public CommandResult apply() {
        applyChange.run();
        return result;
    }
}
