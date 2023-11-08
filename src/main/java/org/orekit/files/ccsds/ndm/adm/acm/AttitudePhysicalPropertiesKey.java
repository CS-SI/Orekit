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

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;


/** Keys for {@link AttitudePhysicalProperties physical properties data} entries.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum AttitudePhysicalPropertiesKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Drag coefficient. */
    DRAG_COEFF((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                    container::setDragCoefficient)),

    /** Total mass at T₀. */
    WET_MASS((token, context, container) -> token.processAsDouble(Unit.KILOGRAM, context.getParsedUnitsBehavior(),
                                                                  container::setWetMass)),

    /** Mass without propellant. */
    DRY_MASS((token, context, container) -> token.processAsDouble(Unit.KILOGRAM, context.getParsedUnitsBehavior(),
                                                                  container::setDryMass)),

    /** Reference frame for center of pressure. */
    CP_REF_FRAME((token, context, container) -> token.processAsFrame(container::setCenterOfPressureReferenceFrame, context, false, false, true)),

    /** Center of pressure. */
    CP((token, context, container) -> token.processAsVector(Unit.METRE, context.getParsedUnitsBehavior(),
                                                            container::setCenterOfPressure)),

    /** Inertia reference frame. */
    INERTIA_REF_FRAME((token, context, container) -> token.processAsFrame(container::setInertiaReferenceFrame, context, false, false, true)),

    /** Moment of inertia about X-axis. */
    IXX((token, context, container) -> token.processAsDoublyIndexedDouble(0, 0, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Moment of inertia about Y-axis. */
    IYY((token, context, container) -> token.processAsDoublyIndexedDouble(1, 1, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Moment of inertia about Z-axis. */
    IZZ((token, context, container) -> token.processAsDoublyIndexedDouble(2, 2, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Inertia cross product of the X and Y axes. */
    IXY((token, context, container) -> token.processAsDoublyIndexedDouble(0, 1, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Inertia cross product of the X and Z axes. */
    IXZ((token, context, container) -> token.processAsDoublyIndexedDouble(0, 2, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Inertia cross product of the Y and Z axes. */
    IYZ((token, context, container) -> token.processAsDoublyIndexedDouble(1, 2, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    AttitudePhysicalPropertiesKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param data data to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final AttitudePhysicalProperties data) {
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
        boolean process(ParseToken token, ContextBinding context, AttitudePhysicalProperties data);
    }

}
