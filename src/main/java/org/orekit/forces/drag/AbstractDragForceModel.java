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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;

import java.util.Arrays;

/**
 * Base class for drag force models.
 * @see DragForce
 * @author Bryan Cazabonne
 * @since 10.2
 */
public abstract class AbstractDragForceModel implements ForceModel {

    /** Atmospheric model. */
    private final Atmosphere atmosphere;

    /**
     * Flag to use (first-order) finite differences instead of automatic differentiation when computing density derivatives w.r.t. position.
     */
    private final boolean useFiniteDifferencesOnDensityWrtPosition;

    /** Atmospheric model used for partial derivatives computation. */
    private final Atmosphere atmosphereForField;

    /**
     * Constructor with default value for finite differences flag.
     * @param atmosphere atmospheric model
     */
    protected AbstractDragForceModel(final Atmosphere atmosphere) {
        this(atmosphere, true, atmosphere);
    }

    /**
     * Constructor.
     * @param atmosphere atmospheric model
     * @param useFiniteDifferencesOnDensityWrtPosition flag to use finite differences to compute density derivatives w.r.t.
     *                                                 position (is less accurate but can be faster depending on model)
     * @param atmosphereForField atmospheric model for partial derivatives (use faster one for performance)
     * @since 14.0
     */
    protected AbstractDragForceModel(final Atmosphere atmosphere, final boolean useFiniteDifferencesOnDensityWrtPosition,
                                     final Atmosphere atmosphereForField) {
        this.atmosphere = atmosphere;
        this.useFiniteDifferencesOnDensityWrtPosition = useFiniteDifferencesOnDensityWrtPosition;
        this.atmosphereForField = atmosphereForField;
    }

    /** Get the atmospheric model.
     * @return atmosphere model
     * @since 12.1
     */
    public Atmosphere getAtmosphere() {
        return atmosphere;
    }

    /**
     * Getter for atmosphere used with partial derivatives.
     * @return atmospheric model
     * @since 14.0
     */
    public Atmosphere getAtmosphereForField() {
        return atmosphereForField;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /**
     * Compute acceleration vector.
     * @param s state
     * @param dragSensitive drag sensitive object
     * @param dragParameters drag parameters
     * @return acceleration
     * @since 14.0
     */
    protected Vector3D acceleration(final SpacecraftState s, final DragSensitive dragSensitive,
                                    final double[] dragParameters) {
        // Local atmospheric density
        final AbsoluteDate date     = s.getDate();
        final Frame        frame    = s.getFrame();
        final Vector3D     position = s.getPosition();
        final double rho    = getAtmosphere().getDensity(date, position, frame);

        // Spacecraft relative velocity with respect to the atmosphere
        final Vector3D vAtm = getAtmosphere().getVelocity(date, position, frame);
        final Vector3D relativeVelocity = vAtm.subtract(s.getVelocity());

        // Compute and return drag acceleration
        return dragSensitive.dragAcceleration(s, rho, relativeVelocity, dragParameters);
    }

    /**
     * Compute acceleration vector (Field).
     * @param s state
     * @param dragSensitive drag sensitive object
     * @param dragParameters drag parameters
     * @param <T> field type
     * @return acceleration
     * @since 14.0
     */
    protected  <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                                 final DragSensitive dragSensitive,
                                                                                 final T[] dragParameters) {
        // Density and its derivatives
        final T rho = getFieldDensity(s);

        // Spacecraft relative velocity with respect to the atmosphere
        final FieldAbsoluteDate<T> date     = s.getDate();
        final Frame                frame    = s.getFrame();
        final FieldVector3D<T>     position = s.getPosition();
        final FieldVector3D<T> vAtm = getAtmosphere().getVelocity(date, position, frame);
        final FieldVector3D<T> relativeVelocity = vAtm.subtract(s.getVelocity());

        // Drag acceleration along with its derivatives
        return dragSensitive.dragAcceleration(s, rho, relativeVelocity, dragParameters);
    }

    /** Check if a field state corresponds to derivatives with respect to state.
     * @param state state to check
     * @param <T> type of the field elements
     * @return true if state corresponds to derivatives with respect to state
     */
    protected <T extends CalculusFieldElement<T>> boolean isGradientStateDerivative(final FieldSpacecraftState<T> state) {
        if (state.getMass() instanceof Gradient) {
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
        } else {
            return false;
        }
    }

    /**
     * Evaluate the Field density.
     * @param s spacecraft state
     * @return atmospheric density
     * @param <T> field type
     * @since 12.1
     */
    @SuppressWarnings("unchecked")
    protected <T extends CalculusFieldElement<T>> T getFieldDensity(final FieldSpacecraftState<T> s) {
        final FieldAbsoluteDate<T> date     = s.getDate();
        final Frame                frame    = s.getFrame();
        final FieldVector3D<T>     position = s.getPosition();
        if (isGradientStateDerivative(s)) {
            if (useFiniteDifferencesOnDensityWrtPosition) {
                return (T) this.getGradientDensityWrtStateUsingFiniteDifferences(date.toAbsoluteDate(), frame,
                        (FieldVector3D<Gradient>) position);
            } else {
                return (T) this.getGradientDensityWrtState(date.toAbsoluteDate(), frame, (FieldVector3D<Gradient>) position);
            }
        } else {
            final T fieldDensity = atmosphereForField.getDensity(date, position, frame);
            final double density = atmosphere == atmosphereForField ? fieldDensity.getReal() :
                    atmosphere.getDensity(date.toAbsoluteDate(), position.toVector3D(), frame);
            return fieldDensity.getAddendum().add(density);
        }
    }

    /** Check if a derivative represents a specified variable.
     * @param g derivative to check
     * @param index index of the variable
     * @return true if the derivative represents a specified variable
     */
    private boolean isVariable(final Gradient g, final int index) {
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
     * specific case of {@link Gradient} with respect to state, so
     * it is less general. However, it can be faster depending the Field implementation.
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
        final Frame      atmFrame  = atmosphereForField.getFrame();
        final StaticTransform toBody = frame.getStaticTransformTo(atmFrame, date);
        final FieldVector3D<Gradient> posBodyDS = toBody.transformPosition(position3);
        final Vector3D   posBody   = posBodyDS.toVector3D();

        // Estimate density model by finite differences and composition
        // Using a delta of 1m
        final double delta  = 1.0;
        final double x      = posBody.getX();
        final double y      = posBody.getY();
        final double z      = posBody.getZ();
        final double rho0   = atmosphereForField.getDensity(date, posBody, atmFrame);
        final double dRhodX = (atmosphereForField.getDensity(date, new Vector3D(x + delta, y,         z),         atmFrame) - rho0) / delta;
        final double dRhodY = (atmosphereForField.getDensity(date, new Vector3D(x,         y + delta, z),         atmFrame) - rho0) / delta;
        final double dRhodZ = (atmosphereForField.getDensity(date, new Vector3D(x,         y,         z + delta), atmFrame) - rho0) / delta;
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

        final double rho = atmosphere == atmosphereForField ? rho0 : atmosphere.getDensity(date, posBody, atmFrame);
        return new Gradient(rho, rhoAll);
    }

    /** Compute density and its derivatives.
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
     * @since 12.1
     */
    protected Gradient getGradientDensityWrtState(final AbsoluteDate date, final Frame frame,
                                                  final FieldVector3D<Gradient> position) {

        // Build a Gradient using only derivatives with respect to position
        final int positionDimension = 3;
        final FieldVector3D<Gradient> position3 =
                new FieldVector3D<>(Gradient.variable(positionDimension, 0, position.getX().getReal()),
                        Gradient.variable(positionDimension, 1,  position.getY().getReal()),
                        Gradient.variable(positionDimension, 2,  position.getZ().getReal()));

        // Get atmosphere properties in atmosphere own frame
        final Frame      atmFrame  = atmosphereForField.getFrame();
        final StaticTransform toBody = frame.getStaticTransformTo(atmFrame, date);
        final FieldVector3D<Gradient> posBodyGradient = toBody.transformPosition(position3);
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(position3.getX().getField(), date);
        final Gradient density = atmosphereForField.getDensity(fieldDate, posBodyGradient, atmFrame);

        // Density with derivatives:
        // - The value and only the 3 first derivatives (those with respect to spacecraft position) are computed
        // - Others are set to 0.
        final double[] derivatives = Arrays.copyOf(density.getGradient(), position.getX().getFreeParameters());

        final double rho =  atmosphere == atmosphereForField ? density.getValue() :
                atmosphere.getDensity(date, posBodyGradient.toVector3D(), atmFrame);
        return new Gradient(rho, derivatives);
    }
}
