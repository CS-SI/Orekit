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
import org.hipparchus.linear.EigenDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.cr3bp.STMEquations;
import org.orekit.utils.LagrangianPoints;
import org.orekit.utils.PVCoordinates;

/** Class calculating different parameters of a Lyapunov Orbit.
 * @author Vincent Mouraux
 */
public class LyapunovOrbit {

    /** Orbital Period of the Lissajous Orbit. */
    private double orbitalPeriod;

    /** CR3BP System of the Lissajous Orbit. */
    private final CR3BPSystem cr3bpSystem;

    /** Position-Velocity first guess for a point on a Lyapunov Orbit. */
    private final PVCoordinates firstGuess;

    /** Position-Velocity initial position on a Lyapunov Orbit. */
    private PVCoordinates initialPV;

    /** Name of the needed additional state. */
    private final String stm = "stmEquations";

    /**
     * Simple Constructor.
     * <p>
     * This constructor can be used if the user wants to use a first guess from
     * any other sources or if he has the initial conditions of a well defined Lyapunov Orbit. In that case, it is assumed that the user knows the
     * characteristics of the Lyapunov Orbit leading to this first guess/point. Also, the
     * orbital period of this Lyapunov Orbit has to be specified for further
     * computation.
     * </p>
     * @param syst CR3BP System considered
     * @param pv PVCoordinates of the initial point or of the first guess
     * @param orbitalPeriod Normalized orbital period linked to the given Lyapunov
     *        Orbit first guess
     */
    public LyapunovOrbit(final CR3BPSystem syst,
                     final PVCoordinates pv, final double orbitalPeriod) {
        this.cr3bpSystem = syst;
        this.initialPV = pv;
        this.orbitalPeriod = orbitalPeriod;
        this.firstGuess = pv;
    }

    /**
     * Simple Constructor.
     * <p>
     * Standard constructor, the first guess will be computed with both start
     * time and phase equal to zero.
     * </p>
     * @param syst CR3BP System considered
     * @param point Lagrangian Point considered
     * @param ay y-axis amplitude of the required Lyapunov Orbit, meters
     */
    public LyapunovOrbit(final CR3BPSystem syst, final LagrangianPoints point, final double ay) {
        this(syst, point, ay, 0.0, 0.0);
    }

    /**
     * Simple Constructor.
     * <p>
     * This constructor has to be used in case the user wants a start time
     * and/or phase different from zero in Richardson first guess computation
     * </p>
     * @param syst CR3BP System considered
     * @param point Lagrangian Point considered
     * @param ay y-axis Amplitude of the required Lyapunov Orbit, meters
     * @param t time, seconds (!=0)
     * @param phi Orbit phase, rad
     */
    private LyapunovOrbit(final CR3BPSystem syst, final LagrangianPoints point, final double ay, final double t, final double phi) {
        this.cr3bpSystem = syst;

        firstGuess =
            new RichardsonExpansion(cr3bpSystem, point)
                .computeLyapunovFirstGuess(ay, t, phi);

        orbitalPeriod =
            new RichardsonExpansion(cr3bpSystem, point)
                .getLyapunovOrbitalPeriod(ay);
    }

    /** Apply differential correction.
     * <p>
     * this will update initialPV and orbital Period
     * <p>
     */
    public void ApplyDifferentialCorrection() {
        final CR3BPDifferentialCorrection diff = new CR3BPDifferentialCorrection(firstGuess, cr3bpSystem,
                                          orbitalPeriod);
        initialPV = diff.LyapunovCompute();
        orbitalPeriod = diff.getOrbitalPeriod();
    }

    /** Return the orbital period of the Lyapunov Orbit.
     * @return orbitalPeriod  orbital period of the Lyapunov Orbit
     */
    public double getOrbitalPeriod() {
        return orbitalPeriod;
    }

    /** Return the first guess Position-Velocity of a point on the Lyapunov Orbit.
     * @return firstGuess first guess Position-Velocity of a point on the Lyapunov Orbit
     */
    public PVCoordinates getFirstGuess() {
        return firstGuess;
    }

    /** Return the initialPV on the Lyapunov Orbit.
     * <p>
     * This will return the exact initialPV only if you applied a prior
     * differential correction. If you did not, you can use the method
     * ApplyDifferentialCorrection()
     * </p>
     * @return initialPV initialPV on the Lyapunov Orbit
     */
    public PVCoordinates getExactInitialPV() {
        return initialPV;
    }

    /** Return a manifold direction from one position on a Lyapunov Orbit.
     * @param s SpacecraftState with additionnal equations
     * @param isStable True if...
     * @return manifold first guess Position-Velocity of a point on the Lyapunov Orbit
     */
    public PVCoordinates getManifolds(final SpacecraftState s,
                                      final boolean isStable) {

        final PVCoordinates pv;
        final SpacecraftState[] state = new SpacecraftState[1];
        state[0] = s;

        if (isStable) {
            // Stable
            pv = getStableManifolds(state)[0];
        } else {
            // Unstable
            pv = getUnstableManifolds(state)[0];;
        }

        return pv;

    }

    /** Return the stable manifold direction for several positions on a Lyapunov Orbit.
     * @param s SpacecraftStates (with STM equations) to compute from
     * @return Stable manifold first direction from a point on the Lyapunov Orbit
     */
    public PVCoordinates[]
        getStableManifolds(final SpacecraftState[] s) {

        final PVCoordinates[] pv = new PVCoordinates[s.length];

        RealVector eigenVector;

        // Small delta, linked to the characteristic velocity of the CR3BP system
        final double epsilon =
            cr3bpSystem.getVdim() * 1E2 / cr3bpSystem.getDdim();


        int i = 0;
        while (i < s.length) {

            if (s[i].getAdditionalState(stm) == null) {
                throw new OrekitException(OrekitMessages.NO_STM_EQUATIONS, i);
            }

            // Get Normalize eigen vector linked to the stability of the manifold
            final RealMatrix phi =
                new STMEquations(cr3bpSystem).getStateTransitionMatrix(s[i]);

            eigenVector = new EigenDecomposition(phi).getEigenvector(1).unitVector();

            // New PVCoordinates following the manifold
            pv[i] =
                new PVCoordinates(s[i].getPVCoordinates().getPosition()
                    .add(new Vector3D(eigenVector.getEntry(0), eigenVector
                        .getEntry(1), eigenVector.getEntry(2))
                            .scalarMultiply(epsilon)), s[i].getPVCoordinates()
                                .getVelocity()
                                .add(new Vector3D(eigenVector.getEntry(3),
                                                  eigenVector.getEntry(4),
                                                  eigenVector.getEntry(5))
                                                      .scalarMultiply(epsilon)));
            i++;
        }
        return pv;
    }

    /** Return the Unstable manifold direction for several positions on a Lyapunov Orbit.
     * @param s SpacecraftStates (with STM equations) to compute from
     * @return Unstable manifold first direction from a point on the Lyapunov Orbit
     */
    public PVCoordinates[] getUnstableManifolds(final SpacecraftState[] s) {

        final PVCoordinates[] pv = new PVCoordinates[s.length];

        RealVector eigenVector;

        final double epsilon =
            cr3bpSystem.getVdim() * 1E2 / cr3bpSystem.getDdim();

        int i = 0;
        while (i < s.length) {

            if (s[i].getAdditionalState(stm) == null ) {
                throw new OrekitException(OrekitMessages.NO_STM_EQUATIONS, i);
            }

            final RealMatrix phi =
                new STMEquations(cr3bpSystem).getStateTransitionMatrix(s[i]);

            eigenVector =
                new EigenDecomposition(phi).getEigenvector(0).unitVector();

            pv[i] =
                new PVCoordinates(s[i].getPVCoordinates().getPosition()
                    .add(new Vector3D(eigenVector.getEntry(0), eigenVector
                        .getEntry(1), eigenVector.getEntry(2))
                            .scalarMultiply(epsilon)), s[i].getPVCoordinates()
                                .getVelocity()
                                .add(new Vector3D(eigenVector.getEntry(3),
                                                  eigenVector.getEntry(4),
                                                  eigenVector.getEntry(5))
                                                      .scalarMultiply(epsilon)));
            i++;
        }
        return pv;
    }
}

