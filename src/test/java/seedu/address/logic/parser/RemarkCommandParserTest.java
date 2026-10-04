package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
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
    public void parse_validRemark_success() {
        assertParseSuccess(parser, " 1 r/ Likes baseball ",
                new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Likes baseball")));
    }

    @Test
    public void parse_emptyOrMissingRemark_clearsRemark() {
        RemarkCommand expected = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(""));
        assertParseSuccess(parser, " 1 r/", expected);
        assertParseSuccess(parser, " 1", expected);
    }

    @Test
    public void parse_invalidIndex_failure() {
        String message = String.format(MESSAGE_INVALID_COMMAND_FORMAT, RemarkCommand.MESSAGE_USAGE);
        for (String input : new String[] {"", " r/note", " 0 r/note", " -1 r/note", " one r/note",
            " 2147483648 r/note", " 1 extra r/note"}) {
            assertParseFailure(parser, input, message);
        }
    }

    @Test
    public void parse_repeatedPrefix_failure() {
        assertParseFailure(parser, " 1 r/first r/second",
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_REMARK));
    }

    @Test
    public void parseCommand_remark_routesToParser() throws Exception {
        assertEquals(new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Likes baseball")),
                new AddressBookParser().parseCommand("remark 1 r/Likes baseball"));
    }
}
