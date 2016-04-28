/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.TimeStamped;

/**
 * Parser for simple tables containing {@link TimeStamped time stamped} data.
 * @param <T> the type of time stamped data (i.e. parsed table rows)
 * @author Luc Maisonobe
 * @since 6.1
 */
public class SimpleTimeStampedTableParser<T extends TimeStamped> {

    /** Interface for converting a table row into time-stamped data.
     * @param <S> the type of time stamped data (i.e. parsed table rows)
     */
    public interface RowConverter<S extends TimeStamped> {

        /** Convert a row.
         * @param rawFields raw row fields, as read from the file
         * @return converted row
         * @exception OrekitException if conversion cannot be performed
         */
        S convert(double[] rawFields) throws OrekitException;
    }

    /** Pattern for fields with real type. */
    private static final String  REAL_TYPE_PATTERN =
            "[-+]?(?:(?:\\p{Digit}+(?:\\.\\p{Digit}*)?)|(?:\\.\\p{Digit}+))(?:[eE][-+]?\\p{Digit}+)?";

    /** Number of columns. */
    private final int columns;

    /** Converter for rows. */
    private final RowConverter<T> converter;

    /** Simple constructor.
     * @param columns number of columns
     * @param converter converter for rows
     */
    public SimpleTimeStampedTableParser(final int columns, final RowConverter<T> converter) {
        this.columns   = columns;
        this.converter = converter;
    }

    /** Parse a stream.
     * @param stream stream containing the table
     * @param name name of the resource file (for error messages only)
     * @return parsed table
     * @exception OrekitException if stream is null or the table cannot be parsed
     */
    public List<T> parse(final InputStream stream, final String name) throws OrekitException {

        if (stream == null) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, name);
        }

        // regular lines are simply a space separated list of numbers
        final StringBuilder builder = new StringBuilder("^\\p{Space}*");
        for (int i = 0; i < columns; ++i) {
            builder.append("(");
            builder.append(REAL_TYPE_PATTERN);
            builder.append(")");
            builder.append((i < columns - 1) ? "\\p{Space}+" : "\\p{Space}*$");
        }
        final Pattern regularLinePattern = Pattern.compile(builder.toString());

        try {

            // setup the reader
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));

            final List<T> table = new ArrayList<T>();

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {

                // replace unicode minus sign ('−') by regular hyphen ('-') for parsing
                // such unicode characters occur in tables that are copy-pasted from PDF files
                line = line.replace('\u2212', '-');

                final Matcher regularMatcher = regularLinePattern.matcher(line);
                if (regularMatcher.matches()) {
                    // we have found a regular data line

                    final double[] rawFields = new double[columns];
                    for (int i = 0; i < columns; ++i) {
                        rawFields[i] = Double.parseDouble(regularMatcher.group(i + 1));
                    }

                    table.add(converter.convert(rawFields));

                }

            }

            if (table.isEmpty()) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
            }

            return table;

        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }

    }

}
