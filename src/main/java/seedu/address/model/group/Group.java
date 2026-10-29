package seedu.address.model.group;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Represents a named group of contacts identified by persistent IDs. */
public class Group {
    private final String name;
    private final Set<UUID> members;

    /** Creates a group with the given name and member IDs. */
    public Group(String name, Set<UUID> members) {
        requireNonNull(name);
        requireNonNull(members);
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Group name must be 1 to 100 characters and contain a letter or digit.");
        }
        this.name = name;
        this.members = Collections.unmodifiableSet(new LinkedHashSet<>(members));
    }

    /** Returns whether a group name is valid. */
    public static boolean isValidName(String name) {
        return name != null && name.length() <= 100 && name.matches(".*[\\p{L}\\p{N}].*")
                && name.equals(name.strip());
    }

    public String getName() {
        return name;
    }

    /** Returns the IDs of this group's members. */
    public Set<UUID> getMembers() {
        return members;
    }

    /** Returns a group with the given contact ID added. */
    public Group withMember(UUID member) {
        Set<UUID> updatedMembers = new LinkedHashSet<>(members);
        updatedMembers.add(member);
        return new Group(name, updatedMembers);
    }

    /** Returns a group with the given contact ID removed. */
    public Group withoutMember(UUID member) {
        Set<UUID> updatedMembers = new LinkedHashSet<>(members);
        updatedMembers.remove(member);
        return new Group(name, updatedMembers);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Group otherGroup)) {
            return false;
        }
        return name.equals(otherGroup.name) && members.equals(otherGroup.members);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, members);
    }
}
