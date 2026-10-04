package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_REMARK;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.RemarkCommand;
import seedu.address.model.person.Remark;

public class RemarkCommandParserTest {

    private final RemarkCommandParser parser = new RemarkCommandParser();

    @Test
    public void parse_indexSpecified_success() {
        assertParseSuccess(parser, "1 " + PREFIX_REMARK + "Likes baseball",
                new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Likes baseball")));
    }

    @Test
    public void parse_emptyOrMissingRemark_success() {
        RemarkCommand expectedCommand = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(""));
        assertParseSuccess(parser, "1 " + PREFIX_REMARK, expectedCommand);
        assertParseSuccess(parser, "1", expectedCommand);
    }

    @Test
    public void parse_invalidIndex_failure() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, RemarkCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "", expectedMessage);
        assertParseFailure(parser, PREFIX_REMARK + "Likes baseball", expectedMessage);
        assertParseFailure(parser, "0 " + PREFIX_REMARK + "Likes baseball", expectedMessage);
        assertParseFailure(parser, "one " + PREFIX_REMARK + "Likes baseball", expectedMessage);
        assertParseFailure(parser, "1 extra " + PREFIX_REMARK + "Likes baseball", expectedMessage);
    }

    @Test
    public void parse_duplicateRemarkPrefix_failure() {
        assertParseFailure(parser, "1 " + PREFIX_REMARK + "First " + PREFIX_REMARK + "Second",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_REMARK));
    }
}
