package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.BOB;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.ContactSection;
import seedu.address.model.ContactSnapshot;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
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
        history.snapshot = Optional.of(new ContactSnapshot(snapshot, new AddressBook()));
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
    public void prepare_favourites_restoresBothCollectionsAndKeepsActiveSection() throws Exception {
        model.addPerson(BOB);
        model.setActiveSection(ContactSection.FAVOURITES);
        AddressBook favourites = getTypicalAddressBook();
        history.snapshot = Optional.of(new ContactSnapshot(new AddressBook(), favourites));
        PreparedChange change = new UndoCommand(history).prepare(model);
        assertEquals(new AddressBook(), model.getAddressBook());
        assertEquals(0, history.discardCount);

        change.apply();

        assertEquals(favourites, model.getAddressBook());
        assertEquals(favourites.getPersonList(), model.getFilteredPersonList());
        assertEquals(new AddressBook(), model.getAddressBook(ContactSection.CONTACTS));
        assertEquals(ContactSection.FAVOURITES, model.getActiveSection());
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
        private Optional<ContactSnapshot> snapshot = Optional.empty();
        private DataLoadingException failure;
        private int discardCount;
        private int saveCount;
        private String description = UndoCommand.MESSAGE_SUCCESS;

        @Override
        public Optional<ContactSnapshot> readLatestSnapshot() throws DataLoadingException {
            if (failure != null) {
                throw failure;
            }
            return snapshot;
        }

        @Override
        public String getLatestUndoDescription() {
            return description;
        }

        @Override
        public void saveRestoredSnapshot(ContactSnapshot snapshot) {
            assertEquals(this.snapshot.orElseThrow(), snapshot);
            saveCount++;
        }

        @Override
        public void discardLatestSnapshot() {
            discardCount++;
            snapshot = Optional.empty();
        }
    }
}
