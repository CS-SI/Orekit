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

/** Class representing one panel of a satellite, fixed with respect to satellite body.
 * <p>
 * It is mainly used to represent one facet of the body of the satellite.
 * </p>
 *
 * @author Luc Maisonobe
 * @since 3.0
 */
public class FixedPanel extends Panel {

    /** Unit Normal vector, pointing outward. */
    private final Vector3D normal;

    /** Simple constructor.
     * <p>
     * As the sum of absorption coefficient, specular reflection coefficient and
     * diffuse reflection coefficient is exactly 1, only the first two coefficients
     * are needed here, the third one is deduced from the other ones.
     * </p>
     * @param normal vector normal to the panel in spacecraft frame, pointing outward (will be normalized)
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
    public FixedPanel(final Vector3D normal, final double area, final boolean doubleSided,
                      final double drag, final double liftRatio,
                      final double absorption, final double reflection) {
        super(area, doubleSided, drag, liftRatio, absorption, reflection);
        this.normal = normal.normalize();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getNormal(final SpacecraftState state) {
        return normal;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getNormal(final FieldSpacecraftState<T> state) {
        return new FieldVector3D<>(state.getDate().getField(), normal);
    }

}
