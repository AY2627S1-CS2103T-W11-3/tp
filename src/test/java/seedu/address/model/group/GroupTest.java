package seedu.address.model.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;


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
        UUID alice = UUID.randomUUID();
        Group emptyGroup = new Group("Team", Set.of());
        Group withAlice = emptyGroup.withMember(alice);

        assertEquals(Set.of(), emptyGroup.getMembers());
        assertEquals(Set.of(alice), withAlice.getMembers());
        assertThrows(UnsupportedOperationException.class, () -> withAlice.getMembers().clear());
        assertEquals(emptyGroup, withAlice.withoutMember(alice));
    }

    @Test
    public void equality_usesNameAndMembers() {
        UUID alice = UUID.randomUUID();
        Group group = new Group("Team", Set.of(alice));
        Group sameGroup = new Group("Team", Set.of(alice));
        Group otherGroup = new Group("Other", group.getMembers());
        Group otherMembers = new Group("Team", Set.of());

        assertEquals(group, sameGroup);
        assertEquals(group.hashCode(), sameGroup.hashCode());
        assertNotEquals(group, otherGroup);
        assertNotEquals(group, otherMembers);
        assertNotEquals(group, "Team");
    }
}
