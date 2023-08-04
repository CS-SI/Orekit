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
package org.orekit.files.ccsds.ndm.adm.aem;

import org.orekit.files.ccsds.ndm.adm.AdmMetadataKey;
import org.orekit.files.ccsds.ndm.adm.AttitudeType;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;


/** Keys for {@link AemMetadata AEM container} entries.
 * <p>
 * Additional container are also listed in {@link AdmMetadataKey}.
 * </p>
 * @author Bryan Cazabonne
 * @since 11.0
 */
public enum AemMetadataKey {

    /** First reference frame. */
    REF_FRAME_A((token, context, container) -> token.processAsFrame(container.getEndpoints()::setFrameA, context, true, true, true)),

    /** Second reference frame. */
    REF_FRAME_B((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.checkNotNull(container.getEndpoints().getFrameA(), REF_FRAME_A.name());
            final boolean aIsSpaceraftBody = container.getEndpoints().getFrameA().asSpacecraftBodyFrame() != null;
            return token.processAsFrame(container.getEndpoints()::setFrameB, context,
                                        aIsSpaceraftBody, aIsSpaceraftBody, !aIsSpaceraftBody);
        }
        return true;
    }),

    /** Rotation direction entry. */
    ATTITUDE_DIR((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.getEndpoints().setA2b(token.getContentAsUppercaseCharacter() == 'A');
        }
        return true;
    }),

    /** Start time entry. */
    START_TIME((token, context, container) -> token.processAsDate(container::setStartTime, context)),

    /** Stop time entry. */
    STOP_TIME((token, context, container) -> token.processAsDate(container::setStopTime, context)),

    /** Useable start time entry. */
    USEABLE_START_TIME((token, context, container) -> token.processAsDate(container::setUseableStartTime, context)),

    /** Useable stop time entry. */
    USEABLE_STOP_TIME((token, context, container) -> token.processAsDate(container::setUseableStopTime, context)),

    /** Format of the data line entry. */
    ATTITUDE_TYPE((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                container.setAttitudeType(AttitudeType.parseType(token.getRawContent()));
                return true;
            } catch (IllegalArgumentException iae) {
                return false;
            }
        }
        return true;
    }),

    /** Placement of the scalar component in quaternion entry. */
    QUATERNION_TYPE((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.setIsFirst("FIRST".equals(token.getContentAsUppercaseString()));
        }
        return true;
    }),

    /** Rotation order entry for Euler angles. */
    EULER_ROT_SEQ((token, context, container) -> token.processAsRotationOrder(container::setEulerRotSeq)),

    /** Reference frame for Euler rates. (only for ADM V1) */
    RATE_FRAME((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            final String content = token.getContentAsUppercaseString();
            final char   suffix  = content.charAt(content.length() - 1);
            container.setRateFrameIsA(suffix == 'A');
        }
        return true;
    }),

    /** Reference frame for angular velocities.
     * @since 12.0
     */
    ANGVEL_FRAME((token, context, container) -> token.processAsFrame(container::setAngvelFrame, context, true, true, true)),

    /** Interpolation method in ephemeris. */
    INTERPOLATION_METHOD((token, context, container) -> token.processAsUppercaseString(container::setInterpolationMethod)),

    /** Interpolation degree in ephemeris. */
    INTERPOLATION_DEGREE((token, context, container) -> token.processAsInteger(container::setInterpolationDegree));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    AemMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final AemMetadata container) {
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
        boolean process(ParseToken token, ContextBinding context, AemMetadata container);
    }

}
