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
package org.orekit.files.ccsds.ndm.adm.apm;

import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

/** Keywords for APM data sub-structure.
 * @author Luc Maisonobe
 * @since 11.0
 */
enum ApmDataSubStructureKey {

    /** General comment. */
    COMMENT((token, context, parser) -> token.getType() == TokenType.ENTRY ? parser.addGeneralComment(token.getContentAsNormalizedString()) : true),

    /** Epoch. */
    EPOCH((token, context, parser) -> token.processAsDate(parser::setEpoch, context)),

    /** Quaternion block. */
    QUAT((token, context, parser) -> parser.manageQuaternionSection(token.getType() == TokenType.START)),

    /** Quaternion section. */
    quaternionState((token, context, parser) -> parser.manageQuaternionSection(token.getType() == TokenType.START)),

    /** Euler elements. */
    EULER((token, context, parser) -> parser.manageEulerElementsSection(token.getType() == TokenType.START)),

    /** Euler elements. */
    eulerAngleState((token, context, parser) -> parser.manageEulerElementsSection(token.getType() == TokenType.START)),

    /** Angular velocity elements. */
    ANGVEL((token, context, parser) -> parser.manageAngularVelocitylementsSection(token.getType() == TokenType.START)),

    /** Angular velocity elements. */
    angularVelocity((token, context, parser) -> parser.manageAngularVelocitylementsSection(token.getType() == TokenType.START)),

    /** Euler elements / three axis stabilized section (ADM V1 only). */
    eulerElementsThree((token, context, parser) -> parser.manageEulerElementsSection(token.getType() == TokenType.START)),

    /** Euler elements /spin stabilized section (ADM V1 only). */
    eulerElementsSpin((token, context, parser) -> parser.manageSpinElementsSection(token.getType() == TokenType.START)),

    /** Spin elements. */
    SPIN((token, context, parser) -> parser.manageSpinElementsSection(token.getType() == TokenType.START)),

    /** Spin elements. */
    spin((token, context, parser) -> parser.manageSpinElementsSection(token.getType() == TokenType.START)),

    /** Spacecraft parameters section (ADM V1 only). */
    spacecraftParameters((token, context, parser) -> parser.manageInertiaSection(token.getType() == TokenType.START)),

    /** Inertia elements. */
    INERTIA((token, context, parser) -> parser.manageInertiaSection(token.getType() == TokenType.START)),

    /** Inertia elements. */
    inertia((token, context, parser) -> parser.manageInertiaSection(token.getType() == TokenType.START)),

    /** Maneuver parameters section. */
    MAN((token, context, parser) -> parser.manageManeuverParametersSection(token.getType() == TokenType.START)),

    /** Maneuver parameters section. */
    maneuverParameters((token, context, parser) -> parser.manageManeuverParametersSection(token.getType() == TokenType.START));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    ApmDataSubStructureKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param parser APM file parser
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final ApmParser parser) {
        return processor.process(token, context, parser);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context context binding
         * @param parser APM file parser
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ContextBinding context, ApmParser parser);
    }

}
