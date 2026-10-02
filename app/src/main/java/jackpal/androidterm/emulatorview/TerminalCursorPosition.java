package jackpal.androidterm.emulatorview;

/**
 * Package bridge for the emulator's cursor, without reflection or a library fork: tells whether
 * the last transcript row is still open (the cursor sits inside it, as after a prompt) or closed
 * (the cursor already moved to the start of the next row), so a replayed transcript can be joined
 * with the live output stream without gluing two lines together.
 */
public final class TerminalCursorPosition {

    private TerminalCursorPosition() {
    }

    /** The cursor column, or -1 before the emulator exists. */
    public static int column(TermSession session) {
        TerminalEmulator emulator = session.getEmulator();
        return emulator == null ? -1 : emulator.getCursorCol();
    }
}
