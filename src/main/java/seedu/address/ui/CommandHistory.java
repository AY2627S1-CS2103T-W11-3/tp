package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;

/**
 * Stores commands entered during the current application session.
 */
public class CommandHistory {

    private static final String TEMP_FILE_PREFIX = "addressbook-command-history-";
    private static final String TEMP_FILE_SUFFIX = ".txt";
    private static final Logger logger = LogsCenter.getLogger(CommandHistory.class);

    private final Optional<Path> historyFile;
    private final List<String> commands = new ArrayList<>();
    private String commandDraft = "";
    private int nextCommandIndex;

    private CommandHistory() {
        historyFile = Optional.empty();
    }

    CommandHistory(Path historyFile) throws IOException {
        this.historyFile = Optional.of(historyFile);
        Files.writeString(historyFile, "", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Creates an empty command history backed by a temporary text file, or by memory if file creation fails.
     */
    public static CommandHistory create() {
        return create(() -> Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX));
    }

    static CommandHistory create(TemporaryFileCreator temporaryFileCreator) {
        try {
            Path historyFile = temporaryFileCreator.create();
            historyFile.toFile().deleteOnExit();
            return new CommandHistory(historyFile);
        } catch (IOException e) {
            logger.warning("Could not initialize command history file; using in-memory history: " + e.getMessage());
            return new CommandHistory();
        }
    }

    /**
     * Adds a command to the end of this session's history.
     * The command remains available in memory if it cannot be appended to the temporary file.
     *
     * @throws IOException if the command cannot be appended to the temporary file
     */
    public void add(String command) throws IOException {
        requireNonNull(command);
        commands.add(command);
        commandDraft = "";
        nextCommandIndex = commands.size();
        if (historyFile.isPresent()) {
            Files.writeString(historyFile.get(), command + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
        }
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

    /**
     * Creates the temporary file used to store command history.
     */
    @FunctionalInterface
    interface TemporaryFileCreator {
        /**
         * Creates and returns the temporary file path.
         */
        Path create() throws IOException;
    }
}
