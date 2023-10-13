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
package org.orekit.files.ccsds.utils.parsing;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

/**
 * Special {@link ProcessingState} that always generate an error message.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
public class ErrorState implements ProcessingState {

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public ErrorState() {
        // nothing to do
    }

    /** {@inheritDoc}
     * <p>
     * This method always generate an error, as no data is expected in this state.
     * </p>
     */
    @Override
    public boolean processToken(final ParseToken token) {
        if (token.getType() == TokenType.RAW_LINE) {
            throw new OrekitException(OrekitMessages.UNEXPECTED_DATA_AT_LINE_IN_FILE,
                                      token.getLineNumber(), token.getFileName());
        } else {
            throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD,
                                      token.getLineNumber(), token.getFileName(), token.getName());
        }
    }

}
