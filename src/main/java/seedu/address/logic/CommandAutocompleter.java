package seedu.address.logic;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Completes the command word at the start of the command box text.
 * Matching is case-sensitive prefix matching. Repeated completion cycles
 * through matches for the same typed prefix.
 */
public class CommandAutocompleter {

    private final List<String> commandWords;
    private String activePrefix;
    private int matchIndex = -1;

    /**
     * Creates an autocompleter for the given command words.
     * {@code commandWords} should already be in the desired cycle order.
     */
    public CommandAutocompleter(List<String> commandWords) {
        requireNonNull(commandWords);
        this.commandWords = List.copyOf(commandWords);
    }

    /**
     * Clears cycling state. Call when the user edits the command text manually.
     */
    public void reset() {
        activePrefix = null;
        matchIndex = -1;
    }

    /**
     * Suggests a completion for the command word under the caret.
     *
     * @param text full command box text
     * @param caretPosition caret index within {@code text}
     * @return a completion if the caret is in the first word and a match exists
     */
    public Optional<Completion> complete(String text, int caretPosition) {
        requireNonNull(text);
        if (caretPosition < 0 || caretPosition > text.length()) {
            return Optional.empty();
        }

        int wordStart = 0;
        while (wordStart < text.length() && Character.isWhitespace(text.charAt(wordStart))) {
            wordStart++;
        }
        int wordEnd = wordStart;
        while (wordEnd < text.length() && !Character.isWhitespace(text.charAt(wordEnd))) {
            wordEnd++;
        }

        if (caretPosition < wordStart || caretPosition > wordEnd) {
            reset();
            return Optional.empty();
        }

        String currentWord = text.substring(wordStart, wordEnd);
        if (currentWord.isEmpty()) {
            reset();
            return Optional.empty();
        }

        boolean isCycling = false;
        if (activePrefix != null && matchIndex >= 0) {
            List<String> previousMatches = matchesFor(activePrefix);
            if (matchIndex < previousMatches.size()
                    && currentWord.equals(previousMatches.get(matchIndex))) {
                isCycling = true;
            }
        }

        if (!isCycling) {
            activePrefix = currentWord;
            matchIndex = -1;
        }

        List<String> matches = matchesFor(activePrefix);
        if (matches.isEmpty()) {
            reset();
            return Optional.empty();
        }

        if (isCycling) {
            matchIndex = (matchIndex + 1) % matches.size();
        } else {
            matchIndex = 0;
        }

        String completed = matches.get(matchIndex);
        String newText = text.substring(0, wordStart) + completed + text.substring(wordEnd);
        int newCaret = wordStart + completed.length();
        return Optional.of(new Completion(newText, newCaret));
    }

    private List<String> matchesFor(String prefix) {
        List<String> matches = new ArrayList<>();
        for (String word : commandWords) {
            if (word.startsWith(prefix)) {
                matches.add(word);
            }
        }
        return matches;
    }

    /**
     * Result of a successful autocomplete.
     */
    public record Completion(String newText, int newCaretPosition) {
        public Completion {
            requireNonNull(newText);
        }
    }
}
