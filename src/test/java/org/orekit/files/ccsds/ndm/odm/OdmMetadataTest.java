package org.orekit.files.ccsds.ndm.odm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;
import org.orekit.files.ccsds.definitions.TimeSystem;

/**
 * Unit tests for {@link OdmMetadata}.
 *
 * @author Evan Ward
 */
public class OdmMetadataTest {

    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    @Deprecated
    public void testDeprecatedConstructor() {
        // action
        OdmMetadata actual = new OdmMetadata(TimeSystem.UTC);

        // verify
        MatcherAssert.assertThat(actual.getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
        MatcherAssert.assertThat(actual.getTimeSystem(),
                Matchers.is(TimeSystem.UTC));
    }

}
