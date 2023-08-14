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
package org.orekit.estimation.measurements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeSpanMap.Span;

/** Class multiplexing several measurements as one.
 * <p>
 * Date comes from the first measurement, observed and estimated
 * values result from gathering all underlying measurements values.
 *
 * @author Luc Maisonobe
 * @since 10.1
 */
public class MultiplexedMeasurement extends AbstractMeasurement<MultiplexedMeasurement> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "MultiplexedMeasurement";

    /** Multiplexed measurements. */
    private final List<ObservedMeasurement<?>> observedMeasurements;

    /** Multiplexed measurements without derivatives.
     */
    private final List<EstimatedMeasurementBase<?>> estimatedMeasurementsWithoutDerivatives;

    /** Multiplexed measurements. */
    private final List<EstimatedMeasurement<?>> estimatedMeasurements;

    /** Multiplexed parameters drivers. */
    private ParameterDriversList parametersDrivers;

    /** Total dimension. */
    private final int dimension;

    /** Total number of satellites involved. */
    private final int nbSat;

    /** States mapping. */
    private final int[][] mapping;

    /** Simple constructor.
     * @param measurements measurements to multiplex
     * @since 10.1
     */
    public MultiplexedMeasurement(final List<ObservedMeasurement<?>> measurements) {
        super(measurements.get(0).getDate(),
              multiplex(measurements, m -> m.getObservedValue()),
              multiplex(measurements, m -> m.getTheoreticalStandardDeviation()),
              multiplex(measurements, m -> m.getBaseWeight()),
              multiplex(measurements));

        this.observedMeasurements                    = measurements;
        this.estimatedMeasurementsWithoutDerivatives = new ArrayList<>();
        this.estimatedMeasurements                   = new ArrayList<>();
        this.parametersDrivers                       = new ParameterDriversList();

        // gather parameters drivers
        int dim = 0;
        for (final ObservedMeasurement<?> m : measurements) {
            for (final ParameterDriver driver : m.getParametersDrivers()) {
                parametersDrivers.add(driver);
            }
            dim += m.getDimension();
        }
        parametersDrivers.sort();
        for (final ParameterDriver driver : parametersDrivers.getDrivers()) {
            addParameterDriver(driver);
        }
        this.dimension = dim;

        // set up states mappings for observed satellites
        final List<ObservableSatellite> deduplicated = getSatellites();
        this.nbSat   = deduplicated.size();
        this.mapping = new int[measurements.size()][];
        for (int i = 0; i < mapping.length; ++i) {
            final List<ObservableSatellite> satellites = measurements.get(i).getSatellites();
            mapping[i] = new int[satellites.size()];
            for (int j = 0; j < mapping[i].length; ++j) {
                final int index = satellites.get(j).getPropagatorIndex();
                for (int k = 0; k < nbSat; ++k) {
                    if (deduplicated.get(k).getPropagatorIndex() == index) {
                        mapping[i][j] = k;
                        break;
                    }
                }
            }
        }

    }

    /** Get the underlying measurements.
     * @return underlying measurements
     */
    public List<ObservedMeasurement<?>> getMeasurements() {
        return observedMeasurements;
    }

    /** Get the underlying estimated measurements without derivatives.
     * @return underlying estimated measurements without derivatives
     * @since 12.0
     */
    public List<EstimatedMeasurementBase<?>> getEstimatedMeasurementsWithoutDerivatives() {
        return estimatedMeasurementsWithoutDerivatives;
    }

    /** Get the underlying estimated measurements.
     * @return underlying estimated measurements
     */
    public List<EstimatedMeasurement<?>> getEstimatedMeasurements() {
        return estimatedMeasurements;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<MultiplexedMeasurement> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                                       final int evaluation,
                                                                                                       final SpacecraftState[] states) {

        final SpacecraftState[]              evaluationStates = new SpacecraftState[nbSat];
        final List<TimeStampedPVCoordinates> participants     = new ArrayList<>();
        final double[]                       value            = new double[dimension];

        // loop over all multiplexed measurements
        estimatedMeasurementsWithoutDerivatives.clear();
        int index = 0;
        for (int i = 0; i < observedMeasurements.size(); ++i) {

            // filter states involved in the current measurement
            final SpacecraftState[] filteredStates = new SpacecraftState[mapping[i].length];
            for (int j = 0; j < mapping[i].length; ++j) {
                filteredStates[j] = states[mapping[i][j]];
            }

            // perform evaluation
            final EstimatedMeasurementBase<?> eI = observedMeasurements.get(i).estimateWithoutDerivatives(iteration, evaluation, filteredStates);
            estimatedMeasurementsWithoutDerivatives.add(eI);

            // extract results
            final double[] valueI = eI.getEstimatedValue();
            System.arraycopy(valueI, 0, value, index, valueI.length);
            index += valueI.length;

            // extract states
            final SpacecraftState[] statesI = eI.getStates();
            for (int j = 0; j < mapping[i].length; ++j) {
                evaluationStates[mapping[i][j]] = statesI[j];
            }

        }

        // create multiplexed estimation
        final EstimatedMeasurementBase<MultiplexedMeasurement> multiplexed =
                        new EstimatedMeasurementBase<>(this, iteration, evaluation,
                                                       evaluationStates,
                                                       participants.toArray(new TimeStampedPVCoordinates[0]));

        // copy multiplexed value
        multiplexed.setEstimatedValue(value);

        return multiplexed;

    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<MultiplexedMeasurement> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                                 final SpacecraftState[] states) {

        final SpacecraftState[]              evaluationStates = new SpacecraftState[nbSat];
        final List<TimeStampedPVCoordinates> participants     = new ArrayList<>();
        final double[]                       value            = new double[dimension];

        // loop over all multiplexed measurements
        estimatedMeasurements.clear();
        int index = 0;
        for (int i = 0; i < observedMeasurements.size(); ++i) {

            // filter states involved in the current measurement
            final SpacecraftState[] filteredStates = new SpacecraftState[mapping[i].length];
            for (int j = 0; j < mapping[i].length; ++j) {
                filteredStates[j] = states[mapping[i][j]];
            }

            // perform evaluation
            final EstimatedMeasurement<?> eI = observedMeasurements.get(i).estimate(iteration, evaluation, filteredStates);
            estimatedMeasurements.add(eI);

            // extract results
            final double[] valueI = eI.getEstimatedValue();
            System.arraycopy(valueI, 0, value, index, valueI.length);
            index += valueI.length;

            // extract states
            final SpacecraftState[] statesI = eI.getStates();
            for (int j = 0; j < mapping[i].length; ++j) {
                evaluationStates[mapping[i][j]] = statesI[j];
            }

        }

        // create multiplexed estimation
        final EstimatedMeasurement<MultiplexedMeasurement> multiplexed =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                                   evaluationStates,
                                                   participants.toArray(new TimeStampedPVCoordinates[0]));

        // copy multiplexed value
        multiplexed.setEstimatedValue(value);

        // combine derivatives
        final int                            stateSize             = estimatedMeasurements.get(0).getStateSize();
        final double[]                       zeroDerivative        = new double[stateSize];
        final double[][][]                   stateDerivatives      = new double[nbSat][dimension][];
        for (final double[][] m : stateDerivatives) {
            Arrays.fill(m, zeroDerivative);
        }

        final Map<ParameterDriver, TimeSpanMap<double[]>> parametersDerivatives = new IdentityHashMap<>();
        index = 0;
        for (int i = 0; i < observedMeasurements.size(); ++i) {

            final EstimatedMeasurement<?> eI   = estimatedMeasurements.get(i);
            final int                     idx  = index;
            final int                     dimI = eI.getObservedMeasurement().getDimension();

            // state derivatives
            for (int j = 0; j < mapping[i].length; ++j) {
                System.arraycopy(eI.getStateDerivatives(j), 0,
                                 stateDerivatives[mapping[i][j]], index,
                                 dimI);
            }

            // parameters derivatives
            eI.getDerivativesDrivers().forEach(driver -> {
                final ParameterDriversList.DelegatingDriver delegating = parametersDrivers.findByName(driver.getName());

                if (parametersDerivatives.get(delegating) == null) {
                    final TimeSpanMap<double[]> derivativeSpanMap = new TimeSpanMap<double[]>(new double[dimension]);
                    parametersDerivatives.put(delegating, derivativeSpanMap);
                }

                final TimeSpanMap<Double> driverNameSpan = delegating.getValueSpanMap();
                for (Span<Double> span = driverNameSpan.getSpan(driverNameSpan.getFirstSpan().getEnd()); span != null; span = span.next()) {

                    double[] derivatives = parametersDerivatives.get(delegating).get(span.getStart());
                    if (derivatives == null) {
                        derivatives = new double[dimension];
                    }
                    if (!parametersDerivatives.get(delegating).getSpan(span.getStart()).getStart().equals(span.getStart())) {
                        if ((span.getStart()).equals(AbsoluteDate.PAST_INFINITY)) {
                            parametersDerivatives.get(delegating).addValidBefore(derivatives, span.getEnd(), false);
                        } else {
                            parametersDerivatives.get(delegating).addValidAfter(derivatives, span.getStart(), false);
                        }

                    }

                    System.arraycopy(eI.getParameterDerivatives(driver, span.getStart()), 0, derivatives, idx, dimI);

                }

            });

            index += dimI;

        }

        // set states derivatives
        for (int i = 0; i < nbSat; ++i) {
            multiplexed.setStateDerivatives(i, stateDerivatives[i]);
        }

        // set parameters derivatives
        parametersDerivatives.
            entrySet().
            stream().
            forEach(e -> multiplexed.setParameterDerivatives(e.getKey(), e.getValue()));

        return multiplexed;

    }

    /** Multiplex measurements data.
     * @param measurements measurements to multiplex
     * @param extractor data extraction function
     * @return multiplexed data
     */
    private static double[] multiplex(final List<ObservedMeasurement<?>> measurements,
                                      final Function<ObservedMeasurement<?>, double[]> extractor) {

        // gather individual parts
        final List<double[]> parts = new ArrayList<> (measurements.size());
        int n = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {
            final double[] p = extractor.apply(measurement);
            parts.add(p);
            n += p.length;
        }

        // create multiplexed data
        final double[] multiplexed = new double[n];
        int index = 0;
        for (final double[] p : parts) {
            System.arraycopy(p, 0, multiplexed, index, p.length);
            index += p.length;
        }

        return multiplexed;

    }

    /** Multiplex satellites data.
     * @param measurements measurements to multiplex
     * @return multiplexed satellites data
     */
    private static List<ObservableSatellite> multiplex(final List<ObservedMeasurement<?>> measurements) {

        final List<ObservableSatellite> satellites = new ArrayList<>();

        // gather all satellites, removing duplicates
        for (final ObservedMeasurement<?> measurement : measurements) {
            for (final ObservableSatellite satellite : measurement.getSatellites()) {
                boolean searching = true;
                for (int i = 0; i < satellites.size() && searching; ++i) {
                    // check if we already know this satellite
                    searching = satellite.getPropagatorIndex() != satellites.get(i).getPropagatorIndex();
                }
                if (searching) {
                    // this is a new satellite, add it to the global list
                    satellites.add(satellite);
                }
            }
        }

        return satellites;

    }

}
