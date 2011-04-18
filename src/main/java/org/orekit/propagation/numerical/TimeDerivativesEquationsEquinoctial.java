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
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.utils.PVCoordinates;

/** Implementation of the {@link TimeDerivativesEquations} interface for state arrays in
 * {@link EquinoctialOrbit equinoctial parameters}.
 *
 * <p>It implements Gauss equations for equinoctial parameters. This implementation is specialized for
 * state vectors that have the following form:
 *   <pre>
 *     y[0] = a
 *     y[1] = e<sub>x</sub>
 *     y[2] = e<sub>y</sub>
 *     y[3] = h<sub>x</sub>
 *     y[4] = h<sub>y</sub>
 *     y[5] = l<sub>v</sub>
 *     y[6] = mass
 *   </pre>
 * where the six first parameters stands for the equinoctial parameters and the 7<sup>th</sup>
 * for the mass (kg) at the current time.
 * </p>
 * @see org.orekit.orbits.EquinoctialOrbit
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @author Luc Maisonobe
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class TimeDerivativesEquationsEquinoctial extends TimeDerivativesEquations {

    /** Serializable UID. */
    private static final long serialVersionUID = -7676218190057010433L;

    /** First vector of the (t, n, w) local orbital frame. */
    private Vector3D lofT;

    /** Second vector of the (t, n, w) local orbital frame. */
    private Vector3D lofN;

    /** Third vector of the (t, n, w) local orbital frames. */
    private Vector3D lofW;

    // CHECKSTYLE: stop JavadocVariable check

    /** Multiplicative coefficients for the perturbing accelerations along lofT. */
    private double aT;
    private double exT;
    private double eyT;

    /** Multiplicative coefficients for the perturbing accelerations along lofN. */
    private double exN;
    private double eyN;

    /** Multiplicative coefficients for the perturbing accelerations along lofW. */
    private double eyW;
    private double exW;
    private double hxW;
    private double hyW;
    private double lvW;

    // CHECKSTYLE: resume JavadocVariable check

    /** Kepler evolution on true latitude argument. */
    private double lvKepler;

    /** Orbital parameters. */
    private EquinoctialOrbit storedParameters;

    /** Create a new instance.
     * @param orbit current orbit parameters
     */
    public TimeDerivativesEquationsEquinoctial(final EquinoctialOrbit orbit) {
        this.storedParameters = orbit;
        lofT = Vector3D.ZERO;
        lofN = Vector3D.ZERO;
        lofW = Vector3D.ZERO;
        updateOrbitalFrames();
    }

    /** Update the orbital frames. */
    private void updateOrbitalFrames() {
        // get the position/velocity vectors
        final PVCoordinates pvCoordinates = storedParameters.getPVCoordinates();

        // compute (t, n, w) local orbital frame
        lofW = pvCoordinates.getMomentum().normalize();
        lofT = pvCoordinates.getVelocity().normalize();
        lofN = Vector3D.crossProduct(lofW, lofT);
    }


    /** {@inheritDoc} */
    public void initDerivatives(final double[] yDot, final Orbit orbit)
        throws PropagationException {


        storedParameters = (EquinoctialOrbit) orbit;
        updateOrbitalFrames();

        // store derivatives array reference
        this.storedYDot = yDot;

        // initialize derivatives to zero
        Arrays.fill(storedYDot, 0.0);

        // intermediate variables
        final double ex  = storedParameters.getEquinoctialEx();
        final double ey  = storedParameters.getEquinoctialEy();
        final double ex2 = ex * ex;
        final double ey2 = ey * ey;
        final double e2  = ex2 + ey2;
        final double e   = FastMath.sqrt(e2);
        if (e >= 1) {
            throw new PropagationException(OrekitMessages.ORBIT_BECOMES_HYPERBOLIC_UNABLE_TO_PROPAGATE_FURTHER, e);
        }

        // intermediate variables
        final double oMe2        = (1 - e) * (1 + e);
        final double epsilon     = FastMath.sqrt(oMe2);
        final double a           = storedParameters.getA();
        final double mu          = orbit.getMu();
        final double na          = FastMath.sqrt(mu / a);
        final double n           = na / a;
        final double lv          = storedParameters.getLv();
        final double cLv         = FastMath.cos(lv);
        final double sLv         = FastMath.sin(lv);
        final double excLv       = ex * cLv;
        final double eysLv       = ey * sLv;
        final double excLvPeysLv = excLv + eysLv;
        final double ksi         = 1 + excLvPeysLv;
        final double nu          = ex * sLv - ey * cLv;
        final double sqrt        = FastMath.sqrt(ksi * ksi + nu * nu);
        final double hx          = storedParameters.getHx();
        final double hy          = storedParameters.getHy();
        final double h2          = hx * hx  + hy * hy;
        final double oPh2        = 1 + h2;
        final double hxsLvMhycLv = hx * sLv - hy * cLv;

        final double epsilonOnNA        = epsilon / na;
        final double epsilonOnNAKsi     = epsilonOnNA / ksi;
        final double epsilonOnNAKsiSqrt = epsilonOnNAKsi / sqrt;
        final double tOnEpsilonN        = 2 / (n * epsilon);
        final double tEpsilonOnNASqrt   = 2 * epsilonOnNA / sqrt;
        final double epsilonOnNAKsit    = epsilonOnNA / (2 * ksi);

        // Kepler natural evolution without the mu part
        lvKepler = n * ksi * ksi / (FastMath.sqrt(mu) * oMe2 * epsilon);

        // coefficients along T
        aT  = tOnEpsilonN * sqrt;
        exT = tEpsilonOnNASqrt * (ex + cLv);
        eyT = tEpsilonOnNASqrt * (ey + sLv);

        // coefficients along N
        exN = -epsilonOnNAKsiSqrt * (2 * ey * ksi + oMe2 * sLv);
        eyN =  epsilonOnNAKsiSqrt * (2 * ex * ksi + oMe2 * cLv);

        // coefficients along W
        lvW =  epsilonOnNAKsi * hxsLvMhycLv;
        exW = -ey * lvW;
        eyW =  ex * lvW;
        hxW =  epsilonOnNAKsit * oPh2 * cLv;
        hyW =  epsilonOnNAKsit * oPh2 * sLv;

    }

    /** {@inheritDoc} */
    public void addKeplerContribution(final double mu) {
        storedYDot[5] += lvKepler * FastMath.sqrt(mu);
    }

    /** Add the contribution of an acceleration expressed in (t, n, w)
     * local orbital frame.
     * @param t acceleration along the T axis (m/s<sup>2</sup>)
     * @param n acceleration along the N axis (m/s<sup>2</sup>)
     * @param w acceleration along the W axis (m/s<sup>2</sup>)
     */
    private void addTNWAcceleration(final double t, final double n, final double w) {
        storedYDot[0] += aT  * t;
        storedYDot[1] += exT * t + exN * n + exW * w;
        storedYDot[2] += eyT * t + eyN * n + eyW * w;
        storedYDot[3] += hxW * w;
        storedYDot[4] += hyW * w;
        storedYDot[5] += lvW * w;
    }


    /** {@inheritDoc} */
    public void addXYZAcceleration(final double x, final double y, final double z) {
        addTNWAcceleration(x * lofT.getX() + y * lofT.getY() + z * lofT.getZ(),
                           x * lofN.getX() + y * lofN.getY() + z * lofN.getZ(),
                           x * lofW.getX() + y * lofW.getY() + z * lofW.getZ());
    }

    /** {@inheritDoc} */
    public void addAcceleration(final Vector3D gamma, final Frame frame)
        throws OrekitException {
        final Transform t = frame.getTransformTo(storedParameters.getFrame(),
                                                 storedParameters.getDate());
        final Vector3D gammInRefFrame = t.transformVector(gamma);
        addXYZAcceleration(gammInRefFrame.getX(), gammInRefFrame.getY(), gammInRefFrame.getZ());
    }


}
