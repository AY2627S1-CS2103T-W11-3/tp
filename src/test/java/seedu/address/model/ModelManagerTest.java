package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;

import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonContainsKeywordsPredicate;
import seedu.address.testutil.AddressBookBuilder;
import seedu.address.testutil.PersonBuilder;

public class ModelManagerTest {

    private ModelManager modelManager = new ModelManager();

    @Test
    public void constructor() {
        assertEquals(new UserPrefs(), modelManager.getUserPrefs());
        assertEquals(new GuiSettings(), modelManager.getGuiSettings());
        assertEquals(new AddressBook(), new AddressBook(modelManager.getAddressBook()));
    }

    @Test
    public void constructor_validUserPrefs_copiesUserPrefs() {
        UserPrefs userPrefs = new UserPrefs();
        userPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        modelManager = new ModelManager(new AddressBook(), userPrefs);
        assertEquals(userPrefs, modelManager.getUserPrefs());

        // Modifying userPrefs should not modify modelManager's userPrefs
        UserPrefs oldUserPrefs = new UserPrefs(userPrefs);
        userPrefs.setGuiSettings(new GuiSettings(5, 6, 7, 8));
        assertEquals(oldUserPrefs, modelManager.getUserPrefs());
    }

    @Test
    public void constructor_sharedInputCollection_createsIndependentCopies() {
        AddressBook original = new AddressBookBuilder().withPerson(ALICE).build();
        modelManager = new ModelManager(original, original, new UserPrefs());
        original.addPerson(BENSON);

        assertEquals(ContactSection.CONTACTS, modelManager.getActiveSection());
        assertEquals(List.of(ALICE), modelManager.getAddressBook().getPersonList());
        assertEquals(List.of(ALICE), modelManager.getAddressBook(ContactSection.FAVOURITES).getPersonList());
        modelManager.deletePerson(ALICE);
        assertEquals(List.of(ALICE), modelManager.getAddressBook(ContactSection.FAVOURITES).getPersonList());
    }

    @Test
    public void constructor_nullCollection_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new ModelManager(null, new AddressBook(), new UserPrefs()));
        assertThrows(NullPointerException.class, () -> new ModelManager(new AddressBook(), null, new UserPrefs()));
    }

    @Test
    public void contactOperations_eitherSection_changeOnlyActiveCollection() {
        AddressBook original = new AddressBookBuilder().withPerson(ALICE).build();
        Person editedAlice = new PersonBuilder(ALICE).withPhone("88888888").build();
        for (ContactSection section : ContactSection.values()) {
            ModelManager model = new ModelManager(original, original, new UserPrefs());
            model.setActiveSection(section);
            ContactSection other = section == ContactSection.CONTACTS
                    ? ContactSection.FAVOURITES : ContactSection.CONTACTS;

            assertFalse(model.hasPerson(BENSON));
            model.addPerson(BENSON);
            assertTrue(model.hasPerson(BENSON));
            assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());
            assertEquals(List.of(ALICE), model.getAddressBook(other).getPersonList());
            model.setPerson(ALICE, editedAlice);
            assertEquals(List.of(editedAlice, BENSON), model.getAddressBook().getPersonList());
            model.deletePerson(BENSON);
            assertEquals(List.of(editedAlice), model.getFilteredPersonList());
            model.setAddressBook(new AddressBook());
            assertTrue(model.getFilteredPersonList().isEmpty());
            assertEquals(List.of(ALICE), model.getAddressBook(other).getPersonList());
        }
    }

    @Test
    public void setActiveSection_changedSection_resetsSearchAndPreservesSharedPreferences() {
        modelManager.addPerson(ALICE);
        modelManager.addPerson(BENSON);
        modelManager.updateFilteredPersonList(person -> person.equals(ALICE));
        modelManager.setActiveSection(ContactSection.CONTACTS);
        assertEquals(List.of(ALICE), modelManager.getFilteredPersonList());

        modelManager.setActiveSection(ContactSection.FAVOURITES);
        assertTrue(modelManager.getFilteredPersonList().isEmpty());
        GuiSettings settings = new GuiSettings(300, 600, 4, 6);
        modelManager.setGuiSettings(settings);
        modelManager.setActiveSection(ContactSection.CONTACTS);
        assertEquals(List.of(ALICE, BENSON), modelManager.getFilteredPersonList());
        assertEquals(settings, modelManager.getGuiSettings());
    }

    @Test
    public void setActiveSection_nullSection_preservesSelection() {
        assertThrows(NullPointerException.class, () -> modelManager.setActiveSection(null));
        assertEquals(ContactSection.CONTACTS, modelManager.getActiveSection());
        assertThrows(NullPointerException.class, () -> modelManager.getAddressBook(null));
    }

    @Test
    public void getFilteredPersonList_favourites_isUnmodifiableAndTracksChanges() {
        modelManager.setActiveSection(ContactSection.FAVOURITES);
        ObservableList<Person> displayed = modelManager.getFilteredPersonList();
        modelManager.addPerson(ALICE);
        assertEquals(List.of(ALICE), displayed);
        assertThrows(UnsupportedOperationException.class, () -> displayed.add(BENSON));
        modelManager.updateFilteredPersonList(person -> false);
        assertTrue(displayed.isEmpty());
    }

    @Test
    public void setGuiSettings_nullGuiSettings_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.setGuiSettings(null));
    }

    @Test
    public void setGuiSettings_validGuiSettings_setsGuiSettings() {
        GuiSettings guiSettings = new GuiSettings(1, 2, 3, 4);
        modelManager.setGuiSettings(guiSettings);
        assertEquals(guiSettings, modelManager.getGuiSettings());
    }

    @Test
    public void hasPerson_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.hasPerson(null));
    }

    @Test
    public void hasPerson_personNotInAddressBook_returnsFalse() {
        assertFalse(modelManager.hasPerson(ALICE));
    }

    @Test
    public void hasPerson_personInAddressBook_returnsTrue() {
        modelManager.addPerson(ALICE);
        assertTrue(modelManager.hasPerson(ALICE));
    }

    @Test
    public void getFilteredPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> modelManager.getFilteredPersonList().remove(0));
    }

    @Test
    public void equals() {
        AddressBook addressBook = new AddressBookBuilder().withPerson(ALICE).withPerson(BENSON).build();
        AddressBook differentAddressBook = new AddressBook();
        UserPrefs userPrefs = new UserPrefs();

        // same values -> returns true
        modelManager = new ModelManager(addressBook, userPrefs);
        ModelManager modelManagerCopy = new ModelManager(addressBook, userPrefs);
        assertTrue(modelManager.equals(modelManagerCopy));

        // same object -> returns true
        assertTrue(modelManager.equals(modelManager));

        // null -> returns false
        assertFalse(modelManager.equals(null));

        // different types -> returns false
        assertFalse(modelManager.equals(5));

        // different addressBook -> returns false
        assertFalse(modelManager.equals(new ModelManager(differentAddressBook, userPrefs)));

        // different filteredList -> returns false
        String[] keywords = ALICE.getName().fullName.split("\\s+");
        modelManager.updateFilteredPersonList(new PersonContainsKeywordsPredicate(List.of(keywords)));
        assertFalse(modelManager.equals(new ModelManager(addressBook, userPrefs)));

        // resets modelManager to initial state for upcoming tests
        modelManager.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);

        // different userPrefs -> returns false
        UserPrefs differentUserPrefs = new UserPrefs();
        differentUserPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        assertFalse(modelManager.equals(new ModelManager(addressBook, differentUserPrefs)));

        // different favorites -> returns false
        assertFalse(modelManager.equals(new ModelManager(addressBook, addressBook, userPrefs)));

        // different active section -> returns false
        modelManagerCopy.setActiveSection(ContactSection.FAVOURITES);
        assertFalse(modelManager.equals(modelManagerCopy));
    }
}
