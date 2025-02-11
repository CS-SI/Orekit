/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldArrayDictionary;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;


class FieldSpacecraftStateTest {

    @Test
    void testWithAttitudeAndOrbit() {
        // GIVEN
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final Binary64Field field = Binary64Field.getInstance();
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(field, state);
        final FieldAttitude<Binary64> attitude = new FieldAttitude<>(fieldState.getDate(), state.getFrame(),
                new FieldAngularCoordinates<>(FieldRotation.getIdentity(field), FieldVector3D.getMinusI(field)));
        // WHEN
        final FieldSpacecraftState<Binary64> fieldStateWithAttitude = fieldState.withAttitude(attitude);
        // THEN
        Assertions.assertEquals(attitude, fieldStateWithAttitude.getAttitude());
        Assertions.assertEquals(fieldState.getMass(), fieldStateWithAttitude.getMass());
        Assertions.assertEquals(fieldState.getOrbit(), fieldStateWithAttitude.getOrbit());
    }

    @Test
    void testWithMassAndOrbit() {
        // GIVEN
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final Binary64Field field = Binary64Field.getInstance();
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(field, state);
        final Binary64 expectedMass = new Binary64(123);
        // WHEN
        final FieldSpacecraftState<Binary64> fieldStateWithMass = fieldState.withMass(expectedMass);
        // THEN
        Assertions.assertEquals(expectedMass, fieldStateWithMass.getMass());
        Assertions.assertEquals(fieldState.getAttitude(), fieldStateWithMass.getAttitude());
        Assertions.assertEquals(fieldState.getOrbit(), fieldStateWithMass.getOrbit());
    }

    @Test
    void testWithAttitudeAndAbsolutePV() {
        // GIVEN
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates());
        final SpacecraftState state = new SpacecraftState(absolutePVCoordinates);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(field, state);
        final FieldAttitude<Binary64> attitude = new FieldAttitude<>(fieldState.getDate(), state.getFrame(),
                new FieldAngularCoordinates<>(FieldRotation.getIdentity(field), FieldVector3D.getMinusI(field)));
        // WHEN
        final FieldSpacecraftState<Binary64> fieldStateWithAttitude = fieldState.withAttitude(attitude);
        // THEN
        Assertions.assertEquals(attitude, fieldStateWithAttitude.getAttitude());
        Assertions.assertEquals(fieldState.getMass(), fieldStateWithAttitude.getMass());
        Assertions.assertEquals(fieldState.getAbsPVA(), fieldStateWithAttitude.getAbsPVA());
    }

    @Test
    void testWithMassAndAbsolutePV() {
        // GIVEN
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates());
        final SpacecraftState state = new SpacecraftState(absolutePVCoordinates);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(field, state);
        final Binary64 expectedMass = new Binary64(123);
        // WHEN
        final FieldSpacecraftState<Binary64> fieldStateWithMass = fieldState.withMass(expectedMass);
        // THEN
        Assertions.assertEquals(expectedMass, fieldStateWithMass.getMass());
        Assertions.assertEquals(fieldState.getAttitude(), fieldStateWithMass.getAttitude());
        Assertions.assertEquals(fieldState.getAbsPVA(), fieldStateWithMass.getAbsPVA());
    }

    @Test
    void testFieldVSReal() {
        doTestFieldVsReal(Binary64Field.getInstance());
    }

    @Test
    void testShiftVsEcksteinHechlerError() {
        doTestShiftVsEcksteinHechlerError(Binary64Field.getInstance());
    }

    @Test
    void testDatesConsistency() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> doTestDatesConsistency(Binary64Field.getInstance()));
    }

    @Test
    void testDateConsistencyClose() {
        doTestDateConsistencyClose(Binary64Field.getInstance());
    }

    @Test
    void testFramesConsistency() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> doTestFramesConsistency(Binary64Field.getInstance()));
    }

    @Test
    void testTransform() {
        doTestTransform(Binary64Field.getInstance());
    }

    @Test
    void testAdditionalStates() {
        doTestAdditionalStates(Binary64Field.getInstance());
    }

    @Test
    void testAdditionalStatesDerivatives() {
        doTestAdditionalStatesDerivatives(Binary64Field.getInstance());
    }

    @Test
    void testFieldVSRealAbsPV() {
        doTestFieldVsRealAbsPV(Binary64Field.getInstance());
    }

    @Test
    void testDateConsistencyCloseAbsPV() {
        doTestDateConsistencyCloseAbsPV(Binary64Field.getInstance());
    }

    @Test
    void testFramesConsistencyAbsPV() {
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> doTestFramesConsistencyAbsPV(Binary64Field.getInstance()));
    }

    @Test
    void testAdditionalStatesAbsPV() {
        doTestAdditionalStatesAbsPV(Binary64Field.getInstance());
    }

    @Test
    void testAdditionalStatesDerivativesAbsPV() {
        doTestAdditionalStatesDerivativesAbsPV(Binary64Field.getInstance());
    }

    @Test
    void testResetOnEventAnalytical() {
        doTestAdditionalTestResetOnEventAnalytical(Binary64Field.getInstance());
    }

    @Test
    void testResetOnEventNumerical() {
        doTestAdditionalTestResetOnEventNumerical(Binary64Field.getInstance());
    }

    @Test
    void testShiftAdditionalDerivativesDouble() {
        doTestShiftAdditionalDerivativesDouble(Binary64Field.getInstance());
    }

    @Test
    void testShiftAdditionalDerivativesField() {
        doTestShiftAdditionalDerivativesField(Binary64Field.getInstance());
    }

    @Test
    void testToTransform() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final FieldOrbit<Complex> orbit = new FieldCartesianOrbit<>(field, rOrbit);
        final TimeStampedAngularCoordinates angularCoordinates = new TimeStampedAngularCoordinates(
                orbit.getDate().toAbsoluteDate(), Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO);
        final FieldAttitude<Complex> attitude = new FieldAttitude<>(orbit.getFrame(),
                new TimeStampedFieldAngularCoordinates<>(field, angularCoordinates));
        final FieldSpacecraftState<Complex> state = new FieldSpacecraftState<>(orbit, attitude);
        // WHEN
        final FieldTransform<Complex> fieldTransform = state.toTransform();
        // THEN
        final Transform expectedTransform = state.toSpacecraftState().toTransform();
        Assertions.assertEquals(expectedTransform.getDate(), fieldTransform.getDate());
        final double tolerance = 1e-10;
        Assertions.assertEquals(expectedTransform.getTranslation().getX(),
                fieldTransform.getTranslation().getX().getReal(), tolerance);
        Assertions.assertEquals(expectedTransform.getTranslation().getY(),
                fieldTransform.getTranslation().getY().getReal(), tolerance);
        Assertions.assertEquals(expectedTransform.getTranslation().getZ(),
                fieldTransform.getTranslation().getZ().getReal(), tolerance);
        Assertions.assertEquals(0., Rotation.distance(expectedTransform.getRotation(),
                fieldTransform.getRotation().toRotation()));
    }
    
    @Test
    void testToStaticTransform() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final FieldOrbit<Complex> orbit = new FieldCartesianOrbit<>(field, rOrbit);
        final TimeStampedAngularCoordinates angularCoordinates = new TimeStampedAngularCoordinates(
                orbit.getDate().toAbsoluteDate(), Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO);
        final FieldAttitude<Complex> attitude = new FieldAttitude<>(orbit.getFrame(),
                new TimeStampedFieldAngularCoordinates<>(field, angularCoordinates));
        final FieldSpacecraftState<Complex> state = new FieldSpacecraftState<>(orbit, attitude);
        // WHEN
        final FieldStaticTransform<Complex> actualStaticTransform = state.toStaticTransform();
        // THEN
        final FieldStaticTransform<Complex> expectedStaticTransform = state.toTransform();
        Assertions.assertEquals(expectedStaticTransform.getDate(), actualStaticTransform.getDate());
        final double tolerance = 1e-10;
        Assertions.assertEquals(expectedStaticTransform.getTranslation().getX().getReal(),
                actualStaticTransform.getTranslation().getX().getReal(), tolerance);
        Assertions.assertEquals(expectedStaticTransform.getTranslation().getY().getReal(),
                actualStaticTransform.getTranslation().getY().getReal(), tolerance);
        Assertions.assertEquals(expectedStaticTransform.getTranslation().getZ().getReal(),
                actualStaticTransform.getTranslation().getZ().getReal(), tolerance);
        Assertions.assertEquals(0., Rotation.distance(expectedStaticTransform.getRotation().toRotation(),
                actualStaticTransform.getRotation().toRotation()));
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldVsReal(final Field<T> field) {
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


        KeplerianOrbit      kep_r = new KeplerianOrbit(a_r, e_r, i_r, pa_r, raan_r, m_r, PositionAngleType.ECCENTRIC, FramesFactory.getEME2000(), t_r, mu);
        FieldKeplerianOrbit<T> kep_f = new FieldKeplerianOrbit<>(a_f, e_f, i_f, pa_f, raan_f, m_f, PositionAngleType.ECCENTRIC, FramesFactory.getEME2000(), t_f, zero.add(mu));

        SpacecraftState ScS_r = new SpacecraftState(kep_r);
        FieldSpacecraftState<T> ScS_f = new FieldSpacecraftState<>(kep_f);

        for (double dt = 0; dt < 500; dt+=100){
            SpacecraftState control_r = ScS_r.shiftedBy(dt);
            FieldSpacecraftState<T> control_f = ScS_f.shiftedBy(zero.add(dt));


            Assertions.assertEquals(control_r.getOrbit().getA(), control_f.getOrbit().getA().getReal(), 1e-10);
            Assertions.assertEquals(control_r.getOrbit().getE(), control_f.getOrbit().getE().getReal(), 1e-10);
            Assertions.assertEquals(control_r.getOrbit().getEquinoctialEx(), control_f.getOrbit().getEquinoctialEx().getReal(), 1e-10);
            Assertions.assertEquals(control_r.getOrbit().getEquinoctialEy(), control_f.getOrbit().getEquinoctialEy().getReal(), 1e-10);
            Assertions.assertEquals(control_r.getPosition().getX(), control_f.getPVCoordinates().toPVCoordinates().getPosition().getX(), 1e-10);
            Assertions.assertEquals(control_r.getPosition().getY(), control_f.getPVCoordinates().toPVCoordinates().getPosition().getY(), 1e-10);
            Assertions.assertEquals(control_r.getPosition().getZ(), control_f.getPVCoordinates().toPVCoordinates().getPosition().getZ(), 1e-10);
            Assertions.assertEquals(control_r.getPVCoordinates().getVelocity().getX(), control_f.getPVCoordinates().toPVCoordinates().getVelocity().getX(), 1e-10);
            Assertions.assertEquals(control_r.getPVCoordinates().getVelocity().getY(), control_f.getPVCoordinates().toPVCoordinates().getVelocity().getY(), 1e-10);
            Assertions.assertEquals(control_r.getPVCoordinates().getVelocity().getZ(), control_f.getPVCoordinates().toPVCoordinates().getVelocity().getZ(), 1e-10);
            Assertions.assertEquals(control_r.getPVCoordinates().getAcceleration().getX(), control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getX(), 1e-10);
            Assertions.assertEquals(control_r.getPVCoordinates().getAcceleration().getY(), control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getY(), 1e-10);
            Assertions.assertEquals(control_r.getPVCoordinates().getAcceleration().getZ(), control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getZ(), 1e-10);
            Assertions.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ0(), control_f.getAttitude().getOrientation().getRotation().getQ0().getReal(), 1e-10);
            Assertions.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ1(), control_f.getAttitude().getOrientation().getRotation().getQ1().getReal(), 1e-10);
            Assertions.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ2(), control_f.getAttitude().getOrientation().getRotation().getQ2().getReal(), 1e-10);
            Assertions.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ3(), control_f.getAttitude().getOrientation().getRotation().getQ3().getReal(), 1e-10);

            Assertions.assertEquals(control_r.getAttitude().getSpin().getAlpha(), control_f.getAttitude().getSpin().getAlpha().getReal(), 1e-10);
            Assertions.assertEquals(control_r.getAttitude().getSpin().getDelta(), control_f.getAttitude().getSpin().getDelta().getReal(), 1e-10);
            Assertions.assertEquals(control_r.getAttitude().getSpin().getNorm(), control_f.getAttitude().getSpin().getNorm().getReal(), 1e-10);

            Assertions.assertEquals(control_r.getAttitude().getReferenceFrame().isPseudoInertial(), control_f.getAttitude().getReferenceFrame().isPseudoInertial());
            Assertions.assertEquals(control_r.getAttitude().getDate().durationFrom(AbsoluteDate.J2000_EPOCH), control_f.getAttitude().getDate().durationFrom(AbsoluteDate.J2000_EPOCH).getReal(), 1e-10);


        }

    }

    private <T extends CalculusFieldElement<T>>  void doTestShiftVsEcksteinHechlerError(final Field<T> field)
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

        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(a, e, i, pa, raan, lv, PositionAngleType.TRUE,
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

        Assertions.assertEquals(0.40,   maxResidualP, 0.01);
        Assertions.assertEquals(4.9e-4, maxResidualV, 1.0e-5);
        Assertions.assertEquals(2.8e-6, maxResidualR, 1.0e-1);

    }

    private <T extends CalculusFieldElement<T>> void doTestDatesConsistency(final Field<T> field) {

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

        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(a, e, i, pa, raan, lv, PositionAngleType.TRUE,
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
    private <T extends CalculusFieldElement<T>> void doTestDateConsistencyClose(final Field<T> field) {


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
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(a, e, i, pa, raan, lv, PositionAngleType.TRUE,
                                                                 FramesFactory.getEME2000(), date, zero.add(mu));

        FieldKeplerianOrbit<T> orbit10Shifts = orbit;
        for (int ii = 0; ii < 10; ii++) {
            orbit10Shifts = orbit10Shifts.shiftedBy(zero.add(0.1));
        }
        final FieldOrbit<T> orbit1Shift = orbit.shiftedBy(one);

        BodyCenterPointing attitudeLaw = new BodyCenterPointing(orbit.getFrame(), earth);

        FieldAttitude<T> shiftedAttitude = attitudeLaw
                .getAttitude(orbit1Shift, orbit1Shift.getDate(), orbit.getFrame());

        // since Orekit 13, dates are equal
        Assertions.assertEquals(shiftedAttitude.getDate(), orbit10Shifts.getDate());

        //action + verify no exception is thrown
        new FieldSpacecraftState<>(orbit10Shifts, shiftedAttitude);
    }

    // 
    private <T extends CalculusFieldElement<T>> void doTestFramesConsistency(final Field<T> field) {

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
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(a, e, i, pa, raan, lv, PositionAngleType.TRUE,
                                                                 FramesFactory.getEME2000(), date, zero.add(mu));

        new FieldSpacecraftState<>(orbit,
                            new FieldAttitude<>(orbit.getDate(),
                                                FramesFactory.getGCRF(),
                                                Rotation.IDENTITY,
                                                Vector3D.ZERO,
                                                Vector3D.ZERO,
                                                field));
    }

    private <T extends CalculusFieldElement<T>> void doTestTransform(final Field<T> field) {

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

        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(a, e, i, pa, raan, lv, PositionAngleType.TRUE,
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
            double alpha = Vector3D.angle(mZDirection, state.getPosition().toVector3D());
            maxDP = FastMath.max(maxDP, dPV.getPosition().getNorm());
            maxDV = FastMath.max(maxDV, dPV.getVelocity().getNorm());
            maxDA = FastMath.max(maxDA, FastMath.toDegrees(alpha));
        }
        Assertions.assertEquals(0.0, maxDP, 1.0e-6);
        Assertions.assertEquals(0.0, maxDV, 1.0e-9);
        Assertions.assertEquals(0.0, maxDA, 8.1e-10);

    }

    private <T extends CalculusFieldElement<T>> void doTestAdditionalStates(final Field<T> field) {

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

        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(a, e, i, pa, raan, lv, PositionAngleType.TRUE,
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
                        addAdditionalData("test-1", add).
                        addAdditionalData("test-2", zero.add(42.0));
        Assertions.assertEquals(0, state.getAdditionalDataValues().size());
        Assertions.assertFalse(state.hasAdditionalData("test-1"));
        try {
            state.getAdditionalData("test-1");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertEquals(oe.getParts()[0], "test-1");
        }
        try {
            state.ensureCompatibleAdditionalStates(extended);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(state);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            T[] kk = MathArrays.buildArray(field, 7);
            extended.ensureCompatibleAdditionalStates(extended.addAdditionalData("test-2", kk));
            Assertions.fail("an exception should have been thrown");
        } catch (MathIllegalStateException mise) {
            Assertions.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH, mise.getSpecifier());
            Assertions.assertEquals(7, ((Integer) mise.getParts()[0]).intValue());
        }
        Assertions.assertEquals(2, extended.getAdditionalDataValues().size());
        Assertions.assertTrue(extended.hasAdditionalData("test-1"));
        Assertions.assertTrue(extended.hasAdditionalData("test-2"));
        Assertions.assertEquals( 1.0, extended.getAdditionalData("test-1")[0].getReal(), 1.0e-15);
        Assertions.assertEquals( 2.0, extended.getAdditionalData("test-1")[1].getReal(), 1.0e-15);
        Assertions.assertEquals(42.0, extended.getAdditionalData("test-2")[0].getReal(), 1.0e-15);

        // test various constructors
        T[] dd = MathArrays.buildArray(field, 1);
        dd[0] = zero.add(-6.0);
        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        dictionary.put("test-3", dd);
        FieldSpacecraftState<T> sO = state.withAdditionalData(dictionary);
        FieldSpacecraftState<T> sFromDouble = new FieldSpacecraftState<>(field, sO.toSpacecraftState());
        Assertions.assertEquals(-6.0, sFromDouble.getAdditionalData("test-3")[0].getReal(), 1.0e-15);

    }

    private <T extends CalculusFieldElement<T>> void doTestAdditionalStatesDerivatives(final Field<T> field) {

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

        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(a, e, i, pa, raan, lv, PositionAngleType.TRUE,
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
                 addAdditionalStateDerivative("test-1", add).
                  addAdditionalStateDerivative("test-2", zero.add(42.0));
        Assertions.assertEquals(0, state.getAdditionalStatesDerivatives().size());
        Assertions.assertFalse(state.hasAdditionalStateDerivative("test-1"));
        try {
            state.getAdditionalStateDerivative("test-1");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertEquals(oe.getParts()[0], "test-1");
        }
        try {
            state.ensureCompatibleAdditionalStates(extended);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(state);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            T[] kk = MathArrays.buildArray(field, 7);
            extended.ensureCompatibleAdditionalStates(extended.addAdditionalStateDerivative("test-2", kk));
            Assertions.fail("an exception should have been thrown");
        } catch (MathIllegalStateException mise) {
            Assertions.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH, mise.getSpecifier());
            Assertions.assertEquals(7, ((Integer) mise.getParts()[0]).intValue());
        }
        Assertions.assertEquals(2, extended.getAdditionalStatesDerivatives().size());
        Assertions.assertTrue(extended.hasAdditionalStateDerivative("test-1"));
        Assertions.assertTrue(extended.hasAdditionalStateDerivative("test-2"));
        Assertions.assertEquals( 1.0, extended.getAdditionalStateDerivative("test-1")[0].getReal(), 1.0e-15);
        Assertions.assertEquals( 2.0, extended.getAdditionalStateDerivative("test-1")[1].getReal(), 1.0e-15);
        Assertions.assertEquals(42.0, extended.getAdditionalStateDerivative("test-2")[0].getReal(), 1.0e-15);

        // test most complete constructor
        T[] dd = MathArrays.buildArray(field, 1);
        dd[0] = zero.add(-6.0);
        FieldArrayDictionary<T> dict = new FieldArrayDictionary<>(field);
        dict.put("test-3", dd);
        FieldSpacecraftState<T> s = new FieldSpacecraftState<>(state.getOrbit(), state.getAttitude(), state.getMass(), null, dict);
        Assertions.assertFalse(s.hasAdditionalData("test-3"));
        Assertions.assertEquals(-6.0, s.getAdditionalStateDerivative("test-3")[0].getReal(), 1.0e-15);

        // test conversion
        FieldSpacecraftState<T> rebuilt = new FieldSpacecraftState<>(field, s.toSpacecraftState());
        rebuilt.ensureCompatibleAdditionalStates(s);

    }

    private <T extends CalculusFieldElement<T>> void doTestFieldVsRealAbsPV(final Field<T> field) {
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
        FieldSpacecraftState<T> withM = new FieldSpacecraftState<>(absPV_f, zero.newInstance(1234.5));
        Assertions.assertEquals(1234.5, withM.getMass().getReal(), 1.0e-10);
        SpacecraftState ScS_r = ScS_f.toSpacecraftState();

        for (double dt = 0; dt < 500; dt+=100){
            SpacecraftState control_r = ScS_r.shiftedBy(dt);
            FieldSpacecraftState<T> control_f = ScS_f.shiftedBy(zero.add(dt));

            Assertions.assertEquals(control_r.getPosition().getX(), control_f.getPVCoordinates().toPVCoordinates().getPosition().getX(), 1e-10);
            Assertions.assertEquals(control_r.getPosition().getY(), control_f.getPVCoordinates().toPVCoordinates().getPosition().getY(), 1e-10);
            Assertions.assertEquals(control_r.getPosition().getZ(), control_f.getPVCoordinates().toPVCoordinates().getPosition().getZ(), 1e-10);
            Assertions.assertEquals(control_r.getPVCoordinates().getVelocity().getX(), control_f.getPVCoordinates().toPVCoordinates().getVelocity().getX(), 1e-10);
            Assertions.assertEquals(control_r.getPVCoordinates().getVelocity().getY(), control_f.getPVCoordinates().toPVCoordinates().getVelocity().getY(), 1e-10);
            Assertions.assertEquals(control_r.getPVCoordinates().getVelocity().getZ(), control_f.getPVCoordinates().toPVCoordinates().getVelocity().getZ(), 1e-10);
            Assertions.assertEquals(control_r.getPVCoordinates().getAcceleration().getX(), control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getX(), 1e-10);
            Assertions.assertEquals(control_r.getPVCoordinates().getAcceleration().getY(), control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getY(), 1e-10);
            Assertions.assertEquals(control_r.getPVCoordinates().getAcceleration().getZ(), control_f.getPVCoordinates().toPVCoordinates().getAcceleration().getZ(), 1e-10);
            Assertions.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ0(), control_f.getAttitude().getOrientation().getRotation().getQ0().getReal(), 1e-10);
            Assertions.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ1(), control_f.getAttitude().getOrientation().getRotation().getQ1().getReal(), 1e-10);
            Assertions.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ2(), control_f.getAttitude().getOrientation().getRotation().getQ2().getReal(), 1e-10);
            Assertions.assertEquals(control_r.getAttitude().getOrientation().getRotation().getQ3(), control_f.getAttitude().getOrientation().getRotation().getQ3().getReal(), 1e-10);

            Assertions.assertEquals(control_r.getAttitude().getSpin().getAlpha(), control_f.getAttitude().getSpin().getAlpha().getReal(), 1e-10);
            Assertions.assertEquals(control_r.getAttitude().getSpin().getDelta(), control_f.getAttitude().getSpin().getDelta().getReal(), 1e-10);
            Assertions.assertEquals(control_r.getAttitude().getSpin().getNorm(), control_f.getAttitude().getSpin().getNorm().getReal(), 1e-10);

            Assertions.assertEquals(control_r.getAttitude().getReferenceFrame().isPseudoInertial(), control_f.getAttitude().getReferenceFrame().isPseudoInertial());
            Assertions.assertEquals(control_r.getAttitude().getDate().durationFrom(AbsoluteDate.J2000_EPOCH), control_f.getAttitude().getDate().durationFrom(AbsoluteDate.J2000_EPOCH).getReal(), 1e-10);


        }
    }


    /**
     * Check orbit and attitude dates can be off by a few ulps. I see this when using
     * FixedRate attitude provider.
     */
    private <T extends CalculusFieldElement<T>> void doTestDateConsistencyCloseAbsPV(final Field<T> field) {


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

        FieldAttitude<T> shiftedAttitude = attitudeLaw.getAttitude(AbsolutePVCoordinates1Shift,
                                                                   AbsolutePVCoordinates1Shift.getDate(),
                                                                   absPV_f.getFrame());
        // since Orekit 13, dates are equal
        Assertions.assertEquals(shiftedAttitude.getDate(), AbsolutePVCoordinates10Shifts.getDate());

        //action + verify no exception is thrown
        FieldSpacecraftState<T> s1 = new FieldSpacecraftState<>(AbsolutePVCoordinates10Shifts, shiftedAttitude);
        FieldSpacecraftState<T> s2 = s1.shiftedBy(0.001);

        try {
            // but here, the time difference is too great
            new FieldSpacecraftState<>(AbsolutePVCoordinates10Shifts, s2.getAttitude());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.ORBIT_AND_ATTITUDE_DATES_MISMATCH,
                                oiae.getSpecifier());
        }
    }


    // 
    private <T extends CalculusFieldElement<T>> void doTestFramesConsistencyAbsPV(final Field<T> field) {

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

    private <T extends CalculusFieldElement<T>> void doTestAdditionalStatesAbsPV(final Field<T> field) {

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
                        addAdditionalData("test-1", add).
                        addAdditionalData("test-2", zero.add(42.0));
        Assertions.assertEquals(0, state.getAdditionalDataValues().size());
        Assertions.assertFalse(state.hasAdditionalData("test-1"));
        try {
            state.getAdditionalData("test-1");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertEquals(oe.getParts()[0], "test-1");
        }
        try {
            state.ensureCompatibleAdditionalStates(extended);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(state);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            T[] kk = MathArrays.buildArray(field, 7);
            extended.ensureCompatibleAdditionalStates(extended.addAdditionalData("test-2", kk));
            Assertions.fail("an exception should have been thrown");
        } catch (MathIllegalStateException mise) {
            Assertions.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH, mise.getSpecifier());
            Assertions.assertEquals(7, ((Integer) mise.getParts()[0]).intValue());
        }
        Assertions.assertEquals(2, extended.getAdditionalDataValues().size());
        Assertions.assertTrue(extended.hasAdditionalData("test-1"));
        Assertions.assertTrue(extended.hasAdditionalData("test-2"));
        Assertions.assertEquals( 1.0, extended.getAdditionalData("test-1")[0].getReal(), 1.0e-15);
        Assertions.assertEquals( 2.0, extended.getAdditionalData("test-1")[1].getReal(), 1.0e-15);
        Assertions.assertEquals(42.0, extended.getAdditionalData("test-2")[0].getReal(), 1.0e-15);

        // test various constructors
        T[] dd = MathArrays.buildArray(field, 1);
        dd[0] = zero.add(-6.0);
        FieldArrayDictionary<T> map = new FieldArrayDictionary<>(field);
        map.put("test-3", dd);
        FieldSpacecraftState<T> sO = new FieldSpacecraftState<>(state.getAbsPVA()).withAdditionalData(map);
        Assertions.assertEquals(-6.0, sO.getAdditionalData("test-3")[0].getReal(), 1.0e-15);
        FieldSpacecraftState<T> sFromDouble = new FieldSpacecraftState<>(field, sO.toSpacecraftState());
        Assertions.assertEquals(-6.0, sFromDouble.getAdditionalData("test-3")[0].getReal(), 1.0e-15);

    }

    private <T extends CalculusFieldElement<T>> void doTestAdditionalStatesDerivativesAbsPV(final Field<T> field) {

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
                 addAdditionalStateDerivative("test-1", add).
                  addAdditionalStateDerivative("test-2", zero.add(42.0));
        Assertions.assertEquals(0, state.getAdditionalStatesDerivatives().size());
        Assertions.assertFalse(state.hasAdditionalStateDerivative("test-1"));
        try {
            state.getAdditionalStateDerivative("test-1");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertEquals(oe.getParts()[0], "test-1");
        }
        try {
            state.ensureCompatibleAdditionalStates(extended);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(state);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            T[] kk = MathArrays.buildArray(field, 7);
            extended.ensureCompatibleAdditionalStates(extended.addAdditionalStateDerivative("test-2", kk));
            Assertions.fail("an exception should have been thrown");
        } catch (MathIllegalStateException mise) {
            Assertions.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH, mise.getSpecifier());
            Assertions.assertEquals(7, ((Integer) mise.getParts()[0]).intValue());
        }
        Assertions.assertEquals(2, extended.getAdditionalStatesDerivatives().size());
        Assertions.assertTrue(extended.hasAdditionalStateDerivative("test-1"));
        Assertions.assertTrue(extended.hasAdditionalStateDerivative("test-2"));
        Assertions.assertEquals( 1.0, extended.getAdditionalStateDerivative("test-1")[0].getReal(), 1.0e-15);
        Assertions.assertEquals( 2.0, extended.getAdditionalStateDerivative("test-1")[1].getReal(), 1.0e-15);
        Assertions.assertEquals(42.0, extended.getAdditionalStateDerivative("test-2")[0].getReal(), 1.0e-15);

        // test most complete constructor
        T[] dd = MathArrays.buildArray(field, 1);
        dd[0] = zero.add(-6.0);
        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<T>(field);
        dictionary.put("test-3", dd);
        FieldSpacecraftState<T> s = new FieldSpacecraftState<>(state.getAbsPVA(), state.getAttitude(), state.getMass(), null, dictionary);
        Assertions.assertFalse(s.hasAdditionalData("test-3"));
        Assertions.assertEquals(-6.0, s.getAdditionalStateDerivative("test-3")[0].getReal(), 1.0e-15);

    }

    private <T extends CalculusFieldElement<T>> void doTestAdditionalTestResetOnEventAnalytical(final Field<T> field) {

        T zero = field.getZero();

        // Build orbit
        FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, 2000, 1, 1, TimeScalesFactory.getUTC());
        FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(7.1E6), zero, zero, zero, zero, zero,
                                         PositionAngleType.TRUE, FramesFactory.getGCRF(), date0,
                                         zero.add(Constants.WGS84_EARTH_MU));

        // Build propagator
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);

        // Create initial state with one additional state and add it to the propagator
        final String name = "A";
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit).
                addAdditionalData(name, zero.add(-1));

        propagator.resetInitialState(initialState);

        // Create date detector and handler
        FieldAbsoluteDate<T> changeDate = date0.shiftedBy(3);
        FieldDateDetector<T> dateDetector = new FieldDateDetector<>(field, changeDate).
                                    withHandler(new FieldEventHandler<T>() {

            @Override
            public Action eventOccurred(FieldSpacecraftState<T> s, FieldEventDetector<T> detector, boolean increasing) {
              return Action.RESET_STATE;
            }

            @Override
            public FieldSpacecraftState<T> resetState(FieldEventDetector<T> detector, FieldSpacecraftState<T> oldState) {
                return oldState.addAdditionalData(name, zero.add(+1));
            }

        });

        propagator.addEventDetector(dateDetector);
        propagator.setStepHandler(zero.add(0.125), s -> {
            if (s.getDate().durationFrom(changeDate).getReal() < -0.001) {
                Assertions.assertEquals(-1, s.getAdditionalData(name)[0].getReal(), 1.0e-15);
            } else if (s.getDate().durationFrom(changeDate).getReal() > +0.001) {
                Assertions.assertEquals(+1, s.getAdditionalData(name)[0].getReal(), 1.0e-15);
            }
        });
        FieldSpacecraftState<T> finalState = propagator.propagate(date0, date0.shiftedBy(5));
        Assertions.assertEquals(+1, finalState.getAdditionalData(name)[0].getReal(), 1.0e-15);

    }

    private <T extends CalculusFieldElement<T>> void doTestAdditionalTestResetOnEventNumerical(final Field<T> field) {

        T zero = field.getZero();

        // Build orbit
        FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, 2000, 1, 1, TimeScalesFactory.getUTC());
        FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(7.1E6), zero, zero, zero, zero, zero,
                                         PositionAngleType.TRUE, FramesFactory.getGCRF(), date0,
                                         zero.add(Constants.WGS84_EARTH_MU));

        // Build propagator
        FieldODEIntegrator<T> odeIntegrator = new DormandPrince853FieldIntegrator<>(field, 1E-3, 1E3, 1E-6, 1E-6);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, odeIntegrator);

        // Create initial state with one additional state and add it to the propagator
        final String name = "A";
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit).
                addAdditionalData(name, zero.add(-1));

        propagator.resetInitialState(initialState);

        // Create date detector and handler
        FieldAbsoluteDate<T> changeDate = date0.shiftedBy(3);
        FieldDateDetector<T> dateDetector = new FieldDateDetector<>(field, changeDate).
                                    withHandler(new FieldEventHandler<T>() {

            @Override
            public Action eventOccurred(FieldSpacecraftState<T> s, FieldEventDetector<T> detector, boolean increasing) {
              return Action.RESET_STATE;
            }

            @Override
            public FieldSpacecraftState<T> resetState(FieldEventDetector<T> detector, FieldSpacecraftState<T> oldState) {
                return oldState.addAdditionalData(name, zero.add(+1));
            }

        });

        propagator.addEventDetector(dateDetector);
        propagator.setStepHandler(zero.add(0.125), s -> {
            if (s.getDate().durationFrom(changeDate).getReal() < -0.001) {
                Assertions.assertEquals(-1, s.getAdditionalData(name)[0].getReal(), 1.0e-15);
            } else if (s.getDate().durationFrom(changeDate).getReal() > +0.001) {
                Assertions.assertEquals(+1, s.getAdditionalData(name)[0].getReal(), 1.0e-15);
            }
        });
        FieldSpacecraftState<T> finalState = propagator.propagate(date0, date0.shiftedBy(5));
        Assertions.assertEquals(+1, finalState.getAdditionalData(name)[0].getReal(), 1.0e-15);

    }

    private <T extends CalculusFieldElement<T>> void doTestShiftAdditionalDerivativesDouble(final Field<T> field) {

        T zero = field.getZero();

        // Build orbit
        FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, 2000, 1, 1, TimeScalesFactory.getUTC());
        FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(7.1E6), zero, zero, zero, zero, zero,
                                         PositionAngleType.TRUE, FramesFactory.getGCRF(), date0,
                                         zero.add(Constants.WGS84_EARTH_MU));

        // Build propagator
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);

        final String valueAndDerivative = "value-and-derivative";
        final String valueOnly          = "value-only";
        final String derivativeOnly     = "derivative-only";
        final FieldSpacecraftState<T> s0 = propagator.getInitialState().
                addAdditionalData(valueAndDerivative,           convert(field, new double[] { 1.0,  2.0 })).
                                           addAdditionalStateDerivative(valueAndDerivative, convert(field, new double[] { 3.0,  2.0 })).
                addAdditionalData(valueOnly,                    convert(field, new double[] { 5.0,  4.0 })).
                                           addAdditionalStateDerivative(derivativeOnly,     convert(field, new double[] { 1.0, -1.0 }));
        Assertions.assertEquals( 1.0, s0.getAdditionalData(valueAndDerivative)[0].getReal(),           1.0e-15);
        Assertions.assertEquals( 2.0, s0.getAdditionalData(valueAndDerivative)[1].getReal(),           1.0e-15);
        Assertions.assertEquals( 3.0, s0.getAdditionalStateDerivative(valueAndDerivative)[0].getReal(), 1.0e-15);
        Assertions.assertEquals( 2.0, s0.getAdditionalStateDerivative(valueAndDerivative)[1].getReal(), 1.0e-15);
        Assertions.assertEquals( 5.0, s0.getAdditionalData(valueOnly)[0].getReal(),                    1.0e-15);
        Assertions.assertEquals( 4.0, s0.getAdditionalData(valueOnly)[1].getReal(),                    1.0e-15);
        Assertions.assertEquals( 1.0, s0.getAdditionalStateDerivative(derivativeOnly)[0].getReal(),     1.0e-15);
        Assertions.assertEquals(-1.0, s0.getAdditionalStateDerivative(derivativeOnly)[1].getReal(),     1.0e-15);
        final FieldSpacecraftState<T> s1 = s0.shiftedBy(-2.0);
        Assertions.assertEquals(-5.0, s1.getAdditionalData(valueAndDerivative)[0].getReal(),           1.0e-15);
        Assertions.assertEquals(-2.0, s1.getAdditionalData(valueAndDerivative)[1].getReal(),           1.0e-15);
        Assertions.assertEquals( 3.0, s1.getAdditionalStateDerivative(valueAndDerivative)[0].getReal(), 1.0e-15);
        Assertions.assertEquals( 2.0, s1.getAdditionalStateDerivative(valueAndDerivative)[1].getReal(), 1.0e-15);
        Assertions.assertEquals( 5.0, s1.getAdditionalData(valueOnly)[0].getReal(),                    1.0e-15);
        Assertions.assertEquals( 4.0, s1.getAdditionalData(valueOnly)[1].getReal(),                    1.0e-15);
        Assertions.assertEquals( 1.0, s1.getAdditionalStateDerivative(derivativeOnly)[0].getReal(),     1.0e-15);
        Assertions.assertEquals(-1.0, s1.getAdditionalStateDerivative(derivativeOnly)[1].getReal(),     1.0e-15);

    }

    private <T extends CalculusFieldElement<T>> void doTestShiftAdditionalDerivativesField(final Field<T> field) {

        T zero = field.getZero();

        // Build orbit
        FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, 2000, 1, 1, TimeScalesFactory.getUTC());
        FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(7.1E6), zero, zero, zero, zero, zero,
                                         PositionAngleType.TRUE, FramesFactory.getGCRF(), date0,
                                         zero.add(Constants.WGS84_EARTH_MU));

        // Build propagator
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);

        final String valueAndDerivative = "value-and-derivative";
        final String valueOnly          = "value-only";
        final String derivativeOnly     = "derivative-only";
        final FieldSpacecraftState<T> s0 = propagator.getInitialState().
                addAdditionalData(valueAndDerivative,           convert(field, new double[] { 1.0,  2.0 })).
                                           addAdditionalStateDerivative(valueAndDerivative, convert(field, new double[] { 3.0,  2.0 })).
                addAdditionalData(valueOnly,                    convert(field, new double[] { 5.0,  4.0 })).
                                           addAdditionalStateDerivative(derivativeOnly,     convert(field, new double[] { 1.0, -1.0 }));
        Assertions.assertEquals( 1.0, s0.getAdditionalData(valueAndDerivative)[0].getReal(),           1.0e-15);
        Assertions.assertEquals( 2.0, s0.getAdditionalData(valueAndDerivative)[1].getReal(),           1.0e-15);
        Assertions.assertEquals( 3.0, s0.getAdditionalStateDerivative(valueAndDerivative)[0].getReal(), 1.0e-15);
        Assertions.assertEquals( 2.0, s0.getAdditionalStateDerivative(valueAndDerivative)[1].getReal(), 1.0e-15);
        Assertions.assertEquals( 5.0, s0.getAdditionalData(valueOnly)[0].getReal(),                    1.0e-15);
        Assertions.assertEquals( 4.0, s0.getAdditionalData(valueOnly)[1].getReal(),                    1.0e-15);
        Assertions.assertEquals( 1.0, s0.getAdditionalStateDerivative(derivativeOnly)[0].getReal(),     1.0e-15);
        Assertions.assertEquals(-1.0, s0.getAdditionalStateDerivative(derivativeOnly)[1].getReal(),     1.0e-15);
        final FieldSpacecraftState<T> s1 = s0.shiftedBy(field.getZero().newInstance(-2.0));
        Assertions.assertEquals(-5.0, s1.getAdditionalData(valueAndDerivative)[0].getReal(),           1.0e-15);
        Assertions.assertEquals(-2.0, s1.getAdditionalData(valueAndDerivative)[1].getReal(),           1.0e-15);
        Assertions.assertEquals( 3.0, s1.getAdditionalStateDerivative(valueAndDerivative)[0].getReal(), 1.0e-15);
        Assertions.assertEquals( 2.0, s1.getAdditionalStateDerivative(valueAndDerivative)[1].getReal(), 1.0e-15);
        Assertions.assertEquals( 5.0, s1.getAdditionalData(valueOnly)[0].getReal(),                    1.0e-15);
        Assertions.assertEquals( 4.0, s1.getAdditionalData(valueOnly)[1].getReal(),                    1.0e-15);
        Assertions.assertEquals( 1.0, s1.getAdditionalStateDerivative(derivativeOnly)[0].getReal(),     1.0e-15);
        Assertions.assertEquals(-1.0, s1.getAdditionalStateDerivative(derivativeOnly)[1].getReal(),     1.0e-15);

    }

    @BeforeEach
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
            rOrbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                                        FramesFactory.getEME2000(), rDate, mu);
            earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        } catch (OrekitException oe) {

            Assertions.fail(oe.getLocalizedMessage());
        }
    }

    private <T extends CalculusFieldElement<T>> T[] convert(final Field<T> field, final double[] a) {
        final T[] converted = MathArrays.buildArray(field, a.length);
        for (int i = 0; i < a.length; ++i) {
            converted[i] = field.getZero().newInstance(a[i]);
        }
        return converted;
    }

    private AbsoluteDate rDate;
    private double mu;
    private Orbit rOrbit;
    private OneAxisEllipsoid earth;

}

