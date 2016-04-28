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
package org.orekit.frames;

import java.util.Random;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.FrameAncestorException;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;

public class UpdatableFrameTest {

    @Test
    public void testUpdateTransform() throws OrekitException {
        Random random     = new Random(0x2f6769c23e53e96el);
        UpdatableFrame f0 = new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "dummy");
        AbsoluteDate date = new AbsoluteDate();

        UpdatableFrame f1 = new UpdatableFrame(f0, randomTransform(random), "f1");
        UpdatableFrame f2 = new UpdatableFrame(f1, randomTransform(random), "f2");
        UpdatableFrame f3 = new UpdatableFrame(f2, randomTransform(random), "f3");
        UpdatableFrame f4 = new UpdatableFrame(f2, randomTransform(random), "f4");
        UpdatableFrame f5 = new UpdatableFrame(f4, randomTransform(random), "f5");
        UpdatableFrame f6 = new UpdatableFrame(f0, randomTransform(random), "f6");
        UpdatableFrame f7 = new UpdatableFrame(f6, randomTransform(random), "f7");
        UpdatableFrame f8 = new UpdatableFrame(f6, randomTransform(random), "f8");
        UpdatableFrame f9 = new UpdatableFrame(f7, randomTransform(random), "f9");

        checkFrameAncestorException(f6, f8, f9, randomTransform(random), date);
        checkFrameAncestorException(f6, f9, f8, randomTransform(random), date);
        checkFrameAncestorException(f6, f3, f5, randomTransform(random), date);
        checkFrameAncestorException(f6, f5, f3, randomTransform(random), date);
        checkFrameAncestorException(f0, f5, f9, randomTransform(random), date);
        checkFrameAncestorException(f0, f9, f5, randomTransform(random), date);
        checkFrameAncestorException(f3, f0, f6, randomTransform(random), date);
        checkFrameAncestorException(f3, f6, f0, randomTransform(random), date);

        checkUpdateTransform(f1, f5, f9, date, random);
        checkUpdateTransform(f7, f6, f9, date, random);
        checkUpdateTransform(f6, f0, f7, date, random);

        checkUpdateTransform(f6, f6.getParent(), f6, date, random);

    }

    private void checkFrameAncestorException(UpdatableFrame f0, Frame f1, Frame f2,
                                             Transform transform, AbsoluteDate date) {
        try {
            f0.updateTransform(f1, f2, transform, date);
            Assert.fail("Should raise a FrameAncestorException");
        } catch(FrameAncestorException expected){
            // expected behavior
        } catch (Exception e) {
            Assert.fail("wrong exception caught");
        }
    }

    private void checkUpdateTransform(UpdatableFrame f0, Frame f1, Frame f2,
                                      AbsoluteDate date, Random random)
      throws OrekitException {
        Transform f1ToF2 = randomTransform(random);

        f0.updateTransform(f1, f2, f1ToF2, date);
        Transform obtained12 = f1.getTransformTo(f2, date);
        checkNoTransform(new Transform(date, f1ToF2, obtained12.getInverse()), random);

        f0.updateTransform(f2, f1, f1ToF2.getInverse(), date);
        Transform obtained21 = f2.getTransformTo(f1, date);
        checkNoTransform(new Transform(date, f1ToF2.getInverse(), obtained21.getInverse()), random);

        checkNoTransform(new Transform(date, obtained12, obtained21), random);

    }

    private Transform randomTransform(Random random) {
        Transform transform = Transform.IDENTITY;
        for (int i = random.nextInt(10); i > 0; --i) {
            if (random.nextBoolean()) {
                Vector3D u = new Vector3D(random.nextDouble() * 1000.0,
                                          random.nextDouble() * 1000.0,
                                          random.nextDouble() * 1000.0);
                transform = new Transform(transform.getDate(), transform, new Transform(transform.getDate(), u));
            } else {
                double q0 = random.nextDouble() * 2 - 1;
                double q1 = random.nextDouble() * 2 - 1;
                double q2 = random.nextDouble() * 2 - 1;
                double q3 = random.nextDouble() * 2 - 1;
                double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
                Rotation r = new Rotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
                transform = new Transform(transform.getDate(), transform, new Transform(transform.getDate(), r));
            }
        }
        return transform;
    }

    private void checkNoTransform(Transform transform, Random random) {
        for (int i = 0; i < 100; ++i) {
            Vector3D a = new Vector3D(random.nextDouble(),
                                      random.nextDouble(),
                                      random.nextDouble());
            Vector3D b = transform.transformVector(a);
            Assert.assertEquals(0, a.subtract(b).getNorm(), 1.0e-10);
            Vector3D c = transform.transformPosition(a);
            Assert.assertEquals(0, a.subtract(c).getNorm(), 1.0e-10);
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
