package org.orekit.files.general;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.files.general.EphemerisFile.EphemerisSegment;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Unit tests for {@link EphemerisSegmentPropagator}.
 *
 * @author Evan Ward
 */
public class EphemerisSegmentPropagatorTest {

    /** Set Orekit data. */
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }


    /**
     * Check {@link EphemerisSegmentPropagator} and {@link EphemerisSegment#getPropagator()}.
     *
     * @throws Exception on error.
     */
    @Test
    public void testPropagator() throws Exception {
        // setup
        AbsoluteDate start = AbsoluteDate.J2000_EPOCH, end = start.shiftedBy(60);
        Frame frame = FramesFactory.getEME2000();
        List<TimeStampedPVCoordinates> coordinates = Arrays.asList(
                new TimeStampedPVCoordinates(start, new Vector3D(6778137, 0, 0), new Vector3D(0, 7.5e3, 0)),
                new TimeStampedPVCoordinates(start.shiftedBy(30), new Vector3D(6778137 + 1, 0, 0), new Vector3D(0, 7.5e3, 0)),
                new TimeStampedPVCoordinates(end, new Vector3D(6778137 + 3, 0, 0), new Vector3D(0, 7.5e3, 0)));
        EphemerisSegment ephemeris = new EphemerisSegment() {
            @Override
            public double getMu() {
                return Constants.EGM96_EARTH_MU;
            }

            @Override
            public String getFrameString() {
                return null;
            }

            @Override
            public Frame getFrame() throws OrekitException {
                return frame;
            }

            @Override
            public String getTimeScaleString() {
                return null;
            }

            @Override
            public TimeScale getTimeScale() throws OrekitException {
                return null;
            }

            @Override
            public int getInterpolationSamples() {
                return 2;
            }

            @Override
            public CartesianDerivativesFilter getAvailableDerivatives() {
                return CartesianDerivativesFilter.USE_P;
            }

            @Override
            public List<TimeStampedPVCoordinates> getCoordinates() {
                return coordinates;
            }

            @Override
            public AbsoluteDate getStart() {
                return start;
            }

            @Override
            public AbsoluteDate getStop() {
                return end;
            }
        };

        // action
        BoundedPropagator propagator = ephemeris.getPropagator();

        //verify
        Assert.assertThat(propagator.getMinDate(), CoreMatchers.is(start));
        Assert.assertThat(propagator.getMaxDate(), CoreMatchers.is(end));
        int ulps = 0;
        PVCoordinates expected = new PVCoordinates(
                new Vector3D(6778137, 0, 0),
                new Vector3D(1.0 / 30, 0, 0));
        Assert.assertThat(
                propagator.propagate(start).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(expected, ulps));
        Assert.assertThat(
                propagator.getPVCoordinates(start, frame),
                OrekitMatchers.pvCloseTo(expected, ulps));
        expected = new PVCoordinates(
                new Vector3D(6778137 + 2, 0, 0),
                new Vector3D(2 / 30.0, 0, 0));
        Assert.assertThat(
                propagator.propagate(start.shiftedBy(45)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(expected, ulps));
        Assert.assertThat(
                propagator.getPVCoordinates(start.shiftedBy(45), frame),
                OrekitMatchers.pvCloseTo(expected, ulps));
        expected = new PVCoordinates(
                new Vector3D(6778137 + 3, 0, 0),
                new Vector3D(2 / 30.0, 0, 0));
        Assert.assertThat(
                propagator.propagate(end).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(expected, ulps));
        Assert.assertThat(
                propagator.getPVCoordinates(end, frame),
                OrekitMatchers.pvCloseTo(expected, ulps));

    }

}
