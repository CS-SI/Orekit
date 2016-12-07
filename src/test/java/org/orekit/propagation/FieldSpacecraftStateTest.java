/* Copyright 2002-2016 CS Systèmesids d'Information
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
package org.orekit.propagation;


import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.FieldBodyCenterPointing;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class FieldSpacecraftStateTest {

    @Test
    public void doFieldVSRealTest() throws OrekitException, ParseException {
        testFieldVsReal(Decimal64Field.getInstance());
    }
    @Test
    public void doShiftErrorTest() throws OrekitException, ParseException {
        testShiftError(Decimal64Field.getInstance());
    }
    @Test(expected=IllegalArgumentException.class)
    public void doDatesConsistencyTest() throws OrekitException, ParseException {
        testDatesConsistency(Decimal64Field.getInstance());
    }
    @Test
    public void doDateConsistencyCloseTest() throws OrekitException, ParseException {
        testDateConsistencyClose(Decimal64Field.getInstance());
    }
    @Test(expected=IllegalArgumentException.class)
    public void doFramesConsistencyTest() throws OrekitException, ParseException {
        testFramesConsistency(Decimal64Field.getInstance());
    }
    @Test
    public void doTransformTest() throws OrekitException, ParseException {   
        testTransform(Decimal64Field.getInstance());
    }
    @Test
    public void doAdditionalStatesTest() throws OrekitException, ParseException {
        testAdditionalStates(Decimal64Field.getInstance());
    }

    public <T extends RealFieldElement<T>> void testFieldVsReal(final Field<T> field) throws OrekitException{
        T zero = field.getZero();

        double mu = 3.9860047e14;

        T a_f     = zero.add(150000);
        T e_f     = zero.add(     0);
        T i_f     = zero.add(     0);
        T pa_f    = zero.add(     0);
        T raan_f  = zero.add(     0);
        T m_f     = zero.add(     0);

        FieldAbsoluteDate<T> t_f = new FieldAbsoluteDate<T>(field);

        double a_r = a_f.getReal();
        double e_r = e_f.getReal();
        double i_r = i_f.getReal();
        double pa_r = pa_f.getReal();
        double raan_r = raan_f.getReal();
        double m_r = m_f.getReal();

        AbsoluteDate t_r = t_f.toAbsoluteDate();


        KeplerianOrbit      kep_r = new KeplerianOrbit(a_r, e_r, i_r, pa_r, raan_r, m_r, PositionAngle.ECCENTRIC, FramesFactory.getEME2000(), t_r, mu);
        FieldKeplerianOrbit<T> kep_f = new FieldKeplerianOrbit<T>(a_f,e_f,i_f,pa_f,raan_f, m_f, PositionAngle.ECCENTRIC, FramesFactory.getEME2000(), t_f, mu );

        SpacecraftState ScS_r = new SpacecraftState(kep_r);
        FieldSpacecraftState<T> ScS_f = new FieldSpacecraftState<T>(kep_f);

        for (double dt = 0; dt < 500; dt+=100){
            SpacecraftState control_r = ScS_r.shiftedBy(dt);
            FieldSpacecraftState<T> control_f = ScS_f.shiftedBy(zero.add(dt));


            Assert.assertEquals(control_r.getA(),control_f.getA().getReal(), 1e-10);
            Assert.assertEquals(control_r.getE(),control_f.getE().getReal(), 1e-10);
            Assert.assertEquals(control_r.getEquinoctialEx(),control_f.getEquinoctialEx().getReal(), 1e-10);
            Assert.assertEquals(control_r.getEquinoctialEy(),control_f.getEquinoctialEy().getReal(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getPosition().getX(),control_f.getPVCoordinates().toPVCoordinates().getPosition().getX(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getPosition().getY(),control_f.getPVCoordinates().toPVCoordinates().getPosition().getY(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getPosition().getZ(),control_f.getPVCoordinates().toPVCoordinates().getPosition().getZ(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getVelocity().getX(),control_f.getPVCoordinates().toPVCoordinates().getVelocity().getX(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getVelocity().getY(),control_f.getPVCoordinates().toPVCoordinates().getVelocity().getY(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getVelocity().getZ(),control_f.getPVCoordinates().toPVCoordinates().getVelocity().getZ(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getAcceleration().getX(),control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getX(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getAcceleration().getY(),control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getY(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getAcceleration().getZ(),control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getZ(), 1e-10);
            Assert.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ0(),control_f.getAttitude().getOrientation().getRotation().getQ0().getReal(),1e-10);
            Assert.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ1(),control_f.getAttitude().getOrientation().getRotation().getQ1().getReal(),1e-10);
            Assert.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ2(),control_f.getAttitude().getOrientation().getRotation().getQ2().getReal(),1e-10);
            Assert.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ3(),control_f.getAttitude().getOrientation().getRotation().getQ3().getReal(),1e-10);

            Assert.assertEquals(control_r.getAttitude().getSpin().getAlpha(),control_f.getAttitude().getSpin().getAlpha().getReal(),1e-10);
            Assert.assertEquals(control_r.getAttitude().getSpin().getDelta(),control_f.getAttitude().getSpin().getDelta().getReal(),1e-10);
            Assert.assertEquals(control_r.getAttitude().getSpin().getNorm(),control_f.getAttitude().getSpin().getNorm().getReal(),1e-10);

            Assert.assertEquals(control_r.getAttitude().getReferenceFrame().isPseudoInertial(),control_f.getAttitude().getReferenceFrame().isPseudoInertial());
            Assert.assertEquals(control_r.getAttitude().getDate().durationFrom(AbsoluteDate.J2000_EPOCH),control_f.getAttitude().getDate().durationFrom(AbsoluteDate.J2000_EPOCH).getReal(), 1e-10);


        }

    }

    public <T extends RealFieldElement<T>>  void testShiftError(final Field<T> field)
        throws ParseException, OrekitException {

        T zero = field.getZero();
        T mass = zero.add(2500.);
        T a = zero.add(rOrbit.getA());
        T e = zero.add(rOrbit.getE());
        T i = zero.add(rOrbit.getI());
        T pa = zero.add(1.9674147913622104);
        T raan = zero.add(FastMath.toRadians(261));
        T lv = zero.add(0);


        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field,new DateComponents(2004, 01, 01),
                                                 TimeComponents.H00,
                                                 TimeScalesFactory.getUTC());

        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<T>(a, e, i, pa, raan, lv, PositionAngle.TRUE,
                                  FramesFactory.getEME2000(), date, mu);

        FieldBodyCenterPointing<T> attitudeLaw = new FieldBodyCenterPointing<T>(orbit.getFrame(), earth);

        FieldKeplerianPropagator<T> propagator =
            new FieldKeplerianPropagator<T>(orbit, attitudeLaw, mu, mass);

        FieldAbsoluteDate<T> centerDate = orbit.getDate().shiftedBy(100.0);

        FieldSpacecraftState<T> centerState = propagator.propagate(centerDate);

        AbsoluteDate rCenterDate = centerDate.toAbsoluteDate();
        SpacecraftState rCenterState = rPropagator.propagate(rCenterDate);


        for (T dt = field.getZero(); dt.getReal() < 1100.0; dt =dt.add(5)) {



            SpacecraftState         rShifted = rCenterState.shiftedBy(dt.getReal());
            SpacecraftState         rPropagated = rPropagator.propagate(centerDate.shiftedBy(dt).toAbsoluteDate());

            FieldSpacecraftState<T> shifted = centerState.shiftedBy(dt);
            FieldSpacecraftState<T> propagated = propagator.propagate(centerDate.shiftedBy(dt));



            PVCoordinates        rdpv = new PVCoordinates(rPropagated.getPVCoordinates(),rShifted.getPVCoordinates());
            FieldPVCoordinates<T> dpv = new FieldPVCoordinates<T>(propagated.getPVCoordinates(), shifted.getPVCoordinates());


            double residualP = rdpv.getPosition().getNorm()     - dpv.getPosition().getNorm().getReal();
            double residualV = rdpv.getVelocity().getNorm()     - dpv.getVelocity().getNorm().getReal();
            double residualA = rdpv.getAcceleration().getNorm() - dpv.getAcceleration().getNorm().getReal();
            double residualR =  FastMath.toDegrees(Rotation.     distance(rShifted.getAttitude().getRotation(),
                                                                     rPropagated.getAttitude().getRotation())) -
                                FastMath.toDegrees(FieldRotation.distance(shifted.getAttitude().getRotation(),
                                                                    propagated.getAttitude().getRotation()).getReal());
            Assert.assertEquals(0, residualP, 5e-4);
            Assert.assertEquals(0, residualV, 5e-4);
            Assert.assertEquals(0, residualA, 5e-4);
            Assert.assertEquals(0, residualR, 5e-4);



        }







    }

    public <T extends RealFieldElement<T>> void testDatesConsistency(final Field<T> field) throws OrekitException {

        T zero = field.getZero();
        T a = zero.add(rOrbit.getA());
        T e = zero.add(rOrbit.getE());
        T i = zero.add(rOrbit.getI());
        T pa = zero.add(1.9674147913622104);
        T raan = zero.add(FastMath.toRadians(261));
        T lv = zero.add(0);


        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field,new DateComponents(2004, 01, 01),
                        TimeComponents.H00,
                        TimeScalesFactory.getUTC());

        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<T>(a, e, i, pa, raan, lv, PositionAngle.TRUE,
                        FramesFactory.getEME2000(), date, mu);
        FieldBodyCenterPointing<T> attitudeLaw = new FieldBodyCenterPointing<T>(orbit.getFrame(), earth);

        new FieldSpacecraftState<T>(orbit, attitudeLaw.getAttitude(orbit.shiftedBy(zero.add(10.0)),
                                                           orbit.getDate().shiftedBy(10.0), orbit.getFrame()));
    }

    /**
     * Check orbit and attitude dates can be off by a few ulps. I see this when using
     * FixedRate attitude provider.
     */
    public <T extends RealFieldElement<T>> void testDateConsistencyClose(final Field<T> field) throws OrekitException {


        //setup
        T zero = field.getZero();
        T one  = field.getOne();
        T a = zero.add(rOrbit.getA());
        T e = zero.add(rOrbit.getE());
        T i = zero.add(rOrbit.getI());
        T pa = zero.add(1.9674147913622104);
        T raan = zero.add(FastMath.toRadians(261));
        T lv = zero.add(0);

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field,new DateComponents(2004, 01, 01),
                        TimeComponents.H00,
                        TimeScalesFactory.getUTC());
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<T>(a, e, i, pa, raan, lv, PositionAngle.TRUE,
                        FramesFactory.getEME2000(), date, mu);

        FieldKeplerianOrbit<T> orbit10Shifts = orbit;
        for (int ii = 0; ii < 10; ii++) {
            orbit10Shifts = orbit10Shifts.shiftedBy(zero.add(0.1));
        }
        final FieldOrbit<T> orbit1Shift = orbit.shiftedBy(one);

        FieldBodyCenterPointing<T> attitudeLaw = new FieldBodyCenterPointing<T>(orbit.getFrame(), earth);


        FieldAttitude<T> shiftedAttitude = attitudeLaw
                .getAttitude(orbit1Shift, orbit1Shift.getDate(), orbit.getFrame());

        //verify dates are very close, but not equal
        Assert.assertNotEquals(shiftedAttitude.getDate(), orbit10Shifts.getDate());
        Assert.assertEquals(
                shiftedAttitude.getDate().durationFrom(orbit10Shifts.getDate()).getReal(),
                0, Precision.EPSILON);

        //action + verify no exception is thrown
        new FieldSpacecraftState<T>(orbit10Shifts, shiftedAttitude);
    }

    // (expected=IllegalArgumentException.class)
    public <T extends RealFieldElement<T>> void testFramesConsistency(final Field<T> field) throws OrekitException {

        T one = field.getOne();
        T zero = field.getZero();
        T a = zero.add(rOrbit.getA());
        T e = zero.add(rOrbit.getE());
        T i = zero.add(rOrbit.getI());
        T pa = zero.add(1.9674147913622104);
        T raan = zero.add(FastMath.toRadians(261));
        T lv = zero.add(0);

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field,new DateComponents(2004, 01, 01),
                        TimeComponents.H00,
                        TimeScalesFactory.getUTC());
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<T>(a, e, i, pa, raan, lv, PositionAngle.TRUE,
                        FramesFactory.getEME2000(), date, mu);

        new FieldSpacecraftState<T>(orbit,
                            new FieldAttitude<T>(orbit.getDate(),
                                         FramesFactory.getGCRF(),
                                         new FieldRotation<T>(one,zero,zero,zero, false),new FieldVector3D<T>(zero,zero,zero),new FieldVector3D<T>(zero,zero,zero)));
    }

    public <T extends RealFieldElement<T>> void testTransform(final Field<T> field)
        throws ParseException, OrekitException {

        T zero = field.getZero();
        T a = zero.add(rOrbit.getA());
        T e = zero.add(rOrbit.getE());
        T i = zero.add(rOrbit.getI());
        T pa = zero.add(1.9674147913622104);
        T raan = zero.add(FastMath.toRadians(261));
        T lv = zero.add(0);
        T mass = zero.add(2500);

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field,new DateComponents(2004, 01, 01),
                        TimeComponents.H00,
                        TimeScalesFactory.getUTC());

        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<T>(a, e, i, pa, raan, lv, PositionAngle.TRUE,
                        FramesFactory.getEME2000(), date, mu);

        FieldBodyCenterPointing<T> attitudeLaw = new FieldBodyCenterPointing<T>(orbit.getFrame(), earth);

        FieldKeplerianPropagator<T> propagator =
                        new FieldKeplerianPropagator<T>(orbit, attitudeLaw, mu, mass);

        double maxDP = 0;
        double maxDV = 0;
        double maxDA = 0;
        for (double t = 0; t < orbit.getKeplerianPeriod().getReal(); t += 60) {
            final FieldSpacecraftState<T> state = propagator.propagate(orbit.getDate().shiftedBy(zero.add(t)));
            final Transform transform = state.toSpacecraftState().toTransform().getInverse();
            PVCoordinates pv = transform.transformPVCoordinates(PVCoordinates.ZERO);
            PVCoordinates dPV = new PVCoordinates(pv, state.getPVCoordinates().toPVCoordinates());
            Vector3D mZDirection = transform.transformVector(Vector3D.MINUS_K);
            double alpha = Vector3D.angle(mZDirection, state.getPVCoordinates().toPVCoordinates().getPosition());
            maxDP = FastMath.max(maxDP, dPV.getPosition().getNorm());
            maxDV = FastMath.max(maxDV, dPV.getVelocity().getNorm());
            maxDA = FastMath.max(maxDA, FastMath.toDegrees(alpha));
        }
        Assert.assertEquals(0.0, maxDP, 1.0e-6);
        Assert.assertEquals(0.0, maxDV, 1.0e-9);
        Assert.assertEquals(0.0, maxDA, 1.0e-12);

    }

    public <T extends RealFieldElement<T>> void testAdditionalStates(final Field<T> field) throws OrekitException {

        T zero = field.getZero();
        T a = zero.add(rOrbit.getA());
        T e = zero.add(rOrbit.getE());
        T i = zero.add(rOrbit.getI());
        T pa = zero.add(1.9674147913622104);
        T raan = zero.add(FastMath.toRadians(261));
        T lv = zero.add(0);
        T mass = zero.add(2500);

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field,new DateComponents(2004, 01, 01),
                        TimeComponents.H00,
                        TimeScalesFactory.getUTC());

        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<T>(a, e, i, pa, raan, lv, PositionAngle.TRUE,
                        FramesFactory.getEME2000(), date, mu);

        FieldBodyCenterPointing<T> attitudeLaw = new FieldBodyCenterPointing<T>(orbit.getFrame(), earth);

        FieldKeplerianPropagator<T> propagator =
                        new FieldKeplerianPropagator<T>(orbit, attitudeLaw, mu, mass);




        final FieldSpacecraftState<T> state = propagator.propagate(orbit.getDate().shiftedBy(60));
        T[] add = MathArrays.buildArray(field, 2);
        add[0] = zero.add(1.);
        add[1] = zero.add(2.);
        final FieldSpacecraftState<T> extended =
                state.
                 addAdditionalState("test-1", add).
                  addAdditionalState("test-2", zero.add(42.0));
        Assert.assertEquals(0, state.getAdditionalStates().size());
        Assert.assertFalse(state.hasAdditionalState("test-1"));
        try {
            state.getAdditionalState("test-1");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assert.assertEquals(oe.getParts()[0], "test-1");
        }
        try {
            state.ensureCompatibleAdditionalStates(extended);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assert.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(state);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assert.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            T[] kk = MathArrays.buildArray(field, 7);
            extended.ensureCompatibleAdditionalStates(extended.addAdditionalState("test-2",kk));
            Assert.fail("an exception should have been thrown");
        } catch (MathIllegalStateException mise) {
            Assert.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH, mise.getSpecifier());
            Assert.assertEquals(7, ((Integer) mise.getParts()[0]).intValue());
        }
        Assert.assertEquals(2, extended.getAdditionalStates().size());
        Assert.assertTrue(extended.hasAdditionalState("test-1"));
        Assert.assertTrue(extended.hasAdditionalState("test-2"));
        Assert.assertEquals( 1.0, extended.getAdditionalState("test-1")[0].getReal(), 1.0e-15);
        Assert.assertEquals( 2.0, extended.getAdditionalState("test-1")[1].getReal(), 1.0e-15);
        Assert.assertEquals(42.0, extended.getAdditionalState("test-2")[0].getReal(), 1.0e-15);

        // test various constructors
        T[] dd = MathArrays.buildArray(field, 1);
        dd[0] = zero.add(-6.0);
        Map<String, T[]> map = new HashMap<String, T[]>();
        map.put("test-3",dd);
        FieldSpacecraftState<T> sO = new FieldSpacecraftState<T>(state.getOrbit(), map);
        Assert.assertEquals(-6.0, sO.getAdditionalState("test-3")[0].getReal(), 1.0e-15);
        FieldSpacecraftState<T> sOA = new FieldSpacecraftState<T>(state.getOrbit(), state.getAttitude(), map);
        Assert.assertEquals(-6.0, sOA.getAdditionalState("test-3")[0].getReal(), 1.0e-15);
        FieldSpacecraftState<T> sOM = new FieldSpacecraftState<T>(state.getOrbit(), state.getMass(), map);
        Assert.assertEquals(-6.0, sOM.getAdditionalState("test-3")[0].getReal(), 1.0e-15);
        FieldSpacecraftState<T> sOAM = new FieldSpacecraftState<T>(state.getOrbit(), state.getAttitude(), state.getMass(), map);
        Assert.assertEquals(-6.0, sOAM.getAdditionalState("test-3")[0].getReal(), 1.0e-15);

    }

    @Before
    public void setUp(){
        try {

        Utils.setDataRoot("regular-data");
        mu  = 3.9860047e14;

        rMass = 2500;
        double a = 7187990.1979844316;
        double  e =0.5e-4;
        double i =     1.7105407051081795;
        double omega = 1.9674147913622104;
        double OMEGA = FastMath.toRadians(261);
        double lv =    0;

        rDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                 TimeComponents.H00,
                                                 TimeScalesFactory.getUTC());
        rOrbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), rDate, mu);
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        rAttitudeLaw = new BodyCenterPointing(rOrbit.getFrame(), earth);
        rPropagator =
            new KeplerianPropagator(rOrbit, rAttitudeLaw, mu, rMass);

        } catch (OrekitException oe) {

            Assert.fail(oe.getLocalizedMessage());
        }
    }

    private AbsoluteDate rDate;
    private double mu;
    private double rMass;
    private Orbit rOrbit;
    private AttitudeProvider rAttitudeLaw;
    private Propagator rPropagator;
    private OneAxisEllipsoid earth;

}












