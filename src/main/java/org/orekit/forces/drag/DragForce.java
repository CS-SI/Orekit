/* Copyright 2002-2017 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractForceModel;
import org.orekit.forces.drag.atmosphere.Atmosphere;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
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
 */

public class DragForce extends AbstractForceModel {

    /** Atmospheric model. */
    private final Atmosphere atmosphere;

    /** Spacecraft. */
    private final DragSensitive spacecraft;

    /** Simple constructor.
     * @param atmosphere atmospheric model
     * @param spacecraft the object physical and geometrical information
     */
    public DragForce(final Atmosphere atmosphere, final DragSensitive spacecraft) {
        this.atmosphere = atmosphere;
        this.spacecraft = spacecraft;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters)
        throws OrekitException {

        final AbsoluteDate date     = s.getDate();
        final Frame        frame    = s.getFrame();
        final Vector3D     position = s.getPVCoordinates().getPosition();

        final double rho    = atmosphere.getDensity(date, position, frame);
        final Vector3D vAtm = atmosphere.getVelocity(date, position, frame);
        final Vector3D relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        return spacecraft.dragAcceleration(date, frame, position, s.getAttitude().getRotation(),
                                           s.getMass(), rho, relativeVelocity, parameters);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters)
        throws OrekitException {
        final FieldAbsoluteDate<T> date     = s.getDate();
        final Frame                frame    = s.getFrame();
        final FieldVector3D<T>     position = s.getPVCoordinates().getPosition();

        final T rho    = atmosphere.getDensity(date, position, frame);
        final FieldVector3D<T> vAtm = atmosphere.getVelocity(date, position, frame);
        final FieldVector3D<T> relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        return spacecraft.dragAcceleration(date, frame, position, s.getAttitude().getRotation(),
                                           s.getMass(), rho, relativeVelocity, parameters);

    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        return spacecraft.getDragParametersDrivers();
    }

}
