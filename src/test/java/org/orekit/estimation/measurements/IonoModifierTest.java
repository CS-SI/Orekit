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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.models.earth.KlobucharIonoModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UTCScale;

public class IonoModifierTest {
    /** utility constant to convert from radians to degrees. */
    private static double RADIANS_TO_DEGREES = 180. / Math.PI;
    
    /** ionospheric model. */
    private KlobucharIonoModel model;
    
    @Before
    public void setUp() throws Exception {
        // Navigation message data
        // .3820D-07   .1490D-07  -.1790D-06   .0000D-00          ION ALPHA           
        // .1430D+06   .0000D+00  -.3280D+06   .1130D+06          ION BETA              
        model = new KlobucharIonoModel(new double[]{.3820e-07, .1490e-07, -.1790e-06,0},
                                       new double[]{.1430e+06, 0, -.3280e+06, .1130e+06});
    }

    @After
    public void tearDown() {

    }
    
    @Test
    public void testRangeIonoModifier() throws OrekitException {

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
        final List<Measurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();

        
        final RangeIonosphericDelayModifier modifier = new RangeIonosphericDelayModifier(model);
        
        for (final Measurement<?> measurement : measurements) {
            Range range = (Range) measurement;
            range.addModifier(modifier);

            // parameter corresponding to station position offset
            final GroundStation stationParameter = range.getStation();
            final AbsoluteDate date = range.getDate();
            final SpacecraftState refstate     = propagator.propagate(date);

            // 
            Evaluation<Range> eval = range.evaluate(0,  refstate);
        }
    }
    
    @Test
    public void testRangeRateIonoModifier() throws OrekitException {

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
        final List<Measurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();
        
        final RangeRateIonosphericDelayModifier modifier = new RangeRateIonosphericDelayModifier(model);
        
        for (final Measurement<?> measurement : measurements) {
            RangeRate rangeRate = (RangeRate) measurement;
            rangeRate.addModifier(modifier);

            // parameter corresponding to station position offset
            final GroundStation stationParameter = rangeRate.getStation();
            final AbsoluteDate date = rangeRate.getDate();
            final SpacecraftState refstate     = propagator.propagate(date);

            // 
            Evaluation<RangeRate> eval = rangeRate.evaluate(0,  refstate);
        }
    }
    
    @Test
    public void testKlobucharIonoModel() throws OrekitException {
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
        final List<Measurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();
                
        for (final Measurement<?> measurement : measurements) {

            // parameter corresponding to station position offset
            final GroundStation station = ((Range) measurement).getStation();
            final AbsoluteDate date = ((Range) measurement).getDate();
            final SpacecraftState state     = propagator.propagate(date);

            final Vector3D position = state.getPVCoordinates().getPosition();
            
            //
            final GeodeticPoint geo = station.getBaseFrame().getPoint();
            
            // elevation in radians
            final double elevation =
                    station.getBaseFrame().getElevation(position,
                                                        state.getFrame(),
                                                        state.getDate()) * RADIANS_TO_DEGREES;
            
            // elevation in radians
            final double azimuth =
                    station.getBaseFrame().getAzimuth(position,
                                                        state.getFrame(),
                                                        state.getDate()) * RADIANS_TO_DEGREES;
            
            double delay = model.calculatePathDelay(date, geo, elevation, azimuth);
            System.out.println("Azimuth: " + azimuth + "; Elevation: " + elevation + "; Delay: " + delay);
        }        
        
    }
}


