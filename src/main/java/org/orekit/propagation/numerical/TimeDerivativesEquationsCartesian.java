/* Copyright 2002-2010 CS Centre National d'Études Spatiales
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

import java.util.Arrays;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;

/** Implementation of the {@link TimeDerivativesEquations} interface for state arrays in cartesian coordinates
*
* <p>It implements Gauss equations for cartesian parameters. This implementation is specialized
* for state vectors that have the following form:
*   <pre>
*     y[0] = x
*     y[1] = y
*     y[2] = z
*     y[3] = v<sub>x</sub>
*     y[4] = v<sub>y</sub>
*     y[5] = v<sub>z</sub>
*     y[6] = mass
*   </pre>
* where the six first parameters stands for the cartesian parameters and the 7<sup>th</sup>
* for the mass (kg) at the current time.
* </p>
* @see org.orekit.orbits.CartesianOrbit
* @see org.orekit.propagation.numerical.NumericalPropagator
* @author Luc Maisonobe
* @author Fabien Maussion
* @author V&eacute;ronique Pommier-Maurussane
* @version $Revision$ $Date$
*/
public class TimeDerivativesEquationsCartesian extends TimeDerivativesEquations {

    /** Serializable UID. */
    private static final long serialVersionUID = -1882870530923825699L;

    /** Orbital parameters. */
    private CartesianOrbit storedParameters;

    /** Create a new instance.
     * @param orbit current orbit parameters
     */
    public TimeDerivativesEquationsCartesian(final CartesianOrbit orbit) {
        this.storedParameters = orbit;
    }

    /** {@inheritDoc} */
    public void addAcceleration(final Vector3D gamma, final Frame frame)
        throws OrekitException {
        final Transform t = frame.getTransformTo(storedParameters.getFrame(),
                                                 storedParameters.getDate());
        final Vector3D gammInRefFrame = t.transformVector(gamma);
        addXYZAcceleration(gammInRefFrame.getX(), gammInRefFrame.getY(), gammInRefFrame.getZ());
    }

    /** {@inheritDoc} */
    public void addKeplerContribution(final double mu) {
        final Vector3D position = storedParameters.getPVCoordinates().getPosition();
        final double r2         = position.getNormSq();
        final double coeff      = -mu / (r2 * FastMath.sqrt(r2));
        storedYDot[3] += coeff * position.getX();
        storedYDot[4] += coeff * position.getY();
        storedYDot[5] += coeff * position.getZ();
    }

    /** {@inheritDoc} */
    public void addXYZAcceleration(final double x, final double y, final double z) {
        storedYDot[3] += x;
        storedYDot[4] += y;
        storedYDot[5] += z;
    }

    /** {@inheritDoc} */
    void initDerivatives(final double[] yDot, final Orbit orbit)
        throws PropagationException {

        storedParameters = (CartesianOrbit) orbit;

        // store derivatives array reference
        this.storedYDot = yDot;

        // initialize derivatives to zero
        Arrays.fill(storedYDot, 0.0);

        // position derivative is velocity
        final Vector3D velocity = storedParameters.getPVCoordinates().getVelocity();
        storedYDot[0] = velocity.getX();
        storedYDot[1] = velocity.getY();
        storedYDot[2] = velocity.getZ();

    }

}
