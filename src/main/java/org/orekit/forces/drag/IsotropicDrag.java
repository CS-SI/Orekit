/* Copyright 2002-2023 CS GROUP
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
package org.orekit.forces.drag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;

/** This class models isotropic drag effects.
 * <p>The model of this spacecraft is a simple spherical model, this
 * means that all coefficients are constant and do not depend of
 * the direction.</p>
 *
 * @see org.orekit.forces.BoxAndSolarArraySpacecraft
 * @see org.orekit.forces.radiation.IsotropicRadiationCNES95Convention
 * @author Luc Maisonobe
 * @since 7.1
 */
public class IsotropicDrag implements DragSensitive {

    /** Parameters scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private final double SCALE = FastMath.scalb(1.0, -3);

    /** Drivers for drag coefficient parameter. */
    private final List<ParameterDriver> dragParametersDrivers;

    /** Cross section (m²). */
    private final double crossSection;

    /** Constructor with drag coefficient min/max set to ±∞.
     * @param crossSection Surface (m²)
     * @param dragCoeff drag coefficient
     */
    public IsotropicDrag(final double crossSection, final double dragCoeff) {
        this(crossSection, dragCoeff, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /** Constructor with drag coefficient min/max set by user.
     * @param crossSection Surface (m²)
     * @param dragCoeff drag coefficient
     * @param dragCoeffMin Minimum value of drag coefficient
     * @param dragCoeffMax Maximum value of drag coefficient
     */
    public IsotropicDrag(final double crossSection, final double dragCoeff,
                         final double dragCoeffMin, final double dragCoeffMax) {
        // in some corner cases (unknown spacecraft, fuel leaks, active piloting ...)
        // the single coefficient may be arbitrary, and even negative
        // the DRAG_COEFFICIENT parameter should be sufficient, but GLOBAL_DRAG_FACTOR
        // was added as of 12.0 for consistency with BoxAndSolarArraySpacecraft
        // that only has a global multiplicatof factor, hence allowing this name
        // to be used for both models
        this.dragParametersDrivers = new ArrayList<>(2);
        dragParametersDrivers.add(new ParameterDriver(DragSensitive.GLOBAL_DRAG_FACTOR,
                                                      1.0, SCALE,
                                                      0.0, Double.POSITIVE_INFINITY));
        dragParametersDrivers.add(new ParameterDriver(DragSensitive.DRAG_COEFFICIENT,
                                                      dragCoeff, SCALE,
                                                      dragCoeffMin, dragCoeffMax));
        this.crossSection = crossSection;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getDragParametersDrivers() {
        return Collections.unmodifiableList(dragParametersDrivers);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D dragAcceleration(final SpacecraftState state,
                                     final double density, final Vector3D relativeVelocity,
                                     final double[] parameters) {
        final double dragCoeff = parameters[0] * parameters[1];
        return new Vector3D(relativeVelocity.getNorm() * density * dragCoeff * crossSection / (2 * state.getMass()),
                            relativeVelocity);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T>
        dragAcceleration(final FieldSpacecraftState<T> state, final T density,
                         final FieldVector3D<T> relativeVelocity,
                         final T[] parameters) {
        final T dragCoeff = parameters[0].multiply(parameters[1]);
        return new FieldVector3D<>(relativeVelocity.getNorm().
                                   multiply(density.multiply(dragCoeff).multiply(crossSection / 2)).
                                   divide(state.getMass()),
                                   relativeVelocity);
    }
}
