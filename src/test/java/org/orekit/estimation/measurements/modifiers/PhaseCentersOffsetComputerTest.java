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
package org.orekit.estimation.measurements.modifiers;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.frames.Transform;
import org.orekit.gnss.antenna.FrequencyPattern;
import org.orekit.gnss.antenna.OneDVariation;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.PVCoordinates;

public class PhaseCentersOffsetComputerTest {

    @Test
    public void testAllZero() {
        PhaseCentersOffsetComputer computer =
                        new PhaseCentersOffsetComputer(FrequencyPattern.ZERO_CORRECTION,
                                                       FrequencyPattern.ZERO_CORRECTION);
        final RandomGenerator random = new Well1024a(0xb84f12fc3ba761d6l);
        for (int i = 0; i < 1000; ++i) {
            final Transform eTransform = new Transform(emitterToInert.getDate(), emitterToInert,
                                                       randomTransform(random, emitterToInert.getDate()));
            final Transform rTransform = new Transform(reveiverToInert.getDate(), reveiverToInert,
                                                       randomTransform(random, reveiverToInert.getDate()));
            Assertions.assertEquals(0.0, computer.offset(eTransform, rTransform), 1.0e-15);
        }
    }

    @Test
    public void testPCVWithoutEffect() {
        PhaseCentersOffsetComputer computer =
                        new PhaseCentersOffsetComputer(new FrequencyPattern(Vector3D.ZERO,
                                                                            new OneDVariation(0.0, FastMath.PI,
                                                                                              new double[] { 0.0, 0.0 })),
                                                       new FrequencyPattern(Vector3D.ZERO,
                                                                            new OneDVariation(0.0, FastMath.PI,
                                                                                              new double[] { 0.0, 0.0 })));
        final RandomGenerator random = new Well1024a(0x787f77a831792a43l);
        for (int i = 0; i < 1000; ++i) {
            final Transform eTransform = new Transform(emitterToInert.getDate(), emitterToInert,
                                                       randomTransform(random, emitterToInert.getDate()));
            final Transform rTransform = new Transform(reveiverToInert.getDate(), reveiverToInert,
                                                       randomTransform(random, reveiverToInert.getDate()));
            Assertions.assertEquals(0.0, computer.offset(eTransform, rTransform), 1.0e-15);
        }
    }

    @Test
    public void testOnlyMeanOffset() {
        PhaseCentersOffsetComputer computer =
                        new PhaseCentersOffsetComputer(new FrequencyPattern(new Vector3D(0, -1, 1), null),
                                                       new FrequencyPattern(new Vector3D(-1, 0, 0), null));
        Assertions.assertEquals(4.0 - FastMath.sqrt(5.0),
                                computer.offset(emitterToInert, reveiverToInert),
                                1.0e-15);
    }

    @Test
    public void testComplete() {
        for (double pcvE = -1.0; pcvE < 1.0; pcvE += 0.015625) {
            for (double pcvR = -1.0; pcvR < 1.0; pcvR += 0.015625) {
                PhaseCentersOffsetComputer computer =
                                new PhaseCentersOffsetComputer(new FrequencyPattern(new Vector3D(0, -1, 1),
                                                                                    new OneDVariation(0.0, 0.5 * FastMath.PI,
                                                                                                      new double[] { 0.0, pcvE, 10.0 })),
                                                               new FrequencyPattern(new Vector3D(-1, 0, 0),
                                                                                    new OneDVariation(0.0, 0.5 * FastMath.PI,
                                                                                                      new double[] { 0.0, pcvR, 12.0 })));
                Assertions.assertEquals(4.0 - FastMath.sqrt(5.0) + pcvE + pcvR,
                                        computer.offset(emitterToInert, reveiverToInert),
                                        1.0e-15);
            }
        }
    }

    @BeforeEach
    public void setUp() {

        emitterToInert  = new Transform(AbsoluteDate.ARBITRARY_EPOCH,
                                        new PVCoordinates(new Vector3D(0, -2, -2)),
                                        new AngularCoordinates(new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                                            Vector3D.PLUS_K, Vector3D.MINUS_I)));

        Assertions.assertEquals(0.0, Vector3D.distance(new Vector3D(2, 2, 0), emitterToInert.transformPosition(Vector3D.ZERO)),   1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(new Vector3D(2, 2, 1), emitterToInert.transformPosition(Vector3D.PLUS_I)), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(new Vector3D(1, 2, 0), emitterToInert.transformPosition(Vector3D.PLUS_J)), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(new Vector3D(2, 1, 0), emitterToInert.transformPosition(Vector3D.PLUS_K)), 1.0e-15);

        reveiverToInert = new Transform(AbsoluteDate.ARBITRARY_EPOCH,
                                       new PVCoordinates(new Vector3D(0, 1, 0)),
                                       new AngularCoordinates(Rotation.IDENTITY));
        Assertions.assertEquals(0.0, Vector3D.distance(new Vector3D(0, 1, 0), reveiverToInert.transformPosition(Vector3D.ZERO)),   1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(new Vector3D(1, 1, 0), reveiverToInert.transformPosition(Vector3D.PLUS_I)), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(new Vector3D(0, 2, 0), reveiverToInert.transformPosition(Vector3D.PLUS_J)), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(new Vector3D(0, 1, 1), reveiverToInert.transformPosition(Vector3D.PLUS_K)), 1.0e-15);

    }

    private Transform randomTransform(final RandomGenerator random, final AbsoluteDate date) {
        return new Transform(date,
                             new PVCoordinates(randomVector(random)),
                             new AngularCoordinates(randomRotation(random)));
    }

    private Vector3D randomVector(final RandomGenerator random) {
        return new Vector3D(random.nextDouble(), random.nextDouble(), random.nextDouble());
    }

    private Rotation randomRotation(final RandomGenerator random) {
        return new Rotation(random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(),
                            true);
    }

    Transform emitterToInert;
    Transform reveiverToInert;

}


