package seedu.address.model;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Objects;

/**
 * Captures both contact collections without retaining mutable references to the source address books.
 * Contact values are immutable, so snapshots can share those values safely.
 */
public final class ContactSnapshot {
    private final ReadOnlyAddressBook contacts;
    private final ReadOnlyAddressBook favourites;

    /**
     * Copies both collections, including their contact order, IDs, groups, tags, and notes.
     */
    public ContactSnapshot(ReadOnlyAddressBook contacts, ReadOnlyAddressBook favourites) {
        requireAllNonNull(contacts, favourites);
        this.contacts = new AddressBook(contacts);
        this.favourites = new AddressBook(favourites);
    }

    /**
     * Returns the complete collection captured for the given section.
     */
    public ReadOnlyAddressBook getAddressBook(ContactSection section) {
        Objects.requireNonNull(section);
        return section == ContactSection.CONTACTS ? contacts : favourites;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof ContactSnapshot snapshot
                && contacts.equals(snapshot.contacts) && favourites.equals(snapshot.favourites);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contacts, favourites);
    }
}
