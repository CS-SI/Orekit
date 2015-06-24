/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.estimation.measurements;

import java.util.List;

import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937a;
import org.junit.Test;
import org.orekit.bodies.BodyShape;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;

public class GroundStationTest {

    @Test
    public void testEstimateStationPosition() throws OrekitException {

            Context context = EstimationTestUtils.eccentricContext();

            final NumericalPropagatorBuilder propagatorBuilder =
                            context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                                  1.0e-6, 60.0, 0.001);

            // create perfect range measurements
            final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                               propagatorBuilder);
            final List<Measurement> measurements =
                            EstimationTestUtils.createMeasurements(propagator,
                                                                   new RangeMeasurementCreator(context),
                                                                   1.0, 3.0, 300.0);

        // move the stations
        final RandomGenerator random = new Well19937a(0x4adbecfc743bda60l);
        final String movedSuffix = "-moved";
        final Vector3D[] deltaTopo = new Vector3D[context.stations.size()];
        final GroundStation[] moved = new GroundStation[context.stations.size()];
        for (int i = 0; i < context.stations.size(); ++i) {
            final TopocentricFrame base = context.stations.get(i).getBaseFrame();
            final BodyShape parent = base.getParentShape();
            final Vector3D baseOrigin = parent.transform(base.getPoint());
            deltaTopo[i] = new Vector3D(2 * random.nextDouble() - 1,
                                        2 * random.nextDouble() - 1,
                                        2 * random.nextDouble() - 1);
            final Transform topoToParent = base.getTransformTo(parent.getBodyFrame(), null);
            final Vector3D deltaParent = topoToParent.transformVector(deltaTopo[i]);
            moved[i] = new GroundStation(new TopocentricFrame(parent,
                                                              parent.transform(baseOrigin.add(deltaParent),
                                                                               parent.getBodyFrame(),
                                                                               null),
                                                              base.getName() + movedSuffix));
        }

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(propagatorBuilder,
                                                                new LevenbergMarquardtOptimizer());
        for (final Measurement measurement : measurements) {
            final Range range = (Range) measurement;
            final String name = range.getStation().getBaseFrame().getName() + movedSuffix;
            for (final GroundStation s : moved) {
                if (s.getBaseFrame().getName().equals(name)) {
                    estimator.addMeasurement(new Range(s, range.getDate(),
                                                       range.getObservedValue()[0],
                                                       range.getTheoreticalStandardDeviation()[0],
                                                       range.getBaseWeight()[0]));
                }
            }
        }
        estimator.setConvergenceThreshold(1.0e-14, 1.0e-12);
        estimator.setMaxIterations(100);

        // we want to estimate station offsets
        for (int i = 0; i < moved.length; ++i) {
            moved[i].setEstimated(true);
        }

        EstimationTestUtils.checkFit(context, estimator, 37,
                                     5.2e-4, 1.0e-5,
                                     1.0e-3, 1.0e-4,
                                     44091,  2.0,
                                     8.7,    3.0e-2);
        for (int i = 0; i < moved.length; ++i) {
            System.out.println(deltaTopo[i].getX() + " " + moved[i].getValue()[0] +
                               " " + (deltaTopo[i].getX() - moved[i].getValue()[0]));
            System.out.println(deltaTopo[i].getY() + " " + moved[i].getValue()[1] +
                               " " + (deltaTopo[i].getY() - moved[i].getValue()[1]));
            System.out.println(deltaTopo[i].getZ() + " " + moved[i].getValue()[2] +
                               " " + (deltaTopo[i].getZ() - moved[i].getValue()[2]));
        }

    }

}


