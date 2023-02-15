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
package org.orekit.files.ccsds.utils.lexical;

/** Interface for CCSDS messages lexical analysis.
 * <p>
 * Lexical analyzer implementations split raw streams
 * of characters into tokens and feed them to
 * {@link MessageParser message parsers}. Each
 * lexical analyzer knows about a basic character
 * stream format ({@link KvnLexicalAnalyzer Key-Value
 * Notation} or {@link XmlLexicalAnalyzer XML}) but
 * knows nothing about the CCSDS messages themselves.
 * The {@link MessageParser message parsers} know about
 * CCSDS messages.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public interface LexicalAnalyzer {

    /** Parse a CCSDS Message.
     * @param messageParser CCSDS Message parser to use
     * @param <T> type of the file
     * @return parsed fileO
     */
    <T> T accept(MessageParser<T> messageParser);

}
