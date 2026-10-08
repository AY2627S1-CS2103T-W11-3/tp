package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's optional note in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidNote(String)}.
 */
public class Note {

    public static final String MESSAGE_CONSTRAINTS =
            "Notes must be 1–1000 characters long and cannot contain line breaks or control characters.";

    private static final int MAX_LENGTH = 1000;
    private static final Note EMPTY_NOTE = new Note();

    public final String value;

    /**
     * Constructs a {@code Note} containing a validated value.
     *
     * @param note A valid, non-empty note.
     */
    public Note(String note) {
        requireNonNull(note);
        checkArgument(isValidNote(note), MESSAGE_CONSTRAINTS);
        value = stripUnicodeWhitespace(note);
    }

    /**
     * Constructs the singleton empty note used when the optional note is not specified.
     */
    private Note() {
        value = "";
    }

    /**
     * Returns an empty note representing an omitted optional note.
     */
    public static Note empty() {
        return EMPTY_NOTE;
    }

    /**
     * Returns true if this note represents an omitted optional note.
     */
    public boolean isEmpty() {
        return value.isEmpty();
    }

    /**
     * Returns true if a given string is a valid note.
     */
    public static boolean isValidNote(String test) {
        requireNonNull(test);
        if (!containsOnlyPrintableCharacters(test)) {
            return false;
        }
        String trimmedTest = stripUnicodeWhitespace(test);
        int length = trimmedTest.codePointCount(0, trimmedTest.length());
        return length >= 1 && length <= MAX_LENGTH;
    }

    /**
     * Returns true if the string contains no line breaks, control characters, or other non-printing code points.
     */
    private static boolean containsOnlyPrintableCharacters(String test) {
        requireNonNull(test);
        return test.codePoints().allMatch(Note::isPrintable);
    }

    private static boolean isPrintable(int codePoint) {
        int type = Character.getType(codePoint);
        return Character.isDefined(codePoint)
                && !Character.isISOControl(codePoint)
                && type != Character.FORMAT
                && type != Character.LINE_SEPARATOR
                && type != Character.PARAGRAPH_SEPARATOR
                && type != Character.SURROGATE;
    }

    private static String stripUnicodeWhitespace(String value) {
        int start = 0;
        int end = value.length();

        while (start < end) {
            int codePoint = value.codePointAt(start);
            if (!isUnicodeWhitespace(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }
        while (start < end) {
            int codePoint = value.codePointBefore(end);
            if (!isUnicodeWhitespace(codePoint)) {
                break;
            }
            end -= Character.charCount(codePoint);
        }
        return value.substring(start, end);
    }

    private static boolean isUnicodeWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof Note otherNote)) {
            return false;
        }

        return value.equals(otherNote.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
