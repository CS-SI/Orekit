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
package org.orekit.estimation.leastsquares;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.Incrementor;
import org.hipparchus.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.DSSTContext;
import org.orekit.estimation.DSSTEstimationTestUtils;
import org.orekit.estimation.DSSTForce;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DSSTBatchLSModelTest {

    @Test
    public void testPerfectValue() {

        final DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(true, 1.0e-6, 60.0, 0.001);
        final DSSTPropagatorBuilder[] builders = { propagatorBuilder };

        // create perfect PV measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                               propagatorBuilder);

        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 1.0, 300.0);
        final ParameterDriversList estimatedMeasurementsParameters = new ParameterDriversList();
        for (ObservedMeasurement<?> measurement : measurements) {
            for (final ParameterDriver driver : measurement.getParametersDrivers()) {
                if (driver.isSelected()) {
                    estimatedMeasurementsParameters.add(driver);
                }
            }
        }

        // create model
        final ModelObserver modelObserver = new ModelObserver() {
            /** {@inheritDoc} */
            @Override
            public void modelCalled(final Orbit[] newOrbits,
                                    final Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> newEvaluations) {
                Assertions.assertEquals(1, newOrbits.length);
                Assertions.assertEquals(0,
                                    context.initialOrbit.getDate().durationFrom(newOrbits[0].getDate()),
                                    1.0e-15);
                Assertions.assertEquals(0,
                                    Vector3D.distance(context.initialOrbit.getPosition(),
                                                      newOrbits[0].getPosition()),
                                    1.0e-15);
                Assertions.assertEquals(measurements.size(), newEvaluations.size());
            }
        };
        final DSSTBatchLSModel model = new DSSTBatchLSModel(builders, measurements, estimatedMeasurementsParameters, modelObserver, PropagationType.MEAN);
        model.setIterationsCounter(new Incrementor(100));
        model.setEvaluationsCounter(new Incrementor(100));

        // Test forward propagation flag to true
        Assertions.assertEquals(true, model.isForwardPropagation());

        // evaluate model on perfect start point
        final double[] normalizedProp = propagatorBuilder.getSelectedNormalizedParameters();
        final double[] normalized = new double[normalizedProp.length + estimatedMeasurementsParameters.getNbParams()];
        System.arraycopy(normalizedProp, 0, normalized, 0, normalizedProp.length);
        int i = normalizedProp.length;
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            normalized[i++] = driver.getNormalizedValue(new AbsoluteDate());
        }
        Pair<RealVector, RealMatrix> value = model.value(new ArrayRealVector(normalized));
        int index = 0;
        for (ObservedMeasurement<?> measurement : measurements) {
            for (int k = 0; k < measurement.getDimension(); ++k) {
                // the value is already a weighted residual
                Assertions.assertEquals(0.0, value.getFirst().getEntry(index++), 6.3e-8);
            }
        }
        Assertions.assertEquals(index, value.getFirst().getDimension());

    }

    @Test
    public void testBackwardPropagation() {

        final DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(true, 1.0e-6, 60.0, 0.001);
        final DSSTPropagatorBuilder[] builders = { propagatorBuilder };

        // create perfect PV measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, -1.0, 300.0);
        final ParameterDriversList estimatedMeasurementsParameters = new ParameterDriversList();
        for (ObservedMeasurement<?> measurement : measurements) {
            for (final ParameterDriver driver : measurement.getParametersDrivers()) {
                if (driver.isSelected()) {
                    estimatedMeasurementsParameters.add(driver);
                }
            }
        }

        // create model
        final ModelObserver modelObserver = new ModelObserver() {
            /** {@inheritDoc} */
            @Override
            public void modelCalled(final Orbit[] newOrbits,
                                    final Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> newEvaluations) {
                // Do nothing here
            }
        };
        final DSSTBatchLSModel model = new DSSTBatchLSModel(builders, measurements, estimatedMeasurementsParameters, modelObserver, PropagationType.MEAN);
        // Test forward propagation flag to false
        Assertions.assertEquals(false, model.isForwardPropagation());
    }

    @Test
    public void testIssue718() {

        // Context
        final DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Force models
        final DSSTForce zonal = DSSTForce.ZONAL;
        final List<DSSTForceModel> forces = new ArrayList<>();
        forces.add(zonal.getForceModel(context));

        // Create propagator builders
        final DSSTPropagatorBuilder propagatorBuilderMean =
                        context.createBuilder(true, 0.01, 600.0, 1.0);
        final DSSTPropagatorBuilder propagatorBuilderOsc  =
                        context.createBuilder(PropagationType.OSCULATING, PropagationType.OSCULATING, true, 0.01, 600.0, 1.0, zonal);

        // Propagators
        final Propagator propagatorMean = DSSTEstimationTestUtils.createPropagator(context.initialOrbit, propagatorBuilderMean);
        final Propagator propagatorOsc  = DSSTEstimationTestUtils.createPropagator(context.initialOrbit, propagatorBuilderOsc);

        // Measurements
        final List<ObservedMeasurement<?>> measurements = DSSTEstimationTestUtils.createMeasurements(propagatorMean, new PVMeasurementCreator(),
                                                                                                     0.0, 1.0, 300.0);
        // Empty list of measurement parameters
        final ParameterDriversList estimatedMeasurementsParameters = new ParameterDriversList();

        // Verify MEAN case
        final ModelObserver observerMean = new ModelObserver() {
            /** {@inheritDoc} */
            @Override
            public void modelCalled(final Orbit[] newOrbits,
                                    final Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> newEvaluations) {
                // Verify length
                Assertions.assertEquals(1, newOrbits.length);
                // Verify first orbit
                Assertions.assertEquals(0, context.initialOrbit.getDate().durationFrom(newOrbits[0].getDate()), 1.0e-15);
                Assertions.assertEquals(0, Vector3D.distance(context.initialOrbit.getPosition(),
                                                         newOrbits[0].getPosition()), 1.0e-15);

            }
        };

        final DSSTBatchLSModel modelMean = propagatorBuilderMean.buildLeastSquaresModel(new DSSTPropagatorBuilder[] {propagatorBuilderMean}, measurements, estimatedMeasurementsParameters, observerMean);
        modelMean.setIterationsCounter(new Incrementor(100));
        modelMean.setEvaluationsCounter(new Incrementor(100));

        // Evaluate model (MEAN)
        modelMean.value(new ArrayRealVector(propagatorBuilderMean.getSelectedNormalizedParameters()));

        // Verify OSCULATING case
        final ModelObserver observerOsc = new ModelObserver() {
            /** {@inheritDoc} */
            @Override
            public void modelCalled(final Orbit[] newOrbits,
                                    final Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> newEvaluations) {
                // Compute mean state from osculating propagator
                final SpacecraftState meanState = DSSTPropagator.computeMeanState(propagatorOsc.getInitialState(), propagatorOsc.getAttitudeProvider(), forces);
                // Verify length
                Assertions.assertEquals(1, newOrbits.length);
                // Verify first orbit
                Assertions.assertEquals(0, context.initialOrbit.getDate().durationFrom(newOrbits[0].getDate()), 1.0e-15);
                Assertions.assertEquals(0, Vector3D.distance(meanState.getPosition(),
                                                         newOrbits[0].getPosition()), 1.0e-15);

            }
        };

        final DSSTBatchLSModel modelOsc = propagatorBuilderOsc.buildLeastSquaresModel(new DSSTPropagatorBuilder[] {propagatorBuilderOsc}, measurements, estimatedMeasurementsParameters, observerOsc);
        modelOsc.setIterationsCounter(new Incrementor(100));
        modelOsc.setEvaluationsCounter(new Incrementor(100));

        // Evaluate model (OSCULATING)
        modelOsc.value(new ArrayRealVector(propagatorBuilderOsc.getSelectedNormalizedParameters()));

    }

}
