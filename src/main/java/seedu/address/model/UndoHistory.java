package seedu.address.model;

import java.util.Optional;

import seedu.address.commons.exceptions.DataLoadingException;

/**
 * Provides contact snapshots for undo without exposing their storage representation.
 * Reading, saving, and consuming a snapshot must run synchronously without intervening commands.
 */
public interface UndoHistory {

    /**
     * Reads the section's latest snapshot without consuming it, or returns empty when history is exhausted.
     * Missing or unreadable snapshot data leaves history unchanged and throws DataLoadingException.
     */
    Optional<ReadOnlyAddressBook> readLatestSnapshot(ContactSection section) throws DataLoadingException;

    /**
     * Consumes the section's latest snapshot after its data has been saved successfully.
     * History must be nonempty. Backup cleanup failures are logged without failing the completed change.
     */
    void discardLatestSnapshot(ContactSection section);
}
