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
package org.orekit.files.sinex;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.EopHistoryLoader;
import org.orekit.frames.ITRFVersion;
import org.orekit.gnss.GnssSignal;
import org.orekit.gnss.SatInSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScales;

import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Container for Solution INdependent EXchange (SINEX) files.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 13.0
 */
public class Sinex extends AbstractSinex {

    /** Satellites phase centers. */
    private final Map<SatInSystem, Map<GnssSignal, Vector3D>> satellitesPhaseCenters;

    /** Station data. */
    private final Map<String, Station> stations;

    /** Earth Orientation Parameters data. */
    private final Map<AbsoluteDate, SinexEopEntry> eop;

    /** Simple constructor.
     * @param timeScales time scales
     * @param creationDate SINEX file creation date
     * @param startDate start time of the data used in the Sinex solution
     * @param endDate end time of the data used in the Sinex solution
     * @param satellitesPhaseCenters satellites phase centers
     * @param stations station data
     * @param eop Earth Orientation Parameters data
     */
    public Sinex(final TimeScales timeScales,
                 final AbsoluteDate creationDate, final AbsoluteDate startDate, final AbsoluteDate endDate,
                 final Map<SatInSystem, Map<GnssSignal, Vector3D>> satellitesPhaseCenters,
                 final Map<String, Station> stations, final Map<AbsoluteDate, SinexEopEntry> eop) {
        super(timeScales, creationDate, startDate, endDate);
        this.satellitesPhaseCenters = satellitesPhaseCenters;
        this.stations               = stations;
        this.eop                    = eop;
    }

    /** Get the parsed satellites phase centers.
     * @return unmodifiable view of parsed satellites phase centers
     */
    public Map<SatInSystem, Map<GnssSignal, Vector3D>> getSatellitesPhaseCenters() {
        return Collections.unmodifiableMap(satellitesPhaseCenters);
    }

    /** Get the parsed station data.
     * @return unmodifiable view of parsed station data
     */
    public Map<String, Station> getStations() {
        return Collections.unmodifiableMap(stations);
    }

    /** Get the parsed EOP data.
     * @param itrfVersion ITRF version corresponding to the entries
     * @return loader for EOP data
     */
    public EopHistoryLoader getEopLoader(final ITRFVersion itrfVersion) {
        return (converter, history) -> {

            // first set up all entries explicitly present in the parsed files
            final SortedSet<SinexEopEntry> sorted = new TreeSet<>(new ChronologicalComparator());
            sorted.addAll(eop.values());

            // copy first and last entries according to files validity
            sorted.add(sorted.first().toNewEpoch(getFileEpochStartTime()));
            sorted.add(sorted.last().toNewEpoch(getFileEpochEndTime()));

            if (sorted.size() < 4) {
                // insert extra entries after first and before last to allow interpolation
                sorted.add(sorted.first().toNewEpoch(getFileEpochStartTime().shiftedBy(1.0)));
                sorted.add(sorted.last().toNewEpoch(getFileEpochEndTime().shiftedBy(-1.0)));
            }

            // convert to regular EOP history
            sorted.forEach(e -> history.add(e.toEopEntry(converter, itrfVersion, getTimeScales().getUTC())));

        };
    }

}
