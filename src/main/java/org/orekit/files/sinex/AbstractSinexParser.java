/* Copyright 2002-2025 CS GROUP
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
package org.orekit.files.sinex;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.TimeScales;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;

/** Base parser for Solution INdependent EXchange (SINEX) files.
 * @param <T> type of the SINEX file
 * @param <P> type of the SINEX files parse info
 * @author Luc Maisonobe
 * @since 13.0
 */
public abstract class AbstractSinexParser<T extends AbstractSinex, P extends ParseInfo<T>> {

    /** Time scales. */
    private final TimeScales timeScales;

    /** Simple constructor.
     * @param timeScales time scales
     */
    protected AbstractSinexParser(final TimeScales timeScales) {
        this.timeScales = timeScales;
    }

    /** Parse one or more SINEX files.
     * @param sources sources providing the data to parse
     * @return parsed file combining all sources
     */
    public T parse(final DataSource... sources) {

        // placeholders for parsed data
        final P parseInfo = buildParseInfo();

        for (final DataSource source : sources) {

            // start parsing a new file
            parseInfo.newSource(source.getName());
            Iterable<LineParser<P>> candidateParsers = Collections.singleton(firstLineParser());

            try (Reader reader = source.getOpener().openReaderOnce(); BufferedReader br = new BufferedReader(reader)) {
                nextLine:
                for (parseInfo.setLine(br.readLine()); parseInfo.getLine() != null; parseInfo.setLine(br.readLine())) {
                    parseInfo.incrementLineNumber();

                    // ignore all comment lines
                    if (parseInfo.getLine().charAt(0) == '*') {
                        continue;
                    }

                    // find a parser for the current line among the available candidates
                    for (final LineParser<P> candidate : candidateParsers) {
                        try {
                            if (candidate.parseIfRecognized(parseInfo)) {
                                // candidate successfully parsed the line
                                candidateParsers = candidate.allowedNextParsers(parseInfo);
                                continue nextLine;
                            }
                        } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
                            throw new OrekitException(e, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                      parseInfo.getLineNumber(), parseInfo.getName(),
                                                      parseInfo.getLine());
                        }
                    }

                    // no candidate could parse this line
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              parseInfo.getLineNumber(), parseInfo.getName(),
                                              parseInfo.getLine());

                }

            } catch (IOException ioe) {
                throw new OrekitException(ioe, LocalizedCoreFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
            }

        }

        return parseInfo.build();

    }

    /** Get parser for the first line.
     * @return parser for the firsty line of the file
     */
    protected abstract LineParser<P> firstLineParser();

    /** Build the container for parsing info.
     * @return container for parsing info
     */
    protected abstract P buildParseInfo();

    /** Get the time scales.
     * @return time scales
     */
    public TimeScales getTimeScales() {
        return timeScales;
    }

}
