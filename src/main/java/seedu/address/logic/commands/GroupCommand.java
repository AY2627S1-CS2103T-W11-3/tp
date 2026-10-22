package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Set;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.group.Group;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;

/** Creates or deletes a group, or changes one of its memberships. */
public class GroupCommand extends Command {
    public static final String COMMAND_WORD = "group";
    public static final String MESSAGE_USAGE = "Usage: group add n/<GROUP NAME> | "
            + "group delete g/<GROUP NAME> | "
            + "group member g/<GROUP NAME> add/<CONTACT NAME> | "
            + "group member g/<GROUP NAME> remove/<CONTACT NAME>";

    /** The supported group operations. */
    public enum Action { CREATE, DELETE, ADD_MEMBER, REMOVE_MEMBER }

    private final Action action;
    private final String groupName;
    private final Name personName;

    /** Creates a group operation. Contact name is null when creating or deleting a group. */
    public GroupCommand(Action action, String groupName, Name personName) {
        this.action = requireNonNull(action);
        this.groupName = requireNonNull(groupName);
        this.personName = personName;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Group group = model.getGroup(groupName);
        if (action == Action.CREATE) {
            if (group != null) {
                throw new CommandException("Group already exists: " + groupName);
            }
            model.addGroup(new Group(groupName, Set.of()));
            return new CommandResult("Created group: " + groupName);
        }
        if (group == null) {
            throw new CommandException("Group does not exist: " + groupName);
        }
        if (action == Action.DELETE) {
            model.deleteGroup(groupName);
            return new CommandResult("Deleted group: " + groupName);
        }
        boolean personExists = model.getAddressBook().getPersonList().stream()
                .map(Person::getName).anyMatch(personName::equals);
        if (!personExists) {
            throw new CommandException("Contact does not exist: " + personName);
        }
        boolean isMember = group.getMembers().contains(personName);
        if (action == Action.ADD_MEMBER) {
            if (isMember) {
                throw new CommandException("Contact is already in group: " + personName);
            }
            model.setGroup(group.withMember(personName));
            return new CommandResult("Added " + personName + " to " + groupName);
        }
        if (!isMember) {
            throw new CommandException("Contact is not in group: " + personName);
        }
        model.setGroup(group.withoutMember(personName));
        return new CommandResult("Removed " + personName + " from " + groupName);
    }
}
