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
package org.orekit.estimation.measurements.gnss;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.InterSatellitesRange;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** One-way GNSS range measurement.
 * <p>
 * This class can be used in precise orbit determination applications
 * for modeling a range measurement between a GNSS satellite (emitter)
 * and a LEO satellite (receiver).
 * <p>
 * The one-way GNSS range measurement assumes knowledge of the orbit and
 * the clock offset of the emitting GNSS satellite. For instance, it is
 * possible to use a SP3 file or a GNSS navigation message to recover
 * the satellite's orbit and clock.
 * <p>
 * This class is very similar to {@link InterSatellitesRange} measurement
 * class. However, using the one-way GNSS range measurement, the orbit and clock
 * of the emitting GNSS satellite are <b>NOT</b> estimated simultaneously with
 * LEO satellite coordinates.
 *
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class OneWayGNSSRange extends AbstractMeasurement<OneWayGNSSRange> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "OneWayGNSSRange";

    /** Emitting satellite. */
    private final PVCoordinatesProvider remote;

    /** Clock offset of the emitting satellite. */
    private final double dtRemote;

    /** Simple constructor.
     * @param remote provider for GNSS satellite which simply emits the signal
     * @param dtRemote clock offset of the GNSS satellite, in seconds
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param local satellite which receives the signal and perform the measurement
     */
    public OneWayGNSSRange(final PVCoordinatesProvider remote,
                           final double dtRemote,
                           final AbsoluteDate date,
                           final double range, final double sigma,
                           final double baseWeight, final ObservableSatellite local) {
        // Call super constructor
        super(date, range, sigma, baseWeight, Collections.singletonList(local));
        // The local satellite clock offset affects the measurement
        addParameterDriver(local.getClockOffsetDriver());
        // Initialise fields
        this.dtRemote = dtRemote;
        this.remote   = remote;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<OneWayGNSSRange> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                                final int evaluation,
                                                                                                final SpacecraftState[] states) {

        // Coordinates of both satellites in local satellite frame
        final SpacecraftState          localState = states[0];
        final TimeStampedPVCoordinates pvaLocal   = localState.getPVCoordinates();
        final TimeStampedPVCoordinates pvaRemote  = remote.getPVCoordinates(getDate(), localState.getFrame());

        // Downlink delay
        final double dtLocal = getSatellites().get(0).getClockOffsetDriver().getValue(localState.getDate());
        final AbsoluteDate arrivalDate = getDate().shiftedBy(-dtLocal);

        final TimeStampedPVCoordinates s1Downlink = pvaLocal.shiftedBy(arrivalDate.durationFrom(pvaLocal.getDate()));
        final double tauD = signalTimeOfFlight(pvaRemote, s1Downlink.getPosition(), arrivalDate);

        // Transit state
        final double delta      = getDate().durationFrom(pvaRemote.getDate());
        final double deltaMTauD = delta - tauD;

        // Estimated measurement
        final EstimatedMeasurementBase<OneWayGNSSRange> estimatedRange =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       new SpacecraftState[] {
                                                           localState.shiftedBy(deltaMTauD)
                                                       }, new TimeStampedPVCoordinates[] {
                                                           pvaRemote.shiftedBy(delta - tauD),
                                                           localState.shiftedBy(delta).getPVCoordinates()
                                                       });

        // Range value
        final double range = (tauD + dtLocal - dtRemote) * Constants.SPEED_OF_LIGHT;

        // Set value of the estimated measurement
        estimatedRange.setEstimatedValue(range);

        // Return the estimated measurement
        return estimatedRange;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<OneWayGNSSRange> theoreticalEvaluation(final int iteration,
                                                                          final int evaluation,
                                                                          final SpacecraftState[] states) {

        // Range derivatives are computed with respect to spacecraft state in inertial frame
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - measurements parameters (clock offset, etc)
        int nbEstimatedParams = 6;
        final Map<String, Integer> parameterIndices = new HashMap<>();
        for (ParameterDriver measurementDriver : getParametersDrivers()) {
            if (measurementDriver.isSelected()) {
                for (Span<String> span = measurementDriver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    parameterIndices.put(span.getData(), nbEstimatedParams++);
                }
            }
        }

        // Coordinates of both satellites in local satellite frame
        final SpacecraftState localState  = states[0];
        final TimeStampedFieldPVCoordinates<Gradient> pvaLocal  = getCoordinates(localState, 0, nbEstimatedParams);
        final TimeStampedPVCoordinates                pvaRemote = remote.getPVCoordinates(getDate(), localState.getFrame());

        // Downlink delay
        final Gradient dtLocal = getSatellites().get(0).getClockOffsetDriver().getValue(nbEstimatedParams, parameterIndices, localState.getDate());
        final FieldAbsoluteDate<Gradient> arrivalDate = new FieldAbsoluteDate<>(getDate(), dtLocal.negate());

        final TimeStampedFieldPVCoordinates<Gradient> s1Downlink = pvaLocal.shiftedBy(arrivalDate.durationFrom(pvaLocal.getDate()));
        final Gradient tauD = signalTimeOfFlight(new TimeStampedFieldPVCoordinates<>(pvaRemote.getDate(), dtLocal.getField().getOne(), pvaRemote),
                                                 s1Downlink.getPosition(), arrivalDate);

        // Transit state
        final double   delta      = getDate().durationFrom(pvaRemote.getDate());
        final Gradient deltaMTauD = tauD.negate().add(delta);

        // Estimated measurement
        final EstimatedMeasurement<OneWayGNSSRange> estimatedRange =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       localState.shiftedBy(deltaMTauD.getValue())
                                                   }, new TimeStampedPVCoordinates[] {
                                                       pvaRemote.shiftedBy(delta - tauD.getValue()),
                                                       localState.shiftedBy(delta).getPVCoordinates()
                                                   });

        // Range value
        final Gradient range            = tauD.add(dtLocal).subtract(dtRemote).multiply(Constants.SPEED_OF_LIGHT);
        final double[] rangeDerivatives = range.getGradient();

        // Set value and state derivatives of the estimated measurement
        estimatedRange.setEstimatedValue(range.getValue());
        estimatedRange.setStateDerivatives(0, Arrays.copyOfRange(rangeDerivatives, 0,  6));

        // Set partial derivatives with respect to parameters
        for (final ParameterDriver measurementDriver : getParametersDrivers()) {
            for (Span<String> span = measurementDriver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                final Integer index = parameterIndices.get(span.getData());
                if (index != null) {
                    estimatedRange.setParameterDerivatives(measurementDriver, span.getStart(), rangeDerivatives[index]);
                }
            }
        }

        // Return the estimated measurement
        return estimatedRange;

    }

}
