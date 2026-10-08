package seedu.address.logic.parser;

import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses input arguments and creates a new DeleteCommand object.
 */
public class DeleteCommandParser implements Parser<DeleteCommand> {

    /**
     * Parses exactly one positive ASCII index, allowing surrounding spaces and tabs.
     * Argument count is validated before index format.
     *
     * @throws ParseException if the input has a missing, extra, or malformed index
     */
    public DeleteCommand parse(String args) throws ParseException {
        String trimmedArgs = args.replaceAll("^[ \t]+|[ \t]+$", "");
        if (trimmedArgs.isEmpty()) {
            throw new ParseException(DeleteCommand.MESSAGE_MISSING_INDEX);
        }
        if (trimmedArgs.split("[ \t]+").length > 1) {
            throw new ParseException(DeleteCommand.MESSAGE_MULTIPLE_INDICES);
        }
        try {
            return new DeleteCommand(ParserUtil.parsePositiveInteger(trimmedArgs));
        } catch (ParseException pe) {
            throw new ParseException(DeleteCommand.MESSAGE_INVALID_INDEX, pe);
        }
    }
}
