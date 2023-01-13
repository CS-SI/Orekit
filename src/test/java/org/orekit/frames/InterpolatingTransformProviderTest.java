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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public class InterpolatingTransformProviderTest {

    @Test
    public void testCacheHitWithDerivatives() {

        AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        CirclingProvider referenceProvider = new CirclingProvider(t0, 0.2);
        CirclingProvider rawProvider = new CirclingProvider(t0, 0.2);
        InterpolatingTransformProvider interpolatingProvider =
                new InterpolatingTransformProvider(rawProvider,
                                                   CartesianDerivativesFilter.USE_PVA,
                                                   AngularDerivativesFilter.USE_RR,
                                                   5, 0.8, 10, 60.0, 60.0);

        for (double dt = 0.1; dt <= 3.1; dt += 0.001) {
            Transform reference = referenceProvider.getTransform(t0.shiftedBy(dt));
            Transform interpolated = interpolatingProvider.getTransform(t0.shiftedBy(dt));
            Transform error = new Transform(reference.getDate(), reference, interpolated.getInverse());
            Assertions.assertEquals(0.0, error.getCartesian().getPosition().getNorm(),           7.0e-15);
            Assertions.assertEquals(0.0, error.getCartesian().getVelocity().getNorm(),           3.0e-14);
            Assertions.assertEquals(0.0, error.getAngular().getRotation().getAngle(),            1.3e-15);
            Assertions.assertEquals(0.0, error.getAngular().getRotationRate().getNorm(),         2.2e-15);
            Assertions.assertEquals(0.0, error.getAngular().getRotationAcceleration().getNorm(), 1.2e-14);

        }
        Assertions.assertEquals(10,   rawProvider.getCount());
        Assertions.assertEquals(3001, referenceProvider.getCount());

    }

    @Test
    public void testCacheHitWithoutDerivatives() {

        AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        CirclingProvider referenceProvider = new CirclingProvider(t0, 0.2);
        CirclingProvider rawProvider = new CirclingProvider(t0, 0.2);
        InterpolatingTransformProvider interpolatingProvider =
                new InterpolatingTransformProvider(rawProvider,
                                                   CartesianDerivativesFilter.USE_P,
                                                   AngularDerivativesFilter.USE_R,
                                                   5, 0.8, 10, 60.0, 60.0);

        for (double dt = 0.1; dt <= 3.1; dt += 0.001) {
            Transform reference = referenceProvider.getTransform(t0.shiftedBy(dt));
            Transform interpolated = interpolatingProvider.getTransform(t0.shiftedBy(dt));
            Transform error = new Transform(reference.getDate(), reference, interpolated.getInverse());
            Assertions.assertEquals(0.0, error.getCartesian().getPosition().getNorm(),   1.3e-6);
            Assertions.assertEquals(0.0, error.getCartesian().getVelocity().getNorm(),   7.0e-6);
            Assertions.assertEquals(0.0, error.getAngular().getRotation().getAngle(),    2.0e-15);
            Assertions.assertEquals(0.0, error.getAngular().getRotationRate().getNorm(), 2.0e-15);

        }
        Assertions.assertEquals(10,   rawProvider.getCount());
        Assertions.assertEquals(3001, referenceProvider.getCount());

    }

    @Test
    public void testForwardException() {
        Assertions.assertThrows(OrekitException.class, () -> {
            InterpolatingTransformProvider interpolatingProvider =
                    new InterpolatingTransformProvider(new TransformProvider() {
                        private static final long serialVersionUID = -3126512810306982868L;
                        public Transform getTransform(AbsoluteDate date) {
                            throw new OrekitException(OrekitMessages.INTERNAL_ERROR);
                        }
                        public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
                            throw new UnsupportedOperationException("never called in this test");
                        }
                    },
                            CartesianDerivativesFilter.USE_PVA,
                            AngularDerivativesFilter.USE_RRA,
                            5, 0.8, 10, 60.0, 60.0);
            interpolatingProvider.getTransform(AbsoluteDate.J2000_EPOCH);
        });
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {

        AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        CirclingProvider rawProvider = new CirclingProvider(t0, 0.2);
        InterpolatingTransformProvider interpolatingProvider =
                new InterpolatingTransformProvider(rawProvider,
                                                   CartesianDerivativesFilter.USE_PVA,
                                                   AngularDerivativesFilter.USE_RRA,
                                                   5, 0.8, 10, 60.0, 60.0);

        for (double dt = 0.1; dt <= 3.1; dt += 0.001) {
            interpolatingProvider.getTransform(t0.shiftedBy(dt));
        }
        Assertions.assertEquals(10, rawProvider.getCount());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(interpolatingProvider);

        Assertions.assertTrue(bos.size () >  450);
        Assertions.assertTrue(bos.size () <  550);

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        InterpolatingTransformProvider deserialized =
                (InterpolatingTransformProvider) ois.readObject();
        Assertions.assertEquals(0, ((CirclingProvider) deserialized.getRawProvider()).getCount());
        for (double dt = 0.1; dt <= 3.1; dt += 0.001) {
            Transform t1 = interpolatingProvider.getTransform(t0.shiftedBy(dt));
            Transform t2 = deserialized.getTransform(t0.shiftedBy(dt));
            Transform error = new Transform(t1.getDate(), t1, t2.getInverse());
            // both interpolators should give the same results
            Assertions.assertEquals(0.0, error.getCartesian().getPosition().getNorm(),   1.0e-15);
            Assertions.assertEquals(0.0, error.getCartesian().getVelocity().getNorm(),   1.0e-15);
            Assertions.assertEquals(0.0, error.getAngular().getRotation().getAngle(),    1.0e-15);
            Assertions.assertEquals(0.0, error.getAngular().getRotationRate().getNorm(), 1.0e-15);
        }

        // the original interpolator should not have triggered any new calls
        Assertions.assertEquals(10, rawProvider.getCount());

        // the deserialized interpolator should have triggered new calls
        Assertions.assertEquals(10, ((CirclingProvider) deserialized.getRawProvider()).getCount());

    }

    private static class CirclingProvider implements TransformProvider {

        private static final long serialVersionUID = 473784183299281612L;
        private int count;
        private final AbsoluteDate t0;
        private final double omega;

        public CirclingProvider(final AbsoluteDate t0, final double omega) {
            this.count = 0;
            this.t0    = t0;
            this.omega = omega;
        }

        public Transform getTransform(final AbsoluteDate date) {
            // the following transform corresponds to a frame moving along the circle r = 1
            // with its x axis always pointing to the reference frame center
            ++count;
            final double dt = date.durationFrom(t0);
            final double cos = FastMath.cos(omega * dt);
            final double sin = FastMath.sin(omega * dt);
            return new Transform(date,
                                 new Transform(date,
                                               new Vector3D(-cos, -sin, 0),
                                               new Vector3D(omega * sin, -omega * cos, 0),
                                               new Vector3D(omega * omega * cos, omega * omega * sin, 0)),
                                 new Transform(date,
                                               new Rotation(Vector3D.PLUS_K,
                                                            FastMath.PI - omega * dt,
                                                            RotationConvention.VECTOR_OPERATOR),
                                               new Vector3D(omega, Vector3D.PLUS_K)));
        }

        public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
            throw new UnsupportedOperationException("never called in this test");
        }

        public int getCount() {
            return count;
        }

        private Object readResolve() {
            count = 0;
            return this;
        }
    }

}
