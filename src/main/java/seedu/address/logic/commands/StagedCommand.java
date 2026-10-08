package seedu.address.logic.commands;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;

/** A command whose changes must be saved before they are applied to the live model. */
public interface StagedCommand {
    /**
     * Validates and prepares a change without mutating the live model.
     * Preparation, saving, and application must run synchronously without intervening commands.
     */
    PreparedChange prepare(Model model) throws CommandException;
}
