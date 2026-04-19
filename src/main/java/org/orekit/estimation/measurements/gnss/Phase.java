/* Copyright 2002-2026 CS GROUP
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
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.AbstractParticipant;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.MeasurementQuality;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Observer;
import org.orekit.estimation.measurements.SignalBasedMeasurement;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.AdjustableEmitterSignalTimer;
import org.orekit.signal.FieldAdjustableEmitterSignalTimer;
import org.orekit.signal.FieldSignalReceptionCondition;
import org.orekit.signal.SignalReceptionCondition;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a phase measurement from a ground station.
 * <p>
 * The measurement is considered to be a signal emitted from
 * a spacecraft and received on a ground station.
 * Its value is the number of cycles between emission and
 * reception. The motion of both the station and the
 * spacecraft during the signal flight time are taken into
 * account. The date of the measurement corresponds to the
 * reception on ground of the emitted signal.
 * </p>
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 9.2
 */
public class Phase extends SignalBasedMeasurement<Phase> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "Phase";

    /** Driver for ambiguity. */
    private final AmbiguityDriver ambiguityDriver;

    /** Wavelength of the phase observed value [m]. */
    private final double wavelength;

    /** Observer that receives signal from satellite. */
    private final Observer observer;

    /** Simple constructor.
     * @param observer observer that performs the measurement
     * @param date date of the measurement
     * @param phase observed value (cycles)
     * @param wavelength phase observed value wavelength (m)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @param cache from which ambiguity drive should come
     * @since 12.1
     */
    public Phase(final Observer observer, final AbsoluteDate date,
                 final double phase, final double wavelength, final double sigma,
                 final double baseWeight, final ObservableSatellite satellite,
                 final AmbiguityCache cache) {
        this(observer, date, phase, wavelength, new MeasurementQuality(sigma, baseWeight), new SignalTravelTimeModel(),
                satellite, cache);
    }

    /** Simple constructor.
     * @param observer observer that performs the measurement
     * @param date date of the measurement
     * @param phase observed value (cycles)
     * @param wavelength phase observed value wavelength (m)
     * @param measurementQuality measurement quality data as used in orbit determination
     * @param signalTravelTimeModel signal model
     * @param satellite satellite related to this measurement
     * @param cache from which ambiguity drive should come
     * @since 14.0
     */
    public Phase(final Observer observer, final AbsoluteDate date,
                 final double phase, final double wavelength, final MeasurementQuality measurementQuality,
                 final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite satellite,
                 final AmbiguityCache cache) {
        super(date, false, phase, measurementQuality, signalTravelTimeModel,
                Collections.singletonList(satellite));
        ambiguityDriver = cache.getAmbiguity(satellite.getName(), observer.getName(), wavelength);
        addParametersDrivers(observer.getParametersDrivers());
        addParameterDriver(ambiguityDriver);
        this.observer = observer;
        this.wavelength = wavelength;
    }

    /** Get receiving observer.
     * @return observer
     */
    public final Observer getObserver() {
        return observer;
    }

    /** Get the wavelength.
     * @return wavelength (m)
     */
    public double getWavelength() {
        return wavelength;
    }

    /** Get the driver for phase ambiguity.
     * @return the driver for phase ambiguity
     * @since 10.3
     */
    public AmbiguityDriver getAmbiguityDriver() {
        return ambiguityDriver;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<Phase> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                      final int evaluation,
                                                                                      final SpacecraftState[] states) {
        // Coordinates of the measured spacecraft
        final SpacecraftState state = states[0];
        final Frame frame = state.getFrame();
        final TimeStampedPVCoordinates pva   = state.getPVCoordinates();

        // transform between remote observer frame and inertial frame
        final AbsoluteDate measurementDate = getDate();
        final Transform offsetToInertialDownlink = getObserver().getOffsetToInertial(frame, measurementDate, false);
        final AbsoluteDate downlinkDate             = offsetToInertialDownlink.getDate();

        // Observer position in inertial frame at end of the downlink leg
        final TimeStampedPVCoordinates origin = new TimeStampedPVCoordinates(downlinkDate, PVCoordinates.ZERO);
        final TimeStampedPVCoordinates satelliteDownlink = offsetToInertialDownlink.transformPVCoordinates(origin);

        // Coordinates provider for emitting object (observed spacecraft)
        final PVCoordinatesProvider pvCoordinatesProvider = AbstractParticipant.extractPVCoordinatesProvider(states[0], pva);

        // Downlink delay / determine time of emission of signal by ObservableSatellite
        final AdjustableEmitterSignalTimer signalTimeOfFlight = getSignalTravelTimeModel()
                .getAdjustableEmitterComputer(pvCoordinatesProvider);
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(downlinkDate,
                satelliteDownlink.getPosition(), frame);
        final double tauD = signalTimeOfFlight.computeDelay(receptionCondition, pva.getDate());

        // Transit state & Transit state (re)computed with gradients
        final double          delta             = downlinkDate.durationFrom(state.getDate());
        final double          deltaMTauD        = delta - tauD;
        final SpacecraftState transitState      = states[0].shiftedBy(deltaMTauD);

        // prepare the evaluation
        final EstimatedMeasurementBase<Phase> estimated =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       new SpacecraftState[] {
                                                           transitState
                                                       }, new TimeStampedPVCoordinates[] {
                                                           transitState.getPVCoordinates(), satelliteDownlink
                                                       });

        // Clock offsets
        final ObservableSatellite satellite = getSatellites().get(0);

        final double dts = satellite.getOffsetValue(state.getDate());
        final double dtg = getObserver().getOffsetValue(getDate());

        // Phase value
        final double cOverLambda = Constants.SPEED_OF_LIGHT / wavelength;
        final double ambiguity   = ambiguityDriver.getValue(state.getDate());
        final double phase       = (tauD + dtg - dts) * cOverLambda + ambiguity;

        estimated.setEstimatedValue(phase);

        return estimated;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<Phase> theoreticalEvaluation(final int iteration,
                                                                final int evaluation,
                                                                final SpacecraftState[] states) {
        // Create the parameter indices map
        final SpacecraftState state = states[0];
        final Frame                frame        = state.getFrame();
        final Map<String, Integer> paramIndices = getParameterIndices(states);
        final int                  nbParams     = 6 * states.length + paramIndices.size();

        // Coordinates of the spacecraft expressed as a gradient
        final TimeStampedFieldPVCoordinates<Gradient> pva = AbstractMeasurement.getCoordinates(states[0], 0, nbParams);

        // transform between Observer object and inertial frame, expressed as a gradient
        // The components of the Observer's position in offset frame are the 3 last derivative parameters
        final FieldTransform<Gradient> offsetToInertialDownlink = getObserver().
                getOffsetToInertial(frame, getDate(), nbParams, paramIndices);
        final FieldAbsoluteDate<Gradient> downlinkDate = offsetToInertialDownlink.getFieldDate();

        // Observer position in inertial frame at end of the downlink leg
        final GradientField field = GradientField.getField(nbParams);
        final TimeStampedFieldPVCoordinates<Gradient> satelliteDownlink =
                offsetToInertialDownlink.transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(downlinkDate,
                        FieldPVCoordinates.getZero(field)));

        // Form coordinates provider
        final FieldPVCoordinatesProvider<Gradient> fieldPVCoordinatesProvider = AbstractParticipant.extractFieldPVCoordinatesProvider(states[0], pva);

        // Downlink delay
        final FieldAdjustableEmitterSignalTimer<Gradient> fieldComputer = getSignalTravelTimeModel()
                .getFieldAdjustableEmitterComputer(field, fieldPVCoordinatesProvider);
        final FieldSignalReceptionCondition<Gradient> receptionCondition = new FieldSignalReceptionCondition<>(downlinkDate,
                satelliteDownlink.getPosition(), frame);
        final Gradient tauD = fieldComputer.computeDelay(receptionCondition, pva.getDate());

        // Transit state & Transit state (re)computed with gradients
        final Gradient        delta        = downlinkDate.durationFrom(states[0].getDate());
        final Gradient        deltaMTauD   = tauD.negate().add(delta);
        final SpacecraftState transitState = states[0].shiftedBy(deltaMTauD.getValue());
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, states[0].getDate()).shiftedBy(deltaMTauD);
        final TimeStampedFieldPVCoordinates<Gradient> transitPV = fieldPVCoordinatesProvider.getPVCoordinates(fieldDate, frame);

        // prepare the evaluation
        final EstimatedMeasurement<Phase> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                new SpacecraftState[] { transitState},
                                new TimeStampedPVCoordinates[] {
                                        transitPV.toTimeStampedPVCoordinates(),
                                        satelliteDownlink.toTimeStampedPVCoordinates()});

        // Clock offsets
        final ObservableSatellite satellite = getSatellites().get(0);

        final Gradient dts = satellite.getFieldOffsetValue(nbParams, state.getDate(), paramIndices);
        final Gradient dtg = getObserver().getFieldOffsetValue(nbParams, getDate(), paramIndices);

        // Phase value
        final double   cOverLambda = Constants.SPEED_OF_LIGHT / wavelength;
        final Gradient ambiguity   = ambiguityDriver.getValue(nbParams, paramIndices, state.getDate());
        final Gradient phase       = tauD.add(dtg).subtract(dts).multiply(cOverLambda).add(ambiguity);

        estimated.setEstimatedValue(phase.getValue());

        // Phase first order derivatives with respect to state
        final double[] derivatives = phase.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0, 6));

        // Set first order derivatives with respect to parameters
        for (final ParameterDriver driver : getParametersDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                final Integer index = paramIndices.get(span.getData());
                if (index != null) {
                    estimated.setParameterDerivatives(driver, span.getStart(), derivatives[index]);
                }
            }
        }

        return estimated;

    }

}
