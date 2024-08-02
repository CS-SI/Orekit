/* Copyright 2002-2024 CS GROUP
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

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.PocMethodType;
import org.orekit.files.ccsds.definitions.YesNoUnknown;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.odm.ocm.ObjectType;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class CdmParserTest {

    private static final double DISTANCE_PRECISION = 1e-8;
    private static final double DOUBLE_PRECISION = 1e-8;
    private static final double DERIVATION_PRECISION = 1e-12;
    private static final double COVARIANCE_DIAG_PRECISION = 1e-10;
    private static final double COVARIANCE_PRECISION = 1e-8;

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testParseCDM1() {
        /** Test for CdmExample1.txt, with only required data. */
        // File
        final String ex = "/ccsds/cdm/CDMExample1.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        // Verify general data
        assertEquals(IERSConventions.IERS_2010, file.getConventions());
        assertEquals(DataContext.getDefault(), file.getDataContext());

        // Check Header Block
        assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12,
                                                 TimeScalesFactory.getUTC()), file.getHeader().getCreationDate());
        assertEquals("JSPOC", file.getHeader().getOriginator());
        assertEquals("201113719185", file.getHeader().getMessageId());

        // OBJECT1
        // Check Relative Metadata Block
        assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.618,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getTca());
        assertEquals(715.0, file.getRelativeMetadata().getMissDistance(), DISTANCE_PRECISION);

        // Check Metadata Block
        assertEquals("OBJECT1", file.getMetadataObject1().getObject());
        assertEquals("12345", file.getMetadataObject1().getObjectDesignator());
        assertEquals("SATCAT", file.getMetadataObject1().getCatalogName());
        assertEquals("SATELLITE A", file.getMetadataObject1().getObjectName());
        assertEquals("1997−030E", file.getMetadataObject1().getInternationalDes());
        assertEquals("EPHEMERIS SATELLITE A", file.getMetadataObject1().getEphemName());
        assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject1().getCovarianceMethod());
        assertEquals(Maneuvrable.YES, file.getMetadataObject1().getManeuverable());
        assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject1().getRefFrame().asCelestialBodyFrame());
        assertEquals("UTC", file.getMetadataObject1().getTimeSystem().name());

        // Check data block
        // State vector block
        assertEquals(2570.097065e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getX(),
                                DISTANCE_PRECISION);
        assertEquals(2244.654904e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getY(),
                                DISTANCE_PRECISION);
        assertEquals(6281.497978e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getZ(),
                                DISTANCE_PRECISION);
        assertEquals(4.418769571e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getX(),
                                DERIVATION_PRECISION);
        assertEquals(4.833547743e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getY(),
                                DERIVATION_PRECISION);
        assertEquals(-3.526774282e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getZ(),
                                DERIVATION_PRECISION);
        // Covariance Matrix block
        assertEquals(4.142e1, file.getDataObject1().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-8.579, file.getDataObject1().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.533e3, file.getDataObject1().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(1.336e1, file.getDataObject1().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(7.098e1, file.getDataObject1().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        assertEquals(2.520e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        assertEquals(-5.476, file.getDataObject1().getRTNCovarianceBlock().getCrdott(),
                                COVARIANCE_PRECISION);
        assertEquals(8.626e-4, file.getDataObject1().getRTNCovarianceBlock().getCrdotn(),
                                COVARIANCE_PRECISION);
        assertEquals(5.744e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        assertEquals(-1.006e-2, file.getDataObject1().getRTNCovarianceBlock().getCtdotr(),
                                COVARIANCE_PRECISION);
        assertEquals(4.041e-3, file.getDataObject1().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.359e-3, file.getDataObject1().getRTNCovarianceBlock().getCtdotn(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.502e-5, file.getDataObject1().getRTNCovarianceBlock().getCtdotrdot(),
                                COVARIANCE_PRECISION);
        assertEquals(1.049e-5, file.getDataObject1().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        assertEquals(1.053e-3, file.getDataObject1().getRTNCovarianceBlock().getCndotr(),
                                COVARIANCE_PRECISION);
        assertEquals(-3.412e-3, file.getDataObject1().getRTNCovarianceBlock().getCndott(),
                                COVARIANCE_PRECISION);
        assertEquals(1.213e-2, file.getDataObject1().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        assertEquals(-3.004e-6, file.getDataObject1().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.091e-6, file.getDataObject1().getRTNCovarianceBlock().getCndottdot(),
                                COVARIANCE_PRECISION);
        assertEquals(5.529e-5, file.getDataObject1().getRTNCovarianceBlock().getCndotndot(),
                                COVARIANCE_PRECISION);

        assertEquals(2.520e-3, file.getDataObject1().getRTNCovarianceBlock().
                                getRTNCovarianceMatrix().getEntry(3, 0), COVARIANCE_PRECISION);
        assertEquals(Double.NaN, file.getDataObject1().getRTNCovarianceBlock().
                                getRTNCovarianceMatrix().getEntry(7, 6), COVARIANCE_PRECISION);



        // OBJECT2
        // Check Relative Metadata Block
        assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getTca(),
                                file.getRelativeMetadata().getTca());
        assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getMissDistance(),
                                file.getRelativeMetadata().getMissDistance(), DISTANCE_PRECISION);

        // Check Metadata Block
        assertEquals("OBJECT2", file.getMetadataObject2().getObject());
        assertEquals("30337", file.getMetadataObject2().getObjectDesignator());
        assertEquals("SATCAT", file.getMetadataObject2().getCatalogName());
        assertEquals("FENGYUN 1C DEB", file.getMetadataObject2().getObjectName());
        assertEquals("1999-025AA", file.getMetadataObject2().getInternationalDes());
        assertEquals("NONE", file.getMetadataObject2().getEphemName());
        assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject2().getCovarianceMethod());
        assertEquals(Maneuvrable.NO, file.getMetadataObject2().getManeuverable());
        assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject2().getRefFrame().asCelestialBodyFrame());
        assertEquals("UTC", file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        assertEquals(2569.540800e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getX(),
                                DISTANCE_PRECISION);
        assertEquals(2245.093614e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getY(),
                                DISTANCE_PRECISION);
        assertEquals(6281.599946e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getZ(),
                                DISTANCE_PRECISION);
        assertEquals(-2.888612500e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(),
                                DERIVATION_PRECISION);
        assertEquals(-6.007247516e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getY(),
                                DERIVATION_PRECISION);
        assertEquals(3.328770172e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getZ(),
                                DERIVATION_PRECISION);
        // Covariance Matrix block
        assertEquals(1.337e3, file.getDataObject2().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-4.806e4, file.getDataObject2().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.492e6, file.getDataObject2().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-3.298e1, file.getDataObject2().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(7.105e1, file.getDataObject2().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        assertEquals(2.591e-3, file.getDataObject2().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        assertEquals(-4.152e-2, file.getDataObject2().getRTNCovarianceBlock().getCrdott(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.784e-6, file.getDataObject2().getRTNCovarianceBlock().getCrdotn(),
                                COVARIANCE_PRECISION);
        assertEquals(6.886e-5, file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        assertEquals(-1.016e-2, file.getDataObject2().getRTNCovarianceBlock().getCtdotr(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        assertEquals(1.637e-3, file.getDataObject2().getRTNCovarianceBlock().getCtdotn(),
                                COVARIANCE_PRECISION);
        assertEquals(-2.987e-6, file.getDataObject2().getRTNCovarianceBlock().getCtdotrdot(),
                                COVARIANCE_PRECISION);
        assertEquals(1.059e-5, file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        assertEquals(4.400e-3, file.getDataObject2().getRTNCovarianceBlock().getCndotr(),
                                COVARIANCE_PRECISION);
        assertEquals(8.482e-3, file.getDataObject2().getRTNCovarianceBlock().getCndott(),
                                COVARIANCE_PRECISION);
        assertEquals(8.633e-5, file.getDataObject2().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);
        assertEquals(-4.594e-6, file.getDataObject2().getRTNCovarianceBlock().getCndottdot(),
                                COVARIANCE_PRECISION);
        assertEquals(5.178e-5, file.getDataObject2().getRTNCovarianceBlock().getCndotndot(),
                                COVARIANCE_PRECISION);
        // Test in the matrix
        assertEquals(1.337e3, file.getDataObject2().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-4.806e4, file.getDataObject2().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.492e6, file.getDataObject2().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-3.298e1, file.getDataObject2().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(7.105e1, file.getDataObject2().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        assertEquals(2.591e-3,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 0),
                                COVARIANCE_PRECISION);
        assertEquals(-4.152e-2,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 1),
                                COVARIANCE_PRECISION);
        assertEquals(-1.784e-6,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 2),
                                COVARIANCE_PRECISION);
        assertEquals(6.886e-5,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 3),
                                COVARIANCE_PRECISION);

        assertEquals(-1.016e-2,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 0),
                                COVARIANCE_PRECISION);
        assertEquals(-1.506e-4,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 1),
                                COVARIANCE_PRECISION);
        assertEquals(1.637e-3,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 2),
                                COVARIANCE_PRECISION);
        assertEquals(-2.987e-6,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 3),
                                COVARIANCE_PRECISION);
        assertEquals(1.059e-5,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 4),
                                COVARIANCE_PRECISION);

        assertEquals(4.400e-3,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 0),
                                COVARIANCE_PRECISION);
        assertEquals(8.482e-3,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 1),
                                COVARIANCE_PRECISION);
        assertEquals(8.633e-5,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 2),
                                COVARIANCE_PRECISION);
        assertEquals(-1.903e-6,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 3),
                                COVARIANCE_PRECISION);
        assertEquals(-4.594e-6,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 4),
                                COVARIANCE_PRECISION);
        assertEquals(5.178e-5,
                                file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 5),
                                COVARIANCE_PRECISION);

    }

    @Test
    void testParseCDM2() {
        /** Test for CdmExample2.txt, with only required data. */
        // File
        final String ex = "/ccsds/cdm/CDMExample2.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        assertEquals(IERSConventions.IERS_2010, file.getConventions());
        assertEquals(DataContext.getDefault(), file.getDataContext());

        // Check Header Block
        assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12,
                                                 TimeScalesFactory.getUTC()), file.getHeader().getCreationDate());
        assertEquals("JSPOC", file.getHeader().getOriginator());
        assertEquals("201113719185", file.getHeader().getMessageId());

        // OBJECT1
        // Check Relative Metadata Block
        assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.618,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getTca());
        assertEquals(715.0, file.getRelativeMetadata().getMissDistance(), DISTANCE_PRECISION);
        assertEquals(14762.0, file.getRelativeMetadata().getRelativeSpeed(), DERIVATION_PRECISION);
        assertEquals(27.4, file.getRelativeMetadata().getRelativePosition().getX(), DISTANCE_PRECISION);
        assertEquals(-70.2, file.getRelativeMetadata().getRelativePosition().getY(), DISTANCE_PRECISION);
        assertEquals(711.8, file.getRelativeMetadata().getRelativePosition().getZ(), DISTANCE_PRECISION);
        assertEquals(-7.2, file.getRelativeMetadata().getRelativeVelocity().getX(), DERIVATION_PRECISION);
        assertEquals(-14692.0, file.getRelativeMetadata().getRelativeVelocity().getY(),
                                DERIVATION_PRECISION);
        assertEquals(-1437.2, file.getRelativeMetadata().getRelativeVelocity().getZ(), DERIVATION_PRECISION);
        assertEquals(new AbsoluteDate(2010, 3, 12, 18, 29, 32.212,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStartScreenPeriod());
        assertEquals(new AbsoluteDate(2010, 3, 15, 18, 29, 32.212,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStopScreenPeriod());
        assertEquals(ScreenVolumeFrame.RTN, file.getRelativeMetadata().getScreenVolumeFrame());
        assertEquals(ScreenVolumeShape.ELLIPSOID, file.getRelativeMetadata().getScreenVolumeShape());
        assertEquals(200, file.getRelativeMetadata().getScreenVolumeX(), 0);
        assertEquals(1000, file.getRelativeMetadata().getScreenVolumeY(), 0);
        assertEquals(1000, file.getRelativeMetadata().getScreenVolumeZ(), 0);
        assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.222,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenEntryTime());
        assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.824,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenExitTime());
        assertEquals(4.835E-05, file.getRelativeMetadata().getCollisionProbability(), 1e-30);
        assertEquals("FOSTER-1992", file.getRelativeMetadata().getCollisionProbaMethod().getName());
        assertEquals(PocMethodType.FOSTER_1992,
                                file.getRelativeMetadata().getCollisionProbaMethod().getType());

        // Check Metadata Block
        assertEquals("OBJECT1", file.getMetadataObject1().getObject());
        assertEquals(ObjectType.PAYLOAD, file.getMetadataObject1().getObjectType());
        assertEquals("OSA", file.getMetadataObject1().getOperatorContactPosition());
        assertEquals("EUMETSAT", file.getMetadataObject1().getOperatorOrganization());
        assertEquals("+49615130312", file.getMetadataObject1().getOperatorPhone());
        assertEquals("JOHN.DOE@SOMEWHERE.NET", file.getMetadataObject1().getOperatorEmail());
        assertEquals("EPHEMERIS SATELLITE A", file.getMetadataObject1().getEphemName());
        assertEquals(FramesFactory.getEME2000(), file.getMetadataObject1().getFrame());
        assertEquals("JACCHIA 70 DCA", file.getMetadataObject1().getAtmosphericModel());
        assertEquals("EGM-96", file.getMetadataObject1().getGravityModel());
        assertEquals(36, file.getMetadataObject1().getGravityDegree(), 0);
        assertEquals(36, file.getMetadataObject1().getGravityOrder(), 0);
        assertEquals("MOON", file.getMetadataObject1().getNBodyPerturbations().get(0).getName());
        assertEquals("SUN", file.getMetadataObject1().getNBodyPerturbations().get(1).getName());
        assertEquals("NO", file.getMetadataObject1().getSolarRadiationPressure().name());
        assertEquals("NO", file.getMetadataObject1().getEarthTides().name());
        assertEquals("NO", file.getMetadataObject1().getIntrackThrust().name());
        assertEquals("UTC", file.getMetadataObject1().getTimeSystem().name());

        // Check data block
        // OD parameters block
        assertEquals(new AbsoluteDate(2010, 3, 12, 02, 14, 12.746,
                                                 TimeScalesFactory.getUTC()),
                                file.getDataObject1().getODParametersBlock().getTimeLastObsStart());
        assertEquals(new AbsoluteDate(2010, 3, 12, 02, 14, 12.746,
                                                 TimeScalesFactory.getUTC()),
                                file.getDataObject1().getODParametersBlock().getTimeLastObsEnd());
        assertEquals(7.88*3600.0*24.0, file.getDataObject1().getODParametersBlock().getRecommendedOdSpan(),
                                DOUBLE_PRECISION);
        assertEquals(5.50*3600.0*24.0, file.getDataObject1().getODParametersBlock().getActualOdSpan(),
                                DOUBLE_PRECISION);
        assertEquals(592, file.getDataObject1().getODParametersBlock().getObsAvailable(), DOUBLE_PRECISION);
        assertEquals(579, file.getDataObject1().getODParametersBlock().getObsUsed(), DOUBLE_PRECISION);
        assertEquals(123, file.getDataObject1().getODParametersBlock().getTracksAvailable(),
                                DOUBLE_PRECISION);
        assertEquals(119, file.getDataObject1().getODParametersBlock().getTracksUsed(), DOUBLE_PRECISION);
        assertEquals(97.8/100.0, file.getDataObject1().getODParametersBlock().getResidualsAccepted(),
                                DOUBLE_PRECISION);
        assertEquals(0.864, file.getDataObject1().getODParametersBlock().getWeightedRMS(), DOUBLE_PRECISION);
        // Additional parameters block
        assertEquals(5.2, file.getDataObject1().getAdditionalParametersBlock().getAreaPC(),
                                DOUBLE_PRECISION);
        assertEquals(251.6, file.getDataObject1().getAdditionalParametersBlock().getMass(),
                                DOUBLE_PRECISION);
        assertEquals(0.045663, file.getDataObject1().getAdditionalParametersBlock().getCDAreaOverMass(),
                                DOUBLE_PRECISION);
        assertEquals(0.000000, file.getDataObject1().getAdditionalParametersBlock().getCRAreaOverMass(),
                                DOUBLE_PRECISION);
        assertEquals(0.0, file.getDataObject1().getAdditionalParametersBlock().getThrustAcceleration(),
                                DOUBLE_PRECISION);
        assertEquals(4.54570E-05, file.getDataObject1().getAdditionalParametersBlock().getSedr(),
                                DOUBLE_PRECISION);
        // State vector block
        assertEquals(2570.097065e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getX(),
                                DISTANCE_PRECISION);
        assertEquals(4.833547743e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getY(),
                                DERIVATION_PRECISION);
        // Covariance Matrix block
        assertEquals(4.142e1, file.getDataObject1().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-8.579, file.getDataObject1().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.533e3, file.getDataObject1().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(1.336e1, file.getDataObject1().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(7.098e1, file.getDataObject1().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        assertEquals(-1.862E+00, file.getDataObject1().getRTNCovarianceBlock().getCdrgr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(3.530E+00, file.getDataObject1().getRTNCovarianceBlock().getCdrgt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-3.100E-01, file.getDataObject1().getRTNCovarianceBlock().getCdrgn(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-1.214E-04, file.getDataObject1().getRTNCovarianceBlock().getCdrgrdot(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.580E-04, file.getDataObject1().getRTNCovarianceBlock().getCdrgtdot(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-6.467E-05, file.getDataObject1().getRTNCovarianceBlock().getCdrgndot(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(3.483E-06, file.getDataObject1().getRTNCovarianceBlock().getCdrgdrg(),
                                COVARIANCE_DIAG_PRECISION);

        assertEquals(-1.492E+02, file.getDataObject1().getRTNCovarianceBlock().getCsrpr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.044E+02, file.getDataObject1().getRTNCovarianceBlock().getCsrpt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-2.331E+01, file.getDataObject1().getRTNCovarianceBlock().getCsrpn(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-1.254E-03, file.getDataObject1().getRTNCovarianceBlock().getCsrprdot(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.013E-02, file.getDataObject1().getRTNCovarianceBlock().getCsrptdot(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-4.700E-03, file.getDataObject1().getRTNCovarianceBlock().getCsrpndot(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.210E-04, file.getDataObject1().getRTNCovarianceBlock().getCsrpdrg(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(1.593E-02, file.getDataObject1().getRTNCovarianceBlock().getCsrpsrp(),
                                COVARIANCE_DIAG_PRECISION);


        // OBJECT2
        // Check Relative Metadata Block
        assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getScreenVolumeX(),
                                file.getRelativeMetadata().getScreenVolumeX(), DISTANCE_PRECISION);
        assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativePosition().getY(),
                                file.getRelativeMetadata().getRelativePosition().getY(), DISTANCE_PRECISION);
        assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativeVelocity().getZ(),
                                file.getRelativeMetadata().getRelativeVelocity().getZ(), DERIVATION_PRECISION);
        assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbability(),
                                file.getRelativeMetadata().getCollisionProbability(), 1e-30);
        assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbaMethod(),
                                file.getRelativeMetadata().getCollisionProbaMethod());

        // Check Metadata Block
        assertEquals("OBJECT2", file.getMetadataObject2().getObject());
        assertEquals("30337", file.getMetadataObject2().getObjectDesignator());
        assertEquals("SATCAT", file.getMetadataObject2().getCatalogName());
        assertEquals("FENGYUN 1C DEB", file.getMetadataObject2().getObjectName());
        assertEquals("1999-025AA", file.getMetadataObject2().getInternationalDes());
        assertEquals("NONE", file.getMetadataObject2().getEphemName());
        assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject2().getCovarianceMethod());
        assertEquals(Maneuvrable.NO, file.getMetadataObject2().getManeuverable());
        assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject2().getRefFrame().asCelestialBodyFrame());
        assertEquals("UTC", file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        assertEquals(2569.540800e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getX(),
                                DISTANCE_PRECISION);
        assertEquals(-2.888612500e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(),
                                DERIVATION_PRECISION);
        // Covariance Matrix block
        assertEquals(1.337e3, file.getDataObject2().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);

        assertEquals(2.591e-3, file.getDataObject2().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        assertEquals(6.886e-5, file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        assertEquals(1.059e-5, file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        assertEquals(8.633e-5, file.getDataObject2().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);

        assertEquals(-5.117E-01, file.getDataObject2().getRTNCovarianceBlock().getCdrgr(),
                                COVARIANCE_PRECISION);
        assertEquals(1.319E+00, file.getDataObject2().getRTNCovarianceBlock().getCdrgt(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.903E-05, file.getDataObject2().getRTNCovarianceBlock().getCdrgndot(),
                                COVARIANCE_PRECISION);
        assertEquals(7.402E-05, file.getDataObject2().getRTNCovarianceBlock().getCdrgtdot(),
                                COVARIANCE_PRECISION);
        assertEquals(-3.297E+01, file.getDataObject2().getRTNCovarianceBlock().getCsrpr(),
                                COVARIANCE_PRECISION);
        assertEquals(8.164E+01, file.getDataObject2().getRTNCovarianceBlock().getCsrpt(),
                                COVARIANCE_PRECISION);

        // Verify comments
        assertEquals("[Relative Metadata/Data]", file.getRelativeMetadata().getComment().toString());
        assertEquals("[Object1 Metadata]", file.getMetadataObject1().getComments().toString());
        assertEquals("[Object2 Metadata]", file.getMetadataObject2().getComments().toString());
        assertEquals("[Object1 Data]", file.getDataObject1().getComments().toString());
        assertEquals("[Object1 OD Parameters]",
                                file.getDataObject1().getODParametersBlock().getComments().toString());
        assertEquals("[Object2 OD Parameters]",
                                file.getDataObject2().getODParametersBlock().getComments().toString());
        assertEquals(
                                "[Object1 Additional Parameters, Apogee Altitude=779 km, Perigee Altitude=765 km, Inclination=86.4 deg]",
                                file.getDataObject1().getAdditionalParametersBlock().getComments().toString());
        assertEquals(
                                "[Object2 Additional Parameters, Apogee Altitude=786 km, Perigee Altitude=414 km, Inclination=98.9 deg]",
                                file.getDataObject2().getAdditionalParametersBlock().getComments().toString());

    }

    @Test
    void testParseCDM3() {
        /** Test for CdmExample3.txt, with only required data. */
        // File
        final String ex = "/ccsds/cdm/CDMExample3.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);


        assertEquals(IERSConventions.IERS_2010, file.getConventions());
        assertEquals(DataContext.getDefault(), file.getDataContext());

        // Check Header Block
        assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        assertEquals(new AbsoluteDate(2012, 9, 12, 22, 31, 12,
                                                 TimeScalesFactory.getUTC()), file.getHeader().getCreationDate());
        assertEquals("SDC", file.getHeader().getOriginator());
        assertEquals("GALAXY 15", file.getHeader().getMessageFor());
        assertEquals("20120912223112", file.getHeader().getMessageId());

        // OBJECT1
        // Check Relative Metadata Block
        assertEquals(new AbsoluteDate(2012, 9, 13, 22, 37, 52.618,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getTca());
        assertEquals(104.92, file.getRelativeMetadata().getMissDistance(), DISTANCE_PRECISION);
        assertEquals(12093.52, file.getRelativeMetadata().getRelativeSpeed(), DERIVATION_PRECISION);
        assertEquals(30.6, file.getRelativeMetadata().getRelativePosition().getX(), DISTANCE_PRECISION);
        assertEquals(100.2, file.getRelativeMetadata().getRelativePosition().getY(), DISTANCE_PRECISION);
        assertEquals(5.7, file.getRelativeMetadata().getRelativePosition().getZ(), DISTANCE_PRECISION);
        assertEquals(-20.3, file.getRelativeMetadata().getRelativeVelocity().getX(), DERIVATION_PRECISION);
        assertEquals(-12000.0, file.getRelativeMetadata().getRelativeVelocity().getY(),
                                DERIVATION_PRECISION);
        assertEquals(-1500.9, file.getRelativeMetadata().getRelativeVelocity().getZ(), DERIVATION_PRECISION);
        assertEquals(new AbsoluteDate(2012, 9, 12, 18, 29, 32.212,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStartScreenPeriod());
        assertEquals(new AbsoluteDate(2012, 9, 15, 18, 29, 32.212,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStopScreenPeriod());
        assertEquals(ScreenVolumeFrame.RTN, file.getRelativeMetadata().getScreenVolumeFrame());
        assertEquals(ScreenVolumeShape.ELLIPSOID, file.getRelativeMetadata().getScreenVolumeShape());
        assertEquals(500, file.getRelativeMetadata().getScreenVolumeX(), 0);
        assertEquals(500, file.getRelativeMetadata().getScreenVolumeY(), 0);
        assertEquals(500, file.getRelativeMetadata().getScreenVolumeZ(), 0);
        assertEquals(new AbsoluteDate(2012, 9, 13, 20, 25, 43.222,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenEntryTime());
        assertEquals(new AbsoluteDate(2012, 9, 13, 23, 44, 29.324,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenExitTime());
        assertEquals(2.355e-03, file.getRelativeMetadata().getCollisionProbability(), 1e-30);
        assertEquals("ALFANO-2005", file.getRelativeMetadata().getCollisionProbaMethod().getName());
        assertEquals(PocMethodType.ALFANO_2005,
                                file.getRelativeMetadata().getCollisionProbaMethod().getType());

        // Check Metadata Block
        assertEquals("OBJECT1", file.getMetadataObject1().getObject());
        assertEquals("28884", file.getMetadataObject1().getObjectDesignator());
        assertEquals("SATCAT", file.getMetadataObject1().getCatalogName());
        assertEquals("GALAXY 15", file.getMetadataObject1().getObjectName());
        assertEquals("2005-041A", file.getMetadataObject1().getInternationalDes());
        assertEquals(ObjectType.PAYLOAD, file.getMetadataObject1().getObjectType());
        assertNull(file.getMetadataObject1().getOperatorContactPosition());
        assertEquals("INTELSAT", file.getMetadataObject1().getOperatorOrganization());
        assertEquals("GALAXY-15A-2012JAN-WMANEUVER23A", file.getMetadataObject1().getEphemName());
        assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject1().getCovarianceMethod());
        assertEquals(Maneuvrable.YES, file.getMetadataObject1().getManeuverable());
        assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject1().getRefFrame().asCelestialBodyFrame());
        assertEquals("UTC", file.getMetadataObject1().getTimeSystem().name());

        // Check data block
        // OD parameters block
        assertEquals(new AbsoluteDate(2012, 9, 6, 20, 25, 43.222,
                                                 TimeScalesFactory.getUTC()),
                                file.getDataObject1().getODParametersBlock().getTimeLastObsStart());
        assertEquals(new AbsoluteDate(2012, 9, 6, 20, 25, 43.222,
                                                 TimeScalesFactory.getUTC()),
                                file.getDataObject1().getODParametersBlock().getTimeLastObsEnd());
        // State vector block
        assertEquals(-41600.46272465e3,
                                file.getDataObject1().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        assertEquals(3626.912120064e3,
                                file.getDataObject1().getStateVectorBlock().getPositionVector().getY(), DISTANCE_PRECISION);
        assertEquals(6039.06350924e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getZ(),
                                DISTANCE_PRECISION);
        assertEquals(-0.306132852503e3,
                                file.getDataObject1().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
        assertEquals(-3.044998353334e3,
                                file.getDataObject1().getStateVectorBlock().getVelocityVector().getY(), DERIVATION_PRECISION);
        assertEquals(-0.287674310725e3,
                                file.getDataObject1().getStateVectorBlock().getVelocityVector().getZ(), DERIVATION_PRECISION);
        // Covariance Matrix block
        assertEquals(4.142e1, file.getDataObject1().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-8.579, file.getDataObject1().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.533e3, file.getDataObject1().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(1.336e1, file.getDataObject1().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(7.098e1, file.getDataObject1().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        assertEquals(2.520E-03, file.getDataObject1().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        assertEquals(-5.476E+00, file.getDataObject1().getRTNCovarianceBlock().getCrdott(),
                                COVARIANCE_PRECISION);
        assertEquals(8.626E-04, file.getDataObject1().getRTNCovarianceBlock().getCrdotn(),
                                COVARIANCE_PRECISION);
        assertEquals(5.744E-03, file.getDataObject1().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        assertEquals(-1.006E-02, file.getDataObject1().getRTNCovarianceBlock().getCtdotr(),
                                COVARIANCE_PRECISION);
        assertEquals(4.041E-03, file.getDataObject1().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.359E-03, file.getDataObject1().getRTNCovarianceBlock().getCtdotn(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.502E-05, file.getDataObject1().getRTNCovarianceBlock().getCtdotrdot(),
                                COVARIANCE_PRECISION);
        assertEquals(1.049E-05, file.getDataObject1().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        assertEquals(1.053E-03, file.getDataObject1().getRTNCovarianceBlock().getCndotr(),
                                COVARIANCE_PRECISION);
        assertEquals(-3.412E-03, file.getDataObject1().getRTNCovarianceBlock().getCndott(),
                                COVARIANCE_PRECISION);
        assertEquals(1.213E-02, file.getDataObject1().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        assertEquals(-3.004E-06, file.getDataObject1().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.091E-06, file.getDataObject1().getRTNCovarianceBlock().getCndottdot(),
                                COVARIANCE_PRECISION);
        assertEquals(5.529E-05, file.getDataObject1().getRTNCovarianceBlock().getCndotndot(),
                                COVARIANCE_PRECISION);

        // OBJECT2
        // Check Relative Metadata Block
        assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getScreenVolumeX(),
                                file.getRelativeMetadata().getScreenVolumeX(), DISTANCE_PRECISION);
        assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativePosition().getX(),
                                file.getRelativeMetadata().getRelativePosition().getX(), DISTANCE_PRECISION);
        assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativeVelocity().getZ(),
                                file.getRelativeMetadata().getRelativeVelocity().getZ(), DERIVATION_PRECISION);
        assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbability(),
                                file.getRelativeMetadata().getCollisionProbability(), 1e-30);
        assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbaMethod(),
                                file.getRelativeMetadata().getCollisionProbaMethod());

        // Check Metadata Block
        assertEquals("OBJECT2", file.getMetadataObject2().getObject());
        assertEquals("21139", file.getMetadataObject2().getObjectDesignator());
        assertEquals("SATCAT", file.getMetadataObject2().getCatalogName());
        assertEquals("ASTRA 1B", file.getMetadataObject2().getObjectName());
        assertEquals("1991-051A", file.getMetadataObject2().getInternationalDes());
        assertEquals(ObjectType.PAYLOAD, file.getMetadataObject2().getObjectType());
        assertEquals("NONE", file.getMetadataObject2().getEphemName());
        assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject2().getCovarianceMethod());
        assertEquals(Maneuvrable.YES, file.getMetadataObject2().getManeuverable());
        assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject2().getRefFrame().asCelestialBodyFrame());
        assertEquals("UTC", file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        assertEquals(-2956.02034826e3,
                                file.getDataObject2().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        assertEquals(-3.047096589536e3,
                                file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
        // Covariance Matrix block
        assertEquals(1.337e3, file.getDataObject2().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);

        assertEquals(2.591e-3, file.getDataObject2().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        assertEquals(6.886e-5, file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        assertEquals(1.059e-5, file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        assertEquals(8.633e-5, file.getDataObject2().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);

        // Verify comments
        assertEquals("[Relative Metadata/Data]",
                                file.getMetadataObject1().getRelativeMetadata().getComment().toString());
        assertEquals("[Object1 Metadata]", file.getMetadataObject1().getComments().toString());
        assertEquals("[Object2 Metadata]", file.getMetadataObject2().getComments().toString());
        assertEquals("[Object1 OD Parameters]",
                                file.getDataObject1().getODParametersBlock().getComments().toString());
        assertEquals("[Object1 Data]", file.getDataObject1().getComments().toString());
        assertEquals("[Object2 Data]", file.getDataObject2().getComments().toString());
        assertEquals("[Object2 OD Parameters]",
                                file.getDataObject2().getODParametersBlock().getComments().toString());
        assertEquals("[]", file.getDataObject1().getStateVectorBlock().getComments().toString());
        assertEquals("[Object1 Covariance in the RTN Coordinate Frame]",
                                file.getDataObject1().getRTNCovarianceBlock().getComments().toString());

    }

    @Test
    void testParseCDM4() {
        /** Test for CdmExample2.txt, with only required data. */
        // File
        final String ex = "/ccsds/cdm/CDMExample4.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        assertEquals(IERSConventions.IERS_2010, file.getConventions());
        assertEquals(DataContext.getDefault(), file.getDataContext());

        // Check Header Block
        assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12,
                                                 TimeScalesFactory.getUTC()), file.getHeader().getCreationDate());
        assertEquals("JSPOC", file.getHeader().getOriginator());
        assertEquals("201113719185", file.getHeader().getMessageId());

        // OBJECT1
        // Check Relative Metadata Block
        assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.618,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getTca());
        assertEquals(715.0, file.getRelativeMetadata().getMissDistance(), DISTANCE_PRECISION);

        assertEquals(new AbsoluteDate(2010, 3, 12, 18, 29, 32.212,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStartScreenPeriod());
        assertEquals(new AbsoluteDate(2010, 3, 15, 18, 29, 32.212,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStopScreenPeriod());
        assertEquals(ScreenVolumeFrame.RTN, file.getRelativeMetadata().getScreenVolumeFrame());
        assertEquals(ScreenVolumeShape.ELLIPSOID, file.getRelativeMetadata().getScreenVolumeShape());
        assertEquals(200, file.getRelativeMetadata().getScreenVolumeX(), 0);
        assertEquals(1000, file.getRelativeMetadata().getScreenVolumeY(), 0);
        assertEquals(1000, file.getRelativeMetadata().getScreenVolumeZ(), 0);
        assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.222,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenEntryTime());
        assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.824,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenExitTime());
        assertEquals(4.835E-05, file.getRelativeMetadata().getCollisionProbability(), 1e-30);
        assertEquals("FOSTER-1992", file.getRelativeMetadata().getCollisionProbaMethod().getName());
        assertEquals(PocMethodType.FOSTER_1992,
                                file.getRelativeMetadata().getCollisionProbaMethod().getType());

        // Check Metadata Block
        assertEquals("OBJECT1", file.getMetadataObject1().getObject());
        assertEquals(ObjectType.PAYLOAD, file.getMetadataObject1().getObjectType());
        assertEquals("OSA", file.getMetadataObject1().getOperatorContactPosition());
        assertEquals("EUMETSAT", file.getMetadataObject1().getOperatorOrganization());

        // Covariance Matrix block
        assertEquals(4.142e1, file.getDataObject1().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-8.579, file.getDataObject1().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.533e3, file.getDataObject1().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(1.336e1, file.getDataObject1().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(7.098e1, file.getDataObject1().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        assertEquals(-1.862E+00, file.getDataObject1().getRTNCovarianceBlock().getCdrgr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(3.530E+00, file.getDataObject1().getRTNCovarianceBlock().getCdrgt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-3.100E-01, file.getDataObject1().getRTNCovarianceBlock().getCdrgn(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-1.214E-04, file.getDataObject1().getRTNCovarianceBlock().getCdrgrdot(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.580E-04, file.getDataObject1().getRTNCovarianceBlock().getCdrgtdot(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-6.467E-05, file.getDataObject1().getRTNCovarianceBlock().getCdrgndot(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(3.483E-06, file.getDataObject1().getRTNCovarianceBlock().getCdrgdrg(),
                                COVARIANCE_DIAG_PRECISION);

        assertEquals(-1.492E+02, file.getDataObject1().getRTNCovarianceBlock().getCsrpr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.044E+02, file.getDataObject1().getRTNCovarianceBlock().getCsrpt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-2.331E+01, file.getDataObject1().getRTNCovarianceBlock().getCsrpn(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-1.254E-03, file.getDataObject1().getRTNCovarianceBlock().getCsrprdot(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.013E-02, file.getDataObject1().getRTNCovarianceBlock().getCsrptdot(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-4.700E-03, file.getDataObject1().getRTNCovarianceBlock().getCsrpndot(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.210E-04, file.getDataObject1().getRTNCovarianceBlock().getCsrpdrg(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(1.593E-02, file.getDataObject1().getRTNCovarianceBlock().getCsrpsrp(),
                                COVARIANCE_DIAG_PRECISION);

        assertEquals(-1.803E-06, file.getDataObject1().getRTNCovarianceBlock().getCthrr(),
                                COVARIANCE_PRECISION);
        assertEquals(3.803E-03, file.getDataObject1().getRTNCovarianceBlock().getCthrt(),
                                COVARIANCE_PRECISION);
        assertEquals(3.303E02, file.getDataObject1().getRTNCovarianceBlock().getCthrn(),
                                COVARIANCE_PRECISION);
        assertEquals(7.203E01, file.getDataObject1().getRTNCovarianceBlock().getCthrrdot(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.654E01, file.getDataObject1().getRTNCovarianceBlock().getCthrtdot(),
                                COVARIANCE_PRECISION);
        assertEquals(9.203E-01, file.getDataObject1().getRTNCovarianceBlock().getCthrndot(),
                                COVARIANCE_PRECISION);
        assertEquals(-3.876, file.getDataObject1().getRTNCovarianceBlock().getCthrdrg(),
                                COVARIANCE_PRECISION);
        assertEquals(6.876E03, file.getDataObject1().getRTNCovarianceBlock().getCthrsrp(),
                                COVARIANCE_PRECISION);
        assertEquals(-2.986E-02, file.getDataObject1().getRTNCovarianceBlock().getCthrthr(),
                                COVARIANCE_PRECISION);

        // OBJECT2
        // Check Relative Metadata Block
        assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getScreenVolumeX(),
                                file.getRelativeMetadata().getScreenVolumeX(), DISTANCE_PRECISION);
        assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativePosition().getY(),
                                file.getRelativeMetadata().getRelativePosition().getY(), DISTANCE_PRECISION);
        assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativeVelocity().getZ(),
                                file.getRelativeMetadata().getRelativeVelocity().getZ(), DERIVATION_PRECISION);
        assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbability(),
                                file.getRelativeMetadata().getCollisionProbability(), 1e-30);
        assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbaMethod(),
                                file.getRelativeMetadata().getCollisionProbaMethod());

        // Check Metadata Block
        assertEquals("OBJECT2", file.getMetadataObject2().getObject());
        assertEquals("30337", file.getMetadataObject2().getObjectDesignator());
        assertEquals("SATCAT", file.getMetadataObject2().getCatalogName());
        assertEquals("FENGYUN 1C DEB", file.getMetadataObject2().getObjectName());
        assertEquals("1999-025AA", file.getMetadataObject2().getInternationalDes());
        assertEquals("NONE", file.getMetadataObject2().getEphemName());
        assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject2().getCovarianceMethod());
        assertEquals(Maneuvrable.NO, file.getMetadataObject2().getManeuverable());
        assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject2().getRefFrame().asCelestialBodyFrame());
        assertEquals("UTC", file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        assertEquals(2569.540800e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getX(),
                                DISTANCE_PRECISION);
        assertEquals(-2.888612500e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(),
                                DERIVATION_PRECISION);
        // Covariance Matrix block
        assertEquals(1.337e3, file.getDataObject2().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);

        assertEquals(2.591e-3, file.getDataObject2().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        assertEquals(6.886e-5, file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        assertEquals(1.059e-5, file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        assertEquals(8.633e-5, file.getDataObject2().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);

        assertEquals(-5.117E-01, file.getDataObject2().getRTNCovarianceBlock().getCdrgr(),
                                COVARIANCE_PRECISION);
        assertEquals(1.319E+00, file.getDataObject2().getRTNCovarianceBlock().getCdrgt(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.903E-05, file.getDataObject2().getRTNCovarianceBlock().getCdrgndot(),
                                COVARIANCE_PRECISION);
        assertEquals(7.402E-05, file.getDataObject2().getRTNCovarianceBlock().getCdrgtdot(),
                                COVARIANCE_PRECISION);
        assertEquals(-3.297E+01, file.getDataObject2().getRTNCovarianceBlock().getCsrpr(),
                                COVARIANCE_PRECISION);
        assertEquals(8.164E+01, file.getDataObject2().getRTNCovarianceBlock().getCsrpt(),
                                COVARIANCE_PRECISION);

        // Verify comments
        assertEquals("[Relative Metadata/Data]", file.getRelativeMetadata().getComment().toString());
        assertEquals("[Object1 Metadata]", file.getMetadataObject1().getComments().toString());
        assertEquals("[Object2 Metadata]", file.getMetadataObject2().getComments().toString());
        assertEquals("[Object1 Data]", file.getDataObject1().getComments().toString());
        assertEquals("[Object1 OD Parameters]",
                                file.getDataObject1().getODParametersBlock().getComments().toString());
        assertEquals("[Object2 OD Parameters]",
                                file.getDataObject2().getODParametersBlock().getComments().toString());
        assertEquals(
                                "[Object1 Additional Parameters, Apogee Altitude=779 km, Perigee Altitude=765 km, Inclination=86.4 deg]",
                                file.getDataObject1().getAdditionalParametersBlock().getComments().toString());
        assertEquals(
                                "[Object2 Additional Parameters, Apogee Altitude=786 km, Perigee Altitude=414 km, Inclination=98.9 deg]",
                                file.getDataObject2().getAdditionalParametersBlock().getComments().toString());

    }


    @Test
    void testParseXML_CDM1() {
        /** Test for CdmExample1.xml, with only required data. */
        // File
        final String ex = "/ccsds/cdm/CDMExample1.xml";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        assertEquals(IERSConventions.IERS_2010, file.getConventions());
        assertEquals(DataContext.getDefault(), file.getDataContext());

        // Check Header Block
        assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12.000,
                                                 TimeScalesFactory.getUTC()), file.getHeader().getCreationDate());
        assertEquals("JSPOC", file.getHeader().getOriginator());
        assertEquals("SATELLITE A", file.getHeader().getMessageFor());
        assertEquals("20111371985", file.getHeader().getMessageId());

        // OBJECT1
        // Check Relative Metadata Block
        assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.618,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getTca());
        assertEquals(715.0, file.getRelativeMetadata().getMissDistance(), DISTANCE_PRECISION);
        assertEquals(14762.0, file.getRelativeMetadata().getRelativeSpeed(), DERIVATION_PRECISION);
        assertEquals(27.4, file.getRelativeMetadata().getRelativePosition().getX(), DISTANCE_PRECISION);
        assertEquals(-70.2, file.getRelativeMetadata().getRelativePosition().getY(), DISTANCE_PRECISION);
        assertEquals(711.8, file.getRelativeMetadata().getRelativePosition().getZ(), DISTANCE_PRECISION);
        assertEquals(-7.2, file.getRelativeMetadata().getRelativeVelocity().getX(), DERIVATION_PRECISION);
        assertEquals(-14692.0, file.getRelativeMetadata().getRelativeVelocity().getY(),
                                DERIVATION_PRECISION);
        assertEquals(-1437.2, file.getRelativeMetadata().getRelativeVelocity().getZ(),
                                DERIVATION_PRECISION);
        assertEquals(new AbsoluteDate(2010, 3, 12, 18, 29, 32.212,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStartScreenPeriod());
        assertEquals(new AbsoluteDate(2010, 3, 15, 18, 29, 32.212,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getStopScreenPeriod());
        assertEquals(ScreenVolumeFrame.RTN, file.getRelativeMetadata().getScreenVolumeFrame());
        assertEquals(ScreenVolumeShape.ELLIPSOID, file.getRelativeMetadata().getScreenVolumeShape());
        assertEquals(200, file.getRelativeMetadata().getScreenVolumeX(), 0);
        assertEquals(1000, file.getRelativeMetadata().getScreenVolumeY(), 0);
        assertEquals(1000, file.getRelativeMetadata().getScreenVolumeZ(), 0);
        assertEquals(new AbsoluteDate(2010, 3, 13, 20, 25, 43.222,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenEntryTime());
        assertEquals(new AbsoluteDate(2010, 3, 13, 23, 44, 29.324,
                                                 TimeScalesFactory.getUTC()), file.getRelativeMetadata().getScreenExitTime());
        assertEquals(4.835E-05, file.getRelativeMetadata().getCollisionProbability(), 1e-30);
        assertEquals("FOSTER-1992", file.getRelativeMetadata().getCollisionProbaMethod().getName());
        assertEquals(PocMethodType.FOSTER_1992,
                                file.getRelativeMetadata().getCollisionProbaMethod().getType());

        // Check Metadata Block
        assertEquals("OBJECT1", file.getMetadataObject1().getObject());
        assertEquals("12345", file.getMetadataObject1().getObjectDesignator());
        assertEquals("SATCAT", file.getMetadataObject1().getCatalogName());
        assertEquals("SATELLITE A", file.getMetadataObject1().getObjectName());
        assertEquals("1997-030E", file.getMetadataObject1().getInternationalDes());
        assertEquals("EPHEMERIS SATELLITE A", file.getMetadataObject1().getEphemName());
        assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject1().getCovarianceMethod());
        assertEquals(Maneuvrable.YES, file.getMetadataObject1().getManeuverable());
        assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject1().getRefFrame().asCelestialBodyFrame());
        assertEquals("UTC", file.getMetadataObject1().getTimeSystem().name());


        // Check data block
        // State vector block
        assertEquals(2570.097065e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getX(),
                                DISTANCE_PRECISION);
        assertEquals(2244.654904e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getY(),
                                DISTANCE_PRECISION);
        assertEquals(6281.497978e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getZ(),
                                DISTANCE_PRECISION);
        assertEquals(4.418769571e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getX(),
                                DERIVATION_PRECISION);
        assertEquals(4.833547743e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getY(),
                                DERIVATION_PRECISION);
        assertEquals(-3.526774282e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getZ(),
                                DERIVATION_PRECISION);
        // Covariance Matrix block
        assertEquals(4.142e1, file.getDataObject1().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-8.579, file.getDataObject1().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.533e3, file.getDataObject1().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(1.336e1, file.getDataObject1().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(7.098e1, file.getDataObject1().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        assertEquals(2.520e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        assertEquals(-5.476, file.getDataObject1().getRTNCovarianceBlock().getCrdott(),
                                COVARIANCE_PRECISION);
        assertEquals(8.626e-4, file.getDataObject1().getRTNCovarianceBlock().getCrdotn(),
                                COVARIANCE_PRECISION);
        assertEquals(5.744e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        assertEquals(-1.006e-2, file.getDataObject1().getRTNCovarianceBlock().getCtdotr(),
                                COVARIANCE_PRECISION);
        assertEquals(4.041e-3, file.getDataObject1().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.359e-3, file.getDataObject1().getRTNCovarianceBlock().getCtdotn(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.502e-5, file.getDataObject1().getRTNCovarianceBlock().getCtdotrdot(),
                                COVARIANCE_PRECISION);
        assertEquals(1.049e-5, file.getDataObject1().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        assertEquals(1.053e-3, file.getDataObject1().getRTNCovarianceBlock().getCndotr(),
                                COVARIANCE_PRECISION);
        assertEquals(-3.412e-3, file.getDataObject1().getRTNCovarianceBlock().getCndott(),
                                COVARIANCE_PRECISION);
        assertEquals(1.213e-2, file.getDataObject1().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        assertEquals(-3.004e-6, file.getDataObject1().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.091e-6, file.getDataObject1().getRTNCovarianceBlock().getCndottdot(),
                                COVARIANCE_PRECISION);
        assertEquals(5.529e-5, file.getDataObject1().getRTNCovarianceBlock().getCndotndot(),
                                COVARIANCE_PRECISION);

        // OBJECT2
        // Check Relative Metadata Block
        assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getScreenVolumeX(),
                                file.getRelativeMetadata().getScreenVolumeX(), DISTANCE_PRECISION);
        assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativePosition().getZ(),
                                file.getRelativeMetadata().getRelativePosition().getZ(), DISTANCE_PRECISION);
        assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativeVelocity().getZ(),
                                file.getRelativeMetadata().getRelativeVelocity().getZ(), DERIVATION_PRECISION);
        assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbability(),
                                file.getRelativeMetadata().getCollisionProbability(), 1e-30);
        assertEquals(
                                file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbaMethod(),
                                file.getRelativeMetadata().getCollisionProbaMethod());

        // Check Metadata Block
        assertEquals("OBJECT2", file.getMetadataObject2().getObject());
        assertEquals("30337", file.getMetadataObject2().getObjectDesignator());
        assertEquals("SATCAT", file.getMetadataObject2().getCatalogName());
        assertEquals("FENGYUN 1C DEB", file.getMetadataObject2().getObjectName());
        assertEquals("1999-025AA", file.getMetadataObject2().getInternationalDes());
        assertEquals("NONE", file.getMetadataObject2().getEphemName());
        assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject2().getCovarianceMethod());
        assertEquals(Maneuvrable.NO, file.getMetadataObject2().getManeuverable());
        assertEquals(CelestialBodyFrame.EME2000,
                                file.getMetadataObject2().getRefFrame().asCelestialBodyFrame());
        assertEquals("UTC", file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        assertEquals(2569.540800e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getX(),
                                DISTANCE_PRECISION);
        assertEquals(2245.093614e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getY(),
                                DISTANCE_PRECISION);
        assertEquals(6281.599946e3, file.getDataObject2().getStateVectorBlock().getPositionVector().getZ(),
                                DISTANCE_PRECISION);
        assertEquals(-2.888612500e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(),
                                DERIVATION_PRECISION);
        assertEquals(-6.007247516e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getY(),
                                DERIVATION_PRECISION);
        assertEquals(3.328770172e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getZ(),
                                DERIVATION_PRECISION);
        // Covariance Matrix block
        assertEquals(1.337e3, file.getDataObject2().getRTNCovarianceBlock().getCrr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-4.806e4, file.getDataObject2().getRTNCovarianceBlock().getCtr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(2.492e6, file.getDataObject2().getRTNCovarianceBlock().getCtt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-3.298e1, file.getDataObject2().getRTNCovarianceBlock().getCnr(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(),
                                COVARIANCE_DIAG_PRECISION);
        assertEquals(7.105e1, file.getDataObject2().getRTNCovarianceBlock().getCnn(),
                                COVARIANCE_DIAG_PRECISION);

        assertEquals(2.591e-3, file.getDataObject2().getRTNCovarianceBlock().getCrdotr(),
                                COVARIANCE_PRECISION);
        assertEquals(-4.152e-2, file.getDataObject2().getRTNCovarianceBlock().getCrdott(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.784e-6, file.getDataObject2().getRTNCovarianceBlock().getCrdotn(),
                                COVARIANCE_PRECISION);
        assertEquals(6.886e-5, file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(),
                                COVARIANCE_PRECISION);

        assertEquals(-1.016e-2, file.getDataObject2().getRTNCovarianceBlock().getCtdotr(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(),
                                COVARIANCE_PRECISION);
        assertEquals(1.637e-3, file.getDataObject2().getRTNCovarianceBlock().getCtdotn(),
                                COVARIANCE_PRECISION);
        assertEquals(-2.987e-6, file.getDataObject2().getRTNCovarianceBlock().getCtdotrdot(),
                                COVARIANCE_PRECISION);
        assertEquals(1.059e-5, file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(),
                                COVARIANCE_PRECISION);

        assertEquals(4.400e-3, file.getDataObject2().getRTNCovarianceBlock().getCndotr(),
                                COVARIANCE_PRECISION);
        assertEquals(8.482e-3, file.getDataObject2().getRTNCovarianceBlock().getCndott(),
                                COVARIANCE_PRECISION);
        assertEquals(8.633e-5, file.getDataObject2().getRTNCovarianceBlock().getCndotn(),
                                COVARIANCE_PRECISION);
        assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(),
                                COVARIANCE_PRECISION);
        assertEquals(-4.594e-6, file.getDataObject2().getRTNCovarianceBlock().getCndottdot(),
                                COVARIANCE_PRECISION);
        assertEquals(5.178e-5, file.getDataObject2().getRTNCovarianceBlock().getCndotndot(),
                                COVARIANCE_PRECISION);

        // Check relative metadata comments for Object1
        ArrayList<String> relativeMetadataComment = new ArrayList<String>();
        relativeMetadataComment.add("Relative Metadata/Data");
        assertEquals(relativeMetadataComment, file.getRelativeMetadata().getComment());

        // Check metadata comments for Object1
        ArrayList<String> MetadataComment = new ArrayList<String>();
        MetadataComment.add("Object1 Metadata");
        assertEquals(MetadataComment, file.getMetadataObject1().getComments());

        // Check data general comments and OD parameters comments for Object1
        ArrayList<String> generalComment = new ArrayList<String>();
        generalComment.add("Object1 Data");
        assertEquals(generalComment, file.getDataObject1().getComments());

        // Check additional parameters comments Object1
        ArrayList<String> addParametersComment = new ArrayList<String>();
        addParametersComment.add("Object 1 Additional Parameters");
        assertEquals(addParametersComment,
                                file.getDataObject1().getAdditionalParametersBlock().getComments());

        // Check state vector comments Object1
        ArrayList<String> stateVectorComment = new ArrayList<String>();
        stateVectorComment.add("Object1 State Vector");
        assertEquals(stateVectorComment, file.getDataObject1().getStateVectorBlock().getComments());

        // Check RTN covariance comments Object1
        ArrayList<String> RTNComment = new ArrayList<String>();
        RTNComment.add("Object1 Covariance in the RTN Coordinate Frame");
        assertEquals(RTNComment, file.getDataObject1().getRTNCovarianceBlock().getComments());


        // Check general comments Object2
        ArrayList<String> generalCommentObj2AddParam = new ArrayList<String>();
        generalCommentObj2AddParam.add("Object2 Additional Parameters");
        generalCommentObj2AddParam.add("Apogee Altitude=768 km, Perigee Altitude=414 km, Inclination=98.8 deg");
        assertEquals(generalCommentObj2AddParam.toString(),
                                file.getDataObject2().getAdditionalParametersBlock().getComments().toString());

    }

    @Test
    void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/cdm/CDMExample1.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        try {
            final DataSource source = new DataSource(wrongName, () -> getClass().getResourceAsStream(wrongName));
            new ParserBuilder().
            buildCdmParser().
            parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    void testMissingTCA() throws URISyntaxException {
        final String name = "/ccsds/cdm/CDM-missing-TCA.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            buildCdmParser().
            parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            assertEquals("TCA", oe.getParts()[0]);
        }
    }

    @Test
    void testMissingObj2StateVectorX() throws URISyntaxException {
        final String name = "/ccsds/cdm/CDM-missing-object2-state-vector.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            buildCdmParser().
            parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            assertEquals("X", oe.getParts()[0]);
        }
    }

    @Test
    void testMissingObj1CovarianceCNDOT_NDOT() throws URISyntaxException {
        final String name = "/ccsds/cdm/CDM-missing-object1-covariance-block.xml";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            buildCdmParser().
            parseMessage(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            assertEquals("CR_R", oe.getParts()[0]);
        }
    }

    /** Test that the Earth is returned by default when no orbit center were explicitly defined. */
    @Test
    void testMissingObj1OrbitCenterGetFrame() {

        // GIVEN
        final String ex = "/ccsds/cdm/CDM-no-orbit-center-defined-obj1.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // WHEN
        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        // WHEN
        final BodyFacade obj1OrbitCenter     = file.getMetadataObject1().getOrbitCenter();
        final BodyFacade expectedOrbitCenter = new BodyFacade(CelestialBodyFactory.EARTH.toUpperCase(),
                                                              CelestialBodyFactory.getEarth());

        assertEquals(expectedOrbitCenter.getName(), obj1OrbitCenter.getName());
        assertEquals(expectedOrbitCenter.getBody(), obj1OrbitCenter.getBody());

    }

    @Test
    void testCovarianceNumberFormatErrorType() {
        final String ex = "/ccsds/cdm/CDM-covariance-wrong-entry-format.txt";
        try {
            final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            new ParserBuilder().
            buildCdmParser().
            parseMessage(source);
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            assertEquals("CRDOT_T", oe.getParts()[0]);
            assertEquals(65, oe.getParts()[1]);
        }
    }

    @Test
    void test_issue_940() {

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
        assertEquals("\"Free text - for examples see CDM pink book (CCSDS 508.0-P-1.0.2)\"", 
                                file.getHeader().getClassification(),"CLASSIFICATION");



        // Test additional CDM Relative Metadata Keys



        // Check CONJUNCTION_ID is correctly read
        assertEquals("20220708T10hz SATELLITEA SATELLITEB", file.getRelativeMetadata().getConjunctionId(), "CONJUNCTION ID");

        // Check APPROACH_ANGLE is correctly read
        assertEquals(180.0, FastMath.toDegrees(file.getRelativeMetadata().getApproachAngle()), 0.0);

        // Check SCREEN_TYPE is correctly read
        assertEquals(ScreenType.SHAPE, file.getRelativeMetadata().getScreenType(), "SCREEN_TYPE");

        // Check SCREEN_VOLUME_SHAPE is correctly read
        assertEquals(ScreenVolumeShape.ELLIPSOID, file.getRelativeMetadata().getScreenVolumeShape(),"SCREEN_VOLUME_SHAPE");

        // Check SCREEN_VOLUME_X is correctly read
        assertEquals(200, file.getRelativeMetadata().getScreenVolumeX(), 0.0);

        // Check SCREEN_VOLUME_Y is correctly read
        assertEquals(1000, file.getRelativeMetadata().getScreenVolumeY(), 0.0);

        // Check SCREEN_VOLUME_Z is correctly read
        assertEquals(1000, file.getRelativeMetadata().getScreenVolumeZ(), 0.0);

        // Check SCREEN_ENTRY_TIME is correctly read
        assertEquals(new AbsoluteDate(2010, 3, 13, 20, 25, 43.222, TimeScalesFactory.getUTC()), 
                                file.getRelativeMetadata().getScreenEntryTime(), "SCREEN_ENTRY_TIME");

        // Check SCREEN_EXIT_TIME is correctly read
        assertEquals(new AbsoluteDate(2010, 3, 13, 23, 44, 29.324, TimeScalesFactory.getUTC()), 
                                file.getRelativeMetadata().getScreenExitTime(), "SCREEN_EXIT_TIME");

        // SCREEN_PC_THRESHOLD
        assertEquals(1.000E-03, file.getRelativeMetadata().getScreenPcThreshold(), 0.0);

        // COLLISION_PERCENTILE
        int[] collisionPercentile = {50, 51, 52};
        assertArrayEquals(collisionPercentile, file.getRelativeMetadata().getCollisionPercentile());

        // COLLISION_PROBABILITY
        assertEquals(4.835E-05, file.getRelativeMetadata().getCollisionProbability(), 0.0);

        // Check COLLISION_PROBABILITY_METHOD is correctly read
        assertEquals("FOSTER-1992", file.getRelativeMetadata().getCollisionProbaMethod().getName(), "COLLISION_PROBABILITY_METHOD");

        // COLLISION_MAX_PROBABILITY
        assertEquals(1.234E-05, file.getRelativeMetadata().getMaxCollisionProbability(), 0.0);

        // Check COLLISION_MAX_PC_METHOD is correctly read
        assertEquals("SCALE_COMBINED_COVAR", file.getRelativeMetadata().getMaxCollisionProbabilityMethod().getName(), "COLLISION_MAX_PC_METHOD");

        // SEFI_COLLISION_PROBABILITY
        assertEquals(1.234E-05, file.getRelativeMetadata().getSefiCollisionProbability(), 0.0);

        // Check SEFI_COLLISION_PROBABILITY_METHOD is correctly read
        assertEquals("SEFI_PC_METHOD", file.getRelativeMetadata().getSefiCollisionProbabilityMethod().getName(),"SEFI_COLLISION_PROBABILITY_METHOD");

        // Check SEFI_FRAGMENTATION_MODEL is correctly read
        assertEquals("NASA STD BREAKUP MODEL", file.getRelativeMetadata().getSefiFragmentationModel(), "SEFI_FRAGMENTATION_MODEL");

        // Check PREVIOUS_MESSAGE_ID is correctly read
        assertEquals("201113719185-0", file.getRelativeMetadata().getPreviousMessageId(), "PREVIOUS_MESSAGE_ID");

        // Check PREVIOUS_MESSAGE_EPOCH is correctly read
        assertEquals(new AbsoluteDate(2010, 3, 12, 10, 31, 12, TimeScalesFactory.getUTC()), 
                                file.getRelativeMetadata().getPreviousMessageEpoch(), "PREVIOUS_MESSAGE_EPOCH");

        // Check NEXT_MESSAGE_EPOCH is correctly read
        assertEquals(new AbsoluteDate(2010, 3, 13, 10, 31, 12, TimeScalesFactory.getUTC()), 
                                file.getRelativeMetadata().getNextMessageEpoch(), "NEXT_MESSAGE_EPOCH");



        // Tests additional CDM Metadata keys



        // Check ODM_MSG_LINK is correctly read
        assertEquals("ODM_MSG_35132.txt", file.getMetadataObject1().getOdmMsgLink(), "ODM_MSG_LINK");

        // Check ADM_MSG_LINK is correctly read
        assertEquals("ATT_MSG_35132.txt", file.getMetadataObject1().getAdmMsgLink(),"ADM_MSG_LINK");   

        // Check OBS_BEFORE_NEXT_MESSAGE is correctly read
        assertEquals(YesNoUnknown.YES, file.getMetadataObject1().getObsBeforeNextMessage(), "OBS_BEFORE_NEXT_MESSAGE");  

        // Check COVARIANCE_SOURCE is correctly read
        assertEquals("HAC Covariance", file.getMetadataObject1().getCovarianceSource(), "COVARIANCE_SOURCE");

        // Check ALT_COV_TYPE is correctly read
        assertEquals(AltCovarianceType.XYZ, file.getMetadataObject1().getAltCovType(), "ALT_COV_TYPE");

        // Check ALT_COV_REF_FRAME is correctly read
        assertEquals("EME2000", file.getMetadataObject1().getAltCovRefFrame().getName(), "ALT_COV_REF_FRAME");



        // CDM OD Parameters



        // Check OD_EPOCH is correctly read
        assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12, TimeScalesFactory.getUTC()), 
                                file.getDataObject1().getODParametersBlock().getOdEpoch(), "OD_EPOCH");



        // CDM Additional Parameters



        // Check AREA_PC_MIN is correctly read
        assertEquals(5.0, file.getDataObject1().getAdditionalParametersBlock().getAreaPCMin(), 0.0);

        // Check AREA_PC_MAX is correctly read
        assertEquals(5.4, file.getDataObject1().getAdditionalParametersBlock().getAreaPCMax(), 0.0);

        // Check OEB_PARENT_FRAME is correctly read
        assertEquals("OEB_YAW", file.getDataObject1().getAdditionalParametersBlock().getOebParentFrame().getName(), "OEB_PARENT_FRAME");

        // Check OEB_PARENT_FRAME_EPOCH is correctly read
        assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12, TimeScalesFactory.getUTC()), 
                                file.getDataObject1().getAdditionalParametersBlock().getOebParentFrameEpoch(), "OEB_PARENT_FRAME_EPOCH");

        // Check OEB_Q1 is correctly read
        assertEquals(0.03123, file.getDataObject1().getAdditionalParametersBlock().getOebQ().getQ1(), 0.0);  

        // Check OEB_Q2 is correctly read
        assertEquals(0.78543, file.getDataObject1().getAdditionalParametersBlock().getOebQ().getQ2(), 0.0);  	    

        // Check OEB_Q3 is correctly read
        assertEquals(0.39158, file.getDataObject1().getAdditionalParametersBlock().getOebQ().getQ3(), 0.0);  	    

        // Check OEB_QC is correctly read
        assertEquals(0.47832, file.getDataObject1().getAdditionalParametersBlock().getOebQ().getQ0(), 0.0); 

        // Check OEB_MAX is correctly read
        assertEquals(2.0, file.getDataObject1().getAdditionalParametersBlock().getOebMax(), 0.0); 

        // Check OEB_MED is correctly read
        assertEquals(1.0, file.getDataObject1().getAdditionalParametersBlock().getOebIntermediate(), 0.0); 

        // Check OEB_MIN is correctly read
        assertEquals(0.5, file.getDataObject1().getAdditionalParametersBlock().getOebMin(), 0.0); 

        // Check AREA_ALONG_OEB_MAX is correctly read
        assertEquals(0.15, file.getDataObject1().getAdditionalParametersBlock().getOebAreaAlongMax(), 0.0); 	    

        // Check AREA_ALONG_OEB_MED is correctly read
        assertEquals(0.3, file.getDataObject1().getAdditionalParametersBlock().getOebAreaAlongIntermediate(), 0.0); 

        // Check AREA_ALONG_OEB_MIN is correctly read
        assertEquals(0.5, file.getDataObject1().getAdditionalParametersBlock().getOebAreaAlongMin(), 0.0); 

        // Check RCS is correctly read
        assertEquals(2.4, file.getDataObject1().getAdditionalParametersBlock().getRcs(), 0.0);

        // Check RCS_MIN is correctly read
        assertEquals(1.4, file.getDataObject1().getAdditionalParametersBlock().getMinRcs(), 0.0);

        // Check RCS_MAX is correctly read
        assertEquals(3.4, file.getDataObject1().getAdditionalParametersBlock().getMaxRcs(), 0.0);

        // Check VM_ABSOLUTE is correctly read
        assertEquals(15.0, file.getDataObject1().getAdditionalParametersBlock().getVmAbsolute(), 0.0);

        // Check VM_APPARENT_MIN is correctly read
        assertEquals(19.0, file.getDataObject1().getAdditionalParametersBlock().getVmApparentMin(), 0.0);

        // Check VM_APPARENT is correctly read
        assertEquals(15.4, file.getDataObject1().getAdditionalParametersBlock().getVmApparent(), 0.0);

        // Check VM_APPARENT_MAX is correctly read
        assertEquals(14.0, file.getDataObject1().getAdditionalParametersBlock().getVmApparentMax(), 0.0);

        // Check REFLECTANCE is correctly read
        assertEquals(0.7, file.getDataObject1().getAdditionalParametersBlock().getReflectance(), 0.0);

        // Check HBR is correctly read
        assertEquals(2.5, file.getDataObject1().getAdditionalParametersBlock().getHbr(), 0.0); 	    

        // Check APOAPSIS_HEIGHT is correctly read
        assertEquals(800000, file.getDataObject1().getAdditionalParametersBlock().getApoapsisAltitude(), 0.0); 	

        // Check PERIAPSIS_HEIGHT is correctly read
        assertEquals(750000, file.getDataObject1().getAdditionalParametersBlock().getPeriapsisAltitude(), 0.0);

        // Check INCLINATION is correctly read
        assertEquals(FastMath.toRadians(89.0), file.getDataObject1().getAdditionalParametersBlock().getInclination(), 0.0);    

        // Check COV_CONFIDENCE is correctly read
        assertEquals(1.0, file.getDataObject1().getAdditionalParametersBlock().getCovConfidence(), 0.0);

        // Check COV_CONFIDENCE_METHOD is correctly read
        assertEquals("Wald test", file.getDataObject1().getAdditionalParametersBlock().getCovConfidenceMethod(), "COV_CONFIDENCE_METHOD");


        // XYZ Covariance Matrix block	    
        assertEquals(0.1,  file.getDataObject1().getXYZCovarianceBlock().getCxx(), COVARIANCE_DIAG_PRECISION);
        assertEquals(0.2,   file.getDataObject1().getXYZCovarianceBlock().getCyx(), COVARIANCE_DIAG_PRECISION);
        assertEquals(0.3,  file.getDataObject1().getXYZCovarianceBlock().getCyy(), COVARIANCE_DIAG_PRECISION);
        assertEquals(0.4, file.getDataObject1().getXYZCovarianceBlock().getCzx(), COVARIANCE_DIAG_PRECISION);
        assertEquals(0.5,  file.getDataObject1().getXYZCovarianceBlock().getCzy(), COVARIANCE_DIAG_PRECISION);
        assertEquals(0.6,  file.getDataObject1().getXYZCovarianceBlock().getCzz(), COVARIANCE_DIAG_PRECISION);

        assertEquals(0.7, file.getDataObject1().getXYZCovarianceBlock().getCxdotx(), COVARIANCE_PRECISION);
        assertEquals(0.8,   file.getDataObject1().getXYZCovarianceBlock().getCxdoty(), COVARIANCE_PRECISION);
        assertEquals(0.9, file.getDataObject1().getXYZCovarianceBlock().getCxdotz(), COVARIANCE_PRECISION);
        assertEquals(0.01, file.getDataObject1().getXYZCovarianceBlock().getCxdotxdot(), COVARIANCE_PRECISION);

        assertEquals(0.02, file.getDataObject1().getXYZCovarianceBlock().getCydotx(), COVARIANCE_PRECISION);
        assertEquals(0.03,  file.getDataObject1().getXYZCovarianceBlock().getCydoty(), COVARIANCE_PRECISION);
        assertEquals(0.04, file.getDataObject1().getXYZCovarianceBlock().getCydotz(), COVARIANCE_PRECISION);
        assertEquals(0.05, file.getDataObject1().getXYZCovarianceBlock().getCydotxdot(), COVARIANCE_PRECISION);
        assertEquals(0.06,  file.getDataObject1().getXYZCovarianceBlock().getCydotydot(), COVARIANCE_PRECISION);

        assertEquals(0.07,  file.getDataObject1().getXYZCovarianceBlock().getCzdotx(), COVARIANCE_PRECISION);
        assertEquals(0.08, file.getDataObject1().getXYZCovarianceBlock().getCzdoty(), COVARIANCE_PRECISION);
        assertEquals(0.09,  file.getDataObject1().getXYZCovarianceBlock().getCzdotz(), COVARIANCE_PRECISION);
        assertEquals(0.001, file.getDataObject1().getXYZCovarianceBlock().getCzdotxdot(), COVARIANCE_PRECISION);
        assertEquals(0.002, file.getDataObject1().getXYZCovarianceBlock().getCzdotydot(), COVARIANCE_PRECISION);
        assertEquals(0.003,  file.getDataObject1().getXYZCovarianceBlock().getCzdotzdot(), COVARIANCE_PRECISION);
        
        assertEquals(0.07,  file.getDataObject1().getXYZCovarianceBlock().getCdrgx(), COVARIANCE_PRECISION);
        assertEquals(0.08, file.getDataObject1().getXYZCovarianceBlock().getCdrgy(), COVARIANCE_PRECISION);
        assertEquals(0.09,  file.getDataObject1().getXYZCovarianceBlock().getCdrgz(), COVARIANCE_PRECISION);
        assertEquals(0.001, file.getDataObject1().getXYZCovarianceBlock().getCdrgxdot(), COVARIANCE_PRECISION);
        assertEquals(0.002, file.getDataObject1().getXYZCovarianceBlock().getCdrgydot(), COVARIANCE_PRECISION);
        assertEquals(0.003,  file.getDataObject1().getXYZCovarianceBlock().getCdrgzdot(), COVARIANCE_PRECISION);
        assertEquals(0.004,  file.getDataObject1().getXYZCovarianceBlock().getCdrgdrg(), COVARIANCE_PRECISION);
        
        assertEquals(0.07,  file.getDataObject1().getXYZCovarianceBlock().getCsrpx(), COVARIANCE_PRECISION);
        assertEquals(0.08, file.getDataObject1().getXYZCovarianceBlock().getCsrpy(), COVARIANCE_PRECISION);
        assertEquals(0.09,  file.getDataObject1().getXYZCovarianceBlock().getCsrpz(), COVARIANCE_PRECISION);
        assertEquals(0.001, file.getDataObject1().getXYZCovarianceBlock().getCsrpxdot(), COVARIANCE_PRECISION);
        assertEquals(0.002, file.getDataObject1().getXYZCovarianceBlock().getCsrpydot(), COVARIANCE_PRECISION);
        assertEquals(0.003,  file.getDataObject1().getXYZCovarianceBlock().getCsrpzdot(), COVARIANCE_PRECISION);
        assertEquals(0.004,  file.getDataObject1().getXYZCovarianceBlock().getCsrpdrg(), COVARIANCE_PRECISION);
        assertEquals(0.005,  file.getDataObject1().getXYZCovarianceBlock().getCsrpsrp(), COVARIANCE_PRECISION);
        
        assertEquals(0.07,  file.getDataObject1().getXYZCovarianceBlock().getCthrx(), COVARIANCE_PRECISION);
        assertEquals(0.08, file.getDataObject1().getXYZCovarianceBlock().getCthry(), COVARIANCE_PRECISION);
        assertEquals(0.09,  file.getDataObject1().getXYZCovarianceBlock().getCthrz(), COVARIANCE_PRECISION);
        assertEquals(0.001, file.getDataObject1().getXYZCovarianceBlock().getCthrxdot(), COVARIANCE_PRECISION);
        assertEquals(0.002, file.getDataObject1().getXYZCovarianceBlock().getCthrydot(), COVARIANCE_PRECISION);
        assertEquals(0.003,  file.getDataObject1().getXYZCovarianceBlock().getCthrzdot(), COVARIANCE_PRECISION);
        assertEquals(0.004,  file.getDataObject1().getXYZCovarianceBlock().getCthrdrg(), COVARIANCE_PRECISION);
        assertEquals(0.005,  file.getDataObject1().getXYZCovarianceBlock().getCthrsrp(), COVARIANCE_PRECISION);
        assertEquals(0.006,  file.getDataObject1().getXYZCovarianceBlock().getCthrthr(), COVARIANCE_PRECISION);
        

        // OBJECT 2 - Eigenvector covariance block
        assertEquals(AltCovarianceType.CSIG3EIGVEC3, file.getMetadataObject2().getAltCovType(), "ALT_COV_TYPE");
        assertEquals("Object2 Covariance in the Sigma / eigenvector format",  file.getDataObject2().getSig3EigVec3CovarianceBlock().getComments().get(0));
        assertEquals(12,  file.getDataObject2().getSig3EigVec3CovarianceBlock().getCsig3eigvec3().length);
        for (int i=0; i<12; i++) {
            assertEquals(i+1, file.getDataObject2().getSig3EigVec3CovarianceBlock().getCsig3eigvec3()[i], COVARIANCE_DIAG_PRECISION);
        }



        // Additional covariance metadata OBJ 1

        assertEquals(2.5,  file.getDataObject1().getAdditionalCovMetadataBlock().getDensityForecastUncertainty(), 0.0);
        assertEquals(0.5,  file.getDataObject1().getAdditionalCovMetadataBlock().getcScaleFactorMin(), 0.0);
        assertEquals(1.0,  file.getDataObject1().getAdditionalCovMetadataBlock().getcScaleFactor(), 0.0);
        assertEquals(1.5,  file.getDataObject1().getAdditionalCovMetadataBlock().getcScaleFactorMax(), 0.0);
        assertEquals("Data source of additional covariance metadata", 
                                file.getDataObject1().getAdditionalCovMetadataBlock().getScreeningDataSource(), "SCREENING_DATA_SOURCE");
        assertEquals(3,  file.getDataObject1().getAdditionalCovMetadataBlock().getDcpSensitivityVectorPosition().length);
        assertEquals(1.0,  file.getDataObject1().getAdditionalCovMetadataBlock().getDcpSensitivityVectorPosition()[0], 0.0);
        assertEquals(2.0,  file.getDataObject1().getAdditionalCovMetadataBlock().getDcpSensitivityVectorPosition()[1], 0.0);
        assertEquals(3.0,  file.getDataObject1().getAdditionalCovMetadataBlock().getDcpSensitivityVectorPosition()[2], 0.0);
        assertEquals(3,  file.getDataObject1().getAdditionalCovMetadataBlock().getDcpSensitivityVectorVelocity().length);
        assertEquals(0.1,  file.getDataObject1().getAdditionalCovMetadataBlock().getDcpSensitivityVectorVelocity()[0], 0.0);
        assertEquals(0.2,  file.getDataObject1().getAdditionalCovMetadataBlock().getDcpSensitivityVectorVelocity()[1], 0.0);
        assertEquals(0.3,  file.getDataObject1().getAdditionalCovMetadataBlock().getDcpSensitivityVectorVelocity()[2], 0.0);


        // User defined parameters

        assertEquals(1, file.getUserDefinedParameters().getComments().size());
        assertEquals("User Parameters", file.getUserDefinedParameters().getComments().get(0));
        assertEquals(1, file.getUserDefinedParameters().getParameters().size());
        assertEquals("2020-01-29T13:30:00", file.getUserDefinedParameters().getParameters().get("OBJ1_TIME_LASTOB_START"));


        // Check the rest of the file against any regressions.

        // Verify general data
        assertEquals(IERSConventions.IERS_2010, file.getConventions());
        assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        assertEquals(2.0, file.getHeader().getFormatVersion(), 1.0e-10);
        assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12,
                                                 TimeScalesFactory.getUTC()),
                                file.getHeader().getCreationDate());
        assertEquals("JSPOC", file.getHeader().getOriginator());
        assertEquals("201113719185-1", file.getHeader().getMessageId());

        // OBJECT1
        // Check Relative Metadata Block
        assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.618,
                                                 TimeScalesFactory.getUTC()),
                                file.getRelativeMetadata().getTca());
        assertEquals(715.0,  file.getRelativeMetadata().getMissDistance(),DISTANCE_PRECISION);

        // Check Metadata Block
        assertEquals("OBJECT1",                    file.getMetadataObject1().getObject());
        assertEquals("12345",                      file.getMetadataObject1().getObjectDesignator());
        assertEquals("SATCAT",                     file.getMetadataObject1().getCatalogName());
        assertEquals("SATELLITE A",                file.getMetadataObject1().getObjectName());
        assertEquals("1997-030E",                  file.getMetadataObject1().getInternationalDes());
        assertEquals("EPHEMERIS SATELLITE A",      file.getMetadataObject1().getEphemName());
        assertEquals(CovarianceMethod.CALCULATED,  file.getMetadataObject1().getCovarianceMethod());
        assertEquals(Maneuvrable.YES,              file.getMetadataObject1().getManeuverable());
        assertEquals(CelestialBodyFrame.EME2000,   file.getMetadataObject1().getRefFrame().asCelestialBodyFrame());
        assertEquals("UTC",                        file.getMetadataObject1().getTimeSystem().name());

        // Check data block
        // State vector block
        assertEquals(2570.097065e3,  file.getDataObject1().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        assertEquals(2244.654904e3,  file.getDataObject1().getStateVectorBlock().getPositionVector().getY(), DISTANCE_PRECISION);
        assertEquals(6281.497978e3,  file.getDataObject1().getStateVectorBlock().getPositionVector().getZ(), DISTANCE_PRECISION);
        assertEquals(4.418769571e3,  file.getDataObject1().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
        assertEquals(4.833547743e3,  file.getDataObject1().getStateVectorBlock().getVelocityVector().getY(), DERIVATION_PRECISION);
        assertEquals(-3.526774282e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getZ(), DERIVATION_PRECISION);
        // Covariance Matrix block
        assertEquals(4.142e1,  file.getDataObject1().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        assertEquals(-8.579,   file.getDataObject1().getRTNCovarianceBlock().getCtr(), COVARIANCE_DIAG_PRECISION);
        assertEquals(2.533e3,  file.getDataObject1().getRTNCovarianceBlock().getCtt(), COVARIANCE_DIAG_PRECISION);
        assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(), COVARIANCE_DIAG_PRECISION);
        assertEquals(1.336e1,  file.getDataObject1().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        assertEquals(7.098e1,  file.getDataObject1().getRTNCovarianceBlock().getCnn(), COVARIANCE_DIAG_PRECISION);

        assertEquals(2.520e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotr(), COVARIANCE_PRECISION);
        assertEquals(-5.476,   file.getDataObject1().getRTNCovarianceBlock().getCrdott(), COVARIANCE_PRECISION);
        assertEquals(8.626e-4, file.getDataObject1().getRTNCovarianceBlock().getCrdotn(), COVARIANCE_PRECISION);
        assertEquals(5.744e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotrdot(), COVARIANCE_PRECISION);

        assertEquals(-1.006e-2, file.getDataObject1().getRTNCovarianceBlock().getCtdotr(), COVARIANCE_PRECISION);
        assertEquals(4.041e-3,  file.getDataObject1().getRTNCovarianceBlock().getCtdott(), COVARIANCE_PRECISION);
        assertEquals(-1.359e-3, file.getDataObject1().getRTNCovarianceBlock().getCtdotn(), COVARIANCE_PRECISION);
        assertEquals(-1.502e-5, file.getDataObject1().getRTNCovarianceBlock().getCtdotrdot(), COVARIANCE_PRECISION);
        assertEquals(1.049e-5,  file.getDataObject1().getRTNCovarianceBlock().getCtdottdot(), COVARIANCE_PRECISION);

        assertEquals(1.053e-3,  file.getDataObject1().getRTNCovarianceBlock().getCndotr(), COVARIANCE_PRECISION);
        assertEquals(-3.412e-3, file.getDataObject1().getRTNCovarianceBlock().getCndott(), COVARIANCE_PRECISION);
        assertEquals(1.213e-2,  file.getDataObject1().getRTNCovarianceBlock().getCndotn(), COVARIANCE_PRECISION);
        assertEquals(-3.004e-6, file.getDataObject1().getRTNCovarianceBlock().getCndotrdot(), COVARIANCE_PRECISION);
        assertEquals(-1.091e-6, file.getDataObject1().getRTNCovarianceBlock().getCndottdot(), COVARIANCE_PRECISION);
        assertEquals(5.529e-5,  file.getDataObject1().getRTNCovarianceBlock().getCndotndot(), COVARIANCE_PRECISION);

        assertEquals(2.520e-3,   file.getDataObject1().getRTNCovarianceBlock().
                                getRTNCovarianceMatrix().getEntry(3, 0), COVARIANCE_PRECISION);
        assertEquals(Double.NaN, file.getDataObject1().getRTNCovarianceBlock().
                                getRTNCovarianceMatrix().getEntry(7, 6), COVARIANCE_PRECISION);

        // OBJECT2
        // Check Relative Metadata Block
        assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getTca(),
                                file.getRelativeMetadata().getTca());
        assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getMissDistance(),  
                                file.getRelativeMetadata().getMissDistance(),DISTANCE_PRECISION);

        // Check Metadata Block
        assertEquals("OBJECT2",                    file.getMetadataObject2().getObject());
        assertEquals("30337",                      file.getMetadataObject2().getObjectDesignator());
        assertEquals("SATCAT",                     file.getMetadataObject2().getCatalogName());
        assertEquals("FENGYUN 1C DEB",             file.getMetadataObject2().getObjectName());
        assertEquals("1999-025AA",                 file.getMetadataObject2().getInternationalDes());
        assertEquals("NONE",                       file.getMetadataObject2().getEphemName());
        assertEquals(CovarianceMethod.CALCULATED,  file.getMetadataObject2().getCovarianceMethod());
        assertEquals(Maneuvrable.NO,               file.getMetadataObject2().getManeuverable());
        assertEquals(CelestialBodyFrame.EME2000,   file.getMetadataObject2().getRefFrame().asCelestialBodyFrame());
        assertEquals("UTC",                        file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        assertEquals(2569.540800e3,  file.getDataObject2().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        assertEquals(2245.093614e3,  file.getDataObject2().getStateVectorBlock().getPositionVector().getY(), DISTANCE_PRECISION);
        assertEquals(6281.599946e3,  file.getDataObject2().getStateVectorBlock().getPositionVector().getZ(), DISTANCE_PRECISION);
        assertEquals(-2.888612500e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
        assertEquals(-6.007247516e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getY(), DERIVATION_PRECISION);
        assertEquals(3.328770172e3,  file.getDataObject2().getStateVectorBlock().getVelocityVector().getZ(), DERIVATION_PRECISION);
        // Covariance Matrix block
        assertEquals(1.337e3,   file.getDataObject2().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        assertEquals(-4.806e4,  file.getDataObject2().getRTNCovarianceBlock().getCtr(), COVARIANCE_DIAG_PRECISION);
        assertEquals(2.492e6,   file.getDataObject2().getRTNCovarianceBlock().getCtt(), COVARIANCE_DIAG_PRECISION);
        assertEquals(-3.298e1,  file.getDataObject2().getRTNCovarianceBlock().getCnr(), COVARIANCE_DIAG_PRECISION);
        assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        assertEquals(7.105e1,   file.getDataObject2().getRTNCovarianceBlock().getCnn(), COVARIANCE_DIAG_PRECISION);

        assertEquals(2.591e-3,  file.getDataObject2().getRTNCovarianceBlock().getCrdotr(), COVARIANCE_PRECISION);
        assertEquals(-4.152e-2, file.getDataObject2().getRTNCovarianceBlock().getCrdott(), COVARIANCE_PRECISION);
        assertEquals(-1.784e-6, file.getDataObject2().getRTNCovarianceBlock().getCrdotn(), COVARIANCE_PRECISION);
        assertEquals(6.886e-5,  file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(), COVARIANCE_PRECISION);

        assertEquals(-1.016e-2, file.getDataObject2().getRTNCovarianceBlock().getCtdotr(), COVARIANCE_PRECISION);
        assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(), COVARIANCE_PRECISION);
        assertEquals(1.637e-3,  file.getDataObject2().getRTNCovarianceBlock().getCtdotn(), COVARIANCE_PRECISION);
        assertEquals(-2.987e-6, file.getDataObject2().getRTNCovarianceBlock().getCtdotrdot(), COVARIANCE_PRECISION);
        assertEquals(1.059e-5,  file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(), COVARIANCE_PRECISION);

        assertEquals(4.400e-3,  file.getDataObject2().getRTNCovarianceBlock().getCndotr(), COVARIANCE_PRECISION);
        assertEquals(8.482e-3,  file.getDataObject2().getRTNCovarianceBlock().getCndott(), COVARIANCE_PRECISION);
        assertEquals(8.633e-5,  file.getDataObject2().getRTNCovarianceBlock().getCndotn(), COVARIANCE_PRECISION);
        assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(), COVARIANCE_PRECISION);
        assertEquals(-4.594e-6, file.getDataObject2().getRTNCovarianceBlock().getCndottdot(), COVARIANCE_PRECISION);
        assertEquals(5.178e-5,  file.getDataObject2().getRTNCovarianceBlock().getCndotndot(), COVARIANCE_PRECISION);
        // Test in the matrix
        assertEquals(1.337e3,   file.getDataObject2().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        assertEquals(-4.806e4,  file.getDataObject2().getRTNCovarianceBlock().getCtr(), COVARIANCE_DIAG_PRECISION);
        assertEquals(2.492e6,   file.getDataObject2().getRTNCovarianceBlock().getCtt(), COVARIANCE_DIAG_PRECISION);
        assertEquals(-3.298e1,  file.getDataObject2().getRTNCovarianceBlock().getCnr(), COVARIANCE_DIAG_PRECISION);
        assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        assertEquals(7.105e1,   file.getDataObject2().getRTNCovarianceBlock().getCnn(), COVARIANCE_DIAG_PRECISION);

        assertEquals(2.591e-3,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 0), COVARIANCE_PRECISION);
        assertEquals(-4.152e-2, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 1), COVARIANCE_PRECISION);
        assertEquals(-1.784e-6, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 2), COVARIANCE_PRECISION);
        assertEquals(6.886e-5,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 3), COVARIANCE_PRECISION);

        assertEquals(-1.016e-2, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 0), COVARIANCE_PRECISION);
        assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 1), COVARIANCE_PRECISION);
        assertEquals(1.637e-3,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 2), COVARIANCE_PRECISION);
        assertEquals(-2.987e-6, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 3), COVARIANCE_PRECISION);
        assertEquals(1.059e-5,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 4), COVARIANCE_PRECISION);

        assertEquals(4.400e-3,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 0), COVARIANCE_PRECISION);
        assertEquals(8.482e-3,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 1), COVARIANCE_PRECISION);
        assertEquals(8.633e-5,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 2), COVARIANCE_PRECISION);
        assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 3), COVARIANCE_PRECISION);
        assertEquals(-4.594e-6, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 4), COVARIANCE_PRECISION);
        assertEquals(5.178e-5,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 5), COVARIANCE_PRECISION);

        // Improve coverage
        // ----------------
        
        assertTrue(parser.inData()); // Always true by construction
        
        // AdditionalCovarianceMetadata conditions coverage
        file.getDataObject1().getAdditionalCovMetadataBlock().validate(1.0);
        file.getDataObject1().getAdditionalCovMetadataBlock().setDcpSensitivityVectorPosition(null);
        file.getDataObject1().getAdditionalCovMetadataBlock().setDcpSensitivityVectorVelocity(null);
        assertNull(file.getDataObject1().getAdditionalCovMetadataBlock().getDcpSensitivityVectorPosition());
        assertNull(file.getDataObject1().getAdditionalCovMetadataBlock().getDcpSensitivityVectorVelocity());
    }

    @Test
    void test_issue_942_KVN() {

        // File
        final String ex = "/ccsds/cdm/CDMExample_issue942.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        // OBJECT1
        assertEquals(Maneuvrable.N_A, file.getMetadataObject1().getManeuverable());    
        // OBJECT2
        assertEquals(Maneuvrable.NO, file.getMetadataObject2().getManeuverable());
    }

    @Test
    void test_issue_942_XML() {

        // File
        final String ex = "/ccsds/cdm/CDMExample_issue942.xml";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        // OBJECT1
        assertEquals(Maneuvrable.N_A, file.getMetadataObject1().getManeuverable());    
        // OBJECT2
        assertEquals(Maneuvrable.NO, file.getMetadataObject2().getManeuverable());
    }

    @Test
    void test_issue_944() {

        // File
        final String ex = "/ccsds/cdm/CDMExample_issue_944.xml";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        // Check AREA_DRG
        assertEquals(3, file.getDataObject1().getAdditionalParametersBlock().getAreaDRG(), 0.0);

        // Check AREA_SRP
        assertEquals(10, file.getDataObject1().getAdditionalParametersBlock().getAreaSRP(), 0.0);
    }

    @Test
    void test_issue_1319() {

        // File
        final String ex = "/ccsds/cdm/CDMExample_issue1319.xml";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        assertNull(file.getRelativeMetadata().getStartScreenPeriod());
    }

    @Test
    void test_issue_1319_throw_exception_when_mandatory_empty_values() {

        // File
        final String ex = "/ccsds/cdm/CDMExample_issue1319_mandatory_empty_value.xml";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        OrekitException exception = assertThrows(OrekitException.class, () -> parser.parseMessage(source));
        assertEquals("value for key X has not been initialized", exception.getMessage());
    }

    @Test
    void test_issue_1458() {

        // File
        final String ex = "/ccsds/cdm/CDMExample_issue1458.xml";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        Cdm file = parser.parseMessage(source);

        // dummy assertion: the aim of the test is to show that empty unit can be parsed
        assertNull(file.getDataObject1().getODParametersBlock());
    }
}
