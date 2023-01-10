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
package org.orekit.files.ccsds.ndm.cdm;

import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

/** Keywords for CDM data sub-structure in XML files.
 * @author Melina Vanel
 * @since 11.2
 */
public enum XmlSubStructureKey {

    /** General comment. */
    COMMENT((token, parser) -> token.getType() == TokenType.ENTRY ? parser.addGeneralComment(token.getContentAsNormalizedString()) : true),

    /** Relative Metadata section. */
    relativeMetadataData((token, parser) -> parser.manageRelativeMetadataSection(token.getType() == TokenType.START)),

    /** Segment structure. */
    segment((token, parser) -> true),

    /** Relative Metadata section. */
    relativeStateVector((token, parser) -> parser.manageRelativeStateVectorSection(token.getType() == TokenType.START)),

    /** OD Parameters section. */
    odParameters((token, parser) -> parser.manageODParametersSection(token.getType() == TokenType.START)),

    /** Additional Parameter section. */
    additionalParameters((token, parser) -> parser.manageAdditionalParametersSection(token.getType() == TokenType.START)),

    /** State Vector section. */
    stateVector((token, parser) -> parser.manageStateVectorSection(token.getType() == TokenType.START)),

    /** Covariance Matrix section. */
    covarianceMatrix((token, parser) -> parser.manageXmlGeneralCovarianceSection(token.getType() == TokenType.START)),

        /** User-defined parameters section. */
    userDefinedParameters((token, parser) -> parser.manageUserDefinedParametersSection(token.getType() == TokenType.START));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    XmlSubStructureKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param parser CDM file parser
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final CdmParser parser) {
        return processor.process(token, parser);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param parser CDM file parser
         * @return true of token was accepted
         */
        boolean process(ParseToken token, CdmParser parser);
    }

}
