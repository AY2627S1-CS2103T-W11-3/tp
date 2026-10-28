package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) for {@code AddCommand}.
 */
public class AddCommandIntegrationTest {

    private Model model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
    }

    @Test
    public void execute_newPerson_success() {
        Person validPerson = new PersonBuilder().build();

        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.addPerson(validPerson);

        assertCommandSuccess(new AddCommand(validPerson), model,
                String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(validPerson)),
                expectedModel);
    }

    @Test
    public void execute_duplicatePerson_throwsCommandException() {
        Person personInList = model.getAddressBook().getPersonList().get(0);
        assertCommandFailure(new AddCommand(personInList), model,
                String.format(AddCommand.MESSAGE_DUPLICATE_PHONE_AND_EMAIL, personInList.getName()));
    }

    @Test
    public void execute_sameNameButDifferentPhoneAndEmail_success() {
        Person personWithSameName = new PersonBuilder().withName(ALICE.getName().fullName).build();
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.addPerson(personWithSameName);

        assertCommandSuccess(new AddCommand(personWithSameName), model,
                String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(personWithSameName)),
                expectedModel);
    }

    @Test
    public void execute_duplicatePhone_throwsCommandException() {
        Person personWithDuplicatePhone = new PersonBuilder().withPhone(ALICE.getPhone().value).build();
        int originalSize = model.getAddressBook().getPersonList().size();

        assertCommandFailure(new AddCommand(personWithDuplicatePhone), model,
                String.format(AddCommand.MESSAGE_DUPLICATE_PHONE, ALICE.getName()));
        assertEquals(originalSize, model.getAddressBook().getPersonList().size());
    }

    @Test
    public void execute_duplicateEmail_throwsCommandException() {
        Person personWithDuplicateEmail = new PersonBuilder().withEmail(ALICE.getEmail().value).build();

        assertCommandFailure(new AddCommand(personWithDuplicateEmail), model,
                String.format(AddCommand.MESSAGE_DUPLICATE_EMAIL, ALICE.getName()));
    }

    @Test
    public void execute_phoneAndEmailConflictWithDifferentPeople_reportsBothPeople() {
        Person personWithTwoConflicts = new PersonBuilder()
                .withPhone(ALICE.getPhone().value)
                .withEmail(BENSON.getEmail().value)
                .build();
        String expectedMessage = String.format(AddCommand.MESSAGE_DUPLICATE_PHONE, ALICE.getName())
                + "\n" + String.format(AddCommand.MESSAGE_DUPLICATE_EMAIL, BENSON.getName());

        assertCommandFailure(new AddCommand(personWithTwoConflicts), model, expectedMessage);
    }

    @Test
    public void execute_duplicateHiddenByFilter_reportsPersonFromCompleteAddressBook() {
        model.updateFilteredPersonList(person -> !person.equals(ALICE));
        assertFalse(model.getFilteredPersonList().contains(ALICE));
        Person personWithDuplicatePhone = new PersonBuilder().withPhone(ALICE.getPhone().value).build();

        assertCommandFailure(new AddCommand(personWithDuplicatePhone), model,
                String.format(AddCommand.MESSAGE_DUPLICATE_PHONE, ALICE.getName()));
    }

    @Test
    public void execute_peopleWithEmptyPhoneAndEmail_addSuccessful() throws ReflectiveOperationException {
        Person existingPerson = new PersonBuilder().withName("Existing Person")
                .withPhone("11111111").withEmail("existing@example.com").build();
        Person personToAdd = new PersonBuilder().withName("New Person")
                .withPhone("22222222").withEmail("new@example.com").build();
        setValueToEmpty(existingPerson.getPhone());
        setValueToEmpty(existingPerson.getEmail());
        setValueToEmpty(personToAdd.getPhone());
        setValueToEmpty(personToAdd.getEmail());
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(existingPerson);
        model = new ModelManager(addressBook, new UserPrefs());
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.addPerson(personToAdd);

        assertCommandSuccess(new AddCommand(personToAdd), model,
                String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(personToAdd)), expectedModel);
    }

    /**
     * Sets an identity field to an otherwise invalid empty value to exercise duplicate detection's defensive
     * empty-value checks without changing production validation rules.
     */
    private void setValueToEmpty(Object identityField) throws ReflectiveOperationException {
        Field valueField = identityField.getClass().getField("value");
        valueField.setAccessible(true);
        valueField.set(identityField, "");
    }

}
