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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class AttitudeTest {

    @Test
    public void testZeroRate() throws OrekitException {
        Attitude attitude = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(),
                                         new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                         Vector3D.ZERO, Vector3D.ZERO);
        Assert.assertEquals(Vector3D.ZERO, attitude.getSpin());
        double dt = 10.0;
        Attitude shifted = attitude.shiftedBy(dt);
        Assert.assertEquals(Vector3D.ZERO, shifted.getRotationAcceleration());
        Assert.assertEquals(Vector3D.ZERO, shifted.getSpin());
        Assert.assertEquals(0.0, Rotation.distance(attitude.getRotation(), shifted.getRotation()), 1.0e-15);
    }

    @Test
    public void testShift() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        Attitude attitude = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(),
                                         Rotation.IDENTITY,
                                         new Vector3D(rate, Vector3D.PLUS_K), Vector3D.ZERO);
        Assert.assertEquals(rate, attitude.getSpin().getNorm(), 1.0e-10);
        double dt = 10.0;
        double alpha = rate * dt;
        Attitude shifted = attitude.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getSpin().getNorm(), 1.0e-10);
        Assert.assertEquals(alpha, Rotation.distance(attitude.getRotation(), shifted.getRotation()), 1.0e-10);

        Vector3D xSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Assert.assertEquals(0.0, xSat.subtract(new Vector3D(FastMath.cos(alpha), FastMath.sin(alpha), 0)).getNorm(), 1.0e-10);
        Vector3D ySat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Assert.assertEquals(0.0, ySat.subtract(new Vector3D(-FastMath.sin(alpha), FastMath.cos(alpha), 0)).getNorm(), 1.0e-10);
        Vector3D zSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assert.assertEquals(0.0, zSat.subtract(Vector3D.PLUS_K).getNorm(), 1.0e-10);

    }

    @Test
    public void testSpin() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        Attitude attitude = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(),
                                         new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                         new Vector3D(rate, Vector3D.PLUS_K), Vector3D.ZERO);
        Assert.assertEquals(rate, attitude.getSpin().getNorm(), 1.0e-10);
        double dt = 10.0;
        Attitude shifted = attitude.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getSpin().getNorm(), 1.0e-10);
        Assert.assertEquals(rate * dt, Rotation.distance(attitude.getRotation(), shifted.getRotation()), 1.0e-10);

        Vector3D shiftedX  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D shiftedY  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Vector3D shiftedZ  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Vector3D originalX = attitude.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D originalY = attitude.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Vector3D originalZ = attitude.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assert.assertEquals( FastMath.cos(rate * dt), Vector3D.dotProduct(shiftedX, originalX), 1.0e-10);
        Assert.assertEquals( FastMath.sin(rate * dt), Vector3D.dotProduct(shiftedX, originalY), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedX, originalZ), 1.0e-10);
        Assert.assertEquals(-FastMath.sin(rate * dt), Vector3D.dotProduct(shiftedY, originalX), 1.0e-10);
        Assert.assertEquals( FastMath.cos(rate * dt), Vector3D.dotProduct(shiftedY, originalY), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedY, originalZ), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedZ, originalX), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedZ, originalY), 1.0e-10);
        Assert.assertEquals( 1.0,                 Vector3D.dotProduct(shiftedZ, originalZ), 1.0e-10);

        Vector3D forward = AngularCoordinates.estimateRate(attitude.getRotation(), shifted.getRotation(), dt);
        Assert.assertEquals(0.0, forward.subtract(attitude.getSpin()).getNorm(), 1.0e-10);

        Vector3D reversed = AngularCoordinates.estimateRate(shifted.getRotation(), attitude.getRotation(), dt);
        Assert.assertEquals(0.0, reversed.add(attitude.getSpin()).getNorm(), 1.0e-10);

    }

    @Test
    public void testInterpolation() throws OrekitException {

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
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        propagator.setAttitudeProvider(new BodyCenterPointing(initialOrbit.getFrame(), earth));
        final Attitude initialAttitude = propagator.propagate(initialOrbit.getDate()).getAttitude();

        // set up a 5 points sample
        List<Attitude> sample = new ArrayList<Attitude>();
        for (double dt = 0; dt < 251.0; dt += 60.0) {
            sample.add(propagator.propagate(date.shiftedBy(dt)).getAttitude());
        }

        // well inside the sample, interpolation should be better than quadratic shift
        double maxShiftAngleError = 0;
        double maxInterpolationAngleError = 0;
        double maxShiftRateError = 0;
        double maxInterpolationRateError = 0;
        for (double dt = 0; dt < 240.0; dt += 1.0) {
            AbsoluteDate t                 = initialOrbit.getDate().shiftedBy(dt);
            Attitude propagated            = propagator.propagate(t).getAttitude();
            double shiftAngleError         = Rotation.distance(propagated.getRotation(),
                                                               initialAttitude.shiftedBy(dt).getRotation());
            double interpolationAngleError = Rotation.distance(propagated.getRotation(),
                                                               initialAttitude.interpolate(t, sample).getRotation());
            double shiftRateError          = Vector3D.distance(propagated.getSpin(),
                                                               initialAttitude.shiftedBy(dt).getSpin());
            double interpolationRateError  = Vector3D.distance(propagated.getSpin(),
                                                               initialAttitude.interpolate(t, sample).getSpin());
            maxShiftAngleError             = FastMath.max(maxShiftAngleError, shiftAngleError);
            maxInterpolationAngleError     = FastMath.max(maxInterpolationAngleError, interpolationAngleError);
            maxShiftRateError              = FastMath.max(maxShiftRateError, shiftRateError);
            maxInterpolationRateError      = FastMath.max(maxInterpolationRateError, interpolationRateError);
        }
        Assert.assertTrue(maxShiftAngleError         > 4.0e-6);
        Assert.assertTrue(maxInterpolationAngleError < 1.5e-13);
        Assert.assertTrue(maxShiftRateError          > 6.0e-8);
        Assert.assertTrue(maxInterpolationRateError  < 2.5e-14);

        // past sample end, interpolation error should increase, but still be far better than quadratic shift
        maxShiftAngleError = 0;
        maxInterpolationAngleError = 0;
        maxShiftRateError = 0;
        maxInterpolationRateError = 0;
        for (double dt = 250.0; dt < 300.0; dt += 1.0) {
            AbsoluteDate t                 = initialOrbit.getDate().shiftedBy(dt);
            Attitude propagated            = propagator.propagate(t).getAttitude();
            double shiftAngleError         = Rotation.distance(propagated.getRotation(),
                                                               initialAttitude.shiftedBy(dt).getRotation());
            double interpolationAngleError = Rotation.distance(propagated.getRotation(),
                                                               initialAttitude.interpolate(t, sample).getRotation());
            double shiftRateError          = Vector3D.distance(propagated.getSpin(),
                                                               initialAttitude.shiftedBy(dt).getSpin());
            double interpolationRateError  = Vector3D.distance(propagated.getSpin(),
                                                               initialAttitude.interpolate(t, sample).getSpin());
            maxShiftAngleError             = FastMath.max(maxShiftAngleError, shiftAngleError);
            maxInterpolationAngleError     = FastMath.max(maxInterpolationAngleError, interpolationAngleError);
            maxShiftRateError              = FastMath.max(maxShiftRateError, shiftRateError);
            maxInterpolationRateError      = FastMath.max(maxInterpolationRateError, interpolationRateError);
        }
        Assert.assertTrue(maxShiftAngleError         > 9.0e-6);
        Assert.assertTrue(maxInterpolationAngleError < 6.0e-11);
        Assert.assertTrue(maxShiftRateError          > 9.0e-8);
        Assert.assertTrue(maxInterpolationRateError  < 4.0e-12);

    }

}

