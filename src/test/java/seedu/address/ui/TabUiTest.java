package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import seedu.address.logic.LogicManager;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.AddressBookBuilder;

/** Exercises section switching through the real command box on machines with a display. */
public class TabUiTest {
    @TempDir
    public Path temporaryFolder;

    @BeforeAll
    public static void startToolkit() throws Exception {
        DeleteUiTest.startToolkit();
    }

    @Test
    public void command_switchSections_updatesListLabelPathAndInput() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager(new AddressBookBuilder().withPerson(ALICE).build(),
                    new AddressBookBuilder().withPerson(BOB).build(), new UserPrefs());
            StorageManager storage = new StorageManager(
                    new JsonAddressBookStorage(temporaryFolder.resolve("contacts.json")),
                    new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, new LogicManager(model, storage),
                        storage.getAddressBookFilePath());
                window.fillInnerParts();
                Parent root = stage.getScene().getRoot();
                TextField input = (TextField) root.lookup("#commandTextField");
                ListView<?> list = (ListView<?>) root.lookup("#personListView");
                Label section = (Label) root.lookup("#activeSectionLabel");
                Label path = (Label) root.lookup("#saveLocationStatus");

                enter(input, "tab favourites");
                assertEquals("", input.getText());
                assertEquals("Favourites", section.getText());
                assertEquals(List.of(BOB), list.getItems());
                assertTrue(path.getText().endsWith("favourites.json"));

                enter(input, "tab other");
                assertEquals("tab other", input.getText());
                assertEquals("Favourites", section.getText());
                assertEquals(List.of(BOB), list.getItems());
                assertTrue(path.getText().endsWith("favourites.json"));

                enter(input, "tab contacts");
                assertEquals("Contacts", section.getText());
                assertEquals(List.of(ALICE), list.getItems());
                assertTrue(path.getText().endsWith("contacts.json"));
            } finally {
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    private static void enter(TextField input, String command) {
        input.setText(command);
        input.fireEvent(new ActionEvent());
    }
}
