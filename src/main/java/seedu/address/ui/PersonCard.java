package seedu.address.ui;

import java.util.Comparator;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import seedu.address.model.person.Person;

/**
 * A UI component that displays information of a {@code Person}.
 */
public class PersonCard extends UiPart<Region> {

    private static final String FXML = "PersonListCard.fxml";

    /**
     * Note: Certain keywords such as "location" and "resources" are reserved keywords in JavaFX.
     * As a consequence, UI elements' variable names cannot be set to such keywords
     * or an exception will be thrown by JavaFX during runtime.
     *
     * @see <a href="https://github.com/se-edu/addressbook-level4/issues/336">The issue on AddressBook level 4</a>
     */

    public final Person person;

    @FXML
    private HBox cardPane;
    @FXML
    private Label name;
    @FXML
    private Label favouriteStar;
    @FXML
    private Label id;
    @FXML
    private Label phone;
    @FXML
    private Label address;
    @FXML
    private Label email;
    @FXML
    private Label note;
    @FXML
    private FlowPane tags;

    /**
     * Creates a contact card showing its details, displayed index, and optional favorite star.
     * Hides the star and its layout space when the contact is not marked as a favorite.
     *
     * @param person The contact whose details are displayed.
     * @param displayedIndex The one-based index shown in the current contact list.
     * @param isFavourite Whether to display a star beside the contact's name.
     */
    public PersonCard(Person person, int displayedIndex, boolean isFavourite) {
        super(FXML);
        this.person = person;
        id.setText(displayedIndex + ". ");
        name.setText(person.getName().fullName);
        favouriteStar.setVisible(isFavourite);
        favouriteStar.setManaged(isFavourite);
        phone.setText(person.getPhone().value);
        address.setText(person.getAddress().value);
        email.setText(person.getEmail().value);
        note.setText("Note: " + person.getNote().value);
        note.setVisible(!person.getNote().isEmpty());
        note.setManaged(!person.getNote().isEmpty());
        person.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.tagName))
                .forEach(tag -> tags.getChildren().add(new Label(tag.tagName)));
    }
}
