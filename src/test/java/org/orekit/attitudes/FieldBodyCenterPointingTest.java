/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.attitudes;


import java.util.ArrayList;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.fitting.PolynomialCurveFitter;
import org.hipparchus.fitting.WeightedObservedPoint;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


public class FieldBodyCenterPointingTest {

    private double mu;

    /** Test if target is on Earth surface
     * @throws OrekitException
     */

    @Test
    public void testTarget() throws OrekitException{
        doTestTarget(Decimal64Field.getInstance());
    }
    @Test
    public void doxBodyCenterInPointingDirectionTest() throws OrekitException{
        doTestBodyCenterInPointingDirection(Decimal64Field.getInstance());
    }

    @Test
    public void testQDot() throws OrekitException{
        doTestQDot(Decimal64Field.getInstance());
    }

    @Test
    public void testSpin() throws OrekitException {
        doTestSpin(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>void doTestTarget(final Field<T> field) throws OrekitException {

        mu = 3.9860047e14;
        T zero = field.getZero();
        // Satellite position as circular parameters
        final T raan = zero.add(FastMath.toRadians(270.));
        final T a =zero.add(7178000.0);
        final T e =zero.add(7e-5);
        final T i =zero.add(FastMath.toRadians(50.));
        final T pa=zero.add(FastMath.toRadians(45.));
        final T m =zero.add(FastMath.toRadians(5.3-270));

     // Computation date
        FieldAbsoluteDate<T> date= new FieldAbsoluteDate<T>(field,new DateComponents(2008, 04, 07),
                        TimeComponents.H00,
                        TimeScalesFactory.getUTC());;
        // Orbit
        FieldKeplerianOrbit<T> circ = new FieldKeplerianOrbit<T>(a,e, i, pa, raan,
                        m, PositionAngle.MEAN,
                        FramesFactory.getEME2000(), date, mu);
        // WGS84 Earth model
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Earth center pointing attitude provider
        FieldBodyCenterPointing<T> earthCenterAttitudeLaw= new FieldBodyCenterPointing<T>(circ.getFrame(), earth);

        // Call get target method
        TimeStampedFieldPVCoordinates<T> target = earthCenterAttitudeLaw.getTargetPV(circ, date, circ.getFrame());

        // Check that target is on Earth surface
        GeodeticPoint gp = earth.transform(target.getPosition().toVector3D(), circ.getFrame(), date.toAbsoluteDate());

        Assert.assertEquals(0.0, gp.getAltitude(), 1.0e-8); //less precision because i suppose we are working with keplerian instead of circular
        Assert.assertEquals(date, target.getDate());

    }

    private <T extends RealFieldElement<T>> void doTestBodyCenterInPointingDirection(final Field<T> field) throws OrekitException {
        mu = 3.9860047e14;
        T zero = field.getZero();
        // Satellite position as circular parameters
        final T raan = zero.add(FastMath.toRadians(270.));
        final T a =zero.add(7178000.0);
        final T e =zero.add(7E-5);
        final T i =zero.add(FastMath.toRadians(50.));
        final T pa=zero.add(FastMath.toRadians(45.));

        final T m =zero.add(FastMath.toRadians(5.300-270.));

     // Computation date
        FieldAbsoluteDate<T> date= new FieldAbsoluteDate<T>(field,new DateComponents(2008, 04, 07),
                        TimeComponents.H00,
                        TimeScalesFactory.getUTC());;
        // Orbit
        FieldKeplerianOrbit<T> circ = new FieldKeplerianOrbit<T>(a,e, i, pa, raan,
                        m, PositionAngle.MEAN,
                        FramesFactory.getEME2000(), date, mu);

        // WGS84 Earth model
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Transform from EME2000 to ITRF2008
        Transform eme2000ToItrf = FramesFactory.getEME2000().getTransformTo(earth.getBodyFrame(), date.toAbsoluteDate());

        // Earth center pointing attitude provider
        FieldBodyCenterPointing<T> earthCenterAttitudeLaw= new FieldBodyCenterPointing<T>(circ.getFrame(), earth);
        // Transform satellite position to position/velocity parameters in EME2000 frame
        FieldPVCoordinates<T> pvSatEME2000 = circ.getPVCoordinates();

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
        FieldPVCoordinates<T> pvSatITRF2008C = eme2000ToItrf.transformPVCoordinates(pvSatEME2000);


       // Line containing satellite point and following pointing direction
        Line pointingLine = new Line(pvSatITRF2008C.getPosition().toVector3D(),
                                     pvSatITRF2008C.getPosition().toVector3D().add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      zSatITRF2008C.toVector3D()),
                                     2.0e-8);

        // Check that the line contains Earth center
        Assert.assertTrue(pointingLine.contains(Vector3D.ZERO));

    }

    private <T extends RealFieldElement<T>> void doTestQDot(final Field<T> field) throws OrekitException {

        T zero = field.getZero();
        // Satellite position as circular parameters
        final T raan = zero.add(FastMath.toRadians(270.));
        final T a =zero.add(7178000.0);
        final T e =zero.add(7e-5);
        final T i =zero.add(FastMath.toRadians(50.));
        final T pa=zero.add(FastMath.toRadians(45.));
        final T m =zero.add(FastMath.toRadians(5.3-270));

     // Computation date
        FieldAbsoluteDate<T> date_comp= new FieldAbsoluteDate<T>(field,new DateComponents(2008, 04, 07),
                        TimeComponents.H00,
                        TimeScalesFactory.getUTC());;
        // Orbit
        FieldKeplerianOrbit<T> circ = new FieldKeplerianOrbit<T>(a,e, i, pa, raan,
                        m, PositionAngle.MEAN,
                        FramesFactory.getEME2000(), date_comp, mu);
        // WGS84 Earth model
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        // Earth center pointing attitude provider
        FieldBodyCenterPointing<T> earthCenterAttitudeLaw= new FieldBodyCenterPointing<T>(circ.getFrame(), earth);


        Utils.setDataRoot("regular-data");
        final double ehMu  = 3.9860047e14;
        final double ae= 6.378137e6;
        final double c20_D= -1.08263e-3;
        final double c30_D= 2.54e-6;
        final double c40_D= 1.62e-6;
        final double c50_D= 2.3e-7;
        final double c60_D= -5.5e-7;
        
        T  c20  = zero.add( c20_D );
        T  c30  = zero.add( c30_D );
        T  c40  = zero.add( c40_D );
        T  c50  = zero.add( c50_D );
        T  c60  = zero.add( c60_D );
        
        final FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(584.);
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));
        final FieldCircularOrbit<T> initialOrbit = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                             FramesFactory.getEME2000(), date, ehMu);

        FieldEcksteinHechlerPropagator<T> propagator =
                new FieldEcksteinHechlerPropagator<T>(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);
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

        Assert.assertEquals(q0DotRef, q0Dot, 5.0e-9);
        Assert.assertEquals(q1DotRef, q1Dot, 5.0e-9);
        Assert.assertEquals(q2DotRef, q2Dot, 5.0e-9);
        Assert.assertEquals(q3DotRef, q3Dot, 5.0e-9);

    }

    private <T extends RealFieldElement<T>> void doTestSpin(final Field<T> field) throws OrekitException {

        T zero = field.getZero();
        // Satellite position as circular parameters
        final T raan = zero.add(FastMath.toRadians(270.));
        final T a =zero.add(7178000.0);
        final T e =zero.add(7e-5);
        final T i =zero.add(FastMath.toRadians(50.));
        final T pa=zero.add(FastMath.toRadians(45.));
        final T m =zero.add(FastMath.toRadians(5.3-270));

     // Computation date
        FieldAbsoluteDate<T> date_R = new FieldAbsoluteDate<T>(field,new DateComponents(2008, 04, 07),
                        TimeComponents.H00,
                        TimeScalesFactory.getUTC());;
        // Orbit
        FieldKeplerianOrbit<T> circ = new FieldKeplerianOrbit<T>(a,e, i, pa, raan,
                        m, PositionAngle.MEAN,
                        FramesFactory.getEME2000(), date_R, mu);
        // WGS84 Earth model
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));

       // Earth center pointing attitude provider
        FieldBodyCenterPointing<T> earthCenterAttitudeLaw= new FieldBodyCenterPointing<T>(circ.getFrame(), earth);


        Utils.setDataRoot("regular-data");
        final double ehMu  = 3.9860047e14;
        final double ae  = 6.378137e6;
        final double c20_D = -1.08263e-3;
        final double c30_D = 2.54e-6;
        final double c40_D = 1.62e-6;
        final double c50_D = 2.3e-7;
        final double c60_D = -5.5e-7;
        T  c20  = zero.add( c20_D );
        T  c30  = zero.add( c30_D );
        T  c40  = zero.add( c40_D );
        T  c50  = zero.add( c50_D );
        T  c60  = zero.add( c60_D );
        final FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(584.);
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));
        final FieldCircularOrbit<T> initialOrbit = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                             FramesFactory.getEME2000(), date, ehMu);

        FieldEcksteinHechlerPropagator<T> propagator =
                new FieldEcksteinHechlerPropagator<T>(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);
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
        Assert.assertEquals(0.0, errorAngleMinus.getReal(), 1.0e-6 * evolutionAngleMinus.getReal());
        T errorAnglePlus      = FieldRotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.shiftedBy(zero.add(-h)).getAttitude().getRotation());
        T evolutionAnglePlus  = FieldRotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAnglePlus.getReal(), 1.0e-6 * evolutionAnglePlus.getReal());

        FieldVector3D<T> spin0 = s0.getAttitude().getSpin();
        FieldVector3D<T> reference = FieldAngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
                                                             sPlus.getAttitude().getRotation(),
                                                             2 * h);
        Assert.assertTrue(spin0.getNorm().getReal() > 1.0e-3);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm().getReal(), 1.0e-13);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @After
    public void tearDown() {

    }

}

