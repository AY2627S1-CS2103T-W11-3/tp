package seedu.address.logic.parser;

import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.DeleteCommand;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;

/**
 * Tests delete argument validation and oversized indices through command execution.
 */
public class DeleteCommandParserTest {

    private final DeleteCommandParser parser = new DeleteCommandParser();

    @Test
    public void parse_validArgs_returnsDeleteCommand() {
        for (String input : new String[] {"1", " 1 ", "\t1\t", " \t1 \t"}) {
            assertParseSuccess(parser, input, new DeleteCommand(INDEX_FIRST_PERSON));
        }
    }

    @Test
    public void parse_missingArgs_throwsMissingIndex() {
        for (String input : new String[] {"", " ", "\t", " \t "}) {
            assertParseFailure(parser, input, DeleteCommand.MESSAGE_MISSING_INDEX);
        }
    }

    @Test
    public void parse_multipleArgs_throwsArgumentCountBeforeFormat() {
        for (String input : new String[] {"1 2", "1\t2", "abc 0", "-1 2", " 1 \t 2 "}) {
            assertParseFailure(parser, input, DeleteCommand.MESSAGE_MULTIPLE_INDICES);
        }
    }

    @Test
    public void parse_invalidIndex_throwsInvalidFormat() {
        for (String input : new String[] {"abc", "Sarah", "0", "-1", "+1", "01", "00", "1.0", "1e3",
            "١", "１", "1_000", "1\n2"}) {
            assertParseFailure(parser, input, DeleteCommand.MESSAGE_INVALID_INDEX);
        }
    }

    @Test
    public void parse_largeIndex_reachesRangeValidationWithoutMutation() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        for (String input : new String[] {"2147483647", "2147483648", "9223372036854775808", "9".repeat(1000)}) {
            assertParseSuccess(parser, input, new DeleteCommand(new BigInteger(input)));
            assertCommandFailure(parser.parse(input), model,
                    String.format(DeleteCommand.MESSAGE_INDEX_OUT_OF_RANGE, model.getFilteredPersonList().size()));
        }
    }

    @Test
    public void parse_largeIndexEmptyDisplay_reportsEmptyListBeforeRange() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        model.updateFilteredPersonList(person -> false);
        assertCommandFailure(parser.parse("9".repeat(1000)), model, DeleteCommand.MESSAGE_EMPTY_LIST);
        assertParseFailure(parser, "01", DeleteCommand.MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, "abc 2", DeleteCommand.MESSAGE_MULTIPLE_INDICES);
    }
}
