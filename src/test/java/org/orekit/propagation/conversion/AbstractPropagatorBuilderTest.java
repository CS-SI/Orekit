/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.propagation.conversion;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

public class AbstractPropagatorBuilderTest {

    /** Test method restOrbit. */
    @Test
    public void testResetOrbit() {
        // Load a context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        
        // Use a Cartesian orbit so the parameters are changed sufficiently when shifting the orbit of a minute
        final Orbit initialOrbit = new CartesianOrbit(context.initialOrbit);
        
        final AbstractPropagatorBuilder propagatorBuilder = new AbstractPropagatorBuilder(initialOrbit, PositionAngle.TRUE, 10., true) {
            
            @Override
            public Propagator buildPropagator(double[] normalizedParameters) {
                // Dummy function "buildPropagator", copied from KeplerianPropagatorBuilder
                setParameters(normalizedParameters);
                return new KeplerianPropagator(createInitialOrbit());
            }
        };
        
        // Shift the orbit of a minute
        // Reset the builder and check the orbits value
        final Orbit newOrbit = initialOrbit.shiftedBy(60.);
        propagatorBuilder.resetOrbit(newOrbit);
        
        // Check that the new orbit was properly set in the builder and 
        Assert.assertEquals(0., propagatorBuilder.getInitialOrbitDate().durationFrom(newOrbit.getDate()), 0.);
        final double[] stateVector = new double[6];
        propagatorBuilder.getOrbitType().mapOrbitToArray(newOrbit, PositionAngle.TRUE, stateVector, null);
        int i = 0;
        for (DelegatingDriver driver : propagatorBuilder.getOrbitalParametersDrivers().getDrivers()) {
            final double expectedValue = stateVector[i++];
            Assert.assertEquals(expectedValue, driver.getValue(), 0.);
            Assert.assertEquals(expectedValue, driver.getReferenceValue(), 0.);
        }
    }
}

