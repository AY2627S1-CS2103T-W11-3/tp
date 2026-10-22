package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.RecentCommand;

public class RecentCommandParserTest {

    private final RecentCommandParser parser = new RecentCommandParser();

    @Test
    public void parse_noArguments_returnsRecentCommand() throws Exception {
        assertTrue(parser.parse("   ") instanceof RecentCommand);
    }

    @Test
    public void parse_unexpectedArguments_throwsParseException() {
        assertParseFailure(parser, "contacts",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, RecentCommand.MESSAGE_USAGE));
    }
}
