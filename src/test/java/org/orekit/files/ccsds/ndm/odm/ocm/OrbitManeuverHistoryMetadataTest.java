package org.orekit.files.ccsds.ndm.odm.ocm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;

/**
 * Unit tests for {@link OrbitManeuverHistoryMetadata}.
 *
 * @author Evan Ward
 */
public class OrbitManeuverHistoryMetadataTest {

    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    public void testDeprecatedConstructor() {
        // action
        OrbitManeuverHistoryMetadata actual =
                new OrbitManeuverHistoryMetadata(null);

        // verify
        MatcherAssert.assertThat(actual.getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
    }

}
