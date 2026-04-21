package org.orekit.files.ccsds.ndm.adm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;

/**
 * Unit tests for {@link AdmMetadata}.
 *
 * @author Evan Ward
 */
public class AdmMetadataTest {

    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    public void testDeprecatedConstructor() {
        // action
        AdmMetadata actual = new AdmMetadata();

        // verify
        MatcherAssert.assertThat(actual.getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
    }

}
