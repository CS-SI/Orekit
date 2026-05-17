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
package org.orekit.estimation.sequential;

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.filtering.kalman.extended.ExtendedKalmanFilter;
import org.hipparchus.filtering.kalman.extended.NonLinearEvolution;
import org.hipparchus.filtering.kalman.extended.NonLinearProcess;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTHarvester;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeSpanMap.Span;

import java.util.ArrayList;
import java.util.List;

/** Process model to use with a {@link SemiAnalyticalKalmanEstimator}.
 *
 * @see "Folcik Z., Orbit Determination Using Modern Filters/Smoothers and Continuous Thrust Modeling,
 *       Master of Science Thesis, Department of Aeronautics and Astronautics, MIT, June, 2008."
 *
 * @see "Cazabonne B., Bayard J., Journot M., and Cefola P. J., A Semi-analytical Approach for Orbit
 *       Determination based on Extended Kalman Filter, AAS Paper 21-614, AAS/AIAA Astrodynamics
 *       Specialist Conference, Big Sky, August 2021."
 *
 * @author Julie Bayard
 * @author Bryan Cazabonne
 * @author Maxime Journot
 * @since 11.1
 */
public class SemiAnalyticalKalmanModel extends AbstractSemiAnalyticalKalmanEstimationCommon
                                       implements NonLinearProcess<MeasurementDecorator>, SemiAnalyticalProcess {

    /** Harvester between two-dimensional Jacobian matrices and one-dimensional additional state arrays. */
    private DSSTHarvester[] harvesters;

    /** Propagators for the reference trajectories, up to current date. */
    private DSSTPropagator[] dsstPropagators;

    /** Observer to retrieve current estimation info. */
    private KalmanObserver observer;

    /** Predicted mean element filter correction. */
    private RealVector predictedFilterCorrection;

    /** Corrected mean element filter correction. */
    private RealVector correctedFilterCorrection;

    /** Nominal mean spacecraft states (one per satellite). */
    private SpacecraftState[] nominalMeanSpacecraftStates;

    /** Previous nominal mean spacecraft states (one per satellite). */
    private SpacecraftState[] previousNominalMeanSpacecraftStates;

    /** Inverse of the orbital part of the state transition matrix (one per satellite). */
    private RealMatrix[] phiS;

    /** Propagation parameters part of the state transition matrix (one per satellite). */
    private RealMatrix[] psiS;

    /** Kalman process model constructor (package private).
     * @param propagatorBuilders propagator builders used to evaluate the orbits.
     * @param covarianceMatrixProviders providers for covariance matrices
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     */
    SemiAnalyticalKalmanModel(final List<DSSTPropagatorBuilder> propagatorBuilders,
                              final List<CovarianceMatrixProvider> covarianceMatrixProviders,
                              final ParameterDriversList estimatedMeasurementParameters,
                              final CovarianceMatrixProvider measurementProcessNoiseMatrix) {
        super(propagatorBuilders, covarianceMatrixProviders, estimatedMeasurementParameters, measurementProcessNoiseMatrix);

        // Build the reference propagators and add their partial derivatives equations implementation
        updateReferenceTrajectories(getEstimatedPropagators());

        // Initialize nominal states arrays
        final int nSat = propagatorBuilders.size();
        this.nominalMeanSpacecraftStates = new SpacecraftState[nSat];
        this.previousNominalMeanSpacecraftStates = new SpacecraftState[nSat];
        for (int k = 0; k < nSat; k++) {
            this.nominalMeanSpacecraftStates[k] = dsstPropagators[k].getInitialState();
            this.previousNominalMeanSpacecraftStates[k] = nominalMeanSpacecraftStates[k];
        }

        // Initialize "field" short periodic terms for all satellites
        for (int k = 0; k < nSat; k++) {
            harvesters[k].initializeFieldShortPeriodTerms(nominalMeanSpacecraftStates[k]);
        }

        // Initialize the estimated normalized mean element filter correction
        final int columns = getScale().length;
        this.predictedFilterCorrection = MatrixUtils.createRealVector(columns);
        this.correctedFilterCorrection = predictedFilterCorrection;

        // Initialize phiS and psiS for each satellite
        final int nSatellites = getBuilders().size();
        phiS = new RealMatrix[nSatellites];
        psiS = new RealMatrix[nSatellites];
        for (int k = 0; k < nSatellites; k++) {
            phiS[k] = MatrixUtils.createRealIdentityMatrix(getNumberSelectedOrbitalDriversValuesToEstimate(k));
            final int nbProp = getNumberSelectedPropagationDriversValuesToEstimate(k);
            if (nbProp != 0) {
                psiS[k] = MatrixUtils.createRealMatrix(getNumberSelectedOrbitalDriversValuesToEstimate(k), nbProp);
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public KalmanObserver getObserver() {
        return observer;
    }

    /** Set the observer.
     * @param observer the observer
     */
    public void setObserver(final KalmanObserver observer) {
        this.observer = observer;
    }

    /** Process measurements.
     * <p>
     * Update the filter with the new measurements.
     * </p>
     * @param observedMeasurements the list of measurements to process
     * @param filter Extended Kalman Filter
     * @return estimated propagators
     */
    public DSSTPropagator[] processMeasurements(final List<ObservedMeasurement<?>> observedMeasurements,
                                                final ExtendedKalmanFilter<MeasurementDecorator> filter) {
        try {

            // Sort the measurements
            observedMeasurements.sort(new ChronologicalComparator());
            final AbsoluteDate tStart             = observedMeasurements.getFirst().getDate();
            final AbsoluteDate tEnd               = observedMeasurements.getLast().getDate();
            final double       overshootTimeRange = FastMath.nextAfter(tEnd.durationFrom(tStart),
                                                    Double.POSITIVE_INFINITY);

            // Initialize step handler and set it to the first propagator
            // The handler will manage all propagators internally
            final SemiAnalyticalMeasurementHandler stepHandler = new SemiAnalyticalMeasurementHandler(this, filter, observedMeasurements, getReferenceDate());
            dsstPropagators[0].getMultiplexer().add(stepHandler);
            dsstPropagators[0].propagate(tStart, tStart.shiftedBy(overshootTimeRange));

            // Return the last estimated propagators
            return getEstimatedPropagators();

        } catch (MathRuntimeException mrte) {
            throw new OrekitException(mrte);
        }
    }

    /** Get the propagators estimated with the values set in the propagator builders.
     * @return propagators based on the current values in the builders
     */
    public DSSTPropagator[] getEstimatedPropagators() {
        return super.getEstimatedPropagators();
    }

    /** {@inheritDoc} */
    @Override
    public NonLinearEvolution getEvolution(final double previousTime, final RealVector previousState,
                                           final MeasurementDecorator measurement) {

        // Set a reference date for all measurements parameters that lack one
        final ObservedMeasurement<?> observedMeasurement = measurement.getObservedMeasurement();
        for (final ParameterDriver driver : observedMeasurement.getParametersDrivers()) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(getReferenceDate());
            }
        }

        // Increment measurement number
        incrementCurrentMeasurementNumber();

        // Update the current date
        setCurrentDate(measurement.getObservedMeasurement().getDate());

        // Normalized state transition matrix
        final RealMatrix stm = getErrorStateTransitionMatrix();

        // Predict filter correction
        predictedFilterCorrection = predictFilterCorrection(stm);

        // Determine which satellite this measurement is for
        final int satelliteIndex = observedMeasurement.getSatellites().getFirst().getPropagatorIndex();

        // Short period term derivatives for the relevant satellite
        analyticalDerivativeComputations(satelliteIndex, nominalMeanSpacecraftStates[satelliteIndex]);

        // Calculate the predicted osculating elements for the relevant satellite
        final double[] osculating = computeOsculatingElements(satelliteIndex, predictedFilterCorrection);
        final Orbit osculatingOrbit = OrbitType.EQUINOCTIAL.mapArrayToOrbit(osculating, null,
                                                                            getBuilders().get(satelliteIndex).getPositionAngleType(),
                                                                            getCurrentDate(),
                                                                            nominalMeanSpacecraftStates[satelliteIndex].getOrbit().getMu(),
                                                                            nominalMeanSpacecraftStates[satelliteIndex].getFrame());

        // Compute the predicted measurements
        predictedMeasurement = observedMeasurement.estimate(getCurrentMeasurementNumber(),
                                                            getCurrentMeasurementNumber(),
                                                            new SpacecraftState[] {
                                                                new SpacecraftState(osculatingOrbit,
                                                                                    nominalMeanSpacecraftStates[satelliteIndex].getAttitude(),
                                                                                    nominalMeanSpacecraftStates[satelliteIndex].getMass(),
                                                                                    nominalMeanSpacecraftStates[satelliteIndex].getAdditionalDataValues(),
                                                                                    nominalMeanSpacecraftStates[satelliteIndex].getAdditionalStatesDerivatives())
                                                            });

        // Normalized measurement matrix
        final RealMatrix measurementMatrix = getMeasurementMatrix();

        // Compute process noise matrix
        final RealMatrix normalizedProcessNoise = getNormalizedProcessNoise(previousState.getDimension());

        // Return
        return new NonLinearEvolution(measurement.getTime(), predictedFilterCorrection, stm,
                                      normalizedProcessNoise, measurementMatrix);
    }

    /** {@inheritDoc} */
    @Override
    public RealVector getInnovation(final MeasurementDecorator measurement, final NonLinearEvolution evolution,
                                    final RealMatrix innovationCovarianceMatrix) {

        // Apply the dynamic outlier filter, if it exists
        KalmanEstimatorUtil.applyDynamicOutlierFilter(predictedMeasurement, innovationCovarianceMatrix);
        // Compute the innovation vector
        return KalmanEstimatorUtil.computeInnovationVector(predictedMeasurement, predictedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation());
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeEstimation(final ObservedMeasurement<?> observedMeasurement,
                                   final ProcessEstimate estimate) {
        // Update the process estimate
        setCorrectedEstimate(estimate);
        // Corrected filter correction
        correctedFilterCorrection = estimate.getState();

        // Determine which satellite this measurement is for
        final int satelliteIndex = observedMeasurement.getSatellites().getFirst().getPropagatorIndex();

        // Update the previous nominal mean spacecraft state
        previousNominalMeanSpacecraftStates[satelliteIndex] = nominalMeanSpacecraftStates[satelliteIndex];

        // Calculate the corrected osculating elements
        final double[] osculating = computeOsculatingElements(satelliteIndex, correctedFilterCorrection);
        final Orbit osculatingOrbit = OrbitType.EQUINOCTIAL.mapArrayToOrbit(osculating, null,
                                                                            getBuilders().get(satelliteIndex).getPositionAngleType(),
                                                                            getCurrentDate(),
                                                                            nominalMeanSpacecraftStates[satelliteIndex].getOrbit().getMu(),
                                                                            nominalMeanSpacecraftStates[satelliteIndex].getFrame());

        // Compute the corrected measurements
        correctedMeasurement = observedMeasurement.estimate(getCurrentMeasurementNumber(),
                                                            getCurrentMeasurementNumber(),
                                                            new SpacecraftState[] {
                                                                new SpacecraftState(osculatingOrbit,
                                                                                    nominalMeanSpacecraftStates[satelliteIndex].getAttitude(),
                                                                                    nominalMeanSpacecraftStates[satelliteIndex].getMass(),
                                                                                    nominalMeanSpacecraftStates[satelliteIndex].getAdditionalDataValues(),
                                                                                    nominalMeanSpacecraftStates[satelliteIndex].getAdditionalStatesDerivatives())
                                                            });
        // Call the observer if the user added one
        if (observer != null) {
            observer.evaluationPerformed(this);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeOperationsObservationGrid() {
        // Update parameters
        updateParameters();
    }

    /** {@inheritDoc} */
    @Override
    public void updateNominalSpacecraftState(final SpacecraftState nominal) {
        // For backward compatibility, update the first satellite
        updateNominalSpacecraftState(nominal, 0);
    }

    /** Update the nominal spacecraft state for a specific satellite.
     * @param nominal nominal spacecraft state
     * @param satelliteIndex index of the satellite
     */
    public void updateNominalSpacecraftState(final SpacecraftState nominal, final int satelliteIndex) {
        this.nominalMeanSpacecraftStates[satelliteIndex] = nominal;
        // Update the builder with the nominal mean elements orbit
        getBuilders().get(satelliteIndex).resetOrbit(nominal.getOrbit(), PropagationType.MEAN);

        // Additionally, update the builder with the predicted mass value.
        getBuilders().get(satelliteIndex).setMass(nominal.getMass());
    }

    /** Update the reference trajectories using the propagators as input.
     * @param propagators The new propagators to use
     */
    public void updateReferenceTrajectories(final DSSTPropagator[] propagators) {

        dsstPropagators = propagators;

        // Equation name
        final String equationName = SemiAnalyticalKalmanEstimator.class.getName() + "-derivatives-";

        // Harvesters
        harvesters = new DSSTHarvester[propagators.length];

        for (int k = 0; k < propagators.length; k++) {
            // Mean state
            final SpacecraftState meanState = propagators[k].initialIsOsculating() ?
                           DSSTPropagator.computeMeanState(propagators[k].getInitialState(), propagators[k].getAttitudeProvider(), propagators[k].getAllForceModels()) :
                           propagators[k].getInitialState();

            // Update the jacobian harvester
            propagators[k].setInitialState(meanState, PropagationType.MEAN);
            harvesters[k] = propagators[k].setupMatricesComputation(equationName + k, null, null);
        }

    }

    /** {@inheritDoc} */
    @Override
    public void updateShortPeriods(final SpacecraftState state) {
        // For backward compatibility, update the first satellite
        updateShortPeriods(state, 0);
    }

    /** Update the DSST short periodic terms for a specific satellite.
     * @param state current mean state
     * @param satelliteIndex index of the satellite
     */
    public void updateShortPeriods(final SpacecraftState state, final int satelliteIndex) {
        // Loop on DSST force models
        for (final DSSTForceModel model : getBuilders().get(satelliteIndex).getAllForceModels()) {
            model.updateShortPeriodTerms(model.getParametersAllValues(), state);
        }
        harvesters[satelliteIndex].updateFieldShortPeriodTerms(state);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeShortPeriodicTerms(final SpacecraftState meanState) {
        // For backward compatibility, initialize the first satellite
        initializeShortPeriodicTerms(meanState, 0);
    }

    /** Initialize the short periodic terms for a specific satellite.
     * @param meanState mean state for auxiliary elements
     * @param satelliteIndex index of the satellite
     */
    public void initializeShortPeriodicTerms(final SpacecraftState meanState, final int satelliteIndex) {
        final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<>();
        // initialize ForceModels in OSCULATING mode even if propagation is MEAN
        final PropagationType type = PropagationType.OSCULATING;
        for (final DSSTForceModel force : getBuilders().get(satelliteIndex).getAllForceModels()) {
            shortPeriodTerms.addAll(force.initializeShortPeriodTerms(new AuxiliaryElements(meanState.getOrbit(), 1), type, force.getParameters(meanState.getDate())));
        }
        dsstPropagators[satelliteIndex].setShortPeriodTerms(shortPeriodTerms);
        // also need to initialize the Field terms in the same mode
        harvesters[satelliteIndex].initializeFieldShortPeriodTerms(meanState, type);
    }

    /** Get the normalized state transition matrix (STM) from previous point to current point.
     * The STM contains the partial derivatives of current state with respect to previous state.
     * The STM is an mxm matrix where m is the size of the state vector.
     * m = nbOrb + nbPropag + nbMeas
     * @return the normalized error state transition matrix
     */
    private RealMatrix getErrorStateTransitionMatrix() {

        /* The state transition matrix is obtained as follows, with:
         *  - Phi(k, k-1) : Transitional orbital matrix
         *  - Psi(k, k-1) : Transitional propagation parameters matrix
         *
         *       |             |             |   .    |
         *       | Phi(k, k-1) | Psi(k, k-1) | ..0..  |
         *       |             |             |   .    |
         *       |-------------|-------------|--------|
         *       |      .      |    1 0 0    |   .    |
         * STM = |    ..0..    |    0 1 0    | ..0..  |
         *       |      .      |    0 0 1    |   .    |
         *       |-------------|-------------|--------|
         *       |      .      |      .      | 1 0 0..|
         *       |    ..0..    |    ..0..    | 0 1 0..|
         *       |      .      |      .      | 0 0 1..|
         */

        // Initialize to the proper size identity matrix
        final RealMatrix stm = MatrixUtils.createRealIdentityMatrix(getCorrectedEstimate().getState().getDimension());

        final int[][] covarianceIndirection = getCovarianceIndirection();
        final double[] scale = getScale();
        final int nSat = getBuilders().size();

        // Loop over all satellites
        for (int k = 0; k < nSat; ++k) {
            final int nbOrb = getNumberSelectedOrbitalDriversValuesToEstimate(k);
            if (nbOrb == 0) {
                continue;
            }

            // Derivatives of the state vector with respect to initial state vector
            final RealMatrix dYdY0 = harvesters[k].getB2(nominalMeanSpacecraftStates[k]);

            // Calculate transitional orbital matrix
            final RealMatrix phi = dYdY0.multiply(phiS[k]);

            // Fill the state transition matrix with the orbital drivers
            final int[] indK = covarianceIndirection[k];
            final ParameterDriversList orbitalDrivers = getEstimatedOrbitalParametersArray()[k];
            int stmRow = 0;
            for (int i = 0; i < nbOrb; ++i) {
                int stmCol = 0;
                for (int j = 0; j < nbOrb; ++j) {
                    stm.setEntry(indK[stmRow], indK[stmCol], phi.getEntry(i, j));
                    stmCol += 1;
                }
                stmRow += 1;
            }

            // Update PhiS
            phiS[k] = new QRDecomposition(dYdY0).getSolver().getInverse();

            // Derivatives of the state vector with respect to propagation parameters
            if (psiS[k] != null) {
                final int nbProp = getNumberSelectedPropagationDriversValuesToEstimate(k);
                final RealMatrix dYdPp = harvesters[k].getB3(nominalMeanSpacecraftStates[k]);

                // Calculate transitional parameters matrix
                final RealMatrix psi = dYdPp.subtract(phi.multiply(psiS[k]));

                // Fill 1st row, 2nd column (dY/dPp)
                for (int i = 0; i < nbOrb; ++i) {
                    for (int j = 0; j < nbProp; ++j) {
                        stm.setEntry(indK[i], indK[nbOrb + j], psi.getEntry(i, j));
                    }
                }

                // Update PsiS
                psiS[k] = dYdPp;
            }
        }

        // Normalization of the STM
        for (int i = 0; i < scale.length; i++) {
            for (int j = 0; j < scale.length; j++) {
                stm.setEntry(i, j, stm.getEntry(i, j) * scale[j] / scale[i]);
            }
        }

        // Return the error state transition matrix
        return stm;

    }

    /** Get the normalized measurement matrix H.
     * H contains the partial derivatives of the measurement with respect to the state.
     * H is an nxm matrix where n is the size of the measurement vector and m the size of the state vector.
     * @return the normalized measurement matrix H
     */
    private RealMatrix getMeasurementMatrix() {

        // Observed measurement characteristics
        final SpacecraftState[]      evaluationStates    = predictedMeasurement.getStates();
        final ObservedMeasurement<?> observedMeasurement = predictedMeasurement.getObservedMeasurement();
        final double[] sigma  = predictedMeasurement.getObservedMeasurement().getTheoreticalStandardDeviation();

        // Initialize measurement matrix H: nxm
        final RealMatrix measurementMatrix = MatrixUtils.
                createRealMatrix(observedMeasurement.getDimension(),
                                 getCorrectedEstimate().getState().getDimension());

        // Loop over all orbits involved in the measurement
        final int[] orbitsStartColumns = getOrbitsStartColumns();
        final ParameterDriversList[] estimatedPropagationParameters = getEstimatedPropagationParametersArray();
        for (int k = 0; k < evaluationStates.length; ++k) {
            final int p = observedMeasurement.getSatellites().get(k).getPropagatorIndex();

            // Predicted orbit
            final Orbit predictedOrbit = evaluationStates[k].getOrbit();

            // Measurement matrix's columns related to orbital and propagation parameters
            final int nbOrb  = getNumberSelectedOrbitalDriversValuesToEstimate(p);
            final int nbProp = getNumberSelectedPropagationDriversValuesToEstimate(p);
            final double[][] aCY = new double[nbOrb][nbOrb];
            predictedOrbit.getJacobianWrtParameters(getBuilders().get(p).getPositionAngleType(), aCY);
            final RealMatrix dCdY = new Array2DRowRealMatrix(aCY, false);

            // Jacobian of the measurement with respect to current Cartesian coordinates
            final RealMatrix dMdC = new Array2DRowRealMatrix(predictedMeasurement.getStateDerivatives(k), false);

            // Jacobian of the measurement with respect to current orbital state
            RealMatrix dMdY = dMdC.multiply(dCdY);

            // Compute factor dShortPeriod_dMeanState = I+B1 | B4
            final RealMatrix IpB1B4 = MatrixUtils.createRealMatrix(nbOrb, nbOrb + nbProp);

            // B1
            final RealMatrix B1 = harvesters[p].getB1();

            // I + B1
            final RealMatrix I = MatrixUtils.createRealIdentityMatrix(nbOrb);
            final RealMatrix IpB1 = I.add(B1);
            IpB1B4.setSubMatrix(IpB1.getData(), 0, 0);

            // If there are propagation parameters, B4 is not null
            if (psiS[p] != null) {
                final RealMatrix B4 = harvesters[p].getB4();
                IpB1B4.setSubMatrix(B4.getData(), 0, nbOrb);
            }

            dMdY = dMdY.multiply(IpB1B4);

            for (int i = 0; i < dMdY.getRowDimension(); i++) {
                for (int j = 0; j < nbOrb; j++) {
                    final int col = orbitsStartColumns[p] + j;
                    final double driverScale = getBuilders().get(p).getOrbitalParametersDrivers().getDrivers().get(j).getScale();
                    measurementMatrix.setEntry(i, col, dMdY.getEntry(i, j) / sigma[i] * driverScale);
                }

                int col = 0;
                for (int j = 0; j < nbProp; j++) {
                    final double driverScale = estimatedPropagationParameters[p].getDrivers().get(j).getScale();
                    for (Span<Double> span = estimatedPropagationParameters[p].getDrivers().get(j).getValueSpanMap().getFirstSpan();
                                       span != null; span = span.next()) {

                        final Integer propCol = getPropagationParameterColumns().get(estimatedPropagationParameters[p].getDrivers().get(j).getName());
                        if (propCol != null) {
                            measurementMatrix.setEntry(i, propCol + col,
                                                       dMdY.getEntry(i, nbOrb + col) / sigma[i] * driverScale);
                        }
                        col++;
                    }
                }
            }

            // Normalized measurement matrix's columns related to measurement parameters
            for (final ParameterDriver driver : observedMeasurement.getParametersDrivers()) {
                if (driver.isSelected()) {
                    for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                        // Derivatives of current measurement w/r to selected measurement parameter
                        final double[] aMPm = predictedMeasurement.getParameterDerivatives(driver, span.getStart());

                        // Check that the measurement parameter is managed by the filter
                        if (getMeasurementParameterColumns().get(span.getData()) != null) {
                            // Column of the driver in the measurement matrix
                            final int driverColumn = getMeasurementParameterColumns().get(span.getData());

                            // Fill the corresponding indexes of the measurement matrix
                            for (int i = 0; i < aMPm.length; ++i) {
                                measurementMatrix.setEntry(i, driverColumn, aMPm[i] / sigma[i] * driver.getScale());
                            }
                        }
                    }
                }
            }
        }

        return measurementMatrix;
    }

    /** Predict the filter correction for the new observation.
     * @param stm normalized state transition matrix
     * @return the predicted filter correction for the new observation
     */
    private RealVector predictFilterCorrection(final RealMatrix stm) {
        return stm.operate(correctedFilterCorrection);
    }

    /** Compute the predicted osculating elements for a specific satellite.
     * @param satelliteIndex index of the satellite
     * @param filterCorrection kalman filter correction
     * @return the predicted osculating elements
     */
    private double[] computeOsculatingElements(final int satelliteIndex, final RealVector filterCorrection) {

        // Number of estimated orbital parameters
        final int nbOrb = getNumberSelectedOrbitalDriversValuesToEstimate(satelliteIndex);

        // B1
        final RealMatrix B1 = harvesters[satelliteIndex].getB1();

        // Short periodic terms
        final double[] shortPeriodTerms = dsstPropagators[satelliteIndex].getShortPeriodTermsValue(nominalMeanSpacecraftStates[satelliteIndex]);

        // Physical filter correction
        final RealVector physicalFilterCorrection = MatrixUtils.createRealVector(nbOrb);
        final int orbitStartCol = getOrbitsStartColumns()[satelliteIndex];
        for (int index = 0; index < nbOrb; index++) {
            physicalFilterCorrection.setEntry(index, filterCorrection.getEntry(orbitStartCol + index) * getScale()[orbitStartCol + index]);
        }

        // B1 * physicalCorrection
        final RealVector B1Correction = B1.operate(physicalFilterCorrection);

        // Nominal mean elements
        final double[] nominalMeanElements = new double[nbOrb];
        OrbitType.EQUINOCTIAL.mapOrbitToArray(nominalMeanSpacecraftStates[satelliteIndex].getOrbit(),
                                              getBuilders().get(satelliteIndex).getPositionAngleType(),
                                              nominalMeanElements, null);

        final double[] osculatingElements = new double[nbOrb];
        for (int i = 0; i < nbOrb; i++) {
            osculatingElements[i] = nominalMeanElements[i] +
                                    physicalFilterCorrection.getEntry(i) +
                                    shortPeriodTerms[i] +
                                    B1Correction.getEntry(i);
        }

        return osculatingElements;

    }

    /** Analytical computation of derivatives.
     * @param satelliteIndex index of the satellite
     * @param state mean state used to calculate short period perturbations
     */
    private void analyticalDerivativeComputations(final int satelliteIndex, final SpacecraftState state) {
        harvesters[satelliteIndex].setReferenceState(state);
    }

}
