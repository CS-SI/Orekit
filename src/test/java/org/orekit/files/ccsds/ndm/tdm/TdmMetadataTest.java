package org.orekit.files.ccsds.ndm.tdm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;

/**
 * Unit tests for {@link TdmMetadata}.
 *
 * @author Evan Ward
 */
public class TdmMetadataTest {

    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    @Deprecated
    public void testDeprecatedConstructor() {
        // action
        TdmMetadata actual = new TdmMetadata();

        // verify
        MatcherAssert.assertThat(actual.getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
    }

}
