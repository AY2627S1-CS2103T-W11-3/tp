package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import seedu.address.logic.commands.UndoCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.UndoHistory;

/**
 * Parses the argument-free undo command.
 */
public class UndoCommandParser implements Parser<UndoCommand> {
    private final UndoHistory undoHistory;

    /**
     * Creates a parser that supplies history to undo commands without reading or modifying it.
     */
    public UndoCommandParser(UndoHistory undoHistory) {
        this.undoHistory = requireNonNull(undoHistory);
    }

    @Override
    public UndoCommand parse(String args) throws ParseException {
        requireNonNull(args);
        if (!args.isBlank()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, UndoCommand.MESSAGE_USAGE));
        }
        return new UndoCommand(undoHistory);
    }
}
