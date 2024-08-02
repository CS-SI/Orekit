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

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ilrs.CRD.AnglesMeasurement;
import org.orekit.files.ilrs.CRD.CRDDataBlock;
import org.orekit.files.ilrs.CRD.Calibration;
import org.orekit.files.ilrs.CRD.CalibrationDetail;
import org.orekit.files.ilrs.CRD.FrRangeMeasurement;
import org.orekit.files.ilrs.CRD.Meteo;
import org.orekit.files.ilrs.CRD.MeteorologicalMeasurement;
import org.orekit.files.ilrs.CRD.NptRangeMeasurement;
import org.orekit.files.ilrs.CRD.RangeMeasurement;
import org.orekit.files.ilrs.CRD.RangeSupplement;
import org.orekit.files.ilrs.CRD.SessionStatistics;
import org.orekit.files.ilrs.CRDConfiguration.CalibrationTargetConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.DetectorConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.LaserConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.MeteorologicalConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.SoftwareConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.SystemConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.TimingSystemConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.TransponderConfiguration;
import org.orekit.files.ilrs.CRDHeader.DataType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

class CRDParserTest {

    @Test
    void testInvalidFormat() throws URISyntaxException, IOException {
        try {
            final String ex = "/ilrs/crd_invalid_format.v2C";
            final CRDParser parser = new CRDParser();
            parser.parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNEXPECTED_FORMAT_FOR_ILRS_FILE,
                                oe.getSpecifier());
            assertEquals("CRD", oe.getParts()[0]);
            assertEquals("CPF", oe.getParts()[1]);
        }
    }

    @Test
    void testMissingEOF() throws IOException, URISyntaxException {
        try {
            final String ex = "/ilrs/crd_unexpected_end_of_file.v2C";
            final CRDParser parser = new CRDParser();
            final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
            parser.parse(new DataSource(fileName));
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CRD_UNEXPECTED_END_OF_FILE,
                                oe.getSpecifier());
            assertEquals(23, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    void testCorruptedData() throws IOException, URISyntaxException {
        try {
            final String ex = "/ilrs/crd_corrupted_data.v2C";
            final CRDParser parser = new CRDParser();
            parser.parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                oe.getSpecifier());
            assertEquals(19, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    void testInvalidRangeType() throws IOException, URISyntaxException {
        try {
            final String ex = "/ilrs/crd_invalid_range_type.v2C";
            final CRDParser parser = new CRDParser();
            parser.parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.INVALID_RANGE_INDICATOR_IN_CRD_FILE,
                                oe.getSpecifier());
            assertEquals(9, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    void testLageos2Version2() throws URISyntaxException, IOException {

        // Simple test for version 2.0
        final String ex = "/ilrs/lageos2_201802.npt.v2C";

        final CRDParser parser = new CRDParser();
        final CRD file = parser.parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify first data block
        final CRDDataBlock first = file.getDataBlocks().get(0);
        final CRDHeader firstHeader = first.getHeader();
        final CRDConfiguration firstConf = first.getConfigurationRecords();
        final AbsoluteDate firstStartDate = new AbsoluteDate("2018-02-01T15:14:58.000", TimeScalesFactory.getUTC());
        final AbsoluteDate firstEndDate   = new AbsoluteDate("2018-02-01T15:48:57.000", TimeScalesFactory.getUTC());

        // Header
        assertEquals(2, firstHeader.getVersion());
        assertEquals(2018, firstHeader.getProductionEpoch().getYear());
        assertEquals(2, firstHeader.getProductionEpoch().getMonth());
        assertEquals(1, firstHeader.getProductionEpoch().getDay());
        assertEquals(17, firstHeader.getProductionHour());
        assertEquals("CHAL", firstHeader.getStationName());
        assertEquals(9998, firstHeader.getSystemIdentifier());
        assertEquals(19, firstHeader.getSystemNumber());
        assertEquals(1, firstHeader.getSystemOccupancy());
        assertEquals(4, firstHeader.getEpochIdentifier());
        assertEquals("WPLTN", firstHeader.getStationNetword());
        assertEquals("lageos2", firstHeader.getName());
        assertEquals("9207002", firstHeader.getIlrsSatelliteId());
        assertEquals("5986", firstHeader.getSic());
        assertEquals("22195", firstHeader.getNoradId());
        assertEquals(0, firstHeader.getSpacecraftEpochTimeScale());
        assertEquals(1, firstHeader.getTargetClass());
        assertEquals(1, firstHeader.getTargetLocation());
        assertEquals(1, firstHeader.getDataType());
        assertEquals(0.0, firstHeader.getStartEpoch().durationFrom(firstStartDate), 1.0e-5);
        assertEquals(0.0, firstHeader.getEndEpoch().durationFrom(firstEndDate), 1.0e-5);
        assertEquals(0, firstHeader.getDataReleaseFlag());
        assertFalse(firstHeader.isTroposphericRefractionApplied());
        assertFalse(firstHeader.isCenterOfMassCorrectionApplied());
        assertFalse(firstHeader.isReceiveAmplitudeCorrectionApplied());
        assertTrue(firstHeader.isStationSystemDelayApplied());
        assertFalse(firstHeader.isTransponderDelayApplied());
        assertEquals(2, firstHeader.getRangeType().getIndicator());
        assertEquals(0, firstHeader.getQualityIndicator());
        assertEquals(1, firstHeader.getPredictionType());
        assertEquals(18, firstHeader.getYearOfCentury());
        assertEquals("020115", firstHeader.getDateAndTime());
        assertEquals("hts", firstHeader.getPredictionProvider());
        assertEquals(3202, firstHeader.getSequenceNumber());

        // Configuration records
        assertEquals(532.000 * 1.0e-9, firstConf.getSystemRecord().getWavelength(), 1.0e-15);
        assertEquals("std", firstConf.getSystemRecord().getSystemId());
        assertEquals("CL1", firstConf.getLaserRecord().getLaserId());
        assertEquals("RG30-L", firstConf.getLaserRecord().getLaserType());
        assertEquals(1064.00 * 1.0e-9, firstConf.getLaserRecord().getPrimaryWavelength(), 1.0e-15);
        assertEquals(1000.00, firstConf.getLaserRecord().getNominalFireRate(), 1.0e-15);
        assertEquals(1.50, firstConf.getLaserRecord().getPulseEnergy(), 1.0e-15);
        assertEquals(10.0, firstConf.getLaserRecord().getPulseWidth(), 1.0e-15);
        assertEquals(92.82, firstConf.getLaserRecord().getBeamDivergence(), 1.0e-15);
        assertEquals(0, firstConf.getLaserRecord().getPulseInOutgoingSemiTrain());
        assertEquals("CD1", firstConf.getDetectorRecord().getDetectorId());
        assertEquals("CSPAD", firstConf.getDetectorRecord().getDetectorType());
        assertEquals(532.000 * 1.0e-9, firstConf.getDetectorRecord().getApplicableWavelength(), 1.0e-15);
        assertEquals(20.0, firstConf.getDetectorRecord().getQuantumEfficiency(), 1.0e-15);
        assertEquals(5.0, firstConf.getDetectorRecord().getAppliedVoltage(), 1.0e-15);
        assertEquals(60000.0, firstConf.getDetectorRecord().getDarkCount(), 1.0e-15);
        assertEquals("TTL", firstConf.getDetectorRecord().getOutputPulseType());
        assertEquals(0.0, firstConf.getDetectorRecord().getOutputPulseWidth(), 1.0e-15);
        assertEquals(1.70 * 1.0e-9, firstConf.getDetectorRecord().getSpectralFilter(), 1.0e-15);
        assertEquals(0.0, firstConf.getDetectorRecord().getTransmissionOfSpectralFilter(), 1.0e-15);
        assertEquals(0.0, firstConf.getDetectorRecord().getSpatialFilter(), 1.0e-15);
        assertEquals("none", firstConf.getDetectorRecord().getExternalSignalProcessing());
        assertEquals(0.0, firstConf.getDetectorRecord().getAmplifierGain(), 1.0e-15);
        assertEquals(0.0, firstConf.getDetectorRecord().getAmplifierBandwidth(), 1.0e-15);
        assertEquals("0", firstConf.getDetectorRecord().getAmplifierInUse());
        assertEquals("CT1", firstConf.getTimingRecord().getLocalTimingId());
        assertEquals("Meridian", firstConf.getTimingRecord().getTimeSource());
        assertEquals("Meridian", firstConf.getTimingRecord().getFrequencySource());
        assertEquals("ET-A032", firstConf.getTimingRecord().getTimer());
        assertEquals("003309", firstConf.getTimingRecord().getTimerSerialNumber());
        assertEquals(0.0, firstConf.getTimingRecord().getEpochDelayCorrection(), 1.0e-15);
        assertEquals("pgms", firstConf.getSoftwareRecord().getSoftwareId());
        assertEquals("Sattrk", firstConf.getSoftwareRecord().getTrackingSoftwares()[1]);
        assertEquals("2.00Cm", firstConf.getSoftwareRecord().getTrackingSoftwareVersions()[1]);
        assertEquals("crd_cal", firstConf.getSoftwareRecord().getProcessingSoftwares()[1]);
        assertEquals("1.7", firstConf.getSoftwareRecord().getProcessingSoftwareVersions()[1]);
        assertEquals("mets", firstConf.getMeteorologicalRecord().getMeteorologicalId());
        assertEquals("Paroscientific", firstConf.getMeteorologicalRecord().getPressSensorManufacturer());
        assertEquals("Met4a", firstConf.getMeteorologicalRecord().getPressSensorModel());
        assertEquals("123456", firstConf.getMeteorologicalRecord().getPressSensorSerialNumber());
        assertEquals("Paroscientific", firstConf.getMeteorologicalRecord().getTempSensorManufacturer());
        assertEquals("Met4a", firstConf.getMeteorologicalRecord().getTempSensorModel());
        assertEquals("123456", firstConf.getMeteorologicalRecord().getTempSensorSerialNumber());
        assertEquals("Paroscientific", firstConf.getMeteorologicalRecord().getHumiSensorManufacturer());
        assertEquals("Met4a", firstConf.getMeteorologicalRecord().getHumiSensorModel());
        assertEquals("123456", firstConf.getMeteorologicalRecord().getHumiSensorSerialNumber());
        assertNull(firstConf.getTransponderRecord());

        // Meteorological data (only one data)
        final MeteorologicalMeasurement meteoFirst = first.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH);
        assertEquals(56940.0, meteoFirst.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        assertEquals(0.9989, meteoFirst.getPressure(), 1.0e-15);
        assertEquals(259.10, meteoFirst.getTemperature(), 1.0e-15);
        assertEquals(80.0, meteoFirst.getHumidity(), 1.0e-15);

        // Range data
        final RangeMeasurement rangeFirst1 = first.getRangeData().get(0);
        assertEquals(54927.620161400002, rangeFirst1.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        assertEquals(0.044106029140, rangeFirst1.getTimeOfFlight(), 1.0e-15);
        assertEquals(2, rangeFirst1.getEpochEvent());
        assertEquals(5.7, rangeFirst1.getSnr(), 1.0e-15);
        final RangeMeasurement rangeFirst2 = first.getRangeData().get(5);
        assertEquals(56899.718161400000, rangeFirst2.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        assertEquals(0.050148193335, rangeFirst2.getTimeOfFlight(), 1.0e-15);
        assertEquals(2, rangeFirst2.getEpochEvent());
        assertEquals(5.7, rangeFirst2.getSnr(), 1.0e-15);

        // Angles data
        assertEquals(0, first.getAnglesData().size());

        // Verify last data block
        final CRDDataBlock last = file.getDataBlocks().get(file.getDataBlocks().size() - 1);
        final CRDHeader lastHeader = last.getHeader();
        final CRDConfiguration lastConf = last.getConfigurationRecords();
        final AbsoluteDate lastStartDate = new AbsoluteDate("2018-02-27T14:10:10.000", TimeScalesFactory.getUTC());
        final AbsoluteDate lastEndDate   = new AbsoluteDate("2018-02-27T14:39:06.000", TimeScalesFactory.getUTC());

        // Header
        assertEquals(2, lastHeader.getVersion());
        assertEquals(2018, lastHeader.getProductionEpoch().getYear());
        assertEquals(2, lastHeader.getProductionEpoch().getMonth());
        assertEquals(27, lastHeader.getProductionEpoch().getDay());
        assertEquals(14, lastHeader.getProductionHour());
        assertEquals("CHAL", lastHeader.getStationName());
        assertEquals(9998, lastHeader.getSystemIdentifier());
        assertEquals(19, lastHeader.getSystemNumber());
        assertEquals(1, lastHeader.getSystemOccupancy());
        assertEquals(4, lastHeader.getEpochIdentifier());
        assertEquals("WPLTN", lastHeader.getStationNetword());
        assertEquals("lageos2", lastHeader.getName());
        assertEquals("9207002", lastHeader.getIlrsSatelliteId());
        assertEquals("5986", lastHeader.getSic());
        assertEquals("22195", lastHeader.getNoradId());
        assertEquals(0, lastHeader.getSpacecraftEpochTimeScale());
        assertEquals(1, lastHeader.getTargetClass());
        assertEquals(1, lastHeader.getTargetLocation());
        assertEquals(1, lastHeader.getDataType());
        assertEquals(0.0, lastHeader.getStartEpoch().durationFrom(lastStartDate), 1.0e-5);
        assertEquals(0.0, lastHeader.getEndEpoch().durationFrom(lastEndDate), 1.0e-5);
        assertEquals(0, lastHeader.getDataReleaseFlag());
        assertFalse(lastHeader.isTroposphericRefractionApplied());
        assertFalse(lastHeader.isCenterOfMassCorrectionApplied());
        assertFalse(lastHeader.isReceiveAmplitudeCorrectionApplied());
        assertTrue(lastHeader.isStationSystemDelayApplied());
        assertFalse(lastHeader.isTransponderDelayApplied());
        assertEquals(2, lastHeader.getRangeType().getIndicator());
        assertEquals(0, lastHeader.getQualityIndicator());
        assertEquals(1, lastHeader.getPredictionType());
        assertEquals(18, lastHeader.getYearOfCentury());
        assertEquals("022714", lastHeader.getDateAndTime());
        assertEquals("hts", lastHeader.getPredictionProvider());
        assertEquals(5802, lastHeader.getSequenceNumber());

        // Configuration records
        assertEquals(532.000 * 1.0e-9, lastConf.getSystemRecord().getWavelength(), 1.0e-15);
        assertEquals("std", lastConf.getSystemRecord().getSystemId());
        assertEquals("CL1", lastConf.getLaserRecord().getLaserId());
        assertEquals("RG30-L", lastConf.getLaserRecord().getLaserType());
        assertEquals(1064.00 * 1.0e-9, lastConf.getLaserRecord().getPrimaryWavelength(), 1.0e-15);
        assertEquals(1000.00, lastConf.getLaserRecord().getNominalFireRate(), 1.0e-15);
        assertEquals(1.50, lastConf.getLaserRecord().getPulseEnergy(), 1.0e-15);
        assertEquals(10.0, lastConf.getLaserRecord().getPulseWidth(), 1.0e-15);
        assertEquals(92.82, lastConf.getLaserRecord().getBeamDivergence(), 1.0e-15);
        assertEquals(0, lastConf.getLaserRecord().getPulseInOutgoingSemiTrain());
        assertEquals("CD1", lastConf.getDetectorRecord().getDetectorId());
        assertEquals("CSPAD", lastConf.getDetectorRecord().getDetectorType());
        assertEquals(532.000 * 1.0e-9, lastConf.getDetectorRecord().getApplicableWavelength(), 1.0e-15);
        assertEquals(20.0, lastConf.getDetectorRecord().getQuantumEfficiency(), 1.0e-15);
        assertEquals(5.0, lastConf.getDetectorRecord().getAppliedVoltage(), 1.0e-15);
        assertEquals(60000.0, lastConf.getDetectorRecord().getDarkCount(), 1.0e-15);
        assertEquals("TTL", lastConf.getDetectorRecord().getOutputPulseType());
        assertEquals(0.0, lastConf.getDetectorRecord().getOutputPulseWidth(), 1.0e-15);
        assertEquals(1.70 * 1.0e-9, lastConf.getDetectorRecord().getSpectralFilter(), 1.0e-15);
        assertEquals(0.0, lastConf.getDetectorRecord().getTransmissionOfSpectralFilter(), 1.0e-15);
        assertEquals(0.0, lastConf.getDetectorRecord().getSpatialFilter(), 1.0e-15);
        assertEquals("none", lastConf.getDetectorRecord().getExternalSignalProcessing());
        assertEquals(0.0, lastConf.getDetectorRecord().getAmplifierGain(), 1.0e-15);
        assertEquals(0.0, lastConf.getDetectorRecord().getAmplifierBandwidth(), 1.0e-15);
        assertEquals("0", lastConf.getDetectorRecord().getAmplifierInUse());
        assertEquals("CT1", lastConf.getTimingRecord().getLocalTimingId());
        assertEquals("Meridian", lastConf.getTimingRecord().getTimeSource());
        assertEquals("Meridian", lastConf.getTimingRecord().getFrequencySource());
        assertEquals("ET-A032", lastConf.getTimingRecord().getTimer());
        assertEquals("003309", lastConf.getTimingRecord().getTimerSerialNumber());
        assertEquals(0.0, lastConf.getTimingRecord().getEpochDelayCorrection(), 1.0e-15);
        assertEquals("pgms", lastConf.getSoftwareRecord().getSoftwareId());
        assertEquals("Sattrk", lastConf.getSoftwareRecord().getTrackingSoftwares()[1]);
        assertEquals("2.00Cm", lastConf.getSoftwareRecord().getTrackingSoftwareVersions()[1]);
        assertEquals("crd_cal", lastConf.getSoftwareRecord().getProcessingSoftwares()[1]);
        assertEquals("1.7", lastConf.getSoftwareRecord().getProcessingSoftwareVersions()[1]);
        assertEquals("mets", lastConf.getMeteorologicalRecord().getMeteorologicalId());
        assertEquals("Paroscientific", lastConf.getMeteorologicalRecord().getPressSensorManufacturer());
        assertEquals("Met4a", lastConf.getMeteorologicalRecord().getPressSensorModel());
        assertEquals("123456", lastConf.getMeteorologicalRecord().getPressSensorSerialNumber());
        assertEquals("Paroscientific", lastConf.getMeteorologicalRecord().getTempSensorManufacturer());
        assertEquals("Met4a", lastConf.getMeteorologicalRecord().getTempSensorModel());
        assertEquals("123456", lastConf.getMeteorologicalRecord().getTempSensorSerialNumber());
        assertEquals("Paroscientific", lastConf.getMeteorologicalRecord().getHumiSensorManufacturer());
        assertEquals("Met4a", lastConf.getMeteorologicalRecord().getHumiSensorModel());
        assertEquals("123456", lastConf.getMeteorologicalRecord().getHumiSensorSerialNumber());
        assertNull(lastConf.getTransponderRecord());

        // Meteorological data (only one data)
        final MeteorologicalMeasurement meteoLast = last.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH);
        assertEquals(52749.0, meteoLast.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        assertEquals(0.9921, meteoLast.getPressure(), 1.0e-15);
        assertEquals(260.80, meteoLast.getTemperature(), 1.0e-15);
        assertEquals(67.0, meteoLast.getHumidity(), 1.0e-15);

        // Range data
        final RangeMeasurement rangeLast1 = last.getRangeData().get(0);
        assertEquals(51080.935001603524, rangeLast1.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        assertEquals(0.045673108965, rangeLast1.getTimeOfFlight(), 1.0e-15);
        assertEquals(2, rangeLast1.getEpochEvent());
        assertEquals(5.7, rangeLast1.getSnr(), 1.0e-15);
        final RangeMeasurement rangeLast2 = last.getRangeData().get(13);
        assertEquals(52618.095001597932, rangeLast2.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        assertEquals(0.042733272755, rangeLast2.getTimeOfFlight(), 1.0e-15);
        assertEquals(2, rangeLast2.getEpochEvent());
        assertEquals(5.7, rangeLast2.getSnr(), 1.0e-15);

        // Angles data
        assertEquals(0, first.getAnglesData().size());

    }

    @Test
    void testChampVersion1() throws URISyntaxException, IOException {

        // Simple test for version 1.0
        final String ex = "/ilrs/champ_201709-small.frd";

        final CRDParser parser = new CRDParser();
        final CRD file = parser.parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Data block
        final CRDDataBlock block = file.getDataBlocks().get(0);
        final CRDHeader header = block.getHeader();
        final CRDConfiguration conf = block.getConfigurationRecords();
        final AbsoluteDate startDate = new AbsoluteDate("2017-09-26T03:55:41.000", TimeScalesFactory.getUTC());
        final AbsoluteDate endDate   = new AbsoluteDate("2017-09-26T04:04:48.000", TimeScalesFactory.getUTC());

        // Header
        assertEquals(1, header.getVersion());
        assertEquals(2017, header.getProductionEpoch().getYear());
        assertEquals(9, header.getProductionEpoch().getMonth());
        assertEquals(26, header.getProductionEpoch().getDay());
        assertEquals(4, header.getProductionHour());
        assertEquals("STL3", header.getStationName());
        assertEquals(7825, header.getSystemIdentifier());
        assertEquals(90, header.getSystemNumber());
        assertEquals(1, header.getSystemOccupancy());
        assertEquals(4, header.getEpochIdentifier());
        assertEquals("champ", header.getName());
        assertEquals("0003902", header.getIlrsSatelliteId());
        assertEquals("8002", header.getSic());
        assertEquals("026405", header.getNoradId());
        assertEquals(0, header.getSpacecraftEpochTimeScale());
        assertEquals(1, header.getTargetClass());
        assertEquals(0, header.getDataType());
        assertEquals(0.0, header.getStartEpoch().durationFrom(startDate), 1.0e-5);
        assertEquals(0.0, header.getEndEpoch().durationFrom(endDate), 1.0e-5);
        assertEquals(0, header.getDataReleaseFlag());
        assertFalse(header.isTroposphericRefractionApplied());
        assertFalse(header.isCenterOfMassCorrectionApplied());
        assertFalse(header.isReceiveAmplitudeCorrectionApplied());
        assertTrue(header.isStationSystemDelayApplied());
        assertFalse(header.isTransponderDelayApplied());
        assertEquals(2, header.getRangeType().getIndicator());
        assertEquals(0, header.getQualityIndicator());

        // Configuration records
        assertEquals(532.100 * 1.0e-9, conf.getSystemRecord().getWavelength(), 1.0e-15);
        assertEquals("IDAA", conf.getSystemRecord().getSystemId());
        assertEquals("IDAB", conf.getLaserRecord().getLaserId());
        assertEquals("Nd-YAG", conf.getLaserRecord().getLaserType());
        assertEquals(532.10 * 1.0e-9, conf.getLaserRecord().getPrimaryWavelength(), 1.0e-15);
        assertEquals(0.0, conf.getLaserRecord().getNominalFireRate(), 1.0e-15);
        assertEquals(21.0, conf.getLaserRecord().getPulseEnergy(), 1.0e-15);
        assertEquals(12.0, conf.getLaserRecord().getPulseWidth(), 1.0e-15);
        assertEquals(0.0, conf.getLaserRecord().getBeamDivergence(), 1.0e-15);
        assertEquals(1, conf.getLaserRecord().getPulseInOutgoingSemiTrain());
        assertEquals("IDAJ", conf.getDetectorRecord().getDetectorId());
        assertEquals("CSPAD", conf.getDetectorRecord().getDetectorType());
        assertEquals(532.000 * 1.0e-9, conf.getDetectorRecord().getApplicableWavelength(), 1.0e-15);
        assertEquals(20.0, conf.getDetectorRecord().getQuantumEfficiency(), 1.0e-15);
        assertEquals(11.0, conf.getDetectorRecord().getAppliedVoltage(), 1.0e-15);
        assertEquals(100000.0, conf.getDetectorRecord().getDarkCount(), 1.0e-15);
        assertEquals("ECL", conf.getDetectorRecord().getOutputPulseType());
        assertEquals(12.0, conf.getDetectorRecord().getOutputPulseWidth(), 1.0e-15);
        assertEquals(2.00 * 1.0e-9, conf.getDetectorRecord().getSpectralFilter(), 1.0e-15);
        assertEquals(90.0, conf.getDetectorRecord().getTransmissionOfSpectralFilter(), 1.0e-15);
        assertEquals(12.0, conf.getDetectorRecord().getSpatialFilter(), 1.0e-15);
        assertEquals("Manual", conf.getDetectorRecord().getExternalSignalProcessing());
        assertEquals("IDAV", conf.getTimingRecord().getLocalTimingId());
        assertEquals("TrueTime_XLi", conf.getTimingRecord().getTimeSource());
        assertEquals("TrueTime_OCXO", conf.getTimingRecord().getFrequencySource());
        assertEquals("MRCS", conf.getTimingRecord().getTimer());
        assertEquals("NA", conf.getTimingRecord().getTimerSerialNumber());
        assertEquals(0.2322 * 1.0e-6, conf.getTimingRecord().getEpochDelayCorrection(), 1.0e-15);

        // Meteorological data
        final MeteorologicalMeasurement meteoFirst = block.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH);
        assertEquals(14353.388283000000, meteoFirst.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        assertEquals(0.92374, meteoFirst.getPressure(), 1.0e-15);
        assertEquals(289.42, meteoFirst.getTemperature(), 1.0e-15);
        assertEquals(28.1, meteoFirst.getHumidity(), 1.0e-15);

        // Range data
        assertEquals(4, block.getRangeData().size());
        final RangeMeasurement range = block.getRangeData().get(0);
        assertEquals(14487.343206247217, range.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        assertEquals(0.003603959600, range.getTimeOfFlight(), 1.0e-15);
        assertEquals(2, range.getEpochEvent());

        // Angles data
        assertEquals(4, block.getAnglesData().size());
        final AnglesMeasurement angles = block.getAnglesData().get(0);
        assertEquals(14343.574333000000, angles.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        assertEquals(FastMath.toRadians(215.000000), angles.getAzimuth(), 1.0e-15);
        assertEquals(FastMath.toRadians(15.000010), angles.getElevation(), 1.0e-15);
        assertEquals(0, angles.getDirectionFlag());
        assertEquals(2, angles.getOriginIndicator());
        assertFalse(angles.isRefractionCorrected());
        assertEquals(Double.NaN, angles.getAzimuthRate(), 1.0e-15);
        assertEquals(Double.NaN, angles.getElevationRate(), 1.0e-15);

    }

    @Test
    void testAllFields() throws URISyntaxException, IOException {

        final String ex = "/ilrs/crd_all_fields.frd";

        final CRD file = new CRDParser().parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        final CRDDataBlock block = file.getDataBlocks().get(0);
        assertEquals(0, file.getComments().size());
        assertEquals(4, block.getRangeData().size());
        assertEquals(4, block.getAnglesData().size());

        // Transponder
        final TransponderConfiguration transponder = block.getConfigurationRecords().getTransponderRecord();
        assertEquals("id", transponder.getTransponderId());
        assertEquals(0.0, transponder.getStationUTCOffset(), 1.0e-15);
        assertEquals(0.0, transponder.getStationOscDrift(), 1.0e-15);
        assertEquals(0.0, transponder.getTranspUTCOffset(), 1.0e-15);
        assertEquals(0.0, transponder.getTranspOscDrift(), 1.0e-15);
        assertEquals(0.0, transponder.getTranspClkRefTime(), 1.0e-15);
        assertEquals(0, transponder.getSpacecraftClockAndDriftApplied());
        assertEquals(0, transponder.getStationClockAndDriftApplied());
        assertFalse(transponder.isSpacecraftTimeSimplified());
    }

    @Test
    void testMeteorologicalData() {

        // Initialise an empty data block
        final CRDDataBlock block = new CRDDataBlock();

        // Verify null object
        assertNull(block.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH));

        // Add a meteorological data entry
        final AbsoluteDate date = new AbsoluteDate("2020-10-29T11:40:00.000", TimeScalesFactory.getUTC());
        final MeteorologicalMeasurement meteoData1 = new MeteorologicalMeasurement(date, 1013.0, 273.0, 50.0);
        block.addMeteoData(meteoData1);

        // Verify values
        MeteorologicalMeasurement data1 = block.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH);
        assertEquals(meteoData1.getPressure(),    data1.getPressure(),    1.0e-15);
        assertEquals(meteoData1.getTemperature(), data1.getTemperature(), 1.0e-15);
        assertEquals(meteoData1.getHumidity(),    data1.getHumidity(),    1.0e-15);
        data1 = block.getMeteoData().getMeteo(date);
        assertEquals(meteoData1.getPressure(),    data1.getPressure(),    1.0e-15);
        assertEquals(meteoData1.getTemperature(), data1.getTemperature(), 1.0e-15);
        assertEquals(meteoData1.getHumidity(),    data1.getHumidity(),    1.0e-15);
        data1 = block.getMeteoData().getMeteo(date.shiftedBy(60.0));
        assertEquals(meteoData1.getPressure(),    data1.getPressure(),    1.0e-15);
        assertEquals(meteoData1.getTemperature(), data1.getTemperature(), 1.0e-15);
        assertEquals(meteoData1.getHumidity(),    data1.getHumidity(),    1.0e-15);

        // Add another meteorological data entry
        final MeteorologicalMeasurement meteoData2 = new MeteorologicalMeasurement(date.shiftedBy(60.0), 1015.0, 275.0, 70.0);
        block.addMeteoData(meteoData2);

        // Verify values
        MeteorologicalMeasurement data2 = block.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH); // before first data
        assertEquals(meteoData1.getPressure(),    data2.getPressure(),    1.0e-15);
        assertEquals(meteoData1.getTemperature(), data2.getTemperature(), 1.0e-15);
        assertEquals(meteoData1.getHumidity(),    data2.getHumidity(),    1.0e-15);
        data2 = block.getMeteoData().getMeteo(date.shiftedBy(180.0)); // After second data
        assertEquals(meteoData2.getPressure(),    data2.getPressure(),    1.0e-15);
        assertEquals(meteoData2.getTemperature(), data2.getTemperature(), 1.0e-15);
        assertEquals(meteoData2.getHumidity(),    data2.getHumidity(),    1.0e-15);
        data2 = block.getMeteoData().getMeteo(date.shiftedBy(30.0)); // between first and second datta
        assertEquals(1014.0,                      data2.getPressure(),    1.0e-15);
        assertEquals(274.0,                       data2.getTemperature(), 1.0e-15);
        assertEquals(60.0,                        data2.getHumidity(),    1.0e-15);

    }

    @Test
    void testIssue801() throws URISyntaxException, IOException {

        final String ex = "/ilrs/crd_all_fields.frd";

        final CRD file = new CRDParser().parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        final CRDDataBlock block = file.getDataBlocks().get(0);
        assertEquals(0, file.getComments().size());
        assertEquals(4, block.getRangeData().size());
        assertEquals(4, block.getAnglesData().size());

        final Meteo meteo = block.getMeteoData();
        final List<MeteorologicalMeasurement> data = meteo.getData();
        assertEquals(1, data.size());

        final MeteorologicalMeasurement measurement = data.get(0);
        assertEquals(0.92374, measurement.getPressure(),    0.00001);
        assertEquals(289.42,  measurement.getTemperature(), 0.01);
        assertEquals(28.1,    measurement.getHumidity(),    0.01);

    }

    @Test
    void testIssue847() throws IOException {

        // Read the file
        final String ex = "/ilrs/lageos1-test.npt";
        final CRD file = new CRDParser().parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify
        assertEquals(6, file.getComments().size());
        assertEquals("New CFD in the STOP channel", file.getComments().get(0));
        assertEquals("No CFD in the START channel", file.getComments().get(1));
        assertEquals("New experimental detector (transistor) in the START channel**", file.getComments().get(2));
        assertEquals("New CFD in the STOP channel", file.getComments().get(3));
        assertEquals("No CFD in the START channel", file.getComments().get(4));
        assertEquals("New experimental detector (transistor) in the START channel", file.getComments().get(5));
    }

    @Test
    void testIssue886() throws IOException {
        final String ex = "/ilrs/glonass125_trunc.frd";
        final CRD file = new CRDParser().parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        final CRDDataBlock block = file.getDataBlocks().get(0);
        final List<RangeMeasurement> rangeBlock = block.getRangeData();
        final RangeMeasurement rangeFirst = rangeBlock.get(0);
        final RangeMeasurement rangeLast = rangeBlock.get(rangeBlock.size() - 1);

        DateComponents startEpoch = new DateComponents(2019, 04, 19);
        DateComponents lastEpoch = new DateComponents(2019, 04, 20);
        double firstSecOfDay = 77387.019063653420;
        double lastSecOfDay = 694.119563650340;
        final AbsoluteDate firstDate = new AbsoluteDate(startEpoch, new TimeComponents(firstSecOfDay), DataContext.getDefault().getTimeScales().getUTC());
        final AbsoluteDate lastDate = new AbsoluteDate(lastEpoch, new TimeComponents(lastSecOfDay), DataContext.getDefault().getTimeScales().getUTC());


        assertEquals(firstDate, rangeFirst.getDate());
        assertEquals(lastDate, rangeLast.getDate());
    }

    @Test
    void testIssue886Bis() throws IOException {
        final String ex = "/ilrs/Rollover.frd";
        final CRD file = new CRDParser().parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify each block

        // Block 1
        CRDDataBlock block = file.getDataBlocks().get(0);
        List<RangeMeasurement> rangeBlock = block.getRangeData();
        RangeMeasurement rangeFirst = rangeBlock.get(0);
        RangeMeasurement rangeLast = rangeBlock.get(rangeBlock.size() - 1);

        DateComponents startEpoch = new DateComponents(2022, 6, 6);
        DateComponents lastEpoch = new DateComponents(2022, 6, 6);
        double firstSecOfDay = 43410.8898329;
        double lastSecOfDay = 43444.1690476;
        AbsoluteDate firstDate = new AbsoluteDate(startEpoch, new TimeComponents(firstSecOfDay), DataContext.getDefault().getTimeScales().getUTC());
        AbsoluteDate lastDate = new AbsoluteDate(lastEpoch, new TimeComponents(lastSecOfDay), DataContext.getDefault().getTimeScales().getUTC());

        assertEquals(firstDate, rangeFirst.getDate());
        assertEquals(lastDate, rangeLast.getDate());

        // Block 2
        block = file.getDataBlocks().get(1);
        rangeBlock = block.getRangeData();
        rangeFirst = rangeBlock.get(0);
        rangeLast = rangeBlock.get(rangeBlock.size() - 1);

        startEpoch = new DateComponents(2022, 6, 6);
        lastEpoch = new DateComponents(2022, 6, 6);
        firstSecOfDay = 26579.400543200001;
        lastSecOfDay = 26618.200540700000;
        firstDate = new AbsoluteDate(startEpoch, new TimeComponents(firstSecOfDay), DataContext.getDefault().getTimeScales().getUTC());
        lastDate = new AbsoluteDate(lastEpoch, new TimeComponents(lastSecOfDay), DataContext.getDefault().getTimeScales().getUTC());

        assertEquals(firstDate, rangeFirst.getDate());
        assertEquals(lastDate, rangeLast.getDate());

        // Block 3
        block = file.getDataBlocks().get(2);
        rangeBlock = block.getRangeData();
        rangeFirst = rangeBlock.get(0);
        rangeLast = rangeBlock.get(rangeBlock.size() - 1);

        startEpoch = new DateComponents(2021, 1, 26);
        lastEpoch = new DateComponents(2021, 1, 27);
        firstSecOfDay = 86181.271863631440;
        lastSecOfDay = 1007.946763625370;
        firstDate = new AbsoluteDate(startEpoch, new TimeComponents(firstSecOfDay), DataContext.getDefault().getTimeScales().getUTC());
        lastDate = new AbsoluteDate(lastEpoch, new TimeComponents(lastSecOfDay), DataContext.getDefault().getTimeScales().getUTC());

        assertEquals(firstDate, rangeFirst.getDate());
        assertEquals(lastDate, rangeLast.getDate());

    }

    @Test
    void testIssue938() throws IOException {
        final String ex = "/ilrs/crd201_all_samples";
        final CRD file = new CRDParser().parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        final TimeScale utc = TimeScalesFactory.getUTC();

        final double DELTA_PS = 1e-12;  // 1ps, TimeOfFlight
        final double DELTA_TENTH_PS = 0.1e-12;  // 0.1ps, Rms, PeakMinusMean, SystemDelay, DelayShift, TroposphericRefractionCorrection
        final double DELTA_TENTH_US = 0.1e-6;  // 0.1us, EpochDelayCorrection
        final double DELTA_MILLI_NM = 1e-12;  // 0.001nm, Wavelength, ApplicableWavelength, PrimaryWavelength
        final double DELTA_TENTH_MM = 1e-3;  // 0.1mm, CenterOfMassCorrection, OneWayDistance, SurveyedTargetDistance, SumOfAllConstantDelays, SurveyError
        final double DELTA_MILLI = 1e-3;  // milli, Skew, Kurtosis
        final double DELTA_CENTI = 1e-2;  // centi, ReturnRate, Snr

        final List<CRDDataBlock> dataBlocks = file.getDataBlocks();
        assertEquals(12, dataBlocks.size());

        // block0: Full rate
        final CRDDataBlock block0 = dataBlocks.get(0);
        final List<RangeMeasurement> b0_rangeData = block0.getRangeData();
        final List<RangeSupplement> b0_rangeSupplementData = block0.getRangeSupplementData();
        final List<MeteorologicalMeasurement> b0_meteorologicalMeasurementData = block0.getMeteoData().getData();
        final List<AnglesMeasurement> b0_anglesData = block0.getAnglesData();
        final List<Calibration> b0_calibrations = block0.getCalibrationRecords();

        assertEquals(2, block0.getHeader().getVersion());
        assertEquals(3, b0_rangeData.size());
        assertEquals(0, block0.getHeader().getDataType());  // 0=full rate
        assertEquals(DataType.FULL_RATE, DataType.getDataType(block0.getHeader().getDataType()));
        assertInstanceOf(FrRangeMeasurement.class, b0_rangeData.get(0));
        final FrRangeMeasurement b0_fr0 = (FrRangeMeasurement)b0_rangeData.get(0);
        assertEquals(0.047960587856, b0_fr0.getTimeOfFlight(), DELTA_PS);
        assertEquals(2, b0_fr0.getEpochEvent());
        assertEquals(Double.NaN, b0_fr0.getSnr(), DELTA_CENTI);
        assertEquals("2006-11-13T15:23:52.0414338", b0_fr0.getDate().toStringWithoutUtcOffset(utc, 7));
        assertEquals("std1", b0_fr0.getSystemConfigurationId());
        assertEquals(0, b0_fr0.getFilterFlag());
        assertEquals(0, b0_fr0.getDetectorChannel());
        assertEquals(0, b0_fr0.getStopNumber());
        assertEquals(-1, b0_fr0.getReceiveAmplitude());
        assertEquals(-1, b0_fr0.getTransmitAmplitude());
        assertEquals("10 55432.041433800000     0.047960587856 std1 2 0 0 0    na    na", b0_fr0.toCrdString());

        final RangeSupplement b0_rangeSupplement1 = b0_rangeSupplementData.get(1);
        assertEquals(3, b0_rangeSupplementData.size());
        assertEquals("2006-11-13T15:23:55.6429746", b0_rangeSupplement1.getDate().toStringWithoutUtcOffset(utc, 7));
        assertEquals("std1", b0_rangeSupplement1.getSystemConfigurationId());
        assertEquals(20697.0e-12, b0_rangeSupplement1.getTroposphericRefractionCorrection(), DELTA_TENTH_PS);
        assertEquals(1601.0000, b0_rangeSupplement1.getCenterOfMassCorrection(), DELTA_TENTH_MM);
        assertEquals(0.00, b0_rangeSupplement1.getNdFilterValue(), DELTA_CENTI);
        assertEquals(0.0000, b0_rangeSupplement1.getTimeBiasApplied(), 0.1e-4);
        assertEquals(0.0, b0_rangeSupplement1.getRangeRate(), DELTA_MILLI);
        assertEquals("12 55435.642974600000 std1 20697.0 1601.0000  0.00   0.0000 0.000000", b0_rangeSupplement1.toCrdString());

        final MeteorologicalMeasurement b0_meteorologicalMeasurement0 = b0_meteorologicalMeasurementData.get(0);
        assertEquals(1, b0_meteorologicalMeasurementData.size());
        assertEquals("2006-11-13T15:23:52.0414338", b0_meteorologicalMeasurement0.getDate().toStringWithoutUtcOffset(utc, 7));
        assertEquals(801.80e-3, b0_meteorologicalMeasurement0.getPressure(), 0.01e-3);
        assertEquals(301.36, b0_meteorologicalMeasurement0.getTemperature(), DELTA_CENTI);
        assertEquals(39, b0_meteorologicalMeasurement0.getHumidity(), 1);
        assertEquals(0, b0_meteorologicalMeasurement0.getOriginOfValues());
        assertEquals("20 55432.041  801.80 301.36   39 0", b0_meteorologicalMeasurement0.toCrdString());

        final AnglesMeasurement b0_angles2 = b0_anglesData.get(2);
        assertEquals(3, b0_anglesData.size());
        assertEquals("2006-11-13T15:45:35.8021609", b0_angles2.getDate().toStringWithoutUtcOffset(utc, 7));
        assertEquals(15.2330, FastMath.toDegrees(b0_angles2.getAzimuth()), 1e-4);
        assertEquals(45.7100, FastMath.toDegrees(b0_angles2.getElevation()), 1e-4);
        assertEquals(0, b0_angles2.getDirectionFlag());
        assertEquals(2, b0_angles2.getOriginIndicator());
        assertTrue(b0_angles2.isRefractionCorrected());
        assertEquals(Double.NaN, b0_angles2.getAzimuthRate(), 1e-6);
        assertEquals(Double.NaN, b0_angles2.getElevationRate(), 1e-6);
        assertEquals("30 56735.802  15.2330  45.7100 0 2 1        na        na", b0_angles2.toCrdString());

        final Calibration b0_calibration0 = b0_calibrations.get(0);
        assertEquals(1, b0_calibrations.size());
        assertEquals("2006-11-13T15:23:52.0414338", b0_calibration0.getDate().toStringWithoutUtcOffset(utc, 7));
        assertEquals(0, b0_calibration0.getTypeOfData());
        assertEquals("std1", b0_calibration0.getSystemConfigurationId());
        assertEquals(-1, b0_calibration0.getNumberOfPointsRecorded());
        assertEquals(-1, b0_calibration0.getNumberOfPointsUsed());
        assertEquals(0.000, b0_calibration0.getOneWayDistance(), DELTA_TENTH_MM);
        assertEquals(-913.0e-12, b0_calibration0.getSystemDelay(), DELTA_TENTH_PS);
        assertEquals(0.0, b0_calibration0.getDelayShift(), DELTA_TENTH_PS);
        assertEquals(56.0e-12, b0_calibration0.getRms(), DELTA_TENTH_PS);
        assertEquals(Double.NaN, b0_calibration0.getSkew(), DELTA_MILLI);
        assertEquals(Double.NaN, b0_calibration0.getKurtosis(), DELTA_MILLI);
        assertEquals(Double.NaN, b0_calibration0.getPeakMinusMean(), DELTA_TENTH_PS);
        assertEquals(3, b0_calibration0.getTypeIndicator());
        assertEquals(3, b0_calibration0.getShiftTypeIndicator());
        assertEquals(0, b0_calibration0.getDetectorChannel());
        assertEquals(4, b0_calibration0.getSpan());
        assertEquals(Double.NaN, b0_calibration0.getReturnRate(), DELTA_CENTI);
        assertEquals("40 55432.041433800000 0 std1       na       na   0.0000     -913.0      0.0   56.0     na     na    na 3 3 0 4   na", 
                b0_calibration0.toCrdString());

        // There is no SessionStatisticsRecord.
        assertTrue(block0.getSessionStatisticsData().isEmpty());
        assertNull(block0.getSessionStatisticsRecord());

        // block1: Normal Point
        final CRDDataBlock block1 = dataBlocks.get(1);
        final List<RangeMeasurement> b1_rangeData = block1.getRangeData();
        assertEquals(2, block1.getHeader().getVersion());
        assertEquals(8, b1_rangeData.size());
        assertEquals(1, block1.getHeader().getDataType());  // 1=normal point
        assertEquals(DataType.NORMAL_POINT, DataType.getDataType(block1.getHeader().getDataType()));
        assertInstanceOf(NptRangeMeasurement.class, b1_rangeData.get(0));
        final NptRangeMeasurement b1_npt3 = (NptRangeMeasurement)b1_rangeData.get(3);
        assertEquals(0.044605221903, b1_npt3.getTimeOfFlight(), DELTA_PS);
        assertEquals(2, b1_npt3.getEpochEvent());
        assertEquals(0.0, b1_npt3.getSnr(), DELTA_CENTI);
        assertEquals("2006-11-13T15:37:03.2817254", b1_npt3.getDate().toStringWithoutUtcOffset(utc, 7));
        assertEquals("std1", b1_npt3.getSystemConfigurationId());
        assertEquals(120, b1_npt3.getWindowLength(), 1e-15);
        assertEquals(25, b1_npt3.getNumberOfRawRanges());
        assertEquals(87.0e-12, b1_npt3.getBinRms(), DELTA_TENTH_PS);
        assertEquals(Double.NaN, b1_npt3.getBinSkew(), DELTA_MILLI);
        assertEquals(Double.NaN, b1_npt3.getBinKurtosis(), DELTA_MILLI);
        assertEquals(Double.NaN, b1_npt3.getBinPeakMinusMean(), DELTA_TENTH_PS);
        assertEquals(0.0, b1_npt3.getReturnRate(), DELTA_CENTI);
        assertEquals(0, b1_npt3.getDetectorChannel());
        assertEquals("11 56223.281725400000     0.044605221903 std1 2  120.0     25      87.0     na     na       na  0.00 0   0.0", 
                b1_npt3.toCrdString());

        final SessionStatistics b1_sessionStatistics = block1.getSessionStatisticsRecord();
        assertEquals("std1", b1_sessionStatistics.getSystemConfigurationId());
        assertEquals(86.0e-12, b1_sessionStatistics.getRms(), DELTA_TENTH_PS);
        assertEquals(Double.NaN, b1_sessionStatistics.getSkewness(), DELTA_MILLI);
        assertEquals(Double.NaN, b1_sessionStatistics.getKurtosis(), DELTA_MILLI);
        assertEquals(Double.NaN, b1_sessionStatistics.getPeakMinusMean(), DELTA_TENTH_PS);
        assertEquals(0, b1_sessionStatistics.getDataQulityIndicator());
        assertEquals("50 std1   86.0     na     na    na 0", b1_sessionStatistics.toCrdString());

        final List<MeteorologicalMeasurement> b1_meteorologicalMeasurementData = block1.getMeteoData().getData();
        assertEquals(5, b1_meteorologicalMeasurementData.size());

        // There is only one SessionStatisticsRecord related to "std1".
        assertNull(block1.getSessionStatisticsRecord("std2"));

        // block2: Sampled Engineering (Quicklook)
        final CRDDataBlock block2 = dataBlocks.get(2);
        assertEquals(2, block2.getHeader().getDataType());  // 2=sampled engineering
        assertEquals(DataType.SAMPLED_ENGIEERING, DataType.getDataType(block2.getHeader().getDataType()));
        // There is no CalibrationRecords.
        assertTrue(block2.getCalibrationData().isEmpty());
        assertNull(block2.getCalibrationRecords());
        
        // block3: Sample 2-Color Normal Point file
        final CRDDataBlock block3 = dataBlocks.get(3);
        final CRDConfiguration b3_config = block3.getConfigurationRecords();
        final List<SystemConfiguration> b3_systemConfigurations = b3_config.getSystemConfigurationRecords();
        assertEquals(2, b3_systemConfigurations.size());
        assertSame(b3_config.getSystemRecord(), b3_config.getSystemRecord(null));
        final Set<String> b3_config_systemIds = b3_config.getSystemConfigurationIds();
        assertEquals(2, b3_config_systemIds.size());
        final String b3_systemConfigId_std1 = "std1";
        final String b3_systemConfigId_std2 = "std2";
        assertTrue(b3_config_systemIds.contains(b3_systemConfigId_std1) && b3_config_systemIds.contains(b3_systemConfigId_std2));
        final SystemConfiguration b3_systemConfig_std1 = b3_config.getSystemRecord(b3_systemConfigId_std1);
        final SystemConfiguration b3_systemConfig_std2 = b3_config.getSystemRecord(b3_systemConfigId_std2);
        assertEquals(846.000e-9, b3_systemConfig_std1.getWavelength(), DELTA_MILLI_NM);
        assertEquals(423.000e-9, b3_systemConfig_std2.getWavelength(), DELTA_MILLI_NM);
        
        final List<Calibration> b3_calibrations_std1 = block3.getCalibrationRecords(b3_systemConfigId_std1);
        final List<Calibration> b3_calibrations_std2 = block3.getCalibrationRecords(b3_systemConfigId_std2);
        assertEquals(1, b3_calibrations_std1.size());
        assertEquals(0, b3_calibrations_std2.size());

        final List<SessionStatistics> b3_sessionStatisticsData = block3.getSessionStatisticsData();
        assertEquals(2, b3_sessionStatisticsData.size());
        
        final SessionStatistics b3_sessionStatistics_std1 = block3.getSessionStatisticsRecord(b3_systemConfigId_std1);
        final SessionStatistics b3_sessionStatistics_std2 = block3.getSessionStatisticsRecord(b3_systemConfigId_std2);
        assertEquals(165.0e-12, b3_sessionStatistics_std1.getRms(), DELTA_TENTH_PS);
        assertEquals(78.0e-12, b3_sessionStatistics_std2.getRms(), DELTA_TENTH_PS);

        final List<RangeMeasurement> b3_rangeData = block3.getRangeData();
        assertEquals(20, b3_rangeData.size());
        final NptRangeMeasurement b3_npt3 = (NptRangeMeasurement)b3_rangeData.get(3);
        assertEquals(0.050886342010, b3_npt3.getTimeOfFlight(), DELTA_PS);
        assertEquals(2, b3_npt3.getEpochEvent());
        assertEquals(0.0, b3_npt3.getSnr(), DELTA_CENTI);
        assertEquals("2006-12-30T07:36:13.1080893", b3_npt3.getDate().toStringWithoutUtcOffset(utc, 7));
        assertEquals("std1", b3_npt3.getSystemConfigurationId());
        assertEquals(120, b3_npt3.getWindowLength(), 1e-15);
        assertEquals(17, b3_npt3.getNumberOfRawRanges());
        assertEquals(158.0e-12, b3_npt3.getBinRms(), DELTA_TENTH_PS);
        assertEquals(Double.NaN, b3_npt3.getBinSkew(),DELTA_MILLI);
        assertEquals(Double.NaN, b3_npt3.getBinKurtosis(), DELTA_MILLI);
        assertEquals(Double.NaN, b3_npt3.getBinPeakMinusMean(), DELTA_TENTH_PS);
        assertEquals(0.0, b3_npt3.getReturnRate(), DELTA_CENTI);
        assertEquals(0, b3_npt3.getDetectorChannel());
        assertEquals("11 27373.108089300000     0.050886342010 std1 2  120.0     17     158.0     na     na       na  0.00 0   0.0", 
                b3_npt3.toCrdString());
        final NptRangeMeasurement b3_npt5 = (NptRangeMeasurement)b3_rangeData.get(5);
        assertEquals(0.042208378233, b3_npt5.getTimeOfFlight(), DELTA_PS);
        assertEquals(2, b3_npt5.getEpochEvent());
        assertEquals(0.0, b3_npt5.getSnr(), DELTA_CENTI);
        assertEquals("2006-12-30T07:46:48.7080899", b3_npt5.getDate().toStringWithoutUtcOffset(utc, 7));
        assertEquals("std2", b3_npt5.getSystemConfigurationId());
        assertEquals(120, b3_npt5.getWindowLength(), 1e-15);
        assertEquals(85, b3_npt5.getNumberOfRawRanges());
        assertEquals(71.0e-12, b3_npt5.getBinRms(), DELTA_TENTH_PS);
        assertEquals(Double.NaN, b3_npt5.getBinSkew(), DELTA_MILLI);
        assertEquals(Double.NaN, b3_npt5.getBinKurtosis(), DELTA_MILLI);
        assertEquals(Double.NaN, b3_npt5.getBinPeakMinusMean(), DELTA_TENTH_PS);
        assertEquals(0.0, b3_npt5.getReturnRate(), DELTA_CENTI);
        assertEquals(0, b3_npt5.getDetectorChannel());
        assertEquals("11 28008.708089900000     0.042208378233 std2 2  120.0     85      71.0     na     na       na  0.00 0   0.0", 
                b3_npt5.toCrdString());

        assertEquals(846.000e-9, 
                block3.getConfigurationRecords().getSystemRecord(b3_npt3.getSystemConfigurationId()).getWavelength(),
                DELTA_MILLI_NM);
        assertEquals(846.000e-9, block3.getWavelength(b3_npt3), DELTA_MILLI_NM);
        assertEquals(423.000e-9, 
                block3.getConfigurationRecords().getSystemRecord(b3_npt5.getSystemConfigurationId()).getWavelength(),
                DELTA_MILLI_NM);
        assertEquals(423.000e-9, block3.getWavelength(b3_npt5), DELTA_MILLI_NM);

        // block4: Sample showing all current record types
        final CRDDataBlock block4 = dataBlocks.get(4);
        final CRDConfiguration b4_config = block4.getConfigurationRecords();
        final SystemConfiguration b4_systemConfig = b4_config.getSystemRecord();
        final LaserConfiguration b4_laserConfig = b4_config.getLaserRecord();
        final DetectorConfiguration b4_detectorConfig = b4_config.getDetectorRecord();
        final TimingSystemConfiguration b4_timingConfig = b4_config.getTimingRecord();
        final SoftwareConfiguration b4_softwareConfig = b4_config.getSoftwareRecord();
        final MeteorologicalConfiguration b4_meteorologicalConfig = b4_config.getMeteorologicalRecord();
        final TransponderConfiguration b4_transponderConfig = b4_config.getTransponderRecord();

        assertEquals(6, b4_config.getConfigurationRecordMap().size());
        assertInstanceOf(LaserConfiguration.class, b4_config.getConfigurationRecord("ml1"));

        assertEquals(532.000e-9, b4_systemConfig.getWavelength(), DELTA_MILLI_NM);
        assertEquals("std", b4_systemConfig.getSystemId());
        assertEquals("[ml1, mcp, mt1, swv, met]", b4_systemConfig.getComponents().toString());
        assertEquals("C0 0    532.000 std ml1 mcp mt1 swv met", b4_systemConfig.toCrdString());

        assertEquals("ml1", b4_laserConfig.getLaserId());
        assertEquals("Nd-Yag", b4_laserConfig.getLaserType());
        assertEquals(1064.00e-9, b4_laserConfig.getPrimaryWavelength(), DELTA_MILLI_NM);
        assertEquals(10.00, b4_laserConfig.getNominalFireRate(), DELTA_CENTI);
        assertEquals(100.00, b4_laserConfig.getPulseEnergy(), DELTA_CENTI);
        assertEquals(200.0, b4_laserConfig.getPulseWidth(), DELTA_CENTI);
        assertEquals(Double.NaN, b4_laserConfig.getBeamDivergence(), DELTA_CENTI);
        assertEquals(1, b4_laserConfig.getPulseInOutgoingSemiTrain());
        assertEquals("C1 0 ml1 Nd-Yag 1064.00 10.00 100.00 200.0 na 1", b4_laserConfig.toCrdString());

        assertEquals("mcp", b4_detectorConfig.getDetectorId());
        assertEquals("mcp", b4_detectorConfig.getDetectorType());
        assertEquals(532.00e-9, b4_detectorConfig.getApplicableWavelength(), DELTA_MILLI_NM);
        assertEquals(Double.NaN, b4_detectorConfig.getQuantumEfficiency(), DELTA_CENTI);
        assertEquals(3800.0, b4_detectorConfig.getAppliedVoltage(), DELTA_CENTI);
        assertEquals(0.0, b4_detectorConfig.getDarkCount(), 0.1e3);
        assertEquals("unknown", b4_detectorConfig.getOutputPulseType());
        assertEquals(Double.NaN, b4_detectorConfig.getOutputPulseWidth(), DELTA_CENTI);
        assertEquals(0.0, b4_detectorConfig.getSpectralFilter(), 0.1e-9);
        assertEquals(Double.NaN, b4_detectorConfig.getTransmissionOfSpectralFilter());
        assertEquals(0.0, b4_detectorConfig.getSpatialFilter(), 0.1);
        assertEquals("none", b4_detectorConfig.getExternalSignalProcessing());
        assertEquals(5.0, b4_detectorConfig.getAmplifierGain(), 0.1);
        assertEquals(10.0e3, b4_detectorConfig.getAmplifierBandwidth(), 0.1e3);
        assertEquals("1", b4_detectorConfig.getAmplifierInUse());
        assertEquals("C2 0 mcp mcp 532.000 na 3800.0 0.0 unknown na 0.00 na 0.0 none 5.0 10.0 1", b4_detectorConfig.toCrdString());

        assertEquals("mt1", b4_timingConfig.getLocalTimingId());
        assertEquals("TAC", b4_timingConfig.getTimeSource());
        assertEquals("TAC", b4_timingConfig.getFrequencySource());
        assertEquals("MLRS_CMOS_TMRB_TD811", b4_timingConfig.getTimer());
        assertEquals("na", b4_timingConfig.getTimerSerialNumber());
        assertEquals(445.9e-6, b4_timingConfig.getEpochDelayCorrection(), DELTA_TENTH_US);
        assertEquals("C3 0 mt1 TAC TAC MLRS_CMOS_TMRB_TD811 na 445.9", b4_timingConfig.toCrdString());

        assertEquals("swv", b4_softwareConfig.getConfigurationId());
        assertEquals("swv", b4_softwareConfig.getSoftwareId());
        assertEquals("[Monitor, Sattrk]", Arrays.toString(b4_softwareConfig.getTrackingSoftwares()));
        assertEquals("[2.000Bm, 2.00Cm]", Arrays.toString(b4_softwareConfig.getTrackingSoftwareVersions()));
        assertEquals("[conpro, crd_cal, PoissonCRD, gnp]", Arrays.toString(b4_softwareConfig.getProcessingSoftwares()));
        assertEquals("[2.4a, 1.7, 2.2a, CM-2.01a]", Arrays.toString(b4_softwareConfig.getProcessingSoftwareVersions()));
        assertEquals("C5 0 swv Monitor,Sattrk 2.000Bm,2.00Cm conpro,crd_cal,PoissonCRD,gnp 2.4a,1.7,2.2a,CM-2.01a", b4_softwareConfig.toCrdString());

        assertEquals("met", b4_meteorologicalConfig.getConfigurationId());
        assertEquals("met", b4_meteorologicalConfig.getMeteorologicalId());
        assertEquals("Paroscientific", b4_meteorologicalConfig.getPressSensorManufacturer());
        assertEquals("Met4", b4_meteorologicalConfig.getPressSensorModel());
        assertEquals("123456", b4_meteorologicalConfig.getPressSensorSerialNumber());
        assertEquals("Paroscientific", b4_meteorologicalConfig.getTempSensorManufacturer());
        assertEquals("Met4", b4_meteorologicalConfig.getTempSensorModel());
        assertEquals("123456", b4_meteorologicalConfig.getTempSensorSerialNumber());
        assertEquals("Paroscientific", b4_meteorologicalConfig.getHumiSensorManufacturer());
        assertEquals("Met4", b4_meteorologicalConfig.getHumiSensorModel());
        assertEquals("123456", b4_meteorologicalConfig.getHumiSensorSerialNumber());
        assertEquals("C6 0 met Paroscientific Met4 123456 Paroscientific Met4 123456 Paroscientific Met4 123456", b4_meteorologicalConfig.toCrdString());

        final SessionStatistics b4_sessionStatistics = block4.getSessionStatisticsRecord();
        assertEquals("std", b4_sessionStatistics.getSystemConfigurationId());
        assertEquals(72.7e-12, b4_sessionStatistics.getRms(), DELTA_TENTH_PS);
        assertEquals(1.494, b4_sessionStatistics.getSkewness(), DELTA_MILLI);
        assertEquals(-0.536, b4_sessionStatistics.getKurtosis(), DELTA_MILLI);
        assertEquals(-32.4e-12, b4_sessionStatistics.getPeakMinusMean(), DELTA_TENTH_PS);
        assertEquals(0, b4_sessionStatistics.getDataQulityIndicator());
        assertEquals("50  std   72.7   1.494  -0.536  -32.4 0", b4_sessionStatistics.toCrdString());

        assertNull(b4_transponderConfig);

        assertFalse(b4_systemConfig.equals(null));
        assertTrue(b4_systemConfig.equals(b4_systemConfig));
        assertEquals(-766950339, b4_systemConfig.hashCode());
        assertFalse(b4_systemConfig.equals(b3_systemConfig_std1));
        final SystemConfiguration b4_systemConfig_new = new SystemConfiguration();
        b4_systemConfig_new.setSystemId(b4_systemConfig.getConfigurationId());
        b4_systemConfig_new.setWavelength(b4_systemConfig.getWavelength());
        String[] components = new String[b4_systemConfig.getComponents().size()];
        b4_systemConfig_new.setComponents(b4_systemConfig.getComponents().toArray(components));
        assertTrue(b4_systemConfig.equals(b4_systemConfig_new));

        // block5: c4
        final CRDDataBlock block5 = dataBlocks.get(5);
        final TransponderConfiguration b5_transponderConfig = block5.getConfigurationRecords().getTransponderRecord();
        assertEquals("C4 0 mc1 0.000 0.00 1234567890123456.800 0.00 0.000000000000 0 0 0", b5_transponderConfig.toCrdString());

        // block9: real npt, rollover
        final CRDDataBlock block9 = dataBlocks.get(9);
        assertEquals(1, block9.getHeader().getVersion());
        final List<RangeMeasurement> b9_rangeData = block9.getRangeData();
        assertEquals(10, b9_rangeData.size());
        final NptRangeMeasurement b9_npt7 = (NptRangeMeasurement)b9_rangeData.get(7);
        assertEquals(0.049383687622, b9_npt7.getTimeOfFlight(), DELTA_PS);
        assertEquals(2, b9_npt7.getEpochEvent());
        assertEquals(Double.NaN, b9_npt7.getSnr(), DELTA_CENTI);
        assertEquals("2022-03-25T23:59:06.0200637", b9_npt7.getDate().toStringWithoutUtcOffset(utc, 7));
        assertEquals("0902", b9_npt7.getSystemConfigurationId());
        assertEquals(120, b9_npt7.getWindowLength(), 1e-15);
        assertEquals(1516, b9_npt7.getNumberOfRawRanges());
        assertEquals(34.4e-12, b9_npt7.getBinRms(), DELTA_TENTH_PS);
        assertEquals(-0.017, b9_npt7.getBinSkew(),DELTA_MILLI);
        assertEquals(-1.038, b9_npt7.getBinKurtosis(), DELTA_MILLI);
        assertEquals(11.4e-12, b9_npt7.getBinPeakMinusMean(), DELTA_TENTH_PS);
        assertEquals(0.6, b9_npt7.getReturnRate(), DELTA_CENTI);
        assertEquals(0, b9_npt7.getDetectorChannel());
        assertEquals("11 86346.020063735530     0.049383687622 0902 2  120.0   1516      34.4  -0.017  -1.038      11.4  0.60 0   na", 
                b9_npt7.toCrdString());
        final NptRangeMeasurement b9_npt8 = (NptRangeMeasurement)b9_rangeData.get(8);
        assertEquals(0.056059159587, b9_npt8.getTimeOfFlight(), DELTA_PS);
        assertEquals(2, b9_npt8.getEpochEvent());
        assertEquals(Double.NaN, b9_npt8.getSnr(), DELTA_CENTI);
        assertEquals("2022-03-26T00:05:45.6451637", b9_npt8.getDate().toStringWithoutUtcOffset(utc, 7));
        assertEquals("0902", b9_npt8.getSystemConfigurationId());
        assertEquals(120, b9_npt8.getWindowLength(), 1e-15);
        assertEquals(525, b9_npt8.getNumberOfRawRanges());
        assertEquals(33.7e-12, b9_npt8.getBinRms(), DELTA_TENTH_PS);
        assertEquals(0.023, b9_npt8.getBinSkew(),DELTA_MILLI);
        assertEquals(-0.939, b9_npt8.getBinKurtosis(), DELTA_MILLI);
        assertEquals(12.9e-12, b9_npt8.getBinPeakMinusMean(), DELTA_TENTH_PS);
        assertEquals(0.2, b9_npt8.getReturnRate(), DELTA_CENTI);
        assertEquals(0, b9_npt8.getDetectorChannel());
        assertEquals("11   345.645163732581     0.056059159587 0902 2  120.0    525      33.7   0.023  -0.939      12.9  0.20 0   na", 
                b9_npt8.toCrdString());

        final MeteorologicalMeasurement b9_meteorologicalMeasurement1 = block9.getMeteoData().getData().get(1);
        assertEquals("2022-03-26T00:06:50.0000000", b9_meteorologicalMeasurement1.getDate().toStringWithoutUtcOffset(utc, 7));
        assertEquals(969.45e-3, b9_meteorologicalMeasurement1.getPressure(), 0.01e-3);
        assertEquals(283.15, b9_meteorologicalMeasurement1.getTemperature(), DELTA_CENTI);
        assertEquals(37.5, b9_meteorologicalMeasurement1.getHumidity(), 1);
        assertEquals(1, b9_meteorologicalMeasurement1.getOriginOfValues());
        assertEquals("20   410.000  969.45 283.15   38 1", b9_meteorologicalMeasurement1.toCrdString());

        final Calibration b9_calibration1 = block9.getCalibrationRecords().get(1);
        assertEquals("2022-03-26T00:06:50.0000000", b9_calibration1.getDate().toStringWithoutUtcOffset(utc, 7));
        assertEquals(0, b9_calibration1.getTypeOfData());
        assertEquals("0902", b9_calibration1.getSystemConfigurationId());
        assertEquals(10000, b9_calibration1.getNumberOfPointsRecorded());
        assertEquals(8073, b9_calibration1.getNumberOfPointsUsed());
        assertEquals(1.742, b9_calibration1.getOneWayDistance(), DELTA_TENTH_MM);
        assertEquals(112205.4e-12, b9_calibration1.getSystemDelay(), DELTA_TENTH_PS);
        assertEquals(3.8e-12, b9_calibration1.getDelayShift(), DELTA_TENTH_PS);
        assertEquals(16e-12, b9_calibration1.getRms(), DELTA_TENTH_PS);
        assertEquals(0.043, b9_calibration1.getSkew(), DELTA_MILLI);
        assertEquals(-0.665, b9_calibration1.getKurtosis(), DELTA_MILLI);
        assertEquals(-1e-12, b9_calibration1.getPeakMinusMean(), DELTA_TENTH_PS);
        assertEquals(2, b9_calibration1.getTypeIndicator());
        assertEquals(2, b9_calibration1.getShiftTypeIndicator());
        assertEquals(0, b9_calibration1.getDetectorChannel());
        assertEquals(0, b9_calibration1.getSpan());
        assertEquals(Double.NaN, b9_calibration1.getReturnRate(), DELTA_CENTI);
        assertEquals("40   410.000000000000 0 0902    10000     8073   1.7420   112205.4      3.8   16.0   0.043  -0.665   -1.0 2 2 0 0   na", 
                b9_calibration1.toCrdString());

        final SessionStatistics b9_sessionStatistics = block9.getSessionStatisticsRecord();
        assertEquals("0902", b9_sessionStatistics.getSystemConfigurationId());
        assertEquals(34.0e-12, b9_sessionStatistics.getRms(), DELTA_TENTH_PS);
        assertEquals(0.258, b9_sessionStatistics.getSkewness(), DELTA_MILLI);
        assertEquals(-0.949, b9_sessionStatistics.getKurtosis(), DELTA_MILLI);
        assertEquals(-18.6e-12, b9_sessionStatistics.getPeakMinusMean(), DELTA_TENTH_PS);
        assertEquals(1, b9_sessionStatistics.getDataQulityIndicator());
        assertEquals("50  std   72.7   1.494  -0.536  -32.4 0", b4_sessionStatistics.toCrdString());

        assertNull(block9.getConfigurationRecords().getCalibrationTargetRecord());
        
        final List<Calibration> b9_calibrationData = block9.getCalibrationData();
        assertEquals(2, b9_calibrationData.size());

        final List<CalibrationDetail> b9_calibrationDetailData = block9.getCalibrationDetailData();
        assertTrue(b9_calibrationDetailData.isEmpty());
        assertNull(block9.getCalibrationDetailRecords());

        final CRDHeader b9_header = block9.getHeader();
        assertEquals("H1 CRD  1 2022 03 26 20", b9_header.getH1CrdString());
        assertEquals("H2 GRZL 7839 34 02  4 na", b9_header.getH2CrdString());
        assertEquals("H3 lageos1 7603901 1155 08820 0 1  1", b9_header.getH3CrdString());
        assertEquals("H4  1 2022 03 25 23 10 20 2022 03 26 00 14 20 0 0 0 0 1 0 2 0", b9_header.getH4CrdString());
        assertEquals("H5  0 00 null null     0", b9_header.getH5CrdString());

        // block10: real npt, c7
        final CRDDataBlock block10 = dataBlocks.get(10);
        assertEquals(2, block10.getHeader().getVersion());
        final List<RangeMeasurement> b10_rangeData = block10.getRangeData();
        assertEquals(4, b10_rangeData.size());
        final NptRangeMeasurement b10_npt0 = (NptRangeMeasurement)b10_rangeData.get(0);
        assertEquals(0.040190018544, b10_npt0.getTimeOfFlight(), DELTA_PS);
        assertEquals(532.000e-9, block10.getWavelength(b10_npt0), DELTA_MILLI_NM);
        
        final CRDConfiguration b10_config = block10.getConfigurationRecords();
        final SystemConfiguration b10_systemConfig = b10_config.getSystemRecord();
        final LaserConfiguration b10_laserConfig = b10_config.getLaserRecord();
        final DetectorConfiguration b10_detectorConfig = b10_config.getDetectorRecord();
        final TimingSystemConfiguration b10_timingConfig = b10_config.getTimingRecord();
        final SoftwareConfiguration b10_softwareConfig = b10_config.getSoftwareRecord();
        final MeteorologicalConfiguration b10_meteorologicalConfig = b10_config.getMeteorologicalRecord();
        final CalibrationTargetConfiguration b10_calibrationTargetConfig = b10_config.getCalibrationTargetRecord();
        
        assertEquals(7, b10_config.getConfigurationRecordMap().size());
        assertInstanceOf(LaserConfiguration.class, b10_config.getConfigurationRecord("la1"));

        assertEquals(532.000e-9, b10_systemConfig.getWavelength(), DELTA_MILLI_NM);
        assertEquals("new", b10_systemConfig.getSystemId());
        assertEquals("[la1, mcp, ti1, swm, met, cac]", b10_systemConfig.getComponents().toString());
        assertEquals("C0 0    532.000 new la1 mcp ti1 swm met cac", b10_systemConfig.toCrdString());

        assertEquals("la1", b10_laserConfig.getLaserId());
        assertEquals("Nd:Yag", b10_laserConfig.getLaserType());
        assertEquals(1064.00e-9, b10_laserConfig.getPrimaryWavelength(), DELTA_MILLI_NM);
        assertEquals(5.00, b10_laserConfig.getNominalFireRate(), DELTA_CENTI);
        assertEquals(100.00, b10_laserConfig.getPulseEnergy(), DELTA_CENTI);
        assertEquals(150.0, b10_laserConfig.getPulseWidth(), DELTA_CENTI);
        assertEquals(15.00, b10_laserConfig.getBeamDivergence(), DELTA_CENTI);
        assertEquals(1, b10_laserConfig.getPulseInOutgoingSemiTrain());
        assertEquals("C1 0 la1 Nd:Yag 1064.00 5.00 100.00 150.0 15.00 1", b10_laserConfig.toCrdString());

        assertEquals("mcp", b10_detectorConfig.getDetectorId());
        assertEquals("MCP-PMT", b10_detectorConfig.getDetectorType());
        assertEquals(532.00e-9, b10_detectorConfig.getApplicableWavelength(), DELTA_MILLI_NM);
        assertEquals(15.0, b10_detectorConfig.getQuantumEfficiency(), DELTA_CENTI);
        assertEquals(3488.3, b10_detectorConfig.getAppliedVoltage(), 0.1);
        assertEquals(31.0e3, b10_detectorConfig.getDarkCount(), 0.1e3);
        assertEquals("analog", b10_detectorConfig.getOutputPulseType());
        assertEquals(400.0, b10_detectorConfig.getOutputPulseWidth(), 0.1);
        assertEquals(1.0e-9, b10_detectorConfig.getSpectralFilter(), 0.1e-9);
        assertEquals(80.0, b10_detectorConfig.getTransmissionOfSpectralFilter(), 0.1);
        assertEquals(30.00, b10_detectorConfig.getSpatialFilter(), DELTA_CENTI);
        assertEquals("none", b10_detectorConfig.getExternalSignalProcessing());
        assertEquals(Double.NaN, b10_detectorConfig.getAmplifierGain(), 0.1);
        assertEquals(Double.NaN, b10_detectorConfig.getAmplifierBandwidth(), 0.1e3);
        assertEquals("0", b10_detectorConfig.getAmplifierInUse());
        assertEquals("C2 0 mcp MCP-PMT 532.000 15.00 3488.3 31.0 analog 400.0 1.00 80.0 30.0 none na na 0", 
                b10_detectorConfig.toCrdString());

        assertEquals("ti1", b10_timingConfig.getLocalTimingId());
        assertEquals("Truetime_XLDC", b10_timingConfig.getTimeSource());
        assertEquals("Truetime_XLDC", b10_timingConfig.getFrequencySource());
        assertEquals("Cybi_ETM", b10_timingConfig.getTimer());
        assertEquals("na", b10_timingConfig.getTimerSerialNumber());
        assertEquals(0.0, b10_timingConfig.getEpochDelayCorrection(), DELTA_TENTH_US);
        assertEquals("C3 0 ti1 Truetime_XLDC Truetime_XLDC Cybi_ETM na 0.0", b10_timingConfig.toCrdString());

        assertEquals("swm", b10_softwareConfig.getConfigurationId());
        assertEquals("swm", b10_softwareConfig.getSoftwareId());
        assertEquals("[sattrk]", Arrays.toString(b10_softwareConfig.getTrackingSoftwares()));
        assertEquals("[6.10]", Arrays.toString(b10_softwareConfig.getTrackingSoftwareVersions()));
        assertEquals("[HPLDP, GNP]", Arrays.toString(b10_softwareConfig.getProcessingSoftwares()));
        assertEquals("[9.11.3, 2.8.3]", Arrays.toString(b10_softwareConfig.getProcessingSoftwareVersions()));
        assertEquals("C5 0 swm sattrk 6.10 HPLDP,GNP 9.11.3,2.8.3", b10_softwareConfig.toCrdString());

        assertEquals("met", b10_meteorologicalConfig.getConfigurationId());
        assertEquals("met", b10_meteorologicalConfig.getMeteorologicalId());
        assertEquals("Paroscientific", b10_meteorologicalConfig.getPressSensorManufacturer());
        assertEquals("MET-4", b10_meteorologicalConfig.getPressSensorModel());
        assertEquals("106772", b10_meteorologicalConfig.getPressSensorSerialNumber());
        assertEquals("Paroscientific", b10_meteorologicalConfig.getTempSensorManufacturer());
        assertEquals("MET-4", b10_meteorologicalConfig.getTempSensorModel());
        assertEquals("106772", b10_meteorologicalConfig.getTempSensorSerialNumber());
        assertEquals("Paroscientific", b10_meteorologicalConfig.getHumiSensorManufacturer());
        assertEquals("MET-4", b10_meteorologicalConfig.getHumiSensorModel());
        assertEquals("106772", b10_meteorologicalConfig.getHumiSensorSerialNumber());
        assertEquals("C6 0 met Paroscientific MET-4 106772 Paroscientific MET-4 106772 Paroscientific MET-4 106772", 
                b10_meteorologicalConfig.toCrdString());

        assertEquals("cac", b10_calibrationTargetConfig.getConfigurationId());
        assertEquals("B", b10_calibrationTargetConfig.getTargetName());
        assertEquals(150.42450, b10_calibrationTargetConfig.getSurveyedTargetDistance(), DELTA_TENTH_MM);
        assertEquals(1.0e-3, b10_calibrationTargetConfig.getSurveyError(), DELTA_TENTH_MM);
        assertEquals(0.1651, b10_calibrationTargetConfig.getSumOfAllConstantDelays(), DELTA_TENTH_MM);
        assertEquals(Double.NaN, b10_calibrationTargetConfig.getPulseEnergy(), DELTA_CENTI);
        assertEquals("HPLDP", b10_calibrationTargetConfig.getProcessingSoftwareName());
        assertEquals("9.11.3", b10_calibrationTargetConfig.getProcessingSoftwareVersion());
        assertEquals("C7 0 cac B 150.42450 1.00 0.1651 na HPLDP 9.11.3", b10_calibrationTargetConfig.toCrdString());

        final List<CalibrationDetail> b10_calibrationDetailData = block10.getCalibrationDetailData();
        assertEquals(2, b10_calibrationDetailData.size());
        final List<CalibrationDetail> b10_calibrationDetails = block10.getCalibrationDetailRecords();
        assertEquals(2, b10_calibrationDetails.size());
        final CalibrationDetail b10_calibrationDetail0 = b10_calibrationDetails.get(0);
        assertEquals("41  7907.550577400252 0  new      822      819 150.4245    96809.6      na   18.0     na     na    na 2 2 0 1   na",
                b10_calibrationDetail0.toCrdString());
        assertTrue(block10.getCalibrationDetailRecords("std").isEmpty());
        assertEquals(2, block10.getCalibrationDetailRecords("new").size());

        final CRDHeader b10_header = block10.getHeader();
        assertEquals("H1 CRD  2 2022 05 01 03", b10_header.getH1CrdString());
        assertEquals("H2 YARL 7090 05 13  3 ILRS", b10_header.getH2CrdString());
        assertEquals("H3 lageos2 9207002 5986 22195 0 1  1", b10_header.getH3CrdString());
        assertEquals("H4  1 2022 05 01 02 18 58 2022 05 01 02 24 03 0 0 0 0 1 0 2 0", b10_header.getH4CrdString());
        assertEquals("H5  1 22 043000 HTS 12001", b10_header.getH5CrdString());

        final CRDConfiguration config = new CRDConfiguration();
        assertTrue(config.getSystemConfigurationRecords().isEmpty());
        assertNull(config.getSystemRecord());
        assertNull(config.getSystemRecord(null));
        assertNull(config.getSystemRecord("std"));
        
        config.addConfigurationRecord(b3_systemConfig_std1);
        config.addConfigurationRecord(b3_systemConfig_std2);
        assertEquals(2, config.getSystemConfigurationRecords().size());
        assertSame(b3_systemConfig_std1, config.getSystemRecord());        

        final RangeMeasurement range_new = new RangeMeasurement(b10_npt0.getDate(), 
                b10_npt0.getTimeOfFlight(), b10_npt0.getEpochEvent());
        assertEquals(0.040190018544,range_new.getTimeOfFlight(), DELTA_PS);
        assertEquals(8357.400568200001, 
                range_new.getDate().getComponents(utc).getTime().getSecondsInUTCDay(), DELTA_PS);
        final RangeMeasurement range_new2 = new RangeMeasurement(b10_npt0.getDate(), 
                b10_npt0.getTimeOfFlight(), b10_npt0.getEpochEvent(), b10_npt0.getSnr());
        assertEquals("00 not supported. use NptRangeMeasurement or FrRangeMeasurement instead.", 
                range_new2.toCrdString());

        final NptRangeMeasurement npt_new = new NptRangeMeasurement(b10_npt0.getDate(), 
                b10_npt0.getTimeOfFlight(), b10_npt0.getEpochEvent(), b10_npt0.getSnr(),
                b10_npt0.getSystemConfigurationId());
        assertEquals("11  8357.400568200000     0.040190018544  new 2   -1.0     -1       na     na     na       na   na 0   na", 
                npt_new.toCrdString());

        assertEquals(0, DataType.FULL_RATE.getIndicator());
        assertEquals(1, DataType.NORMAL_POINT.getIndicator());
        assertEquals(2, DataType.SAMPLED_ENGIEERING.getIndicator());
        assertEquals(DataType.FULL_RATE, DataType.getDataType(0));
        assertEquals(DataType.NORMAL_POINT, DataType.getDataType(1));
        assertEquals(DataType.SAMPLED_ENGIEERING, DataType.getDataType(2));
        try {
            DataType.getDataType(3);
            fail("an exception should have been thrown");
        } catch (RuntimeException oe) {
        }

        // block11: real npt, monthE=-1
        final CRDDataBlock block11 = dataBlocks.get(11);
        final AbsoluteDate startEpoch = new AbsoluteDate("2012-01-16T03:11:54", utc);
        final AbsoluteDate endEpoch = new AbsoluteDate("2012-01-16T00:00:00", utc).shiftedBy(11532.317500081099);
        assertEquals(1, block11.getHeader().getVersion());
        assertEquals(startEpoch, block11.getHeader().getStartEpoch());
        assertEquals(endEpoch, block11.getHeader().getEndEpoch());
    }

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
