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
import java.util.Collections;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.MathUtils;
import org.orekit.frames.Frame;
import org.orekit.signal.AdjustableEmitterSignalTimer;
import org.orekit.signal.FieldAdjustableEmitterSignalTimer;
import org.orekit.signal.FieldSignalReceptionCondition;
import org.orekit.signal.SignalReceptionCondition;
import org.orekit.signal.SignalTravelTimeModel;
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
public abstract class AngularMeasurement<T extends SignalBasedMeasurement<T>> extends SignalBasedMeasurement<T> {

    /** Simple constructor.
     * @param signalTravelTimeModel signal travel time model
     * @param date date of the measurement
     * @param angular observed value
     * @param measurementQuality measurement quality as used in estimation (in Orekit, the crossed-terms
     *                           of the covariance matrix are only used by Kalman filters, not least squares)
     * @param satellite satellite related to this measurement
     */
    protected AngularMeasurement(final AbsoluteDate date,
                                 final double[] angular, final MeasurementQuality measurementQuality,
                                 final SignalTravelTimeModel signalTravelTimeModel,
                                 final ObservableSatellite satellite) {
        super(date, false, angular, measurementQuality, signalTravelTimeModel, Collections.singletonList(satellite));
    }

    /**
     * Compute the signal emission date.
     * @param receptionCondition signal reception condition
     * @param emitter signal emitter
     * @return emission date
     */
    protected AbsoluteDate computeEmissionDate(final SignalReceptionCondition receptionCondition,
                                               final PVCoordinatesProvider emitter) {
        final AdjustableEmitterSignalTimer signalTimer = getSignalTravelTimeModel().getAdjustableEmitterComputer(emitter);
        final double signalTravelTime = signalTimer.computeDelay(receptionCondition);
        return receptionCondition.getReceptionDate().shiftedBy(-signalTravelTime);
    }

    /**
     * Compute the signal emission date.
     * @param frame frame where to perform signal propagation
     * @param receiverPosition signal receiver position at reception
     * @param receptionDate reception date
     * @param emitter signal emitter
     * @return emission date
     */
    protected FieldAbsoluteDate<Gradient> computeEmissionDateField(final Frame frame,
                                                                   final FieldVector3D<Gradient> receiverPosition,
                                                                   final FieldAbsoluteDate<Gradient> receptionDate,
                                                                   final FieldPVCoordinatesProvider<Gradient> emitter) {
        final FieldAdjustableEmitterSignalTimer<Gradient> fieldAdjustableEmitterSignalTimer = getSignalTravelTimeModel().
                getFieldAdjustableEmitterComputer(receptionDate.getField(), emitter);
        final FieldSignalReceptionCondition<Gradient> receptionCondition = new FieldSignalReceptionCondition<>(receptionDate,
                receiverPosition, frame);
        final Gradient signalTravelTime = fieldAdjustableEmitterSignalTimer.computeDelay(receptionCondition,
                receptionDate);
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
