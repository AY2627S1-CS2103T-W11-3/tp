package seedu.address.ui;

/**
 * Adds a command to history and terminates without running normal JVM shutdown cleanup.
 */
public final class CommandHistoryAbnormalExitTestApp {

    static final int HALT_EXIT_CODE = 73;
    static final String COMMAND_ADDED_MARKER = "COMMAND_ADDED_TO_HISTORY";

    private CommandHistoryAbnormalExitTestApp() {
    }

    /**
     * Adds the command argument to history before halting the JVM.
     */
    public static void main(String[] args) {
        assert args.length == 1;

        CommandHistory commandHistory = new CommandHistory();
        commandHistory.add(args[0]);
        System.out.println(COMMAND_ADDED_MARKER);
        System.out.flush();
        Runtime.getRuntime().halt(HALT_EXIT_CODE);
    }
}
