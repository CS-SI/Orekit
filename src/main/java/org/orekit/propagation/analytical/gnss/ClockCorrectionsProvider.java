/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

/** Provider for clock corrections as additional states.
 * <p>
 * The value of this additional state is a three elements array containing
 * </p>
 * <ul>
 *   <li>at index 0, the polynomial satellite clock model
 *       Δtₛₐₜ = {@link GPSOrbitalElements#getAf0() a₀} +
 *               {@link GPSOrbitalElements#getAf1() a₁} (t - {@link GPSOrbitalElements#getToc() toc}) +
 *               {@link GPSOrbitalElements#getAf1() a₂} (t - {@link GPSOrbitalElements#getToc() toc})²
 *   </li>
 *   <li>at index 1 the relativistic clock correction due to eccentricity</li>
 *   <li>at index 2 the estimated group delay differential {@link GPSOrbitalElements#getTGD() TGD} for L1-L2 correction</li>
 * </ul>
 * @author Luc Maisonobe
 * @since 9.3
 */
public class ClockCorrectionsProvider implements AdditionalStateProvider {

    /** Name of the additional state for satellite clock corrections.
     * @since 9.3
     */
    public static final String CLOCK_CORRECTIONS = "";

    /** Duration of the GPS cycle in seconds. */
    private static final double GPS_CYCLE_DURATION = GPSOrbitalElements.GPS_WEEK_IN_SECONDS *
                                                     GPSOrbitalElements.GPS_WEEK_NB;
    /** The GPS orbital elements. */
    private final GPSOrbitalElements gpsOrbit;

    /** Clock reference epoch. */
    private final AbsoluteDate clockRef;

    /** Simple constructor.
     * @param gpsOrbit GPS orbital elements
     */
    public ClockCorrectionsProvider(final GPSOrbitalElements gpsOrbit) {
        this.gpsOrbit = gpsOrbit;
        this.clockRef = gpsOrbit.getDate();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return CLOCK_CORRECTIONS;
    }

    /**
     * Get the duration from clock Reference epoch.
     * <p>This takes the GPS week roll-over into account.</p>
     *
     * @param date the considered date
     * @return the duration from clock Reference epoch (s)
     */
    private double getDT(final AbsoluteDate date) {
        // Time from ephemeris reference epoch
        double dt = date.durationFrom(clockRef);
        // Adjusts the time to take roll over week into account
        while (dt > 0.5 * GPS_CYCLE_DURATION) {
            dt -= GPS_CYCLE_DURATION;
        }
        while (dt < -0.5 * GPS_CYCLE_DURATION) {
            dt += GPS_CYCLE_DURATION;
        }
        // Returns the time from ephemeris reference epoch
        return dt;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getAdditionalState(final SpacecraftState state) {

        // polynomial clock model
        final double  dt    = getDT(state.getDate());
        final double  dtSat = gpsOrbit.getAf0() + dt * (gpsOrbit.getAf1() + dt * gpsOrbit.getAf2());

        // relativistic effect due to eccentricity
        final PVCoordinates pv    = state.getPVCoordinates();
        final double        dtRel = -2 * Vector3D.dotProduct(pv.getPosition(), pv.getVelocity()) /
                        (Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT);

        // estimated group delay differential
        final double tg = gpsOrbit.getTGD();

        return new double[] {
            dtSat, dtRel, tg
        };
    }

}
