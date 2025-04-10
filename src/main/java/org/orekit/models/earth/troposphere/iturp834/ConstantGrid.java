/* Copyright 2022-2025 Thales Alenia Space
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
import org.hipparchus.analysis.interpolation.GridAxis;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.utils.units.Unit;

/** Constant (with respect to time) grid data.
 * @author Luc Maisonobe
 * @since 13.0
 */
class ConstantGrid extends AbstractGrid {

    /** Constant data. */
    private final double[][] data;

    /** Build a grid by parsing a resource file.
     * @param unit unit of values in resource file
     * @param name name of the resource holding the data
     */
    ConstantGrid(final Unit unit, final String name) {
        data = parse(unit, name);
    }

    /** Build a grid by parsing a resource file.
     * @param data constant data
     */
    private ConstantGrid(final double[][] data) {
        this.data = data;
    }

    /** Create a grid by applying a function to all nodes.
     * @param function function to apply to all nodes
     * @return new grid
     */
    public ConstantGrid apply(final Function function) {
        final GridAxis latitudeAxis  = getLatitudeAxis();
        final GridAxis longitudeAxis = getLongitudeAxis();
        final double[][] values = new double[latitudeAxis.size()][longitudeAxis.size()];
        for (int i = 0; i < latitudeAxis.size(); ++i) {
            final double latitude = latitudeAxis.node(i);
            for (int j = 0; j < longitudeAxis.size(); ++j) {
                values[i][j] = function.apply(latitude, longitudeAxis.node(j), data[i][j]);
            }
        }
        return new ConstantGrid(values);
    }

    /** {@inheritDoc} */
    @Override
    public GridCell getCell(final GeodeticPoint location, final double ignored) {
        return getRawCell(location, data);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldGridCell<T> getCell(final FieldGeodeticPoint<T> location,
                                                                        final T ignored) {
        return getRawCell(location, data);
    }

    /** Interface for transforming nodes data. */
    @FunctionalInterface
    public interface Function {
        /** Compute a new node data.
         * @param latitude node latitude
         * @param longitude node longitude
         * @param data node value
         * @return value for new node
         */
        double apply(double latitude, double longitude, double data);
    }

}
