package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.BOB;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.ContactSection;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UndoHistory;

public class UndoCommandTest {
    private final Model model = new ModelManager();
    private final UndoHistoryStub history = new UndoHistoryStub();

    @Test
    public void prepare_validHistory_restoresDataAndConsumesHistoryOnlyOnApply() throws Exception {
        model.addPerson(BOB);
        model.updateFilteredPersonList(unused -> false);
        AddressBook before = new AddressBook(model.getAddressBook());
        AddressBook snapshot = getTypicalAddressBook();
        history.snapshot = Optional.of(snapshot);
        history.description = "Undid clear of Contacts.";

        PreparedChange change = new UndoCommand(history).prepare(model);
        assertEquals(snapshot, change.getCandidate());
        assertEquals(UndoCommand.MESSAGE_SAVE_ERROR, change.getSaveErrorMessage());
        assertEquals(before, model.getAddressBook());
        assertEquals(0, model.getFilteredPersonList().size());
        assertEquals(0, history.discardCount);

        change.save(() -> {
            throw new AssertionError("Undo must use its recovery-aware save");
        });
        assertEquals(1, history.saveCount);
        assertEquals(before, model.getAddressBook());
        assertEquals(new CommandResult(history.description), change.apply());
        assertEquals(snapshot, model.getAddressBook());
        assertEquals(snapshot.getPersonList(), model.getFilteredPersonList());
        assertEquals(1, history.discardCount);
        assertEquals(ContactSection.CONTACTS, history.readSection);
        assertEquals(ContactSection.CONTACTS, history.discardedSection);
    }

    @Test
    public void prepare_emptyHistory_throwsWithoutChangingModelOrHistory() {
        assertPreparationFailure(UndoCommand.MESSAGE_NO_HISTORY);
    }

    @Test
    public void prepare_unreadableHistory_throwsWithoutChangingModelOrHistory() {
        history.failure = new DataLoadingException(new IOException("Unreadable snapshot"));
        assertPreparationFailure(UndoCommand.MESSAGE_LOAD_ERROR);
    }

    @Test
    public void prepare_favourites_restoresOnlyFavouritesAndConsumesTheirHistory() throws Exception {
        model.addPerson(BOB);
        model.setActiveSection(ContactSection.FAVOURITES);
        AddressBook snapshot = getTypicalAddressBook();
        history.snapshot = Optional.of(snapshot);
        PreparedChange change = new UndoCommand(history).prepare(model);
        assertEquals(ContactSection.FAVOURITES, history.readSection);
        assertEquals(new AddressBook(), model.getAddressBook());
        assertEquals(0, history.discardCount);

        change.apply();
        assertEquals(snapshot, model.getAddressBook());
        assertEquals(snapshot.getPersonList(), model.getFilteredPersonList());
        assertEquals(List.of(BOB), model.getAddressBook(ContactSection.CONTACTS).getPersonList());
        assertEquals(ContactSection.FAVOURITES, history.discardedSection);
        assertEquals(1, history.discardCount);
    }

    private void assertPreparationFailure(String message) {
        model.addPerson(BOB);
        model.updateFilteredPersonList(unused -> false);
        AddressBook before = new AddressBook(model.getAddressBook());
        assertThrows(CommandException.class, message, () -> new UndoCommand(history).prepare(model));
        assertEquals(before, model.getAddressBook());
        assertEquals(0, model.getFilteredPersonList().size());
        assertEquals(0, history.discardCount);
    }

    private static class UndoHistoryStub implements UndoHistory {
        private Optional<ReadOnlyAddressBook> snapshot = Optional.empty();
        private DataLoadingException failure;
        private int discardCount;
        private int saveCount;
        private String description = UndoCommand.MESSAGE_SUCCESS;
        private ContactSection readSection;
        private ContactSection discardedSection;

        @Override
        public Optional<ReadOnlyAddressBook> readLatestSnapshot(ContactSection section) throws DataLoadingException {
            readSection = section;
            if (failure != null) {
                throw failure;
            }
            return snapshot;
        }

        @Override
        public String getLatestUndoDescription(ContactSection section) {
            assertEquals(readSection, section);
            return description;
        }

        @Override
        public void saveRestoredSnapshot(ContactSection section, ReadOnlyAddressBook snapshot) {
            assertEquals(readSection, section);
            assertEquals(this.snapshot.orElseThrow(), snapshot);
            saveCount++;
        }

        @Override
        public void discardLatestSnapshot(ContactSection section) {
            discardedSection = section;
            discardCount++;
            snapshot = Optional.empty();
        }
    }
}
