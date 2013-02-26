/* Copyright 2002-2013 CS Systèmes d'Information
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

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractParameterizable;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;


/** Atmospheric drag force model.
 *
 * The drag acceleration is computed as follows :
 *
 * &gamma; = (1/2 * &rho; * V<sup>2</sup> * S / Mass) * DragCoefVector
 *
 * With DragCoefVector = {C<sub>x</sub>, C<sub>y</sub>, C<sub>z</sub>} and S given by the user through the interface
 * {@link DragSensitive}
 *
 * @author &Eacute;douard Delente
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Pascal Parraud
 */

public class DragForce extends AbstractParameterizable implements ForceModel {

    /** Atmospheric model. */
    private final Atmosphere atmosphere;

    /** Spacecraft. */
    private final DragSensitive spacecraft;

    /** Simple constructor.
     * @param atmosphere atmospheric model
     * @param spacecraft the object physical and geometrical information
     */
    public DragForce(final Atmosphere atmosphere, final DragSensitive spacecraft) {
        super(DragSensitive.DRAG_COEFFICIENT);
        this.atmosphere = atmosphere;
        this.spacecraft = spacecraft;
    }

    /** Compute the contribution of the drag to the perturbing acceleration.
     * @param s the current state information : date, kinematics, attitude
     * @param adder object where the contribution should be added
     * @exception OrekitException if some specific error occurs
     */
    public void addContribution(final SpacecraftState s,
                                final TimeDerivativesEquations adder)
        throws OrekitException {

        final AbsoluteDate date     = s.getDate();
        final Frame        frame    = s.getFrame();
        final Vector3D     position = s.getPVCoordinates().getPosition();

        final double rho    = atmosphere.getDensity(date, position, frame);
        final Vector3D vAtm = atmosphere.getVelocity(date, position, frame);
        final Vector3D relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        // Addition of calculated acceleration to adder
        adder.addAcceleration(spacecraft.dragAcceleration(date, frame, position, s.getAttitude().getRotation(),
                                                          s.getMass(), rho, relativeVelocity), frame);

    }

    /** There are no discrete events for this model.
     * @return an empty array
     */
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    public double getParameter(final String name) throws IllegalArgumentException {
        complainIfNotSupported(name);
        return spacecraft.getDragCoefficient();
    }

    /** {@inheritDoc} */
    public void setParameter(final String name, final double value) throws IllegalArgumentException {
        complainIfNotSupported(name);
        spacecraft.setDragCoefficient(value);
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final AbsoluteDate date, final Frame frame,
                                              final FieldVector3D<DerivativeStructure> position, final FieldVector3D<DerivativeStructure> velocity,
                                              final FieldRotation<DerivativeStructure> rotation, DerivativeStructure mass)
        throws OrekitException {

        // retrieve derivation properties
        final int parameters = mass.getFreeParameters();
        final int order      = mass.getOrder();

        // get atmosphere properties in atmosphere own frame
        final Frame      atmFrame  = atmosphere.getFrame();
        final Transform  toBody    = frame.getTransformTo(atmFrame, date);
        final FieldVector3D<DerivativeStructure> posBodyDS = toBody.transformPosition(position);
        final Vector3D   posBody   = posBodyDS.toVector3D();
        final double     rho       = atmosphere.getDensity(date, posBody, atmFrame);
        final Vector3D   vAtmBody  = atmosphere.getVelocity(date, posBody, atmFrame);

        // we consider that at first order the atmosphere velocity in atmosphere frame
        // does not depend on local position; however atmosphere velocity in inertial
        // frame DOES depend on position since the transform between the frames depends
        // on it, due to central body rotation rate and velocity composition.
        // So we use the transform to get the correct partial derivatives on vAtm
        final FieldVector3D<DerivativeStructure> vAtmBodyDS =
                new FieldVector3D<DerivativeStructure>(new DerivativeStructure(parameters, order, vAtmBody.getX()),
                                                       new DerivativeStructure(parameters, order, vAtmBody.getY()),
                                                       new DerivativeStructure(parameters, order, vAtmBody.getZ()));
        final FieldPVCoordinates<DerivativeStructure> pvAtmBody = new FieldPVCoordinates<DerivativeStructure>(posBodyDS, vAtmBodyDS);
        final FieldPVCoordinates<DerivativeStructure> pvAtm     = toBody.getInverse().transformPVCoordinates(pvAtmBody);

        // now we can compute relative velocity, it takes into account partial derivatives with respect to position
        final FieldVector3D<DerivativeStructure> relativeVelocity = pvAtm.getVelocity().subtract(velocity);

        // compute acceleration with all its partial derivatives
        return spacecraft.dragAcceleration(date, frame, position, rotation, mass, rho, relativeVelocity);

    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final SpacecraftState s, final String paramName)
        throws OrekitException {

        complainIfNotSupported(paramName);

        final AbsoluteDate date     = s.getDate();
        final Frame        frame    = s.getFrame();
        final Vector3D     position = s.getPVCoordinates().getPosition();

        final double rho    = atmosphere.getDensity(date, position, frame);
        final Vector3D vAtm = atmosphere.getVelocity(date, position, frame);
        final Vector3D relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        // compute acceleration with all its partial derivatives
        return spacecraft.dragAcceleration(date, frame, position, s.getAttitude().getRotation(),
                                           s.getMass(), rho, relativeVelocity, paramName);

    }

}
