/* Copyright 2002-2021 CS GROUP
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

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.Incrementor;
import org.hipparchus.util.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.estimation.KeplerianContext;
import org.orekit.estimation.KeplerianEstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.KeplerianPropagatorBuilder;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;


public class KeplerianBatchLSModelTest {

    @Test
    public void testPerfectValue() {

        final KeplerianContext context = KeplerianEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final KeplerianPropagatorBuilder propagatorBuilder = context.createBuilder(0.001);
        final KeplerianPropagatorBuilder[] builders = { propagatorBuilder };

        // create perfect PV measurements
        final Propagator propagator = KeplerianEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                    propagatorBuilder);

        final List<ObservedMeasurement<?>> measurements = KeplerianEstimationTestUtils.createMeasurements(propagator,
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
                Orbit initialOrbit = context.initialOrbit;
                Assert.assertEquals(1, newOrbits.length);
                Assert.assertEquals(0,
                                    context.initialOrbit.getDate().durationFrom(newOrbits[0].getDate()),
                                    3.0e-8);
                Assert.assertEquals(0,
                                    Vector3D.distance(initialOrbit.getPVCoordinates().getPosition(),
                                                      newOrbits[0].getPVCoordinates().getPosition()),
                                    3.0e-8);
                Assert.assertEquals(measurements.size(), newEvaluations.size());
            }
        };
        final KeplerianBatchLSModel model = new KeplerianBatchLSModel(builders, measurements, estimatedMeasurementsParameters, modelObserver);
        model.setIterationsCounter(new Incrementor(100));
        model.setEvaluationsCounter(new Incrementor(100));
        
        // Test forward propagation flag to true
        assertEquals(true, model.isForwardPropagation());

        // evaluate model on perfect start point
        final double[] normalizedProp = propagatorBuilder.getSelectedNormalizedParameters();
        final double[] normalized = new double[normalizedProp.length + estimatedMeasurementsParameters.getNbParams()];
        System.arraycopy(normalizedProp, 0, normalized, 0, normalizedProp.length);
        int i = normalizedProp.length;
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            normalized[i++] = driver.getNormalizedValue();
        }
        Pair<RealVector, RealMatrix> value = model.value(new ArrayRealVector(normalized));
        int index = 0;
        for (ObservedMeasurement<?> measurement : measurements) {
            for (int k = 0; k < measurement.getDimension(); ++k) {
                // the value is already a weighted residual
                Assert.assertEquals(0.0, value.getFirst().getEntry(index++), 5.e-7);
            }
        }
        Assert.assertEquals(index, value.getFirst().getDimension());

    }
    
    @Test
    public void testBackwardPropagation() {

        final KeplerianContext context = KeplerianEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final KeplerianPropagatorBuilder propagatorBuilder = context.createBuilder(0.001);
        final KeplerianPropagatorBuilder[] builders = { propagatorBuilder };

        // create perfect PV measurements
        final Propagator propagator = KeplerianEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                    propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements = KeplerianEstimationTestUtils.createMeasurements(propagator,
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
        final KeplerianBatchLSModel model = new KeplerianBatchLSModel(builders, measurements, estimatedMeasurementsParameters, modelObserver);
        // Test forward propagation flag to false
        assertEquals(false, model.isForwardPropagation());
    }

}
