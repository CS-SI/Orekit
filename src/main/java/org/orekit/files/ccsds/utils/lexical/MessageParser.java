/* Copyright 2002-2022 CS GROUP
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

import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

    /** Add a filter for parsed tokens.
     * <p>
     * This filter allows to change parsed tokens. The filters are always
     * applied in the order they were set. There are several use cases for
     * this feature.
     * </p>
     * <p>
     * The first use case is to allow parsing malformed CCSDS messages with some known
     * discrepancies that can be fixed. One real life example (the one that motivated the
     * development of this feature) is OMM files in XML format that add an empty
     * OBJECT_ID. This could be fixed by setting a filter as follows:
     * </p>
     * <pre>{@code
     * OmmmParser parser = new ParserBuilder().buildOmmmParser();
     * parser.addFilter(token -> {
     *                      if ("OBJECT_ID".equals(token.getName()) &&
     *                          (token.getRawContent() == null || token.getRawContent().isEmpty())) {
     *                          // replace null/empty entries with "unknown"
     *                          return Collections.singletonList(new ParseToken(token.getType(), token.getName(),
     *                                                                          "unknown", token.getUnits(),
     *                                                                          token.getLineNumber(), token.getFileName()));
     *                      } else {
     *                          return Collections.singletonList(token);
     *                      }
     *                  });
     * Omm omm = parser.parseMessage(message);
     * }</pre>
     * <p>
     * A second use case is to remove unwanted data. For example in order to remove all user-defined data
     * one could use:
     * </p>
     * <pre>{@code
     * OmmmParser parser = new ParserBuilder().buildOmmmParser();
     * parser.addFilter(token -> {
     *                      if (token.getName().startsWith("USER_DEFINED")) {
     *                          return Collections.emptyList();
     *                      } else {
     *                          return Collections.singletonList(token);
     *                      }
     *                  });
     * Omm omm = parser.parseMessage(message);
     * }</pre>
     * <p>
     * A third use case is to add data not originally present in the file. For example in order
     * to add a generated ODM V3 message id to an ODM V2 message that lacks it, one could do:
     * </p>
     * <pre>{@code
     * OmmmParser parser = new ParserBuilder().buildOmmmParser();
     * final String myMessageId = ...; // this could be computed from a counter, or a SHA256 digest, or some metadata
     * parser.addFilter(token -> {
     *                      if ("CCSDS_OMM_VERS".equals(token.getName())) {
     *                          // enforce ODM V3
     *                          return Collections.singletonList(new ParseToken(token.getType(), token.getName(),
     *                                                                          "3.0", token.getUnits(),
     *                                                                          token.getLineNumber(), token.getFileName()));
     *                      } else {
     *                          return Collections.singletonList(token);
     *                      }
     *                  });
     * parser.addFilter(token -> {
     *                      if ("ORIGINATOR".equals(token.getName())) {
     *                          // add generated message ID after ORIGINATOR entry
     *                          return Arrays.asList(token,
     *                                               new ParseToken(TokenType.ENTRY, "MESSAGE_ID",
     *                                                              myMessageId, null,
     *                                                              -1, token.getFileName()));
     *                      } else {
     *                          return Collections.singletonList(token);
     *                      }
     *                  });
     * }</pre>
     * Omm omm = parser.parseMessage(message);
     * @param filter token filter to add
     * @since 12.0
     */
    void addFilter(Function<ParseToken, List<ParseToken>> filter);

    /** Build the file from parsed entries.
     * @return parsed file
     */
    T build();

}
