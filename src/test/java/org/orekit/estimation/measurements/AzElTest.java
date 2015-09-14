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

//import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
//import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
//import org.apache.commons.math3.random.RandomGenerator;
//import org.apache.commons.math3.random.Well19937a;
import org.junit.Test;
//import org.orekit.bodies.BodyShape;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
//import org.orekit.estimation.leastsquares.BatchLSEstimator;
//import org.orekit.frames.TopocentricFrame;
//import org.orekit.frames.Transform;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;

public class AzElTest {

    @Test
    public void testComputeAzEl() throws OrekitException {
        
        Context context = EstimationTestUtils.geoStationnaryContext();
        System.out.println("Geostationnary Context Created");
        
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);
        System.out.println("Geostationnary Orbit Created");
        
        // create Azimuth-Elevation measurements
        final List<Measurement> measurements =
                        EstimationTestUtils.createMeasurements(context, propagatorBuilder,
                                                               new AzElMeasurementCreator(context),
                                                               0.0, 1.0, 300.0);
        System.out.println("Azimuth and Elevation Measurement Created \n");
        System.out.println("The number of measurements is : " + measurements.size() + "\n");
        
        // Compute measurement to develop...
        for (int i = 0; i < measurements.size(); ++i) {
            System.out.println(i);
        }

    }

}

