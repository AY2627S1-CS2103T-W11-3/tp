package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;

/** Tests deletion UI behavior on machines with a graphical display. */
public class DeleteUiTest {
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
    public void list_emptyPlaceholderAndChangedCellIndex_showCorrectText() throws Exception {
        runOnFxThread(() -> {
            PersonListPanel panel = new PersonListPanel(FXCollections.observableArrayList(ALICE));
            ListView<?> list = (ListView<?>) panel.getRoot().lookup("#personListView");
            assertEquals("No contacts to display.", ((Label) list.getPlaceholder()).getText());
            PersonListPanel.PersonListViewCell cell = panel.new PersonListViewCell();
            cell.updateListView(new ListView<>(FXCollections.observableArrayList(ALICE, ALICE, ALICE)));
            cell.updateIndex(2);
            assertEquals("3. ", ((Label) cell.getGraphic().lookup("#id")).getText());
            cell.updateIndex(1);
            assertEquals("2. ", ((Label) cell.getGraphic().lookup("#id")).getText());
            cell.updateItem(null, true);
            assertEquals(null, cell.getGraphic());
        });
    }

    @Test
    public void command_successClearsInputAndFailuresRetainInput() throws Exception {
        runOnFxThread(() -> {
            CommandBox success = new CommandBox(text -> new CommandResult("Deleted contact: Alice (index 1)."));
            TextField successInput = (TextField) success.getRoot().lookup("#commandTextField");
            successInput.setText("delete 1");
            successInput.fireEvent(new ActionEvent());
            assertEquals("", successInput.getText());
            for (boolean parseFailure : new boolean[] {true, false}) {
                CommandBox failure = new CommandBox(text -> {
                    if (parseFailure) {
                        throw new ParseException("Invalid index");
                    }
                    throw new CommandException("Could not save changes");
                });
                TextField input = (TextField) failure.getRoot().lookup("#commandTextField");
                input.setText("delete 1");
                input.fireEvent(new ActionEvent());
                assertEquals("delete 1", input.getText());
                assertTrue(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
            }
        });
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}
