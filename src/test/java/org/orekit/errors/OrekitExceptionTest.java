package org.orekit.errors;

import org.hipparchus.exception.DummyLocalizable;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link OrekitException}.
 *
 * @author Evan Ward
 */
public class OrekitExceptionTest {

    /** Check {@link OrekitException#getMessage()} does not throw a NPE. */
    @Test
    public void testNullString() {
        // action
        OrekitException exception = new OrekitException(new DummyLocalizable(null));

        // verify
        Assert.assertEquals(exception.getMessage(), "");
    }

}
