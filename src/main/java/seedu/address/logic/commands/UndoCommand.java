package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UndoHistory;

/**
 * Restores the most recent contact snapshot and shows all contacts after it has been saved.
 */
public class UndoCommand extends Command implements StagedCommand {
    public static final String COMMAND_WORD = "undo";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Undoes the most recent change to the address book.\n"
            + "Example: " + COMMAND_WORD;
    public static final String MESSAGE_SUCCESS = "Undo successful.";
    public static final String MESSAGE_NO_HISTORY = "No changes to undo.";
    public static final String MESSAGE_SAVE_ERROR =
            "Could not save changes. Nothing was undone. Please try again.";
    public static final String MESSAGE_LOAD_ERROR =
            "Could not read undo history. Nothing was undone. Please try again.";

    private static final Logger logger = LogsCenter.getLogger(UndoCommand.class);

    private final UndoHistory undoHistory;

    /**
     * Creates an undo command using the given history to prepare and consume a snapshot.
     */
    public UndoCommand(UndoHistory undoHistory) {
        this.undoHistory = requireNonNull(undoHistory);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        return prepare(model).apply();
    }

    @Override
    public PreparedChange prepare(Model model) throws CommandException {
        requireNonNull(model);
        ReadOnlyAddressBook snapshot;
        try {
            snapshot = undoHistory.readLatestSnapshot()
                    .orElseThrow(() -> new CommandException(MESSAGE_NO_HISTORY));
        } catch (DataLoadingException e) {
            logger.warning("Could not read undo snapshot: " + e.getMessage());
            throw new CommandException(MESSAGE_LOAD_ERROR, e);
        }
        return new PreparedChange(snapshot, () -> {
            model.setAddressBook(snapshot);
            model.updateFilteredPersonList(Model.PREDICATE_SHOW_ALL_PERSONS);
            undoHistory.discardLatestSnapshot();
        }, new CommandResult(MESSAGE_SUCCESS), MESSAGE_SAVE_ERROR);
    }
}
