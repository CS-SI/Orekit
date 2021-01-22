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
import org.orekit.files.ccsds.ndm.ParsingContext;
import org.orekit.files.ccsds.ndm.adm.LocalSpacecraftBodyFrame;
import org.orekit.files.ccsds.utils.lexical.TokenType;
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
    COMMENT((token, context, maneuver) -> {
        if (token.getType() == TokenType.ENTRY) {
            if (maneuver.getEpochStart() == null) {
                // we are still at block start, we accept comments
                token.processAsFreeTextString(maneuver::addComment);
                return false;
            } else {
                // we have already processed some content in the block
                // the comment belongs to the next block
                return false;
            }
        }
        return true;
    }),

    /** Epoch start entry. */
    MAN_EPOCH_START((token, context, maneuver) -> {
        token.processAsDate(maneuver::setEpochStart, context);
        return true;
    }),

    /** Duration entry. */
    MAN_DURATION((token, context, maneuver) -> {
        token.processAsDouble(maneuver::setDuration);
        return true;
    }),

    /** Reference frame entry. */
    MAN_REF_FRAME((token, context, maneuver) -> {
        final String[] fields = token.getNormalizedContent().split(" ");
        if (fields.length != 2) {
            throw token.generateException();
        }
        try {
            final LocalSpacecraftBodyFrame.Type type = LocalSpacecraftBodyFrame.Type.valueOf(fields[0]);
            maneuver.setRefFrame(new LocalSpacecraftBodyFrame(type, fields[1]));
            return true;
        } catch (IllegalArgumentException iae) {
            throw token.generateException();
        }
    }),

    /** First torque vector component entry. */
    MAN_TOR_1((token, context, maneuver) -> {
        maneuver.setTorque(new Vector3D(token.getContentAsDouble(),
                                        maneuver.getTorque().getY(),
                                        maneuver.getTorque().getZ()));
        return true;
    }),

    /** Second torque vector component entry. */
    MAN_TOR_2((token, context, maneuver) -> {
        maneuver.setTorque(new Vector3D(maneuver.getTorque().getX(),
                                        token.getContentAsDouble(),
                                        maneuver.getTorque().getZ()));
        return true;
    }),

    /** Third torque vector component entry. */
    MAN_TOR_3((token, context, maneuver) -> {
        maneuver.setTorque(new Vector3D(maneuver.getTorque().getX(),
                                        maneuver.getTorque().getY(),
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
     * @param maneuver maneuver to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final APMManeuver maneuver) {
        return processor.process(token, context, maneuver);
    }

    /** Interface for processing one token. */
    interface ManeuverEntryProcessor {
        /** Process one token.
         * @param token token to process
         * @param context parsing context
         * @param maneuver maneuver to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ParsingContext context, APMManeuver maneuver);
    }

}
