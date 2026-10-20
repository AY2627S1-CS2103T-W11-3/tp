package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.CARL;
import static seedu.address.testutil.TypicalPersons.DANIEL;
import static seedu.address.testutil.TypicalPersons.ELLE;
import static seedu.address.testutil.TypicalPersons.FIONA;
import static seedu.address.testutil.TypicalPersons.GEORGE;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.EditCommand.EditPersonDescriptor;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonContainsKeywordsPredicate;
import seedu.address.testutil.EditPersonDescriptorBuilder;
import seedu.address.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) for RecentCommand.
 */
public class RecentCommandTest {

    private final RecentCommand recentCommand = new RecentCommand();

    @Test
    public void execute_multipleContacts_showsNewestAddedFirst() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        AddressBook original = new AddressBook(model.getAddressBook());

        CommandResult result = recentCommand.execute(model);

        assertEquals(RecentCommand.MESSAGE_SUCCESS, result.getFeedbackToUser());
        assertEquals(List.of(GEORGE, FIONA, ELLE, DANIEL, CARL, BENSON, ALICE),
                model.getFilteredPersonList());
        assertEquals(original, model.getAddressBook());
    }

    @Test
    public void execute_filteredList_showsAllContactsInReverseAdditionOrder() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.updateFilteredPersonList(new PersonContainsKeywordsPredicate(List.of("Alice")));

        recentCommand.execute(model);

        assertEquals(List.of(GEORGE, FIONA, ELLE, DANIEL, CARL, BENSON, ALICE),
                model.getFilteredPersonList());
    }

    @Test
    public void execute_afterEditingContact_doesNotMoveEditedContactToTop() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        Person editedAlice = new PersonBuilder(ALICE).withPhone("91234567").build();
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withPhone("91234567").build();
        recentCommand.execute(model);
        new EditCommand(Index.fromOneBased(7), descriptor).execute(model);

        recentCommand.execute(model);

        assertEquals(List.of(GEORGE, FIONA, ELLE, DANIEL, CARL, BENSON, editedAlice),
                model.getFilteredPersonList());
    }

    @Test
    public void execute_afterDeletingContact_omitsDeletedContact() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        recentCommand.execute(model);
        new DeleteCommand(Index.fromOneBased(1)).execute(model);

        recentCommand.execute(model);

        assertEquals(List.of(FIONA, ELLE, DANIEL, CARL, BENSON, ALICE), model.getFilteredPersonList());
    }

    @Test
    public void execute_emptyAddressBook_showsNoContactsMessage() throws Exception {
        Model model = new ModelManager();

        CommandResult result = recentCommand.execute(model);

        assertEquals(RecentCommand.MESSAGE_NO_CONTACTS, result.getFeedbackToUser());
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void execute_listAfterRecent_restoresAdditionOrder() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        recentCommand.execute(model);

        new ListCommand().execute(model);

        assertEquals(List.of(ALICE, BENSON, CARL, DANIEL, ELLE, FIONA, GEORGE),
                model.getFilteredPersonList());
    }

    @Test
    public void execute_findAfterRecent_showsMatchesInAdditionOrder() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        recentCommand.execute(model);

        new FindCommand(new PersonContainsKeywordsPredicate(List.of("Meier"))).execute(model);

        assertEquals(List.of(BENSON, DANIEL), model.getFilteredPersonList());
    }
}
