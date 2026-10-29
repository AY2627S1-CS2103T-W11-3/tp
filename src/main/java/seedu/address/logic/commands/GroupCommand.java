package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import seedu.address.commons.core.index.Index;
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
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Creates, lists, views, or deletes groups and changes their members.\n"
            + "Parameters:\n"
            + "group list\n"
            + "group view g/GROUP_NAME\n"
            + "group add n/GROUP_NAME\n"
            + "group delete g/GROUP_NAME\n"
            + "group member g/GROUP_NAME add/CONTACT_NAME\n"
            + "group member g/GROUP_NAME remove/CONTACT_NAME\n"
            + "group member g/GROUP_NAME add/i/INDEX\n"
            + "group member g/GROUP_NAME remove/i/INDEX\n"
            + "Examples:\n"
            + "group list\n"
            + "group view g/Project Team\n"
            + "group add n/Project Team\n"
            + "group delete g/Project Team\n"
            + "group member g/Project Team add/Alex Tan\n"
            + "group member g/Project Team remove/Alex Tan\n"
            + "group member g/Project Team add/i/2\n"
            + "group member g/Project Team remove/i/2";

    /** The supported group operations. */
    public enum Action { LIST, VIEW, CREATE, DELETE, ADD_MEMBER, REMOVE_MEMBER }

    private final Action action;
    private final String groupName;
    private final Name personName;
    private final Index personIndex;

    /**
     * Creates a group operation. Group name is null only for listing;
     * contact name is null unless changing membership.
     */
    public GroupCommand(Action action, String groupName, Name personName) {
        this(action, groupName, personName, null);
    }

    private GroupCommand(Action action, String groupName, Name personName, Index personIndex) {
        this.action = requireNonNull(action);
        this.groupName = action == Action.LIST ? null : requireNonNull(groupName);
        this.personName = personName;
        this.personIndex = personIndex;
    }

    /** Creates a membership command that selects a contact by its displayed index. */
    public static GroupCommand forMemberIndex(Action action, String groupName, Index personIndex) {
        if (action != Action.ADD_MEMBER && action != Action.REMOVE_MEMBER) {
            throw new IllegalArgumentException("An index can only be used for membership changes.");
        }
        return new GroupCommand(action, groupName, null, requireNonNull(personIndex));
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
        Person person = resolvePerson(model);
        UUID personId = person.getId();
        String personLabel = personIndex == null ? person.getName().toString()
                : person.getName() + " (Phone: " + person.getPhone() + ")";
        boolean isMember = group.getMembers().contains(personId);
        if (action == Action.ADD_MEMBER) {
            if (isMember) {
                throw new CommandException("Contact is already in group: " + personLabel);
            }
            model.setGroup(group.withMember(personId));
            return new CommandResult("Added " + personLabel + " to " + groupName);
        }
        if (!isMember) {
            throw new CommandException("Contact is not in group: " + personLabel);
        }
        model.setGroup(group.withoutMember(personId));
        return new CommandResult("Removed " + personLabel + " from " + groupName);
    }

    private Person resolvePerson(Model model) throws CommandException {
        if (personIndex != null) {
            List<Person> displayed = model.getFilteredPersonList();
            if (personIndex.getZeroBased() >= displayed.size()) {
                throw new CommandException("Contact index out of range. Choose an index shown in the contact list.");
            }
            return displayed.get(personIndex.getZeroBased());
        }
        List<Person> matches = model.getAddressBook().getPersonList().stream()
                .filter(person -> person.getName().equals(personName)).toList();
        if (matches.isEmpty()) {
            throw new CommandException("Contact does not exist: " + personName);
        }
        if (matches.size() > 1) {
            throw new CommandException("Multiple contacts named " + personName
                    + ". Use add/i/INDEX or remove/i/INDEX with a displayed contact index.");
        }
        return matches.getFirst();
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
        for (UUID memberId : group.getMembers()) {
            for (Person person : persons) {
                if (person.getId().equals(memberId)) {
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
            case ADD_MEMBER -> "Undid addition of " + changedMember(candidate, previous)
                    + " to group " + groupName + ".";
            case REMOVE_MEMBER -> "Undid removal of " + changedMember(previous, candidate)
                    + " from group " + groupName + ".";
        };
    }

    private String changedMember(ReadOnlyAddressBook source, ReadOnlyAddressBook comparison) {
        Group sourceGroup = source.getGroupList().stream()
                .filter(group -> group.getName().equals(groupName)).findFirst().orElseThrow();
        Group comparisonGroup = comparison.getGroupList().stream()
                .filter(group -> group.getName().equals(groupName)).findFirst().orElseThrow();
        UUID changedId = sourceGroup.getMembers().stream()
                .filter(id -> !comparisonGroup.getMembers().contains(id)).findFirst().orElseThrow();
        Person person = source.getPersonList().stream()
                .filter(candidate -> candidate.getId().equals(changedId)).findFirst().orElseThrow();
        return personIndex == null ? person.getName().toString()
                : person.getName() + " (Phone: " + person.getPhone() + ")";
    }
}
