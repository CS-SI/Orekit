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
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.cr3bp.STMEquations;
import org.orekit.utils.LagrangianPoints;
import org.orekit.utils.PVCoordinates;

/** Class calculating different parameters of a Halo Orbit.
 * @author Vincent Mouraux
 */
public class HaloOrbit {

    /** Orbital Period of the Halo Orbit. */
    private double orbitalPeriod;

    /** CR3BP System of the Halo Orbit. */
    private final CR3BPSystem cr3bpSystem;

    /** Position-Velocity first guess for a point on a Halo Orbit. */
    private final PVCoordinates firstGuess;

    /** Simple Constructor.
     * @param syst CR3BP System considered
     * @param point Lagrangian Point considered
     * @param firstGuess PVCoordinates of the first guess
    */
    public HaloOrbit(final CR3BPSystem syst, final LagrangianPoints point,
                     final PVCoordinates firstGuess) {
        this.cr3bpSystem = syst;
        this.firstGuess = firstGuess;
        orbitalPeriod = 2;
    }

    /** Simple Constructor.
     * @param syst CR3BP System considered
     * @param point Lagrangian Point considered
     * @param az z-axis Amplitude of the required Halo Orbit, meters
     * @param type type of the Halo Orbit ("Northern" or "Southern")
    */
    public HaloOrbit(final CR3BPSystem syst, final LagrangianPoints point, final double az, final String type) {
        this(syst, point, az, type, 0.0, 0.0);
    }

    /** Simple Constructor.
     * @param syst CR3BP System considered
     * @param point Lagrangian Point considered
     * @param az z-axis Amplitude of the required Halo Orbit, meters
     * @param type type of the Halo Orbit ("Northern" or "Southern")
     * @param t Orbit time, seconds (>0)
     * @param phi Orbit phase, rad
    */
    public HaloOrbit(final CR3BPSystem syst, final LagrangianPoints point, final double az,
                     final String type, final double t, final double phi) {
        this.cr3bpSystem = syst;

        firstGuess =
            new RichardsonExpansionContext(cr3bpSystem, point)
                .computeFirstGuess(az, type, t, phi);
        orbitalPeriod =
            new RichardsonExpansionContext(cr3bpSystem, point)
                .getOrbitalPeriod(az, type);
    }

    /** Return the orbital period of the Halo Orbit.
     * @return orbitalPeriod  orbital period of the Halo Orbit
     */
    public double getOrbitalPeriod() {
        return orbitalPeriod;
    }

    /** Return the first guess Position-Velocity of a point on the Halo Orbit.
     * @return firstGuess first guess Position-Velocity of a point on the Halo Orbit
     */
    public PVCoordinates getFirstGuess() {
        return firstGuess;
    }

    /** Return the manifold vector.
     * @param s SpacecraftState with additionnal equations
     * @param type Stability of the manifold required
     * @return manifold first guess Position-Velocity of a point on the Halo Orbit
     */
    public PVCoordinates getManifolds(final SpacecraftState s,
                                      final boolean type) {

        final RealVector eigenVector;

        final RealMatrix phi = new STMEquations(cr3bpSystem).getStateTransitionMatrix(s);
        if (type) {
            eigenVector = new EigenDecomposition(phi).getEigenvector(1);
        } else {
            eigenVector = new EigenDecomposition(phi).getEigenvector(0);
        }

        final double epsilon = cr3bpSystem.getVdim() * 1E2 / cr3bpSystem.getLdim();

        final PVCoordinates pv =
            new PVCoordinates(s.getPVCoordinates().getPosition()
                .add(new Vector3D(eigenVector.getEntry(0), eigenVector
                    .getEntry(1), eigenVector.getEntry(2)).normalize()
                        .scalarMultiply(epsilon)), s.getPVCoordinates()
                            .getVelocity()
                            .add(new Vector3D(eigenVector.getEntry(3),
                                              eigenVector.getEntry(4),
                                              eigenVector.getEntry(5))
                                                  .normalize()
                                                  .scalarMultiply(epsilon)));
        return pv;

    }

}

