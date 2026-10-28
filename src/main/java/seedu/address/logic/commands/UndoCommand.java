package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.ContactSection;
import seedu.address.model.ContactSnapshot;
import seedu.address.model.Model;
import seedu.address.model.UndoHistory;

/**
 * Restores both collections before the latest successful change, regardless of the active section.
 */
public class UndoCommand extends Command implements StagedCommand {
    public static final String COMMAND_WORD = "undo";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Undoes the most recent change to Contacts or Favourites.\n"
            + "Example: " + COMMAND_WORD;
    /** Default feedback when the recorded operation has no more specific description. */
    public static final String MESSAGE_SUCCESS = UndoHistory.MESSAGE_SUCCESS;
    public static final String MESSAGE_NO_HISTORY = "No changes to undo.";
    public static final String MESSAGE_SAVE_ERROR =
            "Could not save changes. Nothing was undone. Please try again.";
    public static final String MESSAGE_LOAD_ERROR =
            "Could not read undo history. Nothing was undone. Please try again.";

    private static final Logger logger = LogsCenter.getLogger(UndoCommand.class);

    private final UndoHistory undoHistory;

    /**
     * Creates an undo command using shared history to restore both collections together.
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
        ContactSection section = model.getActiveSection();
        ContactSnapshot snapshot;
        try {
            snapshot = undoHistory.readLatestSnapshot()
                    .orElseThrow(() -> new CommandException(MESSAGE_NO_HISTORY));
        } catch (DataLoadingException e) {
            logger.warning("Could not read undo snapshot for " + section + ": " + e.getMessage());
            throw new CommandException(MESSAGE_LOAD_ERROR, e);
        }
        return new PreparedChange(snapshot.getAddressBook(section), () -> {
            for (ContactSection restoredSection : ContactSection.values()) {
                model.setActiveSection(restoredSection);
                model.setAddressBook(snapshot.getAddressBook(restoredSection));
                model.updateFilteredPersonList(Model.PREDICATE_SHOW_ALL_PERSONS);
            }
            model.setActiveSection(section);
            undoHistory.discardLatestSnapshot();
        }, new CommandResult(undoHistory.getLatestUndoDescription()), MESSAGE_SAVE_ERROR, () ->
                undoHistory.saveRestoredSnapshot(snapshot));
    }
}
