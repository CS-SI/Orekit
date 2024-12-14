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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.attitudes.AttitudeRotationModel;
import org.orekit.forces.maneuvers.propulsion.PropulsionModel;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggers;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;


/** A generic model for maneuvers with finite-valued acceleration magnitude, as opposed to instantaneous changes
 * in the velocity vector which are defined via detectors (in {@link org.orekit.forces.maneuvers.ImpulseManeuver} and
 * {@link org.orekit.forces.maneuvers.FieldImpulseManeuver}).
 * It contains:
 *  - An attitude override, this is the attitude used during the maneuver, it can be different from the one
 *    used for propagation;
 *  - A maneuver triggers object from the trigger sub-package. It defines the triggers used to start and stop the maneuvers (dates or events for example).
 *  - A propulsion model from sub-package propulsion. It defines the thrust or ΔV, isp, flow rate etc.
 * Both the propulsion model and the maneuver triggers can contain parameter drivers (for estimation), as well as the attitude override if set.
 * The convention here is the following: drivers from propulsion model first, then maneuver triggers and if any the attitude override when calling the
 * method {@link #getParametersDrivers()}
 * @author Maxime Journot
 * @since 10.2
 */
public class TriggeredManeuver extends Maneuver {

    /** Maneuver triggers. */
    private final ManeuverTriggers maneuverTriggers;

    /** Generic maneuver constructor.
     * @param attitudeOverride attitude provider for the attitude during the maneuver, if set
     * @param maneuverTriggers maneuver triggers
     * @param propulsionModel propulsion model
     */
    public TriggeredManeuver(final AttitudeRotationModel attitudeOverride,
                             final ManeuverTriggers maneuverTriggers,
                             final PropulsionModel propulsionModel) {
        super(attitudeOverride, propulsionModel);
        this.maneuverTriggers = maneuverTriggers;
    }

    /** Get the name of the maneuver.
     * The name can be in the propulsion model, in the maneuver triggers or both.
     * If it is in both it should be the same since it refers to the same maneuver.
     * The name is inferred from the propulsion model first, then from the maneuver triggers if
     * the propulsion model had an empty name.
     * @return the name
     */
    @Override
    public String getName() {

        //FIXME: Potentially, throw an exception if both propulsion model
        // and maneuver triggers define a name but they are different
        String name = getPropulsionModel().getName();

        if (name.isEmpty()) {
            name = maneuverTriggers.getName();
        }
        return name;
    }

    /** Get the maneuver triggers.
     * @return the maneuver triggers
     */
    public ManeuverTriggers getManeuverTriggers() {
        return maneuverTriggers;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        super.init(initialState, target);
        maneuverTriggers.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target) {
        super.init(initialState, target);
        maneuverTriggers.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isFiring(final AbsoluteDate date, final double[] parameters) {
        return maneuverTriggers.isFiring(date, getManeuverTriggersParameters(parameters));
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> boolean isFiring(final FieldAbsoluteDate<T> date, final T[] parameters) {
        return maneuverTriggers.isFiring(date, getManeuverTriggersParameters(parameters));
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        // Event detectors are extracted from both the maneuver triggers and the propulsion model
        return Stream.concat(maneuverTriggers.getEventDetectors(), super.getEventDetectors());
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        // Event detectors are extracted from both the maneuver triggers and the propulsion model
        return Stream.concat(maneuverTriggers.getFieldEventDetectors(field), super.getFieldEventDetectors(field));
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        // Prepare final drivers' array
        final List<ParameterDriver> drivers = new ArrayList<>();

        // Convention: Propulsion drivers are given before maneuver triggers drivers
        // Add propulsion drivers first
        drivers.addAll(0, getPropulsionModel().getParametersDrivers());

        // Then maneuver triggers' drivers
        drivers.addAll(drivers.size(), maneuverTriggers.getParametersDrivers());

        // Then attitude override' drivers if defined
        if (getAttitudeOverride() != null) {
            drivers.addAll(getAttitudeOverride().getParametersDrivers());
        }

        // Return full drivers' array
        return drivers;
    }

    /** Extract maneuver triggers' parameters from the parameters' array called in by the ForceModel interface.
     *  Convention: Propulsion parameters are given before maneuver triggers parameters
     * @param parameters parameters' array called in by ForceModel interface
     * @return maneuver triggers' parameters
     */
    public double[] getManeuverTriggersParameters(final double[] parameters) {
        final int nbPropulsionModelDrivers = getPropulsionModel().getParametersDrivers().size();
        return Arrays.copyOfRange(parameters, nbPropulsionModelDrivers,
                                  nbPropulsionModelDrivers + maneuverTriggers.getParametersDrivers().size());
    }

    /** Extract maneuver triggers' parameters from the parameters' array called in by the ForceModel interface.
     *  Convention: Propulsion parameters are given before maneuver triggers parameters
     * @param parameters parameters' array called in by ForceModel interface
     * @param <T> extends CalculusFieldElement&lt;T&gt;
     * @return maneuver triggers' parameters
     */
    public <T extends CalculusFieldElement<T>> T[] getManeuverTriggersParameters(final T[] parameters) {
        final int nbPropulsionModelDrivers = getPropulsionModel().getParametersDrivers().size();
        return Arrays.copyOfRange(parameters, nbPropulsionModelDrivers,
                                  nbPropulsionModelDrivers + maneuverTriggers.getParametersDrivers().size());
    }

}
