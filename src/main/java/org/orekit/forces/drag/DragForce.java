/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.forces.AbstractForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
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

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

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
                                                                         final T[] parameters) {

        final FieldAbsoluteDate<T> date     = s.getDate();
        final Frame                frame    = s.getFrame();
        final FieldVector3D<T>     position = s.getPVCoordinates().getPosition();

        // Density and its derivatives
        final T rho;

        // Check for faster computation dedicated to derivatives with respect to state
        // Using finite differences instead of automatic differentiation as it seems to be much
        // faster for the drag's derivatives' computation
        if (isStateDerivative(s)) {
            rho = this.getDensityWrtStateUsingFiniteDifferences(date.toAbsoluteDate(), frame, position);
        } else {
            rho    = atmosphere.getDensity(date, position, frame);
        }

        // Spacecraft relative velocity with respect to the atmosphere
        final FieldVector3D<T> vAtm = atmosphere.getVelocity(date, position, frame);
        final FieldVector3D<T> relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        // Drag acceleration along with its derivatives
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

    /** Check if a field state corresponds to derivatives with respect to state.
     * @param state state to check
     * @param <T> type of the field elements
     * @return true if state corresponds to derivatives with respect to state
     * @since 9.0
     */
    private <T extends RealFieldElement<T>> boolean isStateDerivative(final FieldSpacecraftState<T> state) {
        try {
            final DerivativeStructure dsMass = (DerivativeStructure) state.getMass();
            final int o = dsMass.getOrder();
            final int p = dsMass.getFreeParameters();

            // To be in the desired case:
            // Order must be 1 (first order derivatives only)
            // Number of parameters must be 6 (PV), 7 (PV + drag coefficient) or 8 (PV + drag coefficient + lift ratio)
            if (o != 1 || (p != 6 && p != 7 && p != 8)) {
                return false;
            }

            // Check that the first 6 parameters are position and velocity
            @SuppressWarnings("unchecked")
            final FieldPVCoordinates<DerivativeStructure> pv = (FieldPVCoordinates<DerivativeStructure>) state.getPVCoordinates();
            return isVariable(pv.getPosition().getX(), 0) &&
                   isVariable(pv.getPosition().getY(), 1) &&
                   isVariable(pv.getPosition().getZ(), 2) &&
                   isVariable(pv.getVelocity().getX(), 3) &&
                   isVariable(pv.getVelocity().getY(), 4) &&
                   isVariable(pv.getVelocity().getZ(), 5);
        } catch (ClassCastException cce) {
            return false;
        }
    }

    /** Check if a derivative represents a specified variable.
     * @param ds derivative to check
     * @param index index of the variable
     * @return true if the derivative represents a specified variable
     * @since 9.0
     */
    private boolean isVariable(final DerivativeStructure ds, final int index) {
        final double[] derivatives = ds.getAllDerivatives();
        boolean check = true;
        for (int i = 1; i < derivatives.length; ++i) {
            check &= derivatives[i] == ((index + 1 == i) ? 1.0 : 0.0);
        }
        return check;
    }

    /** Compute density and its derivatives.
     * Using finite differences for the derivatives.
     * And doing the actual computation only for the derivatives with respect to position (others are set to 0.).
     * <p>
     * From a theoretical point of view, this method computes the same values
     * as {@link Atmosphere#getDensity(FieldAbsoluteDate, FieldVector3D, Frame)} in the
     * specific case of {@link DerivativeStructure} with respect to state, so
     * it is less general. However, it is *much* faster in this important case.
     * <p>
     * <p>
     * The derivatives should be computed with respect to position. The input
     * parameters already take into account the free parameters (6, 7 or 8 depending
     * on derivation with respect to drag coefficient and lift ratio being considered or not)
     * and order (always 1). Free parameters at indices 0, 1 and 2 correspond to derivatives
     * with respect to position. Free parameters at indices 3, 4 and 5 correspond
     * to derivatives with respect to velocity (these derivatives will remain zero
     * as the atmospheric density does not depend on velocity). Free parameter
     * at indexes 6 and 7 (if present) corresponds to derivatives with respect to drag coefficient
     * and/or lift ratio (one of these or both).
     * This 2 last derivatives will remain zero as atmospheric density does not depend on them.
     * </p>
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in inertial frame
     * @param <T> type of the elements
     * @return the density and its derivatives
          * @since 9.0
     */
    private <T extends RealFieldElement<T>> T getDensityWrtStateUsingFiniteDifferences(final AbsoluteDate date,
                                                                                       final Frame frame,
                                                                                       final FieldVector3D<T> position) {

        // Retrieve derivation properties for parameter T
        // It is implied here that T is a DerivativeStructure
        // With order 1 and 6, 7 or 8 free parameters
        // This is all checked before in method isStateDerivatives
        final DSFactory factory = ((DerivativeStructure) position.getX()).getFactory();

        // Build a DerivativeStructure using only derivatives with respect to position
        final DSFactory factory3 = new DSFactory(3, 1);
        final FieldVector3D<DerivativeStructure> position3 =
                        new FieldVector3D<>(factory3.variable(0, position.getX().getReal()),
                                            factory3.variable(1,  position.getY().getReal()),
                                            factory3.variable(2,  position.getZ().getReal()));

        // Get atmosphere properties in atmosphere own frame
        final Frame      atmFrame  = atmosphere.getFrame();
        final Transform  toBody    = frame.getTransformTo(atmFrame, date);
        final FieldVector3D<DerivativeStructure> posBodyDS = toBody.transformPosition(position3);
        final Vector3D   posBody   = posBodyDS.toVector3D();

        // Estimate density model by finite differences and composition
        // Using a delta of 1m
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

        // Density with derivatives:
        // - The value and only the 3 first derivatives (those with respect to spacecraft position) are computed
        // - Others are set to 0.
        final int p = factory.getCompiler().getFreeParameters();
        final double[] rhoAll = new double[p + 1];
        rhoAll[0] = rho0;
        for (int i = 1; i < 4; ++i) {
            rhoAll[i] = dRhodX * dXdQ[i] + dRhodY * dYdQ[i] + dRhodZ * dZdQ[i];
        }
        @SuppressWarnings("unchecked")
        final T rho = (T) (factory.build(rhoAll));

        return rho;
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
