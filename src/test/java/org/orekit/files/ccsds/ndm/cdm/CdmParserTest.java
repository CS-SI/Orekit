/* Copyright 2002-2026 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.files.ccsds.ndm.cdm;

import java.net.URISyntaxException;
import java.util.ArrayList;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.CcsdsFrameMapper;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.PocMethodType;
import org.orekit.files.ccsds.definitions.YesNoUnknown;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.odm.ocm.ObjectType;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeOffset;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

public class CdmParserTest {

    private static final double DISTANCE_PRECISION = 1e-8;
    private static final double DOUBLE_PRECISION = 1e-8;
    private static final double DERIVATION_PRECISION = 1e-12;
    private static final double COVARIANCE_DIAG_PRECISION = 1e-10;
    private static final double COVARIANCE_PRECISION = 1e-8;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testParseCDM1() {
        /* Test for CdmExample1.txt, with only required data. */
        // File
        final String ex = "/ccsds/cdm/CDMExample1.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(), file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12,
                                                 TimeScalesFactory.getUTC()), file.getHeader().getCreationDate());
        Assertions.assertEquals("JSPOC", file.getHeader().getOriginator());
        Assertions.assertEquals("201113719185", file.getHeader().getMessageId().orElseThrow());

        // OBJECT1
        // Check Relative Metadata Block
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, new TimeOffset(52, TimeOffset.SECOND, 618, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getTca());
        Assertions.assertEquals(715.0, file.getRelativeMetadata().getMissDistance(), DISTANCE_PRECISION);

        // Check Metadata Block
        Assertions.assertEquals("OBJECT1", file.getMetadataObject1().getObject());
        Assertions.assertEquals("12345", file.getMetadataObject1().getObjectDesignator());
        Assertions.assertEquals("SATCAT", file.getMetadataObject1().getCatalogName());
        Assertions.assertEquals("SATELLITE A", file.getMetadataObject1().getObjectName());
        Assertions.assertEquals("1997−030E", file.getMetadataObject1().getInternationalDes());
        Assertions.assertEquals("EPHEMERIS SATELLITE A", file.getMetadataObject1().getEphemName());
        Assertions.assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject1().getCovarianceMethod());
        Assertions.assertEquals(Maneuvrable.YES, file.getMetadataObject1().getManeuverable());
        Assertions.assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject1().getRefFrame().asCelestialBodyFrame().orElseThrow());
        Assertions.assertEquals("UTC", file.getMetadataObject1().getTimeSystem().name());

        // Check data block
        // State vector block
        Assertions.assertEquals(2570.097065e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getX(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(2244.654904e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getY(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(6281.497978e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getZ(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(4.418769571e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getX(),
                                DERIVATION_PRECISION);
        Assertions.assertEquals(4.833547743e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getY(),
                                DERIVATION_PRECISION);
        Assertions.assertEquals(-3.526774282e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getZ(),
                                DERIVATION_PRECISION);
        // Covariance Matrix block
        Assertions.assertEquals(4.142e1, file.getDataObject1().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-8.579, file.getDataObject1().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.533e3, file.getDataObject1().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(1.336e1, file.getDataObject1().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(7.098e1, file.getDataObject1().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(2.520e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-5.476, file.getDataObject1().getRTNCovarianceBlock().getCrdott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(8.626e-4, file.getDataObject1().getRTNCovarianceBlock().getCrdotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(5.744e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(-1.006e-2, file.getDataObject1().getRTNCovarianceBlock().getCtdotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(4.041e-3, file.getDataObject1().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.359e-3, file.getDataObject1().getRTNCovarianceBlock().getCtdotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.502e-5, file.getDataObject1().getRTNCovarianceBlock().getCtdotrdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.049e-5, file.getDataObject1().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(1.053e-3, file.getDataObject1().getRTNCovarianceBlock().getCndotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-3.412e-3, file.getDataObject1().getRTNCovarianceBlock().getCndott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.213e-2, file.getDataObject1().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-3.004e-6, file.getDataObject1().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.091e-6, file.getDataObject1().getRTNCovarianceBlock().getCndottdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(5.529e-5, file.getDataObject1().getRTNCovarianceBlock().getCndotndot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(2.520e-3, file.getDataObject1().getRTNCovarianceBlock().
                                getRTNCovarianceMatrix().getEntry(3, 0), COVARIANCE_PRECISION);
        Assertions.assertEquals(Double.NaN, file.getDataObject1().getRTNCovarianceBlock().
                                getRTNCovarianceMatrix().getEntry(7, 6), COVARIANCE_PRECISION);



        // OBJECT2
        // Check Relative Metadata Block
        Assertions.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getTca(),
                                file.getRelativeMetadata().getTca());
        Assertions.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getMissDistance(),
                                file.getRelativeMetadata().getMissDistance(), DISTANCE_PRECISION);

        // Check Metadata Block
        Assertions.assertEquals("OBJECT2", file.getMetadataObject2().getObject());
        Assertions.assertEquals("30337", file.getMetadataObject2().getObjectDesignator());
        Assertions.assertEquals("SATCAT", file.getMetadataObject2().getCatalogName());
        Assertions.assertEquals("FENGYUN 1C DEB", file.getMetadataObject2().getObjectName());
        Assertions.assertEquals("1999-025AA", file.getMetadataObject2().getInternationalDes());
        Assertions.assertEquals("NONE", file.getMetadataObject2().getEphemName());
        Assertions.assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject2().getCovarianceMethod());
        Assertions.assertEquals(Maneuvrable.NO, file.getMetadataObject2().getManeuverable());
        Assertions.assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject2().getRefFrame().asCelestialBodyFrame().orElseThrow());
        Assertions.assertEquals("UTC", file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        Assertions.assertEquals(2569.540800e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getX(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(2245.093614e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getY(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(6281.599946e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getZ(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(-2.888612500e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(),
                                DERIVATION_PRECISION);
        Assertions.assertEquals(-6.007247516e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getY(),
                                DERIVATION_PRECISION);
        Assertions.assertEquals(3.328770172e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getZ(),
                                DERIVATION_PRECISION);
        // Covariance Matrix block
        Assertions.assertEquals(1.337e3, file.getDataObject2().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-4.806e4, file.getDataObject2().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.492e6, file.getDataObject2().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-3.298e1, file.getDataObject2().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(7.105e1, file.getDataObject2().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(2.591e-3, file.getDataObject2().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-4.152e-2, file.getDataObject2().getRTNCovarianceBlock().getCrdott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.784e-6, file.getDataObject2().getRTNCovarianceBlock().getCrdotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(6.886e-5, file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(-1.016e-2, file.getDataObject2().getRTNCovarianceBlock().getCtdotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.637e-3, file.getDataObject2().getRTNCovarianceBlock().getCtdotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-2.987e-6, file.getDataObject2().getRTNCovarianceBlock().getCtdotrdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.059e-5, file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(4.400e-3, file.getDataObject2().getRTNCovarianceBlock().getCndotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(8.482e-3, file.getDataObject2().getRTNCovarianceBlock().getCndott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(8.633e-5, file.getDataObject2().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-4.594e-6, file.getDataObject2().getRTNCovarianceBlock().getCndottdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(5.178e-5, file.getDataObject2().getRTNCovarianceBlock().getCndotndot(),
                                COVARIANCE_PRECISION);
        // Test in the matrix
        Assertions.assertEquals(1.337e3, file.getDataObject2().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-4.806e4, file.getDataObject2().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.492e6, file.getDataObject2().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-3.298e1, file.getDataObject2().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(7.105e1, file.getDataObject2().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(2.591e-3,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 0),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-4.152e-2,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 1),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.784e-6,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 2),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(6.886e-5,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 3),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(-1.016e-2,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 0),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.506e-4,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 1),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.637e-3,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 2),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-2.987e-6,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 3),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.059e-5,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 4),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(4.400e-3,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 0),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(8.482e-3,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 1),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(8.633e-5,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 2),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.903e-6,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 3),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-4.594e-6,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 4),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(5.178e-5,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 5),
                                COVARIANCE_PRECISION);

    }

    @Test
    public void testParseCDM2() {
        /* Test for CdmExample2.txt, with only required data. */
        // File
        final String ex = "/ccsds/cdm/CDMExample2.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(), file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12,
                                                 TimeScalesFactory.getUTC()), file.getHeader().getCreationDate());
        Assertions.assertEquals("JSPOC", file.getHeader().getOriginator());
        Assertions.assertEquals("201113719185", file.getHeader().getMessageId().orElseThrow());

        // OBJECT1
        // Check Relative Metadata Block
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, new TimeOffset(52, TimeOffset.SECOND, 618, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getTca());
        Assertions.assertEquals(715.0, file.getRelativeMetadata().getMissDistance(), DISTANCE_PRECISION);
        Assertions.assertEquals(14762.0, file.getRelativeMetadata().getRelativeSpeed().orElseThrow(), DERIVATION_PRECISION);
        Assertions.assertEquals(27.4, file.getRelativeMetadata().getRelativePosition().orElseThrow().getX(), DISTANCE_PRECISION);
        Assertions.assertEquals(-70.2, file.getRelativeMetadata().getRelativePosition().orElseThrow().getY(), DISTANCE_PRECISION);
        Assertions.assertEquals(711.8, file.getRelativeMetadata().getRelativePosition().orElseThrow().getZ(), DISTANCE_PRECISION);
        Assertions.assertEquals(-7.2, file.getRelativeMetadata().getRelativeVelocity().orElseThrow().getX(), DERIVATION_PRECISION);
        Assertions.assertEquals(-14692.0, file.getRelativeMetadata().getRelativeVelocity().orElseThrow().getY(),
                                DERIVATION_PRECISION);
        Assertions.assertEquals(-1437.2, file.getRelativeMetadata().getRelativeVelocity().orElseThrow().getZ(), DERIVATION_PRECISION);
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 12, 18, 29, new TimeOffset(32, TimeOffset.SECOND, 212, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStartScreenPeriod().orElseThrow());
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 15, 18, 29, new TimeOffset(32, TimeOffset.SECOND, 212, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStopScreenPeriod().orElseThrow());
        Assertions.assertEquals(ScreenVolumeFrame.RTN, file.getRelativeMetadata().getScreenVolumeFrame().orElseThrow());
        Assertions.assertEquals(ScreenVolumeShape.ELLIPSOID, file.getRelativeMetadata().getScreenVolumeShape().orElseThrow());
        Assertions.assertEquals(200, file.getRelativeMetadata().getScreenVolumeX().orElseThrow(), 0);
        Assertions.assertEquals(1000, file.getRelativeMetadata().getScreenVolumeY().orElseThrow(), 0);
        Assertions.assertEquals(1000, file.getRelativeMetadata().getScreenVolumeZ().orElseThrow(), 0);
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, new TimeOffset(52, TimeOffset.SECOND, 222, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenEntryTime().orElseThrow());
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, new TimeOffset(52, TimeOffset.SECOND, 824, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenExitTime().orElseThrow());
        Assertions.assertEquals(4.835E-05, file.getRelativeMetadata().getCollisionProbability().orElseThrow(), 1e-30);
        Assertions.assertEquals("FOSTER-1992", file.getRelativeMetadata().getCollisionProbaMethod().orElseThrow().getName());
        Assertions.assertEquals(PocMethodType.FOSTER_1992,
                                file.getRelativeMetadata().getCollisionProbaMethod().orElseThrow().getType());

        // Check Metadata Block
        Assertions.assertEquals("OBJECT1", file.getMetadataObject1().getObject());
        Assertions.assertEquals(ObjectType.PAYLOAD, file.getMetadataObject1().getObjectType().orElseThrow());
        Assertions.assertEquals("OSA", file.getMetadataObject1().getOperatorContactPosition().orElseThrow());
        Assertions.assertEquals("EUMETSAT", file.getMetadataObject1().getOperatorOrganization().orElseThrow());
        Assertions.assertEquals("+49615130312", file.getMetadataObject1().getOperatorPhone().orElseThrow());
        Assertions.assertEquals("JOHN.DOE@SOMEWHERE.NET", file.getMetadataObject1().getOperatorEmail().orElseThrow());
        Assertions.assertEquals("EPHEMERIS SATELLITE A", file.getMetadataObject1().getEphemName());
        Assertions.assertEquals(FramesFactory.getEME2000(), file.getMetadataObject1().getFrame());
        Assertions.assertEquals("JACCHIA 70 DCA", file.getMetadataObject1().getAtmosphericModel().orElseThrow());
        Assertions.assertEquals("EGM-96", file.getMetadataObject1().getGravityModel().orElseThrow());
        Assertions.assertEquals(36, file.getMetadataObject1().getGravityDegree().orElseThrow(), 0);
        Assertions.assertEquals(36, file.getMetadataObject1().getGravityOrder().orElseThrow(), 0);
        Assertions.assertEquals("MOON", file.getMetadataObject1().getNBodyPerturbations().getFirst().getName());
        Assertions.assertEquals("SUN", file.getMetadataObject1().getNBodyPerturbations().get(1).getName());
        Assertions.assertEquals("NO", file.getMetadataObject1().getSolarRadiationPressure().orElseThrow().name());
        Assertions.assertEquals("NO", file.getMetadataObject1().getEarthTides().orElseThrow().name());
        Assertions.assertEquals("NO", file.getMetadataObject1().getIntrackThrust().orElseThrow().name());
        Assertions.assertEquals("UTC", file.getMetadataObject1().getTimeSystem().name());

        // Check data block
        // OD parameters block
        ODParameters odBlock1 = file.getDataObject1().getODParametersBlock().orElseThrow();
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 12, 2, 14,
                                                 new TimeOffset(12, TimeOffset.SECOND, 746, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()),
                                odBlock1.getTimeLastObsStart().orElseThrow());
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 12, 2, 14,
                                                 new TimeOffset(12, TimeOffset.SECOND, 746, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()),
                                odBlock1.getTimeLastObsEnd().orElseThrow());
        Assertions.assertEquals(7.88*3600.0*24.0, odBlock1.getRecommendedOdSpan().orElseThrow(), DOUBLE_PRECISION);
        Assertions.assertEquals(5.50*3600.0*24.0, odBlock1.getActualOdSpan().orElseThrow(), DOUBLE_PRECISION);
        Assertions.assertEquals(592, odBlock1.getObsAvailable().orElseThrow(), DOUBLE_PRECISION);
        Assertions.assertEquals(579, odBlock1.getObsUsed().orElseThrow(), DOUBLE_PRECISION);
        Assertions.assertEquals(123, odBlock1.getTracksAvailable().orElseThrow(), DOUBLE_PRECISION);
        Assertions.assertEquals(119, odBlock1.getTracksUsed().orElseThrow(), DOUBLE_PRECISION);
        Assertions.assertEquals(97.8/100.0, odBlock1.getResidualsAccepted().orElseThrow(), DOUBLE_PRECISION);
        Assertions.assertEquals(0.864, odBlock1.getWeightedRMS().orElseThrow(), DOUBLE_PRECISION);
        // Additional parameters block
        AdditionalParameters add1 = file.getDataObject1().getAdditionalParametersBlock().orElseThrow();
        Assertions.assertEquals(5.2, add1.getAreaPC().orElseThrow(), DOUBLE_PRECISION);
        Assertions.assertEquals(251.6, add1.getMass().orElseThrow(), DOUBLE_PRECISION);
        Assertions.assertEquals(0.045663, add1.getCDAreaOverMass().orElseThrow(), DOUBLE_PRECISION);
        Assertions.assertEquals(0.000000, add1.getCRAreaOverMass().orElseThrow(), DOUBLE_PRECISION);
        Assertions.assertEquals(0.0, add1.getThrustAcceleration().orElseThrow(), DOUBLE_PRECISION);
        Assertions.assertEquals(4.54570E-05, add1.getSedr().orElseThrow(), DOUBLE_PRECISION);
        // State vector block
        Assertions.assertEquals(2570.097065e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getX(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(4.833547743e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getY(),
                                DERIVATION_PRECISION);
        // Covariance Matrix block
        Assertions.assertEquals(4.142e1, file.getDataObject1().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-8.579, file.getDataObject1().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.533e3, file.getDataObject1().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(1.336e1, file.getDataObject1().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(7.098e1, file.getDataObject1().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(-1.862E+00, file.getDataObject1().getRTNCovarianceBlock().getCdrgr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(3.530E+00, file.getDataObject1().getRTNCovarianceBlock().getCdrgt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-3.100E-01, file.getDataObject1().getRTNCovarianceBlock().getCdrgn(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-1.214E-04, file.getDataObject1().getRTNCovarianceBlock().getCdrgrdot(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.580E-04, file.getDataObject1().getRTNCovarianceBlock().getCdrgtdot(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-6.467E-05, file.getDataObject1().getRTNCovarianceBlock().getCdrgndot(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(3.483E-06, file.getDataObject1().getRTNCovarianceBlock().getCdrgdrg(),
                                COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(-1.492E+02, file.getDataObject1().getRTNCovarianceBlock().getCsrpr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.044E+02, file.getDataObject1().getRTNCovarianceBlock().getCsrpt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-2.331E+01, file.getDataObject1().getRTNCovarianceBlock().getCsrpn(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-1.254E-03, file.getDataObject1().getRTNCovarianceBlock().getCsrprdot(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.013E-02, file.getDataObject1().getRTNCovarianceBlock().getCsrptdot(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-4.700E-03, file.getDataObject1().getRTNCovarianceBlock().getCsrpndot(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.210E-04, file.getDataObject1().getRTNCovarianceBlock().getCsrpdrg(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(1.593E-02, file.getDataObject1().getRTNCovarianceBlock().getCsrpsrp(),
                                COVARIANCE_DIAG_PRECISION);


        // OBJECT2
        // Check Relative Metadata Block
        Assertions.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getScreenVolumeX().orElseThrow(),
                                file.getRelativeMetadata().getScreenVolumeX().orElseThrow(), DISTANCE_PRECISION);
        Assertions.assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativePosition().orElseThrow().getY(),
                                file.getRelativeMetadata().getRelativePosition().orElseThrow().getY(), DISTANCE_PRECISION);
        Assertions.assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativeVelocity().orElseThrow().getZ(),
                                file.getRelativeMetadata().getRelativeVelocity().orElseThrow().getZ(), DERIVATION_PRECISION);
        Assertions.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbability().orElseThrow(),
                                file.getRelativeMetadata().getCollisionProbability().orElseThrow(), 1e-30);
        Assertions.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbaMethod(),
                                file.getRelativeMetadata().getCollisionProbaMethod());

        // Check Metadata Block
        Assertions.assertEquals("OBJECT2", file.getMetadataObject2().getObject());
        Assertions.assertEquals("30337", file.getMetadataObject2().getObjectDesignator());
        Assertions.assertEquals("SATCAT", file.getMetadataObject2().getCatalogName());
        Assertions.assertEquals("FENGYUN 1C DEB", file.getMetadataObject2().getObjectName());
        Assertions.assertEquals("1999-025AA", file.getMetadataObject2().getInternationalDes());
        Assertions.assertEquals("NONE", file.getMetadataObject2().getEphemName());
        Assertions.assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject2().getCovarianceMethod());
        Assertions.assertEquals(Maneuvrable.NO, file.getMetadataObject2().getManeuverable());
        Assertions.assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject2().getRefFrame().asCelestialBodyFrame().orElseThrow());
        Assertions.assertEquals("UTC", file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        Assertions.assertEquals(2569.540800e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getX(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(-2.888612500e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(),
                                DERIVATION_PRECISION);
        // Covariance Matrix block
        Assertions.assertEquals(1.337e3, file.getDataObject2().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(2.591e-3, file.getDataObject2().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(6.886e-5, file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.059e-5, file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(8.633e-5, file.getDataObject2().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(-5.117E-01, file.getDataObject2().getRTNCovarianceBlock().getCdrgr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.319E+00, file.getDataObject2().getRTNCovarianceBlock().getCdrgt(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.903E-05, file.getDataObject2().getRTNCovarianceBlock().getCdrgndot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(7.402E-05, file.getDataObject2().getRTNCovarianceBlock().getCdrgtdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-3.297E+01, file.getDataObject2().getRTNCovarianceBlock().getCsrpr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(8.164E+01, file.getDataObject2().getRTNCovarianceBlock().getCsrpt(),
                                COVARIANCE_PRECISION);

        // Verify comments
        Assertions.assertEquals("[Relative Metadata/Data]", file.getRelativeMetadata().getComment().toString());
        Assertions.assertEquals("[Object1 Metadata]", file.getMetadataObject1().getComments().toString());
        Assertions.assertEquals("[Object2 Metadata]", file.getMetadataObject2().getComments().toString());
        Assertions.assertEquals("[Object1 Data]", file.getDataObject1().getComments().toString());
        Assertions.assertEquals("[Object1 OD Parameters]",
                                odBlock1.getComments().toString());
        Assertions.assertEquals("[Object2 OD Parameters]",
                                file.getDataObject2().getODParametersBlock().orElseThrow().getComments().toString());
        Assertions.assertEquals(
                                "[Object1 Additional Parameters, Apogee Altitude=779 km, Perigee Altitude=765 km, Inclination=86.4 deg]",
                                add1.getComments().toString());
        Assertions.assertEquals(
                                "[Object2 Additional Parameters, Apogee Altitude=786 km, Perigee Altitude=414 km, Inclination=98.9 deg]",
                                file.getDataObject2().getAdditionalParametersBlock().orElseThrow().getComments().toString());

    }

    @Test
    public void testParseCDM3() {
        /* Test for CdmExample3.txt, with only required data. */
        // File
        final String ex = "/ccsds/cdm/CDMExample3.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);


        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(), file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assertions.assertEquals(new AbsoluteDate(2012, 9, 12, 22, 31, 12,
                                                 TimeScalesFactory.getUTC()), file.getHeader().getCreationDate());
        Assertions.assertEquals("SDC", file.getHeader().getOriginator());
        Assertions.assertEquals("GALAXY 15", file.getHeader().getMessageFor().orElseThrow());
        Assertions.assertEquals("20120912223112", file.getHeader().getMessageId().orElseThrow());

        // OBJECT1
        // Check Relative Metadata Block
        Assertions.assertEquals(new AbsoluteDate(2012, 9, 13, 22, 37, new TimeOffset(52, TimeOffset.SECOND, 618, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getTca());
        Assertions.assertEquals(104.92, file.getRelativeMetadata().getMissDistance(), DISTANCE_PRECISION);
        Assertions.assertEquals(12093.52, file.getRelativeMetadata().getRelativeSpeed().orElseThrow(), DERIVATION_PRECISION);
        Assertions.assertEquals(30.6, file.getRelativeMetadata().getRelativePosition().orElseThrow().getX(), DISTANCE_PRECISION);
        Assertions.assertEquals(100.2, file.getRelativeMetadata().getRelativePosition().orElseThrow().getY(), DISTANCE_PRECISION);
        Assertions.assertEquals(5.7, file.getRelativeMetadata().getRelativePosition().orElseThrow().getZ(), DISTANCE_PRECISION);
        Assertions.assertEquals(-20.3, file.getRelativeMetadata().getRelativeVelocity().orElseThrow().getX(), DERIVATION_PRECISION);
        Assertions.assertEquals(-12000.0, file.getRelativeMetadata().getRelativeVelocity().orElseThrow().getY(),
                                DERIVATION_PRECISION);
        Assertions.assertEquals(-1500.9, file.getRelativeMetadata().getRelativeVelocity().orElseThrow().getZ(), DERIVATION_PRECISION);
        Assertions.assertEquals(new AbsoluteDate(2012, 9, 12, 18, 29, new TimeOffset(32, TimeOffset.SECOND, 212, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStartScreenPeriod().orElseThrow());
        Assertions.assertEquals(new AbsoluteDate(2012, 9, 15, 18, 29, new TimeOffset(32, TimeOffset.SECOND, 212, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStopScreenPeriod().orElseThrow());
        Assertions.assertEquals(ScreenVolumeFrame.RTN, file.getRelativeMetadata().getScreenVolumeFrame().orElseThrow());
        Assertions.assertEquals(ScreenVolumeShape.ELLIPSOID, file.getRelativeMetadata().getScreenVolumeShape().orElseThrow());
        Assertions.assertEquals(500, file.getRelativeMetadata().getScreenVolumeX().orElseThrow(), 0);
        Assertions.assertEquals(500, file.getRelativeMetadata().getScreenVolumeY().orElseThrow(), 0);
        Assertions.assertEquals(500, file.getRelativeMetadata().getScreenVolumeZ().orElseThrow(), 0);
        Assertions.assertEquals(new AbsoluteDate(2012, 9, 13, 20, 25, new TimeOffset(43, TimeOffset.SECOND, 222, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenEntryTime().orElseThrow());
        Assertions.assertEquals(new AbsoluteDate(2012, 9, 13, 23, 44, new TimeOffset(29, TimeOffset.SECOND, 324, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenExitTime().orElseThrow());
        Assertions.assertEquals(2.355e-03, file.getRelativeMetadata().getCollisionProbability().orElseThrow(), 1e-30);
        Assertions.assertEquals("ALFANO-2005", file.getRelativeMetadata().getCollisionProbaMethod().orElseThrow().getName());
        Assertions.assertEquals(PocMethodType.ALFANO_2005,
                                file.getRelativeMetadata().getCollisionProbaMethod().orElseThrow().getType());

        // Check Metadata Block
        Assertions.assertEquals("OBJECT1", file.getMetadataObject1().getObject());
        Assertions.assertEquals("28884", file.getMetadataObject1().getObjectDesignator());
        Assertions.assertEquals("SATCAT", file.getMetadataObject1().getCatalogName());
        Assertions.assertEquals("GALAXY 15", file.getMetadataObject1().getObjectName());
        Assertions.assertEquals("2005-041A", file.getMetadataObject1().getInternationalDes());
        Assertions.assertEquals(ObjectType.PAYLOAD, file.getMetadataObject1().getObjectType().orElseThrow());
        Assertions.assertFalse(file.getMetadataObject1().getOperatorContactPosition().isPresent());
        Assertions.assertEquals("INTELSAT", file.getMetadataObject1().getOperatorOrganization().orElseThrow());
        Assertions.assertEquals("GALAXY-15A-2012JAN-WMANEUVER23A", file.getMetadataObject1().getEphemName());
        Assertions.assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject1().getCovarianceMethod());
        Assertions.assertEquals(Maneuvrable.YES, file.getMetadataObject1().getManeuverable());
        Assertions.assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject1().getRefFrame().asCelestialBodyFrame().orElseThrow());
        Assertions.assertEquals("UTC", file.getMetadataObject1().getTimeSystem().name());

        // Check data block
        // OD parameters block
        Assertions.assertEquals(new AbsoluteDate(2012, 9, 6, 20, 25, new TimeOffset(43, TimeOffset.SECOND, 222, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()),
                                file.getDataObject1().getODParametersBlock().orElseThrow().getTimeLastObsStart().orElseThrow());
        Assertions.assertEquals(new AbsoluteDate(2012, 9, 6, 20, 25, new TimeOffset(43, TimeOffset.SECOND, 222, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()),
                                file.getDataObject1().getODParametersBlock().orElseThrow().getTimeLastObsEnd().orElseThrow());
        // State vector block
        Assertions.assertEquals(-41600.46272465e3,
                                file.getDataObject1().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        Assertions.assertEquals(3626.912120064e3,
                                file.getDataObject1().getStateVectorBlock().getPositionVector().getY(), DISTANCE_PRECISION);
        Assertions.assertEquals(6039.06350924e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getZ(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(-0.306132852503e3,
                                file.getDataObject1().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
        Assertions.assertEquals(-3.044998353334e3,
                                file.getDataObject1().getStateVectorBlock().getVelocityVector().getY(), DERIVATION_PRECISION);
        Assertions.assertEquals(-0.287674310725e3,
                                file.getDataObject1().getStateVectorBlock().getVelocityVector().getZ(), DERIVATION_PRECISION);
        // Covariance Matrix block
        Assertions.assertEquals(4.142e1, file.getDataObject1().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-8.579, file.getDataObject1().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.533e3, file.getDataObject1().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(1.336e1, file.getDataObject1().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(7.098e1, file.getDataObject1().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(2.520E-03, file.getDataObject1().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-5.476E+00, file.getDataObject1().getRTNCovarianceBlock().getCrdott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(8.626E-04, file.getDataObject1().getRTNCovarianceBlock().getCrdotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(5.744E-03, file.getDataObject1().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(-1.006E-02, file.getDataObject1().getRTNCovarianceBlock().getCtdotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(4.041E-03, file.getDataObject1().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.359E-03, file.getDataObject1().getRTNCovarianceBlock().getCtdotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.502E-05, file.getDataObject1().getRTNCovarianceBlock().getCtdotrdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.049E-05, file.getDataObject1().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(1.053E-03, file.getDataObject1().getRTNCovarianceBlock().getCndotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-3.412E-03, file.getDataObject1().getRTNCovarianceBlock().getCndott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.213E-02, file.getDataObject1().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-3.004E-06, file.getDataObject1().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.091E-06, file.getDataObject1().getRTNCovarianceBlock().getCndottdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(5.529E-05, file.getDataObject1().getRTNCovarianceBlock().getCndotndot(),
                                COVARIANCE_PRECISION);

        // OBJECT2
        // Check Relative Metadata Block
        Assertions.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getScreenVolumeX().orElseThrow(),
                                file.getRelativeMetadata().getScreenVolumeX().orElseThrow(), DISTANCE_PRECISION);
        Assertions.assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativePosition().orElseThrow().getX(),
                                file.getRelativeMetadata().getRelativePosition().orElseThrow().getX(), DISTANCE_PRECISION);
        Assertions.assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativeVelocity().orElseThrow().getZ(),
                                file.getRelativeMetadata().getRelativeVelocity().orElseThrow().getZ(), DERIVATION_PRECISION);
        Assertions.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbability().orElseThrow(),
                                file.getRelativeMetadata().getCollisionProbability().orElseThrow(), 1e-30);
        Assertions.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbaMethod(),
                                file.getRelativeMetadata().getCollisionProbaMethod());

        // Check Metadata Block
        Assertions.assertEquals("OBJECT2", file.getMetadataObject2().getObject());
        Assertions.assertEquals("21139", file.getMetadataObject2().getObjectDesignator());
        Assertions.assertEquals("SATCAT", file.getMetadataObject2().getCatalogName());
        Assertions.assertEquals("ASTRA 1B", file.getMetadataObject2().getObjectName());
        Assertions.assertEquals("1991-051A", file.getMetadataObject2().getInternationalDes());
        Assertions.assertEquals(ObjectType.PAYLOAD, file.getMetadataObject2().getObjectType().orElseThrow());
        Assertions.assertEquals("NONE", file.getMetadataObject2().getEphemName());
        Assertions.assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject2().getCovarianceMethod());
        Assertions.assertEquals(Maneuvrable.YES, file.getMetadataObject2().getManeuverable());
        Assertions.assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject2().getRefFrame().asCelestialBodyFrame().orElseThrow());
        Assertions.assertEquals("UTC", file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        Assertions.assertEquals(-2956.02034826e3,
                                file.getDataObject2().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        Assertions.assertEquals(-3.047096589536e3,
                                file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
        // Covariance Matrix block
        Assertions.assertEquals(1.337e3, file.getDataObject2().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(2.591e-3, file.getDataObject2().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(6.886e-5, file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.059e-5, file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(8.633e-5, file.getDataObject2().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);

        // Verify comments
        Assertions.assertEquals("[Relative Metadata/Data]",
                                file.getMetadataObject1().getRelativeMetadata().getComment().toString());
        Assertions.assertEquals("[Object1 Metadata]", file.getMetadataObject1().getComments().toString());
        Assertions.assertEquals("[Object2 Metadata]", file.getMetadataObject2().getComments().toString());
        Assertions.assertEquals("[Object1 OD Parameters]",
                                file.getDataObject1().getODParametersBlock().orElseThrow().getComments().toString());
        Assertions.assertEquals("[Object1 Data]", file.getDataObject1().getComments().toString());
        Assertions.assertEquals("[Object2 Data]", file.getDataObject2().getComments().toString());
        Assertions.assertEquals("[Object2 OD Parameters]",
                                file.getDataObject2().getODParametersBlock().orElseThrow().getComments().toString());
        Assertions.assertEquals("[]", file.getDataObject1().getStateVectorBlock().getComments().toString());
        Assertions.assertEquals("[Object1 Covariance in the RTN Coordinate Frame]",
                                file.getDataObject1().getRTNCovarianceBlock().getComments().toString());

    }

    @Test
    public void testParseCDM4() {
        /* Test for CdmExample2.txt, with only required data. */
        // File
        final String ex = "/ccsds/cdm/CDMExample4.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(), file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12,
                                                 TimeScalesFactory.getUTC()), file.getHeader().getCreationDate());
        Assertions.assertEquals("JSPOC", file.getHeader().getOriginator());
        Assertions.assertEquals("201113719185", file.getHeader().getMessageId().orElseThrow());

        // OBJECT1
        // Check Relative Metadata Block
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, new TimeOffset(52, TimeOffset.SECOND, 618, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getTca());
        Assertions.assertEquals(715.0, file.getRelativeMetadata().getMissDistance(), DISTANCE_PRECISION);

        Assertions.assertEquals(new AbsoluteDate(2010, 3, 12, 18, 29, new TimeOffset(32, TimeOffset.SECOND, 212, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStartScreenPeriod().orElseThrow());
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 15, 18, 29, new TimeOffset(32, TimeOffset.SECOND, 212, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStopScreenPeriod().orElseThrow());
        Assertions.assertEquals(ScreenVolumeFrame.RTN, file.getRelativeMetadata().getScreenVolumeFrame().orElseThrow());
        Assertions.assertEquals(ScreenVolumeShape.ELLIPSOID, file.getRelativeMetadata().getScreenVolumeShape().orElseThrow());
        Assertions.assertEquals(200, file.getRelativeMetadata().getScreenVolumeX().orElseThrow(), 0);
        Assertions.assertEquals(1000, file.getRelativeMetadata().getScreenVolumeY().orElseThrow(), 0);
        Assertions.assertEquals(1000, file.getRelativeMetadata().getScreenVolumeZ().orElseThrow(), 0);
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, new TimeOffset(52, TimeOffset.SECOND, 222, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenEntryTime().orElseThrow());
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, new TimeOffset(52, TimeOffset.SECOND, 824, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenExitTime().orElseThrow());
        Assertions.assertEquals(4.835E-05, file.getRelativeMetadata().getCollisionProbability().orElseThrow(), 1e-30);
        Assertions.assertEquals("FOSTER-1992", file.getRelativeMetadata().getCollisionProbaMethod().orElseThrow().getName());
        Assertions.assertEquals(PocMethodType.FOSTER_1992,
                                file.getRelativeMetadata().getCollisionProbaMethod().orElseThrow().getType());

        // Check Metadata Block
        Assertions.assertEquals("OBJECT1", file.getMetadataObject1().getObject());
        Assertions.assertEquals(ObjectType.PAYLOAD, file.getMetadataObject1().getObjectType().orElseThrow());
        Assertions.assertEquals("OSA", file.getMetadataObject1().getOperatorContactPosition().orElseThrow());
        Assertions.assertEquals("EUMETSAT", file.getMetadataObject1().getOperatorOrganization().orElseThrow());

        // Covariance Matrix block
        Assertions.assertEquals(4.142e1, file.getDataObject1().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-8.579, file.getDataObject1().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.533e3, file.getDataObject1().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(1.336e1, file.getDataObject1().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(7.098e1, file.getDataObject1().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(-1.862E+00, file.getDataObject1().getRTNCovarianceBlock().getCdrgr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(3.530E+00, file.getDataObject1().getRTNCovarianceBlock().getCdrgt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-3.100E-01, file.getDataObject1().getRTNCovarianceBlock().getCdrgn(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-1.214E-04, file.getDataObject1().getRTNCovarianceBlock().getCdrgrdot(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.580E-04, file.getDataObject1().getRTNCovarianceBlock().getCdrgtdot(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-6.467E-05, file.getDataObject1().getRTNCovarianceBlock().getCdrgndot(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(3.483E-06, file.getDataObject1().getRTNCovarianceBlock().getCdrgdrg(),
                                COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(-1.492E+02, file.getDataObject1().getRTNCovarianceBlock().getCsrpr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.044E+02, file.getDataObject1().getRTNCovarianceBlock().getCsrpt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-2.331E+01, file.getDataObject1().getRTNCovarianceBlock().getCsrpn(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-1.254E-03, file.getDataObject1().getRTNCovarianceBlock().getCsrprdot(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.013E-02, file.getDataObject1().getRTNCovarianceBlock().getCsrptdot(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-4.700E-03, file.getDataObject1().getRTNCovarianceBlock().getCsrpndot(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.210E-04, file.getDataObject1().getRTNCovarianceBlock().getCsrpdrg(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(1.593E-02, file.getDataObject1().getRTNCovarianceBlock().getCsrpsrp(),
                                COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(-1.803E-06, file.getDataObject1().getRTNCovarianceBlock().getCthrr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(3.803E-03, file.getDataObject1().getRTNCovarianceBlock().getCthrt(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(3.303E02, file.getDataObject1().getRTNCovarianceBlock().getCthrn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(7.203E01, file.getDataObject1().getRTNCovarianceBlock().getCthrrdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.654E01, file.getDataObject1().getRTNCovarianceBlock().getCthrtdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(9.203E-01, file.getDataObject1().getRTNCovarianceBlock().getCthrndot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-3.876, file.getDataObject1().getRTNCovarianceBlock().getCthrdrg(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(6.876E03, file.getDataObject1().getRTNCovarianceBlock().getCthrsrp(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-2.986E-02, file.getDataObject1().getRTNCovarianceBlock().getCthrthr(),
                                COVARIANCE_PRECISION);

        // OBJECT2
        // Check Relative Metadata Block
        Assertions.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getScreenVolumeX().orElseThrow(),
                                file.getRelativeMetadata().getScreenVolumeX().orElseThrow(), DISTANCE_PRECISION);
        Assertions.assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativePosition().orElseThrow().getY(),
                                file.getRelativeMetadata().getRelativePosition().orElseThrow().getY(), DISTANCE_PRECISION);
        Assertions.assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativeVelocity().orElseThrow().getZ(),
                                file.getRelativeMetadata().getRelativeVelocity().orElseThrow().getZ(), DERIVATION_PRECISION);
        Assertions.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbability().orElseThrow(),
                                file.getRelativeMetadata().getCollisionProbability().orElseThrow(), 1e-30);
        Assertions.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbaMethod(),
                                file.getRelativeMetadata().getCollisionProbaMethod());

        // Check Metadata Block
        Assertions.assertEquals("OBJECT2", file.getMetadataObject2().getObject());
        Assertions.assertEquals("30337", file.getMetadataObject2().getObjectDesignator());
        Assertions.assertEquals("SATCAT", file.getMetadataObject2().getCatalogName());
        Assertions.assertEquals("FENGYUN 1C DEB", file.getMetadataObject2().getObjectName());
        Assertions.assertEquals("1999-025AA", file.getMetadataObject2().getInternationalDes());
        Assertions.assertEquals("NONE", file.getMetadataObject2().getEphemName());
        Assertions.assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject2().getCovarianceMethod());
        Assertions.assertEquals(Maneuvrable.NO, file.getMetadataObject2().getManeuverable());
        Assertions.assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject2().getRefFrame().asCelestialBodyFrame().orElseThrow());
        Assertions.assertEquals("UTC", file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        Assertions.assertEquals(2569.540800e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getX(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(-2.888612500e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(),
                                DERIVATION_PRECISION);
        // Covariance Matrix block
        Assertions.assertEquals(1.337e3, file.getDataObject2().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(2.591e-3, file.getDataObject2().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(6.886e-5, file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.059e-5, file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(8.633e-5, file.getDataObject2().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(-5.117E-01, file.getDataObject2().getRTNCovarianceBlock().getCdrgr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.319E+00, file.getDataObject2().getRTNCovarianceBlock().getCdrgt(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.903E-05, file.getDataObject2().getRTNCovarianceBlock().getCdrgndot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(7.402E-05, file.getDataObject2().getRTNCovarianceBlock().getCdrgtdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-3.297E+01, file.getDataObject2().getRTNCovarianceBlock().getCsrpr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(8.164E+01, file.getDataObject2().getRTNCovarianceBlock().getCsrpt(),
                                COVARIANCE_PRECISION);

        // Verify comments
        Assertions.assertEquals("[Relative Metadata/Data]", file.getRelativeMetadata().getComment().toString());
        Assertions.assertEquals("[Object1 Metadata]", file.getMetadataObject1().getComments().toString());
        Assertions.assertEquals("[Object2 Metadata]", file.getMetadataObject2().getComments().toString());
        Assertions.assertEquals("[Object1 Data]", file.getDataObject1().getComments().toString());
        Assertions.assertEquals("[Object1 OD Parameters]",
                                file.getDataObject1().getODParametersBlock().orElseThrow().getComments().toString());
        Assertions.assertEquals("[Object2 OD Parameters]",
                                file.getDataObject2().getODParametersBlock().orElseThrow().getComments().toString());
        Assertions.assertEquals(
                                "[Object1 Additional Parameters, Apogee Altitude=779 km, Perigee Altitude=765 km, Inclination=86.4 deg]",
                                file.getDataObject1().getAdditionalParametersBlock().orElseThrow().getComments().toString());
        Assertions.assertEquals(
                                "[Object2 Additional Parameters, Apogee Altitude=786 km, Perigee Altitude=414 km, Inclination=98.9 deg]",
                                file.getDataObject2().getAdditionalParametersBlock().orElseThrow().getComments().toString());

    }


    @Test
    public void testParseXML_CDM1() {
        /* Test for CdmExample1.xml, with only required data. */
        // File
        final String ex = "/ccsds/cdm/CDMExample1.xml";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(), file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12.000,
                                                 TimeScalesFactory.getUTC()), file.getHeader().getCreationDate());
        Assertions.assertEquals("JSPOC", file.getHeader().getOriginator());
        Assertions.assertEquals("SATELLITE A", file.getHeader().getMessageFor().orElseThrow());
        Assertions.assertEquals("20111371985", file.getHeader().getMessageId().orElseThrow());

        // OBJECT1
        // Check Relative Metadata Block
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, new TimeOffset(52, TimeOffset.SECOND,
                                                                                     618, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getTca());
        Assertions.assertEquals(715.0, file.getRelativeMetadata().getMissDistance(), DISTANCE_PRECISION);
        Assertions.assertEquals(14762.0, file.getRelativeMetadata().getRelativeSpeed().orElseThrow(), DERIVATION_PRECISION);
        Assertions.assertEquals(27.4, file.getRelativeMetadata().getRelativePosition().orElseThrow().getX(), DISTANCE_PRECISION);
        Assertions.assertEquals(-70.2, file.getRelativeMetadata().getRelativePosition().orElseThrow().getY(), DISTANCE_PRECISION);
        Assertions.assertEquals(711.8, file.getRelativeMetadata().getRelativePosition().orElseThrow().getZ(), DISTANCE_PRECISION);
        Assertions.assertEquals(-7.2, file.getRelativeMetadata().getRelativeVelocity().orElseThrow().getX(), DERIVATION_PRECISION);
        Assertions.assertEquals(-14692.0, file.getRelativeMetadata().getRelativeVelocity().orElseThrow().getY(),
                                DERIVATION_PRECISION);
        Assertions.assertEquals(-1437.2, file.getRelativeMetadata().getRelativeVelocity().orElseThrow().getZ(),
                                DERIVATION_PRECISION);
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 12, 18, 29, new TimeOffset(32, TimeOffset.SECOND,
                                                                                     212, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStartScreenPeriod().orElseThrow());
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 15, 18, 29, new TimeOffset(32, TimeOffset.SECOND,
                                                                                     212, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStopScreenPeriod().orElseThrow());
        Assertions.assertEquals(ScreenVolumeFrame.RTN, file.getRelativeMetadata().getScreenVolumeFrame().orElseThrow());
        Assertions.assertEquals(ScreenVolumeShape.ELLIPSOID, file.getRelativeMetadata().getScreenVolumeShape().orElseThrow());
        Assertions.assertEquals(200, file.getRelativeMetadata().getScreenVolumeX().orElseThrow(), 0);
        Assertions.assertEquals(1000, file.getRelativeMetadata().getScreenVolumeY().orElseThrow(), 0);
        Assertions.assertEquals(1000, file.getRelativeMetadata().getScreenVolumeZ().orElseThrow(), 0);
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 13, 20, 25, new TimeOffset(43, TimeOffset.SECOND,
                                                                                     222, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenEntryTime().orElseThrow());
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 13, 23, 44, new TimeOffset(29, TimeOffset.SECOND,
                                                                                     324, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenExitTime().orElseThrow());
        Assertions.assertEquals(4.835E-05, file.getRelativeMetadata().getCollisionProbability().orElseThrow(), 1e-30);
        Assertions.assertEquals("FOSTER-1992", file.getRelativeMetadata().getCollisionProbaMethod().orElseThrow().getName());
        Assertions.assertEquals(PocMethodType.FOSTER_1992,
                                file.getRelativeMetadata().getCollisionProbaMethod().orElseThrow().getType());

        // Check Metadata Block
        Assertions.assertEquals("OBJECT1", file.getMetadataObject1().getObject());
        Assertions.assertEquals("12345", file.getMetadataObject1().getObjectDesignator());
        Assertions.assertEquals("SATCAT", file.getMetadataObject1().getCatalogName());
        Assertions.assertEquals("SATELLITE A", file.getMetadataObject1().getObjectName());
        Assertions.assertEquals("1997-030E", file.getMetadataObject1().getInternationalDes());
        Assertions.assertEquals("EPHEMERIS SATELLITE A", file.getMetadataObject1().getEphemName());
        Assertions.assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject1().getCovarianceMethod());
        Assertions.assertEquals(Maneuvrable.YES, file.getMetadataObject1().getManeuverable());
        Assertions.assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject1().getRefFrame().asCelestialBodyFrame().orElseThrow());
        Assertions.assertEquals("UTC", file.getMetadataObject1().getTimeSystem().name());


        // Check data block
        // State vector block
        Assertions.assertEquals(2570.097065e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getX(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(2244.654904e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getY(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(6281.497978e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getZ(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(4.418769571e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getX(),
                                DERIVATION_PRECISION);
        Assertions.assertEquals(4.833547743e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getY(),
                                DERIVATION_PRECISION);
        Assertions.assertEquals(-3.526774282e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getZ(),
                                DERIVATION_PRECISION);
        // Covariance Matrix block
        Assertions.assertEquals(4.142e1, file.getDataObject1().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-8.579, file.getDataObject1().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.533e3, file.getDataObject1().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(1.336e1, file.getDataObject1().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(7.098e1, file.getDataObject1().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(2.520e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-5.476, file.getDataObject1().getRTNCovarianceBlock().getCrdott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(8.626e-4, file.getDataObject1().getRTNCovarianceBlock().getCrdotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(5.744e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(-1.006e-2, file.getDataObject1().getRTNCovarianceBlock().getCtdotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(4.041e-3, file.getDataObject1().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.359e-3, file.getDataObject1().getRTNCovarianceBlock().getCtdotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.502e-5, file.getDataObject1().getRTNCovarianceBlock().getCtdotrdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.049e-5, file.getDataObject1().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(1.053e-3, file.getDataObject1().getRTNCovarianceBlock().getCndotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-3.412e-3, file.getDataObject1().getRTNCovarianceBlock().getCndott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.213e-2, file.getDataObject1().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-3.004e-6, file.getDataObject1().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.091e-6, file.getDataObject1().getRTNCovarianceBlock().getCndottdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(5.529e-5, file.getDataObject1().getRTNCovarianceBlock().getCndotndot(),
                                COVARIANCE_PRECISION);

        // OBJECT2
        // Check Relative Metadata Block
        Assertions.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getScreenVolumeX().orElseThrow(),
                                file.getRelativeMetadata().getScreenVolumeX().orElseThrow(), DISTANCE_PRECISION);
        Assertions.assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativePosition().orElseThrow().getZ(),
                                file.getRelativeMetadata().getRelativePosition().orElseThrow().getZ(), DISTANCE_PRECISION);
        Assertions.assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativeVelocity().orElseThrow().getZ(),
                                file.getRelativeMetadata().getRelativeVelocity().orElseThrow().getZ(), DERIVATION_PRECISION);
        Assertions.assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbability().orElseThrow(),
                                file.getRelativeMetadata().getCollisionProbability().orElseThrow(), 1e-30);
        Assertions.assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbaMethod(),
                                file.getRelativeMetadata().getCollisionProbaMethod());

        // Check Metadata Block
        Assertions.assertEquals("OBJECT2", file.getMetadataObject2().getObject());
        Assertions.assertEquals("30337", file.getMetadataObject2().getObjectDesignator());
        Assertions.assertEquals("SATCAT", file.getMetadataObject2().getCatalogName());
        Assertions.assertEquals("FENGYUN 1C DEB", file.getMetadataObject2().getObjectName());
        Assertions.assertEquals("1999-025AA", file.getMetadataObject2().getInternationalDes());
        Assertions.assertEquals("NONE", file.getMetadataObject2().getEphemName());
        Assertions.assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject2().getCovarianceMethod());
        Assertions.assertEquals(Maneuvrable.NO, file.getMetadataObject2().getManeuverable());
        Assertions.assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject2().getRefFrame().asCelestialBodyFrame().orElseThrow());
        Assertions.assertEquals("UTC", file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        Assertions.assertEquals(2569.540800e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getX(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(2245.093614e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getY(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(6281.599946e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getZ(),
                                DISTANCE_PRECISION);
        Assertions.assertEquals(-2.888612500e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(),
                                DERIVATION_PRECISION);
        Assertions.assertEquals(-6.007247516e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getY(),
                                DERIVATION_PRECISION);
        Assertions.assertEquals(3.328770172e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getZ(),
                                DERIVATION_PRECISION);
        // Covariance Matrix block
        Assertions.assertEquals(1.337e3, file.getDataObject2().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-4.806e4, file.getDataObject2().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.492e6, file.getDataObject2().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-3.298e1, file.getDataObject2().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(7.105e1, file.getDataObject2().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(2.591e-3, file.getDataObject2().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-4.152e-2, file.getDataObject2().getRTNCovarianceBlock().getCrdott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.784e-6, file.getDataObject2().getRTNCovarianceBlock().getCrdotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(6.886e-5, file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(-1.016e-2, file.getDataObject2().getRTNCovarianceBlock().getCtdotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.637e-3, file.getDataObject2().getRTNCovarianceBlock().getCtdotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-2.987e-6, file.getDataObject2().getRTNCovarianceBlock().getCtdotrdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(1.059e-5, file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        Assertions.assertEquals(4.400e-3, file.getDataObject2().getRTNCovarianceBlock().getCndotr(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(8.482e-3, file.getDataObject2().getRTNCovarianceBlock().getCndott(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(8.633e-5, file.getDataObject2().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(-4.594e-6, file.getDataObject2().getRTNCovarianceBlock().getCndottdot(),
                                COVARIANCE_PRECISION);
        Assertions.assertEquals(5.178e-5, file.getDataObject2().getRTNCovarianceBlock().getCndotndot(),
                                COVARIANCE_PRECISION);

        // Check relative metadata comments for Object1
        ArrayList<String> relativeMetadataComment = new ArrayList<>();
        relativeMetadataComment.add("Relative Metadata/Data");
        Assertions.assertEquals(relativeMetadataComment, file.getRelativeMetadata().getComment());

        // Check metadata comments for Object1
        ArrayList<String> MetadataComment = new ArrayList<>();
        MetadataComment.add("Object1 Metadata");
        Assertions.assertEquals(MetadataComment, file.getMetadataObject1().getComments());

        // Check data general comments and OD parameters comments for Object1
        ArrayList<String> generalComment = new ArrayList<>();
        generalComment.add("Object1 Data");
        Assertions.assertEquals(generalComment, file.getDataObject1().getComments());

        // Check additional parameters comments Object1
        ArrayList<String> addParametersComment = new ArrayList<>();
        addParametersComment.add("Object 1 Additional Parameters");
        Assertions.assertEquals(addParametersComment,
                                file.getDataObject1().getAdditionalParametersBlock().orElseThrow().getComments());

        // Check state vector comments Object1
        ArrayList<String> stateVectorComment = new ArrayList<>();
        stateVectorComment.add("Object1 State Vector");
        Assertions.assertEquals(stateVectorComment, file.getDataObject1().getStateVectorBlock().getComments());

        // Check RTN covariance comments Object1
        ArrayList<String> RTNComment = new ArrayList<>();
        RTNComment.add("Object1 Covariance in the RTN Coordinate Frame");
        Assertions.assertEquals(RTNComment, file.getDataObject1().getRTNCovarianceBlock().getComments());


        // Check general comments Object2
        ArrayList<String> generalCommentObj2AddParam = new ArrayList<>();
        generalCommentObj2AddParam.add("Object2 Additional Parameters");
        generalCommentObj2AddParam.add("Apogee Altitude=768 km, Perigee Altitude=414 km, Inclination=98.8 deg");
        Assertions.assertEquals(generalCommentObj2AddParam.toString(),
                                file.getDataObject2().getAdditionalParametersBlock().orElseThrow().getComments().toString());

    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/cdm/CDMExample1.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        try {
            final DataSource source = new DataSource(wrongName, () -> getClass().getResourceAsStream(wrongName));
            new ParserBuilder().
            buildCdmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assertions.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingTCA() {
        final String name = "/ccsds/cdm/CDM-missing-TCA.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            buildCdmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assertions.assertEquals("TCA", oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingObj2StateVectorX() {
        final String name = "/ccsds/cdm/CDM-missing-object2-state-vector.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            buildCdmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assertions.assertEquals("X", oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingObj1CovarianceCNDOT_NDOT() {
        final String name = "/ccsds/cdm/CDM-missing-object1-covariance-block.xml";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            buildCdmParser().
            parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assertions.assertEquals("CR_R", oe.getParts()[0]);
        }
    }

    /** Test that the Earth is returned by default when no orbit center were explicitly defined. */
    @Test
    public void testMissingObj1OrbitCenterGetFrame() {

        // GIVEN
        final String ex = "/ccsds/cdm/CDM-no-orbit-center-defined-obj1.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // WHEN
        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        // WHEN
        final BodyFacade obj1OrbitCenter     = file.getMetadataObject1().getOrbitCenter().orElseThrow();
        final BodyFacade expectedOrbitCenter = new BodyFacade(CelestialBodyFactory.EARTH.toUpperCase(),
                                                              CelestialBodyFactory.getEarth());

        Assertions.assertEquals(expectedOrbitCenter.getName(), obj1OrbitCenter.getName());
        Assertions.assertEquals(expectedOrbitCenter.getBody(), obj1OrbitCenter.getBody());

    }

    @Test
    public void testCovarianceNumberFormatErrorType() {
        final String ex = "/ccsds/cdm/CDM-covariance-wrong-entry-format.txt";
        try {
            final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            new ParserBuilder().
            buildCdmParser().
            parseMessage(source);
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals("CRDOT_T", oe.getParts()[0]);
            Assertions.assertEquals(65, oe.getParts()[1]);
        }
    }

    @Test
    public void test_issue_940() {

        // Files
        final String cdm_kvn = "/ccsds/cdm/CDMExample_issue_940.txt";
        final String cdm_xml = "/ccsds/cdm/CDMExample_issue_940.xml";


        test_issue_940_data(cdm_kvn);
        test_issue_940_data(cdm_xml);

    }

    public void test_issue_940_data(String ex) {

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);



        // Tests additional CDM Header keys



        // Check CLASSIFICATION is correctly read
        Assertions.assertEquals("\"Free text - for examples see CDM pink book (CCSDS 508.0-P-1.0.2)\"", 
                                file.getHeader().getClassification().orElseThrow(),"CLASSIFICATION");



        // Test additional CDM Relative Metadata Keys



        // Check CONJUNCTION_ID is correctly read
        Assertions.assertEquals("20220708T10hz SATELLITEA SATELLITEB", file.getRelativeMetadata().getConjunctionId().orElseThrow(), "CONJUNCTION ID");

        // Check APPROACH_ANGLE is correctly read
        Assertions.assertEquals(180.0, FastMath.toDegrees(file.getRelativeMetadata().getApproachAngle().orElseThrow()), 0.0);

        // Check SCREEN_TYPE is correctly read
        Assertions.assertEquals(ScreenType.SHAPE, file.getRelativeMetadata().getScreenType().orElseThrow(), "SCREEN_TYPE");

        // Check SCREEN_VOLUME_SHAPE is correctly read
        Assertions.assertEquals(ScreenVolumeShape.ELLIPSOID, file.getRelativeMetadata().getScreenVolumeShape().orElseThrow(), "SCREEN_VOLUME_SHAPE");

        // Check SCREEN_VOLUME_X is correctly read
        Assertions.assertEquals(200, file.getRelativeMetadata().getScreenVolumeX().orElseThrow(), 0.0);

        // Check SCREEN_VOLUME_Y is correctly read
        Assertions.assertEquals(1000, file.getRelativeMetadata().getScreenVolumeY().orElseThrow(), 0.0);

        // Check SCREEN_VOLUME_Z is correctly read
        Assertions.assertEquals(1000, file.getRelativeMetadata().getScreenVolumeZ().orElseThrow(), 0.0);

        // Check SCREEN_ENTRY_TIME is correctly read
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 13, 20, 25, new TimeOffset(43, TimeOffset.SECOND,
                                                                                     222, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()),
                                file.getRelativeMetadata().getScreenEntryTime().orElseThrow(), "SCREEN_ENTRY_TIME");

        // Check SCREEN_EXIT_TIME is correctly read
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 13, 23, 44, new TimeOffset(29, TimeOffset.SECOND,
                                                                                     324, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()),
                                file.getRelativeMetadata().getScreenExitTime().orElseThrow(), "SCREEN_EXIT_TIME");

        // SCREEN_PC_THRESHOLD
        Assertions.assertEquals(1.000E-03, file.getRelativeMetadata().getScreenPcThreshold().orElseThrow(), 0.0);

        // COLLISION_PERCENTILE
        int[] collisionPercentile = {50, 51, 52};
        Assertions.assertArrayEquals(collisionPercentile, file.getRelativeMetadata().getCollisionPercentile().orElseThrow());

        // COLLISION_PROBABILITY
        Assertions.assertEquals(4.835E-05, file.getRelativeMetadata().getCollisionProbability().orElseThrow(), 0.0);

        // Check COLLISION_PROBABILITY_METHOD is correctly read
        Assertions.assertEquals("FOSTER-1992", file.getRelativeMetadata().getCollisionProbaMethod().orElseThrow().getName(), "COLLISION_PROBABILITY_METHOD");

        // COLLISION_MAX_PROBABILITY
        Assertions.assertEquals(1.234E-05, file.getRelativeMetadata().getMaxCollisionProbability().orElseThrow(), 0.0);

        // Check COLLISION_MAX_PC_METHOD is correctly read
        Assertions.assertEquals("SCALE_COMBINED_COVAR", file.getRelativeMetadata().getMaxCollisionProbabilityMethod().orElseThrow().getName(), "COLLISION_MAX_PC_METHOD");

        // SEFI_COLLISION_PROBABILITY
        Assertions.assertEquals(1.234E-05, file.getRelativeMetadata().getSefiCollisionProbability().orElseThrow(), 0.0);

        // Check SEFI_COLLISION_PROBABILITY_METHOD is correctly read
        Assertions.assertEquals("SEFI_PC_METHOD", file.getRelativeMetadata().getSefiCollisionProbabilityMethod().orElseThrow().getName(),"SEFI_COLLISION_PROBABILITY_METHOD");

        // Check SEFI_FRAGMENTATION_MODEL is correctly read
        Assertions.assertEquals("NASA STD BREAKUP MODEL", file.getRelativeMetadata().getSefiFragmentationModel().orElseThrow(), "SEFI_FRAGMENTATION_MODEL");

        // Check PREVIOUS_MESSAGE_ID is correctly read
        Assertions.assertEquals("201113719185-0", file.getRelativeMetadata().getPreviousMessageId().orElseThrow(), "PREVIOUS_MESSAGE_ID");

        // Check PREVIOUS_MESSAGE_EPOCH is correctly read
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 12, 10, 31, 12, TimeScalesFactory.getUTC()), 
                                file.getRelativeMetadata().getPreviousMessageEpoch().orElseThrow(), "PREVIOUS_MESSAGE_EPOCH");

        // Check NEXT_MESSAGE_EPOCH is correctly read
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 13, 10, 31, 12, TimeScalesFactory.getUTC()), 
                                file.getRelativeMetadata().getNextMessageEpoch().orElseThrow(), "NEXT_MESSAGE_EPOCH");



        // Tests additional CDM Metadata keys



        // Check ODM_MSG_LINK is correctly read
        Assertions.assertEquals("ODM_MSG_35132.txt", file.getMetadataObject1().getOdmMsgLink().orElseThrow(), "ODM_MSG_LINK");

        // Check ADM_MSG_LINK is correctly read
        Assertions.assertEquals("ATT_MSG_35132.txt", file.getMetadataObject1().getAdmMsgLink().orElseThrow(),"ADM_MSG_LINK");

        // Check OBS_BEFORE_NEXT_MESSAGE is correctly read
        Assertions.assertEquals(YesNoUnknown.YES, file.getMetadataObject1().getObsBeforeNextMessage().orElseThrow(), "OBS_BEFORE_NEXT_MESSAGE");

        // Check COVARIANCE_SOURCE is correctly read
        Assertions.assertEquals("HAC Covariance", file.getMetadataObject1().getCovarianceSource().orElseThrow(), "COVARIANCE_SOURCE");

        // Check ALT_COV_TYPE is correctly read
        Assertions.assertEquals(AltCovarianceType.XYZ, file.getMetadataObject1().getAltCovType().orElseThrow(), "ALT_COV_TYPE");

        // Check ALT_COV_REF_FRAME is correctly read
        Assertions.assertEquals("EME2000", file.getMetadataObject1().getAltCovRefFrame().orElseThrow().getName(), "ALT_COV_REF_FRAME");



        // CDM OD Parameters



        // Check OD_EPOCH is correctly read
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12, TimeScalesFactory.getUTC()), 
                                file.getDataObject1().getODParametersBlock().orElseThrow().getOdEpoch().orElseThrow(), "OD_EPOCH");



        // CDM Additional Parameters



        // Check AREA_PC_MIN is correctly read
        AdditionalParameters add1 = file.getDataObject1().getAdditionalParametersBlock().orElseThrow();
        Assertions.assertEquals(5.0, add1.getAreaPCMin().orElseThrow(), 0.0);

        // Check AREA_PC_MAX is correctly read
        Assertions.assertEquals(5.4, add1.getAreaPCMax().orElseThrow(), 0.0);

        // Check OEB_PARENT_FRAME is correctly read
        Assertions.assertEquals("OEB_YAW", add1.getOebParentFrame().getName(), "OEB_PARENT_FRAME");

        // Check OEB_PARENT_FRAME_EPOCH is correctly read
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12, TimeScalesFactory.getUTC()), 
                                add1.getOebParentFrameEpoch().orElseThrow(), "OEB_PARENT_FRAME_EPOCH");

        // Check OEB_Q1 is correctly read
        Assertions.assertEquals(0.03123, add1.getOebQ().orElseThrow().getQ1(), 0.0);

        // Check OEB_Q2 is correctly read
        Assertions.assertEquals(0.78543, add1.getOebQ().orElseThrow().getQ2(), 0.0);

        // Check OEB_Q3 is correctly read
        Assertions.assertEquals(0.39158, add1.getOebQ().orElseThrow().getQ3(), 0.0);

        // Check OEB_QC is correctly read
        Assertions.assertEquals(0.47832, add1.getOebQ().orElseThrow().getQ0(), 0.0);

        // Check OEB_MAX is correctly read
        Assertions.assertEquals(2.0, add1.getOebMax().orElseThrow(), 0.0);

        // Check OEB_MED is correctly read
        Assertions.assertEquals(1.0, add1.getOebIntermediate().orElseThrow(), 0.0);

        // Check OEB_MIN is correctly read
        Assertions.assertEquals(0.5, add1.getOebMin().orElseThrow(), 0.0);

        // Check AREA_ALONG_OEB_MAX is correctly read
        Assertions.assertEquals(0.15, add1.getOebAreaAlongMax().orElseThrow(), 0.0);

        // Check AREA_ALONG_OEB_MED is correctly read
        Assertions.assertEquals(0.3, add1.getOebAreaAlongIntermediate().orElseThrow(), 0.0);

        // Check AREA_ALONG_OEB_MIN is correctly read
        Assertions.assertEquals(0.5, add1.getOebAreaAlongMin().orElseThrow(), 0.0);

        // Check RCS is correctly read
        Assertions.assertEquals(2.4, add1.getRcs().orElseThrow(), 0.0);

        // Check RCS_MIN is correctly read
        Assertions.assertEquals(1.4, add1.getMinRcs().orElseThrow(), 0.0);

        // Check RCS_MAX is correctly read
        Assertions.assertEquals(3.4, add1.getMaxRcs().orElseThrow(), 0.0);

        // Check VM_ABSOLUTE is correctly read
        Assertions.assertEquals(15.0, add1.getVmAbsolute().orElseThrow(), 0.0);

        // Check VM_APPARENT_MIN is correctly read
        Assertions.assertEquals(19.0, add1.getVmApparentMin().orElseThrow(), 0.0);

        // Check VM_APPARENT is correctly read
        Assertions.assertEquals(15.4, add1.getVmApparent().orElseThrow(), 0.0);

        // Check VM_APPARENT_MAX is correctly read
        Assertions.assertEquals(14.0, add1.getVmApparentMax().orElseThrow(), 0.0);

        // Check REFLECTANCE is correctly read
        Assertions.assertEquals(0.7, add1.getReflectance().orElseThrow(), 0.0);

        // Check HBR is correctly read
        Assertions.assertEquals(2.5, add1.getHbr().orElseThrow(), 0.0);

        // Check APOAPSIS_HEIGHT is correctly read
        Assertions.assertEquals(800000, add1.getApoapsisAltitude().orElseThrow(), 0.0);

        // Check PERIAPSIS_HEIGHT is correctly read
        Assertions.assertEquals(750000, add1.getPeriapsisAltitude().orElseThrow(), 0.0);

        // Check INCLINATION is correctly read
        Assertions.assertEquals(FastMath.toRadians(89.0), add1.getInclination().orElseThrow(), 0.0);

        // Check COV_CONFIDENCE is correctly read
        Assertions.assertEquals(1.0, add1.getCovConfidence().orElseThrow(), 0.0);

        // Check COV_CONFIDENCE_METHOD is correctly read
        Assertions.assertEquals("Wald test", add1.getCovConfidenceMethod().orElseThrow(), "COV_CONFIDENCE_METHOD");


        // XYZ Covariance Matrix block
        XYZCovariance cov1 = file.getDataObject1().getXYZCovarianceBlock().orElseThrow();
        Assertions.assertEquals(0.1,  cov1.getCxx(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(0.2,   cov1.getCyx(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(0.3,  cov1.getCyy(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(0.4, cov1.getCzx(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(0.5,  cov1.getCzy(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(0.6,  cov1.getCzz(), COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(0.7, cov1.getCxdotx(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.8,   cov1.getCxdoty(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.9, cov1.getCxdotz(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.01, cov1.getCxdotxdot(), COVARIANCE_PRECISION);

        Assertions.assertEquals(0.02, cov1.getCydotx(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.03,  cov1.getCydoty(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.04, cov1.getCydotz(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.05, cov1.getCydotxdot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.06,  cov1.getCydotydot(), COVARIANCE_PRECISION);

        Assertions.assertEquals(0.07,  cov1.getCzdotx(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.08, cov1.getCzdoty(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.09,  cov1.getCzdotz(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.001, cov1.getCzdotxdot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.002, cov1.getCzdotydot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.003,  cov1.getCzdotzdot(), COVARIANCE_PRECISION);
        
        Assertions.assertEquals(0.07,  cov1.getCdrgx(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.08, cov1.getCdrgy(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.09,  cov1.getCdrgz(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.001, cov1.getCdrgxdot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.002, cov1.getCdrgydot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.003,  cov1.getCdrgzdot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.004,  cov1.getCdrgdrg(), COVARIANCE_PRECISION);
        
        Assertions.assertEquals(0.07,  cov1.getCsrpx(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.08, cov1.getCsrpy(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.09,  cov1.getCsrpz(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.001, cov1.getCsrpxdot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.002, cov1.getCsrpydot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.003,  cov1.getCsrpzdot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.004,  cov1.getCsrpdrg(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.005,  cov1.getCsrpsrp(), COVARIANCE_PRECISION);
        
        Assertions.assertEquals(0.07,  cov1.getCthrx(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.08, cov1.getCthry(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.09,  cov1.getCthrz(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.001, cov1.getCthrxdot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.002, cov1.getCthrydot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.003,  cov1.getCthrzdot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.004,  cov1.getCthrdrg(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.005,  cov1.getCthrsrp(), COVARIANCE_PRECISION);
        Assertions.assertEquals(0.006,  cov1.getCthrthr(), COVARIANCE_PRECISION);
        

        // OBJECT 2 - Eigenvector covariance block
        Assertions.assertEquals(AltCovarianceType.CSIG3EIGVEC3, file.getMetadataObject2().getAltCovType().orElseThrow(), "ALT_COV_TYPE");
        Assertions.assertEquals("Object2 Covariance in the Sigma / eigenvector format",
                                file.getDataObject2().getSig3EigVec3CovarianceBlock().orElseThrow().getComments().getFirst());
        Assertions.assertEquals(12,  file.getDataObject2().getSig3EigVec3CovarianceBlock().orElseThrow().getCsig3eigvec3().length);
        for (int i=0; i<12; i++) {
            Assertions.assertEquals(i+1, file.getDataObject2().getSig3EigVec3CovarianceBlock().orElseThrow().getCsig3eigvec3()[i],
                                    COVARIANCE_DIAG_PRECISION);
        }



        // Additional covariance metadata OBJ 1
        AdditionalCovarianceMetadata addCov1 = file.getDataObject1().getAdditionalCovMetadataBlock().orElseThrow();
        Assertions.assertEquals(2.5,  addCov1.getDensityForecastUncertainty().orElseThrow(), 0.0);
        Assertions.assertEquals(0.5,  addCov1.getcScaleFactorMin().orElseThrow(), 0.0);
        Assertions.assertEquals(1.0,  addCov1.getcScaleFactor().orElseThrow(), 0.0);
        Assertions.assertEquals(1.5,  addCov1.getcScaleFactorMax().orElseThrow(), 0.0);
        Assertions.assertEquals("Data source of additional covariance metadata", 
                                addCov1.getScreeningDataSource().orElseThrow(), "SCREENING_DATA_SOURCE");
        Assertions.assertEquals(3,  addCov1.getDcpSensitivityVectorPosition().orElseThrow().length);
        Assertions.assertEquals(1.0,  addCov1.getDcpSensitivityVectorPosition().orElseThrow()[0], 0.0);
        Assertions.assertEquals(2.0,  addCov1.getDcpSensitivityVectorPosition().orElseThrow()[1], 0.0);
        Assertions.assertEquals(3.0,  addCov1.getDcpSensitivityVectorPosition().orElseThrow()[2], 0.0);
        Assertions.assertEquals(3,  addCov1.getDcpSensitivityVectorVelocity().orElseThrow().length);
        Assertions.assertEquals(0.1,  addCov1.getDcpSensitivityVectorVelocity().orElseThrow()[0], 0.0);
        Assertions.assertEquals(0.2,  addCov1.getDcpSensitivityVectorVelocity().orElseThrow()[1], 0.0);
        Assertions.assertEquals(0.3,  addCov1.getDcpSensitivityVectorVelocity().orElseThrow()[2], 0.0);


        // User defined parameters

        Assertions.assertEquals(1, file.getUserDefinedParameters().orElseThrow().getComments().size());
        Assertions.assertEquals("User Parameters", file.getUserDefinedParameters().orElseThrow().getComments().getFirst());
        Assertions.assertEquals(1, file.getUserDefinedParameters().orElseThrow().getParameters().size());
        Assertions.assertEquals("2020-01-29T13:30:00", file.getUserDefinedParameters().orElseThrow().getParameters().get("OBJ1_TIME_LASTOB_START"));


        // Check the rest of the file against any regressions.

        // Verify general data
        Assertions.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assertions.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assertions.assertEquals(2.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12,
                                                 TimeScalesFactory.getUTC()),
                                file.getHeader().getCreationDate());
        Assertions.assertEquals("JSPOC", file.getHeader().getOriginator());
        Assertions.assertEquals("201113719185-1", file.getHeader().getMessageId().orElseThrow());

        // OBJECT1
        // Check Relative Metadata Block
        Assertions.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, new TimeOffset(52, TimeOffset.SECOND, 618, TimeOffset.MILLISECOND),
                                                 TimeScalesFactory.getUTC()),
                                file.getRelativeMetadata().getTca());
        Assertions.assertEquals(715.0,  file.getRelativeMetadata().getMissDistance(),DISTANCE_PRECISION);

        // Check Metadata Block
        Assertions.assertEquals("OBJECT1",                    file.getMetadataObject1().getObject());
        Assertions.assertEquals("12345",                      file.getMetadataObject1().getObjectDesignator());
        Assertions.assertEquals("SATCAT",                     file.getMetadataObject1().getCatalogName());
        Assertions.assertEquals("SATELLITE A",                file.getMetadataObject1().getObjectName());
        Assertions.assertEquals("1997-030E",                  file.getMetadataObject1().getInternationalDes());
        Assertions.assertEquals("EPHEMERIS SATELLITE A",      file.getMetadataObject1().getEphemName());
        Assertions.assertEquals(CovarianceMethod.CALCULATED,  file.getMetadataObject1().getCovarianceMethod());
        Assertions.assertEquals(Maneuvrable.YES,              file.getMetadataObject1().getManeuverable());
        Assertions.assertEquals(CelestialBodyFrame.EME2000,   file.getMetadataObject1().getRefFrame().asCelestialBodyFrame().orElseThrow());
        Assertions.assertEquals("UTC",                        file.getMetadataObject1().getTimeSystem().name());

        // Check data block
        // State vector block
        Assertions.assertEquals(2570.097065e3,  file.getDataObject1().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        Assertions.assertEquals(2244.654904e3,  file.getDataObject1().getStateVectorBlock().getPositionVector().getY(), DISTANCE_PRECISION);
        Assertions.assertEquals(6281.497978e3,  file.getDataObject1().getStateVectorBlock().getPositionVector().getZ(), DISTANCE_PRECISION);
        Assertions.assertEquals(4.418769571e3,  file.getDataObject1().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
        Assertions.assertEquals(4.833547743e3,  file.getDataObject1().getStateVectorBlock().getVelocityVector().getY(), DERIVATION_PRECISION);
        Assertions.assertEquals(-3.526774282e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getZ(), DERIVATION_PRECISION);
        // Covariance Matrix block
        Assertions.assertEquals(4.142e1,  file.getDataObject1().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-8.579,   file.getDataObject1().getRTNCovarianceBlock().getCtr(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.533e3,  file.getDataObject1().getRTNCovarianceBlock().getCtt(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(1.336e1,  file.getDataObject1().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(7.098e1,  file.getDataObject1().getRTNCovarianceBlock().getCnn(), COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(2.520e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotr(), COVARIANCE_PRECISION);
        Assertions.assertEquals(-5.476,   file.getDataObject1().getRTNCovarianceBlock().getCrdott(), COVARIANCE_PRECISION);
        Assertions.assertEquals(8.626e-4, file.getDataObject1().getRTNCovarianceBlock().getCrdotn(), COVARIANCE_PRECISION);
        Assertions.assertEquals(5.744e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotrdot(), COVARIANCE_PRECISION);

        Assertions.assertEquals(-1.006e-2, file.getDataObject1().getRTNCovarianceBlock().getCtdotr(), COVARIANCE_PRECISION);
        Assertions.assertEquals(4.041e-3,  file.getDataObject1().getRTNCovarianceBlock().getCtdott(), COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.359e-3, file.getDataObject1().getRTNCovarianceBlock().getCtdotn(), COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.502e-5, file.getDataObject1().getRTNCovarianceBlock().getCtdotrdot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(1.049e-5,  file.getDataObject1().getRTNCovarianceBlock().getCtdottdot(), COVARIANCE_PRECISION);

        Assertions.assertEquals(1.053e-3,  file.getDataObject1().getRTNCovarianceBlock().getCndotr(), COVARIANCE_PRECISION);
        Assertions.assertEquals(-3.412e-3, file.getDataObject1().getRTNCovarianceBlock().getCndott(), COVARIANCE_PRECISION);
        Assertions.assertEquals(1.213e-2,  file.getDataObject1().getRTNCovarianceBlock().getCndotn(), COVARIANCE_PRECISION);
        Assertions.assertEquals(-3.004e-6, file.getDataObject1().getRTNCovarianceBlock().getCndotrdot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.091e-6, file.getDataObject1().getRTNCovarianceBlock().getCndottdot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(5.529e-5,  file.getDataObject1().getRTNCovarianceBlock().getCndotndot(), COVARIANCE_PRECISION);

        Assertions.assertEquals(2.520e-3,   file.getDataObject1().getRTNCovarianceBlock().
                                getRTNCovarianceMatrix().getEntry(3, 0), COVARIANCE_PRECISION);
        Assertions.assertEquals(Double.NaN, file.getDataObject1().getRTNCovarianceBlock().
                                getRTNCovarianceMatrix().getEntry(7, 6), COVARIANCE_PRECISION);

        // OBJECT2
        // Check Relative Metadata Block
        Assertions.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getTca(),
                                file.getRelativeMetadata().getTca());
        Assertions.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getMissDistance(),  
                                file.getRelativeMetadata().getMissDistance(),DISTANCE_PRECISION);

        // Check Metadata Block
        Assertions.assertEquals("OBJECT2",                    file.getMetadataObject2().getObject());
        Assertions.assertEquals("30337",                      file.getMetadataObject2().getObjectDesignator());
        Assertions.assertEquals("SATCAT",                     file.getMetadataObject2().getCatalogName());
        Assertions.assertEquals("FENGYUN 1C DEB",             file.getMetadataObject2().getObjectName());
        Assertions.assertEquals("1999-025AA",                 file.getMetadataObject2().getInternationalDes());
        Assertions.assertEquals("NONE",                       file.getMetadataObject2().getEphemName());
        Assertions.assertEquals(CovarianceMethod.CALCULATED,  file.getMetadataObject2().getCovarianceMethod());
        Assertions.assertEquals(Maneuvrable.NO,               file.getMetadataObject2().getManeuverable());
        Assertions.assertEquals(CelestialBodyFrame.EME2000,   file.getMetadataObject2().getRefFrame().asCelestialBodyFrame().orElseThrow());
        Assertions.assertEquals("UTC",                        file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        Assertions.assertEquals(2569.540800e3,  file.getDataObject2().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        Assertions.assertEquals(2245.093614e3,  file.getDataObject2().getStateVectorBlock().getPositionVector().getY(), DISTANCE_PRECISION);
        Assertions.assertEquals(6281.599946e3,  file.getDataObject2().getStateVectorBlock().getPositionVector().getZ(), DISTANCE_PRECISION);
        Assertions.assertEquals(-2.888612500e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
        Assertions.assertEquals(-6.007247516e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getY(), DERIVATION_PRECISION);
        Assertions.assertEquals(3.328770172e3,  file.getDataObject2().getStateVectorBlock().getVelocityVector().getZ(), DERIVATION_PRECISION);
        // Covariance Matrix block
        Assertions.assertEquals(1.337e3,   file.getDataObject2().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-4.806e4,  file.getDataObject2().getRTNCovarianceBlock().getCtr(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.492e6,   file.getDataObject2().getRTNCovarianceBlock().getCtt(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-3.298e1,  file.getDataObject2().getRTNCovarianceBlock().getCnr(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(7.105e1,   file.getDataObject2().getRTNCovarianceBlock().getCnn(), COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(2.591e-3,  file.getDataObject2().getRTNCovarianceBlock().getCrdotr(), COVARIANCE_PRECISION);
        Assertions.assertEquals(-4.152e-2, file.getDataObject2().getRTNCovarianceBlock().getCrdott(), COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.784e-6, file.getDataObject2().getRTNCovarianceBlock().getCrdotn(), COVARIANCE_PRECISION);
        Assertions.assertEquals(6.886e-5,  file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(), COVARIANCE_PRECISION);

        Assertions.assertEquals(-1.016e-2, file.getDataObject2().getRTNCovarianceBlock().getCtdotr(), COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(), COVARIANCE_PRECISION);
        Assertions.assertEquals(1.637e-3,  file.getDataObject2().getRTNCovarianceBlock().getCtdotn(), COVARIANCE_PRECISION);
        Assertions.assertEquals(-2.987e-6, file.getDataObject2().getRTNCovarianceBlock().getCtdotrdot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(1.059e-5,  file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(), COVARIANCE_PRECISION);

        Assertions.assertEquals(4.400e-3,  file.getDataObject2().getRTNCovarianceBlock().getCndotr(), COVARIANCE_PRECISION);
        Assertions.assertEquals(8.482e-3,  file.getDataObject2().getRTNCovarianceBlock().getCndott(), COVARIANCE_PRECISION);
        Assertions.assertEquals(8.633e-5,  file.getDataObject2().getRTNCovarianceBlock().getCndotn(), COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(-4.594e-6, file.getDataObject2().getRTNCovarianceBlock().getCndottdot(), COVARIANCE_PRECISION);
        Assertions.assertEquals(5.178e-5,  file.getDataObject2().getRTNCovarianceBlock().getCndotndot(), COVARIANCE_PRECISION);
        // Test in the matrix
        Assertions.assertEquals(1.337e3,   file.getDataObject2().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-4.806e4,  file.getDataObject2().getRTNCovarianceBlock().getCtr(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(2.492e6,   file.getDataObject2().getRTNCovarianceBlock().getCtt(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-3.298e1,  file.getDataObject2().getRTNCovarianceBlock().getCnr(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        Assertions.assertEquals(7.105e1,   file.getDataObject2().getRTNCovarianceBlock().getCnn(), COVARIANCE_DIAG_PRECISION);

        Assertions.assertEquals(2.591e-3,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 0), COVARIANCE_PRECISION);
        Assertions.assertEquals(-4.152e-2, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 1), COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.784e-6, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 2), COVARIANCE_PRECISION);
        Assertions.assertEquals(6.886e-5,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 3), COVARIANCE_PRECISION);

        Assertions.assertEquals(-1.016e-2, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 0), COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 1), COVARIANCE_PRECISION);
        Assertions.assertEquals(1.637e-3,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 2), COVARIANCE_PRECISION);
        Assertions.assertEquals(-2.987e-6, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 3), COVARIANCE_PRECISION);
        Assertions.assertEquals(1.059e-5,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 4), COVARIANCE_PRECISION);

        Assertions.assertEquals(4.400e-3,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 0), COVARIANCE_PRECISION);
        Assertions.assertEquals(8.482e-3,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 1), COVARIANCE_PRECISION);
        Assertions.assertEquals(8.633e-5,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 2), COVARIANCE_PRECISION);
        Assertions.assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 3), COVARIANCE_PRECISION);
        Assertions.assertEquals(-4.594e-6, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 4), COVARIANCE_PRECISION);
        Assertions.assertEquals(5.178e-5,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 5), COVARIANCE_PRECISION);

        // Improve coverage
        // ----------------

        Assertions.assertTrue(parser.inData()); // Always true by construction
        
        // AdditionalCovarianceMetadata conditions coverage
        addCov1.validate(1.0);
        addCov1.setDcpSensitivityVectorPosition(null);
        addCov1.setDcpSensitivityVectorVelocity(null);
        Assertions.assertFalse(addCov1.getDcpSensitivityVectorPosition().isPresent());
        Assertions.assertFalse(addCov1.getDcpSensitivityVectorVelocity().isPresent());
    }

    @Test
    public void test_issue_942_KVN() {

        // File
        final String ex = "/ccsds/cdm/CDMExample_issue942.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        // OBJECT1
        Assertions.assertEquals(Maneuvrable.N_A, file.getMetadataObject1().getManeuverable());    
        // OBJECT2
        Assertions.assertEquals(Maneuvrable.NO, file.getMetadataObject2().getManeuverable());
    }

    @Test
    public void test_issue_942_XML() {

        // File
        final String ex = "/ccsds/cdm/CDMExample_issue942.xml";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        // OBJECT1
        Assertions.assertEquals(Maneuvrable.N_A, file.getMetadataObject1().getManeuverable());    
        // OBJECT2
        Assertions.assertEquals(Maneuvrable.NO, file.getMetadataObject2().getManeuverable());
    }

    @Test
    public void test_issue_944() {

        // File
        final String ex = "/ccsds/cdm/CDMExample_issue_944.xml";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        // Check AREA_DRG
        Assertions.assertEquals(3, file.getDataObject1().getAdditionalParametersBlock().orElseThrow().getAreaDRG().orElseThrow(), 0.0);

        // Check AREA_SRP
        Assertions.assertEquals(10, file.getDataObject1().getAdditionalParametersBlock().orElseThrow().getAreaSRP().orElseThrow(), 0.0);
    }

    @Test
    public void test_issue_1319() {

        // File
        final String ex = "/ccsds/cdm/CDMExample_issue1319.xml";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        Assertions.assertFalse(file.getRelativeMetadata().getStartScreenPeriod().isPresent());
    }

    @Test
    public void test_issue_1319_throw_exception_when_mandatory_empty_values() {

        // File
        final String ex = "/ccsds/cdm/CDMExample_issue1319_mandatory_empty_value.xml";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        OrekitException exception = Assertions.assertThrows(OrekitException.class, () -> parser.parseMessage(source));
        Assertions.assertEquals("value for key X has not been initialized", exception.getMessage());
    }

    @Test
    public void test_issue_1458() {

        // File
        final String ex = "/ccsds/cdm/CDMExample_issue1458.xml";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        Cdm file = parser.parseMessage(source);

        // dummy assertion: the aim of the test is to show that empty unit can be parsed
        Assertions.assertFalse(file.getDataObject1().getODParametersBlock().isPresent());
    }

    /** Check that an invalid TCA throws an exception saying so. */
    @Test
    public void testInvalidTca() {
        // File
        final String ex = "/ccsds/cdm/CDMExample1InvalidTca.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        try {
            parser.parseMessage(source);
            Assertions.fail("an exception should have been thrown");
        } catch (Exception e) {
            MatcherAssert.assertThat(
                    e.getMessage(),
                    Matchers.containsString("non-existent date 2010-04-31"));
        }
    }

    /** Unit tests for parsing a CDM with a custom frame mapper. */
    @Test
    public void testFrameMapper() {
        // setup
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2003, false);
        Frame myItrf = new Frame(itrf, Transform.IDENTITY, "MyItrf");
        CcsdsFrameMapper mapper = new CcsdsFrameMapper() {
            @Override
            public Frame buildCcsdsFrame(FrameFacade orientation, AbsoluteDate epoch) {
                if ("ITRF".equals(orientation.getName()) && null == epoch) {
                    return myItrf;
                }
                throw new IllegalArgumentException(" " + orientation + " " + epoch);
            }

            @Override
            public Frame buildCcsdsFrame(BodyFacade center,
                                         FrameFacade orientation,
                                         AbsoluteDate frameEpoch) {
                if ("EARTH".equals(center.getName()) &&
                        "ITRF".equals(orientation.getName()) &&
                        null == frameEpoch) {
                    return myItrf;
                }
                throw new IllegalArgumentException(
                        center + " " + orientation + " " + frameEpoch);
            }
        };
        final CdmParser parser = new ParserBuilder().withFrameMapper(mapper).buildCdmParser();
        String name = "/ccsds/cdm/CDM-frame-mapper.txt";
        DataSource source =
                new DataSource(name, () -> getClass().getResourceAsStream(name));

        // action
        Cdm cdm = parser.parseMessage(source);

        // verify object reference frames (ignore screen volume frames, these are local orbital frames and
        // not using FrameFacade)
        MatcherAssert.assertThat(cdm.getMetadataObject1().getFrame(), Matchers.sameInstance(myItrf));
        MatcherAssert.assertThat(cdm.getMetadataObject2().getFrame(), Matchers.sameInstance(myItrf));
        // verify altcovariance reference frames
        MatcherAssert.assertThat(cdm.getMetadataObject1().getAltCovFrame().orElseThrow(),
                Matchers.sameInstance(myItrf));
        MatcherAssert.assertThat(
                cdm.getDataObject1().getAdditionalParametersBlock().orElseThrow().getOebParent(),
                Matchers.sameInstance(myItrf));
    }
}
