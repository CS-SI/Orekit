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

import java.util.Map;

import org.orekit.data.DataSource;
import org.orekit.files.ccsds.utils.FileFormat;

/** Parser for CCSDS messages.
 * @param <T> type of the file
 * @author Luc Maisonobe
 * @since 11.0
 */
public interface MessageParser<T> {

    /** Parse a data source.
     * @param source data source to parse
     * @return parsed file
     */
    T parseMessage(DataSource source);

    /** Get the key for format version.
     * @return format version key
     */
    String getFormatVersionKey();

    /** Get the non-default token builders for special XML elements.
     * @return map of token builders for special XML elements (keyed by XML element name)
     */
    Map<String, XmlTokenBuilder> getSpecialXmlElementsBuilders();

    /** Reset parser to initial state before parsing.
     * @param fileFormat format of the file ready to be parsed
     */
    void reset(FileFormat fileFormat);

    /** Process a parse token.
     * @param token token to process
     */
    void process(ParseToken token);

    /** Build the file from parsed entries.
     * @return parsed file
     */
    T build();

    /** Get the file format of the last message parsed.
     * @return file format of the last message parsed
     * @since 12.0
     */
    FileFormat getFileFormat();

}
