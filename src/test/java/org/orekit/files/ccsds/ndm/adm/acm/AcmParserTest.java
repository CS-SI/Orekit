/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.ccsds.ndm.adm.acm;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.linear.DiagonalMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.AdMethodType;
import org.orekit.files.ccsds.definitions.CenterName;
import org.orekit.files.ccsds.definitions.SpacecraftBodyFrame.BaseEquipment;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.XmlGenerator;
import org.orekit.files.ccsds.utils.lexical.KvnLexicalAnalyzer;
import org.orekit.files.ccsds.utils.lexical.XmlLexicalAnalyzer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class AcmParserTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testNonExistentKvnFile() throws URISyntaxException {
        final String realName = "/ccsds/adm/acm/ACMExample01.txt";
        final String wrongName = realName + "xxxxx";
        final DataSource source = new DataSource(wrongName, () -> getClass().getResourceAsStream(wrongName));
        try {
            new KvnLexicalAnalyzer(source).accept(new ParserBuilder().buildAcmParser());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assertions.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testNonExistentXmlFile() throws URISyntaxException {
        final String realName = "/ccsds/adm/acm/ACMExample01.txt";
        final String wrongName = realName + "xxxxx";
        final DataSource source = new DataSource(wrongName, () -> getClass().getResourceAsStream(wrongName));
        try {
            new XmlLexicalAnalyzer(source).accept(new ParserBuilder().buildAcmParser());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assertions.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testIncompatibleKeys() throws URISyntaxException {
        final String name = "/ccsds/adm/acm/incompatible-keys.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().buildAcmParser().parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_INCOMPATIBLE_KEYS_BOTH_USED, oe.getSpecifier());
            Assertions.assertEquals(AttitudeManeuverKey.MAN_END_TIME, oe.getParts()[0]);
            Assertions.assertEquals(AttitudeManeuverKey.MAN_DURATION, oe.getParts()[1]);
        }
    }

    @Test
    public void testSensorIndexAlreadyUsed() throws URISyntaxException {
        final String name = "/ccsds/adm/acm/sensor-index-already-used.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().buildAcmParser().parse(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_SENSOR_INDEX_ALREADY_USED, oe.getSpecifier());
            Assertions.assertEquals(2, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    @Test
    public void testMissingSensorIndex() throws URISyntaxException {
        final String name = "/ccsds/adm/acm/missing-sensor-index.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().buildAcmParser().parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_MISSING_SENSOR_INDEX, oe.getSpecifier());
            Assertions.assertEquals(3, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    @Test
    public void testWrongStdDevNumber() throws URISyntaxException {
        final String name = "/ccsds/adm/acm/wrong-stddev-number.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().buildAcmParser().parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCONSISTENT_NUMBER_OF_ELEMENTS, oe.getSpecifier());
            Assertions.assertEquals(1, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals(2, ((Integer) oe.getParts()[1]).intValue());
        }
    }

    @Test
    public void testSpuriousMetaDataSection() throws URISyntaxException {
        final String name = "/ccsds/adm/acm/spurious-metadata.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().buildAcmParser().parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(13, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("META", oe.getParts()[2]);
        }
    }

    @Test
    public void testMissingTargetMomentum() throws URISyntaxException {
        final String name = "/ccsds/adm/acm/missing-target-momentum.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().buildAcmParser().parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assertions.assertEquals(AttitudeManeuverKey.TARGET_MOMENTUM.name(), oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingCenterOfPressure() throws URISyntaxException {
        final String name = "/ccsds/adm/acm/missing-center-of-pressure.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().buildAcmParser().parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assertions.assertEquals(AttitudePhysicalPropertiesKey.CP.name(), oe.getParts()[0]);
        }
    }

    @Test
    public void testParseACM01() {
        final String   name  = "/ccsds/adm/acm/ACMExample01.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Acm    acm    = new ParserBuilder().buildAcmParser().parseMessage(source);

        // Check Header Block;
        Assertions.assertEquals(2.0, acm.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals("unrestricted", acm.getHeader().getClassification());
        Assertions.assertEquals(new AbsoluteDate(1998, 11, 06, 9, 23, 57, TimeScalesFactory.getUTC()),
                                acm.getHeader().getCreationDate());
        Assertions.assertEquals("JAXA", acm.getHeader().getOriginator());
        Assertions.assertEquals("A7015Z4", acm.getHeader().getMessageId());

        // metadata
        Assertions.assertEquals("EUROBIRD-4A", acm.getMetadata().getObjectName());
        Assertions.assertEquals("2000-052A",   acm.getMetadata().getInternationalDesignator());
        Assertions.assertEquals(2000,          acm.getMetadata().getLaunchYear());
        Assertions.assertEquals(52,            acm.getMetadata().getLaunchNumber());
        Assertions.assertEquals("A",           acm.getMetadata().getLaunchPiece());
        Assertions.assertEquals("UTC",         acm.getMetadata().getTimeSystem().name());
        Assertions.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172, TimeScalesFactory.getUTC()),
                                acm.getMetadata().getEpochT0());        

        // attitude data
        Assertions.assertEquals(1, acm.getData().getAttitudeBlocks().size());
        AttitudeStateHistory history = acm.getData().getAttitudeBlocks().get(0);
        Assertions.assertTrue(history.getMetadata().getComments().isEmpty());
        Assertions.assertNull(history.getMetadata().getAttID());
        Assertions.assertNull(history.getMetadata().getAttPrevID());
        Assertions.assertNull(history.getMetadata().getAttBasis());
        Assertions.assertNull(history.getMetadata().getAttBasisID());
        Assertions.assertEquals("J2000", history.getMetadata().getEndpoints().getFrameA().getName());
        Assertions.assertEquals(BaseEquipment.SC_BODY, history.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", history.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(4, history.getMetadata().getNbStates());
        Assertions.assertEquals(AttitudeElementsType.QUATERNION, history.getMetadata().getAttitudeType());
        Assertions.assertNull(history.getMetadata().getRateType());
        List<AttitudeState> states = history.getAttitudeStates();
        Assertions.assertEquals(3, states.size());

        Assertions.assertEquals(0.0, states.get(0).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        Assertions.assertEquals(4,   states.get(0).getElements().length);
        Assertions.assertEquals( 0.73566,  states.get(0).getElements()[0], 1.0e-15);
        Assertions.assertEquals(-0.50547,  states.get(0).getElements()[1], 1.0e-15);
        Assertions.assertEquals( 0.41309,  states.get(0).getElements()[2], 1.0e-15);
        Assertions.assertEquals( 0.180707, states.get(0).getElements()[3], 1.0e-15);

        Assertions.assertEquals(0.25, states.get(1).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        Assertions.assertEquals(4,    states.get(1).getElements().length);
        Assertions.assertEquals( 0.73529,  states.get(1).getElements()[0], 1.0e-15);
        Assertions.assertEquals(-0.50531,  states.get(1).getElements()[1], 1.0e-15);
        Assertions.assertEquals( 0.41375,  states.get(1).getElements()[2], 1.0e-15);
        Assertions.assertEquals( 0.181158, states.get(1).getElements()[3], 1.0e-15);

        Assertions.assertEquals(0.50, states.get(2).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        Assertions.assertEquals(4,    states.get(2).getElements().length);
        Assertions.assertEquals( 0.73492,  states.get(2).getElements()[0], 1.0e-15);
        Assertions.assertEquals(-0.50515,  states.get(2).getElements()[1], 1.0e-15);
        Assertions.assertEquals( 0.41441,  states.get(2).getElements()[2], 1.0e-15);
        Assertions.assertEquals( 0.181610, states.get(2).getElements()[3], 1.0e-15);

        Assertions.assertNull(acm.getSegments().get(0).getData().getPhysicBlock());
        Assertions.assertNull(acm.getSegments().get(0).getData().getCovarianceBlocks());
        Assertions.assertNull(acm.getSegments().get(0).getData().getManeuverBlocks());
        Assertions.assertNull(acm.getSegments().get(0).getData().getAttitudeDeterminationBlock());
        Assertions.assertNull(acm.getSegments().get(0).getData().getUserDefinedBlock());

    }

    @Test
    public void testParseACM02() {
        final String   name  = "/ccsds/adm/acm/ACMExample02.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Acm    acm    = new ParserBuilder().buildAcmParser().parseMessage(source);

        // Check Header Block;
        Assertions.assertEquals(2.0, acm.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertNull(acm.getHeader().getClassification());
        Assertions.assertEquals(new AbsoluteDate(2017, 12, 1, TimeScalesFactory.getUTC()),
                                acm.getHeader().getCreationDate());
        Assertions.assertEquals("NASA", acm.getHeader().getOriginator());
        Assertions.assertEquals("A7015Z5", acm.getHeader().getMessageId());

        // metadata
        Assertions.assertEquals("SDO",         acm.getMetadata().getObjectName());
        Assertions.assertEquals("2010-005A",   acm.getMetadata().getInternationalDesignator());
        Assertions.assertEquals(2010,          acm.getMetadata().getLaunchYear());
        Assertions.assertEquals(5,             acm.getMetadata().getLaunchNumber());
        Assertions.assertEquals("A",           acm.getMetadata().getLaunchPiece());
        Assertions.assertEquals("UTC",         acm.getMetadata().getTimeSystem().name());
        Assertions.assertEquals(new AbsoluteDate(2017, 12, 26, 19, 40, 0.0, TimeScalesFactory.getUTC()),
                                acm.getMetadata().getEpochT0());        

        // attitude data
        Assertions.assertEquals(1, acm.getData().getAttitudeBlocks().size());
        AttitudeStateHistory history = acm.getData().getAttitudeBlocks().get(0);
        Assertions.assertEquals("OBC Attitude and Bias during momentum management maneuver",
                                history.getMetadata().getComments().get(0));
        Assertions.assertNull(history.getMetadata().getAttID());
        Assertions.assertNull(history.getMetadata().getAttPrevID());
        Assertions.assertNull(history.getMetadata().getAttBasis());
        Assertions.assertNull(history.getMetadata().getAttBasisID());
        Assertions.assertEquals("J2000", history.getMetadata().getEndpoints().getFrameA().getName());
        Assertions.assertEquals(BaseEquipment.SC_BODY, history.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", history.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(7, history.getMetadata().getNbStates());
        Assertions.assertEquals(AttitudeElementsType.QUATERNION, history.getMetadata().getAttitudeType());
        Assertions.assertEquals(RateElementsType.GYRO_BIAS, history.getMetadata().getRateType());
        List<AttitudeState> states = history.getAttitudeStates();
        Assertions.assertEquals(4, states.size());

        Assertions.assertEquals(0.0, states.get(0).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        Assertions.assertEquals(7,   states.get(0).getElements().length);
        Assertions.assertEquals( 0.1153,    states.get(0).getElements()[0], 1.0e-15);
        Assertions.assertEquals(-0.1424,    states.get(0).getElements()[1], 1.0e-15);
        Assertions.assertEquals( 0.8704,    states.get(0).getElements()[2], 1.0e-15);
        Assertions.assertEquals( 0.4571,    states.get(0).getElements()[3], 1.0e-15);
        Assertions.assertEquals( 2.271e-06, FastMath.toDegrees(states.get(0).getElements()[4]), 1.0e-15);
        Assertions.assertEquals(-4.405e-06, FastMath.toDegrees(states.get(0).getElements()[5]), 1.0e-15);
        Assertions.assertEquals(-3.785e-06, FastMath.toDegrees(states.get(0).getElements()[6]), 1.0e-15);

        Assertions.assertEquals(1, acm.getData().getManeuverBlocks().size());
        AttitudeManeuver man = acm.getData().getManeuverBlocks().get(0);
        Assertions.assertEquals("Momentum management maneuver", man.getComments().get(0));
        Assertions.assertEquals("MOM_DESAT",                    man.getManPurpose());
        Assertions.assertEquals(100.0,                          man.getBeginTime());
        Assertions.assertTrue(Double.isNaN(man.getEndTime()));
        Assertions.assertEquals(450.0,                          man.getDuration());
        Assertions.assertEquals("ATT-THRUSTER",                 man.getActuatorUsed());
        Assertions.assertEquals(  1.3,                          man.getTargetMomentum().getX(), 1.0e-10);
        Assertions.assertEquals(-16.4,                          man.getTargetMomentum().getY(), 1.0e-10);
        Assertions.assertEquals(-11.35,                         man.getTargetMomentum().getZ(), 1.0e-10);
        Assertions.assertNull(man.getTargetAttitude());
        Assertions.assertTrue(Double.isNaN(man.getTargetSpinRate()));

        AttitudeDetermination ad = acm.getData().getAttitudeDeterminationBlock();
        Assertions.assertEquals("SDO Onboard Filter",    ad.getComments().get(0));
        Assertions.assertEquals(AdMethodType.EKF,        ad.getMethod());
        Assertions.assertEquals("OBC",                   ad.getSource());
        Assertions.assertEquals(AttitudeElementsType.QUATERNION, ad.getAttitudeStates());
        Assertions.assertEquals("J2000",                 ad.getEndpoints().getFrameA().getName());
        Assertions.assertEquals(BaseEquipment.SC_BODY,   ad.getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1",                     ad.getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(4,                       ad.getSensorsUsed().size());
        Assertions.assertEquals("AST1",                  ad.getSensorsUsed().get(0).getSensorUsed());
        Assertions.assertEquals("AST2",                  ad.getSensorsUsed().get(1).getSensorUsed());
        Assertions.assertEquals("DSS",                   ad.getSensorsUsed().get(2).getSensorUsed());
        Assertions.assertEquals("IMU",                   ad.getSensorsUsed().get(3).getSensorUsed());

    }

    @Test
    public void testParseACM03() {
        final String     name   = "/ccsds/adm/acm/ACMExample03.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Acm        acm    = new ParserBuilder().buildAcmParser().parseMessage(source);

        // Check Header Block;
        Assertions.assertEquals(2.0, acm.getHeader().getFormatVersion(), 1.0e-10);
       Assertions.assertEquals(new AbsoluteDate(1998, 11, 06, 9, 23, 57, TimeScalesFactory.getUTC()),
                                acm.getHeader().getCreationDate());
        Assertions.assertEquals("JAXA", acm.getHeader().getOriginator());
        Assertions.assertEquals("A7015Z6", acm.getHeader().getMessageId());

        // metadata
        Assertions.assertEquals("TEST_SAT",    acm.getMetadata().getObjectName());
        Assertions.assertNull(acm.getMetadata().getInternationalDesignator());
        Assertions.assertEquals("TAI",         acm.getMetadata().getTimeSystem().name());
        Assertions.assertEquals(new AbsoluteDate(1998, 12, 18, 14, 28, 15.1172, TimeScalesFactory.getTAI()),
                                acm.getMetadata().getEpochT0());  
        Assertions.assertEquals(36.0,          acm.getMetadata().getTaimutcT0(), 1.0e-15);

        Assertions.assertNull(acm.getData().getAttitudeBlocks());
        AttitudePhysicalProperties phys = acm.getData().getPhysicBlock();
        Assertions.assertEquals("Spacecraft Physical Parameters", phys.getComments().get(0));
        Assertions.assertEquals(1916.0, phys.getWetMass(), 1.0e-15);
        Assertions.assertEquals( 0.04,  phys.getCenterOfPressure().getX(),      1.0e-15);
        Assertions.assertEquals(-0.78,  phys.getCenterOfPressure().getY(),      1.0e-15);
        Assertions.assertEquals(-0.023, phys.getCenterOfPressure().getZ(),      1.0e-15);
        Assertions.assertEquals( 752.0, phys.getInertiaMatrix().getEntry(0, 0), 1.0e-15);
        Assertions.assertEquals(1305.0, phys.getInertiaMatrix().getEntry(1, 1), 1.0e-15);
        Assertions.assertEquals(1490.0, phys.getInertiaMatrix().getEntry(2, 2), 1.0e-15);
        Assertions.assertEquals(  81.1, phys.getInertiaMatrix().getEntry(0, 1), 1.0e-15);
        Assertions.assertEquals(  81.1, phys.getInertiaMatrix().getEntry(1, 0), 1.0e-15);
        Assertions.assertEquals( -25.7, phys.getInertiaMatrix().getEntry(0, 2), 1.0e-15);
        Assertions.assertEquals( -25.7, phys.getInertiaMatrix().getEntry(2, 0), 1.0e-15);
        Assertions.assertEquals(  74.1, phys.getInertiaMatrix().getEntry(1, 2), 1.0e-15);
        Assertions.assertEquals(  74.1, phys.getInertiaMatrix().getEntry(2, 1), 1.0e-15);

    }

    @Test
    public void testParseACM04() {
        final String   name  = "/ccsds/adm/acm/ACMExample04.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Acm    acm    = new ParserBuilder().buildAcmParser().parseMessage(source);

        // Check Header Block;
        Assertions.assertEquals(2.0, acm.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2017, 12, 30, TimeScalesFactory.getUTC()),
                                acm.getHeader().getCreationDate());
        Assertions.assertEquals("NASA", acm.getHeader().getOriginator());
        Assertions.assertEquals("A7015Z7", acm.getHeader().getMessageId());

        // metadata
        Assertions.assertEquals("LRO",         acm.getMetadata().getObjectName());
        Assertions.assertEquals("2009-031A",   acm.getMetadata().getInternationalDesignator());
        Assertions.assertEquals(2009,          acm.getMetadata().getLaunchYear());
        Assertions.assertEquals(31,            acm.getMetadata().getLaunchNumber());
        Assertions.assertEquals("A",           acm.getMetadata().getLaunchPiece());
        Assertions.assertEquals("UTC",         acm.getMetadata().getTimeSystem().name());
        Assertions.assertEquals(new AbsoluteDate(2017, 12, 30, 0, 0, 0.0, TimeScalesFactory.getUTC()),
                                acm.getMetadata().getEpochT0()); 
        Assertions.assertEquals(2,             acm.getMetadata().getAcmDataElements().size());
        Assertions.assertEquals(AcmElements.COV, acm.getMetadata().getAcmDataElements().get(0));
        Assertions.assertEquals(AcmElements.AD,  acm.getMetadata().getAcmDataElements().get(1));

        // covariance data
        Assertions.assertEquals(1, acm.getData().getCovarianceBlocks().size());
        AttitudeCovarianceHistory history = acm.getData().getCovarianceBlocks().get(0);
        Assertions.assertEquals("Diagonal Covariance for LRO Onboard Kalman Filter", history.getMetadata().getComments().get(0));
        Assertions.assertEquals("DETERMINED_OBC", history.getMetadata().getCovBasis());
        Assertions.assertEquals(BaseEquipment.SC_BODY, history.getMetadata().getCovReferenceFrame().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", history.getMetadata().getCovReferenceFrame().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(AttitudeCovarianceType.ANGLE_GYROBIAS, history.getMetadata().getCovType());
        List<AttitudeCovariance> covariances = history.getCovariances();
        Assertions.assertEquals(3, covariances.size());

        Assertions.assertEquals(0.0, covariances.get(0).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        DiagonalMatrix m = covariances.get(0).getMatrix();
        Assertions.assertEquals(6,   m.getRowDimension());
        Assertions.assertEquals( 6.74E-11, FastMath.toDegrees(FastMath.toDegrees(m.getEntry(0, 0))), 1.0e-22);
        Assertions.assertEquals( 8.10E-11, FastMath.toDegrees(FastMath.toDegrees(m.getEntry(1, 1))), 1.0e-22);
        Assertions.assertEquals( 9.22E-11, FastMath.toDegrees(FastMath.toDegrees(m.getEntry(2, 2))), 1.0e-22);
        Assertions.assertEquals( 1.11E-15, FastMath.toDegrees(FastMath.toDegrees(m.getEntry(3, 3))), 1.0e-22);
        Assertions.assertEquals( 1.11E-15, FastMath.toDegrees(FastMath.toDegrees(m.getEntry(4, 4))), 1.0e-22);
        Assertions.assertEquals( 1.12E-15, FastMath.toDegrees(FastMath.toDegrees(m.getEntry(5, 5))), 1.0e-22);

        AttitudeDetermination ad = acm.getData().getAttitudeDeterminationBlock();
        Assertions.assertEquals("LRO Onboard Filter, A Multiplicative Extended Kalman Filter", ad.getComments().get(0));
        Assertions.assertEquals(AdMethodType.EKF,                      ad.getMethod());
        Assertions.assertEquals("OBC",                                 ad.getSource());
        Assertions.assertEquals(7,                                     ad.getNbStates());
        Assertions.assertEquals(AttitudeElementsType.QUATERNION,       ad.getAttitudeStates());
        Assertions.assertEquals(AttitudeCovarianceType.ANGLE_GYROBIAS, ad.getCovarianceType());
        Assertions.assertEquals("EME2000",                             ad.getEndpoints().getFrameA().getName());
        Assertions.assertEquals(BaseEquipment.SC_BODY,                 ad.getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1",                                   ad.getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(RateElementsType.GYRO_BIAS,            ad.getRateStates());
        Assertions.assertEquals(3,                                     ad.getSensorsUsed().size());
        Assertions.assertEquals("AST1",                                ad.getSensorsUsed().get(0).getSensorUsed());
        Assertions.assertEquals("AST2",                                ad.getSensorsUsed().get(1).getSensorUsed());
        Assertions.assertEquals("IMU",                                 ad.getSensorsUsed().get(2).getSensorUsed());

    }

    @Test
    public void testParseACM05() {
        final String   name  = "/ccsds/adm/acm/ACMExample05.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        validateAcm05(new ParserBuilder().buildAcmParser().parseMessage(source));
    }

    private void validateAcm05(Acm acm) {

        final AbsoluteDate t0 = new AbsoluteDate(2016, 3, 15, 0, 00, 0.0, TimeScalesFactory.getUTC());

        // Check Header Block;
        Assertions.assertEquals(2.0, acm.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(2, acm.getHeader().getComments().size());
        Assertions.assertEquals("This is an arbitrary test file with probably inconsistent data",
                                acm.getHeader().getComments().get(0));
        Assertions.assertEquals("its purpose is only to exercise all possible entries in ACM files",
                                acm.getHeader().getComments().get(1));
        Assertions.assertEquals("free to use under Orekit license",  acm.getHeader().getClassification());
        Assertions.assertEquals(new AbsoluteDate(2023, 4, 8, 14, 31, 0.0, TimeScalesFactory.getUTC()),
                                acm.getHeader().getCreationDate());
        Assertions.assertEquals("OREKIT", acm.getHeader().getOriginator());
        Assertions.assertEquals("a4830b29-a805-4d31-ab6e-06b57c843323", acm.getHeader().getMessageId());

        // metadata
        Assertions.assertEquals("comment at metadata start",    acm.getMetadata().getComments().get(0));
        Assertions.assertEquals("Korrigan",                     acm.getMetadata().getObjectName());
        Assertions.assertEquals("1703-999Z",                    acm.getMetadata().getInternationalDesignator());
        Assertions.assertEquals(1703,                           acm.getMetadata().getLaunchYear());
        Assertions.assertEquals(999,                            acm.getMetadata().getLaunchNumber());
        Assertions.assertEquals("Z",                            acm.getMetadata().getLaunchPiece());
        Assertions.assertEquals("FAERY",                        acm.getMetadata().getCatalogName());
        Assertions.assertEquals("Korrik-17",                    acm.getMetadata().getObjectDesignator());
        Assertions.assertEquals("Melusine",                     acm.getMetadata().getOriginatorPOC());
        Assertions.assertEquals("Odonatoptera-lead",            acm.getMetadata().getOriginatorPosition());
        Assertions.assertEquals("+9911223344",                  acm.getMetadata().getOriginatorPhone());
        Assertions.assertEquals("melusine@avalon.surreal",      acm.getMetadata().getOriginatorEmail());
        Assertions.assertEquals("-1 dolmen avenue, Stonehenge", acm.getMetadata().getOriginatorAddress());
        Assertions.assertEquals("odm-7c32f8a9c126432f",         acm.getMetadata().getOdmMessageLink());
        Assertions.assertEquals(CenterName.MOON.name(),         acm.getMetadata().getCenter().getName());
        Assertions.assertEquals("UTC",                          acm.getMetadata().getTimeSystem().name());
        Assertions.assertEquals(t0,                             acm.getMetadata().getEpochT0()); 
        Assertions.assertEquals(17, acm.getMetadata().getAcmDataElements().size());
        Assertions.assertEquals(AcmElements.ATT,  acm.getMetadata().getAcmDataElements().get( 0));
        Assertions.assertEquals(AcmElements.ATT,  acm.getMetadata().getAcmDataElements().get( 1));
        Assertions.assertEquals(AcmElements.ATT,  acm.getMetadata().getAcmDataElements().get( 2));
        Assertions.assertEquals(AcmElements.ATT,  acm.getMetadata().getAcmDataElements().get( 3));
        Assertions.assertEquals(AcmElements.ATT,  acm.getMetadata().getAcmDataElements().get( 4));
        Assertions.assertEquals(AcmElements.PHYS, acm.getMetadata().getAcmDataElements().get( 5));
        Assertions.assertEquals(AcmElements.COV,  acm.getMetadata().getAcmDataElements().get( 6));
        Assertions.assertEquals(AcmElements.COV,  acm.getMetadata().getAcmDataElements().get( 7));
        Assertions.assertEquals(AcmElements.COV,  acm.getMetadata().getAcmDataElements().get( 8));
        Assertions.assertEquals(AcmElements.COV,  acm.getMetadata().getAcmDataElements().get( 9));
        Assertions.assertEquals(AcmElements.COV,  acm.getMetadata().getAcmDataElements().get(10));
        Assertions.assertEquals(AcmElements.COV,  acm.getMetadata().getAcmDataElements().get(11));
        Assertions.assertEquals(AcmElements.MAN,  acm.getMetadata().getAcmDataElements().get(12));
        Assertions.assertEquals(AcmElements.MAN,  acm.getMetadata().getAcmDataElements().get(13));
        Assertions.assertEquals(AcmElements.MAN,  acm.getMetadata().getAcmDataElements().get(14));
        Assertions.assertEquals(AcmElements.AD,   acm.getMetadata().getAcmDataElements().get(15));
        Assertions.assertEquals(AcmElements.USER, acm.getMetadata().getAcmDataElements().get(16));
        Assertions.assertEquals(18600.0,          acm.getMetadata().getStartTime().durationFrom(t0), 1.0e-12);
        Assertions.assertEquals(19000.0,          acm.getMetadata().getStopTime().durationFrom(t0), 1.0e-12);
        Assertions.assertEquals(36,               acm.getMetadata().getTaimutcT0());
        Assertions.assertEquals(0.0,
                                acm.getMetadata().getNextLeapEpoch().durationFrom(new AbsoluteDate("2017-01-01", TimeScalesFactory.getUTC())),
                                1.0e-12);
        Assertions.assertEquals(37,               acm.getMetadata().getNextLeapTaimutc());

        Assertions.assertEquals(5, acm.getData().getAttitudeBlocks().size());

        // first attitude block
        AttitudeStateHistory att1 = acm.getData().getAttitudeBlocks().get(0);
        Assertions.assertEquals("first attitude block",          att1.getMetadata().getComments().get(0));
        Assertions.assertEquals("ATT_1",                         att1.getMetadata().getAttID());
        Assertions.assertEquals("ATT_0",                         att1.getMetadata().getAttPrevID());
        Assertions.assertEquals("SIMULATED",                     att1.getMetadata().getAttBasis());
        Assertions.assertEquals("rnd-25",                        att1.getMetadata().getAttBasisID());
        Assertions.assertEquals("EME2000",                       att1.getMetadata().getEndpoints().getFrameA().getName());
        Assertions.assertEquals(BaseEquipment.GYRO,              att1.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("3",                             att1.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(4,                               att1.getMetadata().getNbStates());
        Assertions.assertEquals(AttitudeElementsType.QUATERNION, att1.getMetadata().getAttitudeType());
        Assertions.assertEquals(RateElementsType.NONE,           att1.getMetadata().getRateType());
        Assertions.assertEquals(2,                               att1.getAttitudeStates().size());
        Assertions.assertEquals(0.0,       att1.getAttitudeStates().get(0).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        Assertions.assertEquals(4,         att1.getAttitudeStates().get(0).getElements().length);
        Assertions.assertEquals( 0.73566,  att1.getAttitudeStates().get(0).getElements()[0], 1.0e-15);
        Assertions.assertEquals(-0.50547,  att1.getAttitudeStates().get(0).getElements()[1], 1.0e-15);
        Assertions.assertEquals( 0.41309,  att1.getAttitudeStates().get(0).getElements()[2], 1.0e-15);
        Assertions.assertEquals( 0.180707, att1.getAttitudeStates().get(0).getElements()[3], 1.0e-15);
        Assertions.assertEquals(0.25,      att1.getAttitudeStates().get(1).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        Assertions.assertEquals(4,         att1.getAttitudeStates().get(1).getElements().length);
        Assertions.assertEquals( 0.73529,  att1.getAttitudeStates().get(1).getElements()[0], 1.0e-15);
        Assertions.assertEquals(-0.50531,  att1.getAttitudeStates().get(1).getElements()[1], 1.0e-15);
        Assertions.assertEquals( 0.41375,  att1.getAttitudeStates().get(1).getElements()[2], 1.0e-15);
        Assertions.assertEquals( 0.181158, att1.getAttitudeStates().get(1).getElements()[3], 1.0e-15);
        Assertions.assertEquals(0.0, att1.getAttitudeStates().get(0).toAngular(RotationOrder.XYZ).getRotationRate().getNorm(), 1.0e-10);

        // second attitude block
        AttitudeStateHistory att2 = acm.getData().getAttitudeBlocks().get(1);
        Assertions.assertEquals("second attitude block",           att2.getMetadata().getComments().get(0));
        Assertions.assertEquals("ATT_2",                           att2.getMetadata().getAttID());
        Assertions.assertEquals("ATT_1",                           att2.getMetadata().getAttPrevID());
        Assertions.assertEquals("SIMULATED",                       att2.getMetadata().getAttBasis());
        Assertions.assertEquals("rnd-25",                          att2.getMetadata().getAttBasisID());
        Assertions.assertEquals("EME2000",                         att2.getMetadata().getEndpoints().getFrameA().getName());
        Assertions.assertEquals(BaseEquipment.ACC,                 att2.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("0",                               att2.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(6,                                 att2.getMetadata().getNbStates());
        Assertions.assertEquals(AttitudeElementsType.EULER_ANGLES, att2.getMetadata().getAttitudeType());
        Assertions.assertEquals(RateElementsType.ANGVEL,           att2.getMetadata().getRateType());
        Assertions.assertEquals(2,                                 att2.getAttitudeStates().size());
        Assertions.assertEquals(0.50, att2.getAttitudeStates().get(0).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        Assertions.assertEquals(6,    att2.getAttitudeStates().get(0).getElements().length);
        Assertions.assertEquals( 1.0, FastMath.toDegrees(att2.getAttitudeStates().get(0).getElements()[0]), 1.0e-15);
        Assertions.assertEquals( 1.2, FastMath.toDegrees(att2.getAttitudeStates().get(0).getElements()[1]), 1.0e-15);
        Assertions.assertEquals( 1.3, FastMath.toDegrees(att2.getAttitudeStates().get(0).getElements()[2]), 1.0e-15);
        Assertions.assertEquals(-0.4, FastMath.toDegrees(att2.getAttitudeStates().get(0).getElements()[3]), 1.0e-15);
        Assertions.assertEquals(-0.5, FastMath.toDegrees(att2.getAttitudeStates().get(0).getElements()[4]), 1.0e-15);
        Assertions.assertEquals(-0.6, FastMath.toDegrees(att2.getAttitudeStates().get(0).getElements()[5]), 1.0e-15);
        Assertions.assertEquals(0.75, att2.getAttitudeStates().get(1).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        Assertions.assertEquals(6,    att2.getAttitudeStates().get(1).getElements().length);
        Assertions.assertEquals( 2.0, FastMath.toDegrees(att2.getAttitudeStates().get(1).getElements()[0]), 1.0e-15);
        Assertions.assertEquals( 2.2, FastMath.toDegrees(att2.getAttitudeStates().get(1).getElements()[1]), 1.0e-15);
        Assertions.assertEquals( 2.3, FastMath.toDegrees(att2.getAttitudeStates().get(1).getElements()[2]), 1.0e-15);
        Assertions.assertEquals(-1.4, FastMath.toDegrees(att2.getAttitudeStates().get(1).getElements()[3]), 1.0e-15);
        Assertions.assertEquals(-1.5, FastMath.toDegrees(att2.getAttitudeStates().get(1).getElements()[4]), 1.0e-15);
        Assertions.assertEquals(-1.6, FastMath.toDegrees(att2.getAttitudeStates().get(1).getElements()[5]), 1.0e-15);
        Assertions.assertEquals(0.0153152, att2.getAttitudeStates().get(0).toAngular(RotationOrder.XYZ).getRotationRate().getNorm(), 1.0e-7);

        // third attitude block
        AttitudeStateHistory att3 = acm.getData().getAttitudeBlocks().get(2);
        Assertions.assertEquals("third attitude block",           att3.getMetadata().getComments().get(0));
        Assertions.assertEquals("ATT_3",                           att3.getMetadata().getAttID());
        Assertions.assertEquals("ATT_2",                           att3.getMetadata().getAttPrevID());
        Assertions.assertEquals("SIMULATED",                       att3.getMetadata().getAttBasis());
        Assertions.assertEquals("rnd-25",                          att3.getMetadata().getAttBasisID());
        Assertions.assertEquals("EME2000",                         att3.getMetadata().getEndpoints().getFrameA().getName());
        Assertions.assertEquals(BaseEquipment.AST,                 att3.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1",                               att3.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(8,                                 att3.getMetadata().getNbStates());
        Assertions.assertEquals(AttitudeElementsType.QUATERNION,   att3.getMetadata().getAttitudeType());
        Assertions.assertEquals(RateElementsType.Q_DOT,            att3.getMetadata().getRateType());
        Assertions.assertEquals(2,                                 att3.getAttitudeStates().size());
        Assertions.assertEquals(1.0,         att3.getAttitudeStates().get(0).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        Assertions.assertEquals(8,           att3.getAttitudeStates().get(0).getElements().length);
        Assertions.assertEquals( 0.73566,    att3.getAttitudeStates().get(0).getElements()[0], 1.0e-15);
        Assertions.assertEquals(-0.50547,    att3.getAttitudeStates().get(0).getElements()[1], 1.0e-15);
        Assertions.assertEquals( 0.41309,    att3.getAttitudeStates().get(0).getElements()[2], 1.0e-15);
        Assertions.assertEquals( 0.180707,   att3.getAttitudeStates().get(0).getElements()[3], 1.0e-15);
        Assertions.assertEquals( 0.0073566,  att3.getAttitudeStates().get(0).getElements()[4], 1.0e-15);
        Assertions.assertEquals(-0.0050547,  att3.getAttitudeStates().get(0).getElements()[5], 1.0e-15);
        Assertions.assertEquals( 0.0041309,  att3.getAttitudeStates().get(0).getElements()[6], 1.0e-15);
        Assertions.assertEquals( 0.00180707, att3.getAttitudeStates().get(0).getElements()[7], 1.0e-15);
        Assertions.assertEquals(1.25,        att3.getAttitudeStates().get(1).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        Assertions.assertEquals(8,           att3.getAttitudeStates().get(1).getElements().length);
        Assertions.assertEquals( 0.73529,    att3.getAttitudeStates().get(1).getElements()[0], 1.0e-15);
        Assertions.assertEquals(-0.50531,    att3.getAttitudeStates().get(1).getElements()[1], 1.0e-15);
        Assertions.assertEquals( 0.41375,    att3.getAttitudeStates().get(1).getElements()[2], 1.0e-15);
        Assertions.assertEquals( 0.181158,   att3.getAttitudeStates().get(1).getElements()[3], 1.0e-15);
        Assertions.assertEquals( 0.0073529,  att3.getAttitudeStates().get(1).getElements()[4], 1.0e-15);
        Assertions.assertEquals(-0.0050531,  att3.getAttitudeStates().get(1).getElements()[5], 1.0e-15);
        Assertions.assertEquals( 0.0041375,  att3.getAttitudeStates().get(1).getElements()[6], 1.0e-15);
        Assertions.assertEquals( 0.00181158, att3.getAttitudeStates().get(1).getElements()[7], 1.0e-15);
        Assertions.assertEquals(0.0, att3.getAttitudeStates().get(0).toAngular(RotationOrder.XYZ).getRotationRate().getNorm(), 1.0e-10);

        // fourth attitude block
        AttitudeStateHistory att4 = acm.getData().getAttitudeBlocks().get(3);
        Assertions.assertEquals("fourth attitude block",           att4.getMetadata().getComments().get(0));
        Assertions.assertEquals("ATT_4",                           att4.getMetadata().getAttID());
        Assertions.assertEquals("ATT_3",                           att4.getMetadata().getAttPrevID());
        Assertions.assertEquals("SIMULATED",                       att4.getMetadata().getAttBasis());
        Assertions.assertEquals("rnd-25",                          att4.getMetadata().getAttBasisID());
        Assertions.assertEquals("EME2000",                         att4.getMetadata().getEndpoints().getFrameA().getName());
        Assertions.assertEquals(BaseEquipment.CSS,                 att4.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("7",                               att4.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(13,                                att4.getMetadata().getNbStates());
        Assertions.assertEquals(AttitudeElementsType.DCM,          att4.getMetadata().getAttitudeType());
        Assertions.assertEquals(RateElementsType.Q_DOT,            att4.getMetadata().getRateType());
        Assertions.assertEquals(2,                                 att4.getAttitudeStates().size());
        Assertions.assertEquals(1.50, att4.getAttitudeStates().get(0).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        Assertions.assertEquals(13,   att4.getAttitudeStates().get(0).getElements().length);
        Assertions.assertEquals(1.0,  att4.getAttitudeStates().get(0).getElements()[ 0], 1.0e-15);
        Assertions.assertEquals(0.0,  att4.getAttitudeStates().get(0).getElements()[ 1], 1.0e-15);
        Assertions.assertEquals(0.0,  att4.getAttitudeStates().get(0).getElements()[ 2], 1.0e-15);
        Assertions.assertEquals(0.0,  att4.getAttitudeStates().get(0).getElements()[ 3], 1.0e-15);
        Assertions.assertEquals(1.0,  att4.getAttitudeStates().get(0).getElements()[ 4], 1.0e-15);
        Assertions.assertEquals(0.0,  att4.getAttitudeStates().get(0).getElements()[ 5], 1.0e-15);
        Assertions.assertEquals(0.0,  att4.getAttitudeStates().get(0).getElements()[ 6], 1.0e-15);
        Assertions.assertEquals(0.0,  att4.getAttitudeStates().get(0).getElements()[ 7], 1.0e-15);
        Assertions.assertEquals(1.0,  att4.getAttitudeStates().get(0).getElements()[ 8], 1.0e-15);
        Assertions.assertEquals(0.01, att4.getAttitudeStates().get(0).getElements()[ 9], 1.0e-15);
        Assertions.assertEquals(0.02, att4.getAttitudeStates().get(0).getElements()[10], 1.0e-15);
        Assertions.assertEquals(0.03, att4.getAttitudeStates().get(0).getElements()[11], 1.0e-15);
        Assertions.assertEquals(0.04, att4.getAttitudeStates().get(0).getElements()[12], 1.0e-15);
        Assertions.assertEquals(1.75, att4.getAttitudeStates().get(1).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        Assertions.assertEquals(13,   att4.getAttitudeStates().get(1).getElements().length);
        Assertions.assertEquals(0.0,  att4.getAttitudeStates().get(1).getElements()[ 0], 1.0e-15);
        Assertions.assertEquals(1.0,  att4.getAttitudeStates().get(1).getElements()[ 1], 1.0e-15);
        Assertions.assertEquals(0.0,  att4.getAttitudeStates().get(1).getElements()[ 2], 1.0e-15);
        Assertions.assertEquals(0.0,  att4.getAttitudeStates().get(1).getElements()[ 3], 1.0e-15);
        Assertions.assertEquals(0.0,  att4.getAttitudeStates().get(1).getElements()[ 4], 1.0e-15);
        Assertions.assertEquals(1.0,  att4.getAttitudeStates().get(1).getElements()[ 5], 1.0e-15);
        Assertions.assertEquals(1.0,  att4.getAttitudeStates().get(1).getElements()[ 6], 1.0e-15);
        Assertions.assertEquals(0.0,  att4.getAttitudeStates().get(1).getElements()[ 7], 1.0e-15);
        Assertions.assertEquals(0.0,  att4.getAttitudeStates().get(1).getElements()[ 8], 1.0e-15);
        Assertions.assertEquals(0.05, att4.getAttitudeStates().get(1).getElements()[ 9], 1.0e-15);
        Assertions.assertEquals(0.06, att4.getAttitudeStates().get(1).getElements()[10], 1.0e-15);
        Assertions.assertEquals(0.07, att4.getAttitudeStates().get(1).getElements()[11], 1.0e-15);
        Assertions.assertEquals(0.08, att4.getAttitudeStates().get(1).getElements()[12], 1.0e-15);
        Assertions.assertEquals(0.0748331, att4.getAttitudeStates().get(0).toAngular(RotationOrder.XYZ).getRotationRate().getNorm(), 1.0e-7);

        // fifth attitude block
        AttitudeStateHistory att5 = acm.getData().getAttitudeBlocks().get(4);
        Assertions.assertEquals("fifth attitude block",           att5.getMetadata().getComments().get(0));
        Assertions.assertEquals("ATT_5",                           att5.getMetadata().getAttID());
        Assertions.assertEquals("ATT_4",                           att5.getMetadata().getAttPrevID());
        Assertions.assertEquals("SIMULATED",                       att5.getMetadata().getAttBasis());
        Assertions.assertEquals("rnd-25",                          att5.getMetadata().getAttBasisID());
        Assertions.assertEquals("EME2000",                         att5.getMetadata().getEndpoints().getFrameA().getName());
        Assertions.assertEquals(BaseEquipment.ESA,                 att5.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("9",                               att5.getMetadata().getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(6,                                 att5.getMetadata().getNbStates());
        Assertions.assertEquals(AttitudeElementsType.EULER_ANGLES, att5.getMetadata().getAttitudeType());
        Assertions.assertEquals(RateElementsType.GYRO_BIAS,        att5.getMetadata().getRateType());
        Assertions.assertEquals(2,                                 att5.getAttitudeStates().size());
        Assertions.assertEquals(2.00, att5.getAttitudeStates().get(0).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        Assertions.assertEquals(6,    att5.getAttitudeStates().get(0).getElements().length);
        Assertions.assertEquals( 1.0, FastMath.toDegrees(att5.getAttitudeStates().get(0).getElements()[0]), 1.0e-15);
        Assertions.assertEquals( 1.2, FastMath.toDegrees(att5.getAttitudeStates().get(0).getElements()[1]), 1.0e-15);
        Assertions.assertEquals( 1.3, FastMath.toDegrees(att5.getAttitudeStates().get(0).getElements()[2]), 1.0e-15);
        Assertions.assertEquals(-0.4, FastMath.toDegrees(att5.getAttitudeStates().get(0).getElements()[3]), 1.0e-15);
        Assertions.assertEquals(-0.5, FastMath.toDegrees(att5.getAttitudeStates().get(0).getElements()[4]), 1.0e-15);
        Assertions.assertEquals(-0.6, FastMath.toDegrees(att5.getAttitudeStates().get(0).getElements()[5]), 1.0e-15);
        Assertions.assertEquals(2.25, att5.getAttitudeStates().get(1).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        Assertions.assertEquals(6,    att5.getAttitudeStates().get(1).getElements().length);
        Assertions.assertEquals( 2.0, FastMath.toDegrees(att5.getAttitudeStates().get(1).getElements()[0]), 1.0e-15);
        Assertions.assertEquals( 2.2, FastMath.toDegrees(att5.getAttitudeStates().get(1).getElements()[1]), 1.0e-15);
        Assertions.assertEquals( 2.3, FastMath.toDegrees(att5.getAttitudeStates().get(1).getElements()[2]), 1.0e-15);
        Assertions.assertEquals(-1.4, FastMath.toDegrees(att5.getAttitudeStates().get(1).getElements()[3]), 1.0e-15);
        Assertions.assertEquals(-1.5, FastMath.toDegrees(att5.getAttitudeStates().get(1).getElements()[4]), 1.0e-15);
        Assertions.assertEquals(-1.6, FastMath.toDegrees(att5.getAttitudeStates().get(1).getElements()[5]), 1.0e-15);
        try {
            att5.getAttitudeStates().get(0).toAngular(RotationOrder.XYZ);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_UNSUPPORTED_ELEMENT_SET_TYPE, oe.getSpecifier());
        }

        // physical properties
        AttitudePhysicalProperties phys = acm.getData().getPhysicBlock();
        Assertions.assertEquals("Spacecraft Physical Parameters", phys.getComments().get(0));
        Assertions.assertEquals(1.8,    phys.getDragCoefficient(), 1.0e-15);
        Assertions.assertEquals(1916.0, phys.getWetMass(), 1.0e-15);
        Assertions.assertEquals(800.0,  phys.getDryMass(), 1.0e-15);
        Assertions.assertEquals(1916.0, phys.getWetMass(), 1.0e-15);
        Assertions.assertEquals(BaseEquipment.SC_BODY, phys.getCenterOfPressureReferenceFrame().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", phys.getCenterOfPressureReferenceFrame().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals( 0.04,  phys.getCenterOfPressure().getX(),      1.0e-15);
        Assertions.assertEquals(-0.78,  phys.getCenterOfPressure().getY(),      1.0e-15);
        Assertions.assertEquals(-0.023, phys.getCenterOfPressure().getZ(),      1.0e-15);
        Assertions.assertEquals(BaseEquipment.SC_BODY, phys.getInertiaReferenceFrame().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("2", phys.getInertiaReferenceFrame().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals( 752.0, phys.getInertiaMatrix().getEntry(0, 0), 1.0e-15);
        Assertions.assertEquals(1305.0, phys.getInertiaMatrix().getEntry(1, 1), 1.0e-15);
        Assertions.assertEquals(1490.0, phys.getInertiaMatrix().getEntry(2, 2), 1.0e-15);
        Assertions.assertEquals(  81.1, phys.getInertiaMatrix().getEntry(0, 1), 1.0e-15);
        Assertions.assertEquals(  81.1, phys.getInertiaMatrix().getEntry(1, 0), 1.0e-15);
        Assertions.assertEquals( -25.7, phys.getInertiaMatrix().getEntry(0, 2), 1.0e-15);
        Assertions.assertEquals( -25.7, phys.getInertiaMatrix().getEntry(2, 0), 1.0e-15);
        Assertions.assertEquals(  74.1, phys.getInertiaMatrix().getEntry(1, 2), 1.0e-15);
        Assertions.assertEquals(  74.1, phys.getInertiaMatrix().getEntry(2, 1), 1.0e-15);

        Assertions.assertEquals(6, acm.getData().getCovarianceBlocks().size());

        // first covariance block
        AttitudeCovarianceHistory cov1 = acm.getData().getCovarianceBlocks().get(0);
        Assertions.assertEquals("first covariance block", cov1.getMetadata().getComments().get(0));
        Assertions.assertEquals("COV_1", cov1.getMetadata().getCovID());
        Assertions.assertEquals("COV_0", cov1.getMetadata().getCovPrevID());
        Assertions.assertEquals("DETERMINED_OBC", cov1.getMetadata().getCovBasis());
        Assertions.assertEquals("blip-12", cov1.getMetadata().getCovBasisID());
        Assertions.assertEquals(BaseEquipment.SC_BODY, cov1.getMetadata().getCovReferenceFrame().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", cov1.getMetadata().getCovReferenceFrame().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(AttitudeCovarianceType.ANGLE_GYROBIAS, cov1.getMetadata().getCovType());
        Assertions.assertEquals(2, cov1.getCovariances().size());
        Assertions.assertEquals(0.0, cov1.getCovariances().get(0).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        DiagonalMatrix m1 = cov1.getCovariances().get(0).getMatrix();
        Assertions.assertEquals(6,   m1.getRowDimension());
        Assertions.assertEquals( 6.74E-11, FastMath.toDegrees(FastMath.toDegrees(m1.getEntry(0, 0))), 1.0e-22);
        Assertions.assertEquals( 8.10E-11, FastMath.toDegrees(FastMath.toDegrees(m1.getEntry(1, 1))), 1.0e-22);
        Assertions.assertEquals( 9.22E-11, FastMath.toDegrees(FastMath.toDegrees(m1.getEntry(2, 2))), 1.0e-22);
        Assertions.assertEquals( 1.11E-15, FastMath.toDegrees(FastMath.toDegrees(m1.getEntry(3, 3))), 1.0e-22);
        Assertions.assertEquals( 1.11E-15, FastMath.toDegrees(FastMath.toDegrees(m1.getEntry(4, 4))), 1.0e-22);
        Assertions.assertEquals( 1.12E-15, FastMath.toDegrees(FastMath.toDegrees(m1.getEntry(5, 5))), 1.0e-22);

        // second covariance block
        AttitudeCovarianceHistory cov2 = acm.getData().getCovarianceBlocks().get(1);
        Assertions.assertEquals("second covariance block", cov2.getMetadata().getComments().get(0));
        Assertions.assertEquals("COV_2", cov2.getMetadata().getCovID());
        Assertions.assertEquals("COV_1", cov2.getMetadata().getCovPrevID());
        Assertions.assertEquals("DETERMINED_OBC", cov2.getMetadata().getCovBasis());
        Assertions.assertEquals("blip-12", cov2.getMetadata().getCovBasisID());
        Assertions.assertEquals(BaseEquipment.SC_BODY, cov2.getMetadata().getCovReferenceFrame().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", cov2.getMetadata().getCovReferenceFrame().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(AttitudeCovarianceType.ANGLE, cov2.getMetadata().getCovType());
        Assertions.assertEquals(2, cov2.getCovariances().size());
        Assertions.assertEquals(0.0, cov2.getCovariances().get(0).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        DiagonalMatrix m2 = cov2.getCovariances().get(0).getMatrix();
        Assertions.assertEquals(3,   m2.getRowDimension());
        Assertions.assertEquals( 6.74E-11, FastMath.toDegrees(FastMath.toDegrees(m2.getEntry(0, 0))), 1.0e-22);
        Assertions.assertEquals( 8.10E-11, FastMath.toDegrees(FastMath.toDegrees(m2.getEntry(1, 1))), 1.0e-22);
        Assertions.assertEquals( 9.22E-11, FastMath.toDegrees(FastMath.toDegrees(m2.getEntry(2, 2))), 1.0e-22);

        // third covariance block
        AttitudeCovarianceHistory cov3 = acm.getData().getCovarianceBlocks().get(2);
        Assertions.assertEquals("third covariance block", cov3.getMetadata().getComments().get(0));
        Assertions.assertEquals("COV_3", cov3.getMetadata().getCovID());
        Assertions.assertEquals("COV_2", cov3.getMetadata().getCovPrevID());
        Assertions.assertEquals("DETERMINED_OBC", cov3.getMetadata().getCovBasis());
        Assertions.assertEquals("blip-12", cov3.getMetadata().getCovBasisID());
        Assertions.assertEquals(BaseEquipment.SC_BODY, cov3.getMetadata().getCovReferenceFrame().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", cov3.getMetadata().getCovReferenceFrame().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(AttitudeCovarianceType.ANGLE_ANGVEL, cov3.getMetadata().getCovType());
        Assertions.assertEquals(2, cov3.getCovariances().size());
        Assertions.assertEquals(0.0, cov3.getCovariances().get(0).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        DiagonalMatrix m3 = cov3.getCovariances().get(0).getMatrix();
        Assertions.assertEquals(6,   m3.getRowDimension());
        Assertions.assertEquals( 6.74E-11, FastMath.toDegrees(FastMath.toDegrees(m3.getEntry(0, 0))), 1.0e-22);
        Assertions.assertEquals( 8.10E-11, FastMath.toDegrees(FastMath.toDegrees(m3.getEntry(1, 1))), 1.0e-22);
        Assertions.assertEquals( 9.22E-11, FastMath.toDegrees(FastMath.toDegrees(m3.getEntry(2, 2))), 1.0e-22);
        Assertions.assertEquals( 1.11E-15, FastMath.toDegrees(FastMath.toDegrees(m3.getEntry(3, 3))), 1.0e-22);
        Assertions.assertEquals( 1.11E-15, FastMath.toDegrees(FastMath.toDegrees(m3.getEntry(4, 4))), 1.0e-22);
        Assertions.assertEquals( 1.12E-15, FastMath.toDegrees(FastMath.toDegrees(m3.getEntry(5, 5))), 1.0e-22);

        // fourth covariance block
        AttitudeCovarianceHistory cov4 = acm.getData().getCovarianceBlocks().get(3);
        Assertions.assertEquals("fourth covariance block", cov4.getMetadata().getComments().get(0));
        Assertions.assertEquals("COV_4", cov4.getMetadata().getCovID());
        Assertions.assertEquals("COV_3", cov4.getMetadata().getCovPrevID());
        Assertions.assertEquals("DETERMINED_OBC", cov4.getMetadata().getCovBasis());
        Assertions.assertEquals("blip-12", cov4.getMetadata().getCovBasisID());
        Assertions.assertEquals(BaseEquipment.SC_BODY, cov4.getMetadata().getCovReferenceFrame().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", cov4.getMetadata().getCovReferenceFrame().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(AttitudeCovarianceType.QUATERNION, cov4.getMetadata().getCovType());
        Assertions.assertEquals(2, cov4.getCovariances().size());
        Assertions.assertEquals(0.0, cov4.getCovariances().get(0).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        DiagonalMatrix m4 = cov4.getCovariances().get(0).getMatrix();
        Assertions.assertEquals(4,   m4.getRowDimension());
        Assertions.assertEquals( 6.74E-11, m4.getEntry(0, 0), 1.0e-22);
        Assertions.assertEquals( 8.10E-11, m4.getEntry(1, 1), 1.0e-22);
        Assertions.assertEquals( 9.22E-11, m4.getEntry(2, 2), 1.0e-22);
        Assertions.assertEquals( 2.17E-11, m4.getEntry(3, 3), 1.0e-22);

        // fifth covariance block
        AttitudeCovarianceHistory cov5 = acm.getData().getCovarianceBlocks().get(4);
        Assertions.assertEquals("fifth covariance block", cov5.getMetadata().getComments().get(0));
        Assertions.assertEquals("COV_5", cov5.getMetadata().getCovID());
        Assertions.assertEquals("COV_4", cov5.getMetadata().getCovPrevID());
        Assertions.assertEquals("DETERMINED_OBC", cov5.getMetadata().getCovBasis());
        Assertions.assertEquals("blip-12", cov5.getMetadata().getCovBasisID());
        Assertions.assertEquals(BaseEquipment.SC_BODY, cov5.getMetadata().getCovReferenceFrame().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", cov5.getMetadata().getCovReferenceFrame().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(AttitudeCovarianceType.QUATERNION_GYROBIAS, cov5.getMetadata().getCovType());
        Assertions.assertEquals(2, cov5.getCovariances().size());
        Assertions.assertEquals(0.0, cov5.getCovariances().get(0).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        DiagonalMatrix m5 = cov5.getCovariances().get(0).getMatrix();
        Assertions.assertEquals(7,   m5.getRowDimension());
        Assertions.assertEquals( 6.74E-11, m5.getEntry(0, 0), 1.0e-22);
        Assertions.assertEquals( 8.10E-11, m5.getEntry(1, 1), 1.0e-22);
        Assertions.assertEquals( 9.22E-11, m5.getEntry(2, 2), 1.0e-22);
        Assertions.assertEquals( 2.17E-11, m5.getEntry(3, 3), 1.0e-22);
        Assertions.assertEquals( 1.11E-15, FastMath.toDegrees(FastMath.toDegrees(m5.getEntry(4, 4))), 1.0e-22);
        Assertions.assertEquals( 1.11E-15, FastMath.toDegrees(FastMath.toDegrees(m5.getEntry(5, 5))), 1.0e-22);
        Assertions.assertEquals( 1.12E-15, FastMath.toDegrees(FastMath.toDegrees(m5.getEntry(6, 6))), 1.0e-22);

        // sixth covariance block
        AttitudeCovarianceHistory cov6 = acm.getData().getCovarianceBlocks().get(5);
        Assertions.assertEquals("sixth covariance block", cov6.getMetadata().getComments().get(0));
        Assertions.assertEquals("COV_6", cov6.getMetadata().getCovID());
        Assertions.assertEquals("COV_5", cov6.getMetadata().getCovPrevID());
        Assertions.assertEquals("DETERMINED_OBC", cov6.getMetadata().getCovBasis());
        Assertions.assertEquals("blip-12", cov6.getMetadata().getCovBasisID());
        Assertions.assertEquals(BaseEquipment.SC_BODY, cov6.getMetadata().getCovReferenceFrame().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1", cov6.getMetadata().getCovReferenceFrame().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(AttitudeCovarianceType.QUATERNION_ANGVEL, cov6.getMetadata().getCovType());
        Assertions.assertEquals(2, cov6.getCovariances().size());
        Assertions.assertEquals(0.0, cov6.getCovariances().get(0).getDate().durationFrom(acm.getMetadata().getEpochT0()), 1.0e-15);
        DiagonalMatrix m6 = cov6.getCovariances().get(0).getMatrix();
        Assertions.assertEquals(7,   m6.getRowDimension());
        Assertions.assertEquals( 6.74E-11, m6.getEntry(0, 0), 1.0e-22);
        Assertions.assertEquals( 8.10E-11, m6.getEntry(1, 1), 1.0e-22);
        Assertions.assertEquals( 9.22E-11, m6.getEntry(2, 2), 1.0e-22);
        Assertions.assertEquals( 2.17E-11, m6.getEntry(3, 3), 1.0e-22);
        Assertions.assertEquals( 1.11E-15, FastMath.toDegrees(FastMath.toDegrees(m6.getEntry(4, 4))), 1.0e-22);
        Assertions.assertEquals( 1.11E-15, FastMath.toDegrees(FastMath.toDegrees(m6.getEntry(5, 5))), 1.0e-22);
        Assertions.assertEquals( 1.12E-15, FastMath.toDegrees(FastMath.toDegrees(m6.getEntry(6, 6))), 1.0e-22);

        Assertions.assertEquals(3, acm.getData().getManeuverBlocks().size());

        // first maneuver
        AttitudeManeuver man1 = acm.getData().getManeuverBlocks().get(0);
        Assertions.assertEquals("first maneuver",               man1.getComments().get(0));
        Assertions.assertEquals("MAN_1",                        man1.getID());
        Assertions.assertEquals("MAN_0",                        man1.getPrevID());
        Assertions.assertEquals("MOM_DESAT",                    man1.getManPurpose());
        Assertions.assertEquals(100.0,                          man1.getBeginTime());
        Assertions.assertTrue(Double.isNaN(man1.getEndTime()));
        Assertions.assertEquals(450.0,                          man1.getDuration());
        Assertions.assertEquals("ATT-THRUSTER",                 man1.getActuatorUsed());
        Assertions.assertEquals(  1.3,                          man1.getTargetMomentum().getX(), 1.0e-10);
        Assertions.assertEquals(-16.4,                          man1.getTargetMomentum().getY(), 1.0e-10);
        Assertions.assertEquals(-11.35,                         man1.getTargetMomentum().getZ(), 1.0e-10);
        Assertions.assertNull(man1.getTargetAttitude());
        Assertions.assertTrue(Double.isNaN(man1.getTargetSpinRate()));

        // second maneuver
        AttitudeManeuver man2 = acm.getData().getManeuverBlocks().get(1);
        Assertions.assertEquals("second maneuver",              man2.getComments().get(0));
        Assertions.assertEquals("MAN_2",                        man2.getID());
        Assertions.assertEquals("MAN_1",                        man2.getPrevID());
        Assertions.assertEquals("ATT_ADJUST",                   man2.getManPurpose());
        Assertions.assertEquals(500.0,                          man2.getBeginTime());
        Assertions.assertEquals(600.0,                          man2.getEndTime());
        Assertions.assertTrue(Double.isNaN(man2.getDuration()));
        Assertions.assertEquals("MAGNETOTORQUER",               man2.getActuatorUsed());
        Assertions.assertNull(man2.getTargetMomentum());
        Assertions.assertEquals(  0.0,                          man2.getTargetAttitude().getQ1(), 1.0e-10);
        Assertions.assertEquals(  0.0,                          man2.getTargetAttitude().getQ2(), 1.0e-10);
        Assertions.assertEquals(-0.707106781187,                man2.getTargetAttitude().getQ3(), 1.0e-10);
        Assertions.assertEquals( 0.707106781187,                man2.getTargetAttitude().getQ0(), 1.0e-10);
        Assertions.assertTrue(Double.isNaN(man2.getTargetSpinRate()));

        // third maneuver
        AttitudeManeuver man3 = acm.getData().getManeuverBlocks().get(2);
        Assertions.assertEquals("third maneuver",               man3.getComments().get(0));
        Assertions.assertEquals("MAN_3",                        man3.getID());
        Assertions.assertEquals("MAN_2",                        man3.getPrevID());
        Assertions.assertEquals("SPIN_RATE_ADJUST",             man3.getManPurpose());
        Assertions.assertEquals(700.0,                          man3.getBeginTime());
        Assertions.assertTrue(Double.isNaN(man3.getEndTime()));
        Assertions.assertEquals(200.0,                          man3.getDuration());
        Assertions.assertEquals("REACTION-WHEEL",               man3.getActuatorUsed());
        Assertions.assertNull(man3.getTargetMomentum());
        Assertions.assertNull(man3.getTargetAttitude());
        Assertions.assertEquals( 0.12,                          FastMath.toDegrees(man3.getTargetSpinRate()), 1.0e-10);

        // attitude determi nation
        AttitudeDetermination ad = acm.getData().getAttitudeDeterminationBlock();
        Assertions.assertEquals("attitude determination block",        ad.getComments().get(0));
        Assertions.assertEquals("AD_1",                                ad.getId());
        Assertions.assertEquals("AD_0",                                ad.getPrevId());
        Assertions.assertEquals(AdMethodType.Q_METHOD,                 ad.getMethod());
        Assertions.assertEquals("OBC",                                 ad.getSource());
        Assertions.assertEquals(RotationOrder.XYZ,                     ad.getEulerRotSeq());
        Assertions.assertEquals(7,                                     ad.getNbStates());
        Assertions.assertEquals(AttitudeElementsType.QUATERNION,       ad.getAttitudeStates());
        Assertions.assertEquals(AttitudeCovarianceType.ANGLE_GYROBIAS, ad.getCovarianceType());
        Assertions.assertEquals("EME2000",                             ad.getEndpoints().getFrameA().getName());
        Assertions.assertEquals(BaseEquipment.SC_BODY,                 ad.getEndpoints().getFrameB().asSpacecraftBodyFrame().getBaseEquipment());
        Assertions.assertEquals("1",                                   ad.getEndpoints().getFrameB().asSpacecraftBodyFrame().getLabel());
        Assertions.assertEquals(RateElementsType.ANGVEL,               ad.getRateStates());
        Assertions.assertEquals(3,                                     ad.getSensorsUsed().size());
        Assertions.assertEquals(1,                                     ad.getSensorsUsed().get(0).getSensorNumber());
        Assertions.assertEquals("AST1",                                ad.getSensorsUsed().get(0).getSensorUsed());
        Assertions.assertEquals( 2,                                    ad.getSensorsUsed().get(0).getNbSensorNoiseCovariance());
        Assertions.assertEquals(0.0097, FastMath.toDegrees(ad.getSensorsUsed().get(0).getSensorNoiseCovariance()[0]), 1.0e-10);
        Assertions.assertEquals(0.0098, FastMath.toDegrees(ad.getSensorsUsed().get(0).getSensorNoiseCovariance()[1]), 1.0e-10);
        Assertions.assertEquals( 5.0, ad.getSensorsUsed().get(0).getSensorFrequency(), 1.0e-10);
        Assertions.assertEquals(2,                                     ad.getSensorsUsed().get(1).getSensorNumber());
        Assertions.assertEquals("AST2",                                ad.getSensorsUsed().get(1).getSensorUsed());
        Assertions.assertEquals( 2,                                    ad.getSensorsUsed().get(1).getNbSensorNoiseCovariance());
        Assertions.assertEquals(0.0079, FastMath.toDegrees(ad.getSensorsUsed().get(1).getSensorNoiseCovariance()[0]), 1.0e-10);
        Assertions.assertEquals(0.0089, FastMath.toDegrees(ad.getSensorsUsed().get(1).getSensorNoiseCovariance()[1]), 1.0e-10);
        Assertions.assertEquals(10.0, ad.getSensorsUsed().get(1).getSensorFrequency(), 1.0e-10);
        Assertions.assertEquals(3,                                     ad.getSensorsUsed().get(2).getSensorNumber());
        Assertions.assertEquals("IMU",                                 ad.getSensorsUsed().get(2).getSensorUsed());
        Assertions.assertEquals(-1,                                    ad.getSensorsUsed().get(2).getNbSensorNoiseCovariance());
        Assertions.assertNull(ad.getSensorsUsed().get(2).getSensorNoiseCovariance());
        Assertions.assertTrue(Double.isNaN(ad.getSensorsUsed().get(2).getSensorFrequency()));

        Assertions.assertEquals(1, acm.getData().getUserDefinedBlock().getParameters().size());
        Assertions.assertEquals("viscum-album", acm.getData().getUserDefinedBlock().getParameters().get("OXIDIZER"));

    }

    @Test
    public void testWriteACM05() throws URISyntaxException, IOException {
        final String name = "/ccsds/adm/acm/ACMExample05.txt";
        final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
        AcmParser parser = new ParserBuilder().buildAcmParser();
        final Acm original = parser.parseMessage(source);

        // write the parsed file back to a characters array
        final CharArrayWriter caw = new CharArrayWriter();
        final Generator generator = new XmlGenerator(caw, 2, "dummy", Constants.JULIAN_DAY, true, null);
        new WriterBuilder().buildAcmWriter().writeMessage(generator, original);

        // reparse the written file
        final byte[]     bytes   = caw.toString().getBytes(StandardCharsets.UTF_8);
        final DataSource source2 = new DataSource(name, () -> new ByteArrayInputStream(bytes));
        final Acm    rebuilt = new ParserBuilder().buildAcmParser().parseMessage(source2);
        validateAcm05(rebuilt);

    }

}
