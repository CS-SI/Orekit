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
package org.orekit.files.ccsds.ndm.odm;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.utils.units.Unit;


/** Keys for {@link KeplerianElements Keplerian elements} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum KeplerianElementsKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Epoch of Keplerian elements. */
    EPOCH((token, context, container) -> token.processAsDate(container::setEpoch, context)),

    /** Orbit semi-major axis. */
    SEMI_MAJOR_AXIS((token, context, container) -> token.processAsDouble(Unit.KILOMETRE, context.getParsedUnitsBehavior(), container::setA)),

    /** Mean motion. */
    MEAN_MOTION((token, context, container) -> token.processAsDouble(Units.REV_PER_DAY, context.getParsedUnitsBehavior(), container::setMeanMotion)),

    /** Orbit eccentricity. */
    ECCENTRICITY((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(), container::setE)),

    /** Orbit inclination. */
    INCLINATION((token, context, container) -> token.processAsDouble(Unit.DEGREE, context.getParsedUnitsBehavior(), container::setI)),

    /** Orbit right ascension of ascending node. */
    RA_OF_ASC_NODE((token, context, container) -> token.processAsDouble(Unit.DEGREE, context.getParsedUnitsBehavior(), container::setRaan)),

    /** Orbit argument of pericenter. */
    ARG_OF_PERICENTER((token, context, container) -> token.processAsDouble(Unit.DEGREE, context.getParsedUnitsBehavior(), container::setPa)),

    /** Orbit true anomaly. */
    TRUE_ANOMALY((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            final double angle = context.
                                 getParsedUnitsBehavior().
                                 select(token.getUnits(), Unit.DEGREE).
                                 toSI(token.getContentAsDouble());
            container.setAnomaly(angle);
            container.setAnomalyType(PositionAngleType.TRUE);
        }
        return true;
    }),

    /** Orbit mean anomaly. */
    MEAN_ANOMALY((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            final double angle = context.
                                 getParsedUnitsBehavior().
                                 select(token.getUnits(), Unit.DEGREE).
                                 toSI(token.getContentAsDouble());
            container.setAnomaly(angle);
            container.setAnomalyType(PositionAngleType.MEAN);
        }
        return true;
    }),

    /** Gravitational coefficient. */
    GM((token, context, container) -> token.processAsDouble(Units.KM3_PER_S2, context.getParsedUnitsBehavior(), container::setMu));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    KeplerianElementsKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final KeplerianElements container) {
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
        boolean process(ParseToken token, ContextBinding context, KeplerianElements container);
    }

}
