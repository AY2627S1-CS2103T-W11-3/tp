package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

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
    private final SaveAction saveAction;

    /** A fallible save that completes before the prepared change becomes visible. */
    @FunctionalInterface
    public interface SaveAction {
        /** Persists the candidate or throws before it is applied to the live model. */
        void save() throws IOException;
    }

    /** Creates a change whose application requires no further validation or fallible persistence. */
    public PreparedChange(ReadOnlyAddressBook candidate, Runnable applyChange,
            CommandResult result, String saveErrorMessage) {
        this(candidate, applyChange, result, saveErrorMessage, null);
    }

    /** Creates a change with an optional save operation for commands requiring additional persistence. */
    public PreparedChange(ReadOnlyAddressBook candidate, Runnable applyChange,
            CommandResult result, String saveErrorMessage, SaveAction saveAction) {
        this.candidate = requireNonNull(candidate);
        this.applyChange = requireNonNull(applyChange);
        this.result = requireNonNull(result);
        this.saveErrorMessage = requireNonNull(saveErrorMessage);
        this.saveAction = saveAction;
    }

    public ReadOnlyAddressBook getCandidate() {
        return candidate;
    }

    public String getSaveErrorMessage() {
        return saveErrorMessage;
    }

    /** Saves using the supplied operation, or the normal collection save when no override is needed. */
    public void save(SaveAction defaultSave) throws IOException {
        requireNonNull(defaultSave);
        if (saveAction == null) {
            defaultSave.save();
        } else {
            saveAction.save();
        }
    }

    /** Applies the prepared change after saving and returns its success feedback. */
    public CommandResult apply() {
        applyChange.run();
        return result;
    }
}
