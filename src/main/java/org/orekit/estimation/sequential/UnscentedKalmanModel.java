/* Copyright 2002-2024 CS GROUP
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
package org.orekit.estimation.sequential;

import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.filtering.kalman.unscented.UnscentedEvolution;
import org.hipparchus.filtering.kalman.unscented.UnscentedProcess;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.AbstractPropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

import java.util.List;

/** Class defining the process model dynamics to use with a {@link UnscentedKalmanEstimator}.
 * @author Gaëtan Pierre
 * @author Bryan Cazabonne
 * @since 11.3
 */
public class UnscentedKalmanModel extends KalmanEstimationCommon implements UnscentedProcess<MeasurementDecorator> {

    /** Reference values. */
    private final double[] referenceValues;

    /** Unscented Kalman process model constructor (package private).
     * @param propagatorBuilders propagators builders used to evaluate the orbits.
     * @param covarianceMatricesProviders provider for covariance matrix
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     */
    protected UnscentedKalmanModel(final List<PropagatorBuilder> propagatorBuilders,
                                   final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                                   final ParameterDriversList estimatedMeasurementParameters,
                                   final CovarianceMatrixProvider measurementProcessNoiseMatrix) {

        super(propagatorBuilders, covarianceMatricesProviders, estimatedMeasurementParameters, measurementProcessNoiseMatrix);

        // Record the initial reference values
        int stateDimension = 0;
        for (final ParameterDriver ignored : getEstimatedOrbitalParameters().getDrivers()) {
            stateDimension += 1;
        }
        for (final ParameterDriver ignored : getEstimatedPropagationParameters().getDrivers()) {
            stateDimension += 1;
        }
        for (final ParameterDriver ignored : getEstimatedMeasurementsParameters().getDrivers()) {
            stateDimension += 1;
        }

        this.referenceValues = new double[stateDimension];
        int index = 0;
        for (final ParameterDriver driver : getEstimatedOrbitalParameters().getDrivers()) {
            referenceValues[index++] = driver.getReferenceValue();
        }
        for (final ParameterDriver driver : getEstimatedPropagationParameters().getDrivers()) {
            referenceValues[index++] = driver.getReferenceValue();
        }
        for (final ParameterDriver driver : getEstimatedMeasurementsParameters().getDrivers()) {
            referenceValues[index++] = driver.getReferenceValue();
        }
    }

    /** {@inheritDoc} */
    @Override
    public UnscentedEvolution getEvolution(final double previousTime, final RealVector[] sigmaPoints,
                                           final MeasurementDecorator measurement) {

        // Set a reference date for all measurements parameters that lack one (including the not estimated ones)
        final ObservedMeasurement<?> observedMeasurement = measurement.getObservedMeasurement();
        for (final ParameterDriver driver : observedMeasurement.getParametersDrivers()) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(getBuilders().get(0).getInitialOrbitDate());
            }
        }

        // Increment measurement number
        incrementCurrentMeasurementNumber();

        // Update the current date
        setCurrentDate(measurement.getObservedMeasurement().getDate());

        // Initialize array of predicted sigma points
        final RealVector[] predictedSigmaPoints = new RealVector[sigmaPoints.length];

        // Propagate each sigma point
        //
        // We need to make a choice about what happens with the non-estimated parts of the orbital states.
        // Here we've assumed that the zero'th sigma point is roughly the mean and keep those propagated
        // orbital parameters.  This is why we loop backward through the sigma-points and don't reset the
        // propagator builders on the last iteration (corresponding to the zero-th sigma point).
        //
        // Note that -not- resetting the builders on the last iteration means that their time-stamps correspond
        // to the prediction time.  The assumption is that the unscented filter calls getEvolution, then
        // getPredictedMeasurements, then getInnovation.
        for (int i = sigmaPoints.length - 1; i >= 0; i--) {

            // Set parameters for this sigma point
            final RealVector sigmaPoint = sigmaPoints[i].copy();
            updateParameters(sigmaPoint);

            // Get propagators
            final Propagator[] propagators = getEstimatedPropagators();

            // Do prediction
            predictedSigmaPoints[i] =
                    predictState(observedMeasurement.getDate(), sigmaPoint, propagators, i != 0);
        }

        // Reset the driver reference values based on the first sigma point
        int d = 0;
        for (final DelegatingDriver driver : getEstimatedOrbitalParameters().getDrivers()) {
            driver.setReferenceValue(referenceValues[d]);
            driver.setNormalizedValue(predictedSigmaPoints[0].getEntry(d));
            referenceValues[d] = driver.getValue();

            // Make remaining sigma points relative to the first
            for (int i = 1; i < predictedSigmaPoints.length; ++i) {
                predictedSigmaPoints[i].setEntry(d, predictedSigmaPoints[i].getEntry(d) - predictedSigmaPoints[0].getEntry(d));
            }
            predictedSigmaPoints[0].setEntry(d, 0.0);

            d += 1;
        }

        // Return
        return new UnscentedEvolution(measurement.getTime(), predictedSigmaPoints);
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getProcessNoiseMatrix(final double previousTime, final RealVector predictedState,
                                            final MeasurementDecorator measurement) {
        // Set parameters from predicted state
        final RealVector predictedStateCopy = predictedState.copy();
        updateParameters(predictedStateCopy);

        // Get propagators
        Propagator[] propagators = getEstimatedPropagators();

        // "updateParameters" sets the correct orbital info, but doesn't reset the time.
        for (int k = 0; k < propagators.length; ++k) {
            final SpacecraftState predicted = propagators[k].getInitialState();
            final Orbit predictedOrbit = getBuilders().get(k).getOrbitType().convertType(
                    new CartesianOrbit(predicted.getPVCoordinates(),
                            predicted.getFrame(),
                            measurement.getObservedMeasurement().getDate(),
                            predicted.getMu()
                    )
            );
            getBuilders().get(k).resetOrbit(predictedOrbit);
        }
        propagators = getEstimatedPropagators();

        // Predicted states
        for (int k = 0; k < propagators.length; ++k) {
            setPredictedSpacecraftState(propagators[k].getInitialState(), k);
        }

        return getNormalizedProcessNoise(predictedState.getDimension());
    }

    /** {@inheritDoc} */
    @Override
    public RealVector[] getPredictedMeasurements(final RealVector[] predictedSigmaPoints, final MeasurementDecorator measurement) {

        // Observed measurement
        final ObservedMeasurement<?> observedMeasurement = measurement.getObservedMeasurement();

        // Standard deviation as a vector
        final RealVector theoreticalStandardDeviation =
                MatrixUtils.createRealVector(observedMeasurement.getTheoreticalStandardDeviation());

        // Initialize arrays of predicted states and measurements
        final RealVector[] predictedMeasurements = new RealVector[predictedSigmaPoints.length];

        // Loop on sigma points to predict measurements
        for (int i = 0; i < predictedSigmaPoints.length; ++i) {
            // Set parameters for this sigma point
            final RealVector predictedSigmaPoint = predictedSigmaPoints[i].copy();
            updateParameters(predictedSigmaPoint);

            // Get propagators
            final Propagator[] propagators = getEstimatedPropagators();

            // Predicted states
            final SpacecraftState[] predictedStates = new SpacecraftState[propagators.length];
            for (int k = 0; k < propagators.length; ++k) {
                predictedStates[k] = propagators[k].getInitialState();
            }

            // Calculated estimated measurement from predicted sigma point
            final EstimatedMeasurement<?> estimated = estimateMeasurement(observedMeasurement, getCurrentMeasurementNumber(),
                                                                                   KalmanEstimatorUtil.filterRelevant(observedMeasurement,
                                                                                                                      predictedStates));
            predictedMeasurements[i] = new ArrayRealVector(estimated.getEstimatedValue())
                    .ebeDivide(theoreticalStandardDeviation);
        }

        // Return the predicted measurements
        return predictedMeasurements;

    }

    /** {@inheritDoc} */
    @Override
    public RealVector getInnovation(final MeasurementDecorator measurement, final RealVector predictedMeas,
                                    final RealVector predictedState, final RealMatrix innovationCovarianceMatrix) {
        // Standard deviation as a vector
        final RealVector theoreticalStandardDeviation =
                MatrixUtils.createRealVector(measurement.getObservedMeasurement().getTheoreticalStandardDeviation());

        // Get propagators
        final Propagator[] propagators = getEstimatedPropagators();

        // Predicted states
        for (int k = 0; k < propagators.length; ++k) {
            setPredictedSpacecraftState(propagators[k].getInitialState(), k);
        }

        // set estimated value to the predicted value from the filter
        final EstimatedMeasurement<?> predictedMeasurement =
            estimateMeasurement(measurement.getObservedMeasurement(), getCurrentMeasurementNumber(),
                                KalmanEstimatorUtil.filterRelevant(measurement.getObservedMeasurement(),
                                getPredictedSpacecraftStates()));
        setPredictedMeasurement(predictedMeasurement);
        predictedMeasurement.setEstimatedValue(predictedMeas.ebeMultiply(theoreticalStandardDeviation).toArray());

        // Check for outliers
        KalmanEstimatorUtil.applyDynamicOutlierFilter(predictedMeasurement, innovationCovarianceMatrix);

        // Compute the innovation vector
        return KalmanEstimatorUtil.computeInnovationVector(predictedMeasurement,
                predictedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation());
    }


    private RealVector predictState(final AbsoluteDate date,
                                    final RealVector previousState,
                                    final Propagator[] propagators,
                                    final boolean resetState) {

        // Initialise predicted state
        final RealVector predictedState = previousState.copy();

        // Orbital parameters counter
        int jOrb = 0;

        // Loop over propagators
        for (int k = 0; k < propagators.length; ++k) {

            // Record original state
            final SpacecraftState originalState = propagators[k].getInitialState();

            // Propagate
            final SpacecraftState predicted = propagators[k].propagate(date);

            // Update the builder with the predicted orbit
            // This updates the orbital drivers with the values of the predicted orbit
            getBuilders().get(k).resetOrbit(predicted.getOrbit());

            // Additionally, for PropagatorBuilders which use mass, update the builder with the predicted mass value.
            // If any mass changes have occurred during this estimation step, such as maneuvers,
            // the updated mass value must be carried over so that new Propagators from this builder start with the updated mass.
            if (getBuilders().get(k) instanceof AbstractPropagatorBuilder) {
                ((AbstractPropagatorBuilder) (getBuilders().get(k))).setMass(predicted.getMass());
            }

            // The orbital parameters in the state vector are replaced with their predicted values
            // The propagation & measurement parameters are not changed by the prediction (i.e. the propagation)
            // As the propagator builder was previously updated with the predicted orbit,
            // the selected orbital drivers are already up to date with the prediction
            for (DelegatingDriver orbitalDriver : getBuilders().get(k).getOrbitalParametersDrivers().getDrivers()) {
                if (orbitalDriver.isSelected()) {
                    orbitalDriver.setReferenceValue(referenceValues[jOrb]);
                    predictedState.setEntry(jOrb, orbitalDriver.getNormalizedValue());

                    jOrb += 1;
                }
            }

            // Set the builder back to the original time
            if (resetState) {
                getBuilders().get(k).resetOrbit(originalState.getOrbit());
            }
        }

        return predictedState;
    }


    /** Finalize estimation.
     * @param observedMeasurement measurement that has just been processed
     * @param estimate corrected estimate
     */
    public void finalizeEstimation(final ObservedMeasurement<?> observedMeasurement,
                                   final ProcessEstimate estimate) {
        // Update the parameters with the estimated state
        // The min/max values of the parameters are handled by the ParameterDriver implementation
        setCorrectedEstimate(estimate);
        updateParameters(estimate.getState());

        // Get the estimated propagator (mirroring parameter update in the builder)
        // and the estimated spacecraft state
        final Propagator[] estimatedPropagators = getEstimatedPropagators();
        for (int k = 0; k < estimatedPropagators.length; ++k) {
            setCorrectedSpacecraftState(estimatedPropagators[k].getInitialState(), k);
        }

        // Corrected measurement
        setCorrectedMeasurement(estimateMeasurement(observedMeasurement, getCurrentMeasurementNumber(),
                                                    KalmanEstimatorUtil.filterRelevant(observedMeasurement,
                                                    getCorrectedSpacecraftStates())));
    }

    /**
     * Estimate measurement (without derivatives).
     * @param <T> measurement type
     * @param observedMeasurement observed measurement
     * @param measurementNumber measurement number
     * @param spacecraftStates states
     * @return estimated measurement
     * @since 12.1
     */
    private static <T extends ObservedMeasurement<T>> EstimatedMeasurement<T> estimateMeasurement(final ObservedMeasurement<T> observedMeasurement,
                                                                                                  final int measurementNumber,
                                                                                                  final SpacecraftState[] spacecraftStates) {
        final EstimatedMeasurementBase<T> estimatedMeasurementBase = observedMeasurement.
                estimateWithoutDerivatives(measurementNumber, measurementNumber,
                KalmanEstimatorUtil.filterRelevant(observedMeasurement, spacecraftStates));
        return new EstimatedMeasurement<>(estimatedMeasurementBase);
    }

    /** Update parameter drivers with a normalised state, adjusting state according to the driver limits.
     * @param normalizedState the input state
     * The min/max allowed values are handled by the parameter themselves.
     */
    private void updateParameters(final RealVector normalizedState) {
        int i = 0;
        for (final DelegatingDriver driver : getEstimatedOrbitalParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setReferenceValue(referenceValues[i]);
            driver.setNormalizedValue(normalizedState.getEntry(i));
            normalizedState.setEntry(i++, driver.getNormalizedValue());
        }
        for (final DelegatingDriver driver : getEstimatedPropagationParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(normalizedState.getEntry(i));
            normalizedState.setEntry(i++, driver.getNormalizedValue());
        }
        for (final DelegatingDriver driver : getEstimatedMeasurementsParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(normalizedState.getEntry(i));
            normalizedState.setEntry(i++, driver.getNormalizedValue());
        }
    }
}
