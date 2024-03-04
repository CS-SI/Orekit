/* Copyright 2002-2024 CS GROUP
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
package org.orekit.models.earth.troposphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.weather.ConstantPressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.water.CIPM2007;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TrackingCoordinates;

public class FieldMendesPavlisModelTest {

    private static double epsilon = 1e-6;

    @BeforeAll
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

    @BeforeEach
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

    @Test
    public void testZenithDelay() {
        doTestZenithDelay(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestZenithDelay(final Field<T> field) {
        final T zero = field.getZero();
        // Site:   McDonald Observatory
        //         latitude:  30.67166667 °
        //         longitude: -104.0250 °
        //         height:    2010.344 m
        //
        // Meteo:  pressure:            798.4188 hPa
        //         water vapor presure: 14.322 hPa
        //         temperature:         300.15 K
        //         humidity:            40 %
        //
        // Ref:    Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
        //         IERS Technical Note No. 36, BKG (2010)

        final double latitude     = FastMath.toRadians(30.67166667);
        final double longitude    = FastMath.toRadians(-104.0250);
        final double height       = 2010.344;
        final double pressure     = TroposphericModelUtils.HECTO_PASCAL.toSI(798.4188);
        final double temperature  = 300.15;
        final double humidity     = 0.4;
        final PressureTemperatureHumidity pth = new PressureTemperatureHumidity(height,
                                                                                pressure,
                                                                                temperature,
                                                                                new CIPM2007().
                                                                                waterVaporPressure(pressure,
                                                                                                   temperature,
                                                                                                   humidity),
                                                                                Double.NaN,
                                                                                Double.NaN);
        final double lambda       = 0.532;
        final FieldGeodeticPoint<T> point = new FieldGeodeticPoint<>(zero.add(latitude), zero.add(longitude), zero.add(height));

        // Expected zenith hydrostatic delay: 1.932992 m (Ref)
        final double expectedHydroDelay = 1.932992;
        // Expected zenith wet delay: 0.223375*10-2 m (Ref)
        final double expectedWetDelay   = 0.223375e-2;
        // Expected total zenith delay: 1.935226 m (Ref)
        final double expectedDelay      = 1.935226;

        final double precision = 4.0e-6;

        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2009, 8, 12, TimeScalesFactory.getUTC());

        final MendesPavlisModel model = new MendesPavlisModel(new ConstantPressureTemperatureHumidityProvider(pth),
                                                              lambda, TroposphericModelUtils.MICRO_M);
        
        final T[] computedDelay = model.computeZenithDelay(point, model.getParameters(field), date);

        Assertions.assertEquals(expectedHydroDelay, computedDelay[0].getReal(),                    precision);
        Assertions.assertEquals(expectedWetDelay,   computedDelay[1].getReal(), precision);
        Assertions.assertEquals(expectedDelay,      computedDelay[0].getReal() + computedDelay[1].getReal(), precision);

    }

    @Test
    public void testMappingFactors() {
        doTestMappingFactors(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestMappingFactors(final Field<T> field) {
        final T zero = field.getZero();

        // Site:   McDonald Observatory
        //         latitude:  30.67166667 °
        //         longitude: -104.0250 °
        //         height:    2075 m
        //
        // Meteo:  pressure:            798.4188 hPa
        //         water vapor presure: 14.322 hPa
        //         temperature:         300.15 K
        //         humidity:            40 %
        //
        // Ref:    Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
        //         IERS Technical Note No. 36, BKG (2010)

        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2009, 8, 12, TimeScalesFactory.getUTC());

        final double latitude     = FastMath.toRadians(30.67166667);
        final double longitude    = FastMath.toRadians(-104.0250);
        final double height       = 2075;
        final double pressure     = TroposphericModelUtils.HECTO_PASCAL.toSI(798.4188);
        final double temperature  = 300.15;
        final double humidity     = 0.4;
        final PressureTemperatureHumidity pth = new PressureTemperatureHumidity(height,
                                                                                pressure,
                                                                                temperature,
                                                                                new CIPM2007().
                                                                                waterVaporPressure(pressure,
                                                                                                   temperature,
                                                                                                   humidity),
                                                                                Double.NaN,
                                                                                Double.NaN);
        final double lambda       = 0.532;
        final FieldGeodeticPoint<T> point = new FieldGeodeticPoint<>(zero.add(latitude), zero.add(longitude), zero.add(height));

        final FieldTrackingCoordinates<T> trackingCoordinates = new FieldTrackingCoordinates<>(zero,
                                                                                               zero.newInstance(FastMath.toRadians(15.0)),
                                                                                               zero);
        // Expected mapping factor: 3.80024367 (Ref)
        final double expectedMapping    = 3.80024367;

        // Test for the second constructor
        final MendesPavlisModel model = new MendesPavlisModel(new ConstantPressureTemperatureHumidityProvider(pth),
                                                              lambda, TroposphericModelUtils.MICRO_M);

        final T[] computedMapping = model.mappingFactors(trackingCoordinates, point,
                                                         new FieldPressureTemperatureHumidity<>(field,
                                                                                                TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                                         date);

        Assertions.assertEquals(expectedMapping, computedMapping[0].getReal(), 5.0e-8);
        Assertions.assertEquals(expectedMapping, computedMapping[1].getReal(), 5.0e-8);
    }

    @Test
    public void testDelay() {
        doTestDelay(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestDelay(final Field<T> field) {
        final T zero = field.getZero();
        final double elevation = 10d;
        final double height = 100d;
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final FieldGeodeticPoint<T> point = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(45.0)), zero.add(FastMath.toRadians(45.0)), zero.add(height));
        MendesPavlisModel model = MendesPavlisModel.getStandardModel(0.6943, TroposphericModelUtils.MICRO_M);
        final T path = model.pathDelay(new FieldTrackingCoordinates<>(zero,
                                                                      zero.newInstance(FastMath.toRadians(elevation)),
                                                                      zero),
                                       point,
                                       new FieldPressureTemperatureHumidity<>(field, TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                       model.getParameters(field), date).getDelay();
        Assertions.assertTrue(Precision.compareTo(path.getReal(), 20d, epsilon) < 0);
        Assertions.assertTrue(Precision.compareTo(path.getReal(), 0d, epsilon) > 0);
    }

    @Test
    public void testFixedHeight() {
        doTestFixedHeight(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFixedHeight(final Field<T> field) {
        final T zero = field.getZero();
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final FieldGeodeticPoint<T> point = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(45.0)), zero.add(FastMath.toRadians(45.0)), zero.add(350.0));
        MendesPavlisModel model = MendesPavlisModel.getStandardModel(0.6943, TroposphericModelUtils.MICRO_M);
        T lastDelay = zero.add(Double.MAX_VALUE);
        // delay shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final T delay = model.pathDelay(new FieldTrackingCoordinates<>(zero,
                                                                           zero.newInstance(FastMath.toRadians(elev)),
                                                                           zero),
                                            point,
                                            new FieldPressureTemperatureHumidity<>(field, TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                            model.getParameters(field), date).getDelay();
            Assertions.assertTrue(Precision.compareTo(delay.getReal(), lastDelay.getReal(), epsilon) < 0);
            lastDelay = delay;
        }
    }

    @Test
    public void testDelayStateDerivatives() {

        // Geodetic point
        final double latitude     = FastMath.toRadians(45.0);
        final double longitude    = FastMath.toRadians(45.0);
        final double height       = 0.0;
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, height);
        // Body: earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Topocentric frame
        final TopocentricFrame baseFrame = new TopocentricFrame(earth, point, "topo");

        // Station
        final GroundStation station = new GroundStation(baseFrame,
                                                        TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER);

        // Tropospheric model
        final MendesPavlisModel model = MendesPavlisModel.getStandardModel(0.65, TroposphericModelUtils.MICRO_M);

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

        // Initial satellite elevation
        final FieldVector3D<DerivativeStructure> position = dsState.getPosition();
        final FieldTrackingCoordinates<DerivativeStructure> dsTrackingCoordinates =
                        baseFrame.getTrackingCoordinates(position, frame, dsDate);

        // Compute Delay with state derivatives
        final FieldGeodeticPoint<DerivativeStructure> dsPoint = new FieldGeodeticPoint<>(zero.add(latitude), zero.add(longitude), zero.add(height));
        final DerivativeStructure delay = model.pathDelay(dsTrackingCoordinates, dsPoint,
                                                          new FieldPressureTemperatureHumidity<>(field, TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                                          model.getParameters(field, dsDate), dsDate).getDelay();

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
            final Vector3D positionM4 = stateM4.getPosition();
            final TrackingCoordinates trackingCoordinatesM4  = station.getBaseFrame().
                                        getTrackingCoordinates(positionM4, stateM4.getFrame(), stateM4.getDate());
            double  delayM4 = model.pathDelay(trackingCoordinatesM4, point, TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                              model.getParameters(), stateM4.getDate()).getDelay();
            
            SpacecraftState stateM3 = shiftState(state, orbitType, angleType, -3 * steps[i], i);
            final Vector3D positionM3 = stateM3.getPosition();
            final TrackingCoordinates trackingCoordinatesM3  = station.getBaseFrame().
                                        getTrackingCoordinates(positionM3, stateM3.getFrame(), stateM3.getDate());
            double  delayM3 = model.pathDelay(trackingCoordinatesM3, point, TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                              model.getParameters(), stateM3.getDate()).getDelay();
            
            SpacecraftState stateM2 = shiftState(state, orbitType, angleType, -2 * steps[i], i);
            final Vector3D positionM2 = stateM2.getPosition();
            final TrackingCoordinates trackingCoordinatesM2  = station.getBaseFrame().
                                        getTrackingCoordinates(positionM2, stateM2.getFrame(), stateM2.getDate());
            double  delayM2 = model.pathDelay(trackingCoordinatesM2, point, TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                              model.getParameters(), stateM2.getDate()).getDelay();
 
            SpacecraftState stateM1 = shiftState(state, orbitType, angleType, -1 * steps[i], i);
            final Vector3D positionM1 = stateM1.getPosition();
            final TrackingCoordinates trackingCoordinatesM1  = station.getBaseFrame().
                                        getTrackingCoordinates(positionM1, stateM1.getFrame(), stateM1.getDate());
            double  delayM1 = model.pathDelay(trackingCoordinatesM1, point, TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                              model.getParameters(), stateM1.getDate()).getDelay();
           
            SpacecraftState stateP1 = shiftState(state, orbitType, angleType, 1 * steps[i], i);
            final Vector3D positionP1 = stateP1.getPosition();
            final TrackingCoordinates trackingCoordinatesP1  = station.getBaseFrame().
                                        getTrackingCoordinates(positionP1, stateP1.getFrame(), stateP1.getDate());
            double  delayP1 = model.pathDelay(trackingCoordinatesP1, point, TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                              model.getParameters(), stateP1.getDate()).getDelay();
            
            SpacecraftState stateP2 = shiftState(state, orbitType, angleType, 2 * steps[i], i);
            final Vector3D positionP2 = stateP2.getPosition();
            final TrackingCoordinates trackingCoordinatesP2  = station.getBaseFrame().
                                        getTrackingCoordinates(positionP2, stateP2.getFrame(), stateP2.getDate());
            double  delayP2 = model.pathDelay(trackingCoordinatesP2, point, TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                              model.getParameters(), stateP2.getDate()).getDelay();
            
            SpacecraftState stateP3 = shiftState(state, orbitType, angleType, 3 * steps[i], i);
            final Vector3D positionP3 = stateP3.getPosition();
            final TrackingCoordinates trackingCoordinatesP3  = station.getBaseFrame().
                                        getTrackingCoordinates(positionP3, stateP3.getFrame(), stateP3.getDate());
            double  delayP3 = model.pathDelay(trackingCoordinatesP3, point, TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                              model.getParameters(), stateP3.getDate()).getDelay();
            
            SpacecraftState stateP4 = shiftState(state, orbitType, angleType, 4 * steps[i], i);
            final Vector3D positionP4 = stateP4.getPosition();
            final TrackingCoordinates trackingCoordinatesP4  = station.getBaseFrame().
                                        getTrackingCoordinates(positionP4, stateP4.getFrame(), stateP4.getDate());
            double  delayP4 = model.pathDelay(trackingCoordinatesP4, point, TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                              model.getParameters(), stateP4.getDate()).getDelay();
            
            fillJacobianColumn(refDeriv, i, orbitType, angleType, steps[i],
                               delayM4, delayM3, delayM2, delayM1,
                               delayP1, delayP2, delayP3, delayP4);
        }

        for (int i = 0; i < 6; i++) {
            Assertions.assertEquals(compDeriv[i + 1], refDeriv[0][i], 2.0e-11);
        }
    }

    private void fillJacobianColumn(double[][] jacobian, int column,
                                    OrbitType orbitType, PositionAngleType angleType, double h,
                                    double sM4h, double sM3h,
                                    double sM2h, double sM1h,
                                    double sP1h, double sP2h,
                                    double sP3h, double sP4h) {
        for (int i = 0; i < jacobian.length; ++i) {
            jacobian[i][column] = ( -3 * (sP4h - sM4h) +
                                    32 * (sP3h - sM3h) -
                                   168 * (sP2h - sM2h) +
                                   672 * (sP1h - sM1h)) / (840 * h);
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

}
