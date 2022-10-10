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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ilrs.CRD.AnglesMeasurement;
import org.orekit.files.ilrs.CRD.CRDDataBlock;
import org.orekit.files.ilrs.CRD.Meteo;
import org.orekit.files.ilrs.CRD.MeteorologicalMeasurement;
import org.orekit.files.ilrs.CRD.NptRangeMeasurement;
import org.orekit.files.ilrs.CRD.RangeMeasurement;
import org.orekit.files.ilrs.CRDConfiguration.TransponderConfiguration;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

public class CRDParserTest {

    @Test
    public void testInvalidFormat() throws URISyntaxException, IOException {
        try {
            final String ex = "/ilrs/crd_invalid_format.v2C";
            final CRDParser parser = new CRDParser();
            parser.parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNEXPECTED_FORMAT_FOR_ILRS_FILE,
                                oe.getSpecifier());
            Assertions.assertEquals("CRD", oe.getParts()[0]);
            Assertions.assertEquals("CPF", oe.getParts()[1]);
        }
    }

    @Test
    public void testMissingEOF() throws IOException, URISyntaxException {
        try {
            final String ex = "/ilrs/crd_unexpected_end_of_file.v2C";
            final CRDParser parser = new CRDParser();
            final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
            parser.parse(new DataSource(fileName));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CRD_UNEXPECTED_END_OF_FILE,
                                oe.getSpecifier());
            Assertions.assertEquals(23, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    public void testCorruptedData() throws IOException, URISyntaxException {
        try {
            final String ex = "/ilrs/crd_corrupted_data.v2C";
            final CRDParser parser = new CRDParser();
            parser.parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                oe.getSpecifier());
            Assertions.assertEquals(19, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    public void testInvalidRangeType() throws IOException, URISyntaxException {
        try {
            final String ex = "/ilrs/crd_invalid_range_type.v2C";
            final CRDParser parser = new CRDParser();
            parser.parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INVALID_RANGE_INDICATOR_IN_CRD_FILE,
                                oe.getSpecifier());
            Assertions.assertEquals(9, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    public void testLageos2Version2() throws URISyntaxException, IOException {

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
        Assertions.assertEquals(2, firstHeader.getVersion());
        Assertions.assertEquals(2018, firstHeader.getProductionEpoch().getYear());
        Assertions.assertEquals(2, firstHeader.getProductionEpoch().getMonth());
        Assertions.assertEquals(1, firstHeader.getProductionEpoch().getDay());
        Assertions.assertEquals(17, firstHeader.getProductionHour());
        Assertions.assertEquals("CHAL", firstHeader.getStationName());
        Assertions.assertEquals(9998, firstHeader.getSystemIdentifier());
        Assertions.assertEquals(19, firstHeader.getSystemNumber());
        Assertions.assertEquals(1, firstHeader.getSystemOccupancy());
        Assertions.assertEquals(4, firstHeader.getEpochIdentifier());
        Assertions.assertEquals("WPLTN", firstHeader.getStationNetword());
        Assertions.assertEquals("lageos2", firstHeader.getName());
        Assertions.assertEquals("9207002", firstHeader.getIlrsSatelliteId());
        Assertions.assertEquals("5986", firstHeader.getSic());
        Assertions.assertEquals("22195", firstHeader.getNoradId());
        Assertions.assertEquals(0, firstHeader.getSpacecraftEpochTimeScale());
        Assertions.assertEquals(1, firstHeader.getTargetClass());
        Assertions.assertEquals(1, firstHeader.getTargetLocation());
        Assertions.assertEquals(1, firstHeader.getDataType());
        Assertions.assertEquals(0.0, firstHeader.getStartEpoch().durationFrom(firstStartDate), 1.0e-5);
        Assertions.assertEquals(0.0, firstHeader.getEndEpoch().durationFrom(firstEndDate), 1.0e-5);
        Assertions.assertEquals(0, firstHeader.getDataReleaseFlag());
        Assertions.assertFalse(firstHeader.isTroposphericRefractionApplied());
        Assertions.assertFalse(firstHeader.isCenterOfMassCorrectionApplied());
        Assertions.assertFalse(firstHeader.isReceiveAmplitudeCorrectionApplied());
        Assertions.assertTrue(firstHeader.isStationSystemDelayApplied());
        Assertions.assertFalse(firstHeader.isTransponderDelayApplied());
        Assertions.assertEquals(2, firstHeader.getRangeType().getIndicator());
        Assertions.assertEquals(0, firstHeader.getQualityIndicator());
        Assertions.assertEquals(1, firstHeader.getPredictionType());
        Assertions.assertEquals(18, firstHeader.getYearOfCentury());
        Assertions.assertEquals("020115", firstHeader.getDateAndTime());
        Assertions.assertEquals("hts", firstHeader.getPredictionProvider());
        Assertions.assertEquals(3202, firstHeader.getSequenceNumber());

        // Configuration records
        Assertions.assertEquals(532.000 * 1.0e-9, firstConf.getSystemRecord().getWavelength(), 1.0e-15);
        Assertions.assertEquals("std", firstConf.getSystemRecord().getSystemId());
        Assertions.assertEquals("CL1", firstConf.getLaserRecord().getLaserId());
        Assertions.assertEquals("RG30-L", firstConf.getLaserRecord().getLaserType());
        Assertions.assertEquals(1064.00 * 1.0e-9, firstConf.getLaserRecord().getPrimaryWavelength(), 1.0e-15);
        Assertions.assertEquals(1000.00, firstConf.getLaserRecord().getNominalFireRate(), 1.0e-15);
        Assertions.assertEquals(1.50, firstConf.getLaserRecord().getPulseEnergy(), 1.0e-15);
        Assertions.assertEquals(10.0, firstConf.getLaserRecord().getPulseWidth(), 1.0e-15);
        Assertions.assertEquals(92.82, firstConf.getLaserRecord().getBeamDivergence(), 1.0e-15);
        Assertions.assertEquals(0, firstConf.getLaserRecord().getPulseInOutgoingSemiTrain());
        Assertions.assertEquals("CD1", firstConf.getDetectorRecord().getDetectorId());
        Assertions.assertEquals("CSPAD", firstConf.getDetectorRecord().getDetectorType());
        Assertions.assertEquals(532.000 * 1.0e-9, firstConf.getDetectorRecord().getApplicableWavelength(), 1.0e-15);
        Assertions.assertEquals(20.0, firstConf.getDetectorRecord().getQuantumEfficiency(), 1.0e-15);
        Assertions.assertEquals(5.0, firstConf.getDetectorRecord().getAppliedVoltage(), 1.0e-15);
        Assertions.assertEquals(60000.0, firstConf.getDetectorRecord().getDarkCount(), 1.0e-15);
        Assertions.assertEquals("TTL", firstConf.getDetectorRecord().getOutputPulseType());
        Assertions.assertEquals(0.0, firstConf.getDetectorRecord().getOutputPulseWidth(), 1.0e-15);
        Assertions.assertEquals(1.70 * 1.0e-9, firstConf.getDetectorRecord().getSpectralFilter(), 1.0e-15);
        Assertions.assertEquals(0.0, firstConf.getDetectorRecord().getTransmissionOfSpectralFilter(), 1.0e-15);
        Assertions.assertEquals(0.0, firstConf.getDetectorRecord().getSpatialFilter(), 1.0e-15);
        Assertions.assertEquals("none", firstConf.getDetectorRecord().getExternalSignalProcessing());
        Assertions.assertEquals(0.0, firstConf.getDetectorRecord().getAmplifierGain(), 1.0e-15);
        Assertions.assertEquals(0.0, firstConf.getDetectorRecord().getAmplifierBandwidth(), 1.0e-15);
        Assertions.assertEquals("0", firstConf.getDetectorRecord().getAmplifierInUse());
        Assertions.assertEquals("CT1", firstConf.getTimingRecord().getLocalTimingId());
        Assertions.assertEquals("Meridian", firstConf.getTimingRecord().getTimeSource());
        Assertions.assertEquals("Meridian", firstConf.getTimingRecord().getFrequencySource());
        Assertions.assertEquals("ET-A032", firstConf.getTimingRecord().getTimer());
        Assertions.assertEquals("003309", firstConf.getTimingRecord().getTimerSerialNumber());
        Assertions.assertEquals(0.0, firstConf.getTimingRecord().getEpochDelayCorrection(), 1.0e-15);
        Assertions.assertEquals("pgms", firstConf.getSoftwareRecord().getSoftwareId());
        Assertions.assertEquals("Sattrk", firstConf.getSoftwareRecord().getTrackingSoftwares()[1]);
        Assertions.assertEquals("2.00Cm", firstConf.getSoftwareRecord().getTrackingSoftwareVersions()[1]);
        Assertions.assertEquals("crd_cal", firstConf.getSoftwareRecord().getProcessingSoftwares()[1]);
        Assertions.assertEquals("1.7", firstConf.getSoftwareRecord().getProcessingSoftwareVersions()[1]);
        Assertions.assertEquals("mets", firstConf.getMeteorologicalRecord().getMeteorologicalId());
        Assertions.assertEquals("Paroscientific", firstConf.getMeteorologicalRecord().getPressSensorManufacturer());
        Assertions.assertEquals("Met4a", firstConf.getMeteorologicalRecord().getPressSensorModel());
        Assertions.assertEquals("123456", firstConf.getMeteorologicalRecord().getPressSensorSerialNumber());
        Assertions.assertEquals("Paroscientific", firstConf.getMeteorologicalRecord().getTempSensorManufacturer());
        Assertions.assertEquals("Met4a", firstConf.getMeteorologicalRecord().getTempSensorModel());
        Assertions.assertEquals("123456", firstConf.getMeteorologicalRecord().getTempSensorSerialNumber());
        Assertions.assertEquals("Paroscientific", firstConf.getMeteorologicalRecord().getHumiSensorManufacturer());
        Assertions.assertEquals("Met4a", firstConf.getMeteorologicalRecord().getHumiSensorModel());
        Assertions.assertEquals("123456", firstConf.getMeteorologicalRecord().getHumiSensorSerialNumber());
        Assertions.assertNull(firstConf.getTransponderRecord());

        // Meteorological data (only one data)
        final MeteorologicalMeasurement meteoFirst = first.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH);
        Assertions.assertEquals(56940.0, meteoFirst.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals(0.9989, meteoFirst.getPressure(), 1.0e-15);
        Assertions.assertEquals(259.10, meteoFirst.getTemperature(), 1.0e-15);
        Assertions.assertEquals(80.0, meteoFirst.getHumidity(), 1.0e-15);

        // Range data
        final RangeMeasurement rangeFirst1 = first.getRangeData().get(0);
        Assertions.assertEquals(54927.620161400002, rangeFirst1.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals(0.044106029140, rangeFirst1.getTimeOfFlight(), 1.0e-15);
        Assertions.assertEquals(2, rangeFirst1.getEpochEvent());
        Assertions.assertEquals(5.7, rangeFirst1.getSnr(), 1.0e-15);
        final RangeMeasurement rangeFirst2 = first.getRangeData().get(5);
        Assertions.assertEquals(56899.718161400000, rangeFirst2.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals(0.050148193335, rangeFirst2.getTimeOfFlight(), 1.0e-15);
        Assertions.assertEquals(2, rangeFirst2.getEpochEvent());
        Assertions.assertEquals(5.7, rangeFirst2.getSnr(), 1.0e-15);

        // Angles data
        Assertions.assertEquals(0, first.getAnglesData().size());

        // Verify last data block
        final CRDDataBlock last = file.getDataBlocks().get(file.getDataBlocks().size() - 1);
        final CRDHeader lastHeader = last.getHeader();
        final CRDConfiguration lastConf = last.getConfigurationRecords();
        final AbsoluteDate lastStartDate = new AbsoluteDate("2018-02-27T14:10:10.000", TimeScalesFactory.getUTC());
        final AbsoluteDate lastEndDate   = new AbsoluteDate("2018-02-27T14:39:06.000", TimeScalesFactory.getUTC());

        // Header
        Assertions.assertEquals(2, lastHeader.getVersion());
        Assertions.assertEquals(2018, lastHeader.getProductionEpoch().getYear());
        Assertions.assertEquals(2, lastHeader.getProductionEpoch().getMonth());
        Assertions.assertEquals(27, lastHeader.getProductionEpoch().getDay());
        Assertions.assertEquals(14, lastHeader.getProductionHour());
        Assertions.assertEquals("CHAL", lastHeader.getStationName());
        Assertions.assertEquals(9998, lastHeader.getSystemIdentifier());
        Assertions.assertEquals(19, lastHeader.getSystemNumber());
        Assertions.assertEquals(1, lastHeader.getSystemOccupancy());
        Assertions.assertEquals(4, lastHeader.getEpochIdentifier());
        Assertions.assertEquals("WPLTN", lastHeader.getStationNetword());
        Assertions.assertEquals("lageos2", lastHeader.getName());
        Assertions.assertEquals("9207002", lastHeader.getIlrsSatelliteId());
        Assertions.assertEquals("5986", lastHeader.getSic());
        Assertions.assertEquals("22195", lastHeader.getNoradId());
        Assertions.assertEquals(0, lastHeader.getSpacecraftEpochTimeScale());
        Assertions.assertEquals(1, lastHeader.getTargetClass());
        Assertions.assertEquals(1, lastHeader.getTargetLocation());
        Assertions.assertEquals(1, lastHeader.getDataType());
        Assertions.assertEquals(0.0, lastHeader.getStartEpoch().durationFrom(lastStartDate), 1.0e-5);
        Assertions.assertEquals(0.0, lastHeader.getEndEpoch().durationFrom(lastEndDate), 1.0e-5);
        Assertions.assertEquals(0, lastHeader.getDataReleaseFlag());
        Assertions.assertFalse(lastHeader.isTroposphericRefractionApplied());
        Assertions.assertFalse(lastHeader.isCenterOfMassCorrectionApplied());
        Assertions.assertFalse(lastHeader.isReceiveAmplitudeCorrectionApplied());
        Assertions.assertTrue(lastHeader.isStationSystemDelayApplied());
        Assertions.assertFalse(lastHeader.isTransponderDelayApplied());
        Assertions.assertEquals(2, lastHeader.getRangeType().getIndicator());
        Assertions.assertEquals(0, lastHeader.getQualityIndicator());
        Assertions.assertEquals(1, lastHeader.getPredictionType());
        Assertions.assertEquals(18, lastHeader.getYearOfCentury());
        Assertions.assertEquals("022714", lastHeader.getDateAndTime());
        Assertions.assertEquals("hts", lastHeader.getPredictionProvider());
        Assertions.assertEquals(5802, lastHeader.getSequenceNumber());

        // Configuration records
        Assertions.assertEquals(532.000 * 1.0e-9, lastConf.getSystemRecord().getWavelength(), 1.0e-15);
        Assertions.assertEquals("std", lastConf.getSystemRecord().getSystemId());
        Assertions.assertEquals("CL1", lastConf.getLaserRecord().getLaserId());
        Assertions.assertEquals("RG30-L", lastConf.getLaserRecord().getLaserType());
        Assertions.assertEquals(1064.00 * 1.0e-9, lastConf.getLaserRecord().getPrimaryWavelength(), 1.0e-15);
        Assertions.assertEquals(1000.00, lastConf.getLaserRecord().getNominalFireRate(), 1.0e-15);
        Assertions.assertEquals(1.50, lastConf.getLaserRecord().getPulseEnergy(), 1.0e-15);
        Assertions.assertEquals(10.0, lastConf.getLaserRecord().getPulseWidth(), 1.0e-15);
        Assertions.assertEquals(92.82, lastConf.getLaserRecord().getBeamDivergence(), 1.0e-15);
        Assertions.assertEquals(0, lastConf.getLaserRecord().getPulseInOutgoingSemiTrain());
        Assertions.assertEquals("CD1", lastConf.getDetectorRecord().getDetectorId());
        Assertions.assertEquals("CSPAD", lastConf.getDetectorRecord().getDetectorType());
        Assertions.assertEquals(532.000 * 1.0e-9, lastConf.getDetectorRecord().getApplicableWavelength(), 1.0e-15);
        Assertions.assertEquals(20.0, lastConf.getDetectorRecord().getQuantumEfficiency(), 1.0e-15);
        Assertions.assertEquals(5.0, lastConf.getDetectorRecord().getAppliedVoltage(), 1.0e-15);
        Assertions.assertEquals(60000.0, lastConf.getDetectorRecord().getDarkCount(), 1.0e-15);
        Assertions.assertEquals("TTL", lastConf.getDetectorRecord().getOutputPulseType());
        Assertions.assertEquals(0.0, lastConf.getDetectorRecord().getOutputPulseWidth(), 1.0e-15);
        Assertions.assertEquals(1.70 * 1.0e-9, lastConf.getDetectorRecord().getSpectralFilter(), 1.0e-15);
        Assertions.assertEquals(0.0, lastConf.getDetectorRecord().getTransmissionOfSpectralFilter(), 1.0e-15);
        Assertions.assertEquals(0.0, lastConf.getDetectorRecord().getSpatialFilter(), 1.0e-15);
        Assertions.assertEquals("none", lastConf.getDetectorRecord().getExternalSignalProcessing());
        Assertions.assertEquals(0.0, lastConf.getDetectorRecord().getAmplifierGain(), 1.0e-15);
        Assertions.assertEquals(0.0, lastConf.getDetectorRecord().getAmplifierBandwidth(), 1.0e-15);
        Assertions.assertEquals("0", lastConf.getDetectorRecord().getAmplifierInUse());
        Assertions.assertEquals("CT1", lastConf.getTimingRecord().getLocalTimingId());
        Assertions.assertEquals("Meridian", lastConf.getTimingRecord().getTimeSource());
        Assertions.assertEquals("Meridian", lastConf.getTimingRecord().getFrequencySource());
        Assertions.assertEquals("ET-A032", lastConf.getTimingRecord().getTimer());
        Assertions.assertEquals("003309", lastConf.getTimingRecord().getTimerSerialNumber());
        Assertions.assertEquals(0.0, lastConf.getTimingRecord().getEpochDelayCorrection(), 1.0e-15);
        Assertions.assertEquals("pgms", lastConf.getSoftwareRecord().getSoftwareId());
        Assertions.assertEquals("Sattrk", lastConf.getSoftwareRecord().getTrackingSoftwares()[1]);
        Assertions.assertEquals("2.00Cm", lastConf.getSoftwareRecord().getTrackingSoftwareVersions()[1]);
        Assertions.assertEquals("crd_cal", lastConf.getSoftwareRecord().getProcessingSoftwares()[1]);
        Assertions.assertEquals("1.7", lastConf.getSoftwareRecord().getProcessingSoftwareVersions()[1]);
        Assertions.assertEquals("mets", lastConf.getMeteorologicalRecord().getMeteorologicalId());
        Assertions.assertEquals("Paroscientific", lastConf.getMeteorologicalRecord().getPressSensorManufacturer());
        Assertions.assertEquals("Met4a", lastConf.getMeteorologicalRecord().getPressSensorModel());
        Assertions.assertEquals("123456", lastConf.getMeteorologicalRecord().getPressSensorSerialNumber());
        Assertions.assertEquals("Paroscientific", lastConf.getMeteorologicalRecord().getTempSensorManufacturer());
        Assertions.assertEquals("Met4a", lastConf.getMeteorologicalRecord().getTempSensorModel());
        Assertions.assertEquals("123456", lastConf.getMeteorologicalRecord().getTempSensorSerialNumber());
        Assertions.assertEquals("Paroscientific", lastConf.getMeteorologicalRecord().getHumiSensorManufacturer());
        Assertions.assertEquals("Met4a", lastConf.getMeteorologicalRecord().getHumiSensorModel());
        Assertions.assertEquals("123456", lastConf.getMeteorologicalRecord().getHumiSensorSerialNumber());
        Assertions.assertNull(lastConf.getTransponderRecord());

        // Meteorological data (only one data)
        final MeteorologicalMeasurement meteoLast = last.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH);
        Assertions.assertEquals(52749.0, meteoLast.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals(0.9921, meteoLast.getPressure(), 1.0e-15);
        Assertions.assertEquals(260.80, meteoLast.getTemperature(), 1.0e-15);
        Assertions.assertEquals(67.0, meteoLast.getHumidity(), 1.0e-15);

        // Range data
        final RangeMeasurement rangeLast1 = last.getRangeData().get(0);
        Assertions.assertEquals(51080.935001603524, rangeLast1.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals(0.045673108965, rangeLast1.getTimeOfFlight(), 1.0e-15);
        Assertions.assertEquals(2, rangeLast1.getEpochEvent());
        Assertions.assertEquals(5.7, rangeLast1.getSnr(), 1.0e-15);
        final RangeMeasurement rangeLast2 = last.getRangeData().get(13);
        Assertions.assertEquals(52618.095001597932, rangeLast2.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals(0.042733272755, rangeLast2.getTimeOfFlight(), 1.0e-15);
        Assertions.assertEquals(2, rangeLast2.getEpochEvent());
        Assertions.assertEquals(5.7, rangeLast2.getSnr(), 1.0e-15);

        // Angles data
        Assertions.assertEquals(0, first.getAnglesData().size());

    }

    @Test
    public void testChampVersion1() throws URISyntaxException, IOException {

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
        Assertions.assertEquals(1, header.getVersion());
        Assertions.assertEquals(2017, header.getProductionEpoch().getYear());
        Assertions.assertEquals(9, header.getProductionEpoch().getMonth());
        Assertions.assertEquals(26, header.getProductionEpoch().getDay());
        Assertions.assertEquals(4, header.getProductionHour());
        Assertions.assertEquals("STL3", header.getStationName());
        Assertions.assertEquals(7825, header.getSystemIdentifier());
        Assertions.assertEquals(90, header.getSystemNumber());
        Assertions.assertEquals(1, header.getSystemOccupancy());
        Assertions.assertEquals(4, header.getEpochIdentifier());
        Assertions.assertEquals("champ", header.getName());
        Assertions.assertEquals("0003902", header.getIlrsSatelliteId());
        Assertions.assertEquals("8002", header.getSic());
        Assertions.assertEquals("026405", header.getNoradId());
        Assertions.assertEquals(0, header.getSpacecraftEpochTimeScale());
        Assertions.assertEquals(1, header.getTargetClass());
        Assertions.assertEquals(0, header.getDataType());
        Assertions.assertEquals(0.0, header.getStartEpoch().durationFrom(startDate), 1.0e-5);
        Assertions.assertEquals(0.0, header.getEndEpoch().durationFrom(endDate), 1.0e-5);
        Assertions.assertEquals(0, header.getDataReleaseFlag());
        Assertions.assertFalse(header.isTroposphericRefractionApplied());
        Assertions.assertFalse(header.isCenterOfMassCorrectionApplied());
        Assertions.assertFalse(header.isReceiveAmplitudeCorrectionApplied());
        Assertions.assertTrue(header.isStationSystemDelayApplied());
        Assertions.assertFalse(header.isTransponderDelayApplied());
        Assertions.assertEquals(2, header.getRangeType().getIndicator());
        Assertions.assertEquals(0, header.getQualityIndicator());

        // Configuration records
        Assertions.assertEquals(532.100 * 1.0e-9, conf.getSystemRecord().getWavelength(), 1.0e-15);
        Assertions.assertEquals("IDAA", conf.getSystemRecord().getSystemId());
        Assertions.assertEquals("IDAB", conf.getLaserRecord().getLaserId());
        Assertions.assertEquals("Nd-YAG", conf.getLaserRecord().getLaserType());
        Assertions.assertEquals(532.10 * 1.0e-9, conf.getLaserRecord().getPrimaryWavelength(), 1.0e-15);
        Assertions.assertEquals(0.0, conf.getLaserRecord().getNominalFireRate(), 1.0e-15);
        Assertions.assertEquals(21.0, conf.getLaserRecord().getPulseEnergy(), 1.0e-15);
        Assertions.assertEquals(12.0, conf.getLaserRecord().getPulseWidth(), 1.0e-15);
        Assertions.assertEquals(0.0, conf.getLaserRecord().getBeamDivergence(), 1.0e-15);
        Assertions.assertEquals(1, conf.getLaserRecord().getPulseInOutgoingSemiTrain());
        Assertions.assertEquals("IDAJ", conf.getDetectorRecord().getDetectorId());
        Assertions.assertEquals("CSPAD", conf.getDetectorRecord().getDetectorType());
        Assertions.assertEquals(532.000 * 1.0e-9, conf.getDetectorRecord().getApplicableWavelength(), 1.0e-15);
        Assertions.assertEquals(20.0, conf.getDetectorRecord().getQuantumEfficiency(), 1.0e-15);
        Assertions.assertEquals(11.0, conf.getDetectorRecord().getAppliedVoltage(), 1.0e-15);
        Assertions.assertEquals(100000.0, conf.getDetectorRecord().getDarkCount(), 1.0e-15);
        Assertions.assertEquals("ECL", conf.getDetectorRecord().getOutputPulseType());
        Assertions.assertEquals(12.0, conf.getDetectorRecord().getOutputPulseWidth(), 1.0e-15);
        Assertions.assertEquals(2.00 * 1.0e-9, conf.getDetectorRecord().getSpectralFilter(), 1.0e-15);
        Assertions.assertEquals(90.0, conf.getDetectorRecord().getTransmissionOfSpectralFilter(), 1.0e-15);
        Assertions.assertEquals(12.0, conf.getDetectorRecord().getSpatialFilter(), 1.0e-15);
        Assertions.assertEquals("Manual", conf.getDetectorRecord().getExternalSignalProcessing());
        Assertions.assertEquals("IDAV", conf.getTimingRecord().getLocalTimingId());
        Assertions.assertEquals("TrueTime_XLi", conf.getTimingRecord().getTimeSource());
        Assertions.assertEquals("TrueTime_OCXO", conf.getTimingRecord().getFrequencySource());
        Assertions.assertEquals("MRCS", conf.getTimingRecord().getTimer());
        Assertions.assertEquals("NA", conf.getTimingRecord().getTimerSerialNumber());
        Assertions.assertEquals(0.2322 * 1.0e-6, conf.getTimingRecord().getEpochDelayCorrection(), 1.0e-15);

        // Meteorological data
        final MeteorologicalMeasurement meteoFirst = block.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH);
        Assertions.assertEquals(14353.388283000000, meteoFirst.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals(0.92374, meteoFirst.getPressure(), 1.0e-15);
        Assertions.assertEquals(289.42, meteoFirst.getTemperature(), 1.0e-15);
        Assertions.assertEquals(28.1, meteoFirst.getHumidity(), 1.0e-15);

        // Range data
        Assertions.assertEquals(4, block.getRangeData().size());
        final RangeMeasurement range = block.getRangeData().get(0);
        Assertions.assertEquals(14487.343206247217, range.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals(0.003603959600, range.getTimeOfFlight(), 1.0e-15);
        Assertions.assertEquals(2, range.getEpochEvent());

        // Angles data
        Assertions.assertEquals(4, block.getAnglesData().size());
        final AnglesMeasurement angles = block.getAnglesData().get(0);
        Assertions.assertEquals(14343.574333000000, angles.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assertions.assertEquals(FastMath.toRadians(215.000000), angles.getAzimuth(), 1.0e-15);
        Assertions.assertEquals(FastMath.toRadians(15.000010), angles.getElevation(), 1.0e-15);
        Assertions.assertEquals(0, angles.getDirectionFlag());
        Assertions.assertEquals(2, angles.getOriginIndicator());
        Assertions.assertFalse(angles.isRefractionCorrected());
        Assertions.assertEquals(Double.NaN, angles.getAzimuthRate(), 1.0e-15);
        Assertions.assertEquals(Double.NaN, angles.getElevationRate(), 1.0e-15);

    }

    @Test
    public void testAllFields() throws URISyntaxException, IOException {

        final String ex = "/ilrs/crd_all_fields.frd";

        final CRD file = new CRDParser().parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        final CRDDataBlock block = file.getDataBlocks().get(0);
        Assertions.assertEquals(0, file.getComments().size());
        Assertions.assertEquals(4, block.getRangeData().size());
        Assertions.assertEquals(4, block.getAnglesData().size());

        // Transponder
        final TransponderConfiguration transponder = block.getConfigurationRecords().getTransponderRecord();
        Assertions.assertEquals("id", transponder.getTransponderId());
        Assertions.assertEquals(0.0, transponder.getStationUTCOffset(), 1.0e-15);
        Assertions.assertEquals(0.0, transponder.getStationOscDrift(), 1.0e-15);
        Assertions.assertEquals(0.0, transponder.getTranspUTCOffset(), 1.0e-15);
        Assertions.assertEquals(0.0, transponder.getTranspOscDrift(), 1.0e-15);
        Assertions.assertEquals(0.0, transponder.getTranspClkRefTime(), 1.0e-15);
        Assertions.assertEquals(0, transponder.getSpacecraftClockAndDriftApplied());
        Assertions.assertEquals(0, transponder.getStationClockAndDriftApplied());
        Assertions.assertFalse(transponder.isSpacecraftTimeSimplified());
    }

    @Test
    public void testMeteorologicalData() {

        // Initialise an empty data block
        final CRDDataBlock block = new CRDDataBlock();

        // Verify null object
        Assertions.assertNull(block.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH));

        // Add a meteorological data entry
        final AbsoluteDate date = new AbsoluteDate("2020-10-29T11:40:00.000", TimeScalesFactory.getUTC());
        final MeteorologicalMeasurement meteoData1 = new MeteorologicalMeasurement(date, 1013.0, 273.0, 50.0);
        block.addMeteoData(meteoData1);

        // Verify values
        MeteorologicalMeasurement data1 = block.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH);
        Assertions.assertEquals(meteoData1.getPressure(),    data1.getPressure(),    1.0e-15);
        Assertions.assertEquals(meteoData1.getTemperature(), data1.getTemperature(), 1.0e-15);
        Assertions.assertEquals(meteoData1.getHumidity(),    data1.getHumidity(),    1.0e-15);
        data1 = block.getMeteoData().getMeteo(date);
        Assertions.assertEquals(meteoData1.getPressure(),    data1.getPressure(),    1.0e-15);
        Assertions.assertEquals(meteoData1.getTemperature(), data1.getTemperature(), 1.0e-15);
        Assertions.assertEquals(meteoData1.getHumidity(),    data1.getHumidity(),    1.0e-15);
        data1 = block.getMeteoData().getMeteo(date.shiftedBy(60.0));
        Assertions.assertEquals(meteoData1.getPressure(),    data1.getPressure(),    1.0e-15);
        Assertions.assertEquals(meteoData1.getTemperature(), data1.getTemperature(), 1.0e-15);
        Assertions.assertEquals(meteoData1.getHumidity(),    data1.getHumidity(),    1.0e-15);

        // Add another meteorological data entry
        final MeteorologicalMeasurement meteoData2 = new MeteorologicalMeasurement(date.shiftedBy(60.0), 1015.0, 275.0, 70.0);
        block.addMeteoData(meteoData2);

        // Verify values
        MeteorologicalMeasurement data2 = block.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH); // before first data
        Assertions.assertEquals(meteoData1.getPressure(),    data2.getPressure(),    1.0e-15);
        Assertions.assertEquals(meteoData1.getTemperature(), data2.getTemperature(), 1.0e-15);
        Assertions.assertEquals(meteoData1.getHumidity(),    data2.getHumidity(),    1.0e-15);
        data2 = block.getMeteoData().getMeteo(date.shiftedBy(180.0)); // After second data
        Assertions.assertEquals(meteoData2.getPressure(),    data2.getPressure(),    1.0e-15);
        Assertions.assertEquals(meteoData2.getTemperature(), data2.getTemperature(), 1.0e-15);
        Assertions.assertEquals(meteoData2.getHumidity(),    data2.getHumidity(),    1.0e-15);
        data2 = block.getMeteoData().getMeteo(date.shiftedBy(30.0)); // between first and second datta
        Assertions.assertEquals(1014.0,                      data2.getPressure(),    1.0e-15);
        Assertions.assertEquals(274.0,                       data2.getTemperature(), 1.0e-15);
        Assertions.assertEquals(60.0,                        data2.getHumidity(),    1.0e-15);

    }

    @Test
    public void testIssue801() throws URISyntaxException, IOException {

        final String ex = "/ilrs/crd_all_fields.frd";

        final CRD file = new CRDParser().parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        final CRDDataBlock block = file.getDataBlocks().get(0);
        Assertions.assertEquals(0, file.getComments().size());
        Assertions.assertEquals(4, block.getRangeData().size());
        Assertions.assertEquals(4, block.getAnglesData().size());

        final Meteo meteo = block.getMeteoData();
        final List<MeteorologicalMeasurement> data = meteo.getData();
        Assertions.assertEquals(1, data.size());

        final MeteorologicalMeasurement measurement = data.get(0);
        Assertions.assertEquals(0.92374, measurement.getPressure(),    0.00001);
        Assertions.assertEquals(289.42,  measurement.getTemperature(), 0.01);
        Assertions.assertEquals(28.1,    measurement.getHumidity(),    0.01);

    }

    @Test
    public void testIssue847() throws IOException {

        // Read the file
        final String ex = "/ilrs/lageos1-test.npt";
        final CRD file = new CRDParser().parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify
        Assertions.assertEquals(6, file.getComments().size());
        Assertions.assertEquals("New CFD in the STOP channel", file.getComments().get(0));
        Assertions.assertEquals("No CFD in the START channel", file.getComments().get(1));
        Assertions.assertEquals("New experimental detector (transistor) in the START channel**", file.getComments().get(2));
        Assertions.assertEquals("New CFD in the STOP channel", file.getComments().get(3));
        Assertions.assertEquals("No CFD in the START channel", file.getComments().get(4));
        Assertions.assertEquals("New experimental detector (transistor) in the START channel", file.getComments().get(5));
    }

    @Test
    public void testIssue886() throws IOException {
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


        Assertions.assertEquals(firstDate, rangeFirst.getDate());
        Assertions.assertEquals(lastDate, rangeLast.getDate());
    }

    @Test
    public void testIssue886Bis() throws IOException {
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

        Assertions.assertEquals(firstDate, rangeFirst.getDate());
        Assertions.assertEquals(lastDate, rangeLast.getDate());

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

        Assertions.assertEquals(firstDate, rangeFirst.getDate());
        Assertions.assertEquals(lastDate, rangeLast.getDate());

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

        Assertions.assertEquals(firstDate, rangeFirst.getDate());
        Assertions.assertEquals(lastDate, rangeLast.getDate());

    }
    
    @Test
    public void testIssue938() throws IOException {        
        final String ex = "/ilrs/crd201_all_samples";
        final CRD file = new CRDParser().parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify each block
        CRDDataBlock block;
        
        // Block 1
        block = file.getDataBlocks().get(0);
        String c0 = "C0 0    532.000 std1";
        String ten0 = "10 55432.041433800000     0.047960587856 std1 2 0 0 0    na    na";
        String meteo0 = "20 55432.041  801.80  28.21   39 0";
        String angles0 = "30 55432.041 297.2990  38.6340 0 2 1        na        na";
        String cal = "40 55432.041433800000 0 std1       na       na   0.000     -913.0      0.0   56.0     na     na    na 3 3 0 4   na";
        
        String h1 = "H1 CRD  2 2007 03 20 14";
        String h2 = "H2 MLRS 7080 24 19  4 NASA";
        String h3 = "H3 LAGEOS2 9207002 5986 22195 0 1  1";
        String h4 = "H4  0 2006 11 13 15 23 52 2006 11 13 15 45 35 1 1 1 1 0 0 2 0";

        Assertions.assertEquals(h1, block.getHeader().getH1CrdString());
        Assertions.assertEquals(h2, block.getHeader().getH2CrdString());
        Assertions.assertEquals(h3, block.getHeader().getH3CrdString());
        Assertions.assertEquals(h4, block.getHeader().getH4CrdString());
        
        Assertions.assertEquals(c0, block.getConfigurationRecords().getSystemRecord().toCrdString());
        Assertions.assertEquals(ten0, block.getRangeData().get(0).toCrdString());
        Assertions.assertEquals(meteo0, block.getMeteoData().getData().get(0).toCrdString());
        Assertions.assertEquals(angles0, block.getAnglesData().get(0).toCrdString());
        Assertions.assertEquals(cal, block.getCalibrationRecord().toCrdString());

        // Block 2
        block = file.getDataBlocks().get(1);
        String eleven0 = "11 55504.972803000000     0.047379676080 std1 2  120.0     18      94.0     na     na       na  0.00 0   0.0";
        meteo0 = "20 55504.973  801.80 282.10   39 1";
        String eleven7 = "11 56680.878541900000     0.045804632570 std1 2  120.0     10      55.0     na     na       na  0.00 0   0.0";
        String stat = "50 std1   86.0     na     na    na 0";
        
        Assertions.assertEquals(eleven0, block.getRangeData().get(0).toCrdString());
        Assertions.assertEquals(eleven7, block.getRangeData().get(7).toCrdString());
        Assertions.assertEquals(meteo0, block.getMeteoData().getData().get(0).toCrdString());
        Assertions.assertEquals(stat, block.getSessionStatisticRecord().toCrdString());
        
        // Block 4
        block = file.getDataBlocks().get(3);
        String c0s = "[   846.000 std1,    423.000 std2]";
        String c0_std1 = "C0 0    846.000 std1";
        String c0_std2 = "C0 0    423.000 std2";
        eleven0 = "11 27334.108089000000     0.051571851861 std1 2  120.0     36     154.0     na     na       na  0.00 0   0.0";
        String eleven1 = "11 27343.508089500000     0.051405458691 std2 2  120.0     28      79.0     na     na       na  0.00 0   0.0";
        meteo0 = "20 27334.108  923.30 275.40   43 1";
        String stat_std1 = "50 std1  165.0     na     na    na 0";
        String stat_std2 = "50 std2   78.0     na     na    na 0";
        String cal_std1 = "40 27334.108089000000 0 std1       na       na   0.000   113069.0      0.0  138.0     na     na    na 2 2 0 1   na";

        Assertions.assertEquals(c0s, block.getConfigurationRecords().getSystemConfigurationRecords().toString());
        Assertions.assertEquals(c0_std1, block.getConfigurationRecords().getSystemRecord().toCrdString());
        Assertions.assertEquals(c0_std1, block.getConfigurationRecords().getSystemRecord("std1").toCrdString());
        Assertions.assertEquals(c0_std2, block.getConfigurationRecords().getSystemRecord("std2").toCrdString());
        Assertions.assertEquals(eleven0, block.getRangeData().get(0).toCrdString());
        Assertions.assertEquals(eleven1, block.getRangeData().get(1).toCrdString());
        Assertions.assertEquals(meteo0, block.getMeteoData().getData().get(0).toCrdString());
        Assertions.assertEquals(stat_std1, block.getSessionStatisticRecord().toCrdString());
        Assertions.assertEquals(stat_std1, block.getSessionStatisticRecord("std1").toCrdString());
        Assertions.assertEquals(stat_std2, block.getSessionStatisticRecord("std2").toCrdString());
        Assertions.assertEquals(cal_std1, block.getCalibrationRecord().toCrdString());
        Assertions.assertEquals(cal_std1, block.getCalibrationRecord("std1").toCrdString());

        // Block 5
        block = file.getDataBlocks().get(4);
        h1 = "H1 CRD  2 2008 03 25 01";
        h2 = "H2 MDOL 7080 24 19  4 NASA";
        h3 = "H3 jason1  105501 4378 26997 0 1  1";
        h4 = "H4  1 2008 03 25 00 45 17 2008 03 25 00 55 09 0 0 0 0 1 0 2 0";
        String h5 = "H5  1 08 032500 esa  8401";
        c0 = "C0 0    532.000 std ml1 mcp mt1 swv met";
        String c1 = "C1 0 ml1 Nd-Yag 1064.00 10.00 100.00 200.0 na 1";
        String c2 = "C2 0 mcp mcp 532.000 na 3800.0 0.0 unknown na 0.00 na 0.0 none 5.0 10.0 1";
        String c3 = "C3 0 mt1 TAC TAC MLRS_CMOS_TMRB_TD811 na 445.9";
        String c5 = "C5 0 swv Monitor,Sattrk 2.000Bm,2.00Cm conpro,crd_cal,PoissonCRD,gnp 2.4a,1.7,2.2a,CM-2.01a";
        String c6 = "C6 0 met Paroscientific Met4 123456 Paroscientific Met4 123456 Paroscientific Met4 123456";
        cal = "40  2716.000000000000 0  std       67       58     na     -883.3      0.0   96.4   0.718  -0.126  364.4 3 3 0 3  14.5";
        String meteo1 = "20  3151.000  801.73 286.16   35 0";
        stat = "50  std   72.7   1.494  -0.536  -32.4 0";
        eleven0 = "11  2726.697640514675     0.013737698432  std 2   15.0      1      72.7   1.494  -0.536     -32.4  0.67 0  20.7";
        eleven7 = "11  3124.950255557618     0.011244819341  std 2   15.0     14      65.2   1.635   0.207       4.5  9.33 0  71.5";

        Assertions.assertEquals(h1, block.getHeader().getH1CrdString());
        Assertions.assertEquals(h2, block.getHeader().getH2CrdString());
        Assertions.assertEquals(h3, block.getHeader().getH3CrdString());
        Assertions.assertEquals(h4, block.getHeader().getH4CrdString());
        Assertions.assertEquals(h5, block.getHeader().getH5CrdString());
        Assertions.assertEquals(c0, block.getConfigurationRecords().getSystemRecord().toCrdString());
        Assertions.assertEquals(c1, block.getConfigurationRecords().getLaserRecord().toCrdString());
        Assertions.assertEquals(c2, block.getConfigurationRecords().getDetectorRecord().toCrdString());
        Assertions.assertEquals(c3, block.getConfigurationRecords().getTimingRecord().toCrdString());
        Assertions.assertEquals(c5, block.getConfigurationRecords().getSoftwareRecord().toCrdString());
        Assertions.assertEquals(c6, block.getConfigurationRecords().getMeteorologicalRecord().toCrdString());
        Assertions.assertEquals(cal, block.getCalibrationRecord().toCrdString());
        Assertions.assertEquals(meteo1, block.getMeteoData().getData().get(1).toCrdString());
        Assertions.assertEquals(stat, block.getSessionStatisticRecord().toCrdString());
        Assertions.assertEquals(11, block.getRangeData().size());
        Assertions.assertEquals(eleven0, block.getRangeData().get(0).toCrdString());
        NptRangeMeasurement npt = (NptRangeMeasurement)block.getRangeData().get(7);
        Assertions.assertEquals(eleven7, npt.toCrdString());
        Assertions.assertEquals(15.0, npt.getWindowLength());
        Assertions.assertEquals(14, npt.getNumberOfRawRanges());
        Assertions.assertEquals(65.2e-12, npt.getBinRms());
        Assertions.assertEquals(1.635, npt.getBinSkew());
        Assertions.assertEquals(0.207, npt.getBinKurtosis());
        Assertions.assertEquals(4.5e-12, npt.getBinPeakMinusMean());
        Assertions.assertEquals(9.33, npt.getReturnRate());
        Assertions.assertEquals(71.5, npt.getSnr());
        
        // Block 6
        block = file.getDataBlocks().get(5);
        String c7 = "C7 0 spi SpiderCCR na na 0.0000 80.00 crdcal 1.7";
        ten0 = "10  2726.697640514675     0.013737698432  std 2 2 0 0    na    na";
        angles0 = "30  2717.996 326.8923  32.9177 0 1 1        na        na";
        String angles3 = "30  2742.799 325.9195  36.4168 0 1 1        na        na";
        String calDetails0 = "41  1016.000000000000 0  std       37       28     na     -883.2      0.0   96.2   0.715  -0.125  364.3 3 3 0 1  15.5";
        String calDetails1 = "41  4416.000000000000 0  std       30       30     na     -883.4      0.0   96.6   0.721  -0.127  364.4 3 3 0 2  13.7";

        Assertions.assertEquals(2, block.getHeader().getVersion());
        Assertions.assertEquals(c7, block.getConfigurationRecords().getCalibrationTargetRecord().toCrdString());
        Assertions.assertEquals(ten0, block.getRangeData().get(0).toCrdString());
        Assertions.assertEquals(angles0, block.getAnglesData().get(0).toCrdString());
        Assertions.assertEquals(angles3, block.getAnglesData().get(3).toCrdString());
        Assertions.assertEquals(calDetails0, block.getCalibrationDetailData().get(0).toCrdString());
        Assertions.assertEquals(calDetails1, block.getCalibrationDetailData().get(1).toCrdString());
        
        // Block 7
        block = file.getDataBlocks().get(6);
        c0 = "C0 0    532.000 std ml1 mcp_with_amp mt1";
        c1 = "C1 0 ml1 Nd-Yag 1064.00 10.00 100.00 200.0 na 1";
        c2 = "C2 0 mcp_with_amp mcp_and_avantek_amp 532.000 na 3800.0 0.0 unknown na 0.00 na 0.0 none 5.0 10.0 0";
        c3 = "C3 0 mt1 TAC TAC MLRS_CMOS_TMRB_TD811 na 439.4";
        cal = "40 34823.000000000000 0  std      398      190     na      402.3      0.0  131.1   0.168  -0.130  494.4 3 3 0 4   na";
        stat = "50  std  178.8   1.711   0.451 -128.2 0";
        
        Assertions.assertEquals(c0, block.getConfigurationRecords().getSystemRecord().toCrdString());
        Assertions.assertEquals(c1, block.getConfigurationRecords().getLaserRecord().toCrdString());
        Assertions.assertEquals(c2, block.getConfigurationRecords().getDetectorRecord().toCrdString());
        Assertions.assertEquals(c3, block.getConfigurationRecords().getTimingRecord().toCrdString());
        Assertions.assertEquals(cal, block.getCalibrationRecord().toCrdString());
        Assertions.assertEquals(stat, block.getSessionStatisticRecord().toCrdString());

        // Block 8
        block = file.getDataBlocks().get(7);
        c0 = "C0 0    532.080 ES 10hz SPD5 GPS NA";
        c1 = "C1 0 10hz Nd-Yag 1064.16 10.00 20.00 100.0 20.00 4";
        c2 = "C2 0 SPD5 SPAD5 532.000 20.00 0.0 0.0 +0.7v 0.0 0.15 20.0 0.0 Single_fot na na na";
        c3 = "C3 0 GPS Radiocode_GPS_8000 Radiocode_GPS_8000 HxET_=_3x_dassault No_Sn 0.0";
        cal = "40 19185.120000000000 0   ES       na       na 122.977   105423.9      0.0   35.4   0.200   2.800    0.0 2 2 0 0   na";
        eleven0 = "11 19755.563535300000     0.015411425559   ES 2   30.0     42     217.0   0.000   0.000       0.0  5.40 0   na";
        String eleven10 = "11 20053.092679200000     0.015489205740   ES 2   30.0     59     185.0   0.000   0.000       0.0  7.60 0   na";
        
        Assertions.assertEquals(c0, block.getConfigurationRecords().getSystemRecord().toCrdString());
        Assertions.assertEquals(c1, block.getConfigurationRecords().getLaserRecord().toCrdString());
        Assertions.assertEquals(c2, block.getConfigurationRecords().getDetectorRecord().toCrdString());
        Assertions.assertEquals(c3, block.getConfigurationRecords().getTimingRecord().toCrdString());
        Assertions.assertEquals(cal, block.getCalibrationRecord().toCrdString());
        Assertions.assertEquals(eleven0, block.getRangeData().get(0).toCrdString());
        Assertions.assertEquals(12, block.getRangeData().size());
        Assertions.assertEquals(eleven10, block.getRangeData().get(10).toCrdString());
        
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
