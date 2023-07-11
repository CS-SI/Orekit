/* Copyright 2002-2012 Space Applications Services
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
package org.orekit.files.ilrs;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ilrs.CPF.CPFCoordinate;
import org.orekit.files.ilrs.CPF.CPFEphemeris;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;


public class CPFParserTest {

    @Test
    public void testJason3Version2() throws URISyntaxException, IOException {

        // Simple test for version 2.0, only contains position entries
        final String ex = "/ilrs/jason3_cpf_180613_16401.cne";

        final CPFParser parser = new CPFParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final CPF file = (CPF) parser.parse(new DataSource(fileName));

        // Start date
        final AbsoluteDate start = new AbsoluteDate("2018-06-13T00:00:00.000", file.getTimeScale());

        // End date
        final AbsoluteDate end   = new AbsoluteDate("2018-06-18T00:00:00.000", file.getTimeScale());

        // Verify comments
        final List<String> comments = file.getComments();
        Assertions.assertEquals(8, comments.size());
        Assertions.assertEquals("Col 1 : <Record type=10)>", comments.get(0));
        Assertions.assertEquals("Col 8 : <Geocentric Z position in meters>", comments.get(7));

        // Verify header
        final CPFHeader header = file.getHeader();
        Assertions.assertEquals("CPF",    header.getFormat());
        Assertions.assertEquals(2,        header.getVersion());
        Assertions.assertEquals("CNE",    header.getSource());
        Assertions.assertEquals(2018,     header.getProductionEpoch().getYear());
        Assertions.assertEquals(6,        header.getProductionEpoch().getMonth());
        Assertions.assertEquals(13,       header.getProductionEpoch().getDay());
        Assertions.assertEquals(6,        header.getProductionHour());
        Assertions.assertEquals(164,      header.getSequenceNumber());
        Assertions.assertEquals(1,        header.getSubDailySequenceNumber());
        Assertions.assertEquals("jason3", header.getName());
        Assertions.assertEquals("1600201", header.getIlrsSatelliteId());
        Assertions.assertEquals("4379",   header.getSic());
        Assertions.assertEquals("41240",  header.getNoradId());
        Assertions.assertEquals(240,      header.getStep());
        Assertions.assertEquals(1,        header.getTargetClass());
        Assertions.assertEquals(0,        header.getRotationalAngleType());
        Assertions.assertEquals(1,        header.getTargetLocation());
        Assertions.assertTrue(header.isCompatibleWithTIVs());
        Assertions.assertFalse(header.isCenterOfMassCorrectionApplied());
        Assertions.assertEquals(0.0, start.durationFrom(header.getStartEpoch()), 1.0e-15);
        Assertions.assertEquals(0.0, end.durationFrom(header.getEndEpoch()),     1.0e-15);
        Assertions.assertEquals(FramesFactory.getITRF(IERSConventions.IERS_2010, false), header.getRefFrame());
        Assertions.assertEquals(0,        header.getRefFrameId());

        // Coordinates
        final CPFEphemeris ephemeris    = file.getSatellites().get("1600201");
        final List<CPFCoordinate> coord = ephemeris.getCoordinates();

        // Verify first coordinate
        final AbsoluteDate firstEpoch = AbsoluteDate.createMJDDate(58282, 0.0, file.getTimeScale());
        final Vector3D firstPos = new Vector3D(6566174.663, 2703003.220, -3022783.901);
        Assertions.assertEquals(0, coord.get(0).getLeap());
        Assertions.assertEquals(0.0, firstPos.distance(coord.get(0).getPosition()), 1.0e-15);
        Assertions.assertEquals(0.0, firstEpoch.durationFrom(coord.get(0).getDate()), 1.0e-15);

        // Verify last coordinate
        final AbsoluteDate lastEpoch = AbsoluteDate.createMJDDate(58287, 0.0, file.getTimeScale());
        final Vector3D lastPos  = new Vector3D(6045281.907, 1607181.391, -4519215.355);
        Assertions.assertEquals(0, coord.get(coord.size() - 1).getLeap());
        Assertions.assertEquals(0.0, lastPos.distance(coord.get(coord.size() - 1).getPosition()), 1.0e-15);
        Assertions.assertEquals(0.0, lastEpoch.durationFrom(coord.get(coord.size() - 1).getDate()), 1.0e-15);

        // Verify Ephemeris
        Assertions.assertEquals(ephemeris.getFrame(),     header.getRefFrame());
        Assertions.assertEquals(10, ephemeris.getInterpolationSamples());
        Assertions.assertEquals(CartesianDerivativesFilter.USE_P, ephemeris.getAvailableDerivatives());
        Assertions.assertEquals(Constants.EIGEN5C_EARTH_MU,       ephemeris.getMu(), 1.0e-15);
        Assertions.assertEquals(ephemeris,                        ephemeris.getSegments().get(0));
        Assertions.assertEquals(0.0, start.durationFrom(ephemeris.getStart()), 1.0e-15);
        Assertions.assertEquals(0.0, end.durationFrom(ephemeris.getStop()), 1.0e-15);
        Assertions.assertEquals(0.0, firstPos.distance(ephemeris.getEphemeridesDataLines().get(0).getPosition()), 1.0e-15);
        Assertions.assertEquals(0.0, lastPos.distance(ephemeris.getEphemeridesDataLines().get(ephemeris.getEphemeridesDataLines().size() - 1).getPosition()), 1.0e-15);

        // Verify propagator
        final BoundedPropagator propagator = ephemeris.getPropagator(new FrameAlignedProvider(ephemeris.getInertialFrame()));
        // 10 0 58283  56640.000000  0      -1578630.043      -2922293.651      -6964482.056
        final AbsoluteDate date = AbsoluteDate.createMJDDate(58283, 56640.000000, file.getTimeScale());
        final Vector3D position = new Vector3D(-1578630.043, -2922293.651, -6964482.056);
        Assertions.assertEquals(0.0, position.distance(propagator.getPosition(date, ephemeris.getFrame())), 2.4e-10);

    }

    @Test
    public void testLageos1Version2() throws URISyntaxException, IOException {

        // Simple test for version 2.0, only contains position entries
        final String    ex     = "/ilrs/lageos1_cpf_180613_16401.hts";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final CPF   file   = new CPFParser().parse(source);

        // Start date
        final AbsoluteDate start = new AbsoluteDate("2018-06-13T00:00:00.000", file.getTimeScale());

        // End date
        final AbsoluteDate end   = new AbsoluteDate("2018-06-15T00:00:00.000", file.getTimeScale());

        // Verify comments
        final List<String> comments = file.getComments();
        Assertions.assertEquals(0, comments.size());

        // Verify header
        final CPFHeader header = file.getHeader();
        Assertions.assertEquals("CPF",     header.getFormat());
        Assertions.assertEquals(2,         header.getVersion());
        Assertions.assertEquals("HTS",     header.getSource());
        Assertions.assertEquals(2018,      header.getProductionEpoch().getYear());
        Assertions.assertEquals(6,         header.getProductionEpoch().getMonth());
        Assertions.assertEquals(13,        header.getProductionEpoch().getDay());
        Assertions.assertEquals(12,        header.getProductionHour());
        Assertions.assertEquals(164,       header.getSequenceNumber());
        Assertions.assertEquals(1,         header.getSubDailySequenceNumber());
        Assertions.assertEquals("lageos1", header.getName());
        Assertions.assertEquals("7603901",   header.getIlrsSatelliteId());
        Assertions.assertEquals("1155",      header.getSic());
        Assertions.assertEquals("8820",      header.getNoradId());
        Assertions.assertEquals(300,       header.getStep());
        Assertions.assertEquals(1,         header.getTargetClass());
        Assertions.assertEquals(0,         header.getRotationalAngleType());
        Assertions.assertEquals(1,         header.getTargetLocation());
        Assertions.assertTrue(header.isCompatibleWithTIVs());
        Assertions.assertFalse(header.isCenterOfMassCorrectionApplied());
        Assertions.assertEquals(0.0,    start.durationFrom(header.getStartEpoch()), 1.0e-15);
        Assertions.assertEquals(0.0,    end.durationFrom(header.getEndEpoch()),     1.0e-15);
        Assertions.assertEquals(0.2510, header.getCenterOfMassOffset(),             1.0e-15);
        Assertions.assertEquals(FramesFactory.getITRF(IERSConventions.IERS_2010, false), header.getRefFrame());
        Assertions.assertEquals(0,        header.getRefFrameId());

        // Coordinates
        final CPFEphemeris ephemeris    = file.getSatellites().get("7603901");
        final List<CPFCoordinate> coord = ephemeris.getCoordinates();

        // Verify first coordinate
        final AbsoluteDate firstEpoch = AbsoluteDate.createMJDDate(58281, 84600.00000, file.getTimeScale());
        final Vector3D firstPos = new Vector3D(2966379.904, 4195129.466, -11136763.061);
        Assertions.assertEquals(0, coord.get(0).getLeap());
        Assertions.assertEquals(0.0, firstPos.distance(coord.get(0).getPosition()), 1.0e-15);
        Assertions.assertEquals(0.0, firstEpoch.durationFrom(coord.get(0).getDate()), 1.0e-15);

        // Verify last coordinate
        final AbsoluteDate lastEpoch = AbsoluteDate.createMJDDate(58283, 86100.00000, file.getTimeScale());
        final Vector3D lastPos  = new Vector3D(-5292229.761, 4106329.723, -10235338.181);
        Assertions.assertEquals(0, coord.get(coord.size() - 1).getLeap());
        Assertions.assertEquals(0.0, lastPos.distance(coord.get(coord.size() - 1).getPosition()), 1.0e-15);
        Assertions.assertEquals(0.0, lastEpoch.durationFrom(coord.get(coord.size() - 1).getDate()), 1.0e-15);

        // Verify Ephemeris
        Assertions.assertEquals(ephemeris.getFrame(),     header.getRefFrame());
        Assertions.assertEquals(10, ephemeris.getInterpolationSamples());
        Assertions.assertEquals(CartesianDerivativesFilter.USE_P, ephemeris.getAvailableDerivatives());
        Assertions.assertEquals(Constants.EIGEN5C_EARTH_MU,       ephemeris.getMu(), 1.0e-15);
        Assertions.assertEquals(ephemeris,                        ephemeris.getSegments().get(0));
        Assertions.assertEquals(0.0, firstPos.distance(ephemeris.getEphemeridesDataLines().get(0).getPosition()), 1.0e-15);
        Assertions.assertEquals(0.0, lastPos.distance(ephemeris.getEphemeridesDataLines().get(ephemeris.getEphemeridesDataLines().size() - 1).getPosition()), 1.0e-15);

        // Verify propagator
        final BoundedPropagator propagator = ephemeris.getPropagator();
        // 10 0 58282  78000.00000  0   -5843276.537    1074212.914  -10696380.103
        final AbsoluteDate date = AbsoluteDate.createMJDDate(58282, 78000.00000, file.getTimeScale());
        final Vector3D position = new Vector3D(-5843276.537, 1074212.914, -10696380.103);
        Assertions.assertEquals(0.0, position.distance(propagator.getPosition(date, ephemeris.getFrame())), 2.0e-9);

    }

    @Test
    public void testGalileoVersion1() throws URISyntaxException, IOException {

        // Simple test for version 1.0, only contains position entries
        final String    ex     = "/ilrs/galileo212_cpf_180613_6641.esa";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final CPF   file   = new CPFParser().parse(source);

        // Start date
        final AbsoluteDate start = new AbsoluteDate("2018-06-12T23:59:42.000", file.getTimeScale());

        // End date
        final AbsoluteDate end   = new AbsoluteDate("2018-06-14T23:59:42.000", file.getTimeScale());

        // Verify comments
        final List<String> comments = file.getComments();
        Assertions.assertEquals(0, comments.size());

        // Verify header
        final CPFHeader header = file.getHeader();
        Assertions.assertEquals("CPF",        header.getFormat());
        Assertions.assertEquals(1,            header.getVersion());
        Assertions.assertEquals("ESA",        header.getSource());
        Assertions.assertEquals(2018,         header.getProductionEpoch().getYear());
        Assertions.assertEquals(6,            header.getProductionEpoch().getMonth());
        Assertions.assertEquals(13,           header.getProductionEpoch().getDay());
        Assertions.assertEquals(10,           header.getProductionHour());
        Assertions.assertEquals(6641,         header.getSequenceNumber());
        Assertions.assertEquals("galileo212", header.getName());
        Assertions.assertEquals("1606902",    header.getIlrsSatelliteId());
        Assertions.assertEquals("7212",       header.getSic());
        Assertions.assertEquals("41860",      header.getNoradId());
        Assertions.assertEquals(900,          header.getStep());
        Assertions.assertEquals(1,            header.getTargetClass());
        Assertions.assertEquals(0,            header.getRotationalAngleType());
        Assertions.assertTrue(header.isCompatibleWithTIVs());
        Assertions.assertFalse(header.isCenterOfMassCorrectionApplied());
        Assertions.assertEquals(0.0,    start.durationFrom(header.getStartEpoch()), 1.0e-15);
        Assertions.assertEquals(0.0,    end.durationFrom(header.getEndEpoch()),     1.0e-15);
        Assertions.assertEquals(FramesFactory.getITRF(IERSConventions.IERS_2010, false), header.getRefFrame());
        Assertions.assertEquals(0,        header.getRefFrameId());

        // Coordinates
        final CPFEphemeris ephemeris    = file.getSatellites().get("1606902");
        final List<CPFCoordinate> coord = ephemeris.getCoordinates();

        // Verify first coordinate
        final AbsoluteDate firstEpoch = AbsoluteDate.createMJDDate(58281, 86382.000000, file.getTimeScale());
        final Vector3D firstPos = new Vector3D(-3442706.377, 29234902.063, 3170080.159);
        Assertions.assertEquals(0, coord.get(0).getLeap());
        Assertions.assertEquals(0.0, firstPos.distance(coord.get(0).getPosition()), 1.0e-15);
        Assertions.assertEquals(0.0, firstEpoch.durationFrom(coord.get(0).getDate()), 1.0e-15);

        // Verify last coordinate
        final AbsoluteDate lastEpoch = AbsoluteDate.createMJDDate(58283, 86382.00000, file.getTimeScale());
        final Vector3D lastPos  = new Vector3D(-7329586.488, -24111259.078, -15507306.979);
        Assertions.assertEquals(0, coord.get(coord.size() - 1).getLeap());
        Assertions.assertEquals(0.0, lastPos.distance(coord.get(coord.size() - 1).getPosition()), 1.0e-15);
        Assertions.assertEquals(0.0, lastEpoch.durationFrom(coord.get(coord.size() - 1).getDate()), 1.0e-15);

        // Verify Ephemeris
        Assertions.assertEquals(ephemeris.getFrame(),     header.getRefFrame());
        Assertions.assertEquals(10, ephemeris.getInterpolationSamples());
        Assertions.assertEquals(CartesianDerivativesFilter.USE_P, ephemeris.getAvailableDerivatives());
        Assertions.assertEquals(Constants.EIGEN5C_EARTH_MU,       ephemeris.getMu(), 1.0e-15);
        Assertions.assertEquals(ephemeris,                        ephemeris.getSegments().get(0));
        Assertions.assertEquals(0.0, firstPos.distance(ephemeris.getEphemeridesDataLines().get(0).getPosition()), 1.0e-15);
        Assertions.assertEquals(0.0, lastPos.distance(ephemeris.getEphemeridesDataLines().get(ephemeris.getEphemeridesDataLines().size() - 1).getPosition()), 1.0e-15);

        // Verify propagator
        final BoundedPropagator propagator = ephemeris.getPropagator(new FrameAlignedProvider(ephemeris.getInertialFrame()));
        // 10 0 58282  78282.000000  0      22173889.124     -19259262.865       3650461.090
        final AbsoluteDate date = AbsoluteDate.createMJDDate(58282, 78282.000000, file.getTimeScale());
        final Vector3D position = new Vector3D(22173889.124, -19259262.865, 3650461.090);
        Assertions.assertEquals(0.0, position.distance(propagator.getPosition(date, ephemeris.getFrame())), 4.7e-10);

    }

    @Test
    public void testAllFields() throws URISyntaxException, IOException {

        // Simple test for version 2.0, only contains position entries
        final String    ex     = "/ilrs/cpf_all_fields.csg";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final CPF   file   = new CPFParser().parse(source);

        // Verify comments
        final List<String> comments = file.getComments();
        Assertions.assertEquals(2, comments.size());

        // Verify header
        final CPFHeader header = file.getHeader();
        Assertions.assertEquals("CPF",        header.getFormat());
        Assertions.assertEquals(2,            header.getVersion());
        Assertions.assertEquals("CSG",        header.getSource());
        Assertions.assertEquals(2018,         header.getProductionEpoch().getYear());
        Assertions.assertEquals(6,            header.getProductionEpoch().getMonth());
        Assertions.assertEquals(13,           header.getProductionEpoch().getDay());
        Assertions.assertEquals(10,           header.getProductionHour());
        Assertions.assertEquals(6641,         header.getSequenceNumber());
        Assertions.assertEquals("orekitSat",  header.getName());
        Assertions.assertEquals("1234567",    header.getIlrsSatelliteId());
        Assertions.assertEquals("705",        header.getSic());
        Assertions.assertEquals("99999",      header.getNoradId());
        Assertions.assertEquals(900,          header.getStep());
        Assertions.assertEquals(1,            header.getTargetClass());
        Assertions.assertEquals(0,            header.getRotationalAngleType());
        Assertions.assertEquals(0.0,          header.getPrf(), 0.0);
        Assertions.assertEquals(0.0,          header.getTranspTransmitDelay(), 0.0);
        Assertions.assertEquals(0.0,          header.getTranspUtcOffset(), 0.0);
        Assertions.assertEquals(0.0,          header.getTranspOscDrift(), 0.0);
        Assertions.assertEquals(0.0,          header.getTranspClkRef(), 0.0);

    }

    @Test
    public void testInvalifFormat() throws URISyntaxException, IOException {
        try {
            final String ex = "/ilrs/cpf_invalid_format.csg";
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            new CPFParser().parse(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNEXPECTED_FORMAT_FOR_ILRS_FILE,
                                oe.getSpecifier());
            Assertions.assertEquals("CPF", oe.getParts()[0]);
            Assertions.assertEquals("CDR", oe.getParts()[1]);
        }
    }

    @Test
    public void testMissingEOF() throws IOException, URISyntaxException {
        try {
            final String ex = "/ilrs/cpf_unexpected_end_of_file.csg";
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            new CPFParser().parse(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CPF_UNEXPECTED_END_OF_FILE,
                                oe.getSpecifier());
            Assertions.assertEquals(5, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    public void testCorruptedData() throws IOException, URISyntaxException {
        try {
            final String ex = "/ilrs/cpf_corrupted_data.csg";
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            new CPFParser().parse(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                oe.getSpecifier());
            Assertions.assertEquals(4, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
