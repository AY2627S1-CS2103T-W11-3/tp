package seedu.address.storage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.group.Group;
import seedu.address.model.person.Name;

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
        members = group.getMembers().stream().map(Name::toString).toList();
    }

    /** Converts the saved group to a model group. */
    public Group toModelType() throws IllegalValueException {
        if (!Group.isValidName(name) || members == null) {
            throw new IllegalValueException("Invalid group name or members.");
        }
        Set<Name> modelMembers = new HashSet<>();
        for (String member : members) {
            if (member == null || !Name.isValidName(member) || !modelMembers.add(new Name(member))) {
                throw new IllegalValueException("Invalid or duplicate group member.");
            }
        }
        return new Group(name, modelMembers);
    }
}
