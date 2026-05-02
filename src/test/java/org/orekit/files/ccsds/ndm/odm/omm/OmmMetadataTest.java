package org.orekit.files.ccsds.ndm.odm.omm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;

/**
 * Unit tests for {@link OmmMetadata}.
 *
 * @author Evan Ward
 */
class OmmMetadataTest {

    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    @Deprecated
    void testDeprecatedConstructor() {
        // action
        OmmMetadata actual = new OmmMetadata();

        // verify
        MatcherAssert.assertThat(actual.getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
    }

}
