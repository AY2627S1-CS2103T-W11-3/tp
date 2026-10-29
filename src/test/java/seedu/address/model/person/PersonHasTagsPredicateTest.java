package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.model.tag.Tag;
import seedu.address.testutil.PersonBuilder;

public class PersonHasTagsPredicateTest {

    @Test
    public void equals() {
        Set<Tag> firstTags = Set.of(new Tag("friends"));
        Set<Tag> secondTags = Set.of(new Tag("friends"), new Tag("owesMoney"));

        PersonHasTagsPredicate firstPredicate = new PersonHasTagsPredicate(firstTags);
        PersonHasTagsPredicate secondPredicate = new PersonHasTagsPredicate(secondTags);

        // same object -> returns true
        assertTrue(firstPredicate.equals(firstPredicate));

        // same values -> returns true
        PersonHasTagsPredicate firstPredicateCopy = new PersonHasTagsPredicate(firstTags);
        assertTrue(firstPredicate.equals(firstPredicateCopy));

        // case-insensitive tag set -> returns true
        PersonHasTagsPredicate mixedCasePredicate = new PersonHasTagsPredicate(Set.of(new Tag("Friends")));
        assertTrue(firstPredicate.equals(mixedCasePredicate));

        // different types -> returns false
        assertFalse(firstPredicate.equals(1));

        // null -> returns false
        assertFalse(firstPredicate.equals(null));

        // different tags -> returns false
        assertFalse(firstPredicate.equals(secondPredicate));
    }

    @Test
    public void test_personHasAllRequiredTags_returnsTrue() {
        // One required tag
        PersonHasTagsPredicate predicate = new PersonHasTagsPredicate(List.of(new Tag("friends")));
        assertTrue(predicate.test(new PersonBuilder().withTags("friends").build()));

        // Person has extra tags
        assertTrue(predicate.test(new PersonBuilder().withTags("friends", "owesMoney").build()));

        // Multiple required tags
        predicate = new PersonHasTagsPredicate(List.of(new Tag("friends"), new Tag("owesMoney")));
        assertTrue(predicate.test(new PersonBuilder().withTags("owesMoney", "friends").build()));

        // Mixed-case match
        predicate = new PersonHasTagsPredicate(List.of(new Tag("Friends")));
        assertTrue(predicate.test(new PersonBuilder().withTags("friends").build()));
    }

    @Test
    public void test_personMissingRequiredTags_returnsFalse() {
        // No tags
        PersonHasTagsPredicate predicate = new PersonHasTagsPredicate(List.of(new Tag("friends")));
        assertFalse(predicate.test(new PersonBuilder().build()));

        // Different tag
        assertFalse(predicate.test(new PersonBuilder().withTags("colleagues").build()));

        // Partial tag name
        assertFalse(predicate.test(new PersonBuilder().withTags("friend").build()));

        // Has only one of two required tags
        predicate = new PersonHasTagsPredicate(List.of(new Tag("friends"), new Tag("owesMoney")));
        assertFalse(predicate.test(new PersonBuilder().withTags("friends").build()));
    }

    @Test
    public void getDisplayNames_sortsAndKeepsFirstSpelling() {
        PersonHasTagsPredicate predicate = new PersonHasTagsPredicate(
                List.of(new Tag("owesMoney"), new Tag("Friends"), new Tag("friends")));
        assertEquals("Friends, owesMoney", predicate.getDisplayNames());
    }

    @Test
    public void toStringMethod() {
        Set<Tag> tags = Set.of(new Tag("friends"));
        PersonHasTagsPredicate predicate = new PersonHasTagsPredicate(tags);

        String expected = PersonHasTagsPredicate.class.getCanonicalName()
                + "{requiredTags=[friends]}";
        assertEquals(expected, predicate.toString());
    }
}
