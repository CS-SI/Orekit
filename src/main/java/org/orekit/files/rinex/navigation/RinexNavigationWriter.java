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
import org.orekit.files.rinex.observation.ObservationDataSet;
import org.orekit.files.rinex.section.CommonLabel;
import org.orekit.files.rinex.utils.BaseRinexWriter;
import org.orekit.gnss.PredefinedObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;

/** Writer for Rinex navigation file.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class RinexNavigationWriter extends BaseRinexWriter<RinexNavigationHeader> {

    /** Mapper from satellite system to time scales. */
    private final BiFunction<SatelliteSystem, TimeScales, ? extends TimeScale> timeScaleBuilder;

    /** Set of time scales. */
    private final TimeScales timeScales;

    /** Time scale for writing dates. */
    private TimeScale timeScale;

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

    /** Write a complete observation file.
     * <p>
     * This method calls {@link #prepareComments(List)} and
     * {@link #writeHeader(RinexNavigationHeader)} once and then loops on
     * calling {@link #writeObservationDataSet(ObservationDataSet)}
     * for all observation data sets in the file
     * </p>
     * @param rinexNavigation Rinex navigation file to write
     * @see #writeHeader(RinexNavigationHeader)
     * @see #writeObservationDataSet(ObservationDataSet)
     * @exception IOException if an I/O error occurs.
     */
    @DefaultDataContext
    public void writeCompleteFile(final RinexNavigation rinexNavigation) throws IOException {
        prepareComments(rinexNavigation.getComments());
        writeHeader(rinexNavigation.getHeader());
//        for (final ObservationDataSet observationDataSet : rinexNavigation.getObservationDataSets()) {
//            writeObservationDataSet(observationDataSet);
//        }
    }

    /** Write header.
     * <p>
     * This method must be called exactly once at the beginning
     * (directly or by {@link #writeCompleteFile(RinexNavigation)})
     * </p>
     * @param header header to write
     * @exception IOException if an I/O error occurs.
     */
    @DefaultDataContext
    public void writeHeader(final RinexNavigationHeader header) throws IOException {

        super.writeHeader(header, RinexNavigationHeader.LABEL_INDEX);

        final String timeScaleName;
        final TimeScale built = timeScaleBuilder.apply(header.getSatelliteSystem(), timeScales);
        if (built != null) {
            timeScale     = built;
            timeScaleName = "   ";
        } else {
            timeScale     = timeScaleBuilder.apply(SatelliteSystem.GPS, timeScales);
            timeScaleName = timeScale.getName();
        }

        // RINEX VERSION / TYPE
        outputField(NINE_TWO_DIGITS_FLOAT, header.getFormatVersion(), 9);
        outputField("",                20, true);
        outputField("NAVIGATION DATA", 40, true);
        outputField(header.getSatelliteSystem().getKey(), 41);
        finishHeaderLine(CommonLabel.VERSION);

        // PGM / RUN BY / DATE
        outputField(header.getProgramName(), 20, true);
        outputField(header.getRunByName(),   40, true);
        final DateTimeComponents dtc = header.getCreationDateComponents();
        if (header.getFormatVersion() < 3.0 && dtc.getTime().getSecond() < 0.5) {
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getDate().getDay(), 42);
            outputField('-', 43);
            outputField(dtc.getDate().getMonthEnum().getUpperCaseAbbreviation(), 46,  true);
            outputField('-', 47);
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getDate().getYear() % 100, 49);
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getTime().getHour(), 52);
            outputField(':', 53);
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getTime().getMinute(), 55);
            outputField(header.getCreationTimeZone(), 58, true);
        } else {
            outputField(PADDED_FOUR_DIGITS_INTEGER, dtc.getDate().getYear(), 44);
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getDate().getMonth(), 46);
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getDate().getDay(), 48);
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getTime().getHour(), 51);
            outputField(PADDED_TWO_DIGITS_INTEGER, dtc.getTime().getMinute(), 53);
            outputField(PADDED_TWO_DIGITS_INTEGER, (int) FastMath.rint(dtc.getTime().getSecond()), 55);
            outputField(header.getCreationTimeZone(), 59, false);
        }
        finishHeaderLine(CommonLabel.PROGRAM);

        // DOI
        writeHeaderLine(header.getDoi(), CommonLabel.DOI);

        // LICENSE OF USE
        writeHeaderLine(header.getLicense(), CommonLabel.LICENSE);

        // STATION INFORMATION
        writeHeaderLine(header.getStationInformation(), CommonLabel.STATION_INFORMATION);

        // END OF HEADER
        writeHeaderLine("", CommonLabel.END);

    }

//    /** Write one observation data set.
//     * <p>
//     * Note that this writer outputs only regular observations, so
//     * the event flag is always set to 0
//     * </p>
//     * @param observationDataSet observation data set to write
//     * @exception IOException if an I/O error occurs.
//     */
//    public void writeObservationDataSet(final ObservationDataSet observationDataSet) throws IOException {
//
//        // check header has already been written
//        checkHeaderWritten();
//
//        if (!pending.isEmpty() && observationDataSet.durationFrom(pending.get(0).getDate()) > EPS_DATE) {
//            // the specified observation belongs to the next batch
//            // we must process the current batch of pending observations
//            processPending();
//        }
//
//        // add the observation to the pending list, so it is written later on
//        pending.add(observationDataSet);
//
//    }

}
