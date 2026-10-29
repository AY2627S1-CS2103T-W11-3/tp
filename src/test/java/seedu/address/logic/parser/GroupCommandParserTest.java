package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.exceptions.ParseException;

/** Tests invalid group command arguments. */
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
}
