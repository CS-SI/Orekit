/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.propagation.FieldAdditionalDataProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.gnss.data.FieldGNSSClockElements;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;

/** Provider for clock corrections as additional states.
 * <p>
 * The value of this additional state is a three elements array containing
 * </p>
 * <ul>
 *   <li>at index 0, the polynomial satellite clock model
 *       Δtₛₐₜ = {@link FieldGNSSClockElements#getAf0() a₀} +
 *               {@link FieldGNSSClockElements#getAf1() a₁} (t - {@link FieldGNSSClockElements#getToc() toc}) +
 *               {@link FieldGNSSClockElements#getAf1() a₂} (t - {@link FieldGNSSClockElements#getToc() toc})²
 *   </li>
 *   <li>at index 1 the relativistic clock correction due to eccentricity</li>
 *   <li>at index 2 the estimated group delay differential {@link FieldGNSSClockElements#getTGD() TGD} for L1-L2 correction</li>
 * </ul>
 *
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
public class FieldClockCorrectionsProvider<T extends CalculusFieldElement<T>>
    implements FieldAdditionalDataProvider<T[], T> {

    /** The GPS clock elements. */
    private final FieldGNSSClockElements<T> gnssClk;

    /** Clock reference epoch. */
    private final FieldAbsoluteDate<T> clockRef;

    /** Duration of the GNSS cycle in seconds. */
    private final double cycleDuration;

    /** Simple constructor.
     * @param gnssClk GNSS clock elements
     * @param cycleDuration duration of the GNSS cycle in seconds
     */
    public FieldClockCorrectionsProvider(final FieldGNSSClockElements<T> gnssClk,
                                         final double cycleDuration) {
        this.gnssClk       = gnssClk;
        this.clockRef      = gnssClk.getDate();
        this.cycleDuration = cycleDuration;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return ClockCorrectionsProvider.CLOCK_CORRECTIONS;
    }

    /**
     * Get the duration from clock Reference epoch.
     * <p>This takes the GNSS week roll-over into account.</p>
     *
     * @param date the considered date
     * @return the duration from clock Reference epoch (s)
     */
    private T getDT(final FieldAbsoluteDate<T> date) {
        // Time from ephemeris reference epoch
        T dt = date.durationFrom(clockRef);
        // Adjusts the time to take roll over week into account
        while (dt.getReal() > 0.5 * cycleDuration) {
            dt = dt.subtract(cycleDuration);
        }
        while (dt.getReal() < -0.5 * cycleDuration) {
            dt = dt.add(cycleDuration);
        }
        // Returns the time from ephemeris reference epoch
        return dt;
    }

    /** {@inheritDoc} */
    @Override
    public T[] getAdditionalData(final FieldSpacecraftState<T> state) {

        // polynomial clock model
        final T  dt    = getDT(state.getDate());
        final T  dtSat = gnssClk.getAf0().add(dt.multiply(gnssClk.getAf1().add(dt.multiply(gnssClk.getAf2()))));

        // relativistic effect due to eccentricity
        final FieldPVCoordinates<T> pv    = state.getPVCoordinates();
        final T                     dtRel = FieldVector3D.dotProduct(pv.getPosition(), pv.getVelocity()).
                                            multiply(-2 / (Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT));

        // estimated group delay differential
        final T tg = gnssClk.getTGD();

        final T[] array = MathArrays.buildArray(dt.getField(), 3);
        array[0] = dtSat;
        array[1] = dtRel;
        array[2] = tg;
        return array;
    }

}
