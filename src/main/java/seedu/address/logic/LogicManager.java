package seedu.address.logic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.PreparedChange;
import seedu.address.logic.commands.StagedCommand;
import seedu.address.logic.commands.UndoableCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ContactSection;
import seedu.address.model.ContactSnapshot;
import seedu.address.model.Model;
import seedu.address.model.StagedModel;
import seedu.address.model.UndoHistory;
import seedu.address.model.person.Person;
import seedu.address.storage.Storage;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {
    public static final String MESSAGE_SAVE_ERROR =
            "Could not save changes. No changes were applied. Please try again.";

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final AddressBookParser addressBookParser;

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and
     * {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this.model = model;
        this.storage = storage;
        addressBookParser = new AddressBookParser(storage);
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        logger.info("----------------[USER COMMAND][" + commandText + "]");

        Command command = addressBookParser.parseCommand(commandText);
        if (command instanceof StagedCommand stagedCommand) {
            return executeStaged(stagedCommand);
        }
        StagedModel stagedModel = new StagedModel(model);
        CommandResult commandResult = command.execute(stagedModel);

        if (stagedModel.hasContactChanges()) {
            try {
                ContactSection changedSection = stagedModel.getChangedSection();
                ContactSnapshot previous = capture(model);
                ContactSnapshot candidate = capture(stagedModel);
                String description = command instanceof UndoableCommand undoableCommand
                        ? undoableCommand.describeUndo(previous.getAddressBook(changedSection),
                                candidate.getAddressBook(changedSection), changedSection)
                        : UndoHistory.MESSAGE_SUCCESS;
                storage.saveUndoableChange(previous, candidate, description);
            } catch (IOException ioe) {
                logger.warning("Could not save contact changes: " + ioe.getMessage());
                throw new CommandException(MESSAGE_SAVE_ERROR, ioe);
            }
        }

        stagedModel.commitTo(model);
        return commandResult;
    }

    /**
     * Saves a prepared operation and both collections' history before changing the observable model.
     */
    private CommandResult executeStaged(StagedCommand command) throws CommandException {
        PreparedChange change = command.prepare(model);
        ContactSection section = model.getActiveSection();
        ContactSnapshot previous = capture(model);
        ContactSnapshot candidate = new ContactSnapshot(
                section == ContactSection.CONTACTS ? change.getCandidate()
                        : model.getAddressBook(ContactSection.CONTACTS),
                section == ContactSection.FAVOURITES ? change.getCandidate()
                        : model.getAddressBook(ContactSection.FAVOURITES));
        String description = command instanceof UndoableCommand undoableCommand
                ? undoableCommand.describeUndo(previous.getAddressBook(section), change.getCandidate(), section)
                : UndoHistory.MESSAGE_SUCCESS;
        try {
            change.save(() -> storage.saveUndoableChange(previous, candidate, description));
        } catch (IOException e) {
            logger.warning("Could not save prepared contact changes: " + e.getMessage());
            throw new CommandException(change.getSaveErrorMessage(), e);
        }
        return change.apply();
    }

    /**
     * Copies the complete data of both sections without including their current search or sorting.
     */
    private ContactSnapshot capture(Model source) {
        return new ContactSnapshot(source.getAddressBook(ContactSection.CONTACTS),
                source.getAddressBook(ContactSection.FAVOURITES));
    }

    @Override
    public ContactSection getActiveSection() {
        return model.getActiveSection();
    }

    @Override
    public Path getActiveDataFilePath() {
        return getActiveSection() == ContactSection.FAVOURITES
                ? storage.getFavouritesFilePath() : storage.getAddressBookFilePath();
    }

    @Override
    public List<String> getCommandWords() {
        return CommandWords.get();
    }

    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return model.getFilteredPersonList();
    }

    @Override
    public GuiSettings getGuiSettings() {
        return model.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        model.setGuiSettings(guiSettings);
    }
}
