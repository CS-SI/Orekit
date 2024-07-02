/* Copyright 2002-2024 CS GROUP
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

import org.orekit.bodies.CR3BPSystem;
import org.orekit.utils.PVCoordinates;

/** Class calculating different parameters of a Halo Orbit.
 * @author Vincent Mouraux
 * @since 10.2
 */
public class HaloOrbit extends LibrationOrbit {

    /**
     * Simple Constructor.
     * <p>
     * This constructor can be used if the user wants to use a first guess from
     * any other sources or if he has the initial conditions of a well defined Halo Orbit.
     * In that case, it is assumed that the user knows the
     * characteristics of the Halo Orbit leading to this first guess/point. Also, the
     * orbital period of this Halo Orbit has to be specified for further
     * computation.
     * </p>
     * @param syst CR3BP System considered
     * @param pv PVCoordinates of the initial point or of the first guess
     * @param orbitalPeriod Normalized orbital period linked to the given Halo Orbit first guess
     */
    public HaloOrbit(final CR3BPSystem syst,
                     final PVCoordinates pv, final double orbitalPeriod) {
        super(syst, pv, orbitalPeriod);
    }

    /**
     * Simple Constructor.
     * <p>
     * Standard constructor, the first guess will be computed with both start
     * time and phase equal to zero.
     * </p>
     * @param richardson third-Order Richardson Expansion
     * @param az z-axis Amplitude of the required Halo Orbit, meters
     * @param type type of the Halo Orbit (Northern or Southern)
     */
    public HaloOrbit(final RichardsonExpansion richardson,
                     final double az, final LibrationOrbitFamily type) {
        super(richardson.getCr3bpSystem(),
              richardson.computeHaloFirstGuess(az, type, 0.0, 0.0),
              richardson.getHaloOrbitalPeriod(az));
    }

    /** {@inheritDoc} */
    @Override
    protected PVCoordinates applyCorrectionOnPV(final CR3BPDifferentialCorrection diff) {
        return diff.compute(LibrationOrbitType.HALO);
    }

}

