/* Copyright 2002-2024 CS GROUP
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

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;


/** Atmospheric drag force model.
 *
 * The drag acceleration is computed as follows :
 *
 * γ = (1/2 * ρ * V² * S / Mass) * DragCoefVector
 *
 * With DragCoefVector = {C<sub>x</sub>, C<sub>y</sub>, C<sub>z</sub>} and S given by the user through the interface
 * {@link DragSensitive}
 *
 * @author &Eacute;douard Delente
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Pascal Parraud
 * @author Melina Vanel
 */

public class DragForce extends AbstractDragForceModel {

    /** Spacecraft. */
    private final DragSensitive spacecraft;

    /** Constructor with default flag for finite differences.
     * @param atmosphere atmospheric model
     * @param spacecraft the object physical and geometrical information
     */
    public DragForce(final Atmosphere atmosphere, final DragSensitive spacecraft) {
        super(atmosphere);
        this.spacecraft = spacecraft;
    }

    /** Simple constructor.
     * @param atmosphere atmospheric model
     * @param spacecraft the object physical and geometrical information
     * @param useFiniteDifferencesOnDensityWrtPosition flag to use finite differences to compute density derivatives w.r.t.
     *                                                 position (is less accurate but can be faster depending on model)
     * @since 12.1
     */
    public DragForce(final Atmosphere atmosphere, final DragSensitive spacecraft,
                     final boolean useFiniteDifferencesOnDensityWrtPosition) {
        super(atmosphere, useFiniteDifferencesOnDensityWrtPosition);
        this.spacecraft = spacecraft;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnAttitudeRate() {
        return getSpacecraft().dependsOnAttitudeRate();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        final AbsoluteDate date     = s.getDate();
        final Frame        frame    = s.getFrame();
        final Vector3D     position = s.getPosition();

        final double rho    = getAtmosphere().getDensity(date, position, frame);
        final Vector3D vAtm = getAtmosphere().getVelocity(date, position, frame);
        final Vector3D relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        return spacecraft.dragAcceleration(s, rho, relativeVelocity, parameters);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                             final T[] parameters) {
        // Density and its derivatives
        final T rho = getFieldDensity(s);

        // Spacecraft relative velocity with respect to the atmosphere
        final FieldAbsoluteDate<T> date     = s.getDate();
        final Frame                frame    = s.getFrame();
        final FieldVector3D<T>     position = s.getPosition();
        final FieldVector3D<T> vAtm = getAtmosphere().getVelocity(date, position, frame);
        final FieldVector3D<T> relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        // Drag acceleration along with its derivatives
        return spacecraft.dragAcceleration(s, rho, relativeVelocity, parameters);

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return spacecraft.getDragParametersDrivers();
    }

    /** Get spacecraft that are sensitive to atmospheric drag forces.
     * @return drag sensitive spacecraft model
     */
    public DragSensitive getSpacecraft() {
        return spacecraft;
    }

}
