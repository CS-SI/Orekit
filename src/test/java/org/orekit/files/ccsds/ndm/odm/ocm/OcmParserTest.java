/* Copyright 2002-2022 CS GROUP
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

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.CenterName;
import org.orekit.files.ccsds.definitions.DutyCycleType;
import org.orekit.files.ccsds.definitions.ElementsType;
import org.orekit.files.ccsds.definitions.OdMethodType;
import org.orekit.files.ccsds.definitions.OnOff;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.files.ccsds.definitions.SpacecraftBodyFrame;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.odm.UserDefined;
import org.orekit.files.ccsds.ndm.odm.oem.InterpolationMethod;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.files.ccsds.utils.lexical.KvnLexicalAnalyzer;
import org.orekit.files.ccsds.utils.lexical.XmlLexicalAnalyzer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.units.Unit;

public class OcmParserTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testNonExistentKvnFile() throws URISyntaxException {
        final String realName = "/ccsds/odm/ocm/OCMExample1.txt";
        final String wrongName = realName + "xxxxx";
        final DataSource source = new DataSource(wrongName, () -> getClass().getResourceAsStream(wrongName));
        try {
            new KvnLexicalAnalyzer(source).accept(new ParserBuilder().buildOcmParser());
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testNonExistentXmlFile() throws URISyntaxException {
        final String realName = "/ccsds/odm/ocm/OCMExample1.txt";
        final String wrongName = realName + "xxxxx";
        final DataSource source = new DataSource(wrongName, () -> getClass().getResourceAsStream(wrongName));
        try {
            new XmlLexicalAnalyzer(source).accept(new ParserBuilder().buildOcmParser());
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
    public void testMissingRevnumBasis() throws URISyntaxException {
        final String name = "/ccsds/odm/ocm/OCM-missing-revnum-basis.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(Constants.EIGEN5C_EARTH_MU).
            buildOcmParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assert.assertEquals(TrajectoryStateHistoryMetadataKey.ORB_REVNUM_BASIS.name(), oe.getParts()[0]);
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
        final Ocm    ocm    = new ParserBuilder().
                                  withMu(Constants.EIGEN5C_EARTH_MU).
                                  buildOcmParser().
                                  parseMessage(source);
        Assert.assertEquals("CS GROUP", ocm.getHeader().getOriginator());
        Assert.assertEquals("728b0d2a-01fc-4d0e-9f0a-370c6930ea84", ocm.getHeader().getMessageId().toLowerCase(Locale.US));
        final TrajectoryStateHistory h = ocm.getData().getOTrajectoryBlocks().get(0);
        Assert.assertEquals("ZZRF", h.getMetadata().getTrajReferenceFrame().getName());
        List<TimeStampedPVCoordinates> l = h.getCoordinates();
        Assert.assertEquals( 3.0e6, l.get(0).getPosition().getX(), 1.0e-9);
        Assert.assertEquals( 4.0e6, l.get(0).getPosition().getY(), 1.0e-9);
        Assert.assertEquals( 5.0e6, l.get(0).getPosition().getZ(), 1.0e-9);
        Assert.assertEquals(-1.0e3, l.get(0).getVelocity().getX(), 1.0e-12);
        Assert.assertEquals(-2.0e3, l.get(0).getVelocity().getY(), 1.0e-12);
        Assert.assertEquals(-3.0e3, l.get(0).getVelocity().getZ(), 1.0e-1);
        try {
            ocm.getData().getOTrajectoryBlocks().get(0).getFrame();
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
        final Ocm    ocm    = new ParserBuilder().
                                  withMu(Constants.EIGEN5C_EARTH_MU).
                                  buildOcmParser().
                                  parseMessage(source);
        Assert.assertEquals("CS GROUP", ocm.getHeader().getOriginator());
        Assert.assertEquals("b77d785c-f7a8-4a80-a9b1-a540eac19d7a", ocm.getHeader().getMessageId().toLowerCase(Locale.US));
        Assert.assertNull(ocm.getData().getOTrajectoryBlocks());
        Assert.assertEquals(1, ocm.getData().getUserDefinedBlock().getComments().size());
        Assert.assertEquals("some user data", ocm.getData().getUserDefinedBlock().getComments().get(0));
        Assert.assertEquals(1, ocm.getData().getUserDefinedBlock().getParameters().size());
        Assert.assertEquals("OREKIT", ocm.getData().getUserDefinedBlock().getParameters().get("GENERATOR"));
    }

    @Test
    public void testParseOCM1() {
        final String   name  = "/ccsds/odm/ocm/OCMExample1.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Ocm file = new ParserBuilder().
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

        // trajectory data
        Assert.assertEquals(1, file.getData().getOTrajectoryBlocks().size());
        TrajectoryStateHistory history = file.getData().getOTrajectoryBlocks().get(0);
        Assert.assertEquals("intervening data records omitted between DT=20.0 and DT=500.0",
                            history.getMetadata().getComments().get(0));
        Assert.assertEquals("OSCULATING", history.getMetadata().getOrbAveraging());
        Assert.assertEquals("EARTH", history.getMetadata().getCenter().getName());
        Assert.assertEquals(CelestialBodyFrame.ITRF2000, history.getMetadata().getTrajReferenceFrame().asCelestialBodyFrame());
        Assert.assertEquals(ElementsType.CARTPV, history.getMetadata().getTrajType());
        Assert.assertEquals(0.0, file.getMetadata().getEpochT0().durationFrom(t0), 1.0e-15);
        List<TrajectoryState> states = history.getTrajectoryStates();
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

        Assert.assertEquals(1, file.getSatellites().size());
        Assert.assertEquals("UNKNOWN", file.getSatellites().entrySet().iterator().next().getKey());

    }

    @Test
    public void testParseOCM2KVN() {
        final String  name = "/ccsds/odm/ocm/OCMExample2.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Ocm file = new ParserBuilder().
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

        // check trajectory data
        Assert.assertEquals(1, file.getData().getOTrajectoryBlocks().size());
        final TrajectoryStateHistory orb = file.getData().getOTrajectoryBlocks().get(0);
        Assert.assertEquals(2, orb.getMetadata().getComments().size());
        Assert.assertEquals("GEOCENTRIC, CARTESIAN, EARTH FIXED", orb.getMetadata().getComments().get(0));
        Assert.assertEquals("THIS IS MY SECOND COMMENT LINE",     orb.getMetadata().getComments().get(1));
        Assert.assertEquals("PREDICTED", orb.getMetadata().getTrajBasis());
        Assert.assertEquals("EFG", orb.getMetadata().getTrajReferenceFrame().getName());
        Assert.assertNull(orb.getMetadata().getTrajReferenceFrame().asFrame());
        Assert.assertNull(orb.getMetadata().getTrajReferenceFrame().asCelestialBodyFrame());
        Assert.assertNull(orb.getMetadata().getTrajReferenceFrame().asOrbitRelativeFrame());
        Assert.assertNull(orb.getMetadata().getTrajReferenceFrame().asSpacecraftBodyFrame());
        Assert.assertEquals(ElementsType.CARTPV, orb.getMetadata().getTrajType());
        Assert.assertEquals(6, orb.getMetadata().getTrajUnits().size());
        Assert.assertEquals("km",   orb.getMetadata().getTrajUnits().get(0).getName());
        Assert.assertEquals("km",   orb.getMetadata().getTrajUnits().get(1).getName());
        Assert.assertEquals("km",   orb.getMetadata().getTrajUnits().get(2).getName());
        Assert.assertEquals("km/s", orb.getMetadata().getTrajUnits().get(3).getName());
        Assert.assertEquals("km/s", orb.getMetadata().getTrajUnits().get(4).getName());
        Assert.assertEquals("km/s", orb.getMetadata().getTrajUnits().get(5).getName());
        Assert.assertEquals(1, orb.getTrajectoryStates().size());
        Assert.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 25.1172, ts),
                            orb.getTrajectoryStates().get(0).getDate());
        Assert.assertEquals( 2854533.0, orb.getTrajectoryStates().get(0).getElements()[0], 1.0e-10);
        Assert.assertEquals(-2916187.0, orb.getTrajectoryStates().get(0).getElements()[1], 1.0e-10);
        Assert.assertEquals(-5360774.0, orb.getTrajectoryStates().get(0).getElements()[2], 1.0e-10);
        Assert.assertEquals(    5688.0, orb.getTrajectoryStates().get(0).getElements()[3], 1.0e-10);
        Assert.assertEquals(    4652.0, orb.getTrajectoryStates().get(0).getElements()[4], 1.0e-10);
        Assert.assertEquals(     520.0, orb.getTrajectoryStates().get(0).getElements()[5], 1.0e-10);

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
        Assert.assertEquals( 12.0, Units.NANO_TESLA.fromSI(perts.getFixedGeomagneticKp()), 1.0e-10);
        Assert.assertEquals(105.0, Unit.SOLAR_FLUX_UNIT.fromSI(perts.getFixedF10P7()),     1.0e-10);
        Assert.assertEquals(120.0, Unit.SOLAR_FLUX_UNIT.fromSI(perts.getFixedF10P7Mean()), 1.0e-10);

        // check no orbit determination
        Assert.assertNull(file.getData().getOrbitDeterminationBlock());

        // check user data
        UserDefined user = file.getData().getUserDefinedBlock();
        Assert.assertTrue(user.getComments().isEmpty());
        Assert.assertEquals(1, user.getParameters().size());
        Assert.assertEquals("MAXWELL RAFERTY", user.getParameters().get("CONSOLE_POC"));

        Assert.assertEquals(1, file.getSatellites().size());
        Assert.assertEquals("OSPREY 5", file.getSatellites().entrySet().iterator().next().getKey());

    }

    @Test
    public void testParseOCM2XMLBinary() {
        final String  name = "/ccsds/odm/ocm/OCMExample2.xml";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        validateOCM2XML(new ParserBuilder().
                        withMu(Constants.EIGEN5C_EARTH_MU).
                        buildOcmParser().
                        parseMessage(source));
    }

    @Test
    public void testParseOCM2XMLCharacter() {
        final String  name = "/ccsds/odm/ocm/OCMExample2.xml";
        final DataSource source = new DataSource(name, () -> new InputStreamReader(getClass().getResourceAsStream(name), StandardCharsets.UTF_8));
        validateOCM2XML(new ParserBuilder().
                        withMu(Constants.EIGEN5C_EARTH_MU).
                        buildOcmParser().
                        parseMessage(source));
    }

    @Test
    public void testWriteOCM2() throws URISyntaxException, IOException {
        final String name = "/ccsds/odm/ocm/OCMExample2.xml";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        OcmParser parser = new ParserBuilder(). withMu(Constants.EIGEN5C_EARTH_MU).buildOcmParser();
        final Ocm original = parser.parseMessage(source);

        // write the parsed file back to a characters array
        final CharArrayWriter caw = new CharArrayWriter();
        final Generator generator = new KvnGenerator(caw, OcmWriter.KVN_PADDING_WIDTH, "dummy", 60);
        new WriterBuilder().buildOcmWriter().writeMessage(generator, original);

        // reparse the written file
        final byte[]     bytes   = caw.toString().getBytes(StandardCharsets.UTF_8);
        final DataSource source2 = new DataSource(name, () -> new ByteArrayInputStream(bytes));
        final Ocm    rebuilt = new ParserBuilder().buildOcmParser().parseMessage(source2);
        validateOCM2XML(rebuilt);

    }

    private void validateOCM2XML(final Ocm file) {

        // Check Header Block;
        Assert.assertEquals(3.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals("ODM V.3 Example G-2",
                            file.getHeader().getComments().get(0));
        Assert.assertEquals("OCM example with space object characteristics and perturbations.",
                            file.getHeader().getComments().get(1));
        Assert.assertEquals("This OCM reflects the latest conditions post-maneuver A67Z",
                            file.getHeader().getComments().get(2));
        Assert.assertEquals("This example shows the specification of multiple comment lines",
                            file.getHeader().getComments().get(3));
        Assert.assertEquals(new AbsoluteDate(1998, 11, 06, 9, 23, 57, TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assert.assertEquals("JAXA", file.getHeader().getOriginator());
        Assert.assertEquals("OCM 201113719185", file.getHeader().getMessageId());

        // Check metadata
        Assert.assertNull(file.getMetadata().getObjectName());
        Assert.assertNull(file.getMetadata().getObjectDesignator());
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

        // check trajectory data
        Assert.assertEquals(1, file.getData().getOTrajectoryBlocks().size());
        final TrajectoryStateHistory orb = file.getData().getOTrajectoryBlocks().get(0);
        Assert.assertEquals(2, orb.getMetadata().getComments().size());
        Assert.assertEquals("GEOCENTRIC, CARTESIAN, EARTH FIXED", orb.getMetadata().getComments().get(0));
        Assert.assertEquals("THIS IS MY SECOND COMMENT LINE",     orb.getMetadata().getComments().get(1));
        Assert.assertEquals("PREDICTED", orb.getMetadata().getTrajBasis());
        Assert.assertEquals("EFG", orb.getMetadata().getTrajReferenceFrame().getName());
        Assert.assertNull(orb.getMetadata().getTrajReferenceFrame().asFrame());
        Assert.assertNull(orb.getMetadata().getTrajReferenceFrame().asCelestialBodyFrame());
        Assert.assertNull(orb.getMetadata().getTrajReferenceFrame().asOrbitRelativeFrame());
        Assert.assertNull(orb.getMetadata().getTrajReferenceFrame().asSpacecraftBodyFrame());
        Assert.assertEquals(ElementsType.CARTPVA, orb.getMetadata().getTrajType());
        Assert.assertNull(orb.getMetadata().getTrajUnits());
        Assert.assertEquals(1, orb.getTrajectoryStates().size());
        Assert.assertEquals(new AbsoluteDate(1998, 12, 18, 0, 0, 0.0, ts),
                            orb.getTrajectoryStates().get(0).getDate());
        Assert.assertEquals( 2854500.0, orb.getTrajectoryStates().get(0).getElements()[0], 1.0e-10);
        Assert.assertEquals(-2916200.0, orb.getTrajectoryStates().get(0).getElements()[1], 1.0e-10);
        Assert.assertEquals(-5360700.0, orb.getTrajectoryStates().get(0).getElements()[2], 1.0e-10);
        Assert.assertEquals(    5900.0, orb.getTrajectoryStates().get(0).getElements()[3], 1.0e-10);
        Assert.assertEquals(    4860.0, orb.getTrajectoryStates().get(0).getElements()[4], 1.0e-10);
        Assert.assertEquals(     520.0, orb.getTrajectoryStates().get(0).getElements()[5], 1.0e-10);
        Assert.assertEquals(       3.7, orb.getTrajectoryStates().get(0).getElements()[6], 1.0e-10);
        Assert.assertEquals(      -3.8, orb.getTrajectoryStates().get(0).getElements()[7], 1.0e-10);
        Assert.assertEquals(      -7.0, orb.getTrajectoryStates().get(0).getElements()[8], 1.0e-10);

        // check physical data
        PhysicalProperties phys = file.getData().getPhysicBlock();
        Assert.assertEquals(1, phys.getComments().size());
        Assert.assertEquals("Spacecraft Physical Characteristics", phys.getComments().get(0));
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
        Assert.assertEquals("Perturbations Specification", perts.getComments().get(0));
        Assert.assertEquals("NRLMSIS00", perts.getAtmosphericModel());
        Assert.assertEquals("EGM-96", perts.getGravityModel());
        Assert.assertEquals(36, perts.getGravityDegree());
        Assert.assertEquals(36, perts.getGravityOrder());
        Assert.assertEquals(36, perts.getGravityOrder());
        Assert.assertEquals(3.986004415e14, perts.getGm(), 1.0);
        Assert.assertEquals("MOON", perts.getNBodyPerturbations().get(0).getName());
        Assert.assertEquals("SUN",  perts.getNBodyPerturbations().get(1).getName());
        Assert.assertEquals( 12.0, Units.NANO_TESLA.fromSI(perts.getFixedGeomagneticKp()), 1.0e-10);
        Assert.assertEquals(105.0, Unit.SOLAR_FLUX_UNIT.fromSI(perts.getFixedF10P7()),     1.0e-10);
        Assert.assertEquals(120.0, Unit.SOLAR_FLUX_UNIT.fromSI(perts.getFixedF10P7Mean()), 1.0e-10);

        // check no orbit determination
        Assert.assertNull(file.getData().getOrbitDeterminationBlock());

        // check user data
        UserDefined user = file.getData().getUserDefinedBlock();
        Assert.assertTrue(user.getComments().isEmpty());
        Assert.assertEquals(1, user.getParameters().size());
        Assert.assertEquals("WGS-84", user.getParameters().get("EARTH_MODEL"));

        Assert.assertEquals(1, file.getSatellites().size());
        Assert.assertEquals("1998-999A", file.getSatellites().entrySet().iterator().next().getKey());
    }

    @Test
    public void testParseOCM3() throws IOException {
        final String   name  = "/ccsds/odm/ocm/OCMExample3.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Ocm file = new ParserBuilder().
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

        // check trajectory data
        Assert.assertEquals(1, file.getData().getOTrajectoryBlocks().size());
        final TrajectoryStateHistory orb = file.getData().getOTrajectoryBlocks().get(0);
        Assert.assertEquals(2, orb.getMetadata().getComments().size());
        Assert.assertEquals("ORBIT EPHEMERIS INCORPORATING DEPLOYMENTS AND MANEUVERS (BELOW)", orb.getMetadata().getComments().get(0));
        Assert.assertEquals("intervening data records omitted after DT=20.0",     orb.getMetadata().getComments().get(1));
        Assert.assertEquals("PREDICTED", orb.getMetadata().getTrajBasis()); // default value, not present in the file
        Assert.assertEquals("TOD", orb.getMetadata().getTrajReferenceFrame().getName());
        Assert.assertNotNull(orb.getMetadata().getTrajReferenceFrame().asFrame());
        Assert.assertEquals(CelestialBodyFrame.TOD, orb.getMetadata().getTrajReferenceFrame().asCelestialBodyFrame());
        Assert.assertNull(orb.getMetadata().getTrajReferenceFrame().asOrbitRelativeFrame());
        Assert.assertNull(orb.getMetadata().getTrajReferenceFrame().asSpacecraftBodyFrame());
        Assert.assertEquals(t0, orb.getMetadata().getTrajFrameEpoch());
        Assert.assertEquals(ElementsType.CARTPVA, orb.getMetadata().getTrajType());
        Assert.assertEquals(9, orb.getMetadata().getTrajUnits().size());
        Assert.assertEquals("km",      orb.getMetadata().getTrajUnits().get(0).getName());
        Assert.assertEquals("km",      orb.getMetadata().getTrajUnits().get(1).getName());
        Assert.assertEquals("km",      orb.getMetadata().getTrajUnits().get(2).getName());
        Assert.assertEquals("km/s",    orb.getMetadata().getTrajUnits().get(3).getName());
        Assert.assertEquals("km/s",    orb.getMetadata().getTrajUnits().get(4).getName());
        Assert.assertEquals("km/s",    orb.getMetadata().getTrajUnits().get(5).getName());
        Assert.assertEquals("km/s**2", orb.getMetadata().getTrajUnits().get(6).getName());
        Assert.assertEquals("km/s**2", orb.getMetadata().getTrajUnits().get(7).getName());
        Assert.assertEquals("km/s**2", orb.getMetadata().getTrajUnits().get(8).getName());
        Assert.assertEquals(4, orb.getTrajectoryStates().size());
        Assert.assertEquals(       0.0,  orb.getTrajectoryStates().get(0).getDate().durationFrom(t0), 1.0e-10);
        Assert.assertEquals( 2789600.0,  orb.getTrajectoryStates().get(0).getElements()[0], 1.0e-10);
        Assert.assertEquals( -280000.0,  orb.getTrajectoryStates().get(0).getElements()[1], 1.0e-10);
        Assert.assertEquals(-1746800.0,  orb.getTrajectoryStates().get(0).getElements()[2], 1.0e-10);
        Assert.assertEquals(    4730.0,  orb.getTrajectoryStates().get(0).getElements()[3], 1.0e-10);
        Assert.assertEquals(   -2500.0,  orb.getTrajectoryStates().get(0).getElements()[4], 1.0e-10);
        Assert.assertEquals(   -1040.0,  orb.getTrajectoryStates().get(0).getElements()[5], 1.0e-10);
        Assert.assertEquals(       8.0,  orb.getTrajectoryStates().get(0).getElements()[6], 1.0e-10);
        Assert.assertEquals(       1.0,  orb.getTrajectoryStates().get(0).getElements()[7], 1.0e-10);
        Assert.assertEquals(    -159.0,  orb.getTrajectoryStates().get(0).getElements()[8], 1.0e-10);
        Assert.assertEquals(      10.0,  orb.getTrajectoryStates().get(1).getDate().durationFrom(t0), 1.0e-10);
        Assert.assertEquals( 2783400.0,  orb.getTrajectoryStates().get(1).getElements()[0], 1.0e-10);
        Assert.assertEquals( -308100.0,  orb.getTrajectoryStates().get(1).getElements()[1], 1.0e-10);
        Assert.assertEquals(-1877100.0,  orb.getTrajectoryStates().get(1).getElements()[2], 1.0e-10);
        Assert.assertEquals(    5190.0,  orb.getTrajectoryStates().get(1).getElements()[3], 1.0e-10);
        Assert.assertEquals(   -2420.0,  orb.getTrajectoryStates().get(1).getElements()[4], 1.0e-10);
        Assert.assertEquals(   -2000.0,  orb.getTrajectoryStates().get(1).getElements()[5], 1.0e-10);
        Assert.assertEquals(       8.0,  orb.getTrajectoryStates().get(1).getElements()[6], 1.0e-10);
        Assert.assertEquals(       1.0,  orb.getTrajectoryStates().get(1).getElements()[7], 1.0e-10);
        Assert.assertEquals(       1.0,  orb.getTrajectoryStates().get(1).getElements()[8], 1.0e-10);
        Assert.assertEquals(      20.0,  orb.getTrajectoryStates().get(2).getDate().durationFrom(t0), 1.0e-10);
        Assert.assertEquals( 2776000.0,  orb.getTrajectoryStates().get(2).getElements()[0], 1.0e-10);
        Assert.assertEquals( -336900.0,  orb.getTrajectoryStates().get(2).getElements()[1], 1.0e-10);
        Assert.assertEquals(-2008700.0,  orb.getTrajectoryStates().get(2).getElements()[2], 1.0e-10);
        Assert.assertEquals(    5640.0,  orb.getTrajectoryStates().get(2).getElements()[3], 1.0e-10);
        Assert.assertEquals(   -2340.0,  orb.getTrajectoryStates().get(2).getElements()[4], 1.0e-10);
        Assert.assertEquals(   -1950.0,  orb.getTrajectoryStates().get(2).getElements()[5], 1.0e-10);
        Assert.assertEquals(       8.0,  orb.getTrajectoryStates().get(2).getElements()[6], 1.0e-10);
        Assert.assertEquals(       1.0,  orb.getTrajectoryStates().get(2).getElements()[7], 1.0e-10);
        Assert.assertEquals(     159.0,  orb.getTrajectoryStates().get(2).getElements()[8], 1.0e-10);
        Assert.assertEquals(     500.0,  orb.getTrajectoryStates().get(3).getDate().durationFrom(t0), 1.0e-10);
        Assert.assertEquals( 2164375.0,  orb.getTrajectoryStates().get(3).getElements()[0], 1.0e-10);
        Assert.assertEquals( 1115811.0,  orb.getTrajectoryStates().get(3).getElements()[1], 1.0e-10);
        Assert.assertEquals( -688131.0,  orb.getTrajectoryStates().get(3).getElements()[2], 1.0e-10);
        Assert.assertEquals(   -3533.28, orb.getTrajectoryStates().get(3).getElements()[3], 1.0e-10);
        Assert.assertEquals(   -2884.52, orb.getTrajectoryStates().get(3).getElements()[4], 1.0e-10);
        Assert.assertEquals(     885.35, orb.getTrajectoryStates().get(3).getElements()[5], 1.0e-10);
        Assert.assertEquals(       0.0,  orb.getTrajectoryStates().get(3).getElements()[6], 1.0e-10);
        Assert.assertEquals(       0.0,  orb.getTrajectoryStates().get(3).getElements()[7], 1.0e-10);
        Assert.assertEquals(       0.0,  orb.getTrajectoryStates().get(3).getElements()[8], 1.0e-10);

        // check physical data
        PhysicalProperties phys = file.getData().getPhysicBlock();
        Assert.assertEquals(1, phys.getComments().size());
        Assert.assertEquals("S/C Physical Characteristics:", phys.getComments().get(0));
        Assert.assertEquals(10.0,    phys.getDragConstantArea(),         1.0e-10);
        Assert.assertEquals(2.3,     phys.getDragCoefficient(),   1.0e-10);
        Assert.assertEquals(100.0,   phys.getWetMass(),                  1.0e-10);
        Assert.assertEquals(4.0,     phys.getSrpConstantArea(),          1.0e-10);
        Assert.assertEquals(1.3,     phys.getSrpCoefficient(),    1.0e-10);

        // check no covariance
        Assert.assertNull(file.getData().getCovarianceBlocks());

        // check maneuvers
        List<ManeuverHistory> man = file.getData().getManeuverBlocks();
        Assert.assertEquals(2, man.size());

        Assert.assertEquals(2, man.get(0).getMetadata().getComments().size());
        Assert.assertEquals("Ten 1kg objects deployed from 200kg host over 100 s timespan", man.get(0).getMetadata().getComments().get(0));
        Assert.assertEquals("20 deg off of back-track direction", man.get(0).getMetadata().getComments().get(1));
        Assert.assertEquals("CUBESAT DEPLOY", man.get(0).getMetadata().getManID());
        Assert.assertEquals(ManBasis.CANDIDATE, man.get(0).getMetadata().getManBasis());
        Assert.assertEquals("DEPLOY",           man.get(0).getMetadata().getManDeviceID());
        Assert.assertEquals(1,                  man.get(0).getMetadata().getManPurpose().size());
        Assert.assertEquals("DEPLOY",           man.get(0).getMetadata().getManPurpose().get(0));
        Assert.assertEquals("RSW_ROTATING",     man.get(0).getMetadata().getManReferenceFrame().getName());
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
        Assert.assertEquals("E W 20160305B",    man.get(1).getMetadata().getManID());
        Assert.assertEquals(ManBasis.CANDIDATE, man.get(1).getMetadata().getManBasis());
        Assert.assertEquals("THR 01",           man.get(1).getMetadata().getManDeviceID());
        Assert.assertEquals(1,                  man.get(1).getMetadata().getManPurpose().size());
        Assert.assertEquals("ORBIT",            man.get(1).getMetadata().getManPurpose().get(0));
        Assert.assertEquals("RSW_ROTATING",     man.get(1).getMetadata().getManReferenceFrame().getName());
        Assert.assertNull(man.get(1).getMetadata().getManReferenceFrame().asFrame());
        Assert.assertNull(man.get(1).getMetadata().getManReferenceFrame().asCelestialBodyFrame());
        Assert.assertEquals(OrbitRelativeFrame.RSW_ROTATING, man.get(1).getMetadata().getManReferenceFrame().asOrbitRelativeFrame());
        Assert.assertEquals(11,                              man.get(1).getMetadata().getManComposition().size());
        Assert.assertEquals(ManeuverFieldType.TIME_ABSOLUTE, man.get(1).getMetadata().getManComposition().get( 0));
        Assert.assertEquals(ManeuverFieldType.TIME_RELATIVE, man.get(1).getMetadata().getManComposition().get( 1));
        Assert.assertEquals(ManeuverFieldType.MAN_DURA,      man.get(1).getMetadata().getManComposition().get( 2));
        Assert.assertEquals(ManeuverFieldType.THR_X,         man.get(1).getMetadata().getManComposition().get( 3));
        Assert.assertEquals(ManeuverFieldType.THR_Y,         man.get(1).getMetadata().getManComposition().get( 4));
        Assert.assertEquals(ManeuverFieldType.THR_Z,         man.get(1).getMetadata().getManComposition().get( 5));
        Assert.assertEquals(ManeuverFieldType.THR_MAG_SIGMA, man.get(1).getMetadata().getManComposition().get( 6));
        Assert.assertEquals(ManeuverFieldType.THR_DIR_SIGMA, man.get(1).getMetadata().getManComposition().get( 7));
        Assert.assertEquals(ManeuverFieldType.THR_INTERP,    man.get(1).getMetadata().getManComposition().get( 8));
        Assert.assertEquals(ManeuverFieldType.THR_ISP,       man.get(1).getMetadata().getManComposition().get( 9));
        Assert.assertEquals(ManeuverFieldType.THR_EFFIC,     man.get(1).getMetadata().getManComposition().get(10));
        Assert.assertEquals(9, man.get(1).getMetadata().getManUnits().size());
        Assert.assertEquals("s",    man.get(1).getMetadata().getManUnits().get(0).getName());
        Assert.assertEquals("N",    man.get(1).getMetadata().getManUnits().get(1).getName());
        Assert.assertEquals("N",    man.get(1).getMetadata().getManUnits().get(2).getName());
        Assert.assertEquals("N",    man.get(1).getMetadata().getManUnits().get(3).getName());
        Assert.assertEquals("%",    man.get(1).getMetadata().getManUnits().get(4).getName());
        Assert.assertEquals("deg",  man.get(1).getMetadata().getManUnits().get(5).getName());
        Assert.assertEquals("n/a",  man.get(1).getMetadata().getManUnits().get(6).getName());
        Assert.assertEquals("s",    man.get(1).getMetadata().getManUnits().get(7).getName());
        Assert.assertEquals("n/a",  man.get(1).getMetadata().getManUnits().get(8).getName());
        Assert.assertEquals(1,      man.get(1).getManeuvers().size());
        Assert.assertEquals(500.0,        man.get(1).getManeuvers().get(0).getDate().durationFrom(t0), 1.0e-10);
        Assert.assertEquals(100,          man.get(1).getManeuvers().get(0).getDuration(),         1.0e-10);
        Assert.assertEquals(  0.0,        man.get(1).getManeuvers().get(0).getThrust().getX(),    1.0e-10);
        Assert.assertEquals(  0.5,        man.get(1).getManeuvers().get(0).getThrust().getY(),    1.0e-10);
        Assert.assertEquals( 0.05,        man.get(1).getManeuvers().get(0).getThrustMagnitudeSigma(), 1.0e-10);
        Assert.assertEquals( 1.0,         FastMath.toDegrees(man.get(1).getManeuvers().get(0).getThrustDirectionSigma()), 1.0e-10);
        Assert.assertEquals(OnOff.ON,     man.get(1).getManeuvers().get(0).getThrustInterpolation());
        Assert.assertTrue(man.get(1).getManeuvers().get(0).getThrustInterpolation().isOn());
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
        Assert.assertEquals("OOD #10059",    od.getId());
        Assert.assertEquals("OOD #10058",    od.getPrevId());
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
        final Ocm file = new ParserBuilder().
                             withMu(Constants.IERS2003_EARTH_MU).
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
        Assert.assertEquals("POLYSAT",                             file.getMetadata().getObjectName());
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
        final AbsoluteDate epoch = file.getMetadata().getEpochT0();
        Assert.assertEquals(new AbsoluteDate(2019, 7, 23,  0, 0, 0.0, TimeScalesFactory.getUTC()), epoch);
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

        // check trajectory data
        Assert.assertEquals(2, file.getData().getOTrajectoryBlocks().size());
        TrajectoryStateHistory osh0 = file.getData().getOTrajectoryBlocks().get(0);
        Assert.assertEquals("this is number 1 ORB comment", osh0.getMetadata().getComments().get(0));
        Assert.assertEquals("orbit 1",       osh0.getMetadata().getTrajID());
        Assert.assertEquals("orbit 0",       osh0.getMetadata().getTrajPrevID());
        Assert.assertEquals("orbit 2",       osh0.getMetadata().getTrajNextID());
        Assert.assertEquals("DETERMINED OD", osh0.getMetadata().getTrajBasis());
        Assert.assertEquals("OD 17",         osh0.getMetadata().getTrajBasisID());
        Assert.assertEquals(InterpolationMethod.HERMITE, osh0.getMetadata().getInterpolationMethod());
        Assert.assertEquals(3, osh0.getMetadata().getInterpolationDegree());
        Assert.assertEquals("ECKSTEIN HECHLER", osh0.getMetadata().getOrbAveraging());
        Assert.assertEquals(CenterName.EARTH.name(), osh0.getMetadata().getCenter().getName());
        Assert.assertEquals(CelestialBodyFrame.TOD, osh0.getMetadata().getTrajReferenceFrame().asCelestialBodyFrame());
        Assert.assertEquals(    0.0,   osh0.getMetadata().getTrajFrameEpoch().durationFrom(epoch),    1.0e-12);
        Assert.assertEquals(34200.0,   osh0.getMetadata().getUseableStartTime().durationFrom(epoch), 1.0e-12);
        Assert.assertEquals(35999.999, osh0.getMetadata().getUseableStopTime().durationFrom(epoch),  1.0e-12);
        Assert.assertEquals(17, osh0.getMetadata().getOrbRevNum());
        Assert.assertEquals( 1, osh0.getMetadata().getOrbRevNumBasis());
        Assert.assertEquals(ElementsType.CARTPVA, osh0.getMetadata().getTrajType());
        Assert.assertEquals(9, osh0.getMetadata().getTrajUnits().size());
        Assert.assertEquals(Unit.KILOMETRE,  osh0.getMetadata().getTrajUnits().get(0));
        Assert.assertEquals(Unit.KILOMETRE,  osh0.getMetadata().getTrajUnits().get(1));
        Assert.assertEquals(Unit.KILOMETRE,  osh0.getMetadata().getTrajUnits().get(2));
        Assert.assertEquals(Units.KM_PER_S,  osh0.getMetadata().getTrajUnits().get(3));
        Assert.assertEquals(Units.KM_PER_S,  osh0.getMetadata().getTrajUnits().get(4));
        Assert.assertEquals(Units.KM_PER_S,  osh0.getMetadata().getTrajUnits().get(5));
        Assert.assertEquals(Units.KM_PER_S2, osh0.getMetadata().getTrajUnits().get(6));
        Assert.assertEquals(Units.KM_PER_S2, osh0.getMetadata().getTrajUnits().get(7));
        Assert.assertEquals(Units.KM_PER_S2, osh0.getMetadata().getTrajUnits().get(8));
        Assert.assertEquals(3, osh0.getCoordinates().size());
        Assert.assertEquals(   0.0, osh0.getCoordinates().get(0).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals( 1.0e6, osh0.getCoordinates().get(0).getPosition().getX(), 1.0e-10);
        Assert.assertEquals( 300.0, osh0.getCoordinates().get(1).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals( 3.0e3, osh0.getCoordinates().get(1).getVelocity().getY(), 1.0e-10);
        Assert.assertEquals( 600.0, osh0.getCoordinates().get(2).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(  -6.0, osh0.getCoordinates().get(2).getAcceleration().getZ(), 1.0e-10);
        TrajectoryStateHistory osh1 = file.getData().getOTrajectoryBlocks().get(1);
        Assert.assertEquals("this is number 2 ORB comment", osh1.getMetadata().getComments().get(0));
        Assert.assertEquals("orbit 2",    osh1.getMetadata().getTrajID());
        Assert.assertEquals("orbit 1",    osh1.getMetadata().getTrajPrevID());
        Assert.assertEquals("orbit 3",    osh1.getMetadata().getTrajNextID());
        Assert.assertEquals("PREDICTED",  osh1.getMetadata().getTrajBasis());
        Assert.assertEquals("SIMULATION", osh1.getMetadata().getTrajBasisID());
        Assert.assertEquals(-1,           osh1.getMetadata().getOrbRevNum());
        Assert.assertEquals(-1,           osh1.getMetadata().getOrbRevNumBasis());
        Assert.assertEquals(3, osh1.getCoordinates().size());
        Assert.assertEquals(1800.0, osh1.getCoordinates().get(0).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(2100.0, osh1.getCoordinates().get(1).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(2400.0, osh1.getCoordinates().get(2).getDate().durationFrom(epoch), 1.0e-10);

        // check physical data
        PhysicalProperties phys = file.getData().getPhysicBlock();
        Assert.assertEquals("this is PHYS comment", phys.getComments().get(0));
        Assert.assertEquals("AIRBUS",   phys.getManufacturer());
        Assert.assertEquals("EUROSTAR", phys.getBusModel());
        Assert.assertEquals(3,          phys.getDockedWith().size());
        Assert.assertEquals("A1",       phys.getDockedWith().get(0));
        Assert.assertEquals("A2",       phys.getDockedWith().get(1));
        Assert.assertEquals("A3",       phys.getDockedWith().get(2));
        Assert.assertEquals(5.0,        phys.getDragConstantArea(),               1.0e-10);
        Assert.assertEquals(2.1,        phys.getDragCoefficient(),                1.0e-10);
        Assert.assertEquals(0.1,        phys.getDragUncertainty(),                1.0e-10);
        Assert.assertEquals(700.0,      phys.getInitialWetMass(),                 1.0e-10);
        Assert.assertEquals(600.0,      phys.getWetMass(),                        1.0e-10);
        Assert.assertEquals(500.0,      phys.getDryMass(),                        1.0e-10);
        Assert.assertEquals(OrbitRelativeFrame.RSW_ROTATING, phys.getOebParentFrame().asOrbitRelativeFrame());
        Assert.assertEquals(32400.0,    phys.getOebParentFrameEpoch().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(0.64,       phys.getOebQ().getQ1(),                   1.0e-10);
        Assert.assertEquals(0.48,       phys.getOebQ().getQ2(),                   1.0e-10);
        Assert.assertEquals(0.48,       phys.getOebQ().getQ3(),                   1.0e-10);
        Assert.assertEquals(0.36,       phys.getOebQ().getQ0(),                   1.0e-10);
        Assert.assertEquals(3.0,        phys.getOebMax(),                         1.0e-10);
        Assert.assertEquals(2.0,        phys.getOebIntermediate(),                1.0e-10);
        Assert.assertEquals(1.0,        phys.getOebMin(),                         1.0e-10);
        Assert.assertEquals(2.2,        phys.getOebAreaAlongMax(),                1.0e-10);
        Assert.assertEquals(3.2,        phys.getOebAreaAlongIntermediate(),       1.0e-10);
        Assert.assertEquals(6.2,        phys.getOebAreaAlongMin(),                1.0e-10);
        Assert.assertEquals(4.3,        phys.getMinAreaForCollisionProbability(), 1.0e-10);
        Assert.assertEquals(6.3,        phys.getMaxAreaForCollisionProbability(), 1.0e-10);
        Assert.assertEquals(5.3,        phys.getTypAreaForCollisionProbability(), 1.0e-10);
        Assert.assertEquals(2.4,        phys.getRcs(),                            1.0e-10);
        Assert.assertEquals(1.4,        phys.getMinRcs(),                         1.0e-10);
        Assert.assertEquals(3.4,        phys.getMaxRcs(),                         1.0e-10);
        Assert.assertEquals(3.5,        phys.getSrpConstantArea(),                1.0e-10);
        Assert.assertEquals(1.7,        phys.getSrpCoefficient(),                 1.0e-10);
        Assert.assertEquals(0.2,        phys.getSrpUncertainty(),                 1.0e-10);
        Assert.assertEquals(15.0,       phys.getVmAbsolute(),                     1.0e-10);
        Assert.assertEquals(19.0,       phys.getVmApparentMin(),                  1.0e-10);
        Assert.assertEquals(15.4,       phys.getVmApparent(),                     1.0e-10);
        Assert.assertEquals(14.0,       phys.getVmApparentMax(),                  1.0e-10);
        Assert.assertEquals(0.7,        phys.getReflectivity(),                   1.0e-10);
        Assert.assertEquals("THREE AXIS",      phys.getAttitudeControlMode());
        Assert.assertEquals("REACTION WHEELS", phys.getAttitudeActuatorType());
        Assert.assertEquals(0.3, FastMath.toDegrees(phys.getAttitudeKnowledgeAccuracy()), 1.0e-10);
        Assert.assertEquals(2.0, FastMath.toDegrees(phys.getAttitudeControlAccuracy()),   1.0e-10);
        Assert.assertEquals(2.3, FastMath.toDegrees(phys.getAttitudePointingAccuracy()),  1.0e-10);
        Assert.assertEquals(20.0,       phys.getManeuversPerYear(),             1.0e-10);
        Assert.assertEquals(6.8,        phys.getMaxThrust(),                    1.0e-10);
        Assert.assertEquals(1900.0,     phys.getBolDv(),                        1.0e-10);
        Assert.assertEquals(200.0,      phys.getRemainingDv(),                  1.0e-10);
        Assert.assertEquals(1000.0,     phys.getInertiaMatrix().getEntry(0, 0), 1.0e-10);
        Assert.assertEquals( 800.0,     phys.getInertiaMatrix().getEntry(1, 1), 1.0e-10);
        Assert.assertEquals( 400.0,     phys.getInertiaMatrix().getEntry(2, 2), 1.0e-10);
        Assert.assertEquals(  20.0,     phys.getInertiaMatrix().getEntry(0, 1), 1.0e-10);
        Assert.assertEquals(  20.0,     phys.getInertiaMatrix().getEntry(1, 0), 1.0e-10);
        Assert.assertEquals(  40.0,     phys.getInertiaMatrix().getEntry(0, 2), 1.0e-10);
        Assert.assertEquals(  40.0,     phys.getInertiaMatrix().getEntry(2, 0), 1.0e-10);
        Assert.assertEquals(  60.0,     phys.getInertiaMatrix().getEntry(1, 2), 1.0e-10);
        Assert.assertEquals(  60.0,     phys.getInertiaMatrix().getEntry(2, 1), 1.0e-10);

        // check covariance data
        Assert.assertEquals(2, file.getData().getCovarianceBlocks().size());
        CovarianceHistory ch0 = file.getData().getCovarianceBlocks().get(0);
        Assert.assertEquals("this is number 1 COV comment", ch0.getMetadata().getComments().get(0));
        Assert.assertEquals("covariance 1", ch0.getMetadata().getCovID());
        Assert.assertEquals("covariance 0", ch0.getMetadata().getCovPrevID());
        Assert.assertEquals("covariance 2", ch0.getMetadata().getCovNextID());
        Assert.assertEquals("EMPIRICAL",    ch0.getMetadata().getCovBasis());
        Assert.assertEquals("basis 1",      ch0.getMetadata().getCovBasisID());
        Assert.assertEquals(OrbitRelativeFrame.TNW_INERTIAL, ch0.getMetadata().getCovReferenceFrame().asOrbitRelativeFrame());
        Assert.assertEquals(33000.0,        ch0.getMetadata().getCovFrameEpoch().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(0.5,            ch0.getMetadata().getCovScaleMin(),   1.0e-10);
        Assert.assertEquals(5.0,            ch0.getMetadata().getCovScaleMax(),   1.0e-10);
        Assert.assertEquals(0.25,           ch0.getMetadata().getCovConfidence(), 1.0e-10);
        Assert.assertEquals(ElementsType.CARTPV, ch0.getMetadata().getCovType());
        Assert.assertEquals(6, ch0.getMetadata().getCovUnits().size());
        Assert.assertEquals(Unit.KILOMETRE,  ch0.getMetadata().getCovUnits().get(0));
        Assert.assertEquals(Unit.KILOMETRE,  ch0.getMetadata().getCovUnits().get(1));
        Assert.assertEquals(Unit.KILOMETRE,  ch0.getMetadata().getCovUnits().get(2));
        Assert.assertEquals(Units.KM_PER_S,  ch0.getMetadata().getCovUnits().get(3));
        Assert.assertEquals(Units.KM_PER_S,  ch0.getMetadata().getCovUnits().get(4));
        Assert.assertEquals(Units.KM_PER_S,  ch0.getMetadata().getCovUnits().get(5));
        Assert.assertEquals(3, ch0.getCovariances().size());
        Assert.assertEquals(   0.0,  ch0.getCovariances().get(0).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(1.1e6,   ch0.getCovariances().get(0).getMatrix().getEntry(0, 0),    1.0e-10);
        Assert.assertEquals( 300.0,  ch0.getCovariances().get(1).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(13.2e6,  ch0.getCovariances().get(1).getMatrix().getEntry(2, 1),    1.0e-10);
        Assert.assertEquals( 600.0,  ch0.getCovariances().get(2).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(26.5e6,  ch0.getCovariances().get(2).getMatrix().getEntry(4, 5),    1.0e-10);
        CovarianceHistory ch1 = file.getData().getCovarianceBlocks().get(1);
        Assert.assertEquals("this is number 2 COV comment", ch1.getMetadata().getComments().get(0));
        Assert.assertEquals("covariance 2", ch1.getMetadata().getCovID());
        Assert.assertEquals("covariance 1", ch1.getMetadata().getCovPrevID());
        Assert.assertEquals("covariance 3", ch1.getMetadata().getCovNextID());
        Assert.assertEquals("SIMULATED",    ch1.getMetadata().getCovBasis());
        Assert.assertEquals("basis 2",      ch1.getMetadata().getCovBasisID());
        Assert.assertEquals(1, ch1.getCovariances().size());
        Assert.assertEquals(1800.0, ch1.getCovariances().get(0).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(43.0e6,   ch1.getCovariances().get(0).getMatrix().getEntry(0, 0),    1.0e-10);
        Assert.assertEquals(20.0e6,   ch1.getCovariances().get(0).getMatrix().getEntry(0, 1),    1.0e-10);
        Assert.assertEquals( 6.0e6,   ch1.getCovariances().get(0).getMatrix().getEntry(0, 5),    1.0e-10);
        Assert.assertEquals( 2.0e6,   ch1.getCovariances().get(0).getMatrix().getEntry(4, 3),    1.0e-10);

        // check maneuver data
        Assert.assertEquals(3, file.getData().getManeuverBlocks().size());
        ManeuverHistory m0 = file.getData().getManeuverBlocks().get(0);
        Assert.assertEquals("this is number 1 MAN comment",  m0.getMetadata().getComments().get(0));
        Assert.assertEquals("maneuver 1",                    m0.getMetadata().getManID());
        Assert.assertEquals("maneuver 0",                    m0.getMetadata().getManPrevID());
        Assert.assertEquals("maneuver 2",                    m0.getMetadata().getManNextID());
        Assert.assertEquals(ManBasis.DETERMINED_TLM,         m0.getMetadata().getManBasis());
        Assert.assertEquals("TLM 203",                       m0.getMetadata().getManBasisID());
        Assert.assertEquals("THR 02",                        m0.getMetadata().getManDeviceID());
        Assert.assertEquals(-100.0,                          m0.getMetadata().getManPrevEpoch().durationFrom(epoch), 1.0-10);
        Assert.assertEquals(+100.0,                          m0.getMetadata().getManNextEpoch().durationFrom(epoch), 1.0-10);
        Assert.assertEquals("ORBIT",                         m0.getMetadata().getManPurpose().get(0));
        Assert.assertEquals("OD 5",                          m0.getMetadata().getManPredSource());
        Assert.assertEquals(OrbitRelativeFrame.TNW_INERTIAL, m0.getMetadata().getManReferenceFrame().asOrbitRelativeFrame());
        Assert.assertEquals(2.3,                             m0.getMetadata().getManFrameEpoch().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals("MOON",                          m0.getMetadata().getGravitationalAssist().getName());
        Assert.assertEquals(DutyCycleType.TIME,              m0.getMetadata().getDcType());
        Assert.assertEquals(2.0,                             m0.getMetadata().getDcWindowOpen().durationFrom(epoch),  1.0e-10);
        Assert.assertEquals(100.0,                           m0.getMetadata().getDcWindowClose().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals( 5,                              m0.getMetadata().getDcMinCycles());
        Assert.assertEquals(30,                              m0.getMetadata().getDcMaxCycles());
        Assert.assertEquals( 5.0,                            m0.getMetadata().getDcExecStart().durationFrom(epoch),   1.0e-10);
        Assert.assertEquals(95.0,                            m0.getMetadata().getDcExecStop().durationFrom(epoch),    1.0e-10);
        Assert.assertEquals(8000.0,                          m0.getMetadata().getDcRefTime().durationFrom(epoch),     1.0e-10);
        Assert.assertEquals( 10.0,                           m0.getMetadata().getDcTimePulseDuration(),               1.0e-10);
        Assert.assertEquals(200.0,                           m0.getMetadata().getDcTimePulsePeriod(),                 1.0e-10);
        Assert.assertEquals(23,                              m0.getMetadata().getManComposition().size());
        Assert.assertEquals(ManeuverFieldType.TIME_ABSOLUTE, m0.getMetadata().getManComposition().get( 0));
        Assert.assertEquals(ManeuverFieldType.TIME_RELATIVE, m0.getMetadata().getManComposition().get( 1));
        Assert.assertEquals(ManeuverFieldType.MAN_DURA,      m0.getMetadata().getManComposition().get( 2));
        Assert.assertEquals(ManeuverFieldType.DELTA_MASS,    m0.getMetadata().getManComposition().get( 3));
        Assert.assertEquals(ManeuverFieldType.ACC_X,         m0.getMetadata().getManComposition().get( 4));
        Assert.assertEquals(ManeuverFieldType.ACC_Y,         m0.getMetadata().getManComposition().get( 5));
        Assert.assertEquals(ManeuverFieldType.ACC_Z,         m0.getMetadata().getManComposition().get( 6));
        Assert.assertEquals(ManeuverFieldType.ACC_INTERP,    m0.getMetadata().getManComposition().get( 7));
        Assert.assertEquals(ManeuverFieldType.ACC_MAG_SIGMA, m0.getMetadata().getManComposition().get( 8));
        Assert.assertEquals(ManeuverFieldType.ACC_DIR_SIGMA, m0.getMetadata().getManComposition().get( 9));
        Assert.assertEquals(ManeuverFieldType.DV_X,          m0.getMetadata().getManComposition().get(10));
        Assert.assertEquals(ManeuverFieldType.DV_Y,          m0.getMetadata().getManComposition().get(11));
        Assert.assertEquals(ManeuverFieldType.DV_Z,          m0.getMetadata().getManComposition().get(12));
        Assert.assertEquals(ManeuverFieldType.DV_MAG_SIGMA,  m0.getMetadata().getManComposition().get(13));
        Assert.assertEquals(ManeuverFieldType.DV_DIR_SIGMA,  m0.getMetadata().getManComposition().get(14));
        Assert.assertEquals(ManeuverFieldType.THR_X,         m0.getMetadata().getManComposition().get(15));
        Assert.assertEquals(ManeuverFieldType.THR_Y,         m0.getMetadata().getManComposition().get(16));
        Assert.assertEquals(ManeuverFieldType.THR_Z,         m0.getMetadata().getManComposition().get(17));
        Assert.assertEquals(ManeuverFieldType.THR_EFFIC,     m0.getMetadata().getManComposition().get(18));
        Assert.assertEquals(ManeuverFieldType.THR_INTERP,    m0.getMetadata().getManComposition().get(19));
        Assert.assertEquals(ManeuverFieldType.THR_ISP,       m0.getMetadata().getManComposition().get(20));
        Assert.assertEquals(ManeuverFieldType.THR_MAG_SIGMA, m0.getMetadata().getManComposition().get(21));
        Assert.assertEquals(ManeuverFieldType.THR_DIR_SIGMA, m0.getMetadata().getManComposition().get(22));
        Assert.assertEquals(m0.getMetadata().getManComposition().size() - 2, m0.getMetadata().getManUnits().size());
        for (int i = 0; i < m0.getMetadata().getManUnits().size(); ++i) {
            ManeuverFieldType type = m0.getMetadata().getManComposition().get(i + 2);
            Unit              unit = m0.getMetadata().getManUnits().get(i);
            Assert.assertEquals(type.getUnit(), unit);
        }
        Assert.assertEquals(2, m0.getManeuvers().size());
        Assert.assertEquals(   0.0,   m0.getManeuvers().get(0).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals( 200.0,   m0.getManeuvers().get(0).getDuration(),                 1.0e-10);
        Assert.assertEquals(OnOff.ON, m0.getManeuvers().get(0).getAccelerationInterpolation());
        Assert.assertEquals( 600.0,   m0.getManeuvers().get(1).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(  -5.0,   m0.getManeuvers().get(1).getDeltaMass(),                1.0e-10);

        ManeuverHistory m1 = file.getData().getManeuverBlocks().get(1);
        Assert.assertEquals("this is number 2 MAN comment",  m1.getMetadata().getComments().get(0));
        Assert.assertEquals("maneuver 2",                    m1.getMetadata().getManID());
        Assert.assertEquals("maneuver 1",                    m1.getMetadata().getManPrevID());
        Assert.assertEquals("maneuver 3",                    m1.getMetadata().getManNextID());
        Assert.assertEquals(ManBasis.PLANNED,                m1.getMetadata().getManBasis());
        Assert.assertEquals("analysis 17",                   m1.getMetadata().getManBasisID());
        Assert.assertEquals("THR 07",                        m1.getMetadata().getManDeviceID());
        Assert.assertEquals( 200.0,                          m1.getMetadata().getManPrevEpoch().durationFrom(epoch), 1.0-10);
        Assert.assertEquals( 300.0,                          m1.getMetadata().getManNextEpoch().durationFrom(epoch), 1.0-10);
        Assert.assertEquals("PERIOD",                        m1.getMetadata().getManPurpose().get(0));
        Assert.assertEquals("OD 5",                          m1.getMetadata().getManPredSource());
        Assert.assertEquals(OrbitRelativeFrame.TNW_INERTIAL, m1.getMetadata().getManReferenceFrame().asOrbitRelativeFrame());
        Assert.assertEquals(2.3,                             m1.getMetadata().getManFrameEpoch().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals("EARTH",                         m1.getMetadata().getGravitationalAssist().getName());
        Assert.assertEquals(DutyCycleType.TIME_AND_ANGLE,    m1.getMetadata().getDcType());
        Assert.assertEquals(1002.0,                          m1.getMetadata().getDcWindowOpen().durationFrom(epoch),  1.0e-10);
        Assert.assertEquals(1100.0,                          m1.getMetadata().getDcWindowClose().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(14,                              m1.getMetadata().getDcMinCycles());
        Assert.assertEquals(60,                              m1.getMetadata().getDcMaxCycles());
        Assert.assertEquals(1005.0,                          m1.getMetadata().getDcExecStart().durationFrom(epoch),   1.0e-10);
        Assert.assertEquals(1095.0,                          m1.getMetadata().getDcExecStop().durationFrom(epoch),    1.0e-10);
        Assert.assertEquals(12000.0,                         m1.getMetadata().getDcRefTime().durationFrom(epoch),     1.0e-10);
        Assert.assertEquals( 20.0,                           m1.getMetadata().getDcTimePulseDuration(),               1.0e-10);
        Assert.assertEquals(400.0,                           m1.getMetadata().getDcTimePulsePeriod(),                 1.0e-10);
        Assert.assertEquals(22,                              m1.getMetadata().getManComposition().size());
        Assert.assertEquals(0.0,
                            Vector3D.distance(m1.getMetadata().getDcRefDir(),
                                              Vector3D.PLUS_I),
                            1.0e-10);
        Assert.assertEquals(SpacecraftBodyFrame.BaseEquipment.SENSOR, m1.getMetadata().getDcBodyFrame().getBaseEquipment());
        Assert.assertEquals("3",                             m1.getMetadata().getDcBodyFrame().getLabel());
        Assert.assertEquals(0.0,
                            Vector3D.distance(m1.getMetadata().getDcBodyTrigger(),
                                              new Vector3D(0.707, 0.0, 0.707)),
                            1.0e-10);
        Assert.assertEquals(25.0, FastMath.toDegrees(m1.getMetadata().getDcPhaseStartAngle()), 1.0e-10);
        Assert.assertEquals(35.0, FastMath.toDegrees(m1.getMetadata().getDcPhaseStopAngle()),  1.0e-10);
        Assert.assertEquals(22,                              m1.getMetadata().getManComposition().size());
        Assert.assertEquals(ManeuverFieldType.TIME_RELATIVE, m1.getMetadata().getManComposition().get( 0));
        Assert.assertEquals(ManeuverFieldType.MAN_DURA,      m1.getMetadata().getManComposition().get( 1));
        Assert.assertEquals(ManeuverFieldType.DELTA_MASS,    m1.getMetadata().getManComposition().get( 2));
        Assert.assertEquals(ManeuverFieldType.ACC_X,         m1.getMetadata().getManComposition().get( 3));
        Assert.assertEquals(ManeuverFieldType.ACC_Y,         m1.getMetadata().getManComposition().get( 4));
        Assert.assertEquals(ManeuverFieldType.ACC_Z,         m1.getMetadata().getManComposition().get( 5));
        Assert.assertEquals(ManeuverFieldType.ACC_INTERP,    m1.getMetadata().getManComposition().get( 6));
        Assert.assertEquals(ManeuverFieldType.ACC_MAG_SIGMA, m1.getMetadata().getManComposition().get( 7));
        Assert.assertEquals(ManeuverFieldType.ACC_DIR_SIGMA, m1.getMetadata().getManComposition().get( 8));
        Assert.assertEquals(ManeuverFieldType.DV_X,          m1.getMetadata().getManComposition().get( 9));
        Assert.assertEquals(ManeuverFieldType.DV_Y,          m1.getMetadata().getManComposition().get(10));
        Assert.assertEquals(ManeuverFieldType.DV_Z,          m1.getMetadata().getManComposition().get(11));
        Assert.assertEquals(ManeuverFieldType.DV_MAG_SIGMA,  m1.getMetadata().getManComposition().get(12));
        Assert.assertEquals(ManeuverFieldType.DV_DIR_SIGMA,  m1.getMetadata().getManComposition().get(13));
        Assert.assertEquals(ManeuverFieldType.THR_X,         m1.getMetadata().getManComposition().get(14));
        Assert.assertEquals(ManeuverFieldType.THR_Y,         m1.getMetadata().getManComposition().get(15));
        Assert.assertEquals(ManeuverFieldType.THR_Z,         m1.getMetadata().getManComposition().get(16));
        Assert.assertEquals(ManeuverFieldType.THR_EFFIC,     m1.getMetadata().getManComposition().get(17));
        Assert.assertEquals(ManeuverFieldType.THR_INTERP,    m1.getMetadata().getManComposition().get(18));
        Assert.assertEquals(ManeuverFieldType.THR_ISP,       m1.getMetadata().getManComposition().get(19));
        Assert.assertEquals(ManeuverFieldType.THR_MAG_SIGMA, m1.getMetadata().getManComposition().get(20));
        Assert.assertEquals(ManeuverFieldType.THR_DIR_SIGMA, m1.getMetadata().getManComposition().get(21));
        Assert.assertEquals(m1.getMetadata().getManComposition().size() - 1, m1.getMetadata().getManUnits().size());
        for (int i = 0; i < m1.getMetadata().getManUnits().size(); ++i) {
            ManeuverFieldType type = m1.getMetadata().getManComposition().get(i + 1);
            Unit              unit = m1.getMetadata().getManUnits().get(i);
            Assert.assertEquals(type.getUnit(), unit);
        }
        Assert.assertEquals(2, m1.getManeuvers().size());
        Assert.assertEquals(1000.0,    m1.getManeuvers().get(0).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(0.02,      m1.getManeuvers().get(0).getThrustMagnitudeSigma(),     1.0e-10);
        Assert.assertEquals(4.3,       FastMath.toDegrees(m1.getManeuvers().get(0).getThrustDirectionSigma()), 1.0e-10);
        Assert.assertEquals(OnOff.OFF, m1.getManeuvers().get(0).getAccelerationInterpolation());
        Assert.assertEquals(1600.0,    m1.getManeuvers().get(1).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals( 25.0,     m1.getManeuvers().get(1).getDv().getX(),                1.0e-10);


        ManeuverHistory m2 = file.getData().getManeuverBlocks().get(2);
        Assert.assertEquals("this is number 3 MAN comment",     m2.getMetadata().getComments().get(0));
        Assert.assertEquals("maneuver 3",                       m2.getMetadata().getManID());
        Assert.assertEquals("DEPLOYMENT",                       m2.getMetadata().getManDeviceID());
        Assert.assertEquals(10,                                 m2.getMetadata().getManComposition().size());
        Assert.assertEquals(ManeuverFieldType.TIME_ABSOLUTE,    m2.getMetadata().getManComposition().get( 0));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_ID,        m2.getMetadata().getManComposition().get( 1));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_DV_X,      m2.getMetadata().getManComposition().get( 2));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_DV_Y,      m2.getMetadata().getManComposition().get( 3));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_DV_Z,      m2.getMetadata().getManComposition().get( 4));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_MASS,      m2.getMetadata().getManComposition().get( 5));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_DV_SIGMA,  m2.getMetadata().getManComposition().get( 6));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_DIR_SIGMA, m2.getMetadata().getManComposition().get( 7));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_DV_RATIO,  m2.getMetadata().getManComposition().get( 8));
        Assert.assertEquals(ManeuverFieldType.DEPLOY_DV_CDA,    m2.getMetadata().getManComposition().get( 9));
        Assert.assertEquals(m2.getMetadata().getManComposition().size() - 1, m2.getMetadata().getManUnits().size());
        for (int i = 0; i < m2.getMetadata().getManUnits().size(); ++i) {
            ManeuverFieldType type = m2.getMetadata().getManComposition().get(i + 1);
            Unit              unit = m2.getMetadata().getManUnits().get(i);
            Assert.assertEquals(type.getUnit(), unit);
        }
        Assert.assertEquals(8, m2.getManeuvers().size());
        Assert.assertEquals(35100.0,   m2.getManeuvers().get(0).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals("BEE_1",   m2.getManeuvers().get(0).getDeployId());
        Assert.assertEquals(35160.0,   m2.getManeuvers().get(1).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(    0.3,   m2.getManeuvers().get(1).getDeployDv().getY(),          1.0e-10);

        // check perturbation data
        final Perturbations pert = file.getData().getPerturbationsBlock();
        Assert.assertEquals("this is PERT comment",         pert.getComments().get(0));
        Assert.assertEquals("NRLMSIS00",                    pert.getAtmosphericModel());
        Assert.assertEquals("TEG-4",                        pert.getGravityModel());
        Assert.assertEquals(36,                             pert.getGravityDegree());
        Assert.assertEquals(12,                             pert.getGravityOrder());
        Assert.assertEquals(6378137.0,                      pert.getEquatorialRadius(), 1.0e-9);
        Assert.assertEquals(3.986004415e14,                 pert.getGm(),               1.0);
        Assert.assertEquals("MOON",                         pert.getNBodyPerturbations().get(0).getName());
        Assert.assertEquals("SUN",                          pert.getNBodyPerturbations().get(1).getName());
        Assert.assertEquals(4.17807421629e-3,               FastMath.toDegrees(pert.getCentralBodyRotation()), 1.0e-12);
        Assert.assertEquals(0.00335281066475,               pert.getOblateFlattening(), 1.0e-12);
        Assert.assertEquals("SEMI-DIURNAL",                 pert.getOceanTidesModel());
        Assert.assertEquals("DIURNAL",                      pert.getSolidTidesModel());
        Assert.assertEquals("IERS2010",                     pert.getReductionTheory());
        Assert.assertEquals("KNOCKE",                       pert.getAlbedoModel());
        Assert.assertEquals(100,                            pert.getAlbedoGridSize());
        Assert.assertEquals(ShadowModel.DUAL_CONE,          pert.getShadowModel());
        Assert.assertEquals("EARTH",                        pert.getShadowBodies().get(0).getName());
        Assert.assertEquals("MOON",                         pert.getShadowBodies().get(1).getName());
        Assert.assertEquals("BOX WING",                     pert.getSrpModel());
        Assert.assertEquals("CELESTRAK",                    pert.getSpaceWeatherSource());
        Assert.assertEquals(0.0, pert.getSpaceWeatherEpoch().durationFrom(new AbsoluteDate(2019, 7, 22, TimeScalesFactory.getUTC())), 1.0e-10);
        Assert.assertEquals("LAGRANGE ORDER 5",             pert.getInterpMethodSW());
        Assert.assertEquals(3.2e-9,                         pert.getFixedGeomagneticKp(),  1.0e-20);
        Assert.assertEquals(2.1e-9,                         pert.getFixedGeomagneticAp(),  1.0e-20);
        Assert.assertEquals(-20.0e-9,                       pert.getFixedGeomagneticDst(), 1.0e-20);
        Assert.assertEquals(1.20e-20,                       pert.getFixedF10P7(),          1.0e-30);
        Assert.assertEquals(1.32e-20,                       pert.getFixedF10P7Mean(),      1.0e-30);
        Assert.assertEquals(1.30e-20,                       pert.getFixedM10P7(),          1.0e-30);
        Assert.assertEquals(1.42e-20,                       pert.getFixedM10P7Mean(),      1.0e-30);
        Assert.assertEquals(1.40e-20,                       pert.getFixedS10P7(),          1.0e-30);
        Assert.assertEquals(1.52e-20,                       pert.getFixedS10P7Mean(),      1.0e-30);
        Assert.assertEquals(1.50e-20,                       pert.getFixedY10P7(),          1.0e-30);
        Assert.assertEquals(1.62e-20,                       pert.getFixedY10P7Mean(),      1.0e-30);

        // check orbit determination data
        OrbitDetermination od = file.getData().getOrbitDeterminationBlock();
        Assert.assertEquals("this is OD comment", od.getComments().get(0));
        Assert.assertEquals("OD 24",              od.getId());
        Assert.assertEquals("OD 23",              od.getPrevId());
        Assert.assertEquals("BWLS",               od.getMethod().getName());
        Assert.assertEquals(OdMethodType.BWLS,    od.getMethod().getType());
        Assert.assertEquals("OREKIT",             od.getMethod().getTool());
        Assert.assertEquals(0.0,
                            od.getEpoch().durationFrom(new AbsoluteDate(2019, 7, 22, 17, 32, 27.0,
                                                                        TimeScalesFactory.getUTC())),
                            1.0e-10);
        Assert.assertEquals(302400.0,             od.getTimeSinceFirstObservation(), 1.0e-10);
        Assert.assertEquals(103680.0,             od.getTimeSinceLastObservation(),  1.0e-10);
        Assert.assertEquals(449280.0,             od.getRecommendedOdSpan(),         1.0e-10);
        Assert.assertEquals(198720.0,             od.getActualOdSpan(),              1.0e-10);
        Assert.assertEquals(100,                  od.getObsAvailable());
        Assert.assertEquals( 90,                  od.getObsUsed());
        Assert.assertEquals( 33,                  od.getTracksAvailable());
        Assert.assertEquals( 30,                  od.getTracksUsed());
        Assert.assertEquals(86400.0,              od.getMaximumObsGap(),             1.0e-10);
        Assert.assertEquals(58.73,                od.getEpochEigenMaj(),             1.0e-10);
        Assert.assertEquals(35.7,                 od.getEpochEigenMed(),             1.0e-10);
        Assert.assertEquals(21.5,                 od.getEpochEigenMin(),             1.0e-10);
        Assert.assertEquals(32.5,                 od.getMaxPredictedEigenMaj(),      1.0e-10);
        Assert.assertEquals(22.0,                 od.getMinPredictedEigenMin(),      1.0e-10);
        Assert.assertEquals(0.953,                od.getConfidence(),                1.0e-10);
        Assert.assertEquals(0.857,                od.getGdop(),                      1.0e-10);
        Assert.assertEquals(6,                    od.getSolveN());
        Assert.assertEquals("POS[3]",             od.getSolveStates().get(0));
        Assert.assertEquals("VEL[3]",             od.getSolveStates().get(1));
        Assert.assertEquals(2,                    od.getConsiderN());
        Assert.assertEquals("DRAG",               od.getConsiderParameters().get(0));
        Assert.assertEquals("SRP",                od.getConsiderParameters().get(1));
        Assert.assertEquals(3,                    od.getSensorsN());
        Assert.assertEquals("EGLIN",              od.getSensors().get(0));
        Assert.assertEquals("FYLINGDALES",        od.getSensors().get(1));
        Assert.assertEquals("PLAGNOLE",           od.getSensors().get(2));
        Assert.assertEquals(1.3,                  od.getWeightedRms(), 1.0e-10);
        Assert.assertEquals("RANGE",              od.getDataTypes().get(0));
        Assert.assertEquals("DOPPLER",            od.getDataTypes().get(1));
        Assert.assertEquals("AZEL",               od.getDataTypes().get(2));

        // check user data
        Assert.assertEquals(1, file.getData().getUserDefinedBlock().getParameters().size());
        Assert.assertEquals("OREKIT", file.getData().getUserDefinedBlock().getParameters().get("LIBRARY"));

        Assert.assertEquals(1, file.getSatellites().size());
        OcmSatelliteEphemeris ephemeris = file.getSatellites().get("POLYSAT");
        Assert.assertEquals("POLYSAT", ephemeris.getId());
        Assert.assertEquals(3.986004415e14, ephemeris.getMu(), 1e5);
        Assert.assertEquals(3.0e5, Constants.IERS2003_EARTH_MU - ephemeris.getMu(), 1e0);
        Assert.assertEquals(2, ephemeris.getSegments().size());
        List<TimeStampedPVCoordinates> h0 = ephemeris.getSegments().get(0).getCoordinates();
        Assert.assertEquals(3, h0.size());
        Assert.assertEquals(   0.0, h0.get(0).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals( 300.0, h0.get(1).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals( 600.0, h0.get(2).getDate().durationFrom(epoch), 1.0e-10);
        List<TimeStampedPVCoordinates> h1 = ephemeris.getSegments().get(1).getCoordinates();
        Assert.assertEquals(3, h1.size());
        Assert.assertEquals(1800.0, h1.get(0).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(2100.0, h1.get(1).getDate().durationFrom(epoch), 1.0e-10);
        Assert.assertEquals(2400.0, h1.get(2).getDate().durationFrom(epoch), 1.0e-10);

        Assert.assertEquals(   0.0, ephemeris.getStart().durationFrom(epoch), 10e-10);
        Assert.assertEquals(2400.0, ephemeris.getStop().durationFrom(epoch), 10e-10);

    }

}
