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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.CombinatoricsUtils;
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
        Stream<TorqueFree> useCases = Stream.empty();

        // add all possible permutations of the base rotation rate
        useCases = Stream.concat(useCases,
                                 permute(initialAttitude.getSpin()).
                                 map(s -> new TorqueFree(new Attitude(initialDate, frame,
                                                                      initialAttitude.getRotation(),
                                                                      s,
                                                                      initialAttitude.getRotationAcceleration()),
                                                         inertia)));

        // add all possible permutations of the base rotation
        useCases = Stream.concat(useCases,
                                 permute(initialAttitude.getRotation()).
                                 map(r -> new TorqueFree(new Attitude(initialDate, frame,
                                                                      r,
                                                                      initialAttitude.getSpin(),
                                                                      initialAttitude.getRotationAcceleration()),
                                                         inertia)));

        // add all possible permutations of the base inertia
        useCases = Stream.concat(useCases,
                                 permute(inertia).map(i -> new TorqueFree(initialAttitude, i)));

         useCases.forEach(tf -> {
            doTestMomentum(tf, orbit, 1.6e-15);
        });

    }

    private void doTestMomentum(final TorqueFree torqueFree, final Orbit orbit, final double tol) {
        final Attitude initialAttitude = torqueFree.getInitialAttitude();
        final Inertia  inertia         = torqueFree.getInertia();
        final Vector3D initialMomentum = initialAttitude.getRotation().
                                         applyInverseTo(torqueFree.getInertia().momentum(initialAttitude.getSpin()));

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
        Stream<TorqueFree> useCases = Stream.empty();

        // add all possible permutations of the base rotation rate
        useCases = Stream.concat(useCases,
                                 permute(initialAttitude.getSpin()).
                                 map(s -> new TorqueFree(new Attitude(initialDate, frame,
                                                                      initialAttitude.getRotation(),
                                                                      s,
                                                                      initialAttitude.getRotationAcceleration()),
                                                         inertia)));

        // add all possible permutations of the base rotation
        useCases = Stream.concat(useCases,
                                 permute(initialAttitude.getRotation()).
                                 map(r -> new TorqueFree(new Attitude(initialDate, frame,
                                                                      r,
                                                                      initialAttitude.getSpin(),
                                                                      initialAttitude.getRotationAcceleration()),
                                                         inertia)));

        // add all possible permutations of the base inertia
        useCases = Stream.concat(useCases,
                                 permute(inertia).map(i -> new TorqueFree(initialAttitude, i)));

        useCases.forEach(tf -> {
            doTestMomentum(Binary64Field.getInstance(), tf, orbit, 1.6e-15);
        });

    }

    private <T extends CalculusFieldElement<T>> void doTestMomentum(final Field<T> field,
                                                                    final TorqueFree torqueFree,
                                                                    final Orbit orbit,
                                                                    final double tol) {
        final T zero = field.getZero();
        final Attitude initialAttitude = torqueFree.getInitialAttitude();
        final Inertia  inertia         = torqueFree.getInertia();
        final Vector3D initialMomentum = initialAttitude.getRotation().applyInverseTo(inertia.momentum(initialAttitude.getSpin()));
        final FieldVector3D<T> fInitialMomentum = new FieldVector3D<>(field, initialMomentum);
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

    /** Generate all permutations of vector coordinates.
     * @param v vector to permute
     * @return permuted vector
     */
    private Stream<Vector3D> permute(final Vector3D v) {
        return CombinatoricsUtils.
                        permutations(Arrays.asList(v.getX(), v.getY(), v.getZ())).
                        map(a -> new Vector3D(a.get(0), a.get(1), a.get(2)));
    }

    /** Generate all permutations of rotation coordinates.
     * @param r rotation to permute
     * @return permuted rotation
     */
    private Stream<Rotation> permute(final Rotation r) {
        return CombinatoricsUtils.
                        permutations(Arrays.asList(r.getQ0(), r.getQ1(), r.getQ2(), r.getQ3())).
                        map(a -> new Rotation(a.get(0), a.get(1), a.get(2), a.get(3), false));
    }

    /** Generate all permutations of inertia.
     * @param inertia inertia to permute
     * @return permuted inertia
     */
    private Stream<Inertia> permute(final Inertia inertia) {
        List<Inertia> permuted = new ArrayList<>();
        // the external "loop" permutes the inertia axes as a whole
        // the internal "loop" permutes the moment of inertia within a fixed triad
        CombinatoricsUtils.
            permutations(Arrays.asList(inertia.getInertiaAxis1(), inertia.getInertiaAxis2(), inertia.getInertiaAxis3())).
            forEach(ia ->
                    permuted.addAll(CombinatoricsUtils.permutations(Arrays.asList(0, 1, 2)).
                                    map(i -> new Inertia(new InertiaAxis(ia.get(i.get(0)).getI(), ia.get(0).getA()),
                                                         new InertiaAxis(ia.get(i.get(1)).getI(), ia.get(1).getA()),
                                                         new InertiaAxis(ia.get(i.get(2)).getI(), ia.get(2).getA()))).
                                    collect(Collectors.toList())));
        return permuted.stream();
    }

}

