package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

/** Stores snapshot order and the checksum of the collection to which the history belongs. */
class JsonUndoHistory {
    private final String dataChecksum;
    private final List<String> snapshots;

    /** Creates metadata for a cleanly closed undo history, ordered from oldest to newest. */
    JsonUndoHistory(String dataChecksum, List<String> snapshots) {
        this.dataChecksum = requireNonNull(dataChecksum);
        this.snapshots = List.copyOf(snapshots);
    }

    /** Constructor for Jackson. Missing fields are rejected when the metadata is read. */
    private JsonUndoHistory() {
        dataChecksum = null;
        snapshots = null;
    }

    String getDataChecksum() {
        return dataChecksum;
    }

    /** Validates metadata before resolving file names inside the owning history directory. */
    List<Path> resolveSnapshots(Path directory) throws IOException {
        if (dataChecksum == null || !dataChecksum.matches("[0-9a-f]{64}") || snapshots == null
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
