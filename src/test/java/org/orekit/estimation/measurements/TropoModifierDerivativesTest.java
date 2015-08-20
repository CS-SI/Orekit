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
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.StateFunction;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;

public class TropoModifierDerivativesTest {

    @Test
    public void testModifierElevationStateDerivatives() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        for (final GroundStation station : context.stations) {
            station.setEstimated(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<Measurement> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();

        for (final Measurement measurement : measurements) {

            // parameter corresponding to station position offset
            final GroundStation stationParameter = ((Range) measurement).getStation();
            final AbsoluteDate date = ((Range) measurement).getDate();
            final SpacecraftState refstate     = propagator.propagate(date);

            final double[]      jacobian  = RangeTroposphericDelayModifier.Derivatives.derivElevationWrtState(date, stationParameter, refstate);

            final double[][] finiteDifferencesJacobian =
                            EstimationTestUtils.differentiate(new StateFunction() {
                                public double[] value(final SpacecraftState state) throws OrekitException {
                                    try {
                                        // evaluate target's elevation with a changed target position                                                                                       
                                        
                                        final Vector3D extPoint = state.getPVCoordinates().getPosition();
                                        final Frame frameSat = state.getFrame();
                                        
                                        final double[] result = new double[]{
                                                            stationParameter.getOffsetFrame().getElevation(extPoint, frameSat, date)
                                        };
                                        
                                        return result;

                                    } catch (OrekitException oe) {
                                        throw new OrekitExceptionWrapper(oe);
                                    }
                                }
                                                              }, measurement.getDimension(), OrbitType.CARTESIAN,
                                                              PositionAngle.TRUE, 15.0, 3).value(refstate);                            


            Assert.assertEquals(finiteDifferencesJacobian[0].length, jacobian.length);

            final double tolerance = 1e-10;
            for (int i = 0; i < jacobian.length; ++i) {
                    Assert.assertEquals(finiteDifferencesJacobian[0][i],
                                        jacobian[i],
                                        tolerance * FastMath.max(FastMath.abs(finiteDifferencesJacobian[0][i]), 1.0));
            }
        }
    }

    @Test
    public void testModifierElevationParameterDerivatives() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        for (final GroundStation station : context.stations) {
            station.setEstimated(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<Measurement> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();

        for (final Measurement measurement : measurements) {

            // parameter corresponding to station position offset
            final GroundStation stationParameter = ((Range) measurement).getStation();
            final AbsoluteDate date = ((Range) measurement).getDate();
            final SpacecraftState state     = propagator.propagate(date);
            final Vector3D extPoint = state.getPVCoordinates().getPosition();
            final Frame frameSat = state.getFrame();
            
            final double[]      jacobian  = RangeTroposphericDelayModifier.Derivatives.derivElevationWrtGroundstation(date, stationParameter, state);

            final double[][] finiteDifferencesJacobian =
                EstimationTestUtils.differentiate(new MultivariateVectorFunction() {
                        public double[] value(double[] point) throws OrekitExceptionWrapper {
                            try {

                                final double[] savedParameter = stationParameter.getValue();

                                // evaluate target's elevation with a changed station position
                                stationParameter.setValue(point);
                                
                                //final double[] result = new double[]{
                                //    stationParameter.getOffsetFrame().getElevation(extPoint, frameSat, date)
                                //};

                                final Frame frameSta = stationParameter.getOffsetFrame();
                                final Transform tSat2Sta = frameSat.getTransformTo(frameSta, date);
                                final Vector3D extPointTopo = tSat2Sta.transformPosition(extPoint);
                                final double[] result = new double[]{
                                    extPointTopo.getX()
                                };
                                
                                stationParameter.setValue(savedParameter);
                                
                                return result;

                            } catch (OrekitException oe) {
                                throw new OrekitExceptionWrapper(oe);
                            }
                        }
                    }, measurement.getDimension(), 3, 10.0, 10.0, 10.0).value(stationParameter.getValue());

            Assert.assertEquals(finiteDifferencesJacobian[0].length, jacobian.length);

            final double tolerance = 1e-10;
            for (int i = 0; i < jacobian.length; ++i) {
                    Assert.assertEquals(finiteDifferencesJacobian[0][i],
                                        jacobian[i],
                                        tolerance * FastMath.max(FastMath.abs(finiteDifferencesJacobian[0][i]), 1.));
            }

        }

    }
    
    @Test
    public void testModifierHeightParameterDerivatives() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        for (final GroundStation station : context.stations) {
            station.setEstimated(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<Measurement> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();

        for (final Measurement measurement : measurements) {

            // parameter corresponding to station position offset
            final GroundStation stationParameter = ((Range) measurement).getStation();
            final AbsoluteDate date = ((Range) measurement).getDate();
            final SpacecraftState state     = propagator.propagate(date);
           
            final double[]      jacobian  = RangeTroposphericDelayModifier.Derivatives.derivHeightWrtGroundstation(date, stationParameter, state);

            // compute dAltitude / dPosition
            final double[][] finiteDifferencesJacobian =
                EstimationTestUtils.differentiate(new MultivariateVectorFunction() {
                        public double[] value(double[] point) throws OrekitExceptionWrapper {
                            try {

                                final double[] savedParameter = stationParameter.getValue();

                                // evaluate target's altitude with a changed station position
                                stationParameter.setValue(point);

                                final double[] result = new double[]{
                                                    stationParameter.getOffsetFrame().getPoint().getAltitude()
                                };

                                stationParameter.setValue(savedParameter);
                                
                                return result;

                            } catch (OrekitException oe) {
                                throw new OrekitExceptionWrapper(oe);
                            }
                        }
                    }, measurement.getDimension(), 3, 20.0, 20.0, 20.0).value(stationParameter.getValue());

            Assert.assertEquals(finiteDifferencesJacobian[0].length, jacobian.length);

            final double tolerance = 1e-10;
            for (int i = 0; i < jacobian.length; ++i) {
                    Assert.assertEquals(finiteDifferencesJacobian[0][i],
                                        jacobian[i],
                                        tolerance * FastMath.max(FastMath.abs(finiteDifferencesJacobian[0][i]), 1.));
            }

        }

    }
    
}


