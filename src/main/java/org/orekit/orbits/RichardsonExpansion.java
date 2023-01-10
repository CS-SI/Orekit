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
package org.orekit.orbits;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.LagrangianPoints;
import org.orekit.utils.PVCoordinates;

/** Class implementing the Third-Order Richardson Expansion.
 * @see "Dynamical systems, the three-body problem, and space mission design, Koon, Lo, Marsden, Ross"
 * @author Vincent Mouraux
 * @since 10.2
 */
public class RichardsonExpansion {

    /** Distance between a Lagrangian Point and its closest primary body, meters. */
    private final double gamma;

    /** Mass ratio of the CR3BP System. */
    private final double mu;

    /** Distance between the two primary bodies, meters. */
    private final double dDim;

    /** Halo Orbit frequency. */
    private final double wp;

    /** Simple Halo Orbit coefficient. */
    private final double k;

    /** Simple Richardson coefficient. */
    private final double a21;

    /** Simple Richardson coefficient. */
    private final double a22;

    /** Simple Richardson coefficient. */
    private final double a23;

    /** Simple Richardson coefficient. */
    private final double a24;

    /** Simple Richardson coefficient. */
    private final double b21;

    /** Simple Richardson coefficient. */
    private final double b22;

    /** Simple Richardson coefficient. */
    private final double d21;

    /** Simple Richardson coefficient. */
    private final double a31;

    /** Simple Richardson coefficient. */
    private final double a32;

    /** Simple Richardson coefficient. */
    private final double b31;

    /** Simple Richardson coefficient. */
    private final double b32;

    /** Simple Richardson coefficient. */
    private final double d31;

    /** Simple Richardson coefficient. */
    private final double d32;

    /** Simple Richardson coefficient. */
    private final double s1;

    /** Simple Richardson coefficient. */
    private final double s2;

    /** Simple Richardson coefficient. */
    private final double l1;

    /** Simple Richardson coefficient. */
    private final double l2;

    /** Simple Halo Orbit coefficient. */
    private final double delta;

    /** Orbital Period of the Halo Orbit, seconds. */
    private double orbitalPeriod;

    /** Lagrangian Point considered. */
    private LagrangianPoints point;

    /** CR3BP System considered. */
    private CR3BPSystem cr3bpSystem;

    /** Simple Constructor.
     * @param cr3bpSystem CR3BP System considered
     * @param point Lagrangian Point considered
    */
    public RichardsonExpansion(final CR3BPSystem cr3bpSystem,
                                      final LagrangianPoints point) {

        this.point       = point;
        this.cr3bpSystem = cr3bpSystem;
        this.mu          = cr3bpSystem.getMassRatio();
        this.dDim        = cr3bpSystem.getDdim();
        this.gamma       = cr3bpSystem.getGamma(point);

        final double c2 = getCn(2.0);
        final double c3 = getCn(3.0);
        final double c4 = getCn(4.0);

        wp = FastMath.sqrt(0.5 * (2.0 - c2 + FastMath.sqrt(9.0 * c2 * c2 - 8.0 * c2)));

        final double ld = FastMath.sqrt(0.5 * (2.0 - c2 + FastMath.sqrt(9.0 * c2 * c2 - 8.0 * c2)));

        k = (wp * wp + 1.0 + 2.0 * c2) / (2.0 * wp);

        final double d1 = 3.0 * ld * ld * (k * (6.0 * ld * ld - 1.0) - 2.0 * ld) / k;
        final double d2 = 8.0 * ld * ld * (k * (11.0 * ld * ld - 1.0) - 2.0 * ld) / k;

        a21 = 3.0 * c3 * (k * k - 2.0) / (4.0 * (1.0 + 2.0 * c2));

        a22 = 3.0 * c3 / (4.0 * (1.0 + 2.0 * c2));

        a23 = -3.0 * c3 * ld * (3.0 * k * k * k * ld - 6.0 * k * (k - ld) + 4.0) / (4.0 * k * d1);

        a24 = -3.0 * c3 * ld * (2.0 + 3.0 * k * ld) / (4.0 * k * d1);

        b21 = -3.0 * c3 * ld * (3.0 * k * ld - 4.0) / (2.0 * d1);

        b22 = 3.0 * c3 * ld / d1;

        d21 = -c3 / (2.0 * ld * ld);

        a31 =
            -9.0 * ld * (4.0 * c3 * (k * a23 - b21) + k * c4 * (4.0 + k * k)) / (4.0 * d2) +
            (9.0 * ld * ld + 1.0 - c2) / (2.0 * d2) * (2.0 * c3 * (2.0 * a23 - k * b21) + c4 * (2.0 + 3.0 * k * k));

        a32 =
            -9.0 * ld * (4.0 * c3 * (k * a24 - b22) + k * c4) / (4.0 * d2) -
            3 * (9.0 * ld * ld + 1.0 - c2) * (c3 * (k * b22 + d21 - 2.0 * a24) - c4) / (2.0 * d2);

        b31 =
            3.0 * 8.0 * ld * (3.0 * c3 * (k * b21 - 2.0 * a23) - c4 * (2.0 + 3.0 * k * k)) / (8.0 * d2) +
            3.0 * ((9.0 * ld * ld + 1.0 + 2.0 * c2) * (4.0 * c3 * (k * a23 - b21) + k * c4 * (4.0 + k * k))) / (8.0 * d2);

        b32 =
            9.0 * ld * (c3 * (k * b22 + d21 - 2.0 * a24) - c4) / d2 +
            3.0 * ((9.0 * ld * ld + 1.0 + 2.0 * c2) * (4.0 * c3 * (k * a24 - b22) +  k * c4)) / (8.0 * d2);

        d31 = 3.0 / (64.0 * ld * ld) * (4.0 * c3 * a24 + c4);

        d32 = 3.0 / (64.0 * ld * ld) * (4.0 * c3 * (a23 - d21) + c4 * (4.0 + k * k));

        s1 =
            (3.0 * c3 * (2.0 * a21 * (k * k - 2.0) - a23 * (k * k + 2.0) - 2.0 * k * b21) / 2.0 -
            3.0 * c4 * (3.0 * k * k * k * k - 8.0 * k * k + 8.0) / 8.0) / (2.0 * ld * (ld * (1.0 + k * k) - 2.0 * k));

        s2 =
            (3.0 * c3 * (2.0 * a22 * (k * k - 2.0) + a24 * (k * k + 2.0) + 2.0 * k * b22 + 5.0 * d21) / 2.0 +
            3.0 * c4 * (12.0 - k * k) / 8.0) / (2.0 * ld * (ld * (1.0 + k * k) - 2.0 * k));

        l1 = -3.0 * c3 * (2.0 * a21 + a23 + 5.0 * d21) / 2.0 - 3.0 * c4 * (12.0 - k * k) / 8.0 + 2.0 * ld * ld * s1;

        l2 = 3.0 * c3 * (a24 - 2.0 * a22) / 2.0 + (9.0 * c4 / 8.0) + (2.0 * ld * ld * s2);

        delta = wp * wp - c2;
    }


    /** Calculate Cn Richardson Coefficient.
     * @param order Order 'n' of the coefficient needed.
     * @return cn Cn Richardson Coefficient
    */
    private double getCn(final double order) {
        final double gamma3 = gamma * gamma * gamma;
        final double cn;
        switch (point) {
            case L1:
                cn = (1.0 / gamma3) * (mu + FastMath.pow(-1, order) * (1 - mu) * FastMath.pow(gamma, order + 1) / FastMath.pow(1 - gamma, order + 1));
                break;
            case L2:
                cn = (1.0 / gamma3) * (FastMath.pow(-1, order) * mu + FastMath.pow(-1, order) * (1 - mu) * FastMath.pow(gamma, order + 1) / FastMath.pow(1 + gamma, order + 1));
                break;
            default:
                throw new OrekitException(OrekitMessages.CANNOT_COMPUTE_LAGRANGIAN, point);
        }
        return cn;
    }

    /** Calculate first Guess.
     * @param azr z-axis Amplitude of the required Halo Orbit, meters
     * @param type type of the Halo Orbit ("Northern" or "Southern")
     * @param t Orbit time, seconds (must be greater than 0)
     * @param phi Orbit phase, rad
     * @return firstGuess PVCoordinates of the first guess
    */
    public PVCoordinates computeHaloFirstGuess(final double azr, final LibrationOrbitFamily type,
                                           final double t, final double phi) {

        // Z-Axis Halo Orbit Amplitude
        final double az = azr / (gamma * dDim);

        // X-Axis Halo Orbit Amplitude
        final double ax      = FastMath.sqrt((delta + l2 * az * az) / -l1);
        final double nu      = 1.0 + s1 * ax * ax + s2 * az * az;
        final double tau     = nu * t;
        final double tau1    = wp * tau + phi;
        final double m       = (type == LibrationOrbitFamily.NORTHERN) ? 1 : 3;
        final double dm      = 2 - m;
        final SinCos scTau1  = FastMath.sinCos(tau1);
        final SinCos sc2Tau1 = SinCos.sum(scTau1, scTau1);
        final SinCos sc3Tau1 = SinCos.sum(scTau1, sc2Tau1);

        // First guess position relative to its Lagrangian point
        final double firstx =
                        a21 * ax * ax +
                        a22 * az * az - ax * scTau1.cos() +
                        (a23 * ax * ax - a24 * az * az) * sc2Tau1.cos() +
                        (a31 * ax * ax * ax - a32 * ax * az * az) * sc3Tau1.cos();

        final double firsty =
                        k * ax * scTau1.sin() +
                        (b21 * ax * ax - b22 * az * az) * sc2Tau1.sin() +
                        (b31 * ax * ax * ax - b32 * ax * az * az) * sc3Tau1.sin();

        final double firstz =
                        dm * az * scTau1.cos() +
                        dm * d21 * ax * az * (sc2Tau1.cos() - 3.0) +
                        dm * (d32 * az * ax * ax - d31 * az * az * az) * sc3Tau1.cos();

        // First guess Velocity relative to its Lagrangian point
        final double vx =
                        ax * wp * nu * scTau1.sin() -
                        2.0 * (a23 * ax * ax - a24 * az * az) * wp * nu * sc2Tau1.sin() -
                        3.0 * (a31 * ax * ax * ax - a32 * ax * az * az) * wp * nu * sc3Tau1.sin();

        final double vy =
                        k * ax * wp * nu * scTau1.cos() +
                        2.0 * wp * nu * (b21 * ax * ax - b22 * az * az) * sc2Tau1.cos() +
                        3.0 * wp * nu * (b31 * ax * ax * ax - b32 * ax * az * az) * sc3Tau1.cos();

        final double vz =
                        -dm * az * wp * nu * scTau1.sin() -
                        2.0 * dm * d21 * ax * az * wp * nu * sc2Tau1.sin() -
                        3.0 * dm * (d32 * az * ax * ax - d31 * az * az * az) * wp * nu * sc3Tau1.sin();

        // Return PV Coordinates
        return point == LagrangianPoints.L1 ?
                new PVCoordinates(new Vector3D(firstx * gamma + 1.0 - mu - gamma, firsty * gamma, firstz * gamma), new Vector3D(vx * gamma, vy * gamma, vz * gamma)) :
                new PVCoordinates(new Vector3D(firstx * gamma + 1.0 - mu + gamma, firsty * gamma, firstz * gamma), new Vector3D(vx * gamma, vy * gamma, vz * gamma));
    }

    /** Calculate first Guess.
     * @param ayr x-axis Amplitude of the required Lyapunov Orbit, meters
     * @param t time
     * @param phi Orbit phase, rad
     * @return firstGuess PVCoordinates of the first guess
    */
    public PVCoordinates computeLyapunovFirstGuess(final double ayr, final double t, final double phi) {

        // Z-Axis Lyapunov Orbit Amplitude
        final double az = 0;

        // y-Axis Lyapunov Orbit Amplitude
        final double ay   = ayr / (gamma * dDim);
        final double ax   = ay / k;
        final double nu   = 1.0 + s1 * ax * ax + s2 * az * az;
        final double tau  = nu * t;
        final double tau1 = wp * tau + phi;

        // First guess position relative to its Lagrangian point
        final double firstx = -ax * FastMath.cos(tau1);
        final double firsty = 0.0;
        final double firstz = 0.0;

        // First guess Velocity relative to its Lagrangian point
        final double vx = 0.0;
        final double vy = k * ax * wp * nu * FastMath.cos(tau1);
        final double vz = 0.0;

        // Return PV Coordinates
        return point == LagrangianPoints.L1 ?
                 new PVCoordinates(new Vector3D(firstx * gamma + 1.0 - mu - gamma, firsty * gamma, firstz * gamma),
                                   new Vector3D(vx * gamma, vy * gamma, vz * gamma)) :
                 new PVCoordinates(new Vector3D(firstx * gamma + 1.0 - mu + gamma, firsty * gamma, firstz * gamma),
                                   new Vector3D(vx * gamma, vy * gamma, vz * gamma));
    }

    /** Return the orbital period of the Halo Orbit.
     * @param azr z-axis Amplitude of the required Halo Orbit, meters
     * @return the orbitalPeriod
     */
    public double getHaloOrbitalPeriod(final double azr) {
        final double az = azr / (gamma * dDim);
        final double ax = FastMath.sqrt((delta + l2 * az * az) / -l1);
        final double nu = 1.0 + s1 * ax * ax + s2 * az * az;
        orbitalPeriod   = FastMath.abs(2.0 * FastMath.PI / (wp * nu));
        return orbitalPeriod;
    }

    /** Return the orbital period of the Halo Orbit.
     * @param axr x-axis Amplitude of the required Lyapunov Orbit, meters
     * @return the orbitalPeriod
     */
    public double getLyapunovOrbitalPeriod(final double axr) {
        final double az = 0;
        final double ax = axr / (gamma * dDim);
        final double nu = 1.0 + s1 * ax * ax + s2 * az * az;
        orbitalPeriod   = FastMath.abs(2.0 * FastMath.PI / (wp * nu));
        return orbitalPeriod;
    }

    /** Get the considered CR3BP system.
     * @return CRR3BP system
     */
    public CR3BPSystem getCr3bpSystem() {
        return cr3bpSystem;
    }

    /** Get the considered lagrangian point.
     * @return lagrangian point
     */
    public LagrangianPoints getLagrangianPoint() {
        return point;
    }

}
