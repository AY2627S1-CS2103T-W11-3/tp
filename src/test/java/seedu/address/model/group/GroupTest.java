package seedu.address.model.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.Name;

/** Tests the value and membership rules of a group. */
public class GroupTest {
    @Test
    public void constructor_invalidName_rejected() {
        assertThrows(NullPointerException.class, () -> new Group(null, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Group(" ", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Group(" Team ", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Group("T".repeat(101), Set.of()));
        assertFalse(Group.isValidName(null));
        assertTrue(Group.isValidName("CS2103T Team"));
    }

    @Test
    public void membershipChanges_returnIndependentGroups() {
        Name alice = new Name("Alice Pauline");
        Group emptyGroup = new Group("Team", Set.of());
        Group withAlice = emptyGroup.withMember(alice);

        assertEquals(Set.of(), emptyGroup.getMembers());
        assertEquals(Set.of(alice), withAlice.getMembers());
        assertThrows(UnsupportedOperationException.class, () -> withAlice.getMembers().clear());
        assertEquals(emptyGroup, withAlice.withoutMember(alice));
    }

    @Test
    public void equality_usesNameAndMembers() {
        Group group = new Group("Team", Set.of(new Name("Alice Pauline")));
        Group sameGroup = new Group("Team", Set.of(new Name("Alice Pauline")));
        Group otherGroup = new Group("Other", group.getMembers());
        Group otherMembers = new Group("Team", Set.of());

        assertEquals(group, sameGroup);
        assertEquals(group.hashCode(), sameGroup.hashCode());
        assertNotEquals(group, otherGroup);
        assertNotEquals(group, otherMembers);
        assertNotEquals(group, "Team");
    }
}
