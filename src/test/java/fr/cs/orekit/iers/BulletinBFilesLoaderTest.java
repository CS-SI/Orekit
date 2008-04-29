package fr.cs.orekit.iers;

import java.text.ParseException;

import fr.cs.orekit.errors.OrekitException;
import junit.framework.Test;
import junit.framework.TestSuite;

public class BulletinBFilesLoaderTest extends AbstractFilesLoaderTest {

    public void testMissingMonths() throws OrekitException {
        setRoot("missing-months");
        new BulletinBFilesLoader(eop).loadEOP();
        assertTrue(getMaxGap() > 5);
    }

    public void testStartDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        new BulletinBFilesLoader(eop).loadEOP();
        assertTrue(getMaxGap() < 5);
        assertEquals(53709, ((EarthOrientationParameters) eop.first()).getMjd());
    }

    public void testEndDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        new BulletinBFilesLoader(eop).loadEOP();
        assertTrue(getMaxGap() < 5);
        assertEquals(53799, ((EarthOrientationParameters) eop.last()).getMjd());
    }


    public static Test suite() {
        return new TestSuite(BulletinBFilesLoaderTest.class);
    }

}
