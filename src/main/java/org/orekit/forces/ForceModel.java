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
package org.orekit.forces;

import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventDetectorsProvider;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.FieldTimeDerivativesEquations;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriversProvider;

/** This interface represents a force modifying spacecraft motion.
 *
 * <p>
 * Objects implementing this interface are intended to be added to a
 * {@link org.orekit.propagation.numerical.NumericalPropagator numerical propagator}
 * before the propagation is started.
 *
 * <p>
 * The propagator will call at each step the {@link #addContribution(SpacecraftState,
 * TimeDerivativesEquations)} method. The force model instance will extract all the
 * state data it needs (date, position, velocity, frame, attitude, mass) from the first
 * parameter. From these state data, it will compute the perturbing acceleration. It
 * will then add this acceleration to the second parameter which will take thins
 * contribution into account and will use the Gauss equations to evaluate its impact
 * on the global state derivative.
 * </p>
 * <p>
 * Force models which create discontinuous acceleration patterns (typically for maneuvers
 * start/stop or solar eclipses entry/exit) must provide one or more {@link
 * org.orekit.propagation.events.EventDetector events detectors} to the
 * propagator thanks to their {@link #getEventDetectors()} method. This method
 * is called once just before propagation starts. The events states will be checked by
 * the propagator to ensure accurate propagation and proper events handling.
 * </p>
 *
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Melina Vanel
 */
public interface ForceModel extends ParameterDriversProvider, EventDetectorsProvider {

    /**
     * Initialize the force model at the start of propagation. This method will be called
     * before any calls to {@link #addContribution(SpacecraftState, TimeDerivativesEquations)},
     * {@link #addContribution(FieldSpacecraftState, FieldTimeDerivativesEquations)},
     * {@link #acceleration(SpacecraftState, double[])} or {@link #acceleration(FieldSpacecraftState, CalculusFieldElement[])}
     *
     * <p> The default implementation of this method does nothing.</p>
     *
     * @param initialState spacecraft state at the start of propagation.
     * @param target       date of propagation. Not equal to {@code initialState.getDate()}.
     */
    default void init(SpacecraftState initialState, AbsoluteDate target) {
    }

    /**
     * Initialize the force model at the start of propagation. This method will be called
     * before any calls to {@link #addContribution(SpacecraftState, TimeDerivativesEquations)},
     * {@link #addContribution(FieldSpacecraftState, FieldTimeDerivativesEquations)},
     * {@link #acceleration(SpacecraftState, double[])} or {@link #acceleration(FieldSpacecraftState, CalculusFieldElement[])}
     *
     * <p> The default implementation of this method does nothing.</p>
     *
     * @param initialState spacecraft state at the start of propagation.
     * @param target       date of propagation. Not equal to {@code initialState.getDate()}.
     * @param <T> type of the elements
     */
    default <T extends CalculusFieldElement<T>> void init(FieldSpacecraftState<T> initialState, FieldAbsoluteDate<T> target) {
        init(initialState.toSpacecraftState(), target.toAbsoluteDate());
    }

    /** {@inheritDoc}.*/
    @Override
    default Stream<EventDetector> getEventDetectors() {
        return getEventDetectors(getParametersDrivers());
    }

    /** {@inheritDoc}.*/
    @Override
    default <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {
        return getFieldEventDetectors(field, getParametersDrivers());
    }

    /** Compute the contribution of the force model to the perturbing
     * acceleration.
     * <p>
     * The default implementation simply adds the {@link #acceleration(SpacecraftState, double[]) acceleration}
     * as a non-Keplerian acceleration.
     * </p>
     * @param s current state information: date, kinematics, attitude
     * @param adder object where the contribution should be added
     */
    default void addContribution(SpacecraftState s, TimeDerivativesEquations adder) {
        adder.addNonKeplerianAcceleration(acceleration(s, getParameters(s.getDate())));
    }

    /** Compute the contribution of the force model to the perturbing
     * acceleration.
     * @param s current state information: date, kinematics, attitude
     * @param adder object where the contribution should be added
     * @param <T> type of the elements
     */
    default <T extends CalculusFieldElement<T>> void addContribution(FieldSpacecraftState<T> s, FieldTimeDerivativesEquations<T> adder) {
        adder.addNonKeplerianAcceleration(acceleration(s, getParameters(s.getDate().getField(), s.getDate())));
    }

    /** Check if force models depends on position only.
     * @return true if force model depends on position only, false
     * if it depends on velocity, either directly or due to a dependency
     * on attitude
     * @since 9.0
     */
    boolean dependsOnPositionOnly();

    /** Compute acceleration.
     * @param s current state information: date, kinematics, attitude
     * @param parameters values of the force model parameters at state date,
     * only 1 value for each parameterDriver
     * @return acceleration in same frame as state
     * @since 9.0
     */
    Vector3D acceleration(SpacecraftState s, double[] parameters);

    /** Compute acceleration.
     * @param s current state information: date, kinematics, attitude
     * @param parameters values of the force model parameters at state date,
     * only 1 value for each parameterDriver
     * @return acceleration in same frame as state
     * @param <T> type of the elements
     * @since 9.0
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(FieldSpacecraftState<T> s, T[] parameters);
}
