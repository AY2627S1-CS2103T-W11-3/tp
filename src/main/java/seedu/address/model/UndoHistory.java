package seedu.address.model;

import java.io.IOException;
import java.util.Optional;

import seedu.address.commons.exceptions.DataLoadingException;

/**
 * Provides contact snapshots for undo without exposing their storage representation.
 * Reading, saving, and consuming a snapshot must run synchronously without intervening commands.
 */
public interface UndoHistory {

    /** Default feedback for commands without a more specific undo description. */
    String MESSAGE_SUCCESS = "Undo successful.";

    /**
     * Reads the latest snapshot of both collections without consuming it, or returns empty when history is exhausted.
     * Missing or unreadable snapshot data leaves history unchanged and throws DataLoadingException.
     */
    Optional<ContactSnapshot> readLatestSnapshot() throws DataLoadingException;

    /**
     * Returns feedback for the latest entry in a nonempty history, with generic feedback for legacy entries.
     */
    String getLatestUndoDescription();

    /**
     * Saves the previously read snapshot with recovery metadata before applying it to the live model.
     * A failed save preserves the saved collection and history. Consumption follows only after success.
     */
    void saveRestoredSnapshot(ContactSnapshot snapshot) throws IOException;

    /**
     * Consumes the latest shared snapshot after its data has been saved successfully.
     * History must be nonempty. Backup cleanup failures are logged without failing the completed change.
     */
    void discardLatestSnapshot();
}
