/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.files.rinex.clock;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.AppliedDCBS;
import org.orekit.files.rinex.AppliedPCVS;
import org.orekit.files.rinex.observation.CustomType;
import org.orekit.files.rinex.section.RinexComment;
import org.orekit.frames.Frame;
import org.orekit.gnss.IGSUtils;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.PredefinedObservationType;
import org.orekit.gnss.PredefinedTimeSystem;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;
import org.orekit.utils.TimeSpanMap;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class RinexClockWriterTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @DefaultDataContext
    @Test
    public void testWriteHeaderTwice() throws IOException {
        final RinexClock rclock = load("gnss/clock/Exple_analysis_1_304.clk",
                                       IGSUtils::guessFrame,
                                       PredefinedObservationType::valueOf,
                                       PredefinedTimeSystem::parseTimeSystem,
                                       DataContext.getDefault().getTimeScales());
        final CharArrayWriter  caw  = new CharArrayWriter();
        RinexClockWriter writer = new RinexClockWriter(caw, "dummy");
        writer.prepareComments(rclock.getComments());
        try {
            writer.writeHeader(rclock.getHeader());
            writer.writeHeader(rclock.getHeader());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.HEADER_ALREADY_WRITTEN, oe.getSpecifier());
        }
    }

    @DefaultDataContext
    @Test
    public void testTooLongRunBy() throws IOException {
        final RinexClock rclock = load("gnss/clock/igr21101_truncated_300.clk",
                                       IGSUtils::guessFrame,
                                       PredefinedObservationType::valueOf,
                                       PredefinedTimeSystem::parseTimeSystem,
                                       DataContext.getDefault().getTimeScales());
        rclock.getHeader().setRunByName("much too long run-by name exceeding 20 characters");
        final CharArrayWriter  caw  = new CharArrayWriter();
        try {
            RinexClockWriter writer = new RinexClockWriter(caw, "dummy");
            writer.prepareComments(rclock.getComments());
            writer.writeHeader(rclock.getHeader());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.FIELD_TOO_LONG, oe.getSpecifier());
            Assertions.assertEquals("much too long run-by name exceeding 20 characters", oe.getParts()[0]);
            Assertions.assertEquals(20, (Integer) oe.getParts()[1]);
        }
    }

    @DefaultDataContext
    @Test
    public void testNoWriteHeader() throws IOException {
        final RinexClock rclock = load("gnss/clock/Exple_analysis_1_304.clk",
                                       IGSUtils::guessFrame,
                                       PredefinedObservationType::valueOf,
                                       PredefinedTimeSystem::parseTimeSystem,
                                       DataContext.getDefault().getTimeScales());
        final CharArrayWriter  caw  = new CharArrayWriter();
        try {
            RinexClockWriter writer = new RinexClockWriter(caw, "dummy");
            writer.writeClockDataLine(rclock.getClockData().get("AREQ00USA").get(0));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.HEADER_NOT_WRITTEN, oe.getSpecifier());
            Assertions.assertEquals("dummy", oe.getParts()[0]);
        }
    }

    @DefaultDataContext
    @Test
    public void testRoundTripRinex200() throws IOException {
        doTestRoundTrip("gnss/clock/cod17381_truncated_200.clk");
    }

    @DefaultDataContext
    @Test
    public void testRoundTripRinex300() throws IOException {
        doTestRoundTrip("gnss/clock/igr21101_truncated_300.clk");
    }

    @DefaultDataContext
    @Test
    public void testRoundTripRinex304A() throws IOException {
        doTestRoundTrip("gnss/clock/Exple_analysis_1_304.clk");
    }

    @DefaultDataContext
    @Test
    public void testRoundTripRinex304B() throws IOException {
        doTestRoundTrip("gnss/clock/Exple_calibration_304.clk");
    }

    @DefaultDataContext
    @Test
    public void testCustomSystem() throws IOException {
        doTestRoundTrip("gnss/clock/custom-system.clk",
                        IGSUtils::guessFrame,
                        CustomType::new,
                        CustomTimeSystem::new,
                        DataContext.getDefault().getTimeScales());
    }

    private RinexClock load(final String name,
                            final Function<? super String, ? extends Frame> frameBuilder,
                            final Function<? super String, ? extends ObservationType> typeBuilder,
                            final Function<? super String, ? extends TimeSystem> timeSystemBuilder,
                            final TimeScales timeScales) {
        final DataSource dataSource = new DataSource(name, () -> Utils.class.getClassLoader().getResourceAsStream(name));
        return new RinexClockParser(frameBuilder, typeBuilder, timeSystemBuilder, timeScales).parse(dataSource);
     }

    @DefaultDataContext
    private void doTestRoundTrip(final String resourceName) throws IOException {
        doTestRoundTrip(resourceName,
                        IGSUtils::guessFrame,
                        PredefinedObservationType::valueOf,
                        PredefinedTimeSystem::parseTimeSystem,
                        DataContext.getDefault().getTimeScales());
     }

    private void doTestRoundTrip(final String resourceName,
                                 final Function<? super String, ? extends Frame> frameBuilder,
                                 final Function<? super String, ? extends ObservationType> typeBuilder,
                                 final Function<? super String, ? extends TimeSystem> timeSystemBuilder,
                                 final TimeScales timeScales) throws IOException {

        final RinexClock rclock = load(resourceName, frameBuilder, typeBuilder, timeSystemBuilder, timeScales);
        final CharArrayWriter  caw  = new CharArrayWriter();
        RinexClockWriter writer = new RinexClockWriter(caw, "dummy");
        RinexClock patched = load(resourceName, frameBuilder, typeBuilder, timeSystemBuilder, timeScales);
        writer.writeCompleteFile(patched);

        // reparse the written file
        final byte[]     bytes   = caw.toString().getBytes(StandardCharsets.UTF_8);
        final DataSource source  = new DataSource("", () -> new ByteArrayInputStream(bytes));
        final RinexClock rebuilt = new RinexClockParser(frameBuilder, typeBuilder, timeSystemBuilder, timeScales).
                                   parse(source);

        checkRinexFile(rclock, rebuilt);

    }

    private void checkRinexFile(final RinexClock first, final RinexClock second) {
        checkRinexHeader(first.getHeader(), second.getHeader());
        Assertions.assertEquals(first.getComments().size(), second.getComments().size());
        for (int i = 0; i < second.getComments().size(); ++i) {
            checkRinexComments(first.getComments().get(i), second.getComments().get(i));
        }
        Assertions.assertEquals(first.getClockData().size(), second.getClockData().size());
        for (final String name : first.getClockData().keySet()) {
            final List<ClockDataLine> linesFirst  = first.getClockData().get(name);
            final List<ClockDataLine> linesSecond = second.getClockData().get(name);
            Assertions.assertEquals(linesFirst.size(), linesSecond.size());
            for (int i = 0; i < linesFirst.size(); ++i) {
                checkClockDataLine(linesFirst.get(i), linesSecond.get(i));
            }
        }
    }

    private void checkRinexHeader(final RinexClockHeader first, final RinexClockHeader second) {

        // base header
        Assertions.assertEquals(first.getFormatVersion(),          second.getFormatVersion(), 0.001);
        Assertions.assertEquals(first.getFileType(),               second.getFileType());
        Assertions.assertEquals(first.getSatelliteSystem(),        second.getSatelliteSystem());
        Assertions.assertEquals(first.getProgramName(),            second.getProgramName());
        Assertions.assertEquals(first.getRunByName(),              second.getRunByName());
        if (first.getCreationDateComponents() != null) {
            Assertions.assertEquals(first.getCreationDateComponents(), second.getCreationDateComponents());
        }
        Assertions.assertEquals(first.getCreationTimeZone(),       second.getCreationTimeZone());
        if (first.getCreationDate() != null) {
            // some reference files have a null creation date (which is probably not really standard-compliant)
            // we check the date only if it was non-null in the reference file
            checkDate(first.getCreationDate(), second.getCreationDate());
        }
        Assertions.assertEquals(first.getDoi(),                    second.getDoi());
        Assertions.assertEquals(first.getLicense(),                second.getLicense());
        Assertions.assertEquals(first.getStationInformation(),     second.getStationInformation());

        // clock-obs header
        Assertions.assertEquals(first.getLeapSecondsTAI(),         second.getLeapSecondsTAI());
        Assertions.assertEquals(first.getListAppliedDCBS().size(), second.getListAppliedDCBS().size());
        for (int i = 0; i < first.getListAppliedDCBS().size(); ++i) {
            checkDCB(first.getListAppliedDCBS().get(i), second.getListAppliedDCBS().get(i));
        }
        Assertions.assertEquals(first.getListAppliedPCVS().size(), second.getListAppliedPCVS().size());
        for (int i = 0; i < first.getListAppliedPCVS().size(); ++i) {
            checkPCV(first.getListAppliedPCVS().get(i), second.getListAppliedPCVS().get(i));
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

        // clock header
        Assertions.assertEquals(first.getTimeSystem(),             second.getTimeSystem());
        Assertions.assertEquals(first.getTimeScale().getName(),    second.getTimeScale().getName());
        Assertions.assertEquals(first.getStationName(),            second.getStationName());
        Assertions.assertEquals(first.getStationIdentifier(),      second.getStationIdentifier());
        Assertions.assertEquals(first.getExternalClockReference(), second.getExternalClockReference());
        Assertions.assertEquals(first.getAnalysisCenterID(),       second.getAnalysisCenterID());
        Assertions.assertEquals(first.getAnalysisCenterName(),     second.getAnalysisCenterName());
        checReferenceClocks(first.getReferenceClocks(),            second.getReferenceClocks());
        checkObservationTypes(first.getSystemObservationTypes(),   second.getSystemObservationTypes());
        for (final SatelliteSystem system : SatelliteSystem.values()) {
            Assertions.assertEquals(first.numberOfObsTypes(system), second.numberOfObsTypes(system));
        }
        Assertions.assertEquals(first.getNumberOfClockDataTypes(), second.getNumberOfClockDataTypes());
        for (int i = 0; i < first.getNumberOfClockDataTypes(); ++i) {
            Assertions.assertEquals(first.getClockDataTypes().get(i), second.getClockDataTypes().get(i));
        }
        Assertions.assertEquals(first.getLeapSecondsGNSS(), second.getLeapSecondsGNSS());
        if (first.getFrame() != null) {
            Assertions.assertEquals(first.getFrame().getName(), second.getFrame().getName());
            Assertions.assertEquals(first.getFrameName(),       second.getFrameName());
        }
        Assertions.assertEquals(first.getNumberOfReceivers(), second.getNumberOfReceivers());
        for (int i = 0; i < first.getNumberOfReceivers(); ++i) {
            checkReceivers(first.getReceivers().get(i), second.getReceivers().get(i));
        }
        Assertions.assertEquals(first.getNumberOfSatellites(), second.getNumberOfSatellites());
        for (int i = 0; i < first.getNumberOfSatellites(); ++i) {
            checkSatellites(first.getSatellites().get(i), second.getSatellites().get(i));
        }

    }

    private void checkRinexComments(final RinexComment first, final RinexComment second) {
        Assertions.assertEquals(first.getLineNumber(), second.getLineNumber());
        Assertions.assertEquals(first.getText(),       second.getText());
    }

    private void checkClockDataLine(final ClockDataLine first, final ClockDataLine second) {
        Assertions.assertEquals(first.getName(), second.getName());
        checkDate(first.getDate(), second.getDate());
        Assertions.assertEquals(first.getNumberOfValues(), second.getNumberOfValues());
        checkClockValue(first, second, ClockDataLine::getClockBias);
        checkClockValue(first, second, ClockDataLine::getClockBiasSigma);
        checkClockValue(first, second, ClockDataLine::getClockRate);
        checkClockValue(first, second, ClockDataLine::getClockRateSigma);
        checkClockValue(first, second, ClockDataLine::getClockAcceleration);
        checkClockValue(first, second, ClockDataLine::getClockAccelerationSigma);
    }

    private void checkClockValue(final ClockDataLine first, final ClockDataLine second,
                                 final ToDoubleFunction<ClockDataLine> extractor) {
        Assertions.assertEquals(extractor.applyAsDouble(first), extractor.applyAsDouble(second),
                                1.0e-15 * FastMath.abs(extractor.applyAsDouble(first)));
    }

    private void checReferenceClocks(final TimeSpanMap<List<ReferenceClock>> first,
                                     final TimeSpanMap<List<ReferenceClock>> second) {

        TimeSpanMap.Span<List<ReferenceClock>> spanFirst  = first.getFirstSpan();
        TimeSpanMap.Span<List<ReferenceClock>> spanSecond = second.getFirstSpan();

        while (spanFirst != null) {
            if (spanFirst.getData() != null) {
                Assertions.assertNotNull(spanSecond.getData());
                checkDate(spanFirst.getStart(), spanSecond.getStart());
                checkDate(spanFirst.getEnd(), spanSecond.getEnd());
                Assertions.assertEquals(spanFirst.getData().size(), spanSecond.getData().size());
                for (int i = 0; i < spanFirst.getData().size(); ++i) {
                    final ReferenceClock clockFirst = spanFirst.getData().get(i);
                    final ReferenceClock clockSecond = spanSecond.getData().get(i);
                    Assertions.assertEquals(clockFirst.getReferenceName(), clockSecond.getReferenceName());
                    Assertions.assertEquals(clockFirst.getClockID(), clockSecond.getClockID());
                    Assertions.assertEquals(clockFirst.getClockConstraint(), clockSecond.getClockConstraint(),
                                            1.0e-15 * FastMath.abs(clockFirst.getClockConstraint()));
                    checkDate(clockFirst.getStartDate(), clockSecond.getStartDate());
                    checkDate(clockFirst.getEndDate(), clockSecond.getEndDate());
                }
            }
            spanFirst  = spanFirst.next();
            spanSecond = spanSecond.next();
        }

        Assertions.assertNull(spanSecond);

    }

    private void checkObservationTypes(final Map<SatelliteSystem, List<ObservationType>> first,
                                       final Map<SatelliteSystem, List<ObservationType>> second) {
        Assertions.assertEquals(first.size(), second.size());
        for (SatelliteSystem system : first.keySet()) {
            final List<ObservationType> firstObservationTypes  = first.get(system);
            final List<ObservationType> secondObservationTypes = second.get(system);
            Assertions.assertEquals(firstObservationTypes.size(), secondObservationTypes.size());
            for (int i = 0; i < firstObservationTypes.size(); ++i) {
                final ObservationType firstType  = firstObservationTypes.get(i);
                final ObservationType secondType = secondObservationTypes.get(i);
                Assertions.assertEquals(firstType.getName(),            secondType.getName());
                Assertions.assertEquals(firstType.getMeasurementType(), secondType.getMeasurementType());
                Assertions.assertEquals(firstType.getSignalCode(),      secondType.getSignalCode());
                Assertions.assertEquals(firstType.getSignal(system),    secondType.getSignal(system));
            }
        }
    }

    private void checkReceivers(final Receiver first, final Receiver second) {
        Assertions.assertEquals(first.getDesignator(), second.getDesignator());
        Assertions.assertEquals(first.getReceiverIdentifier(), second.getReceiverIdentifier());
        Assertions.assertEquals(first.getX(), second.getX(), 1.0e-3);
        Assertions.assertEquals(first.getY(), second.getY(), 1.0e-3);
        Assertions.assertEquals(first.getZ(), second.getZ(), 1.0e-3);
    }

    private void checkSatellites(final SatInSystem first, final SatInSystem second) {
        Assertions.assertEquals(first.getSystem(), second.getSystem());
        Assertions.assertEquals(first.getPRN(),    second.getPRN());
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

    private void checkDate(final AbsoluteDate first, final AbsoluteDate second) {
        if (first == null) {
            Assertions.assertNull(second);
        } else if (Double.isInfinite(first.durationFrom(AbsoluteDate.ARBITRARY_EPOCH))) {
            Assertions.assertEquals(first, second);
        } else {
            Assertions.assertEquals(0.0, second.durationFrom(first), 1.0e-6);
        }
    }

}
