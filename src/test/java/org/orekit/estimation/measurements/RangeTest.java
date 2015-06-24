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

import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.StateFunction;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public class RangeTest {

    @Test
    public void testStateDerivatives() throws OrekitException {

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
        propagator.setSlaveMode();

        for (final Measurement measurement : measurements) {

            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // range would have depend only on the current position but
            // not on the current velocity.
            final double meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
            final AbsoluteDate date = measurement.getDate().shiftedBy(-0.75 * meanDelay);
            final SpacecraftState state = propagator.propagate(date);

            final double[][] jacobian = measurement.evaluate(0, state).getStateDerivatives();

            final double[][] finiteDifferencesJacobian =
                    EstimationTestUtils.differentiate(new StateFunction() {
                public double[] value(final SpacecraftState state) throws OrekitException {
                    return measurement.evaluate(0, state).getValue();
                }
            }, 1, OrbitType.CARTESIAN, PositionAngle.TRUE, 10.0, 5).value(state);

            Assert.assertEquals(finiteDifferencesJacobian.length, jacobian.length);
            Assert.assertEquals(finiteDifferencesJacobian[0].length, jacobian[0].length);
            for (int i = 0; i < jacobian.length; ++i) {
                for (int j = 0; j < jacobian[i].length; ++j) {
                    System.out.println(i + " " + j + " " +
                            finiteDifferencesJacobian[i][j] + " " +
                            jacobian[i][j] + " " +
                            (finiteDifferencesJacobian[i][j] - jacobian[i][j]) + " " +
                            ((finiteDifferencesJacobian[i][j] - jacobian[i][j]) /
                                    FastMath.abs(finiteDifferencesJacobian[i][j])));
//                    Assert.assertEquals(finiteDifferencesJacobian[i][j],
//                                        jacobian[i][j],
//                                        1.0e-6 * FastMath.abs(finiteDifferencesJacobian[i][j]));
                }
            }

        }

    }

}


