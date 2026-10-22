package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import seedu.address.logic.commands.FavouriteCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/** Parses exactly one positive ASCII index without narrowing oversized integers. */
public class FavouriteCommandParser implements Parser<FavouriteCommand> {
    @Override
    public FavouriteCommand parse(String args) throws ParseException {
        try {
            return new FavouriteCommand(ParserUtil.parsePositiveInteger(args));
        } catch (ParseException e) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FavouriteCommand.MESSAGE_USAGE), e);
        }
    }
}
