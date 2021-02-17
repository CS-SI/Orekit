/* Copyright 2002-2021 CS GROUP
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

import org.hipparchus.util.FastMath;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.orbits.PositionAngle;


/** Keys for {@link KeplerianElements Keplerian elements} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum KeplerianElementsKey {

    /** Comment entry. */
    COMMENT((token, context, data) ->
            token.getType() == TokenType.ENTRY ? data.addComment(token.getContent()) : true),

    /** Epoch of Keplerian elements. */
    EPOCH((token, context, data) -> token.processAsDate(data::setEpoch, context)),

    /** Orbit semi-major axis. */
    SEMI_MAJOR_AXIS((token, context, data) -> token.processAsDouble(1.0e3, data::setA)),

    /** Mean motion. */
    MEAN_MOTION((token, context, data) ->
                 token.processAsDouble(FastMath.PI / 43200.0, data::setMeanMotion)),

    /** Orbit eccentricity. */
    ECCENTRICITY((token, context, data) -> token.processAsDouble(1.0, data::setE)),

    /** Orbit inclination. */
    INCLINATION((token, context, data) -> token.processAsAngle(data::setI)),

    /** Orbit right ascension of ascending node. */
    RA_OF_ASC_NODE((token, context, data) -> token.processAsAngle(data::setRaan)),

    /** Orbit argument of pericenter. */
    ARG_OF_PERICENTER((token, context, data) -> token.processAsAngle(data::setPa)),

    /** Orbit true anomaly. */
    TRUE_ANOMALY((token, context, data) -> {
        if (token.getType() == TokenType.ENTRY) {
            data.setAnomaly(token.getContentAsAngle());
            data.setAnomalyType(PositionAngle.TRUE);
        }
        return true;
    }),

    /** Orbit mean anomaly. */
    MEAN_ANOMALY((token, context, data) -> {
        if (token.getType() == TokenType.ENTRY) {
            data.setAnomaly(token.getContentAsAngle());
            data.setAnomalyType(PositionAngle.MEAN);
        }
        return true;
    }),

    /** Gravitational coefficient. */
    GM((token, context, data) -> token.processAsDouble(1.0e9, data::setMu));

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
     * @param context parsing context
     * @param data data to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final KeplerianElements data) {
        return processor.process(token, context, data);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context parsing context
         * @param data data to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ParsingContext context, KeplerianElements data);
    }

}
