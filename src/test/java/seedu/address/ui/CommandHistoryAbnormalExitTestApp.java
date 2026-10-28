package seedu.address.ui;

/**
 * Adds a command to history and terminates without running normal JVM shutdown cleanup.
 */
public final class CommandHistoryAbnormalExitTestApp {

    private static final int ABNORMAL_EXIT_CODE = 1;

    private CommandHistoryAbnormalExitTestApp() {
    }

    /**
     * Adds the command argument to history before halting the JVM.
     */
    public static void main(String[] args) {
        assert args.length == 1;

        CommandHistory commandHistory = new CommandHistory();
        commandHistory.add(args[0]);
        Runtime.getRuntime().halt(ABNORMAL_EXIT_CODE);
    }
}
