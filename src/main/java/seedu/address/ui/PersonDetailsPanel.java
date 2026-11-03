package seedu.address.ui;

import java.util.List;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import seedu.address.model.person.Person;

/**
 * Displays the contact requested by the view command independently of command feedback.
 */
public class PersonDetailsPanel extends UiPart<Region> {
    private static final String FXML = "PersonDetailsPanel.fxml";

    private Person displayedPerson;

    @FXML
    private Label name;

    @FXML
    private GridPane fields;

    /**
     * Creates an empty details panel.
     */
    public PersonDetailsPanel() {
        super(FXML);
    }

    /**
     * Displays the given contact, with wrapping values and alphabetically sorted tags.
     */
    public void showPerson(Person person) {
        displayedPerson = person;
        name.setText(person.getName().toString());
        fields.getChildren().clear();
        addField("Phone", person.getPhone().toString());
        addField("Email", person.getEmail().toString());
        addField("Address", person.getAddress().toString());
        addField("Note", person.getNote().toString());
        addField("Tags", person.getTags().stream().map(tag -> tag.tagName).sorted()
                .collect(Collectors.joining(", ")));
    }

    /**
     * Clears details when the displayed contact was changed or is no longer in the current list.
     */
    public void clearIfMissing(List<Person> persons) {
        if (displayedPerson != null && !persons.contains(displayedPerson)) {
            displayedPerson = null;
            name.setText("Use view INDEX to show contact details.");
            fields.getChildren().clear();
        }
    }

    private void addField(String title, String text) {
        Label caption = new Label(title);
        caption.getStyleClass().add("label-bright");
        Label value = new Label(text.isEmpty() ? "Not specified" : text);
        value.getStyleClass().add("label-bright");
        value.setWrapText(true);
        value.setMinWidth(0);
        value.setMaxWidth(Double.MAX_VALUE);
        value.setMinHeight(Region.USE_PREF_SIZE);
        GridPane.setValignment(caption, VPos.TOP);
        GridPane.setValignment(value, VPos.TOP);
        fields.addRow(fields.getRowCount(), caption, value);
    }
}
