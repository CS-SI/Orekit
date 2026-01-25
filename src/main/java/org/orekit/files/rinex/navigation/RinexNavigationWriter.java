/* Copyright 2022-2026 Thales Alenia Space
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
import org.orekit.files.rinex.navigation.writers.ionosphere.BDGIMMessageWriter;
import org.orekit.files.rinex.navigation.writers.ephemeris.BeidouCivilianNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.ephemeris.BeidouLegacyNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.EarthOrientationParametersMessageWriter;
import org.orekit.files.rinex.navigation.writers.ephemeris.GPSCivilianNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.ephemeris.GPSLegacyNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.ephemeris.GalileoNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.ionosphere.GlonassCDMSMessageWriter;
import org.orekit.files.rinex.navigation.writers.ephemeris.GlonassFdmaNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.ionosphere.KlobucharMessageWriter;
import org.orekit.files.rinex.navigation.writers.ionosphere.NavICKlobucharMessageWriter;
import org.orekit.files.rinex.navigation.writers.ephemeris.NavICL1NVNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.ephemeris.NavICLegacyNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.ionosphere.NavICNeQuickNMessageWriter;
import org.orekit.files.rinex.navigation.writers.NavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.ionosphere.NequickGMessageWriter;
import org.orekit.files.rinex.navigation.writers.ephemeris.QZSSCivilianNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.ephemeris.QZSSLegacyNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.ephemeris.SBASNavigationMessageWriter;
import org.orekit.files.rinex.navigation.writers.SystemTimeOffsetMessageWriter;
import org.orekit.files.rinex.observation.ObservationLabel;
import org.orekit.files.rinex.section.CommonLabel;
import org.orekit.files.rinex.utils.BaseRinexWriter;
import org.orekit.gnss.PredefinedObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.analytical.gnss.data.NavigationMessage;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.formatting.FastDoubleFormatter;
import org.orekit.utils.formatting.FastLongFormatter;
import org.orekit.utils.formatting.FastScientificFormatter;
import org.orekit.utils.units.Unit;

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
    private static final FastLongFormatter NINE_DIGITS_INTEGER = new FastLongFormatter(9, false);

    /** Format for one 12 digits float field. */
    private static final FastDoubleFormatter TWELVE_DIGITS_SCIENTIFIC = new FastScientificFormatter(12);

    /** Format for one 16 digits float field. */
    private static final FastDoubleFormatter SIXTEEN_DIGITS_SCIENTIFIC = new FastScientificFormatter(16);

    /** Format for one 17 digits float field. */
    private static final FastDoubleFormatter SEVENTEEN_DIGITS_SCIENTIFIC = new FastScientificFormatter(17);

    /** Identifier for system time offset messages.
     * <p>
     * The identifier is prefixed with "00_" so all time offset messages
     * are grouped together before satellite messages
     * </p>
     */
    private static final String STO_IDENTIFIER = "00_STO";

    /** Identifier for Earth Orientation Parameters messages.
     * <p>
     * The identifier is prefixed with "00_" so all EOP messages
     * are grouped together before satellite messages
     * </p>
     */
    private static final String EOP_IDENTIFIER = "00_EOP";

    /** Identifier for Klobuchar model ionospheric messages.
     * <p>
     * The identifier is prefixed with "00_" so all Klobuchar messages
     * are grouped together before satellite messages
     * </p>
     */
    private static final String KLOBUCHAR_IDENTIFIER = "00_IONO_KLOBUCHAR";

    /** Identifier for NeQuick G ionospheric messages.
     * <p>
     * The identifier is prefixed with "00_" so all NeQuick messages
     * are grouped together before satellite messages
     * </p>
     */
    private static final String NEQUICK_IDENTIFIER = "00_IONO_NEQUICK";

    /** Identifier for BDGIM ionospheric messages.
     * <p>
     * The identifier is prefixed with "00_" so all BDGIM messages
     * are grouped together before satellite messages
     * </p>
     */
    private static final String BDGIM_IDENTIFIER = "00_IONO_BDGIM";

    /** Identifier for NavIC Klobuchar ionospheric messages.
     * <p>
     * The identifier is prefixed with "00_" so all NavIC Klobuchar messages
     * are grouped together before satellite messages
     * </p>
     */
    private static final String NAVIC_KLOBUCHAR_IDENTIFIER = "00_IONO_NAVIC_KLOBUCHAR";

    /** Identifier for NavIC NeQuick N ionospheric messages.
     * <p>
     * The identifier is prefixed with "00_" so all NavIC NeQuick N messages
     * are grouped together before satellite messages
     * </p>
     */
    private static final String NAVIC_NEQUICK_N_IDENTIFIER = "00_IONO_NAVIC_NEQUICK_N";

    /** Identifier for GLONASS CDMS ionospheric messages.
     * <p>
     * The identifier is prefixed with "00_" so all GLONASS CDMS messages
     * are grouped together before satellite messages
     * </p>
     */
    private static final String GLONASS_CDMS_IDENTIFIER = "00_IONO_GLONASS_CDMS_N";

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

    /** Get the known time scales.
     * @return known time scales
     */
    public TimeScales getTimeScales() {
        return timeScales;
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

        // ephemeris messages
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
                                      new GlonassFdmaNavigationMessageWriter()));
        pending.addAll(createHandlers(rinexNavigation.getSBASNavigationMessages(),
                                      new SBASNavigationMessageWriter()));

        // STO messages
        addHandler(pending, STO_IDENTIFIER,
                   rinexNavigation.getSystemTimeOffsets(), new SystemTimeOffsetMessageWriter());

        // EOP messages
        addHandler(pending, EOP_IDENTIFIER,
                   rinexNavigation.getEarthOrientationParameters(), new EarthOrientationParametersMessageWriter());

        // ION messages
        addHandler(pending, KLOBUCHAR_IDENTIFIER,
                   rinexNavigation.getKlobucharMessages(), new KlobucharMessageWriter());
        addHandler(pending, NEQUICK_IDENTIFIER,
                   rinexNavigation.getNequickGMessages(), new NequickGMessageWriter());
        addHandler(pending, BDGIM_IDENTIFIER,
                   rinexNavigation.getBDGIMMessages(), new BDGIMMessageWriter());
        addHandler(pending, NAVIC_KLOBUCHAR_IDENTIFIER,
                   rinexNavigation.getNavICKlobucharMessages(), new NavICKlobucharMessageWriter());
        addHandler(pending, NAVIC_NEQUICK_N_IDENTIFIER,
                   rinexNavigation.getNavICNeQuickNMessages(), new NavICNeQuickNMessageWriter());
        addHandler(pending, GLONASS_CDMS_IDENTIFIER,
                   rinexNavigation.getGlonassCDMSMessages(), new GlonassCDMSMessageWriter());

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
    private <T extends NavigationMessage> List<PendingMessages<?>> createHandlers(final Map<String, List<T>> map,
                                                                                  final NavigationMessageWriter<T> messageWriter) {
        final List<PendingMessages<?>> handlers = new ArrayList<>();
        for (final Map.Entry<String, List<T>> entry : map.entrySet()) {
            addHandler(handlers, entry.getKey(), entry.getValue(), messageWriter);
        }
        return handlers;
    }

    /** Add message handler for one message type.
     * @param <T> type of the navigation message
     * @param pending list to complete
     * @param identifier identifier
     * @param list messages list
     * @param messageWriter writer for the current message type
     */
    private <T extends NavigationMessage> void addHandler(final List<PendingMessages<?>> pending,
                                                          final String identifier, final List<T> list,
                                                          final NavigationMessageWriter<T> messageWriter) {
        if (!list.isEmpty()) {
            pending.add(new PendingMessages<>(identifier, messageWriter, list));
        }
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
        if (header.getFormatVersion() < 3.0 && header.getSatelliteSystem() == SatelliteSystem.GLONASS) {
            outputField("GLONASS NAV DATA", 40, true);
        } else {
            outputField("NAVIGATION DATA", 40, true);
        }
        outputField(header.getSatelliteSystem().getKey(), 41);
        finishHeaderLine(CommonLabel.VERSION);

        // PGM / RUN BY / DATE
        writeProgramRunByDate(header);

        if (header.getFormatVersion() < 3.0) {

            // IONOSPHERIC CORR
            for (final IonosphericCorrection correction : header.getIonosphericCorrections()) {
                if (correction.getType() != IonosphericCorrectionType.GAL) {
                    // Rinex 2 only supports Klobuchar
                    final KlobucharIonosphericCorrection klobuchar = (KlobucharIonosphericCorrection) correction;
                    final double[] alpha = klobuchar.getKlobucharAlpha();
                    final double[] beta  = klobuchar.getKlobucharBeta();
                    outputField(' ', 2);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, alpha[0], 14);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, alpha[1], 26);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, alpha[2], 38);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, alpha[3], 50);
                    finishHeaderLine(NavigationLabel.ION_ALPHA);
                    outputField(' ', 2);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, beta[0], 14);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, beta[1], 26);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, beta[2], 38);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, beta[3], 50);
                    finishHeaderLine(NavigationLabel.ION_BETA);
                }
            }

            // TIME SYSTEM CORR
            for (final TimeSystemCorrection correction : header.getTimeSystemCorrections()) {
                if ("GPUT".equals(correction.getTimeSystemCorrectionType())) {
                    final GNSSDate date = new GNSSDate(
                            correction.getReferenceDate(),
                            SatelliteSystem.GPS,
                            getTimeScales());
                    outputField(' ', 3);
                    outputField(NINETEEN_SCIENTIFIC_FLOAT, correction.getTimeSystemCorrectionA0(), 22);
                    outputField(NINETEEN_SCIENTIFIC_FLOAT, correction.getTimeSystemCorrectionA1(), 41);
                    outputField(NINE_DIGITS_INTEGER, (int) FastMath.rint(date.getSecondsInWeek()), 50);
                    outputField(NINE_DIGITS_INTEGER, date.getWeekNumber(), 59);
                    finishHeaderLine(NavigationLabel.DELTA_UTC);
                } else {
                    final DateComponents dt = correction.getReferenceDate().
                                              getComponents(getTimeScales().getGLONASS()).
                                              getDate();
                    outputField(SIX_DIGITS_INTEGER, dt.getYear(),   6);
                    outputField(SIX_DIGITS_INTEGER, dt.getMonth(), 12);
                    outputField(SIX_DIGITS_INTEGER, dt.getDay(),   18);
                    outputField(' ', 21);
                    outputField(NINETEEN_SCIENTIFIC_FLOAT, correction.getTimeSystemCorrectionA0(), 40);
                    finishHeaderLine(NavigationLabel.CORR_TO_SYSTEM_TIME);
                }
            }

        } else if (header.getFormatVersion() < 4.0) {

            // IONOSPHERIC CORR
            for (final IonosphericCorrection correction : header.getIonosphericCorrections()) {
                if (correction.getType() == IonosphericCorrectionType.GAL) {
                    final NeQuickGIonosphericCorrection nequick = (NeQuickGIonosphericCorrection) correction;
                    final double[] alpha = nequick.getNeQuickAlpha();
                    outputField(nequick.getType().toString(), 5, true);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, alpha[0], 17);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, alpha[1], 29);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, alpha[2], 41);
                    outputField("", 53, true);
                    outputField(nequick.getTimeMark(), 54);
                    finishHeaderLine(NavigationLabel.IONOSPHERIC_CORR);
                } else {
                    final KlobucharIonosphericCorrection klobuchar = (KlobucharIonosphericCorrection) correction;
                    final double[] alpha = klobuchar.getKlobucharAlpha();
                    final double[] beta  = klobuchar.getKlobucharBeta();
                    outputField(klobuchar.getType().toString(), 3, true);
                    outputField("A ", 5, true);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, alpha[0], 17);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, alpha[1], 29);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, alpha[2], 41);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, alpha[3], 53);
                    outputField(klobuchar.getTimeMark(), 54);
                    finishHeaderLine(NavigationLabel.IONOSPHERIC_CORR);
                    outputField(klobuchar.getType().toString(), 3, true);
                    outputField("B ", 5, true);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, beta[0], 17);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, beta[1], 29);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, beta[2], 41);
                    outputField(TWELVE_DIGITS_SCIENTIFIC, beta[3], 53);
                    outputField(klobuchar.getTimeMark(), 54);
                    finishHeaderLine(NavigationLabel.IONOSPHERIC_CORR);
                }
            }

            // TIME SYSTEM CORR
            for (final TimeSystemCorrection correction : header.getTimeSystemCorrections()) {
                final SatelliteSystem system = header.getSatelliteSystem() == SatelliteSystem.BEIDOU ?
                                               header.getSatelliteSystem() :
                                               SatelliteSystem.GPS;
                final GNSSDate date;
                if (correction.getReferenceDate() == null) {
                    date = new GNSSDate(0, 0, system, getTimeScales());
                } else {
                    date = new GNSSDate(
                            correction.getReferenceDate(),
                            system,
                            getTimeScales());
                }
                outputField(correction.getTimeSystemCorrectionType(), 5, true);
                outputField(SEVENTEEN_DIGITS_SCIENTIFIC, correction.getTimeSystemCorrectionA0(), 22);
                outputField(SIXTEEN_DIGITS_SCIENTIFIC,   correction.getTimeSystemCorrectionA1(), 38);
                outputField(' ', 39);
                outputField(SIX_DIGITS_INTEGER, (int) FastMath.rint(date.getSecondsInWeek()), 45);
                outputField(' ', 46);
                outputField(FOUR_DIGITS_INTEGER, date.getWeekNumber(), 50);
                outputField(' ', 51);
                outputField(correction.getSatId(), 56, false);
                outputField(' ', 57);
                outputField(TWO_DIGITS_INTEGER, correction.getUtcId(), 59);
                finishHeaderLine(NavigationLabel.TIME_SYSTEM_CORR);
            }

        } else {

            // REC # / TYPE / VERS
            if (header.getReceiverNumber() != null) {
                outputField(header.getReceiverNumber(), 20, true);
                outputField(header.getReceiverType(), 40, true);
                outputField(header.getReceiverVersion(), 60, true);
                finishHeaderLine(ObservationLabel.REC_NB_TYPE_VERS);
            }

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

        }

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
    public void writeDate(final DateTimeComponents dtc) throws IOException {
        final DateTimeComponents rounded = dtc.roundIfNeeded(60, 0);
        final int start = getColumn();
        outputField(BaseRinexWriter.FOUR_DIGITS_INTEGER, rounded.getDate().getYear(), start + 4);
        outputField(' ', start + 5);
        outputField(BaseRinexWriter.PADDED_TWO_DIGITS_INTEGER, rounded.getDate().getMonth(), start + 7);
        outputField(' ', start + 8);
        outputField(BaseRinexWriter.PADDED_TWO_DIGITS_INTEGER, rounded.getDate().getDay(), start + 10);
        outputField(' ', start + 11);
        outputField(BaseRinexWriter.PADDED_TWO_DIGITS_INTEGER, rounded.getTime().getHour(), start + 13);
        outputField(' ', start + 14);
        outputField(BaseRinexWriter.PADDED_TWO_DIGITS_INTEGER, rounded.getTime().getMinute(), start + 16);
        outputField(' ', start + 17);
        outputField(BaseRinexWriter.PADDED_TWO_DIGITS_INTEGER,
                    (int) FastMath.round(rounded.getTime().getSecond()), start + 19);
    }

    /** Write a double field.
     * <p>
     * The field will span over 19 characters.
     * </p>
     * @param value field value to write, in SI units
     * @param unit unit to use
     * @exception IOException if an I/O error occurs.
     */
    public void writeDouble(final double value, final Unit unit) throws IOException {
        outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, unit.fromSI(value), getColumn() + 19);
    }

    /** Write an integer field.
     * <p>
     * The field will span over 19 characters.
     * </p>
     * @param value field value to write, in SI units
     * @exception IOException if an I/O error occurs.
     */
    public void writeInt(final int value) throws IOException {
        outputField(BaseRinexWriter.NINETEEN_SCIENTIFIC_FLOAT, value, getColumn() + 19);
    }

    /** Write an empty field.
     * <p>
     * The field will span over 19 characters.
     * </p>
     * @exception IOException if an I/O error occurs.
     */
    public void writeEmpty() throws IOException {
        outputField("", getColumn() + 19, true);
    }

    /** Start (indent) a new line.
     * @param header header
     * @exception IOException if an I/O error occurs.
     */
    public void indentLine(final RinexNavigationHeader header)
        throws
        IOException {
        if (header.getFormatVersion() < 3.0) {
            outputField("   ",  3, true);
        } else {
            outputField("    ", 4, true);
        }
    }

    /** Container for navigation messages iterator.
     * @param <T> type of the navigation message
     */
    private class PendingMessages<T extends NavigationMessage> {

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
