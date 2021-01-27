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

import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;

/** Keys for {@link APMQuaternion APM quaternion} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public enum APMQuaternionKey {

    /** Block wrapping element in XML files. */
    quaternionState((token, context, data) -> true),

    /** Quaternion wrapping element in XML files. */
    quaternion((token, context, data) -> true),

    /** Quaternion wrapping element in XML files. */
    quaternionRate((token, context, data) -> true),

    /** Comment entry. */
    COMMENT((token, context, data) -> {
        if (token.getType() == TokenType.ENTRY) {
            if (data.getEpoch() == null) {
                // we are still at block start, we accept comments
                token.processAsFreeTextString(data::addComment);
                return true;
            } else {
                // we have already processed some content in the block
                // the comment belongs to the next block
                return false;
            }
        } return true;
    }),

    /** Epoch entry. */
    EPOCH((token, context, data) -> {
        token.processAsDate(data::setEpoch, context);
        return true;
    }),

    /** First reference frame entry. */
    Q_FRAME_A((token, context, data) -> {
        token.processAsNormalizedString(data.getEndPoints()::setFrameA);
        return true;
    }),

    /** Second reference frame entry. */
    Q_FRAME_B((token, context, data) -> {
        token.processAsNormalizedString(data.getEndPoints()::setFrameB);
        return true;
    }),

    /** Rotation direction entry. */
    Q_DIR((token, context, data) -> {
        token.processAsNormalizedString(data.getEndPoints()::setDirection);
        return true;
    }),

    /** Scalar part of the quaternion entry. */
    QC((token, context, data) -> {
        token.processAsDouble(data::setQ0);
        return true;
    }),

    /** First component of the vector part of the quaternion entry. */
    Q1((token, context, data) -> {
        token.processAsDouble(data::setQ1);
        return true;
    }),

    /** Second component of the vector part of the quaternion entry. */
    Q2((token, context, data) -> {
        token.processAsDouble(data::setQ2);
        return true;
    }),

    /** Third component of the vector part of the quaternion entry. */
    Q3((token, context, data) -> {
        token.processAsDouble(data::setQ3);
        return true;
    }),

    /** Scalar part of the quaternion derivative entry. */
    QC_DOT((token, context, data) -> {
        token.processAsDouble(data::setQ0Dot);
        return true;
    }),

    /** First component of the vector part of the quaternion derivative entry. */
    Q1_DOT((token, context, data) -> {
        token.processAsDouble(data::setQ1Dot);
        return true;
    }),

    /** Second component of the vector part of the quaternion derivative entry. */
    Q2_DOT((token, context, data) -> {
        token.processAsDouble(data::setQ2Dot);
        return true;
    }),

    /** Third component of the vector part of the quaternion derivative entry. */
    Q3_DOT((token, context, data) -> {
        token.processAsDouble(data::setQ3Dot);
        return true;
    });

    /** Processing method. */
    private final QuaternionEntryProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    APMQuaternionKey(final QuaternionEntryProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context parsing context
     * @param data data to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final APMQuaternion data) {
        return processor.process(token, context, data);
    }

    /** Interface for processing one token. */
    interface QuaternionEntryProcessor {
        /** Process one token.
         * @param token token to process
         * @param context parsing context
         * @param data data to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ParsingContext context, APMQuaternion data);
    }

}
