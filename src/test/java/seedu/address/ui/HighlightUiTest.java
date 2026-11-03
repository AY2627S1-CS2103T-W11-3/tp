package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.CARL;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ListView;

/** Tests that the contact list can highlight a contact on machines with a graphical display. */
public class HighlightUiTest {
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
    public void highlight_selectsRequestedRow() throws Exception {
        runOnFxThread(() -> {
            PersonListPanel panel = new PersonListPanel(FXCollections.observableArrayList(ALICE, BENSON, CARL));
            ListView<?> list = (ListView<?>) panel.getRoot().lookup("#personListView");

            panel.highlight(1);

            assertEquals(1, list.getSelectionModel().getSelectedIndex());
        });
    }

    @Test
    public void highlight_replacesEarlierHighlight() throws Exception {
        runOnFxThread(() -> {
            PersonListPanel panel = new PersonListPanel(FXCollections.observableArrayList(ALICE, BENSON, CARL));
            ListView<?> list = (ListView<?>) panel.getRoot().lookup("#personListView");

            panel.highlight(0);
            panel.highlight(2);

            assertEquals(2, list.getSelectionModel().getSelectedIndex());
        });
    }

    @Test
    public void clearHighlight_removesSelection() throws Exception {
        runOnFxThread(() -> {
            PersonListPanel panel = new PersonListPanel(FXCollections.observableArrayList(ALICE, BENSON, CARL));
            ListView<?> list = (ListView<?>) panel.getRoot().lookup("#personListView");
            panel.highlight(1);

            panel.clearHighlight();

            assertEquals(-1, list.getSelectionModel().getSelectedIndex());
        });
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}
