/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.util.List;

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.CholeskyDecomposition;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;


/**
 * Implementation of a Kalman filter to perform orbit determination.<p>
 * The algorithm is the same as in <i>[1] Vallado, "Fundamentals of Astrodynamics and Applications, Fourth Edition", ch.10.6.</i><br>
 * 
 * Algorithm 69 (see <i>[1]§10.6 p.791</i>) is for the simple linearized Kalman filter.
 * Algorithm 70 (see <i>[1]§10.6 p.793</i>) is for the Extended Kalman filter.
 * This class handles both filter types. The choice of algorithm is made via the enum {@link FilterType}.
 * The difference between the two filter types is that the "extended" filter re-initializes its reference
 * trajectory at every step with the estimated state. While the "simple" filter always keeps the same reference trajectory.<p>
 *
 * Throughout the class, the following notations are used:<ul>
 *  <li>M is the size of the state vector,
 *  <li>N is the size of the measurement being processed by the filter,<p>
 *  <li>The <i>est</i> suffix is for an estimated variable,<p>
 *  <li>The <i>pred</i> suffix is for a predicted variable.<p>
 * </ul>
 * All the variables here (states, covariances, measurement matrices...) are normalized
 * using a specific scale for each estimated parameters or standard deviation noise for each measurement components.<p>
 * 
 * The filter uses a {@link NumericalPropagatorBuilder} to initialize its reference trajectory {@link NumericalPropagator}.<p>
 * The estimated parameters are driven by {@link ParameterDriver} objects. They are of 3 different types:<ol>
 *   <li><b>Orbital parameters</b>: The position and velocity of the spacecraft, or, more generally, its orbit. These parameters are retrieved
 *          from the reference trajectory propagator builder when the filter is initialized.
 *   <li><b>Propagation parameters</b>: Some parameters modelling physical processes (SRP or drag coefficients etc...). They are also retrieved
 *          from the propagator builder during the initialization phase.
 *   <li><b>Measurements parameters</b>: Parameters related to measurements (station biases, positions etc...). They are passed down to 
 *          the filter in its constructor.
 * </ol>
 * The total number of estimated parameters is M, the size of the state vector.<p>
 * 
 * A {@link KalmanEstimator} object is built using the function <i>build()</i> of a {@link KalmanEstimatorBuilder}.<p>
 * Its constructor takes as argument:<ul>
 * <li> A {@link NumericalPropagatorBuilder} that describes the set of forces used to model the reference trajectory
 *      and also holds the orbital and propagation parameters,
 * <li> A {@link ParameterDriversList} containing the estimated measurements parameters,
 * <li> A {@link KalmanProcessModel} that handles the operation linked with the underlying physical process model,
 * <li> A {@link KalmanMeasurementModel} for the operations related to the measurements.
 * </ul>
 * The algorithm is as follows. Given:<ul>
 *   <li>A set of M estimated parameters driven by {@link ParameterDriver} objects,<p>
 *   <li>The estimated state vector Xest<sub>k-1</sub> at epoch t<sub>k-1</sub>, of size M,<p>
 *   <li>The estimated state error vector &delta;Xest<sub>k-1</sub> at epoch t<sub>k-1</sub>, of size M,<p>
 *   <li>The estimated covariance matrix Pest<sub>k-1</sub> at epoch t<sub>k-1</sub>, of size MxM,<p>
 *   <li>A physical propagation model called the reference trajectory,
 *   <li>An estimation of the error of this model with the process noise covariance matrix <b>Q</b>, of size MxM,  
 *   <li>A measurement z at epoch t<sub>k</sub>, of size N, and its noise matrix <b>R</b>, of size NxN.<p>
 * </ul>
 * The first phase of the filtering process is the <b>prediction phase</b>:<ol>
 *   <li>The reference trajectory model is called to propagate the spacecraft state and its derivativesto t<sub>k</sub>,<p>
 *   <li><b>Predicted State:</b> The predicted state Xpred<sub>k</sub> is computed from this spacecraft state,<p>
 *   <li>The derivatives are used to compute the error state transition matrix <b>&Phi;</b>.
 *         It is an MxM matrix that contains the derivatives of the state at t<sub>k</sub> with respect to the state at t<sub>k-1</sub>,<p>
 *   <li><b>Predicted State Error</b>: The error state prediction is given by:<ul>
 *      <li>&delta;Xpred<sub>k</sub> = <b>&Phi;</b>.&delta;Xest<sub>k-1</sub> for the simple filter,
 *      <li>&delta;Xpred<sub>k</sub> = <b>0</b><sub>M</sub> for the extended filter.
 *      </ul>
 *   <li><b>Predicted Covariance</b>: The predicted covariance matrix is then given by the relationship:<p> 
 *   <b>Ppred</b><sub>k</sub> = <b>&Phi;</b>.<b>Pest</b><sub>k-1</sub><b>&Phi;</b><sup>T</sup> + <b>Q</b>
 * </ol>
 * 
 * The second phase of the filtering process is the <b>correction phase</b>:<ol>
 *   <li>The measurement matrix <b>H</b><sub>k</sub> is computed. It holds the derivatives of the measurement
 *       with respect to the state,
 *   <li>The innovation vector Inno<sub>k</sub> is computed. It is the difference between the observed measurement and
 *       the predicted measurement. Note that, in classical Kalman filtering, the value of the innovations is given by:<p>
 *       Inno<sub>k</sub> = z - <b>H</b><sub>k</sub>.Xpred<sub>k</sub><p>
 *       In Orekit we use the function <i>estimate</i> of class {@link EstimatedMeasurement} to directly compute the predicted measurement.<p>
 *       Thus the measurement matrix is not needed for this step.
 *   <li>Kalman gain: The optimal gain from Kalman theory is:<p>
 *   <b>K</b><sub>k</sub> = <b>Ppred</b><sub>k</sub>.<b>H</b><sub>k</sub><sup>T</sup>.
 *   (<b>H</b><sub>k</sub>.<b>Ppred</b><sub>k</sub>.<b>H</b><sub>k</sub><sup>T</sup> + <b>R</b>)<sup>-1</sup>     
 *   <li><b>Estimated State Error:</b> The error state vector estimation is given by:<ul>
 *          <li>&delta;Xest<sub>k</sub> = &delta;Xpred<sub>k</sub> + <b>K</b><sub>k</sub>.
 *          (Inno<sub>k</sub> - <b>H</b><sub>k</sub>.&delta;Xpred<sub>k</sub>) for the simple type filter,
 *          <li>&delta;Xest<sub>k</sub> = <b>K</b><sub>k</sub>.Inno<sub>k</sub> for the extended Kalman filter,
 *          since &delta;Xpred<sub>k</sub> = <b>0</b><sub>M</sub>. 
 *   </ul>
 *   <li><b>Estimated Covariance</b>: The classical equation is:<p>
 *   <b>Pest</b><sub>k</sub> = (I<sub>M</sub> - <b>K</b><sub>k</sub>.<b>H</b><sub>k</sub>).<b>Ppred</b><sub>k</sub>
 *   But here we prefer to use the so called <i>Joseph algorithm</i> (see <i>[1]§10.6 eq. 10.34</i>) which is the same but has the
 *   advantage of numerically keeping the covariance matrix symmetric:<p>
 *   <b>Pest</b><sub>k</sub> = (I<sub>M</sub> - <b>K</b><sub>k</sub>.<b>H</b><sub>k</sub>).<b>Ppred</b><sub>k</sub>.(I<sub>M</sub> - <b>K</b><sub>k</sub>.<b>H</b><sub>k</sub>)<sup>T</sup>
 *    + <b>K</b><sub>k</sub>.<b>R</b>.<b>K</b><sub>k</sub><sup>T</sup>
 *   <li><b>Estimated State</b>: The estimated state vector is updated using:<p>
 *   Xest<sub>k</sub> = Xpred<sub>k</sub> + &delta;Xest<sub>k</sub>
 * </ul>
 * Finally, if the filter is extended, the reference trajectory is updated using the estimated
 * orbital parameters of the filter.
 * 
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @since 9.2
 */
public class KalmanEstimator {

    /** Filter type. */
    private final FilterType filterType;
    
    /** Builder for a numerical propagator. */
    private NumericalPropagatorBuilder propagatorBuilder;

    /** Kalman filter process model. */
    private final KalmanProcessModel processModel;
    
    /** Kalman filter measurement model. */
    private final KalmanMeasurementModel measurementModel;
    
    /** Current date of the filter. */
    private AbsoluteDate currentDate;
    
    /** Current estimated measurement. */
    private EstimatedMeasurement<?> estimatedMeasurement;

    /** Current number of the measurement being processed by the filter since it was created. */
    private int currentMeasurementNumber;

    /** Status of the current measurement being processed by the filter.
     *  True if the measurement was kept.
     *  False if it is rejected
     */
    private boolean currentMeasurementStatus;
    
    /** Current estimated state. */
    private RealVector estimatedState;

    /** Current estimated state error. */
    private RealVector estimatedStateError;

    /** Current estimated error covariance matrix. */
    private RealMatrix estimatedCovariance;

    /** Observer to retrieve current estimation info. */
    private KalmanObserver observer;

    /** Kalman filter estimator constructor (package private).
     * @param propagatorBuilder propagator builder used to evaluate the orbit.
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param initialCovariance initial covariance matrix
     * @param processNoiseMatrix process noise matrix
     * @param filterType Type of the Kalman filter (SIMPLE or EXTENDED)
     * @param covariancePrediction Method for the prediction of the covariance (LINEARIZED or UNSCENTED)
     * @throws OrekitException propagation exception.
     */
    KalmanEstimator(final NumericalPropagatorBuilder propagatorBuilder,
                    final ParameterDriversList estimatedMeasurementParameters,
                    final RealMatrix physicalInitialCovariance,
                    final RealMatrix physicalInitialProcessNoiseMatrix,
                    final FilterType filterType)
                                          throws OrekitException {

        this.propagatorBuilder                  = propagatorBuilder;
        this.filterType                         = filterType;
        this.estimatedMeasurement               = null;
        this.observer                           = null;
        this.currentDate                        = propagatorBuilder.getInitialOrbitDate();
        this.currentMeasurementNumber           = 0;
        this.currentMeasurementStatus           = false;

        // Build the process model and measurement model
        this.processModel = new KalmanProcessModel(propagatorBuilder,
                                                   getOrbitalParametersDrivers(true),
                                                   getPropagationParametersDrivers(true),
                                                   estimatedMeasurementParameters,
                                                   physicalInitialCovariance,
                                                   physicalInitialProcessNoiseMatrix);
        this.measurementModel = new KalmanMeasurementModel();

        // Initialize normalized estimated covariance matrix and process noise matrix
        this.estimatedCovariance = processModel.getInitialCovarianceMatrix();

        // Build the first estimated state from the drivers
        this.estimatedState = processModel.getInitialEstimatedState();

        // Initialize estimated state error to a nil vector
        this.estimatedStateError = MatrixUtils.createRealVector(new double[estimatedState.getDimension()]);
    }

    /** Set the observer.
     * @param observer the observer...
     */
    public void setObserver(final KalmanObserver observer) {
        this.observer = observer;
    }

    /** Get the process model.
     * @return the process model
     */
    public KalmanProcessModel getProcessModel() {
        return processModel;
    }
    
    /** Get the measurement model.
     * @return the measurement model
     */
    public KalmanMeasurementModel getMeasurementModel() {
        return measurementModel;
    }
    
    /** Get current Kalman filter date.
     * @return the date
     */
    public AbsoluteDate getCurrentDate() {
        return currentDate;
    }

    /** Get current Kalman filter measurement number.
     * @return the number of measurement that were processed by the filter so far
     */
    public int getCurrentMeasurementNumber() {
        return currentMeasurementNumber;
    }

    /** Get current measurement status.
     * @return measurement status (true if the measurement was processed, false if it was rejected)
     */
    public boolean getCurrentMeasurementStatus() {
        return currentMeasurementStatus;
    }

    /** Get the "physical" estimated state (ie. not normalized)
     * @return the "physical" estimated state
     */
    public RealVector getPhysicalEstimatedState() {
        return processModel.unNormalizeStateVector(estimatedState);
    }

    /** Get the "physical" estimated covariance matrix (ie. not normalized)
     * @return the "physical" estimated covariance matrix
     */
    public RealMatrix getPhysicalEstimatedCovarianceMatrix() {
        return processModel.unNormalizeCovarianceMatrix(estimatedCovariance);
    }

    /** Get the orbital parameters supported by this estimator.
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return orbital parameters supported by this estimator
     * @exception OrekitException if different parameters have the same name
     */
    ParameterDriversList getOrbitalParametersDrivers(final boolean estimatedOnly)
                    throws OrekitException {

        final ParameterDriversList estimated = new ParameterDriversList();
        for (final DelegatingDriver delegating : propagatorBuilder.getOrbitalParametersDrivers().getDrivers()) {
            if (delegating.isSelected() || !estimatedOnly) {
                for (final ParameterDriver driver : delegating.getRawDrivers()) {
                    estimated.add(driver);
                }
            }
        }
        return estimated;
    }

    /** Get the propagator parameters supported by this estimator.
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return propagator parameters supported by this estimator
     * @exception OrekitException if different parameters have the same name
     */
    ParameterDriversList getPropagationParametersDrivers(final boolean estimatedOnly)
                    throws OrekitException {

        final ParameterDriversList estimated = new ParameterDriversList();
        for (final DelegatingDriver delegating : propagatorBuilder.getPropagationParametersDrivers().getDrivers()) {
            if (delegating.isSelected() || !estimatedOnly) {
                for (final ParameterDriver driver : delegating.getRawDrivers()) {
                    estimated.add(driver);
                }
            }
        }
        return estimated;
    }
    

    public void processMeasurement(final ObservedMeasurement<?> observedMeasurement)
                    throws OrekitException {

        // Measurement date
        final AbsoluteDate measurementDate = observedMeasurement.getDate();

        // Update current date, measurement number and initialize status to false
        currentDate = measurementDate;
        currentMeasurementNumber++;
        currentMeasurementStatus = false;

        // Set a reference date for all measurements parameters that lack one (including the not estimated ones)
        for (final ParameterDriver driver : observedMeasurement.getParametersDrivers()) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(propagatorBuilder.getInitialOrbitDate());
            }
        }
        // Call the estimate method
        try {
            estimate(observedMeasurement);
        } catch (MathRuntimeException mrte) {
            throw new OrekitException(mrte);
        } catch (OrekitExceptionWrapper oew) {
            throw oew.getException();
        }
        //}
    }

    public void processMeasurements(final List<ObservedMeasurement<?>> observedMeasurements)
                    throws OrekitException {
        for (ObservedMeasurement<?> observedMeasurement : observedMeasurements) {
            processMeasurement(observedMeasurement);
        }
    }

    private void estimate(final ObservedMeasurement<?> observedMeasurement)
                    throws OrekitException {

        // Note:
        // - N = size of the current measurement
        //  Example:
        //   * 1 for Range, RangeRate and TurnAroundRange
        //   * 2 for Angular (Azimuth/Elevation or Right-ascension/Declination)
        //   * 6 for Position/Velocity
        // - M = size of the state vector. N = nbOrb + nbPropag + nbMeas

        // Initialize Jacobians computation on the reference trajectory propagator
        // Used for the error state transition matrix and the measurement matrix computations
        // The initial Jacobians (state and parameters) are re-initialized each time a measurement is processed
        processModel.initializeDerivatives();

        // Propagate the reference trajectory to measurement date
        final SpacecraftState predictedSpacecraftState = processModel.
                        propagateReferenceTrajectory(observedMeasurement.getDate());

        // Predict the state vector (Mx1)
        final RealVector predictedState = predictState(predictedSpacecraftState.getOrbit());

        // Get the error state transition matrix (MxM)
        final RealMatrix stateTransitionMatrix =
                        processModel.getErrorStateTransitionMatrix(predictedSpacecraftState,
                                                                   processModel.getReferenceTrajectoryPartialDerivatives());

        // Predict the state error (Mx1)
        final RealVector predictedStateError = predictStateError(stateTransitionMatrix);


        // Predict the error covariance matrix (MxM), using the linearized method
        final RealMatrix predictedCovariance = predictCovariance(stateTransitionMatrix,
                                                                 processModel.getProcessNoiseMatrix());


        // Predict the measurement based on predicted spacecraft state
        // Compute the innovations (ie. residuals of the predicted measurement)
        // ------------------------------------------------------------

        // Predicted measurement
        // Note: here the "iteration/evaluation" formalism from the batch LS method
        // is twisted to fit the need of the Kalman filter.
        // The number of "iterations" is actually the number of measurements processed by the filter
        // so far. We use this to be able to apply the OutlierFilter modifiers on the predicted measurement.
        final EstimatedMeasurement<?> predictedMeasurement =
                        observedMeasurement.estimate(currentMeasurementNumber,
                                                     currentMeasurementNumber,
                                                     new SpacecraftState[] {predictedSpacecraftState});

        // Normalized innovations of the measurement (Nx1)
        final RealVector innovations = measurementModel.getResiduals(predictedMeasurement);

        // Normalized measurement matrix (NxM)
        final RealMatrix measurementMatrix = processModel.getMeasurementMatrix(predictedSpacecraftState,
                                                                               predictedMeasurement);

        // Measurement noise matrix (NxN)
        final RealMatrix measurementNoiseMatrix = measurementModel.getMeasurementNoiseMatrix(observedMeasurement);

        // Innovation covariance matrix (NxN)
        final RealMatrix innovationCovarianceMatrix = getInnovationCovarianceMatrix(predictedCovariance,
                                                                                    measurementMatrix,
                                                                                    measurementNoiseMatrix);
        
        // Apply the dynamic outlier filter, if it exists
        measurementModel.applyDynamicOutlierFilter(predictedMeasurement,
                                                   innovationCovarianceMatrix);

        // Update the status of the predicted measurement
        currentMeasurementStatus = measurementModel.getMeasurementStatus(predictedMeasurement);

        // If the predicted measurement is rejected, the gain is not computed.
        // And the estimates are equal to the predictions
        if (!currentMeasurementStatus)  {
            estimatedStateError = predictedStateError;
            estimatedCovariance = predictedCovariance;
            estimatedState      = predictedState;

        } else {
            // The predicted measurement is not rejected, compute Kalman gain and
            // correct the predictions
            // --------------------------------------------------------------------
            // FIXME: How to handle several measurements with very close measurement dates ?
            //        Slip the prediction phase or replace it by shiftedBy ?
            //        How to set the time threshold to determine whether measurement are too close or not

            // Kalman gain (MxN)
            final RealMatrix kalmanGain = getKalmanGain(predictedCovariance,
                                                        measurementMatrix,
                                                        innovationCovarianceMatrix);


            // Update estimated state error, state and covariance matrix
            // ---------------------------------------------------------

            // Estimated state error (Mx1)
            estimatedStateError = estimateStateError(predictedStateError,
                                                     innovations,
                                                     kalmanGain,
                                                     measurementMatrix);
            // Estimate covariance matrix (MxM)
            estimatedCovariance = estimateCovariance(predictedCovariance,
                                                     kalmanGain,
                                                     measurementMatrix,
                                                     measurementNoiseMatrix);
            // Estimate state (Mx1)
            estimatedState = estimateState(predictedState);
        }

        // Update the parameters with the estimated state
        // The min/max values of the parameters are handled by the ParameterDriver implementation
        updateParameters();

        // Get the estimated propagator (mirroring parameter update in the builder)
        // and the estimated spacecraft state
        final NumericalPropagator estimatedPropagator = processModel.getEstimatedPropagator();
        final SpacecraftState estimatedSpacecraftState = estimatedPropagator.getInitialState();

        // Compute the estimated measurement using estimated spacecraft state
        estimatedMeasurement = observedMeasurement.estimate(currentMeasurementNumber,
                                                            currentMeasurementNumber,
                                                            new SpacecraftState[] {estimatedSpacecraftState});
        // Update the trajectory
        // (in the case of the EXTENDED filter type only)
        // ---------------------

        if (filterType.equals(FilterType.EXTENDED)) {
            processModel.updateReferenceTrajectory(estimatedPropagator);
        }

        // Call the observer
        // -----------------

        if (observer != null) {
            try {
                // Build current evaluation
                final KalmanEvaluation kalmanEvaluation =
                                new KalmanEvaluation(processModel.unNormalizeStateVector(predictedState),
                                                     processModel.unNormalizeStateVector(predictedStateError),
                                                     processModel.unNormalizeCovarianceMatrix(predictedCovariance),
                                                     processModel.unNormalizeStateVector(estimatedState),
                                                     processModel.unNormalizeStateVector(estimatedStateError),
                                                     processModel.unNormalizeCovarianceMatrix(estimatedCovariance));

                observer.evaluationPerformed(new Orbit[] {predictedSpacecraftState.getOrbit()},
                                             new Orbit[] {estimatedSpacecraftState.getOrbit()},
                                             processModel.getEstimatedOrbitalParameters(),
                                             processModel.getEstimatedPropagationParameters(),
                                             processModel.getEstimatedMeasurementsParameters(),
                                             predictedMeasurement,
                                             estimatedMeasurement,
                                             kalmanEvaluation);
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }
    }

    /** Get the predicted normalized state vector.
     * The predicted/propagated orbit is used to update the state vector
     * @param predictedOrbit the predicted orbit at measurement date
     * @return the normalized predicted state vector
     * @throws OrekitException if the propagator builder could not be reset
     */
    private RealVector predictState(final Orbit predictedOrbit) 
                    throws OrekitException {

        // First, update the builder with the predicted orbit
        // This updates the orbital drivers with the values of the predicted orbit
        propagatorBuilder.resetOrbit(predictedOrbit);
        
        // Predicted state is initialized to previous estimated state
        final RealVector predictedState = MatrixUtils.createRealVector(estimatedState.toArray());
        
        // The orbital parameters in the state vector are replaced with their predicted values
        // The propagation & measurement parameters are not changed by the prediction (ie. the propagation)
        if ( processModel.getNbOrbitalParameters() > 0 ) {
            // As the propagator builder was previously updated with the predicted orbit,
            // the selected orbital drivers are already up to date with the prediction
            
            // Orbital parameters counter
            int jOrb = 0;
            for (DelegatingDriver orbitalDriver : propagatorBuilder.getOrbitalParametersDrivers().getDrivers()) {
                if (orbitalDriver.isSelected()) {
                    predictedState.setEntry(jOrb++, orbitalDriver.getNormalizedValue());
                }
            }
        }
        // Return the predicted state
        return predictedState;
    }
    
    /** Get the predicted normalized error state vector.<p>
     * dXpred[k] = PHI[k/k-1].Xest[k-1]<p>
     * Where:<p>
     *   - dXPred[k] is the current normalized error state vector prediction
     *   - Xest[k-1] is the previous estimated state vector
     *   - PHI[k/k-1] is the error state transition matrix between current and previous state
     * Note: The extended filter does not need this step. Indeed the reference trajectory
     * is updated with the previous estimate. So the predicted state error is always 0.
     * @param stateTransitionMatrix
     * @return
     */
    private RealVector predictStateError(final RealMatrix stateTransitionMatrix) {

        // Initialized state error to a nil vector
        RealVector predictedStateError = new ArrayRealVector(processModel.getNbOrbitalParameters() +
                                                             processModel.getNbPropagationParameters() +
                                                             processModel.getNbMeasurementsParameters());

        // Apply the formula only if the filter is not extended
        if (!filterType.equals(FilterType.EXTENDED)) {
            predictedStateError = stateTransitionMatrix.operate(estimatedStateError);
        }
        return predictedStateError;
    }

    /** Get the normalized predicted covariance matrix (MxM).<p>
     * The predicted covariance Ppred is obtained with the equation:<p>
     * Ppred = PHI x Ppred x PHIt + Q<p>
     * Where:<p>
     *   - PHI is the normalized state transition matrix between predicted state and
     *         previous estimated state (MxM)<p>
     *   - Q is the normalized process noise matrix
     * @param stateTransitionMatrix the normalized state transition matrix
     * @param processNoiseMatrix the normalized process noise matrix
     * @return the normalized predicted covariance matrix
     */
    public RealMatrix predictCovariance(final RealMatrix stateTransitionMatrix,
                                        final RealMatrix processNoiseMatrix) {

        return stateTransitionMatrix.multiply(estimatedCovariance)
                                    .multiply(stateTransitionMatrix.transpose())
                                    .add(processNoiseMatrix);
    }

    /** Get the normalized estimated state vector error (Mx1).<p>
     *  The estimated state vector error dXest is given by the equation:<p>
     *  dXest = dXpred + K x (Inno - H.dXpred)<p>
     *  With:<p>
     *    - dXpred: the predicted state vector error (Mx1),<p>
     *    - K     : the Kalman gain (MxN),<p>
     *    - Inno  : the innovations vector (Nx1),<p>
     *    - H     : the measurement matrix (NXM).<p>
     * If the Kalman filter is extended the predicted state vector error is nil.
     * Thus the former equation is reduced to: <p>
     * dXest = K x Inno
     * @param predictedStateError the normalized predicted state vector error
     * @param innovations the innovations vector
     * @param kalmanGain the Kalman gain
     * @param measurementMatrix the measurement matrix
     * @return the normalized estimated state vector error
     */
    private RealVector estimateStateError(final RealVector predictedStateError,
                                          final RealVector innovations,
                                          final RealMatrix kalmanGain,
                                          final RealMatrix measurementMatrix) {

        // If the filter type is extended, the predicted state vector error is nil
        if (filterType.equals(FilterType.EXTENDED)) {
            return kalmanGain.operate(innovations);
        } else {
            return predictedStateError
                            .add(kalmanGain.operate(innovations.subtract(measurementMatrix
                                                                        .operate(predictedStateError))));
        }
    }

    /** Get the normalized estimated covariance matrix (MxM).<p>
     * The estimated covariance matrix Pest is given by:<p>
     * Pest = Ppred x (I - K x H)<p>
     * Here we use the Joseph algorithm (see Vallado [1]§10.6 eq.10-34) which is
     * equivalent but guarantees that the output stays symmetric:<p>
     * Pest = (I - K x H) x Ppred x (I - K x H)t + K x R x Kt<p>
     * With:<p>
     *  - Ppred: the predicted covariance matrix (MxM),<p>
     *  - I    : the M sized identity matrix (MxM), <p>
     *  - K    : the Kalman gain (MxN) (and Kt its transpose),
     *  - H    : the measurement matrix (NxM),
     *  - R    : the measurement covariance (or noise) matrix (NxN).
     * @param predictedCovariance the normalized predicted covariance matrix
     * @param kalmanGain the normalized Kalman gain
     * @param measurementMatrix the normalized measurement matrix
     * @param measurementNoiseMatrix the normalized measurement noise matrix
     * @return the normalized estimated covariance matrix
     */
    private RealMatrix estimateCovariance(final RealMatrix predictedCovariance,
                                          final RealMatrix kalmanGain,
                                          final RealMatrix measurementMatrix,
                                          final RealMatrix measurementNoiseMatrix) {

        // Pre-compute M = (I - K x H)
        final RealMatrix M = MatrixUtils.createRealIdentityMatrix(predictedCovariance.getColumnDimension())
                        .subtract(kalmanGain.multiply(measurementMatrix));

        // Estimate covariance
        // Joseph algorithm: Pest = (I - K x H) x Ppred x (I - K x H)t + K x R x Kt
        // This algorithm guarantees the positive definiteness of the covariance
        // matrix throughout the filtering process.
        // It is equivalent to the classical computation: Pest = (I - K x H) x Ppred
        return M.multiply(predictedCovariance).multiply(M.transpose())
                        .add(kalmanGain.multiply(measurementNoiseMatrix).multiply(kalmanGain.transpose()));
    }

    /** Get the normalized estimated state vector (Mx1).
     * The estimated state vector is given by the equation:<p>
     * Xest = Xpred + dXest<p>
     * With:<p>
     *   - Xpred: the predicted state vector (Mx1),
     *   - dXest: the estimated state vector error (Mx1).
     * @param predictedState the normalized predicted state vector
     * @return the normalized estimated state vector
     */
    private RealVector estimateState(final RealVector predictedState) {

        return predictedState.add(estimatedStateError);
    }

    /** Get the normalized innovation covariance matrix (NxN).<p>
     * The innovation covariance matrix S is given by:<p>
     * S = H x Ppred x Ht + R<p>
     * With:<p>
     *   - H    : the measurement matrix (Ht its transpose) (NxM)<p>
     *   - Ppred: the predicted covariance matrix (MxM)<p>
     *   - R    : the measurement covariance matrix (or noise matrix) (NxN)
     * @param predictedCovariance The normalized predicted covariance matrix
     * @param measurementMatrix the normalized measurement matrix
     * @param measurementNoiseMatrix the normalized measurement noise matrix
     * @return the normalized innovation covariance matrix
     */
    private RealMatrix getInnovationCovarianceMatrix(final RealMatrix predictedCovariance,
                                                     final RealMatrix measurementMatrix,
                                                     final RealMatrix measurementNoiseMatrix) {
        return measurementMatrix.multiply(predictedCovariance)
                                .multiply(measurementMatrix.transpose())
                                .add(measurementNoiseMatrix);
    }
    
    /** Get the normalized Kalman filter gain K (MxN).<p>
     * The optimal Kalman gain K is given by the equation<p>
     * K =  Ppred x Ht x (H x Ppred x Ht + R)^(-1)<p>
     * or K = Ppred x Ht x S^(-1)<p>
     * With:<p>
     *   - H    : the measurement matrix (Ht its transpose) (NxM),<p>
     *   - Ppred: the predicted covariance matrix (MxM),<p>
     *   - R    : the measurement covariance matrix (or noise matrix) (NxN),<p>
     *   - S    : the innovation covariance matrix (NxN).
     * @param predictedCovariance the normalized predicted covariance matrix
     * @param measurementMatrix the normalized measurement matrix 
     * @param innovationCovarianceMatrix the normalized innovation covariance matrix
     * @return the normalized gain matrix
     */
    private RealMatrix getKalmanGain(final RealMatrix predictedCovariance,
                                     final RealMatrix measurementMatrix,
                                     final RealMatrix innovationCovarianceMatrix) {
        // Compute gain matrix:
        // K = Ppred x Ht x (H x Ppred x Ht + R)^-1 (1)
        // K: M x N
        // Let S = Ht x Ppred x H + R be the innovation covariance matrix
        // Instead of calculating the inverse of S we can rearrange the formula,
        // and then solve the linear equation A x X = B with A = St, X = Kt and B = H x Ppredt
        // Indeed:
        // Multiplying (1) on the right by S gives: K x S = Ppred x Ht
        // Taking the transpose gives: St x Kt = H x Ppredt
        final RealMatrix K = new QRDecomposition(innovationCovarianceMatrix.transpose()).getSolver()
                        .solve(measurementMatrix.multiply(predictedCovariance.transpose()))
                        .transpose();
        return K;
    }
    
    // FIXME: Which matrix decomposition is best to use for the gain ? QR or Cholesky.
    // Knowing that for Cholesky, the matrix has to be symmetrized before (due to numerical discrepancies) 
    /** Get the normalized Kalman filter gain K (MxN) using the Cholesky decomposition.<p>
     * The optimal Kalman gain K is given by the equation<p>
     * K =  Ppred x Ht x (H x Ppred x Ht + R)^(-1)
     * or K = Ppred x Ht x S^(-1)
     * With:<p>
     *   - H    : the measurement matrix (Ht its transpose) (NxM)<p>
     *   - Ppred: the predicted covariance matrix (MxM)<p>
     *   - R    : the measurement covariance matrix (or noise matrix) (NxN)
     *   - S    : the innovation covariance matrix (NxN)
     * @param predictedCovariance the normalized predicted covariance matrix
     * @param measurementMatrix the normalized measurement matrix 
     * @param innovationCovarianceMatrix the normalized innovation covariance matrix
     * @return the normalized gain matrix
     */
    private RealMatrix getKalmanGainCholesky(final RealMatrix predictedCovariance,
                                     final RealMatrix measurementMatrix,
                                     final RealMatrix innovationCovarianceMatrix) {
        // Compute gain matrix:
        // K = Ppred x Ht x (H x Ppred x Ht + R)^-1 (1)
        // K: M x N
        // Let S = Ht x Ppred x H + R be the innovation covariance matrix
        // Instead of calculating the inverse of S we can rearrange the formula,
        // and then solve the linear equation A x X = B with A = St, X = Kt and B = H x Ppredt
        // Indeed:
        // Multiplying (1) on the right by S gives: K x S = Ppred x Ht
        // Taking the transpose gives: St x Kt = H x Ppredt
        // The S matrix is first made symmetric to avoid any issue in the Cholesky decompostion 
        final RealMatrix K = new CholeskyDecomposition(symmetrizeMatrix(innovationCovarianceMatrix).transpose()).getSolver()
                        .solve(measurementMatrix.multiply(predictedCovariance.transpose()))
                        .transpose();
        return K;
    }
    
    /** Make a square matrix symmetric.
     * This is used for nearly symmetric matrix like the innovation covariance matrix.
     * This is supposed to be a symmetric matrix but small computation errors makes it
     * not perfectly symmetric.
     * For each element Mij of the matrix (i != j), the new value of Mij is the mean
     * value of Mij and Mji.
     * @param M the nearly symmetric matrix
     * @return the matrix made perfectly symmetric
     */
    private RealMatrix symmetrizeMatrix(final RealMatrix M) {
        for (int i = 0; i < M.getRowDimension(); i++) {
            for (int j = 0; j < i; j++) {
                final double Mij = 0.5 * (M.getEntry(i, j) + M.getEntry(j, i));
                M.setEntry(i, j, Mij);
                M.setEntry(j, i, Mij);
            }
        }
        return M;
    }
    
    private void updateParameters() throws OrekitException {
        int i = 0;
        for (final DelegatingDriver driver : processModel.getEstimatedOrbitalParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(estimatedState.getEntry(i));
            estimatedState.setEntry(i++, driver.getNormalizedValue());
        }
        for (final DelegatingDriver driver : processModel.getEstimatedPropagationParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(estimatedState.getEntry(i));
            estimatedState.setEntry(i++, driver.getNormalizedValue());
        }
        for (final DelegatingDriver driver : processModel.getEstimatedMeasurementsParameters().getDrivers()) {
            // let the parameter handle min/max clipping
            driver.setNormalizedValue(estimatedState.getEntry(i));
            estimatedState.setEntry(i++, driver.getNormalizedValue());
        }
    }

    public enum FilterType {
        SIMPLE,
        EXTENDED;
    }

    /** Class containing data from Kalman filter current evaluation.
     *  The matrices here are "physical" matrices (in opposition to the 
     *  normalized matrices handled by the filter).
     */
    public static class KalmanEvaluation {

        /** Multivariate function model. */
        //private final Model model;

        /** Current predicted state. */
        private final RealVector predictedState;

        /** Current predicted state error. */
        private final RealVector predictedStateError;

        /** Current predicted covariance matrix. */
        private final RealMatrix predictedCovariance;

        /** Current estimated state. */
        private final RealVector estimatedState;

        /** Current estimated state error. */
        private final RealVector estimatedStateError;

        /** Current estimated covariance matrix. */
        private final RealMatrix estimatedCovariance;

        /** Simple constructor.
         * @param predictedState State vector after the prediction phase of the filter
         * @param predictedStateError State vector error after the prediction phase of the filter
         * @param predictedCovariance Covariance matrix after the prediction phase of the filter
         * @param estimatedState State vector after the correction phase of the filter
         * @param estimatedStateError State vector error after the correction phase of the filter
         * @param estimatedCovariance Covariance matrix after the correction phase of the filter
         */
        KalmanEvaluation(final RealVector predictedState,
                         final RealVector predictedStateError,
                         final RealMatrix predictedCovariance,
                         final RealVector estimatedState,
                         final RealVector estimatedStateError,
                         final RealMatrix estimatedCovariance) {

            this.predictedState                  = predictedState;
            this.predictedStateError             = predictedStateError;
            this.predictedCovariance             = predictedCovariance;
            this.estimatedState                  = estimatedState;
            this.estimatedStateError             = estimatedStateError;
            this.estimatedCovariance             = estimatedCovariance;
        }

        /** Getter for the predictedState.
         * @return the predictedState
         */
        public RealVector getPredictedState() {
            return predictedState;
        }

        /** Getter for the predictedStateError.
         * @return the predictedStateError
         */
        public RealVector getPredictedStateError() {
            return predictedStateError;
        }

        /** Getter for the predictedCovariance.
         * @return the predictedCovariance
         */
        public RealMatrix getPredictedCovariance() {
            return predictedCovariance;
        }

        /** Getter for the estimatedState.
         * @return the estimatedState
         */
        public RealVector getEstimatedState() {
            return estimatedState;
        }

        /** Getter for the estimatedStateError.
         * @return the estimatedStateError
         */
        public RealVector getEstimatedStateError() {
            return estimatedStateError;
        }

        /** Getter for the estimatedCovariance.
         * @return the estimatedCovariance
         */
        public RealMatrix getEstimatedCovariance() {
            return estimatedCovariance;
        }
    }
}
