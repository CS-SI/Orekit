package org.orekit.files.ccsds.section;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;
import org.orekit.files.ccsds.definitions.TimeSystem;


/**
 * Unit tests for {@link Metadata}.
 *
 * @author Evan Ward
 */
public class MetadataTest {

    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    public void testDeprecatedConstructor() {
        // action
        final Metadata actual = new Metadata(TimeSystem.UTC);

        // verify
        MatcherAssert.assertThat(actual.getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
        MatcherAssert.assertThat(actual.getTimeSystem(),
                Matchers.is(TimeSystem.UTC));
    }

}
