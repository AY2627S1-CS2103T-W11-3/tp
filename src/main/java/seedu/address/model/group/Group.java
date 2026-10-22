package seedu.address.model.group;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import seedu.address.model.person.Name;

/** Represents a named group of contacts. */
public class Group {
    private final String name;
    private final Set<Name> members;

    /** Creates a group with the given name and members. */
    public Group(String name, Set<Name> members) {
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

    public Set<Name> getMembers() {
        return members;
    }

    /** Returns a group with the given contact added. */
    public Group withMember(Name member) {
        Set<Name> updatedMembers = new LinkedHashSet<>(members);
        updatedMembers.add(member);
        return new Group(name, updatedMembers);
    }

    /** Returns a group with the given contact removed. */
    public Group withoutMember(Name member) {
        Set<Name> updatedMembers = new LinkedHashSet<>(members);
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
