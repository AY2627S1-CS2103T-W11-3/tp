package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import seedu.address.logic.commands.RecentCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses input arguments and creates a new RecentCommand object.
 */
public class RecentCommandParser implements Parser<RecentCommand> {

    /**
     * Parses the given {@code String} of arguments in the context of the RecentCommand
     * and returns a RecentCommand object for execution.
     *
     * @throws ParseException if the user input contains arguments
     */
    public RecentCommand parse(String args) throws ParseException {
        if (!args.trim().isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, RecentCommand.MESSAGE_USAGE));
        }
        return new RecentCommand();
    }
}
