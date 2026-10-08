package seedu.address.logic.parser;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Note;

/**
 * Separates the optional final note from the other arguments of an add or edit command.
 */
record NoteArgument(String arguments, Optional<Note> note) {
    private static final Pattern NOTE_PREFIX_PATTERN = Pattern.compile("(?i)(?<!\\S)note/");
    private static final Pattern PREFIX_AFTER_NOTE_PATTERN =
            Pattern.compile("(?<!\\S)(?:n/|p/|e/|a/|t/|(?i:note/))");

    /**
     * Parses a note using the same validation and prefix placement rules for both commands.
     */
    static NoteArgument parse(String args) throws ParseException {
        Matcher matcher = NOTE_PREFIX_PATTERN.matcher(args);
        if (!matcher.find()) {
            return new NoteArgument(args, Optional.empty());
        }
        String rawNote = args.substring(matcher.end());
        if (PREFIX_AFTER_NOTE_PATTERN.matcher(rawNote).find()) {
            throw new ParseException(AddCommand.MESSAGE_NOTE_NOT_AT_END);
        }
        return new NoteArgument(args.substring(0, matcher.start()), Optional.of(ParserUtil.parseNote(rawNote)));
    }
}
