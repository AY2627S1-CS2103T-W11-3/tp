package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Note;

public class NoteArgumentTest {

    @Test
    public void parse_noteAbsent_returnsOriginalArgumentsAndEmptyNote() throws Exception {
        String arguments = " n/Amy p/12345678 e/amy@example.com a/NUS";

        NoteArgument result = NoteArgument.parse(arguments);

        assertEquals(arguments, result.arguments());
        assertTrue(result.note().isEmpty());
    }

    @Test
    public void parse_mixedCaseFinalNote_separatesAndParsesNote() throws Exception {
        NoteArgument result = NoteArgument.parse(" n/Amy p/12345678 NoTe/  Met at NUS  ");

        assertEquals(" n/Amy p/12345678 ", result.arguments());
        assertEquals(new Note("Met at NUS"), result.note().orElseThrow());
    }

    @Test
    public void parse_emptyNote_throwsNoteConstraintMessage() {
        assertThrows(ParseException.class, Note.MESSAGE_CONSTRAINTS, () -> NoteArgument.parse(" n/Amy note/   "));
    }

    @Test
    public void parse_prefixAfterNote_throwsPlacementMessage() {
        String expectedMessage = AddCommand.MESSAGE_NOTE_NOT_AT_END;
        String fieldAfterNote = " n/Amy note/Met at NUS p/12345678";
        String duplicateNote = " n/Amy note/First NOTE/Second";

        assertThrows(ParseException.class, expectedMessage, () -> NoteArgument.parse(fieldAfterNote));
        assertThrows(ParseException.class, expectedMessage, () -> NoteArgument.parse(duplicateNote));
    }

    @Test
    public void parse_unknownSlashTextInNote_treatsItAsNoteContent() throws Exception {
        NoteArgument result = NoteArgument.parse(" n/Amy note/Profile: source/example");

        assertFalse(result.note().isEmpty());
        assertEquals(new Note("Profile: source/example"), result.note().orElseThrow());
    }
}
