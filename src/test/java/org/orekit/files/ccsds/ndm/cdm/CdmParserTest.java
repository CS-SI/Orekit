/* Copyright 2002-2022 CS GROUP
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.PocMethodType;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.odm.ocm.ObjectType;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

public class CdmParserTest {

	private static final double DISTANCE_PRECISION = 1e-8;
	private static final double DOUBLE_PRECISION = 1e-8;
	private static final double DERIVATION_PRECISION = 1e-12;
	private static final double COVARIANCE_DIAG_PRECISION = 1e-10;
	private static final double COVARIANCE_PRECISION = 1e-8;

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testParseCDM1() {
    	/** Test for CdmExample1.txt, with only required data. */
    	// File
        final String ex = "/ccsds/cdm/CDMExample1.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        // Verify general data
        Assert.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assert.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12,
                                             TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assert.assertEquals("JSPOC", file.getHeader().getOriginator());
        Assert.assertEquals("201113719185", file.getHeader().getMessageId());

       // OBJECT1
       // Check Relative Metadata Block
        Assert.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.618,
                                             TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getTca());
        Assert.assertEquals(715.0,  file.getRelativeMetadata().getMissDistance(),DISTANCE_PRECISION);

        // Check Metadata Block
        Assert.assertEquals("OBJECT1",                    file.getMetadataObject1().getObject());
        Assert.assertEquals("12345",                      file.getMetadataObject1().getObjectDesignator());
        Assert.assertEquals("SATCAT",                     file.getMetadataObject1().getCatalogName());
        Assert.assertEquals("SATELLITE A",                file.getMetadataObject1().getObjectName());
        Assert.assertEquals("1997−030E",                  file.getMetadataObject1().getInternationalDes());
        Assert.assertEquals("EPHEMERIS SATELLITE A",      file.getMetadataObject1().getEphemName());
        Assert.assertEquals(CovarianceMethod.CALCULATED,  file.getMetadataObject1().getCovarianceMethod());
        Assert.assertEquals(Maneuvrable.YES,              file.getMetadataObject1().getManeuverable());
        Assert.assertEquals(CelestialBodyFrame.EME2000,   file.getMetadataObject1().getRefFrame().asCelestialBodyFrame());
        Assert.assertEquals("UTC",                        file.getMetadataObject1().getTimeSystem().name());
        
        // Check data block
        // State vector block
        Assert.assertEquals(2570.097065e3,  file.getDataObject1().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        Assert.assertEquals(2244.654904e3,  file.getDataObject1().getStateVectorBlock().getPositionVector().getY(), DISTANCE_PRECISION);
        Assert.assertEquals(6281.497978e3,  file.getDataObject1().getStateVectorBlock().getPositionVector().getZ(), DISTANCE_PRECISION);
        Assert.assertEquals(4.418769571e3,  file.getDataObject1().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
        Assert.assertEquals(4.833547743e3,  file.getDataObject1().getStateVectorBlock().getVelocityVector().getY(), DERIVATION_PRECISION);
        Assert.assertEquals(-3.526774282e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getZ(), DERIVATION_PRECISION);
        // Covariance Matrix block
        Assert.assertEquals(4.142e1,  file.getDataObject1().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-8.579,   file.getDataObject1().getRTNCovarianceBlock().getCtr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(2.533e3,  file.getDataObject1().getRTNCovarianceBlock().getCtt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(1.336e1,  file.getDataObject1().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(7.098e1,  file.getDataObject1().getRTNCovarianceBlock().getCnn(), COVARIANCE_DIAG_PRECISION);
        
        Assert.assertEquals(2.520e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotr(), COVARIANCE_PRECISION);
        Assert.assertEquals(-5.476,   file.getDataObject1().getRTNCovarianceBlock().getCrdott(), COVARIANCE_PRECISION);
        Assert.assertEquals(8.626e-4, file.getDataObject1().getRTNCovarianceBlock().getCrdotn(), COVARIANCE_PRECISION);
        Assert.assertEquals(5.744e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotrdot(), COVARIANCE_PRECISION);
        
        Assert.assertEquals(-1.006e-2, file.getDataObject1().getRTNCovarianceBlock().getCtdotr(), COVARIANCE_PRECISION);
        Assert.assertEquals(4.041e-3,  file.getDataObject1().getRTNCovarianceBlock().getCtdott(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.359e-3, file.getDataObject1().getRTNCovarianceBlock().getCtdotn(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.502e-5, file.getDataObject1().getRTNCovarianceBlock().getCtdotrdot(), COVARIANCE_PRECISION);
        Assert.assertEquals(1.049e-5,  file.getDataObject1().getRTNCovarianceBlock().getCtdottdot(), COVARIANCE_PRECISION);
        
        Assert.assertEquals(1.053e-3,  file.getDataObject1().getRTNCovarianceBlock().getCndotr(), COVARIANCE_PRECISION);
        Assert.assertEquals(-3.412e-3, file.getDataObject1().getRTNCovarianceBlock().getCndott(), COVARIANCE_PRECISION);
        Assert.assertEquals(1.213e-2,  file.getDataObject1().getRTNCovarianceBlock().getCndotn(), COVARIANCE_PRECISION);
        Assert.assertEquals(-3.004e-6, file.getDataObject1().getRTNCovarianceBlock().getCndotrdot(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.091e-6, file.getDataObject1().getRTNCovarianceBlock().getCndottdot(), COVARIANCE_PRECISION);
        Assert.assertEquals(5.529e-5,  file.getDataObject1().getRTNCovarianceBlock().getCndotndot(), COVARIANCE_PRECISION);
     
        Assert.assertEquals(2.520e-3,   file.getDataObject1().getRTNCovarianceBlock().
                            getRTNCovarianceMatrix().getEntry(3, 0), COVARIANCE_PRECISION);
        Assert.assertEquals(Double.NaN, file.getDataObject1().getRTNCovarianceBlock().
                            getRTNCovarianceMatrix().getEntry(7, 6), COVARIANCE_PRECISION);
        
        
        
        // OBJECT2
        // Check Relative Metadata Block
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getTca(),
                file.getRelativeMetadata().getTca());
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getMissDistance(),  
                file.getRelativeMetadata().getMissDistance(),DISTANCE_PRECISION);
        
        // Check Metadata Block
        Assert.assertEquals("OBJECT2",                    file.getMetadataObject2().getObject());
        Assert.assertEquals("30337",                      file.getMetadataObject2().getObjectDesignator());
        Assert.assertEquals("SATCAT",                     file.getMetadataObject2().getCatalogName());
        Assert.assertEquals("FENGYUN 1C DEB",             file.getMetadataObject2().getObjectName());
        Assert.assertEquals("1999-025AA",                 file.getMetadataObject2().getInternationalDes());
        Assert.assertEquals("NONE",                       file.getMetadataObject2().getEphemName());
        Assert.assertEquals(CovarianceMethod.CALCULATED,  file.getMetadataObject2().getCovarianceMethod());
        Assert.assertEquals(Maneuvrable.NO,               file.getMetadataObject2().getManeuverable());
        Assert.assertEquals(CelestialBodyFrame.EME2000,   file.getMetadataObject2().getRefFrame().asCelestialBodyFrame());
        Assert.assertEquals("UTC",                        file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        Assert.assertEquals(2569.540800e3,  file.getDataObject2().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        Assert.assertEquals(2245.093614e3,  file.getDataObject2().getStateVectorBlock().getPositionVector().getY(), DISTANCE_PRECISION);
        Assert.assertEquals(6281.599946e3,  file.getDataObject2().getStateVectorBlock().getPositionVector().getZ(), DISTANCE_PRECISION);
        Assert.assertEquals(-2.888612500e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
        Assert.assertEquals(-6.007247516e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getY(), DERIVATION_PRECISION);
        Assert.assertEquals(3.328770172e3,  file.getDataObject2().getStateVectorBlock().getVelocityVector().getZ(), DERIVATION_PRECISION);
        // Covariance Matrix block
        Assert.assertEquals(1.337e3,   file.getDataObject2().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-4.806e4,  file.getDataObject2().getRTNCovarianceBlock().getCtr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(2.492e6,   file.getDataObject2().getRTNCovarianceBlock().getCtt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-3.298e1,  file.getDataObject2().getRTNCovarianceBlock().getCnr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(7.105e1,   file.getDataObject2().getRTNCovarianceBlock().getCnn(), COVARIANCE_DIAG_PRECISION);

        Assert.assertEquals(2.591e-3,  file.getDataObject2().getRTNCovarianceBlock().getCrdotr(), COVARIANCE_PRECISION);
        Assert.assertEquals(-4.152e-2, file.getDataObject2().getRTNCovarianceBlock().getCrdott(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.784e-6, file.getDataObject2().getRTNCovarianceBlock().getCrdotn(), COVARIANCE_PRECISION);
        Assert.assertEquals(6.886e-5,  file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(), COVARIANCE_PRECISION);
        
        Assert.assertEquals(-1.016e-2, file.getDataObject2().getRTNCovarianceBlock().getCtdotr(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(), COVARIANCE_PRECISION);
        Assert.assertEquals(1.637e-3,  file.getDataObject2().getRTNCovarianceBlock().getCtdotn(), COVARIANCE_PRECISION);
        Assert.assertEquals(-2.987e-6, file.getDataObject2().getRTNCovarianceBlock().getCtdotrdot(), COVARIANCE_PRECISION);
        Assert.assertEquals(1.059e-5,  file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(), COVARIANCE_PRECISION);
        
        Assert.assertEquals(4.400e-3,  file.getDataObject2().getRTNCovarianceBlock().getCndotr(), COVARIANCE_PRECISION);
        Assert.assertEquals(8.482e-3,  file.getDataObject2().getRTNCovarianceBlock().getCndott(), COVARIANCE_PRECISION);
        Assert.assertEquals(8.633e-5,  file.getDataObject2().getRTNCovarianceBlock().getCndotn(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(), COVARIANCE_PRECISION);
        Assert.assertEquals(-4.594e-6, file.getDataObject2().getRTNCovarianceBlock().getCndottdot(), COVARIANCE_PRECISION);
        Assert.assertEquals(5.178e-5,  file.getDataObject2().getRTNCovarianceBlock().getCndotndot(), COVARIANCE_PRECISION);
        // Test in the matrix
        Assert.assertEquals(1.337e3,   file.getDataObject2().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-4.806e4,  file.getDataObject2().getRTNCovarianceBlock().getCtr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(2.492e6,   file.getDataObject2().getRTNCovarianceBlock().getCtt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-3.298e1,  file.getDataObject2().getRTNCovarianceBlock().getCnr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(7.105e1,   file.getDataObject2().getRTNCovarianceBlock().getCnn(), COVARIANCE_DIAG_PRECISION);

        Assert.assertEquals(2.591e-3,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 0), COVARIANCE_PRECISION);
        Assert.assertEquals(-4.152e-2, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 1), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.784e-6, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 2), COVARIANCE_PRECISION);
        Assert.assertEquals(6.886e-5,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(3, 3), COVARIANCE_PRECISION);
        
        Assert.assertEquals(-1.016e-2, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 0), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 1), COVARIANCE_PRECISION);
        Assert.assertEquals(1.637e-3,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 2), COVARIANCE_PRECISION);
        Assert.assertEquals(-2.987e-6, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 3), COVARIANCE_PRECISION);
        Assert.assertEquals(1.059e-5,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(4, 4), COVARIANCE_PRECISION);
        
        Assert.assertEquals(4.400e-3,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 0), COVARIANCE_PRECISION);
        Assert.assertEquals(8.482e-3,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 1), COVARIANCE_PRECISION);
        Assert.assertEquals(8.633e-5,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 2), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 3), COVARIANCE_PRECISION);
        Assert.assertEquals(-4.594e-6, file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 4), COVARIANCE_PRECISION);
        Assert.assertEquals(5.178e-5,  file.getDataObject2().getRTNCovarianceBlock().getRTNCovarianceMatrix().getEntry(5, 5), COVARIANCE_PRECISION);

    }
    
    @Test
    public void testParseCDM2() {
    	/** Test for CdmExample2.txt, with only required data. */
    	// File
        final String ex = "/ccsds/cdm/CDMExample2.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);
        
        Assert.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assert.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12,
                            TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assert.assertEquals("JSPOC", file.getHeader().getOriginator());
        Assert.assertEquals("201113719185", file.getHeader().getMessageId());

       // OBJECT1
       // Check Relative Metadata Block
        Assert.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.618,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getTca());
        Assert.assertEquals(715.0,     file.getRelativeMetadata().getMissDistance(),     DISTANCE_PRECISION);
        Assert.assertEquals(14762.0,   file.getRelativeMetadata().getRelativeSpeed(),    DERIVATION_PRECISION);
        Assert.assertEquals(27.4,      file.getRelativeMetadata().getRelativePosition().getX(),DISTANCE_PRECISION);
        Assert.assertEquals(-70.2,     file.getRelativeMetadata().getRelativePosition().getY(),DISTANCE_PRECISION);
        Assert.assertEquals(711.8,     file.getRelativeMetadata().getRelativePosition().getZ(),DISTANCE_PRECISION);
        Assert.assertEquals(-7.2,      file.getRelativeMetadata().getRelativeVelocity().getX(),DERIVATION_PRECISION);
        Assert.assertEquals(-14692.0,  file.getRelativeMetadata().getRelativeVelocity().getY(),DERIVATION_PRECISION);
        Assert.assertEquals(-1437.2,   file.getRelativeMetadata().getRelativeVelocity().getZ(),DERIVATION_PRECISION);
        Assert.assertEquals(new AbsoluteDate(2010, 3, 12, 18, 29, 32.212,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getStartScreenPeriod());
        Assert.assertEquals(new AbsoluteDate(2010, 3, 15, 18, 29, 32.212,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getStopScreenPeriod());
        Assert.assertEquals(ScreenVolumeFrame.RTN,        file.getRelativeMetadata().getScreenVolumeFrame());
        Assert.assertEquals(ScreenVolumeShape.ELLIPSOID,  file.getRelativeMetadata().getScreenVolumeShape());
        Assert.assertEquals(200,   file.getRelativeMetadata().getScreenVolumeX(), 0);
        Assert.assertEquals(1000,  file.getRelativeMetadata().getScreenVolumeY(), 0);
        Assert.assertEquals(1000,  file.getRelativeMetadata().getScreenVolumeZ(), 0);
        Assert.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.222,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getScreenEntryTime());
        Assert.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.824,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getScreenExitTime());
        Assert.assertEquals(4.835E-05,      file.getRelativeMetadata().getCollisionProbability(), 1e-30);
        Assert.assertEquals("FOSTER-1992",  file.getRelativeMetadata().getCollisionProbaMethod().getName());
        Assert.assertEquals(PocMethodType.FOSTER_1992,  file.getRelativeMetadata().getCollisionProbaMethod().getType());
        
        // Check Metadata Block
        Assert.assertEquals("OBJECT1" ,                 file.getMetadataObject1().getObject());
        Assert.assertEquals(ObjectType.PAYLOAD,         file.getMetadataObject1().getObjectType());
        Assert.assertEquals("OSA",                      file.getMetadataObject1().getOperatorContactPosition());
        Assert.assertEquals("EUMETSAT",                 file.getMetadataObject1().getOperatorOrganization());
        Assert.assertEquals("+49615130312",             file.getMetadataObject1().getOperatorPhone());
        Assert.assertEquals("JOHN.DOE@SOMEWHERE.NET",   file.getMetadataObject1().getOperatorEmail());
        Assert.assertEquals("EPHEMERIS SATELLITE A",    file.getMetadataObject1().getEphemName());
        Assert.assertEquals(FramesFactory.getEME2000(), file.getMetadataObject1().getFrame());
        Assert.assertEquals("JACCHIA 70 DCA",           file.getMetadataObject1().getAtmosphericModel());
        Assert.assertEquals("EGM-96",                   file.getMetadataObject1().getGravityModel());
        Assert.assertEquals(36,                         file.getMetadataObject1().getGravityDegree(), 0);
        Assert.assertEquals(36,                         file.getMetadataObject1().getGravityOrder(), 0);
        Assert.assertEquals("MOON",                     file.getMetadataObject1().getNBodyPerturbations().get(0).getName());
        Assert.assertEquals("SUN",                      file.getMetadataObject1().getNBodyPerturbations().get(1).getName());
        Assert.assertEquals(false,                      file.getMetadataObject1().getSolarRadiationPressure());
        Assert.assertEquals(false,                      file.getMetadataObject1().getEarthTides());
        Assert.assertEquals(false,                      file.getMetadataObject1().getIntrackThrust());
        Assert.assertEquals("UTC",                      file.getMetadataObject1().getTimeSystem().name());
        
        // Check data block
        // OD parameters block
        Assert.assertEquals(new AbsoluteDate(2010, 3, 12, 02, 14, 12.746,
                            TimeScalesFactory.getUTC()),
                            file.getDataObject1().getODParametersBlock().getTimeLastObsStart());
        Assert.assertEquals(new AbsoluteDate(2010, 3, 12, 02, 14, 12.746,
                            TimeScalesFactory.getUTC()),
                            file.getDataObject1().getODParametersBlock().getTimeLastObsEnd());
        Assert.assertEquals(7.88*3600.0*24.0, file.getDataObject1().getODParametersBlock().getRecommendedOdSpan(), DOUBLE_PRECISION);
        Assert.assertEquals(5.50*3600.0*24.0, file.getDataObject1().getODParametersBlock().getActualOdSpan(), DOUBLE_PRECISION);
        Assert.assertEquals(592,              file.getDataObject1().getODParametersBlock().getObsAvailable(), DOUBLE_PRECISION);
        Assert.assertEquals(579,              file.getDataObject1().getODParametersBlock().getObsUsed(), DOUBLE_PRECISION);
        Assert.assertEquals(123,              file.getDataObject1().getODParametersBlock().getTracksAvailable(), DOUBLE_PRECISION);
        Assert.assertEquals(119,              file.getDataObject1().getODParametersBlock().getTracksUsed(), DOUBLE_PRECISION);
        Assert.assertEquals(97.8/100.0,       file.getDataObject1().getODParametersBlock().getResidualsAccepted(), DOUBLE_PRECISION);
        Assert.assertEquals(0.864,            file.getDataObject1().getODParametersBlock().getWeightedRMS(), DOUBLE_PRECISION);
        // Additional parameters block
        Assert.assertEquals(5.2,         file.getDataObject1().getAdditionalParametersBlock().getAreaPC(), DOUBLE_PRECISION);
        Assert.assertEquals(251.6,       file.getDataObject1().getAdditionalParametersBlock().getMass(), DOUBLE_PRECISION);
        Assert.assertEquals(0.045663,    file.getDataObject1().getAdditionalParametersBlock().getCDAreaOverMass(), DOUBLE_PRECISION);
        Assert.assertEquals(0.000000,    file.getDataObject1().getAdditionalParametersBlock().getCRAreaOverMass(), DOUBLE_PRECISION);
        Assert.assertEquals(0.0,         file.getDataObject1().getAdditionalParametersBlock().getThrustAcceleration(), DOUBLE_PRECISION);
        Assert.assertEquals(4.54570E-05, file.getDataObject1().getAdditionalParametersBlock().getSedr(), DOUBLE_PRECISION);
        // State vector block
        Assert.assertEquals(2570.097065e3,  file.getDataObject1().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        Assert.assertEquals(4.833547743e3,  file.getDataObject1().getStateVectorBlock().getVelocityVector().getY(), DERIVATION_PRECISION);
        // Covariance Matrix block
        Assert.assertEquals(4.142e1,  file.getDataObject1().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-8.579,   file.getDataObject1().getRTNCovarianceBlock().getCtr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(2.533e3,  file.getDataObject1().getRTNCovarianceBlock().getCtt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(1.336e1,  file.getDataObject1().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(7.098e1,  file.getDataObject1().getRTNCovarianceBlock().getCnn(), COVARIANCE_DIAG_PRECISION);
        
        Assert.assertEquals(-1.862E+00, file.getDataObject1().getRTNCovarianceBlock().getCdrgr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(3.530E+00,  file.getDataObject1().getRTNCovarianceBlock().getCdrgt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-3.100E-01, file.getDataObject1().getRTNCovarianceBlock().getCdrgn(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-1.214E-04, file.getDataObject1().getRTNCovarianceBlock().getCdrgrdot(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(2.580E-04,  file.getDataObject1().getRTNCovarianceBlock().getCdrgtdot(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-6.467E-05, file.getDataObject1().getRTNCovarianceBlock().getCdrgndot(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(3.483E-06,  file.getDataObject1().getRTNCovarianceBlock().getCdrgdrg(), COVARIANCE_DIAG_PRECISION);
        
        Assert.assertEquals(-1.492E+02, file.getDataObject1().getRTNCovarianceBlock().getCsrpr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(2.044E+02,  file.getDataObject1().getRTNCovarianceBlock().getCsrpt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-2.331E+01, file.getDataObject1().getRTNCovarianceBlock().getCsrpn(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-1.254E-03, file.getDataObject1().getRTNCovarianceBlock().getCsrprdot(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(2.013E-02,  file.getDataObject1().getRTNCovarianceBlock().getCsrptdot(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-4.700E-03, file.getDataObject1().getRTNCovarianceBlock().getCsrpndot(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(2.210E-04,  file.getDataObject1().getRTNCovarianceBlock().getCsrpdrg(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(1.593E-02,  file.getDataObject1().getRTNCovarianceBlock().getCsrpsrp(), COVARIANCE_DIAG_PRECISION);
        
        
        // OBJECT2
        // Check Relative Metadata Block
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getScreenVolumeX(),
                            file.getRelativeMetadata().getScreenVolumeX(), DISTANCE_PRECISION);
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativePosition().getY(),
                            file.getRelativeMetadata().getRelativePosition().getY(), DISTANCE_PRECISION);
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativeVelocity().getZ(),  
                            file.getRelativeMetadata().getRelativeVelocity().getZ(), DERIVATION_PRECISION);
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbability(),  
                            file.getRelativeMetadata().getCollisionProbability(),1e-30);
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbaMethod(),  
                            file.getRelativeMetadata().getCollisionProbaMethod());
        
        // Check Metadata Block
        Assert.assertEquals("OBJECT2" ,                   file.getMetadataObject2().getObject());
        Assert.assertEquals("30337",                      file.getMetadataObject2().getObjectDesignator());
        Assert.assertEquals("SATCAT",                     file.getMetadataObject2().getCatalogName());
        Assert.assertEquals("FENGYUN 1C DEB",             file.getMetadataObject2().getObjectName());
        Assert.assertEquals("1999-025AA",                 file.getMetadataObject2().getInternationalDes());
        Assert.assertEquals("NONE",                       file.getMetadataObject2().getEphemName());
        Assert.assertEquals(CovarianceMethod.CALCULATED,  file.getMetadataObject2().getCovarianceMethod());
        Assert.assertEquals(Maneuvrable.NO,               file.getMetadataObject2().getManeuverable());
        Assert.assertEquals(CelestialBodyFrame.EME2000,   file.getMetadataObject2().getRefFrame().asCelestialBodyFrame());
        Assert.assertEquals("UTC",                        file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        Assert.assertEquals(2569.540800e3 ,  file.getDataObject2().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        Assert.assertEquals(-2.888612500e3 , file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
        // Covariance Matrix block
        Assert.assertEquals(1.337e3,   file.getDataObject2().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        
        Assert.assertEquals(2.591e-3,  file.getDataObject2().getRTNCovarianceBlock().getCrdotr(), COVARIANCE_PRECISION);
        Assert.assertEquals(6.886e-5,  file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(), COVARIANCE_PRECISION);
        
        Assert.assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(), COVARIANCE_PRECISION);
        Assert.assertEquals(1.059e-5,  file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(), COVARIANCE_PRECISION);
        
        Assert.assertEquals(8.633e-5,  file.getDataObject2().getRTNCovarianceBlock().getCndotn(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(), COVARIANCE_PRECISION);
        
        Assert.assertEquals(-5.117E-01, file.getDataObject2().getRTNCovarianceBlock().getCdrgr(), COVARIANCE_PRECISION);
        Assert.assertEquals(1.319E+00,  file.getDataObject2().getRTNCovarianceBlock().getCdrgt(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.903E-05, file.getDataObject2().getRTNCovarianceBlock().getCdrgndot(), COVARIANCE_PRECISION);
        Assert.assertEquals(7.402E-05,  file.getDataObject2().getRTNCovarianceBlock().getCdrgtdot(), COVARIANCE_PRECISION);
        Assert.assertEquals(-3.297E+01, file.getDataObject2().getRTNCovarianceBlock().getCsrpr(), COVARIANCE_PRECISION);
        Assert.assertEquals(8.164E+01,  file.getDataObject2().getRTNCovarianceBlock().getCsrpt(), COVARIANCE_PRECISION);

        // Verify comments
        Assert.assertEquals("[Relative Metadata/Data]", file.getRelativeMetadata().getComment().toString());
        Assert.assertEquals("[Object1 Metadata]", file.getMetadataObject1().getComments().toString());
        Assert.assertEquals("[Object2 Metadata]", file.getMetadataObject2().getComments().toString());
        Assert.assertEquals("[Object1 Data]", file.getDataObject1().getComments().toString());
        Assert.assertEquals("[Object1 OD Parameters]", file.getDataObject1().getODParametersBlock().getComments().toString());
        Assert.assertEquals("[Object2 OD Parameters]", file.getDataObject2().getODParametersBlock().getComments().toString());
        Assert.assertEquals("[Object1 Additional Parameters, Apogee Altitude=779 km, Perigee Altitude=765 km, Inclination=86.4 deg]",
                            file.getDataObject1().getAdditionalParametersBlock().getComments().toString());
        Assert.assertEquals("[Object2 Additional Parameters, Apogee Altitude=786 km, Perigee Altitude=414 km, Inclination=98.9 deg]",
                            file.getDataObject2().getAdditionalParametersBlock().getComments().toString());

    }

    @Test
    public void testParseCDM3() {
    	/** Test for CdmExample3.txt, with only required data. */
    	// File
        final String ex = "/ccsds/cdm/CDMExample3.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);

        
        Assert.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assert.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assert.assertEquals(new AbsoluteDate(2012, 9, 12, 22, 31, 12,
                            TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assert.assertEquals("SDC", file.getHeader().getOriginator());
        Assert.assertEquals("GALAXY 15", file.getHeader().getMessageFor());
        Assert.assertEquals("20120912223112", file.getHeader().getMessageId());

       // OBJECT1
       // Check Relative Metadata Block
        Assert.assertEquals(new AbsoluteDate(2012, 9, 13, 22, 37, 52.618,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getTca());
        Assert.assertEquals(104.92,   file.getRelativeMetadata().getMissDistance(),     DISTANCE_PRECISION);
        Assert.assertEquals(12093.52, file.getRelativeMetadata().getRelativeSpeed(),    DERIVATION_PRECISION);
        Assert.assertEquals(30.6,     file.getRelativeMetadata().getRelativePosition().getX(),DISTANCE_PRECISION);
        Assert.assertEquals(100.2,    file.getRelativeMetadata().getRelativePosition().getY(),DISTANCE_PRECISION);
        Assert.assertEquals(5.7,      file.getRelativeMetadata().getRelativePosition().getZ(),DISTANCE_PRECISION);
        Assert.assertEquals(-20.3,    file.getRelativeMetadata().getRelativeVelocity().getX(),DERIVATION_PRECISION);
        Assert.assertEquals(-12000.0, file.getRelativeMetadata().getRelativeVelocity().getY(),DERIVATION_PRECISION);
        Assert.assertEquals(-1500.9,  file.getRelativeMetadata().getRelativeVelocity().getZ(),DERIVATION_PRECISION);
        Assert.assertEquals(new AbsoluteDate(2012, 9, 12, 18, 29, 32.212,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getStartScreenPeriod());
        Assert.assertEquals(new AbsoluteDate(2012, 9, 15, 18, 29, 32.212,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getStopScreenPeriod());
        Assert.assertEquals(ScreenVolumeFrame.RTN,        file.getRelativeMetadata().getScreenVolumeFrame());
        Assert.assertEquals(ScreenVolumeShape.ELLIPSOID,  file.getRelativeMetadata().getScreenVolumeShape());
        Assert.assertEquals(500,  file.getRelativeMetadata().getScreenVolumeX(), 0);
        Assert.assertEquals(500,  file.getRelativeMetadata().getScreenVolumeY(), 0);
        Assert.assertEquals(500,  file.getRelativeMetadata().getScreenVolumeZ(), 0);
        Assert.assertEquals(new AbsoluteDate(2012, 9, 13, 20, 25, 43.222,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getScreenEntryTime());
        Assert.assertEquals(new AbsoluteDate(2012, 9, 13, 23, 44, 29.324,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getScreenExitTime());
        Assert.assertEquals(2.355e-03,      file.getRelativeMetadata().getCollisionProbability(), 1e-30);
        Assert.assertEquals("ALFANO-2005",  file.getRelativeMetadata().getCollisionProbaMethod().getName());
        Assert.assertEquals(PocMethodType.ALFANO_2005,  file.getRelativeMetadata().getCollisionProbaMethod().getType());
        
        // Check Metadata Block
        Assert.assertEquals("OBJECT1" ,                        file.getMetadataObject1().getObject());
        Assert.assertEquals("28884",                           file.getMetadataObject1().getObjectDesignator());
        Assert.assertEquals("SATCAT",                          file.getMetadataObject1().getCatalogName());
        Assert.assertEquals("GALAXY 15",                       file.getMetadataObject1().getObjectName());
        Assert.assertEquals("2005-041A",                       file.getMetadataObject1().getInternationalDes());
        Assert.assertEquals(ObjectType.PAYLOAD,                file.getMetadataObject1().getObjectType());
        Assert.assertEquals(null,                              file.getMetadataObject1().getOperatorContactPosition());
        Assert.assertEquals("INTELSAT",                        file.getMetadataObject1().getOperatorOrganization());
        Assert.assertEquals("GALAXY-15A-2012JAN-WMANEUVER23A", file.getMetadataObject1().getEphemName());
        Assert.assertEquals(CovarianceMethod.CALCULATED,       file.getMetadataObject1().getCovarianceMethod());
        Assert.assertEquals(Maneuvrable.YES,                   file.getMetadataObject1().getManeuverable());
        Assert.assertEquals(CelestialBodyFrame.EME2000,        file.getMetadataObject1().getRefFrame().asCelestialBodyFrame());
        Assert.assertEquals("UTC",                             file.getMetadataObject1().getTimeSystem().name());
        
        // Check data block
        // OD parameters block
        Assert.assertEquals(new AbsoluteDate(2012, 9, 6, 20, 25, 43.222,
                            TimeScalesFactory.getUTC()),
                            file.getDataObject1().getODParametersBlock().getTimeLastObsStart());
        Assert.assertEquals(new AbsoluteDate(2012, 9, 6, 20, 25, 43.222,
                            TimeScalesFactory.getUTC()),
                            file.getDataObject1().getODParametersBlock().getTimeLastObsEnd());
        // State vector block
        Assert.assertEquals(-41600.46272465e3, file.getDataObject1().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        Assert.assertEquals(3626.912120064e3,  file.getDataObject1().getStateVectorBlock().getPositionVector().getY(), DISTANCE_PRECISION);
        Assert.assertEquals(6039.06350924e3,   file.getDataObject1().getStateVectorBlock().getPositionVector().getZ(), DISTANCE_PRECISION);
        Assert.assertEquals(-0.306132852503e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
        Assert.assertEquals(-3.044998353334e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getY(), DERIVATION_PRECISION);
        Assert.assertEquals(-0.287674310725e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getZ(), DERIVATION_PRECISION);
        // Covariance Matrix block
        Assert.assertEquals(4.142e1,  file.getDataObject1().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-8.579,   file.getDataObject1().getRTNCovarianceBlock().getCtr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(2.533e3,  file.getDataObject1().getRTNCovarianceBlock().getCtt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(1.336e1,  file.getDataObject1().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(7.098e1,  file.getDataObject1().getRTNCovarianceBlock().getCnn(), COVARIANCE_DIAG_PRECISION);
        
        Assert.assertEquals(2.520E-03,  file.getDataObject1().getRTNCovarianceBlock().getCrdotr(), COVARIANCE_PRECISION);
        Assert.assertEquals(-5.476E+00, file.getDataObject1().getRTNCovarianceBlock().getCrdott(), COVARIANCE_PRECISION);
        Assert.assertEquals(8.626E-04,  file.getDataObject1().getRTNCovarianceBlock().getCrdotn(), COVARIANCE_PRECISION);
        Assert.assertEquals(5.744E-03,  file.getDataObject1().getRTNCovarianceBlock().getCrdotrdot(), COVARIANCE_PRECISION);
       
        Assert.assertEquals(-1.006E-02, file.getDataObject1().getRTNCovarianceBlock().getCtdotr(), COVARIANCE_PRECISION);
        Assert.assertEquals(4.041E-03,  file.getDataObject1().getRTNCovarianceBlock().getCtdott(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.359E-03, file.getDataObject1().getRTNCovarianceBlock().getCtdotn(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.502E-05, file.getDataObject1().getRTNCovarianceBlock().getCtdotrdot(), COVARIANCE_PRECISION);
        Assert.assertEquals(1.049E-05,  file.getDataObject1().getRTNCovarianceBlock().getCtdottdot(), COVARIANCE_PRECISION);
        
        Assert.assertEquals(1.053E-03,  file.getDataObject1().getRTNCovarianceBlock().getCndotr(), COVARIANCE_PRECISION);
        Assert.assertEquals(-3.412E-03, file.getDataObject1().getRTNCovarianceBlock().getCndott(), COVARIANCE_PRECISION);
        Assert.assertEquals(1.213E-02,  file.getDataObject1().getRTNCovarianceBlock().getCndotn(), COVARIANCE_PRECISION);
        Assert.assertEquals(-3.004E-06, file.getDataObject1().getRTNCovarianceBlock().getCndotrdot(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.091E-06, file.getDataObject1().getRTNCovarianceBlock().getCndottdot(), COVARIANCE_PRECISION);
        Assert.assertEquals(5.529E-05,  file.getDataObject1().getRTNCovarianceBlock().getCndotndot(), COVARIANCE_PRECISION);
                
        // OBJECT2
        // Check Relative Metadata Block
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getScreenVolumeX(),
                            file.getRelativeMetadata().getScreenVolumeX(), DISTANCE_PRECISION);
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativePosition().getX(),
                            file.getRelativeMetadata().getRelativePosition().getX(), DISTANCE_PRECISION);
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativeVelocity().getZ(),  
                            file.getRelativeMetadata().getRelativeVelocity().getZ(), DERIVATION_PRECISION);
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbability(),  
                            file.getRelativeMetadata().getCollisionProbability(),1e-30);
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbaMethod(),  
                            file.getRelativeMetadata().getCollisionProbaMethod());
        
        // Check Metadata Block
        Assert.assertEquals("OBJECT2" ,                  file.getMetadataObject2().getObject());
        Assert.assertEquals("21139",                     file.getMetadataObject2().getObjectDesignator());
        Assert.assertEquals("SATCAT",                    file.getMetadataObject2().getCatalogName());
        Assert.assertEquals("ASTRA 1B",                  file.getMetadataObject2().getObjectName());
        Assert.assertEquals("1991-051A",                 file.getMetadataObject2().getInternationalDes());
        Assert.assertEquals(ObjectType.PAYLOAD,          file.getMetadataObject2().getObjectType());
        Assert.assertEquals("NONE",                      file.getMetadataObject2().getEphemName());
        Assert.assertEquals(CovarianceMethod.CALCULATED, file.getMetadataObject2().getCovarianceMethod());
        Assert.assertEquals(Maneuvrable.YES,             file.getMetadataObject2().getManeuverable());
        Assert.assertEquals(CelestialBodyFrame.EME2000,  file.getMetadataObject2().getRefFrame().asCelestialBodyFrame());
        Assert.assertEquals("UTC",                       file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        Assert.assertEquals(-2956.02034826e3,  file.getDataObject2().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        Assert.assertEquals(-3.047096589536e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
        // Covariance Matrix block
        Assert.assertEquals(1.337e3,   file.getDataObject2().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        
        Assert.assertEquals(2.591e-3,  file.getDataObject2().getRTNCovarianceBlock().getCrdotr(), COVARIANCE_PRECISION);
        Assert.assertEquals(6.886e-5,  file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(), COVARIANCE_PRECISION);
        
        Assert.assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(), COVARIANCE_PRECISION);
        Assert.assertEquals(1.059e-5,  file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(), COVARIANCE_PRECISION);
        
        Assert.assertEquals(8.633e-5,  file.getDataObject2().getRTNCovarianceBlock().getCndotn(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(), COVARIANCE_PRECISION);

        // Verify comments
        Assert.assertEquals("[Relative Metadata/Data]",                         file.getMetadataObject1().getRelativeMetadata().getComment().toString());
        Assert.assertEquals("[Object1 Metadata]",                               file.getMetadataObject1().getComments().toString());
        Assert.assertEquals("[Object2 Metadata]",                               file.getMetadataObject2().getComments().toString());
        Assert.assertEquals("[Object1 OD Parameters]",                          file.getDataObject1().getODParametersBlock().getComments().toString());
        Assert.assertEquals("[Object1 Data]",                                   file.getDataObject1().getComments().toString());
        Assert.assertEquals("[Object2 Data]",                                   file.getDataObject2().getComments().toString());
        Assert.assertEquals("[Object2 OD Parameters]",                          file.getDataObject2().getODParametersBlock().getComments().toString());
        Assert.assertEquals("[]",                                               file.getDataObject1().getStateVectorBlock().getComments().toString());
        Assert.assertEquals("[Object1 Covariance in the RTN Coordinate Frame]", file.getDataObject1().getRTNCovarianceBlock().getComments().toString());

    }

    @Test
    public void testParseCDM4() {
    	/** Test for CdmExample2.txt, with only required data. */
    	// File
        final String ex = "/ccsds/cdm/CDMExample4.txt";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);
        
        Assert.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assert.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 1.0e-10);
        Assert.assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12,
                            TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assert.assertEquals("JSPOC", file.getHeader().getOriginator());
        Assert.assertEquals("201113719185", file.getHeader().getMessageId());

       // OBJECT1
       // Check Relative Metadata Block
        Assert.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.618,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getTca());
        Assert.assertEquals(715.0,     file.getRelativeMetadata().getMissDistance(),     DISTANCE_PRECISION);

        Assert.assertEquals(new AbsoluteDate(2010, 3, 12, 18, 29, 32.212,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getStartScreenPeriod());
        Assert.assertEquals(new AbsoluteDate(2010, 3, 15, 18, 29, 32.212,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getStopScreenPeriod());
        Assert.assertEquals(ScreenVolumeFrame.RTN,        file.getRelativeMetadata().getScreenVolumeFrame());
        Assert.assertEquals(ScreenVolumeShape.ELLIPSOID,  file.getRelativeMetadata().getScreenVolumeShape());
        Assert.assertEquals(200,   file.getRelativeMetadata().getScreenVolumeX(), 0);
        Assert.assertEquals(1000,  file.getRelativeMetadata().getScreenVolumeY(), 0);
        Assert.assertEquals(1000,  file.getRelativeMetadata().getScreenVolumeZ(), 0);
        Assert.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.222,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getScreenEntryTime());
        Assert.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.824,
                            TimeScalesFactory.getUTC()),
                            file.getRelativeMetadata().getScreenExitTime());
        Assert.assertEquals(4.835E-05,      file.getRelativeMetadata().getCollisionProbability(), 1e-30);
        Assert.assertEquals("FOSTER-1992",  file.getRelativeMetadata().getCollisionProbaMethod().getName());
        Assert.assertEquals(PocMethodType.FOSTER_1992,  file.getRelativeMetadata().getCollisionProbaMethod().getType());
        
        // Check Metadata Block
        Assert.assertEquals("OBJECT1" ,                 file.getMetadataObject1().getObject());
        Assert.assertEquals(ObjectType.PAYLOAD,         file.getMetadataObject1().getObjectType());
        Assert.assertEquals("OSA",                      file.getMetadataObject1().getOperatorContactPosition());
        Assert.assertEquals("EUMETSAT",                 file.getMetadataObject1().getOperatorOrganization());

        // Covariance Matrix block
        Assert.assertEquals(4.142e1,  file.getDataObject1().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-8.579,   file.getDataObject1().getRTNCovarianceBlock().getCtr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(2.533e3,  file.getDataObject1().getRTNCovarianceBlock().getCtt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(1.336e1,  file.getDataObject1().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(7.098e1,  file.getDataObject1().getRTNCovarianceBlock().getCnn(), COVARIANCE_DIAG_PRECISION);
        
        Assert.assertEquals(-1.862E+00, file.getDataObject1().getRTNCovarianceBlock().getCdrgr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(3.530E+00,  file.getDataObject1().getRTNCovarianceBlock().getCdrgt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-3.100E-01, file.getDataObject1().getRTNCovarianceBlock().getCdrgn(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-1.214E-04, file.getDataObject1().getRTNCovarianceBlock().getCdrgrdot(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(2.580E-04,  file.getDataObject1().getRTNCovarianceBlock().getCdrgtdot(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-6.467E-05, file.getDataObject1().getRTNCovarianceBlock().getCdrgndot(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(3.483E-06,  file.getDataObject1().getRTNCovarianceBlock().getCdrgdrg(), COVARIANCE_DIAG_PRECISION);
        
        Assert.assertEquals(-1.492E+02, file.getDataObject1().getRTNCovarianceBlock().getCsrpr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(2.044E+02,  file.getDataObject1().getRTNCovarianceBlock().getCsrpt(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-2.331E+01, file.getDataObject1().getRTNCovarianceBlock().getCsrpn(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-1.254E-03, file.getDataObject1().getRTNCovarianceBlock().getCsrprdot(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(2.013E-02,  file.getDataObject1().getRTNCovarianceBlock().getCsrptdot(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-4.700E-03, file.getDataObject1().getRTNCovarianceBlock().getCsrpndot(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(2.210E-04,  file.getDataObject1().getRTNCovarianceBlock().getCsrpdrg(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(1.593E-02,  file.getDataObject1().getRTNCovarianceBlock().getCsrpsrp(), COVARIANCE_DIAG_PRECISION);
        
        Assert.assertEquals(-1.803E-06, file.getDataObject1().getRTNCovarianceBlock().getCthrr(), COVARIANCE_PRECISION);
        Assert.assertEquals(3.803E-03 , file.getDataObject1().getRTNCovarianceBlock().getCthrt(), COVARIANCE_PRECISION);
        Assert.assertEquals(3.303E02  , file.getDataObject1().getRTNCovarianceBlock().getCthrn(), COVARIANCE_PRECISION);
        Assert.assertEquals(7.203E01  , file.getDataObject1().getRTNCovarianceBlock().getCthrrdot(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.654E01 , file.getDataObject1().getRTNCovarianceBlock().getCthrtdot(), COVARIANCE_PRECISION);
        Assert.assertEquals(9.203E-01 , file.getDataObject1().getRTNCovarianceBlock().getCthrndot(), COVARIANCE_PRECISION);
        Assert.assertEquals(-3.876    , file.getDataObject1().getRTNCovarianceBlock().getCthrdrg(), COVARIANCE_PRECISION);
        Assert.assertEquals(6.876E03  , file.getDataObject1().getRTNCovarianceBlock().getCthrsrp(), COVARIANCE_PRECISION);        
        Assert.assertEquals(-2.986E-02, file.getDataObject1().getRTNCovarianceBlock().getCthrthr(), COVARIANCE_PRECISION);
                
        // OBJECT2
        // Check Relative Metadata Block
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getScreenVolumeX(),
                            file.getRelativeMetadata().getScreenVolumeX(), DISTANCE_PRECISION);
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativePosition().getY(),
                            file.getRelativeMetadata().getRelativePosition().getY(), DISTANCE_PRECISION);
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativeVelocity().getZ(),  
                            file.getRelativeMetadata().getRelativeVelocity().getZ(), DERIVATION_PRECISION);
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbability(),  
                            file.getRelativeMetadata().getCollisionProbability(),1e-30);
        Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbaMethod(),  
                            file.getRelativeMetadata().getCollisionProbaMethod());
        
        // Check Metadata Block
        Assert.assertEquals("OBJECT2" ,                   file.getMetadataObject2().getObject());
        Assert.assertEquals("30337",                      file.getMetadataObject2().getObjectDesignator());
        Assert.assertEquals("SATCAT",                     file.getMetadataObject2().getCatalogName());
        Assert.assertEquals("FENGYUN 1C DEB",             file.getMetadataObject2().getObjectName());
        Assert.assertEquals("1999-025AA",                 file.getMetadataObject2().getInternationalDes());
        Assert.assertEquals("NONE",                       file.getMetadataObject2().getEphemName());
        Assert.assertEquals(CovarianceMethod.CALCULATED,  file.getMetadataObject2().getCovarianceMethod());
        Assert.assertEquals(Maneuvrable.NO,               file.getMetadataObject2().getManeuverable());
        Assert.assertEquals(CelestialBodyFrame.EME2000,   file.getMetadataObject2().getRefFrame().asCelestialBodyFrame());
        Assert.assertEquals("UTC",                        file.getMetadataObject2().getTimeSystem().name());

        // Check data block
        Assert.assertEquals(2569.540800e3 ,  file.getDataObject2().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
        Assert.assertEquals(-2.888612500e3 , file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
        // Covariance Matrix block
        Assert.assertEquals(1.337e3,   file.getDataObject2().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
        Assert.assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
        
        Assert.assertEquals(2.591e-3,  file.getDataObject2().getRTNCovarianceBlock().getCrdotr(), COVARIANCE_PRECISION);
        Assert.assertEquals(6.886e-5,  file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(), COVARIANCE_PRECISION);
        
        Assert.assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(), COVARIANCE_PRECISION);
        Assert.assertEquals(1.059e-5,  file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(), COVARIANCE_PRECISION);
        
        Assert.assertEquals(8.633e-5,  file.getDataObject2().getRTNCovarianceBlock().getCndotn(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(), COVARIANCE_PRECISION);
        
        Assert.assertEquals(-5.117E-01, file.getDataObject2().getRTNCovarianceBlock().getCdrgr(), COVARIANCE_PRECISION);
        Assert.assertEquals(1.319E+00,  file.getDataObject2().getRTNCovarianceBlock().getCdrgt(), COVARIANCE_PRECISION);
        Assert.assertEquals(-1.903E-05, file.getDataObject2().getRTNCovarianceBlock().getCdrgndot(), COVARIANCE_PRECISION);
        Assert.assertEquals(7.402E-05,  file.getDataObject2().getRTNCovarianceBlock().getCdrgtdot(), COVARIANCE_PRECISION);
        Assert.assertEquals(-3.297E+01, file.getDataObject2().getRTNCovarianceBlock().getCsrpr(), COVARIANCE_PRECISION);
        Assert.assertEquals(8.164E+01,  file.getDataObject2().getRTNCovarianceBlock().getCsrpt(), COVARIANCE_PRECISION);

        // Verify comments
        Assert.assertEquals("[Relative Metadata/Data]", file.getRelativeMetadata().getComment().toString());
        Assert.assertEquals("[Object1 Metadata]", file.getMetadataObject1().getComments().toString());
        Assert.assertEquals("[Object2 Metadata]", file.getMetadataObject2().getComments().toString());
        Assert.assertEquals("[Object1 Data]", file.getDataObject1().getComments().toString());
        Assert.assertEquals("[Object1 OD Parameters]", file.getDataObject1().getODParametersBlock().getComments().toString());
        Assert.assertEquals("[Object2 OD Parameters]", file.getDataObject2().getODParametersBlock().getComments().toString());
        Assert.assertEquals("[Object1 Additional Parameters, Apogee Altitude=779 km, Perigee Altitude=765 km, Inclination=86.4 deg]",
                            file.getDataObject1().getAdditionalParametersBlock().getComments().toString());
        Assert.assertEquals("[Object2 Additional Parameters, Apogee Altitude=786 km, Perigee Altitude=414 km, Inclination=98.9 deg]",
                            file.getDataObject2().getAdditionalParametersBlock().getComments().toString());

    }
    
    
    @Test
    public void testParseXML_CDM1() {
    	/** Test for CdmExample1.xml, with only required data. */
    	// File
        final String ex = "/ccsds/cdm/CDMExample1.xml";

        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);
        
        Assert.assertEquals(IERSConventions.IERS_2010, file.getConventions());
        Assert.assertEquals(DataContext.getDefault(),  file.getDataContext());

        // Check Header Block
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assert.assertEquals(new AbsoluteDate(2010, 3, 12, 22, 31, 12.000,
                            TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assert.assertEquals("JSPOC", file.getHeader().getOriginator());
        Assert.assertEquals("SATELLITE A", file.getHeader().getMessageFor());
        Assert.assertEquals("20111371985", file.getHeader().getMessageId());

        // OBJECT1
        // Check Relative Metadata Block
         Assert.assertEquals(new AbsoluteDate(2010, 3, 13, 22, 37, 52.618,
                             TimeScalesFactory.getUTC()),
                             file.getRelativeMetadata().getTca());
         Assert.assertEquals(715.0,     file.getRelativeMetadata().getMissDistance(),     DISTANCE_PRECISION);
         Assert.assertEquals(14762.0,   file.getRelativeMetadata().getRelativeSpeed(),    DERIVATION_PRECISION);
         Assert.assertEquals(27.4,      file.getRelativeMetadata().getRelativePosition().getX(),DISTANCE_PRECISION);
         Assert.assertEquals(-70.2,     file.getRelativeMetadata().getRelativePosition().getY(),DISTANCE_PRECISION);
         Assert.assertEquals(711.8,     file.getRelativeMetadata().getRelativePosition().getZ(),DISTANCE_PRECISION);
         Assert.assertEquals(-7.2,      file.getRelativeMetadata().getRelativeVelocity().getX(),DERIVATION_PRECISION);
         Assert.assertEquals(-14692.0,  file.getRelativeMetadata().getRelativeVelocity().getY(),DERIVATION_PRECISION);
         Assert.assertEquals(-1437.2,   file.getRelativeMetadata().getRelativeVelocity().getZ(),DERIVATION_PRECISION);
         Assert.assertEquals(new AbsoluteDate(2010, 3, 12, 18, 29, 32.212,
                             TimeScalesFactory.getUTC()),
                             file.getRelativeMetadata().getStartScreenPeriod());
         Assert.assertEquals(new AbsoluteDate(2010, 3, 15, 18, 29, 32.212,
                             TimeScalesFactory.getUTC()),
                             file.getRelativeMetadata().getStopScreenPeriod());
         Assert.assertEquals(ScreenVolumeFrame.RTN,        file.getRelativeMetadata().getScreenVolumeFrame());
         Assert.assertEquals(ScreenVolumeShape.ELLIPSOID,  file.getRelativeMetadata().getScreenVolumeShape());
         Assert.assertEquals(200,   file.getRelativeMetadata().getScreenVolumeX(), 0);
         Assert.assertEquals(1000,  file.getRelativeMetadata().getScreenVolumeY(), 0);
         Assert.assertEquals(1000,  file.getRelativeMetadata().getScreenVolumeZ(), 0);
         Assert.assertEquals(new AbsoluteDate(2010, 3, 13, 20, 25, 43.222,
                             TimeScalesFactory.getUTC()),
                             file.getRelativeMetadata().getScreenEntryTime());
         Assert.assertEquals(new AbsoluteDate(2010, 3, 13, 23, 44, 29.324,
                             TimeScalesFactory.getUTC()),
                             file.getRelativeMetadata().getScreenExitTime());
         Assert.assertEquals(4.835E-05,      file.getRelativeMetadata().getCollisionProbability(), 1e-30);
         Assert.assertEquals("FOSTER-1992",  file.getRelativeMetadata().getCollisionProbaMethod().getName());
         Assert.assertEquals(PocMethodType.FOSTER_1992,  file.getRelativeMetadata().getCollisionProbaMethod().getType());

         // Check Metadata Block
         Assert.assertEquals("OBJECT1",                    file.getMetadataObject1().getObject());
         Assert.assertEquals("12345",                      file.getMetadataObject1().getObjectDesignator());
         Assert.assertEquals("SATCAT",                     file.getMetadataObject1().getCatalogName());
         Assert.assertEquals("SATELLITE A",                file.getMetadataObject1().getObjectName());
         Assert.assertEquals("1997-030E",                  file.getMetadataObject1().getInternationalDes());
         Assert.assertEquals("EPHEMERIS SATELLITE A",      file.getMetadataObject1().getEphemName());
         Assert.assertEquals(CovarianceMethod.CALCULATED,  file.getMetadataObject1().getCovarianceMethod());
         Assert.assertEquals(Maneuvrable.YES,              file.getMetadataObject1().getManeuverable());
         Assert.assertEquals(CelestialBodyFrame.EME2000,   file.getMetadataObject1().getRefFrame().asCelestialBodyFrame());
         Assert.assertEquals("UTC",                        file.getMetadataObject1().getTimeSystem().name());
         
         
         // Check data block
         // State vector block
         Assert.assertEquals(2570.097065e3,  file.getDataObject1().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
         Assert.assertEquals(2244.654904e3,  file.getDataObject1().getStateVectorBlock().getPositionVector().getY(), DISTANCE_PRECISION);
         Assert.assertEquals(6281.497978e3,  file.getDataObject1().getStateVectorBlock().getPositionVector().getZ(), DISTANCE_PRECISION);
         Assert.assertEquals(4.418769571e3,  file.getDataObject1().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
         Assert.assertEquals(4.833547743e3,  file.getDataObject1().getStateVectorBlock().getVelocityVector().getY(), DERIVATION_PRECISION);
         Assert.assertEquals(-3.526774282e3, file.getDataObject1().getStateVectorBlock().getVelocityVector().getZ(), DERIVATION_PRECISION);
         // Covariance Matrix block
         Assert.assertEquals(4.142e1,  file.getDataObject1().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
         Assert.assertEquals(-8.579,   file.getDataObject1().getRTNCovarianceBlock().getCtr(), COVARIANCE_DIAG_PRECISION);
         Assert.assertEquals(2.533e3,  file.getDataObject1().getRTNCovarianceBlock().getCtt(), COVARIANCE_DIAG_PRECISION);
         Assert.assertEquals(-2.313e1, file.getDataObject1().getRTNCovarianceBlock().getCnr(), COVARIANCE_DIAG_PRECISION);
         Assert.assertEquals(1.336e1,  file.getDataObject1().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
         Assert.assertEquals(7.098e1,  file.getDataObject1().getRTNCovarianceBlock().getCnn(), COVARIANCE_DIAG_PRECISION);
         
         Assert.assertEquals(2.520e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotr(), COVARIANCE_PRECISION);
         Assert.assertEquals(-5.476,   file.getDataObject1().getRTNCovarianceBlock().getCrdott(), COVARIANCE_PRECISION);
         Assert.assertEquals(8.626e-4, file.getDataObject1().getRTNCovarianceBlock().getCrdotn(), COVARIANCE_PRECISION);
         Assert.assertEquals(5.744e-3, file.getDataObject1().getRTNCovarianceBlock().getCrdotrdot(), COVARIANCE_PRECISION);
         
         Assert.assertEquals(-1.006e-2, file.getDataObject1().getRTNCovarianceBlock().getCtdotr(), COVARIANCE_PRECISION);
         Assert.assertEquals(4.041e-3,  file.getDataObject1().getRTNCovarianceBlock().getCtdott(), COVARIANCE_PRECISION);
         Assert.assertEquals(-1.359e-3, file.getDataObject1().getRTNCovarianceBlock().getCtdotn(), COVARIANCE_PRECISION);
         Assert.assertEquals(-1.502e-5, file.getDataObject1().getRTNCovarianceBlock().getCtdotrdot(), COVARIANCE_PRECISION);
         Assert.assertEquals(1.049e-5,  file.getDataObject1().getRTNCovarianceBlock().getCtdottdot(), COVARIANCE_PRECISION);
         
         Assert.assertEquals(1.053e-3,  file.getDataObject1().getRTNCovarianceBlock().getCndotr(), COVARIANCE_PRECISION);
         Assert.assertEquals(-3.412e-3, file.getDataObject1().getRTNCovarianceBlock().getCndott(), COVARIANCE_PRECISION);
         Assert.assertEquals(1.213e-2,  file.getDataObject1().getRTNCovarianceBlock().getCndotn(), COVARIANCE_PRECISION);
         Assert.assertEquals(-3.004e-6, file.getDataObject1().getRTNCovarianceBlock().getCndotrdot(), COVARIANCE_PRECISION);
         Assert.assertEquals(-1.091e-6, file.getDataObject1().getRTNCovarianceBlock().getCndottdot(), COVARIANCE_PRECISION);
         Assert.assertEquals(5.529e-5,  file.getDataObject1().getRTNCovarianceBlock().getCndotndot(), COVARIANCE_PRECISION);
         
         // OBJECT2
         // Check Relative Metadata Block
         Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getScreenVolumeX(),
                             file.getRelativeMetadata().getScreenVolumeX(), DISTANCE_PRECISION);
         Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativePosition().getZ(),
                             file.getRelativeMetadata().getRelativePosition().getZ(), DISTANCE_PRECISION);
         Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getRelativeVelocity().getZ(),  
                             file.getRelativeMetadata().getRelativeVelocity().getZ(), DERIVATION_PRECISION);
         Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbability(),  
                             file.getRelativeMetadata().getCollisionProbability(),1e-30);
         Assert.assertEquals(file.getSegments().get(1).getMetadata().getRelativeMetadata().getCollisionProbaMethod(),  
                             file.getRelativeMetadata().getCollisionProbaMethod());
         
         // Check Metadata Block
         Assert.assertEquals("OBJECT2",                    file.getMetadataObject2().getObject());
         Assert.assertEquals("30337",                      file.getMetadataObject2().getObjectDesignator());
         Assert.assertEquals("SATCAT",                     file.getMetadataObject2().getCatalogName());
         Assert.assertEquals("FENGYUN 1C DEB",             file.getMetadataObject2().getObjectName());
         Assert.assertEquals("1999-025AA",                 file.getMetadataObject2().getInternationalDes());
         Assert.assertEquals("NONE",                       file.getMetadataObject2().getEphemName());
         Assert.assertEquals(CovarianceMethod.CALCULATED,  file.getMetadataObject2().getCovarianceMethod());
         Assert.assertEquals(Maneuvrable.NO,               file.getMetadataObject2().getManeuverable());
         Assert.assertEquals(CelestialBodyFrame.EME2000,   file.getMetadataObject2().getRefFrame().asCelestialBodyFrame());
         Assert.assertEquals("UTC",                        file.getMetadataObject2().getTimeSystem().name());

         // Check data block
         Assert.assertEquals(2569.540800e3,  file.getDataObject2().getStateVectorBlock().getPositionVector().getX(), DISTANCE_PRECISION);
         Assert.assertEquals(2245.093614e3,  file.getDataObject2().getStateVectorBlock().getPositionVector().getY(), DISTANCE_PRECISION);
         Assert.assertEquals(6281.599946e3,  file.getDataObject2().getStateVectorBlock().getPositionVector().getZ(), DISTANCE_PRECISION);
         Assert.assertEquals(-2.888612500e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getX(), DERIVATION_PRECISION);
         Assert.assertEquals(-6.007247516e3, file.getDataObject2().getStateVectorBlock().getVelocityVector().getY(), DERIVATION_PRECISION);
         Assert.assertEquals(3.328770172e3,  file.getDataObject2().getStateVectorBlock().getVelocityVector().getZ(), DERIVATION_PRECISION);
         // Covariance Matrix block
         Assert.assertEquals(1.337e3,   file.getDataObject2().getRTNCovarianceBlock().getCrr(), COVARIANCE_DIAG_PRECISION);
         Assert.assertEquals(-4.806e4,  file.getDataObject2().getRTNCovarianceBlock().getCtr(), COVARIANCE_DIAG_PRECISION);
         Assert.assertEquals(2.492e6,   file.getDataObject2().getRTNCovarianceBlock().getCtt(), COVARIANCE_DIAG_PRECISION);
         Assert.assertEquals(-3.298e1,  file.getDataObject2().getRTNCovarianceBlock().getCnr(), COVARIANCE_DIAG_PRECISION);
         Assert.assertEquals(-7.5888e2, file.getDataObject2().getRTNCovarianceBlock().getCnt(), COVARIANCE_DIAG_PRECISION);
         Assert.assertEquals(7.105e1,   file.getDataObject2().getRTNCovarianceBlock().getCnn(), COVARIANCE_DIAG_PRECISION);

         Assert.assertEquals(2.591e-3,  file.getDataObject2().getRTNCovarianceBlock().getCrdotr(), COVARIANCE_PRECISION);
         Assert.assertEquals(-4.152e-2, file.getDataObject2().getRTNCovarianceBlock().getCrdott(), COVARIANCE_PRECISION);
         Assert.assertEquals(-1.784e-6, file.getDataObject2().getRTNCovarianceBlock().getCrdotn(), COVARIANCE_PRECISION);
         Assert.assertEquals(6.886e-5,  file.getDataObject2().getRTNCovarianceBlock().getCrdotrdot(), COVARIANCE_PRECISION);
         
         Assert.assertEquals(-1.016e-2, file.getDataObject2().getRTNCovarianceBlock().getCtdotr(), COVARIANCE_PRECISION);
         Assert.assertEquals(-1.506e-4, file.getDataObject2().getRTNCovarianceBlock().getCtdott(), COVARIANCE_PRECISION);
         Assert.assertEquals(1.637e-3,  file.getDataObject2().getRTNCovarianceBlock().getCtdotn(), COVARIANCE_PRECISION);
         Assert.assertEquals(-2.987e-6, file.getDataObject2().getRTNCovarianceBlock().getCtdotrdot(), COVARIANCE_PRECISION);
         Assert.assertEquals(1.059e-5,  file.getDataObject2().getRTNCovarianceBlock().getCtdottdot(), COVARIANCE_PRECISION);
         
         Assert.assertEquals(4.400e-3,  file.getDataObject2().getRTNCovarianceBlock().getCndotr(), COVARIANCE_PRECISION);
         Assert.assertEquals(8.482e-3,  file.getDataObject2().getRTNCovarianceBlock().getCndott(), COVARIANCE_PRECISION);
         Assert.assertEquals(8.633e-5,  file.getDataObject2().getRTNCovarianceBlock().getCndotn(), COVARIANCE_PRECISION);
         Assert.assertEquals(-1.903e-6, file.getDataObject2().getRTNCovarianceBlock().getCndotrdot(), COVARIANCE_PRECISION);
         Assert.assertEquals(-4.594e-6, file.getDataObject2().getRTNCovarianceBlock().getCndottdot(), COVARIANCE_PRECISION);
         Assert.assertEquals(5.178e-5,  file.getDataObject2().getRTNCovarianceBlock().getCndotndot(), COVARIANCE_PRECISION);

         // Check relative metadata comments for Object1
         ArrayList<String> relativeMetadataComment = new ArrayList<String>();
         relativeMetadataComment.add("Relative Metadata/Data");
         Assert.assertEquals(relativeMetadataComment, file.getRelativeMetadata().getComment());

         // Check metadata comments for Object1
         ArrayList<String> MetadataComment = new ArrayList<String>();
         MetadataComment.add("Object1 Metadata");
         Assert.assertEquals(MetadataComment, file.getMetadataObject1().getComments());

         // Check data general comments and OD parameters comments for Object1
         ArrayList<String> generalComment = new ArrayList<String>();
         generalComment.add("Object1 Data");
         Assert.assertEquals(generalComment, file.getDataObject1().getComments());

         // Check additional parameters comments Object1
         ArrayList<String> addParametersComment = new ArrayList<String>();
         addParametersComment.add("Object 1 Additional Parameters");
         Assert.assertEquals(addParametersComment, file.getDataObject1().getAdditionalParametersBlock().getComments());

         // Check state vector comments Object1
         ArrayList<String> stateVectorComment = new ArrayList<String>();
         stateVectorComment.add("Object1 State Vector");
         Assert.assertEquals(stateVectorComment, file.getDataObject1().getStateVectorBlock().getComments());

         // Check RTN covariance comments Object1
         ArrayList<String> RTNComment = new ArrayList<String>();
         RTNComment.add("Object1 Covariance in the RTN Coordinate Frame");
         Assert.assertEquals(RTNComment, file.getDataObject1().getRTNCovarianceBlock().getComments());
         
         
         // Check general comments Object2
         ArrayList<String> generalCommentObj2AddParam = new ArrayList<String>();
         generalCommentObj2AddParam.add("Object2 Additional Parameters");
         generalCommentObj2AddParam.add("Apogee Altitude=768 km, Perigee Altitude=414 km, Inclination=98.8 deg");
         Assert.assertEquals(generalCommentObj2AddParam.toString(), file.getDataObject2().getAdditionalParametersBlock().getComments().toString());
    
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
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals(wrongName, oe.getParts()[0]);
        }
    }
    
    @Test
    public void testMissingTCA() throws URISyntaxException {
        final String name = "/ccsds/cdm/CDM-missing-TCA.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            buildCdmParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assert.assertEquals("TCA", oe.getParts()[0]);
        }
    }
    
    @Test
    public void testMissingObj2StateVectorX() throws URISyntaxException {
        final String name = "/ccsds/cdm/CDM-missing-object2-state-vector.txt";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            buildCdmParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assert.assertEquals("X", oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingObj1CovarianceCNDOT_NDOT() throws URISyntaxException {
        final String name = "/ccsds/cdm/CDM-missing-object1-covariance-block.xml";
        try {
            final DataSource source = new DataSource(name, () -> getClass().getResourceAsStream(name));
            new ParserBuilder().
            buildCdmParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assert.assertEquals("CR_R", oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingObj1OrbitCenterAsk_getFrame() throws URISyntaxException {
        final String ex = "/ccsds/cdm/CDM-no-orbit-center-defined-obj1.txt";
        
        // Initialize the parser
        final CdmParser parser = new ParserBuilder().buildCdmParser();

        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

        // Generated CDM file
        final Cdm file = parser.parseMessage(source);
        try {
            file.getMetadataObject1().getFrame();
            Assert.fail("Expected Exception");
        } catch (OrekitException e){
            Assert.assertEquals(e.getSpecifier(), OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY);
        }
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
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("CRDOT_T", oe.getParts()[0]);
            Assert.assertEquals(65, oe.getParts()[1]);
        }
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
	    Assert.assertEquals(Maneuvrable.N_A,              file.getMetadataObject1().getManeuverable());    
	    // OBJECT2
	    Assert.assertEquals(Maneuvrable.NO,               file.getMetadataObject2().getManeuverable());
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
	    Assert.assertEquals(Maneuvrable.N_A,              file.getMetadataObject1().getManeuverable());    
	    // OBJECT2
	    Assert.assertEquals(Maneuvrable.NO,               file.getMetadataObject2().getManeuverable());
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
        Assert.assertEquals(3, file.getDataObject1().getAdditionalParametersBlock().getAreaDRG(), 0.0);
        
        // Check AREA_SRP
        Assert.assertEquals(10, file.getDataObject1().getAdditionalParametersBlock().getAreaSRP(), 0.0);
    }
}
