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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;

/**
 * Base class for drag force models.
 * @see DragForce
 * @see TimeSpanDragForce
 * @author Bryan Cazabonne
 * @since 10.2
 */
public abstract class AbstractDragForceModel implements ForceModel {

    /** Atmospheric model. */
    private final Atmosphere atmosphere;

    /**
     * Constructor.
     * @param atmosphere atmospheric model
     */
    protected AbstractDragForceModel(final Atmosphere atmosphere) {
        this.atmosphere = atmosphere;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** Check if a field state corresponds to derivatives with respect to state.
     * @param state state to check
     * @param <T> type of the field elements
     * @return true if state corresponds to derivatives with respect to state
     */
    protected <T extends CalculusFieldElement<T>> boolean isDSStateDerivative(final FieldSpacecraftState<T> state) {
        try {
            final DerivativeStructure dsMass = (DerivativeStructure) state.getMass();
            final int o = dsMass.getOrder();
            final int p = dsMass.getFreeParameters();

            // To be in the desired case:
            // Order must be 1 (first order derivatives only)
            // Number of parameters must be 6 (PV), 7 (PV + drag coefficient) or 8 (PV + drag coefficient + lift ratio)
            if (o != 1 || p != 6 && p != 7 && p != 8) {
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

    /** Check if a field state corresponds to derivatives with respect to state.
     * @param state state to check
     * @param <T> type of the field elements
     * @return true if state corresponds to derivatives with respect to state
     */
    protected <T extends CalculusFieldElement<T>> boolean isGradientStateDerivative(final FieldSpacecraftState<T> state) {
        try {
            final Gradient gMass = (Gradient) state.getMass();
            final int p = gMass.getFreeParameters();

            // To be in the desired case:
            // Number of parameters must be 6 (PV), 7 (PV + drag coefficient) or 8 (PV + drag coefficient + lift ratio)
            if (p != 6 && p != 7 && p != 8) {
                return false;
            }

            // Check that the first 6 parameters are position and velocity
            @SuppressWarnings("unchecked")
            final FieldPVCoordinates<Gradient> pv = (FieldPVCoordinates<Gradient>) state.getPVCoordinates();
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
     */
    protected boolean isVariable(final DerivativeStructure ds, final int index) {
        final double[] derivatives = ds.getAllDerivatives();
        boolean check = true;
        for (int i = 1; i < derivatives.length; ++i) {
            check &= derivatives[i] == ((index + 1 == i) ? 1.0 : 0.0);
        }
        return check;
    }

    /** Check if a derivative represents a specified variable.
     * @param g derivative to check
     * @param index index of the variable
     * @return true if the derivative represents a specified variable
     */
    protected boolean isVariable(final Gradient g, final int index) {
        final double[] derivatives = g.getGradient();
        boolean check = true;
        for (int i = 0; i < derivatives.length; ++i) {
            check &= derivatives[i] == ((index == i) ? 1.0 : 0.0);
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
     * </p>
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
     * @return the density and its derivatives
     */
    protected DerivativeStructure getDSDensityWrtStateUsingFiniteDifferences(final AbsoluteDate date,
                                                                             final Frame frame,
                                                                             final FieldVector3D<DerivativeStructure> position) {

        // Retrieve derivation properties for parameter T
        // It is implied here that T is a DerivativeStructure
        // With order 1 and 6, 7 or 8 free parameters
        // This is all checked before in method isStateDerivatives
        final DSFactory factory = position.getX().getFactory();

        // Build a DerivativeStructure using only derivatives with respect to position
        final DSFactory factory3 = new DSFactory(3, 1);
        final FieldVector3D<DerivativeStructure> position3 =
                        new FieldVector3D<>(factory3.variable(0, position.getX().getReal()),
                                            factory3.variable(1,  position.getY().getReal()),
                                            factory3.variable(2,  position.getZ().getReal()));

        // Get atmosphere properties in atmosphere own frame
        final Frame      atmFrame  = atmosphere.getFrame();
        final StaticTransform toBody = frame.getStaticTransformTo(atmFrame, date);
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

        return factory.build(rhoAll);
    }

    /** Compute density and its derivatives.
     * Using finite differences for the derivatives.
     * And doing the actual computation only for the derivatives with respect to position (others are set to 0.).
     * <p>
     * From a theoretical point of view, this method computes the same values
     * as {@link Atmosphere#getDensity(FieldAbsoluteDate, FieldVector3D, Frame)} in the
     * specific case of {@link Gradient} with respect to state, so
     * it is less general. However, it is *much* faster in this important case.
     * </p>
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
     * @return the density and its derivatives
     */
    protected Gradient getGradientDensityWrtStateUsingFiniteDifferences(final AbsoluteDate date,
                                                                        final Frame frame,
                                                                        final FieldVector3D<Gradient> position) {

        // Build a Gradient using only derivatives with respect to position
        final FieldVector3D<Gradient> position3 =
                        new FieldVector3D<>(Gradient.variable(3, 0, position.getX().getReal()),
                                            Gradient.variable(3, 1,  position.getY().getReal()),
                                            Gradient.variable(3, 2,  position.getZ().getReal()));

        // Get atmosphere properties in atmosphere own frame
        final Frame      atmFrame  = atmosphere.getFrame();
        final StaticTransform toBody = frame.getStaticTransformTo(atmFrame, date);
        final FieldVector3D<Gradient> posBodyDS = toBody.transformPosition(position3);
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
        final double[] dXdQ = posBodyDS.getX().getGradient();
        final double[] dYdQ = posBodyDS.getY().getGradient();
        final double[] dZdQ = posBodyDS.getZ().getGradient();

        // Density with derivatives:
        // - The value and only the 3 first derivatives (those with respect to spacecraft position) are computed
        // - Others are set to 0.
        final int p = position.getX().getFreeParameters();
        final double[] rhoAll = new double[p];
        for (int i = 0; i < 3; ++i) {
            rhoAll[i] = dRhodX * dXdQ[i] + dRhodY * dYdQ[i] + dRhodZ * dZdQ[i];
        }

        return new Gradient(rho0, rhoAll);
    }

}
