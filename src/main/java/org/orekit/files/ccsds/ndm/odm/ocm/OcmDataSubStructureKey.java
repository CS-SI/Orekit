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
package org.orekit.files.ccsds.ndm.odm.ocm;

import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

/** Keywords for OCM data sub-structure.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum OcmDataSubStructureKey {

    /** Trajectory state time history section. */
    TRAJ((token, parser) -> parser.manageTrajectoryStateSection(token.getType() == TokenType.START)),

    /** Trajectory state time history section. */
    traj((token, parser) -> parser.manageTrajectoryStateSection(token.getType() == TokenType.START)),

    /** Physical properties section. */
    PHYS((token, parser) -> parser.managePhysicalPropertiesSection(token.getType() == TokenType.START)),

    /** Physical properties section. */
    phys((token, parser) -> parser.managePhysicalPropertiesSection(token.getType() == TokenType.START)),

    /** Covariance time history section. */
    COV((token, parser) -> parser.manageCovarianceHistorySection(token.getType() == TokenType.START)),

    /** Covariance time history section. */
    cov((token, parser) -> parser.manageCovarianceHistorySection(token.getType() == TokenType.START)),

    /** Maneuvers section. */
    MAN((token, parser) -> parser.manageManeuversSection(token.getType() == TokenType.START)),

    /** Maneuvers section. */
    man((token, parser) -> parser.manageManeuversSection(token.getType() == TokenType.START)),

    /** Perturbations parameters section. */
    PERT((token, parser) -> parser.managePerturbationParametersSection(token.getType() == TokenType.START)),

    /** Perturbations parameters section. */
    pert((token, parser) -> parser.managePerturbationParametersSection(token.getType() == TokenType.START)),

    /** Orbit determination section. */
    OD((token, parser) -> parser.manageOrbitDeterminationSection(token.getType() == TokenType.START)),

    /** Orbit determination section. */
    od((token, parser) -> parser.manageOrbitDeterminationSection(token.getType() == TokenType.START)),

    /** User-defined parameters section. */
    USER((token, parser) -> parser.manageUserDefinedParametersSection(token.getType() == TokenType.START)),

    /** User-defined parameters section. */
    user((token, parser) -> parser.manageUserDefinedParametersSection(token.getType() == TokenType.START));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    OcmDataSubStructureKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param parser OCM file parser
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final OcmParser parser) {
        return processor.process(token, parser);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param parser OCM file parser
         * @return true of token was accepted
         */
        boolean process(ParseToken token, OcmParser parser);
    }

}
