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

import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
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
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.time.TimeInterpolator;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinatesHermiteInterpolator;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinatesHermiteInterpolator;

import java.util.ArrayList;
import java.util.List;

public class FieldAttitudeInterpolatorTest {

    final Field<Binary64> field = Binary64Field.getInstance();

    @Test
    public void testInterpolation() {

        // Given
        Utils.setDataRoot("regular-data");
        final Binary64 ehMu = new Binary64(3.9860047e14);
        final double   ae   = 6.378137e6;
        final double   c20  = -1.08263e-3;
        final double   c30  = 2.54e-6;
        final double   c40  = 1.62e-6;
        final double   c50  = 2.3e-7;
        final double   c60  = -5.5e-7;

        final FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(field, new AbsoluteDate()).shiftedBy(584.);
        final FieldVector3D<Binary64> position = new FieldVector3D<>(new Binary64(3220103.),
                                                                     new Binary64(69623.),
                                                                     new Binary64(6449822.));
        final FieldVector3D<Binary64> velocity = new FieldVector3D<>(new Binary64(6414.7),
                                                                     new Binary64(-2006.),
                                                                     new Binary64(-3180.));
        final FieldCircularOrbit<Binary64> initialOrbit =
                new FieldCircularOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                         FramesFactory.getEME2000(), date, ehMu);

        FieldPropagator<Binary64> propagator =
                new FieldEcksteinHechlerPropagator<>(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        propagator.setAttitudeProvider(new BodyCenterPointing(initialOrbit.getFrame(), earth));
        final FieldAttitude<Binary64> initialAttitude = propagator.propagate(initialOrbit.getDate()).getAttitude();

        // set up a 5 points sample
        List<FieldAttitude<Binary64>> sample = new ArrayList<>();
        for (double dt = 0; dt < 251.0; dt += 60.0) {
            sample.add(propagator.propagate(date.shiftedBy(dt)).getAttitude());
        }

        // Create interpolator
        final double extrapolationThreshold = 59;
        final FieldTimeInterpolator<TimeStampedFieldAngularCoordinates<Binary64>, Binary64> angularInterpolator =
                new TimeStampedFieldAngularCoordinatesHermiteInterpolator<>(sample.size(), extrapolationThreshold,
                                                                            AngularDerivativesFilter.USE_RR);

        final FieldAttitudeInterpolator<Binary64> attitudeInterpolator =
                new FieldAttitudeInterpolator<>(initialOrbit.getFrame(), angularInterpolator);

        // well inside the sample, interpolation should be better than quadratic shift
        double maxShiftAngleError         = 0;
        double maxInterpolationAngleError = 0;
        double maxShiftRateError          = 0;
        double maxInterpolationRateError  = 0;
        for (double dt = 0; dt < 240.0; dt += 1.0) {
            FieldAbsoluteDate<Binary64> t          = initialOrbit.getDate().shiftedBy(dt);
            FieldAttitude<Binary64>     propagated = propagator.propagate(t).getAttitude();
            Binary64 shiftAngleError =
                    FieldRotation.distance(propagated.getRotation(), initialAttitude.shiftedBy(dt).getRotation());
            Binary64 interpolationAngleError = FieldRotation.distance(propagated.getRotation(),
                                                                      attitudeInterpolator.interpolate(t, sample)
                                                                                          .getRotation());
            Binary64 shiftRateError = FieldVector3D.distance(propagated.getSpin(),
                                                             initialAttitude.shiftedBy(dt).getSpin());
            Binary64 interpolationRateError = FieldVector3D.distance(propagated.getSpin(),
                                                                     attitudeInterpolator.interpolate(t, sample).getSpin());
            maxShiftAngleError         = FastMath.max(maxShiftAngleError, shiftAngleError.getReal());
            maxInterpolationAngleError = FastMath.max(maxInterpolationAngleError, interpolationAngleError.getReal());
            maxShiftRateError          = FastMath.max(maxShiftRateError, shiftRateError.getReal());
            maxInterpolationRateError  = FastMath.max(maxInterpolationRateError, interpolationRateError.getReal());
        }
        Assertions.assertEquals(0.0, maxShiftAngleError, 6.9e-6);
        Assertions.assertEquals(0.0, maxInterpolationAngleError, 8.8e-15);
        Assertions.assertEquals(0.0, maxShiftRateError, 7.6e-8);
        Assertions.assertEquals(0.0, maxInterpolationRateError, 2.0e-16);

        // past sample end, interpolation error should increase, but still be far better than quadratic shift
        maxShiftAngleError         = 0;
        maxInterpolationAngleError = 0;
        maxShiftRateError          = 0;
        maxInterpolationRateError  = 0;
        for (double dt = 250.0; dt < 300.0; dt += 1.0) {
            FieldAbsoluteDate<Binary64> t          = initialOrbit.getDate().shiftedBy(dt);
            FieldAttitude<Binary64>     propagated = propagator.propagate(t).getAttitude();
            Binary64 shiftAngleError = FieldRotation.distance(propagated.getRotation(),
                                                              initialAttitude.shiftedBy(dt).getRotation());
            Binary64 interpolationAngleError = FieldRotation.distance(propagated.getRotation(),
                                                                      attitudeInterpolator.interpolate(t, sample)
                                                                                          .getRotation());
            Binary64 shiftRateError = FieldVector3D.distance(propagated.getSpin(),
                                                             initialAttitude.shiftedBy(dt).getSpin());
            Binary64 interpolationRateError = FieldVector3D.distance(propagated.getSpin(),
                                                                     attitudeInterpolator.interpolate(t, sample).getSpin());
            maxShiftAngleError         = FastMath.max(maxShiftAngleError, shiftAngleError.getReal());
            maxInterpolationAngleError = FastMath.max(maxInterpolationAngleError, interpolationAngleError.getReal());
            maxShiftRateError          = FastMath.max(maxShiftRateError, shiftRateError.getReal());
            maxInterpolationRateError  = FastMath.max(maxInterpolationRateError, interpolationRateError.getReal());
        }
        Assertions.assertEquals(0.0, maxShiftAngleError, 1.3e-5);
        Assertions.assertEquals(0.0, maxInterpolationAngleError, 1.2e-12);
        Assertions.assertEquals(0.0, maxShiftRateError, 1.2e-7);
        Assertions.assertEquals(0.0, maxInterpolationRateError, 8.4e-14);

        Assertions.assertEquals(initialOrbit.getFrame(), attitudeInterpolator.getReferenceFrame());
        Assertions.assertEquals(angularInterpolator, attitudeInterpolator.getAngularInterpolator());

    }

    @Test
    @DisplayName("Test constructor")
    void testConstructor() {
        // Given
        final Frame frameMock = Mockito.mock(Frame.class);

        @SuppressWarnings("unchecked")
        final FieldTimeInterpolator<TimeStampedFieldAngularCoordinates<Binary64>, Binary64> angularInterpolatorMock =
                Mockito.mock(FieldTimeInterpolator.class);

        // When
        final FieldAttitudeInterpolator<Binary64> attitudeInterpolator =
                new FieldAttitudeInterpolator<>(frameMock, angularInterpolatorMock);

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
