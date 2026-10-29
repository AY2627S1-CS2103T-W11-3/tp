package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;

import java.util.List;
import java.util.Set;

import seedu.address.logic.commands.FilterCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.PersonHasTagsPredicate;
import seedu.address.model.tag.Tag;

/**
 * Parses input arguments and creates a new FilterCommand object
 */
public class FilterCommandParser implements Parser<FilterCommand> {

    /**
     * Parses the given {@code String} of arguments in the context of the FilterCommand
     * and returns a FilterCommand object for execution.
     * @throws ParseException if the user input does not conform to the expected format
     */
    public FilterCommand parse(String args) throws ParseException {
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args, PREFIX_TAG);
        List<String> tagValues = argMultimap.getAllValues(PREFIX_TAG);

        if (!argMultimap.getPreamble().isEmpty() || tagValues.isEmpty() || hasUnexpectedText(tagValues)) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FilterCommand.MESSAGE_USAGE));
        }

        Set<Tag> tags = ParserUtil.parseTags(tagValues);
        return new FilterCommand(new PersonHasTagsPredicate(tags));
    }

    /**
     * Returns true if a tag value contains text outside a single tag name.
     * The tokenizer keeps that text inside the preceding {@code t/} value, so a name such as
     * {@code friends junk} is an unexpected argument rather than an invalid tag.
     */
    private static boolean hasUnexpectedText(List<String> tagValues) {
        return tagValues.stream().anyMatch(value -> value.chars().anyMatch(Character::isWhitespace));
    }
}
