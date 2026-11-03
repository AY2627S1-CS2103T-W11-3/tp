package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.TypicalPersons;

public class CommandResultTest {
    @Test
    public void personToView_optionalPayload() {
        CommandResult result = new CommandResult("Viewed person", TypicalPersons.ALICE);
        assertEquals(TypicalPersons.ALICE, result.getPersonToView().orElseThrow());
        assertTrue(new CommandResult("feedback").getPersonToView().isEmpty());
        assertEquals(result, new CommandResult("Viewed person", TypicalPersons.ALICE));
        assertEquals(result.hashCode(), new CommandResult("Viewed person", TypicalPersons.ALICE).hashCode());
        assertNotEquals(result, new CommandResult("Viewed person"));
        assertNotEquals(result, new CommandResult("Viewed person", TypicalPersons.BOB));
    }

    @Test
    public void equals() {
        CommandResult commandResult = new CommandResult("feedback");

        // same values -> returns true
        assertTrue(commandResult.equals(new CommandResult("feedback")));
        assertTrue(commandResult.equals(new CommandResult("feedback", false, false)));

        // same object -> returns true
        assertTrue(commandResult.equals(commandResult));

        // null -> returns false
        assertFalse(commandResult.equals(null));

        // different types -> returns false
        assertFalse(commandResult.equals(0.5f));

        // different feedbackToUser value -> returns false
        assertFalse(commandResult.equals(new CommandResult("different")));

        // different showHelp value -> returns false
        assertFalse(commandResult.equals(new CommandResult("feedback", true, false)));

        // different exit value -> returns false
        assertFalse(commandResult.equals(new CommandResult("feedback", false, true)));

        // different highlighted index -> returns false
        assertFalse(commandResult.equals(CommandResult.withHighlight("feedback", 0)));
        assertFalse(CommandResult.withHighlight("feedback", 0).equals(CommandResult.withHighlight("feedback", 1)));
        assertTrue(CommandResult.withHighlight("feedback", 1).equals(CommandResult.withHighlight("feedback", 1)));
    }

    @Test
    public void getHighlightedIndex() {
        assertTrue(new CommandResult("feedback").getHighlightedIndex().isEmpty());
        assertEquals(3, CommandResult.withHighlight("feedback", 3).getHighlightedIndex().getAsInt());
        assertThrows(IllegalArgumentException.class, () -> CommandResult.withHighlight("feedback", -1));
    }

    @Test
    public void hashcode() {
        CommandResult commandResult = new CommandResult("feedback");

        // same values -> returns same hashcode
        assertEquals(commandResult.hashCode(), new CommandResult("feedback").hashCode());

        // different feedbackToUser value -> returns different hashcode
        assertNotEquals(commandResult.hashCode(), new CommandResult("different").hashCode());

        // different showHelp value -> returns different hashcode
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", true, false).hashCode());

        // different exit value -> returns different hashcode
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", false, true).hashCode());
    }

    @Test
    public void toStringMethod() {
        CommandResult commandResult = new CommandResult("feedback");
        String expected = CommandResult.class.getCanonicalName() + "{feedbackToUser="
                + commandResult.getFeedbackToUser() + ", showHelp=" + commandResult.isShowHelp()
                + ", exit=" + commandResult.isExit() + ", highlightedIndex=-1}";
        assertEquals(expected, commandResult.toString());
    }
}
