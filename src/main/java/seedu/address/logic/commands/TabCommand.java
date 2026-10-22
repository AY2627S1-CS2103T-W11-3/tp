package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.model.ContactSection;
import seedu.address.model.Model;

/** Selects the collection used by subsequent contact commands. */
public class TabCommand extends Command {
    public static final String COMMAND_WORD = "tab";
    public static final String MESSAGE_USAGE = "tab: Switches the active contact collection.\n"
            + "Parameters: contacts | favourites\nExample: tab favourites";
    public static final String MESSAGE_SUCCESS = "Showing %1$s.";

    private final ContactSection section;

    /** Creates a command selecting the given collection. */
    public TabCommand(ContactSection section) {
        this.section = requireNonNull(section);
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model).setActiveSection(section);
        return new CommandResult(String.format(MESSAGE_SUCCESS, section.getDisplayName()));
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof TabCommand command && section == command.section;
    }

    @Override
    public int hashCode() {
        return section.hashCode();
    }
}
