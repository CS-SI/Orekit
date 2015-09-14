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
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.modifiers.RangeIonosphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.RangeRateIonosphericDelayModifier;
import org.orekit.models.earth.KlobucharIonoModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;

public class IonoModifierTest {
    
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
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate     = propagator.propagate(date);
            
            Range range = (Range) measurement;
            Evaluation<Range> evalNoMod = range.evaluate(0,  refstate);
            
            // add mofifier
            range.addModifier(modifier);
            // 
            Evaluation<Range> eval = range.evaluate(0,  refstate);
            
            final double diffMeters = eval.getValue()[0] - evalNoMod.getValue()[0];
            // TODO: check threshold
            Assert.assertEquals(0.0, diffMeters, 30.0);

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
        
        final RangeRateIonosphericDelayModifier modifier = new RangeRateIonosphericDelayModifier(model, true);
        
        for (final Measurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate     = propagator.propagate(date);
            
            RangeRate rangeRate = (RangeRate) measurement;
            Evaluation<RangeRate> evalNoMod = rangeRate.evaluate(0,  refstate);
            
            // add mofifier
            rangeRate.addModifier(modifier);

            // 
            Evaluation<RangeRate> eval = rangeRate.evaluate(0,  refstate);

            final double diffMetersSec = eval.getValue()[0] - evalNoMod.getValue()[0];
            // TODO: check threshold
            Assert.assertEquals(0.0, diffMetersSec, 0.015);

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
            
            // elevation in degrees
            final double elevation =
                    FastMath.toDegrees(station.getBaseFrame().getElevation(position,
                                                                           state.getFrame(),
                                                                           state.getDate()));
            
            // elevation in degrees
            final double azimuth =
                    FastMath.toDegrees(station.getBaseFrame().getAzimuth(position,
                                                                         state.getFrame(),
                                                                         state.getDate()));
            
            double delayMeters = model.calculatePathDelay(date, geo, elevation, azimuth);
            
            final double epsilon = 1e-6;
            Assert.assertTrue(Precision.compareTo(delayMeters, 15., epsilon) < 0);
            Assert.assertTrue(Precision.compareTo(delayMeters, 0., epsilon) > 0);            
        }        
        
    }
}


