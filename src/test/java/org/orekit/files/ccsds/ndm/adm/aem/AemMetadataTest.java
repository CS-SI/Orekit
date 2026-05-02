package org.orekit.files.ccsds.ndm.adm.aem;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;

/**
 * Unit tests for {@link AemMetadata}.
 *
 * @author Evan Ward
 */
public class AemMetadataTest {

    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    @Deprecated
    public void testDeprecatedConstructor() {
        // action
        AemMetadata actual = new AemMetadata(5);

        // verify
        MatcherAssert.assertThat(actual.getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
        MatcherAssert.assertThat(actual.getInterpolationDegree(),
                Matchers.is(5));
    }

}
