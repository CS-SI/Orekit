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

package org.orekit.forces.maneuvers.propulsion;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.forces.maneuvers.Control3DVectorCostType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/** This abstract class simply serve as a container for a constant thrust maneuver.
 * It re-writes all spacecraft dependent methods from {@link ThrustPropulsionModel}
 * and removes their dependencies to current spacecraft state.
 * Indeed since the thrust is constant (i.e. not variable during the maneuver), most of the
 * calculated parameters (thrust vector, flow rate etc.) do not depend on current spacecraft state.
 * @author Maxime Journot
 * @since 10.2
 */
public abstract class AbstractConstantThrustPropulsionModel implements ThrustPropulsionModel {

    /** Default control vector cost type. */
    static final Control3DVectorCostType DEFAULT_CONTROL_3D_VECTOR_COST_TYPE = Control3DVectorCostType.TWO_NORM;

    /** Initial thrust vector (N) in S/C frame, when building the object. */
    private final Vector3D initialThrustVector;

    /** Initial flow rate (kg/s), when building the object. */
    private final double initialFlowRate;

    /** Type of norm linking thrust vector to mass flow rate. */
    private final Control3DVectorCostType control3DVectorCostType;

    /** User-defined name of the maneuver.
     * This String attribute is empty by default.
     * It is added as a prefix to the parameter drivers of the maneuver.
     * The purpose is to differentiate between drivers in the case where several maneuvers
     * were added to a propagator force model.
     * Additionally, the user can retrieve the whole maneuver by looping on the force models of a propagator,
     * scanning for its name.
     * @since 9.2
     */
    private final String name;

    /** Generic constructor.
     * @param thrust initial thrust value (N)
     * @param isp initial isp value (s)
     * @param direction initial thrust direction in S/C frame
     * @param control3DVectorCostType control cost type
     * @param name name of the maneuver
     * @since 12.0
     */
    public AbstractConstantThrustPropulsionModel(final double thrust,
                                                 final double isp,
                                                 final Vector3D direction,
                                                 final Control3DVectorCostType control3DVectorCostType,
                                                 final String name) {
        this.name = name;
        this.initialThrustVector = direction.normalize().scalarMultiply(thrust);
        this.control3DVectorCostType = control3DVectorCostType;
        this.initialFlowRate = -control3DVectorCostType.evaluate(initialThrustVector) / (Constants.G0_STANDARD_GRAVITY * isp);
    }

    /** Constructor with default control cost type.
     * @param thrust initial thrust value (N)
     * @param isp initial isp value (s)
     * @param direction initial thrust direction in S/C frame
     * @param name name of the maneuver
     */
    public AbstractConstantThrustPropulsionModel(final double thrust,
                                                 final double isp,
                                                 final Vector3D direction,
                                                 final String name) {
        this(thrust, isp, direction, DEFAULT_CONTROL_3D_VECTOR_COST_TYPE, name);
    }

    /** Get the initial thrust vector.
     * @return the initial thrust vector
     */
    protected Vector3D getInitialThrustVector() {
        return initialThrustVector;
    }

    /** Get the initial flow rate.
     * @return the initial flow rate
     */
    protected double getInitialFlowRate() {
        return initialFlowRate;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public Control3DVectorCostType getControl3DVectorCostType() {
        return control3DVectorCostType;
    }

    /** Get the specific impulse.
     * @return specific impulse (s), will throw exception if
     * used on PDriver having several driven values, because
     * in this case a date is needed.
     */
    public double getIsp() {
        final double flowRate = getFlowRate();
        return -control3DVectorCostType.evaluate(getThrustVector()) / (Constants.G0_STANDARD_GRAVITY * flowRate);
    }

    /** Get the specific impulse at given date.
     * @param date date at which the Isp wants to be known
     * @return specific impulse (s).
     */
    public double getIsp(final AbsoluteDate date) {
        final double flowRate = getFlowRate(date);
        return -control3DVectorCostType.evaluate(getThrustVector(date)) / (Constants.G0_STANDARD_GRAVITY * flowRate);
    }

    /** Get the thrust direction in S/C frame.
     * @param date date at which the direction wants to be known
     * @return the thrust direction in S/C frame
     */
    public Vector3D getDirection(final AbsoluteDate date) {
        return getThrustVector(date).normalize();
    }

    /** Get the thrust direction in S/C frame.
     * @return the thrust direction in S/C frame,  will throw exception if
     * used on PDriver having several driven values, because
     * in this case a date is needed.
     */
    public Vector3D getDirection() {
        return getThrustVector().normalize();
    }

    /** Get the thrust magnitude (N).
     * @return the thrust value (N), will throw
     * an exception if called of a driver having several
     * values driven
     */
    public double getThrustMagnitude() {
        return getThrustVector().getNorm();
    }

    /** Get the thrust magnitude (N) at given date.
     * @param date date at which the thrust vector wants to be known,
     * often the date parameter will not be important and can be whatever
     * if the thrust parameter driver as only value estimated over the all
     * orbit determination interval
     * @return the thrust value (N)
     */
    public double getThrustMagnitude(final AbsoluteDate date) {
        return getThrustVector(date).getNorm();
    }

    /** {@inheritDoc}
     * Here the thrust vector do not depend on current S/C state.
     */
    @Override
    public Vector3D getThrustVector(final SpacecraftState s) {
        // Call the abstract function that do not depend on current S/C state
        return getThrustVector(s.getDate());
    }

    /** {@inheritDoc}
     * Here the flow rate do not depend on current S/C state
     */
    @Override
    public double getFlowRate(final SpacecraftState s) {
        // Call the abstract function that do not depend on current S/C state
        return getFlowRate(s.getDate());
    }

    /** {@inheritDoc}
     * Here the thrust vector do not depend on current S/C state.
     */
    @Override
    public Vector3D getThrustVector(final SpacecraftState s, final double[] parameters) {
        // Call the abstract function that do not depend on current S/C state
        return getThrustVector(parameters);
    }

    /** {@inheritDoc}
     * Here the flow rate do not depend on current S/C state
     */
    public double getFlowRate(final SpacecraftState s, final double[] parameters) {
        // Call the abstract function that do not depend on current S/C state
        return getFlowRate(parameters);
    }

    /** {@inheritDoc}
     * Here the thrust vector do not depend on current S/C state.
     */
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(final FieldSpacecraftState<T> s,
                                                                            final T[] parameters) {
        // Call the abstract function that do not depend on current S/C state
        return getThrustVector(parameters);
    }

    /** {@inheritDoc}
     * Here the flow rate do not depend on current S/C state
     */
    public <T extends CalculusFieldElement<T>> T getFlowRate(final FieldSpacecraftState<T> s, final T[] parameters) {
        // Call the abstract function that do not depend on current S/C state
        return getFlowRate(parameters);
    }

    /** Get the thrust vector in spacecraft frame (N).
     * Here it does not depend on current S/C state.
     * @return thrust vector in spacecraft frame (N),
     * will throw an exception if used on driver
     * containing several value spans
     */
    public abstract Vector3D getThrustVector();

    /** Get the thrust vector in spacecraft frame (N).
     * Here it does not depend on current S/C state.
     * @param date date at which the thrust vector wants to be known,
     * often the date parameter will not be important and can be whatever
     * if the thrust parameter driver as only value estimated over the all
     * orbit determination interval
     * @return thrust vector in spacecraft frame (N)
     */
    public abstract Vector3D getThrustVector(AbsoluteDate date);

    /** Get the flow rate (kg/s).
     * Here it does not depend on current S/C.
     * @return flow rate (kg/s)
     * will throw an exception if used on driver
     * containing several value spans
     */
    public abstract double getFlowRate();

    /** Get the flow rate (kg/s).
     * Here it does not depend on current S/C.
     * @param date date at which the thrust vector wants to be known,
     * often the date parameter will not be important and can be whatever
     * if the thrust parameter driver as only value estimated over the all
     * orbit determination interval
     * @return flow rate (kg/s)
     */
    public abstract double getFlowRate(AbsoluteDate date);

    /** Get the thrust vector in spacecraft frame (N).
     * Here it does not depend on current S/C state.
     * @param parameters propulsion model parameters
     * @return thrust vector in spacecraft frame (N)
     */
    public abstract Vector3D getThrustVector(double[] parameters);

    /** Get the flow rate (kg/s).
     * Here it does not depend on current S/C state.
     * @param parameters propulsion model parameters
     * @return flow rate (kg/s)
     */
    public abstract double getFlowRate(double[] parameters);

    /** Get the thrust vector in spacecraft frame (N).
     * Here it does not depend on current S/C state.
     * @param parameters propulsion model parameters
     * @param <T> extends CalculusFieldElement&lt;T&gt;
     * @return thrust vector in spacecraft frame (N)
     */
    public abstract <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(T[] parameters);

    /** Get the flow rate (kg/s).
     * Here it does not depend on current S/C state.
     * @param parameters propulsion model parameters
     * @param <T> extends CalculusFieldElement&lt;T&gt;
     * @return flow rate (kg/s)
     */
    public abstract <T extends CalculusFieldElement<T>> T getFlowRate(T[] parameters);
}
