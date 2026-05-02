package org.orekit.files.ccsds.ndm.odm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;

/**
 * Unit tests for {@link CartesianCovariance}.
 *
 * @author Evan Ward
 */
public class CartesianCovarianceTest {

    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    @Deprecated
    public void testDeprecatedConstructor() {
        // action
        CartesianCovariance actual = new CartesianCovariance(() -> null);

        // verify
        MatcherAssert.assertThat(actual.getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
    }

}
