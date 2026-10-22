package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import seedu.address.logic.commands.CommandResult;

/**
 * Tests Tab autocomplete behaviour in {@code CommandBox} on machines with a graphical display.
 */
public class CommandBoxAutocompleteUiTest {

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
    public void tab_completesUniqueCommandPrefix() throws Exception {
        runOnFxThread(() -> {
            CommandBox commandBox = new CommandBox(text -> new CommandResult("ok"));
            TextField input = (TextField) commandBox.getRoot().lookup("#commandTextField");
            input.setText("li");
            input.positionCaret(2);
            input.fireEvent(tabPressed());
            assertEquals("list", input.getText());
            assertEquals(4, input.getCaretPosition());
        });
    }

    @Test
    public void tab_cyclesSharedCommandPrefix() throws Exception {
        runOnFxThread(() -> {
            CommandBox commandBox = new CommandBox(text -> new CommandResult("ok"));
            TextField input = (TextField) commandBox.getRoot().lookup("#commandTextField");
            input.setText("fi");
            input.positionCaret(2);
            input.fireEvent(tabPressed());
            assertEquals("filter", input.getText());
            input.fireEvent(tabPressed());
            assertEquals("find", input.getText());
            input.fireEvent(tabPressed());
            assertEquals("filter", input.getText());
        });
    }

    @Test
    public void tab_inArguments_doesNotChangeText() throws Exception {
        runOnFxThread(() -> {
            CommandBox commandBox = new CommandBox(text -> new CommandResult("ok"));
            TextField input = (TextField) commandBox.getRoot().lookup("#commandTextField");
            input.setText("find alice");
            input.positionCaret(7);
            input.fireEvent(tabPressed());
            assertEquals("find alice", input.getText());
        });
    }

    private static KeyEvent tabPressed() {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB,
                false, false, false, false);
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}
