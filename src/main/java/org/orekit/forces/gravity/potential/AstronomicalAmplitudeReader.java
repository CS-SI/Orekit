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
package org.orekit.forces.gravity.potential;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataLoader;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Parser for tides astronomical amplitude H<sub>f</sub>.
 * @author Luc Maisonobe
 * @since 6.1
 */
public class AstronomicalAmplitudeReader implements DataLoader {

    /** Pattern for optional fields (either nothing or non-space characters). */
    private static final String  OPTIONAL_FIELD_PATTERN = "\\S*";

    /** Pattern for fields with Doodson number. */
    private static final String  DOODSON_TYPE_PATTERN = "\\p{Digit}{2,3}[.,]\\p{Digit}{3}";

    /** Pattern for fields with real type. */
    private static final String  REAL_TYPE_PATTERN =
            "[-+]?(?:(?:\\p{Digit}+(?:\\.\\p{Digit}*)?)|(?:\\.\\p{Digit}+))(?:[eE][-+]?\\p{Digit}+)?";

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Pattern for regular data lines. */
    private final Pattern regularLinePattern;

    /** Doodson number column. */
    private final int columnDoodson;

    /** H<sub>f</sub> column. */
    private final int columnHf;

    /** Scaling factor for astronomical amplitude. */
    private final double scale;

    /** Amplitudes map. */
    private final Map<Integer, Double> amplitudesMap;

    /** Simple constructor.
     * @param supportedNames regular expression for supported files names
     * @param columns number of columns
     * @param columnDoodson Doodson number column (counting from 1)
     * @param columnHf H<sub>f</sub> column (counting from 1)
     * @param scale scaling factor for astronomical amplitude
     */
    public AstronomicalAmplitudeReader(final String supportedNames, final int columns,
                                       final int columnDoodson, final int columnHf,
                                       final double scale) {

        // build the pattern for the regular data lines
        final StringBuilder builder = new StringBuilder("^\\p{Space}*");
        for (int i = 1; i <= columns; ++i) {
            builder.append("(");
            if (i == columnDoodson) {
                builder.append(DOODSON_TYPE_PATTERN);
            } else if (i == columnHf) {
                builder.append(REAL_TYPE_PATTERN);
            } else {
                builder.append(OPTIONAL_FIELD_PATTERN);
            }
            builder.append(")");
            builder.append(i < FastMath.max(columnDoodson, columnHf) ? "\\p{Space}+" : "\\p{Space}*");
        }
        builder.append('$');
        this.regularLinePattern = Pattern.compile(builder.toString());

        this.supportedNames = supportedNames;
        this.columnDoodson  = columnDoodson;
        this.columnHf       = columnHf;
        this.scale          = scale;

        this.amplitudesMap  = new HashMap<Integer, Double>();

    }

    /** Get the regular expression for supported files names.
     * @return regular expression for supported files names
     */
    public String getSupportedNames() {
        return supportedNames;
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return amplitudesMap.isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input, final String name)
        throws OrekitException, IOException {

        // parse the file
        final BufferedReader r = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        int lineNumber      = 0;
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            ++lineNumber;

            try {

                // replace unicode minus sign ('−') by regular hyphen ('-') for parsing
                // such unicode characters occur in tables that are copy-pasted from PDF files
                line = line.replace('\u2212', '-');

                final Matcher regularMatcher = regularLinePattern.matcher(line);
                if (regularMatcher.matches()) {
                    // we have found a regular data line
                    final int    doodson = Integer.parseInt(regularMatcher.group(columnDoodson).replaceAll("[.,]", ""));
                    final double hf      = scale * Double.parseDouble(regularMatcher.group(columnHf));
                    amplitudesMap.put(doodson, hf);
                }

            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }

        }

        if (amplitudesMap.isEmpty()) {
            throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
        }

    }

    /** Get astronomical amplitudes map.
     * @return an unmodifiable map containing astronomical amplitudes H<sub>f</sub>
     * from a Doodson number key
     */
    public Map<Integer, Double> getAstronomicalAmplitudesMap() {
        return Collections.unmodifiableMap(amplitudesMap);
    }

}
