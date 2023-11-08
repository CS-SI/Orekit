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
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

public class FixedRateTest {

    @Test
    public void testZeroRate() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 3, 2),
                                             new TimeComponents(13, 17, 7.865),
                                             TimeScalesFactory.getUTC());
        final Frame frame = FramesFactory.getEME2000();
        FixedRate law = new FixedRate(new Attitude(date, frame,
                                                   new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                                   Vector3D.ZERO, Vector3D.ZERO));
        PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                              new Vector3D(0, 0, 3680.853673522056));
        Orbit orbit = new KeplerianOrbit(pv, frame, date, 3.986004415e14);
        Rotation attitude0 = law.getAttitude(orbit, date, frame).getRotation();
        Assertions.assertEquals(0, Rotation.distance(attitude0, law.getReferenceAttitude().getRotation()), 1.0e-10);
        Rotation attitude1 = law.getAttitude(orbit.shiftedBy(10.0), date.shiftedBy(10.0), frame).getRotation();
        Assertions.assertEquals(0, Rotation.distance(attitude1, law.getReferenceAttitude().getRotation()), 1.0e-10);
        Rotation attitude2 = law.getAttitude(orbit.shiftedBy(20.0), date.shiftedBy(20.0), frame).getRotation();
        Assertions.assertEquals(0, Rotation.distance(attitude2, law.getReferenceAttitude().getRotation()), 1.0e-10);

    }

    @Test
    public void testNonZeroRate() {
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 3, 2),
                                                   new TimeComponents(13, 17, 7.865),
                                                   TimeScalesFactory.getUTC());
        final double rate = 2 * FastMath.PI / (12 * 60);
        final Frame frame = FramesFactory.getEME2000();
        final Frame gcrf  = FramesFactory.getGCRF();
        FixedRate law = new FixedRate(new Attitude(date, frame,
                                                   new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                                   new Vector3D(rate, Vector3D.PLUS_K), Vector3D.ZERO));
        final Rotation ref = law.getReferenceAttitude().getRotation().applyTo(gcrf.getTransformTo(frame, date).getRotation());
        PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                              new Vector3D(0, 0, 3680.853673522056));
        Orbit orbit = new KeplerianOrbit(pv, FramesFactory.getEME2000(), date, 3.986004415e14);
        Rotation attitude0 = law.getAttitude(orbit, date, gcrf).getRotation();
        Assertions.assertEquals(0, Rotation.distance(attitude0, ref), 1.0e-10);
        Rotation attitude1 = law.getAttitude(orbit.shiftedBy(10.0), date.shiftedBy(10.0), gcrf).getRotation();
        Assertions.assertEquals(10 * rate, Rotation.distance(attitude1, ref), 1.0e-10);
        Rotation attitude2 = law.getAttitude(orbit.shiftedBy(-20.0), date.shiftedBy(-20.0), gcrf).getRotation();
        Assertions.assertEquals(20 * rate, Rotation.distance(attitude2, ref), 1.0e-10);
        Assertions.assertEquals(30 * rate, Rotation.distance(attitude2, attitude1), 1.0e-10);
        Rotation attitude3 = law.getAttitude(orbit.shiftedBy(0.0), date, frame).getRotation();
        Assertions.assertEquals(0, Rotation.distance(attitude3, law.getReferenceAttitude().getRotation()), 1.0e-10);

    }

    @Test
    public void testSpin() {

        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());

        final double rate = 2 * FastMath.PI / (12 * 60);
        AttitudeProvider law =
            new FixedRate(new Attitude(date, FramesFactory.getEME2000(),
                                       new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                       new Vector3D(rate, Vector3D.PLUS_K),
                                       Vector3D.ZERO));

        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngleType.MEAN,
                              FramesFactory.getEME2000(), date, 3.986004415e14);

        Propagator propagator = new KeplerianPropagator(orbit, law);

        double h = 0.01;
        SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        SpacecraftState s0     = propagator.propagate(date);
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
        Assertions.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.0e-14);

    }

    @Test
    public void testZeroRateField() {
        doTestZeroRate(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestZeroRate(final Field<T> field)
        {
        final T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field,
                                                            new DateComponents(2004, 3, 2),
                                                            new TimeComponents(13, 17, 7.865),
                                                            TimeScalesFactory.getUTC());
        final Frame frame = FramesFactory.getEME2000();
        final Frame gcrf  = FramesFactory.getGCRF();
        FixedRate law = new FixedRate(new Attitude(date.toAbsoluteDate(), frame,
                                                   new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                                   Vector3D.ZERO, Vector3D.ZERO));
        final Rotation ref = law.getReferenceAttitude().getRotation().applyTo(gcrf.getTransformTo(frame, date.toAbsoluteDate()).getRotation());
        FieldPVCoordinates<T> pv =
            new FieldPVCoordinates<>(field.getOne(),
                                     new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                                                       new Vector3D(0, 0, 3680.853673522056)));
        FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(pv, frame, date, zero.add(3.986004415e14));
        FieldRotation<T> attitude0 = law.getAttitude(orbit, date, gcrf).getRotation();
        Assertions.assertEquals(0, Rotation.distance(attitude0.toRotation(), ref), 1.0e-10);
        FieldRotation<T> attitude1 = law.getAttitude(orbit.shiftedBy(zero.add(10.0)), date.shiftedBy(10.0), gcrf).getRotation();
        Assertions.assertEquals(0, Rotation.distance(attitude1.toRotation(), ref), 1.0e-10);
        FieldRotation<T> attitude2 = law.getAttitude(orbit.shiftedBy(zero.add(20.0)), date.shiftedBy(20.0), gcrf).getRotation();
        Assertions.assertEquals(0, Rotation.distance(attitude2.toRotation(), ref), 1.0e-10);

    }

    @Test
    public void testNonZeroRateField() {
        doTestNonZeroRate(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestNonZeroRate(final Field<T> field) {
        final T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field,
                                                            new DateComponents(2004, 3, 2),
                                                            new TimeComponents(13, 17, 7.865),
                                                            TimeScalesFactory.getUTC());
        final T rate = zero.add(2 * FastMath.PI / (12 * 60));
        final Frame frame = FramesFactory.getEME2000();
        FixedRate law = new FixedRate(new Attitude(date.toAbsoluteDate(), frame,
                                                   new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                                   new Vector3D(rate.getReal(), Vector3D.PLUS_K), Vector3D.ZERO));
        FieldPVCoordinates<T> pv =
                        new FieldPVCoordinates<>(field.getOne(),
                                                 new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                                                                   new Vector3D(0, 0, 3680.853673522056)));
        FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(pv, FramesFactory.getEME2000(), date, zero.add(3.986004415e14));
        FieldRotation<T> attitude0 = law.getAttitude(orbit, date, frame).getRotation();
        Assertions.assertEquals(0, Rotation.distance(attitude0.toRotation(), law.getReferenceAttitude().getRotation()), 1.0e-10);
        FieldRotation<T> attitude1 = law.getAttitude(orbit.shiftedBy(zero.add(10.0)), date.shiftedBy(10.0), frame).getRotation();
        Assertions.assertEquals(10 * rate.getReal(), Rotation.distance(attitude1.toRotation(), law.getReferenceAttitude().getRotation()), 1.0e-10);
        FieldRotation<T> attitude2 = law.getAttitude(orbit.shiftedBy(zero.add(-20.0)), date.shiftedBy(-20.0), frame).getRotation();
        Assertions.assertEquals(20 * rate.getReal(), Rotation.distance(attitude2.toRotation(), law.getReferenceAttitude().getRotation()), 1.0e-10);
        Assertions.assertEquals(30 * rate.getReal(), Rotation.distance(attitude2.toRotation(), attitude1.toRotation()), 1.0e-10);
        FieldRotation<T> attitude3 = law.getAttitude(orbit.shiftedBy(zero.add(0.0)), date, frame).getRotation();
        Assertions.assertEquals(0, Rotation.distance(attitude3.toRotation(), law.getReferenceAttitude().getRotation()), 1.0e-10);

    }

    @Test
    public void testSpinField() {
        doTestSpin(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestSpin(final Field<T> field) {

        final T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field,
                                                            new DateComponents(1970, 01, 01),
                                                            new TimeComponents(3, 25, 45.6789),
                                                            TimeScalesFactory.getUTC());

        final T rate = zero.add(2 * FastMath.PI / (12 * 60));
        AttitudeProvider law =
                        new FixedRate(new Attitude(date.toAbsoluteDate(), FramesFactory.getEME2000(),
                                                   new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                                   new Vector3D(rate.getReal(), Vector3D.PLUS_K),
                                                   Vector3D.ZERO));

        FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7178000.0),
                                      zero.add(1.e-4),
                                      zero.add(FastMath.toRadians(50.)),
                                      zero.add(FastMath.toRadians(10.)),
                                      zero.add(FastMath.toRadians(20.)),
                                      zero.add(FastMath.toRadians(30.)), PositionAngleType.MEAN,
                                      FramesFactory.getEME2000(), date, zero.add(3.986004415e14));

        FieldPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit, law);

        T h = zero.add(0.01);
        FieldSpacecraftState<T> sMinus = propagator.propagate(date.shiftedBy(h.negate()));
        FieldSpacecraftState<T> s0     = propagator.propagate(date);
        FieldSpacecraftState<T> sPlus  = propagator.propagate(date.shiftedBy(h));

        // check spin is consistent with attitude evolution
        double errorAngleMinus     = FieldRotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(),
                                                            s0.getAttitude().getRotation()).getReal();
        double evolutionAngleMinus = FieldRotation.distance(sMinus.getAttitude().getRotation(),
                                                            s0.getAttitude().getRotation()).getReal();
        Assertions.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        double errorAnglePlus      = FieldRotation.distance(s0.getAttitude().getRotation(),
                                                            sPlus.shiftedBy(h.negate()).getAttitude().getRotation()).getReal();
        double evolutionAnglePlus  = FieldRotation.distance(s0.getAttitude().getRotation(),
                                                            sPlus.getAttitude().getRotation()).getReal();
        Assertions.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        FieldVector3D<T> spin0 = s0.getAttitude().getSpin();
        FieldVector3D<T> reference = FieldAngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
                                                                          sPlus.getAttitude().getRotation(),
                                                                          h.multiply(2));
        Assertions.assertEquals(0.0, spin0.subtract(reference).getNorm().getReal(), 1.0e-14);

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

