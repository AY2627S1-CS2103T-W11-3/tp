package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.GroupCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/** Tests group command parsing and usage guidance. */
public class GroupCommandParserTest {
    private final GroupCommandParser parser = new GroupCommandParser();

    @Test
    public void parse_missingOrInvalidArguments_throwsParseException() {
        assertThrows(ParseException.class, () -> parser.parse(" add"));
        assertThrows(ParseException.class, () -> parser.parse(" add n/ "));
        assertThrows(ParseException.class, () -> parser.parse(" delete g/ "));
        assertThrows(ParseException.class, () -> parser.parse(" member g/Team add/ "));
        assertThrows(ParseException.class, () -> parser.parse(" member g/Team remove/Invalid/Name"));
        assertThrows(ParseException.class, () -> parser.parse(" member g/Team add/i/0"));
        assertThrows(ParseException.class, () -> parser.parse(" member g/Team remove/i/abc"));
        assertThrows(ParseException.class, () -> parser.parse(" list extra"));
        assertThrows(ParseException.class, () -> parser.parse(" view"));
        assertThrows(ParseException.class, () -> parser.parse(" view g/ "));
        assertThrows(ParseException.class, () -> parser.parse(" view g/!!!"));
    }

    @Test
    public void parse_invalidSyntax_showsFormattedUsage() {
        ParseException exception = assertThrows(ParseException.class, () -> parser.parse("unknown"));
        assertEquals(String.format(MESSAGE_INVALID_COMMAND_FORMAT, GroupCommand.MESSAGE_USAGE),
                exception.getMessage());
        assertTrue(GroupCommand.MESSAGE_USAGE.contains("group member g/GROUP_NAME add/i/INDEX"));
        assertTrue(GroupCommand.MESSAGE_USAGE.contains("group member g/GROUP_NAME remove/i/INDEX"));
    }

    @Test
    public void parse_usageExamples_areValidCommands() {
        assertDoesNotThrow(() -> parser.parse("list"));
        assertDoesNotThrow(() -> parser.parse("view g/Project Team"));
        assertDoesNotThrow(() -> parser.parse("add n/Project Team"));
        assertDoesNotThrow(() -> parser.parse("delete g/Project Team"));
        assertDoesNotThrow(() -> parser.parse("member g/Project Team add/Alex Tan"));
        assertDoesNotThrow(() -> parser.parse("member g/Project Team remove/Alex Tan"));
        assertDoesNotThrow(() -> parser.parse("member g/Project Team add/i/2"));
        assertDoesNotThrow(() -> parser.parse("member g/Project Team remove/i/2"));
    }
}
