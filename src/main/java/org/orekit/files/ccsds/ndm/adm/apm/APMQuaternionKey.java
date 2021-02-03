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

import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

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
    COMMENT((token, context, data) ->
            token.getType() == TokenType.ENTRY ? data.addComment(token.getContent()) : true),

    /** Epoch entry. */
    EPOCH((token, context, data) -> token.processAsDate(data::setEpoch, context)),

    /** First reference frame entry. */
    Q_FRAME_A((token, context, data) -> token.processAsNormalizedString(data.getEndPoints()::setFrameA)),

    /** Second reference frame entry. */
    Q_FRAME_B((token, context, data) -> token.processAsNormalizedString(data.getEndPoints()::setFrameB)),

    /** Rotation direction entry. */
    Q_DIR((token, context, data) -> token.processAsNormalizedString(data.getEndPoints()::setDirection)),

    /** Scalar part of the quaternion entry. */
    QC((token, context, data) -> token.processAsIndexedDouble(data::setQ, 0)),

    /** First component of the vector part of the quaternion entry. */
    Q1((token, context, data) -> token.processAsIndexedDouble(data::setQ, 1)),

    /** Second component of the vector part of the quaternion entry. */
    Q2((token, context, data) -> token.processAsIndexedDouble(data::setQ, 2)),

    /** Third component of the vector part of the quaternion entry. */
    Q3((token, context, data) -> token.processAsIndexedDouble(data::setQ, 3)),

    /** Scalar part of the quaternion derivative entry. */
    QC_DOT((token, context, data) -> token.processAsIndexedDouble(data::setQDot, 0)),

    /** First component of the vector part of the quaternion derivative entry. */
    Q1_DOT((token, context, data) -> token.processAsIndexedDouble(data::setQDot, 1)),

    /** Second component of the vector part of the quaternion derivative entry. */
    Q2_DOT((token, context, data) -> token.processAsIndexedDouble(data::setQDot, 2)),

    /** Third component of the vector part of the quaternion derivative entry. */
    Q3_DOT((token, context, data) -> token.processAsIndexedDouble(data::setQDot, 3));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    APMQuaternionKey(final TokenProcessor processor) {
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
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context parsing context
         * @param data data to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ParsingContext context, APMQuaternion data);
    }

}
