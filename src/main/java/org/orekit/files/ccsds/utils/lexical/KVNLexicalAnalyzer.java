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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NDMFile;

/** Lexical analyzer for Key-Value Notation CCSDS messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class KVNLexicalAnalyzer implements LexicalAnalyzer {

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

    /** Stream containing message. */
    private final InputStream stream;

    /** Name of the file containing the message (for error messages). */
    private final String fileName;

    /** Simple constructor.
     * @param fileName name of the file containing the message (for error messages)
     */
    public KVNLexicalAnalyzer(final String fileName) {
        try {
            this.stream   = new FileInputStream(fileName);
            this.fileName = fileName;
        } catch (IOException e) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, fileName);
        }
    }

    /** Simple constructor.
     * @param stream stream containing message
     */
    public KVNLexicalAnalyzer(final InputStream stream) {
        this(stream, "<unknown>");
    }

    /** Simple constructor.
     * @param stream stream containing message
     * @param fileName name of the file containing the message (for error messages)
     */
    public KVNLexicalAnalyzer(final InputStream stream, final String fileName) {
        this.stream   = stream;
        this.fileName = fileName;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends NDMFile<?, ?>, P extends MessageParser<T, ?>>
        T accept(final MessageParser<T, P> messageParser) {
        messageParser.reset();
        try (InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            int lineNumber = 0;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++lineNumber;
                if (line.trim().length() == 0) {
                    continue;
                }

                final Matcher nonComment = NON_COMMENT_ENTRY.matcher(line);
                if (nonComment.matches()) {
                    // regular key=value line
                    messageParser.process(new ParseEvent(EventType.ENTRY,
                                                         nonComment.group(1), nonComment.group(2),
                                                         nonComment.groupCount() > 2 ? nonComment.group(3) : null,
                                                         lineNumber, fileName));
                } else {
                    final Matcher comment = COMMENT_ENTRY.matcher(line);
                    if (comment.matches()) {
                        // comment line
                        messageParser.process(new ParseEvent(EventType.ENTRY,
                                                             comment.group(1), comment.group(2), null,
                                                             lineNumber, fileName));
                    } else {
                        final Matcher start = START_ENTRY.matcher(line);
                        if (start.matches()) {
                            // block start
                            messageParser.process(new ParseEvent(EventType.START,
                                                                 start.group(1), null, null,
                                                                 lineNumber, fileName));
                        } else {
                            final Matcher stop = STOP_ENTRY.matcher(line);
                            if (stop.matches()) {
                                // block end
                                messageParser.process(new ParseEvent(EventType.END,
                                                                     stop.group(1), null, null,
                                                                     lineNumber, fileName));
                            } else {
                                // raw data line
                                messageParser.process(new ParseEvent(EventType.RAW_LINE,
                                                                     null, line, null,
                                                                     lineNumber, fileName));
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
