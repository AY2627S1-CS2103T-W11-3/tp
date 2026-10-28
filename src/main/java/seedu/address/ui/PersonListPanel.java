package seedu.address.ui;

import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import seedu.address.commons.core.LogsCenter;
import seedu.address.model.person.Person;

/**
 * Panel containing the list of persons.
 */
public class PersonListPanel extends UiPart<Region> {
    private static final String FXML = "PersonListPanel.fxml";
    private final Logger logger = LogsCenter.getLogger(PersonListPanel.class);
    private Predicate<Person> isFavourite = person -> false;

    @FXML
    private ListView<Person> personListView;

    /**
     * Creates a {@code PersonListPanel} with the given {@code ObservableList}.
     */
    public PersonListPanel(ObservableList<Person> personList) {
        super(FXML);
        personListView.setPlaceholder(new Label("No contacts to display."));
        personListView.setItems(personList);
        personListView.setCellFactory(listView -> new PersonListViewCell());
    }

    /**
     * Displays the given contacts and refreshes their favorite indicators, including unchanged cards.
     * The predicate determines which contacts display a star; this panel does not check collection membership.
     *
     * @param persons The observable contacts to display in their current order.
     * @param isFavourite The predicate that returns true for contacts whose cards should display a star.
     */
    public void setPersonList(ObservableList<Person> persons, Predicate<Person> isFavourite) {
        this.isFavourite = isFavourite;
        personListView.setItems(persons);
        personListView.refresh();
    }

    /**
     * Custom {@code ListCell} that displays the graphics of a {@code Person} using a {@code PersonCard}.
     */
    class PersonListViewCell extends ListCell<Person> {
        PersonListViewCell() {
            indexProperty().addListener((observable, oldIndex, newIndex) -> updateItem(getItem(), isEmpty()));
        }

        @Override
        protected void updateItem(Person person, boolean empty) {
            super.updateItem(person, empty);

            if (empty || person == null) {
                setGraphic(null);
                setText(null);
            } else {
                setGraphic(new PersonCard(person, getIndex() + 1, isFavourite.test(person)).getRoot());
            }
        }
    }

}
