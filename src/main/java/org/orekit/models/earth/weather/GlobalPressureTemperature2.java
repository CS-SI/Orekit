/* Copyright 2002-2024 Thales Alenia Space
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

import java.io.IOException;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.TimeScale;

/** The Global Pressure and Temperature 2 (GPT2) model.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class GlobalPressureTemperature2 extends AbstractGlobalPressureTemperature<Grid2Entry> {

    /**
     * Constructor with supported names and source of GPT2 auxiliary data given by user.
     *
     * @param source grid data source
     * @param utc UTC time scale.
     * @exception IOException if grid data cannot be read
     */
    public GlobalPressureTemperature2(final DataSource source, final TimeScale utc)
        throws IOException {
        super(source, new GPT2Parser(), utc);
    }

    /**
     * Constructor with supported names and source of GPT2 auxiliary data given by user.
     *
     * @param supportedNames supported names
     * @param dataProvidersManager provides access to auxiliary data.
     * @param utc UTC time scale.
     * @deprecated as of 12.1 used only by {@link GlobalPressureTemperature2Model}
     */
    @Deprecated
    protected GlobalPressureTemperature2(final String supportedNames,
                                         final DataProvidersManager dataProvidersManager,
                                         final TimeScale utc) {
        super(buildGrid(supportedNames, dataProvidersManager), utc);
    }

    /** Builder for grid provided as supported names and source of GPT2 auxiliary data given by user.
     *
     * @param supportedNames supported names
     * @param dataProvidersManager provides access to auxiliary data.
     * @deprecated as of 12.1 used only by {@link GlobalPressureTemperature2Model}
     */
    @Deprecated
    private static Grid<Grid2Entry> buildGrid(final String supportedNames,
                                              final DataProvidersManager dataProvidersManager) {
        final GPT2Parser parser = new GPT2Parser();
        dataProvidersManager.feed(supportedNames, parser);
        return parser.getGrid();
    }

    /** Parser for GPT2 grid files. */
    private static class GPT2Parser extends AbstractGptParser<Grid2Entry> {

        /** {@inheritDoc} */
        @Override
        protected Grid2Entry parseEntry(final String line, final int lineNumber, final String name) {

            try {
                final String[] fields = SEPARATOR.split(line);
                final double latDegree = Double.parseDouble(fields[0]);
                final double lonDegree = Double.parseDouble(fields[1]);
                return new Grid2Entry(FastMath.toRadians(latDegree),
                                      (int) FastMath.rint(latDegree * GridEntry.DEG_TO_MAS),
                                      FastMath.toRadians(lonDegree),
                                      (int) FastMath.rint(lonDegree * GridEntry.DEG_TO_MAS),
                                      Double.parseDouble(fields[22]),
                                      Double.parseDouble(fields[23]),
                                      createModel(fields,  2),
                                      createModel(fields,  7),
                                      createModel(fields, 12),
                                      createModel(fields, 17),
                                      createModel(fields, 24),
                                      createModel(fields, 29));
            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }

        }

    }

}
