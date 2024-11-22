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

/** Holder for one cell of grid data surrounding one point.
 * @author Luc Maisonobe
 * @since 13.0
 */
class GridCell {

    /** Latitude difference with respect to South cell edge. */
    private final double deltaSouth;

    /** Longitude difference with respect to West cell edge. */
    private final double deltaWest;

    /** Cell size in latitude. */
    private final double sizeLat;

    /** Cell size in longitude. */
    private final double sizeLon;

    /** North-West value. */
    private final double nw;

    /** South-West value. */
    private final double sw;

    /** South-East value. */
    private final double se;

    /** North-East value. */
    private final double ne;

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
    GridCell(final double deltaSouth, final double deltaWest, final double sizeLat, final double sizeLon,
             final double nw, final double sw, final double se, final double ne) {
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
     GridCell(final BiFunction function,
              final GridCell cell1, final GridCell cell2) {
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
    GridCell(final TriFunction function,
             final GridCell cell1, final GridCell cell2, final GridCell cell3) {
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
     GridCell(final QuarticFunction function,
              final GridCell cell1, final GridCell cell2,
              final GridCell cell3, final GridCell cell4) {
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
     GridCell(final QuinticFunction function,
              final GridCell cell1, final GridCell cell2, final GridCell cell3,
              final GridCell cell4, final GridCell cell5) {
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
    public double evaluate() {
        final double deltaNorth = sizeLat - deltaSouth;
        final double deltaEast  = sizeLon - deltaWest;
        return (deltaSouth * (deltaWest * ne + deltaEast * nw) +
                deltaNorth * (deltaWest * se + deltaEast * sw)) /
               (sizeLat * sizeLon);
    }

    /** Interface for function that can be applied to the corners of two cells. */
    @FunctionalInterface
    public interface BiFunction {
        /** Apply function to similar corners coming from two cells.
         * @param corner1 value at corner of first cell
         * @param corner2 value at corner of second cell
         * @return function evaluated at similar corners of two cells
         */
        double apply(double corner1, double corner2);
    }

    /** Interface for function that can be applied to the corners of three cells. */
    @FunctionalInterface
    public interface TriFunction {
        /** Apply function to similar corners coming from three cells.
         * @param corner1 value at corner of first cell
         * @param corner2 value at corner of second cell
         * @param corner3 value at corner of third cell
         * @return function evaluated at similar corners of three cells
         */
        double apply(double corner1, double corner2, double corner3);
    }

    /** Interface for function that can be applied to the corners of four cells. */
    @FunctionalInterface
    public interface QuarticFunction {
        /** Apply function to similar corners coming from four cells.
         * @param corner1 value at corner of first cell
         * @param corner2 value at corner of second cell
         * @param corner3 value at corner of third cell
         * @param corner4 value at corner of fourth cell
         * @return function evaluated at similar corners of four cells
         */
        double apply(double corner1, double corner2, double corner3, double corner4);
    }

    /** Interface for function that can be applied to the corners of five cells. */
    @FunctionalInterface
    public interface QuinticFunction {
        /** Apply function to similar corners coming from five cells.
         * @param corner1 value at corner of first cell
         * @param corner2 value at corner of second cell
         * @param corner3 value at corner of third cell
         * @param corner4 value at corner of fourth cell
         * @param corner5 value at corner of fifth cell
         * @return function evaluated at similar corners of five cells
         */
        double apply(double corner1, double corner2, double corner3, double corner4, double corner5);
    }

}
