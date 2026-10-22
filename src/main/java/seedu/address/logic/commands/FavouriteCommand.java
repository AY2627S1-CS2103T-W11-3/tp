package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.util.List;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.ContactSection;
import seedu.address.model.Model;
import seedu.address.model.person.Person;

/** Copies a displayed contact into the independent favorites collection. */
public class FavouriteCommand extends Command {
    public static final String COMMAND_WORD = "favourite";
    public static final String MESSAGE_USAGE = "favourite: Copies a displayed contact into Favourites.\n"
            + "Parameters: INDEX (a positive integer without leading zeros)\nExample: favourite 1";
    public static final String MESSAGE_WRONG_SECTION =
            "You are already in Favourites. Switch to Contacts to favourite a contact.";
    public static final String MESSAGE_DUPLICATE = "This contact is already in Favourites.";
    public static final String MESSAGE_EMPTY_LIST = "No contacts are displayed. Nothing to favourite.";
    public static final String MESSAGE_INDEX_OUT_OF_RANGE =
            "Contact index out of range. Choose an index from 1 to %1$d.";
    public static final String MESSAGE_SUCCESS = "Added to Favourites: %1$s (index %2$d).";

    private final BigInteger targetIndex;

    /** Creates a command for a positive, one-based displayed index. */
    public FavouriteCommand(BigInteger targetIndex) {
        this.targetIndex = requireNonNull(targetIndex);
        if (targetIndex.signum() <= 0) {
            throw new IllegalArgumentException("Contact index must be positive");
        }
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        if (model.getActiveSection() == ContactSection.FAVOURITES) {
            throw new CommandException(MESSAGE_WRONG_SECTION);
        }
        List<Person> displayed = model.getFilteredPersonList();
        if (displayed.isEmpty()) {
            throw new CommandException(MESSAGE_EMPTY_LIST);
        }
        if (targetIndex.compareTo(BigInteger.valueOf(displayed.size())) > 0) {
            throw new CommandException(String.format(MESSAGE_INDEX_OUT_OF_RANGE, displayed.size()));
        }
        Person person = displayed.get(targetIndex.intValueExact() - 1);
        if (model.getAddressBook(ContactSection.FAVOURITES).getPersonList().stream().anyMatch(person::isSamePerson)) {
            throw new CommandException(MESSAGE_DUPLICATE);
        }
        model.addFavourite(person);
        return new CommandResult(String.format(MESSAGE_SUCCESS, person.getName().fullName, targetIndex));
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof FavouriteCommand command && targetIndex.equals(command.targetIndex);
    }

    @Override
    public int hashCode() {
        return targetIndex.hashCode();
    }
}
