package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.ContactSection;
import seedu.address.model.Model;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.group.Group;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;

/** Creates, deletes, lists, or views groups and changes their memberships. */
public class GroupCommand extends UndoableCommand {
    public static final String COMMAND_WORD = "group";
    public static final String MESSAGE_USAGE = "Invalid group command. Try:\n"
            + "group list | group view g/NAME\n"
            + "group add n/NAME | group delete g/NAME\n"
            + "group member g/NAME add/CONTACT or remove/CONTACT";

    /** The supported group operations. */
    public enum Action { LIST, VIEW, CREATE, DELETE, ADD_MEMBER, REMOVE_MEMBER }

    private final Action action;
    private final String groupName;
    private final Name personName;

    /**
     * Creates a group operation. Group name is null only for listing;
     * contact name is null unless changing membership.
     */
    public GroupCommand(Action action, String groupName, Name personName) {
        this.action = requireNonNull(action);
        this.groupName = action == Action.LIST ? null : requireNonNull(groupName);
        this.personName = personName;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        if (action == Action.LIST) {
            return listGroups(model);
        }
        Group group = model.getGroup(groupName);
        if (action == Action.VIEW) {
            if (group == null) {
                throw new CommandException("Group does not exist: " + groupName);
            }
            return viewGroup(group, model.getAddressBook().getPersonList());
        }
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

    private CommandResult listGroups(Model model) {
        List<Group> groups = model.getAddressBook().getGroupList();
        if (groups.isEmpty()) {
            return new CommandResult("No groups exist.");
        }
        List<Person> persons = model.getAddressBook().getPersonList();
        StringBuilder message = new StringBuilder("Groups:");
        for (int i = 0; i < groups.size(); i++) {
            Group group = groups.get(i);
            int count = matchingContacts(group, persons).size();
            message.append("\n").append(i + 1).append(". ").append(group.getName())
                    .append(" (").append(count).append(count == 1 ? " member)" : " members)");
        }
        return new CommandResult(message.toString());
    }

    private CommandResult viewGroup(Group group, List<Person> persons) {
        List<Person> members = matchingContacts(group, persons);
        if (members.isEmpty()) {
            return new CommandResult("Group " + group.getName() + " has no members.");
        }
        StringBuilder message = new StringBuilder("Members of ").append(group.getName()).append(":");
        for (int i = 0; i < members.size(); i++) {
            Person member = members.get(i);
            message.append("\n").append(i + 1).append(". ").append(member.getName())
                    .append(" (Phone: ").append(member.getPhone()).append(")");
        }
        return new CommandResult(message.toString());
    }

    private List<Person> matchingContacts(Group group, List<Person> persons) {
        List<Person> members = new ArrayList<>();
        for (Name memberName : group.getMembers()) {
            for (Person person : persons) {
                if (person.getName().equals(memberName)) {
                    members.add(person);
                }
            }
        }
        return members;
    }

    @Override
    public String describeUndo(ReadOnlyAddressBook previous, ReadOnlyAddressBook candidate, ContactSection section) {
        return switch (action) {
            case LIST, VIEW -> throw new IllegalStateException("Read-only group operations cannot be undone.");
            case CREATE -> "Undid creation of group " + groupName + ".";
            case DELETE -> "Undid deletion of group " + groupName + ".";
            case ADD_MEMBER -> "Undid addition of " + personName + " to group " + groupName + ".";
            case REMOVE_MEMBER -> "Undid removal of " + personName + " from group " + groupName + ".";
        };
    }
}
