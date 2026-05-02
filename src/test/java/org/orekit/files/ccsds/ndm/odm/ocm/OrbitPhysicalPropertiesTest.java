package org.orekit.files.ccsds.ndm.odm.ocm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;
import org.orekit.time.AbsoluteDate;

/**
 * Unit tests for {@link OrbitPhysicalProperties}.
 *
 * @author Evan Ward
 */
public class OrbitPhysicalPropertiesTest {

    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    @Deprecated
    public void testDeprecatedConstructor() {
        // setup
        AbsoluteDate epoch = AbsoluteDate.ARBITRARY_EPOCH;

        // action
        OrbitPhysicalProperties actual = new OrbitPhysicalProperties(epoch);

        // verify
        MatcherAssert.assertThat(actual.getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
    }

}
