package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
        assertEquals("find Alex", commandHistory.getPreviousCommand("draft").orElseThrow());
        assertEquals("list", commandHistory.getPreviousCommand("find Alex").orElseThrow());
        assertTrue(commandHistory.getPreviousCommand("list").isEmpty());
        assertEquals("find Alex", commandHistory.getNextCommand().orElseThrow());
        assertEquals("draft", commandHistory.getNextCommand().orElseThrow());
        assertTrue(commandHistory.getNextCommand().isEmpty());
    }

    @Test
    public void add_nullCommand_throwsNullPointerException() throws Exception {
        CommandHistory commandHistory = new CommandHistory(temporaryFolder.resolve("command-history.txt"));

        assertThrows(NullPointerException.class, () -> commandHistory.add(null));
    }

    @Test
    public void getPreviousCommand_nullCurrentCommandText_throwsNullPointerException() throws Exception {
        CommandHistory commandHistory = new CommandHistory(temporaryFolder.resolve("command-history.txt"));

        assertThrows(NullPointerException.class, () -> commandHistory.getPreviousCommand(null));
    }

    @Test
    public void getPreviousCommand_afterDraftRestored_savesLatestDraft() throws Exception {
        CommandHistory commandHistory = new CommandHistory(temporaryFolder.resolve("command-history.txt"));
        commandHistory.add("list");
        commandHistory.getPreviousCommand("initial draft");
        assertEquals("initial draft", commandHistory.getNextCommand().orElseThrow());

        assertEquals("list", commandHistory.getPreviousCommand("edited draft").orElseThrow());

        assertEquals("edited draft", commandHistory.getNextCommand().orElseThrow());
    }

    @Test
    public void getPreviousCommand_invalidCommandIndex_throwsAssertionError() throws Exception {
        CommandHistory commandHistory = new CommandHistory(temporaryFolder.resolve("command-history.txt"));

        setNextCommandIndex(commandHistory, -1);
        assertThrows(AssertionError.class, () -> commandHistory.getPreviousCommand("draft"));

        setNextCommandIndex(commandHistory, 1);
        assertThrows(AssertionError.class, () -> commandHistory.getPreviousCommand("draft"));
    }

    @Test
    public void getNextCommand_invalidCommandIndex_throwsAssertionError() throws Exception {
        CommandHistory commandHistory = new CommandHistory(temporaryFolder.resolve("command-history.txt"));

        setNextCommandIndex(commandHistory, -1);
        assertThrows(AssertionError.class, commandHistory::getNextCommand);

        setNextCommandIndex(commandHistory, 1);
        assertThrows(AssertionError.class, commandHistory::getNextCommand);
    }

    @Test
    public void add_afterNavigating_resetsPositionToNewestCommand() throws Exception {
        CommandHistory commandHistory = new CommandHistory(temporaryFolder.resolve("command-history.txt"));
        commandHistory.add("list");
        commandHistory.add("find Alex");
        commandHistory.getPreviousCommand("");
        commandHistory.getPreviousCommand("find Alex");

        commandHistory.add("view 1");

        assertEquals("view 1", commandHistory.getPreviousCommand("").orElseThrow());
    }

    @Test
    public void create_fileInitializationFails_usesInMemoryHistory() throws Exception {
        CommandHistory commandHistory = CommandHistory.create(() -> {
            throw new IOException("Unable to create temporary file");
        });

        commandHistory.add("list");

        assertEquals("list", commandHistory.getPreviousCommand("").orElseThrow());
    }

    @Test
    public void add_fileAppendFails_retainsCommandInMemory() throws Exception {
        Path historyFile = temporaryFolder.resolve("command-history.txt");
        CommandHistory commandHistory = new CommandHistory(historyFile);
        commandHistory.add("list");
        commandHistory.getPreviousCommand("");
        Files.delete(historyFile);

        assertThrows(IOException.class, () -> commandHistory.add("find Alex"));

        assertEquals("find Alex", commandHistory.getPreviousCommand("").orElseThrow());
    }

    private static void setNextCommandIndex(CommandHistory commandHistory, int nextCommandIndex) throws Exception {
        var nextCommandIndexField = CommandHistory.class.getDeclaredField("nextCommandIndex");
        nextCommandIndexField.setAccessible(true);
        nextCommandIndexField.setInt(commandHistory, nextCommandIndex);
    }
}
