/* Copyright 2022-2026 Romain Serra
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
package org.orekit.estimation.measurements;

import java.util.Arrays;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.util.MathUtils;
import org.orekit.estimation.measurements.signal.FieldSignalTravelTimeAdjustableEmitter;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

/** Abstract class for ground-based angular measurements, when the sensor receives the signal.
 *
 * @author Romain Serra
 * @since 14.0
 */
public abstract class GroundBasedAngularMeasurement<T extends ObservedMeasurement<T>> extends GroundReceiverMeasurement<T> {

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param signalTravelTimeModel signal travel time model
     * @param date date of the measurement
     * @param angular observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     */
    protected GroundBasedAngularMeasurement(final GroundStation station, final AbsoluteDate date,
                                            final double[] angular, final double[] sigma, final double[] baseWeight,
                                            final SignalTravelTimeModel signalTravelTimeModel,
                                            final ObservableSatellite satellite) {
        super(station, false, date, angular, sigma, baseWeight, signalTravelTimeModel, satellite);
    }

    /** Get the ground station that receives the signal.
     * @return ground station
     */
    public final GroundStation getStation() {
        return getReceiverStation();
    }

    /**
     * Compute the signal emission date.
     * @param frame frame where to perform signal propagation
     * @param receiver signal receiver
     * @param receptionDate reception date
     * @param emitter signal emitter
     * @return emission date
     */
    protected AbsoluteDate computeEmissionDate(final Frame frame, final PVCoordinatesProvider receiver,
                                               final AbsoluteDate receptionDate, final PVCoordinatesProvider emitter) {
        final double signalTravelTime = getSignalTravelTimeModel().getAdjustableEmitterComputer(emitter)
                .computeDelay(receptionDate, receiver.getPosition(receptionDate, frame), receptionDate, frame);
        return receptionDate.shiftedBy(-signalTravelTime);
    }

    /**
     * Compute the signal emission date.
     * @param frame frame where to perform signal propagation
     * @param receiver signal receiver
     * @param receptionDate reception date
     * @param emitter signal emitter
     * @return emission date
     */
    protected FieldAbsoluteDate<Gradient> computeEmissionDateField(final Frame frame,
                                                                   final FieldPVCoordinatesProvider<Gradient> receiver,
                                                                   final FieldAbsoluteDate<Gradient> receptionDate,
                                                                   final FieldPVCoordinatesProvider<Gradient> emitter) {
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> fieldSignalTravelTimeAdjustableEmitter = getSignalTravelTimeModel().
                getFieldAdjustableEmitterComputer(receptionDate.getField(), emitter);
        final Gradient signalTravelTime = fieldSignalTravelTimeAdjustableEmitter.computeDelay(receptionDate,
                receiver.getPosition(receptionDate, frame), receptionDate, frame);
        return receptionDate.shiftedBy(signalTravelTime.negate());
    }

    /**
     * Wrap angle according to observed one.
     * @param baseAngle base angle
     * @return wrapped angle
     */
    protected double wrapFirstAngle(final double baseAngle) {
        final double twoPiWrap = MathUtils.normalizeAngle(baseAngle, getObservedValue()[0]) - baseAngle;
        return baseAngle + twoPiWrap;
    }

    /**
     * Wrap angle according to observed one.
     * @param baseAngle base angle
     * @return wrapped angle
     */
    protected Gradient wrapFirstAngle(final Gradient baseAngle) {
        final double twoPiWrap = MathUtils.normalizeAngle(baseAngle.getReal(), getObservedValue()[0]) - baseAngle.getReal();
        return baseAngle.add(twoPiWrap);
    }

    /**
     * Method filling estimated measurement.
     * @param firstAngle first angle
     * @param secondAngle second angle
     * @param paramIndices mapping between parameter name and variable index
     * @param estimatedMeasurement object to fill
     */
    protected void fillEstimatedMeasurement(final Gradient firstAngle, final Gradient secondAngle,
                                            final Map<String, Integer> paramIndices, final EstimatedMeasurement<T> estimatedMeasurement) {
        // azimuth - elevation values
        final Gradient wrappedAngle = wrapFirstAngle(firstAngle);
        estimatedMeasurement.setEstimatedValue(wrappedAngle.getValue(), secondAngle.getValue());

        // First order derivatives of azimuth/elevation with respect to state
        final double[] azDerivatives = firstAngle.getGradient();
        final double[] elDerivatives = secondAngle.getGradient();
        fillDerivatives(azDerivatives, elDerivatives, paramIndices, estimatedMeasurement);
    }

    /**
     * Method filling derivatives in the estimated measurement.
     * @param firstAngleDerivatives first angle derivatives
     * @param secondAngleDerivatives second angle derivatives
     * @param paramIndices mapping between parameter name and variable index
     * @param estimatedMeasurement object to fill
     */
    protected void fillDerivatives(final double[] firstAngleDerivatives, final double[] secondAngleDerivatives,
                                   final Map<String, Integer> paramIndices, final EstimatedMeasurement<T> estimatedMeasurement) {
        estimatedMeasurement.setStateDerivatives(0,
                Arrays.copyOfRange(firstAngleDerivatives, 0, 6), Arrays.copyOfRange(secondAngleDerivatives, 0, 6));

        // Set first order derivatives of azimuth/elevation with respect to state
        for (final ParameterDriver driver : getParametersDrivers()) {

            for (TimeSpanMap.Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                final Integer index = paramIndices.get(span.getData());
                if (index != null) {
                    estimatedMeasurement.setParameterDerivatives(driver, span.getStart(), firstAngleDerivatives[index],
                            secondAngleDerivatives[index]);
                }
            }
        }
    }
}
