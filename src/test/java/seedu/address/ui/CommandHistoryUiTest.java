package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.parser.exceptions.ParseException;

/** Tests command history interactions on machines with a graphical display. */
public class CommandHistoryUiTest {

    @TempDir
    private Path temporaryFolder;

    @BeforeAll
    public static void startToolkit() throws Exception {
        assumeTrue(!System.getProperty("os.name").toLowerCase().contains("linux")
                || System.getenv("DISPLAY") != null, "JavaFX UI tests require a display");
        FutureTask<Void> started = new FutureTask<>(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
        try {
            Platform.startup(started);
        } catch (IllegalStateException e) {
            Platform.runLater(started);
        }
        started.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void arrowKeysPressed_navigatesCommandHistory() throws Exception {
        CommandHistory commandHistory = new CommandHistory(temporaryFolder.resolve("command-history.txt"));
        runOnFxThread(() -> {
            CommandBox commandBox = new CommandBox(text -> new CommandResult("Success"), commandHistory);
            TextField input = (TextField) commandBox.getRoot().lookup("#commandTextField");
            enterCommand(input, "list");
            enterCommand(input, "find Alex");

            input.fireEvent(createArrowKeyEvent(KeyCode.UP));
            assertEquals("find Alex", input.getText());
            assertEquals(input.getText().length(), input.getCaretPosition());
            input.fireEvent(createArrowKeyEvent(KeyCode.UP));
            assertEquals("list", input.getText());
            input.fireEvent(createArrowKeyEvent(KeyCode.UP));
            assertEquals("list", input.getText());
            input.fireEvent(createArrowKeyEvent(KeyCode.DOWN));
            assertEquals("find Alex", input.getText());
            assertEquals(input.getText().length(), input.getCaretPosition());
            input.fireEvent(createArrowKeyEvent(KeyCode.DOWN));
            assertEquals("find Alex", input.getText());
        });
    }

    @Test
    public void failedCommandEntered_addsCommandToHistory() throws Exception {
        CommandHistory commandHistory = new CommandHistory(temporaryFolder.resolve("command-history.txt"));
        runOnFxThread(() -> {
            CommandBox commandBox = new CommandBox(text -> {
                throw new ParseException("Invalid command");
            }, commandHistory);
            TextField input = (TextField) commandBox.getRoot().lookup("#commandTextField");
            enterCommand(input, "invalid command");
            input.setText("");

            input.fireEvent(createArrowKeyEvent(KeyCode.UP));

            assertEquals("invalid command", input.getText());
        });
    }

    private static void enterCommand(TextField input, String command) {
        input.setText(command);
        input.fireEvent(new ActionEvent());
    }

    private static KeyEvent createArrowKeyEvent(KeyCode keyCode) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode, false, false, false, false);
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}
