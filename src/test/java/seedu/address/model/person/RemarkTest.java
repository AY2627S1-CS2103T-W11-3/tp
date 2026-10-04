package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class RemarkTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Remark(null));
    }

    @Test
    public void constructor_anyContent_success() {
        for (String value : new String[] {"", " ", "Likes baseball!", "备注"}) {
            assertEquals(value, new Remark(value).value);
        }
    }

    @Test
    public void equals() {
        Remark remark = new Remark("Likes baseball");

        assertTrue(remark.equals(remark));
        assertTrue(remark.equals(new Remark("Likes baseball")));
        assertFalse(remark.equals(new Remark("Likes swimming")));
        assertFalse(remark.equals(null));
        assertFalse(remark.equals("Likes baseball"));
    }

    @Test
    public void hashCode_sameValue_sameHashCode() {
        assertEquals(new Remark("Likes baseball").hashCode(), new Remark("Likes baseball").hashCode());
    }

    @Test
    public void toStringMethod() {
        String value = "Likes baseball";
        assertEquals(value, new Remark(value).toString());
    }
}
