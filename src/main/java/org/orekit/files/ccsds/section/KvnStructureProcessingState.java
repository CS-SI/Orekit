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
package org.orekit.files.ccsds.section;

import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.parsing.AbstractConstituentParser;
import org.orekit.files.ccsds.utils.parsing.ProcessingState;

/** {@link ProcessingState} for structure of {@link FileFormat#KVN} CCSDS Messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class KvnStructureProcessingState implements ProcessingState {

    /** Parser for the complete message. */
    private final AbstractConstituentParser<?, ?, ?> parser;

    /** Simple constructor.
     * @param parser parser for the complete message
     */
    public KvnStructureProcessingState(final AbstractConstituentParser<?, ?, ?> parser) {
        this.parser = parser;
    }

    /** {@inheritDoc} */
    @Override
    public boolean processToken(final ParseToken token) {
        try {
            return token.getName() != null &&
                   KvnStructureKey.valueOf(token.getName()).process(token, parser);
        } catch (IllegalArgumentException iae) {
            // ignored, we delegate handling this token to fallback state
            return false;
        }
    }

}
