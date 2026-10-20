package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.FavouriteCommand;
import seedu.address.logic.parser.exceptions.ParseException;

public class FavouriteCommandParserTest {
    private final AddressBookParser parser = new AddressBookParser();

    @Test
    public void parseCommand_validIndexWithWhitespaceAndOversizedIndex_success() throws Exception {
        assertEquals(new FavouriteCommand(BigInteger.ONE), parser.parseCommand("  favourite \t1 \t"));
        String oversized = "9".repeat(1000);
        assertEquals(new FavouriteCommand(new BigInteger(oversized)), parser.parseCommand("favourite " + oversized));
    }

    @Test
    public void parseCommand_missingExtraOrInvalidIndex_reportsFavouriteUsage() {
        for (String input : List.of("favourite", "favourite 1 2", "favourite Alice", "favourite 0")) {
            assertEquals(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FavouriteCommand.MESSAGE_USAGE),
                    assertThrows(ParseException.class, () -> parser.parseCommand(input)).getMessage());
        }
        assertThrows(ParseException.class, () -> parser.parseCommand("Favourite 1"));
    }
}
