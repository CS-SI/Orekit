/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.estimation.leastsquares;

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
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.Measurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class ModelTest {

    @Test
    public void testPerfectValue() throws OrekitException {

        final Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);

        // create perfect PV measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<Measurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 1.0, 300.0);
        final ParameterDriversList measurementsParameters = new ParameterDriversList();
        for (Measurement<?> measurement : measurements) {
            for (final ParameterDriver parameter : measurement.getParametersDrivers()) {
                measurementsParameters.add(parameter);
            }
        }

        // create model
        final ModelObserver modelObserver = new ModelObserver() {
            /** {@inheritDoc} */
            @Override
            public void modelCalled(final Orbit newOrbit,
                                    final Map<Measurement<?>, Evaluation<?>> newEvaluations) {
                Assert.assertEquals(0,
                                    context.initialOrbit.getDate().durationFrom(newOrbit.getDate()),
                                    1.0e-15);
                Assert.assertEquals(0,
                                    Vector3D.distance(context.initialOrbit.getPVCoordinates().getPosition(),
                                                      newOrbit.getPVCoordinates().getPosition()),
                                    1.0e-15);
                Assert.assertEquals(measurements.size(), newEvaluations.size());
            }
        };
        final ParameterDriversList estimatedPropagatorParameters = new ParameterDriversList();
        for (final ParameterDriver parameterDriver : propagatorBuilder.getParametersDrivers().getDrivers()) {
            if (parameterDriver.isSelected()) {
                estimatedPropagatorParameters.add(parameterDriver);
            }
        }
        final Model model = new Model(propagatorBuilder, estimatedPropagatorParameters,
                                      measurements, measurementsParameters,
                                      context.initialOrbit.getDate(), modelObserver);
        model.setIterationsCounter(new Incrementor(100));
        model.setEvaluationsCounter(new Incrementor(100));

        // evaluate model on perfect start point
        RealVector point = startPoint(context, propagatorBuilder, measurementsParameters);
        Pair<RealVector, RealMatrix> value = model.value(point);
        int index = 0;
        for (Measurement<?> measurement : measurements) {
            for (int i = 0; i < measurement.getDimension(); ++i) {
                // the value is already a weighted residual
                Assert.assertEquals(0.0, value.getFirst().getEntry(index++), 1.4e-7);
            }
        }
        Assert.assertEquals(index, value.getFirst().getDimension());

    }

    private RealVector startPoint(final Context context,
                                  final PropagatorBuilder propagatorBuilder,
                                  final ParameterDriversList measurementsParameters)
        throws OrekitException {

        // allocate vector
        int dimension = 6;
        for (final ParameterDriver parameter : propagatorBuilder.getParametersDrivers().getDrivers()) {
            if (parameter.isSelected()) {
                ++dimension;
            }
        }
        for (final ParameterDriver parameter : measurementsParameters.getDrivers()) {
            if (parameter.isSelected()) {
                ++dimension;
            }
        }
        RealVector point = new ArrayRealVector(dimension);

        // orbit
        final double[] orb = new double[6];
        propagatorBuilder.getOrbitType().mapOrbitToArray(context.initialOrbit,
                                                         propagatorBuilder.getPositionAngle(),
                                                         orb);
        int index = 0;
        while (index < 6) {
            point.setEntry(index, orb[index]);
            ++index;
        }

        // propagator parameters
        for (final ParameterDriver propagatorParameter : propagatorBuilder.getParametersDrivers().getDrivers()) {
            if (propagatorParameter.isSelected()) {
                point.setEntry(index++, propagatorParameter.getValue());
            }
        }

        // measurements parameters
        for (final ParameterDriver parameter : measurementsParameters.getDrivers()) {
            if (parameter.isSelected()) {
                point.setEntry(index++, parameter.getValue());
            }
        }

        return point;

    }

}



