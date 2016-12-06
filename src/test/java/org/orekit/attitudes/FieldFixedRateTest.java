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


import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

public class FieldFixedRateTest {

    @Test
    public void testZeroRate() throws OrekitException {
        doTestZeroRate(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestZeroRate(final Field<T> field)
        throws OrekitException {
        final T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field,
                                                            new DateComponents(2004, 3, 2),
                                                            new TimeComponents(13, 17, 7.865),
                                                            TimeScalesFactory.getUTC());
        final Frame frame = FramesFactory.getEME2000();
        final FieldVector3D<T> zero3D = new FieldVector3D<>(zero, zero, zero);
        FieldFixedRate<T> law = new FieldFixedRate<>(new FieldAttitude<>(date, frame,
                                                                         new FieldRotation<>(zero.add(0.48),
                                                                                             zero.add(0.64),
                                                                                             zero.add(0.36),
                                                                                             zero.add(0.48),
                                                                                             false),
                                                                         zero3D, zero3D));
        FieldPVCoordinates<T> pv =
            new FieldPVCoordinates<>(field.getOne(),
                                     new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                                                       new Vector3D(0, 0, 3680.853673522056)));
        FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(pv, frame, date, 3.986004415e14);
        FieldRotation<T> attitude0 = law.getAttitude(orbit, date, frame).getRotation();
        Assert.assertEquals(0, FieldRotation.distance(attitude0, law.getReferenceAttitude().getRotation()).getReal(), 1.0e-10);
        FieldRotation<T> attitude1 = law.getAttitude(orbit.shiftedBy(zero.add(10.0)), date.shiftedBy(10.0), frame).getRotation();
        Assert.assertEquals(0, FieldRotation.distance(attitude1, law.getReferenceAttitude().getRotation()).getReal(), 1.0e-10);
        FieldRotation<T> attitude2 = law.getAttitude(orbit.shiftedBy(zero.add(20.0)), date.shiftedBy(20.0), frame).getRotation();
        Assert.assertEquals(0, FieldRotation.distance(attitude2, law.getReferenceAttitude().getRotation()).getReal(), 1.0e-10);

    }

    @Test
    public void testNonZeroRate() throws OrekitException {
        doTestNonZeroRate(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestNonZeroRate(final Field<T> field) throws OrekitException {
        final T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field,
                                                            new DateComponents(2004, 3, 2),
                                                            new TimeComponents(13, 17, 7.865),
                                                            TimeScalesFactory.getUTC());
        final T rate = zero.add(2 * FastMath.PI / (12 * 60));
        final Frame frame = FramesFactory.getEME2000();
        final FieldVector3D<T> zero3D = new FieldVector3D<>(zero, zero, zero);
        FieldFixedRate<T> law = new FieldFixedRate<>(new FieldAttitude<>(date, frame,
                                                                          new FieldRotation<>(zero.add(0.48),
                                                                                              zero.add(0.64),
                                                                                              zero.add(0.36),
                                                                                              zero.add(0.48),
                                                                                              false),
                                                                          new FieldVector3D<>(rate, Vector3D.PLUS_K),
                                                                          zero3D));
        FieldPVCoordinates<T> pv =
                        new FieldPVCoordinates<>(field.getOne(),
                                                 new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                                                                   new Vector3D(0, 0, 3680.853673522056)));
        FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(pv, FramesFactory.getEME2000(), date, 3.986004415e14);
        FieldRotation<T> attitude0 = law.getAttitude(orbit, date, frame).getRotation();
        Assert.assertEquals(0, FieldRotation.distance(attitude0, law.getReferenceAttitude().getRotation()).getReal(), 1.0e-10);
        FieldRotation<T> attitude1 = law.getAttitude(orbit.shiftedBy(zero.add(10.0)), date.shiftedBy(10.0), frame).getRotation();
        Assert.assertEquals(10 * rate.getReal(), FieldRotation.distance(attitude1, law.getReferenceAttitude().getRotation()).getReal(), 1.0e-10);
        FieldRotation<T> attitude2 = law.getAttitude(orbit.shiftedBy(zero.add(-20.0)), date.shiftedBy(-20.0), frame).getRotation();
        Assert.assertEquals(20 * rate.getReal(), FieldRotation.distance(attitude2, law.getReferenceAttitude().getRotation()).getReal(), 1.0e-10);
        Assert.assertEquals(30 * rate.getReal(), FieldRotation.distance(attitude2, attitude1).getReal(), 1.0e-10);
        FieldRotation<T> attitude3 = law.getAttitude(orbit.shiftedBy(zero.add(0.0)), date, frame).getRotation();
        Assert.assertEquals(0, FieldRotation.distance(attitude3, law.getReferenceAttitude().getRotation()).getReal(), 1.0e-10);

    }

    @Test
    public void testSpin() throws OrekitException {
        doTestSpin(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestSpin(final Field<T> field) throws OrekitException {

        final T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field,
                                                            new DateComponents(1970, 01, 01),
                                                            new TimeComponents(3, 25, 45.6789),
                                                            TimeScalesFactory.getUTC());

        final T rate = zero.add(2 * FastMath.PI / (12 * 60));
        final FieldVector3D<T> zero3D = new FieldVector3D<>(zero, zero, zero);
        FieldAttitudeProvider<T> law =
            new FieldFixedRate<>(new FieldAttitude<>(date, FramesFactory.getEME2000(),
                                                     new FieldRotation<>(zero.add(0.48),
                                                                         zero.add(0.64),
                                                                         zero.add(0.36),
                                                                         zero.add(0.48),
                                                                         false),
                                                     new FieldVector3D<>(rate, Vector3D.PLUS_K),
                                                     zero3D));

        FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7178000.0),
                                      zero.add(1.e-4),
                                      zero.add(FastMath.toRadians(50.)),
                                      zero.add(FastMath.toRadians(10.)),
                                      zero.add(FastMath.toRadians(20.)),
                                      zero.add(FastMath.toRadians(30.)), PositionAngle.MEAN,
                                      FramesFactory.getEME2000(), date, 3.986004415e14);

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
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        double errorAnglePlus      = FieldRotation.distance(s0.getAttitude().getRotation(),
                                                            sPlus.shiftedBy(h.negate()).getAttitude().getRotation()).getReal();
        double evolutionAnglePlus  = FieldRotation.distance(s0.getAttitude().getRotation(),
                                                            sPlus.getAttitude().getRotation()).getReal();
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        FieldVector3D<T> spin0 = s0.getAttitude().getSpin();
        FieldVector3D<T> reference = FieldAngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
                                                                          sPlus.getAttitude().getRotation(),
                                                                          h.multiply(2));
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm().getReal(), 1.0e-14);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

