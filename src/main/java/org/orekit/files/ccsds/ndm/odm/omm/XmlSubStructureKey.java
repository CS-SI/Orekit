/* Copyright 2002-2024 CS GROUP
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
package org.orekit.files.ccsds.ndm.odm.omm;

import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

/** Keywords for OMM data sub-structure in XML files.
 * @author Luc Maisonobe
 * @since 11.0
 */
enum XmlSubStructureKey {

    /** Mean Keplerian elements section. */
    meanElements((token, parser) -> parser.manageKeplerianElementsSection(token.getType() == TokenType.START)),

    /** Spacecraft parameters section. */
    spacecraftParameters((token, parser) -> parser.manageSpacecraftParametersSection(token.getType() == TokenType.START)),

    /** TLE parameters section. */
    tleParameters((token, parser) -> parser.manageTleParametersSection(token.getType() == TokenType.START)),

    /** Covariance matrix section. */
    covarianceMatrix((token, parser) -> parser.manageCovarianceSection(token.getType() == TokenType.START)),

    /** User-defined parameters section. */
    userDefinedParameters((token, parser) -> parser.manageUserDefinedParametersSection(token.getType() == TokenType.START));

    /** Processing method. */
    private final transient TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    XmlSubStructureKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param parser OMM file parser
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final OmmParser parser) {
        return processor.process(token, parser);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param parser OMM file parser
         * @return true of token was accepted
         */
        boolean process(ParseToken token, OmmParser parser);
    }

}
