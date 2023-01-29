/* Copyright 2023 Luc Maisonobe
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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

public class TorqueFreeTest {

    @Test
    public void testLocalBehavior() {
        AbsoluteDate initialDate = new AbsoluteDate(new DateComponents(2004, 3, 2),
                                                    new TimeComponents(13, 17, 7.865),
                                                    TimeScalesFactory.getUTC());
        final Frame frame = FramesFactory.getEME2000();
        final Attitude initialAttitude = new Attitude(initialDate, frame,
                                                      new Rotation(0.9, 0.437, 0.0, 0.0, true),
                                                      new Vector3D(0.05, 0.0, 0.04),
                                                      Vector3D.ZERO);
        final Inertia inertia = new Inertia(new InertiaAxis(3.0 / 8.0, Vector3D.PLUS_I),
                                            new InertiaAxis(1.0 / 2.0, Vector3D.PLUS_J),
                                            new InertiaAxis(5.0 / 8.0, Vector3D.PLUS_K));
        final TorqueFree torqueFree = new TorqueFree(initialAttitude, inertia);
        PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                              new Vector3D(0, 0, 3680.853673522056));
        Orbit orbit = new KeplerianOrbit(pv, frame, initialDate, 3.986004415e14);

        Assertions.assertSame(initialAttitude, torqueFree.getInitialAttitude());
        Assertions.assertSame(inertia, torqueFree.getInertia());

        // check model gives back initial attitude at initial date
        Attitude attitude0 = torqueFree.getAttitude(orbit, initialDate, frame);
        Assertions.assertEquals(0,
                                Rotation.distance(initialAttitude.getRotation(), attitude0.getRotation()),
                                1.0e-10);
        Assertions.assertEquals(0,
                                Vector3D.distance(initialAttitude.getSpin(), attitude0.getSpin()),
                                1.0e-10);

        // check model is close to linear around initial date
        double maxError = 0;
        for (double dt = -0.1; dt < 0.1; dt += 0.001) {
            double error = Rotation.distance(initialAttitude.shiftedBy(dt).getRotation(),
                                             torqueFree.getAttitude(orbit, initialDate.shiftedBy(dt), frame).getRotation());
            maxError = FastMath.max(error, maxError);
        }
        Assertions.assertEquals(5.0e-6, maxError, 1.0e-7);
         maxError = 0;
        for (double dt = -1.0; dt < 1.0; dt += 0.001) {
            double error = Rotation.distance(initialAttitude.shiftedBy(dt).getRotation(),
                                             torqueFree.getAttitude(orbit, initialDate.shiftedBy(dt), frame).getRotation());
            maxError = FastMath.max(error, maxError);
        }
        Assertions.assertEquals(5.0e-4, maxError, 1.0e-7);

    }

    /** Torque-free motion preserves angular momentum in inertial frame. */
    @Test
    public void testMomentum() {
        AbsoluteDate initialDate = new AbsoluteDate(new DateComponents(2004, 3, 2),
                                                    new TimeComponents(13, 17, 7.865),
                                                    TimeScalesFactory.getUTC());
        final Frame frame = FramesFactory.getEME2000();
        final Attitude initialAttitude = new Attitude(initialDate, frame,
                                                      new Rotation(0.9, 0.437, 0.0, 0.0, true),
                                                      new Vector3D(0.05, 0.0, 0.04),
                                                      Vector3D.ZERO);
        final Inertia inertia = new Inertia(new InertiaAxis(3.0 / 8.0, Vector3D.PLUS_I),
                                            new InertiaAxis(1.0 / 2.0, Vector3D.PLUS_J),
                                            new InertiaAxis(5.0 / 8.0, Vector3D.PLUS_K));
        PVCoordinates pv =
                        new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                                          new Vector3D(0, 0, 3680.853673522056));
        Orbit orbit = new KeplerianOrbit(pv, frame, initialDate, 3.986004415e14);
        doTestMomentum(initialAttitude, inertia, orbit, 1.4e-15);

        // TODO: generate all permutations on initial conditions and inertia
        // to test all code paths (see the various permute methods in EmbeddedRungeKuttaIntegratorAbstractTest)

    }

    private void doTestMomentum(final Attitude initialAttitude, final Inertia inertia, final Orbit orbit,
                                final double tol) {
        final Vector3D initialMomentum = initialAttitude.getRotation().applyInverseTo(inertia.momentum(initialAttitude.getSpin()));
        final TorqueFree torqueFree = new TorqueFree(initialAttitude, inertia);

        double   maxError      = 0;
        for (double dt = -40; dt < 40; dt += 0.01) {
            final Attitude attitude = torqueFree.getAttitude(orbit, orbit.getDate().shiftedBy(dt),
                                                             initialAttitude.getReferenceFrame());
            final Vector3D momentum = attitude.getRotation().applyInverseTo(inertia.momentum(attitude.getSpin()));
            maxError = FastMath.max(maxError, Vector3D.angle(momentum, initialMomentum));
        }
        Assertions.assertEquals(0.0, maxError, tol);

    }

    /** Torque-free motion preserves angular momentum in inertial frame. */
    @Test
    public void testFieldMomentum() {
        AbsoluteDate initialDate = new AbsoluteDate(new DateComponents(2004, 3, 2),
                                                    new TimeComponents(13, 17, 7.865),
                                                    TimeScalesFactory.getUTC());
        final Frame frame = FramesFactory.getEME2000();
        final Attitude initialAttitude = new Attitude(initialDate, frame,
                                                      new Rotation(0.9, 0.437, 0.0, 0.0, true),
                                                      new Vector3D(0.05, 0.0, 0.04),
                                                      Vector3D.ZERO);
        final Inertia inertia = new Inertia(new InertiaAxis(3.0 / 8.0, Vector3D.PLUS_I),
                                            new InertiaAxis(1.0 / 2.0, Vector3D.PLUS_J),
                                            new InertiaAxis(5.0 / 8.0, Vector3D.PLUS_K));
        PVCoordinates pv =
                        new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                                          new Vector3D(0, 0, 3680.853673522056));
        Orbit orbit = new KeplerianOrbit(pv, frame, initialDate, 3.986004415e14);
        doTestMomentum(Binary64Field.getInstance(), initialAttitude, inertia, orbit, 1.4e-15);

        // TODO: generate all permutations on initial conditions and inertia
        // to test all code paths (see the various permute methods in EmbeddedRungeKuttaIntegratorAbstractTest)

    }

    private <T extends CalculusFieldElement<T>> void doTestMomentum(final Field<T> field,
                                                                    final Attitude initialAttitude,
                                                                    final Inertia inertia,
                                                                    final Orbit orbit,
                                                                    final double tol) {
        final T zero = field.getZero();
        final Vector3D initialMomentum = initialAttitude.getRotation().applyInverseTo(inertia.momentum(initialAttitude.getSpin()));
        final FieldVector3D<T> fInitialMomentum = new FieldVector3D<>(field, initialMomentum);
        final TorqueFree torqueFree = new TorqueFree(initialAttitude, inertia);
        final FieldInertia<T> fInertia =
                        new FieldInertia<>(new FieldInertiaAxis<>(zero.newInstance(inertia.getInertiaAxis1().getI()),
                                                                  new FieldVector3D<>(field, inertia.getInertiaAxis1().getA())),
                                           new FieldInertiaAxis<>(zero.newInstance(inertia.getInertiaAxis2().getI()),
                                                                  new FieldVector3D<>(field, inertia.getInertiaAxis2().getA())),
                                           new FieldInertiaAxis<>(zero.newInstance(inertia.getInertiaAxis3().getI()),
                                                                  new FieldVector3D<>(field, inertia.getInertiaAxis3().getA())));

        FieldOrbit<T> fOrbit = new FieldKeplerianOrbit<>(new FieldPVCoordinates<>(field, orbit.getPVCoordinates()),
                                                         orbit.getFrame(), new FieldAbsoluteDate<>(field, orbit.getDate()),
                                                         field.getZero().newInstance(orbit.getMu()));
        T maxError = field.getZero();
        for (double dt = -40; dt < 40; dt += 0.01) {
            final FieldAttitude<T> attitude = torqueFree.getAttitude(fOrbit, fOrbit.getDate().shiftedBy(dt),
                                                                     initialAttitude.getReferenceFrame());
            final FieldVector3D<T> momentum = attitude.getRotation().applyInverseTo(fInertia.momentum(attitude.getSpin()));
            maxError = FastMath.max(maxError, FieldVector3D.angle(momentum, fInitialMomentum));
        }
        Assertions.assertEquals(0.0, maxError.getReal(), tol);

    }

    @Test
    public void testField() {
        AbsoluteDate initialDate = new AbsoluteDate(new DateComponents(2004, 3, 2),
                                                    new TimeComponents(13, 17, 7.865),
                                                    TimeScalesFactory.getUTC());
        final Frame frame = FramesFactory.getEME2000();
        final Attitude initialAttitude = new Attitude(initialDate, frame,
                                                      new Rotation(0.9, 0.437, 0.0, 0.0, true),
                                                      new Vector3D(0.05, 0.0, 0.04),
                                                      Vector3D.ZERO);
        final Inertia inertia = new Inertia(new InertiaAxis(3.0 / 8.0, Vector3D.PLUS_I),
                                            new InertiaAxis(1.0 / 2.0, Vector3D.PLUS_J),
                                            new InertiaAxis(5.0 / 8.0, Vector3D.PLUS_K));
        final TorqueFree torqueFree = new TorqueFree(initialAttitude, inertia);
        PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                              new Vector3D(0, 0, 3680.853673522056));
        Orbit orbit = new KeplerianOrbit(pv, frame, initialDate, 3.986004415e14);
        Field<Binary64> field = Binary64Field.getInstance();
        FieldOrbit<Binary64> orbit64= new FieldKeplerianOrbit<>(new FieldPVCoordinates<>(field, pv),
                                                                frame, new FieldAbsoluteDate<>(field, orbit.getDate()),
                                                                field.getZero().newInstance(orbit.getMu()));

        for (double dt = -20.0; dt < 20.0; dt += 0.1) {
            final Attitude                a   = torqueFree.getAttitude(orbit,
                                                                       orbit.getDate().shiftedBy(dt),
                                                                       frame);
            final FieldAttitude<Binary64> a64 = torqueFree.getAttitude(orbit64,
                                                                       orbit64.getDate().shiftedBy(dt),
                                                                       frame);
            Assertions.assertEquals(0.0,
                                    Rotation.distance(a.getRotation(), a64.getRotation().toRotation()),
                                    1.0e-10);
            Assertions.assertEquals(0.0,
                                    Vector3D.distance(a.getSpin(), a64.getSpin().toVector3D()),
                                    1.0e-10);
            Assertions.assertEquals(0.0,
                                    Vector3D.distance(a.getRotationAcceleration(), a64.getRotationAcceleration().toVector3D()),
                                    1.0e-10);
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

