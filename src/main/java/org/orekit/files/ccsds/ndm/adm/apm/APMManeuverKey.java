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
package org.orekit.files.ccsds.ndm.adm.apm;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;

/** Keys for {@link APMManeuver APM maneuver} entries.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum APMManeuverKey {

    /** Block wrapping element in XML files. */
    maneuverParameters((token, context, data) -> true),

    /** Comment entry. */
    COMMENT((token, context, data) -> {
        if (token.getType() == TokenType.ENTRY) {
            return data.addComment(token.getContent());
        }
        return true;
    }),

    /** Epoch start entry. */
    MAN_EPOCH_START((token, context, data) -> {
        token.processAsDate(data::setEpochStart, context);
        return true;
    }),

    /** Duration entry. */
    MAN_DURATION((token, context, data) -> {
        token.processAsDouble(data::setDuration);
        return true;
    }),

    /** Reference frame entry. */
    MAN_REF_FRAME((token, context, data) -> {
        token.processAsNormalizedString(data::setRefFrameString);
        return true;
    }),

    /** First torque vector component entry. */
    MAN_TOR_1((token, context, data) -> {
        data.setTorque(new Vector3D(token.getContentAsDouble(),
                                        data.getTorque().getY(),
                                        data.getTorque().getZ()));
        return true;
    }),

    /** Second torque vector component entry. */
    MAN_TOR_2((token, context, data) -> {
        data.setTorque(new Vector3D(data.getTorque().getX(),
                                        token.getContentAsDouble(),
                                        data.getTorque().getZ()));
        return true;
    }),

    /** Third torque vector component entry. */
    MAN_TOR_3((token, context, data) -> {
        data.setTorque(new Vector3D(data.getTorque().getX(),
                                        data.getTorque().getY(),
                                        token.getContentAsDouble()));
        return true;
    });

    /** Processing method. */
    private final ManeuverEntryProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    APMManeuverKey(final ManeuverEntryProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context parsing context
     * @param data data to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final APMManeuver data) {
        return processor.process(token, context, data);
    }

    /** Interface for processing one token. */
    interface ManeuverEntryProcessor {
        /** Process one token.
         * @param token token to process
         * @param context parsing context
         * @param data data to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ParsingContext context, APMManeuver data);
    }

}
