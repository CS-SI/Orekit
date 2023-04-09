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
package org.orekit.files.ccsds.ndm.adm.apm;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;


/** Keys for {@link Inertia inertia} entries.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum InertiaKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Frame in which inertia is defined. */
    INERTIA_REF_FRAME((token, context, container) -> token.processAsFrame(container::setFrame, context, false, false, true)),

    /** Moment of inertia about X-axis (ADM V1 only). */
    I11((token, context, container) -> token.processAsDoublyIndexedDouble(0, 0, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Moment of inertia about X-axis. */
    IXX((token, context, container) -> token.processAsDoublyIndexedDouble(0, 0, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Moment of inertia about Y-axis (ADM V1 only). */
    I22((token, context, container) -> token.processAsDoublyIndexedDouble(1, 1, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Moment of inertia about Y-axis. */
    IYY((token, context, container) -> token.processAsDoublyIndexedDouble(1, 1, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Moment of inertia about Z-axis (ADM V1 only). */
    I33((token, context, container) -> token.processAsDoublyIndexedDouble(2, 2, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Moment of inertia about Z-axis. */
    IZZ((token, context, container) -> token.processAsDoublyIndexedDouble(2, 2, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Inertia cross product of the X and Y axes (ADM V1 only). */
    I12((token, context, container) -> token.processAsDoublyIndexedDouble(0, 1, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Inertia cross product of the X and Y axes. */
    IXY((token, context, container) -> token.processAsDoublyIndexedDouble(0, 1, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Inertia cross product of the X and Z axes (ADM V1 only). */
    I13((token, context, container) -> token.processAsDoublyIndexedDouble(0, 2, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Inertia cross product of the X and Z axes. */
    IXZ((token, context, container) -> token.processAsDoublyIndexedDouble(0, 2, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Inertia cross product of the Y and Z axes (ADM V1 only). */
    I23((token, context, container) -> token.processAsDoublyIndexedDouble(1, 2, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry)),

    /** Inertia cross product of the Y and Z axes. */
    IYZ((token, context, container) -> token.processAsDoublyIndexedDouble(1, 2, Units.KG_M2, context.getParsedUnitsBehavior(),
                                                                          container::setInertiaMatrixEntry));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    InertiaKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param data data to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final Inertia data) {
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
        boolean process(ParseToken token, ContextBinding context, Inertia data);
    }

}
