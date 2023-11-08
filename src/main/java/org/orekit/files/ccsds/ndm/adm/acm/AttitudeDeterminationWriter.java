/* Copyright 2023 Luc Maisonobe
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

package org.orekit.files.ccsds.ndm.adm.acm;

import java.io.IOException;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.units.Unit;

/** Writer for attitude determination data.
 * @author Luc Maisonobe
 * @since 12.0
 */
class AttitudeDeterminationWriter extends AbstractWriter {

    /** Attitude determination block. */
    private final AttitudeDetermination ad;

    /** Create a writer.
     * @param attitudeDetermination attitude determination to write
     */
    AttitudeDeterminationWriter(final AttitudeDetermination attitudeDetermination) {
        super(AcmDataSubStructureKey.ad.name(), AcmDataSubStructureKey.AD.name());
        this.ad = attitudeDetermination;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // attitude determination block
        generator.writeComments(ad.getComments());

        // identifiers
        generator.writeEntry(AttitudeDeterminationKey.AD_ID.name(),      ad.getId(),            null, false);
        generator.writeEntry(AttitudeDeterminationKey.AD_PREV_ID.name(), ad.getPrevId(),        null, false);
        if (ad.getMethod() != null) {
            generator.writeEntry(AttitudeDeterminationKey.AD_METHOD.name(), ad.getMethod().name(), null, false);
        }

        generator.writeEntry(AttitudeDeterminationKey.ATTITUDE_SOURCE.name(), ad.getSource(), null, false);

        // parameters
        generator.writeEntry(AttitudeDeterminationKey.EULER_ROT_SEQ.name(),             ad.getEulerRotSeq(),    false);
        generator.writeEntry(AttitudeDeterminationKey.NUMBER_STATES.name(),             ad.getNbStates(),       false);
        generator.writeEntry(AttitudeDeterminationKey.ATTITUDE_STATES.name(),           ad.getAttitudeStates(), true);
        generator.writeEntry(AttitudeDeterminationKey.COV_TYPE.name(),                  ad.getCovarianceType(), false);
        generator.writeEntry(AttitudeDeterminationKey.REF_FRAME_A.name(),               ad.getEndpoints().getFrameA().getName(), null, false);
        generator.writeEntry(AttitudeDeterminationKey.REF_FRAME_B.name(),               ad.getEndpoints().getFrameB().getName(), null, false);
        generator.writeEntry(AttitudeDeterminationKey.RATE_STATES.name(),               ad.getRateStates(),                  false);
        generator.writeEntry(AttitudeDeterminationKey.SIGMA_U.name(),                   ad.getSigmaU(), Units.DEG_PER_S_3_2, false);
        generator.writeEntry(AttitudeDeterminationKey.SIGMA_V.name(),                   ad.getSigmaV(), Units.DEG_PER_S_1_2, false);
        generator.writeEntry(AttitudeDeterminationKey.RATE_PROCESS_NOISE_STDDEV.name(), ad.getRateProcessNoiseStdDev(), Units.DEG_PER_S_3_2, false);

        // sensors
        for (final AttitudeDeterminationSensor sensor : ad.getSensorsUsed()) {
            generator.enterSection(AttitudeDeterminationKey.SENSOR.name());
            generator.writeEntry(AttitudeDeterminationSensorKey.SENSOR_NUMBER.name(), sensor.getSensorNumber(),   true);
            generator.writeEntry(AttitudeDeterminationSensorKey.SENSOR_USED.name(), sensor.getSensorUsed(), null, true);
            final double[] stddevDouble = sensor.getSensorNoiseCovariance();
            if (stddevDouble != null) {
                generator.writeEntry(AttitudeDeterminationSensorKey.NUMBER_SENSOR_NOISE_COVARIANCE.name(), stddevDouble.length, true);
                final StringBuilder stddev = new StringBuilder();
                for (int k = 0; k < stddevDouble.length; ++k) {
                    if (k > 0) {
                        stddev.append(' ');
                    }
                    stddev.append(AccurateFormatter.format(Unit.DEGREE.fromSI(stddevDouble[k])));
                }
                generator.writeEntry(AttitudeDeterminationSensorKey.SENSOR_NOISE_STDDEV.name(),
                                     stddev.toString(), Unit.DEGREE, false);
            }
            generator.writeEntry(AttitudeDeterminationSensorKey.SENSOR_FREQUENCY.name(), sensor.getSensorFrequency(), Unit.HERTZ, false);
            generator.exitSection();
        }

    }

}
