package org.orekit.files.ccsds.ndm.odm.ocm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;

/**
 * Unit tests for {@link OrbitCovarianceHistoryMetadata}.
 *
 * @author Evan Ward
 */
public class OrbitCovarianceHistoryMetadataTest {

    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    public void testDeprecatedConstructor() {
        // action
        OrbitCovarianceHistoryMetadata actual =
                new OrbitCovarianceHistoryMetadata(null);

        // verify
        MatcherAssert.assertThat(actual.getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
    }

}
