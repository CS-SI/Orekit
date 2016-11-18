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
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.fieldattitudes.FieldAttitude;
import org.orekit.frames.FramesFactory;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldAngularCoordinates;

public class FieldAttitudeTest {



    @Test
    public void testZeroRate() throws OrekitException {
        testShift(Decimal64Field.getInstance());
        testSpin(Decimal64Field.getInstance());
    }

    public <T extends RealFieldElement<T>> void testShift(final Field<T> field){
        T zero = field.getZero();
        T one = field.getOne();
        T rate = zero.add(2 * FastMath.PI / (12 * 60));
        FieldAttitude<T> attitude = new FieldAttitude<T>(new FieldAbsoluteDate<T>(field), FramesFactory.getEME2000(),
                        new FieldRotation<T>(one, zero, zero, zero, false),
                                         new FieldVector3D<T>(rate, new FieldVector3D<T>(zero, zero, one)), new FieldVector3D<T>(zero, zero, zero));
        Assert.assertEquals(rate.getReal(), attitude.getSpin().getNorm().getReal(), 1.0e-10);
        T dt = zero.add(10.0);
        T alpha = rate.multiply(dt);
        FieldAttitude<T> shifted = attitude.shiftedBy(dt);
        Assert.assertEquals(rate.getReal(), shifted.getSpin().getNorm().getReal(), 1.0e-10);
        Assert.assertEquals(alpha.getReal(), FieldRotation.distance(attitude.getRotation(), shifted.getRotation()).getReal(), 1.0e-10);

        FieldVector3D<T> xSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Assert.assertEquals(0.0, xSat.subtract(new FieldVector3D<T>(alpha.cos(), alpha.sin(), zero)).getNorm().getReal(), 1.0e-10);
        FieldVector3D<T> ySat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Assert.assertEquals(0.0, ySat.subtract(new FieldVector3D<T>(alpha.sin().multiply(-1), alpha.cos(), zero)).getNorm().getReal(), 1.0e-10);
        FieldVector3D<T> zSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assert.assertEquals(0.0, zSat.subtract(Vector3D.PLUS_K).getNorm().getReal(), 1.0e-10);

    }


    public <T extends RealFieldElement<T>> void testSpin(final Field<T> field) throws OrekitException {
        T zero = field.getZero();
        T one = field.getOne();
        T rate = zero.add(2 * FastMath.PI / (12 * 60));
        FieldAttitude<T> attitude = new FieldAttitude<T>(new FieldAbsoluteDate<T>(field), FramesFactory.getEME2000(),
                                         new FieldRotation<T>(zero.add(0.48), zero.add(0.64), zero.add(0.36), zero.add(0.48), false),
                                         new FieldVector3D<T>(rate, new FieldVector3D<T>(zero, zero, one)),new FieldVector3D<T>(zero,zero,zero));
        Assert.assertEquals(rate.getReal(), attitude.getSpin().getNorm().getReal(), 1.0e-10);
        T dt = zero.add(10.0);
        FieldAttitude<T> shifted = attitude.shiftedBy(dt);
        Assert.assertEquals(rate.getReal(), shifted.getSpin().getNorm().getReal(), 1.0e-10);
        Assert.assertEquals(rate.multiply(dt).getReal(), FieldRotation.distance(attitude.getRotation(), shifted.getRotation()).getReal(), 1.0e-10);

        FieldVector3D<T> shiftedX  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        FieldVector3D<T> shiftedY  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        FieldVector3D<T> shiftedZ  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        FieldVector3D<T> originalX = attitude.getRotation().applyInverseTo(Vector3D.PLUS_I);
        FieldVector3D<T> originalY = attitude.getRotation().applyInverseTo(Vector3D.PLUS_J);
        FieldVector3D<T> originalZ = attitude.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assert.assertEquals( FastMath.cos(rate.getReal() * dt.getReal()), FieldVector3D.dotProduct(shiftedX, originalX).getReal(), 1.0e-10);
        Assert.assertEquals( FastMath.sin(rate.getReal() * dt.getReal()), FieldVector3D.dotProduct(shiftedX, originalY).getReal(), 1.0e-10);
        Assert.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedX, originalZ).getReal(), 1.0e-10);
        Assert.assertEquals(-FastMath.sin(rate.getReal() * dt.getReal()), FieldVector3D.dotProduct(shiftedY, originalX).getReal(), 1.0e-10);
        Assert.assertEquals( FastMath.cos(rate.getReal() * dt.getReal()), FieldVector3D.dotProduct(shiftedY, originalY).getReal(), 1.0e-10);
        Assert.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedY, originalZ).getReal(), 1.0e-10);
        Assert.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedZ, originalX).getReal(), 1.0e-10);
        Assert.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedZ, originalY).getReal(), 1.0e-10);
        Assert.assertEquals( 1.0,                 FieldVector3D.dotProduct(shiftedZ, originalZ).getReal(), 1.0e-10);

        FieldVector3D<T> forward = FieldAngularCoordinates.estimateRate(attitude.getRotation(), shifted.getRotation(), dt);
        Assert.assertEquals(0.0, forward.subtract(attitude.getSpin()).getNorm().getReal(), 1.0e-10);

        FieldVector3D<T> reversed = FieldAngularCoordinates.estimateRate(shifted.getRotation(), attitude.getRotation(), dt);
        Assert.assertEquals(0.0, reversed.add(attitude.getSpin()).getNorm().getReal(), 1.0e-10);

    }

//    public <T extends RealFieldElement<T>> void testInterpolation(final Field<T> field) throws OrekitException {
//        T zero = field.getZero();
//        T one = field.getOne();
//        Utils.setDataRoot("regular-data");
//        final double ehMu =zero.add( 3.9860047e14);
//        final T ae   =zero.add( 6.378137e6);
//        final T c20  =zero.add( -1.08263e-3);
//        final T c30  =zero.add( 2.54e-6);
//        final T c40  =zero.add( 1.62e-6);
//        final T c50  =zero.add( 2.3e-7);
//        final T c60  =zero.add( -5.5e-7);
//
//        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field).shiftedBy(584.);
//        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
//        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));
//        final CircularOrbit initialOrbit = new CircularOrbit(new PVCoordinates(position.toVector3D(), velocity.toVector3D()),
//                                                             FramesFactory.getEME2000(), date.toAbsoluteDate(), ehMu);
//
//        EcksteinHechlerPropagator propagator =
//                new EcksteinHechlerPropagator(initialOrbit, ae.getReal(), ehMu, c20.getReal(), c30.getReal(), c40.getReal(), c50.getReal(), c60.getReal());
//        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
//                                                      Constants.WGS84_EARTH_FLATTENING,
//                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
//        propagator.setAttitudeProvider(new BodyCenterPointing(initialOrbit.getFrame(), earth));
//        final FieldAttitude<T> initialAttitude = propagator.propagate(initialOrbit.getDate()).getAttitude();
//
//        // set up a 5 points sample
//        List<Attitude> sample = new ArrayList<Attitude>();
//        for (double dt = 0; dt < 251.0; dt += 60.0) {
//            sample.add(propagator.propagate(date.shiftedBy(dt).toAbsoluteDate()).getAttitude());
//        }
//
//        // well inside the sample, interpolation should be better than quadratic shift
//        double maxShiftAngleError = 0;
//        double maxInterpolationAngleError = 0;
//        double maxShiftRateError = 0;
//        double maxInterpolationRateError = 0;
//        for (double dt = 0; dt < 240.0; dt += 1.0) {
//            AbsoluteDate t                 = initialOrbit.getDate().shiftedBy(dt);
//            Attitude propagated            = propagator.propagate(t).getAttitude();
//            double shiftAngleError         = Rotation.distance(propagated.getRotation(),
//                                                               initialAttitude.shiftedBy(dt).getRotation());
//            double interpolationAngleError = Rotation.distance(propagated.getRotation(),
//                                                               initialAttitude.interpolate(t, sample).getRotation());
//            double shiftRateError          = Vector3D.distance(propagated.getSpin(),
//                                                               initialAttitude.shiftedBy(dt).getSpin());
//            double interpolationRateError  = Vector3D.distance(propagated.getSpin(),
//                                                               initialAttitude.interpolate(t, sample).getSpin());
//            maxShiftAngleError             = FastMath.max(maxShiftAngleError, shiftAngleError);
//            maxInterpolationAngleError     = FastMath.max(maxInterpolationAngleError, interpolationAngleError);
//            maxShiftRateError              = FastMath.max(maxShiftRateError, shiftRateError);
//            maxInterpolationRateError      = FastMath.max(maxInterpolationRateError, interpolationRateError);
//        }
//        Assert.assertTrue(maxShiftAngleError         > 4.0e-6);
//        Assert.assertTrue(maxInterpolationAngleError < 1.5e-13);
//        Assert.assertTrue(maxShiftRateError          > 6.0e-8);
//        Assert.assertTrue(maxInterpolationRateError  < 2.5e-14);
//
//        // past sample end, interpolation error should increase, but still be far better than quadratic shift
//        maxShiftAngleError = 0;
//        maxInterpolationAngleError = 0;
//        maxShiftRateError = 0;
//        maxInterpolationRateError = 0;
//        for (double dt = 250.0; dt < 300.0; dt += 1.0) {
//            AbsoluteDate t                 = initialOrbit.getDate().shiftedBy(dt);
//            Attitude propagated            = propagator.propagate(t).getAttitude();
//            double shiftAngleError         = Rotation.distance(propagated.getRotation(),
//                                                               initialAttitude.shiftedBy(dt).getRotation());
//            double interpolationAngleError = Rotation.distance(propagated.getRotation(),
//                                                               initialAttitude.interpolate(t, sample).getRotation());
//            double shiftRateError          = Vector3D.distance(propagated.getSpin(),
//                                                               initialAttitude.shiftedBy(dt).getSpin());
//            double interpolationRateError  = Vector3D.distance(propagated.getSpin(),
//                                                               initialAttitude.interpolate(t, sample).getSpin());
//            maxShiftAngleError             = FastMath.max(maxShiftAngleError, shiftAngleError);
//            maxInterpolationAngleError     = FastMath.max(maxInterpolationAngleError, interpolationAngleError);
//            maxShiftRateError              = FastMath.max(maxShiftRateError, shiftRateError);
//            maxInterpolationRateError      = FastMath.max(maxInterpolationRateError, interpolationRateError);
//        }
//        Assert.assertTrue(maxShiftAngleError         > 9.0e-6);
//        Assert.assertTrue(maxInterpolationAngleError < 6.0e-11);
//        Assert.assertTrue(maxShiftRateError          > 9.0e-8);
//        Assert.assertTrue(maxInterpolationRateError  < 4.0e-12);
//
//    }

}

