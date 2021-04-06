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
package org.orekit.files.ccsds.utils.lexical;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NdmFile;
import org.orekit.files.ccsds.utils.FileFormat;

/** Lexical analyzer for Key-Value Notation CCSDS messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
class KvnLexicalAnalyzer implements LexicalAnalyzer {

    /** Regular expression matching blanks at start of line. */
    private static final String LINE_START          = "^\\p{Blank}*";

    /** Regular expression matching the special COMMENT key that must be stored in the matcher. */
    private static final String COMMENT_KEY         = "(COMMENT)\\p{Blank}*";

    /** Regular expression matching a non-comment key that must be stored in the matcher. */
    private static final String NON_COMMENT_KEY     = "([A-Z][A-Z_0-9]*)\\p{Blank}*=\\p{Blank}*";

    /** Regular expression matching a no-value key starting a block that must be stored in the matcher. */
    private static final String START_KEY           = "([A-Z][A-Z_0-9]*)_START";

    /** Regular expression matching a no-value key ending a block that must be stored in the matcher. */
    private static final String STOP_KEY            = "([A-Z][A-Z_0-9]*)_STOP";

    /** Regular expression matching a value that must be stored in the matcher. */
    private static final String VALUE               = "(\\p{Graph}.*?)";

    /** Regular expression matching units that must be stored in the matcher. */
    private static final String UNITS               = "(?:\\p{Blank}+\\[([-*/A-Za-z0-9]*)\\])?";

    /** Regular expression matching blanks at end of line. */
    private static final String LINE_END            = "\\p{Blank}*$";

    /** Regular expression matching comment entry. */
    private static final Pattern COMMENT_ENTRY      = Pattern.compile(LINE_START + COMMENT_KEY + VALUE + LINE_END);

    /** Regular expression matching non-comment entry with optional units. */
    private static final Pattern NON_COMMENT_ENTRY  = Pattern.compile(LINE_START + NON_COMMENT_KEY + VALUE + UNITS + LINE_END);

    /** Regular expression matching no-value entry starting a block. */
    private static final Pattern START_ENTRY        = Pattern.compile(LINE_START + START_KEY + LINE_END);

    /** Regular expression matching no-value entry ending a block. */
    private static final Pattern STOP_ENTRY         = Pattern.compile(LINE_START + STOP_KEY + LINE_END);

    /** Source providing the data to analyze. */
    private final DataSource source;

    /** Simple constructor.
     * @param source source providing the data to parse
     */
    KvnLexicalAnalyzer(final DataSource source) {
        this.source = source;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends NdmFile<?, ?>> T accept(final MessageParser<T> messageParser) {

        messageParser.reset(FileFormat.KVN);

        try (InputStream       is     = source.getStreamOpener().openOnce();
             InputStreamReader isr    = (is  == null) ? null : new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader    reader = (isr == null) ? null : new BufferedReader(isr)) {

            if (reader == null) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, source.getName());
            }

            int lineNumber = 0;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++lineNumber;
                if (line.trim().length() == 0) {
                    continue;
                }

                final Matcher nonComment = NON_COMMENT_ENTRY.matcher(line);
                if (nonComment.matches()) {
                    // regular key=value line
                    messageParser.process(new ParseToken(TokenType.ENTRY,
                                                         nonComment.group(1), nonComment.group(2),
                                                         nonComment.groupCount() > 2 ? nonComment.group(3) : null,
                                                         lineNumber, source.getName()));
                } else {
                    final Matcher comment = COMMENT_ENTRY.matcher(line);
                    if (comment.matches()) {
                        // comment line
                        messageParser.process(new ParseToken(TokenType.ENTRY,
                                                             comment.group(1), comment.group(2), null,
                                                             lineNumber, source.getName()));
                    } else {
                        final Matcher start = START_ENTRY.matcher(line);
                        if (start.matches()) {
                            // block start
                            messageParser.process(new ParseToken(TokenType.START,
                                                                 start.group(1), null, null,
                                                                 lineNumber, source.getName()));
                        } else {
                            final Matcher stop = STOP_ENTRY.matcher(line);
                            if (stop.matches()) {
                                // block end
                                messageParser.process(new ParseToken(TokenType.STOP,
                                                                     stop.group(1), null, null,
                                                                     lineNumber, source.getName()));
                            } else {
                                // raw data line
                                messageParser.process(new ParseToken(TokenType.RAW_LINE,
                                                                     null, line, null,
                                                                     lineNumber, source.getName()));
                            }
                        }
                    }
                }

            }

            return messageParser.build();

        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

}
