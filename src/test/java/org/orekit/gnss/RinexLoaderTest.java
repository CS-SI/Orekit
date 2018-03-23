package org.orekit.gnss;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

public class RinexLoaderTest {
    
    
    @Before
    public void setUp() throws OrekitException {
        // Sets the root of data to read
        Utils.setDataRoot("gnss:rinex");
    }

    @Test
    public void testGPSFile() throws OrekitException {

        //Tests Rinex 2 with only GPS Constellation
        RinexLoader  loader = new RinexLoader("^jnu10110\\.17o$");
        String[] typesobs = {"L1","L2","P1","P2","C1","S1","S2"};
        
        Assert.assertEquals(44, loader.getRinexObservations().size());

        checkObservation(loader.getRinexObservations().get(0),
                         2017, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 2, -0.03,
                         typesobs, "L1", 124458652.886, 4, 0);
        checkObservation(loader.getRinexObservations().get(0),
                         2017, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 2, -0.03,
                         typesobs, "P1", 0, 0, 0);
        checkObservation(loader.getRinexObservations().get(3),
                         2017, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 6, -0.03,
                         typesobs, "S2", 42.300, 4, 0);
        checkObservation(loader.getRinexObservations().get(11),
                         2017, 1, 11, 0, 0, 30, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 2, -0.08,
                         typesobs, "C1", 23688342.361, 4, 0);
        checkObservation(loader.getRinexObservations().get(23),
                         2017, 1, 11, 0, 1, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 3, 0,
                         typesobs, "P2", 25160656.959, 4, 0);
        checkObservation(loader.getRinexObservations().get(23),
                         2017, 1, 11, 0, 1, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 3, 0,
                         typesobs, "P1", 0, 0, 0);
        checkObservation(loader.getRinexObservations().get(43),
                         2017, 1, 11, 0, 1, 30, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 30, 0,
                         typesobs, "S1", 41.6, 4, 0);
        
    }

    @Test
    public void testGPSGlonassFile() throws OrekitException {
        //Tests Rinex 2 with GPS and GLONASS Constellations
        RinexLoader  loader2 = new RinexLoader("^aiub0000\\.00o$");
        String[] typesobs2 = {"P1","L1","L2","P2"};
        
        checkObservation(loader2.getRinexObservations().get(0),
                         2001, 3, 24, 13, 10, 36, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 12, -.123456789,
                         typesobs2, "P1", 23629347.915, 0, 0);
        checkObservation(loader2.getRinexObservations().get(1),
                         2001, 3, 24, 13, 10, 36, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 9, -.123456789,
                         typesobs2, "L1", -0.12, 0, 9);
        checkObservation(loader2.getRinexObservations().get(2),
                         2001, 3, 24, 13, 10, 36, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 6, -.123456789,
                         typesobs2, "P2", 20607605.848, 4, 4);
        checkObservation(loader2.getRinexObservations().get(3),
                         2001, 3, 24, 13, 10, 54, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 12, -.123456789,
                         typesobs2, "L2", -41981.375, 0, 0);
        checkObservation(loader2.getRinexObservations().get(6),
                         2001, 3, 24, 13, 10, 54, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GLONASS, 21, -.123456789,
                         typesobs2, "P1", 21345678.576, 0, 0);
        checkObservation(loader2.getRinexObservations().get(7),
                         2001, 3, 24, 13, 10, 54, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GLONASS, 22, -.123456789,
                         typesobs2, "P2", 0, 0, 0);
        checkObservation(loader2.getRinexObservations().get(23),
                         2001, 3, 24, 13, 14, 48, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 6, -.123456234,
                         typesobs2, "L1", 267583.678, 1, 7);
        
    }

    @Test
    public void testMultipleConstellationsFile() throws OrekitException {
        //Tests Rinex 3 with Multiple Constellations
        RinexLoader  loader3 = new RinexLoader("^aaaa0000\\.00o$");

        String[] typesobsG = {"C1C","L1C","S1C","C2W","L2W","S2W","C2X","L2X","S2X","C5X","L5X","S5X"};
        String[] typesobsR = {"C1C","L1C","S1C","C1P","L1P","S1P","C2C","L2C","S2C","C2P","L2P","S2P"};
        String[] typesobsE = {"C1X","L1X","S1X","C5X","L5X","S5X","C7X","L7X","S7X","C8X","L8X","S8X"};
        String[] typesobsC = {"C1I","L1I","S1I","C7I","L7I","S7I","C6I","L6I","S6I"};
        checkObservation(loader3.getRinexObservations().get(0),
                         2016, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GLONASS, 10, 0.0,
                         typesobsR, "C1C", 23544632.969, 0, 6);
        checkObservation(loader3.getRinexObservations().get(1),
                         2016, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 27, 0.0,
                         typesobsG, "C1C", 22399181.883, 0, 7);
        checkObservation(loader3.getRinexObservations().get(9),
                         2016, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 3, 0.0,
                         typesobsG, "S5X",         47.600, 0, 0);
        checkObservation(loader3.getRinexObservations().get(10),
                         2016, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GALILEO, 14, 0.0,
                         typesobsE, "L8X", 76221970.869, 0, 8);
        checkObservation(loader3.getRinexObservations().get(25),
                         2016, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.COMPASS, 12, 0.0,
                         typesobsC, "S7I", 31.100, 0, 0);
        checkObservation(loader3.getRinexObservations().get(25),
                         2016, 1, 11, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.COMPASS, 12, 0.0,
                         typesobsC, "S7I", 31.100, 0, 0);
        checkObservation(loader3.getRinexObservations().get(50),
                         2016, 1, 11, 0, 0, 15, TimeScalesFactory.getGPS(),
                         SatelliteSystem.COMPASS, 11, 0.0,
                         typesobsC, "C7I", 23697971.738, 0, 7);
        
    }

    @Test
    public void testMultipleConstellationsGlonassScaleFactorFile() throws OrekitException {
        //Tests Rinex 3 with Multiple Constellations and Scale Factor for some GLONASS Observations
        RinexLoader  loader4 = new RinexLoader("^bbbb0000\\.00o$");
        String[] typesobsG2 = {"C1C","L1C","S1C","C1W","S1W","C2W","L2W","S2W","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        String[] typesobsR2 = {"C1C","L1C","S1C","C2C","L2C","S2C"};
        String[] typesobsE2 = {"C1C","L1C","S1C","C6C","L6C","S6C","C5Q","L5Q","S5Q","C7Q","L7Q","S7Q","C8Q","L8Q","S8Q"};
        String[] typesobsS2 = {"C1C","L1C","S1C","C5I","L5I","S5I"};
        String[] typesobsC2 = {"C2I","L2I","S2I","C7I","L7I","S7I","C6I","L6I","S6I"};
        String[] typesobsJ2 = {"C1C","L1C","S1C","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        checkObservation(loader4.getRinexObservations().get(0),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 30, 0.0,
                         typesobsG2, "C1C", 20422534.056, 0, 8);
        checkObservation(loader4.getRinexObservations().get(2),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GLONASS, 10, 0.0,
                         typesobsR2, "S2C", 49.250, 0, 0);
        checkObservation(loader4.getRinexObservations().get(2),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GLONASS, 10, 0.0,
                         typesobsR2, "C1C", 19186.904493, 0, 9);
        checkObservation(loader4.getRinexObservations().get(7),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GALILEO, 5, 0.0,
                         typesobsE2, "L8Q", 103747111.324, 0, 8);
        checkObservation(loader4.getRinexObservations().get(13),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.COMPASS, 4, 0.0,
                         typesobsC2, "C7I", 41010665.465, 0, 5);
        checkObservation(loader4.getRinexObservations().get(13),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.COMPASS, 4, 0.0,
                         typesobsC2, "L2I", 0, 0, 0);
        checkObservation(loader4.getRinexObservations().get(12),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.SBAS, 138, 0.0,
                         typesobsS2, "C1C", 40430827.124, 0, 6);
        checkObservation(loader4.getRinexObservations().get(12),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.SBAS, 138, 0.0,
                         typesobsS2, "S5I", 39.750, 0, 0);
        checkObservation(loader4.getRinexObservations().get(34),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.QZSS, 193, 0.0,
                         typesobsJ2, "L2L", 168639076.823, 0, 6);
        checkObservation(loader4.getRinexObservations().get(32),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GLONASS, 2, 0.0,
                         typesobsR2, "S1C", 0.0445, 0, 0);
    }

    @Test
    public void testMultipleConstellationsGalileoScaleFactorFile() throws OrekitException {
        //Tests Rinex 3 with Multiple Constellations and Scale Factor for all GALILEO Observations
        RinexLoader  loader6 = new RinexLoader("^bbbb0000\\.01o$");
        String[] typesobsG4 = {"C1C","L1C","S1C","C1W","S1W","C2W","L2W","S2W","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        String[] typesobsR4 = {"C1C","L1C","S1C","C2C","L2C","S2C"};
        String[] typesobsE4 = {"C1C","L1C","S1C","C6C","L6C","S6C","C5Q","L5Q","S5Q","C7Q","L7Q","S7Q","C8Q","L8Q","S8Q"};
        String[] typesobsS4 = {"C1C","L1C","S1C","C5I","L5I","S5I"};
        String[] typesobsC4 = {"C2I","L2I","S2I","C7I","L7I","S7I","C6I","L6I","S6I"};
        String[] typesobsJ4 = {"C1C","L1C","S1C","C2L","L2L","S2L","C5Q","L5Q","S5Q"};
        checkObservation(loader6.getRinexObservations().get(0),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GPS, 30, 0.0,
                         typesobsG4, "C1C", 20422534.056, 0, 8);
        checkObservation(loader6.getRinexObservations().get(2),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GLONASS, 10, 0.0,
                         typesobsR4, "S2C", 49.250, 0, 0);
        checkObservation(loader6.getRinexObservations().get(2),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GLONASS, 10, 0.0,
                         typesobsR4, "C1C", 19186904.493, 0, 9);
        checkObservation(loader6.getRinexObservations().get(7),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GALILEO, 5, 0.0,
                         typesobsE4, "L8Q", 103747.111324, 0, 8);
        checkObservation(loader6.getRinexObservations().get(26),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GALILEO, 8, 0.0,
                         typesobsE4, "C1C", 23499.584944, 0, 7);
        checkObservation(loader6.getRinexObservations().get(26),
                         2018, 1, 29, 0, 0, 0, TimeScalesFactory.getGPS(),
                         SatelliteSystem.GALILEO, 8, 0.0,
                         typesobsE4, "S8Q", 0.051, 0, 0);

    }
    
    
    @Test
    public void testWrongLabel() {
        try {
            new RinexLoader("^unknown-label\\.00o$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(22, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("THIS IS NOT A RINEX LABEL", ((String) oe.getParts()[2]).substring(60).trim());
        }
    }
    
    @Test
    public void testMissingHeaderLabel() {
        try {
            //Test with RinexV3 Missing Label inside Header
            new RinexLoader("^missing-label\\.00o$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }
    
    
    @Test
    public void testUnknownSatelliteSystemHeader() throws OrekitException {
        try {
            //Test with RinexV3 Unknown Satellite System inside Header
            new RinexLoader("^unknown-satsystem\\.00o$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oe) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_SATELLITE_SYSTEM, oe.getSpecifier());
            Assert.assertEquals('Z', oe.getParts()[0]);
        }
    }
    
    @Test
    public void testInconsistentNumSatellites() throws OrekitException {
        try {
            //Test with RinexV3 inconsistent number of sats in an observation w/r to max sats in header
            new RinexLoader("^inconsistent-satsnum\\.00o$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INCONSISTENT_NUMBER_OF_SATS, oe.getSpecifier());
            Assert.assertEquals(25, oe.getParts()[3]); //N. max sats
            Assert.assertEquals(26, oe.getParts()[2]); //N. sats observation incoherent
        }
    }
    
    @Test
    public void testInconsistentSatSystem() throws OrekitException {
        try {
            //Test with RinexV3 inconsistent satellite system in an observation w/r to file sat system
            new RinexLoader("^inconsistent-satsystem\\.00o$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INCONSISTENT_SATELLITE_SYSTEM, oe.getSpecifier());
            Assert.assertEquals(SatelliteSystem.GPS, oe.getParts()[2]); //Rinex Satellite System (GPS)
            Assert.assertEquals(SatelliteSystem.GLONASS, oe.getParts()[3]); //First observation of a sat that is not GPS (GLONASS)
        }
    }
    
    @Test
    public void testUnknownFrequency() {
        try {
            new RinexLoader("^unknown-rinex-frequency\\.00o$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_RINEX_FREQUENCY, oe.getSpecifier());
            Assert.assertEquals("AAA", (String) oe.getParts()[0]);
            Assert.assertEquals(14, ((Integer) oe.getParts()[2]).intValue());
        }
    }
    
    
    private void checkObservation(final ObservationData obser,
                                  final int year, final int month, final int day,
                                  final int hour, final int minute, final double second,
                                  final TimeScale timescale,
                                  final SatelliteSystem system, final int prnNumber,
                                  final double rcvrClkOffset, final String[] typesObs,
                                  final String type, final double obsValue,
                                  final int lliValue, final int sigstrength) {

          final AbsoluteDate date = new AbsoluteDate(year, month, day, hour, minute, second,
                                                     timescale);
          
          Assert.assertEquals(system,         obser.getSatelliteSystem());
          Assert.assertEquals(prnNumber,      obser.getPrnNumber());
          Assert.assertEquals(date,           obser.getTObs());
          Assert.assertEquals(rcvrClkOffset,  obser.getRcvrClkOffset(), 1.E-17);
          for (int i = 0; i < typesObs.length; i++) {
              Assert.assertEquals(RinexFrequency.valueOf(typesObs[i]), obser.getTypesObs().get(i));
          }
          Assert.assertEquals(obsValue,       obser.getObs(type), 1.E-3);
          Assert.assertEquals(lliValue,       obser.getLli(type));
          Assert.assertEquals(sigstrength,    obser.getSignStrength(type));

      }

}
