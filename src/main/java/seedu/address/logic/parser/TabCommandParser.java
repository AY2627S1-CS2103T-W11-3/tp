package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import seedu.address.logic.commands.TabCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ContactSection;

/**
 * Parses the contact section for the tab command.
 * Accepts exactly the lowercase name {@code contacts} or {@code favourites}, ignoring surrounding whitespace.
 */
public class TabCommandParser implements Parser<TabCommand> {
    @Override
    public TabCommand parse(String args) throws ParseException {
        return switch (args.trim()) {
            case "contacts" -> new TabCommand(ContactSection.CONTACTS);
            case "favourites" -> new TabCommand(ContactSection.FAVOURITES);
            default -> throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, TabCommand.MESSAGE_USAGE));
        };
    }
}
