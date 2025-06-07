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
package org.orekit.estimation.measurements.gnss;

import java.util.Collections;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.QuadraticClockModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Base class for one-way GNSS measurement.
 * <p>
 * This class can be used in precise orbit determination applications
 * for modeling a range measurement between a GNSS satellite (emitter)
 * and a LEO satellite (receiver).
 * </p>
 * <p>
 * The one-way GNSS range measurement assumes knowledge of the orbit and
 * the clock offset of the emitting GNSS satellite. For instance, it is
 * possible to use a SP3 file or a GNSS navigation message to recover
 * the satellite's orbit and clock.
 * </p>
 * <p>
 * This class is very similar to {@link AbstractInterSatellitesMeasurement} measurement
 * class. However, using the one-way GNSS range measurement, the orbit and clock
 * of the emitting GNSS satellite are <b>NOT</b> estimated simultaneously with
 * LEO satellite coordinates.
 * </p>
 *
 * @param <T> type of the measurement
 * @author Luc Maisonobe
 * @since 12.1
 */
public abstract class AbstractOneWayGNSSMeasurement<T extends ObservedMeasurement<T>>
    extends AbstractOnBoardMeasurement<T> {

    /** Emitting satellite. */
    private final PVCoordinatesProvider remotePV;

    /** Clock offset of the emitting satellite. */
    private final QuadraticClockModel remoteClock;

    /** Simple constructor.
     * @param remotePV provider for GNSS satellite which simply emits the signal
     * @param remoteClock clock offset of the GNSS satellite
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param local satellite which receives the signal and perform the measurement
     */
    public AbstractOneWayGNSSMeasurement(final PVCoordinatesProvider remotePV,
                                         final QuadraticClockModel remoteClock,
                                         final AbsoluteDate date,
                                         final double range, final double sigma,
                                         final double baseWeight, final ObservableSatellite local) {
        // Call super constructor
        super(date, range, sigma, baseWeight, Collections.singletonList(local));

        // Initialise fields
        this.remotePV    = remotePV;
        this.remoteClock = remoteClock;
    }

    /** {@inheritDoc} */
    @Override
    protected PVCoordinatesProvider getRemotePV(final SpacecraftState[] states) {
        return remotePV;
    }

    /** {@inheritDoc} */
    @Override
    protected QuadraticClockModel getRemoteClock() {
        return remoteClock;
    }

    /** {@inheritDoc} */
    @Override
    protected FieldPVCoordinatesProvider<Gradient> getRemotePV(final SpacecraftState[] states,
                                                               final int freeParameters) {
        // convert the PVCoordinatesProvider to a FieldPVCoordinatesProvider<Gradient>
        return (date, frame) -> {

            // apply the raw (no derivatives) remote provider
            final AbsoluteDate             dateBase = date.toAbsoluteDate();
            final TimeStampedPVCoordinates pvBase   = remotePV.getPVCoordinates(dateBase, frame);
            final TimeStampedFieldPVCoordinates<Gradient> pvWithoutDerivatives =
                new TimeStampedFieldPVCoordinates<>(date.getField(), pvBase);

            // add derivatives, using a trick: we shift the date by 0, with derivatives
            final Gradient zeroWithDerivatives = date.durationFrom(dateBase);
            return pvWithoutDerivatives.shiftedBy(zeroWithDerivatives);

        };
    }

}
