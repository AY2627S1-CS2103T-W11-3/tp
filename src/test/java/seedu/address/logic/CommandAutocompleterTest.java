package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.logic.CommandAutocompleter.Completion;

public class CommandAutocompleterTest {

    private CommandAutocompleter autocompleter;

    @BeforeEach
    public void setUp() {
        autocompleter = new CommandAutocompleter(List.of(
                "add", "clear", "delete", "edit", "exit", "filter", "find", "help", "list", "view"));
    }

    @Test
    public void complete_emptyText_returnsEmpty() {
        assertFalse(autocompleter.complete("", 0).isPresent());
        assertFalse(autocompleter.complete("   ", 2).isPresent());
    }

    @Test
    public void complete_uniquePrefix_completesCommandWord() {
        Optional<Completion> result = autocompleter.complete("li", 2);

        assertTrue(result.isPresent());
        assertEquals("list", result.get().newText());
        assertEquals(4, result.get().newCaretPosition());
    }

    @Test
    public void complete_multipleMatches_cyclesInOrder() {
        Optional<Completion> first = autocompleter.complete("e", 1);
        assertEquals("edit", first.get().newText());

        Optional<Completion> second = autocompleter.complete("edit", 4);
        assertEquals("exit", second.get().newText());

        Optional<Completion> third = autocompleter.complete("exit", 4);
        assertEquals("edit", third.get().newText());
    }

    @Test
    public void complete_sharedPrefix_cyclesFilterAndFind() {
        Optional<Completion> first = autocompleter.complete("fi", 2);
        assertEquals("filter", first.get().newText());

        Optional<Completion> second = autocompleter.complete("filter", 6);
        assertEquals("find", second.get().newText());

        Optional<Completion> third = autocompleter.complete("find", 4);
        assertEquals("filter", third.get().newText());
    }

    @Test
    public void complete_exactCommandWord_returnsSameWord() {
        Optional<Completion> result = autocompleter.complete("list", 4);

        assertTrue(result.isPresent());
        assertEquals("list", result.get().newText());
    }

    @Test
    public void complete_noMatch_returnsEmpty() {
        assertFalse(autocompleter.complete("xyz", 3).isPresent());
    }

    @Test
    public void complete_caretInArguments_returnsEmpty() {
        assertFalse(autocompleter.complete("find alice", 7).isPresent());
        assertFalse(autocompleter.complete("find alice", 10).isPresent());
    }

    @Test
    public void complete_preservesLeadingSpacesAndTrailingArgs() {
        Optional<Completion> result = autocompleter.complete("  fi alice", 4);

        assertTrue(result.isPresent());
        assertEquals("  filter alice", result.get().newText());
        assertEquals(8, result.get().newCaretPosition());
    }

    @Test
    public void complete_afterManualEdit_usesNewPrefix() {
        autocompleter.complete("e", 1);
        autocompleter.reset();

        Optional<Completion> result = autocompleter.complete("de", 2);
        assertEquals("delete", result.get().newText());
    }

    @Test
    public void complete_invalidCaret_returnsEmpty() {
        assertFalse(autocompleter.complete("list", -1).isPresent());
        assertFalse(autocompleter.complete("list", 5).isPresent());
    }

    @Test
    public void commandWords_areSortedAndIncludeExpectedCommands() {
        List<String> words = CommandWords.get();
        assertEquals(List.of(
                "add", "clear", "delete", "edit", "exit", "favourite", "filter", "find",
                "help", "list", "recent", "tab", "undo", "view"), words);
        assertEquals(words, words.stream().sorted().toList());
    }
}
