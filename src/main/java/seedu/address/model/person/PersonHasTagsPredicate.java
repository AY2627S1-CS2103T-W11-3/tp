package seedu.address.model.person;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.tag.Tag;

/**
 * Tests that a {@code Person} has all of the given tags.
 * Tag matching is case-insensitive and requires a full tag-name match.
 */
public class PersonHasTagsPredicate implements Predicate<Person> {

    private final Set<String> requiredTags;
    private final String displayNames;

    /**
     * Constructs a predicate that requires every tag in {@code tags}.
     * Duplicate tags that differ only by case are treated as one requirement.
     */
    public PersonHasTagsPredicate(Collection<Tag> tags) {
        Objects.requireNonNull(tags);
        TreeMap<String, String> namesByKey = new TreeMap<>();
        for (Tag tag : tags) {
            namesByKey.putIfAbsent(tag.tagName.toLowerCase(), tag.tagName);
        }
        requiredTags = Set.copyOf(namesByKey.keySet());
        displayNames = String.join(", ", namesByKey.values());
    }

    /**
     * Returns the required tag names in alphabetical order, keeping the first spelling of each name.
     */
    public String getDisplayNames() {
        return displayNames;
    }

    @Override
    public boolean test(Person person) {
        Set<String> personTags = person.getTags().stream()
                .map(tag -> tag.tagName.toLowerCase())
                .collect(Collectors.toCollection(HashSet::new));
        return personTags.containsAll(requiredTags);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof PersonHasTagsPredicate otherPersonHasTagsPredicate)) {
            return false;
        }

        return requiredTags.equals(otherPersonHasTagsPredicate.requiredTags);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("requiredTags", requiredTags).toString();
    }
}
