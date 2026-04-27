package org.orekit.files.ccsds.ndm.adm.acm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;

/**
 * Unit tests for {@link AttitudeDetermination}.
 *
 * @author Evan Ward
 */
public class AttitudeDeterminationTest {

    /** Test deprecated constructor. Can be removed in 14.0. */
    @Test
    public void testDeprecatedConstructor() {
        // action
        AttitudeDetermination actual = new AttitudeDetermination();

        // verify
        MatcherAssert.assertThat(actual.getEndpoints().getFrameMapper(),
                Matchers.is(new OrekitCcsdsFrameMapper()));
    }

}
