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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.RinexFile;
import org.orekit.time.AbsoluteDate;

/** Container for Rinex observation file.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class RinexObservation extends RinexFile<RinexObservationHeader> {

    /** Observations. */
    private final List<ObservationDataSet> observations;

    /** Simple constructor.
     */
    public RinexObservation() {
        super(new RinexObservationHeader());
        this.observations = new ArrayList<>();
    }

    /** Get an unmodifiable view of the observations.
     * @return unmodifiable view of the observations
     */
    public List<ObservationDataSet> getObservationDataSets() {
        return Collections.unmodifiableList(observations);
    }

    /** Add an observations data set.
     * <p>
     * Observations must be added chronologically, within header date range, and separated
     * by an integer multiple of the {@link RinexObservationHeader#getInterval() interval}
     * (ideally one interval, but entries at same dates and missing entries are allowed so
     * any non-negative integer is allowed).
     * </p>
     * @param observationsDataSet observations data set
     */
    public void addObservationDataSet(final ObservationDataSet observationsDataSet) {

        final RinexObservationHeader header  = getHeader();
        final AbsoluteDate           current = observationsDataSet.getDate();

        // check interval from previous observation
        if (!observations.isEmpty()) {
            final AbsoluteDate previous   = observations.get(observations.size() - 1).getDate();
            final double       factor     = current.durationFrom(previous) / header.getInterval();
            final double       acceptable = FastMath.max(0.0, FastMath.rint(factor));
            if (FastMath.abs(factor - acceptable) > 0.001) {
                throw new OrekitIllegalArgumentException(OrekitMessages.INCONSISTENT_SAMPLING_DATE,
                                                         previous.shiftedBy(acceptable * header.getInterval()),
                                                         current);
            }
        }

        // check global range
        final AbsoluteDate first = header.getTFirstObs();
        final AbsoluteDate last  = header.getTLastObs();
        if (!current.isBetweenOrEqualTo(first, last)) {
            throw new OrekitIllegalArgumentException(OrekitMessages.OUT_OF_RANGE_DATE,
                                                     current, first, last);
        }

        observations.add(observationsDataSet);

    }

}
