package org.orekit.files.ccsds.definitions;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;

/**
 * Unit tests for {@link OrekitCcsdsFrameMapper}.
 *
 * @author Evan Ward
 */
public class OrekitCcsdsFrameMapperTest {

    /** Check that Earth-centered ICRF is GCRF, for #1914. */
    @Test
    public void testBuildCcsdsFrameEarthIcrfIsGcrf() {
        // setup
        CcsdsFrameMapper mapper = new OrekitCcsdsFrameMapper();
        DataContext context = Utils.newDataContext("regular-data");
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
}
