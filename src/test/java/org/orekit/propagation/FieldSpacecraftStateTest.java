/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.attitudes.FieldAttitude;
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
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class FieldSpacecraftStateTest {

    @Test
    public void testFieldVSReal() {
        doTestFieldVsReal(Decimal64Field.getInstance());
    }

    @Test
    public void testShiftVsEcksteinHechlerError() {
        doTestShiftVsEcksteinHechlerError(Decimal64Field.getInstance());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDatesConsistency() {
        doTestDatesConsistency(Decimal64Field.getInstance());
    }

    @Test
    public void testDateConsistencyClose() {
        doTestDateConsistencyClose(Decimal64Field.getInstance());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFramesConsistency() {
        doTestFramesConsistency(Decimal64Field.getInstance());
    }

    @Test
    public void testTransform() {
        doTestTransform(Decimal64Field.getInstance());
    }

    @Test
    public void testAdditionalStates() {
        doTestAdditionalStates(Decimal64Field.getInstance());
    }

    @Test
    public void testInterpolation() throws ParseException {
        doTestInterpolation(Decimal64Field.getInstance());
    }

    @Test
    public void testFieldVSRealAbsPV() {
        doTestFieldVsRealAbsPV(Decimal64Field.getInstance());
    }

    @Test
    public void testDateConsistencyCloseAbsPV() {
        doTestDateConsistencyCloseAbsPV(Decimal64Field.getInstance());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFramesConsistencyAbsPV() {
        doTestFramesConsistencyAbsPV(Decimal64Field.getInstance());
    }

    @Test
    public void testAdditionalStatesAbsPV() {
        doTestAdditionalStatesAbsPV(Decimal64Field.getInstance());
    }

    @Test
    public void testResetOnEventAnalytical() {
        doTestAdditionalTestResetOnEventAnalytical(Decimal64Field.getInstance());
    }

    @Test
    public void testResetOnEventNumerical() {
        doTestAdditionalTestResetOnEventNumerical(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldVsReal(final Field<T> field) {
        T zero = field.getZero();

        double mu = 3.9860047e14;

        T a_f     = zero.add(150000);
        T e_f     = zero.add(     0);
        T i_f     = zero.add(     0);
        T pa_f    = zero.add(     0);
        T raan_f  = zero.add(     0);
        T m_f     = zero.add(     0);

        FieldAbsoluteDate<T> t_f = new FieldAbsoluteDate<>(field);

        double a_r = a_f.getReal();
        double e_r = e_f.getReal();
        double i_r = i_f.getReal();
        double pa_r = pa_f.getReal();
        double raan_r = raan_f.getReal();
        double m_r = m_f.getReal();

        AbsoluteDate t_r = t_f.toAbsoluteDate();


        KeplerianOrbit      kep_r = new KeplerianOrbit(a_r, e_r, i_r, pa_r, raan_r, m_r, PositionAngle.ECCENTRIC, FramesFactory.getEME2000(), t_r, mu);
        FieldKeplerianOrbit<T> kep_f = new FieldKeplerianOrbit<>(a_f, e_f, i_f, pa_f, raan_f, m_f, PositionAngle.ECCENTRIC, FramesFactory.getEME2000(), t_f, zero.add(mu));

        SpacecraftState ScS_r = new SpacecraftState(kep_r);
        FieldSpacecraftState<T> ScS_f = new FieldSpacecraftState<>(kep_f);

        for (double dt = 0; dt < 500; dt+=100){
            SpacecraftState control_r = ScS_r.shiftedBy(dt);
            FieldSpacecraftState<T> control_f = ScS_f.shiftedBy(zero.add(dt));


            Assert.assertEquals(control_r.getA(), control_f.getA().getReal(), 1e-10);
            Assert.assertEquals(control_r.getE(), control_f.getE().getReal(), 1e-10);
            Assert.assertEquals(control_r.getEquinoctialEx(), control_f.getEquinoctialEx().getReal(), 1e-10);
            Assert.assertEquals(control_r.getEquinoctialEy(), control_f.getEquinoctialEy().getReal(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getPosition().getX(), control_f.getPVCoordinates().toPVCoordinates().getPosition().getX(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getPosition().getY(), control_f.getPVCoordinates().toPVCoordinates().getPosition().getY(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getPosition().getZ(), control_f.getPVCoordinates().toPVCoordinates().getPosition().getZ(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getVelocity().getX(), control_f.getPVCoordinates().toPVCoordinates().getVelocity().getX(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getVelocity().getY(), control_f.getPVCoordinates().toPVCoordinates().getVelocity().getY(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getVelocity().getZ(), control_f.getPVCoordinates().toPVCoordinates().getVelocity().getZ(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getAcceleration().getX(), control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getX(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getAcceleration().getY(), control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getY(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getAcceleration().getZ(), control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getZ(), 1e-10);
            Assert.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ0(), control_f.getAttitude().getOrientation().getRotation().getQ0().getReal(), 1e-10);
            Assert.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ1(), control_f.getAttitude().getOrientation().getRotation().getQ1().getReal(), 1e-10);
            Assert.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ2(), control_f.getAttitude().getOrientation().getRotation().getQ2().getReal(), 1e-10);
            Assert.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ3(), control_f.getAttitude().getOrientation().getRotation().getQ3().getReal(), 1e-10);

            Assert.assertEquals(control_r.getAttitude().getSpin().getAlpha(), control_f.getAttitude().getSpin().getAlpha().getReal(), 1e-10);
            Assert.assertEquals(control_r.getAttitude().getSpin().getDelta(), control_f.getAttitude().getSpin().getDelta().getReal(), 1e-10);
            Assert.assertEquals(control_r.getAttitude().getSpin().getNorm(), control_f.getAttitude().getSpin().getNorm().getReal(), 1e-10);

            Assert.assertEquals(control_r.getAttitude().getReferenceFrame().isPseudoInertial(), control_f.getAttitude().getReferenceFrame().isPseudoInertial());
            Assert.assertEquals(control_r.getAttitude().getDate().durationFrom(AbsoluteDate.J2000_EPOCH), control_f.getAttitude().getDate().durationFrom(AbsoluteDate.J2000_EPOCH).getReal(), 1e-10);


        }

    }

    private <T extends RealFieldElement<T>>  void doTestShiftVsEcksteinHechlerError(final Field<T> field)
        {

        T zero = field.getZero();
        T mass = zero.add(2500.);
        T a = zero.add(rOrbit.getA());
        T e = zero.add(rOrbit.getE());
        T i = zero.add(rOrbit.getI());
        T pa = zero.add(1.9674147913622104);
        T raan = zero.add(FastMath.toRadians(261));
        T lv = zero.add(0);
        final double ae  = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 =  2.54e-6;
        final double c40 =  1.62e-6;
        final double c50 =  2.3e-7;
        final double c60 =  -5.5e-7;


        // polynomial models for interpolation error in position, velocity, acceleration and attitude
        // these models grow as follows
        //   interpolation time (s)    position error (m)   velocity error (m/s)   acceleration error (m/s²)  attitude error (°)
        //           60                        2                    0.07                  0.002               0.00002
        //          120                       12                    0.3                   0.005               0.00009
        //          300                      170                    1.6                   0.012               0.0009
        //          600                     1200                    5.7                   0.024               0.006
        //          900                     3600                   10.6                   0.034               0.02
        // the expected maximum residuals with respect to these models are about 0.4m, 0.5mm/s, 8μm/s² and 3e-6°
        PolynomialFunction pModel = new PolynomialFunction(new double[] {
            1.5664070631933846e-01,  7.5504722733047560e-03, -8.2460562451009510e-05,
            6.9546332080305580e-06, -1.7045365367533077e-09, -4.2187860791066264e-13
        });
        PolynomialFunction vModel = new PolynomialFunction(new double[] {
           -3.5472364019908720e-04,  1.6568103861124980e-05,  1.9637913327830596e-05,
           -3.4248792843039766e-09, -5.6565135131014254e-12,  1.4730170946808630e-15
        });
        PolynomialFunction aModel = new PolynomialFunction(new double[] {
            3.0731707577766896e-06,  3.9770746399850350e-05,  1.9779039254538660e-09,
            8.0263328220724900e-12, -1.5600835252366078e-14,  1.1785257001549687e-18
        });
        PolynomialFunction rModel = new PolynomialFunction(new double[] {
           -2.7689062063188115e-06,  1.7406542538258334e-07,  2.5109795349592287e-09,
            2.0399322661074575e-11,  9.9126348912426750e-15, -3.5015638905729510e-18
        });

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new DateComponents(2004, 01, 01),
                                                            TimeComponents.H00,
                                                            TimeScalesFactory.getUTC());

        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(a, e, i, pa, raan, lv, PositionAngle.TRUE,
                                                                 FramesFactory.getEME2000(), date, zero.add(mu));

        BodyCenterPointing attitudeLaw = new BodyCenterPointing(orbit.getFrame(), earth);

        FieldPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<>(orbit, attitudeLaw, mass,
                                                 ae, zero.add(mu), c20, c30, c40, c50, c60);

        FieldAbsoluteDate<T> centerDate = orbit.getDate().shiftedBy(100.0);

        FieldSpacecraftState<T> centerState = propagator.propagate(centerDate);

        double maxResidualP = 0;
        double maxResidualV = 0;
        double maxResidualA = 0;
        double maxResidualR = 0;
        for (T dt = field.getZero(); dt.getReal() < 900.0; dt = dt.add(5)) {

            FieldSpacecraftState<T> shifted = centerState.shiftedBy(dt);
            FieldSpacecraftState<T> propagated = propagator.propagate(centerDate.shiftedBy(dt));
            FieldPVCoordinates<T> dpv = new FieldPVCoordinates<>(propagated.getPVCoordinates(), shifted.getPVCoordinates());


            double residualP = pModel.value(dt.getReal()) - dpv.getPosition().getNorm().getReal();
            double residualV = vModel.value(dt.getReal()) - dpv.getVelocity().getNorm().getReal();
            double residualA = aModel.value(dt.getReal()) - dpv.getAcceleration().getNorm().getReal();
            double residualR = rModel.value(dt.getReal()) -
                               FastMath.toDegrees(FieldRotation.distance(shifted.getAttitude().getRotation(),
                                                                         propagated.getAttitude().getRotation()).getReal());
            maxResidualP = FastMath.max(maxResidualP, FastMath.abs(residualP));
            maxResidualV = FastMath.max(maxResidualV, FastMath.abs(residualV));
            maxResidualA = FastMath.max(maxResidualA, FastMath.abs(residualA));
            maxResidualR = FastMath.max(maxResidualR, FastMath.abs(residualR));

        }

        Assert.assertEquals(0.40,   maxResidualP, 0.01);
        Assert.assertEquals(4.9e-4, maxResidualV, 1.0e-5);
        Assert.assertEquals(2.8e-6, maxResidualR, 1.0e-1);

    }

    private <T extends RealFieldElement<T>> void doTestDatesConsistency(final Field<T> field) {

        T zero = field.getZero();
        T a = zero.add(rOrbit.getA());
        T e = zero.add(rOrbit.getE());
        T i = zero.add(rOrbit.getI());
        T pa = zero.add(1.9674147913622104);
        T raan = zero.add(FastMath.toRadians(261));
        T lv = zero.add(0);


        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new DateComponents(2004, 01, 01),
                                                            TimeComponents.H00,
                                                            TimeScalesFactory.getUTC());

        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(a, e, i, pa, raan, lv, PositionAngle.TRUE,
                                                                 FramesFactory.getEME2000(), date, zero.add(mu));
        BodyCenterPointing attitudeLaw = new BodyCenterPointing(orbit.getFrame(), earth);

        new FieldSpacecraftState<>(orbit, attitudeLaw.getAttitude(orbit.shiftedBy(zero.add(10.0)),
                                                                  orbit.getDate().shiftedBy(10.0),
                                                                  orbit.getFrame()));
    }

    /**
     * Check orbit and attitude dates can be off by a few ulps. I see this when using
     * FixedRate attitude provider.
     */
    private <T extends RealFieldElement<T>> void doTestDateConsistencyClose(final Field<T> field) {


        //setup
        T zero = field.getZero();
        T one  = field.getOne();
        T a = zero.add(rOrbit.getA());
        T e = zero.add(rOrbit.getE());
        T i = zero.add(rOrbit.getI());
        T pa = zero.add(1.9674147913622104);
        T raan = zero.add(FastMath.toRadians(261));
        T lv = zero.add(0);

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new DateComponents(2004, 01, 01),
                                                            TimeComponents.H00,
                                                            TimeScalesFactory.getUTC());
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(a, e, i, pa, raan, lv, PositionAngle.TRUE,
                                                                 FramesFactory.getEME2000(), date, zero.add(mu));

        FieldKeplerianOrbit<T> orbit10Shifts = orbit;
        for (int ii = 0; ii < 10; ii++) {
            orbit10Shifts = orbit10Shifts.shiftedBy(zero.add(0.1));
        }
        final FieldOrbit<T> orbit1Shift = orbit.shiftedBy(one);

        BodyCenterPointing attitudeLaw = new BodyCenterPointing(orbit.getFrame(), earth);

        FieldAttitude<T> shiftedAttitude = attitudeLaw
                .getAttitude(orbit1Shift, orbit1Shift.getDate(), orbit.getFrame());

        //verify dates are very close, but not equal
        Assert.assertNotEquals(shiftedAttitude.getDate(), orbit10Shifts.getDate());
        Assert.assertEquals(
                shiftedAttitude.getDate().durationFrom(orbit10Shifts.getDate()).getReal(),
                0, Precision.EPSILON);

        //action + verify no exception is thrown
        new FieldSpacecraftState<>(orbit10Shifts, shiftedAttitude);
    }

    // (expected=IllegalArgumentException.class)
    private <T extends RealFieldElement<T>> void doTestFramesConsistency(final Field<T> field) {

        T zero = field.getZero();
        T a = zero.add(rOrbit.getA());
        T e = zero.add(rOrbit.getE());
        T i = zero.add(rOrbit.getI());
        T pa = zero.add(1.9674147913622104);
        T raan = zero.add(FastMath.toRadians(261));
        T lv = zero.add(0);

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new DateComponents(2004, 01, 01),
                                                            TimeComponents.H00,
                                                            TimeScalesFactory.getUTC());
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(a, e, i, pa, raan, lv, PositionAngle.TRUE,
                                                                 FramesFactory.getEME2000(), date, zero.add(mu));

        new FieldSpacecraftState<>(orbit,
                            new FieldAttitude<>(orbit.getDate(),
                                                FramesFactory.getGCRF(),
                                                Rotation.IDENTITY,
                                                Vector3D.ZERO,
                                                Vector3D.ZERO,
                                                field));
    }

    private <T extends RealFieldElement<T>> void doTestTransform(final Field<T> field)
        {

        T zero = field.getZero();
        T a = zero.add(rOrbit.getA());
        T e = zero.add(rOrbit.getE());
        T i = zero.add(rOrbit.getI());
        T pa = zero.add(1.9674147913622104);
        T raan = zero.add(FastMath.toRadians(261));
        T lv = zero.add(0);
        T mass = zero.add(2500);

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new DateComponents(2004, 01, 01),
                        TimeComponents.H00,
                        TimeScalesFactory.getUTC());

        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(a, e, i, pa, raan, lv, PositionAngle.TRUE,
                                                                 FramesFactory.getEME2000(), date, zero.add(mu));

        BodyCenterPointing attitudeLaw = new BodyCenterPointing(orbit.getFrame(), earth);

        FieldKeplerianPropagator<T> propagator =
                        new FieldKeplerianPropagator<>(orbit, attitudeLaw, zero.add(mu), mass);

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

    private <T extends RealFieldElement<T>> void doTestAdditionalStates(final Field<T> field) {

        T zero = field.getZero();
        T a = zero.add(rOrbit.getA());
        T e = zero.add(rOrbit.getE());
        T i = zero.add(rOrbit.getI());
        T pa = zero.add(1.9674147913622104);
        T raan = zero.add(FastMath.toRadians(261));
        T lv = zero.add(0);
        T mass = zero.add(2500);

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new DateComponents(2004, 01, 01),
                                                            TimeComponents.H00,
                                                            TimeScalesFactory.getUTC());

        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(a, e, i, pa, raan, lv, PositionAngle.TRUE,
                                                                 FramesFactory.getEME2000(), date, zero.add(mu));

        BodyCenterPointing attitudeLaw = new BodyCenterPointing(orbit.getFrame(), earth);

        FieldKeplerianPropagator<T> propagator =
                        new FieldKeplerianPropagator<>(orbit, attitudeLaw, zero.add(mu), mass);




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
            extended.ensureCompatibleAdditionalStates(extended.addAdditionalState("test-2", kk));
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
        map.put("test-3", dd);
        FieldSpacecraftState<T> sO = new FieldSpacecraftState<>(state.getOrbit(), map);
        Assert.assertEquals(-6.0, sO.getAdditionalState("test-3")[0].getReal(), 1.0e-15);
        FieldSpacecraftState<T> sOA = new FieldSpacecraftState<>(state.getOrbit(), state.getAttitude(), map);
        Assert.assertEquals(-6.0, sOA.getAdditionalState("test-3")[0].getReal(), 1.0e-15);
        FieldSpacecraftState<T> sOM = new FieldSpacecraftState<>(state.getOrbit(), state.getMass(), map);
        Assert.assertEquals(-6.0, sOM.getAdditionalState("test-3")[0].getReal(), 1.0e-15);
        FieldSpacecraftState<T> sOAM = new FieldSpacecraftState<>(state.getOrbit(), state.getAttitude(), state.getMass(), map);
        Assert.assertEquals(-6.0, sOAM.getAdditionalState("test-3")[0].getReal(), 1.0e-15);
        FieldSpacecraftState<T> sFromDouble = new FieldSpacecraftState<>(field, sOAM.toSpacecraftState());
        Assert.assertEquals(-6.0, sFromDouble.getAdditionalState("test-3")[0].getReal(), 1.0e-15);

    }

    private <T extends RealFieldElement<T>> void doTestInterpolation(Field<T> field)
        throws ParseException, OrekitException {
        checkInterpolationError( 2,  106.46533, 0.40709287, 169847806.33e-9, 0.0, 450 * 450, field);
        checkInterpolationError( 3,    0.00353, 0.00003250,    189886.01e-9, 0.0, 0.0, field);
        checkInterpolationError( 4,    0.00002, 0.00000023,       232.25e-9, 0.0, 0.0, field);
    }

    private <T extends RealFieldElement<T>> void checkInterpolationError(int n, double expectedErrorP, double expectedErrorV,
                                         double expectedErrorA, double expectedErrorM, double expectedErrorQ, Field<T> field)
        {

        T zero = field.getZero();
        T mu  = zero.add(3.9860047e14);
        double ae  = 6.378137e6;
        double c20 = -1.08263e-3;
        double c30 = 2.54e-6;
        double c40 = 1.62e-6;
        double c50 = 2.3e-7;
        double c60 = -5.5e-7;


        T mass = zero.add(2500);
        T a = zero.add(7187990.1979844316);
        T e = zero.add(0.5e-4);
        T i = zero.add(1.7105407051081795);
        T omega = zero.add(1.9674147913622104);
        T OMEGA = zero.add(FastMath.toRadians(261));
        T lv = zero;

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new DateComponents(2004, 01, 01),
                        TimeComponents.H00,
                        TimeScalesFactory.getUTC());

        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                        FramesFactory.getEME2000(), date, zero.add(mu));

        BodyCenterPointing attitudeLaw = new BodyCenterPointing(orbit.getFrame(), earth);

        FieldEcksteinHechlerPropagator<T> propagator = new FieldEcksteinHechlerPropagator<>(orbit, attitudeLaw, mass,
                                                                               ae, mu, c20, c30, c40, c50, c60);
        FieldAbsoluteDate<T> centerDate = orbit.getDate().shiftedBy(100.0);
        FieldSpacecraftState<T> centerState = propagator.propagate(centerDate).addAdditionalState("quadratic", zero);
        List<FieldSpacecraftState<T>> sample = new ArrayList<FieldSpacecraftState<T>>();
        for (int ii = 0; ii < n; ++ii) {
            double dt = ii * 900.0 / (n - 1);
            FieldSpacecraftState<T> state = propagator.propagate(centerDate.shiftedBy(dt));
            state = state.addAdditionalState("quadratic", zero.add(dt * dt));
            sample.add(state);
        }
        double maxErrorP = 0;
        double maxErrorV = 0;
        double maxErrorA = 0;
        double maxErrorM = 0;
        double maxErrorQ = 0;
        for (double dt = 0; dt < 900.0; dt += 5) {
            FieldSpacecraftState<T> interpolated = centerState.interpolate(centerDate.shiftedBy(dt), sample);
            FieldSpacecraftState<T> propagated = propagator.propagate(centerDate.shiftedBy(dt));
            FieldPVCoordinates<T> dpv = new FieldPVCoordinates<>(propagated.getPVCoordinates(), interpolated.getPVCoordinates());
            maxErrorP = FastMath.max(maxErrorP, dpv.getPosition().getNorm().getReal());
            maxErrorV = FastMath.max(maxErrorV, dpv.getVelocity().getNorm().getReal());
            maxErrorA = FastMath.max(maxErrorA, FastMath.toDegrees(FieldRotation.distance(interpolated.getAttitude().getRotation(),
                                                                                                  propagated.getAttitude().getRotation()).getReal()));
            maxErrorM = FastMath.max(maxErrorM, FastMath.abs(interpolated.getMass().getReal() - propagated.getMass().getReal()));
            maxErrorQ = FastMath.max(maxErrorQ, FastMath.abs(interpolated.getAdditionalState("quadratic")[0].getReal() - dt * dt));
        }
        Assert.assertEquals(expectedErrorP, maxErrorP, 1.0e-3);
        Assert.assertEquals(expectedErrorV, maxErrorV, 1.0e-6);
        Assert.assertEquals(expectedErrorA, maxErrorA, 4.0e-10);
        Assert.assertEquals(expectedErrorM, maxErrorM, 1.0e-15);
        Assert.assertEquals(expectedErrorQ, maxErrorQ, 2.0e-10);
    }

    private <T extends RealFieldElement<T>> void doTestFieldVsRealAbsPV(final Field<T> field) {
        T zero = field.getZero();

        T x_f     = zero.add(0.8);
        T y_f     = zero.add(0.2);
        T z_f     = zero;
        T vx_f    = zero;
        T vy_f    = zero;
        T vz_f    = zero.add(0.1);

        FieldAbsoluteDate<T> t_f = new FieldAbsoluteDate<>(field);

        FieldPVCoordinates<T> pva_f = new FieldPVCoordinates<>(new FieldVector3D<>(x_f,y_f,z_f), new FieldVector3D<>(vx_f,vy_f,vz_f));

        FieldAbsolutePVCoordinates<T> absPV_f = new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(), t_f, pva_f);

        FieldSpacecraftState<T> ScS_f = new FieldSpacecraftState<>(absPV_f);
        SpacecraftState ScS_r = ScS_f.toSpacecraftState();

        for (double dt = 0; dt < 500; dt+=100){
            SpacecraftState control_r = ScS_r.shiftedBy(dt);
            FieldSpacecraftState<T> control_f = ScS_f.shiftedBy(zero.add(dt));


            Assert.assertEquals(control_r.getMu(), control_f.getMu().getReal(), 1e-10);
            Assert.assertEquals(control_r.getI(), control_f.getI().getReal(), 1e-10);
            Assert.assertEquals(control_r.getHx(), control_f.getHx().getReal(), 1e-10);
            Assert.assertEquals(control_r.getHy(), control_f.getHy().getReal(), 1e-10);
            Assert.assertEquals(control_r.getLv(), control_f.getLv().getReal(), 1e-10);
            Assert.assertEquals(control_r.getLE(), control_f.getLE().getReal(), 1e-10);
            Assert.assertEquals(control_r.getLM(), control_f.getLM().getReal(), 1e-10);
            Assert.assertEquals(control_r.getKeplerianMeanMotion(), control_f.getKeplerianMeanMotion().getReal(), 1e-10);
            Assert.assertEquals(control_r.getKeplerianPeriod(), control_f.getKeplerianPeriod().getReal(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getPosition().getX(), control_f.getPVCoordinates().toPVCoordinates().getPosition().getX(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getPosition().getY(), control_f.getPVCoordinates().toPVCoordinates().getPosition().getY(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getPosition().getZ(), control_f.getPVCoordinates().toPVCoordinates().getPosition().getZ(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getVelocity().getX(), control_f.getPVCoordinates().toPVCoordinates().getVelocity().getX(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getVelocity().getY(), control_f.getPVCoordinates().toPVCoordinates().getVelocity().getY(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getVelocity().getZ(), control_f.getPVCoordinates().toPVCoordinates().getVelocity().getZ(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getAcceleration().getX(), control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getX(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getAcceleration().getY(), control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getY(), 1e-10);
            Assert.assertEquals(control_r.getPVCoordinates().getAcceleration().getZ(), control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getZ(), 1e-10);
            Assert.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ0(), control_f.getAttitude().getOrientation().getRotation().getQ0().getReal(), 1e-10);
            Assert.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ1(), control_f.getAttitude().getOrientation().getRotation().getQ1().getReal(), 1e-10);
            Assert.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ2(), control_f.getAttitude().getOrientation().getRotation().getQ2().getReal(), 1e-10);
            Assert.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ3(), control_f.getAttitude().getOrientation().getRotation().getQ3().getReal(), 1e-10);

            Assert.assertEquals(control_r.getAttitude().getSpin().getAlpha(), control_f.getAttitude().getSpin().getAlpha().getReal(), 1e-10);
            Assert.assertEquals(control_r.getAttitude().getSpin().getDelta(), control_f.getAttitude().getSpin().getDelta().getReal(), 1e-10);
            Assert.assertEquals(control_r.getAttitude().getSpin().getNorm(), control_f.getAttitude().getSpin().getNorm().getReal(), 1e-10);

            Assert.assertEquals(control_r.getAttitude().getReferenceFrame().isPseudoInertial(), control_f.getAttitude().getReferenceFrame().isPseudoInertial());
            Assert.assertEquals(control_r.getAttitude().getDate().durationFrom(AbsoluteDate.J2000_EPOCH), control_f.getAttitude().getDate().durationFrom(AbsoluteDate.J2000_EPOCH).getReal(), 1e-10);


        }
    }


    /**
     * Check orbit and attitude dates can be off by a few ulps. I see this when using
     * FixedRate attitude provider.
     */
    private <T extends RealFieldElement<T>> void doTestDateConsistencyCloseAbsPV(final Field<T> field) {


        //setup
        T zero = field.getZero();
        T one  = field.getOne();
        T x_f     = zero.add(0.8);
        T y_f     = zero.add(0.2);
        T z_f     = zero;
        T vx_f    = zero;
        T vy_f    = zero;
        T vz_f    = zero.add(0.1);
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new DateComponents(2004, 01, 01),
                                                            TimeComponents.H00,
                                                            TimeScalesFactory.getUTC());
        FieldPVCoordinates<T> pva_f = new FieldPVCoordinates<>(new FieldVector3D<>(x_f,y_f,z_f), new FieldVector3D<>(vx_f,vy_f,vz_f));

        FieldAbsolutePVCoordinates<T> absPV_f = new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(), date, pva_f);

        FieldAbsolutePVCoordinates<T> AbsolutePVCoordinates10Shifts = absPV_f;
        for (int ii = 0; ii < 10; ii++) {
            AbsolutePVCoordinates10Shifts = AbsolutePVCoordinates10Shifts.shiftedBy(zero.add(0.1));
        }
        final FieldAbsolutePVCoordinates<T> AbsolutePVCoordinates1Shift = absPV_f.shiftedBy(one);

        BodyCenterPointing attitudeLaw = new BodyCenterPointing(absPV_f.getFrame(), earth);

        FieldAttitude<T> shiftedAttitude = attitudeLaw
                .getAttitude(AbsolutePVCoordinates1Shift, AbsolutePVCoordinates1Shift.getDate(), absPV_f.getFrame());

        //verify dates are very close, but not equal
        Assert.assertNotEquals(shiftedAttitude.getDate(), AbsolutePVCoordinates10Shifts.getDate());
        Assert.assertEquals(
                shiftedAttitude.getDate().durationFrom(AbsolutePVCoordinates10Shifts.getDate()).getReal(),
                0, Precision.EPSILON);

        //action + verify no exception is thrown
        new FieldSpacecraftState<>(AbsolutePVCoordinates10Shifts, shiftedAttitude);
    }


    // (expected=IllegalArgumentException.class)
    private <T extends RealFieldElement<T>> void doTestFramesConsistencyAbsPV(final Field<T> field) {

        T zero = field.getZero();

        T x_f     = zero.add(0.8);
        T y_f     = zero.add(0.2);
        T z_f     = zero;
        T vx_f    = zero;
        T vy_f    = zero;
        T vz_f    = zero.add(0.1);


        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new DateComponents(2004, 01, 01),
                        TimeComponents.H00,
                        TimeScalesFactory.getUTC());

        FieldPVCoordinates<T> pva_f = new FieldPVCoordinates<>(new FieldVector3D<>(x_f,y_f,z_f), new FieldVector3D<>(vx_f,vy_f,vz_f));

        FieldAbsolutePVCoordinates<T> absPV_f = new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(), date, pva_f);

        new FieldSpacecraftState<>(absPV_f,
                        new FieldAttitude<>(absPV_f.getDate(),
                                        FramesFactory.getGCRF(),
                                        FieldRotation.getIdentity(field),
                                        FieldVector3D.getZero(field),
                                        FieldVector3D.getZero(field)));
    }

    private <T extends RealFieldElement<T>> void doTestAdditionalStatesAbsPV(final Field<T> field) {

        T zero = field.getZero();
        T x_f     = zero.add(0.8);
        T y_f     = zero.add(0.2);
        T z_f     = zero;
        T vx_f    = zero;
        T vy_f    = zero;
        T vz_f    = zero.add(0.1);

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new DateComponents(2004, 01, 01),
                                                            TimeComponents.H00,
                                                            TimeScalesFactory.getUTC());

        FieldPVCoordinates<T> pva_f = new FieldPVCoordinates<>(new FieldVector3D<>(x_f,y_f,z_f), new FieldVector3D<>(vx_f,vy_f,vz_f));

        FieldAbsolutePVCoordinates<T> absPV_f = new FieldAbsolutePVCoordinates<>(FramesFactory.getEME2000(), date, pva_f);

        FieldNumericalPropagator<T> prop = new FieldNumericalPropagator<>(field,
                        new DormandPrince853FieldIntegrator<>(field, 0.1, 500, 0.001, 0.001));
        prop.setOrbitType(null);

        final FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(absPV_f);

        prop.resetInitialState(initialState);

        final FieldSpacecraftState<T> state = prop.propagate(absPV_f.getDate().shiftedBy(60));
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
            extended.ensureCompatibleAdditionalStates(extended.addAdditionalState("test-2", kk));
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
        map.put("test-3", dd);
        FieldSpacecraftState<T> sO = new FieldSpacecraftState<>(state.getAbsPVA(), map);
        Assert.assertEquals(-6.0, sO.getAdditionalState("test-3")[0].getReal(), 1.0e-15);
        FieldSpacecraftState<T> sOA = new FieldSpacecraftState<>(state.getAbsPVA(), state.getAttitude(), map);
        Assert.assertEquals(-6.0, sOA.getAdditionalState("test-3")[0].getReal(), 1.0e-15);
        FieldSpacecraftState<T> sOM = new FieldSpacecraftState<>(state.getAbsPVA(), state.getMass(), map);
        Assert.assertEquals(-6.0, sOM.getAdditionalState("test-3")[0].getReal(), 1.0e-15);
        FieldSpacecraftState<T> sOAM = new FieldSpacecraftState<>(state.getAbsPVA(), state.getAttitude(), state.getMass(), map);
        Assert.assertEquals(-6.0, sOAM.getAdditionalState("test-3")[0].getReal(), 1.0e-15);
        FieldSpacecraftState<T> sFromDouble = new FieldSpacecraftState<>(field, sOAM.toSpacecraftState());
        Assert.assertEquals(-6.0, sFromDouble.getAdditionalState("test-3")[0].getReal(), 1.0e-15);

    }

    private <T extends RealFieldElement<T>> void doTestAdditionalTestResetOnEventAnalytical(final Field<T> field) {

        T zero = field.getZero();

        // Build orbit
        FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, 2000, 1, 1, TimeScalesFactory.getUTC());
        FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(7.1E6), zero, zero, zero, zero, zero,
                                         PositionAngle.TRUE, FramesFactory.getGCRF(), date0,
                                         zero.add(Constants.WGS84_EARTH_MU));

        // Build propagator
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);

        // Create initial state with one additional state and add it to the propagator
        final String name = "A";
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit).
                                       addAdditionalState(name, zero.add(-1));

        propagator.resetInitialState(initialState);

        // Create date detector and handler
        FieldAbsoluteDate<T> changeDate = date0.shiftedBy(3);
        FieldDateDetector<T> dateDetector = new FieldDateDetector<>(changeDate).
                                    withHandler(new FieldEventHandler<FieldDateDetector<T>, T>() {

            @Override
            public Action eventOccurred(FieldSpacecraftState<T> s, FieldDateDetector<T> detector, boolean increasing) {
              return Action.RESET_STATE;
            }

            @Override
            public FieldSpacecraftState<T> resetState(FieldDateDetector<T> detector, FieldSpacecraftState<T> oldState) {
                return oldState.addAdditionalState(name, zero.add(+1));
            }

        });

        propagator.addEventDetector(dateDetector);
        propagator.setMasterMode(zero.add(0.125), (s, isFinal) -> {
            if (s.getDate().durationFrom(changeDate).getReal() < -0.001) {
                Assert.assertEquals(-1, s.getAdditionalState(name)[0].getReal(), 1.0e-15);
            } else if (s.getDate().durationFrom(changeDate).getReal() > +0.001) {
                Assert.assertEquals(+1, s.getAdditionalState(name)[0].getReal(), 1.0e-15);
            }
        });
        FieldSpacecraftState<T> finalState = propagator.propagate(date0, date0.shiftedBy(5));
        Assert.assertEquals(+1, finalState.getAdditionalState(name)[0].getReal(), 1.0e-15);

    }

    private <T extends RealFieldElement<T>> void doTestAdditionalTestResetOnEventNumerical(final Field<T> field) {

        T zero = field.getZero();

        // Build orbit
        FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, 2000, 1, 1, TimeScalesFactory.getUTC());
        FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(7.1E6), zero, zero, zero, zero, zero,
                                         PositionAngle.TRUE, FramesFactory.getGCRF(), date0,
                                         zero.add(Constants.WGS84_EARTH_MU));

        // Build propagator
        FieldODEIntegrator<T> odeIntegrator = new DormandPrince853FieldIntegrator<>(field, 1E-3, 1E3, 1E-6, 1E-6);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, odeIntegrator);

        // Create initial state with one additional state and add it to the propagator
        final String name = "A";
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit).
                                       addAdditionalState(name, zero.add(-1));

        propagator.resetInitialState(initialState);

        // Create date detector and handler
        FieldAbsoluteDate<T> changeDate = date0.shiftedBy(3);
        FieldDateDetector<T> dateDetector = new FieldDateDetector<>(changeDate).
                                    withHandler(new FieldEventHandler<FieldDateDetector<T>, T>() {

            @Override
            public Action eventOccurred(FieldSpacecraftState<T> s, FieldDateDetector<T> detector, boolean increasing) {
              return Action.RESET_STATE;
            }

            @Override
            public FieldSpacecraftState<T> resetState(FieldDateDetector<T> detector, FieldSpacecraftState<T> oldState) {
                return oldState.addAdditionalState(name, zero.add(+1));
            }

        });

        propagator.addEventDetector(dateDetector);
        propagator.setMasterMode(zero.add(0.125), (s, isFinal) -> {
            if (s.getDate().durationFrom(changeDate).getReal() < -0.001) {
                Assert.assertEquals(-1, s.getAdditionalState(name)[0].getReal(), 1.0e-15);
            } else if (s.getDate().durationFrom(changeDate).getReal() > +0.001) {
                Assert.assertEquals(+1, s.getAdditionalState(name)[0].getReal(), 1.0e-15);
            }
        });
        FieldSpacecraftState<T> finalState = propagator.propagate(date0, date0.shiftedBy(5));
        Assert.assertEquals(+1, finalState.getAdditionalState(name)[0].getReal(), 1.0e-15);

    }

    @Before
    public void setUp(){
        try {

            Utils.setDataRoot("regular-data");
            mu  = 3.9860047e14;

            double a     = 7187990.1979844316;
            double e     = 0.5e-4;
            double i     = 1.7105407051081795;
            double omega = 1.9674147913622104;
            double OMEGA = FastMath.toRadians(261);
            double lv    =    0;

            rDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                     TimeComponents.H00,
                                     TimeScalesFactory.getUTC());
            rOrbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                                        FramesFactory.getEME2000(), rDate, mu);
            earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        } catch (OrekitException oe) {

            Assert.fail(oe.getLocalizedMessage());
        }
    }

    private AbsoluteDate rDate;
    private double mu;
    private Orbit rOrbit;
    private OneAxisEllipsoid earth;

}

