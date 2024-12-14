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

package org.orekit.forces.maneuvers;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeRotationModel;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.forces.ForceModel;
import org.orekit.forces.maneuvers.propulsion.PropulsionModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.FieldTimeDerivativesEquations;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

import java.util.Arrays;
import java.util.stream.Stream;


/** A generic model for maneuvers with finite-valued acceleration magnitude, as opposed to instantaneous changes
 * in the velocity vector which are defined via detectors (in {@link ImpulseManeuver} and
 * {@link FieldImpulseManeuver}).
 * It contains:
 *  - An attitude override, this is the attitude used during the maneuver, it can be different from the one
 *    used for propagation;
 *  - A propulsion model from sub-package propulsion. It defines the thrust or ΔV, isp, flow rate etc.
 * @author Maxime Journot
 * @since 10.2
 */
public abstract class Maneuver implements ForceModel {

    /** The attitude to override during the maneuver, if set. */
    private final AttitudeRotationModel attitudeOverride;

    /** Propulsion model to use for the thrust. */
    private final PropulsionModel propulsionModel;

    /** Generic maneuver constructor.
     * @param attitudeOverride attitude provider for the attitude during the maneuver
     * @param propulsionModel propulsion model
     */
    protected Maneuver(final AttitudeRotationModel attitudeOverride,
                       final PropulsionModel propulsionModel) {
        this.attitudeOverride = attitudeOverride;
        this.propulsionModel = propulsionModel;
    }

    /** Get the attitude override used for the maneuver.
     * @return the attitude override
     * @since 13.0
     */
    public AttitudeRotationModel getAttitudeOverride() {
        return attitudeOverride;
    }

    /** Get the control vector's cost type.
     * @return control cost type
     * @since 12.0
     */
    public Control3DVectorCostType getControl3DVectorCostType() {
        return propulsionModel.getControl3DVectorCostType();
    }

    /** Get the propulsion model.
     * @return the propulsion model
     */
    public PropulsionModel getPropulsionModel() {
        return propulsionModel;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** Get the name of the maneuver, using the underlying propulsion model.
     * @return the name
     */
    public String getName() {
        return getPropulsionModel().getName();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        propulsionModel.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> void init(final FieldSpacecraftState<T> initialState,
                                                         final FieldAbsoluteDate<T> target) {
        propulsionModel.init(initialState, target);
    }

    protected abstract boolean isFiring(AbsoluteDate date, double[] parameters);

    protected abstract <T extends CalculusFieldElement<T>> boolean isFiring(FieldAbsoluteDate<T> date, T[] parameters);

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder) {

        // Get the parameters associated to the maneuver (from ForceModel)
        final double[] parameters = getParameters(s.getDate());

        // If the maneuver is active, compute and add its contribution
        if (isFiring(s.getDate(), parameters)) {

            // Compute thrust acceleration in inertial frame
            adder.addNonKeplerianAcceleration(acceleration(s, parameters));

            // Compute flow rate using the propulsion model
            // Specific drivers for the propulsion model are extracted from the array given by the ForceModel interface
            adder.addMassDerivative(propulsionModel.getMassDerivatives(s, getPropulsionModelParameters(parameters)));
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> void addContribution(final FieldSpacecraftState<T> s,
                        final FieldTimeDerivativesEquations<T> adder) {

        // Get the parameters associated to the maneuver (from ForceModel)
        final T[] parameters = getParameters(s.getDate().getField(), s.getDate());

        // If the maneuver is active, compute and add its contribution
        if (isFiring(s.getDate(), parameters)) {

            // Compute thrust acceleration in inertial frame
            // the acceleration method extracts the parameter in its core, that is why we call it with
            // parameters and not extracted parameters
            adder.addNonKeplerianAcceleration(acceleration(s, parameters));

            // Compute flow rate using the propulsion model
            // Specific drivers for the propulsion model are extracted from the array given by the ForceModel interface
            adder.addMassDerivative(propulsionModel.getMassDerivatives(s, getPropulsionModelParameters(parameters)));
        }
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        // If the maneuver is active, compute and add its contribution
        if (isFiring(s.getDate(), parameters)) {

            // Attitude during maneuver
            final Attitude maneuverAttitude;
            if (attitudeOverride == null) {
                maneuverAttitude = s.getAttitude();
            } else {
                final Rotation rotation = attitudeOverride.getAttitudeRotation(s,
                        getAttitudeModelParameters(parameters));
                // use dummy rates to build full attitude as they should not be used
                maneuverAttitude = new Attitude(s.getDate(), s.getFrame(), rotation, Vector3D.ZERO, Vector3D.ZERO);
            }

            // Compute acceleration from propulsion model
            // Specific drivers for the propulsion model are extracted from the array given by the ForceModel interface
            return propulsionModel.getAcceleration(s, maneuverAttitude, getPropulsionModelParameters(parameters));
        } else {
            // Constant (and null) acceleration when not firing
            return Vector3D.ZERO;
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                             final T[] parameters) {

        // If the maneuver is active, compute and add its contribution
        if (isFiring(s.getDate(), parameters)) {

            // Attitude during maneuver
            final FieldAttitude<T> maneuverAttitude;
            if (attitudeOverride == null) {
                maneuverAttitude = s.getAttitude();
            } else {
                final FieldRotation<T> rotation = attitudeOverride.getAttitudeRotation(s,
                        getAttitudeModelParameters(parameters));
                // use dummy rates to build full attitude as they should not be used
                final FieldVector3D<T> zeroVector3D = FieldVector3D.getZero(s.getDate().getField());
                maneuverAttitude = new FieldAttitude<>(s.getDate(), s.getFrame(), rotation, zeroVector3D, zeroVector3D);
            }

            // Compute acceleration from propulsion model
            // Specific drivers for the propulsion model are extracted from the array given by the ForceModel interface
            return propulsionModel.getAcceleration(s, maneuverAttitude, getPropulsionModelParameters(parameters));
        } else {
            // Constant (and null) acceleration when not firing
            return FieldVector3D.getZero(s.getMu().getField());
        }
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        return propulsionModel.getEventDetectors();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        return propulsionModel.getFieldEventDetectors(field);
    }

    /** Extract propulsion model parameters from the parameters' array called in by the ForceModel interface.
     * @param parameters parameters' array called in by ForceModel interface
     * @return propulsion model parameters
     */
    public double[] getPropulsionModelParameters(final double[] parameters) {
        return Arrays.copyOfRange(parameters, 0, propulsionModel.getParametersDrivers().size());
    }

    /** Extract propulsion model parameters from the parameters' array called in by the ForceModel interface.
     * @param parameters parameters' array called in by ForceModel interface
     * @param <T> extends CalculusFieldElement&lt;T&gt;
     * @return propulsion model parameters
     */
    public <T extends CalculusFieldElement<T>> T[] getPropulsionModelParameters(final T[] parameters) {
        return Arrays.copyOfRange(parameters, 0, propulsionModel.getParametersDrivers().size());
    }

    /** Extract attitude model' parameters from the parameters' array called in by the ForceModel interface.
     *  Convention: Attitude model parameters are given last
     * @param parameters parameters' array called in by ForceModel interface
     * @return attitude override' parameters
     */
    protected double[] getAttitudeModelParameters(final double[] parameters) {
        final int nbAttitudeModelDrivers = (attitudeOverride == null) ? 0 : attitudeOverride.getParametersDrivers().size();
        return Arrays.copyOfRange(parameters, parameters.length - nbAttitudeModelDrivers, parameters.length);
    }

    /** Extract attitude model' parameters from the parameters' array called in by the ForceModel interface.
     *  Convention: Attitude parameters are given last
     * @param parameters parameters' array called in by ForceModel interface
     * @param <T> extends CalculusFieldElement&lt;T&gt;
     * @return attitude override' parameters
     */
    protected <T extends CalculusFieldElement<T>> T[] getAttitudeModelParameters(final T[] parameters) {
        final int nbAttitudeModelDrivers = (attitudeOverride == null) ? 0 : attitudeOverride.getParametersDrivers().size();
        return Arrays.copyOfRange(parameters, parameters.length - nbAttitudeModelDrivers, parameters.length);
    }
}
