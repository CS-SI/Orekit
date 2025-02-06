/* Copyright 2024-2025 The Johns Hopkins University Applied Physics Laboratory
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
package org.orekit.files.iirv;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFileParser;
import org.orekit.time.UTCScale;
import org.orekit.utils.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parser of {@link IIRVEphemerisFile}s.
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class IIRVParser implements EphemerisFileParser<IIRVEphemerisFile> {

    /** Standard gravitational parameter in m³/s². */
    private final double mu;

    /** Number of data points to use in interpolation. */
    private final int interpolationSamples;

    /** Year of the initial vector in the IIRV ephemeris file. */
    private final int year;

    /** UTC time scale. */
    private final UTCScale utc;

    /**
     * Constructs a {@link IIRVParser} instance with default values.
     * <p>
     * Default gravitational parameter is {@link Constants#IERS96_EARTH_MU}. Default number of
     * interpolation samples is 7.
     *
     * @param year year of the initial vector in the IIRV ephemeris file.
     * @param utc  UTC time scale
     */
    public IIRVParser(final int year, final UTCScale utc) {
        this(Constants.IERS96_EARTH_MU, 7, year, utc);
    }

    /**
     * Constructs a {@link IIRVParser} instance.
     *
     * @param mu                   gravitational parameter (m^3/s^2)
     * @param interpolationSamples is the number of samples to use when interpolating.
     * @param year                 year of the initial vector in the IIRV ephemeris file.
     * @param utc                  UTC time scale
     */
    public IIRVParser(final double mu, final int interpolationSamples, final int year, final UTCScale utc) {
        this.mu = mu;
        this.interpolationSamples = interpolationSamples;
        this.year = year;
        this.utc = utc;
    }

    /** @inheritDoc} */
    @Override
    public IIRVEphemerisFile parse(final DataSource source) {
        if (source == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "source");
        }

        final ArrayList<String> messageLines = new ArrayList<>();

        try (Reader reader = source.getOpener().openReaderOnce();
             BufferedReader bufferedReader = (reader == null) ? null : new BufferedReader(reader)) {

            if (bufferedReader == null) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, source.getName());
            }

            // Message lines
            final List<String> vectorLines = new ArrayList<>(Collections.nCopies(6, ""));
            int currentIIRVLine = vectorLines.indexOf("");
            String line = bufferedReader.readLine();  // Initialize on first line

            if (line == null) {
                throw new OrekitException(OrekitMessages.NO_DATA_IN_FILE, source.getName());
            }

            while (line != null) {
                messageLines.add(line);
                vectorLines.set(currentIIRVLine, line);  // Set the line in the list

                // If every line is set, create an IIRV vector and clear out the strings. Otherwise, increment
                // the line counter
                if (currentIIRVLine == 5) {
                    for (int i = 0; i < 6; i++) { // Reset each line + line counter
                        vectorLines.set(i, "");
                    }
                    currentIIRVLine = 0;
                } else {
                    currentIIRVLine++;
                }

                // Expect two line breaks here (except end of file
                final String linebreak1 = bufferedReader.readLine();
                final String linebreak2 = bufferedReader.readLine();
                if (linebreak1 == null || linebreak2 == null) {
                    break;
                } else if (!linebreak1.isEmpty() || !linebreak2.isEmpty()) {
                    throw new OrekitException(OrekitMessages.IIRV_MISSING_LINEBREAK_IN_FILE, currentIIRVLine, source.getName());
                }

                line = bufferedReader.readLine();
            }

        } catch (IOException ioe) {
            throw new OrekitException(ioe, LocalizedCoreFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
        }
        return parse(messageLines);
    }


    /**
     * Parses a string representing an IIRV message.
     *
     * @param iirv String representation of an IIRV message
     * @return newly created {@link IIRVSegment} object populated with ephemeris data parsed from
     * {@code iirvVectorStrings}
     */
    public IIRVEphemerisFile parse(final String iirv) {
        return parse(Arrays.asList(iirv.split(IIRVVector.LINE_SEPARATOR)));
    }

    /**
     * Parses a list of strings that comprise an {@link IIRVMessage}.
     *
     * @param iirvVectorStrings list of Strings that comprise an {@link IIRVMessage}
     * @return newly created {@link IIRVSegment} object populated with ephemeris data parsed from
     * {@code iirvVectorStrings}
     */
    public IIRVEphemerisFile parse(final List<String> iirvVectorStrings) {
        final ArrayList<IIRVVector> vectors = new ArrayList<>();

        if (iirvVectorStrings == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "iirvVectorStrings");
        }

        if (iirvVectorStrings.isEmpty()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_INVALID_LINE_IN_VECTOR, 1, "");
        }

        // The first vector in a message must *always* include metadata
        Pattern line1Pattern = IIRVVector.LINE_1_PATTERN_METADATA_INCLUDED;

        // Message lines
        final List<String> vectorLines = new ArrayList<>(Collections.nCopies(6, ""));
        int currentIIRVLine = vectorLines.indexOf("");

        for (String line : iirvVectorStrings) {

            // The second vector tells us whether to expect metadata in line 1 for the
            // remainder of the file
            if (vectors.size() == 1 && currentIIRVLine == 0 && IIRVVector.LINE_1_PATTERN_METADATA_OMITTED.matcher(line).matches()) {
                line1Pattern = IIRVVector.LINE_1_PATTERN_METADATA_OMITTED;
            }

            // Check if this line matches an IIRV pattern based on the current line index
            final boolean line1Valid = currentIIRVLine == 0 && line1Pattern.matcher(line).matches();

            boolean isValidLine2to6 = false;
            for (int i = currentIIRVLine; i < 6; i++) {
                final boolean isValidForLineI = IIRVVector.validateLine(i, line);
                if (isValidForLineI) {
                    if (currentIIRVLine == i) {
                        isValidLine2to6 = true;  // Only valid if the line is validated at the right location
                        break;
                    } else {
                        // Valid for the wrong line-> invalid file
                        throw new OrekitException(OrekitMessages.IIRV_INVALID_LINE_IN_VECTOR,
                            currentIIRVLine, line);
                    }
                }
            }

            // Continue if this line matches a pattern
            if (line1Valid || isValidLine2to6) {
                vectorLines.set(currentIIRVLine, line);  // Set the line in the list

                // If every line is set, create an IIRV vector and clear out the strings. Otherwise, increment
                // the line counter
                if (currentIIRVLine == 5) {
                    IIRVVector newVector = new IIRVVector(vectorLines, utc);

                    // Add metadata (if applicable)
                    if (!vectors.isEmpty() && line1Pattern == IIRVVector.LINE_1_PATTERN_METADATA_OMITTED) {
                        vectorLines.set(0, vectors.get(0).buildLine1(true));
                        newVector = new IIRVVector(vectorLines, utc);
                    }

                    vectors.add(newVector);

                    for (int i = 0; i < 6; i++) { // Reset each line + line counter
                        vectorLines.set(i, "");
                    }
                    currentIIRVLine = 0;
                } else {
                    currentIIRVLine++;
                }
            }
        }
        return new IIRVEphemerisFile(mu, interpolationSamples, year, new IIRVMessage(vectors));
    }
}
