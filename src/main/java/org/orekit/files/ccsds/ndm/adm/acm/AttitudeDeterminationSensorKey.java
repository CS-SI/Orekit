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

import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.utils.units.Unit;


/** Keys for {@link AttitudeDetermination attitude determination data} sensor entries.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum AttitudeDeterminationSensorKey {

    /** Sensor number. */
    SENSOR_NUMBER((token, context, container) -> token.processAsInteger(container::setSensorNumber)),

    /** Sensor used. */
    SENSOR_USED((token, context, container) -> token.processAsUppercaseString(container::setSensorUsed)),

    /** Number of noise elements. */
    NUMBER_SENSOR_NOISE_COVARIANCE((token, context, container) -> token.processAsInteger(container::setNbSensorNoiseCovariance)),

    /** Standard deviation of sensor noises. */
    SENSOR_NOISE_STDDEV((token, context, container) -> token.processAsDoubleArray(Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                                  container::setSensorNoiseCovariance)),

    /** Frequency of sensor data. */
    SENSOR_FREQUENCY((token, context, container) -> token.processAsDouble(Unit.HERTZ, context.getParsedUnitsBehavior(),
                                                                          container::setSensorFrequency));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    AttitudeDeterminationSensorKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context,
                           final AttitudeDeterminationSensor container) {
        return processor.process(token, context, container);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context context binding
         * @param container container to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ContextBinding context, AttitudeDeterminationSensor container);
    }

}
