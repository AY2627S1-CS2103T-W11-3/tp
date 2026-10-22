package seedu.address.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stores commands entered during the current application session.
 */
public class CommandHistory {

    private static final String TEMP_FILE_PREFIX = "addressbook-command-history-";
    private static final String TEMP_FILE_SUFFIX = ".txt";

    private final Path historyFile;
    private final List<String> commands = new ArrayList<>();
    private int nextCommandIndex;

    /**
     * Creates an empty command history backed by a temporary text file.
     */
    public CommandHistory() throws IOException {
        this(Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX));
        historyFile.toFile().deleteOnExit();
    }

    CommandHistory(Path historyFile) throws IOException {
        this.historyFile = historyFile;
        Files.writeString(historyFile, "", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Adds a command to the end of this session's history.
     */
    public void add(String command) throws IOException {
        Files.writeString(historyFile, command + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);
        commands.add(command);
        nextCommandIndex = commands.size();
    }

    /**
     * Returns the preceding command, or an empty value if the history has reached its beginning.
     */
    public Optional<String> getPreviousCommand() {
        if (nextCommandIndex == 0) {
            return Optional.empty();
        }

        nextCommandIndex--;
        return Optional.of(commands.get(nextCommandIndex));
    }

    /**
     * Returns the following command, or an empty value if the history has reached its end.
     */
    public Optional<String> getNextCommand() {
        if (nextCommandIndex >= commands.size() - 1) {
            return Optional.empty();
        }

        nextCommandIndex++;
        return Optional.of(commands.get(nextCommandIndex));
    }
}
