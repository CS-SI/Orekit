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
package org.orekit.files.rinex.navigation;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.files.rinex.navigation.writers.BDGIMMessageWriter;
import org.orekit.files.rinex.navigation.writers.BeidouCivilianNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.BeidouLegacyNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.EarthOrientationParametersMessageWriter;
import org.orekit.files.rinex.navigation.writers.GPSCivilianNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.GPSLegacyNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.GalileoNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.GlonassNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.KlobucharMessageWriter;
import org.orekit.files.rinex.navigation.writers.NavICL1NVNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.NavICLegacyNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.NavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.NequickGMessageWriter;
import org.orekit.files.rinex.navigation.writers.QZSSCivilianNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.QZSSLegacyNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.SBASNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.SystemTimeOffsetMessageWriter;
import org.orekit.files.rinex.observation.ObservationLabel;
import org.orekit.files.rinex.section.CommonLabel;
import org.orekit.files.rinex.utils.BaseRinexWriter;
import org.orekit.gnss.PredefinedObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeStamped;
import org.orekit.utils.formatting.FastLongFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/** Writer for Rinex navigation file.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class RinexNavigationWriter extends BaseRinexWriter<RinexNavigationHeader> {

    /** Format for one 9 digits integer field. */
    protected static final FastLongFormatter NINE_DIGITS_INTEGER = new FastLongFormatter(9, false);

    /** Identifier for system time offset messages.
     * <p>
     * The identifier is prefixed with "00_" so all messages that are not related
     * to any satellites are grouped together before satellite messages
     * </p>
     */
    private static final String STO_IDENTIFIER = "00_STO";

    /** Identifier for Earth Orientation Parameters messages.
     * <p>
     * The identifier is prefixed with "00_" so all messages that are not related
     * to any satellites are grouped together before satellite messages
     * </p>
     */
    private static final String EOP_IDENTIFIER = "00_EOP";

    /** Identifier for Klobuchar model ionospheric messages.
     * <p>
     * The identifier is prefixed with "00_" so all messages that are not related
     * to any satellites are grouped together before satellite messages
     * </p>
     */
    private static final String KLOBUCHAR_IDENTIFIER = "00_IONO_KLOBUCHAR";

    /** Identifier for NeQuick G ionospheric messages.
     * <p>
     * The identifier is prefixed with "00_" so all messages that are not related
     * to any satellites are grouped together before satellite messages
     * </p>
     */
    private static final String NEQUICK_IDENTIFIER = "00_IONO_NEQUICK";

    /** Identifier for BDGIM ionospheric messages.
     * <p>
     * The identifier is prefixed with "00_" so all messages that are not related
     * to any satellites are grouped together before satellite messages
     * </p>
     */
    private static final String BDGIM_IDENTIFIER = "00_IONO_BDGIM";

    /** Mapper from satellite system to time scales. */
    private final BiFunction<SatelliteSystem, TimeScales, ? extends TimeScale> timeScaleBuilder;

    /** Set of time scales. */
    private final TimeScales timeScales;

    /** Simple constructor.
     * <p>
     * This constructor uses the {@link DataContext#getDefault() default data context}
     * and recognizes only {@link PredefinedObservationType} and {@link SatelliteSystem}
     * with non-null {@link SatelliteSystem#getObservationTimeScale() time scales}
     * (i.e. neither user-defined, nor {@link SatelliteSystem#SBAS}, nor {@link SatelliteSystem#MIXED}).
     * </p>
     * @param output destination of generated output
     * @param outputName output name for error messages
     */
    @DefaultDataContext
    public RinexNavigationWriter(final Appendable output, final String outputName) {
        this(output, outputName,
             (system, ts) -> system.getObservationTimeScale() == null ?
                             null :
                             system.getObservationTimeScale().getTimeScale(ts),
             DataContext.getDefault().getTimeScales());
    }

    /** Simple constructor.
     * @param output destination of generated output
     * @param outputName output name for error messages
     * @param timeScaleBuilder mapper from satellite system to time scales (useful for user-defined satellite systems)
     * @param timeScales the set of time scales to use when parsing dates
     * @since 13.0
     */
    public RinexNavigationWriter(final Appendable output, final String outputName,
                                 final BiFunction<SatelliteSystem, TimeScales, ? extends TimeScale> timeScaleBuilder,
                                 final TimeScales timeScales) {
        super(output, outputName);
        this.timeScaleBuilder = timeScaleBuilder;
        this.timeScales       = timeScales;
    }

    /** Write a complete navigation file.
     * @param rinexNavigation Rinex navigation file to write
     * @exception IOException if an I/O error occurs.
     */
    public void writeCompleteFile(final RinexNavigation rinexNavigation) throws IOException {

        prepareComments(rinexNavigation.getComments());
        writeHeader(rinexNavigation.getHeader());

        // prepare chronological iteration
        final List<PendingMessages<?>> pending = new ArrayList<>();

        // messages associated with satellites
        pending.addAll(createHandlers(rinexNavigation.getGPSLegacyNavigationMessages(),
                                      new GPSLegacyNavigationMessageWriter()));
        pending.addAll(createHandlers(rinexNavigation.getGPSCivilianNavigationMessages(),
                                      new GPSCivilianNavigationMessageWriter()));
        pending.addAll(createHandlers(rinexNavigation.getGalileoNavigationMessages(),
                                      new GalileoNavigationMessageWriter()));
        pending.addAll(createHandlers(rinexNavigation.getBeidouLegacyNavigationMessages(),
                                      new BeidouLegacyNavigationMessageWriter()));
        pending.addAll(createHandlers(rinexNavigation.getBeidouCivilianNavigationMessages(),
                                      new BeidouCivilianNavigationMessageWriter()));
        pending.addAll(createHandlers(rinexNavigation.getQZSSLegacyNavigationMessages(),
                                      new QZSSLegacyNavigationMessageWriter()));
        pending.addAll(createHandlers(rinexNavigation.getQZSSCivilianNavigationMessages(),
                                      new QZSSCivilianNavigationMessageWriter()));
        pending.addAll(createHandlers(rinexNavigation.getNavICLegacyNavigationMessages(),
                                      new NavICLegacyNavigationMessageWriter()));
        pending.addAll(createHandlers(rinexNavigation.getNavICL1NVNavigationMessages(),
                                      new NavICL1NVNavigationMessageWriter()));
        pending.addAll(createHandlers(rinexNavigation.getGlonassNavigationMessages(),
                                      new GlonassNavigationMessageWriter()));
        pending.addAll(createHandlers(rinexNavigation.getSBASNavigationMessages(),
                                      new SBASNavigationMessageWriter()));

        // messages independent of satellites
        pending.add(new PendingMessages<>(STO_IDENTIFIER, new SystemTimeOffsetMessageWriter(),
                                          rinexNavigation.getSystemTimeOffsets()));
        pending.add(new PendingMessages<>(EOP_IDENTIFIER, new EarthOrientationParametersMessageWriter(),
                                          rinexNavigation.getEarthOrientationParameters()));
        pending.add(new PendingMessages<>(KLOBUCHAR_IDENTIFIER, new KlobucharMessageWriter(),
                                          rinexNavigation.getKlobucharMessages()));
        pending.add(new PendingMessages<>(NEQUICK_IDENTIFIER, new NequickGMessageWriter(),
                                          rinexNavigation.getNequickGMessages()));
        pending.add(new PendingMessages<>(BDGIM_IDENTIFIER, new BDGIMMessageWriter(),
                                          rinexNavigation.getBDGIMMessages()));

        pending.sort(Comparator.comparing(pl -> pl.identifier));

        // write messages in chronological order
        for (AbsoluteDate date = earliest(pending); date.isFinite(); date = earliest(pending)) {
            // write all messages that correspond to this date
            for (final PendingMessages<?> pm : pending) {
                pm.writeMessageAtDate(date, getHeader());
            }
        }

    }

    /** Create messages handler for one message type.
     * @param <T> type of the navigation message
     * @param map messages map
     * @param messageWriter writer for the current message type
     * @return list of handlers for one message type
     */
    private <T extends TimeStamped> List<PendingMessages<T>> createHandlers(final Map<String, List<T>> map,
                                                                            final NavigationMessageWriter<T> messageWriter) {
        final List<PendingMessages<T>> handlers = new ArrayList<>();
        for (final Map.Entry<String, List<T>> entry : map.entrySet()) {
            handlers.add(new PendingMessages<>(entry.getKey(), messageWriter, entry.getValue()));
        }
        return handlers;
    }

    /** Find the earliest pending date.
     * @param pending pending messages
     * @return earliest pending date
     */
    private AbsoluteDate earliest(final List<PendingMessages<?>> pending) {
        AbsoluteDate earliest = AbsoluteDate.FUTURE_INFINITY;
        for (final PendingMessages<?> pm : pending) {
            if (pm.nextDate().isBefore(earliest)) {
                earliest = pm.nextDate();
            }
        }
        return earliest;
    }

    /** Write header.
     * <p>
     * This method must be called exactly once at the beginning
     * (directly or by {@link #writeCompleteFile(RinexNavigation)})
     * </p>
     * @param header header to write
     * @exception IOException if an I/O error occurs.
     */
    public void writeHeader(final RinexNavigationHeader header) throws IOException {

        super.writeHeader(header, RinexNavigationHeader.LABEL_INDEX);

        // RINEX VERSION / TYPE
        outputField(NINE_TWO_DIGITS_FLOAT, header.getFormatVersion(), 9);
        outputField("",                20, true);
        outputField("NAVIGATION DATA", 40, true);
        outputField(header.getSatelliteSystem().getKey(), 41);
        finishHeaderLine(CommonLabel.VERSION);

        // PGM / RUN BY / DATE
        writeProgramRunByDate(header);

        // REC # / TYPE / VERS
        outputField(header.getReceiverNumber(),  20, true);
        outputField(header.getReceiverType(),    40, true);
        outputField(header.getReceiverVersion(), 60, true);
        finishHeaderLine(ObservationLabel.REC_NB_TYPE_VERS);

        // MERGED FILE
        if (header.getMergedFiles() > 0) {
            outputField(NINE_DIGITS_INTEGER, header.getMergedFiles(), 9);
            finishHeaderLine(NavigationLabel.MERGED_FILE);
        }

        // DOI
        writeHeaderLine(header.getDoi(), CommonLabel.DOI);

        // LICENSE OF USE
        writeHeaderLine(header.getLicense(), CommonLabel.LICENSE);

        // STATION INFORMATION
        writeHeaderLine(header.getStationInformation(), CommonLabel.STATION_INFORMATION);

        // LEAP SECONDS
        if (header.getLeapSecondsGNSS() > 0) {
            outputField(SIX_DIGITS_INTEGER, header.getLeapSecondsGNSS(), 6);
            if (header.getFormatVersion() > 3.0) {
                // extra fields introduced in 3.01
                outputField(SIX_DIGITS_INTEGER, header.getLeapSecondsFuture(),  12);
                outputField(SIX_DIGITS_INTEGER, header.getLeapSecondsWeekNum(), 18);
                outputField(SIX_DIGITS_INTEGER, header.getLeapSecondsDayNum(),  24);
            }
            finishHeaderLine(CommonLabel.LEAP_SECONDS);
        }

        // END OF HEADER
        writeHeaderLine("", CommonLabel.END);

    }

    /** Write a date.
     * @param date date to write
     * @param system satellite system
    * @exception IOException if an I/O error occurs.
     */
    public void writeDate(final AbsoluteDate date, final SatelliteSystem system) throws IOException {
        writeDate(date.getComponents(timeScaleBuilder.apply(system, timeScales)));
    }

    /** Write a date.
     * <p>
     * The date will span over 23 characters.
     * </p>
     * @param dtc date to write
     * @exception IOException if an I/O error occurs.
     */
    private void writeDate(final DateTimeComponents dtc) throws IOException {
        final DateTimeComponents rounded = dtc.roundIfNeeded(60, 0);
        final int start = getColumn();
        outputField(' ', start + 4);
        outputField(BaseRinexWriter.FOUR_DIGITS_INTEGER, rounded.getDate().getYear(), start + 8);
        outputField(' ', start + 9);
        outputField(BaseRinexWriter.PADDED_TWO_DIGITS_INTEGER, rounded.getDate().getMonth(), start + 11);
        outputField(' ', start + 12);
        outputField(BaseRinexWriter.PADDED_TWO_DIGITS_INTEGER, rounded.getDate().getDay(), start + 14);
        outputField(' ', start + 15);
        outputField(BaseRinexWriter.PADDED_TWO_DIGITS_INTEGER, rounded.getTime().getHour(), start + 17);
        outputField(' ', start + 18);
        outputField(BaseRinexWriter.PADDED_TWO_DIGITS_INTEGER, rounded.getTime().getMinute(), start + 20);
        outputField(' ', start + 21);
        outputField(BaseRinexWriter.PADDED_TWO_DIGITS_INTEGER,
                    (int) FastMath.round(rounded.getTime().getSecond()), start + 23);
    }

    /** Container for navigation messages iterator.
     * @param <T> type of the navigation message
     */
    private class PendingMessages<T extends TimeStamped> {

        /** Threshold to consider dates are equal. */
        private static final double EPS = 1.0e-9;

        /** Identifier. */
        private final String identifier;

        /** Writer for the current message type. */
        private final NavigationMessageWriter<T> messageWriter;

        /** Navigation messages. */
        private final List<T> messages;

        /** Next entry to process. */
        private int index;

        /** Simple constructor.
         * @param identifier identifier
         * @param messageWriter writer for the current message type
         * @param messages navigation messages
         */
        PendingMessages(final String identifier, final NavigationMessageWriter<T> messageWriter,
                        final List<T> messages) {
            this.identifier    = identifier;
            this.messageWriter = messageWriter;
            this.messages      = messages;
            this.index         = 0;
        }

        /** Write next entry if close to processing date.
         * @param date processing date
         * @param header file header
         * @exception IOException if an I/O error occurs.
         */
        void writeMessageAtDate(final AbsoluteDate date, final RinexNavigationHeader header) throws IOException {
            if (index < messages.size() && FastMath.abs(date.durationFrom(messages.get(index))) <= EPS) {
                // write next entry and advance
                messageWriter.writeMessage(identifier, messages.get(index++), header, RinexNavigationWriter.this);
            }
        }

        /** Get date of next entry.
         * @return date of next entry
         */
        AbsoluteDate nextDate() {
            return index <  messages.size() ? messages.get(index).getDate() : AbsoluteDate.FUTURE_INFINITY;
        }

    }

}
