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
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.TimeScale;

/** The Global Pressure and Temperature 2w (GPT2) model.
 * <p>
 * This model adds humidity data to {@link GlobalPressureTemperature2 GPT2}.
 * </p>
 * @author Luc Maisonobe
 * @since 12.1
 */
public class GlobalPressureTemperature2w extends AbstractGlobalPressureTemperature<Grid2wEntry> {

    /**
     * Constructor with supported names and source of GPT2w auxiliary data given by user.
     *
     * @param source grid data source
     * @param utc UTC time scale.
     * @exception IOException if grid data cannot be read
     */
    public GlobalPressureTemperature2w(final DataSource source, final TimeScale utc)
        throws IOException {
        super(source, new GPT2wParser(), utc);
    }

    /** Parser for GPT2w grid files. */
    private static class GPT2wParser extends AbstractGptParser<Grid2wEntry> {

        /** {@inheritDoc} */
        @Override
        protected Grid2wEntry parseEntry(final String line, final int lineNumber, final String name) {

            try {
                final String[] fields = SEPARATOR.split(line);
                final double latDegree = Double.parseDouble(fields[0]);
                final double lonDegree = Double.parseDouble(fields[1]);
                return new Grid2wEntry(FastMath.toRadians(latDegree),
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
                                       createModel(fields, 29),
                                       createModel(fields, 34),
                                       createModel(fields, 39));
            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }

        }

    }

}
