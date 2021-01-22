/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.utils.state;

import java.util.Deque;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.lexical.ParseToken;

/**
 * Special {@link ProcessingState} used at end of message, to generate an error if spurious data is found.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
public class EndOfMessageState implements ProcessingState {

    /** {@inheritDoc}
     * <p>
     * This method always generate an error, as no data is expected in this state.
     * </p>
     */
    @Override
    public ProcessingState processToken(final ParseToken token, final Deque<ParseToken> next) {
        throw new OrekitException(OrekitMessages.UNEXPECTED_DATA_AT_LINE_IN_FILE,
                                  token.getLineNumber(), token.getFileName());
    }

}
