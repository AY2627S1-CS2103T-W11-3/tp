package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NOTE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;

import java.util.List;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.ContactSection;
import seedu.address.model.Model;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;

/**
 * Adds a person to the address book.
 */
public class AddCommand extends UndoableCommand {

    public static final String COMMAND_WORD = "add";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Adds a person to the address book. "
            + "Parameters: "
            + PREFIX_NAME + "NAME "
            + PREFIX_PHONE + "PHONE "
            + PREFIX_EMAIL + "EMAIL "
            + PREFIX_ADDRESS + "ADDRESS "
            + "[" + PREFIX_TAG + "TAG]... "
            + "[" + PREFIX_NOTE + "NOTE]\n"
            + "Example: " + COMMAND_WORD + " "
            + PREFIX_NAME + "John Doe "
            + PREFIX_PHONE + "98765432 "
            + PREFIX_EMAIL + "johnd@example.com "
            + PREFIX_ADDRESS + "311, Clementi Ave 2, #02-25 "
            + PREFIX_TAG + "friends "
            + PREFIX_TAG + "owesMoney "
            + PREFIX_NOTE + "Known through a dating app";

    public static final String MESSAGE_NOTE_NOT_AT_END =
            "Must be at the end of command. Usage: add n/<NAME> p/<NUMBER> e/EMAIL note/<NOTE>";

    public static final String MESSAGE_SUCCESS = "New person added: %1$s";
    public static final String MESSAGE_DUPLICATE_PHONE =
            "A contact with the same phone number already exists: %1$s";
    public static final String MESSAGE_DUPLICATE_EMAIL =
            "A contact with the same email address already exists: %1$s";
    public static final String MESSAGE_DUPLICATE_PHONE_AND_EMAIL =
            "A contact with the same phone number and email address already exists: %1$s";

    private final Person toAdd;

    /**
     * Creates an AddCommand to add the specified {@code Person}
     */
    public AddCommand(Person person) {
        requireNonNull(person);
        toAdd = person;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);

        List<Person> conflictingPersons = model.getAddressBook().getPersonList().stream()
                .filter(toAdd::isSamePerson)
                .toList();
        Person phoneConflict = conflictingPersons.stream()
                .filter(this::hasSameNonEmptyPhone)
                .findFirst()
                .orElse(null);
        Person emailConflict = conflictingPersons.stream()
                .filter(this::hasSameNonEmptyEmail)
                .findFirst()
                .orElse(null);

        if (phoneConflict != null && phoneConflict.equals(emailConflict)) {
            throw new CommandException(String.format(MESSAGE_DUPLICATE_PHONE_AND_EMAIL,
                    phoneConflict.getName()));
        }
        if (phoneConflict != null && emailConflict != null) {
            String phoneMessage = String.format(MESSAGE_DUPLICATE_PHONE, phoneConflict.getName());
            String emailMessage = String.format(MESSAGE_DUPLICATE_EMAIL, emailConflict.getName());
            throw new CommandException(phoneMessage + "\n" + emailMessage);
        }
        if (phoneConflict != null) {
            throw new CommandException(String.format(MESSAGE_DUPLICATE_PHONE, phoneConflict.getName()));
        }
        if (emailConflict != null) {
            throw new CommandException(String.format(MESSAGE_DUPLICATE_EMAIL, emailConflict.getName()));
        }

        model.addPerson(toAdd);
        return new CommandResult(String.format(MESSAGE_SUCCESS, Messages.format(toAdd)));
    }

    private boolean hasSameNonEmptyPhone(Person person) {
        return !toAdd.getPhone().value.isEmpty()
                && !person.getPhone().value.isEmpty()
                && toAdd.getPhone().equals(person.getPhone());
    }

    private boolean hasSameNonEmptyEmail(Person person) {
        return !toAdd.getEmail().value.isEmpty()
                && !person.getEmail().value.isEmpty()
                && toAdd.getEmail().equals(person.getEmail());
    }

    @Override
    public String describeUndo(ReadOnlyAddressBook previous, ReadOnlyAddressBook candidate, ContactSection section) {
        return describePersonChange(COMMAND_WORD, candidate, previous, section);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof AddCommand otherAddCommand)) {
            return false;
        }

        return toAdd.equals(otherAddCommand.toAdd);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("toAdd", toAdd)
                .toString();
    }
}
