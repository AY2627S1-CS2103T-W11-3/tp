package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.TabCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ContactSection;

public class TabCommandParserTest {
    private final AddressBookParser parser = new AddressBookParser();

    @Test
    public void parseCommand_validSections_success() throws Exception {
        assertEquals(new TabCommand(ContactSection.CONTACTS), parser.parseCommand("tab contacts"));
        assertEquals(new TabCommand(ContactSection.FAVOURITES), parser.parseCommand("  tab \tfavourites \t"));
    }

    @Test
    public void parseCommand_missingUnknownOrExtraSection_throwsParseException() {
        for (String input : List.of("tab", "tab other", "tab Contacts", "tab favourites extra", "Tab contacts")) {
            assertThrows(ParseException.class, () -> parser.parseCommand(input), input);
        }
    }
}
