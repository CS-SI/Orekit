package org.orekit.files.ccsds.definitions;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/**
 * Unit tests for {@link OrekitCcsdsFrameMapper}.
 *
 * @author Evan Ward
 */
public class OrekitCcsdsFrameMapperTest {

    /** Data context for these tests. */
    private final DataContext context = Utils.newDataContext("regular-data");

    /** Check that Earth-centered ICRF is GCRF, for #1914. */
    @Test
    public void testBuildCcsdsFrameEarthIcrfIsGcrf() {
        // setup
        CcsdsFrameMapper mapper = new OrekitCcsdsFrameMapper();
        final Frames frames = context.getFrames();
        final Frame icrf = frames.getICRF();
        final Frame gcrf = frames.getGCRF();
        final CelestialBody earth = context.getCelestialBodies().getEarth();

        // action
        final Frame actual = mapper.buildCcsdsFrame(
                new BodyFacade("EARTH", earth),
                new FrameFacade(icrf, CelestialBodyFrame.ICRF, null, null, "ICRF"),
                null);

        // verify
        MatcherAssert.assertThat(actual, Matchers.sameInstance(gcrf));
    }

    /** Check building a translated ICRF frame. */
    @Test
    public void testBuildCcsdsFrameIcrfNotSsbOrEarth() {
        // setup
        CcsdsFrameMapper mapper = new OrekitCcsdsFrameMapper();
        final Frames frames = context.getFrames();
        final Frame icrf = frames.getICRF();
        final CelestialBody emb = context.getCelestialBodies()
                .getEarthMoonBarycenter();
        final AbsoluteDate date = context.getTimeScales().getJ2000Epoch();

        // action
        final Frame actual = mapper.buildCcsdsFrame(
                new BodyFacade(CelestialBodyFactory.EARTH_MOON, emb),
                new FrameFacade(icrf, CelestialBodyFrame.ICRF, null, null, "ICRF"),
                null);

        // verify - EMB centered ICRF
        Transform actualTransform = actual.getTransformTo(icrf, date);
        MatcherAssert.assertThat(actualTransform.getRotation(),
                OrekitMatchers.distanceIs(Rotation.IDENTITY, Matchers.closeTo(0, 0.0)));
        PVCoordinates actualPv = emb.getPVCoordinates(date, actual);
        MatcherAssert.assertThat(actualPv,
                OrekitMatchers.pvCloseTo(PVCoordinates.ZERO, 0.0));
    }

    /**
     * Check
     * {@link OrekitCcsdsFrameMapper#buildCcsdsFrame(FrameFacade,
     * AbsoluteDate)}.
     */
    @Test
    public void testBuildOrientationOnly() {
        // setup
        CcsdsFrameMapper mapper = new OrekitCcsdsFrameMapper();
        Frame frame = context.getFrames().getVeis1950();
        FrameFacade orientation =
                new FrameFacade(frame, null, null, null, null);

        // action
        final Frame actual = mapper.buildCcsdsFrame(orientation, null);

        // verify
        // just check it returns frame.
        MatcherAssert.assertThat(actual, Matchers.sameInstance(frame));

        // check error cases
        try {
            mapper.buildCcsdsFrame(null, null);
            Assertions.fail("Expected exception");
        } catch (OrekitException e) {
            // expected
        }

        final String name = "Bill";
        orientation = new FrameFacade(null, null, null, null, name);
        try {
            mapper.buildCcsdsFrame(orientation, null);
            Assertions.fail("Expected exception");
        } catch (OrekitException e) {
            // expected
            MatcherAssert.assertThat(e.getMessage(),
                    Matchers.containsString(name));
        }
    }

    /** Exception (not NPE) for a null center. */
    @Test
    public void testBuildNullCenter() {
        // setup
        CcsdsFrameMapper mapper = new OrekitCcsdsFrameMapper();

        // action
        try {
            mapper.buildCcsdsFrame(null, null, null);
            Assertions.fail("Expected exception");
        } catch (OrekitException e) {
            // expected
        }
    }

    /**
     * Check stateless equals implementation, for #2016
     * {@link OrekitCcsdsFrameMapper#equals(Object)}
     */
    @Test
    public void testEquals() {
        OrekitCcsdsFrameMapper first = new OrekitCcsdsFrameMapper();
        OrekitCcsdsFrameMapper second = new OrekitCcsdsFrameMapper();
        OrekitCcsdsFrameMapperSubClass subclass = new OrekitCcsdsFrameMapperSubClass();

        // Invoke equals directly so an assertion's identity shortcut cannot bypass it
        Assertions.assertTrue(first.equals(first));

        Assertions.assertEquals(first, second);
        Assertions.assertEquals(second, first);
        Assertions.assertEquals(first.hashCode(), second.hashCode());

        // Call the mapper's equals directly: assertNotEquals(new Object(), first)
        // would call Object.equals instead and would not test the mapper's implementation
        Assertions.assertFalse(first.equals(new Object()));

        Assertions.assertNotEquals(first, subclass);
        Assertions.assertNotEquals(subclass, first);
    }

    /**
     * Check null case not equal, for #2016
     * {@link OrekitCcsdsFrameMapper#equals(Object)}
     */
    @Test
    public void testEqualsNull() {
        OrekitCcsdsFrameMapper mapper = new OrekitCcsdsFrameMapper();

        // Call equals directly: assertNotEquals(null, mapper) only checks that mapper
        // is non-null and would pass even if mapper.equals(null) threw an exception
        Assertions.assertFalse(mapper.equals(null));
    }

    /**
     * Check same subclass equal, for #2016
     * {@link OrekitCcsdsFrameMapper#equals(Object)}
     */
    @Test
    public void testEqualsSameSubclass() {
        OrekitCcsdsFrameMapperSubClass first = new OrekitCcsdsFrameMapperSubClass();
        OrekitCcsdsFrameMapperSubClass second = new OrekitCcsdsFrameMapperSubClass();

        // Invoke equals directly so an assertion's identity shortcut cannot bypass it
        Assertions.assertTrue(first.equals(first));

        Assertions.assertEquals(first, second);
        Assertions.assertEquals(second, first);
        Assertions.assertEquals(first.hashCode(), second.hashCode());
    }

    /**
     * Check that instances of different subclasses are unequal in both directions, for #2016
     * {@link OrekitCcsdsFrameMapper#equals(Object)}
     */
    @Test
    public void testEqualsDifferentSubclasses() {
        OrekitCcsdsFrameMapperSubClass first = new OrekitCcsdsFrameMapperSubClass();
        OtherOrekitCcsdsFrameMapperSubClass second = new OtherOrekitCcsdsFrameMapperSubClass();

        Assertions.assertNotEquals(first, second);
        Assertions.assertNotEquals(second, first);
    }

    /** Example stateless subclass used to test inherited equality */
    private static class OrekitCcsdsFrameMapperSubClass extends OrekitCcsdsFrameMapper {
        // No definition
    }

    /** A distinct stateless subclass used to test equality across runtime types */
    private static class OtherOrekitCcsdsFrameMapperSubClass extends OrekitCcsdsFrameMapper {
        // No definition
    }

}
