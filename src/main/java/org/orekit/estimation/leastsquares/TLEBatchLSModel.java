/* Copyright 2002-2020 CS GROUP
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
package org.orekit.estimation.leastsquares;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.Pair;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLEJacobiansMapper;
import org.orekit.propagation.analytical.tle.TLEPartialDerivativesEquations;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.ODPropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Bridge between {@link ObservedMeasurement measurements} and {@link
 * org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem
 * least squares problems}.
 * <p>
 * This class is an adaption of the {@link BatchLSModel} class
 * but for the {@link TLEPropagator TLE propagator}.
 * </p>
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @author Thomas Paulet
 * @since 11.0
 *
 */
public class TLEBatchLSModel extends AbstractBatchLSModel {

    /** Mappers for Jacobians. */
    private TLEJacobiansMapper[] mappers;

    /** Simple constructor.
     * @param propagatorBuilders builders to use for propagation
     * @param measurements measurements
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @param observer observer to be notified at model calls
     */
    public TLEBatchLSModel(final PropagatorBuilder[] propagatorBuilders,
                     final List<ObservedMeasurement<?>> measurements,
                     final ParameterDriversList estimatedMeasurementsParameters,
                     final ModelObserver observer) {
        super(propagatorBuilders, measurements,
              estimatedMeasurementsParameters,
              observer);
        this.mappers                         = new TLEJacobiansMapper[getBuilders().length];
    }

    /** {@inheritDoc} */
    @Override
    @DefaultDataContext
    public Pair<RealVector, RealMatrix> value(final RealVector point) {

        // Set up the propagators parallelizer
        final TLEPropagator[] propagators = createPropagators(point);
        final KeplerianOrbit[] orbits = new KeplerianOrbit[propagators.length];
        for (int i = 0; i < propagators.length; ++i) {
            mappers[i] = configureDerivatives(propagators[i]);
            orbits[i]  = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(propagators[i].getInitialState().getOrbit());
        }
        final PropagatorsParallelizer parallelizer =
                        new PropagatorsParallelizer(Arrays.asList(propagators), configureMeasurements(point));

        // Reset value and Jacobian
        final Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> evaluations = getEvaluations();
        final RealVector value = getValue();
        evaluations.clear();
        value.set(0.0);
        final RealMatrix jacobian = getJacobian();
        for (int i = 0; i < jacobian.getRowDimension(); ++i) {
            for (int j = 0; j < jacobian.getColumnDimension(); ++j) {
                jacobian.setEntry(i, j, 0.0);
            }
        }

        // Run the propagation, gathering residuals on the fly
        final AbsoluteDate firstDate = getFirstDate();
        final AbsoluteDate lastDate = getLastDate();
        if (isForwardPropagation()) {
            // Propagate forward from firstDate
            parallelizer.propagate(firstDate.shiftedBy(-1.0), lastDate.shiftedBy(+1.0));
        } else {
            // Propagate backward from lastDate
            parallelizer.propagate(lastDate.shiftedBy(+1.0), firstDate.shiftedBy(-1.0));
        }

        final ModelObserver observer = getObserver();
        observer.modelCalled(orbits, evaluations);

        return new Pair<RealVector, RealMatrix>(value, jacobian);

    }

    /** {@inheritDoc} */
    @Override
    public TLEPropagator[] createPropagators(final RealVector point) {

        final TLEPropagator[] propagators = new TLEPropagator[getBuilders().length];
        final int[] orbitsStartColumns = getOrbitsStartColumns();
        final Map<String, Integer> propagationParameterColumns = getPropagationParameterColumns();
        final ODPropagatorBuilder[] builders = (ODPropagatorBuilder[]) getBuilders();

        // Set up the propagators
        for (int i = 0; i < getBuilders().length; ++i) {

            // Get the number of selected orbital drivers in the builder
            final int nbOrb    = getOrbitsEndColumns()[i] - getOrbitsStartColumns()[i];

            // Get the list of selected propagation drivers in the builder and its size
            final ParameterDriversList selectedPropagationDrivers = getSelectedPropagationDriversForBuilder(i);
            final int nbParams = selectedPropagationDrivers.getNbParams();

            // Init the array of normalized parameters for the builder
            final double[] propagatorArray = new double[nbOrb + nbParams];

            // Add the orbital drivers normalized values
            for (int j = 0; j < nbOrb; ++j) {
                propagatorArray[j] = point.getEntry(orbitsStartColumns[i] + j);
            }

            // Add the propagation drivers normalized values
            for (int j = 0; j < nbParams; ++j) {
                propagatorArray[nbOrb + j] =
                                point.getEntry(propagationParameterColumns.get(selectedPropagationDrivers.getDrivers().get(j).getName()));
            }

            // Build the propagator
            propagators[i] = (TLEPropagator) builders[i].buildPropagator(propagatorArray);
        }

        return propagators;

    }

    /** Configure the propagator to compute derivatives.
     *
     *<p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param propagators {@link Propagator} to configure
     * @return mapper for this propagator
     */
    @DefaultDataContext
    protected TLEJacobiansMapper configureDerivatives(final AbstractPropagator propagators) {

        final String equationName = TLEBatchLSModel.class.getName() + "-derivatives";

        final TLEPartialDerivativesEquations partials = new TLEPartialDerivativesEquations(equationName, (TLEPropagator) propagators);

        // add the derivatives to the initial state
        final SpacecraftState rawState = propagators.getInitialState();
        final SpacecraftState stateWithDerivatives = partials.setInitialJacobians(rawState);
        ((TLEPropagator) propagators).setInitialState(stateWithDerivatives);

        return partials.getMapper();

    }

    /** {@inheritDoc} */
    @Override
    @DefaultDataContext
    public void fetchEvaluatedMeasurement(final int index, final EstimatedMeasurement<?> evaluation) {

        // States and observed measurement
        final SpacecraftState[]      evaluationStates    = evaluation.getStates();
        final ObservedMeasurement<?> observedMeasurement = evaluation.getObservedMeasurement();

        // compute weighted residuals
        final Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> evaluations = getEvaluations();
        evaluations.put(observedMeasurement, evaluation);
        if (evaluation.getStatus() == EstimatedMeasurement.Status.REJECTED) {
            return;
        }

        final double[] evaluated = evaluation.getEstimatedValue();
        final double[] observed  = observedMeasurement.getObservedValue();
        final double[] sigma     = observedMeasurement.getTheoreticalStandardDeviation();
        final double[] weight    = evaluation.getObservedMeasurement().getBaseWeight();
        final RealMatrix jacobian = getJacobian();
        final RealVector value = getValue();
        for (int i = 0; i < evaluated.length; ++i) {
            value.setEntry(index + i, weight[i] * (evaluated[i] - observed[i]) / sigma[i]);
        }

        for (int k = 0; k < evaluationStates.length; ++k) {

            final int p = observedMeasurement.getSatellites().get(k).getPropagatorIndex();
            final int[] orbitsStartColumns = getOrbitsStartColumns();
            final ODPropagatorBuilder[] builders = (ODPropagatorBuilder[]) getBuilders();
            final Map<String, Integer>  propagationParameterColumns = getPropagationParameterColumns();

            // partial derivatives of the current Cartesian coordinates with respect to current orbital state
            final double[][] aCY = new double[6][6];
            final Orbit currentOrbit = evaluationStates[k].getOrbit();
            currentOrbit.getJacobianWrtParameters(builders[p].getPositionAngle(), aCY);
            final RealMatrix dCdY = new Array2DRowRealMatrix(aCY, false);

            // Jacobian of the measurement with respect to current orbital state
            final RealMatrix dMdC = new Array2DRowRealMatrix(evaluation.getStateDerivatives(k), false);
            final RealMatrix dMdY = dMdC.multiply(dCdY);

            // compute state derivatives
            final double dt = mappers[p].getInitialState().getDate().durationFrom(evaluationStates[k].getDate());
            mappers[p].computeDerivatives(evaluationStates[k], dt);

            // Jacobian of the measurement with respect to initial orbital state
            final double[][] aYY0 = new double[6][6];
            mappers[p].getStateJacobian(evaluationStates[k], aYY0);
            final RealMatrix dYdY0 = new Array2DRowRealMatrix(aYY0, false);
            final RealMatrix dMdY0 = dMdY.multiply(dYdY0);
            for (int i = 0; i < dMdY0.getRowDimension(); ++i) {
                int jOrb = orbitsStartColumns[p];
                for (int j = 0; j < dMdY0.getColumnDimension(); ++j) {
                    final ParameterDriver driver = builders[p].getOrbitalParametersDrivers().getDrivers().get(j);
                    if (driver.isSelected()) {
                        jacobian.setEntry(index + i, jOrb++,
                                          weight[i] * dMdY0.getEntry(i, j) / sigma[i] * driver.getScale());
                    }
                }
            }

            // Jacobian of the measurement with respect to propagation parameters
            final ParameterDriversList selectedPropagationDrivers = getSelectedPropagationDriversForBuilder(p);
            final int nbParams = selectedPropagationDrivers.getNbParams();
            if ( nbParams > 0) {
                final double[][] aYPp  = new double[6][nbParams];
                mappers[p].getParametersJacobian(evaluationStates[k], aYPp);
                final RealMatrix dYdPp = new Array2DRowRealMatrix(aYPp, false);
                final RealMatrix dMdPp = dMdY.multiply(dYdPp);
                for (int i = 0; i < dMdPp.getRowDimension(); ++i) {
                    for (int j = 0; j < nbParams; ++j) {
                        final ParameterDriver delegating = selectedPropagationDrivers.getDrivers().get(j);
                        jacobian.addToEntry(index + i, propagationParameterColumns.get(delegating.getName()),
                                            weight[i] * dMdPp.getEntry(i, j) / sigma[i] * delegating.getScale());
                    }
                }
            }
        }
        final Map<String, Integer> measurementParameterColumns = getMeasurementParameterColumns();
        // Jacobian of the measurement with respect to measurements parameters
        for (final ParameterDriver driver : observedMeasurement.getParametersDrivers()) {
            if (driver.isSelected()) {
                final double[] aMPm = evaluation.getParameterDerivatives(driver);
                for (int i = 0; i < aMPm.length; ++i) {
                    jacobian.setEntry(index + i, measurementParameterColumns.get(driver.getName()),
                                      weight[i] * aMPm[i] / sigma[i] * driver.getScale());
                }
            }
        }

    }
}
