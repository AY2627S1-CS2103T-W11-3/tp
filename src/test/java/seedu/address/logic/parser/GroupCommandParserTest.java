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
    }
}
