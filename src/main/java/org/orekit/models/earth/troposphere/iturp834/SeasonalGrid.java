/* Copyright 2002-2024 Thales Alenia Space
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth.troposphere.iturp834;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.utils.Constants;
import org.orekit.utils.units.Unit;

/** Grid data with harmonic seasonal fluctuation.
 * @author Luc Maisonobe
 * @since 13.0
 */
class SeasonalGrid extends AbstractGrid {

    /** Annual pulsation. */
    private static final double OMEGA = MathUtils.TWO_PI / Constants.JULIAN_YEAR;

    /** Average data. */
    private final double[][] average;

    /** Seasonal fluctuation data. */
    private final double[][] seasonal;

    /** Second of minimum data. */
    private final double[][] minSecond;

    /** Build a grid by parsing three resource files.
     * @param unit unit of the average and seasonal fluctuation values in resource files
     * @param averageName name of the resource holding the average data
     * @param seasonalName name of the resource holding the seasonal fluctuation data
     * @param minDayName name of the resource holding the day of minimum data
     */
    public SeasonalGrid(final Unit unit,
                        final String averageName, final String seasonalName, final String minDayName) {
        average   = parse(unit,     averageName);
        seasonal  = parse(unit,     seasonalName);
        // we convert from days to SI units (i.e. seconds) upon reading
        minSecond = parse(Unit.DAY, minDayName);
    }

    /** {@inheritDoc} */
    @Override
    public GridCell getCell(final GeodeticPoint location, final double secondOfYear) {
        return new GridCell((a, s, m) -> a - s * OMEGA * (secondOfYear - m),
                            getRawCell(location, average),
                            getRawCell(location, seasonal),
                            getRawCell(location, minSecond));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldGridCell<T> getCell(final FieldGeodeticPoint<T> location,
                                                                        final T secondOfYear) {
        return new FieldGridCell<>((a, s, m) -> a.subtract(s.multiply(OMEGA).multiply(secondOfYear.subtract(m))),
                                   getRawCell(location, average),
                                   getRawCell(location, seasonal),
                                   getRawCell(location, minSecond));
    }

}
