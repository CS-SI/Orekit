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

import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;

/** Keywords allowed in {@link CdmHeader}.
 * @author Melina Vanel
 * @since 11.2
 */
public enum CdmHeaderKey {

    /** Creating spacecraft name for which the CDM is provided. */
    MESSAGE_FOR((token, context, header) -> token.processAsUppercaseString(header::setMessageFor)),

    /** User-defined free-text message classification or caveats of this CDM. */
    CLASSIFICATION((token, context, header) -> token.processAsNormalizedString(header::setClassification));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    CdmHeaderKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param header header to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final CdmHeader header) {
        return processor.process(token, context, header);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context context binding
         * @param header header to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ContextBinding context, CdmHeader header);
    }

}
