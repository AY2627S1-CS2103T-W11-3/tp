package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stores commands entered during the current application session.
 */
public class CommandHistory {

    private final List<String> commands = new ArrayList<>();
    private String commandDraft = "";
    private int nextCommandIndex;

    /**
     * Adds a command to the end of this session's history.
     */
    public void add(String command) {
        requireNonNull(command);
        commands.add(command);
        commandDraft = "";
        nextCommandIndex = commands.size();
    }

    /**
     * Saves the current draft when history navigation begins, then returns the preceding command.
     * Returns an empty value if the history has reached its beginning.
     */
    public Optional<String> getPreviousCommand(String currentCommandText) {
        requireNonNull(currentCommandText);
        assert nextCommandIndex >= 0 && nextCommandIndex <= commands.size();

        if (nextCommandIndex == commands.size()) {
            commandDraft = currentCommandText;
        }

        if (nextCommandIndex == 0) {
            return Optional.empty();
        }

        nextCommandIndex--;
        return Optional.of(commands.get(nextCommandIndex));
    }

    /**
     * Returns the following command, or the saved draft after the newest command.
     * Returns an empty value if the history has already reached its end.
     */
    public Optional<String> getNextCommand() {
        assert nextCommandIndex >= 0 && nextCommandIndex <= commands.size();

        if (nextCommandIndex >= commands.size()) {
            return Optional.empty();
        }

        nextCommandIndex++;
        if (nextCommandIndex == commands.size()) {
            return Optional.of(commandDraft);
        }

        return Optional.of(commands.get(nextCommandIndex));
    }
}
