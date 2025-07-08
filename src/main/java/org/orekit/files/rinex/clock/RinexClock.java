/* Copyright 2002-2025 CS GROUP
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.RinexFile;
import org.orekit.frames.Frame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.ClockOffset;
import org.orekit.time.SampledClockModel;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.TimeSpanMap;

/** Represents a parsed clock file from the IGS.
 * <p> A time system should be specified in the file. However, if it is not, default time system will be chosen
 * regarding the satellite system. If it is mixed or not specified, default time system will be UTC. </p>
 * <p> Some fields might be null after parsing. It is expected because of the numerous kind of data that can be stored in clock data file. </p>
 * <p> Caution, files with missing information in header can lead to wrong data dates and station positions.
 * It is advised to check the correctness and format compliance of the clock file to be parsed.
 * Some values such as file time scale still can be set by user. </p>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_clock300.txt"> 3.00 clock file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_clock302.txt"> 3.02 clock file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_clock304.txt"> 3.04 clock file format</a>
 *
 * @author Thomas Paulet
 * @since 11.0
 */
public class RinexClock extends RinexFile<RinexClockHeader> {

    /** A map containing receiver/satellite information. */
    private final Map<String, List<ClockDataLine>> clockData;

    /** Earliest epoch.
     * @since 12.1
     */
    private AbsoluteDate earliestEpoch;

    /** Latest epoch.
     * @since 12.1
     */
    private AbsoluteDate latestEpoch;

    /** Constructor.
     * @snce 14.0
     */
    public RinexClock() {
        super(new RinexClockHeader());

        // Initialize fields with default data
        this.clockData     = new HashMap<>();
        this.earliestEpoch = AbsoluteDate.FUTURE_INFINITY;
        this.latestEpoch   = AbsoluteDate.PAST_INFINITY;
    }

    /** Get the total number of complete data lines in the file.
     * @return the total number of complete data lines in the file
     */
    public int getTotalNumberOfDataLines() {
        int result = 0;
        final Map<String, List<ClockDataLine>> data = getClockData();
        for (final Map.Entry<String, List<ClockDataLine>> entry : data.entrySet()) {
            result += entry.getValue().size();
        }
        return result;
    }

    /** Extract the clock model.
     * @param name receiver/satellite name
     * @param nbInterpolationPoints number of points to use in interpolation
     * @return extracted clock model
     * @since 12.1
     */
    public SampledClockModel extractClockModel(final String name,
                                               final int nbInterpolationPoints) {
        final List<ClockOffset> sample = new ArrayList<>();
        clockData.
            get(name).
            forEach(c -> {
                final double offset       = c.getClockBias();
                final double rate         = c.getNumberOfValues() > 2 ? c.getClockRate()         : Double.NaN;
                final double acceleration = c.getNumberOfValues() > 4 ? c.getClockAcceleration() : Double.NaN;
                sample.add(new ClockOffset(c.getDate(), offset, rate, acceleration));
            });
        return new SampledClockModel(sample, nbInterpolationPoints);
    }

    /** Getter for an unmodifiable map of clock data.
     * @return the clock data
     */
    public Map<String, List<ClockDataLine>> getClockData() {
        return Collections.unmodifiableMap(clockData);
    }


    /** Add a clock data line to a specified receiver/satellite.
     * @param id the satellite system to add observation type
     * @param clockDataLine the clock data line to add
     */
    public void addClockData(final String id,
                             final ClockDataLine clockDataLine) {
        final List<ClockDataLine> list;
        synchronized (clockData) {
            list = clockData.computeIfAbsent(id, i -> new ArrayList<>());
        }
        list.add(clockDataLine);
        final AbsoluteDate epoch = clockDataLine.getDate();
        if (epoch.isBefore(earliestEpoch)) {
            earliestEpoch = epoch;
        }
        if (epoch.isAfter(latestEpoch)) {
            latestEpoch = epoch;
        }
    }

    /** Get earliest epoch from the {@link #getClockData() clock data}.
     * @return earliest epoch from the {@link #getClockData() clock data},
     * or {@link AbsoluteDate#FUTURE_INFINITY} if no data has been added
     * @since 12.1
     */
    public AbsoluteDate getEarliestEpoch() {
        return earliestEpoch;
    }

    /** Get latest epoch from the {@link #getClockData() clock data}.
     * @return latest epoch from the {@link #getClockData() clock data},
     * or {@link AbsoluteDate#PAST_INFINITY} if no data has been added
     * @since 12.1
     */
    public AbsoluteDate getLatestEpoch() {
        return latestEpoch;
    }

    /** Splice several Rinex clock files together.
     * <p>
     * Splicing Rinex clock files is intended to be used when continuous computation
     * covering more than one file is needed. The metadata (version number, agency, …)
     * will be retrieved from the earliest file only. Receivers and satellites
     * will be merged from all files. Some receivers or satellites may be missing
     * in some files… Once sorted (which is done internally), if the gap between
     * segments from two files is larger than {@code maxGap}, then an error
     * will be triggered.
     * </p>
     * <p>
     * The spliced file only contains the receivers and satellites that were present
     * in all files. Receivers and satellites present in some files and absent from
     * other files are silently dropped.
     * </p>
     * <p>
     * Depending on producer, successive clock files either have a gap between the last
     * entry of one file and the first entry of the next file (for example, files with
     * a 5 minutes epoch interval may end at 23:55 and the next file start at 00:00),
     * or both files have one point exactly at the splicing date (i.e. 24:00 one day
     * and 00:00 next day). In the later case, the last point of the early file is dropped,
     * and the first point of the late file takes precedence, hence only one point remains
     * in the spliced file; this design choice is made to enforce continuity and
     * regular interpolation.
     * </p>
     * @param clocks clock files to merge
     * @param maxGap maximum time gap between files
     * @return merged clock file
     * @since 12.1
     */
    public static RinexClock splice(final Collection<RinexClock> clocks,
                                    final double maxGap) {

        // sort the files
        final ChronologicalComparator comparator = new ChronologicalComparator();
        final SortedSet<RinexClock> sorted =
            new TreeSet<>((c1, c2) -> comparator.compare(c1.earliestEpoch, c2.earliestEpoch));
        sorted.addAll(clocks);

        // prepare spliced file
        final RinexClock first   = sorted.first();
        final RinexClock spliced = new RinexClock();
        spliced.getHeader().setFormatVersion(first.getHeader().getFormatVersion());
        spliced.getHeader().setSatelliteSystem(first.getHeader().getSatelliteSystem());
        spliced.getHeader().setProgramName(first.getHeader().getProgramName());
        spliced.getHeader().setRunByName(first.getHeader().getRunByName());
        spliced.getHeader().setCreationDateComponents(first.getHeader().getCreationDateComponents());
        spliced.getHeader().setCreationTimeZone(first.getHeader().getCreationTimeZone());
        spliced.getHeader().setCreationDate(first.getHeader().getCreationDate());
        first.getComments().forEach(spliced::addComment);
        first.
            getHeader().
            getSystemObservationTypes().
            forEach((s, l) -> l.forEach(o -> spliced.getHeader().addSystemObservationType(s, o)));
        spliced.getHeader().setTimeSystem(first.getHeader().getTimeSystem());
        spliced.getHeader().setLeapSeconds(first.getHeader().getLeapSeconds());
        spliced.getHeader().setLeapSecondsGNSS(first.getHeader().getLeapSecondsGNSS());
        first.getHeader().getListAppliedDCBS().forEach(dcbs -> spliced.getHeader().addAppliedDCBS(dcbs));
        first.getHeader().getListAppliedPCVS().forEach(pcvs -> spliced.getHeader().addAppliedPCVS(pcvs));
        first.getHeader().getClockDataTypes().forEach(cdt -> spliced.getHeader().addClockDataType(cdt));
        spliced.getHeader().setStationName(first.getHeader().getStationName());
        spliced.getHeader().setStationIdentifier(first.getHeader().getStationIdentifier());
        spliced.getHeader().setExternalClockReference(first.getHeader().getExternalClockReference());
        spliced.getHeader().setAnalysisCenterID(first.getHeader().getAnalysisCenterID());
        spliced.getHeader().setAnalysisCenterName(first.getHeader().getAnalysisCenterName());
        spliced.getHeader().setFrame(first.getHeader().getFrame());
        spliced.getHeader().setFrameName(first.getHeader().getFrameName());

        // merge reference clocks maps
        sorted.forEach(rc -> {
            TimeSpanMap.Span<List<ReferenceClock>> span = rc.getHeader().getReferenceClocks().getFirstSpan();
            while (span != null) {
                if (span.getData() != null) {
                    spliced.getHeader().addReferenceClockList(span.getData(), span.getStart());
                }
                span = span.next();
            }
        });

        final List<String> clockIds = new ArrayList<>();

        // identify the receivers that are present in all files
        first.
            getHeader().
            getReceivers().
            stream().
            filter(r -> availableInAllFiles(r.getDesignator(), sorted)).
            forEach(r -> {
                spliced.getHeader().addReceiver(r);
                clockIds.add(r.getDesignator());
            });

        // identify the satellites that are present in all files
        first.
            getHeader().
            getSatellites().
            stream().
            filter(s -> availableInAllFiles(s, sorted)).
            forEach(s -> {
                spliced.getHeader().addSatellite(s);
                clockIds.add(s);
            });

        // add the clock lines
        for (final String clockId : clockIds) {
            ClockDataLine pending = null;
            for (final RinexClock rc : sorted) {
                for (final ClockDataLine cd : rc.getClockData().get(clockId)) {
                    if (pending != null) {
                        final double dt = cd.getDate().durationFrom(pending.getDate());
                        if (dt > maxGap) {
                            throw new OrekitException(OrekitMessages.TOO_LONG_TIME_GAP_BETWEEN_DATA_POINTS, dt);
                        }

                        if (dt > 1.0e-6) {
                            // the pending date is *not* duplicated by this one, we can consider it
                            spliced.addClockData(clockId, pending);
                        }

                    }

                    // keep the current data line to be checked against the next one
                    pending = cd;

                }
            }

            if (pending != null) {
                // no further data lines, we need to add the remaining pending line
                spliced.addClockData(clockId, pending);
            }

        }

        return spliced;

    }

    /** Check if clock data is available in all files.
     * @param clockId clock id
     * @param files clock files
     * @return true if clock is available in all files
     */
    private static boolean availableInAllFiles(final String clockId, final Collection<RinexClock> files) {
        for (final RinexClock rc : files) {
            if (!rc.getClockData().containsKey(clockId)) {
                return false;
            }
        }
        return true;
    }

}
