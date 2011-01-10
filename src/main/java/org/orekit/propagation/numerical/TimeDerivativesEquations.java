/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.propagation.numerical;

import java.io.Serializable;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;

/** Interface summing up the contribution of several forces into orbit and mass derivatives.
 *
 * <p>The aim of this interface is to gather the contributions of various perturbing
 * forces expressed as accelerations into one set of time-derivatives of
 * {@link org.orekit.orbits.Orbit} plus one mass derivatives.
 * It implements Gauss equations for different kind of parameters.</p>
 *  <p>
 * The state vector handled internally has the form that follows:
 *   <pre>
 *     y[0] 
 *     y[1]
 *     y[2]
 *     y[3]
 *     y[4]
 *     y[5]
 *     y[6]
 *   </pre>
 * where the six firsts parameters stands for the chosen parameters and the 7th
 * for the mass (kg) at the current time.
 * </p>
 * <p>The proper way to use this class is to have the object implementing the
 * FirstOrderDifferentialEquations interface do the following calls each time
 * the computeDerivatives method is called:
 * <ul>
 *   <li>
 *     reinitialize the instance using the
 *     {@link #initDerivatives(double[], Orbit)} method
 *   </li>
 *   <li>
 *     pass the instance to each force model in turn so that they can put their
 *     own contributions using the various AddXxxAcceleration methods
 *   </li>
 *   <li>
 *     finalize the derivatives by adding the Kepler natural evolution
 *     contribution
 *   </li>
 * </ul>
 * </p>
 * @see org.orekit.orbits.Orbit
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public abstract class TimeDerivativesEquations implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 4183908052500627555L;

    // CHECKSTYLE: stop VisibilityModifierCheck

    /** Reference to the derivatives array to initialize. */
    protected double[] storedYDot;

    //CHECKSTYLE: resume VisibilityModifierCheck

    /** Create a new instance.
     */
    protected TimeDerivativesEquations() {
    }

    /** Initialize all derivatives to zero.
     * @param yDot reference to the array where to put the derivatives.
     * @param currentOrbit current orbit parameters
     * @exception PropagationException if the orbit evolve out of supported range
     */
    abstract void initDerivatives(double[] yDot, Orbit currentOrbit)
        throws PropagationException;

    /** Add the contribution of the Kepler evolution.
     * <p>Since the Kepler evolution is the most important, it should
     * be added after all the other ones, in order to improve
     * numerical accuracy.</p>
     * @param mu central body gravitational constant
     */
    public abstract void addKeplerContribution(final double mu);

    /** Add the contribution of an acceleration expressed in the inertial frame
     *  (it is important to make sure this acceleration is defined in the
     *  same frame as the orbit) .
     * @param x acceleration along the X axis (m/s<sup>2</sup>)
     * @param y acceleration along the Y axis (m/s<sup>2</sup>)
     * @param z acceleration along the Z axis (m/s<sup>2</sup>)
     */
    public abstract void addXYZAcceleration(final double x, final double y, final double z);

    /** Add the contribution of an acceleration expressed in some inertial frame.
     * @param gamma acceleration vector (m/s<sup>2</sup>)
     * @param frame frame in which acceleration is defined (must be an inertial frame)
     * @exception OrekitException if frame transforms cannot be computed
     */
    public abstract void addAcceleration(final Vector3D gamma, final Frame frame) throws OrekitException;

    /** Add the contribution of the flow rate (dm/dt).
     * @param q the flow rate, must be negative (dm/dt)
     * @exception IllegalArgumentException if flow-rate is positive
     */
    public void addMassDerivative(final double q) {
        if (q > 0) {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.POSITIVE_FLOW_RATE, q);
        }
        storedYDot[6] += q;
    }


}
