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
import org.orekit.data.DataContext;
import org.orekit.files.rinex.AppliedDCBS;
import org.orekit.files.rinex.AppliedPCVS;
import org.orekit.files.rinex.section.CommonLabel;
import org.orekit.files.rinex.utils.BaseRinexWriter;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.formatting.FastDoubleFormatter;
import org.orekit.utils.formatting.FastLongFormatter;
import org.orekit.utils.units.Unit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Writer for Rinex clock file.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class RinexClockWriter extends BaseRinexWriter<RinexClockHeader> {

    /** Millimeter unit. */
    private static final Unit MILLIMETER = Unit.parse("mm");

    /** Format for one 11 digits integer field. */
    private static final FastLongFormatter ELEVEN_DIGITS_INTEGER = new FastLongFormatter(11, false);

    /** Format for one 4.2 digits float field. */
    private static final FastDoubleFormatter FOUR_TWO_DIGITS_FLOAT = new FastDoubleFormatter(4, 2);

    /** Format for one 9.6 digits float field. */
    private static final FastDoubleFormatter NINE_SIX_DIGITS_FLOAT = new FastDoubleFormatter(9, 6);

    /** Format for one 10.6 digits float field. */
    private static final FastDoubleFormatter TEN_SIX_DIGITS_FLOAT = new FastDoubleFormatter(10, 6);

    /** Simple constructor.
     * <p>
     * This constructor uses the {@link DataContext#getDefault() default data context}
     * and recognizes only {@link org.orekit.gnss.PredefinedTimeSystem}.
     * </p>
     * @param output destination of generated output
     * @param outputName output name for error messages
     */
    public RinexClockWriter(final Appendable output, final String outputName) {
        super(output, outputName);
    }

    /** Write a complete clock file.
     * <p>
     * This method calls {@link #prepareComments(List)} and
     * {@link #writeHeader(RinexClockHeader)} once and then loops on
     * calling {@link #writeClockDataLine(ClockDataLine)}
     * for all data lines in the file
     * </p>
     * @param rinexClock Rinex clock file to write
     * @see #writeHeader(RinexClockHeader)
     * @see #writeClockDataLine(ClockDataLine)
     * @exception IOException if an I/O error occurs.
     */
    public void writeCompleteFile(final RinexClock rinexClock)
        throws IOException {
        prepareComments(rinexClock.getComments());
        writeHeader(rinexClock.getHeader());

        // prepare chronological iteration
        final List<PendingLines> pending = new ArrayList<>();
        for (final Map.Entry<String, List<ClockDataLine>> entry : rinexClock.getClockData().entrySet()) {
            pending.add(new PendingLines(entry.getKey(), entry.getValue()));
        }
        pending.sort(Comparator.comparing(pl -> pl.name));

        // write lines in chronological order
        for (AbsoluteDate date = earliest(pending); date.isFinite(); date = earliest(pending)) {
            // write all lines that correspond to this date
            for (final PendingLines pl : pending) {
                final ClockDataLine dataLine = pl.lineAtDate(date);
                if (dataLine != null) {
                    writeClockDataLine(dataLine);
                }
            }
        }

    }

    /** Find the earliest pending date.
     * @param pending pending lines
     * @return earliest pending date
     */
    private AbsoluteDate earliest(final List<PendingLines> pending) {
        AbsoluteDate earliest = AbsoluteDate.FUTURE_INFINITY;
        for (final PendingLines pl : pending) {
            if (pl.nextDate().isBefore(earliest)) {
                earliest = pl.nextDate();
            }
        }
        return earliest;
    }

    /** Write header.
     * <p>
     * This method must be called exactly once at the beginning
     * (directly or by {@link #writeCompleteFile(RinexClock)})
     * </p>
     * @param header header to write
     * @exception IOException if an I/O error occurs.
     */
    public void writeHeader(final RinexClockHeader header) throws IOException {
        if (header.isBefore304()) {
            super.writeHeader(header, RinexClockHeader.LABEL_INDEX_300_302);
            writeHeaderBefore304(header);
        } else {
            super.writeHeader(header, RinexClockHeader.LABEL_INDEX_304_PLUS);
            writeHeader304AndLater(header);
        }
    }

    /** Write header for a version before version 3.04.
     * @param header header to write
     * @exception IOException if an I/O error occurs.
     */
    private void writeHeaderBefore304(final RinexClockHeader header) throws IOException {

        // RINEX VERSION / TYPE
        outputField(NINE_TWO_DIGITS_FLOAT, header.getFormatVersion(), 9);
        outputField("", 20, true);
        outputField("C", 40, true);
        outputField(header.getSatelliteSystem() == null ? ' ' : header.getSatelliteSystem().getKey(), 41);
        finishHeaderLine(CommonLabel.VERSION);

        // PGM / RUN BY / DATE
        outputField(header.getProgramName(), 20, true);
        outputField(header.getRunByName(),   40, true);
        final DateTimeComponents dtc = header.getCreationDateComponents();
        outputField(PADDED_FOUR_DIGITS_INTEGER, dtc.getDate().getYear(), 44);
        outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getDate().getMonth(), 46);
        outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getDate().getDay(), 48);
        outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getTime().getHour(), 51);
        outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getTime().getMinute(), 53);
        outputField(PADDED_TWO_DIGITS_INTEGER, (int) FastMath.rint(dtc.getTime().getSecond()), 55);
        outputField(header.getCreationTimeZone(), 59, true);
        finishHeaderLine(CommonLabel.PROGRAM);

        // SYS / # / OBS TYPES
        for (Map.Entry<SatelliteSystem, List<ObservationType>> entry : header.getSystemObservationTypes().entrySet()) {
            outputField(entry.getKey().getKey(), 1);
            outputField(' ', 3);
            outputField(THREE_DIGITS_INTEGER, entry.getValue().size(), 6);
            for (final ObservationType type : entry.getValue()) {
                int next = getColumn() + 4;
                if (exceedsHeaderLength(next)) {
                    // we need to set up a continuation line
                    finishHeaderLine(CommonLabel.SYS_NB_TYPES_OF_OBSERV);
                    outputField(' ', 6);
                    next = getColumn() + 4;
                }
                outputField(' ', next - 3);
                outputField(type.getName(), next, false);
            }
            finishHeaderLine(CommonLabel.SYS_NB_TYPES_OF_OBSERV);
        }

        // TIME SYSTEM ID
        if (header.getFormatVersion() > 2.0) {
            outputField(header.getTimeSystem().getKey(), 6, false);
            finishHeaderLine(ClockLabel.TIME_SYSTEM_ID);
        }

        // LEAP SECONDS
        if (header.getLeapSeconds() > 0) {
            outputField(SIX_DIGITS_INTEGER, header.getLeapSeconds(), 6);
            finishHeaderLine(CommonLabel.LEAP_SECONDS);
        }

        // SYS / DCBS APPLIED
        for (final AppliedDCBS appliedDCBS : header.getListAppliedDCBS()) {
            outputField(appliedDCBS.getSatelliteSystem().getKey(),  1);
            outputField(' ',                                        2);
            outputField(appliedDCBS.getProgDCBS(),                 19, true);
            outputField(' ',                                       20);
            outputField(appliedDCBS.getSourceDCBS(),               60, true);
            finishHeaderLine(CommonLabel.SYS_DCBS_APPLIED);
        }

        // SYS / PCVS APPLIED
        for (final AppliedPCVS appliedPCVS : header.getListAppliedPCVS()) {
            outputField(appliedPCVS.getSatelliteSystem().getKey(),  1);
            outputField(' ',                                        2);
            outputField(appliedPCVS.getProgPCVS(),                 19, true);
            outputField(' ',                                       20);
            outputField(appliedPCVS.getSourcePCVS(),               60, true);
            finishHeaderLine(CommonLabel.SYS_PCVS_APPLIED);
        }

        // # / TYPES OF DATA
        outputField(SIX_DIGITS_INTEGER, header.getClockDataTypes().size(), 6);
        for (final ClockDataType clockDataType : header.getClockDataTypes()) {
            outputField(clockDataType.name(), getColumn() + 6, false);
        }
        finishHeaderLine(ClockLabel.NB_TYPES_OF_DATA);

        // STATION NAME / NUM
        if (!header.getStationName().isEmpty()) {
            outputField(header.getStationName(), 4, true);
            outputField(' ', 5);
            outputField(header.getStationIdentifier(), 25, true);
            finishHeaderLine(ClockLabel.STATION_NAME_NUM);
        }

        // STATION CLK REF
        if (!header.getExternalClockReference().isEmpty()) {
            outputField(header.getExternalClockReference(), 60, true);
            finishHeaderLine(ClockLabel.STATION_CLK_REF);
        }

        // ANALYSIS CENTER
        if (!header.getAnalysisCenterName().isEmpty()) {
            outputField(header.getAnalysisCenterID(),   3, true);
            outputField("", 5, true);
            outputField(header.getAnalysisCenterName(), 60, true);
            finishHeaderLine(ClockLabel.ANALYSIS_CENTER);
        }

        // # OF CLK REF / ANALYSIS CLK REF
        for (TimeSpanMap.Span<List<ReferenceClock>> span = header.getReferenceClocks().getFirstSpan();
             span != null;
             span = span.next()) {
            if (span.getData() != null) {
                outputField(SIX_DIGITS_INTEGER, span.getData().size(), 6);
                if (span.getStart().isFinite()) {
                    outputField(' ', 7);
                    final DateTimeComponents startDtc = span.getStart().getComponents(header.getTimeScale());
                    final DateTimeComponents endDtc   = span.getEnd().getComponents(header.getTimeScale());
                    outputField(FOUR_DIGITS_INTEGER, startDtc.getDate().getYear(), 11);
                    outputField(' ', 12);
                    outputField(PADDED_TWO_DIGITS_INTEGER, startDtc.getDate().getMonth(), 14);
                    outputField(' ', 15);
                    outputField(PADDED_TWO_DIGITS_INTEGER, startDtc.getDate().getDay(), 17);
                    outputField(' ', 18);
                    outputField(PADDED_TWO_DIGITS_INTEGER, startDtc.getTime().getHour(), 20);
                    outputField(' ', 21);
                    outputField(PADDED_TWO_DIGITS_INTEGER, startDtc.getTime().getMinute(), 23);
                    outputField(TEN_SIX_DIGITS_FLOAT, startDtc.getTime().getSecond(), 33);
                    outputField(' ', 34);
                    outputField(FOUR_DIGITS_INTEGER, endDtc.getDate().getYear(), 38);
                    outputField(' ', 39);
                    outputField(PADDED_TWO_DIGITS_INTEGER, endDtc.getDate().getMonth(), 41);
                    outputField(' ', 42);
                    outputField(PADDED_TWO_DIGITS_INTEGER, endDtc.getDate().getDay(), 44);
                    outputField(' ', 45);
                    outputField(PADDED_TWO_DIGITS_INTEGER, endDtc.getTime().getHour(), 47);
                    outputField(' ', 48);
                    outputField(PADDED_TWO_DIGITS_INTEGER, endDtc.getTime().getMinute(), 50);
                    outputField(TEN_SIX_DIGITS_FLOAT, endDtc.getTime().getSecond(), 60);
                }
                finishHeaderLine(ClockLabel.NB_OF_CLK_REF);
                for (final ReferenceClock clock : span.getData()) {
                    outputField(clock.getReferenceName(), 4, true);
                    outputField(' ', 5);
                    outputField(clock.getClockID(), 25, true);
                    outputField(' ', 40);
                    if (clock.getClockConstraint() != 0.0) {
                        write1912(clock.getClockConstraint());
                    }
                    finishHeaderLine(ClockLabel.ANALYSIS_CLK_REF);
                }
            }
        }

        // # OF SOLN STA / TRF
        if (header.getNumberOfReceivers() > 0) {
            outputField(SIX_DIGITS_INTEGER, header.getNumberOfReceivers(), 6);
            outputField("", 10, true);
            outputField(header.getFrameName(), 60, true);
            finishHeaderLine(ClockLabel.NB_OF_SOLN_STA_TRF);
        }

        // SOLN STA NAME / NUM
        for (final Receiver receiver : header.getReceivers()) {
            outputField(receiver.getDesignator(), 4, true);
            outputField(' ', 5);
            outputField(receiver.getReceiverIdentifier(), 25, true);
            outputField(ELEVEN_DIGITS_INTEGER, FastMath.round(MILLIMETER.fromSI(receiver.getX())), 36);
            outputField(' ', 37);
            outputField(ELEVEN_DIGITS_INTEGER, FastMath.round(MILLIMETER.fromSI(receiver.getY())), 48);
            outputField(' ', 49);
            outputField(ELEVEN_DIGITS_INTEGER, FastMath.round(MILLIMETER.fromSI(receiver.getZ())), 60);
            finishHeaderLine(ClockLabel.SOLN_STA_NAME_NUM);
        }

        // # OF SOLN SATS
        if (header.getNumberOfSatellites() > 0) {
            outputField(SIX_DIGITS_INTEGER, header.getNumberOfSatellites(), 6);
            finishHeaderLine(ClockLabel.NB_OF_SOLN_SATS);
        }

        // PRN LIST
        boolean wrotePRN = false;
        for (final SatInSystem satInSystem : header.getSatellites()) {
            int next = getColumn() + 4;
            if (exceedsHeaderLength(next)) {
                // we need to set up a continuation line
                finishHeaderLine(ClockLabel.PRN_LIST);
                next = 4;
            }
            outputField(satInSystem.toString(), next, true);
            wrotePRN = true;
        }
        if (wrotePRN) {
            finishHeaderLine(ClockLabel.PRN_LIST);
        }

        // END OF HEADER
        writeHeaderLine("", CommonLabel.END);

    }

    /** Write header for a version 3.04 or later.
     * @param header header to write
     * @exception IOException if an I/O error occurs.
     */
    private void writeHeader304AndLater(final RinexClockHeader header)
        throws IOException {

        // RINEX VERSION / TYPE
        outputField(FOUR_TWO_DIGITS_FLOAT, header.getFormatVersion(), 4);
        outputField("",  21, true);
        outputField("C", 42, true);
        outputField(header.getSatelliteSystem() == null ? ' ' : header.getSatelliteSystem().getKey(), 43);
        finishHeaderLine(CommonLabel.VERSION);

        // PGM / RUN BY / DATE
        outputField(header.getProgramName(), 21, true);
        outputField(header.getRunByName(),   42, true);
        final DateTimeComponents dtc = header.getCreationDateComponents();
        outputField(PADDED_FOUR_DIGITS_INTEGER, dtc.getDate().getYear(), 46);
        outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getDate().getMonth(), 48);
        outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getDate().getDay(), 50);
        outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getTime().getHour(), 54);
        outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getTime().getMinute(), 56);
        outputField(PADDED_TWO_DIGITS_INTEGER, (int) FastMath.rint(dtc.getTime().getSecond()), 58);
        outputField(header.getCreationTimeZone(), 63, true);
        finishHeaderLine(CommonLabel.PROGRAM);

        // SYS / # / OBS TYPES  xxxxxx
        for (Map.Entry<SatelliteSystem, List<ObservationType>> entry : header.getSystemObservationTypes().entrySet()) {
            outputField(entry.getKey().getKey(), 1);
            outputField(' ', 3);
            outputField(THREE_DIGITS_INTEGER, entry.getValue().size(), 6);
            outputField(' ', 8);
            for (final ObservationType type : entry.getValue()) {
                int next = getColumn() + 4;
                if (exceedsHeaderLength(next)) {
                    // we need to set up a continuation line
                    finishHeaderLine(CommonLabel.SYS_NB_TYPES_OF_OBSERV);
                    outputField(' ', 8);
                    next = getColumn() + 4;
                }
                outputField(type.getName(), next - 1, false);
                outputField(' ', next);
            }
            finishHeaderLine(CommonLabel.SYS_NB_TYPES_OF_OBSERV);
        }

        // TIME SYSTEM ID
        if (header.getFormatVersion() > 2.0) {
            outputField(header.getTimeSystem().getKey(), 6, false);
            finishHeaderLine(ClockLabel.TIME_SYSTEM_ID);
        }

        // LEAP SECONDS
        if (header.getLeapSeconds() > 0) {
            outputField(SIX_DIGITS_INTEGER, header.getLeapSeconds(), 6);
            finishHeaderLine(CommonLabel.LEAP_SECONDS);
        }

        // LEAP SECONDS GNSS
        if (header.getLeapSecondsGNSS() > 0) {
            outputField(SIX_DIGITS_INTEGER, header.getLeapSecondsGNSS(), 6);
            finishHeaderLine(ClockLabel.LEAP_SECONDS_GNSS);
        }

        // SYS / DCBS APPLIED
        for (final AppliedDCBS appliedDCBS : header.getListAppliedDCBS()) {
            outputField(appliedDCBS.getSatelliteSystem().getKey(),  1);
            outputField(' ',                                        3);
            outputField(appliedDCBS.getProgDCBS(),                 20, true);
            outputField(' ',                                       22);
            outputField(appliedDCBS.getSourceDCBS(),               65, true);
            finishHeaderLine(CommonLabel.SYS_DCBS_APPLIED);
        }

        // SYS / PCVS APPLIED
        for (final AppliedPCVS appliedPCVS : header.getListAppliedPCVS()) {
            outputField(appliedPCVS.getSatelliteSystem().getKey(),  1);
            outputField(' ',                                        3);
            outputField(appliedPCVS.getProgPCVS(),                 20, true);
            outputField(' ',                                       22);
            outputField(appliedPCVS.getSourcePCVS(),               65, true);
            finishHeaderLine(CommonLabel.SYS_PCVS_APPLIED);
        }

        // # / TYPES OF DATA
        outputField(SIX_DIGITS_INTEGER, header.getClockDataTypes().size(), 6);
        for (final ClockDataType clockDataType : header.getClockDataTypes()) {
            outputField(clockDataType.name(), getColumn() + 6, false);
        }
        finishHeaderLine(ClockLabel.NB_TYPES_OF_DATA);

        // STATION NAME / NUM
        if (!header.getStationName().isEmpty()) {
            outputField(header.getStationName(), 9, true);
            outputField(' ', 10);
            outputField(header.getStationIdentifier(), 30, true);
            finishHeaderLine(ClockLabel.STATION_NAME_NUM);
        }

        // STATION CLK REF
        if (!header.getExternalClockReference().isEmpty()) {
            outputField(header.getExternalClockReference(), 65, true);
            finishHeaderLine(ClockLabel.STATION_CLK_REF);
        }

        // ANALYSIS CENTER
        if (!header.getAnalysisCenterName().isEmpty()) {
            outputField(header.getAnalysisCenterID(),   3, true);
            outputField("", 5, true);
            outputField(header.getAnalysisCenterName(), 60, true);
            finishHeaderLine(ClockLabel.ANALYSIS_CENTER);
        }

        // # OF CLK REF / ANALYSIS CLK REF
        for (TimeSpanMap.Span<List<ReferenceClock>> span = header.getReferenceClocks().getFirstSpan();
             span != null;
             span = span.next()) {
            if (span.getData() != null) {
                outputField(SIX_DIGITS_INTEGER, span.getData().size(), 6);
                if (span.getStart().isFinite()) {
                    outputField(' ', 7);
                    final DateTimeComponents startDtc = span.getStart().getComponents(header.getTimeScale());
                    final DateTimeComponents endDtc   = span.getEnd().getComponents(header.getTimeScale());
                    outputField(FOUR_DIGITS_INTEGER, startDtc.getDate().getYear(), 11);
                    outputField(' ', 12);
                    outputField(PADDED_TWO_DIGITS_INTEGER, startDtc.getDate().getMonth(), 14);
                    outputField(' ', 15);
                    outputField(PADDED_TWO_DIGITS_INTEGER, startDtc.getDate().getDay(), 17);
                    outputField(' ', 18);
                    outputField(PADDED_TWO_DIGITS_INTEGER, startDtc.getTime().getHour(), 20);
                    outputField(' ', 21);
                    outputField(PADDED_TWO_DIGITS_INTEGER, startDtc.getTime().getMinute(), 23);
                    outputField(' ', 24);
                    outputField(TEN_SIX_DIGITS_FLOAT, startDtc.getTime().getSecond(), 34);
                    outputField(' ', 36);
                    outputField(FOUR_DIGITS_INTEGER, endDtc.getDate().getYear(), 40);
                    outputField(' ', 41);
                    outputField(PADDED_TWO_DIGITS_INTEGER, endDtc.getDate().getMonth(), 43);
                    outputField(' ', 44);
                    outputField(PADDED_TWO_DIGITS_INTEGER, endDtc.getDate().getDay(), 46);
                    outputField(' ', 47);
                    outputField(PADDED_TWO_DIGITS_INTEGER, endDtc.getTime().getHour(), 49);
                    outputField(' ', 50);
                    outputField(PADDED_TWO_DIGITS_INTEGER, endDtc.getTime().getMinute(), 52);
                    outputField(' ', 53);
                    outputField(TEN_SIX_DIGITS_FLOAT, endDtc.getTime().getSecond(), 63);
                }
                finishHeaderLine(ClockLabel.NB_OF_CLK_REF);
                for (final ReferenceClock clock : span.getData()) {
                    outputField(clock.getReferenceName(), 9, true);
                    outputField(' ', 10);
                    outputField(clock.getClockID(), 30, true);
                    outputField(' ', 45);
                    if (clock.getClockConstraint() != 0.0) {
                        write1912(clock.getClockConstraint());
                    }
                    finishHeaderLine(ClockLabel.ANALYSIS_CLK_REF);
                }
            }
        }

        // # OF SOLN STA / TRF
        if (header.getNumberOfReceivers() > 0) {
            outputField(SIX_DIGITS_INTEGER, header.getNumberOfReceivers(), 6);
            outputField("", 10, true);
            outputField(header.getFrameName(), 65, true);
            finishHeaderLine(ClockLabel.NB_OF_SOLN_STA_TRF);
        }

        // SOLN STA NAME / NUM
        for (final Receiver receiver : header.getReceivers()) {
            outputField(receiver.getDesignator(), 9, true);
            outputField(' ', 10);
            outputField(receiver.getReceiverIdentifier(), 30, true);
            outputField(ELEVEN_DIGITS_INTEGER, FastMath.round(MILLIMETER.fromSI(receiver.getX())), 41);
            outputField(' ', 42);
            outputField(ELEVEN_DIGITS_INTEGER, FastMath.round(MILLIMETER.fromSI(receiver.getY())), 53);
            outputField(' ', 54);
            outputField(ELEVEN_DIGITS_INTEGER, FastMath.round(MILLIMETER.fromSI(receiver.getZ())), 65);
            finishHeaderLine(ClockLabel.SOLN_STA_NAME_NUM);
        }

        // # OF SOLN SATS
        if (header.getNumberOfSatellites() > 0) {
            outputField(SIX_DIGITS_INTEGER, header.getNumberOfSatellites(), 6);
            finishHeaderLine(ClockLabel.NB_OF_SOLN_SATS);
        }

        // PRN LIST
        boolean wrotePRN = false;
        for (final SatInSystem satInSystem : header.getSatellites()) {
            int next = getColumn() + 4;
            if (exceedsHeaderLength(next)) {
                // we need to set up a continuation line
                finishHeaderLine(ClockLabel.PRN_LIST);
                next = 4;
            }
            outputField(satInSystem.toString(), next, true);
            wrotePRN = true;
        }
        if (wrotePRN) {
            finishHeaderLine(ClockLabel.PRN_LIST);
        }

        // END OF HEADER
        writeHeaderLine("", CommonLabel.END);

    }

    /** Append a number with e12.12 format.
     * @param x number to write
     * @exception IOException if an I/O error occurs.
     */
    private void write1912(final double x) throws IOException {
        outputField(String.format(Locale.US, "%19.12e", x), getColumn() + 19, true);
    }

    /** Write a clock data line.
     * @param clockDataLine clock data line to write
     * @exception IOException if an I/O error occurs.
     */
    public void writeClockDataLine(final ClockDataLine clockDataLine) throws IOException {
        checkHeaderWritten();
        if (getHeader().isBefore304()) {
            outputField(clockDataLine.getDataType().name(), 2, true);
            outputField(' ', 3);
            outputField(clockDataLine.getName(), 7, true);
            outputField(' ', 8);
            final DateTimeComponents epoch = clockDataLine.getDate().getComponents(getHeader().getTimeScale());
            outputField(FOUR_DIGITS_INTEGER, epoch.getDate().getYear(), 12);
            outputField(' ', 13);
            outputField(PADDED_TWO_DIGITS_INTEGER, epoch.getDate().getMonth(), 15);
            outputField(' ', 16);
            outputField(PADDED_TWO_DIGITS_INTEGER, epoch.getDate().getDay(), 18);
            outputField(' ', 19);
            outputField(PADDED_TWO_DIGITS_INTEGER, epoch.getTime().getHour(), 21);
            outputField(' ', 22);
            outputField(PADDED_TWO_DIGITS_INTEGER, epoch.getTime().getMinute(), 24);
            outputField(TEN_SIX_DIGITS_FLOAT, epoch.getTime().getSecond(), 34);
            outputField(THREE_DIGITS_INTEGER, clockDataLine.getNumberOfValues(), 37);
            outputField(' ', 40);
            write1912(clockDataLine.getClockBias());
            if (clockDataLine.getNumberOfValues() > 1) {
                outputField(' ', 60);
                write1912(clockDataLine.getClockBiasSigma());
                if (clockDataLine.getNumberOfValues() > 2) {
                    finishLine();
                    write1912(clockDataLine.getClockRate());
                    if (clockDataLine.getNumberOfValues() > 3) {
                        outputField(' ', 20);
                        write1912(clockDataLine.getClockRateSigma());
                        if (clockDataLine.getNumberOfValues() > 4) {
                            outputField(' ', 40);
                            write1912(clockDataLine.getClockAcceleration());
                            if (clockDataLine.getNumberOfValues() > 5) {
                                outputField(' ', 60);
                                write1912(clockDataLine.getClockAccelerationSigma());
                            }
                        }
                    }
                }
            }
        } else {
            outputField(clockDataLine.getDataType().name(), 2, true);
            outputField(' ', 3);
            outputField(clockDataLine.getName(), 12, true);
            outputField(' ', 13);
            final DateTimeComponents epoch = clockDataLine.getDate().getComponents(getHeader().getTimeScale());
            outputField(FOUR_DIGITS_INTEGER, epoch.getDate().getYear(), 17);
            outputField(' ', 18);
            outputField(PADDED_TWO_DIGITS_INTEGER, epoch.getDate().getMonth(), 20);
            outputField(' ', 21);
            outputField(PADDED_TWO_DIGITS_INTEGER, epoch.getDate().getDay(), 23);
            outputField(' ', 24);
            outputField(PADDED_TWO_DIGITS_INTEGER, epoch.getTime().getHour(), 26);
            outputField(' ', 27);
            outputField(PADDED_TWO_DIGITS_INTEGER, epoch.getTime().getMinute(), 29);
            outputField(' ', 30);
            outputField(NINE_SIX_DIGITS_FLOAT, epoch.getTime().getSecond(), 39);
            outputField(' ', 40);
            outputField(TWO_DIGITS_INTEGER, clockDataLine.getNumberOfValues(), 42);
            outputField(' ', 45);
            write1912(clockDataLine.getClockBias());
            if (clockDataLine.getNumberOfValues() > 1) {
                outputField(' ', 66);
                write1912(clockDataLine.getClockBiasSigma());
                if (clockDataLine.getNumberOfValues() > 2) {
                    finishLine();
                    outputField(' ', 3);
                    write1912(clockDataLine.getClockRate());
                    if (clockDataLine.getNumberOfValues() > 3) {
                        outputField(' ', 24);
                        write1912(clockDataLine.getClockRateSigma());
                        if (clockDataLine.getNumberOfValues() > 4) {
                            outputField(' ', 45);
                            write1912(clockDataLine.getClockAcceleration());
                            if (clockDataLine.getNumberOfValues() > 5) {
                                outputField(' ', 66);
                                write1912(clockDataLine.getClockAccelerationSigma());
                            }
                        }
                    }
                }
            }
        }
        finishLine();
    }

    /** Container for clock data lines iterator. */
    private static class PendingLines {

        /** Threshold to consider dates are equal. */
        private static final double EPS = 1.0e-9;

        /** Clock name. */
        private final String name;

        /** Data lines. */
        private final List<ClockDataLine> lines;

        /** Next entry to process. */
        private int index;

        /** Simple constructor.
         * @param name clock name
         * @param lines data lines
         */
        PendingLines(final String name, final List<ClockDataLine> lines) {
            this.name  = name;
            this.lines = lines;
            this.index = 0;
        }

        /** Get next entry if close to processing date.
         * @param date processing date
         * @return next entry if close to processing date, null otherwise
         */
        ClockDataLine lineAtDate(final AbsoluteDate date) {
            if (index < lines.size() && FastMath.abs(date.durationFrom(lines.get(index))) <= EPS) {
                // return next entry and advance
                return lines.get(index++);
            } else {
                // don't touch the index
                return null;
            }
        }

        /** Get date of next entry.
         * @return date of next entry
         */
        AbsoluteDate nextDate() {
            return index <  lines.size() ? lines.get(index).getDate() : AbsoluteDate.FUTURE_INFINITY;
        }

    }

}
