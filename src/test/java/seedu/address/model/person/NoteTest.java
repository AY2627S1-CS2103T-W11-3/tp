package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class NoteTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Note(null));
    }

    @Test
    public void constructor_invalidNote_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Note(""));
    }

    @Test
    public void isValidNote() {
        assertThrows(NullPointerException.class, () -> Note.isValidNote(null));

        assertFalse(Note.isValidNote(""));
        assertFalse(Note.isValidNote(" \u2003\u00a0"));
        assertFalse(Note.isValidNote("line one\nline two"));
        assertFalse(Note.isValidNote("contains\ta tab"));
        assertFalse(Note.isValidNote("a".repeat(1001)));

        assertTrue(Note.isValidNote("a"));
        assertTrue(Note.isValidNote("a".repeat(1000)));
        assertTrue(Note.isValidNote("  " + "a".repeat(1000) + "\u00a0"));
        assertTrue(Note.isValidNote("认识于约会应用 😊"));
        assertTrue(Note.isValidNote("😊".repeat(1000)));
    }

    @Test
    public void empty_returnsEmptyNote() {
        assertTrue(Note.empty().isEmpty());
        assertFalse(new Note("note").isEmpty());
    }

    @Test
    public void constructor_paddedNote_trimsValue() {
        assertTrue(new Note(" \u2003Known through an app\u00a0 ").equals(new Note("Known through an app")));
    }
}
