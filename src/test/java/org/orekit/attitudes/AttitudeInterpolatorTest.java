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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterpolator;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinatesHermiteInterpolator;

import java.util.ArrayList;
import java.util.List;

class AttitudeInterpolatorTest {

    @Test
    public void testInterpolation() {

        // Given
        Utils.setDataRoot("regular-data");
        final double ehMu = 3.9860047e14;
        final double ae   = 6.378137e6;
        final double c20  = -1.08263e-3;
        final double c30  = 2.54e-6;
        final double c40  = 1.62e-6;
        final double c50  = 2.3e-7;
        final double c60  = -5.5e-7;

        final AbsoluteDate date     = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Vector3D     position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D     velocity = new Vector3D(6414.7, -2006., -3180.);
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
        List<Attitude> sample = new ArrayList<>();
        for (double dt = 0; dt < 251.0; dt += 60.0) {
            sample.add(propagator.propagate(date.shiftedBy(dt)).getAttitude());
        }

        // Create interpolator
        final double extrapolationThreshold = 59;
        final TimeInterpolator<TimeStampedAngularCoordinates> angularInterpolator =
                new TimeStampedAngularCoordinatesHermiteInterpolator(sample.size(), extrapolationThreshold,
                                                                     AngularDerivativesFilter.USE_RR);

        final AttitudeInterpolator attitudeInterpolator =
                new AttitudeInterpolator(initialOrbit.getFrame(), angularInterpolator);

        // well inside the sample, interpolation should be better than quadratic shift
        double maxShiftAngleError         = 0;
        double maxInterpolationAngleError = 0;
        double maxShiftRateError          = 0;
        double maxInterpolationRateError  = 0;
        for (double dt = 0; dt < 240.0; dt += 1.0) {
            AbsoluteDate t          = initialOrbit.getDate().shiftedBy(dt);
            Attitude     propagated = propagator.propagate(t).getAttitude();
            double shiftAngleError = Rotation.distance(propagated.getRotation(),
                                                       initialAttitude.shiftedBy(dt).getRotation());
            double interpolationAngleError = Rotation.distance(propagated.getRotation(),
                                                               attitudeInterpolator.interpolate(t, sample).getRotation());
            double shiftRateError = Vector3D.distance(propagated.getSpin(),
                                                      initialAttitude.shiftedBy(dt).getSpin());
            double interpolationRateError = Vector3D.distance(propagated.getSpin(),
                                                              attitudeInterpolator.interpolate(t, sample).getSpin());
            maxShiftAngleError         = FastMath.max(maxShiftAngleError, shiftAngleError);
            maxInterpolationAngleError = FastMath.max(maxInterpolationAngleError, interpolationAngleError);
            maxShiftRateError          = FastMath.max(maxShiftRateError, shiftRateError);
            maxInterpolationRateError  = FastMath.max(maxInterpolationRateError, interpolationRateError);
        }
        Assertions.assertTrue(maxShiftAngleError > 4.0e-6);
        Assertions.assertTrue(maxInterpolationAngleError < 1.5e-13);
        Assertions.assertTrue(maxShiftRateError > 6.0e-8);
        Assertions.assertTrue(maxInterpolationRateError < 2.5e-14);

        // past sample end, interpolation error should increase, but still be far better than quadratic shift
        maxShiftAngleError         = 0;
        maxInterpolationAngleError = 0;
        maxShiftRateError          = 0;
        maxInterpolationRateError  = 0;
        for (double dt = 250.0; dt < 300.0; dt += 1.0) {
            AbsoluteDate t          = initialOrbit.getDate().shiftedBy(dt);
            Attitude     propagated = propagator.propagate(t).getAttitude();
            double shiftAngleError = Rotation.distance(propagated.getRotation(),
                                                       initialAttitude.shiftedBy(dt).getRotation());
            double interpolationAngleError = Rotation.distance(propagated.getRotation(),
                                                               attitudeInterpolator.interpolate(t, sample).getRotation());
            double shiftRateError = Vector3D.distance(propagated.getSpin(),
                                                      initialAttitude.shiftedBy(dt).getSpin());
            double interpolationRateError = Vector3D.distance(propagated.getSpin(),
                                                              attitudeInterpolator.interpolate(t, sample).getSpin());
            maxShiftAngleError         = FastMath.max(maxShiftAngleError, shiftAngleError);
            maxInterpolationAngleError = FastMath.max(maxInterpolationAngleError, interpolationAngleError);
            maxShiftRateError          = FastMath.max(maxShiftRateError, shiftRateError);
            maxInterpolationRateError  = FastMath.max(maxInterpolationRateError, interpolationRateError);
        }
        Assertions.assertTrue(maxShiftAngleError > 9.0e-6);
        Assertions.assertTrue(maxInterpolationAngleError < 6.0e-11);
        Assertions.assertTrue(maxShiftRateError > 9.0e-8);
        Assertions.assertTrue(maxInterpolationRateError < 4.0e-12);

        Assertions.assertEquals(initialOrbit.getFrame(), attitudeInterpolator.getReferenceFrame());
        Assertions.assertEquals(angularInterpolator, attitudeInterpolator.getAngularInterpolator());

    }

    @Test
    @DisplayName("Test constructor")
    void testConstructor() {
        // Given
        final Frame frameMock = Mockito.mock(Frame.class);

        @SuppressWarnings("unchecked")
        final TimeInterpolator<TimeStampedAngularCoordinates> angularInterpolatorMock = Mockito.mock(TimeInterpolator.class);

        // When
        final AttitudeInterpolator attitudeInterpolator = new AttitudeInterpolator(frameMock, angularInterpolatorMock);

        // Then
        Assertions.assertEquals(frameMock, attitudeInterpolator.getReferenceFrame());
        Assertions.assertEquals(angularInterpolatorMock, attitudeInterpolator.getAngularInterpolator());
    }

    @Test
    @DisplayName("test error thrown when sample is too small")
    void testErrorThrownWhenSampleIsTooSmall() {
        // Given
        final AbsoluteDate interpolationDate = new AbsoluteDate();

        final List<Attitude> attitudes = new ArrayList<>();
        attitudes.add(Mockito.mock(Attitude.class));

        final TimeInterpolator<TimeStampedAngularCoordinates> angularInterpolator =
                new TimeStampedAngularCoordinatesHermiteInterpolator(2, AngularDerivativesFilter.USE_R);

        final TimeInterpolator<Attitude> attitudeInterpolator =
                new AttitudeInterpolator(FramesFactory.getGCRF(), angularInterpolator);

        // When & Then
        OrekitIllegalArgumentException thrown = Assertions.assertThrows(OrekitIllegalArgumentException.class,
                                                                        () -> attitudeInterpolator.interpolate(interpolationDate, attitudes));

        Assertions.assertEquals(OrekitMessages.NOT_ENOUGH_DATA, thrown.getSpecifier());
        Assertions.assertEquals(1, ((Integer) thrown.getParts()[0]).intValue());

    }

}