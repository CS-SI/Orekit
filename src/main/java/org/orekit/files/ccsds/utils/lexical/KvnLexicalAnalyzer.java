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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.utils.units.Unit;
import org.orekit.utils.units.UnitsCache;

/** Lexical analyzer for Key-Value Notation CCSDS messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class KvnLexicalAnalyzer implements LexicalAnalyzer {

    /** Regular expression matching blanks at start of line. */
    private static final String LINE_START         = "^\\p{Blank}*";

    /** Regular expression matching the special COMMENT key that must be stored in the matcher. */
    private static final String COMMENT_KEY        = "(COMMENT)\\p{Blank}*";

    /** Regular expression matching a non-comment key that must be stored in the matcher. */
    private static final String NON_COMMENT_KEY    = "([A-Z][A-Z_0-9]*)\\p{Blank}*=\\p{Blank}*";

    /** Regular expression matching a no-value key starting a block that must be stored in the matcher. */
    private static final String START_KEY          = "([A-Z][A-Z_0-9]*)_START";

    /** Regular expression matching a no-value key ending a block that must be stored in the matcher. */
    private static final String STOP_KEY           = "([A-Z][A-Z_0-9]*)_STOP";

    /** Regular expression matching a value that must be stored in the matcher. */
    private static final String OPTIONAL_VALUE     = "((?:(?:\\p{Graph}.*?)?))";

    /** Operators allowed in units specifications. */
    private static final String UNITS_OPERATORS    = "-+*×.·/⁄^√⁺⁻";

    /** Letters allowed in units specifications. */
    private static final String UNITS_LETTERS      = "A-Za-zµμ"; // beware µ (U+00B5) and μ (U+03BC) look similar but are different

    /** Digits allowed in units specifications. */
    private static final String UNITS_DIGITS       = "0-9⁰¹²³⁴⁵⁶⁷⁸⁹";

    /** Fractions allowed in units specifications. */
    private static final String UNITS_FRACTIONS    = "¼½¾⅐⅑⅒⅓⅔⅕⅖⅗⅘⅙⅚⅛⅜⅝⅞";

    /** Symbols allowed in units specifications. */
    private static final String UNITS_SYMBOLS      = "%°◦′'″\\\"#";

    /** Parentheses allowed in units specifications. */
    private static final String UNITS_PARENTHESES  = "()";

    /** Regular expression matching units that must be stored in the matcher. */
    private static final String UNITS              = "(?:\\p{Blank}+\\[([" +
                                                     UNITS_OPERATORS + UNITS_LETTERS + UNITS_DIGITS +
                                                     UNITS_FRACTIONS + UNITS_SYMBOLS + UNITS_PARENTHESES +
                                                    "]*)\\])?";

    /** Regular expression matching blanks at end of line. */
    private static final String LINE_END           = "\\p{Blank}*$";

    /** Regular expression matching comment entry. */
    private static final Pattern COMMENT_ENTRY     = Pattern.compile(LINE_START + COMMENT_KEY + OPTIONAL_VALUE + LINE_END);

    /** Regular expression matching non-comment entry with optional units.
     * <p>
     * Note than since 12.0, we allow empty values at lexical analysis level and detect them at parsing level
     * </p>
     */
    private static final Pattern NON_COMMENT_ENTRY = Pattern.compile(LINE_START + NON_COMMENT_KEY + OPTIONAL_VALUE + UNITS + LINE_END);

    /** Regular expression matching no-value entry starting a block. */
    private static final Pattern START_ENTRY       = Pattern.compile(LINE_START + START_KEY + LINE_END);

    /** Regular expression matching no-value entry ending a block. */
    private static final Pattern STOP_ENTRY        = Pattern.compile(LINE_START + STOP_KEY + LINE_END);

    /** Source providing the data to analyze. */
    private final DataSource source;

    /** Parsed units cache. */
    private final UnitsCache cache;

    /** Simple constructor.
     * @param source source providing the data to parse
     */
    public KvnLexicalAnalyzer(final DataSource source) {
        this.source = source;
        this.cache  = new UnitsCache();
    }

    /** {@inheritDoc} */
    @Override
    public <T> T accept(final MessageParser<T> messageParser) {

        messageParser.reset(FileFormat.KVN);

        try (Reader         reader = source.getOpener().openReaderOnce();
             BufferedReader br     = (reader == null) ? null : new BufferedReader(reader)) {

            if (br == null) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, source.getName());
            }

            int lineNumber = 0;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;
                if (line.trim().length() == 0) {
                    continue;
                }

                final Matcher nonComment = NON_COMMENT_ENTRY.matcher(line);
                if (nonComment.matches()) {
                    // regular key=value line
                    final Unit units = cache.getUnits(nonComment.groupCount() > 2 ? nonComment.group(3) : null);
                    messageParser.process(new ParseToken(TokenType.ENTRY,
                                                         nonComment.group(1), nonComment.group(2),
                                                         units, lineNumber, source.getName()));
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
