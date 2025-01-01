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

/** Holder for one cell of grid data surrounding one point.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
class FieldGridCell<T extends CalculusFieldElement<T>> {

    /** Latitude difference with respect to South cell edge. */
    private final T deltaSouth;

    /** Longitude difference with respect to West cell edge. */
    private final T deltaWest;

    /** Cell size in latitude. */
    private final double sizeLat;

    /** Cell size in longitude. */
    private final double sizeLon;

    /** North-West value. */
    private final T nw;

    /** South-West value. */
    private final T sw;

    /** South-East value. */
    private final T se;

    /** North-East value. */
    private final T ne;

    /**
     * Build a grid cell from corner data.
     *
     * @param deltaSouth point latitude minus South cell edge latitude
     * @param deltaWest  point longitude minus West cell edge longitude
     * @param sizeLat    cell size in latitude
     * @param sizeLon    cell size in longitude
     * @param nw         North-West value
     * @param sw         South-West value
     * @param se         South-East value
     * @param ne         North-East value
     */
    FieldGridCell(final T deltaSouth, final T deltaWest, final double sizeLat, final double sizeLon,
                  final T nw, final T sw, final T se, final T ne) {
        this.deltaSouth = deltaSouth;
        this.deltaWest  = deltaWest;
        this.sizeLat    = sizeLat;
        this.sizeLon    = sizeLon;
        this.nw         = nw;
        this.sw         = sw;
        this.se         = se;
        this.ne         = ne;
    }

    /** Build a grid cell by applying a function to two existing cells.
     * <p>
     * The cells are expected to be consistent (i.e. same locations,
     * same sizes), but no verification is done here. It works in
     * the context of ITR-R P.834 because the grids have similar
     * samplings, it would not work fro general and inconsistent grids.
     * </p>
     * @param function function to apply to all cells corners
     * @param cell1 first cell
     * @param cell2 second cell
     */
    FieldGridCell(final BiFunction<T> function,
                  final FieldGridCell<T> cell1, final FieldGridCell<T> cell2) {
        this.deltaSouth = cell1.deltaSouth;
        this.deltaWest  = cell1.deltaWest;
        this.sizeLat    = cell1.sizeLat;
        this.sizeLon    = cell1.sizeLon;
        this.nw         = function.apply(cell1.nw, cell2.nw);
        this.sw         = function.apply(cell1.sw, cell2.sw);
        this.se         = function.apply(cell1.se, cell2.se);
        this.ne         = function.apply(cell1.ne, cell2.ne);
    }

    /** Build a grid cell by applying a function to three existing cells.
     * <p>
     * The cells are expected to be consistent (i.e. same locations,
     * same sizes), but no verification is done here. It works in
     * the context of ITR-R P.834 because the grids have similar
     * samplings, it would not work fro general and inconsistent grids.
     * </p>
     * @param function function to apply to all cells corners
     * @param cell1 first cell
     * @param cell2 second cell
     * @param cell3 third cell
     */
    FieldGridCell(final TriFunction<T> function,
                  final FieldGridCell<T> cell1, final FieldGridCell<T> cell2, final FieldGridCell<T> cell3) {
        this.deltaSouth = cell1.deltaSouth;
        this.deltaWest  = cell1.deltaWest;
        this.sizeLat    = cell1.sizeLat;
        this.sizeLon    = cell1.sizeLon;
        this.nw         = function.apply(cell1.nw, cell2.nw, cell3.nw);
        this.sw         = function.apply(cell1.sw, cell2.sw, cell3.sw);
        this.se         = function.apply(cell1.se, cell2.se, cell3.se);
        this.ne         = function.apply(cell1.ne, cell2.ne, cell3.ne);
    }

    /** Build a grid cell by applying a function to four existing cells.
     * <p>
     * The cells are expected to be consistent (i.e. same locations,
     * same sizes), but no verification is done here. It works in
     * the context of ITR-R P.834 because the grids have similar
     * samplings, it would not work fro general and inconsistent grids.
     * </p>
     * @param function function to apply to all cells corners
     * @param cell1 first cell
     * @param cell2 second cell
     * @param cell3 third cell
     * @param cell4 fourth cell
     */
    FieldGridCell(final QuarticFunction<T> function,
                  final FieldGridCell<T> cell1, final FieldGridCell<T> cell2,
                  final FieldGridCell<T> cell3, final FieldGridCell<T> cell4) {
        this.deltaSouth = cell1.deltaSouth;
        this.deltaWest  = cell1.deltaWest;
        this.sizeLat    = cell1.sizeLat;
        this.sizeLon    = cell1.sizeLon;
        this.nw         = function.apply(cell1.nw, cell2.nw, cell3.nw, cell4.nw);
        this.sw         = function.apply(cell1.sw, cell2.sw, cell3.sw, cell4.sw);
        this.se         = function.apply(cell1.se, cell2.se, cell3.se, cell4.se);
        this.ne         = function.apply(cell1.ne, cell2.ne, cell3.ne, cell4.ne);
    }

    /** Build a grid cell by applying a function to five existing cells.
     * <p>
     * The cells are expected to be consistent (i.e. same locations,
     * same sizes), but no verification is done here. It works in
     * the context of ITR-R P.834 because the grids have similar
     * samplings, it would not work fro general and inconsistent grids.
     * </p>
     * @param function function to apply to all cells corners
     * @param cell1 first cell
     * @param cell2 second cell
     * @param cell3 third cell
     * @param cell4 fourth cell
     * @param cell5 fifth cell
     */
    FieldGridCell(final QuinticFunction<T> function,
                  final FieldGridCell<T> cell1, final FieldGridCell<T> cell2, final FieldGridCell<T> cell3,
                  final FieldGridCell<T> cell4, final FieldGridCell<T> cell5) {
        this.deltaSouth = cell1.deltaSouth;
        this.deltaWest  = cell1.deltaWest;
        this.sizeLat    = cell1.sizeLat;
        this.sizeLon    = cell1.sizeLon;
        this.nw         = function.apply(cell1.nw, cell2.nw, cell3.nw, cell4.nw, cell5.nw);
        this.sw         = function.apply(cell1.sw, cell2.sw, cell3.sw, cell4.sw, cell5.sw);
        this.se         = function.apply(cell1.se, cell2.se, cell3.se, cell4.se, cell5.se);
        this.ne         = function.apply(cell1.ne, cell2.ne, cell3.ne, cell4.ne, cell5.ne);
    }

    /** Evaluate cell value at point location using bi-linear interpolation.
     * @return cell value at point location
     */
    public T evaluate() {
        final T deltaNorth = deltaSouth.negate().add(sizeLat);
        final T deltaEast  = deltaWest.negate().add(sizeLon);
        return     deltaSouth.multiply(deltaWest.multiply(ne).add(deltaEast.multiply(nw))).
               add(deltaNorth.multiply(deltaWest.multiply(se).add(deltaEast.multiply(sw)))).
               divide(sizeLat * sizeLon);
    }

    /** Interface for function that can be applied to the corners of two cells. */
    @FunctionalInterface
    public interface BiFunction<T extends CalculusFieldElement<T>> {
        /** Apply function to similar corners coming from two cells.
         * @param corner1 value at corner of first cell
         * @param corner2 value at corner of second cell
         * @return function evaluated at similar corners of two cells
         */
        T apply(T corner1, T corner2);
    }

    /** Interface for function that can be applied to the corners of three cells.
     * @param <T> type of the field elements
     */
    @FunctionalInterface
    public interface TriFunction<T extends CalculusFieldElement<T>> {
        /** Apply function to similar corners coming from three cells.
         * @param corner1 value at corner of first cell
         * @param corner2 value at corner of second cell
         * @param corner3 value at corner of third cell
         * @return function evaluated at similar corners of three cells
         */
        T apply(T corner1, T corner2, T corner3);
    }

    /** Interface for function that can be applied to the corners of four cells.
     * @param <T> type of the field elements
     */
    @FunctionalInterface
    public interface QuarticFunction<T extends CalculusFieldElement<T>> {
        /** Apply function to similar corners coming from four cells.
         * @param corner1 value at corner of first cell
         * @param corner2 value at corner of second cell
         * @param corner3 value at corner of third cell
         * @param corner4 value at corner of fourth cell
         * @return function evaluated at similar corners of four cells
         */
        T apply(T corner1, T corner2, T corner3, T corner4);
    }

    /** Interface for function that can be applied to the corners of five cells.
     * @param <T> type of the field elements
     */
    @FunctionalInterface
    public interface QuinticFunction<T extends CalculusFieldElement<T>> {
        /** Apply function to similar corners coming from five cells.
         * @param corner1 value at corner of first cell
         * @param corner2 value at corner of second cell
         * @param corner3 value at corner of third cell
         * @param corner4 value at corner of fourth cell
         * @param corner5 value at corner of fifth cell
         * @return function evaluated at similar corners of five cells
         */
        T apply(T corner1, T corner2, T corner3, T corner4, T corner5);
    }

}
