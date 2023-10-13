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

import org.hipparchus.complex.Complex;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.EigenDecompositionNonSymmetric;
import org.hipparchus.linear.FieldVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.cr3bp.STMEquations;
import org.orekit.utils.PVCoordinates;

/**
 * Base class for libration orbits.
 * @see HaloOrbit
 * @see LyapunovOrbit
 * @author Vincent Mouraux
 * @author Bryan Cazabonne
 * @since 10.2
 */
public abstract class LibrationOrbit {

    /** CR3BP System of the libration Orbit. */
    private final CR3BPSystem syst;

    /** Position-Velocity initial position on a libration Orbit. */
    private PVCoordinates initialPV;

    /** Orbital Period of the libration Orbit. */
    private double orbitalPeriod;

    /**
     * Constructor.
     * @param system CR3BP System considered
     * @param initialPV initial position on a libration Orbit
     * @param orbitalPeriod initial orbital period of the libration Orbit
     */
    protected LibrationOrbit(final CR3BPSystem system,
                             final PVCoordinates initialPV,
                             final double orbitalPeriod) {
        this.syst = system;
        this.initialPV = initialPV;
        this.orbitalPeriod = orbitalPeriod;
    }

    /** Return the orbital period of the libration orbit.
     * @return orbitalPeriod  orbital period of the libration orbit
     */
    public double getOrbitalPeriod() {
        return orbitalPeriod;
    }

    /** Return the initialPV on the libration orbit.
     * <p>
     * This will return the exact initialPV only if you applied a prior
     * differential correction. If you did not, you can use the method
     * {@link #applyCorrectionOnPV(CR3BPDifferentialCorrection)}
     * </p>
     * @return initialPV initialPV on the libration orbit
     */
    public PVCoordinates getInitialPV() {
        return initialPV;
    }

    /** Apply differential correction.
     * <p>
     * This will update {@link #initialPV} and
     * {@link #orbitalPeriod} parameters.
     * </p>
     */
    public void applyDifferentialCorrection() {
        final CR3BPDifferentialCorrection diff = new CR3BPDifferentialCorrection(initialPV, syst, orbitalPeriod);
        initialPV = applyCorrectionOnPV(diff);
        orbitalPeriod = diff.getOrbitalPeriod();
    }

    /** Return a manifold direction from one position on a libration Orbit.
     * @param s SpacecraftState with additional equations
     * @param isStable true if the manifold is stable
     * @return manifold first guess Position-Velocity of a point on the libration Orbit
     */
    public PVCoordinates getManifolds(final SpacecraftState s, final boolean isStable) {

        // Get the index of the eigen vector of the state transition matrix,
        // depending on the stability or unstability of the manifold
        final int eigenVectorIndex = isStable ? 1 : 0;

        // Small delta, linked to the characteristic velocity of the CR3BP system
        final double epsilon = syst.getVdim() * 1E2 / syst.getDdim();

        // Get monodromy (i.e. state transition) matrix and its eigen decomposition
        final RealMatrix phi = new STMEquations(syst).getStateTransitionMatrix(s);
        final EigenDecompositionNonSymmetric eigen = new EigenDecompositionNonSymmetric(phi);

        // Get normalized eigen vector linked to the stability of the manifold
        final FieldVector<Complex> cv = eigen.getEigenvector(eigenVectorIndex);

        // Get real vector value and normalize
        final RealVector           rv = MatrixUtils.createRealVector(cv.getDimension());
        for (int i = 0; i < cv.getDimension(); ++i) {
            rv.setEntry(i, cv.getEntry(i).getRealPart());
        }
        final RealVector eigenVector = rv.unitVector();

        // New PVCoordinates following the manifold
        return new PVCoordinates(s.getPosition().add(new Vector3D(eigenVector.getEntry(0),
                                                                  eigenVector.getEntry(1),
                                                                  eigenVector.getEntry(2)).scalarMultiply(epsilon)),
                                 s.getPVCoordinates().getVelocity().add(new Vector3D(eigenVector.getEntry(3),
                                                                                     eigenVector.getEntry(4),
                                                                                     eigenVector.getEntry(5)).scalarMultiply(epsilon)));
    }

    /**
     * Apply the differential correction to compute more accurate initial PV.
     * @param diff cr3bp differential correction
     * @return corrected PV coordinates
     */
    protected abstract PVCoordinates applyCorrectionOnPV(CR3BPDifferentialCorrection diff);

}
