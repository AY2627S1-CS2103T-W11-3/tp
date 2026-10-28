package seedu.address.model;

import java.io.IOException;
import java.util.Optional;

import seedu.address.commons.exceptions.DataLoadingException;

/**
 * Provides contact snapshots for undo without exposing their storage representation.
 * Reading, saving, and consuming a snapshot must run synchronously without intervening commands.
 */
public interface UndoHistory {

    /** Fallback feedback for snapshots predating operation descriptions. */
    String MESSAGE_LEGACY_SUCCESS = "Undo successful.";

    /**
     * Reads the section's latest snapshot without consuming it, or returns empty when history is exhausted.
     * Missing or unreadable snapshot data leaves history unchanged and throws DataLoadingException.
     */
    Optional<ReadOnlyAddressBook> readLatestSnapshot(ContactSection section) throws DataLoadingException;

    /**
     * Saves the previously read snapshot with recovery metadata before applying it to the live model.
     * A failed save preserves the saved collection and history. Consumption follows only after success.
     */
    void saveRestoredSnapshot(ContactSection section, ReadOnlyAddressBook snapshot) throws IOException;

    /**
     * Consumes the section's latest snapshot after its data has been saved successfully.
     * History must be nonempty. Backup cleanup failures are logged without failing the completed change.
     */
    void discardLatestSnapshot(ContactSection section);
}
