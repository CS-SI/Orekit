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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;


/** Keys for {@link AttitudeManeuver attitude maneuver} entries.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum AttitudeManeuverKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Maneuver identification number. */
    MAN_ID((token, context, container) -> token.processAsFreeTextString(container::setID)),

    /** Identification number of previous maneuver. */
    MAN_PREV_ID((token, context, container) -> token.processAsFreeTextString(container::setPrevID)),

    /** Purpose of the maneuver. */
    MAN_PURPOSE((token, context, container) -> token.processAsFreeTextString(container::setManPurpose)),

    /** Start time of actual maneuver, relative to t₀. */
    MAN_BEGIN_TIME((token, context, container) -> token.processAsDouble(Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                        container::setBeginTime)),

    /** End time of actual maneuver, relative to t₀. */
    MAN_END_TIME((token, context, container) -> token.processAsDouble(Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                      container::setEndTime)),

    /** Duration. */
    MAN_DURATION((token, context, container) -> token.processAsDouble(Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                      container::setDuration)),

    /** Actuator used. */
    ACTUATOR_USED((token, context, container) -> token.processAsFreeTextString(container::setActuatorUsed)),

    /** Target momentum. */
    TARGET_MOMENTUM((token, context, container) -> token.processAsVector(Units.N_M_S, context.getParsedUnitsBehavior(),
                                                                         container::setTargetMomentum)),

    /** Target momentum frame. */
    TARGET_MOM_FRAME((token, context, container) -> token.processAsFrame(container::setTargetMomFrame, context, true, true, true)),

    /** Target attitude. */
    TARGET_ATTITUDE((token, context, container) -> {
        try {
            if (token.getType() == TokenType.ENTRY) {
                final String[] fields = token.getRawContent().split("\\p{Space}+");
                if (fields.length == 4) {
                    container.setTargetAttitude(new Rotation(Double.parseDouble(fields[3]),
                                                             Double.parseDouble(fields[0]),
                                                             Double.parseDouble(fields[1]),
                                                             Double.parseDouble(fields[2]),
                                                             true));
                    return true;
                }
            } else {
                return true;
            }
        } catch (NumberFormatException nfe) {
            // ignored, error handled below, together with wrong number of fields
        }
        throw token.generateException(null);
    }),

    /** Target spin rate. */
    TARGET_SPINRATE((token, context, container) -> token.processAsDouble(Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                          container::setTargetSpinRate));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    AttitudeManeuverKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param data data to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final AttitudeManeuver data) {
        return processor.process(token, context, data);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context context binding
         * @param data data to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ContextBinding context, AttitudeManeuver data);
    }

}
