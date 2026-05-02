package org.orekit.files.ccsds.ndm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;

/**
 * Unit tests for {@link CommonPhysicalProperties}.
 *
 * @author Evan Ward
 */
class CommonPhysicalPropertiesTest {

    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    @Deprecated
    void testDeprecatedConstructor() {
        // action
        CommonPhysicalProperties actual = new CommonPhysicalProperties();

        // verify
        MatcherAssert.assertThat(actual.getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
    }

}
