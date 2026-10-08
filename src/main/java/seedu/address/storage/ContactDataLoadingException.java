package seedu.address.storage;

import seedu.address.commons.exceptions.DataLoadingException;

/**
 * Identifies a contact-data loading failure with a message suitable for display at startup.
 */
public class ContactDataLoadingException extends DataLoadingException {
    public static final String MESSAGE_UNREADABLE =
            "Unable to load saved contact data because the data file could not be read.";
    public static final String MESSAGE_MALFORMED =
            "Unable to load saved contact data because the data file is corrupted or incorrectly formatted.";
    public static final String MESSAGE_INVALID =
            "Unable to load saved contact data because one or more contacts contain invalid information.";

    private final String userMessage;

    /**
     * Creates a failure with its user-facing explanation and underlying cause.
     */
    public ContactDataLoadingException(String userMessage, Exception cause) {
        super(cause);
        this.userMessage = userMessage;
    }

    @Override
    public String getMessage() {
        return userMessage;
    }
}
