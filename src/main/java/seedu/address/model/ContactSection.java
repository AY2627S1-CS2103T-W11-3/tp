package seedu.address.model;

/**
 * Identifies the independent Contacts and Favourites collections and their names for display.
 */
public enum ContactSection {
    CONTACTS("Contacts"),
    FAVOURITES("Favourites");

    private final String displayName;

    ContactSection(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
