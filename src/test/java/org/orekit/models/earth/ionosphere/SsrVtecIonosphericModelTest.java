/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.models.earth.ionosphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.metric.messages.ssr.subtype.SsrIm201;
import org.orekit.gnss.metric.messages.ssr.subtype.SsrIm201Data;
import org.orekit.gnss.metric.messages.ssr.subtype.SsrIm201Header;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;

import java.util.Collections;

public class SsrVtecIonosphericModelTest {

    private SsrIm201 vtecMessage;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");

        // Header
        final SsrIm201Header header = new SsrIm201Header();
        header.setSsrEpoch1s(302400.0);
        header.setNumberOfIonosphericLayers(1);

        // Data for the single layer (random values)
        final SsrIm201Data data = new SsrIm201Data();
        data.setHeightIonosphericLayer(650000.0);
        data.setSphericalHarmonicsDegree(2);
        data.setSphericalHarmonicsOrder(1);
        data.setCnm(new double[][] { {18.2, 0.0},
                                     {13.4, 26.2},
                                     {14.7, 6.8} });
        data.setSnm(new double[][] { {0.0, 0.0},
                                     {0.0, 6.2},
                                     {0.0, 17.6} });


        // Initialize message
        vtecMessage = new SsrIm201(201, header, Collections.singletonList(data));

    }

    @Test
    public void testDelay() {

        // Frequency
        final double frequency = Frequency.G01.getMHzFrequency() * 1.0e6;

        // Geodetic point
        final double height       = 0.0;
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(25.0), height);
        // Body: earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Topocentric frame
        final TopocentricFrame baseFrame = new TopocentricFrame(earth, point, "topo");

        // Ionospheric model
        final SsrVtecIonosphericModel model = new SsrVtecIonosphericModel(vtecMessage);

        // Spacecraft state
        final AbsoluteDate    date    = AbsoluteDate.J2000_EPOCH;
        final Frame           frame   = FramesFactory.getEME2000();
        final Orbit           orbit   = new KeplerianOrbit(24464560.0, 0.05, 0.122138,
                                               3.10686, 1.00681, 0.048363,
                                               PositionAngleType.MEAN, frame, date, Constants.WGS84_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Delay
        final double delay = model.pathDelay(state, baseFrame, frequency, model.getParameters());
        Assertions.assertEquals(13.488, delay, 0.001);

    }

    @Test
    public void testFieldDelay() {
        doTestFieldDelay(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldDelay(final Field<T> field) {
        final T zero = field.getZero();
        // Frequency
        final double frequency = Frequency.G01.getMHzFrequency() * 1.0e6;

        // Geodetic point
        final double height       = 0.0;
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(25.0), height);
        // Body: earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Topocentric frame
        final TopocentricFrame baseFrame = new TopocentricFrame(earth, point, "topo");

        // Ionospheric model
        final SsrVtecIonosphericModel model = new SsrVtecIonosphericModel(vtecMessage);

        // Spacecraft state
        final FieldAbsoluteDate<T>    date    = FieldAbsoluteDate.getJ2000Epoch(field);
        final Frame                   frame   = FramesFactory.getEME2000();
        final FieldOrbit<T>           orbit   = new FieldKeplerianOrbit<>(zero.add(24464560.0), zero.add(0.05), zero.add(0.122138),
                                                                          zero.add(3.10686), zero.add(1.00681), zero.add(0.048363),
                                                                          PositionAngleType.MEAN, frame, date, zero.add(Constants.WGS84_EARTH_MU));
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit);

        // Delay
        final T delay = model.pathDelay(state, baseFrame, frequency, model.getParameters(field));
        Assertions.assertEquals(13.488, delay.getReal(), 0.001);
    }

    @Test
    public void testZeroDelay() {
        // Frequency
        final double frequency = Frequency.G01.getMHzFrequency() * 1.0e6;

        // Geodetic point
        final double height       = 0.0;
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(45.0), height);
        // Body: earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Topocentric frame
        final TopocentricFrame baseFrame = new TopocentricFrame(earth, point, "topo");

        // Ionospheric model
        final SsrVtecIonosphericModel model = new SsrVtecIonosphericModel(vtecMessage);

        // Spacecraft state
        final AbsoluteDate    date    = AbsoluteDate.J2000_EPOCH;
        final Frame           frame   = FramesFactory.getEME2000();
        final Orbit           orbit   = new KeplerianOrbit(24464560.0, 0.05, 0.122138,
                                               3.10686, 1.00681, 0.048363,
                                               PositionAngleType.MEAN, frame, date, Constants.WGS84_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Delay
        final double delay = model.pathDelay(state, baseFrame, frequency, model.getParameters());
        Assertions.assertEquals(0.0, delay, Double.MIN_VALUE);
    }

    @Test
    public void testFieldZeroDelay() {
        doTestFieldZeroDelay(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldZeroDelay(final Field<T> field) {
        final T zero = field.getZero();
        // Frequency
        final double frequency = Frequency.G01.getMHzFrequency() * 1.0e6;

        // Geodetic point
        final double height       = 0.0;
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(45.0), height);
        // Body: earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Topocentric frame
        final TopocentricFrame baseFrame = new TopocentricFrame(earth, point, "topo");

        // Ionospheric model
        final SsrVtecIonosphericModel model = new SsrVtecIonosphericModel(vtecMessage);

        // Spacecraft state
        final FieldAbsoluteDate<T>    date    = FieldAbsoluteDate.getJ2000Epoch(field);
        final Frame                   frame   = FramesFactory.getEME2000();
        final FieldOrbit<T>           orbit   = new FieldKeplerianOrbit<>(zero.add(24464560.0), zero.add(0.05), zero.add(0.122138),
                                                                          zero.add(3.10686), zero.add(1.00681), zero.add(0.048363),
                                                                          PositionAngleType.MEAN, frame, date, zero.add(Constants.WGS84_EARTH_MU));
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit);

        // Delay
        final T delay = model.pathDelay(state, baseFrame, frequency, model.getParameters(field));
        Assertions.assertEquals(0.0, delay.getReal(), Double.MIN_VALUE);
    }

    @Test
    public void testDelayStateDerivatives() {

        // Frequency
        final double frequency = Frequency.G01.getMHzFrequency() * 1.0e6;

        // Geodetic point
        final double height       = 0.0;
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(0.0), FastMath.toRadians(0.0), height);
        // Body: earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Topocentric frame
        final TopocentricFrame baseFrame = new TopocentricFrame(earth, point, "topo");

        // Ionospheric model
        final SsrVtecIonosphericModel model = new SsrVtecIonosphericModel(vtecMessage);

        // Derivative Structure
        final DSFactory factory = new DSFactory(6, 1);
        final DerivativeStructure a0       = factory.variable(0, 24464560.0);
        final DerivativeStructure e0       = factory.variable(1, 0.05);
        final DerivativeStructure i0       = factory.variable(2, 0.122138);
        final DerivativeStructure pa0      = factory.variable(3, 3.10686);
        final DerivativeStructure raan0    = factory.variable(4, 1.00681);
        final DerivativeStructure anomaly0 = factory.variable(5, 0.048363);
        final Field<DerivativeStructure> field = a0.getField();
        final DerivativeStructure zero = field.getZero();

        // Field Date
        final FieldAbsoluteDate<DerivativeStructure> dsDate = new FieldAbsoluteDate<>(field);
        // Field Orbit
        final Frame frame = FramesFactory.getEME2000();
        final FieldOrbit<DerivativeStructure> dsOrbit = new FieldKeplerianOrbit<>(a0, e0, i0, pa0, raan0, anomaly0,
                                                                                  PositionAngleType.MEAN, frame,
                                                                                  dsDate, zero.add(3.9860047e14));
        // Field State
        final FieldSpacecraftState<DerivativeStructure> dsState = new FieldSpacecraftState<>(dsOrbit);

        // Set drivers reference date
        for (final ParameterDriver driver : model.getParametersDrivers()) {
            driver.setReferenceDate(dsDate.toAbsoluteDate());
        }

        // Compute Delay with state derivatives
        final DerivativeStructure delay = model.pathDelay(dsState, baseFrame, frequency, model.getParameters(field));

        final double[] compDeriv = delay.getAllDerivatives();

        // Field -> non-field
        final Orbit orbit = dsOrbit.toOrbit();
        final SpacecraftState state = dsState.toSpacecraftState();

        // Finite differences for reference values
        final double[][] refDeriv = new double[1][6];
        final OrbitType orbitType = OrbitType.KEPLERIAN;
        final PositionAngleType angleType = PositionAngleType.MEAN;
        double dP = 0.001;
        double[] steps = NumericalPropagator.tolerances(1000000 * dP, orbit, orbitType)[0];
        for (int i = 0; i < 6; i++) {
            SpacecraftState stateM4 = shiftState(state, orbitType, angleType, -4 * steps[i], i);
            double  delayM4 = model.pathDelay(stateM4, baseFrame, frequency, model.getParameters());
            
            SpacecraftState stateM3 = shiftState(state, orbitType, angleType, -3 * steps[i], i);
            double  delayM3 = model.pathDelay(stateM3, baseFrame, frequency, model.getParameters());
            
            SpacecraftState stateM2 = shiftState(state, orbitType, angleType, -2 * steps[i], i);
            double  delayM2 = model.pathDelay(stateM2, baseFrame, frequency, model.getParameters());
 
            SpacecraftState stateM1 = shiftState(state, orbitType, angleType, -1 * steps[i], i);
            double  delayM1 = model.pathDelay(stateM1, baseFrame, frequency, model.getParameters());
           
            SpacecraftState stateP1 = shiftState(state, orbitType, angleType, 1 * steps[i], i);
            double  delayP1 = model.pathDelay(stateP1, baseFrame, frequency, model.getParameters());
            
            SpacecraftState stateP2 = shiftState(state, orbitType, angleType, 2 * steps[i], i);
            double  delayP2 = model.pathDelay(stateP2, baseFrame, frequency, model.getParameters());
            
            SpacecraftState stateP3 = shiftState(state, orbitType, angleType, 3 * steps[i], i);
            double  delayP3 = model.pathDelay(stateP3, baseFrame, frequency, model.getParameters());
            
            SpacecraftState stateP4 = shiftState(state, orbitType, angleType, 4 * steps[i], i);
            double  delayP4 = model.pathDelay(stateP4, baseFrame, frequency, model.getParameters());
            
            fillJacobianColumn(refDeriv, i, steps[i],
                               delayM4, delayM3, delayM2, delayM1,
                               delayP1, delayP2, delayP3, delayP4);
        }

        for (int i = 0; i < 6; i++) {
            Assertions.assertEquals(compDeriv[i + 1], refDeriv[0][i], 2.3e-11);
        }
    }

    @Test
    public void testDelayRange() {

        // Frequency
        final double frequency = Frequency.G01.getMHzFrequency() * 1.0e6;

        // Geodetic point
        final double height = 0.0;

        // Body: earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        // Ionospheric model
        final SsrVtecIonosphericModel model = new SsrVtecIonosphericModel(vtecMessage);

        // Spacecraft state
        final AbsoluteDate    date    = AbsoluteDate.J2000_EPOCH;
        final Frame           frame   = FramesFactory.getEME2000();
        final Orbit           orbit   = new KeplerianOrbit(24464560.0, 0.05, 0.122138,
                                               3.10686, 1.00681, 0.048363,
                                               PositionAngleType.MEAN, frame, date, Constants.WGS84_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Delay for different station location
        for (double latitude = -90.0; latitude <= 90.0; latitude = latitude + 5.0) {
            for (double longitude = -180.0; longitude <= 180.0; longitude += 10.0) {
                final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(latitude), FastMath.toRadians(longitude), height);
                final TopocentricFrame baseFrame = new TopocentricFrame(earth, point, "topo");
                final double delay = model.pathDelay(state, baseFrame, frequency, model.getParameters());
                Assertions.assertTrue(delay >= 0 && delay < 20.0);
            }
        }

    }

    @Test
    public void testFieldDelayRange() {
        doTestFieldDelayRange(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldDelayRange(final Field<T> field) {
        final T zero = field.getZero();
        // Frequency
        final double frequency = Frequency.G01.getMHzFrequency() * 1.0e6;

        // Geodetic point
        final double height       = 0.0;

        // Body: earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        // Ionospheric model
        final SsrVtecIonosphericModel model = new SsrVtecIonosphericModel(vtecMessage);

        // Spacecraft state
        final FieldAbsoluteDate<T>    date    = FieldAbsoluteDate.getJ2000Epoch(field);
        final Frame                   frame   = FramesFactory.getEME2000();
        final FieldOrbit<T>           orbit   = new FieldKeplerianOrbit<>(zero.add(24464560.0), zero.add(0.05), zero.add(0.122138),
                                                                          zero.add(3.10686), zero.add(1.00681), zero.add(0.048363),
                                                                          PositionAngleType.MEAN, frame, date, zero.add(Constants.WGS84_EARTH_MU));
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit);

        // Delay for different station location
        for (double latitude = -90.0; latitude <= 90.0; latitude = latitude + 5.0) {
            for (double longitude = -180.0; longitude <= 180.0; longitude += 10.0) {
                final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(latitude), FastMath.toRadians(longitude), height);
                final TopocentricFrame baseFrame = new TopocentricFrame(earth, point, "topo");
                final T delay = model.pathDelay(state, baseFrame, frequency, model.getParameters(field));
                Assertions.assertTrue(delay.getReal() >= 0 && delay.getReal() < 20.0);
            }
        }

    }

    private SpacecraftState shiftState(SpacecraftState state, OrbitType orbitType, PositionAngleType angleType,
                                       double delta, int column) {

        double[][] array = stateToArray(state, orbitType, angleType, true);
        array[0][column] += delta;

        return arrayToState(array, orbitType, angleType, state.getFrame(), state.getDate(),
                            state.getMu(), state.getAttitude());

    }

    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType, PositionAngleType angleType,
                                    boolean withMass) {
        double[][] array = new double[2][withMass ? 7 : 6];
        orbitType.mapOrbitToArray(state.getOrbit(), angleType, array[0], array[1]);
        if (withMass) {
          array[0][6] = state.getMass();
        }
        return array;
    }

    private SpacecraftState arrayToState(double[][] array, OrbitType orbitType, PositionAngleType angleType,
                                           Frame frame, AbsoluteDate date, double mu,
                                           Attitude attitude) {
        Orbit orbit = orbitType.mapArrayToOrbit(array[0], array[1], angleType, date, mu, frame);
        return (array.length > 6) ?
                new SpacecraftState(orbit, attitude) :
                new SpacecraftState(orbit, attitude, array[0][6]);
    }

    private void fillJacobianColumn(double[][] jacobian, int column, double h,
                                      double sM4h, double sM3h,
                                      double sM2h, double sM1h,
                                      double sP1h, double sP2h,
                                      double sP3h, double sP4h) {

        jacobian[0][column] = ( -3 * (sP4h - sM4h) +
                                32 * (sP3h - sM3h) -
                               168 * (sP2h - sM2h) +
                               672 * (sP1h - sM1h)) / (840 * h);
    }

}
