package org.orekit.files.ccsds.definitions;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.time.AbsoluteDate;

/**
 * Unit tests for {@link OrekitCcsdsFrameMapper}.
 *
 * @author Evan Ward
 */
public class OrekitCcsdsFrameMapperTest {

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

}
