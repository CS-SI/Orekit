/* Copyright 2002-2024 Thales Alenia Space
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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Arrays;

/** One way range-rate measurement between two satellites.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class InterSatellitesOneWayRangeRate
    extends AbstractInterSatellitesMeasurement<InterSatellitesOneWayRangeRate> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "InterSatellitesOneWayRangeRate";

    /** Constructor.
     * @param local satellite which receives the signal and performs the measurement
     * @param remote remote satellite which simply emits the signal
     * @param date date of the measurement
     * @param rangeRate observed value (m/s)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     */
    public InterSatellitesOneWayRangeRate(final ObservableSatellite local,
                                          final ObservableSatellite remote,
                                          final AbsoluteDate date, final double rangeRate,
                                          final double sigma, final double baseWeight) {
        // Call to super constructor
        super(date, rangeRate, sigma, baseWeight, local, remote);
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<InterSatellitesOneWayRangeRate> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                                               final int evaluation,
                                                                                                               final SpacecraftState[] states) {

        final OnBoardCommonParametersWithoutDerivatives common = computeCommonParametersWithout(states, false);

        // prepare the evaluation
        final EstimatedMeasurementBase<InterSatellitesOneWayRangeRate> estimatedPhase =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       new SpacecraftState[] {
                                                           common.getState(),
                                                           states[1]
                                                       }, new TimeStampedPVCoordinates[] {
                                                           common.getRemotePV(),
                                                           common.getTransitPV()
                                                       });

        // Range rate value
        final PVCoordinates delta = new PVCoordinates(common.getRemotePV(), common.getTransitPV());
        final double rangeRate = Vector3D.dotProduct(delta.getVelocity(), delta.getPosition().normalize()) +
                                 Constants.SPEED_OF_LIGHT * (common.getLocalRate() - common.getRemoteRate());

        estimatedPhase.setEstimatedValue(rangeRate);

        // Return the estimated measurement
        return estimatedPhase;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<InterSatellitesOneWayRangeRate> theoreticalEvaluation(final int iteration,
                                                                                         final int evaluation,
                                                                                         final SpacecraftState[] states) {

        final OnBoardCommonParametersWithDerivatives common = computeCommonParametersWith(states, false);

        // prepare the evaluation
        final EstimatedMeasurement<InterSatellitesOneWayRangeRate> estimatedPhase =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   new SpacecraftState[] {
                                                       common.getState(),
                                                       states[1]
                                                   }, new TimeStampedPVCoordinates[] {
                                                       common.getRemotePV().toTimeStampedPVCoordinates(),
                                                       common.getTransitPV().toTimeStampedPVCoordinates()
                                                   });

        // Range rate value
        final FieldPVCoordinates<Gradient> delta = new FieldPVCoordinates<>(common.getRemotePV(), common.getTransitPV());
        final Gradient rangeRate = FieldVector3D.dotProduct(delta.getVelocity(), delta.getPosition().normalize()).
                                   add(common.getLocalRate().subtract(common.getRemoteRate()).multiply(Constants.SPEED_OF_LIGHT));

        estimatedPhase.setEstimatedValue(rangeRate.getValue());

        // Range first order derivatives with respect to states
        final double[] derivatives = rangeRate.getGradient();
        estimatedPhase.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0,  6));
        estimatedPhase.setStateDerivatives(1, Arrays.copyOfRange(derivatives, 6, 12));

        // Set first order derivatives with respect to parameters
        for (final ParameterDriver driver : getParametersDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                final Integer index = common.getIndices().get(span.getData());
                if (index != null) {
                    estimatedPhase.setParameterDerivatives(driver, span.getStart(), derivatives[index]);
                }
            }
        }

        // Return the estimated measurement
        return estimatedPhase;

    }

}
