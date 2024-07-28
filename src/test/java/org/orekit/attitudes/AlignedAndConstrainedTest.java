/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.UnivariateVectorFunction;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.util.function.Function;

public class AlignedAndConstrainedTest {

    @Test
    public void testAlignmentsEarthSun() {
        doTestAlignment(Vector3D.PLUS_K, PredefinedTarget.EARTH,
                        Vector3D.PLUS_I, PredefinedTarget.SUN,
                        t -> Vector3D.ZERO,
                        t -> sun.getPosition(t, eme2000),
                        1.0e-15, 1.0e-15);
    }

    @Test
    public void testAlignmentsEarthSunField() {
        final Binary64Field field = Binary64Field.getInstance();
        doTestAlignment(field,
                        Vector3D.PLUS_K, PredefinedTarget.EARTH,
                        Vector3D.PLUS_I, PredefinedTarget.SUN,
                        t -> FieldVector3D.getZero(field),
                        t -> sun.getPosition(t, eme2000),
                        1.0e-15, 1.0e-15);
    }

    @Test
    public void testDerivativesEarthSun() {
        doTestDerivatives(Vector3D.PLUS_K, PredefinedTarget.EARTH,
                          Vector3D.PLUS_I, PredefinedTarget.SUN,
                          2.0e-15);
    }

    @Test
    public void testAlignmentsNadirNorth() {
        doTestAlignment(Vector3D.PLUS_K, PredefinedTarget.NADIR,
                        Vector3D.PLUS_I, PredefinedTarget.NORTH,
                        t -> {
                            final Vector3D      pH          = orbit.shiftedBy(t.durationFrom(orbit.getDate())).getPosition();
                            final GeodeticPoint gpH         = earth.transform(pH, eme2000, t);
                            final GeodeticPoint gp0         = new GeodeticPoint(gpH.getLatitude(), gpH.getLongitude(), 0.0);
                            final Vector3D      p0          = earth.transform(gp0);
                            final Transform     earth2Inert = earth.getBodyFrame().getTransformTo(eme2000, t);
                            return earth2Inert.transformPosition(p0);
                        },
                        t -> Vector3D.PLUS_K,
                        1.0e-13, 4.0e-5);
    }

    @Test
    public void testAlignmentsNadirNorthField() {
        final Binary64Field field = Binary64Field.getInstance();
        doTestAlignment(field,
                        Vector3D.PLUS_K, PredefinedTarget.NADIR,
                        Vector3D.PLUS_I, PredefinedTarget.NORTH,
                        t -> {
                            final FieldVector3D<Binary64>      pH          = getOrbit(field).shiftedBy(t.durationFrom(orbit.getDate())).getPosition();
                            final FieldGeodeticPoint<Binary64> gpH         = earth.transform(pH, eme2000, t);
                            final FieldGeodeticPoint<Binary64> gp0         = new FieldGeodeticPoint<>(gpH.getLatitude(),
                                                                                                      gpH.getLongitude(),
                                                                                                      field.getZero());
                            final FieldVector3D<Binary64>      p0          = earth.transform(gp0);
                            final FieldTransform<Binary64>     earth2Inert = earth.getBodyFrame().getTransformTo(eme2000, t);
                            return earth2Inert.transformPosition(p0);
                        },
                        t -> FieldVector3D.getPlusK(field),
                        1.0e-15, 4.0e-5);
    }

    @Test
    public void testDerivativesNadirNorth() {
        doTestDerivatives(Vector3D.PLUS_K, PredefinedTarget.NADIR,
                          Vector3D.PLUS_I, PredefinedTarget.NORTH,
                          2.0e-15);
    }

    @Test
    public void testAlignmentsVelocityMomentum() {
        doTestAlignment(Vector3D.MINUS_J, PredefinedTarget.VELOCITY,
                        Vector3D.MINUS_K, PredefinedTarget.MOMENTUM,
                        t -> orbit.shiftedBy(t.durationFrom(orbit.getDate())).getPVCoordinates().getVelocity().normalize(),
                        t -> orbit.shiftedBy(t.durationFrom(orbit.getDate())).getPVCoordinates().getMomentum().normalize(),
                        1.0e-10, 1.0e-15);
    }

    @Test
    public void testAlignmentsVelocityMomentumField() {
        final Binary64Field field = Binary64Field.getInstance();
        doTestAlignment(field,
                        Vector3D.MINUS_J, PredefinedTarget.VELOCITY,
                        Vector3D.MINUS_K, PredefinedTarget.MOMENTUM,
                        t -> getOrbit(field).shiftedBy(t.durationFrom(orbit.getDate())).getPVCoordinates().getVelocity().normalize(),
                        t -> getOrbit(field).shiftedBy(t.durationFrom(orbit.getDate())).getPVCoordinates().getMomentum().normalize(),
                        1.0e-10, 1.0e-15);
    }

    @Test
    public void testDerivativesVelocityMomentum() {
        doTestDerivatives(Vector3D.MINUS_J, PredefinedTarget.VELOCITY,
                          Vector3D.MINUS_K, PredefinedTarget.MOMENTUM,
                          7.0e-16);
    }

    @Test
    public void testAlignmentsStationEast() {
        doTestAlignment(Vector3D.PLUS_K, new GroundPointTarget(station),
                        Vector3D.PLUS_I, PredefinedTarget.EAST,
                        t -> earth.getBodyFrame().getTransformTo(eme2000, t).transformPosition(station),
                        t -> {
                           final Transform earth2Inert = earth.getBodyFrame().getTransformTo(eme2000, t);
                           final Vector3D  pInert      = orbit.shiftedBy(t.durationFrom(orbit.getDate())).getPosition();
                           final GeodeticPoint gp      = earth.transform(pInert, eme2000, t);
                           return earth2Inert.transformVector(gp.getEast()).normalize();
                        },
                        4.0e-10, 1.0e-10);
    }

    @Test
    public void testAlignmentsStationEastField() {
        final Binary64Field field = Binary64Field.getInstance();
        doTestAlignment(field,
                        Vector3D.PLUS_K, new GroundPointTarget(station),
                        Vector3D.PLUS_I, PredefinedTarget.EAST,
                        t -> earth.getBodyFrame().getTransformTo(eme2000, t).transformPosition(station),
                        t -> {
                            final FieldTransform<Binary64> earth2Inert = earth.getBodyFrame().getTransformTo(eme2000, t);
                            final FieldVector3D<Binary64>  pInert      = getOrbit(field).shiftedBy(t.durationFrom(orbit.getDate())).getPosition();
                            final FieldGeodeticPoint<Binary64> gp      = earth.transform(pInert, eme2000, t);
                            return earth2Inert.transformVector(gp.getEast()).normalize();
                        },
                        4.0e-10, 1.0e-15);
    }

    @Test
    public void testDerivativesStationEast() {
        doTestDerivatives(Vector3D.PLUS_K, new GroundPointTarget(station),
                          Vector3D.PLUS_I, PredefinedTarget.EAST,
                          7.0e-13);
    }

    private void doTestAlignment(final Vector3D primarySat, final TargetProvider primaryTarget,
                                 final Vector3D secondarySat, final TargetProvider secondaryTarget,
                                 final Function<AbsoluteDate, Vector3D> referencePrimaryProvider,
                                 final Function<AbsoluteDate, Vector3D> referenceSecondaryProvider,
                                 final double primaryTolerance, final double secondaryTolerance) {

        final Propagator propagator = new KeplerianPropagator(orbit);
        propagator.setAttitudeProvider(new AlignedAndConstrained(primarySat, primaryTarget,
                                                                 secondarySat, secondaryTarget,
                                                                 sun, earth));

        propagator.getMultiplexer().add(60.0, state -> {
            final Vector3D satP        = state.getPVCoordinates().getPosition();
            final Vector3D primaryP    = referencePrimaryProvider.apply(state.getDate());
            final Vector3D secondaryP  = referenceSecondaryProvider.apply(state.getDate());
            final Transform inertToSat = state.toTransform();
            final Vector3D primaryDir;
            if (FastMath.abs(primaryP.getNorm() - 1.0) < 1.0e-10) {
                // reference is a unit vector
                primaryDir = primaryP;
            } else {
                // reference is a position
                primaryDir = primaryP.subtract(satP);
            }
            final Vector3D secondaryDir;
            if (FastMath.abs(secondaryP.getNorm() - 1.0) < 1.0e-10) {
                // reference is a unit vector
                secondaryDir = secondaryP;
            } else {
                // reference is a position
                secondaryDir = secondaryP.subtract(satP);
            }
            Assertions.assertEquals(0,
                                    Vector3D.angle(inertToSat.transformVector(primaryDir), primarySat),
                                    primaryTolerance);
            Assertions.assertEquals(0,
                                    Vector3D.angle(inertToSat.transformVector(Vector3D.crossProduct(primaryDir,
                                                                                                    secondaryDir)),
                                                   Vector3D.crossProduct(primarySat, secondarySat)),
                                    secondaryTolerance);
        });
        propagator.propagate(propagator.getInitialState().getDate().shiftedBy(3600));

    }

    private <T extends CalculusFieldElement<T>> void doTestAlignment(final Field<T> field,
                                                                     final Vector3D primarySat, final TargetProvider primaryTarget,
                                                                     final Vector3D secondarySat, final TargetProvider secondaryTarget,
                                                                     final Function<FieldAbsoluteDate<T>, FieldVector3D<T>> referencePrimaryProvider,
                                                                     final Function<FieldAbsoluteDate<T>, FieldVector3D<T>> referenceSecondaryProvider,
                                                                     final double primaryTolerance, final double secondaryTolerance) {

        final FieldVector3D<T> primarySatF   = new FieldVector3D<>(field, primarySat);
        final FieldVector3D<T> secondarySatF = new FieldVector3D<>(field, secondarySat);

        final FieldPropagator<T> propagator = new FieldKeplerianPropagator<>(getOrbit(field));
        propagator.setAttitudeProvider(new AlignedAndConstrained(primarySat, primaryTarget,
                                                                 secondarySat, secondaryTarget,
                                                                 sun, earth));

        propagator.getMultiplexer().add(field.getZero().newInstance(60.0), state -> {
            final FieldVector3D<T> satP        = state.getPVCoordinates().getPosition();
            final FieldVector3D<T> primaryP    = referencePrimaryProvider.apply(state.getDate());
            final FieldVector3D<T> secondaryP  = referenceSecondaryProvider.apply(state.getDate());
            final FieldTransform<T> inertToSat = state.toTransform();
            final FieldVector3D<T> primaryDir;
            if (FastMath.abs(primaryP.getNorm().getReal() - 1.0) < 1.0e-10) {
                // reference is a unit vector
                primaryDir = primaryP;
            } else {
                // reference is a position
                primaryDir = primaryP.subtract(satP);
            }
            final FieldVector3D<T> secondaryDir;
            if (FastMath.abs(secondaryP.getNorm().getReal() - 1.0) < 1.0e-10) {
                // reference is a unit vector
                secondaryDir = secondaryP;
            } else {
                // reference is a position
                secondaryDir = secondaryP.subtract(satP);
            }
            Assertions.assertEquals(0,
                                    FieldVector3D.angle(inertToSat.transformVector(primaryDir), primarySatF).getReal(),
                                    primaryTolerance);
            Assertions.assertEquals(0,
                                    FieldVector3D.angle(inertToSat.transformVector(FieldVector3D.crossProduct(primaryDir,
                                                                                                              secondaryDir)),
                                                        FieldVector3D.crossProduct(primarySatF, secondarySatF)).getReal(),
                                    secondaryTolerance);
        });
        propagator.propagate(propagator.getInitialState().getDate().shiftedBy(3600));

    }

    private void doTestDerivatives(final Vector3D primarySat, final TargetProvider primaryTarget,
                                   final Vector3D secondarySat, final TargetProvider secondaryTarget,
                                   final double tolerance) {

        final AlignedAndConstrained aac = new AlignedAndConstrained(primarySat, primaryTarget,
                                                                    secondarySat, secondaryTarget,
                                                                    sun, earth);

        // evaluate quaternion derivatives using finite differences
        final UnivariateVectorFunction q = dt -> {
            final Attitude attitude = aac.getAttitude(orbit.shiftedBy(dt), orbit.getDate().shiftedBy(dt), eme2000);
            final Rotation rotation = attitude.getRotation();
            return new double[] {
                rotation.getQ0(), rotation.getQ1(), rotation.getQ2(), rotation.getQ3()
            };
        };
        final UnivariateDerivative1[] qDot = new FiniteDifferencesDifferentiator(8, 0.1).
                                             differentiate(q).
                                             value(new UnivariateDerivative1(0.0, 1.0));

        // evaluate quaternions derivatives using internal model
        final FieldRotation<UnivariateDerivative1> r = aac.
                                                       getAttitude(orbit, orbit.getDate(), eme2000).
                                                       getOrientation().
                                                       toUnivariateDerivative1Rotation();

        Assertions.assertEquals(qDot[0].getDerivative(1), r.getQ0().getDerivative(1), tolerance);
        Assertions.assertEquals(qDot[1].getDerivative(1), r.getQ1().getDerivative(1), tolerance);
        Assertions.assertEquals(qDot[2].getDerivative(1), r.getQ2().getDerivative(1), tolerance);
        Assertions.assertEquals(qDot[3].getDerivative(1), r.getQ3().getDerivative(1), tolerance);

    }

    private <T extends CalculusFieldElement<T>> FieldOrbit<T> getOrbit(final Field<T> field) {
        return orbit.getType().convertToFieldOrbit(field, orbit);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        eme2000 = FramesFactory.getEME2000();
        orbit = new KeplerianOrbit(new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                                                     new Vector3D(0, 0, 3680.853673522056)),
                                   eme2000,
                                   new AbsoluteDate(2003, 3, 2, 13, 17, 7.865, TimeScalesFactory.getUTC()),
                                   3.986004415e14);
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        sun = CelestialBodyFactory.getSun();
        station = earth.transform(new GeodeticPoint(FastMath.toRadians(0.33871), FastMath.toRadians(6.73054), 0.0));
    }

    @AfterEach
    public void tearDown() {
        eme2000 = null;
        orbit   = null;
        earth   = null;
        sun     = null;
        station = null;
    }

    private Frame eme2000;
    private Orbit orbit;
    private CelestialBody sun;
    private OneAxisEllipsoid earth;
    private Vector3D station;

}
