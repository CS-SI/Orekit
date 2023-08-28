/* Copyright 2023 Thales Alenia Space
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
package org.orekit.files.rinex.observation;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.AppliedDCBS;
import org.orekit.files.rinex.AppliedPCVS;
import org.orekit.files.rinex.section.RinexComment;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;

public class RinexObservationWriterTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testWriteHeaderTwice() throws IOException {
        final RinexObservation robs = load("rinex/bbbb0000.00o");
        final CharArrayWriter  caw  = new CharArrayWriter();
        try (RinexObservationWriter writer = new RinexObservationWriter(caw, "dummy")) {
            writer.prepareComments(robs.getComments());
            writer.writeHeader(robs.getHeader());
            writer.writeHeader(robs.getHeader());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.HEADER_ALREADY_WRITTEN, oe.getSpecifier());
            Assertions.assertEquals("dummy", oe.getParts()[0]);
        }
    }

    @Test
    public void testNoWriteHeader() throws IOException {
        final RinexObservation robs = load("rinex/aiub0000.00o");
        final CharArrayWriter  caw  = new CharArrayWriter();
        try (RinexObservationWriter writer = new RinexObservationWriter(caw, "dummy")) {
            writer.writeObservationDataSet(robs.getObservationDataSets().get(0));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.HEADER_NOT_WRITTEN, oe.getSpecifier());
            Assertions.assertEquals("dummy", oe.getParts()[0]);
        }
    }

    @Test
    public void testRoundTripRinex2A() throws IOException {
        doTestRoundTrip("rinex/aiub0000.00o");
    }

    @Test
    public void testRoundTripRinex2B() throws IOException {
        doTestRoundTrip("rinex/cccc0000.07o");
    }

    @Test
    public void testRoundTripRinex3A() throws IOException {
        doTestRoundTrip("rinex/bbbb0000.00o");
    }

    @Test
    public void testRoundTripRinex3B() throws IOException {
        doTestRoundTrip("rinex/dddd0000.01o");
    }

    @Test
    public void testRoundTripDcbs() throws IOException {
        doTestRoundTrip("rinex/dcbs.00o");
    }

    @Test
    public void testRoundTripPcvs() throws IOException {
        doTestRoundTrip("rinex/pcvs.00o");
    }

    @Test
    public void testRoundTripScaleFactor() throws IOException {
        doTestRoundTrip("rinex/bbbb0000.00o");
    }

    @Test
    public void testRoundTripObsScaleFactor() throws IOException {
        doTestRoundTrip("rinex/ice12720-scaled.07o");
    }

    @Test
    public void testRoundTripLeapSecond() throws IOException {
        doTestRoundTrip("rinex/jnu10110.17o");
    }

    @Test
    public void testContinuationPhaseShift() throws IOException {
        doTestRoundTrip("rinex/continuation-phase-shift.23o");
    }

    private RinexObservation load(final String name) {
        final DataSource dataSource = new DataSource(name, () -> Utils.class.getClassLoader().getResourceAsStream(name));
        return new RinexObservationParser().parse(dataSource);
     }

    private void doTestRoundTrip(final String resourceName) throws IOException {

        final RinexObservation robs = load(resourceName);
        final CharArrayWriter  caw  = new CharArrayWriter();
        try (RinexObservationWriter writer = new RinexObservationWriter(caw, "dummy")) {
            writer.writeCompleteFile(robs);
        }

        // reparse the written file
        final byte[]           bytes   = caw.toString().getBytes(StandardCharsets.UTF_8);
        final DataSource       source  = new DataSource("", () -> new ByteArrayInputStream(bytes));
        final RinexObservation rebuilt = new RinexObservationParser().parse(source);

        checkRinexFile(robs, rebuilt);

    }

    private void checkRinexFile(final RinexObservation first, final RinexObservation second) {
        checkRinexHeader(first.getHeader(), second.getHeader());
        // we may have lost comments in events observations
        Assertions.assertTrue(first.getComments().size() >= second.getComments().size());
        for (int i = 0; i < second.getComments().size(); ++i) {
            checkRinexComments(first.getComments().get(i), second.getComments().get(i));
        }
        Assertions.assertEquals(first.getObservationDataSets().size(), second.getObservationDataSets().size());
        for (int i = 0; i < first.getObservationDataSets().size(); ++i) {
            checkRinexObs(first.getObservationDataSets().get(i), second.getObservationDataSets().get(i));
        }
    }

    private void checkRinexHeader(final RinexObservationHeader first, final RinexObservationHeader second) {
        Assertions.assertEquals(first.getFormatVersion(),          second.getFormatVersion(), 0.001);
        Assertions.assertEquals(first.getSatelliteSystem(),        second.getSatelliteSystem());
        Assertions.assertEquals(first.getProgramName(),            second.getProgramName());
        Assertions.assertEquals(first.getRunByName(),              second.getRunByName());
        Assertions.assertEquals(first.getCreationDateComponents(), second.getCreationDateComponents());
        Assertions.assertEquals(first.getCreationTimeZone(),       second.getCreationTimeZone());
        checkDate(first.getCreationDate(), second.getCreationDate());
        Assertions.assertEquals(first.getDoi(),                    second.getDoi());
        Assertions.assertEquals(first.getLicense(),                second.getLicense());
        Assertions.assertEquals(first.getStationInformation(),     second.getStationInformation());
        Assertions.assertEquals(first.getMarkerName(),             second.getMarkerName());
        Assertions.assertEquals(first.getMarkerNumber(),           second.getMarkerNumber());
        Assertions.assertEquals(first.getObserverName(),           second.getObserverName());
        Assertions.assertEquals(first.getAgencyName(),             second.getAgencyName());
        Assertions.assertEquals(first.getReceiverNumber(),         second.getReceiverNumber());
        Assertions.assertEquals(first.getReceiverType(),           second.getReceiverType());
        Assertions.assertEquals(first.getReceiverVersion(),        second.getReceiverVersion());
        Assertions.assertEquals(first.getAntennaNumber(),          second.getAntennaNumber());
        Assertions.assertEquals(first.getAntennaType(),            second.getAntennaType());
        checkVector(first.getApproxPos(), second.getApproxPos());
        Assertions.assertEquals(first.getAntennaHeight(),          second.getAntennaHeight(),         1.0e-12);
        Assertions.assertEquals(first.getEccentricities().getX(),  second.getEccentricities().getX(), 1.0e-12);
        Assertions.assertEquals(first.getEccentricities().getY(),  second.getEccentricities().getY(), 1.0e-12);
        Assertions.assertEquals(first.getClkOffset(),              second.getClkOffset(),             1.0e-12);
        Assertions.assertEquals(first.getInterval(),               second.getInterval(),              1.0e-12);
        checkDate(first.getTFirstObs(), second.getTFirstObs());
        checkDate(first.getTLastObs(),  second.getTLastObs());
        Assertions.assertEquals(first.getLeapSeconds(),            second.getLeapSeconds());
        Assertions.assertEquals(first.getMarkerType(),             second.getMarkerType());
        checkVector(first.getAntennaReferencePoint(),              second.getAntennaReferencePoint());
        Assertions.assertEquals(first.getObservationCode(),        second.getObservationCode());
        checkVector(first.getAntennaPhaseCenter(),                 second.getAntennaPhaseCenter());
        checkVector(first.getAntennaBSight(),                      second.getAntennaBSight());
        Assertions.assertEquals(first.getAntennaAzimuth(),         second.getAntennaAzimuth(), 1.0e-12);
        checkVector(first.getAntennaZeroDirection(),               second.getAntennaZeroDirection());
        checkVector(first.getCenterMass(),                         second.getCenterMass());
        Assertions.assertEquals(first.getSignalStrengthUnit(),     second.getSignalStrengthUnit());
        Assertions.assertEquals(first.getLeapSecondsFuture(),      second.getLeapSecondsFuture());
        Assertions.assertEquals(first.getLeapSecondsWeekNum(),     second.getLeapSecondsWeekNum());
        Assertions.assertEquals(first.getLeapSecondsDayNum(),      second.getLeapSecondsDayNum());
        Assertions.assertEquals(first.getListAppliedDCBS().size(), second.getListAppliedDCBS().size());
        for (int i = 0; i < first.getListAppliedDCBS().size(); ++i) {
            checkDCB(first.getListAppliedDCBS().get(i), second.getListAppliedDCBS().get(i));
        }
        Assertions.assertEquals(first.getListAppliedPCVS().size(), second.getListAppliedPCVS().size());
        for (int i = 0; i < first.getListAppliedPCVS().size(); ++i) {
            checkPCV(first.getListAppliedPCVS().get(i), second.getListAppliedPCVS().get(i));
        }
        Assertions.assertEquals(first.getPhaseShiftCorrections().size(), second.getPhaseShiftCorrections().size());
        for (int i = 0; i < first.getPhaseShiftCorrections().size(); ++i) {
            checkPhaseShiftCorrection(first.getPhaseShiftCorrections().get(i), second.getPhaseShiftCorrections().get(i));
        }
        for (SatelliteSystem system : SatelliteSystem.values()) {
            Assertions.assertEquals(first.getScaleFactorCorrections(system).size(), second.getScaleFactorCorrections(system).size());
            for (int i = 0; i < first.getScaleFactorCorrections(system).size(); ++i) {
                checkScaleFactorCorrection(first.getScaleFactorCorrections(system).get(i),
                                           second.getScaleFactorCorrections(system).get(i));
            }
        }
        Assertions.assertEquals(first.getGlonassChannels().size(), second.getGlonassChannels().size());
        for (int i = 0; i < first.getGlonassChannels().size(); ++i) {
            checkGlonassChannel(first.getGlonassChannels().get(i), second.getGlonassChannels().get(i));
        }
        Assertions.assertEquals(first.getNbSat(),      second.getNbSat());
        Assertions.assertEquals(first.getNbObsPerSat().size(), second.getNbObsPerSat().size());
        for (final Map.Entry<SatInSystem, Map<ObservationType, Integer>> firstE : first.getNbObsPerSat().entrySet()) {
            Map<ObservationType, Integer> firstV  = firstE.getValue();
            Map<ObservationType, Integer> secondV = second.getNbObsPerSat().get(firstE.getKey());
            Assertions.assertEquals(firstV.size(), secondV.size());
            for (final Map.Entry<ObservationType, Integer> firstF : firstV.entrySet()) {
                Assertions.assertEquals(firstF.getValue(), secondV.get(firstF.getKey()));
            }
        }
        Assertions.assertEquals(first.getTypeObs().size(), second.getTypeObs().size());
        for (final Map.Entry<SatelliteSystem, List<ObservationType>> firstE : first.getTypeObs().entrySet()) {
            List<ObservationType> firstT  = firstE.getValue();
            List<ObservationType> secondT = second.getTypeObs().get(firstE.getKey());
            Assertions.assertEquals(firstT.size(), secondT.size());
            for (int i = 0; i < firstT.size(); ++i) {
                Assertions.assertEquals(firstT.get(i), secondT.get(i));
            }
        }
        Precision.equalsIncludingNaN(first.getC1cCodePhaseBias(), second.getC1cCodePhaseBias(), 1.0e-12);
        Precision.equalsIncludingNaN(first.getC1pCodePhaseBias(), second.getC1pCodePhaseBias(), 1.0e-12);
        Precision.equalsIncludingNaN(first.getC2cCodePhaseBias(), second.getC2cCodePhaseBias(), 1.0e-12);
        Precision.equalsIncludingNaN(first.getC2pCodePhaseBias(), second.getC2pCodePhaseBias(), 1.0e-12);

    }

    private void checkRinexComments(final RinexComment first, final RinexComment second) {
        Assertions.assertEquals(first.getLineNumber(), second.getLineNumber());
        Assertions.assertEquals(first.getText(),       second.getText());
    }

    private void checkRinexObs(final ObservationDataSet first, final ObservationDataSet second) {
        Assertions.assertEquals(first.getSatellite().getSystem(), second.getSatellite().getSystem());
        Assertions.assertEquals(first.getSatellite().getPRN(),    second.getSatellite().getPRN());
        checkDate(first.getDate(), second.getDate());
        Assertions.assertEquals(first.getEventFlag(),           second.getEventFlag());
        Assertions.assertEquals(first.getObservationData().size(), second.getObservationData().size());
        for (int i = 0; i < first.getObservationData().size(); ++i) {
            final ObservationData firstO  = first.getObservationData().get(i);
            final ObservationData secondO = second.getObservationData().get(i);
            Assertions.assertEquals(firstO.getValue(),               secondO.getValue(), 1.0e-12);
            Assertions.assertEquals(firstO.getLossOfLockIndicator(), secondO.getLossOfLockIndicator());
            Assertions.assertEquals(firstO.getSignalStrength(),      secondO.getSignalStrength());
        }
        Precision.equalsIncludingNaN(first.getRcvrClkOffset(), second.getRcvrClkOffset(), 1.0e-12);
    }

    private void checkDCB(final AppliedDCBS first, final AppliedDCBS second) {
        Assertions.assertEquals(first.getSatelliteSystem(), second.getSatelliteSystem());
        Assertions.assertEquals(first.getProgDCBS(),        second.getProgDCBS());
        Assertions.assertEquals(first.getSourceDCBS(),      second.getSourceDCBS());
    }

    private void checkPCV(final AppliedPCVS first, final AppliedPCVS second) {
        Assertions.assertEquals(first.getSatelliteSystem(), second.getSatelliteSystem());
        Assertions.assertEquals(first.getProgPCVS(),        second.getProgPCVS());
        Assertions.assertEquals(first.getSourcePCVS(),      second.getSourcePCVS());
    }

    private void checkPhaseShiftCorrection(final PhaseShiftCorrection first, final PhaseShiftCorrection second) {
        Assertions.assertEquals(first.getSatelliteSystem(), second.getSatelliteSystem());
        Assertions.assertEquals(first.getTypeObs(),         second.getTypeObs());
        Assertions.assertEquals(first.getCorrection(),      second.getCorrection(), 1.0e-12);
        Assertions.assertEquals(first.getSatsCorrected().size(), second.getSatsCorrected().size());
        for (int i = 0; i < first.getSatsCorrected().size(); ++i) {
            Assertions.assertEquals(first.getSatsCorrected().get(i).getSystem(), second.getSatsCorrected().get(i).getSystem());
            Assertions.assertEquals(first.getSatsCorrected().get(i).getPRN(),    second.getSatsCorrected().get(i).getPRN());
        }
    }

    private void checkScaleFactorCorrection(final ScaleFactorCorrection first, final ScaleFactorCorrection second) {
        Assertions.assertEquals(first.getCorrection(),      second.getCorrection(), 1.0e-12);
        Assertions.assertEquals(first.getTypesObsScaled().size(), second.getTypesObsScaled().size());
        for (int i = 0; i < first.getTypesObsScaled().size(); ++i) {
            Assertions.assertEquals(first.getTypesObsScaled().get(i), second.getTypesObsScaled().get(i));
        }
    }

    private void checkGlonassChannel(final GlonassSatelliteChannel first, final GlonassSatelliteChannel second) {
        Assertions.assertEquals(first.getSatellite().getSystem(), second.getSatellite().getSystem());
        Assertions.assertEquals(first.getSatellite().getPRN(),    second.getSatellite().getPRN());
        Assertions.assertEquals(first.getK(),                     second.getK());
    }

    private void checkDate(final AbsoluteDate first, final AbsoluteDate second) {
        if (first == null) {
            Assertions.assertNull(second);
        } else {
            Assertions.assertEquals(first, second);
        }
    }

    private void checkVector(final Vector3D first, final Vector3D second) {
        if (first == null) {
            Assertions.assertNull(second);
        } else {
            Assertions.assertEquals(0.0, Vector3D.distance(first, second), 1.0e-12 * first.getNorm());
        }
    }

}
