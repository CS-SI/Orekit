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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.fitting.PolynomialCurveFitter;
import org.hipparchus.fitting.WeightedObservedPoint;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;


public class BodyCenterPointingTest {

    // Computation date
    private AbsoluteDate date;

    // Orbit
    private CircularOrbit circ;

    // WGS84 Earth model
    private OneAxisEllipsoid earth;

    // Transform from EME2000 to ITRF2008
    private Transform eme2000ToItrf;

    // Earth center pointing attitude provider
    private BodyCenterPointing earthCenterAttitudeLaw;

    /** Test if target is on Earth surface
     */
    @Test
    public void testTarget() {

        // Call get target method
        TimeStampedPVCoordinates target = earthCenterAttitudeLaw.getTargetPV(circ, date, circ.getFrame());

        // Check that target is on Earth surface
        GeodeticPoint gp = earth.transform(target.getPosition(), circ.getFrame(), date);
        Assertions.assertEquals(0.0, gp.getAltitude(), 1.0e-10);
        Assertions.assertEquals(date, target.getDate());

    }

    /** Test if body center belongs to the direction pointed by the satellite
     */
    @Test
    public void testBodyCenterInPointingDirection() {

        // Transform satellite position to position/velocity parameters in EME2000 frame
        PVCoordinates pvSatEME2000 = circ.getPVCoordinates();

        //  Pointing direction
        // ********************
        // Get satellite attitude rotation, i.e rotation from EME2000 frame to satellite frame
        Rotation rotSatEME2000 = earthCenterAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Transform Z axis from satellite frame to EME2000
        Vector3D zSatEME2000 = rotSatEME2000.applyInverseTo(Vector3D.PLUS_K);

        // Transform Z axis from EME2000 to ITRF2008
        Vector3D zSatITRF2008C = eme2000ToItrf.transformVector(zSatEME2000);

        // Transform satellite position/velocity from EME2000 to ITRF2008
        PVCoordinates pvSatITRF2008C = eme2000ToItrf.transformPVCoordinates(pvSatEME2000);

       // Line containing satellite point and following pointing direction
        Line pointingLine = new Line(pvSatITRF2008C.getPosition(),
                                     pvSatITRF2008C.getPosition().add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      zSatITRF2008C),
                                     2.0e-8);

        // Check that the line contains Earth center
        Assertions.assertTrue(pointingLine.contains(Vector3D.ZERO));

    }

    @Test
    public void testQDot() {

        Utils.setDataRoot("regular-data");
        final double ehMu  = 3.9860047e14;
        final double ae  = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
        final CircularOrbit initialOrbit = new CircularOrbit(new PVCoordinates(position, velocity),
                                                             FramesFactory.getEME2000(), date, ehMu);

        EcksteinHechlerPropagator propagator =
                new EcksteinHechlerPropagator(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);
        propagator.setAttitudeProvider(earthCenterAttitudeLaw);

        List<WeightedObservedPoint> w0 = new ArrayList<WeightedObservedPoint>();
        List<WeightedObservedPoint> w1 = new ArrayList<WeightedObservedPoint>();
        List<WeightedObservedPoint> w2 = new ArrayList<WeightedObservedPoint>();
        List<WeightedObservedPoint> w3 = new ArrayList<WeightedObservedPoint>();
        for (double dt = -1; dt < 1; dt += 0.01) {
            Rotation rP = propagator.propagate(date.shiftedBy(dt)).getAttitude().getRotation();
            w0.add(new WeightedObservedPoint(1, dt, rP.getQ0()));
            w1.add(new WeightedObservedPoint(1, dt, rP.getQ1()));
            w2.add(new WeightedObservedPoint(1, dt, rP.getQ2()));
            w3.add(new WeightedObservedPoint(1, dt, rP.getQ3()));
        }

        double q0DotRef = PolynomialCurveFitter.create(2).fit(w0)[1];
        double q1DotRef = PolynomialCurveFitter.create(2).fit(w1)[1];
        double q2DotRef = PolynomialCurveFitter.create(2).fit(w2)[1];
        double q3DotRef = PolynomialCurveFitter.create(2).fit(w3)[1];

        Attitude a0 = propagator.propagate(date).getAttitude();
        double   q0 = a0.getRotation().getQ0();
        double   q1 = a0.getRotation().getQ1();
        double   q2 = a0.getRotation().getQ2();
        double   q3 = a0.getRotation().getQ3();
        double   oX = a0.getSpin().getX();
        double   oY = a0.getSpin().getY();
        double   oZ = a0.getSpin().getZ();

        // first time-derivatives of the quaternion
        double q0Dot = 0.5 * MathArrays.linearCombination(-q1, oX, -q2, oY, -q3, oZ);
        double q1Dot = 0.5 * MathArrays.linearCombination( q0, oX, -q3, oY,  q2, oZ);
        double q2Dot = 0.5 * MathArrays.linearCombination( q3, oX,  q0, oY, -q1, oZ);
        double q3Dot = 0.5 * MathArrays.linearCombination(-q2, oX,  q1, oY,  q0, oZ);

        Assertions.assertEquals(q0DotRef, q0Dot, 5.0e-9);
        Assertions.assertEquals(q1DotRef, q1Dot, 5.0e-9);
        Assertions.assertEquals(q2DotRef, q2Dot, 5.0e-9);
        Assertions.assertEquals(q3DotRef, q3Dot, 5.0e-9);

    }

    @Test
    public void testSpin() {

        Utils.setDataRoot("regular-data");
        final double ehMu  = 3.9860047e14;
        final double ae  = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
        final CircularOrbit initialOrbit = new CircularOrbit(new PVCoordinates(position, velocity),
                                                             FramesFactory.getEME2000(), date, ehMu);

        EcksteinHechlerPropagator propagator =
                new EcksteinHechlerPropagator(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);
        propagator.setAttitudeProvider(earthCenterAttitudeLaw);

        double h = 0.01;
        SpacecraftState s0     = propagator.propagate(date);
        SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        SpacecraftState sPlus  = propagator.propagate(date.shiftedBy(h));

        // check spin is consistent with attitude evolution
        double errorAngleMinus     = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        double evolutionAngleMinus = Rotation.distance(sMinus.getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        Assertions.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        double errorAnglePlus      = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.shiftedBy(-h).getAttitude().getRotation());
        double evolutionAnglePlus  = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.getAttitude().getRotation());
        Assertions.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        Vector3D spin0 = s0.getAttitude().getSpin();
        Vector3D reference = AngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
                                                             sPlus.getAttitude().getRotation(),
                                                             2 * h);
        Assertions.assertTrue(spin0.getNorm() > 1.0e-3);
        Assertions.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.0e-13);

    }

    @Test
    public void testTargetField() {
        doTestTarget(Binary64Field.getInstance());
    }
    @Test
    public void doxBodyCenterInPointingDirectionTest() {
        doTestBodyCenterInPointingDirection(Binary64Field.getInstance());
    }

    @Test
    public void testQDotField() {
        doTestQDot(Binary64Field.getInstance());
    }

    @Test
    public void testSpinField() {
        doTestSpin(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>>void doTestTarget(final Field<T> field) {

        T mu = field.getZero().add(3.9860047e14);
        T zero = field.getZero();
        // Satellite position as circular parameters
        final T raan = zero.add(FastMath.toRadians(270.));
        final T a =zero.add(7178000.0);
        final T e =zero.add(7e-5);
        final T i =zero.add(FastMath.toRadians(50.));
        final T pa=zero.add(FastMath.toRadians(45.));
        final T m =zero.add(FastMath.toRadians(5.3-270));

     // Computation date
        FieldAbsoluteDate<T> date= new FieldAbsoluteDate<>(field, new DateComponents(2008, 04, 07),
                                                           TimeComponents.H00,
                                                           TimeScalesFactory.getUTC());
        // Orbit
        FieldKeplerianOrbit<T> circ = new FieldKeplerianOrbit<>(a, e, i, pa, raan,
                                                                m, PositionAngleType.MEAN,
                                                                FramesFactory.getEME2000(), date, mu);
        // WGS84 Earth model
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Earth center pointing attitude provider
        BodyCenterPointing earthCenterAttitudeLaw= new BodyCenterPointing(circ.getFrame(), earth);

        // Call get target method
        TimeStampedFieldPVCoordinates<T> target = earthCenterAttitudeLaw.getTargetPV(circ, date, circ.getFrame());

        // Check that target is on Earth surface
        GeodeticPoint gp = earth.transform(target.getPosition().toVector3D(), circ.getFrame(), date.toAbsoluteDate());

        Assertions.assertEquals(0.0, gp.getAltitude(), 1.0e-8); //less precision because i suppose we are working with Keplerian instead of circular
        Assertions.assertEquals(date, target.getDate());

    }

    private <T extends CalculusFieldElement<T>> void doTestBodyCenterInPointingDirection(final Field<T> field)  {

        T zero = field.getZero();
        T mu = zero.add(3.9860047e14);
        // Satellite position as circular parameters
        final T raan = zero.add(FastMath.toRadians(270.));
        final T a =zero.add(7178000.0);
        final T e =zero.add(7E-5);
        final T i =zero.add(FastMath.toRadians(50.));
        final T pa=zero.add(FastMath.toRadians(45.));

        final T m =zero.add(FastMath.toRadians(5.300-270.));

     // Computation date
        FieldAbsoluteDate<T> date= new FieldAbsoluteDate<>(field, new DateComponents(2008, 04, 07),
                                                           TimeComponents.H00,
                                                           TimeScalesFactory.getUTC());
        // Orbit
        FieldKeplerianOrbit<T> circ = new FieldKeplerianOrbit<>(a, e, i, pa, raan,
                                                                m, PositionAngleType.MEAN,
                                                                FramesFactory.getEME2000(), date, mu);

        // WGS84 Earth model
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Transform from EME2000 to ITRF2008
        StaticTransform eme2000ToItrf = FramesFactory.getEME2000().getStaticTransformTo(earth.getBodyFrame(), date.toAbsoluteDate());

        // Earth center pointing attitude provider
        BodyCenterPointing earthCenterAttitudeLaw= new BodyCenterPointing(circ.getFrame(), earth);
        // Transform satellite position to position/velocity parameters in EME2000 frame
        FieldVector3D<T> positionSatEME2000 = circ.getPosition();

        //  Pointing direction
        // ********************
        // Get satellite attitude rotation, i.e rotation from EME2000 frame to satellite frame
        FieldRotation<T> rotSatEME2000 = earthCenterAttitudeLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        //checked this values with the bodycenterpointing test values
        // Transform Z axis from satellite frame to EME2000
        FieldVector3D<T> zSatEME2000 = rotSatEME2000.applyInverseTo(Vector3D.PLUS_K);

        // Transform Z axis from EME2000 to ITRF2008
        FieldVector3D<T> zSatITRF2008C = eme2000ToItrf.transformVector(zSatEME2000);

        // Transform satellite position/velocity from EME2000 to ITRF2008
        FieldVector3D<T> positionSatITRF2008C = eme2000ToItrf.transformPosition(positionSatEME2000);


       // Line containing satellite point and following pointing direction
        Line pointingLine = new Line(positionSatITRF2008C.toVector3D(),
                                     positionSatITRF2008C.toVector3D().add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      zSatITRF2008C.toVector3D()),
                                     2.0e-8);

        // Check that the line contains Earth center
        Assertions.assertTrue(pointingLine.contains(Vector3D.ZERO));

    }

    private <T extends CalculusFieldElement<T>> void doTestQDot(final Field<T> field) {

        final double ae  = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;

        // Satellite position as circular parameters
        T zero = field.getZero();
        final T a     = zero.add(7178000.0);
        final T e     = zero.add(7e-5);
        final T i     = zero.add(FastMath.toRadians(50.));
        final T pa    = zero.add(FastMath.toRadians(45.));
        final T raan  = zero.add(FastMath.toRadians(270.));
        final T m     = zero.add(FastMath.toRadians(5.3-270));
        final T ehMu  = zero.add(3.9860047e14);

     // Computation date
        FieldAbsoluteDate<T> date_comp= new FieldAbsoluteDate<>(field, new DateComponents(2008, 04, 07),
                                                                TimeComponents.H00,
                                                                TimeScalesFactory.getUTC());
        // Orbit
        FieldKeplerianOrbit<T> circ = new FieldKeplerianOrbit<>(a, e, i, pa, raan,
                                                                m, PositionAngleType.MEAN,
                                                                FramesFactory.getEME2000(), date_comp, ehMu);
        // WGS84 Earth model
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        // Earth center pointing attitude provider
        BodyCenterPointing earthCenterAttitudeLaw= new BodyCenterPointing(circ.getFrame(), earth);


        final FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(584.);
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));
        final FieldCircularOrbit<T> initialOrbit = new FieldCircularOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                             FramesFactory.getEME2000(), date, ehMu);

        FieldEcksteinHechlerPropagator<T> propagator =
                new FieldEcksteinHechlerPropagator<>(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);
        propagator.setAttitudeProvider(earthCenterAttitudeLaw);

        List<WeightedObservedPoint> w0 = new ArrayList<WeightedObservedPoint>();
        List<WeightedObservedPoint> w1 = new ArrayList<WeightedObservedPoint>();
        List<WeightedObservedPoint> w2 = new ArrayList<WeightedObservedPoint>();
        List<WeightedObservedPoint> w3 = new ArrayList<WeightedObservedPoint>();
        for (double dt = -1; dt < 1; dt += 0.01) {
            FieldRotation<T> rP = propagator.propagate(date.shiftedBy(dt)).getAttitude().getRotation();
            w0.add(new WeightedObservedPoint(1, dt, rP.getQ0().getReal()));
            w1.add(new WeightedObservedPoint(1, dt, rP.getQ1().getReal()));
            w2.add(new WeightedObservedPoint(1, dt, rP.getQ2().getReal()));
            w3.add(new WeightedObservedPoint(1, dt, rP.getQ3().getReal()));
        }

        double q0DotRef = PolynomialCurveFitter.create(2).fit(w0)[1];
        double q1DotRef = PolynomialCurveFitter.create(2).fit(w1)[1];
        double q2DotRef = PolynomialCurveFitter.create(2).fit(w2)[1];
        double q3DotRef = PolynomialCurveFitter.create(2).fit(w3)[1];

        FieldAttitude<T> a0 = propagator.propagate(date).getAttitude();
        T   q0 = a0.getRotation().getQ0();
        T   q1 = a0.getRotation().getQ1();
        T   q2 = a0.getRotation().getQ2();
        T   q3 = a0.getRotation().getQ3();
        T   oX = a0.getSpin().getX();
        T   oY = a0.getSpin().getY();
        T   oZ = a0.getSpin().getZ();

        // first time-derivatives of the quaternion
        double q0Dot = 0.5 * MathArrays.linearCombination(-q1.getReal(), oX.getReal(), -q2.getReal(), oY.getReal(), -q3.getReal(), oZ.getReal());
        double q1Dot = 0.5 * MathArrays.linearCombination( q0.getReal(), oX.getReal(), -q3.getReal(), oY.getReal(),  q2.getReal(), oZ.getReal());
        double q2Dot = 0.5 * MathArrays.linearCombination( q3.getReal(), oX.getReal(),  q0.getReal(), oY.getReal(), -q1.getReal(), oZ.getReal());
        double q3Dot = 0.5 * MathArrays.linearCombination(-q2.getReal(), oX.getReal(),  q1.getReal(), oY.getReal(),  q0.getReal(), oZ.getReal());

        Assertions.assertEquals(q0DotRef, q0Dot, 5.0e-9);
        Assertions.assertEquals(q1DotRef, q1Dot, 5.0e-9);
        Assertions.assertEquals(q2DotRef, q2Dot, 5.0e-9);
        Assertions.assertEquals(q3DotRef, q3Dot, 5.0e-9);

    }

    private <T extends CalculusFieldElement<T>> void doTestSpin(final Field<T> field) {

        final double ae  = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;

        // Satellite position as circular parameters
        final T zero  = field.getZero();
        final T a     = zero.add(7178000.0);
        final T e     = zero.add(7e-5);
        final T i     = zero.add(FastMath.toRadians(50.));
        final T pa    = zero.add(FastMath.toRadians(45.));
        final T raan  = zero.add(FastMath.toRadians(270.));
        final T m     = zero.add(FastMath.toRadians(5.3-270));
        final T ehMu  = zero.add(3.9860047e14);

        // Computation date
        FieldAbsoluteDate<T> date_R = new FieldAbsoluteDate<>(field, new DateComponents(2008, 04, 07),
                                                              TimeComponents.H00,
                                                              TimeScalesFactory.getUTC());
        // Orbit
        FieldKeplerianOrbit<T> circ = new FieldKeplerianOrbit<>(a, e, i, pa, raan,
                                                                m, PositionAngleType.MEAN,
                                                                FramesFactory.getEME2000(), date_R, ehMu);
        // WGS84 Earth model
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));

       // Earth center pointing attitude provider
        BodyCenterPointing earthCenterAttitudeLaw= new BodyCenterPointing(circ.getFrame(), earth);


        final FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(584.);
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));
        final FieldCircularOrbit<T> initialOrbit = new FieldCircularOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                             FramesFactory.getEME2000(), date, ehMu);

        FieldEcksteinHechlerPropagator<T> propagator =
                new FieldEcksteinHechlerPropagator<>(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);
        propagator.setAttitudeProvider(earthCenterAttitudeLaw);

        double h = 0.01;

        FieldSpacecraftState<T> s0     = propagator.propagate(date);
        FieldSpacecraftState<T> sMinus = propagator.propagate(date.shiftedBy(-h));
        FieldSpacecraftState<T> sPlus  = propagator.propagate(date.shiftedBy(h));

        // check spin is consistent with attitude evolution
        T errorAngleMinus     = FieldRotation.distance(sMinus.shiftedBy(zero.add(h)).getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        T evolutionAngleMinus = FieldRotation.distance(sMinus.getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        Assertions.assertEquals(0.0, errorAngleMinus.getReal(), 1.0e-6 * evolutionAngleMinus.getReal());
        T errorAnglePlus      = FieldRotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.shiftedBy(zero.add(-h)).getAttitude().getRotation());
        T evolutionAnglePlus  = FieldRotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.getAttitude().getRotation());
        Assertions.assertEquals(0.0, errorAnglePlus.getReal(), 1.0e-6 * evolutionAnglePlus.getReal());

        FieldVector3D<T> spin0 = s0.getAttitude().getSpin();
        FieldVector3D<T> reference = FieldAngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
                                                             sPlus.getAttitude().getRotation(),
                                                             2 * h);
        Assertions.assertTrue(spin0.getNorm().getReal() > 1.0e-3);
        Assertions.assertEquals(0.0, spin0.subtract(reference).getNorm().getReal(), 1.1e-13);

    }

    @BeforeEach
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");

            // Computation date
            date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC());

            // Satellite position as circular parameters
            final double mu = 3.9860047e14;
            final double raan = 270.;
            circ =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(raan),
                                       FastMath.toRadians(5.300 - raan), PositionAngleType.MEAN,
                                       FramesFactory.getEME2000(), date, mu);


            // WGS84 Earth model
            earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true));

            // Transform from EME2000 to ITRF2008
            eme2000ToItrf = FramesFactory.getEME2000().getTransformTo(earth.getBodyFrame(), date);

            // Create earth center pointing attitude provider
            earthCenterAttitudeLaw = new BodyCenterPointing(circ.getFrame(), earth);

        } catch (OrekitException oe) {
            Assertions.fail(oe.getMessage());
        }

    }

    @AfterEach
    public void tearDown() {
        date = null;
        earth = null;
        eme2000ToItrf = null;
        earthCenterAttitudeLaw = null;
        circ = null;
    }

}

