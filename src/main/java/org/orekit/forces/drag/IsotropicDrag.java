/* Copyright 2002-2026 CS GROUP
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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterval;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversSequence;

/** This class models isotropic drag effects.
 * <p>
 * The model of this spacecraft is a simple spherical model, this
 * means that all coefficients are constant and do not depend on
 * the direction.
 * </p>
 * <p>
 * Since 14.0, this model needs to be built using {@link IsotropicDragBuilder}
 * </p>
 * @see IsotropicDragBuilder
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
    private static final double SCALE = FastMath.scalb(1.0, -3);

    /** Cross section (m²). */
    private final double crossSection;

    /** Drivers for drag coefficients valid on specified time spans. */
    private final ParameterDriversSequence timeSpanDrivers;

    /** Drivers for drag coefficient parameter. */
    private final List<ParameterDriver> dragParametersDrivers;

    /** Simple constructor.
     * @param crossSection Surface (m²)
     * @param timeSpanDrivers drivers for drag coefficients valid on specified time spans
     */
    IsotropicDrag(final double crossSection, final ParameterDriversSequence timeSpanDrivers) {

        this.crossSection    = crossSection;
        this.timeSpanDrivers = timeSpanDrivers;

        // prepare drivers, one for the global coefficient and one for each time span
        dragParametersDrivers = new ArrayList<>(1 + timeSpanDrivers.getParametersDrivers().size());

        // global driver
        dragParametersDrivers.add(new ParameterDriver(DragSensitive.GLOBAL_DRAG_FACTOR,
                                                      1.0, SCALE,
                                                      0.0, Double.POSITIVE_INFINITY, TimeInterval.UNLIMITED));

        // time span drivers
        dragParametersDrivers.addAll(timeSpanDrivers.getParametersDrivers());

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getDragParametersDrivers() {
        return Collections.unmodifiableList(dragParametersDrivers);
    }

    /** Get the drag coefficient driver that is active at date.
     * @param date date to check
     * @return drag coefficient driver active at this date
     * @since 14.0
     */
    public ParameterDriver getActiveDriver(final AbsoluteDate date) {
        return timeSpanDrivers.getActiveDriver(date);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D dragAcceleration(final SpacecraftState state,
                                     final double density, final Vector3D relativeVelocity,
                                     final double[] parameters) {
        final int index = 1 + timeSpanDrivers.getActiveDriverIndex(state.getDate());
        final double dragCoeff = parameters[0] * parameters[index];
        return new Vector3D(relativeVelocity.getNorm() * density * dragCoeff * crossSection / (2 * state.getMass()),
                            relativeVelocity);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T>
        dragAcceleration(final FieldSpacecraftState<T> state, final T density,
                         final FieldVector3D<T> relativeVelocity,
                         final T[] parameters) {
        final int index = 1 + timeSpanDrivers.getActiveDriverIndex(state.getDate().toAbsoluteDate());
        final T dragCoeff = parameters[0].multiply(parameters[index]);
        return new FieldVector3D<>(relativeVelocity.getNorm().
                                   multiply(density.multiply(dragCoeff).multiply(crossSection / 2)).
                                   divide(state.getMass()),
                                   relativeVelocity);
    }

}
