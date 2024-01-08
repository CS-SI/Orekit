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
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.orekit.data.DataLoader;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Base parser for Global Pressure and Temperature 2, 2w and 3 models.
 * <p>
 * The format for all models is always the same, with and example shown below
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
 * @param <G> type of the grid elements
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 12.1
 */
abstract class AbstractGptParser<G extends Grid2Entry> implements DataLoader {

    /** Pattern for delimiting regular expressions. */
    protected static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Grid entries. */
    private Grid<G> grid;

    @Override
    public boolean stillAcceptsData() {
        return grid == null;
    }

    @Override
    public void loadData(final InputStream input, final String name)
                    throws IOException {

        final SortedSet<Integer> latSample = new TreeSet<>();
        final SortedSet<Integer> lonSample = new TreeSet<>();
        final List<G>            entries   = new ArrayList<>();

        // Open stream and parse data
        int   lineNumber = 0;
        String line      = null;
        try (InputStreamReader isr = new InputStreamReader(input, StandardCharsets.UTF_8);
                        BufferedReader    br = new BufferedReader(isr)) {

            for (line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;
                line = line.trim();

                // read grid data
                if (line.length() > 0 && !line.startsWith("%")) {
                    final G entry = parseEntry(line, lineNumber, name);
                    latSample.add(entry.getLatKey());
                    lonSample.add(entry.getLonKey());
                    entries.add(entry);
                }

            }
        } catch (NumberFormatException nfe) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, name, line);
        }

        // organize entries in a grid that wraps arouns Earth in longitude
        grid = new Grid<>(latSample, lonSample, entries, name);

    }

    /** Parse one entry in the grid file.
     * @param line grid line
     * @param lineNumber line number
     * @param name file name
     * @param grid entry
     */
    protected abstract G parseEntry(final String line, final int lineNumber, final String name);

    /** Create a seasonal model.
     * @param fields parsed fields
     * @param first index of the constant field
     * @return created model
     */
    protected SeasonalModel createModel(final String[] fields, final int first) {
        return new SeasonalModel(Double.parseDouble(fields[first    ]),
                                 Double.parseDouble(fields[first + 1]),
                                 Double.parseDouble(fields[first + 2]),
                                 Double.parseDouble(fields[first + 3]),
                                 Double.parseDouble(fields[first + 4]));
    }

    /** Get the parsed grid.
     * @return parsed grid
     */
    public Grid<G> getGrid() {
        return grid;
    }

}
