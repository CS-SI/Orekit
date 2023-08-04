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
package org.orekit.files.ccsds.ndm.odm.oem;

import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

/** Keywords for OEM data sub-structure in XML files.
 * @author Luc Maisonobe
 * @since 11.0
 */
enum OemDataSubStructureKey {

    /** State vector section. */
    stateVector((token, parser) -> parser.manageXmlStateVectorSection(token.getType() == TokenType.START)),

    /** Covariance matrix section. */
    covarianceMatrix((token, parser) -> parser.manageCovarianceSection(token.getType() == TokenType.START)),

    /** Covariance matrix section. */
    COVARIANCE((token, parser) -> parser.manageCovarianceSection(token.getType() == TokenType.START));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    OemDataSubStructureKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param parser OEM file parser
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final OemParser parser) {
        return processor.process(token, parser);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param parser OEM file parser
         * @return true of token was accepted
         */
        boolean process(ParseToken token, OemParser parser);
    }

}
