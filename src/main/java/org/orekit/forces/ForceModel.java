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
package org.orekit.forces;

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.FieldTimeDerivativesEquations;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

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
 * propagator thanks to their {@link #getEventsDetectors()} method. This method
 * is called once just before propagation starts. The events states will be checked by
 * the propagator to ensure accurate propagation and proper events handling.
 * </p>
 *
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 */
public interface ForceModel {

    /**
     * Initialize the force model at the start of propagation. This method will be called
     * before any calls to {@link #addContribution(SpacecraftState, TimeDerivativesEquations)},
     * {@link #addContribution(FieldSpacecraftState, FieldTimeDerivativesEquations)},
     * {@link #acceleration(SpacecraftState, double[])} or {@link #acceleration(FieldSpacecraftState, RealFieldElement[])}
     *
     * <p> The default implementation of this method does nothing.</p>
     *
     * @param initialState spacecraft state at the start of propagation.
     * @param target       date of propagation. Not equal to {@code initialState.getDate()}.
     */
    default void init(SpacecraftState initialState, AbsoluteDate target) {
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
        adder.addNonKeplerianAcceleration(acceleration(s, getParameters()));
    }

    /** Compute the contribution of the force model to the perturbing
     * acceleration.
     * @param s current state information: date, kinematics, attitude
     * @param adder object where the contribution should be added
     * @param <T> type of the elements
     */
    default <T extends RealFieldElement<T>> void addContribution(FieldSpacecraftState<T> s, FieldTimeDerivativesEquations<T> adder) {
        adder.addNonKeplerianAcceleration(acceleration(s, getParameters(s.getDate().getField())));
    }

    /** Get force model parameters.
     * @return force model parameters
     * @since 9.0
     */
    default double[] getParameters() {
        final ParameterDriver[] drivers = getParametersDrivers();
        final double[] parameters = new double[drivers.length];
        for (int i = 0; i < drivers.length; ++i) {
            parameters[i] = drivers[i].getValue();
        }
        return parameters;
    }

    /** Get force model parameters.
     * @param field field to which the elements belong
     * @param <T> type of the elements
     * @return force model parameters
     * @since 9.0
     */
    default <T extends RealFieldElement<T>> T[] getParameters(final Field<T> field) {
        final ParameterDriver[] drivers = getParametersDrivers();
        final T[] parameters = MathArrays.buildArray(field, drivers.length);
        for (int i = 0; i < drivers.length; ++i) {
            parameters[i] = field.getZero().add(drivers[i].getValue());
        }
        return parameters;
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
     * @param parameters values of the force model parameters
     * @return acceleration in same frame as state
     * @since 9.0
     */
    Vector3D acceleration(SpacecraftState s, double[] parameters);

    /** Compute acceleration.
     * @param s current state information: date, kinematics, attitude
     * @param parameters values of the force model parameters
     * @return acceleration in same frame as state
     * @param <T> type of the elements
     * @since 9.0
     */
    <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(FieldSpacecraftState<T> s, T[] parameters);

    /** Get the discrete events related to the model.
     * @return stream of events detectors
     */
    Stream<EventDetector> getEventsDetectors();

    /** Get the discrete events related to the model.
     * @param field field to which the state belongs
     * @param <T> extends RealFieldElement&lt;T&gt;
     * @return stream of events detectors
     */
    <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(Field<T> field);

    /** Get the drivers for force model parameters.
     * @return drivers for force model parameters
     * @since 8.0
     */
    ParameterDriver[] getParametersDrivers();

    /** Get parameter value from its name.
     * @param name parameter name
     * @return parameter value
          * @since 8.0
     */
    ParameterDriver getParameterDriver(String name);

    /** Check if a parameter is supported.
     * <p>Supported parameters are those listed by {@link #getParametersDrivers()}.</p>
     * @param name parameter name to check
     * @return true if the parameter is supported
     * @see #getParametersDrivers()
     */
    boolean isSupported(String name);

}
