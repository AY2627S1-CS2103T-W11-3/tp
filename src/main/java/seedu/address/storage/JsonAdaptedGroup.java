package seedu.address.storage;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.group.Group;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;

/** Jackson-friendly version of a group. */
class JsonAdaptedGroup {
    private final String name;
    private final List<String> members;

    @JsonCreator
    public JsonAdaptedGroup(@JsonProperty("name") String name,
            @JsonProperty("members") List<String> members) {
        this.name = name;
        this.members = members;
    }

    /** Creates a JSON group from the model. */
    public JsonAdaptedGroup(Group group) {
        name = group.getName();
        members = group.getMembers().stream().map(UUID::toString).toList();
    }

    /** Converts saved UUIDs or legacy contact names to member IDs. */
    public Group toModelType(List<Person> persons) throws IllegalValueException {
        if (!Group.isValidName(name) || members == null) {
            throw new IllegalValueException("Invalid group name or members.");
        }
        Set<UUID> modelMembers = new LinkedHashSet<>();
        for (String member : members) {
            if (member == null) {
                throw new IllegalValueException("Invalid or duplicate group member.");
            }
            if (Name.isValidName(member)) {
                List<Person> matches = persons.stream()
                        .filter(person -> person.getName().fullName.equals(member)).toList();
                if (matches.isEmpty()) {
                    throw new IllegalValueException("Group contains a contact that does not exist.");
                }
                for (Person person : matches) {
                    if (!modelMembers.add(person.getId())) {
                        throw new IllegalValueException("Invalid or duplicate group member.");
                    }
                }
            } else {
                UUID memberId;
                try {
                    memberId = UUID.fromString(member);
                    if (!memberId.toString().equalsIgnoreCase(member)) {
                        throw new IllegalArgumentException("Invalid UUID format");
                    }
                } catch (IllegalArgumentException e) {
                    throw new IllegalValueException("Invalid or duplicate group member.");
                }
                if (persons.stream().noneMatch(person -> person.getId().equals(memberId))
                        || !modelMembers.add(memberId)) {
                    throw new IllegalValueException("Invalid or duplicate group member.");
                }
            }
        }
        return new Group(name, modelMembers);
    }
}
