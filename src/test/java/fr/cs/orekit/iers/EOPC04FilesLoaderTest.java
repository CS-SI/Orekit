package fr.cs.orekit.iers;

import java.text.ParseException;

import fr.cs.orekit.errors.OrekitException;
import junit.framework.Test;
import junit.framework.TestSuite;

public class EOPC04FilesLoaderTest extends AbstractFilesLoaderTest {

    public void testMissingMonths() throws OrekitException {
        setRoot("missing-months");
        new EOPC04FilesLoader(eop).loadEOP();
        assertTrue(getMaxGap() > 5);
    }

    public void testStartDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        new EOPC04FilesLoader(eop).loadEOP();
        assertEquals(52640, ((EarthOrientationParameters) eop.first()).mjd);
    }

    public void testEndDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        new EOPC04FilesLoader(eop).loadEOP();
        assertEquals(53735, ((EarthOrientationParameters) eop.last()).mjd);
    }

    public static Test suite() {
        return new TestSuite(EOPC04FilesLoaderTest.class);
    }

}
