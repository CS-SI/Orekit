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
package org.orekit.orbits;

import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.time.AbstractTimeInterpolator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.FieldPVCoordinates;

import java.util.ArrayList;
import java.util.List;

class FieldOrbitHermiteInterpolatorTest {

    private final Field<Binary64> field = Binary64Field.getInstance();

    @Test
    public void testCartesianInterpolationWithDerivatives() {
        // Non regression test
        doTestCartesianInterpolation(true, CartesianDerivativesFilter.USE_P,
                                     394, 0.1968, 3.21, 0.021630,
                                     2474, 2707.6418, 6.6, 26.28);

        // Non regression test
        doTestCartesianInterpolation(true, CartesianDerivativesFilter.USE_PV,
                                     394, 9.1042E-9, 3.21, 2.2348E-10,
                                     2474, 0.07998, 6.6, 0.001539);

        // Solution with PVA less precise than with PV only as the interpolating polynomial begins to oscillate heavily
        // outside the interpolating interval
        doTestCartesianInterpolation(true, CartesianDerivativesFilter.USE_PVA,
                                     394, 2.28e-8, 3.21, 1.39e-9,
                                     2474, 6826, 6.55, 186);
    }

    @Test
    public void testCartesianInterpolationWithoutDerivatives() {
        // Non regression test
        doTestCartesianInterpolation(false, CartesianDerivativesFilter.USE_P,
                                     394, 0.1968, 3.21, 0.02163,
                                     2474, 2707.6419, 6.55, 26.2826);

        // Non regression test
        doTestCartesianInterpolation(false, CartesianDerivativesFilter.USE_PV,
                                     394, 9.1042E-9, 3.21, 2.2348E-10,
                                     2474, 0.07998, 6.55, 0.001539);

        // Interpolation without derivatives is very wrong in PVA as we force first and second derivatives to be 0 i.e. we
        // give false information to the interpolator
        doTestCartesianInterpolation(false, CartesianDerivativesFilter.USE_PVA,
                                     394, 2.61, 3.21, 0.154,
                                     2474, 2.28e12, 6.55, 6.22e10);
    }

    @Test
    public void testCircularInterpolationWithDerivatives() {
        doTestCircularInterpolation(true,
                                    397, 1.88e-8,
                                    610, 3.52e-6,
                                    4870, 115);
    }

    @Test
    public void testCircularInterpolationWithoutDerivatives() {
        doTestCircularInterpolation(false,
                                    397, 0.0372,
                                    610.0, 1.23,
                                    4870, 8869);
    }

    @Test
    public void testEquinoctialInterpolationWithDerivatives() {
        doTestEquinoctialInterpolation(true,
                                       397, 1.17e-8,
                                       610, 4.49e-6,
                                       4870, 115);
    }

    @Test
    public void testEquinoctialInterpolationWithoutDerivatives() {
        doTestEquinoctialInterpolation(false,
                                       397, 0.0372,
                                       610.0, 1.23,
                                       4879, 8871);
    }

    @Test
    public void testKeplerianInterpolationWithDerivatives() {
        doTestKeplerianInterpolation(true,
                                     397, 4.01, 4.75e-4, 1.28e-7,
                                     2159, 1.05e7, 1.19e-3, 0.773);
    }

    @Test
    public void testKeplerianInterpolationWithoutDerivatives() {
        doTestKeplerianInterpolation(false,
                                     397, 62.0, 4.75e-4, 2.87e-6,
                                     2159, 79365, 1.19e-3, 3.89e-3);
    }

    private void doTestCartesianInterpolation(boolean useDerivatives, CartesianDerivativesFilter pvaFilter,
                                              double shiftPositionErrorWithin, double interpolationPositionErrorWithin,
                                              double shiftVelocityErrorWithin, double interpolationVelocityErrorWithin,
                                              double shiftPositionErrorFarPast, double interpolationPositionErrorFarPast,
                                              double shiftVelocityErrorFarPast, double interpolationVelocityErrorFarPast) {

        Binary64       zero = field.getZero();
        final Binary64 ehMu = zero.add(3.9860047e14);

        final double ae  = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;

        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(584.);
        final FieldVector3D<Binary64> position =
                new FieldVector3D<>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        final FieldVector3D<Binary64> velocity =
                new FieldVector3D<>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));
        final FieldCartesianOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                          FramesFactory.getEME2000(), date, ehMu);

        FieldEcksteinHechlerPropagator<Binary64> propagator =
                new FieldEcksteinHechlerPropagator<>(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);

        // set up a 5 points sample
        List<FieldOrbit<Binary64>> sample = new ArrayList<>();
        for (Binary64 dt = zero; dt.getReal() < 251.0; dt = dt.add(60.0)) {
            FieldOrbit<Binary64> orbit = propagator.propagate(date.shiftedBy(dt)).getOrbit();
            if (!useDerivatives) {
                // remove derivatives
                Binary64[] stateVector = MathArrays.buildArray(field, 6);
                orbit.getType().mapOrbitToArray(orbit, PositionAngleType.TRUE, stateVector, null);
                orbit = orbit.getType().mapArrayToOrbit(stateVector, null, PositionAngleType.TRUE,
                                                        orbit.getDate(), orbit.getMu(), orbit.getFrame());
            }
            sample.add(orbit);
        }

        // Create interpolator
        final FieldTimeInterpolator<FieldOrbit<Binary64>, Binary64> interpolator =
                new FieldOrbitHermiteInterpolator<>(sample.size(), 409, initialOrbit.getFrame(), pvaFilter);

        // well inside the sample, interpolation should be much better than Keplerian shift
        // this is because we take the full non-Keplerian acceleration into account in
        // the Cartesian parameters, which in this case is preserved by the
        // Eckstein-Hechler propagator
        double maxShiftPError         = 0;
        double maxInterpolationPError = 0;
        double maxShiftVError         = 0;
        double maxInterpolationVError = 0;
        for (Binary64 dt = zero; dt.getReal() < 240.0; dt = dt.add(1.0)) {
            FieldAbsoluteDate<Binary64>  t          = initialOrbit.getDate().shiftedBy(dt);
            FieldPVCoordinates<Binary64> propagated = propagator.propagate(t).getPVCoordinates();
            FieldPVCoordinates<Binary64> shiftError = new FieldPVCoordinates<>(propagated,
                                                                               initialOrbit.shiftedBy(dt.getReal())
                                                                                           .getPVCoordinates());
            FieldPVCoordinates<Binary64> interpolationError = new FieldPVCoordinates<>(propagated,
                                                                                       interpolator.interpolate(t, sample)
                                                                                                   .getPVCoordinates());
            maxShiftPError         = FastMath.max(maxShiftPError,
                                                  shiftError.getPosition().getNorm().getReal());
            maxInterpolationPError = FastMath.max(maxInterpolationPError,
                                                  interpolationError.getPosition().getNorm().getReal());
            maxShiftVError         = FastMath.max(maxShiftVError,
                                                  shiftError.getVelocity().getNorm().getReal());
            maxInterpolationVError = FastMath.max(maxInterpolationVError,
                                                  interpolationError.getVelocity().getNorm().getReal());
        }
        Assertions.assertEquals(shiftPositionErrorWithin, maxShiftPError, 0.01 * shiftPositionErrorWithin);
        Assertions.assertEquals(interpolationPositionErrorWithin, maxInterpolationPError,
                                0.01 * interpolationPositionErrorWithin);
        Assertions.assertEquals(shiftVelocityErrorWithin, maxShiftVError, 0.01 * shiftVelocityErrorWithin);
        Assertions.assertEquals(interpolationVelocityErrorWithin, maxInterpolationVError,
                                0.01 * interpolationVelocityErrorWithin);

        // if we go far past sample end, interpolation becomes worse than Keplerian shift
        maxShiftPError         = 0;
        maxInterpolationPError = 0;
        maxShiftVError         = 0;
        maxInterpolationVError = 0;
        for (Binary64 dt = zero.add(500.0); dt.getReal() < 650.0; dt = dt.add(1.0)) {
            FieldAbsoluteDate<Binary64>  t          = initialOrbit.getDate().shiftedBy(dt);
            FieldPVCoordinates<Binary64> propagated = propagator.propagate(t).getPVCoordinates();
            FieldPVCoordinates<Binary64> shiftError = new FieldPVCoordinates<>(propagated,
                                                                               initialOrbit.shiftedBy(dt)
                                                                                           .getPVCoordinates());
            FieldPVCoordinates<Binary64> interpolationError = new FieldPVCoordinates<>(propagated,
                                                                                       interpolator.interpolate(t, sample)
                                                                                                   .getPVCoordinates());
            maxShiftPError         = FastMath.max(maxShiftPError,
                                                  shiftError.getPosition().getNorm().getReal());
            maxInterpolationPError = FastMath.max(maxInterpolationPError,
                                                  interpolationError.getPosition().getNorm().getReal());
            maxShiftVError         = FastMath.max(maxShiftVError,
                                                  shiftError.getVelocity().getNorm().getReal());
            maxInterpolationVError = FastMath.max(maxInterpolationVError,
                                                  interpolationError.getVelocity().getNorm().getReal());
        }
        Assertions.assertEquals(shiftPositionErrorFarPast, maxShiftPError, 0.01 * shiftPositionErrorFarPast);
        Assertions.assertEquals(interpolationPositionErrorFarPast, maxInterpolationPError,
                                0.01 * interpolationPositionErrorFarPast);
        Assertions.assertEquals(shiftVelocityErrorFarPast, maxShiftVError, 0.01 * shiftVelocityErrorFarPast);
        Assertions.assertEquals(interpolationVelocityErrorFarPast, maxInterpolationVError,
                                0.01 * interpolationVelocityErrorFarPast);

    }

    private void doTestCircularInterpolation(boolean useDerivatives,
                                             double shiftErrorWithin, double interpolationErrorWithin,
                                             double shiftErrorSlightlyPast, double interpolationErrorSlightlyPast,
                                             double shiftErrorFarPast, double interpolationErrorFarPast) {

        Binary64                    zero = field.getZero();
        FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(field);

        final Binary64 ehMu = zero.add(3.9860047e14);
        final double   ae   = 6.378137e6;
        final double   c20  = -1.08263e-3;
        final double   c30  = 2.54e-6;
        final double   c40  = 1.62e-6;
        final double   c50  = 2.3e-7;
        final double   c60  = -5.5e-7;

        date = date.shiftedBy(584.);
        final FieldVector3D<Binary64> position =
                new FieldVector3D<>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        final FieldVector3D<Binary64> velocity = new FieldVector3D<>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));
        final FieldCircularOrbit<Binary64> initialOrbit =
                new FieldCircularOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                         FramesFactory.getEME2000(), date, ehMu);

        FieldEcksteinHechlerPropagator<Binary64> propagator =
                new FieldEcksteinHechlerPropagator<>(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);

        // set up a 5 points sample
        List<FieldOrbit<Binary64>> sample = new ArrayList<>();
        for (Binary64 dt = zero; dt.getReal() < 300.0; dt = dt.add(60.0)) {
            FieldOrbit<Binary64> orbit = OrbitType.CIRCULAR.convertType(propagator.propagate(date.shiftedBy(dt)).getOrbit());
            if (!useDerivatives) {
                // remove derivatives
                Binary64[] stateVector = MathArrays.buildArray(field, 6);
                orbit.getType().mapOrbitToArray(orbit, PositionAngleType.TRUE, stateVector, null);
                orbit = orbit.getType().mapArrayToOrbit(stateVector, null, PositionAngleType.TRUE,
                                                        orbit.getDate(), orbit.getMu(), orbit.getFrame());
            }
            sample.add(orbit);
        }

        // Create interpolator
        final FieldTimeInterpolator<FieldOrbit<Binary64>, Binary64> interpolator =
                new FieldOrbitHermiteInterpolator<>(sample.size(), 759, initialOrbit.getFrame(),
                                                    CartesianDerivativesFilter.USE_PVA);

        // well inside the sample, interpolation should be much better than Keplerian shift
        double maxShiftError         = 0.0;
        double maxInterpolationError = 0.0;
        for (Binary64 dt = zero; dt.getReal() < 241.0; dt = dt.add(1.0)) {
            FieldAbsoluteDate<Binary64> Binary64     = initialOrbit.getDate().shiftedBy(dt);
            FieldVector3D<Binary64>     shifted      = initialOrbit.shiftedBy(dt.getReal()).getPosition();
            FieldVector3D<Binary64>     interpolated = interpolator.interpolate(Binary64, sample).getPosition();
            FieldVector3D<Binary64>     propagated   = propagator.propagate(Binary64).getPosition();
            maxShiftError         = FastMath.max(maxShiftError, shifted.subtract(propagated).getNorm().getReal());
            maxInterpolationError =
                    FastMath.max(maxInterpolationError, interpolated.subtract(propagated).getNorm().getReal());
        }
        Assertions.assertEquals(shiftErrorWithin, maxShiftError, 0.01 * shiftErrorWithin);
        Assertions.assertEquals(interpolationErrorWithin, maxInterpolationError, 0.01 * interpolationErrorWithin);

        // slightly past sample end, interpolation should quickly increase, but remain reasonable
        maxShiftError         = 0;
        maxInterpolationError = 0;
        for (Binary64 dt = zero.add(240); dt.getReal() < 300.0; dt = dt.add(1.0)) {
            FieldAbsoluteDate<Binary64> Binary64     = initialOrbit.getDate().shiftedBy(dt);
            FieldVector3D<Binary64>     shifted      = initialOrbit.shiftedBy(dt).getPosition();
            FieldVector3D<Binary64>     interpolated = interpolator.interpolate(Binary64, sample).getPosition();
            FieldVector3D<Binary64>     propagated   = propagator.propagate(Binary64).getPosition();
            maxShiftError         = FastMath.max(maxShiftError, shifted.subtract(propagated).getNorm().getReal());
            maxInterpolationError =
                    FastMath.max(maxInterpolationError, interpolated.subtract(propagated).getNorm().getReal());
        }
        Assertions.assertEquals(shiftErrorSlightlyPast, maxShiftError, 0.01 * shiftErrorSlightlyPast);
        Assertions.assertEquals(interpolationErrorSlightlyPast, maxInterpolationError,
                                0.01 * interpolationErrorSlightlyPast);

        // far past sample end, interpolation should become really wrong
        // (in this test case, break even occurs at around 863 seconds, with a 3.9 km error)
        maxShiftError         = 0;
        maxInterpolationError = 0;
        for (Binary64 dt = zero.add(300); dt.getReal() < 1000; dt = dt.add(1.0)) {
            FieldAbsoluteDate<Binary64> t            = initialOrbit.getDate().shiftedBy(dt);
            FieldVector3D<Binary64>     shifted      = initialOrbit.shiftedBy(dt).getPosition();
            FieldVector3D<Binary64>     interpolated = interpolator.interpolate(t, sample).getPosition();
            FieldVector3D<Binary64>     propagated   = propagator.propagate(t).getPosition();
            maxShiftError         = FastMath.max(maxShiftError, shifted.subtract(propagated).getNorm().getReal());
            maxInterpolationError =
                    FastMath.max(maxInterpolationError, interpolated.subtract(propagated).getNorm().getReal());
        }
        Assertions.assertEquals(shiftErrorFarPast, maxShiftError, 0.01 * shiftErrorFarPast);
        Assertions.assertEquals(interpolationErrorFarPast, maxInterpolationError, 0.01 * interpolationErrorFarPast);

    }

    private void doTestEquinoctialInterpolation(boolean useDerivatives,
                                                double shiftErrorWithin, double interpolationErrorWithin,
                                                double shiftErrorSlightlyPast, double interpolationErrorSlightlyPast,
                                                double shiftErrorFarPast, double interpolationErrorFarPast) {

        Binary64                    zero = field.getZero();
        FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(field);

        final double ehMu = 3.9860047e14;
        final double ae   = 6.378137e6;
        final double c20  = -1.08263e-3;
        final double c30  = 2.54e-6;
        final double c40  = 1.62e-6;
        final double c50  = 2.3e-7;
        final double c60  = -5.5e-7;

        date = date.shiftedBy(584.);
        final FieldVector3D<Binary64> position =
                new FieldVector3D<>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        final FieldVector3D<Binary64> velocity = new FieldVector3D<>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));
        final FieldEquinoctialOrbit<Binary64> initialOrbit =
                new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                            FramesFactory.getEME2000(), date, zero.add(ehMu));

        FieldEcksteinHechlerPropagator<Binary64> propagator =
                new FieldEcksteinHechlerPropagator<>(initialOrbit, ae, zero.add(ehMu), c20, c30, c40, c50, c60);

        // set up a 5 points sample
        List<FieldOrbit<Binary64>> sample = new ArrayList<>();
        for (double dt = 0; dt < 300.0; dt += 60.0) {
            FieldOrbit<Binary64> orbit =
                    OrbitType.EQUINOCTIAL.convertType(propagator.propagate(date.shiftedBy(dt)).getOrbit());
            if (!useDerivatives) {
                // remove derivatives
                Binary64[] stateVector = MathArrays.buildArray(field, 6);
                orbit.getType().mapOrbitToArray(orbit, PositionAngleType.TRUE, stateVector, null);
                orbit = orbit.getType().mapArrayToOrbit(stateVector, null, PositionAngleType.TRUE,
                                                        orbit.getDate(), orbit.getMu(), orbit.getFrame());
            }
            sample.add(orbit);
        }

        // Create interpolator
        final FieldTimeInterpolator<FieldOrbit<Binary64>, Binary64> interpolator =
                new FieldOrbitHermiteInterpolator<>(sample.size(), 759, initialOrbit.getFrame(),
                                                    CartesianDerivativesFilter.USE_PVA);

        // well inside the sample, interpolation should be much better than Keplerian shift
        double maxShiftError         = 0;
        double maxInterpolationError = 0;
        for (Binary64 dt = zero; dt.getReal() < 241.0; dt = dt.add(1.0)) {
            FieldAbsoluteDate<Binary64> t            = initialOrbit.getDate().shiftedBy(dt);
            FieldVector3D<Binary64>     shifted      = initialOrbit.shiftedBy(dt.getReal()).getPosition();
            FieldVector3D<Binary64>     interpolated = interpolator.interpolate(t, sample).getPosition();
            FieldVector3D<Binary64>     propagated   = propagator.propagate(t).getPosition();
            maxShiftError         = FastMath.max(maxShiftError, shifted.subtract(propagated).getNorm().getReal());
            maxInterpolationError =
                    FastMath.max(maxInterpolationError, interpolated.subtract(propagated).getNorm().getReal());
        }
        Assertions.assertEquals(shiftErrorWithin, maxShiftError, 0.01 * shiftErrorWithin);
        Assertions.assertEquals(interpolationErrorWithin, maxInterpolationError, 0.01 * interpolationErrorWithin);

        // slightly past sample end, interpolation should quickly increase, but remain reasonable
        maxShiftError         = 0;
        maxInterpolationError = 0;
        for (Binary64 dt = zero.add(240); dt.getReal() < 300.0; dt = dt.add(1.0)) {
            FieldAbsoluteDate<Binary64> t            = initialOrbit.getDate().shiftedBy(dt);
            FieldVector3D<Binary64>     shifted      = initialOrbit.shiftedBy(dt).getPosition();
            FieldVector3D<Binary64>     interpolated = interpolator.interpolate(t, sample).getPosition();
            FieldVector3D<Binary64>     propagated   = propagator.propagate(t).getPosition();
            maxShiftError         = FastMath.max(maxShiftError, shifted.subtract(propagated).getNorm().getReal());
            maxInterpolationError =
                    FastMath.max(maxInterpolationError, interpolated.subtract(propagated).getNorm().getReal());
        }
        Assertions.assertEquals(shiftErrorSlightlyPast, maxShiftError, 0.01 * shiftErrorSlightlyPast);
        Assertions.assertEquals(interpolationErrorSlightlyPast, maxInterpolationError,
                                0.01 * interpolationErrorSlightlyPast);

        // far past sample end, interpolation should become really wrong
        // (in this test case, break even occurs at around 863 seconds, with a 3.9 km error)
        maxShiftError         = 0;
        maxInterpolationError = 0;
        for (Binary64 dt = zero.add(300); dt.getReal() < 1000; dt = dt.add(1.0)) {
            FieldAbsoluteDate<Binary64> t            = initialOrbit.getDate().shiftedBy(dt);
            FieldVector3D<Binary64>     shifted      = initialOrbit.shiftedBy(dt).getPosition();
            FieldVector3D<Binary64>     interpolated = interpolator.interpolate(t, sample).getPosition();
            FieldVector3D<Binary64>     propagated   = propagator.propagate(t).getPosition();
            maxShiftError         = FastMath.max(maxShiftError, shifted.subtract(propagated).getNorm().getReal());
            maxInterpolationError =
                    FastMath.max(maxInterpolationError, interpolated.subtract(propagated).getNorm().getReal());
        }
        Assertions.assertEquals(shiftErrorFarPast, maxShiftError, 0.01 * shiftErrorFarPast);
        Assertions.assertEquals(interpolationErrorFarPast, maxInterpolationError, 0.01 * interpolationErrorFarPast);

    }

    private void doTestKeplerianInterpolation(boolean useDerivatives,
                                              double shiftPositionErrorWithin, double interpolationPositionErrorWithin,
                                              double shiftEccentricityErrorWithin,
                                              double interpolationEccentricityErrorWithin,
                                              double shiftPositionErrorSlightlyPast,
                                              double interpolationPositionErrorSlightlyPast,
                                              double shiftEccentricityErrorSlightlyPast,
                                              double interpolationEccentricityErrorSlightlyPast) {

        final Binary64 zero = field.getZero();
        final Binary64 ehMu = zero.add(3.9860047e14);
        final double   ae   = 6.378137e6;
        final double   c20  = -1.08263e-3;
        final double   c30  = 2.54e-6;
        final double   c40  = 1.62e-6;
        final double   c50  = 2.3e-7;
        final double   c60  = -5.5e-7;

        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(584.);
        final FieldVector3D<Binary64> position =
                new FieldVector3D<>(field.getZero().add(3220103.), field.getZero().add(69623.),
                                    field.getZero().add(6449822.));
        final FieldVector3D<Binary64> velocity =
                new FieldVector3D<>(field.getZero().add(6414.7), field.getZero().add(-2006.), field.getZero().add(-3180.));
        final FieldKeplerianOrbit<Binary64> initialOrbit =
                new FieldKeplerianOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                          FramesFactory.getEME2000(), date, ehMu);

        FieldEcksteinHechlerPropagator<Binary64> propagator =
                new FieldEcksteinHechlerPropagator<>(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);

        // set up a 5 points sample
        List<FieldOrbit<Binary64>> sample = new ArrayList<>();
        for (double dt = 0; dt < 300.0; dt += 60.0) {
            FieldOrbit<Binary64> orbit =
                    OrbitType.KEPLERIAN.convertType(propagator.propagate(date.shiftedBy(dt)).getOrbit());
            if (!useDerivatives) {
                // remove derivatives
                Binary64[] stateVector = MathArrays.buildArray(field, 6);
                orbit.getType().mapOrbitToArray(orbit, PositionAngleType.TRUE, stateVector, null);
                orbit = orbit.getType().mapArrayToOrbit(stateVector, null, PositionAngleType.TRUE,
                                                        orbit.getDate(), orbit.getMu(), orbit.getFrame());
            }
            sample.add(orbit);
        }

        // Create interpolator
        final FieldTimeInterpolator<FieldOrbit<Binary64>, Binary64> interpolator =
                new FieldOrbitHermiteInterpolator<>(sample.size(), 359, initialOrbit.getFrame(),
                                                    CartesianDerivativesFilter.USE_PVA);

        // well inside the sample, interpolation should be slightly better than Keplerian shift
        // the relative bad behaviour here is due to eccentricity, which cannot be
        // accurately interpolated with a polynomial in this case
        double maxShiftPositionError             = 0;
        double maxInterpolationPositionError     = 0;
        double maxShiftEccentricityError         = 0;
        double maxInterpolationEccentricityError = 0;
        for (double dt = 0; dt < 241.0; dt += 1.0) {
            FieldAbsoluteDate<Binary64> t             = initialOrbit.getDate().shiftedBy(dt);
            FieldVector3D<Binary64>     shiftedP      = initialOrbit.shiftedBy(dt).getPosition();
            FieldVector3D<Binary64>     interpolatedP = interpolator.interpolate(t, sample).getPosition();
            FieldVector3D<Binary64>     propagatedP   = propagator.propagate(t).getPosition();
            Binary64                    shiftedE      = initialOrbit.shiftedBy(zero.add(dt)).getE();
            Binary64                    interpolatedE = interpolator.interpolate(t, sample).getE();
            Binary64                    propagatedE   = propagator.propagate(t).getE();
            maxShiftPositionError             =
                    FastMath.max(maxShiftPositionError, shiftedP.subtract(propagatedP).getNorm().getReal());
            maxInterpolationPositionError     =
                    FastMath.max(maxInterpolationPositionError, interpolatedP.subtract(propagatedP).getNorm().getReal());
            maxShiftEccentricityError         =
                    FastMath.max(maxShiftEccentricityError, shiftedE.subtract(propagatedE).abs().getReal());
            maxInterpolationEccentricityError =
                    FastMath.max(maxInterpolationEccentricityError, interpolatedE.subtract(propagatedE).abs().getReal());
        }
        Assertions.assertEquals(shiftPositionErrorWithin, maxShiftPositionError, 0.01 * shiftPositionErrorWithin);
        Assertions.assertEquals(interpolationPositionErrorWithin, maxInterpolationPositionError,
                                0.01 * interpolationPositionErrorWithin);
        Assertions.assertEquals(shiftEccentricityErrorWithin, maxShiftEccentricityError,
                                0.01 * shiftEccentricityErrorWithin);
        Assertions.assertEquals(interpolationEccentricityErrorWithin, maxInterpolationEccentricityError,
                                0.01 * interpolationEccentricityErrorWithin);

        // slightly past sample end, bad eccentricity interpolation shows up
        // (in this case, interpolated eccentricity exceeds 1.0 between 1900
        // and 1910s, while semi-major axis remains positive, so this is not
        // even a proper hyperbolic orbit...)
        maxShiftPositionError             = 0;
        maxInterpolationPositionError     = 0;
        maxShiftEccentricityError         = 0;
        maxInterpolationEccentricityError = 0;
        for (double dt = 240; dt < 600; dt += 1.0) {
            FieldAbsoluteDate<Binary64> t             = initialOrbit.getDate().shiftedBy(dt);
            FieldVector3D<Binary64>     shiftedP      = initialOrbit.shiftedBy(zero.add(dt)).getPosition();
            FieldVector3D<Binary64>     interpolatedP = interpolator.interpolate(t, sample).getPosition();
            FieldVector3D<Binary64>     propagatedP   = propagator.propagate(t).getPosition();
            Binary64                    shiftedE      = initialOrbit.shiftedBy(zero.add(dt)).getE();
            Binary64                    interpolatedE = interpolator.interpolate(t, sample).getE();
            Binary64                    propagatedE   = propagator.propagate(t).getE();
            maxShiftPositionError             =
                    FastMath.max(maxShiftPositionError, shiftedP.subtract(propagatedP).getNorm().getReal());
            maxInterpolationPositionError     =
                    FastMath.max(maxInterpolationPositionError, interpolatedP.subtract(propagatedP).getNorm().getReal());
            maxShiftEccentricityError         =
                    FastMath.max(maxShiftEccentricityError, shiftedE.subtract(propagatedE).abs().getReal());
            maxInterpolationEccentricityError =
                    FastMath.max(maxInterpolationEccentricityError, interpolatedE.subtract(propagatedE).abs().getReal());
        }
        Assertions.assertEquals(shiftPositionErrorSlightlyPast, maxShiftPositionError,
                                0.01 * shiftPositionErrorSlightlyPast);
        Assertions.assertEquals(interpolationPositionErrorSlightlyPast, maxInterpolationPositionError,
                                0.01 * interpolationPositionErrorSlightlyPast);
        Assertions.assertEquals(shiftEccentricityErrorSlightlyPast, maxShiftEccentricityError,
                                0.01 * shiftEccentricityErrorSlightlyPast);
        Assertions.assertEquals(interpolationEccentricityErrorSlightlyPast, maxInterpolationEccentricityError,
                                0.01 * interpolationEccentricityErrorSlightlyPast);

    }

    @Test
    @DisplayName("test default constructor")
    void testDefaultConstructor() {
        // Given
        final int interpolationPoints = 2;

        // When
        final FieldOrbitHermiteInterpolator<Binary64> interpolator =
                new FieldOrbitHermiteInterpolator<>(FramesFactory.getGCRF());

        // Then
        Assertions.assertEquals(AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
                                interpolator.getExtrapolationThreshold());
        Assertions.assertEquals(interpolationPoints,
                                interpolator.getNbInterpolationPoints());
        Assertions.assertEquals(CartesianDerivativesFilter.USE_PVA, interpolator.getPVAFilter());
    }
}