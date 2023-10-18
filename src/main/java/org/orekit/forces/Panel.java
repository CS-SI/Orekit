/* Copyright 2002-2023 Luc Maisonobe
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
package org.orekit.forces;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;

/** Base class representing one panel of a satellite.
 * @see FixedPanel
 * @see PointingPanel
 * @see SlewingPanel
 * @author Luc Maisonobe
 * @since 3.0
 */
public abstract class Panel {

    /** Area in m². */
    private final double area;

    /** Indicator for double-sided panels (typically solar arrays). */
    private final boolean doubleSided;

    /** Drag coefficient. */
    private final double drag;

    /** Drag lift ratio. */
    private final double liftRatio;

    /** Radiation pressure absorption coefficient. */
    private final double absorption;

    /** Radiation pressure specular reflection coefficient. */
    private final double reflection;

    /** Simple constructor.
     * <p>
     * As the sum of absorption coefficient, specular reflection coefficient and
     * diffuse reflection coefficient is exactly 1, only the first two coefficients
     * are needed here, the third one is deduced from the other ones.
     * </p>
     * @param area panel area in m²
     * @param doubleSided if true, the panel is double-sided (typically solar arrays),
     * otherwise it is the side of a box and only relevant for flux coming from its
     * positive normal
     * @param drag drag coefficient
     * @param liftRatio drag lift ratio (proportion between 0 and 1 of atmosphere modecules
     * that will experience specular reflection when hitting spacecraft instead
     * of experiencing diffuse reflection, hence producing lift)
     * @param absorption radiation pressure absorption coefficient (between 0 and 1)
     * @param reflection radiation pressure specular reflection coefficient (between 0 and 1)
     */
    protected Panel(final double area, final boolean doubleSided,
                    final double drag, final double liftRatio,
                    final double absorption, final double reflection) {
        this.area        = area;
        this.doubleSided = doubleSided;
        this.drag        = drag;
        this.liftRatio   = liftRatio;
        this.absorption  = absorption;
        this.reflection  = reflection;
    }

    /** Get panel area.
     * @return panel area
     */
    public double getArea() {
        return area;
    }

    /** Check if the panel is double-sided (typically solar arrays).
     * @return true if panel is double-sided
     */
    public boolean isDoubleSided() {
        return doubleSided;
    }

    /** Get drag coefficient.
     * @return drag coefficient
     */
    public double getDrag() {
        return drag;
    }

    /** Get drag lift ratio.
     * @return drag lift ratio
     */
    public double getLiftRatio() {
        return liftRatio;
    }

    /** Get radiation pressure absorption coefficient.
     * @return radiation pressure absorption coefficient
     */
    public double getAbsorption() {
        return absorption;
    }

    /** Get radiation pressure specular reflection coefficient.
     * @return radiation pressure specular reflection coefficient
     */
    public double getReflection() {
        return reflection;
    }

    /** Get panel normal in spacecraft frame.
     * @param state current spacecraft state
     * @return panel normal in spacecraft frame
     */
    public abstract Vector3D getNormal(SpacecraftState state);

    /** Get panel normal in spacecraft frame.
     * @param <T> type of the field elements
     * @param state current spacecraft state
     * @return panel normal in spacecraft frame
     */
    public abstract <T extends CalculusFieldElement<T>> FieldVector3D<T> getNormal(FieldSpacecraftState<T> state);

}
