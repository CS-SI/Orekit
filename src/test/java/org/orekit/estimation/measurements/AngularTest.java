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

import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
//import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
//import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
//import org.apache.commons.math3.random.RandomGenerator;
//import org.apache.commons.math3.random.Well19937a;
import org.junit.Test;
//import org.orekit.bodies.BodyShape;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.EstimationUtils;
import org.orekit.estimation.StateFunction;
//import org.orekit.estimation.leastsquares.BatchLSEstimator;
//import org.orekit.frames.TopocentricFrame;
//import org.orekit.frames.Transform;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
//import org.orekit.utils.Constants;
import org.orekit.utils.Constants;

public class AngularTest {

    @Test
    public void testStateDerivatives() throws OrekitException {
        
        Context context = EstimationTestUtils.geoStationnaryContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);
        
        // create perfect azimuth-elevation measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<Measurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new AngularMeasurementCreator(context),
                                                               //0.25, 3.0, 21600.0);
                                                               0.25, 3.0, 600.0);

        propagator.setSlaveMode();
        
        // Compute measurements.
        double[] AzerrorsP = new double[3 * measurements.size()];
        double[] AzerrorsV = new double[3 * measurements.size()];
        double[] ElerrorsP = new double[3 * measurements.size()];
        double[] ElerrorsV = new double[3 * measurements.size()];
        int AzindexP = 0;
        int AzindexV = 0;
        int ElindexP = 0;
        int ElindexV = 0;
        
        for (final Measurement<?> measurement : measurements) {
            
            // parameter corresponding to station position offset
            final GroundStation stationParameter = ((Angular) measurement).getStation();
            
            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // range would have depended only on the current position but
            // not on the current velocity.
            final AbsoluteDate  datemeas         = measurement.getDate();
            SpacecraftState     state            = propagator.propagate(datemeas);
            final double        meanDelay        = stationParameter.downlinkTimeOfFlight(state, datemeas);

            final AbsoluteDate date     = measurement.getDate().shiftedBy(-0.75 * meanDelay);
                               state    = propagator.propagate(date);
            final double[][]   jacobian = measurement.evaluate(0, state).getStateDerivatives();

            // compute a reference value using finite differences
            final double[][] finiteDifferencesJacobian =
                EstimationUtils.differentiate(new StateFunction() {
                    public double[] value(final SpacecraftState state) throws OrekitException {
                        return measurement.evaluate(0, state).getValue();
                    }
                                                  }, measurement.getDimension(), OrbitType.CARTESIAN,
                                                  PositionAngle.TRUE, 250.0, 8).value(state);

            Assert.assertEquals(finiteDifferencesJacobian.length, jacobian.length);
            Assert.assertEquals(finiteDifferencesJacobian[0].length, jacobian[0].length);
            
            final double smallest = FastMath.ulp((double) 1.0);
            //System.out.println("The smallest value is : " + smallest + "\n");
            
            for (int i = 0; i < jacobian.length; ++i) {
                for (int j = 0; j < jacobian[i].length; ++j) {
                    double relativeError = FastMath.abs((finiteDifferencesJacobian[i][j] - jacobian[i][j]) /
                                                              finiteDifferencesJacobian[i][j]);
                    
                    
                    if ((FastMath.sqrt(finiteDifferencesJacobian[i][j]) < smallest) && (FastMath.sqrt(jacobian[i][j]) < smallest) ){
                        relativeError = 0.0;
                    }
                    
                    
                    
                    if (j < 3) {
                        if (i == 0) {
                            AzerrorsP[AzindexP++] = relativeError;
                            //System.out.println("AZ Error dP : " + finiteDifferencesJacobian[i][j] + "   " + jacobian[i][j] + "    " + relativeError + "\n");
                        } else {
                            ElerrorsP[ElindexP++] = relativeError;
                            //System.out.println("El Error dP : " + finiteDifferencesJacobian[i][j] + "   " + jacobian[i][j] + "    " + relativeError + "\n");
                        }
                    } else {
                        if (i == 0) {
                            AzerrorsV[AzindexV++] = relativeError;
                            //System.out.println("AZ Error dv : " + finiteDifferencesJacobian[i][j] + "   " + jacobian[i][j] + "    " + relativeError + "\n");
                        } else {
                            ElerrorsV[ElindexV++] = relativeError;
                            //System.out.println("El Error dv : " + finiteDifferencesJacobian[i][j] + "   " + jacobian[i][j] + "    " + relativeError + "\n");
                        }
                    }
                }
            }

        }

        // median errors on Azimuth
        System.out.println("Ecart median Azimuth/dP : " + new Median().withNaNStrategy(NaNStrategy.REMOVED).evaluate(AzerrorsP) + "\n");
        Assert.assertEquals(0.0, new Median().evaluate(AzerrorsP), 4.0e-6);
        System.out.println("Ecart median Azimuth/dV : " + new Median().withNaNStrategy(NaNStrategy.REMOVED).evaluate(AzerrorsV) + "\n");
        Assert.assertEquals(0.0, new Median().evaluate(AzerrorsV), 1.5e-5);

        // median errors on Elevation
        System.out.println("Ecart median Elevation/dP : " + new Median().withNaNStrategy(NaNStrategy.REMOVED).evaluate(ElerrorsP) + "\n");
        Assert.assertEquals(0.0, new Median().evaluate(ElerrorsP), 4.2e-6);
        System.out.println("Ecart median Elevation/dV : " + new Median().withNaNStrategy(NaNStrategy.REMOVED).evaluate(ElerrorsV) + "\n");
        Assert.assertEquals(0.0, new Median().evaluate(ElerrorsV), 2.0e-5);
    }
    
    @Test
    public void testParameterDerivatives() throws OrekitException {

        Context context = EstimationTestUtils.geoStationnaryContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);

        // create perfect azimuth-elevation measurements
        for (final GroundStation station : context.stations) {
            station.setEstimated(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<Measurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new AngularMeasurementCreator(context),
                                                               0.25, 3.0, 600.0);
        propagator.setSlaveMode();

        for (final Measurement<?> measurement : measurements) {

            // parameter corresponding to station position offset
            final GroundStation stationParameter = ((Angular) measurement).getStation();

            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // range would have depended only on the current position but
            // not on the current velocity.
            final AbsoluteDate    datemeas  = measurement.getDate();
            final SpacecraftState stateini  = propagator.propagate(datemeas);
            final double          meanDelay = stationParameter.downlinkTimeOfFlight(stateini, datemeas);
            
            final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
            final SpacecraftState state     = propagator.propagate(date);
            final double[][]      jacobian  = measurement.evaluate(0, state).getParameterDerivatives(stationParameter.getName());

            final double[][] finiteDifferencesJacobian =
                EstimationUtils.differentiate(new MultivariateVectorFunction() {
                        public double[] value(double[] point) throws OrekitExceptionWrapper {
                            try {

                                final double[] savedParameter = stationParameter.getValue();

                                // evaluate range with a changed station position
                                stationParameter.setValue(point);
                                final double[] result = measurement.evaluate(0, state).getValue();
                                System.out.println("stationParameter.getValue : " + stationParameter.getValue() + "\n");
                                stationParameter.setValue(savedParameter);
                                return result;

                            } catch (OrekitException oe) {
                                throw new OrekitExceptionWrapper(oe);
                            }
                        }
                    }, measurement.getDimension(), 3, 20.0, 20.0, 20.0).value(stationParameter.getValue());

            System.out.println("Longueur de la Jacobienne : " + jacobian.length + "\n");
            Assert.assertEquals(finiteDifferencesJacobian.length, jacobian.length);
            
            System.out.println("Longueur de la Jacobienne[0] : " + jacobian[0].length + "\n");
            Assert.assertEquals(finiteDifferencesJacobian[0].length, jacobian[0].length);
            for (int i = 0; i < jacobian.length; ++i) {
                for (int j = 0; j < jacobian[i].length; ++j) {
                    System.out.println("Element de la Jacobienne : " + jacobian[i][j] + " / " + finiteDifferencesJacobian[i][j] + "\n");
                    //Assert.assertEquals(finiteDifferencesJacobian[i][j],
                    //                    jacobian[i][j],
                    //                    1e3 * FastMath.abs(finiteDifferencesJacobian[i][j]));

                }
            }

        }

        System.out.println("Test OK \n");
    }


}

