/* Copyright 2002-2016 CS Systèmes d'Information
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
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.AbstractForceModel;
import org.orekit.forces.drag.atmosphere.Atmosphere;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.FieldTimeDerivativesEquations;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
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

    @Override
    public <T extends RealFieldElement<T>> void
        addContribution(final FieldSpacecraftState<T> s,
                        final FieldTimeDerivativesEquations<T> adder)
            throws OrekitException {
        final FieldAbsoluteDate<T> date     = s.getDate();
        final Frame        frame    = s.getFrame();
        final FieldVector3D<T>     position = s.getPVCoordinates().getPosition();

        final T rho    = atmosphere.getDensity(date, position, frame);
        final FieldVector3D<T> vAtm = atmosphere.getVelocity(date, position, frame);
        final FieldVector3D<T> relativeVelocity = s.getPVCoordinates().getVelocity().negate().add(vAtm);

        // Addition of calculated acceleration to adder
        adder.addAcceleration(spacecraft.dragAcceleration(date, frame, position, s.getAttitude().getRotation(),
                                                          s.getMass(), rho, relativeVelocity), frame);
    }

    /** There are no discrete events for this model.
     * @return an empty array
     */
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.empty();
    }

    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    public ParameterDriver[] getParametersDrivers() {
        return spacecraft.getDragParametersDrivers();
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final AbsoluteDate date, final Frame frame,
                                                                      final FieldVector3D<DerivativeStructure> position,
                                                                      final FieldVector3D<DerivativeStructure> velocity,
                                                                      final FieldRotation<DerivativeStructure> rotation,
                                                                      final DerivativeStructure mass)
        throws OrekitException {
        // retrieve derivation properties
        final int parameters = mass.getFreeParameters();
        final int order      = mass.getOrder();

        // get atmosphere properties in atmosphere own frame
        final Frame      atmFrame  = atmosphere.getFrame();
        final Transform  toBody    = frame.getTransformTo(atmFrame, date);
        final FieldVector3D<DerivativeStructure> posBodyDS = toBody.transformPosition(position);
        final Vector3D   posBody   = posBodyDS.toVector3D();
        final Vector3D   vAtmBody  = atmosphere.getVelocity(date, posBody, atmFrame);

        // estimate density model by finite differences and composition
        // the following implementation works only for first order derivatives.
        // this could be improved by adding a new method
        // getDensity(AbsoluteDate, DerivativeStructure, Frame)
        // to the Atmosphere interface
        if (order > 1) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, order);
        }
        final double delta  = 1.0;
        final double x      = posBody.getX();
        final double y      = posBody.getY();
        final double z      = posBody.getZ();
        final double rho0   = atmosphere.getDensity(date, posBody, atmFrame);
        final double dRhodX = (atmosphere.getDensity(date, new Vector3D(x + delta, y,         z),         atmFrame) - rho0) / delta;
        final double dRhodY = (atmosphere.getDensity(date, new Vector3D(x,         y + delta, z),         atmFrame) - rho0) / delta;
        final double dRhodZ = (atmosphere.getDensity(date, new Vector3D(x,         y,         z + delta), atmFrame) - rho0) / delta;
        final double[] dXdQ = posBodyDS.getX().getAllDerivatives();
        final double[] dYdQ = posBodyDS.getY().getAllDerivatives();
        final double[] dZdQ = posBodyDS.getZ().getAllDerivatives();
        final double[] rhoAll = new double[dXdQ.length];
        rhoAll[0] = rho0;
        for (int i = 1; i < rhoAll.length; ++i) {
            rhoAll[i] = dRhodX * dXdQ[i] + dRhodY * dYdQ[i] + dRhodZ * dZdQ[i];
        }
        final DerivativeStructure rho = new DerivativeStructure(parameters, order, rhoAll);

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
