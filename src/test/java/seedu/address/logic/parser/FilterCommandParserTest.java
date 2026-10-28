package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.FilterCommand;
import seedu.address.model.person.PersonHasTagsPredicate;
import seedu.address.model.tag.Tag;

public class FilterCommandParserTest {

    private FilterCommandParser parser = new FilterCommandParser();

    @Test
    public void parse_emptyArg_throwsParseException() {
        assertParseFailure(parser, "     ",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, FilterCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_missingTagPrefix_throwsParseException() {
        assertParseFailure(parser, "friends",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, FilterCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_preamblePresent_throwsParseException() {
        assertParseFailure(parser, "Alice t/friends",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, FilterCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_unexpectedTextAfterTag_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FilterCommand.MESSAGE_USAGE);
        assertParseFailure(parser, " t/friends junk", expectedMessage);
        assertParseFailure(parser, " t/friends t/owesMoney extra", expectedMessage);
    }

    @Test
    public void parse_invalidTag_throwsParseException() {
        assertParseFailure(parser, " t/friend@", Tag.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_validArgs_returnsFilterCommand() {
        FilterCommand expectedFilterCommand =
                new FilterCommand(new PersonHasTagsPredicate(Set.of(new Tag("friends"))));
        assertParseSuccess(parser, " t/friends", expectedFilterCommand);

        // multiple whitespaces
        assertParseSuccess(parser, " \n t/friends \t", expectedFilterCommand);
    }

    @Test
    public void parse_multipleTags_returnsFilterCommand() {
        FilterCommand expectedFilterCommand = new FilterCommand(
                new PersonHasTagsPredicate(Set.of(new Tag("friends"), new Tag("owesMoney"))));
        assertParseSuccess(parser, " t/friends t/owesMoney", expectedFilterCommand);

        // order of tags does not matter
        assertParseSuccess(parser, " t/owesMoney t/friends", expectedFilterCommand);
    }

    @Test
    public void parse_duplicateTags_returnsFilterCommand() {
        FilterCommand expectedFilterCommand =
                new FilterCommand(new PersonHasTagsPredicate(Set.of(new Tag("friends"))));
        assertParseSuccess(parser, " t/friends t/friends", expectedFilterCommand);
    }
}
