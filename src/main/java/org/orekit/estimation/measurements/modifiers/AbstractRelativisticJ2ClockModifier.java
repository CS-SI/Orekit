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

package org.orekit.estimation.measurements.modifiers;

import org.hipparchus.util.FastMath;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Class modifying theoretical measurements with relativistic J2 clock correction.
 * <p>
 * Relativistic clock correction of the effects caused by the oblateness of Earth on
 * the gravity potential.
 * </p>
 * <p>
 * The time delay caused by this effect is computed based on the orbital parameters of the
 * emitter's orbit.
 * </p>
 *
 * @author Louis Aucouturier
 * @since 11.2
 *
 * @see "Teunissen, Peter, and Oliver Montenbruck, eds. Springer handbook of global navigation
 * satellite systems. Chapter 19.2. Equation 19.18 Springer, 2017."
 */
public class AbstractRelativisticJ2ClockModifier {

    /**
     * Relativistic J2 effect constant.
     */
    private final double cJ2;

    /** Central attraction coefficient. */
    private final double gm;

    /**
     * Constructor for the Relativistic J2 Clock modifier.
     * @param gm Earth gravitational constant (mu) in m³/s².
     * @param c20 Earth un-normalized second zonal coefficient (Signed J2 constant, is negative) (Typical value -1.0826e-3).
     * @param equatorialRadius Earth equatorial radius in m.
     */
    public AbstractRelativisticJ2ClockModifier(final double gm,
                                               final double c20,
                                               final double equatorialRadius) {
        this.cJ2 = 1.5 * c20 * equatorialRadius * equatorialRadius /
                (Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT);
        this.gm = gm;
    }

    /**
     * Computes the relativistic J2 clock time delay correction.
     *
     * @param estimated EstimatedMeasurements on which to calculate the correction
     * @return dt_relJ2clk Time delay due to the relativistic J2 clock effect in seconds
     */
    protected double relativisticJ2Correction(final EstimatedMeasurementBase<?> estimated) {

        // Extracting the state of the receiver to determine the frame and mu
        /**
         * The satellite states are stored at the creation of the estimated measurements
         * and can contain up to 2 elements. In most cases, only the receiver's state and
         * therefore frame is stored, with the emitter's frame corresponding to the receiver's.
         * Still, in the InterSatellites case, the states of the 2 spacecrafts are stored,
         * and can contain different frames. This case is treated by looking at the length
         * of SpacecraftState stored in the Estimated Measurements, with the only length 2
         * case is the InterSatellites case.
         */
        final SpacecraftState[] states = estimated.getStates();
        final SpacecraftState state =  (states.length < 2) ? states[0] : states[1];

        final Frame remoteFrame = state.getFrame();

        // Getting Participants to extract the remote PV
        final TimeStampedPVCoordinates[] pvs = estimated.getParticipants();

        // Checking if the correction is applied on a two-way GNSS problem
        // In that case the emitter is at index 1, else index 0
        final TimeStampedPVCoordinates pvRemote = (pvs.length < 3) ? pvs[0] : pvs[1];

        // Define a Keplerian orbit to extract the orbital parameters needed to compute the correction
        final KeplerianOrbit remoteOrbit = new KeplerianOrbit(pvRemote, remoteFrame, gm);
        final double orbitInclination = remoteOrbit.getI();

        // u = perigee argument + true anomaly
        final double orbitU = remoteOrbit.getTrueAnomaly() + remoteOrbit.getPerigeeArgument();
        final double n = remoteOrbit.getKeplerianMeanMotion();

        // Returning the value of the time delay
        return cJ2 * n * FastMath.sin(2 * orbitU) * FastMath.sin(orbitInclination) * FastMath.sin(orbitInclination);
    }

}
