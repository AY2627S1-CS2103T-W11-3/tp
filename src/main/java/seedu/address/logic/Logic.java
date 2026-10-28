package seedu.address.logic;

import java.nio.file.Path;
import java.util.List;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ContactSection;
import seedu.address.model.person.Person;

/**
 * API of the Logic component
 */
public interface Logic {
    /**
     * Returns the section used by contact commands.
     */
    ContactSection getActiveSection();

    /**
     * Returns the data file for the active section.
     */
    Path getActiveDataFilePath();

    /**
     * Executes the command and returns the result.
     * @param commandText The command as entered by the user.
     * @return the result of the command execution.
     * @throws CommandException If an error occurs during command execution.
     * @throws ParseException If an error occurs during parsing.
     */
    CommandResult execute(String commandText) throws CommandException, ParseException;

    /**
     * Returns the command words available for autocomplete, in alphabetical order.
     */
    List<String> getCommandWords();

    /**
     * Returns an unmodifiable view of the active section's displayed contacts.
     */
    ObservableList<Person> getFilteredPersonList();

    /**
     * Returns whether a contact in Favourites shares the given contact's phone number or email address.
     * Searches the complete collection, regardless of the active section or its search filter.
     *
     * @param person The contact to check for a matching favorite.
     */
    boolean isFavourite(Person person);

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Set the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);
}
