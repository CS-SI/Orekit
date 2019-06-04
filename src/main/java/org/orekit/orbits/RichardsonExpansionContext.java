/* Copyright 2002-2019 CS Systèmes d'Information
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

package org.orekit.orbits;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.utils.LagrangianPoints;
import org.orekit.utils.PVCoordinates;

/** Class/Container implementing the Third-Order Richardson Expansion.
 * @author Vincent Mouraux
 */
public class RichardsonExpansionContext {

    /** Distance between a Lagrangian Point and its closest primary body, meters. */
    private final double gamma;

    /** Mass ratio of the CR3BP System. */
    private final double mu;

    /** Distance between the two primary bodies, meters. */
    private final double lDim;

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

    /** Simple Constructor.
     * @param cr3bpSystem CR3BP System considered
     * @param point Lagrangian Point considered
    */
    public RichardsonExpansionContext(final CR3BPSystem cr3bpSystem,
                                      final LagrangianPoints point) {

        this.mu = cr3bpSystem.getMu();

        this.lDim = cr3bpSystem.getLdim();

        this.gamma = getLPosition(cr3bpSystem, point);

        final double c2 = getCn(2, point);

        final double c3 = getCn(3, point);

        final double c4 = getCn(4, point);

        this.wp = FastMath.sqrt(0.5 * (2 - c2 + FastMath.sqrt(9 * c2 * c2 - 8 * c2)));

        final double ld = FastMath.sqrt(0.5 * (2 - c2 + FastMath.sqrt(9 * c2 * c2 - 8 * c2)));

        //  final double wv = FastMath.sqrt(c2);

        k = (wp * wp + 1 + 2 * c2) / (2 * wp);

        final double d1 = 3 * ld * ld * (k * (6 * ld * ld - 1) - 2 * ld) / k;

        final double d2 = 8 * ld * ld * (k * (11 * ld * ld - 1) - 2 * ld) / k;

        a21 = 3 * c3 * (k * k - 2) / (4 * (1 + 2 * c2));

        a22 = 3 * c3 / (4 * (1 + 2 * c2));

        a23 = -3 * c3 * ld * (3 * k * k * k * ld - 6 * k * (k - ld) + 4) / (4 * k * d1);

        a24 = -3 * c3 * ld * (2 + 3 * k * ld) / (4 * k * d1);

        b21 = -3 * c3 * ld * (3 * k * ld - 4) / (2 * d1);

        b22 = 3 * c3 * ld / d1;

        d21 = -c3 / (2 * ld * ld);

        a31 =
            -9 *
              ld * (4 * c3 * (k * a23 - b21) + k * c4 * (4 + k * k)) /
              (4 * d2) +
              (9 * ld * ld + 1 - c2) /
                         (2 * d2) *
                         (2 * c3 * (2 * a23 - k * b21) + c4 * (2 + 3 * k * k));

        a32 =
            -9 *
              ld * (4 * c3 * (k * a24 - b22) + k * c4) / (4 * d2) -
              3 *
                                                                    (9 *
                                                                     ld * ld +
                                                                     1 - c2) *
                                                                    (c3 *
                                                                     (k * b22 +
                                                                      d21 -
                                                                      2 * a24) -
                                                                     c4) /
                                                                    (2 * d2);

        b31 =
            3 *
              8 * ld * (3 * c3 * (k * b21 - 2 * a23) - c4 * (2 + 3 * k * k)) /
              (8 * d2) +
              3 *
                         ((9 * ld * ld + 1 + 2 * c2) *
                          (4 * c3 * (k * a23 - b21) + k * c4 * (4 + k * k))) /
                         (8 * d2);

        b32 =
            9 *
              ld * (c3 * (k * b22 + d21 - 2 * a24) - c4) / d2 +
              3 *
                                                                ((9 * ld * ld +
                                                                  1 + 2 * c2) *
                                                                 (4 *
                                                                  c3 *
                                                                  (k * a24 -
                                                                   b22) +
                                                                  k * c4)) /
                                                                (8 * d2);

        d31 = 3 / (64 * ld * ld) * (4 * c3 * a24 + c4);

        d32 = 3 / (64 * ld * ld) * (4 * c3 * (a23 - d21) + c4 * (4 + k * k));

        s1 =
            (3 *
             c3 * (2 * a21 * (k * k - 2) - a23 * (k * k + 2) - 2 * k * b21) /
             2 -
             3 * c4 * (3 * k * k * k * k - 8 * k * k + 8) / 8) /
             (2 * ld * (ld * (1 + k * k) - 2 * k));

        s2 =
            (3 *
             c3 *
             (2 * a22 * (k * k - 2) +
              a24 * (k * k + 2) + 2 * k * b22 + 5 * d21) /
             2 +
             3 * c4 * (12 - k * k) / 8) / (2 * ld * (ld * (1 + k * k) - 2 * k));

        l1 = -3 * c3 * (2 * a21 + a23 + 5 * d21) / 2 - 3 * c4 * (12 - k * k) / 8 + 2 * ld * ld * s1;

        l2 = 3 * c3 * (a24 - 2 * a22) / 2 + (9 * c4 / 8) + (2 * ld * ld * s2);

        delta = wp * wp - c2;
    }

    /** Calculate gamma.
     * @param syst CR3BP System considered
     * @param point Lagrangian Point considered
     * @return gamma Distance between the Lagrangian point and its closest primary body, meters
    */
    private double getLPosition(final CR3BPSystem syst, final LagrangianPoints point) {

        final double x;

        final double DCP;

        final double xe;

        final double pos;

        switch (point) {
            case L1:
                x = syst.getLPosition(LagrangianPoints.L1).getX();
                DCP = 1 - mu;
                xe = x / lDim;
                pos = DCP - xe;
                break;
            case L2:
                x = syst.getLPosition(LagrangianPoints.L2).getX();
                DCP = 1 - mu;
                xe = x / lDim;
                pos = xe - DCP;
                break;
            case L3:
                x = syst.getLPosition(LagrangianPoints.L3).getX();
                DCP = -mu;
                xe = x / lDim;
                pos = DCP - xe;
                break;
            default:
                pos = 0;
                break;
        }
        return pos;
    }

    /** Calculate Cn Richardson Coefficient.
     * @param order Order 'n' of the coefficient needed.
     * @param point Lagrangian Point considered
     * @return cn Cn Richardson Coefficient
    */
    private double getCn(final double order,
                         final LagrangianPoints point) {
        final double cn;
        switch (point) {
            case L1:
                cn = (1 / FastMath.pow(gamma, 3)) * (mu + FastMath.pow(-1, order) * (1 - mu) * FastMath.pow(gamma, order + 1) / FastMath.pow(1 - gamma, order + 1));
                break;
            case L2:
                cn = (1 / FastMath.pow(gamma, 3)) * (FastMath.pow(-1, order) * mu + FastMath.pow(-1, order) * (1 - mu) * FastMath.pow(gamma, order + 1) / FastMath.pow(1 + gamma, order + 1));
                break;
            case L3:
                cn = (1 / FastMath.pow(gamma, 3)) * (1 - mu + mu * FastMath.pow(gamma, order + 1) / FastMath.pow(1 + gamma, order + 1));
                break;
            default:
                cn = 0;
                break;
        }
        return cn;
    }

    /** Calculate first Guess with t=0 , phi=0.
     * @param azr z-axis Amplitude of the required Halo Orbit, meters
     * @param type type of the Halo Orbit ("Northern" or "Southern")
     * @return firstGuess PVCoordinates of the first guess
    */
    public PVCoordinates computeFirstGuess(final double azr,
                                           final String type) {
        return computeFirstGuess(azr, type, 0.0, 0.0);
    }

    /** Calculate first Guess.
     * @param azr z-axis Amplitude of the required Halo Orbit, meters
     * @param type type of the Halo Orbit ("Northern" or "Southern")
     * @param t Orbit time, seconds (>0)
     * @param phi Orbit phase, rad
     * @return firstGuess PVCoordinates of the first guess
    */
    public PVCoordinates computeFirstGuess(final double azr, final String type,
                                           final double t, final double phi) {

        final double az = azr / (gamma * lDim);

        final double ax = FastMath.sqrt((delta + l2 * az * az) / -l1);

        final double nu = 1 + s1 * ax * ax + s2 * az * az;

        final double tau = nu * t;

        final double tau1 = wp * tau + phi;

        final double m;

        if (type.equals("Northern")) {
            m = 1;
        } else {
            m = 3;
        }

        final double dm = 2 - m;

        final double firstx =
                        a21 * ax * ax +
                        a22 *
                        az * az - ax * FastMath.cos(tau1) +
                        (a23 * ax * ax -
                                        a24 * az * az) * FastMath.cos(2 * tau1) +
                        (a31 * ax * ax * ax -
                                        a32 * ax * az * az) * FastMath.cos(3 * tau1);

        final double firsty =
                        k * ax * FastMath.sin(tau1) +
                        (b21 * ax * ax - b22 * az * az) *
                        FastMath.sin(2 * tau1) +
                        (b31 * ax * ax * ax -
                                        b32 * ax * az * az) * FastMath.sin(3 * tau1);

        final double firstz =
                        dm * az * FastMath.cos(tau1) +
                        dm *
                        d21 * ax * az * (FastMath
                                        .cos(2 * tau1) - 3) +
                        dm *
                        (d32 *
                                        az * ax *
                                        ax -
                                        d31 *
                                        az *
                                        az *
                                        az) *
                        FastMath
                        .cos(3 *
                             tau1);

        final double vx =
                        ax * wp * nu * FastMath.sin(tau1) -
                        2 * (a23 * ax * ax - a24 * az * az) * wp * nu * FastMath.sin(2 * tau1) -
                        3 * (a31 * ax * ax * ax - a32 * ax * az * az) * wp * nu * FastMath.sin(3 * tau1);

        final double vy =
                        k * ax * wp * nu * FastMath.cos(tau1) +
                        2 * wp * nu * (b21 * ax * ax - b22 * az * az) * FastMath.cos(2 * tau1) +
                        3 * wp * nu * (b31 * ax * ax * ax - b32 * ax * az * az) * FastMath.cos(3 * tau1);

        final double vz =
                        -dm * az * wp * nu * FastMath.sin(tau1) -
                        2 * dm * d21 * ax * az * wp * nu * FastMath.sin(2 * tau1) -
                        3 * dm * (d32 * az * ax * ax - d31 * az * az * az) * wp * nu * FastMath.sin(3 * tau1);

        return new PVCoordinates(new Vector3D(firstx  * gamma + 1 - mu - gamma, firsty * gamma, firstz * gamma), new Vector3D(vx * gamma, vy * gamma, vz * gamma));
    }

    /**
     * @return the wp
     */
    public double getWp() {
        return wp;
    }

    /**
     * @return the k
     */
    public double getK() {
        return k;
    }

    /**
     * @return the a21
     */
    public double getA21() {
        return a21;
    }

    /**
     * @return the a22
     */
    public double getA22() {
        return a22;
    }

    /**
     * @return the a23
     */
    public double getA23() {
        return a23;
    }

    /**
     * @return the a24
     */
    public double getA24() {
        return a24;
    }

    /**
     * @return the b21
     */
    public double getB21() {
        return b21;
    }

    /**
     * @return the b22
     */
    public double getB22() {
        return b22;
    }

    /**
     * @return the d21
     */
    public double getD21() {
        return d21;
    }

    /**
     * @return the a31
     */
    public double getA31() {
        return a31;
    }

    /**
     * @return the a32
     */
    public double getA32() {
        return a32;
    }

    /**
     * @return the b31
     */
    public double getB31() {
        return b31;
    }

    /**
     * @return the b32
     */
    public double getB32() {
        return b32;
    }

    /**
     * @return the d31
     */
    public double getD31() {
        return d31;
    }

    /**
     * @return the d32
     */
    public double getD32() {
        return d32;
    }

    /**
     * @return the s1
     */
    public double getS1() {
        return s1;
    }

    /**
     * @return the s2
     */
    public double getS2() {
        return s2;
    }

    /**
     * @return the l1
     */
    public double getL1() {
        return l1;
    }

    /**
     * @return the l2
     */
    public double getL2() {
        return l2;
    }

    /**
     * @return the delta
     */
    public double getDelta() {
        return delta;
    }

    /**
     * @return the gamma
     */
    public double getGamma() {
        return gamma;
    }

    /** Return the orbital period of the Halo Orbit.
     * @param azr z-axis Amplitude of the required Halo Orbit, meters
     * @param type type of the Halo Orbit ("Northern" or "Southern")
     * @return the orbitalPeriod
     */
    public double getOrbitalPeriod(final double azr, final String type) {

        final double az = azr / (gamma * lDim);

        final double ax = FastMath.sqrt((delta + l2 * az * az) / -l1);

        final double nu = 1 + s1 * ax * ax + s2 * az * az;

        orbitalPeriod = FastMath.abs(2 * FastMath.PI / (wp * nu));

        return orbitalPeriod;
    }
}
