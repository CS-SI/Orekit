package org.orekit.files.ccsds.section;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.odm.opm.Opm;

public class CommentsContainerTest {
    
    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testSetComments() {

        final String expectedOldComment  = "GEOCENTRIC, CARTESIAN, EARTH FIXED";
        final String expectedNewComment1 = "NEW COMMENT 1";
        final String expectedNewComment2 = "LAST NEW COMMENT!";

        // Parse OPM and check comments in metadata
        final String opmName = "/ccsds/odm/opm/OPMExample1.txt";
        final DataSource source1  = new DataSource(opmName, () -> getClass().getResourceAsStream(opmName));
        final Opm        original = new ParserBuilder().
                                    withParsedUnitsBehavior(ParsedUnitsBehavior.STRICT_COMPLIANCE).
                                    buildOpmParser().
                                    parseMessage(source1);

        Assertions.assertEquals(1, original.getMetadata().getComments().size());
        Assertions.assertEquals(expectedOldComment, original.getMetadata().getComments().get(0));

        // Set new comments and check
        List<String> newComments = new ArrayList<>();
        newComments.add(expectedNewComment1);
        newComments.add(expectedNewComment2);
        original.getMetadata().setComments(newComments);

        Assertions.assertEquals(2, original.getMetadata().getComments().size());
        Assertions.assertEquals(expectedNewComment1, original.getMetadata().getComments().get(0));
        Assertions.assertEquals(expectedNewComment2, original.getMetadata().getComments().get(1));
    }
}
