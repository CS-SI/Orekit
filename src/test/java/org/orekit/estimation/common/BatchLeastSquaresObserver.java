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
package org.orekit.estimation.common;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.leastsquares.BatchLSObserver;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.MultiplexedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Position;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.orbits.Orbit;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriversList;

import java.util.Locale;
import java.util.Map;

/**
 * Observer for Batch Least Squares orbit determination.
 */
public class BatchLeastSquaresObserver implements BatchLSObserver {

    /** PV of the previous iteration. */
    private PVCoordinates previousPV;

    /** Batch LS estimator. */
    private BatchLSEstimator estimator;

    /** Constructor.
     * @param initialGuess initial guess
     * @param estimator batch LS estimator
     * @param header header to print
     * @param print true if results must be printed
     */
    public BatchLeastSquaresObserver(final Orbit initialGuess, final BatchLSEstimator estimator,
                                     final String header, final boolean print) {
        this.previousPV = initialGuess.getPVCoordinates();
        this.estimator  = estimator;
        // Print header
        if (print) {
            System.out.format(Locale.US, header);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void evaluationPerformed(final int iterationsCount, final int evaluationsCount,
                                    final Orbit[] orbits,
                                    final ParameterDriversList estimatedOrbitalParameters,
                                    final ParameterDriversList estimatedPropagatorParameters,
                                    final ParameterDriversList estimatedMeasurementsParameters,
                                    final EstimationsProvider  evaluationsProvider,
                                    final LeastSquaresProblem.Evaluation lspEvaluation) {
        final PVCoordinates currentPV = orbits[0].getPVCoordinates();
        final String format0 = "    %2d         %2d                                 %16.12f     %s       %s     %s     %s     %s%n";
        final String format  = "    %2d         %2d      %13.6f %12.9f %16.12f     %s       %s     %s     %s     %s%n";
        final EvaluationCounter<Range>       rangeCounter     = new EvaluationCounter<Range>();
        final EvaluationCounter<RangeRate>   rangeRateCounter = new EvaluationCounter<RangeRate>();
        final EvaluationCounter<AngularAzEl> angularCounter   = new EvaluationCounter<AngularAzEl>();
        final EvaluationCounter<Position>    positionCounter  = new EvaluationCounter<Position>();
        final EvaluationCounter<PV>          pvCounter        = new EvaluationCounter<PV>();
        for (final Map.Entry<ObservedMeasurement<?>, EstimatedMeasurement<?>> entry : estimator.getLastEstimations().entrySet()) {
            logEvaluation(entry.getValue(),
                          rangeCounter, rangeRateCounter, angularCounter, null, positionCounter, pvCounter, null);
        }
        if (evaluationsCount == 1) {
            System.out.format(Locale.US, format0,
                              iterationsCount, evaluationsCount,
                              lspEvaluation.getRMS(),
                              rangeCounter.format(8), rangeRateCounter.format(8),
                              angularCounter.format(8), positionCounter.format(8),
                              pvCounter.format(8));
        } else {
            System.out.format(Locale.US, format,
                              iterationsCount, evaluationsCount,
                              Vector3D.distance(previousPV.getPosition(), currentPV.getPosition()),
                              Vector3D.distance(previousPV.getVelocity(), currentPV.getVelocity()),
                              lspEvaluation.getRMS(),
                              rangeCounter.format(8), rangeRateCounter.format(8),
                              angularCounter.format(8), positionCounter.format(8),
                              pvCounter.format(8));
        }
        previousPV = currentPV;
    }

    /** Log evaluations.
     */
    private void logEvaluation(EstimatedMeasurement<?> evaluation,
                               EvaluationLogger<Range> rangeLog,
                               EvaluationLogger<RangeRate> rangeRateLog,
                               EvaluationLogger<AngularAzEl> azimuthLog,
                               EvaluationLogger<AngularAzEl> elevationLog,
                               EvaluationLogger<Position> positionOnlyLog,
                               EvaluationLogger<PV> positionLog,
                               EvaluationLogger<PV> velocityLog) {

        // Get measurement type and send measurement to proper logger.
        final String measurementType = evaluation.getObservedMeasurement().getMeasurementType();
        if (measurementType.equals(Range.MEASUREMENT_TYPE)) {
            @SuppressWarnings("unchecked")
            final EstimatedMeasurement<Range> ev = (EstimatedMeasurement<Range>) evaluation;
            if (rangeLog != null) {
                rangeLog.log(ev);
            }
        } else if (measurementType.equals(RangeRate.MEASUREMENT_TYPE)) {
            @SuppressWarnings("unchecked")
            final EstimatedMeasurement<RangeRate> ev = (EstimatedMeasurement<RangeRate>) evaluation;
            if (rangeRateLog != null) {
                rangeRateLog.log(ev);
            }
        } else if (measurementType.equals(AngularAzEl.MEASUREMENT_TYPE)) {
            @SuppressWarnings("unchecked")
            final EstimatedMeasurement<AngularAzEl> ev = (EstimatedMeasurement<AngularAzEl>) evaluation;
            if (azimuthLog != null) {
                azimuthLog.log(ev);
            }
            if (elevationLog != null) {
                elevationLog.log(ev);
            }
        }  else if (measurementType.equals(Position.MEASUREMENT_TYPE)) {
            @SuppressWarnings("unchecked")
            final EstimatedMeasurement<Position> ev = (EstimatedMeasurement<Position>) evaluation;
            if (positionOnlyLog != null) {
                positionOnlyLog.log(ev);
            }
        } else if (measurementType.equals(PV.MEASUREMENT_TYPE)) {
            @SuppressWarnings("unchecked")
            final EstimatedMeasurement<PV> ev = (EstimatedMeasurement<PV>) evaluation;
            if (positionLog != null) {
                positionLog.log(ev);
            }
            if (velocityLog != null) {
                velocityLog.log(ev);
            }
        } else if (measurementType.equals(MultiplexedMeasurement.MEASUREMENT_TYPE)) {
            for (final EstimatedMeasurement<?> em : ((MultiplexedMeasurement) evaluation.getObservedMeasurement()).getEstimatedMeasurements()) {
                logEvaluation(em, rangeLog, rangeRateLog, azimuthLog, elevationLog, positionOnlyLog, positionLog, velocityLog);
            }
        }
    }
}
