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
    public void constructor_anyString_preservesValue() {
        assertEquals("", new Remark("").value);
        assertEquals(" \t\n", new Remark(" \t\n").value);
        assertEquals("Likes swimming!", new Remark("Likes swimming!").value);
    }

    @Test
    public void equals() {
        Remark remark = new Remark("Likes swimming!");
        assertTrue(remark.equals(remark));
        assertTrue(remark.equals(new Remark("Likes swimming!")));
        assertFalse(remark.equals(null));
        assertFalse(remark.equals("Likes swimming!"));
        assertFalse(remark.equals(new Remark("")));
    }

    @Test
    public void hashCode_equalValues_sameHashCode() {
        assertEquals(new Remark("Likes swimming!").hashCode(), new Remark("Likes swimming!").hashCode());
    }

    @Test
    public void toString_returnsValue() {
        assertEquals("Likes swimming!", new Remark("Likes swimming!").toString());
    }
}
