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
package org.orekit.files.ccsds.ndm.adm.aem;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.files.ccsds.ndm.adm.ADMMetadataKey;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;


/** Keys for {@link AEMMetadata AEM metadata} entries.
 * <p>
 * Additional metadata are also listed in {@link ADMMetadataKey}.
 * </p>
 * @author Bryan Cazabonne
 * @since 11.0
 */
public enum AEMMetadataKey {

    /** First reference frame. */
    REF_FRAME_A((token, context, metadata) -> token.processAsNormalizedString(metadata::setRefFrameAString)),

    /** Second reference frame. */
    REF_FRAME_B((token, context, metadata) -> token.processAsNormalizedString(metadata::setRefFrameBString)),

    /** Rotation direction entry. */
    ATTITUDE_DIR((token, context, metadata) -> token.processAsNormalizedString(metadata::setAttitudeDirection)),

    /** Start time entry. */
    START_TIME((token, context, metadata) -> token.processAsDate(metadata::setStartTime, context)),

    /** Stop time entry. */
    STOP_TIME((token, context, metadata) -> token.processAsDate(metadata::setStopTime, context)),

    /** Useable start time entry. */
    USEABLE_START_TIME((token, context, metadata) -> token.processAsDate(metadata::setUseableStartTime, context)),

    /** Useable stop time entry. */
    USEABLE_STOP_TIME((token, context, metadata) -> token.processAsDate(metadata::setUseableStopTime, context)),

    /** Format of the data line entry. */
    ATTITUDE_TYPE((token, context, metadata) -> token.processAsNormalizedString(metadata::setAttitudeType)),

    /** Placement of the scalar component in quaternion entry. */
    QUATERNION_TYPE((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            metadata.setIsFirst("FIRST".equals(token.getNormalizedContent()));
        }
    }),

    /** Rotation order entry for Euler angles. */
    EULER_ROT_SEQ((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                metadata.setRotationOrder(RotationOrder.valueOf(token.getNormalizedContent().
                                                                replace('1', 'X').
                                                                replace('2', 'Y').
                                                                replace('3', 'Z')));
            } catch (IllegalArgumentException iae) {
                throw token.generateException(iae);
            }
        }
    }),

    /** Reference frame for Euler rates. */
    RATE_FRAME((token, context, metadata) -> token.processAsNormalizedString(metadata::setRateFrameString)),

    /** Interpolation method in ephemeris. */
    INTERPOLATION_METHOD((token, context, metadata) -> token.processAsNormalizedString(metadata::setInterpolationMethod)),

    /** Interpolation degree in ephemeris. */
    INTERPOLATION_DEGREE((token, context, metadata) -> token.processAsInteger(metadata::setInterpolationDegree));

    /** Processing method. */
    private final MetadataEntryProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    AEMMetadataKey(final MetadataEntryProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context parsing context
     * @param metadata metadata to fill
     */
    public void process(final ParseToken token, final ParsingContext context, final AEMMetadata metadata) {
        processor.process(token, context, metadata);
    }

    /** Interface for processing one token. */
    interface MetadataEntryProcessor {
        /** Process one token.
         * @param token token to process
         * @param context parsing context
         * @param metadata metadata to fill
         */
        void process(ParseToken token, ParsingContext context, AEMMetadata metadata);
    }

}
