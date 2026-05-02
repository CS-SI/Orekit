package org.orekit.files.ccsds.ndm.adm.apm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;

/**
 * Unit tests for {@link AngularVelocity}.
 *
 * @author Evan Ward
 */
public class AngularVelocityTest {

    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    @Deprecated
    public void testDeprecatedConstructor() {
        // action
        AngularVelocity actual = new AngularVelocity();

        // verify
        MatcherAssert.assertThat(actual.getEndpoints().getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
    }

}
