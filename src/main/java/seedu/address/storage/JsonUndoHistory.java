package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import seedu.address.model.UndoHistory;

/**
 * Stores snapshot order and the checksum of the collection to which the history belongs.
 */
class JsonUndoHistory {
    private final String dataChecksum;
    private final List<String> snapshots;
    private final JsonUndoHistory previous;
    private final Map<String, String> undoDescriptions;

    /**
     * Creates checkpoint metadata, ordered from oldest to newest.
     */
    JsonUndoHistory(String dataChecksum, List<String> snapshots) {
        this(dataChecksum, snapshots, null);
    }

    /**
     * Records both outcomes before an atomic collection save can be interrupted.
     */
    JsonUndoHistory(String dataChecksum, List<String> snapshots, JsonUndoHistory previous) {
        this(dataChecksum, snapshots, Map.of(), previous);
    }

    /**
     * Records descriptions alongside their snapshot names in the same recoverable metadata.
     */
    JsonUndoHistory(String dataChecksum, List<String> snapshots, Map<String, String> undoDescriptions,
            JsonUndoHistory previous) {
        this.dataChecksum = requireNonNull(dataChecksum);
        this.snapshots = List.copyOf(snapshots);
        this.previous = previous;
        this.undoDescriptions = Map.copyOf(undoDescriptions);
    }

    /**
     * Constructor for Jackson. Required fields are validated on read; legacy descriptions are optional.
     */
    private JsonUndoHistory() {
        dataChecksum = null;
        snapshots = null;
        previous = null;
        undoDescriptions = null;
    }

    /**
     * Older indexes remain usable without guessing which operation created their snapshots.
     */
    String getUndoDescription(String snapshot) {
        String description = undoDescriptions == null ? null : undoDescriptions.get(snapshot);
        return description == null || description.isBlank() ? UndoHistory.MESSAGE_LEGACY_SUCCESS : description;
    }

    /**
     * Selects the completed or uncommitted state without changing the saved collection.
     */
    Optional<JsonUndoHistory> findMatchingData(String checksum) {
        if (checksum.equals(dataChecksum)) {
            return Optional.of(this);
        }
        return previous != null && checksum.equals(previous.dataChecksum) ? Optional.of(previous) : Optional.empty();
    }

    /**
     * Validates metadata before resolving file names inside the owning history directory.
     */
    List<Path> resolveSnapshots(Path directory) throws IOException {
        if (dataChecksum == null || snapshots == null
                || !(dataChecksum.matches("[0-9a-f]{64}") || dataChecksum.equals("missing") && snapshots.isEmpty())
                || snapshots.size() > UndoSnapshotStorage.MAX_HISTORY_SIZE
                || new HashSet<>(snapshots).size() != snapshots.size()) {
            throw new IOException("Invalid undo history metadata");
        }
        for (String snapshot : snapshots) {
            if (snapshot == null || !snapshot.matches("undo-[A-Za-z0-9-]+\\.json")) {
                throw new IOException("Invalid undo snapshot file name");
            }
        }
        return snapshots.stream().map(directory::resolve).toList();
    }
}
