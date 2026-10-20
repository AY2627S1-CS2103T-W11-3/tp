package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import javafx.collections.ListChangeListener;
import seedu.address.model.person.Person;
import seedu.address.testutil.AddressBookBuilder;

public class StagedModelTest {
    @Test
    public void deletePerson_filteredFavourites_isolatedUntilCommitAndPreservesLiveFilter() {
        AddressBook contacts = new AddressBookBuilder().withPerson(ALICE).build();
        AddressBook favourites = new AddressBookBuilder().withPerson(ALICE).withPerson(BENSON).build();
        Model source = new ModelManager(contacts, favourites, new UserPrefs());
        source.setActiveSection(ContactSection.FAVOURITES);
        source.updateFilteredPersonList(person -> person.equals(BENSON));
        AtomicInteger notifications = new AtomicInteger();
        source.getFilteredPersonList().addListener((ListChangeListener<Person>) change
            -> notifications.incrementAndGet());
        StagedModel staged = new StagedModel(source);

        assertEquals(ContactSection.FAVOURITES, staged.getActiveSection());
        assertEquals(List.of(BENSON), staged.getFilteredPersonList());
        assertEquals(List.of(ALICE), staged.getAddressBook(ContactSection.CONTACTS).getPersonList());
        staged.deletePerson(BENSON);
        assertTrue(staged.hasContactChanges());
        assertEquals(List.of(ALICE), staged.getAddressBook().getPersonList());
        assertEquals(List.of(ALICE, BENSON), source.getAddressBook().getPersonList());
        assertEquals(List.of(BENSON), source.getFilteredPersonList());
        assertEquals(0, notifications.get());

        staged.commitTo(source);

        assertEquals(List.of(ALICE), source.getAddressBook().getPersonList());
        assertEquals(List.of(ALICE), source.getAddressBook(ContactSection.CONTACTS).getPersonList());
        assertTrue(source.getFilteredPersonList().isEmpty());
        assertEquals(1, notifications.get());
        source.updateFilteredPersonList(Model.PREDICATE_SHOW_ALL_PERSONS);
        assertEquals(List.of(ALICE), source.getFilteredPersonList());
    }

    @Test
    public void setActiveSection_doesNotChangeSourceOrContactDataUntilCommit() {
        Model source = new ModelManager();
        source.addPerson(ALICE);
        StagedModel staged = new StagedModel(source);

        staged.setActiveSection(ContactSection.FAVOURITES);

        assertEquals(ContactSection.CONTACTS, source.getActiveSection());
        assertEquals(ContactSection.FAVOURITES, staged.getActiveSection());
        assertFalse(staged.hasContactChanges());
        staged.addPerson(BENSON);
        assertTrue(source.getAddressBook(ContactSection.FAVOURITES).getPersonList().isEmpty());
        staged.commitTo(source);
        assertEquals(ContactSection.FAVOURITES, source.getActiveSection());
        assertEquals(List.of(ALICE), source.getAddressBook(ContactSection.CONTACTS).getPersonList());
        assertEquals(List.of(BENSON), source.getFilteredPersonList());
    }
}
