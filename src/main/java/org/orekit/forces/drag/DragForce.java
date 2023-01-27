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

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
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

    /** Atmospheric model. */
    private final Atmosphere atmosphere;

    /** Spacecraft. */
    private final DragSensitive spacecraft;

    /** Simple constructor.
     * @param atmosphere atmospheric model
     * @param spacecraft the object physical and geometrical information
     */
    public DragForce(final Atmosphere atmosphere, final DragSensitive spacecraft) {
        super(atmosphere);
        this.atmosphere = atmosphere;
        this.spacecraft = spacecraft;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        final AbsoluteDate date     = s.getDate();
        final Frame        frame    = s.getFrame();
        final Vector3D     position = s.getPosition();

        final double rho    = atmosphere.getDensity(date, position, frame);
        final Vector3D vAtm = atmosphere.getVelocity(date, position, frame);
        final Vector3D relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        return spacecraft.dragAcceleration(s, rho, relativeVelocity, parameters);

    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {

        final FieldAbsoluteDate<T> date     = s.getDate();
        final Frame                frame    = s.getFrame();
        final FieldVector3D<T>     position = s.getPosition();

        // Density and its derivatives
        final T rho;

        // Check for faster computation dedicated to derivatives with respect to state
        // Using finite differences instead of automatic differentiation as it seems to be much
        // faster for the drag's derivatives' computation
        if (isGradientStateDerivative(s)) {
            rho =  (T) this.getGradientDensityWrtStateUsingFiniteDifferences(date.toAbsoluteDate(), frame, (FieldVector3D<Gradient>) position);
        } else if (isDSStateDerivative(s)) {
            rho = (T) this.getDSDensityWrtStateUsingFiniteDifferences(date.toAbsoluteDate(), frame, (FieldVector3D<DerivativeStructure>) position);
        } else {
            rho = atmosphere.getDensity(date, position, frame);
        }

        // Spacecraft relative velocity with respect to the atmosphere
        final FieldVector3D<T> vAtm = atmosphere.getVelocity(date, position, frame);
        final FieldVector3D<T> relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        // Drag acceleration along with its derivatives
        return spacecraft.dragAcceleration(s, rho, relativeVelocity, parameters);

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return spacecraft.getDragParametersDrivers();
    }

    /** Get the atmospheric model.
     * @return atmosphere model
     */
    public Atmosphere getAtmosphere() {
        return atmosphere;
    }

    /** Get spacecraft that are sensitive to atmospheric drag forces.
     * @return drag sensitive spacecraft model
     */
    public DragSensitive getSpacecraft() {
        return spacecraft;
    }

}
