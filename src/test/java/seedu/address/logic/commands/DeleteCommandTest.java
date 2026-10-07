package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.logic.commands.CommandTestUtil.showPersonAtIndex;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonContainsKeywordsPredicate;

/**
 * Contains integration tests (interaction with the Model) and unit tests for
 * {@code DeleteCommand}.
 */
public class DeleteCommandTest {

    private Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void execute_validIndexUnfilteredList_success() {
        Person personToDelete = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        DeleteCommand deleteCommand = new DeleteCommand(INDEX_FIRST_PERSON);

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_PERSON_SUCCESS,
                personToDelete.getName().fullName, INDEX_FIRST_PERSON.getOneBased());

        ModelManager expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.deletePerson(personToDelete);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_invalidIndexUnfilteredList_throwsCommandException() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredPersonList().size() + 1);
        DeleteCommand deleteCommand = new DeleteCommand(outOfBoundIndex);

        assertCommandFailure(deleteCommand, model, String.format(DeleteCommand.MESSAGE_INDEX_OUT_OF_RANGE,
                model.getFilteredPersonList().size()));
    }

    @Test
    public void execute_validIndexFilteredList_success() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        Person personToDelete = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        DeleteCommand deleteCommand = new DeleteCommand(INDEX_FIRST_PERSON);

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_PERSON_SUCCESS,
                personToDelete.getName().fullName, INDEX_FIRST_PERSON.getOneBased());

        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.deletePerson(personToDelete);
        showNoPerson(expectedModel);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_invalidIndexFilteredList_throwsCommandException() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        Index outOfBoundIndex = INDEX_SECOND_PERSON;
        // ensures that outOfBoundIndex is still in bounds of address book list
        assertTrue(outOfBoundIndex.getZeroBased() < model.getAddressBook().getPersonList().size());

        DeleteCommand deleteCommand = new DeleteCommand(outOfBoundIndex);

        assertCommandFailure(deleteCommand, model, String.format(DeleteCommand.MESSAGE_INDEX_OUT_OF_RANGE,
                model.getFilteredPersonList().size()));
    }

    @Test
    public void execute_firstMiddleLastIndex_preservesOtherContactsAndOrder() throws Exception {
        for (int oneBasedIndex : new int[] {1, 4, 7}) {
            Model testModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());
            List<Person> expectedContacts = new ArrayList<>(testModel.getAddressBook().getPersonList());
            Person deleted = expectedContacts.remove(oneBasedIndex - 1);

            CommandResult result = new DeleteCommand(Index.fromOneBased(oneBasedIndex)).execute(testModel);

            assertEquals("Deleted contact: " + deleted.getName().fullName + " (index " + oneBasedIndex + ").",
                    result.getFeedbackToUser());
            assertEquals(expectedContacts, testModel.getAddressBook().getPersonList());
            assertEquals(expectedContacts, testModel.getFilteredPersonList());
        }
    }

    @Test
    public void execute_filteredIndex_preservesFilterAndContactsOutsideResults() throws Exception {
        model.updateFilteredPersonList(new PersonContainsKeywordsPredicate(List.of("Meier")));
        List<Person> expectedContacts = new ArrayList<>(model.getAddressBook().getPersonList());
        List<Person> expectedResults = new ArrayList<>(model.getFilteredPersonList());
        Person deleted = expectedResults.remove(1);
        expectedContacts.remove(deleted);

        CommandResult result = new DeleteCommand(INDEX_SECOND_PERSON).execute(model);

        assertEquals("Deleted contact: Daniel Meier (index 2).", result.getFeedbackToUser());
        assertEquals(expectedContacts, model.getAddressBook().getPersonList());
        assertEquals(expectedResults, model.getFilteredPersonList());
    }

    @Test
    public void execute_repeatedIndex_usesUpdatedListThenReportsOutOfRange() throws Exception {
        model.updateFilteredPersonList(new PersonContainsKeywordsPredicate(List.of("Meier")));
        DeleteCommand deleteFirst = new DeleteCommand(INDEX_FIRST_PERSON);
        DeleteCommand deleteSecond = new DeleteCommand(INDEX_SECOND_PERSON);
        List<Person> expectedContacts = new ArrayList<>(model.getAddressBook().getPersonList());
        expectedContacts.removeAll(new ArrayList<>(model.getFilteredPersonList()));

        assertEquals("Deleted contact: Benson Meier (index 1).", deleteFirst.execute(model).getFeedbackToUser());
        assertCommandFailure(deleteSecond, model, "Contact index out of range. Choose an index from 1 to 1.");
        assertEquals("Deleted contact: Daniel Meier (index 1).", deleteFirst.execute(model).getFeedbackToUser());
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(expectedContacts, model.getAddressBook().getPersonList());
        assertCommandFailure(deleteFirst, model, DeleteCommand.MESSAGE_EMPTY_LIST);
    }

    @Test
    public void equals() {
        DeleteCommand deleteFirstCommand = new DeleteCommand(INDEX_FIRST_PERSON);
        DeleteCommand deleteSecondCommand = new DeleteCommand(INDEX_SECOND_PERSON);

        // same object -> returns true
        assertTrue(deleteFirstCommand.equals(deleteFirstCommand));

        // same values -> returns true
        DeleteCommand deleteFirstCommandCopy = new DeleteCommand(INDEX_FIRST_PERSON);
        assertTrue(deleteFirstCommand.equals(deleteFirstCommandCopy));

        // different types -> returns false
        assertFalse(deleteFirstCommand.equals(1));

        // null -> returns false
        assertFalse(deleteFirstCommand.equals(null));

        // different person -> returns false
        assertFalse(deleteFirstCommand.equals(deleteSecondCommand));
    }

    @Test
    public void toStringMethod() {
        Index targetIndex = Index.fromOneBased(1);
        DeleteCommand deleteCommand = new DeleteCommand(targetIndex);
        String expected = DeleteCommand.class.getCanonicalName() + "{targetIndex=" + targetIndex.getOneBased() + "}";
        assertEquals(expected, deleteCommand.toString());
    }

    /**
     * Updates {@code model}'s filtered list to show no one.
     */
    private void showNoPerson(Model model) {
        model.updateFilteredPersonList(p -> false);

        assertTrue(model.getFilteredPersonList().isEmpty());
    }
}
