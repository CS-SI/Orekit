package org.orekit.propagation.events;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/**
 * Unit tests for {@link NegateDetector}.
 *
 * @author Evan Ward
 */
public class NegateDetectorTest {

    /** check {@link NegateDetector#init(SpacecraftState, AbsoluteDate)}. */
    @Test
    public void testInit() {
        //setup
        EventDetector a = Mockito.mock(EventDetector.class);
        NegateDetector detector = new NegateDetector(a);
        AbsoluteDate t = AbsoluteDate.GPS_EPOCH;
        SpacecraftState s = Mockito.mock(SpacecraftState.class);
        Mockito.when(s.getDate()).thenReturn(t.shiftedBy(60.0));

        //action
        detector.init(s, t);

        //verify
        Mockito.verify(a).init(s, t);
    }

    /**
     * check g function is negated.
     *
     * @throws OrekitException on error
     */
    @Test
    public void testG() throws OrekitException {
        //setup
        EventDetector a = Mockito.mock(EventDetector.class);
        NegateDetector detector = new NegateDetector(a);
        SpacecraftState s = Mockito.mock(SpacecraftState.class);

        // verify + to -
        Mockito.when(a.g(s)).thenReturn(1.0);
        Assert.assertThat(detector.g(s), CoreMatchers.is(-1.0));
        // verify - to +
        Mockito.when(a.g(s)).thenReturn(-1.0);
        Assert.assertThat(detector.g(s), CoreMatchers.is(1.0));
    }

    /** Check a with___ method. */
    @Test
    public void testCreate() {
        //setup
        EventDetector a = Mockito.mock(EventDetector.class);
        NegateDetector detector = new NegateDetector(a);

        // action
        NegateDetector actual = detector.withMaxCheck(100);

        //verify
        Assert.assertThat(actual.getMaxCheckInterval(), CoreMatchers.is(100.0));
    }
}
