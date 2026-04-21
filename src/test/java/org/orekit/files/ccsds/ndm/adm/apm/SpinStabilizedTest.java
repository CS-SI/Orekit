package org.orekit.files.ccsds.ndm.adm.apm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;

/**
 * Unit tests for {@link SpinStabilized}.
 *
 * @author Evan Ward
 */
public class SpinStabilizedTest {

    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    public void testDeprecatedConstructor() {
        // action
        SpinStabilized actual = new SpinStabilized();

        // verify
        MatcherAssert.assertThat(actual.getEndpoints().getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
    }

}
