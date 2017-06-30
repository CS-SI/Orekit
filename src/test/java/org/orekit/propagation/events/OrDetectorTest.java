package org.orekit.propagation.events;

import java.util.Collections;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler.Action;
import org.orekit.time.AbsoluteDate;

/**
 * Unit tests for {@link BooleanDetector#orCombine(EventDetector...)}.
 *
 * @author Evan Ward
 */
public class OrDetectorTest {

    /** first operand. */
    private MockDetector a;
    /** second operand. */
    private MockDetector b;
    /** null */
    private SpacecraftState s;
    /** subject under test */
    private BooleanDetector or;

    /** create subject under test and dependencies. */
    @Before
    public void setUp() {
        a = new MockDetector();
        b = new MockDetector();
        s = null;
        or = BooleanDetector.orCombine(a, b);
    }

    /**
     * check {@link BooleanDetector#g(SpacecraftState)}.
     *
     * @throws OrekitException on error
     */
    @Test
    public void testG() throws OrekitException {
        // test zero cases
        a.g = b.g = 0.0;
        Assert.assertEquals(0.0, or.g(s), 0);
        a.g = -1;
        b.g = 0;
        Assert.assertEquals(0.0, or.g(s), 0);
        a.g = 0;
        b.g = -1;
        Assert.assertEquals(0.0, or.g(s), 0);

        // test negative cases
        a.g = -1;
        b.g = -1;
        Assert.assertTrue("negative", or.g(s) < 0);

        // test positive cases
        a.g = 0;
        b.g = 1;
        Assert.assertTrue("positive", or.g(s) > 0);
        a.g = 1;
        b.g = -1;
        Assert.assertTrue("positive", or.g(s) > 0);
        a.g = 1;
        b.g = 0;
        Assert.assertTrue("positive", or.g(s) > 0);
        a.g = -1;
        b.g = 1;
        Assert.assertTrue("positive", or.g(s) > 0);
        a.g = 1;
        b.g = 1;
        Assert.assertTrue("positive", or.g(s) > 0);

    }

    /**
     * check when there is numeric cancellation between the two g values.
     *
     * @throws OrekitException on error.
     */
    @Test
    public void testCancellation() throws OrekitException {
        a.g = -1e-10;
        b.g = -1e10;
        Assert.assertTrue("negative", or.g(s) < 0);
        a.g = -1e10;
        b.g = -1e-10;
        Assert.assertTrue("negative", or.g(s) < 0);
        a.g = -1e10;
        b.g = 1e-10;
        Assert.assertTrue("positive", or.g(s) > 0);
        a.g = 1e-10;
        b.g = -1e10;
        Assert.assertTrue("positive", or.g(s) > 0);
        a.g = 1e10;
        b.g = -1e-10;
        Assert.assertTrue("positive", or.g(s) > 0);
        a.g = -1e-10;
        b.g = 1e10;
        Assert.assertTrue("positive", or.g(s) > 0);
    }

    /** Check wrapped detectors are initialized. */
    @Test
    public void testInit() {
        // setup
        EventDetector a = Mockito.mock(EventDetector.class);
        EventDetector b = Mockito.mock(EventDetector.class);
        BooleanDetector or = BooleanDetector.orCombine(a, b);
        AbsoluteDate t = AbsoluteDate.CCSDS_EPOCH;
        s = Mockito.mock(SpacecraftState.class);
        Mockito.when(s.getDate()).thenReturn(t.shiftedBy(60.0));

        // action
        or.init(s, t);

        // verify
        Mockito.verify(a).init(s, t);
        Mockito.verify(b).init(s, t);
    }

    /** check when no operands are passed to the constructor. */
    @Test
    public void testZeroDetectors() {
        // action
        try {
            BooleanDetector.orCombine(Collections.emptyList());
            Assert.fail("Expected Exception");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    /** Mock detector to set the g function to arbitrary values. */
    private static class MockDetector implements EventDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = 1L;

        /** value to return from {@link #g(SpacecraftState)}. */
        public double g = 0;

        @Override
        public void init(SpacecraftState s0, AbsoluteDate t) {

        }

        @Override
        public double g(SpacecraftState s) throws OrekitException {
            return this.g;
        }

        @Override
        public double getThreshold() {
            return 0;
        }

        @Override
        public double getMaxCheckInterval() {
            return 0;
        }

        @Override
        public int getMaxIterationCount() {
            return 0;
        }

        @Override
        public Action eventOccurred(SpacecraftState s, boolean increasing) throws OrekitException {
            return null;
        }

        @Override
        public SpacecraftState resetState(SpacecraftState oldState) throws OrekitException {
            return null;
        }
    }
}
