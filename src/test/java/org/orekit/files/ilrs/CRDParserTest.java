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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ilrs.CRDConfiguration.TransponderConfiguration;
import org.orekit.files.ilrs.CRDFile.AnglesMeasurement;
import org.orekit.files.ilrs.CRDFile.CRDDataBlock;
import org.orekit.files.ilrs.CRDFile.MeteorologicalMeasurement;
import org.orekit.files.ilrs.CRDFile.RangeMeasurement;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class CRDParserTest {

    @Test
    public void testInvalidFormat() throws URISyntaxException, IOException {
        try {
            final String ex = "/ilrs/crd_invalid_format.v2C";
            final CRDParser parser = new CRDParser();
            final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
            parser.parse(fileName);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNEXPECTED_FORMAT_FOR_ILRS_FILE,
                                oe.getSpecifier());
            Assert.assertEquals("CRD", oe.getParts()[0]);
            Assert.assertEquals("CPF", oe.getParts()[1]);
        }
    }

    @Test
    public void testMissingEOF() throws IOException, URISyntaxException {
        try {
            final String ex = "/ilrs/crd_unexpected_end_of_file.v2C";
            final CRDParser parser = new CRDParser();
            final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
            parser.parse(fileName);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CRD_UNEXPECTED_END_OF_FILE,
                                oe.getSpecifier());
            Assert.assertEquals(23, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    public void testCorruptedData() throws IOException, URISyntaxException {
        try {
            final String ex = "/ilrs/crd_corrupted_data.v2C";
            final CRDParser parser = new CRDParser();
            final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
            parser.parse(fileName);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                oe.getSpecifier());
            Assert.assertEquals(19, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    public void testInvalidRangeType() throws IOException, URISyntaxException {
        try {
            final String ex = "/ilrs/crd_invalid_range_type.v2C";
            final CRDParser parser = new CRDParser();
            final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
            parser.parse(fileName);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INVALID_RANGE_INDICATOR_IN_CRD_FILE,
                                oe.getSpecifier());
            Assert.assertEquals(9, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    public void testLageos2Version2() throws URISyntaxException, IOException {

        // Simple test for version 2.0
        final String ex = "/ilrs/lageos2_201802.npt.v2C";

        final CRDParser parser = new CRDParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final CRDFile file = (CRDFile) parser.parse(fileName);

        // Verify first data block
        final CRDDataBlock first = file.getDataBlocks().get(0);
        final CRDHeader firstHeader = first.getHeader();
        final CRDConfiguration firstConf = first.getConfigurationRecords();
        final AbsoluteDate firstStartDate = new AbsoluteDate("2018-02-01T15:14:58.000", TimeScalesFactory.getUTC());
        final AbsoluteDate firstEndDate   = new AbsoluteDate("2018-02-01T15:48:57.000", TimeScalesFactory.getUTC());

        // Header
        Assert.assertEquals(2, firstHeader.getVersion());
        Assert.assertEquals(2018, firstHeader.getProductionEpoch().getYear());
        Assert.assertEquals(2, firstHeader.getProductionEpoch().getMonth());
        Assert.assertEquals(1, firstHeader.getProductionEpoch().getDay());
        Assert.assertEquals(17, firstHeader.getProductionHour());
        Assert.assertEquals("CHAL", firstHeader.getStationName());
        Assert.assertEquals(9998, firstHeader.getSystemIdentifier());
        Assert.assertEquals(19, firstHeader.getSystemNumber());
        Assert.assertEquals(1, firstHeader.getSystemOccupancy());
        Assert.assertEquals(4, firstHeader.getEpochIdentifier());
        Assert.assertEquals("WPLTN", firstHeader.getStationNetword());
        Assert.assertEquals("lageos2", firstHeader.getName());
        Assert.assertEquals("9207002", firstHeader.getIlrsSatelliteId());
        Assert.assertEquals("5986", firstHeader.getSic());
        Assert.assertEquals("22195", firstHeader.getNoradId());
        Assert.assertEquals(0, firstHeader.getSpacecraftEpochTimeScale());
        Assert.assertEquals(1, firstHeader.getTargetClass());
        Assert.assertEquals(1, firstHeader.getTargetLocation());
        Assert.assertEquals(1, firstHeader.getDataType());
        Assert.assertEquals(0.0, firstHeader.getStartEpoch().durationFrom(firstStartDate), 1.0e-5);
        Assert.assertEquals(0.0, firstHeader.getEndEpoch().durationFrom(firstEndDate), 1.0e-5);
        Assert.assertEquals(0, firstHeader.getDataReleaseFlag());
        Assert.assertFalse(firstHeader.isTroposphericRefractionApplied());
        Assert.assertFalse(firstHeader.isCenterOfMassCorrectionApplied());
        Assert.assertFalse(firstHeader.isReceiveAmplitudeCorrectionApplied());
        Assert.assertTrue(firstHeader.isStationSystemDelayApplied());
        Assert.assertFalse(firstHeader.isTransponderDelayApplied());
        Assert.assertEquals(2, firstHeader.getRangeType().getIndicator());
        Assert.assertEquals(0, firstHeader.getQualityIndicator());
        Assert.assertEquals(1, firstHeader.getPredictionType());
        Assert.assertEquals(18, firstHeader.getYearOfCentury());
        Assert.assertEquals("020115", firstHeader.getDateAndTime());
        Assert.assertEquals("hts", firstHeader.getPredictionProvider());
        Assert.assertEquals(3202, firstHeader.getSequenceNumber());

        // Configuration records
        Assert.assertEquals(532.000 * 1.0e-9, firstConf.getSystemRecord().getWavelength(), 1.0e-15);
        Assert.assertEquals("std", firstConf.getSystemRecord().getSystemId());
        Assert.assertEquals("CL1", firstConf.getLaserRecord().getLaserId());
        Assert.assertEquals("RG30-L", firstConf.getLaserRecord().getLaserType());
        Assert.assertEquals(1064.00 * 1.0e-9, firstConf.getLaserRecord().getPrimaryWavelength(), 1.0e-15);
        Assert.assertEquals(1000.00, firstConf.getLaserRecord().getNominalFireRate(), 1.0e-15);
        Assert.assertEquals(1.50, firstConf.getLaserRecord().getPulseEnergy(), 1.0e-15);
        Assert.assertEquals(10.0, firstConf.getLaserRecord().getPulseWidth(), 1.0e-15);
        Assert.assertEquals(92.82, firstConf.getLaserRecord().getBeamDivergence(), 1.0e-15);
        Assert.assertEquals(0, firstConf.getLaserRecord().getPulseInOutgoingSemiTrain());
        Assert.assertEquals("CD1", firstConf.getDetectorRecord().getDetectorId());
        Assert.assertEquals("CSPAD", firstConf.getDetectorRecord().getDetectorType());
        Assert.assertEquals(532.000 * 1.0e-9, firstConf.getDetectorRecord().getApplicableWavelength(), 1.0e-15);
        Assert.assertEquals(20.0, firstConf.getDetectorRecord().getQuantumEfficiency(), 1.0e-15);
        Assert.assertEquals(5.0, firstConf.getDetectorRecord().getAppliedVoltage(), 1.0e-15);
        Assert.assertEquals(60000.0, firstConf.getDetectorRecord().getDarkCount(), 1.0e-15);
        Assert.assertEquals("TTL", firstConf.getDetectorRecord().getOutputPulseType());
        Assert.assertEquals(0.0, firstConf.getDetectorRecord().getOutputPulseWidth(), 1.0e-15);
        Assert.assertEquals(1.70 * 1.0e-9, firstConf.getDetectorRecord().getSpectralFilter(), 1.0e-15);
        Assert.assertEquals(0.0, firstConf.getDetectorRecord().getTransmissionOfSpectralFilter(), 1.0e-15);
        Assert.assertEquals(0.0, firstConf.getDetectorRecord().getSpatialFilter(), 1.0e-15);
        Assert.assertEquals("none", firstConf.getDetectorRecord().getExternalSignalProcessing());
        Assert.assertEquals(0.0, firstConf.getDetectorRecord().getAmplifierGain(), 1.0e-15);
        Assert.assertEquals(0.0, firstConf.getDetectorRecord().getAmplifierBandwidth(), 1.0e-15);
        Assert.assertEquals("0", firstConf.getDetectorRecord().getAmplifierInUse());
        Assert.assertEquals("CT1", firstConf.getTimingRecord().getLocalTimingId());
        Assert.assertEquals("Meridian", firstConf.getTimingRecord().getTimeSource());
        Assert.assertEquals("Meridian", firstConf.getTimingRecord().getFrequencySource());
        Assert.assertEquals("ET-A032", firstConf.getTimingRecord().getTimer());
        Assert.assertEquals("003309", firstConf.getTimingRecord().getTimerSerialNumber());
        Assert.assertEquals(0.0, firstConf.getTimingRecord().getEpochDelayCorrection(), 1.0e-15);
        Assert.assertEquals("pgms", firstConf.getSoftwareRecord().getSoftwareId());
        Assert.assertEquals("Sattrk", firstConf.getSoftwareRecord().getTrackingSoftwares()[1]);
        Assert.assertEquals("2.00Cm", firstConf.getSoftwareRecord().getTrackingSoftwareVersions()[1]);
        Assert.assertEquals("crd_cal", firstConf.getSoftwareRecord().getProcessingSoftwares()[1]);
        Assert.assertEquals("1.7", firstConf.getSoftwareRecord().getProcessingSoftwareVersions()[1]);
        Assert.assertEquals("mets", firstConf.getMeteorologicalRecord().getMeteorologicalId());
        Assert.assertEquals("Paroscientific", firstConf.getMeteorologicalRecord().getPressSensorManufacturer());
        Assert.assertEquals("Met4a", firstConf.getMeteorologicalRecord().getPressSensorModel());
        Assert.assertEquals("123456", firstConf.getMeteorologicalRecord().getPressSensorSerialNumber());
        Assert.assertEquals("Paroscientific", firstConf.getMeteorologicalRecord().getTempSensorManufacturer());
        Assert.assertEquals("Met4a", firstConf.getMeteorologicalRecord().getTempSensorModel());
        Assert.assertEquals("123456", firstConf.getMeteorologicalRecord().getTempSensorSerialNumber());
        Assert.assertEquals("Paroscientific", firstConf.getMeteorologicalRecord().getHumiSensorManufacturer());
        Assert.assertEquals("Met4a", firstConf.getMeteorologicalRecord().getHumiSensorModel());
        Assert.assertEquals("123456", firstConf.getMeteorologicalRecord().getHumiSensorSerialNumber());
        Assert.assertNull(firstConf.getTransponderRecord());

        // Meteorological data (only one data)
        final MeteorologicalMeasurement meteoFirst = first.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH);
        Assert.assertEquals(56940.0, meteoFirst.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals(0.9989, meteoFirst.getPressure(), 1.0e-15);
        Assert.assertEquals(259.10, meteoFirst.getTemperature(), 1.0e-15);
        Assert.assertEquals(80.0, meteoFirst.getHumidity(), 1.0e-15);

        // Range data
        final RangeMeasurement rangeFirst1 = first.getRangeData().get(0);
        Assert.assertEquals(54927.620161400002, rangeFirst1.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals(0.044106029140, rangeFirst1.getTimeOfFlight(), 1.0e-15);
        Assert.assertEquals(2, rangeFirst1.getEpochEvent());
        Assert.assertEquals(5.7, rangeFirst1.getSnr(), 1.0e-15);
        final RangeMeasurement rangeFirst2 = first.getRangeData().get(5);
        Assert.assertEquals(56899.718161400000, rangeFirst2.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals(0.050148193335, rangeFirst2.getTimeOfFlight(), 1.0e-15);
        Assert.assertEquals(2, rangeFirst2.getEpochEvent());
        Assert.assertEquals(5.7, rangeFirst2.getSnr(), 1.0e-15);

        // Angles data
        Assert.assertEquals(0, first.getAnglesData().size());

        // Verify last data block
        final CRDDataBlock last = file.getDataBlocks().get(file.getDataBlocks().size() - 1);
        final CRDHeader lastHeader = last.getHeader();
        final CRDConfiguration lastConf = last.getConfigurationRecords();
        final AbsoluteDate lastStartDate = new AbsoluteDate("2018-02-27T14:10:10.000", TimeScalesFactory.getUTC());
        final AbsoluteDate lastEndDate   = new AbsoluteDate("2018-02-27T14:39:06.000", TimeScalesFactory.getUTC());

        // Header
        Assert.assertEquals(2, lastHeader.getVersion());
        Assert.assertEquals(2018, lastHeader.getProductionEpoch().getYear());
        Assert.assertEquals(2, lastHeader.getProductionEpoch().getMonth());
        Assert.assertEquals(27, lastHeader.getProductionEpoch().getDay());
        Assert.assertEquals(14, lastHeader.getProductionHour());
        Assert.assertEquals("CHAL", lastHeader.getStationName());
        Assert.assertEquals(9998, lastHeader.getSystemIdentifier());
        Assert.assertEquals(19, lastHeader.getSystemNumber());
        Assert.assertEquals(1, lastHeader.getSystemOccupancy());
        Assert.assertEquals(4, lastHeader.getEpochIdentifier());
        Assert.assertEquals("WPLTN", lastHeader.getStationNetword());
        Assert.assertEquals("lageos2", lastHeader.getName());
        Assert.assertEquals("9207002", lastHeader.getIlrsSatelliteId());
        Assert.assertEquals("5986", lastHeader.getSic());
        Assert.assertEquals("22195", lastHeader.getNoradId());
        Assert.assertEquals(0, lastHeader.getSpacecraftEpochTimeScale());
        Assert.assertEquals(1, lastHeader.getTargetClass());
        Assert.assertEquals(1, lastHeader.getTargetLocation());
        Assert.assertEquals(1, lastHeader.getDataType());
        Assert.assertEquals(0.0, lastHeader.getStartEpoch().durationFrom(lastStartDate), 1.0e-5);
        Assert.assertEquals(0.0, lastHeader.getEndEpoch().durationFrom(lastEndDate), 1.0e-5);
        Assert.assertEquals(0, lastHeader.getDataReleaseFlag());
        Assert.assertFalse(lastHeader.isTroposphericRefractionApplied());
        Assert.assertFalse(lastHeader.isCenterOfMassCorrectionApplied());
        Assert.assertFalse(lastHeader.isReceiveAmplitudeCorrectionApplied());
        Assert.assertTrue(lastHeader.isStationSystemDelayApplied());
        Assert.assertFalse(lastHeader.isTransponderDelayApplied());
        Assert.assertEquals(2, lastHeader.getRangeType().getIndicator());
        Assert.assertEquals(0, lastHeader.getQualityIndicator());
        Assert.assertEquals(1, lastHeader.getPredictionType());
        Assert.assertEquals(18, lastHeader.getYearOfCentury());
        Assert.assertEquals("022714", lastHeader.getDateAndTime());
        Assert.assertEquals("hts", lastHeader.getPredictionProvider());
        Assert.assertEquals(5802, lastHeader.getSequenceNumber());

        // Configuration records
        Assert.assertEquals(532.000 * 1.0e-9, lastConf.getSystemRecord().getWavelength(), 1.0e-15);
        Assert.assertEquals("std", lastConf.getSystemRecord().getSystemId());
        Assert.assertEquals("CL1", lastConf.getLaserRecord().getLaserId());
        Assert.assertEquals("RG30-L", lastConf.getLaserRecord().getLaserType());
        Assert.assertEquals(1064.00 * 1.0e-9, lastConf.getLaserRecord().getPrimaryWavelength(), 1.0e-15);
        Assert.assertEquals(1000.00, lastConf.getLaserRecord().getNominalFireRate(), 1.0e-15);
        Assert.assertEquals(1.50, lastConf.getLaserRecord().getPulseEnergy(), 1.0e-15);
        Assert.assertEquals(10.0, lastConf.getLaserRecord().getPulseWidth(), 1.0e-15);
        Assert.assertEquals(92.82, lastConf.getLaserRecord().getBeamDivergence(), 1.0e-15);
        Assert.assertEquals(0, lastConf.getLaserRecord().getPulseInOutgoingSemiTrain());
        Assert.assertEquals("CD1", lastConf.getDetectorRecord().getDetectorId());
        Assert.assertEquals("CSPAD", lastConf.getDetectorRecord().getDetectorType());
        Assert.assertEquals(532.000 * 1.0e-9, lastConf.getDetectorRecord().getApplicableWavelength(), 1.0e-15);
        Assert.assertEquals(20.0, lastConf.getDetectorRecord().getQuantumEfficiency(), 1.0e-15);
        Assert.assertEquals(5.0, lastConf.getDetectorRecord().getAppliedVoltage(), 1.0e-15);
        Assert.assertEquals(60000.0, lastConf.getDetectorRecord().getDarkCount(), 1.0e-15);
        Assert.assertEquals("TTL", lastConf.getDetectorRecord().getOutputPulseType());
        Assert.assertEquals(0.0, lastConf.getDetectorRecord().getOutputPulseWidth(), 1.0e-15);
        Assert.assertEquals(1.70 * 1.0e-9, lastConf.getDetectorRecord().getSpectralFilter(), 1.0e-15);
        Assert.assertEquals(0.0, lastConf.getDetectorRecord().getTransmissionOfSpectralFilter(), 1.0e-15);
        Assert.assertEquals(0.0, lastConf.getDetectorRecord().getSpatialFilter(), 1.0e-15);
        Assert.assertEquals("none", lastConf.getDetectorRecord().getExternalSignalProcessing());
        Assert.assertEquals(0.0, lastConf.getDetectorRecord().getAmplifierGain(), 1.0e-15);
        Assert.assertEquals(0.0, lastConf.getDetectorRecord().getAmplifierBandwidth(), 1.0e-15);
        Assert.assertEquals("0", lastConf.getDetectorRecord().getAmplifierInUse());
        Assert.assertEquals("CT1", lastConf.getTimingRecord().getLocalTimingId());
        Assert.assertEquals("Meridian", lastConf.getTimingRecord().getTimeSource());
        Assert.assertEquals("Meridian", lastConf.getTimingRecord().getFrequencySource());
        Assert.assertEquals("ET-A032", lastConf.getTimingRecord().getTimer());
        Assert.assertEquals("003309", lastConf.getTimingRecord().getTimerSerialNumber());
        Assert.assertEquals(0.0, lastConf.getTimingRecord().getEpochDelayCorrection(), 1.0e-15);
        Assert.assertEquals("pgms", lastConf.getSoftwareRecord().getSoftwareId());
        Assert.assertEquals("Sattrk", lastConf.getSoftwareRecord().getTrackingSoftwares()[1]);
        Assert.assertEquals("2.00Cm", lastConf.getSoftwareRecord().getTrackingSoftwareVersions()[1]);
        Assert.assertEquals("crd_cal", lastConf.getSoftwareRecord().getProcessingSoftwares()[1]);
        Assert.assertEquals("1.7", lastConf.getSoftwareRecord().getProcessingSoftwareVersions()[1]);
        Assert.assertEquals("mets", lastConf.getMeteorologicalRecord().getMeteorologicalId());
        Assert.assertEquals("Paroscientific", lastConf.getMeteorologicalRecord().getPressSensorManufacturer());
        Assert.assertEquals("Met4a", lastConf.getMeteorologicalRecord().getPressSensorModel());
        Assert.assertEquals("123456", lastConf.getMeteorologicalRecord().getPressSensorSerialNumber());
        Assert.assertEquals("Paroscientific", lastConf.getMeteorologicalRecord().getTempSensorManufacturer());
        Assert.assertEquals("Met4a", lastConf.getMeteorologicalRecord().getTempSensorModel());
        Assert.assertEquals("123456", lastConf.getMeteorologicalRecord().getTempSensorSerialNumber());
        Assert.assertEquals("Paroscientific", lastConf.getMeteorologicalRecord().getHumiSensorManufacturer());
        Assert.assertEquals("Met4a", lastConf.getMeteorologicalRecord().getHumiSensorModel());
        Assert.assertEquals("123456", lastConf.getMeteorologicalRecord().getHumiSensorSerialNumber());
        Assert.assertNull(lastConf.getTransponderRecord());

        // Meteorological data (only one data)
        final MeteorologicalMeasurement meteoLast = last.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH);
        Assert.assertEquals(52749.0, meteoLast.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals(0.9921, meteoLast.getPressure(), 1.0e-15);
        Assert.assertEquals(260.80, meteoLast.getTemperature(), 1.0e-15);
        Assert.assertEquals(67.0, meteoLast.getHumidity(), 1.0e-15);

        // Range data
        final RangeMeasurement rangeLast1 = last.getRangeData().get(0);
        Assert.assertEquals(51080.935001603524, rangeLast1.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals(0.045673108965, rangeLast1.getTimeOfFlight(), 1.0e-15);
        Assert.assertEquals(2, rangeLast1.getEpochEvent());
        Assert.assertEquals(5.7, rangeLast1.getSnr(), 1.0e-15);
        final RangeMeasurement rangeLast2 = last.getRangeData().get(13);
        Assert.assertEquals(52618.095001597932, rangeLast2.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals(0.042733272755, rangeLast2.getTimeOfFlight(), 1.0e-15);
        Assert.assertEquals(2, rangeLast2.getEpochEvent());
        Assert.assertEquals(5.7, rangeLast2.getSnr(), 1.0e-15);

        // Angles data
        Assert.assertEquals(0, first.getAnglesData().size());

    }

    @Test
    public void testChampVersion1() throws URISyntaxException, IOException {

        // Simple test for version 1.0
        final String ex = "/ilrs/champ_201709-small.frd";

        final CRDParser parser = new CRDParser();
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final CRDFile file = (CRDFile) parser.parse(inEntry);

        // Data block
        final CRDDataBlock block = file.getDataBlocks().get(0);
        final CRDHeader header = block.getHeader();
        final CRDConfiguration conf = block.getConfigurationRecords();
        final AbsoluteDate startDate = new AbsoluteDate("2017-09-26T03:55:41.000", TimeScalesFactory.getUTC());
        final AbsoluteDate endDate   = new AbsoluteDate("2017-09-26T04:04:48.000", TimeScalesFactory.getUTC());

        // Header
        Assert.assertEquals(1, header.getVersion());
        Assert.assertEquals(2017, header.getProductionEpoch().getYear());
        Assert.assertEquals(9, header.getProductionEpoch().getMonth());
        Assert.assertEquals(26, header.getProductionEpoch().getDay());
        Assert.assertEquals(4, header.getProductionHour());
        Assert.assertEquals("STL3", header.getStationName());
        Assert.assertEquals(7825, header.getSystemIdentifier());
        Assert.assertEquals(90, header.getSystemNumber());
        Assert.assertEquals(1, header.getSystemOccupancy());
        Assert.assertEquals(4, header.getEpochIdentifier());
        Assert.assertEquals("champ", header.getName());
        Assert.assertEquals("0003902", header.getIlrsSatelliteId());
        Assert.assertEquals("8002", header.getSic());
        Assert.assertEquals("026405", header.getNoradId());
        Assert.assertEquals(0, header.getSpacecraftEpochTimeScale());
        Assert.assertEquals(1, header.getTargetClass());
        Assert.assertEquals(0, header.getDataType());
        Assert.assertEquals(0.0, header.getStartEpoch().durationFrom(startDate), 1.0e-5);
        Assert.assertEquals(0.0, header.getEndEpoch().durationFrom(endDate), 1.0e-5);
        Assert.assertEquals(0, header.getDataReleaseFlag());
        Assert.assertFalse(header.isTroposphericRefractionApplied());
        Assert.assertFalse(header.isCenterOfMassCorrectionApplied());
        Assert.assertFalse(header.isReceiveAmplitudeCorrectionApplied());
        Assert.assertTrue(header.isStationSystemDelayApplied());
        Assert.assertFalse(header.isTransponderDelayApplied());
        Assert.assertEquals(2, header.getRangeType().getIndicator());
        Assert.assertEquals(0, header.getQualityIndicator());

        // Configuration records
        Assert.assertEquals(532.100 * 1.0e-9, conf.getSystemRecord().getWavelength(), 1.0e-15);
        Assert.assertEquals("IDAA", conf.getSystemRecord().getSystemId());
        Assert.assertEquals("IDAB", conf.getLaserRecord().getLaserId());
        Assert.assertEquals("Nd-YAG", conf.getLaserRecord().getLaserType());
        Assert.assertEquals(532.10 * 1.0e-9, conf.getLaserRecord().getPrimaryWavelength(), 1.0e-15);
        Assert.assertEquals(0.0, conf.getLaserRecord().getNominalFireRate(), 1.0e-15);
        Assert.assertEquals(21.0, conf.getLaserRecord().getPulseEnergy(), 1.0e-15);
        Assert.assertEquals(12.0, conf.getLaserRecord().getPulseWidth(), 1.0e-15);
        Assert.assertEquals(0.0, conf.getLaserRecord().getBeamDivergence(), 1.0e-15);
        Assert.assertEquals(1, conf.getLaserRecord().getPulseInOutgoingSemiTrain());
        Assert.assertEquals("IDAJ", conf.getDetectorRecord().getDetectorId());
        Assert.assertEquals("CSPAD", conf.getDetectorRecord().getDetectorType());
        Assert.assertEquals(532.000 * 1.0e-9, conf.getDetectorRecord().getApplicableWavelength(), 1.0e-15);
        Assert.assertEquals(20.0, conf.getDetectorRecord().getQuantumEfficiency(), 1.0e-15);
        Assert.assertEquals(11.0, conf.getDetectorRecord().getAppliedVoltage(), 1.0e-15);
        Assert.assertEquals(100000.0, conf.getDetectorRecord().getDarkCount(), 1.0e-15);
        Assert.assertEquals("ECL", conf.getDetectorRecord().getOutputPulseType());
        Assert.assertEquals(12.0, conf.getDetectorRecord().getOutputPulseWidth(), 1.0e-15);
        Assert.assertEquals(2.00 * 1.0e-9, conf.getDetectorRecord().getSpectralFilter(), 1.0e-15);
        Assert.assertEquals(90.0, conf.getDetectorRecord().getTransmissionOfSpectralFilter(), 1.0e-15);
        Assert.assertEquals(12.0, conf.getDetectorRecord().getSpatialFilter(), 1.0e-15);
        Assert.assertEquals("Manual", conf.getDetectorRecord().getExternalSignalProcessing());
        Assert.assertEquals("IDAV", conf.getTimingRecord().getLocalTimingId());
        Assert.assertEquals("TrueTime_XLi", conf.getTimingRecord().getTimeSource());
        Assert.assertEquals("TrueTime_OCXO", conf.getTimingRecord().getFrequencySource());
        Assert.assertEquals("MRCS", conf.getTimingRecord().getTimer());
        Assert.assertEquals("NA", conf.getTimingRecord().getTimerSerialNumber());
        Assert.assertEquals(0.2322 * 1.0e-6, conf.getTimingRecord().getEpochDelayCorrection(), 1.0e-15);

        // Meteorological data
        final MeteorologicalMeasurement meteoFirst = block.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH);
        Assert.assertEquals(14353.388283000000, meteoFirst.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals(0.92374, meteoFirst.getPressure(), 1.0e-15);
        Assert.assertEquals(289.42, meteoFirst.getTemperature(), 1.0e-15);
        Assert.assertEquals(28.1, meteoFirst.getHumidity(), 1.0e-15);

        // Range data
        Assert.assertEquals(4, block.getRangeData().size());
        final RangeMeasurement range = block.getRangeData().get(0);
        Assert.assertEquals(14487.343206247217, range.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals(0.003603959600, range.getTimeOfFlight(), 1.0e-15);
        Assert.assertEquals(2, range.getEpochEvent());

        // Angles data
        Assert.assertEquals(4, block.getAnglesData().size());
        final AnglesMeasurement angles = block.getAnglesData().get(0);
        Assert.assertEquals(14343.574333000000, angles.getDate().getComponents(parser.getTimeScale()).getTime().getSecondsInLocalDay(), 1.0e-15);
        Assert.assertEquals(FastMath.toRadians(215.000000), angles.getAzimuth(), 1.0e-15);
        Assert.assertEquals(FastMath.toRadians(15.000010), angles.getElevation(), 1.0e-15);
        Assert.assertEquals(0, angles.getDirectionFlag());
        Assert.assertEquals(2, angles.getOriginIndicator());
        Assert.assertFalse(angles.isRefractionCorrected());
        Assert.assertEquals(Double.NaN, angles.getAzimuthRate(), 1.0e-15);
        Assert.assertEquals(Double.NaN, angles.getElevationRate(), 1.0e-15);

    }

    @Test
    public void testAllFields() throws URISyntaxException, IOException {

        final String ex = "/ilrs/crd_all_fields.frd";

        final CRDParser parser = new CRDParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final CRDFile file = (CRDFile) parser.parse(fileName);

        final CRDDataBlock block = file.getDataBlocks().get(0);
        Assert.assertEquals(0, file.getComments().size());
        Assert.assertEquals(4, block.getRangeData().size());
        Assert.assertEquals(4, block.getAnglesData().size());

        // Transponder
        final TransponderConfiguration transponder = block.getConfigurationRecords().getTransponderRecord();
        Assert.assertEquals("id", transponder.getTransponderId());
        Assert.assertEquals(0.0, transponder.getStationUTCOffset(), 1.0e-15);
        Assert.assertEquals(0.0, transponder.getStationOscDrift(), 1.0e-15);
        Assert.assertEquals(0.0, transponder.getTranspUTCOffset(), 1.0e-15);
        Assert.assertEquals(0.0, transponder.getTranspOscDrift(), 1.0e-15);
        Assert.assertEquals(0.0, transponder.getTranspClkRefTime(), 1.0e-15);
        Assert.assertEquals(0, transponder.getSpacecraftClockAndDriftApplied());
        Assert.assertEquals(0, transponder.getStationClockAndDriftApplied());
        Assert.assertFalse(transponder.isSpacecraftTimeSimplified());
    }

    @Test
    public void testMeteorologicalData() {

        // Initialise an empty data block
        final CRDDataBlock block = new CRDDataBlock();

        // Verify null object
        Assert.assertNull(block.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH));

        // Add a meteorological data entry
        final AbsoluteDate date = new AbsoluteDate("2020-10-29T11:40:00.000", TimeScalesFactory.getUTC());
        final MeteorologicalMeasurement meteoData1 = new MeteorologicalMeasurement(date, 1013.0, 273.0, 50.0);
        block.addMeteoData(meteoData1);

        // Verify values
        MeteorologicalMeasurement data1 = block.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH);
        Assert.assertEquals(meteoData1.getPressure(),    data1.getPressure(),    1.0e-15);
        Assert.assertEquals(meteoData1.getTemperature(), data1.getTemperature(), 1.0e-15);
        Assert.assertEquals(meteoData1.getHumidity(),    data1.getHumidity(),    1.0e-15);
        data1 = block.getMeteoData().getMeteo(date);
        Assert.assertEquals(meteoData1.getPressure(),    data1.getPressure(),    1.0e-15);
        Assert.assertEquals(meteoData1.getTemperature(), data1.getTemperature(), 1.0e-15);
        Assert.assertEquals(meteoData1.getHumidity(),    data1.getHumidity(),    1.0e-15);
        data1 = block.getMeteoData().getMeteo(date.shiftedBy(60.0));
        Assert.assertEquals(meteoData1.getPressure(),    data1.getPressure(),    1.0e-15);
        Assert.assertEquals(meteoData1.getTemperature(), data1.getTemperature(), 1.0e-15);
        Assert.assertEquals(meteoData1.getHumidity(),    data1.getHumidity(),    1.0e-15);

        // Add another meteorological data entry
        final MeteorologicalMeasurement meteoData2 = new MeteorologicalMeasurement(date.shiftedBy(60.0), 1015.0, 275.0, 70.0);
        block.addMeteoData(meteoData2);

        // Verify values
        MeteorologicalMeasurement data2 = block.getMeteoData().getMeteo(AbsoluteDate.J2000_EPOCH); // before first data
        Assert.assertEquals(meteoData1.getPressure(),    data2.getPressure(),    1.0e-15);
        Assert.assertEquals(meteoData1.getTemperature(), data2.getTemperature(), 1.0e-15);
        Assert.assertEquals(meteoData1.getHumidity(),    data2.getHumidity(),    1.0e-15);
        data2 = block.getMeteoData().getMeteo(date.shiftedBy(180.0)); // After second data
        Assert.assertEquals(meteoData2.getPressure(),    data2.getPressure(),    1.0e-15);
        Assert.assertEquals(meteoData2.getTemperature(), data2.getTemperature(), 1.0e-15);
        Assert.assertEquals(meteoData2.getHumidity(),    data2.getHumidity(),    1.0e-15);
        data2 = block.getMeteoData().getMeteo(date.shiftedBy(30.0)); // between first and second datta
        Assert.assertEquals(1014.0,                      data2.getPressure(),    1.0e-15);
        Assert.assertEquals(274.0,                       data2.getTemperature(), 1.0e-15);
        Assert.assertEquals(60.0,                        data2.getHumidity(),    1.0e-15);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
