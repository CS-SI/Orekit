/* Copyright 2002-2022 CS GROUP
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

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;

/** Keys for {@link ApmQuaternion APM quaternion} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public enum ApmQuaternionKey {

    /** Quaternion wrapping element in XML files. */
    quaternion((token, context, container) -> true),

    /** Quaternion wrapping element in XML files. */
    quaternionRate((token, context, container) -> true),

    /** Epoch entry. */
    EPOCH((token, context, container) -> token.processAsDate(container::setEpoch, context)),

    /** First reference frame entry. */
    Q_FRAME_A((token, context, container) -> token.processAsFrame(container.getEndpoints()::setFrameA, context, true, true, true)),

    /** Second reference frame entry. */
    Q_FRAME_B((token, context, container) -> token.processAsFrame(container.getEndpoints()::setFrameB, context, true, true, true)),

    /** Rotation direction entry. */
    Q_DIR((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.getEndpoints().setA2b(token.getContentAsUppercaseCharacter() == 'A');
        }
        return true;
    }),

    /** Scalar part of the quaternion entry. */
    QC((token, context, container) -> token.processAsIndexedDouble(0, Unit.ONE, context.getParsedUnitsBehavior(),
                                                                   container::setQ)),

    /** First component of the vector part of the quaternion entry. */
    Q1((token, context, container) -> token.processAsIndexedDouble(1, Unit.ONE, context.getParsedUnitsBehavior(),
                                                                   container::setQ)),

    /** Second component of the vector part of the quaternion entry. */
    Q2((token, context, container) -> token.processAsIndexedDouble(2, Unit.ONE, context.getParsedUnitsBehavior(),
                                                                   container::setQ)),

    /** Third component of the vector part of the quaternion entry. */
    Q3((token, context, container) -> token.processAsIndexedDouble(3, Unit.ONE, context.getParsedUnitsBehavior(),
                                                                   container::setQ)),

    /** Scalar part of the quaternion derivative entry. */
    QC_DOT((token, context, container) -> token.processAsIndexedDouble(0, Units.ONE_PER_S, context.getParsedUnitsBehavior(),
                                                                       container::setQDot)),

    /** First component of the vector part of the quaternion derivative entry. */
    Q1_DOT((token, context, container) -> token.processAsIndexedDouble(1, Units.ONE_PER_S, context.getParsedUnitsBehavior(),
                                                                       container::setQDot)),

    /** Second component of the vector part of the quaternion derivative entry. */
    Q2_DOT((token, context, container) -> token.processAsIndexedDouble(2, Units.ONE_PER_S, context.getParsedUnitsBehavior(),
                                                                       container::setQDot)),

    /** Third component of the vector part of the quaternion derivative entry. */
    Q3_DOT((token, context, container) -> token.processAsIndexedDouble(3, Units.ONE_PER_S, context.getParsedUnitsBehavior(),
                                                                       container::setQDot));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    ApmQuaternionKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final ApmQuaternion container) {
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
        boolean process(ParseToken token, ContextBinding context, ApmQuaternion container);
    }

}
