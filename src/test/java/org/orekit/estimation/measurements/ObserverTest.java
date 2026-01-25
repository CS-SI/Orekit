/* Copyright 2002-2025 Brianna Aubin
 * Licensed to Hawkeye 360 (HE360) under one or more
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.signal.FieldSignalTravelTimeAdjustableEmitter;
import org.orekit.estimation.measurements.signal.FieldSignalTravelTimeAdjustableReceiver;
import org.orekit.estimation.measurements.signal.SignalTravelTimeAdjustableEmitter;
import org.orekit.estimation.measurements.signal.SignalTravelTimeAdjustableReceiver;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.clocks.FieldClockOffset;
import org.orekit.time.clocks.QuadraticFieldClockModel;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class ObserverTest {

    private AbsoluteDate initDate;
    private Propagator propagator;
    private GroundStation station;
    private ObserverSatellite observerSatellite;
    private ObservableSatellite observableSatellite;

    @BeforeEach
    public void setup() {
                
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect inter-satellites range measurements
        final TimeStampedPVCoordinates original = context.initialOrbit.getPVCoordinates();
        final Orbit closeOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(context.initialOrbit.getDate(),
                                                                                 original.getPosition().add(new Vector3D(1000, 2000, 3000)),
                                                                                 original.getVelocity().add(new Vector3D(-0.03, 0.01, 0.02))),
                                                    context.initialOrbit.getFrame(),
                                                    context.initialOrbit.getMu());
        final Propagator closePropagator = EstimationTestUtils.createPropagator(closeOrbit,
                                                                                propagatorBuilder);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();

        this.initDate = context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod());
        closePropagator.propagate(initDate);
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        this.propagator = EstimationTestUtils.createPropagator(context.initialOrbit, propagatorBuilder);

        // Set up local observable satellite 
        final double localClockOffset  = 137.0e-9;
        this.observableSatellite = new ObservableSatellite(0);
        this.observableSatellite.getClockOffsetDriver().setValue(localClockOffset);
        
        // Set up remote observer satellite 
        final double remoteSatOffset = 469.0e-9;
        this.observerSatellite = new ObserverSatellite("", ephemeris);
        this.observerSatellite.getClockOffsetDriver().setValue(remoteSatOffset);

        // Set up remote observer ground station
        // Note: The satellite can't actually see this coordinate value, but it
        // doesn't really matter from a mathematical perspective
        final double remoteGroundOffset = 326.0e-9;
        this.station = context.TDOAstations.getFirst();
        this.station.getClockOffsetDriver().setValue(remoteGroundOffset);

        // Have to set reference date for station parameters, or get error in tests
        for (ParameterDriver driver : this.station.getParametersDrivers()) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(initDate);
            }
        }
        
    }

    @AfterEach
    public void tearDown() {
        initDate = null;
        propagator = null;
        observableSatellite = null;
        observerSatellite = null;
        station = null;
    }

    @Test
    public void testGroundStationRemote() {

        // Goes FROM ObservableSatellite TO GroundStation
        // Measured AT GroundStation 

        SpacecraftState[] states = new SpacecraftState[1];
        states[0] = propagator.propagate(initDate);

        // Time of flight from emitter to second station
        final PVCoordinatesProvider remoteGroundReceiverCoordsProvider = station.getPVCoordinatesProvider();
        final SignalTravelTimeAdjustableReceiver signalTimeOfFlight = new SignalTravelTimeAdjustableReceiver(remoteGroundReceiverCoordsProvider);
        final double tau = signalTimeOfFlight.computeDelay(states[0].getPosition(), initDate, states[0].getFrame());
        final AbsoluteDate measurementDate = initDate.shiftedBy(tau);

        final CommonParametersWithoutDerivatives common =
            station.computeRemoteParametersWithout(states, observableSatellite, measurementDate, false);

        // Make sure calculated time of flight is correct
        Assertions.assertEquals(tau, common.getTauD(), 1e-10);

        // Check timing offset of emission and reception dates
        final double remoteOffset = station.getQuadraticClockModel().getOffset(measurementDate).getOffset();
        Assertions.assertEquals(measurementDate.durationFrom(common.getRemotePV().getDate()), remoteOffset, 1e-12);
        Assertions.assertEquals(initDate.durationFrom(common.getTransitPV().getDate()), remoteOffset, 1e-9);

        // Check position of remote ground station at measurement/reception time
        final Vector3D p1 = remoteGroundReceiverCoordsProvider.getPosition(measurementDate.shiftedBy(-remoteOffset), states[0].getFrame());
        final Vector3D p2 = common.getRemotePV().getPosition();
        Assertions.assertEquals(0, Vector3D.distance(p1, p2), 1e-12);

        // Check position of local satellite at emission time
        final Vector3D p3 = states[0].shiftedBy(-remoteOffset).getPosition();
        final Vector3D p4 = common.getTransitPV().getPosition();
        Assertions.assertEquals(0, Vector3D.distance(p3, p4), 1e-12);
    }

    @Test
    public void testSatelliteRemote() {

        // Goes FROM ObservableSatellite TO ObserverSatellite
        // Measured AT ObserverSatellite

        SpacecraftState[] states = new SpacecraftState[1];
        states[0] = propagator.propagate(initDate);

        // Time of flight from emitter to second station
        final PVCoordinatesProvider remotePV = observerSatellite.getPVCoordinatesProvider();
        final SignalTravelTimeAdjustableReceiver signalTimeOfFlight = new SignalTravelTimeAdjustableReceiver(remotePV);
        final double tau = signalTimeOfFlight.computeDelay(states[0].getPosition(), initDate, states[0].getFrame());
        final AbsoluteDate measurementDate = initDate.shiftedBy(tau);

        final CommonParametersWithoutDerivatives common =
            observerSatellite.computeRemoteParametersWithout(states, observableSatellite, measurementDate, false);

        // Make sure calculated time of flight is correct
        Assertions.assertEquals(tau, common.getTauD(), 1e-10);

        // Check timing offset of emission and reception dates
        final double remoteOffset = observerSatellite.getQuadraticClockModel().getOffset(measurementDate).getOffset();
        Assertions.assertEquals(measurementDate.durationFrom(common.getRemotePV().getDate()), remoteOffset, 1e-12);
        Assertions.assertEquals(initDate.durationFrom(common.getTransitPV().getDate()), remoteOffset, 1e-9);

        // Check position of remote satellite at measurement/reception time
        final Vector3D p1 = remotePV.getPosition(measurementDate.shiftedBy(-remoteOffset), states[0].getFrame());
        final Vector3D p2 = common.getRemotePV().getPosition();
        Assertions.assertEquals(0, Vector3D.distance(p1, p2), 1e-12);

        // Check position of local satellite at emission time
        final Vector3D p3 = propagator.propagate(common.getTransitPV().getDate()).getPosition();
        final Vector3D p4 = common.getTransitPV().getPosition();
        Assertions.assertEquals(0, Vector3D.distance(p3, p4), 1e-12);
    }

    @Test
    public void testGroundStationRemoteWithDerivatives() {

        // Goes FROM ObservableSatellite TO GroundStation
        // Measured AT GroundStation 

        SpacecraftState[] states = new SpacecraftState[1];
        states[0] = propagator.propagate(initDate);

        // Dummy values to call the gradient functions
        final int nbParams = 6;
        List<ParameterDriver> parametersDrivers = new ArrayList<>();
        final Map<String, Integer> paramIndices = new HashMap<String, Integer>();

        final FieldAbsoluteDate<Gradient> gDate = new FieldAbsoluteDate<>(GradientField.getField(nbParams),
                                                                          initDate);

        final TimeStampedFieldPVCoordinates<Gradient> pvaLocal = AbstractMeasurement.getCoordinates(states[0], 0, nbParams);

        // Coords provider for remote ground station emitter
        final FieldPVCoordinatesProvider<Gradient> remoteEmitterPVCoordsProvider =
                                        station.getFieldPVCoordinatesProvider(nbParams, paramIndices);

        // Time of flight from emitter to second station
        final FieldSignalTravelTimeAdjustableReceiver<Gradient> signalTimeOfFlight =
                                new FieldSignalTravelTimeAdjustableReceiver<>(remoteEmitterPVCoordsProvider);
        final Gradient tau = signalTimeOfFlight.computeDelay(pvaLocal.getPosition(), gDate, states[0].getFrame());
        final FieldAbsoluteDate<Gradient> measurementDate = gDate.shiftedBy(tau);

        final CommonParametersWithDerivatives common =
            station.computeRemoteParametersWith(states, observableSatellite, 
            measurementDate.toAbsoluteDate(), parametersDrivers);

        final QuadraticFieldClockModel<Gradient> remoteClock = station.
                        getQuadraticFieldClock(nbParams, measurementDate.toAbsoluteDate(), paramIndices);

        final FieldClockOffset<Gradient> remoteClockOffset = remoteClock.getOffset(measurementDate);
        double measuredRemoteOffset  = measurementDate.toAbsoluteDate().durationFrom(common.getRemotePV().getDate().toAbsoluteDate());
        double measuredTransitOffset = initDate.durationFrom(common.getTransitPV().getDate().toAbsoluteDate());

        // Check that calculated time of flight is correct
        Assertions.assertEquals(tau.getValue(), common.getTauD().getValue(), 1e-10);

        // Check that measured remote and local clock offset values are correct
        Assertions.assertEquals(measuredRemoteOffset, remoteClockOffset.getOffset().getValue(), 1e-12);
        Assertions.assertEquals(measuredTransitOffset, remoteClockOffset.getOffset().getValue(), 1e-10);

        // Check position of remote ground station at measurement/reception time
        final Gradient remoteOffset = remoteClockOffset.getOffset();
        final FieldVector3D<Gradient> p1 = remoteEmitterPVCoordsProvider.getPosition(measurementDate.shiftedBy(remoteOffset.negate()), states[0].getFrame());
        final FieldVector3D<Gradient> p2 = common.getRemotePV().getPosition();
        Assertions.assertEquals(0, p1.toVector3D().distance(p2.toVector3D()), 1e-8);

        // Check position of local satellite at emission time
        final FieldVector3D<Gradient> p3 = pvaLocal.shiftedBy(remoteOffset.negate()).getPosition();
        final FieldVector3D<Gradient> p4 = common.getTransitPV().getPosition();
        Assertions.assertEquals(0, p3.toVector3D().distance(p4.toVector3D()), 1e-8);
    }

    @Test
    public void testSatelliteRemoteWithDerivatives() {

        // Goes FROM ObservableSatellite TO ObserverSatellite
        // Measured AT ObserverSatellite 

        SpacecraftState[] states = new SpacecraftState[1];
        states[0] = propagator.propagate(initDate);

        // Dummy values to call the gradient functions
        final int nbParams = 6;
        List<ParameterDriver> parametersDrivers = new ArrayList<>();
        final Map<String, Integer> paramIndices = new HashMap<String, Integer>();

        final FieldAbsoluteDate<Gradient> gDate = new FieldAbsoluteDate<>(GradientField.getField(nbParams),
                                                                          initDate);

        final TimeStampedFieldPVCoordinates<Gradient> pvaLocal = AbstractMeasurement.getCoordinates(states[0], 0, nbParams);

        // Time of flight from emitter to second station
        final FieldPVCoordinatesProvider<Gradient> remotePVCoordsProvider =
                                        observerSatellite.getFieldPVCoordinatesProvider(nbParams, paramIndices);
        final FieldSignalTravelTimeAdjustableReceiver<Gradient> signalTimeOfFlight = 
                        new FieldSignalTravelTimeAdjustableReceiver<>(remotePVCoordsProvider);
        final Gradient tau = signalTimeOfFlight.computeDelay(pvaLocal.getPosition(), gDate, states[0].getFrame());
        final FieldAbsoluteDate<Gradient> measurementDate = gDate.shiftedBy(tau);

        final CommonParametersWithDerivatives common =
            observerSatellite.computeRemoteParametersWith(states, observableSatellite, 
                                                          measurementDate.toAbsoluteDate(), 
                                                          parametersDrivers);

        final QuadraticFieldClockModel<Gradient> remoteClock = observerSatellite.
                        getQuadraticFieldClock(nbParams, measurementDate.toAbsoluteDate(), paramIndices);

        final FieldClockOffset<Gradient> remoteClockOffset = remoteClock.getOffset(measurementDate);
        double measuredRemoteOffset  = measurementDate.toAbsoluteDate().durationFrom(common.getRemotePV().getDate().toAbsoluteDate());
        double measuredTransitOffset = initDate.durationFrom(common.getTransitPV().getDate().toAbsoluteDate());

        Assertions.assertEquals(measuredRemoteOffset, remoteClockOffset.getOffset().getValue(), 1e-12);
        Assertions.assertEquals(measuredTransitOffset, remoteClockOffset.getOffset().getValue(), 1e-10);
        Assertions.assertEquals(tau.getValue(), common.getTauD().getValue(), 1e-9);

        // Check position of remote ground station at measurement/reception time
        final Gradient remoteOffset = remoteClockOffset.getOffset();
        final FieldVector3D<Gradient> p1 = remotePVCoordsProvider.getPosition(measurementDate.shiftedBy(remoteOffset.negate()), states[0].getFrame());
        final FieldVector3D<Gradient> p2 = common.getRemotePV().getPosition();
        Assertions.assertEquals(0, p1.toVector3D().distance(p2.toVector3D()), 1e-12);

        // Check position of local satellite at emission time
        final FieldVector3D<Gradient> p3 = pvaLocal.shiftedBy(remoteOffset.negate()).getPosition();
        final FieldVector3D<Gradient> p4 = common.getTransitPV().getPosition();
        Assertions.assertEquals(0, p3.toVector3D().distance(p4.toVector3D()), 1e-8);
    }


    @Test
    public void testGroundStationLocal() {

        // Goes FROM GroundStation TO ObservableSatellite 
        // Measured AT ObservableSatellite 

        SpacecraftState[] states = new SpacecraftState[1];
        states[0] = propagator.propagate(initDate);

        // Time of flight from receiver to ground station emitter
        final PVCoordinatesProvider groundEmitterCoordsProvider = station.getPVCoordinatesProvider();
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlight =
                        new SignalTravelTimeAdjustableEmitter(groundEmitterCoordsProvider);
        final double tau = signalTimeOfFlight.computeDelay(states[0].getPosition(), initDate, states[0].getFrame());
        final AbsoluteDate emissionDate = initDate.shiftedBy(-tau);

        final CommonParametersWithoutDerivatives common =
            station.computeLocalParametersWithout(states, observableSatellite, initDate, false);

        final double localOffset = observableSatellite.getQuadraticClockModel().getOffset(initDate).getOffset();
        Assertions.assertEquals(emissionDate.durationFrom(common.getRemotePV().getDate()), localOffset, 1e-10);
        Assertions.assertEquals(initDate.durationFrom(common.getTransitPV().getDate()), localOffset, 1e-12);
        Assertions.assertEquals(tau, common.getTauD(), 1e-9);

        // Check position of local satellite at measurement/reception time
        final Vector3D p1 = propagator.propagate(initDate.shiftedBy(-localOffset)).getPosition();
        final Vector3D p2 = common.getTransitPV().getPosition();
        Assertions.assertEquals(0, Vector3D.distance(p1, p2), 1e-8);

        // Check position of remote ground station at emission time
        final Vector3D p3 = groundEmitterCoordsProvider.getPosition(common.getRemotePV().getDate(), states[0].getFrame());
        final Vector3D p4 = common.getRemotePV().getPosition();
        Assertions.assertEquals(0, Vector3D.distance(p3, p4), 1e-12);
    }

    @Test
    public void testSatelliteLocal() {

        // Goes FROM ObserverSatellite TO ObservableSatellite 
        // Measured AT ObservableSatellite 

        SpacecraftState[] states = new SpacecraftState[1];
        states[0] = propagator.propagate(initDate);

        // Time of flight from local receiver to remote satellite emitter
        final PVCoordinatesProvider remotePV = observerSatellite.getPVCoordinatesProvider();
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlight = 
                        new SignalTravelTimeAdjustableEmitter(remotePV);
        final double tau = signalTimeOfFlight.computeDelay(states[0].getPosition(), initDate, states[0].getFrame());
        final AbsoluteDate emissionDate = initDate.shiftedBy(-tau);

        final CommonParametersWithoutDerivatives common =
            observerSatellite.computeLocalParametersWithout(states, observableSatellite, initDate, false);

        //Assertions.assertEquals(common.getTransitState().getDate())
        final double localOffset = observableSatellite.getQuadraticClockModel().getOffset(initDate).getOffset();
        Assertions.assertEquals(initDate.durationFrom(common.getTransitPV().getDate()), localOffset, 1e-12);
        Assertions.assertEquals(emissionDate.durationFrom(common.getRemotePV().getDate()), localOffset, 1e-10);
        Assertions.assertEquals(tau, common.getTauD(), 1e-9);

        // Check position of local satellite at measurement/reception time
        final Vector3D p1 = propagator.propagate(initDate.shiftedBy(-localOffset)).getPosition();
        final Vector3D p2 = common.getTransitPV().getPosition();
        Assertions.assertEquals(0, Vector3D.distance(p1, p2), 1e-8);

        // Check position of remote satellite at emission time
        final Vector3D p3 = remotePV.getPosition(common.getRemotePV().getDate(), states[0].getFrame());
        final Vector3D p4 = common.getRemotePV().getPosition();
        Assertions.assertEquals(0, Vector3D.distance(p3, p4), 1e-12);
    }

    @Test
    public void testGroundStationLocalWithDerivatives() {

        // Goes FROM GroundStation TO ObservableSatellite 
        // Measured AT ObservableSatellite 

        SpacecraftState[] states = new SpacecraftState[1];
        states[0] = propagator.propagate(initDate);

        // Dummy values to call the gradient functions
        final int nbParams = 6;
        List<ParameterDriver> parametersDrivers = new ArrayList<>();
        final Map<String, Integer> paramIndices = new HashMap<String, Integer>();
        
        final FieldAbsoluteDate<Gradient> gDate = new FieldAbsoluteDate<>(GradientField.getField(nbParams),
                                                                          initDate);

        final TimeStampedFieldPVCoordinates<Gradient> pvaLocal = AbstractMeasurement.getCoordinates(states[0], 0, nbParams);

        // Coords provider for remote ground station emitter
        final FieldPVCoordinatesProvider<Gradient> remoteEmitterPVCoordsProvider = station.getFieldPVCoordinatesProvider(nbParams, paramIndices);

        // Time of flight from emitter to second station
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> signalTimeOfFlight =
                                new FieldSignalTravelTimeAdjustableEmitter<>(remoteEmitterPVCoordsProvider);
        final Gradient tau = signalTimeOfFlight.computeDelay(pvaLocal.getPosition(), gDate, states[0].getFrame());
        final FieldAbsoluteDate<Gradient> emissionDate = gDate.shiftedBy(tau.negate());

        final CommonParametersWithDerivatives common =
            station.computeLocalParametersWith(states, observableSatellite, initDate, false, parametersDrivers);

        final QuadraticFieldClockModel<Gradient> localClock  = observableSatellite.
                                        getQuadraticFieldClock(nbParams, initDate, paramIndices);

        final FieldClockOffset<Gradient> localClockOffset  = localClock.getOffset(gDate);
        double measuredRemoteOffset  = emissionDate.toAbsoluteDate().durationFrom(common.getRemotePV().getDate().toAbsoluteDate());
        double measuredTransitOffset = initDate.durationFrom(common.getTransitPV().getDate().toAbsoluteDate());

        Assertions.assertEquals(measuredRemoteOffset, localClockOffset.getOffset().getValue(), 1e-12);
        Assertions.assertEquals(measuredTransitOffset, localClockOffset.getOffset().getValue(), 1e-10);
        Assertions.assertEquals(tau.getValue(), common.getTauD().getValue(), 1e-10);

        // Check position of local satellite at measurement/reception time
        final double localOffset = observableSatellite.getQuadraticClockModel().getOffset(initDate).getOffset();
        final Vector3D p1 = propagator.propagate(initDate.shiftedBy(-localOffset)).getPosition();
        final Vector3D p2 = common.getTransitPV().getPosition().toVector3D();
        Assertions.assertEquals(0, Vector3D.distance(p1, p2), 1e-8);

        // Check position of remote ground station at emission time
        final Vector3D p3 = remoteEmitterPVCoordsProvider.getPosition(common.getRemotePV().getDate(), states[0].getFrame()).toVector3D();
        final Vector3D p4 = common.getRemotePV().getPosition().toVector3D();
        Assertions.assertEquals(0, Vector3D.distance(p3, p4), 1e-12);
    }


    @Test
    public void testSatelliteLocalWithDerivatives() {

        // Goes FROM Observer TO ObservableSatellite 
        // Measured AT ObservableSatellite 

        SpacecraftState[] states = new SpacecraftState[1];
        states[0] = propagator.propagate(initDate);

        // Dummy values to call the gradient functions
        final int nbParams = 6;
        List<ParameterDriver> parametersDrivers = new ArrayList<>();
        final Map<String, Integer> paramIndices = new HashMap<String, Integer>();
        
        final FieldAbsoluteDate<Gradient> gDate = new FieldAbsoluteDate<>(GradientField.getField(nbParams),
                                                                          initDate);

        final TimeStampedFieldPVCoordinates<Gradient> pvaLocal = AbstractMeasurement.getCoordinates(states[0], 0, nbParams);

        // Time of flight from emitter to second station
        final FieldPVCoordinatesProvider<Gradient> remoteCoordsProvider = observerSatellite.getFieldPVCoordinatesProvider(nbParams, paramIndices);
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> signalTimeOfFlight = 
                        new FieldSignalTravelTimeAdjustableEmitter<>(remoteCoordsProvider);
        final Gradient tau = signalTimeOfFlight.computeDelay(pvaLocal.getPosition(), gDate, states[0].getFrame());
        final FieldAbsoluteDate<Gradient> emissionDate = gDate.shiftedBy(tau.negate());

        final CommonParametersWithDerivatives common =
            observerSatellite.computeLocalParametersWith(states, 
                                                         observableSatellite, 
                                                         initDate, 
                                                         false, 
                                                         parametersDrivers);

        final QuadraticFieldClockModel<Gradient> localClock  = observableSatellite.
                                        getQuadraticFieldClock(nbParams, initDate, paramIndices);

        final FieldClockOffset<Gradient> localClockOffset = localClock.getOffset(gDate);
        final double measuredRemoteOffset  = emissionDate.toAbsoluteDate().durationFrom(common.getRemotePV().getDate().toAbsoluteDate());
        final double measuredTransitOffset = initDate.durationFrom(common.getTransitPV().getDate().toAbsoluteDate());

        Assertions.assertEquals(measuredRemoteOffset, localClockOffset.getOffset().getValue(), 1e-12);
        Assertions.assertEquals(measuredTransitOffset, localClockOffset.getOffset().getValue(), 1e-10);
        Assertions.assertEquals(tau.getValue(), common.getTauD().getValue(), 1e-10);

        // Check position of local satellite at measurement/reception time
        final FieldClockOffset<Gradient> localOffset  = observableSatellite.getQuadraticFieldClock(nbParams, initDate, paramIndices).getOffset(gDate);
        final Vector3D p1 = propagator.propagate(gDate.shiftedBy(localOffset.getOffset().negate()).toAbsoluteDate()).getPosition();
        final Vector3D p2 = common.getTransitPV().getPosition().toVector3D();
        Assertions.assertEquals(0, Vector3D.distance(p1, p2), 1e-8);

        // Check position of remote satellite at emission time
        final Vector3D p3 = remoteCoordsProvider.getPosition(common.getRemotePV().getDate(), states[0].getFrame()).toVector3D();
        final Vector3D p4 = common.getRemotePV().getPosition().toVector3D();
        Assertions.assertEquals(0, Vector3D.distance(p3, p4), 1e-12);
    }

}
