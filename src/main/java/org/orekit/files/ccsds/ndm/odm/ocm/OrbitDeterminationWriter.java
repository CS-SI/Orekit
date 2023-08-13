/* Copyright 2002-2023 CS GROUP
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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.io.IOException;

import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/** Writer for orbit determination data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class OrbitDeterminationWriter extends AbstractWriter {

    /** Orbit determination block. */
    private final OrbitDetermination od;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param orbitDetermination orbit determination to write
     * @param timeConverter converter for dates
     */
    OrbitDeterminationWriter(final OrbitDetermination orbitDetermination,
                             final TimeConverter timeConverter) {
        super(OcmDataSubStructureKey.od.name(), OcmDataSubStructureKey.OD.name());
        this.od            = orbitDetermination;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // orbit determination block
        generator.writeComments(od.getComments());

        // identifiers
        generator.writeEntry(OrbitDeterminationKey.OD_ID.name(),      od.getId(),     null, false);
        generator.writeEntry(OrbitDeterminationKey.OD_PREV_ID.name(), od.getPrevId(), null, false);
        if (od.getMethod() != null) {
            final StringBuilder builder = new StringBuilder();
            builder.append(od.getMethod().getName());
            if (od.getMethod().getTool() != null) {
                builder.append(':');
                builder.append(od.getMethod().getTool());
            }
            generator.writeEntry(OrbitDeterminationKey.OD_METHOD.name(),  builder.toString(), null, false);
        }

        // time
        generator.writeEntry(OrbitDeterminationKey.OD_EPOCH.name(),             timeConverter, od.getEpoch(),                false, false);
        generator.writeEntry(OrbitDeterminationKey.DAYS_SINCE_FIRST_OBS.name(), od.getTimeSinceFirstObservation(), Unit.DAY, false);
        generator.writeEntry(OrbitDeterminationKey.DAYS_SINCE_LAST_OBS.name(),  od.getTimeSinceLastObservation(), Unit.DAY,  false);
        generator.writeEntry(OrbitDeterminationKey.RECOMMENDED_OD_SPAN.name(),  od.getRecommendedOdSpan(), Unit.DAY,         false);
        generator.writeEntry(OrbitDeterminationKey.ACTUAL_OD_SPAN.name(),       od.getActualOdSpan(), Unit.DAY,              false);

        // counters
        generator.writeEntry(OrbitDeterminationKey.OBS_AVAILABLE.name(),    od.getObsAvailable(),            false);
        generator.writeEntry(OrbitDeterminationKey.OBS_USED.name(),         od.getObsUsed(),                 false);
        generator.writeEntry(OrbitDeterminationKey.TRACKS_AVAILABLE.name(), od.getTracksAvailable(),         false);
        generator.writeEntry(OrbitDeterminationKey.TRACKS_USED.name(),      od.getTracksUsed(),              false);
        generator.writeEntry(OrbitDeterminationKey.MAXIMUM_OBS_GAP.name(),  od.getMaximumObsGap(), Unit.DAY, false);

        // errors
        generator.writeEntry(OrbitDeterminationKey.OD_EPOCH_EIGMAJ.name(),    od.getEpochEigenMaj(), Unit.METRE,        false);
        generator.writeEntry(OrbitDeterminationKey.OD_EPOCH_EIGINT.name(),    od.getEpochEigenInt(), Unit.METRE,        false);
        generator.writeEntry(OrbitDeterminationKey.OD_EPOCH_EIGMIN.name(),    od.getEpochEigenMin(), Unit.METRE,        false);
        generator.writeEntry(OrbitDeterminationKey.OD_MAX_PRED_EIGMAJ.name(), od.getMaxPredictedEigenMaj(), Unit.METRE, false);
        generator.writeEntry(OrbitDeterminationKey.OD_MIN_PRED_EIGMIN.name(), od.getMinPredictedEigenMin(), Unit.METRE, false);
        generator.writeEntry(OrbitDeterminationKey.OD_CONFIDENCE.name(),      od.getConfidence(), Unit.PERCENT,         false);
        generator.writeEntry(OrbitDeterminationKey.GDOP.name(),               od.getGdop(), Unit.ONE,                   false);

        // parameters
        generator.writeEntry(OrbitDeterminationKey.SOLVE_N.name(),         od.getSolveN(),               false);
        generator.writeEntry(OrbitDeterminationKey.SOLVE_STATES.name(),    od.getSolveStates(),          false);
        generator.writeEntry(OrbitDeterminationKey.CONSIDER_N.name(),      od.getConsiderN(),            false);
        generator.writeEntry(OrbitDeterminationKey.CONSIDER_PARAMS.name(), od.getConsiderParameters(),   false);
        generator.writeEntry(OrbitDeterminationKey.SEDR.name(),            od.getSedr(), Units.W_PER_KG, false);
        generator.writeEntry(OrbitDeterminationKey.SENSORS_N.name(),       od.getSensorsN(),             false);
        generator.writeEntry(OrbitDeterminationKey.SENSORS.name(),         od.getSensors(),              false);

        // observations
        generator.writeEntry(OrbitDeterminationKey.WEIGHTED_RMS.name(),  od.getWeightedRms(), Unit.ONE, false);
        generator.writeEntry(OrbitDeterminationKey.DATA_TYPES.name(),    od.getDataTypes(),             false);

    }

}
