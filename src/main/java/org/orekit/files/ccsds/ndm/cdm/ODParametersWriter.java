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
package org.orekit.files.ccsds.ndm.cdm;

import java.io.IOException;

import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/**
 * Writer for OD parameters data block for CCSDS Conjunction Data Messages.
 *
 * @author Melina Vanel
 * @since 11.2
 */
public class ODParametersWriter extends AbstractWriter {

    /** OD parameters block. */
    private final ODParameters ODparameters;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param ODParameters OD parameters data to write
     * @param timeConverter converter for dates
     */
    ODParametersWriter(final String xmlTag, final String kvnTag,
                       final ODParameters ODParameters, final TimeConverter timeConverter) {
        super(xmlTag, kvnTag);
        this.ODparameters  = ODParameters;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(ODparameters.getComments());
        // OD parameters
        generator.writeEntry(ODParametersKey.TIME_LASTOB_START.name(),   timeConverter, ODparameters.getTimeLastObsStart(),  true, false);
        generator.writeEntry(ODParametersKey.TIME_LASTOB_END.name(),     timeConverter, ODparameters.getTimeLastObsEnd(),    true, false);
        generator.writeEntry(ODParametersKey.RECOMMENDED_OD_SPAN.name(), ODparameters.getRecommendedOdSpan(), Unit.DAY,      false);
        generator.writeEntry(ODParametersKey.ACTUAL_OD_SPAN.name(),      ODparameters.getActualOdSpan(),      Unit.DAY,      false);
        generator.writeEntry(ODParametersKey.OBS_AVAILABLE.name(),       ODparameters.getObsAvailable(),                     false);
        generator.writeEntry(ODParametersKey.OBS_USED.name(),            ODparameters.getObsUsed(),                          false);
        generator.writeEntry(ODParametersKey.TRACKS_AVAILABLE.name(),    ODparameters.getTracksAvailable(),                  false);
        generator.writeEntry(ODParametersKey.TRACKS_USED.name(),         ODparameters.getTracksUsed(),                       false);
        generator.writeEntry(ODParametersKey.RESIDUALS_ACCEPTED.name(),  ODparameters.getResidualsAccepted(), Unit.PERCENT,  false);
        generator.writeEntry(ODParametersKey.WEIGHTED_RMS.name(),        ODparameters.getWeightedRMS(),       Unit.ONE,      false);

    }

}
