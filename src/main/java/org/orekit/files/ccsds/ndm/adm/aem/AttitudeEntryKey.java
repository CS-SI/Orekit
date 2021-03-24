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

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.utils.units.Unit;


/** Keys for {@link AttitudeEntry attitude entries} in XML messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum AttitudeEntryKey {

    /** Quaternion state sub-section. */
    quaternionState((token, context, container) -> true),

    /** Quaternion/derivative sub-section. */
    quaternionDerivative((token, context, container) -> true),

    /** Quaternion/rate sub-section. */
    quaternionEulerRate((token, context, container) -> true),

    /** Euler angle sub-section. */
    eulerAngle((token, context, container) -> true),

    /** Euler angle/rate sub-section. */
    eulerAngleRate((token, context, container) -> true),

    /** Spin sub-section. */
    spin((token, context, container) -> true),

    /** Spin/nutation sub-section. */
    spinNutation((token, context, container) -> true),

    /** Quaternion sub-sub-section. */
    quaternion((token, context, container) -> true),

    /** Rotation angles sub-sub-section. */
    rotationAngles((token, context, container) -> true),

    /** Rotation rates sub-sub-section. */
    rotationRates((token, context, container) -> true),

    /** Entry epoch. */
    EPOCH((token, context, container) -> token.processAsDate(container::setEpoch, context)),

    /** Quaternion first vectorial component. */
    Q1((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 1 : 0,
                                                                   Unit.ONE, container::setComponent)),

    /** Quaternion second vectorial component. */
    Q2((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 2 : 1,
                                                                   Unit.ONE, container::setComponent)),

    /** Quaternion third vectorial component. */
    Q3((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 3 : 2,
                                                                   Unit.ONE, container::setComponent)),

    /** Quaternion scalar component. */
    QC((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 0 : 3,
                                                                   Unit.ONE, container::setComponent)),

    /** Quaternion first vectorial component. */
    Q1_DOT((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 1 : 0,
                                                                       Units.ONE_PER_S, container::setComponent)),

    /** Quaternion second vectorial component. */
    Q2_DOT((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 2 : 1,
                                                                       Units.ONE_PER_S, container::setComponent)),

    /** Quaternion third vectorial component. */
    Q3_DOT((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 3 : 2,
                                                                       Units.ONE_PER_S, container::setComponent)),

    /** Quaternion scalar component. */
    QC_DOT((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 0 : 3,
                                                                       Units.ONE_PER_S, container::setComponent)),

    /** Rotation about X axis. */
    X_ANGLE((token, context, container) -> token.processAsIndexedAngle(0, container::setComponent)),

    /** Rotation about Y axis. */
    Y_ANGLE((token, context, container) -> token.processAsIndexedAngle(1, container::setComponent)),

    /** Rotation about Z axis. */
    Z_ANGLE((token, context, container) -> token.processAsIndexedAngle(2, container::setComponent)),

    /** Rotation about X axis. */
    X_RATE((token, context, container) -> token.processAsIndexedAngle(container.firstRotationIndex(),
                                                                      container::setComponent)),

    /** Rotation about Y axis. */
    Y_RATE((token, context, container) -> token.processAsIndexedAngle(container.firstRotationIndex() + 1,
                                                                      container::setComponent)),

    /** Rotation about Z axis. */
    Z_RATE((token, context, container) -> token.processAsIndexedAngle(container.firstRotationIndex() + 2,
                                                                      container::setComponent));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    AttitudeEntryKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final AttitudeEntry container) {
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
        boolean process(ParseToken token, ContextBinding context, AttitudeEntry container);
    }

}
