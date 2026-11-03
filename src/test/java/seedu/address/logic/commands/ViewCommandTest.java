package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.logic.commands.CommandTestUtil.showPersonAtIndex;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static seedu.address.testutil.TypicalIndexes.INDEX_THIRD_PERSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for
 * {@code ViewCommand}.
 */
public class ViewCommandTest {

    private final Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void execute_validIndexUnfilteredList_success() {
        Person personToView = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        ViewCommand viewCommand = new ViewCommand(INDEX_FIRST_PERSON);

        String expectedMessage = "Viewed person: Alice Pauline";

        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());

        assertCommandSuccess(viewCommand, model, CommandResult.withHighlight(expectedMessage,
                INDEX_FIRST_PERSON.getZeroBased(),
                model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased())), expectedModel);
        assertEquals(personToView, model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased()));
    }

    @Test
    public void execute_personWithMultipleTags_success() {
        Person personToView = model.getFilteredPersonList().get(INDEX_SECOND_PERSON.getZeroBased());
        ViewCommand viewCommand = new ViewCommand(INDEX_SECOND_PERSON);

        String expectedMessage = "Viewed person: Benson Meier";

        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());

        assertCommandSuccess(viewCommand, model, CommandResult.withHighlight(expectedMessage,
                INDEX_SECOND_PERSON.getZeroBased(),
                model.getFilteredPersonList().get(INDEX_SECOND_PERSON.getZeroBased())), expectedModel);
        assertEquals(personToView, model.getFilteredPersonList().get(INDEX_SECOND_PERSON.getZeroBased()));
    }

    @Test
    public void execute_personWithNoTags_success() {
        ViewCommand viewCommand = new ViewCommand(INDEX_THIRD_PERSON);

        String expectedMessage = "Viewed person: Carl Kurz";

        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());

        assertCommandSuccess(viewCommand, model, CommandResult.withHighlight(expectedMessage,
                INDEX_THIRD_PERSON.getZeroBased(),
                model.getFilteredPersonList().get(INDEX_THIRD_PERSON.getZeroBased())), expectedModel);
    }

    @Test
    public void execute_personWithNote_success() {
        Person personWithNote = new PersonBuilder().withNote("Known through a dating app").build();
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(personWithNote);
        Model modelWithNote = new ModelManager(addressBook, new UserPrefs());

        String expectedMessage = "Viewed person: Amy Bee";

        assertCommandSuccess(new ViewCommand(INDEX_FIRST_PERSON), modelWithNote,
                CommandResult.withHighlight(expectedMessage, 0, personWithNote), modelWithNote);
    }

    @Test
    public void execute_invalidIndexUnfilteredList_throwsCommandException() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredPersonList().size() + 1);
        ViewCommand viewCommand = new ViewCommand(outOfBoundIndex);

        assertCommandFailure(viewCommand, model, ViewCommand.MESSAGE_INDEX_OUT_OF_RANGE);
    }

    @Test
    public void execute_validIndexFilteredList_success() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        ViewCommand viewCommand = new ViewCommand(INDEX_FIRST_PERSON);

        String expectedMessage = "Viewed person: Alice Pauline";

        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        showPersonAtIndex(expectedModel, INDEX_FIRST_PERSON);

        assertCommandSuccess(viewCommand, model, CommandResult.withHighlight(expectedMessage,
                INDEX_FIRST_PERSON.getZeroBased(),
                model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased())), expectedModel);
    }

    @Test
    public void execute_validIndex_requestsHighlightOfThatRow() throws Exception {
        CommandResult result = new ViewCommand(INDEX_SECOND_PERSON).execute(model);

        assertEquals(1, result.getHighlightedIndex().getAsInt());
    }

    @Test
    public void execute_invalidIndexFilteredList_throwsCommandException() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        Index outOfBoundIndex = INDEX_SECOND_PERSON;
        // ensures that outOfBoundIndex is still in bounds of address book list
        assertTrue(outOfBoundIndex.getZeroBased() < model.getAddressBook().getPersonList().size());

        ViewCommand viewCommand = new ViewCommand(outOfBoundIndex);

        assertCommandFailure(viewCommand, model, ViewCommand.MESSAGE_INDEX_OUT_OF_RANGE);
    }

    @Test
    public void execute_indexBeyondIntegerMax_throwsCommandException() {
        ViewCommand viewCommand = new ViewCommand(new BigInteger("2147483648"));

        assertCommandFailure(viewCommand, model, ViewCommand.MESSAGE_INDEX_OUT_OF_RANGE);
    }

    @Test
    public void execute_emptyList_throwsCommandException() {
        Model emptyModel = new ModelManager();
        ViewCommand viewCommand = new ViewCommand(INDEX_FIRST_PERSON);

        assertCommandFailure(viewCommand, emptyModel, ViewCommand.MESSAGE_NO_CONTACTS);
    }

    @Test
    public void equals() {
        ViewCommand viewFirstCommand = new ViewCommand(INDEX_FIRST_PERSON);
        ViewCommand viewSecondCommand = new ViewCommand(INDEX_SECOND_PERSON);

        // same object -> returns true
        assertTrue(viewFirstCommand.equals(viewFirstCommand));

        // same values -> returns true
        ViewCommand viewFirstCommandCopy = new ViewCommand(INDEX_FIRST_PERSON);
        assertTrue(viewFirstCommand.equals(viewFirstCommandCopy));

        // different types -> returns false
        assertFalse(viewFirstCommand.equals(1));

        // null -> returns false
        assertFalse(viewFirstCommand.equals(null));

        // different person -> returns false
        assertFalse(viewFirstCommand.equals(viewSecondCommand));
    }

    @Test
    public void toStringMethod() {
        Index targetIndex = Index.fromOneBased(1);
        ViewCommand viewCommand = new ViewCommand(targetIndex);
        String expected = ViewCommand.class.getCanonicalName() + "{targetIndex=" + targetIndex.getOneBased() + "}";
        assertEquals(expected, viewCommand.toString());
    }
}
