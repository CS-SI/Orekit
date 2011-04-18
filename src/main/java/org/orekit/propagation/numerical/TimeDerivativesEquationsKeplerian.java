/* Copyright 2002-2011 CS Communication & Systèmes
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
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;

/** Implementation of the {@link TimeDerivativesEquations} interface for state arrays in
 * {@link KeplerianOrbit Keplerian parameters}.
 *
 * <p>It implements Gauss equations for Keplerian parameters. This implementation is specialized for
 * state vectors that have the following form:
 *   <pre>
 *     y[0] = a
 *     y[1] = e
 *     y[2] = i
 *     y[3] = &omega;
 *     y[4] = &Omega;
 *     y[5] = v
 *     y[6] = mass
 *   </pre>
 * where the six first parameters stands for the Keplerian parameters and the 7<sup>th</sup>
 * for the mass (kg) at the current time.
 * </p>
 * @see org.orekit.orbits.KeplerianOrbit
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class TimeDerivativesEquationsKeplerian extends TimeDerivativesEquations {

    /** Serializable UID. */
    private static final long serialVersionUID = -4177983430003341393L;

    /** Orbit frame. */
    private Frame orbitFrame;

    /** Orbit date. */
    private AbsoluteDate orbitDate;

    /** Jacobian of the orbital parameters with respect to the cartesian parameters. */
    private double[][] jacobian;

    /** Kepler evolution on true anomaly divided by sqrt(mu). */
    private double vKepler;

    /** Create a new instance.
     * @param orbit current orbit parameters
     */
    public TimeDerivativesEquationsKeplerian(final KeplerianOrbit orbit) {
        jacobian   = new double[6][6];
        orbitFrame = orbit.getFrame();
        orbitDate  = orbit.getDate();
    }

    /** {@inheritDoc} */
    public void initDerivatives(final double[] yDot, final Orbit orbit)
        throws PropagationException {


        final KeplerianOrbit keplerianOrbit = (KeplerianOrbit) orbit;
        orbitFrame = orbit.getFrame();
        orbitDate  = orbit.getDate();
        keplerianOrbit.getJacobianWrtCartesian(PositionAngle.TRUE, jacobian);

        // store derivatives array reference
        this.storedYDot = yDot;

        // initialize derivatives to zero
        Arrays.fill(storedYDot, 0.0);

        // Kepler natural evolution without the mu part
        final double e     = keplerianOrbit.getE();
        final double oMe2  = FastMath.abs(1 - e * e);
        final double absA  = FastMath.abs(keplerianOrbit.getA());
        final double nOSmu = FastMath.sqrt(1 / absA) / absA;
        final double ksi   = 1 + e * FastMath.cos(keplerianOrbit.getTrueAnomaly());
        vKepler = nOSmu * ksi * ksi / (FastMath.sqrt(oMe2) * oMe2);

    }

    /** {@inheritDoc} */
    public void addKeplerContribution(final double mu) {
        storedYDot[5] += vKepler * FastMath.sqrt(mu);
    }

    /** {@inheritDoc} */
    public void addXYZAcceleration(final double x, final double y, final double z) {
        for (int i = 0; i < 6; ++i) {
            final double[] jRow = jacobian[i];
            storedYDot[i] += jRow[3] * x + jRow[4] * y + jRow[5] * z;
        }
    }

    /** {@inheritDoc} */
    public void addAcceleration(final Vector3D gamma, final Frame frame)
        throws OrekitException {
        final Transform t = frame.getTransformTo(orbitFrame, orbitDate);
        final Vector3D gammInRefFrame = t.transformVector(gamma);
        addXYZAcceleration(gammInRefFrame.getX(), gammInRefFrame.getY(), gammInRefFrame.getZ());
    }

}
