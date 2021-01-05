/* Copyright 2002-2020 CS GROUP
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
import org.orekit.errors.OrekitException;
import org.orekit.files.ccsds.ndm.NDMFile;
import org.orekit.files.ccsds.ndm.NDMHeader;
import org.orekit.files.ccsds.ndm.NDMSegment;

/** Lexical analyzer for Key-Value Notation CCSDS messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class KVNLexicalAnalyzer implements LexicalAnalyzer {

    /** Regular expression for splitting lines. */
    private static final Pattern PATTERN =
            Pattern.compile("\\p{Space}*([A-Z][A-Z_0-9]*)\\p{Space}*=?\\p{Space}*(.*?)\\p{Space}*(?:\\[.*\\])?\\p{Space}*");

    /** Stream containing message. */
    private final InputStream stream;

    /** Name of the file containing the message (for error messages). */
    private final String fileName;

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
    public <H extends NDMHeader, S extends NDMSegment<?, ?>>
        NDMFile<H, S> parse(final MessageParser<H, S> messageParser) {
        try (InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            int lineNumber = 0;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++lineNumber;
                if (line.trim().length() == 0) {
                    continue;
                }

                final Matcher matcher = PATTERN.matcher(line);
                if (matcher.matches()) {
                    final String key   = matcher.group(1);
                    final String value = matcher.group(2);
                    messageParser.start(key);
                    messageParser.entry(new KVNEntry(key, value, line, lineNumber, fileName));
                }

            }

            return messageParser.build();

        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

}
