package seedu.address.logic;

import java.util.List;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.FavouriteCommand;
import seedu.address.logic.commands.FilterCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.RecentCommand;
import seedu.address.logic.commands.TabCommand;
import seedu.address.logic.commands.ViewCommand;

/**
 * Registry of command words recognised by the application.
 * Used for command-word autocomplete. Keep in sync when adding a new command.
 */
public final class CommandWords {

    private static final List<String> WORDS = List.of(
            AddCommand.COMMAND_WORD,
            ClearCommand.COMMAND_WORD,
            DeleteCommand.COMMAND_WORD,
            EditCommand.COMMAND_WORD,
            ExitCommand.COMMAND_WORD,
            FavouriteCommand.COMMAND_WORD,
            FilterCommand.COMMAND_WORD,
            FindCommand.COMMAND_WORD,
            HelpCommand.COMMAND_WORD,
            ListCommand.COMMAND_WORD,
            RecentCommand.COMMAND_WORD,
            TabCommand.COMMAND_WORD,
            ViewCommand.COMMAND_WORD
    ).stream().sorted().toList();

    private CommandWords() {}

    /**
     * Returns an unmodifiable list of command words in alphabetical order.
     */
    public static List<String> get() {
        return WORDS;
    }
}
