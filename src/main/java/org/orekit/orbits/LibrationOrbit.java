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
import org.orekit.utils.PVCoordinates;

/**
 * Base class for libration orbits.
 * @see HaloOrbit
 * @see LyapunovOrbit
 */
public abstract class LibrationOrbit {

    /** Name of the needed additional state. */
    private final String stm = "stmEquations";

    /** CR3BP System of the libration Orbit. */
    private final CR3BPSystem syst;

    /** Position-Velocity first guess for a point on a libration Orbit. */
    private final PVCoordinates firstGuess;

    /** Position-Velocity initial position on a libration Orbit. */
    private PVCoordinates initialPV;

    /** Orbital Period of the libration Orbit. */
    private double orbitalPeriod;

    /**
     * Constructor.
     * @param system CR3BP System considered
     * @param firstGuess first guess for a point on a libration Orbit
     * @param initialPV initial position on a libration Orbit
     * @param orbitalPeriod initial orbital period of the libration Orbit
     */
    protected LibrationOrbit(final CR3BPSystem system, final PVCoordinates firstGuess,
    						 final PVCoordinates initialPV, final double orbitalPeriod) {
    	this.syst          = system;
    	this.firstGuess    = firstGuess;
    	this.initialPV     = initialPV;
    	this.orbitalPeriod = orbitalPeriod;
    }

    /** Return a manifold direction from one position on a libration Orbit.
     * @param s SpacecraftState with additional equations
     * @param isStable true if the manifold is stable
     * @return manifold first guess Position-Velocity of a point on the libration Orbit
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

    /** Return the stable manifold direction for several positions on a libration Orbit.
     * @param s SpacecraftStates (with STM equations) to compute from
     * @return Stable manifold first direction from a point on the libration Orbit
     */
    public PVCoordinates[] getStableManifolds(final SpacecraftState[] s) {

        final PVCoordinates[] pv = new PVCoordinates[s.length];

        RealVector eigenVector;

        // Small delta, linked to the characteristic velocity of the CR3BP system
        final double epsilon =
            syst.getVdim() * 1E2 / syst.getDdim();


        int i = 0;
        while (i < s.length) {

            if (s[i].getAdditionalState(stm) == null) {
                throw new OrekitException(OrekitMessages.NO_STM_EQUATIONS, i);
            }

            // Get Normalize eigen vector linked to the stability of the manifold
            final RealMatrix phi =
                new STMEquations(syst).getStateTransitionMatrix(s[i]);

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

    /** Return the Unstable manifold direction for several positions on a libration Orbit.
     * @param s SpacecraftStates (with STM equations) to compute from
     * @return Unstable manifold first direction from a point on the libration Orbit
     */
    public PVCoordinates[] getUnstableManifolds(final SpacecraftState[] s) {

        final PVCoordinates[] pv = new PVCoordinates[s.length];

        RealVector eigenVector;

        final double epsilon =
            syst.getVdim() * 1E2 / syst.getDdim();

        int i = 0;
        while (i < s.length) {

            if (s[i].getAdditionalState(stm) == null ) {
                throw new OrekitException(OrekitMessages.NO_STM_EQUATIONS, i);
            }

            final RealMatrix phi =
                new STMEquations(syst).getStateTransitionMatrix(s[i]);

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

    /** Apply differential correction.
     * <p>
     * This will update initialPV and orbital Period
     * <p>
     */
    public void applyDifferentialCorrection() {
        final CR3BPDifferentialCorrection diff = new CR3BPDifferentialCorrection(firstGuess, syst,
                                          										 orbitalPeriod);
        initialPV = applyCorrectionOnPV(diff);
        orbitalPeriod = diff.getOrbitalPeriod();
    }

    /**
     * Apply the differential correction to compute more accurate initial PV
     * @param diff cr3bp differential correction
     * @return corrected PV coordinates
     */
    protected abstract PVCoordinates applyCorrectionOnPV(final CR3BPDifferentialCorrection diff);

    /** Return the orbital period of the libration orbit.
     * @return orbitalPeriod  orbital period of the libration orbit
     */
    public double getOrbitalPeriod() {
        return orbitalPeriod;
    }

    /** Return the first guess Position-Velocity of a point on the libration orbit.
     * @return firstGuess first guess Position-Velocity of a point on the libration obit
     */
    public PVCoordinates getFirstGuess() {
        return firstGuess;
    }

    /** Return the initialPV on the libration orbit.
     * <p>
     * This will return the exact initialPV only if you applied a prior
     * differential correction. If you did not, you can use the method
     * {@link #applyCorrectionOnPV(CR3BPDifferentialCorrection)}
     * </p>
     * @return initialPV initialPV on the libration orbit
     */
    public PVCoordinates getExactInitialPV() {
        return initialPV;
    }

}
