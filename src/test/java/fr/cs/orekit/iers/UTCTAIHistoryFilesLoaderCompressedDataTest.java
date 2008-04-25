package fr.cs.orekit.iers;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.time.UTCScale;

public class UTCTAIHistoryFilesLoaderCompressedDataTest extends TestCase {

    public void testCompressed() throws OrekitException {
        assertEquals(-32.0, UTCScale.getInstance().offsetFromTAI(946684800), 10e-8);
    }

    public void setUp() {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "compressed-data");
    }

    public static Test suite() {
        return new TestSuite(UTCTAIHistoryFilesLoaderCompressedDataTest.class);
    }

}
