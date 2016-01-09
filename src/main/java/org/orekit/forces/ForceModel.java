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
package org.orekit.forces;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.ode.ParameterizedODE;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;

/** This interface represents a force modifying spacecraft motion.
 *
 * <p>
 * Objects implementing this interface are intended to be added to a
 * {@link org.orekit.propagation.numerical.NumericalPropagator numerical propagator}
 * before the propagation is started.
 * <p>
 * <p>
 * The propagator will call at each step the {@link #addContribution(SpacecraftState,
 * TimeDerivativesEquations)} method. The force model instance will extract all the
 * state data it needs (date,position, velocity, frame, attitude, mass) from the first
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
public interface ForceModel extends ParameterizedODE {

    /** Compute the contribution of the force model to the perturbing
     * acceleration.
     * @param s current state information: date, kinematics, attitude
     * @param adder object where the contribution should be added
     * @exception OrekitException if some specific error occurs
     */
    void addContribution(SpacecraftState s, TimeDerivativesEquations adder)
        throws OrekitException;

    /** Compute acceleration derivatives with respect to state parameters.
     * <p>
     * The derivatives should be computed with respect to position, velocity
     * and optionnaly mass. The input parameters already take into account the
     * free parameters (6 or 7 depending on derivation with respect to mass
     * being considered or not) and order (always 1). Free parameters at indices
     * 0, 1 and 2 correspond to derivatives with respect to position. Free
     * parameters at indices 3, 4 and 5 correspond to derivatives with respect
     * to velocity. Free parameter at index 6 (if present) corresponds to
     * to derivatives with respect to mass.
     * </p>
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param velocity velocity of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @param mass spacecraft mass
     * @return acceleration with all derivatives specified by the input parameters own derivatives
     * @exception OrekitException if derivatives cannot be computed
     * @since 6.0
     */
    FieldVector3D<DerivativeStructure> accelerationDerivatives(AbsoluteDate date, Frame frame,
                                       FieldVector3D<DerivativeStructure> position, FieldVector3D<DerivativeStructure> velocity,
                                       FieldRotation<DerivativeStructure> rotation, DerivativeStructure mass)
        throws OrekitException;

    /** Compute acceleration derivatives with respect to additional parameters.
     * @param s spacecraft state
     * @param paramName name of the parameter with respect to which derivatives are required
     * @return acceleration with all derivatives specified by the input parameters own derivatives
     * @exception OrekitException if derivatives cannot be computed
     * @since 6.0
     */
    FieldVector3D<DerivativeStructure> accelerationDerivatives(SpacecraftState s, String paramName)
        throws OrekitException;

    /** Get the discrete events related to the model.
     * @return array of events detectors or null if the model is not
     * related to any discrete events
     */
    EventDetector[] getEventsDetectors();

}
