/* Copyright 2002-2020 CS GROUP
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

import java.util.Arrays;
import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.forces.AbstractForceModel;
import org.orekit.forces.maneuvers.propulsion.PropulsionModel;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggers;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.FieldTimeDerivativesEquations;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;


/** A generic model for maneuvers.
 * It contains:
 *  - An attitude override, this is the attitude used during the maneuver, it can be different than the one
 *    used for propagation;
 *  - A maneuver triggers object from the trigger sub-package. It defines the triggers used to start and stop the maneuvers (dates or events for example).
 *  - A propulsion model from sub-package propulsion. It defines the thrust or ΔV, isp, flow rate etc..
 * Both the propulsion model and the maneuver triggers can contain parameter drivers (for estimation).
 * The convention here is that the propulsion model drivers are given before the maneuver triggers when calling the
 * method {@link #getParametersDrivers()}
 * @author Maxime Journot
 * @since 10.2
 */
public class Maneuver extends AbstractForceModel {

    /** The attitude to override during the maneuver, if set. */
    private final AttitudeProvider attitudeOverride;

    /** Propulsion model to use for the thrust. */
    private final PropulsionModel propulsionModel;

    /** Maneuver triggers. */
    private final ManeuverTriggers maneuverTriggers;

    /** Generic maneuver constructor.
     * @param attitudeOverride attitude provider for the attitude during the maneuver
     * @param maneuverTriggers maneuver triggers
     * @param propulsionModel propulsion model
     */
    public Maneuver(final AttitudeProvider attitudeOverride,
                    final ManeuverTriggers maneuverTriggers,
                    final PropulsionModel propulsionModel) {
        this.maneuverTriggers = maneuverTriggers;
        this.attitudeOverride = attitudeOverride;
        this.propulsionModel = propulsionModel;
    }

    /** Get the name of the maneuver.
     * The name can be in the propulsion model, in the maneuver triggers or both.
     * If it is in both it should be the same since it refers to the same maneuver.
     * The name is inferred from the propulsion model first, then from the maneuver triggers if
     * the propulsion model had an empty name.
     * @return the name
     */
    public String getName() {

        //FIXME: Potentially, throw an exception if both propulsion model
        // and maneuver triggers define a name but they are different
        String name = propulsionModel.getName();

        if (name.length() < 1) {
            name = maneuverTriggers.getName();
        }
        return name;
    }

    /** Get the attitude override used for the maneuver.
     * @return the attitude override
     */
    public AttitudeProvider getAttitudeOverride() {
        return attitudeOverride;
    }

    /** Get the propulsion model.
     * @return the propulsion model
     */
    public PropulsionModel getPropulsionModel() {
        return propulsionModel;
    }

    /** Get the maneuver triggers.
     * @return the maneuver triggers
     */
    public ManeuverTriggers getManeuverTriggers() {
        return maneuverTriggers;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        propulsionModel.init(initialState, target);
        maneuverTriggers.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder) {

        // Get the parameters associated to the maneuver (from ForceModel)
        final double[] parameters = getParameters();

        // If the maneuver is active, compute and add its contribution
        // Maneuver triggers are used to check if the maneuver is currently firing or not
        // Specific drivers for the triggers are extracted from the array given by the ForceModel interface
        if (maneuverTriggers.isFiring(s.getDate(), getManeuverTriggersParameters(parameters))) {

            // Compute thrust acceleration in inertial frame
            adder.addNonKeplerianAcceleration(acceleration(s, parameters));

            // Compute flow rate using the propulsion model
            // Specific drivers for the propulsion model are extracted from the array given by the ForceModel interface
            adder.addMassDerivative(propulsionModel.getMassDerivatives(s, getPropulsionModelParameters(parameters)));
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> void addContribution(final FieldSpacecraftState<T> s,
                        final FieldTimeDerivativesEquations<T> adder) {

        // Get the parameters associated to the maneuver (from ForceModel)
        final T[] parameters = getParameters(s.getDate().getField());

        // If the maneuver is active, compute and add its contribution
        // Maneuver triggers are used to check if the maneuver is currently firing or not
        // Specific drivers for the triggers are extracted from the array given by the ForceModel interface
        if (maneuverTriggers.isFiring(s.getDate(), getManeuverTriggersParameters(parameters))) {

            // Compute thrust acceleration in inertial frame
            adder.addNonKeplerianAcceleration(acceleration(s, parameters));

            // Compute flow rate using the propulsion model
            // Specific drivers for the propulsion model are extracted from the array given by the ForceModel interface
            adder.addMassDerivative(propulsionModel.getMassDerivatives(s, getPropulsionModelParameters(parameters)));
        }
    }

    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        // If the maneuver is active, compute and add its contribution
        // Maneuver triggers are used to check if the maneuver is currently firing or not
        // Specific drivers for the triggers are extracted from the array given by the ForceModel interface
        if (maneuverTriggers.isFiring(s.getDate(), getManeuverTriggersParameters(parameters))) {

            // Attitude during maneuver
            final Attitude maneuverAttitude =
                            attitudeOverride == null ?
                            s.getAttitude() :
                            attitudeOverride.getAttitude(s.getOrbit(),
                                                         s.getDate(),
                                                         s.getFrame());

            // Compute acceleration from propulsion model
            // Specific drivers for the propulsion model are extracted from the array given by the ForceModel interface
            return propulsionModel.getAcceleration(s, maneuverAttitude, getPropulsionModelParameters(parameters));
        } else {
            // Constant (and null) acceleration when not firing
            return Vector3D.ZERO;
        }
    }

    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s, final T[] parameters) {

        // If the maneuver is active, compute and add its contribution
        // Maneuver triggers are used to check if the maneuver is currently firing or not
        // Specific drivers for the triggers are extracted from the array given by the ForceModel interface
        if (maneuverTriggers.isFiring(s.getDate(), getManeuverTriggersParameters(parameters))) {

            // Attitude during maneuver
            final FieldAttitude<T> maneuverAttitude =
                            attitudeOverride == null ?
                            s.getAttitude() :
                            attitudeOverride.getAttitude(s.getOrbit(),
                                                         s.getDate(),
                                                         s.getFrame());

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
    public Stream<EventDetector> getEventsDetectors() {
        // Event detectors are extracted from the maneuver triggers
        return maneuverTriggers.getEventsDetectors();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        // Event detectors are extracted from the maneuver triggers
        return maneuverTriggers.getFieldEventsDetectors(field);
    }

    @Override
    public ParameterDriver[] getParametersDrivers() {

        // Extract parameter drivers from propulsion model and maneuver triggers
        final ParameterDriver[] propulsionModelDrivers  = propulsionModel.getParametersDrivers();
        final ParameterDriver[] maneuverTriggersDrivers = maneuverTriggers.getParametersDrivers();
        final int propulsionModelDriversLength  = propulsionModelDrivers.length;
        final int maneuverTriggersDriversLength = maneuverTriggersDrivers.length;

        // Prepare final drivers' array
        final ParameterDriver[] drivers = new ParameterDriver[propulsionModelDriversLength + maneuverTriggersDriversLength];

        // Convention: Propulsion drivers are given before maneuver triggers drivers
        // Add propulsion drivers first
        System.arraycopy(propulsionModelDrivers, 0, drivers, 0, propulsionModelDriversLength);

        // Then maneuver triggers' drivers
        System.arraycopy(maneuverTriggersDrivers, 0, drivers, propulsionModelDriversLength, maneuverTriggersDriversLength);

        // Return full drivers' array
        return drivers;
    }

    /** Extract propulsion model parameters from the parameters' array called in by the ForceModel interface.
     *  Convention: Propulsion parameters are given before maneuver triggers parameters
     * @param parameters parameters' array called in by ForceModel interface
     * @return propulsion model parameters
     */
    private double[] getPropulsionModelParameters(final double[] parameters) {
        return Arrays.copyOfRange(parameters, 0, propulsionModel.getParametersDrivers().length);
    }

    /** Extract propulsion model parameters from the parameters' array called in by the ForceModel interface.
     *  Convention: Propulsion parameters are given before maneuver triggers parameters
     * @param parameters parameters' array called in by ForceModel interface
     * @param <T> extends RealFieldElement&lt;T&gt;
     * @return propulsion model parameters
     */
    private <T extends RealFieldElement<T>> T[] getPropulsionModelParameters(final T[] parameters) {
        return Arrays.copyOfRange(parameters, 0, propulsionModel.getParametersDrivers().length);
    }

    /** Extract maneuver triggers' parameters from the parameters' array called in by the ForceModel interface.
     *  Convention: Propulsion parameters are given before maneuver triggers parameters
     * @param parameters parameters' array called in by ForceModel interface
     * @return maneuver triggers' parameters
     */
    private double[] getManeuverTriggersParameters(final double[] parameters) {
        final int nbPropulsionModelDrivers = propulsionModel.getParametersDrivers().length;
        return Arrays.copyOfRange(parameters, nbPropulsionModelDrivers,
                                  nbPropulsionModelDrivers + maneuverTriggers.getParametersDrivers().length);
    }

    /** Extract maneuver triggers' parameters from the parameters' array called in by the ForceModel interface.
     *  Convention: Propulsion parameters are given before maneuver triggers parameters
     * @param parameters parameters' array called in by ForceModel interface
     * @param <T> extends RealFieldElement&lt;T&gt;
     * @return maneuver triggers' parameters
     */
    private <T extends RealFieldElement<T>> T[] getManeuverTriggersParameters(final T[] parameters) {
        final int nbPropulsionModelDrivers = propulsionModel.getParametersDrivers().length;
        return Arrays.copyOfRange(parameters, nbPropulsionModelDrivers,
                                  nbPropulsionModelDrivers + maneuverTriggers.getParametersDrivers().length);
    }
}
