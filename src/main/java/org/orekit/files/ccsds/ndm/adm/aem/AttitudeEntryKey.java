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

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.ndm.adm.AttitudeType;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.utils.units.Unit;


/** Keys for {@link AttitudeEntry attitude entries} in XML messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum AttitudeEntryKey {

    /** Quaternion state sub-section (only for ADM V1). */
    quaternionState((token, context, container) -> true),

    /** Quaternion state sub-section.
     * @since 12.0
     */
    quaternionEphemeris((token, context, container) -> true),

    /** Quaternion/derivative sub-section. */
    quaternionDerivative((token, context, container) -> true),

    /** Quaternion/rate sub-section (only for ADM V1). */
    quaternionEulerRate((token, context, container) -> true),

    /** Quaternion/angular velocity sub-section.
     * @since 12.0
     */
    quaternionAngVel((token, context, container) -> true),

    /** Euler angle sub-section. */
    eulerAngle((token, context, container) -> true),

    /** Euler angle/rate sub-section (only for ADM V1). */
    eulerAngleRate((token, context, container) -> true),

    /** Euler angle/derivative sub-section.
     * @since 12.0
     */
    eulerAngleDerivative((token, context, container) -> true),

    /** Euler angle/angular velocity sub-section.
     * @since 12.0
     */
    eulerAngleAngVel((token, context, container) -> true),

    /** Spin sub-section. */
    spin((token, context, container) -> true),

    /** Spin/nutation sub-section. */
    spinNutation((token, context, container) -> true),

    /** Spin/nutation/momentum sub-section.
     * @since 12.0
     */
    spinNutationMom((token, context, container) -> true),

    /** Quaternion sub-sub-section. */
    quaternion((token, context, container) -> true),

    /** Quaternion rate sub-sub-section(only for ADM V1). */
    quaternionRate((token, context, container) -> true),

    /** Quaternion rate sub-sub-section.
     * @since 12.0
     */
    quaternionDot((token, context, container) -> true),

    /** Rotation angles sub-sub-section (only for ADM V1). */
    rotationAngles((token, context, container) -> true),

    /** Rotation rates sub-sub-section (only for ADM V1). */
    rotationRates((token, context, container) -> true),

    /** Angular velocity sub-sub-section.
     * @since 12.0
     */
    angVel((token, context, container) -> true),

    /** Entry epoch. */
    EPOCH((token, context, container) -> token.processAsDate(container::setEpoch, context)),

    /** Quaternion first vectorial component. */
    Q1((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 1 : 0,
                                                                   Unit.ONE, context.getParsedUnitsBehavior(),
                                                                   container::setComponent)),

    /** Quaternion second vectorial component. */
    Q2((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 2 : 1,
                                                                   Unit.ONE, context.getParsedUnitsBehavior(),
                                                                   container::setComponent)),

    /** Quaternion third vectorial component. */
    Q3((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 3 : 2,
                                                                   Unit.ONE, context.getParsedUnitsBehavior(),
                                                                   container::setComponent)),

    /** Quaternion scalar component. */
    QC((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 0 : 3,
                                                                   Unit.ONE, context.getParsedUnitsBehavior(),
                                                                   container::setComponent)),

    /** Quaternion first vectorial component. */
    Q1_DOT((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 5 : 4,
                                                                       Units.ONE_PER_S, context.getParsedUnitsBehavior(),
                                                                       container::setComponent)),

    /** Quaternion second vectorial component. */
    Q2_DOT((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 6 : 5,
                                                                       Units.ONE_PER_S, context.getParsedUnitsBehavior(),
                                                                       container::setComponent)),

    /** Quaternion third vectorial component. */
    Q3_DOT((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 7 : 6,
                                                                       Units.ONE_PER_S, context.getParsedUnitsBehavior(),
                                                                       container::setComponent)),

    /** Quaternion scalar component. */
    QC_DOT((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().isFirst() ? 4 : 7,
                                                                       Units.ONE_PER_S, context.getParsedUnitsBehavior(),
                                                                       container::setComponent)),

    /** Angular velocity about X axis.
     * @since 12.0
     */
    ANGVEL_X((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().getAttitudeType() == AttitudeType.QUATERNION_ANGVEL ? 4 : 3,
                                                                         Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                         container::setComponent)),

    /** Angular velocity about Y axis.
     * @since 12.0
     */
    ANGVEL_Y((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().getAttitudeType() == AttitudeType.QUATERNION_ANGVEL ? 5 : 4,
                                                                         Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                         container::setComponent)),

    /** Angular velocity about Z axis.
     * @since 12.0
     */
    ANGVEL_Z((token, context, container) -> token.processAsIndexedDouble(container.getMetadata().getAttitudeType() == AttitudeType.QUATERNION_ANGVEL ? 6 : 5,
                                                                         Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                         container::setComponent)),

    /** Rotation about first axis.
     * @since 12.0
     */
    ANGLE_1((token, context, container) -> token.processAsIndexedDouble(0,
                                                                        Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                        container::setComponent)),

    /** Rotation about second axis.
     * @since 12.0
     */
    ANGLE_2((token, context, container) -> token.processAsIndexedDouble(1,
                                                                        Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                        container::setComponent)),

    /** Rotation about third axis.
     * @since 12.0
     */
    ANGLE_3((token, context, container) -> token.processAsIndexedDouble(2,
                                                                        Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                        container::setComponent)),

    /** Rotation about first axis.
     * @since 12.0
     */
    ANGLE_1_DOT((token, context, container) -> token.processAsIndexedDouble(3,
                                                                            Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                            container::setComponent)),

    /** Rotation about second axis.
     * @since 12.0
     */
    ANGLE_2_DOT((token, context, container) -> token.processAsIndexedDouble(4,
                                                                            Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                            container::setComponent)),

    /** Rotation about third axis.
     * @since 12.0
     */
    ANGLE_3_DOT((token, context, container) -> token.processAsIndexedDouble(5,
                                                                            Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                            container::setComponent)),

    /** Rotation about X axis (only for ADM V1). */
    X_ANGLE((token, context, container) -> token.processAsLabeledDouble('X', Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                        container::setLabeledAngle)),

    /** Rotation about Y axis (only for ADM V1). */
    Y_ANGLE((token, context, container) -> token.processAsLabeledDouble('Y', Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                        container::setLabeledAngle)),

    /** Rotation about Z axis (only for ADM V1). */
    Z_ANGLE((token, context, container) -> token.processAsLabeledDouble('Z', Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                        container::setLabeledAngle)),

    /** Rotation about X axis (only for ADM V1). */
    X_RATE((token, context, container) -> token.processAsLabeledDouble('X',
                                                                       Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                       container::setLabeledRate)),

    /** Rotation about Y axis (only for ADM V1). */
    Y_RATE((token, context, container) -> token.processAsLabeledDouble('Y',
                                                                       Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                       container::setLabeledRate)),

    /** Rotation about Z axis (only for ADM V1). */
    Z_RATE((token, context, container) -> token.processAsLabeledDouble('Z',
                                                                       Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                       container::setLabeledRate)),

    /** Right ascension of spin axis vector. */
    SPIN_ALPHA((token, context, container) -> token.processAsIndexedDouble(0, Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                           container::setComponent)),

    /** Declination of spin axis vector. */
    SPIN_DELTA((token, context, container) -> token.processAsIndexedDouble(1, Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                           container::setComponent)),

    /** Phase of satellite about spin axis. */
    SPIN_ANGLE((token, context, container) -> token.processAsIndexedDouble(2, Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                           container::setComponent)),

    /** angular velocity of satellite around spin axis. */
    SPIN_ANGLE_VEL((token, context, container) -> token.processAsIndexedDouble(3, Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                               container::setComponent)),

    /** Nutation angle entry. */
    NUTATION((token, context, container) -> token.processAsIndexedDouble(4, Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                         container::setComponent)),

    /** Nutation period entry. */
    NUTATION_PER((token, context, container) -> token.processAsIndexedDouble(5, Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                             container::setComponent)),

    /** Nutation phase entry. */
    NUTATION_PHASE((token, context, container) -> token.processAsIndexedDouble(6, Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                               container::setComponent)),

    /** Right ascension of angular momentum vector.
     * @since 12.0
     */
    MOMENTUM_ALPHA((token, context, container) -> token.processAsIndexedDouble(4, Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                               container::setComponent)),

    /** Declination of angular momentum vector.
     * @since 12.0
     */
    MOMENTUM_DELTA((token, context, container) -> token.processAsIndexedDouble(5, Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                               container::setComponent)),

    /** Nutation velocity entry.
     * @since 12.0
     */
    NUTATION_VEL((token, context, container) -> token.processAsIndexedDouble(6, Units.DEG_PER_S, context.getParsedUnitsBehavior(),
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
