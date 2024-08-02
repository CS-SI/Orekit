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
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractFieldMappingFunctionTest {

    protected abstract TroposphereMappingFunction buildMappingFunction();

    @Test
    public abstract void testMappingFactors();

    protected <T extends CalculusFieldElement<T>> void doTestMappingFactors(final Field<T> field,
                                                                            final double expectedHydro,
                                                                            final double expectedWet) {

        final T zero = field.getZero();

        // Site (Le Mans, France):      latitude:  48.0°
        //                              longitude: 0.20°
        //                              height:    68 m
        //
        // Date: 1st January 1994 at 0h UT
        //
        // Ref: Mercier F., Perosanz F., Mesures GNSS, Résolution des ambiguités.

        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 1994, 1, 1, TimeScalesFactory.getUTC());

        final double latitude    = FastMath.toRadians(48.0);
        final double longitude   = FastMath.toRadians(0.20);
        final double height      = 68.0;

        final FieldGeodeticPoint<T> point = new FieldGeodeticPoint<>(zero.add(latitude), zero.add(longitude), zero.add(height));

        final FieldTrackingCoordinates<T> trackingCoordinates  = new FieldTrackingCoordinates<>(zero,
                                                                                                FastMath.toRadians(zero.newInstance(5.0)),
                                                                                                zero);

        final TroposphereMappingFunction model = buildMappingFunction();

        final T[] computedMapping = model.mappingFactors(trackingCoordinates, point,
                                                         new FieldPressureTemperatureHumidity<>(field,
                                                                                                TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                                         date);

        assertEquals(expectedHydro, computedMapping[0].getReal(), 1.0e-2);
        assertEquals(expectedWet,   computedMapping[1].getReal(), 1.0e-2);
    }

    @Test
    public void testFixedHeight() {
        doTestFixedHeight(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFixedHeight(final Field<T> field) {
        final T zero = field.getZero();
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final FieldGeodeticPoint<T> point = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(45.0)), zero.add(FastMath.toRadians(45.0)), zero.add(350.0));
        final TroposphereMappingFunction model = buildMappingFunction();
        T[] lastFactors = MathArrays.buildArray(field, 2);
        lastFactors[0] = zero.add(Double.MAX_VALUE);
        lastFactors[1] = zero.add(Double.MAX_VALUE);
        // mapping functions shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final T[] factors = model.mappingFactors(new FieldTrackingCoordinates<>(zero,
                                                                                    FastMath.toRadians(zero.newInstance(elev)),
                                                                                    zero),
                                                     point,
                                                     new FieldPressureTemperatureHumidity<>(field,
                                                                                            TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                                     date);
            assertTrue(Precision.compareTo(factors[0].getReal(), lastFactors[0].getReal(), 1.0e-6) < 0);
            assertTrue(Precision.compareTo(factors[1].getReal(), lastFactors[1].getReal(), 1.0e-6) < 0);
            lastFactors[0] = factors[0];
            lastFactors[1] = factors[1];
        }
    }

    @Test
    public abstract void testMFStateDerivatives();

    protected void doTestMFStateDerivatives(final double epsMFH, final double epsMFW) {

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

        // Mapping Function model
        final TroposphereMappingFunction model = buildMappingFunction();

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

        final FieldPressureTemperatureHumidity<DerivativeStructure> weather =
                        new FieldPressureTemperatureHumidity<>(field,
                                                               TroposphericModelUtils.STANDARD_ATMOSPHERE);

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

        // Compute mapping factors with state derivatives
        final FieldGeodeticPoint<DerivativeStructure> dsPoint = new FieldGeodeticPoint<>(zero.add(latitude), zero.add(longitude), zero.add(height));
        final DerivativeStructure[] factors = model.mappingFactors(dsTrackingCoordinates, dsPoint, weather, dsDate);

        final double[] compMFH = factors[0].getAllDerivatives();
        final double[] compMFW = factors[1].getAllDerivatives();

        // Field -> non-field
        final Orbit orbit = dsOrbit.toOrbit();
        final SpacecraftState state = dsState.toSpacecraftState();

        // Finite differences for reference values
        final double[][] refMF = new double[2][6];
        final OrbitType orbitType = OrbitType.KEPLERIAN;
        final PositionAngleType angleType = PositionAngleType.MEAN;
        double dP = 0.001;
        double[] steps = NumericalPropagator.tolerances(1000000 * dP, orbit, orbitType)[0];
        for (int i = 0; i < 6; i++) {
            SpacecraftState stateM4 = shiftState(state, orbitType, angleType, -4 * steps[i], i);
            final Vector3D positionM4 = stateM4.getPosition();
            final TrackingCoordinates trackingCoordinatesM4  = station.getBaseFrame().
                                        getTrackingCoordinates(positionM4, stateM4.getFrame(), stateM4.getDate());
            double[]  delayM4 = model.mappingFactors(trackingCoordinatesM4, point,
                                                     TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                     stateM4.getDate());

            SpacecraftState stateM3 = shiftState(state, orbitType, angleType, -3 * steps[i], i);
            final Vector3D positionM3 = stateM3.getPosition();
            final TrackingCoordinates trackingCoordinatesM3  = station.getBaseFrame().
                                        getTrackingCoordinates(positionM3, stateM3.getFrame(), stateM3.getDate());
            double[]  delayM3 = model.mappingFactors(trackingCoordinatesM3, point,
                                                     TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                     stateM3.getDate());

            SpacecraftState stateM2 = shiftState(state, orbitType, angleType, -2 * steps[i], i);
            final Vector3D positionM2 = stateM2.getPosition();
            final TrackingCoordinates trackingCoordinatesM2  = station.getBaseFrame().
                                        getTrackingCoordinates(positionM2, stateM2.getFrame(), stateM2.getDate());
            double[]  delayM2 = model.mappingFactors(trackingCoordinatesM2, point,
                                                     TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                     stateM2.getDate());

            SpacecraftState stateM1 = shiftState(state, orbitType, angleType, -1 * steps[i], i);
            final Vector3D positionM1 = stateM1.getPosition();
            final TrackingCoordinates trackingCoordinatesM1  = station.getBaseFrame().
                                        getTrackingCoordinates(positionM1, stateM1.getFrame(), stateM1.getDate());
            double[]  delayM1 = model.mappingFactors(trackingCoordinatesM1, point,
                                                     TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                     stateM1.getDate());

            SpacecraftState stateP1 = shiftState(state, orbitType, angleType, 1 * steps[i], i);
            final Vector3D positionP1 = stateP1.getPosition();
            final TrackingCoordinates trackingCoordinatesP1  = station.getBaseFrame().
                                        getTrackingCoordinates(positionP1, stateP1.getFrame(), stateP1.getDate());
            double[]  delayP1 = model.mappingFactors(trackingCoordinatesP1, point,
                                                     TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                     stateP1.getDate());

            SpacecraftState stateP2 = shiftState(state, orbitType, angleType, 2 * steps[i], i);
            final Vector3D positionP2 = stateP2.getPosition();
            final TrackingCoordinates trackingCoordinatesP2  = station.getBaseFrame().
                                        getTrackingCoordinates(positionP2, stateP2.getFrame(), stateP2.getDate());
            double[]  delayP2 = model.mappingFactors(trackingCoordinatesP2, point,
                                                     TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                     stateP2.getDate());

            SpacecraftState stateP3 = shiftState(state, orbitType, angleType, 3 * steps[i], i);
            final Vector3D positionP3 = stateP3.getPosition();
            final TrackingCoordinates trackingCoordinatesP3  = station.getBaseFrame().
                                        getTrackingCoordinates(positionP3, stateP3.getFrame(), stateP3.getDate());
            double[]  delayP3 = model.mappingFactors(trackingCoordinatesP3, point,
                                                     TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                     stateP3.getDate());

            SpacecraftState stateP4 = shiftState(state, orbitType, angleType, 4 * steps[i], i);
            final Vector3D positionP4 = stateP4.getPosition();
            final TrackingCoordinates trackingCoordinatesP4  = station.getBaseFrame().
                                        getTrackingCoordinates(positionP4, stateP4.getFrame(), stateP4.getDate());
            double[]  delayP4 = model.mappingFactors(trackingCoordinatesP4, point,
                                                     TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                     stateP4.getDate());

            fillJacobianColumn(refMF, i, orbitType, angleType, steps[i],
                               delayM4, delayM3, delayM2, delayM1,
                               delayP1, delayP2, delayP3, delayP4);
        }

        // Tolerances
        for (int i = 0; i < 6; i++) {
            assertEquals(0., FastMath.abs(compMFH[i + 1] - refMF[0][i]), epsMFH);
            assertEquals(0., FastMath.abs(compMFW[i + 1] - refMF[1][i]), epsMFW);
        }
    }

    private void fillJacobianColumn(double[][] jacobian, int column,
                                    OrbitType orbitType, PositionAngleType angleType, double h,
                                    double[] sM4h, double[] sM3h,
                                    double[] sM2h, double[] sM1h,
                                    double[] sP1h, double[] sP2h,
                                    double[] sP3h, double[] sP4h) {
        for (int i = 0; i < jacobian.length; ++i) {
            jacobian[i][column] = ( -3 * (sP4h[i] - sM4h[i]) +
                            32 * (sP3h[i] - sM3h[i]) -
                            168 * (sP2h[i] - sM2h[i]) +
                            672 * (sP1h[i] - sM1h[i])) / (840 * h);
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

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
