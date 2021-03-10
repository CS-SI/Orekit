/* Copyright 2002-2021 CS GROUP
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.files.ccsds.ndm.odm.ocm;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.ElementsType;
import org.orekit.files.ccsds.definitions.OdMethodType;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

public class OCMParserTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = "/ccsds/odm/ocm/OCMExample1.txt";
        final String wrongName = realName + "xxxxx";
        final DataSource source = new DataSource(wrongName, () -> getClass().getResourceAsStream(wrongName));
        try {
            new ParserBuilder().
            withMu(Constants.EIGEN5C_EARTH_MU).
            buildOcmParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingT0() throws URISyntaxException {
        final String name = "/ccsds/odm/ocm/OCM-missing-t0.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(Constants.EIGEN5C_EARTH_MU).
            buildOcmParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assert.assertEquals(OcmMetadataKey.EPOCH_TZERO.name(), oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingManeuverTime() throws URISyntaxException {
        final String name = "/ccsds/odm/ocm/OCM-missing-maneuver-time.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(Constants.EIGEN5C_EARTH_MU).
            buildOcmParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_MANEUVER_MISSING_TIME, oe.getSpecifier());
            Assert.assertEquals("MAN-45", oe.getParts()[0]);
        }
    }

    @Test
    public void testWrongTimeSpan() throws URISyntaxException {
        final String name = "/ccsds/odm/ocm/OCM-wrong-time-span.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(Constants.EIGEN5C_EARTH_MU).
            buildOcmParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("TIME_SPAN", oe.getParts()[0]);
            Assert.assertEquals(11, ((Integer) oe.getParts()[1]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).endsWith("OCM-wrong-time-span.txt"));
        }
    }

    @Test
    public void testSpuriousMetaDataSection() throws URISyntaxException {
        final String name = "/ccsds/odm/ocm/OCM-spurious-metadata-section.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(Constants.EIGEN5C_EARTH_MU).
            buildOcmParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(13, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("META", oe.getParts()[2]);
        }
    }

    @Test
    public void testIncompatibleUnitsDimension() throws URISyntaxException {
        final String name = "/ccsds/odm/ocm/OCM-incompatible-units-dimension.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(Constants.EIGEN5C_EARTH_MU).
            buildOcmParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INCOMPATIBLE_UNITS, oe.getSpecifier());
            Assert.assertEquals("km²/s", oe.getParts()[0]);
            Assert.assertEquals("m", oe.getParts()[1]);
        }
    }

    @Test
    public void testIncompatibleUnitsScale() throws URISyntaxException {
        final String name = "/ccsds/odm/ocm/OCM-incompatible-units-scale.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(Constants.EIGEN5C_EARTH_MU).
            buildOcmParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INCOMPATIBLE_UNITS, oe.getSpecifier());
            Assert.assertEquals("km", oe.getParts()[0]);
            Assert.assertEquals("m", oe.getParts()[1]);
        }
    }

    @Test
    public void testWrongNbElements() throws URISyntaxException {
        final String name = "/ccsds/odm/ocm/OCM-wrong-nb-elements.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(Constants.EIGEN5C_EARTH_MU).
            buildOcmParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_ELEMENT_SET_WRONG_NB_COMPONENTS, oe.getSpecifier());
            Assert.assertEquals(ElementsType.CARTP.name(), oe.getParts()[0]);
            Assert.assertEquals(ElementsType.CARTP.toString(), oe.getParts()[1]);
            Assert.assertEquals(3, ((Integer) oe.getParts()[2]).intValue());
        }
    }

    @Test
    public void testUnknownFrame() throws URISyntaxException {
        final String name = "/ccsds/odm/ocm/OCM-unknown-frame.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final OcmFile    ocm    = new ParserBuilder().
                                  withMu(Constants.EIGEN5C_EARTH_MU).
                                  buildOcmParser().
                                  parseMessage(source);
        Assert.assertEquals("CS GROUP", ocm.getHeader().getOriginator());
        Assert.assertEquals("728b0d2a-01fc-4d0e-9f0a-370c6930ea84", ocm.getHeader().getMessageId().toLowerCase(Locale.US));
        final OrbitStateHistory h = ocm.getData().getOrbitBlocks().get(0);
        Assert.assertEquals("ZZRF", h.getMetadata().getOrbReferenceFrame().getName());
        List<TimeStampedPVCoordinates> l = h.getCoordinates();
        Assert.assertEquals( 3.0e6, l.get(0).getPosition().getX(), 1.0e-9);
        Assert.assertEquals( 4.0e6, l.get(0).getPosition().getY(), 1.0e-9);
        Assert.assertEquals( 5.0e6, l.get(0).getPosition().getZ(), 1.0e-9);
        Assert.assertEquals(-1.0e3, l.get(0).getVelocity().getX(), 1.0e-12);
        Assert.assertEquals(-2.0e3, l.get(0).getVelocity().getY(), 1.0e-12);
        Assert.assertEquals(-3.0e3, l.get(0).getVelocity().getZ(), 1.0e-1);
        try {
            ocm.getData().getOrbitBlocks().get(0).getFrame();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_INVALID_FRAME, oe.getSpecifier());
            Assert.assertEquals("ZZRF", oe.getParts()[0]);
        }
    }

    @Test
    public void testUserDefined() throws URISyntaxException {
        final String name = "/ccsds/odm/ocm/OCM-user-defined.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final OcmFile    ocm    = new ParserBuilder().
                                  withMu(Constants.EIGEN5C_EARTH_MU).
                                  buildOcmParser().
                                  parseMessage(source);
        Assert.assertEquals("CS GROUP", ocm.getHeader().getOriginator());
        Assert.assertEquals("b77d785c-f7a8-4a80-a9b1-a540eac19d7a", ocm.getHeader().getMessageId().toLowerCase(Locale.US));
        Assert.assertNull(ocm.getData().getOrbitBlocks());
        Assert.assertEquals(1, ocm.getData().getUserDefinedBlock().getComments().size());
        Assert.assertEquals("some user data", ocm.getData().getUserDefinedBlock().getComments().get(0));
        Assert.assertEquals(1, ocm.getData().getUserDefinedBlock().getParameters().size());
        Assert.assertEquals("OREKIT", ocm.getData().getUserDefinedBlock().getParameters().get("GENERATOR"));
    }

    @Test
    public void testParseOCM1() {
        final String   name  = "/ccsds/odm/ocm/OCMExample1.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final OcmFile file = new ParserBuilder().
                             withMu(Constants.EIGEN5C_EARTH_MU).
                             buildOcmParser().
                             parseMessage(source);

        // check the default values that are not set in this simple file
        Assert.assertEquals("CSPOC",              file.getMetadata().getCatalogName());
        Assert.assertEquals(1.0,                  file.getMetadata().getSclkSecPerSISec(), 1.0e-15);
        Assert.assertEquals("LINEAR",             file.getMetadata().getInterpMethodEOP());

        // Check Header Block;
        Assert.assertEquals(3.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(1998, 11, 06, 9, 23, 57, TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());

        // OCM is the only message for which OBJECT_NAME is not mandatory, it is not present in this minimal file
        Assert.assertNull(file.getMetadata().getObjectName());

        Assert.assertEquals("JAXA", file.getHeader().getOriginator());

        final AbsoluteDate t0 = new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172, TimeScalesFactory.getUTC());
        Assert.assertEquals(t0, file.getMetadata().getEpochT0());
        Assert.assertEquals(TimeSystem.UTC, file.getMetadata().getTimeSystem());

        // orbit data
        Assert.assertEquals(1, file.getData().getOrbitBlocks().size());
        OrbitStateHistory history = file.getData().getOrbitBlocks().get(0);
        Assert.assertEquals("intervening data records omitted between DT=20.0 and DT=500.0",
                            history.getMetadata().getComments().get(0));
        Assert.assertEquals("OSCULATING", history.getMetadata().getOrbAveraging());
        Assert.assertEquals("EARTH", history.getMetadata().getCenter().getName());
        Assert.assertEquals(CelestialBodyFrame.ITRF2000, history.getMetadata().getOrbReferenceFrame().asCelestialBodyFrame());
        Assert.assertEquals(ElementsType.CARTPV, history.getMetadata().getOrbType());
        Assert.assertEquals(0.0, file.getMetadata().getEpochT0().durationFrom(t0), 1.0e-15);
        List<OrbitState> states = history.getOrbitalStates();
        Assert.assertEquals(4, states.size());

        Assert.assertEquals(0.0, states.get(0).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(6, states.get(0).getElements().length);
        Assert.assertEquals( 2789600.0, states.get(0).getElements()[0], 1.0e-15);
        Assert.assertEquals( -280000.0, states.get(0).getElements()[1], 1.0e-15);
        Assert.assertEquals(-1746800.0, states.get(0).getElements()[2], 1.0e-15);
        Assert.assertEquals(    4730.0, states.get(0).getElements()[3], 1.0e-15);
        Assert.assertEquals(   -2500.0, states.get(0).getElements()[4], 1.0e-15);
        Assert.assertEquals(   -1040.0, states.get(0).getElements()[5], 1.0e-15);

        Assert.assertEquals(10.0, states.get(1).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(6, states.get(1).getElements().length);
        Assert.assertEquals( 2783400.0, states.get(1).getElements()[0], 1.0e-15);
        Assert.assertEquals( -308100.0, states.get(1).getElements()[1], 1.0e-15);
        Assert.assertEquals(-1877100.0, states.get(1).getElements()[2], 1.0e-15);
        Assert.assertEquals(    5190.0, states.get(1).getElements()[3], 1.0e-15);
        Assert.assertEquals(   -2420.0, states.get(1).getElements()[4], 1.0e-15);
        Assert.assertEquals(   -2000.0, states.get(1).getElements()[5], 1.0e-15);

        Assert.assertEquals(20.0, states.get(2).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(6, states.get(2).getElements().length);
        Assert.assertEquals( 2776000.0, states.get(2).getElements()[0], 1.0e-15);
        Assert.assertEquals( -336900.0, states.get(2).getElements()[1], 1.0e-15);
        Assert.assertEquals(-2008700.0, states.get(2).getElements()[2], 1.0e-15);
        Assert.assertEquals(    5640.0, states.get(2).getElements()[3], 1.0e-15);
        Assert.assertEquals(   -2340.0, states.get(2).getElements()[4], 1.0e-15);
        Assert.assertEquals(   -1950.0, states.get(2).getElements()[5], 1.0e-15);

        Assert.assertEquals(500.0, states.get(3).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(6, states.get(3).getElements().length);
        Assert.assertEquals( 2164375.0,  states.get(3).getElements()[0], 1.0e-15);
        Assert.assertEquals( 1115811.0,  states.get(3).getElements()[1], 1.0e-15);
        Assert.assertEquals( -688131.0,  states.get(3).getElements()[2], 1.0e-15);
        Assert.assertEquals(   -3533.28, states.get(3).getElements()[3], 1.0e-15);
        Assert.assertEquals(   -2884.52, states.get(3).getElements()[4], 1.0e-15);
        Assert.assertEquals(     885.35, states.get(3).getElements()[5], 1.0e-15);

    }

    @Test
    public void testParseOCM2() {
        final String  name = "/ccsds/odm/ocm/OCMExample2.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final OcmFile file = new ParserBuilder().
                             withMu(Constants.EIGEN5C_EARTH_MU).
                             buildOcmParser().
                             parseMessage(source);

        // Check Header Block;
        Assert.assertEquals(3.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals("This OCM reflects the latest conditions post-maneuver A67Z",
                            file.getHeader().getComments().get(0));
        Assert.assertEquals("This example shows the specification of multiple comment lines",
                            file.getHeader().getComments().get(1));
        Assert.assertEquals(new AbsoluteDate(1998, 11, 06, 9, 23, 57, TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assert.assertEquals("JAXA", file.getHeader().getOriginator());
        Assert.assertEquals("OCM 201113719185", file.getHeader().getMessageId());

        // Check metadata
        Assert.assertEquals("OSPREY 5",                            file.getMetadata().getObjectName());
        Assert.assertEquals("1998-999A",                           file.getMetadata().getInternationalDesignator());
        Assert.assertEquals("R. Rabbit",                           file.getMetadata().getOriginatorPOC());
        Assert.assertEquals("Flight Dynamics Mission Design Lead", file.getMetadata().getOriginatorPosition());
        Assert.assertEquals("(719)555-1234",                       file.getMetadata().getOriginatorPhone());
        Assert.assertEquals("Mr. Rodgers",                         file.getMetadata().getTechPOC());
        Assert.assertEquals("(719)555-1234",                       file.getMetadata().getTechPhone());
        Assert.assertEquals("email@email.XXX",                     file.getMetadata().getTechAddress());
        Assert.assertEquals(TimeSystem.UT1, file.getMetadata().getTimeSystem());
        TimeScale ts = DataContext.getDefault().getTimeScales().getUT1(IERSConventions.IERS_2010, false);
        Assert.assertEquals(0.0,
                            file.getMetadata().getEpochT0().durationFrom(new AbsoluteDate(1998, 12, 18, ts)),
                            1.0e-10);
        Assert.assertEquals(36.0,                                  file.getMetadata().getTaimutcT0(), 1.0e-15);
        Assert.assertEquals(0.357,                                 file.getMetadata().getUt1mutcT0(), 1.0e-15);

        // check orbit data
        Assert.assertEquals(1, file.getData().getOrbitBlocks().size());
        final OrbitStateHistory orb = file.getData().getOrbitBlocks().get(0);
        Assert.assertEquals(2, orb.getMetadata().getComments().size());
        Assert.assertEquals("GEOCENTRIC, CARTESIAN, EARTH FIXED", orb.getMetadata().getComments().get(0));
        Assert.assertEquals("THIS IS MY SECOND COMMENT LINE",     orb.getMetadata().getComments().get(1));
        Assert.assertEquals("PREDICTED", orb.getMetadata().getOrbBasis());
        Assert.assertEquals("EFG", orb.getMetadata().getOrbReferenceFrame().getName());
        Assert.assertNull(orb.getMetadata().getOrbReferenceFrame().asFrame());
        Assert.assertNull(orb.getMetadata().getOrbReferenceFrame().asCelestialBodyFrame());
        Assert.assertNull(orb.getMetadata().getOrbReferenceFrame().asOrbitRelativeFrame());
        Assert.assertNull(orb.getMetadata().getOrbReferenceFrame().asSpacecraftBodyFrame());
        Assert.assertEquals(ElementsType.CARTPV, orb.getMetadata().getOrbType());
        Assert.assertEquals(6, orb.getMetadata().getOrbUnits().size());
        Assert.assertEquals("km",   orb.getMetadata().getOrbUnits().get(0).getName());
        Assert.assertEquals("km",   orb.getMetadata().getOrbUnits().get(1).getName());
        Assert.assertEquals("km",   orb.getMetadata().getOrbUnits().get(2).getName());
        Assert.assertEquals("km/s", orb.getMetadata().getOrbUnits().get(3).getName());
        Assert.assertEquals("km/s", orb.getMetadata().getOrbUnits().get(4).getName());
        Assert.assertEquals("km/s", orb.getMetadata().getOrbUnits().get(5).getName());
        Assert.assertEquals(1, orb.getOrbitalStates().size());
        Assert.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 25.1172, ts),
                            orb.getOrbitalStates().get(0).getDate());
        Assert.assertEquals( 2854533.0, orb.getOrbitalStates().get(0).getElements()[0], 1.0e-10);
        Assert.assertEquals(-2916187.0, orb.getOrbitalStates().get(0).getElements()[1], 1.0e-10);
        Assert.assertEquals(-5360774.0, orb.getOrbitalStates().get(0).getElements()[2], 1.0e-10);
        Assert.assertEquals(    5688.0, orb.getOrbitalStates().get(0).getElements()[3], 1.0e-10);
        Assert.assertEquals(    4652.0, orb.getOrbitalStates().get(0).getElements()[4], 1.0e-10);
        Assert.assertEquals(     520.0, orb.getOrbitalStates().get(0).getElements()[5], 1.0e-10);

        // check physical data
        PhysicalProperties phys = file.getData().getPhysicBlock();
        Assert.assertEquals(1, phys.getComments().size());
        Assert.assertEquals("S/C Physical Characteristics:", phys.getComments().get(0));
        Assert.assertEquals(100.0,   phys.getWetMass(),                  1.0e-10);
        Assert.assertEquals(0.03123, phys.getOebQ().getQ1(),             1.0e-10);
        Assert.assertEquals(0.78543, phys.getOebQ().getQ2(),             1.0e-10);
        Assert.assertEquals(0.39158, phys.getOebQ().getQ3(),             1.0e-10);
        Assert.assertEquals(0.47832, phys.getOebQ().getQ0(),             1.0e-10);
        Assert.assertEquals(2.0,     phys.getOebMax(),                   1.0e-10);
        Assert.assertEquals(1.0,     phys.getOebIntermediate(),          1.0e-10);
        Assert.assertEquals(0.5,     phys.getOebMin(),                   1.0e-10);
        Assert.assertEquals(0.15,    phys.getOebAreaAlongMax(),          1.0e-10);
        Assert.assertEquals(0.30,    phys.getOebAreaAlongIntermediate(), 1.0e-10);
        Assert.assertEquals(0.50,    phys.getOebAreaAlongMin(),          1.0e-10);

        // check no covariance
        Assert.assertNull(file.getData().getCovarianceBlocks());

        // check no maneuvers
        Assert.assertNull(file.getData().getManeuverBlocks());

        // check perturbation data
        Perturbations perts = file.getData().getPerturbationsBlock();
        Assert.assertEquals(1, perts.getComments().size());
        Assert.assertEquals("Perturbations Specification:", perts.getComments().get(0));
        Assert.assertEquals("NRLMSIS00", perts.getAtmosphericModel());
        Assert.assertEquals("EGM-96", perts.getGravityModel());
        Assert.assertEquals(36, perts.getGravityDegree());
        Assert.assertEquals(36, perts.getGravityOrder());
        Assert.assertEquals(2, perts.getNBodyPerturbations().size());
        Assert.assertEquals("MOON", perts.getNBodyPerturbations().get(0).getName());
        Assert.assertEquals("SUN",  perts.getNBodyPerturbations().get(1).getName());
        Assert.assertEquals( 12.0, perts.getFixedGeomagneticKp(), 1.0e-10);
        Assert.assertEquals(105.0, perts.getFixedF10P7(),         1.0e-10);
        Assert.assertEquals(120.0, perts.getFixedF10P7Mean(),     1.0e-10);

        // check no orbit determination
        Assert.assertNull(file.getData().getOrbitDeterminationBlock());

        // check user data
        UserDefined user = file.getData().getUserDefinedBlock();
        Assert.assertTrue(user.getComments().isEmpty());
        Assert.assertEquals(1, user.getParameters().size());
        Assert.assertEquals("MAXWELL RAFERTY", user.getParameters().get("CONSOLE_POC"));

    }

    @Test
    public void testParseOCM3() throws IOException {
        final String   name  = "/ccsds/odm/ocm/OCMExample3.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final OcmFile file = new ParserBuilder().
                             withMu(Constants.EIGEN5C_EARTH_MU).
                             buildOcmParser().
                             parse(source); // using EphemerisFileParser API here

        // Check Header Block;
        TimeScale utc = TimeScalesFactory.getUTC();
        Assert.assertEquals(3.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals(0, file.getMetadata().getComments().size());
        Assert.assertEquals(new AbsoluteDate(1998, 11, 06, 9, 23, 57, utc),
                            file.getHeader().getCreationDate());

        final AbsoluteDate t0 = new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172, utc);
        Assert.assertEquals(t0, file.getMetadata().getEpochT0());
        Assert.assertEquals("UTC", file.getMetadata().getTimeSystem().name());

        // check orbit data
        Assert.assertEquals(1, file.getData().getOrbitBlocks().size());
        final OrbitStateHistory orb = file.getData().getOrbitBlocks().get(0);
        Assert.assertEquals(2, orb.getMetadata().getComments().size());
        Assert.assertEquals("ORBIT EPHEMERIS INCORPORATING DEPLOYMENTS AND MANEUVERS (BELOW)", orb.getMetadata().getComments().get(0));
        Assert.assertEquals("intervening data records omitted after DT=20.0",     orb.getMetadata().getComments().get(1));
        Assert.assertEquals("PREDICTED", orb.getMetadata().getOrbBasis()); // default value, not present in the file
        Assert.assertEquals("TOD", orb.getMetadata().getOrbReferenceFrame().getName());
        Assert.assertNotNull(orb.getMetadata().getOrbReferenceFrame().asFrame());
        Assert.assertEquals(CelestialBodyFrame.TOD, orb.getMetadata().getOrbReferenceFrame().asCelestialBodyFrame());
        Assert.assertNull(orb.getMetadata().getOrbReferenceFrame().asOrbitRelativeFrame());
        Assert.assertNull(orb.getMetadata().getOrbReferenceFrame().asSpacecraftBodyFrame());
        Assert.assertEquals(t0, orb.getMetadata().getOrbFrameEpoch());
        Assert.assertEquals(ElementsType.CARTPVA, orb.getMetadata().getOrbType());
        Assert.assertEquals(9, orb.getMetadata().getOrbUnits().size());
        Assert.assertEquals("km",      orb.getMetadata().getOrbUnits().get(0).getName());
        Assert.assertEquals("km",      orb.getMetadata().getOrbUnits().get(1).getName());
        Assert.assertEquals("km",      orb.getMetadata().getOrbUnits().get(2).getName());
        Assert.assertEquals("km/s",    orb.getMetadata().getOrbUnits().get(3).getName());
        Assert.assertEquals("km/s",    orb.getMetadata().getOrbUnits().get(4).getName());
        Assert.assertEquals("km/s",    orb.getMetadata().getOrbUnits().get(5).getName());
        Assert.assertEquals("km/s**2", orb.getMetadata().getOrbUnits().get(6).getName());
        Assert.assertEquals("km/s**2", orb.getMetadata().getOrbUnits().get(7).getName());
        Assert.assertEquals("km/s**2", orb.getMetadata().getOrbUnits().get(8).getName());
        Assert.assertEquals(4, orb.getOrbitalStates().size());
        Assert.assertEquals(       0.0,  orb.getOrbitalStates().get(0).getDate().durationFrom(t0), 1.0e-10);
        Assert.assertEquals( 2789600.0,  orb.getOrbitalStates().get(0).getElements()[0], 1.0e-10);
        Assert.assertEquals( -280000.0,  orb.getOrbitalStates().get(0).getElements()[1], 1.0e-10);
        Assert.assertEquals(-1746800.0,  orb.getOrbitalStates().get(0).getElements()[2], 1.0e-10);
        Assert.assertEquals(    4730.0,  orb.getOrbitalStates().get(0).getElements()[3], 1.0e-10);
        Assert.assertEquals(   -2500.0,  orb.getOrbitalStates().get(0).getElements()[4], 1.0e-10);
        Assert.assertEquals(   -1040.0,  orb.getOrbitalStates().get(0).getElements()[5], 1.0e-10);
        Assert.assertEquals(       8.0,  orb.getOrbitalStates().get(0).getElements()[6], 1.0e-10);
        Assert.assertEquals(       1.0,  orb.getOrbitalStates().get(0).getElements()[7], 1.0e-10);
        Assert.assertEquals(    -159.0,  orb.getOrbitalStates().get(0).getElements()[8], 1.0e-10);
        Assert.assertEquals(      10.0,  orb.getOrbitalStates().get(1).getDate().durationFrom(t0), 1.0e-10);
        Assert.assertEquals( 2783400.0,  orb.getOrbitalStates().get(1).getElements()[0], 1.0e-10);
        Assert.assertEquals( -308100.0,  orb.getOrbitalStates().get(1).getElements()[1], 1.0e-10);
        Assert.assertEquals(-1877100.0,  orb.getOrbitalStates().get(1).getElements()[2], 1.0e-10);
        Assert.assertEquals(    5190.0,  orb.getOrbitalStates().get(1).getElements()[3], 1.0e-10);
        Assert.assertEquals(   -2420.0,  orb.getOrbitalStates().get(1).getElements()[4], 1.0e-10);
        Assert.assertEquals(   -2000.0,  orb.getOrbitalStates().get(1).getElements()[5], 1.0e-10);
        Assert.assertEquals(       8.0,  orb.getOrbitalStates().get(1).getElements()[6], 1.0e-10);
        Assert.assertEquals(       1.0,  orb.getOrbitalStates().get(1).getElements()[7], 1.0e-10);
        Assert.assertEquals(       1.0,  orb.getOrbitalStates().get(1).getElements()[8], 1.0e-10);
        Assert.assertEquals(      20.0,  orb.getOrbitalStates().get(2).getDate().durationFrom(t0), 1.0e-10);
        Assert.assertEquals( 2776000.0,  orb.getOrbitalStates().get(2).getElements()[0], 1.0e-10);
        Assert.assertEquals( -336900.0,  orb.getOrbitalStates().get(2).getElements()[1], 1.0e-10);
        Assert.assertEquals(-2008700.0,  orb.getOrbitalStates().get(2).getElements()[2], 1.0e-10);
        Assert.assertEquals(    5640.0,  orb.getOrbitalStates().get(2).getElements()[3], 1.0e-10);
        Assert.assertEquals(   -2340.0,  orb.getOrbitalStates().get(2).getElements()[4], 1.0e-10);
        Assert.assertEquals(   -1950.0,  orb.getOrbitalStates().get(2).getElements()[5], 1.0e-10);
        Assert.assertEquals(       8.0,  orb.getOrbitalStates().get(2).getElements()[6], 1.0e-10);
        Assert.assertEquals(       1.0,  orb.getOrbitalStates().get(2).getElements()[7], 1.0e-10);
        Assert.assertEquals(     159.0,  orb.getOrbitalStates().get(2).getElements()[8], 1.0e-10);
        Assert.assertEquals(     500.0,  orb.getOrbitalStates().get(3).getDate().durationFrom(t0), 1.0e-10);
        Assert.assertEquals( 2164375.0,  orb.getOrbitalStates().get(3).getElements()[0], 1.0e-10);
        Assert.assertEquals( 1115811.0,  orb.getOrbitalStates().get(3).getElements()[1], 1.0e-10);
        Assert.assertEquals( -688131.0,  orb.getOrbitalStates().get(3).getElements()[2], 1.0e-10);
        Assert.assertEquals(   -3533.28, orb.getOrbitalStates().get(3).getElements()[3], 1.0e-10);
        Assert.assertEquals(   -2884.52, orb.getOrbitalStates().get(3).getElements()[4], 1.0e-10);
        Assert.assertEquals(     885.35, orb.getOrbitalStates().get(3).getElements()[5], 1.0e-10);
        Assert.assertEquals(       0.0,  orb.getOrbitalStates().get(3).getElements()[6], 1.0e-10);
        Assert.assertEquals(       0.0,  orb.getOrbitalStates().get(3).getElements()[7], 1.0e-10);
        Assert.assertEquals(       0.0,  orb.getOrbitalStates().get(3).getElements()[8], 1.0e-10);

        // check physical data
        PhysicalProperties phys = file.getData().getPhysicBlock();
        Assert.assertEquals(1, phys.getComments().size());
        Assert.assertEquals("S/C Physical Characteristics:", phys.getComments().get(0));
        Assert.assertEquals(10.0,    phys.getDragConstantArea(),         1.0e-10);
        Assert.assertEquals(2.3,     phys.getNominalDragCoefficient(),   1.0e-10);
        Assert.assertEquals(100.0,   phys.getWetMass(),                  1.0e-10);
        Assert.assertEquals(4.0,     phys.getSrpConstantArea(),          1.0e-10);
        Assert.assertEquals(1.3,     phys.getNominalSrpCoefficient(),    1.0e-10);

        // check no covariance
        Assert.assertNull(file.getData().getCovarianceBlocks());

        // check maneuvers
        List<ManeuverHistory> man = file.getData().getManeuverBlocks();
        Assert.assertEquals(2, man.size());

        Assert.assertEquals(2, man.get(0).getMetadata().getComments().size());
        Assert.assertEquals("Ten 1kg objects deployed from 200kg host over 100 s timespan", man.get(0).getMetadata().getComments().get(0));
        Assert.assertEquals("20 deg off of back-track direction", man.get(0).getMetadata().getComments().get(1));
        Assert.assertEquals("CUBESAT DEPLOY", man.get(0).getMetadata().getManID());
        Assert.assertEquals("CANDIDATE",      man.get(0).getMetadata().getManBasis());
        Assert.assertEquals("DEPLOY",         man.get(0).getMetadata().getManDeviceID());
        Assert.assertEquals(1,                man.get(0).getMetadata().getManPurpose().size());
        Assert.assertEquals("DEPLOY",         man.get(0).getMetadata().getManPurpose().get(0));
        Assert.assertEquals("RSW_ROTATING",   man.get(0).getMetadata().getManReferenceFrame().getName());
        Assert.assertNull(man.get(0).getMetadata().getManReferenceFrame().asFrame());
        Assert.assertNull(man.get(0).getMetadata().getManReferenceFrame().asCelestialBodyFrame());
        Assert.assertEquals(OrbitRelativeFrame.RSW_ROTATING, man.get(0).getMetadata().getManReferenceFrame().asOrbitRelativeFrame());
        Assert.assertNull(man.get(0).getMetadata().getManReferenceFrame().asSpacecraftBodyFrame());
        Assert.assertEquals(9, man.get(0).getMetadata().getManComposition().size());
        Assert.assertEquals(ManeuverFieldType.TIME_RELATIVE,   man.get(0).getMetadata().getManComposition().get(0));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_ID,       man.get(0).getMetadata().getManComposition().get(1));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_DV_X,     man.get(0).getMetadata().getManComposition().get(2));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_DV_Y,     man.get(0).getMetadata().getManComposition().get(3));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_DV_Z,     man.get(0).getMetadata().getManComposition().get(4));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_MASS,     man.get(0).getMetadata().getManComposition().get(5));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_DV_SIGMA, man.get(0).getMetadata().getManComposition().get(6));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_DV_RATIO, man.get(0).getMetadata().getManComposition().get(7));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_DV_CDA,   man.get(0).getMetadata().getManComposition().get(8));
        Assert.assertEquals(8, man.get(0).getMetadata().getManUnits().size());
        Assert.assertEquals("n/a",  man.get(0).getMetadata().getManUnits().get(0).getName());
        Assert.assertEquals("km/s", man.get(0).getMetadata().getManUnits().get(1).getName());
        Assert.assertEquals("km/s", man.get(0).getMetadata().getManUnits().get(2).getName());
        Assert.assertEquals("km/s", man.get(0).getMetadata().getManUnits().get(3).getName());
        Assert.assertEquals("kg",   man.get(0).getMetadata().getManUnits().get(4).getName());
        Assert.assertEquals("%",    man.get(0).getMetadata().getManUnits().get(5).getName());
        Assert.assertEquals("n/a",  man.get(0).getMetadata().getManUnits().get(6).getName());
        Assert.assertEquals("m**2", man.get(0).getMetadata().getManUnits().get(7).getName());
        Assert.assertEquals(10, man.get(0).getManeuvers().size());
        Assert.assertEquals(500.0,        man.get(0).getManeuvers().get(0).getDate().durationFrom(t0), 1.0e-10);
        Assert.assertEquals("CUBESAT_10", man.get(0).getManeuvers().get(0).getDeployId());
        Assert.assertEquals( 2.8773E-1,   man.get(0).getManeuvers().get(0).getDeployDv().getX(), 1.0e-10);
        Assert.assertEquals(-9.3969E-1,   man.get(0).getManeuvers().get(0).getDeployDv().getY(), 1.0e-10);
        Assert.assertEquals( 1.8491E-1,   man.get(0).getManeuvers().get(0).getDeployDv().getZ(), 1.0e-10);
        Assert.assertEquals(-1.0,         man.get(0).getManeuvers().get(0).getDeployMass(), 1.0e-10);
        Assert.assertEquals( 0.05,        man.get(0).getManeuvers().get(0).getDeployDvSigma(), 1.0e-10);
        Assert.assertEquals(-0.005025,    man.get(0).getManeuvers().get(0).getDeployDvRatio(), 1.0e-10);
        Assert.assertEquals( 0.033,       man.get(0).getManeuvers().get(0).getDeployDvCda(), 1.0e-10);
        Assert.assertEquals(510.0,        man.get(0).getManeuvers().get(1).getDate().durationFrom(t0), 1.0e-10);
        Assert.assertEquals("CUBESAT_11", man.get(0).getManeuvers().get(1).getDeployId());
        Assert.assertEquals( 1.4208E-1,   man.get(0).getManeuvers().get(1).getDeployDv().getX(), 1.0e-10);
        Assert.assertEquals(-9.3969E-1,   man.get(0).getManeuvers().get(1).getDeployDv().getY(), 1.0e-10);
        Assert.assertEquals( 3.1111E-1,   man.get(0).getManeuvers().get(1).getDeployDv().getZ(), 1.0e-10);
        Assert.assertEquals(-1.0,         man.get(0).getManeuvers().get(1).getDeployMass(), 1.0e-10);
        Assert.assertEquals( 0.05,        man.get(0).getManeuvers().get(1).getDeployDvSigma(), 1.0e-10);
        Assert.assertEquals(-0.005051,    man.get(0).getManeuvers().get(1).getDeployDvRatio(), 1.0e-10);
        Assert.assertEquals( 0.033,       man.get(0).getManeuvers().get(1).getDeployDvCda(), 1.0e-10);
        
        Assert.assertEquals(1, man.get(1).getMetadata().getComments().size());
        Assert.assertEquals("100 s of 0.5N +in-track thrust w/effic η=0.95, Isp=300s, 5% 1-sigma error", man.get(1).getMetadata().getComments().get(0));
        Assert.assertEquals("E W 20160305B", man.get(1).getMetadata().getManID());
        Assert.assertEquals("CANDIDATE",     man.get(1).getMetadata().getManBasis());
        Assert.assertEquals("THR 01",        man.get(1).getMetadata().getManDeviceID());
        Assert.assertEquals(1,               man.get(1).getMetadata().getManPurpose().size());
        Assert.assertEquals("ORBIT",         man.get(1).getMetadata().getManPurpose().get(0));
        Assert.assertEquals("RSW_ROTATING",  man.get(1).getMetadata().getManReferenceFrame().getName());
        Assert.assertNull(man.get(1).getMetadata().getManReferenceFrame().asFrame());
        Assert.assertNull(man.get(1).getMetadata().getManReferenceFrame().asCelestialBodyFrame());
        Assert.assertEquals(OrbitRelativeFrame.RSW_ROTATING, man.get(1).getMetadata().getManReferenceFrame().asOrbitRelativeFrame());
        Assert.assertEquals(10, man.get(1).getMetadata().getManComposition().size());
        Assert.assertEquals(ManeuverFieldType.TIME_ABSOLUTE, man.get(1).getMetadata().getManComposition().get(0));
        Assert.assertEquals(ManeuverFieldType.TIME_RELATIVE, man.get(1).getMetadata().getManComposition().get(1));
        Assert.assertEquals(ManeuverFieldType.MAN_DURA,      man.get(1).getMetadata().getManComposition().get(2));
        Assert.assertEquals(ManeuverFieldType.THR_X,         man.get(1).getMetadata().getManComposition().get(3));
        Assert.assertEquals(ManeuverFieldType.THR_Y,         man.get(1).getMetadata().getManComposition().get(4));
        Assert.assertEquals(ManeuverFieldType.THR_Z,         man.get(1).getMetadata().getManComposition().get(5));
        Assert.assertEquals(ManeuverFieldType.THR_SIGMA,     man.get(1).getMetadata().getManComposition().get(6));
        Assert.assertEquals(ManeuverFieldType.THR_INTERP,    man.get(1).getMetadata().getManComposition().get(7));
        Assert.assertEquals(ManeuverFieldType.THR_ISP,       man.get(1).getMetadata().getManComposition().get(8));
        Assert.assertEquals(ManeuverFieldType.THR_EFFIC,     man.get(1).getMetadata().getManComposition().get(9));
        Assert.assertEquals(8, man.get(1).getMetadata().getManUnits().size());
        Assert.assertEquals("s",    man.get(1).getMetadata().getManUnits().get(0).getName());
        Assert.assertEquals("N",    man.get(1).getMetadata().getManUnits().get(1).getName());
        Assert.assertEquals("N",    man.get(1).getMetadata().getManUnits().get(2).getName());
        Assert.assertEquals("N",    man.get(1).getMetadata().getManUnits().get(3).getName());
        Assert.assertEquals("%",    man.get(1).getMetadata().getManUnits().get(4).getName());
        Assert.assertEquals("n/a",  man.get(1).getMetadata().getManUnits().get(5).getName());
        Assert.assertEquals("s",    man.get(1).getMetadata().getManUnits().get(6).getName());
        Assert.assertEquals("n/a",  man.get(1).getMetadata().getManUnits().get(7).getName());
        Assert.assertEquals(1,      man.get(1).getManeuvers().size());
        Assert.assertEquals(500.0,        man.get(1).getManeuvers().get(0).getDate().durationFrom(t0), 1.0e-10);
        Assert.assertEquals(100,          man.get(1).getManeuvers().get(0).getDuration(),         1.0e-10);
        Assert.assertEquals(  0.0,        man.get(1).getManeuvers().get(0).getThrust().getX(),    1.0e-10);
        Assert.assertEquals(  0.5,        man.get(1).getManeuvers().get(0).getThrust().getY(),    1.0e-10);
        Assert.assertEquals( 0.05,        man.get(1).getManeuvers().get(0).getThrustSigma(),      1.0e-10);
        Assert.assertEquals("ON",         man.get(1).getManeuvers().get(0).getThrustInterpolation());
        Assert.assertEquals(300.0,        man.get(1).getManeuvers().get(0).getThrustIsp(),        1.0e-10);
        Assert.assertEquals(0.95,         man.get(1).getManeuvers().get(0).getThrustEfficiency(), 1.0e-10);

        // check perturbation data
        Perturbations perts = file.getData().getPerturbationsBlock();
        Assert.assertEquals(1, perts.getComments().size());
        Assert.assertEquals("Perturbations specification", perts.getComments().get(0));
        Assert.assertEquals(3.986004415e14, perts.getGm(),     1.0);

        // check orbit determination
        OrbitDetermination od = file.getData().getOrbitDeterminationBlock();
        Assert.assertEquals(1, od.getComments().size());
        Assert.assertEquals("Orbit Determination information", od.getComments().get(0));
        Assert.assertEquals("OOD #10059", od.getId());
        Assert.assertEquals("OOD #10058", od.getPrevId());
        Assert.assertEquals(OdMethodType.SF, od.getMethod().getType());
        Assert.assertEquals("ODTK",          od.getMethod().getTool());
        Assert.assertEquals(new AbsoluteDate(2001, 11, 6, 11, 17, 33.0, utc), od.getEpoch());
        Assert.assertEquals(273, od.getObsUsed());
        Assert.assertEquals( 91, od.getTracksUsed());

        // check no user data
        Assert.assertNull(file.getData().getUserDefinedBlock());

    }

    @Test
    public void testParseOCM4() {
        final String   name  = "/ccsds/odm/ocm/OCMExample4.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final OcmFile file = new ParserBuilder().
                             withMu(Constants.EIGEN5C_EARTH_MU).
                             buildOcmParser().
                             parseMessage(source);

        // Check Header Block;
        Assert.assertEquals(3.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals(2, file.getHeader().getComments().size());
        Assert.assertEquals("This file is a dummy example with inconsistent data", file.getHeader().getComments().get(0));
        Assert.assertEquals("it is used to exercise all possible keys in Key-Value Notation", file.getHeader().getComments().get(1));
        Assert.assertEquals("JPL",                                 file.getHeader().getOriginator());
        Assert.assertEquals("ABC-12 34",                           file.getHeader().getMessageId());

        // Check metadata
        Assert.assertEquals(new AbsoluteDate(2019, 7, 23, 10, 29, 31.576, TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());

        Assert.assertEquals(1,                                     file.getMetadata().getComments().size());
        Assert.assertEquals("Metadata comment",                    file.getMetadata().getComments().get(0));
        Assert.assertEquals("FOUO",                                file.getMetadata().getClassification());
        Assert.assertEquals("UNKNOWN",                             file.getMetadata().getObjectName());
        Assert.assertEquals(3,                                     file.getMetadata().getAlternateNames().size());
        Assert.assertEquals("ALTERNATE",                           file.getMetadata().getAlternateNames().get(0));
        Assert.assertEquals("OTHER",                               file.getMetadata().getAlternateNames().get(1));
        Assert.assertEquals("RELATED",                             file.getMetadata().getAlternateNames().get(2));
        Assert.assertEquals("18SPCS 18571",                        file.getMetadata().getObjectDesignator());
        Assert.assertEquals("2000-053A",                           file.getMetadata().getInternationalDesignator());
        Assert.assertEquals("Mr. Rodgers",                         file.getMetadata().getOriginatorPOC());
        Assert.assertEquals("Flight Dynamics Mission Design Lead", file.getMetadata().getOriginatorPosition());
        Assert.assertEquals("+12345678901",                        file.getMetadata().getOriginatorPhone());
        Assert.assertEquals("JOHN.DOE@EXAMPLE.ORG",                file.getMetadata().getOriginatorAddress());
        Assert.assertEquals("NASA",                                file.getMetadata().getTechOrg());
        Assert.assertEquals("Maxwell Smart",                       file.getMetadata().getTechPOC());
        Assert.assertEquals("+98765432109",                        file.getMetadata().getTechPhone());
        Assert.assertEquals("MAX@EXAMPLE.ORG",                     file.getMetadata().getTechAddress());
        Assert.assertEquals("ABC-12 33",                           file.getMetadata().getPreviousMessageID());
        Assert.assertEquals("ABC-12 35",                           file.getMetadata().getNextMessageID());
        Assert.assertEquals("ADM-MSG-35132.TXT",                   file.getMetadata().getAdmMessageLink());
        Assert.assertEquals("CDM-MSG-35132.TXT",                   file.getMetadata().getCdmMessageLink());
        Assert.assertEquals("PRM-MSG-35132.TXT",                   file.getMetadata().getPrmMessageLink());
        Assert.assertEquals("RDM-MSG-35132.TXT",                   file.getMetadata().getRdmMessageLink());
        Assert.assertEquals("COMSPOC",                             file.getMetadata().getCatalogName());
        Assert.assertEquals("INTELSAT",                            file.getMetadata().getOperator());
        Assert.assertEquals("SIRIUS",                              file.getMetadata().getOwner());
        Assert.assertEquals("FRANCE",                              file.getMetadata().getCountry());
        Assert.assertEquals("SPIRE",                               file.getMetadata().getConstellation());
        Assert.assertEquals("PAYLOAD",                             file.getMetadata().getObjectType().toString());
        Assert.assertEquals("Operational",                         file.getMetadata().getOpsStatus().toString());
        Assert.assertEquals("Extended Geostationary Orbit",        file.getMetadata().getOrbitCategory().toString());
        Assert.assertEquals(7,                                     file.getMetadata().getOcmDataElements().size());
        Assert.assertEquals("ORBIT",                               file.getMetadata().getOcmDataElements().get(0));
        Assert.assertEquals("PHYSICAL DESCRIPTION",                file.getMetadata().getOcmDataElements().get(1));
        Assert.assertEquals("COVARIANCE",                          file.getMetadata().getOcmDataElements().get(2));
        Assert.assertEquals("MANEUVER",                            file.getMetadata().getOcmDataElements().get(3));
        Assert.assertEquals("PERTURBATIONS",                       file.getMetadata().getOcmDataElements().get(4));
        Assert.assertEquals("OD",                                  file.getMetadata().getOcmDataElements().get(5));
        Assert.assertEquals("USER",                                file.getMetadata().getOcmDataElements().get(6));
        Assert.assertEquals("UTC",                                 file.getMetadata().getTimeSystem().name());
        Assert.assertEquals(new AbsoluteDate(2019, 7, 23,  0, 0, 0.0, TimeScalesFactory.getUTC()),
                            file.getMetadata().getEpochT0());
        Assert.assertEquals(28800.0, file.getMetadata().getSclkOffsetAtEpoch(), 1.0e-10);
        Assert.assertEquals(2.5,                                   file.getMetadata().getSclkSecPerSISec(), 1.0e-15);
        Assert.assertEquals(new AbsoluteDate(2019, 7, 23,  9, 29, 31.576, TimeScalesFactory.getUTC()),
                            file.getMetadata().getPreviousMessageEpoch());
        Assert.assertEquals(new AbsoluteDate(2019, 7, 23, 11, 29, 31.576, TimeScalesFactory.getUTC()),
                            file.getMetadata().getNextMessageEpoch());
        Assert.assertEquals(new AbsoluteDate(2019, 7, 23,  9, 30,  0.0, TimeScalesFactory.getUTC()),
                            file.getMetadata().getStartTime());
        Assert.assertEquals(new AbsoluteDate(2019, 7, 23, 10, 29, 50.0, TimeScalesFactory.getUTC()),
                            file.getMetadata().getStopTime());
        Assert.assertEquals(0.041550925925 * Constants.JULIAN_DAY, file.getMetadata().getTimeSpan(), 1.0e-15);
        Assert.assertEquals(37.0,                                  file.getMetadata().getTaimutcT0(), 1.0e-15);
        Assert.assertEquals(-0.1642060,                            file.getMetadata().getUt1mutcT0(), 1.0e-15);
        Assert.assertEquals("IERS",                                file.getMetadata().getEopSource());
        Assert.assertEquals("LAGRANGE ORDER 5",                    file.getMetadata().getInterpMethodEOP());
        Assert.assertEquals("JPL DE 430",                          file.getMetadata().getCelestialSource());

        // TODO test orbit data

        // TODO test physical data

        // TODO test perturbation data

        // TODO test user data

    }

}
