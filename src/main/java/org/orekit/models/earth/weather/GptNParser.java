/* Copyright 2002-2024 CS GROUP
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
package org.orekit.models.earth.weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataLoader;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Base parser for Global Pressure and Temperature 2, 2w and 3 models.
 * <p>
 * The format for all models is always the same, with an example shown below
 * for the pressure and the temperature. The "GPT2w" model (w stands for wet)
 * also provides humidity parameters and the "GPT3" model also provides horizontal
 * gradient, so the number of columns vary depending on the model.
 * <p>
 * Example:
 * </p>
 * <pre>
 * %  lat    lon   p:a0    A1   B1   A2   B2  T:a0    A1   B1   A2   B2
 *   87.5    2.5 101421    21  409 -217 -122 259.2 -13.2 -6.1  2.6  0.3
 *   87.5    7.5 101416    21  411 -213 -120 259.3 -13.1 -6.1  2.6  0.3
 *   87.5   12.5 101411    22  413 -209 -118 259.3 -13.1 -6.1  2.6  0.3
 *   87.5   17.5 101407    23  415 -205 -116 259.4 -13.0 -6.1  2.6  0.3
 *   ...
 * </pre>
 *
 * @see "K. Lagler, M. Schindelegger, J. Böhm, H. Krasna, T. Nilsson (2013),
 * GPT2: empirical slant delay model for radio space geodetic techniques. Geophys
 * Res Lett 40(6):1069–1073. doi:10.1002/grl.50288"
 *
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 12.1
 */
class GptNParser implements DataLoader {

    /** Comment prefix. */
    private static final String COMMENT = "%";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Label for latitude field. */
    private static final String LATITUDE_LABEL = "lat";

    /** Label for longitude field. */
    private static final String LONGITUDE_LABEL = "lon";

    /** Label for undulation field. */
    private static final String UNDULATION_LABEL = "undu";

    /** Label for height correction field. */
    private static final String HEIGHT_CORRECTION_LABEL = "Hs";

    /** Label for annual cosine amplitude field. */
    private static final String A1 = "A1";

    /** Label for annual sine amplitude field. */
    private static final String B1 = "B1";

    /** Label for semi-annual cosine amplitude field. */
    private static final String A2 = "A2";

    /** Label for semi-annual sine amplitude field. */
    private static final String B2 = "B2";

    /** Expected seasonal models types. */
    private final SeasonalModelType[] expected;

    /** Index for latitude field. */
    private int latitudeIndex;

    /** Index for longitude field. */
    private int longitudeIndex;

    /** Index for undulation field. */
    private int undulationIndex;

    /** Index for height correction field. */
    private int heightCorrectionIndex;

    /** Maximum index. */
    private int maxIndex;

    /** Indices for expected seasonal models types field. */
    private final int[] expectedIndices;

    /** Grid entries. */
    private Grid grid;

    /** Simple constructor.
     * @param expected expected seasonal models types
     */
    GptNParser(final SeasonalModelType... expected) {
        this.expected        = expected.clone();
        this.expectedIndices = new int[expected.length];
    }

    @Override
    public boolean stillAcceptsData() {
        return grid == null;
    }

    @Override
    public void loadData(final InputStream input, final String name) throws IOException {

        final SortedSet<Integer> latSample = new TreeSet<>();
        final SortedSet<Integer> lonSample = new TreeSet<>();
        final List<GridEntry>    entries   = new ArrayList<>();

        // Open stream and parse data
        try (InputStreamReader isr = new InputStreamReader(input, StandardCharsets.UTF_8);
             BufferedReader    br  = new BufferedReader(isr)) {
            int     lineNumber = 0;
            String  line;
            for (line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;
                line = line.trim();
                if (lineNumber == 1) {
                    // read header and store columns numbers
                    parseHeader(line, lineNumber, name);
                } else if (!line.isEmpty()) {
                    // read grid data
                    final GridEntry entry = parseEntry(line, lineNumber, name);
                    latSample.add(entry.getLatKey());
                    lonSample.add(entry.getLonKey());
                    entries.add(entry);
                }

            }
        }

        // organize entries in a grid that wraps around Earth in longitude
        grid = new Grid(latSample, lonSample, entries, name);

    }

    /** Parse header line in the grid file.
     * @param line grid line
     * @param lineNumber line number
     * @param name file name
     */
    private void parseHeader(final String line, final int lineNumber, final String name) {

        // reset indices
        latitudeIndex         = -1;
        longitudeIndex        = -1;
        undulationIndex       = -1;
        heightCorrectionIndex = -1;
        maxIndex              = -1;
        Arrays.fill(expectedIndices, -1);

        final String[] fields = SEPARATOR.split(line.substring(COMMENT.length()).trim());
        String lookingFor = LATITUDE_LABEL;
        for (int i = 0; i < fields.length; ++i) {
            maxIndex = FastMath.max(maxIndex, i);
            checkLabel(fields[i], lookingFor, line, lineNumber, name);
            switch (fields[i]) {
                case LATITUDE_LABEL :
                    latitudeIndex = i;
                    lookingFor = LONGITUDE_LABEL;
                    break;
                case LONGITUDE_LABEL :
                    lookingFor = null;
                    longitudeIndex = i;
                    break;
                case UNDULATION_LABEL :
                    lookingFor = HEIGHT_CORRECTION_LABEL;
                    undulationIndex = i;
                    break;
                case HEIGHT_CORRECTION_LABEL :
                    lookingFor = null;
                    heightCorrectionIndex = i;
                    break;
                case A1 :
                    lookingFor = B1;
                    break;
                case B1 :
                    lookingFor = A2;
                    break;
                case A2 :
                    lookingFor = B2;
                    break;
                case B2 :
                    lookingFor = null;
                    break;
                default : {
                    final SeasonalModelType type = SeasonalModelType.parseType(fields[i]);
                    for (int j = 0; j < expected.length; ++j) {
                        if (type == expected[j]) {
                            expectedIndices[j] = i;
                            lookingFor = A1;
                            break;
                        }
                    }
                }
            }
        }

        // check all indices have been set
        int minIndex = FastMath.min(latitudeIndex,
                                    FastMath.min(longitudeIndex,
                                                 FastMath.min(undulationIndex,
                                                              heightCorrectionIndex)));
        for (int index : expectedIndices) {
            minIndex = FastMath.min(minIndex, index);
        }
        if (minIndex < 0) {
            // some indices in the header are missing
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, name, line);
        }

    }

    /** Check if header label is what we are looking for.
     * @param label label to check
     * @param lookingFor label we are looking for, or null if we don't known what to expect
     * @param line grid line
     * @param lineNumber line number
     * @param name file name
     */
    private void checkLabel(final String label, final String lookingFor,
                            final String line, final int lineNumber, final String name) {
        if (lookingFor != null && !lookingFor.equals(label)) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, name, line);
        }
    }

    /** Parse one entry in the grid file.
     * @param line grid line
     * @param lineNumber line number
     * @param name file name
     * @return parsed entry
     */
    private GridEntry parseEntry(final String line, final int lineNumber, final String name) {
        try {

            final String[] fields = SEPARATOR.split(line);
            if (fields.length != maxIndex + 1) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }

            final double latDegree = Double.parseDouble(fields[latitudeIndex]);
            final double lonDegree = Double.parseDouble(fields[longitudeIndex]);

            final Map<SeasonalModelType, SeasonalModel> models = new HashMap<>(expected.length);
            for (int i = 0; i < expected.length; ++i) {
                final int first = expectedIndices[i];
                models.put(expected[i], new SeasonalModel(Double.parseDouble(fields[first    ]),
                                                          Double.parseDouble(fields[first + 1]),
                                                          Double.parseDouble(fields[first + 2]),
                                                          Double.parseDouble(fields[first + 3]),
                                                          Double.parseDouble(fields[first + 4])));
            }

            return new GridEntry(FastMath.toRadians(latDegree),
                                 (int) FastMath.rint(latDegree * GridEntry.DEG_TO_MAS),
                                 FastMath.toRadians(lonDegree),
                                 (int) FastMath.rint(lonDegree * GridEntry.DEG_TO_MAS),
                                 Double.parseDouble(fields[undulationIndex]),
                                 Double.parseDouble(fields[heightCorrectionIndex]),
                                 models);

        } catch (NumberFormatException nfe) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, name, line);
        }
    }

    /** Get the parsed grid.
     * @return parsed grid
     */
    public Grid getGrid() {
        return grid;
    }

}
