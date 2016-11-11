package org.orekit.propagation.analytical;

import java.util.Arrays;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/**
 * Tests for {@link AggregateBoundedPropagator}.
 *
 * @author Evan Ward
 */
public class AggregateBoundedPropagatorTest {

    public static final Frame frame = FramesFactory.getGCRF();

    /** Set Orekit data. */
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Check {@link AggregateBoundedPropagator#propagateOrbit(AbsoluteDate)} when the
     * constituent propagators are exactly adjacent.
     *
     * @throws Exception on error.
     */
    @Test
    public void testAdjacent() throws Exception {
        // setup
        AbsoluteDate date = AbsoluteDate.CCSDS_EPOCH;
        BoundedPropagator p1 = createPropagator(date, date.shiftedBy(10), 0);
        BoundedPropagator p2 = createPropagator(date.shiftedBy(10), date.shiftedBy(20), 1);

        // action
        BoundedPropagator actual = new AggregateBoundedPropagator(Arrays.asList(p1, p2));

        //verify
        int ulps = 0;
        Assert.assertThat(actual.getMinDate(), CoreMatchers.is(date));
        Assert.assertThat(actual.getMaxDate(), CoreMatchers.is(date.shiftedBy(20)));
        Assert.assertThat(
                actual.propagate(date).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p1.propagate(date).getPVCoordinates(), ulps));
        Assert.assertThat(
                actual.propagate(date.shiftedBy(5)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p1.propagate(date.shiftedBy(5)).getPVCoordinates(), ulps));
        Assert.assertThat(
                actual.propagate(date.shiftedBy(10)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(10)).getPVCoordinates(), ulps));
        Assert.assertThat(
                actual.propagate(date.shiftedBy(15)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(15)).getPVCoordinates(), ulps));
        Assert.assertThat(
                actual.propagate(date.shiftedBy(20)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(20)).getPVCoordinates(), ulps));
    }

    /**
     * Check {@link AggregateBoundedPropagator#propagateOrbit(AbsoluteDate)} when the
     * constituent propagators overlap.
     *
     * @throws Exception on error.
     */
    @Test
    public void testOverlap() throws Exception {
        // setup
        AbsoluteDate date = AbsoluteDate.CCSDS_EPOCH;
        BoundedPropagator p1 = createPropagator(date, date.shiftedBy(25), 0);
        BoundedPropagator p2 = createPropagator(date.shiftedBy(10), date.shiftedBy(20), 1);

        // action
        BoundedPropagator actual = new AggregateBoundedPropagator(Arrays.asList(p1, p2));

        //verify
        int ulps = 0;
        Assert.assertThat(actual.getMinDate(), CoreMatchers.is(date));
        Assert.assertThat(actual.getMaxDate(), CoreMatchers.is(date.shiftedBy(20)));
        Assert.assertThat(
                actual.propagate(date).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p1.propagate(date).getPVCoordinates(), ulps));
        Assert.assertThat(
                actual.propagate(date.shiftedBy(5)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p1.propagate(date.shiftedBy(5)).getPVCoordinates(), ulps));
        Assert.assertThat(
                actual.propagate(date.shiftedBy(10)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(10)).getPVCoordinates(), ulps));
        Assert.assertThat(
                actual.propagate(date.shiftedBy(15)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(15)).getPVCoordinates(), ulps));
        Assert.assertThat(
                actual.propagate(date.shiftedBy(20)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(20)).getPVCoordinates(), ulps));
    }

    /**
     * Check {@link AggregateBoundedPropagator#propagateOrbit(AbsoluteDate)} with a gap
     * between the constituent propagators.
     *
     * @throws Exception on error.
     */
    @Test
    public void testGap() throws Exception {
        // setup
        AbsoluteDate date = AbsoluteDate.CCSDS_EPOCH;
        BoundedPropagator p1 = createPropagator(date, date.shiftedBy(1), 0);
        BoundedPropagator p2 = createPropagator(date.shiftedBy(10), date.shiftedBy(20), 1);

        // action
        BoundedPropagator actual = new AggregateBoundedPropagator(Arrays.asList(p1, p2));

        //verify
        int ulps = 0;
        Assert.assertThat(actual.getMinDate(), CoreMatchers.is(date));
        Assert.assertThat(actual.getMaxDate(), CoreMatchers.is(date.shiftedBy(20)));
        Assert.assertThat(
                actual.propagate(date).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p1.propagate(date).getPVCoordinates(), ulps));
        Assert.assertThat(
                actual.propagate(date.shiftedBy(10)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(10)).getPVCoordinates(), ulps));
        Assert.assertThat(
                actual.propagate(date.shiftedBy(15)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(15)).getPVCoordinates(), ulps));
        Assert.assertThat(
                actual.propagate(date.shiftedBy(20)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(20)).getPVCoordinates(), ulps));
        try {
            // may or may not throw an exception depending on the type of propagator.
            Assert.assertThat(
                    actual.propagate(date.shiftedBy(5)).getPVCoordinates(),
                    OrekitMatchers.pvCloseTo(p1.propagate(date.shiftedBy(5)).getPVCoordinates(), ulps));
        } catch (OrekitException e) {
            // expected
        }
    }

    @Test
    public void testOutsideBounds() throws Exception {
        // setup
        AbsoluteDate date = AbsoluteDate.CCSDS_EPOCH;
        BoundedPropagator p1 = createPropagator(date, date.shiftedBy(10), 0);
        BoundedPropagator p2 = createPropagator(date.shiftedBy(10), date.shiftedBy(20), 1);

        // action
        BoundedPropagator actual = new AggregateBoundedPropagator(Arrays.asList(p1, p2));

        // verify
        int ulps = 0;
        // before bound of first propagator
        try {
            // may or may not throw an exception depending on the type of propagator.
            Assert.assertThat(
                    actual.propagate(date.shiftedBy(-60)).getPVCoordinates(),
                    OrekitMatchers.pvCloseTo(p1.propagate(date.shiftedBy(-60)).getPVCoordinates(), ulps));
        } catch (OrekitException e) {
            // expected
        }
        try {
            // may or may not throw an exception depending on the type of propagator.
            Assert.assertThat(
                    actual.getPVCoordinates(date.shiftedBy(-60), frame),
                    OrekitMatchers.pvCloseTo(p1.propagate(date.shiftedBy(-60)).getPVCoordinates(), ulps));
        } catch (OrekitException e) {
            // expected
        }
        // after bound of last propagator
        try {
            // may or may not throw an exception depending on the type of propagator.
            Assert.assertThat(
                    actual.propagate(date.shiftedBy(60)).getPVCoordinates(),
                    OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(60)).getPVCoordinates(), ulps));
        } catch (OrekitException e) {
            // expected
        }
        try {
            // may or may not throw an exception depending on the type of propagator.
            Assert.assertThat(
                    actual.getPVCoordinates(date.shiftedBy(60), frame),
                    OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(60)).getPVCoordinates(), ulps));
        } catch (OrekitException e) {
            // expected
        }

    }

    /**
     * Create a propagator with the given dates.
     *
     * @param start date.
     * @param end   date.
     * @param v     true anomaly.
     * @return a bound propagator with the given dates.
     * @throws OrekitException on error.
     */
    private BoundedPropagator createPropagator(AbsoluteDate start,
                                               AbsoluteDate end,
                                               double v) throws OrekitException {
        double gm = Constants.EGM96_EARTH_MU;
        KeplerianPropagator propagator = new KeplerianPropagator(new KeplerianOrbit(
                6778137, 0, 0, 0, 0, v, PositionAngle.TRUE, frame, start, gm));
        propagator.setEphemerisMode();
        propagator.propagate(start, end);
        return propagator.getGeneratedEphemeris();
    }

}
