package org.orekit.files.ccsds.ndm.adm.apm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;

/**
 * Unit tests for {@link Inertia}.
 *
 * @author Evan Ward
 */
public class InertiaTest {


    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    @Deprecated
    public void testDeprecatedConstructor() {
        // action
        Inertia actual = new Inertia();

        // verify
        MatcherAssert.assertThat(actual.getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
    }

}
