package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Tests deletion UI behavior on machines with a graphical display. */
public class DeleteUiTest {
    @TempDir
    public Path temporaryFolder;

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

    @Test
    public void command_saveFailure_preservesViewedDetailsAndRetryClearsError() throws Exception {
        runOnFxThread(() -> {
            AtomicBoolean failSave = new AtomicBoolean(true);
            Path dataPath = temporaryFolder.resolve("contacts.json");
            JsonAddressBookStorage storage = new JsonAddressBookStorage(dataPath) {
                @Override
                public void saveAddressBook(ReadOnlyAddressBook data) throws IOException {
                    if (failSave.get()) {
                        throw new IOException("Simulated save failure");
                    }
                    super.saveAddressBook(data);
                }
            };
            ModelManager model = new ModelManager();
            model.addPerson(ALICE);
            LogicManager logic = new LogicManager(model, new StorageManager(storage,
                    new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))));
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, dataPath);
                window.fillInnerParts();
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea details = (TextArea) stage.getScene().lookup("#resultDisplay");
                Label error = (Label) stage.getScene().lookup("#commandError");
                input.setText("view 1");
                input.fireEvent(new ActionEvent());
                String viewedDetails = details.getText();
                assertTrue(viewedDetails.contains(ALICE.getName().fullName));
                for (String command : List.of("delete 1", "edit 1 note/Updated note", "clear")) {
                    input.setText(command);
                    input.fireEvent(new ActionEvent());
                    assertEquals(viewedDetails, details.getText());
                    assertEquals(command, input.getText());
                    assertEquals(List.of(ALICE), model.getFilteredPersonList());
                    assertEquals(command.startsWith("delete")
                            ? DeleteCommand.MESSAGE_SAVE_ERROR : LogicManager.MESSAGE_SAVE_ERROR, error.getText());
                    assertTrue(error.isVisible());
                    assertTrue(error.isManaged());
                }
                failSave.set(false);
                input.setText("delete 1");
                input.fireEvent(new ActionEvent());
                assertEquals("", input.getText());
                assertTrue(model.getFilteredPersonList().isEmpty());
                assertTrue(details.getText().startsWith("Deleted contact:"));
                assertEquals("", error.getText());
                assertFalse(error.isVisible());
                assertFalse(error.isManaged());
            } finally {
                stage.close();
            }
        });
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}
