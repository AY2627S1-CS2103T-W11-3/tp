package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CommandHistoryTest {

    private static final int ABNORMAL_EXIT_CODE = 1;
    private static final int PROCESS_TIMEOUT_SECONDS = 10;
    private static final String HISTORY_FILE_PREFIX = "addressbook-command-history-";
    private static final String SENSITIVE_COMMAND = "add n/Alice p/91234567 e/alice@example.com a/NUS";

    @TempDir
    private Path temporaryFolder;

    @Test
    public void add_multipleCommands_storesCommandsAndNavigatesInBothDirections() {
        CommandHistory commandHistory = new CommandHistory();

        commandHistory.add("list");
        commandHistory.add("find Alex");

        assertEquals("find Alex", commandHistory.getPreviousCommand("draft").orElseThrow());
        assertEquals("list", commandHistory.getPreviousCommand("find Alex").orElseThrow());
        assertTrue(commandHistory.getPreviousCommand("list").isEmpty());
        assertEquals("find Alex", commandHistory.getNextCommand().orElseThrow());
        assertEquals("draft", commandHistory.getNextCommand().orElseThrow());
        assertTrue(commandHistory.getNextCommand().isEmpty());
    }

    @Test
    public void add_nullCommand_throwsNullPointerException() {
        CommandHistory commandHistory = new CommandHistory();

        assertThrows(NullPointerException.class, () -> commandHistory.add(null));
    }

    @Test
    public void getPreviousCommand_nullCurrentCommandText_throwsNullPointerException() {
        CommandHistory commandHistory = new CommandHistory();

        assertThrows(NullPointerException.class, () -> commandHistory.getPreviousCommand(null));
    }

    @Test
    public void getPreviousCommand_afterDraftRestored_savesLatestDraft() {
        CommandHistory commandHistory = new CommandHistory();
        commandHistory.add("list");
        commandHistory.getPreviousCommand("initial draft");
        assertEquals("initial draft", commandHistory.getNextCommand().orElseThrow());

        assertEquals("list", commandHistory.getPreviousCommand("edited draft").orElseThrow());

        assertEquals("edited draft", commandHistory.getNextCommand().orElseThrow());
    }

    @Test
    public void getPreviousCommand_invalidCommandIndex_throwsAssertionError() throws Exception {
        CommandHistory commandHistory = new CommandHistory();

        setNextCommandIndex(commandHistory, -1);
        assertThrows(AssertionError.class, () -> commandHistory.getPreviousCommand("draft"));

        setNextCommandIndex(commandHistory, 1);
        assertThrows(AssertionError.class, () -> commandHistory.getPreviousCommand("draft"));
    }

    @Test
    public void getNextCommand_invalidCommandIndex_throwsAssertionError() throws Exception {
        CommandHistory commandHistory = new CommandHistory();

        setNextCommandIndex(commandHistory, -1);
        assertThrows(AssertionError.class, commandHistory::getNextCommand);

        setNextCommandIndex(commandHistory, 1);
        assertThrows(AssertionError.class, commandHistory::getNextCommand);
    }

    @Test
    public void add_afterNavigating_resetsPositionToNewestCommand() {
        CommandHistory commandHistory = new CommandHistory();
        commandHistory.add("list");
        commandHistory.add("find Alex");
        commandHistory.getPreviousCommand("");
        commandHistory.getPreviousCommand("find Alex");

        commandHistory.add("view 1");

        assertEquals("view 1", commandHistory.getPreviousCommand("").orElseThrow());
    }

    @Test
    public void add_abnormalExit_leavesNoReadableHistoryFile() throws Exception {
        Path isolatedTemporaryFolder = Files.createDirectory(temporaryFolder.resolve("process-temp"));
        Process process = startAbnormallyExitingHistoryProcess(isolatedTemporaryFolder);

        assertProcessExitedAbnormally(process);
        assertNoReadableCommandHistory(isolatedTemporaryFolder);
    }

    private static void assertProcessExitedAbnormally(Process process) throws Exception {
        boolean didTerminate = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!didTerminate) {
            process.destroyForcibly();
        }
        assertTrue(didTerminate, "Command history test process did not terminate");

        String processOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(ABNORMAL_EXIT_CODE, process.exitValue(), processOutput);
    }

    private static void assertNoReadableCommandHistory(Path temporaryFolder) throws Exception {
        try (Stream<Path> paths = Files.walk(temporaryFolder)) {
            List<Path> files = paths.filter(Files::isRegularFile).toList();
            assertTrue(files.stream().noneMatch(CommandHistoryTest::isCommandHistoryFile));
            for (Path file : files) {
                String fileContents = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                assertFalse(fileContents.contains(SENSITIVE_COMMAND));
            }
        }
    }

    private static Process startAbnormallyExitingHistoryProcess(Path temporaryFolder) throws Exception {
        return new ProcessBuilder(
                getJavaExecutable().toString(),
                "-Djava.io.tmpdir=" + temporaryFolder.toAbsolutePath(),
                "-cp",
                getSubprocessClasspath(),
                CommandHistoryAbnormalExitTestApp.class.getName(),
                SENSITIVE_COMMAND)
                .redirectErrorStream(true)
                .start();
    }

    private static Path getJavaExecutable() {
        String executableName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executableName);
    }

    private static String getSubprocessClasspath() throws Exception {
        String pathSeparator = System.getProperty("path.separator");
        String mainClasses = Path.of(CommandHistory.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .toString();
        String testClasses = Path.of(CommandHistoryAbnormalExitTestApp.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()).toString();
        return String.join(pathSeparator, mainClasses, testClasses);
    }

    private static boolean isCommandHistoryFile(Path path) {
        return path.getFileName().toString().startsWith(HISTORY_FILE_PREFIX);
    }

    private static void setNextCommandIndex(CommandHistory commandHistory, int nextCommandIndex) throws Exception {
        var nextCommandIndexField = CommandHistory.class.getDeclaredField("nextCommandIndex");
        nextCommandIndexField.setAccessible(true);
        nextCommandIndexField.setInt(commandHistory, nextCommandIndex);
    }
}
