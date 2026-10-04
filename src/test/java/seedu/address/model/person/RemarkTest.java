package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class RemarkTest {
    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Remark(null));
    }

    @Test
    public void constructor_unconstrainedContent_preservesValue() {
        for (String value : new String[] {"", " ", "Likes baseball!", "备注\nSecond line"}) {
            assertEquals(value, new Remark(value).value);
            assertEquals(value, new Remark(value).toString());
        }
    }

    @Test
    public void equalsAndHashCode() {
        Remark remark = new Remark("note");
        assertEquals(remark, remark);
        assertEquals(remark, new Remark("note"));
        assertEquals(remark.hashCode(), new Remark("note").hashCode());
        assertNotEquals(remark, new Remark("other"));
        assertNotEquals(remark, null);
        assertNotEquals(remark, "note");
    }

    @Test
    public void person_differentRemark_changesEqualityButNotIdentity() {
        Person edited = new PersonBuilder(ALICE).withRemark("note").build();
        assertNotEquals(ALICE, edited);
        assertTrue(ALICE.isSamePerson(edited));
        assertEquals(edited, new PersonBuilder(edited).build());
        assertEquals(edited.hashCode(), new PersonBuilder(edited).build().hashCode());
    }
}
