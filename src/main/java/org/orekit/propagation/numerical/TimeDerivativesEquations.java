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
package org.orekit.propagation.numerical;

import org.hipparchus.geometry.euclidean.threed.Vector3D;

/** Interface summing up the contribution of several forces into orbit and mass derivatives.
 *
 * <p>The aim of this interface is to gather the contributions of various perturbing
 * forces expressed as accelerations into one set of time-derivatives of
 * {@link org.orekit.orbits.Orbit} plus one mass derivatives. It implements Gauss
 * equations for different kind of parameters.</p>
 * <p>An implementation of this interface is automatically provided by {@link
 * org.orekit.propagation.integration.AbstractIntegratedPropagator integration-based
 * propagators}, which are either semi-analytical or numerical propagators.
 * </p>
 * @see org.orekit.forces.ForceModel
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
public interface TimeDerivativesEquations {

    /** Add the contribution of the Kepler evolution.
     * <p>Since the Kepler evolution is the most important, it should
     * be added after all the other ones, in order to improve
     * numerical accuracy.</p>
     * @param mu central body gravitational constant
     */
    void addKeplerContribution(double mu);

    /** Add the contribution of a non-Keplerian acceleration.
     * @param gamma acceleration vector in the same inertial frame the spacecraft state is defined in (m/s²)
          * @since 9.0
     */
    void addNonKeplerianAcceleration(Vector3D gamma);

    /** Add the contribution of the flow rate (dm/dt).
     * @param q the flow rate, must be negative (dm/dt)
     * @exception IllegalArgumentException if flow-rate is positive
     */
    void addMassDerivative(double q);


}
