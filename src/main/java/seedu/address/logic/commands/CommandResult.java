package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.person.Person;

/**
 * Represents the result of a command execution.
 */
public class CommandResult {

    private static final int NO_HIGHLIGHT = -1;

    private final String feedbackToUser;

    /** Help information should be shown to the user. */
    private final boolean showHelp;

    /** The application should exit. */
    private final boolean exit;

    private final Person personToView;
    private final int highlightedIndex;

    /**
     * Constructs a {@code CommandResult} with the specified fields.
     */
    public CommandResult(String feedbackToUser, boolean showHelp, boolean exit) {
        this(feedbackToUser, showHelp, exit, null, NO_HIGHLIGHT);
    }

    private CommandResult(String feedbackToUser, boolean showHelp, boolean exit,
            Person personToView, int highlightedIndex) {
        this.feedbackToUser = requireNonNull(feedbackToUser);
        this.showHelp = showHelp;
        this.exit = exit;
        this.personToView = personToView;
        this.highlightedIndex = highlightedIndex;
    }

    /**
     * Constructs a {@code CommandResult} with the specified {@code feedbackToUser},
     * and other fields set to their default value.
     */
    public CommandResult(String feedbackToUser) {
        this(feedbackToUser, false, false);
    }

    /**
     * Constructs feedback with a contact to display in the details panel.
     */
    public CommandResult(String feedbackToUser, Person personToView) {
        this(feedbackToUser, false, false, requireNonNull(personToView), NO_HIGHLIGHT);
    }

    public Optional<Person> getPersonToView() {
        return Optional.ofNullable(personToView);
    }

    /** Constructs feedback requesting a highlight at the given zero-based index. */
    public static CommandResult withHighlight(String feedbackToUser, int highlightedIndex) {
        return withHighlight(feedbackToUser, highlightedIndex, null);
    }

    /** Constructs feedback with a highlighted row and optional contact details. */
    public static CommandResult withHighlight(String feedbackToUser, int highlightedIndex, Person personToView) {
        if (highlightedIndex < 0) {
            throw new IllegalArgumentException("Highlighted index must not be negative");
        }
        return new CommandResult(feedbackToUser, false, false, personToView, highlightedIndex);
    }

    public String getFeedbackToUser() {
        return feedbackToUser;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public boolean isExit() {
        return exit;
    }

    /** Returns the zero-based index of the displayed contact to highlight, if the command asks for one. */
    public OptionalInt getHighlightedIndex() {
        return highlightedIndex == NO_HIGHLIGHT ? OptionalInt.empty() : OptionalInt.of(highlightedIndex);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof CommandResult otherCommandResult)) {
            return false;
        }

        return feedbackToUser.equals(otherCommandResult.feedbackToUser)
                && showHelp == otherCommandResult.showHelp
                && exit == otherCommandResult.exit
                && Objects.equals(personToView, otherCommandResult.personToView)
                && highlightedIndex == otherCommandResult.highlightedIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedbackToUser, showHelp, exit, personToView, highlightedIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("feedbackToUser", feedbackToUser)
                .add("showHelp", showHelp)
                .add("exit", exit)
                .add("highlightedIndex", highlightedIndex)
                .toString();
    }

}
