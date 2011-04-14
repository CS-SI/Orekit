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
import org.orekit.utils.PVCoordinates;

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

    /** First vector of the (q, s, w) local orbital frame. */
    private Vector3D lofQ;

    /** Second vector of the (q, s, w) local orbital frame. */
    private Vector3D lofS;

    /** First vector of the (t, n, w) local orbital frame. */
    private Vector3D lofT;

    /** Second vector of the (t, n, w) local orbital frame. */
    private Vector3D lofN;

    /** Third vector of both the (q, s, w) and (t, n, w) local orbital frames. */
    private Vector3D lofW;

    // CHECKSTYLE: stop JavadocVariable check

    /** Multiplicative coefficients for the perturbing accelerations along lofT. */
    private double aT;
    private double eT;
    private double paT;
    private double vT;

    /** Multiplicative coefficients for the perturbing accelerations along lofN. */
    private double eN;
    private double paN;
    private double vN;

    /** Multiplicative coefficients for the perturbing accelerations along lofW. */
    private double iW;
    private double paW;
    private double raanW;

    // CHECKSTYLE: resume JavadocVariable check

    /** Kepler evolution on true latitude argument. */
    private double lvKepler;

    /** Orbital parameters. */
    private KeplerianOrbit storedParameters;

    /** Create a new instance.
     * @param orbit current orbit parameters
     */
    public TimeDerivativesEquationsKeplerian(final KeplerianOrbit orbit) {
        this.storedParameters = orbit;
        lofQ = Vector3D.ZERO;
        lofS = Vector3D.ZERO;
        lofT = Vector3D.ZERO;
        lofN = Vector3D.ZERO;
        lofW = Vector3D.ZERO;
        updateOrbitalFrames();
    }

    /** Update the orbital frames. */
    private void updateOrbitalFrames() {
        // get the position/velocity vectors
        final PVCoordinates pvCoordinates = storedParameters.getPVCoordinates();

        // compute orbital plane normal vector
        lofW = pvCoordinates.getMomentum().normalize();

        // compute (q, s, w) local orbital frame
        lofQ = pvCoordinates.getPosition().normalize();
        lofS = Vector3D.crossProduct(lofW, lofQ);

        // compute (t, n, w) local orbital frame
        lofT = pvCoordinates.getVelocity().normalize();
        lofN = Vector3D.crossProduct(lofW, lofT);
    }


    /** {@inheritDoc} */
    public void initDerivatives(final double[] yDot, final Orbit orbit)
        throws PropagationException {


        storedParameters = (KeplerianOrbit) orbit;
        updateOrbitalFrames();

        // store derivatives array reference
        this.storedYDot = yDot;

        // initialize derivatives to zero
        Arrays.fill(storedYDot, 0.0);

        // intermediate variables
        final double e           = storedParameters.getE();
        final double e2          = e * e;
        final double oMe2        = 1 - e2;
        final double epsilon     = FastMath.sqrt(oMe2);
        final double a           = storedParameters.getA();
        final double mu          = orbit.getMu();
        final double na          = FastMath.sqrt(mu / a);
        final double n           = na / a;
        final double trueAnomaly = storedParameters.getTrueAnomaly();
        final double cosV        = FastMath.cos(trueAnomaly);
        final double sinV        = FastMath.sin(trueAnomaly);
        final double ksi         = 1 + e * cosV;
        final double u           = storedParameters.getPerigeeArgument() + trueAnomaly;
        final double cosU        = FastMath.cos(u);
        final double sinU        = FastMath.sin(u);
        final double i           = storedParameters.getI();
        final double cosI        = FastMath.cos(i);
        final double sinI        = FastMath.sin(i);
        final double ePcosV      = e + cosV;
        final double vOnNA       = FastMath.sqrt((1 + e * (ePcosV + cosV)) / oMe2);
        final double r           = a * oMe2 / ksi;
        final double v           = vOnNA * na;
        final double dvde        = (1 + ksi) * sinV / oMe2;

        // coefficients along T
        aT  = 2 * vOnNA / n;
        eT  = 2 * ePcosV / v;
        paT = 2 * sinV / (v * e);
        vT  = -2 * sinV * ksi * ksi * (1 + e2 / ksi) / (e * oMe2 * v) + dvde * eT;

        // coefficients along N
        eN  = -r * sinV / (v * a);
        paN =  (2 * e + (1 + e2) * cosV) / (v * e * ksi);
        vN = -oMe2 * ksi * cosV / (e * oMe2 * v) + dvde * eN;

        // coefficients along W
        final double f = r / (na * a * epsilon);
        iW    =  f * cosU;
        raanW = f * sinU / sinI;
        paW   = -raanW * cosI;

        // Kepler natural evolution without the mu part
        lvKepler = n * ksi * ksi / (FastMath.sqrt(mu) * oMe2 * epsilon);

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
        storedYDot[0] += aT * t;
        storedYDot[1] += eT * t + eN * n;
        storedYDot[2] += iW * w;
        storedYDot[3] += paT * t + paN * n + paW * w;
        storedYDot[4] += raanW * w;
        storedYDot[5] += vT * t + vN * n;
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
        addTNWAcceleration(Vector3D.dotProduct(gammInRefFrame, lofT),
                           Vector3D.dotProduct(gammInRefFrame, lofN),
                           Vector3D.dotProduct(gammInRefFrame, lofW));
    }


    /** Get the first vector of the (q, s, w) local orbital frame.
     * @return first vector of the (q, s, w) local orbital frame */
    public Vector3D getQ() {
        return lofQ;
    }

    /** Get the second vector of the (q, s, w) local orbital frame.
     * @return second vector of the (q, s, w) local orbital frame */
    public Vector3D getS() {
        return lofS;
    }

    /** Get the first vector of the (t, n, w) local orbital frame.
     * @return first vector of the (t, n, w) local orbital frame */
    public Vector3D getT() {
        return lofT;
    }

    /** Get the second vector of the (t, n, w) local orbital frame.
     * @return second vector of the (t, n, w) local orbital frame */
    public Vector3D getN() {
        return lofN;
    }

    /** Get the third vector of both the (q, s, w) and (t, n, w) local orbital
     * frames.
     * @return third vector of both the (q, s, w) and (t, n, w) local orbital
     * frames
     */
    public Vector3D getW() {
        return lofW;
    }

}
