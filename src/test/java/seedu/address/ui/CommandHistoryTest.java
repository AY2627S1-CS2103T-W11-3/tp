package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CommandHistoryTest {

    @TempDir
    private Path temporaryFolder;

    @Test
    public void constructor_existingFile_clearsHistory() throws Exception {
        Path historyFile = temporaryFolder.resolve("command-history.txt");
        Files.writeString(historyFile, "command from an earlier launch");

        new CommandHistory(historyFile);

        assertEquals("", Files.readString(historyFile));
    }

    @Test
    public void add_multipleCommands_storesCommandsAndNavigatesInBothDirections() throws Exception {
        Path historyFile = temporaryFolder.resolve("command-history.txt");
        CommandHistory commandHistory = new CommandHistory(historyFile);

        commandHistory.add("list");
        commandHistory.add("find Alex");

        assertEquals(String.join(System.lineSeparator(), "list", "find Alex", ""),
                Files.readString(historyFile));
        assertEquals("find Alex", commandHistory.getPreviousCommand().orElseThrow());
        assertEquals("list", commandHistory.getPreviousCommand().orElseThrow());
        assertTrue(commandHistory.getPreviousCommand().isEmpty());
        assertEquals("find Alex", commandHistory.getNextCommand().orElseThrow());
        assertTrue(commandHistory.getNextCommand().isEmpty());
    }

    @Test
    public void add_afterNavigating_resetsPositionToNewestCommand() throws Exception {
        CommandHistory commandHistory = new CommandHistory(temporaryFolder.resolve("command-history.txt"));
        commandHistory.add("list");
        commandHistory.add("find Alex");
        commandHistory.getPreviousCommand();
        commandHistory.getPreviousCommand();

        commandHistory.add("view 1");

        assertEquals("view 1", commandHistory.getPreviousCommand().orElseThrow());
    }
}
