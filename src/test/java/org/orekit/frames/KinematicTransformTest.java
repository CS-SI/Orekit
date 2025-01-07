/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

class KinematicTransformTest {

    @Test
    void testCompositeVelocity() {
        // GIVEN
        final KinematicTransform kinematicTransform = createArbitraryKineticTransform();
        // WHEN
        final Vector3D actualCompositeVelocity = KinematicTransform.compositeVelocity(kinematicTransform,
                KinematicTransform.getIdentity());
        // THEN
        final Vector3D expectedCompositeVelocity = kinematicTransform.getVelocity();
        Assertions.assertEquals(expectedCompositeVelocity, actualCompositeVelocity);
    }

    @Test
    void testCompositeRotationRate() {
        // GIVEN
        final KinematicTransform kinematicTransform = createArbitraryKineticTransform();
        // WHEN
        final Vector3D actualCompositeRotationRate = KinematicTransform.compositeRotationRate(kinematicTransform,
                KinematicTransform.getIdentity());
        // THEN
        final Vector3D expectedCompositeRotationRate = kinematicTransform.getRotationRate();
        Assertions.assertEquals(expectedCompositeRotationRate, actualCompositeRotationRate);
    }

    @Test
    void testTransformPVOnly() {
        // GIVEN
        final KinematicTransform kinematicTransform = createArbitraryKineticTransform();
        final PVCoordinates pvCoordinates = new PVCoordinates(new Vector3D(1, 2, 3), new Vector3D(4, 5, 6));
        // WHEN
        final PVCoordinates convertedPV = kinematicTransform.transformOnlyPV(pvCoordinates);
        final PVCoordinates reconvertedPV = kinematicTransform.getInverse().transformOnlyPV(convertedPV);
        // THEN
        comparePVCoordinates(pvCoordinates, reconvertedPV);
    }

    @Test
    void testTransformTimeStampedPVCoordinatesWithoutA() {
        // GIVEN
        final KinematicTransform kinematicTransform = createArbitraryKineticTransform();
        final TimeStampedPVCoordinates pvCoordinates = new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH,
                new Vector3D(1, 2, 3), new Vector3D(4, 5, 6));
        // WHEN
        final PVCoordinates convertedPV = kinematicTransform.transformOnlyPV(pvCoordinates);
        final PVCoordinates reconvertedPV = kinematicTransform.getInverse().transformOnlyPV(convertedPV);
        // THEN
        comparePVCoordinates(pvCoordinates, reconvertedPV);
    }

    private void comparePVCoordinates(final PVCoordinates pvCoordinates, final PVCoordinates reconvertedPV) {
        final double tolerance = 1e-10;
        final double[] expectedPosition = pvCoordinates.getPosition().toArray();
        final double[] actualPosition = reconvertedPV.getPosition().toArray();
        final double[] expectedVelocity = pvCoordinates.getVelocity().toArray();
        final double[] actualVelocity = reconvertedPV.getVelocity().toArray();
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(expectedPosition[i], actualPosition[i], tolerance);
            Assertions.assertEquals(expectedVelocity[i], actualVelocity[i], tolerance);
        }
    }

    @Test
    void testOfWithoutRotation() {
        // GIVEN
        final AbsoluteDate expectedDate = AbsoluteDate.ARBITRARY_EPOCH;
        final PVCoordinates pvCoordinates = new PVCoordinates(Vector3D.MINUS_I, Vector3D.PLUS_J);
        // WHEN
        final KinematicTransform kinematicTransform = KinematicTransform.of(expectedDate, pvCoordinates);
        // THEN
        Assertions.assertEquals(expectedDate, kinematicTransform.getDate());
        Assertions.assertEquals(pvCoordinates.getPosition(), kinematicTransform.getTranslation());
        Assertions.assertEquals(pvCoordinates.getVelocity(), kinematicTransform.getVelocity());
        Assertions.assertEquals(0., Rotation.distance(Rotation.IDENTITY, kinematicTransform.getRotation()));
        Assertions.assertEquals(Vector3D.ZERO, kinematicTransform.getRotationRate());
    }

    @Test
    void testOfTranslation() {
        final AbsoluteDate expectedDate = AbsoluteDate.ARBITRARY_EPOCH;
        final Rotation expectedRotation = new Rotation(Vector3D.MINUS_K, Vector3D.PLUS_I);
        final Vector3D expectedRotationRate = Vector3D.MINUS_J;
        // WHEN
        final KinematicTransform kinematicTransform = KinematicTransform.of(expectedDate, expectedRotation,
                expectedRotationRate);
        // THEN
        Assertions.assertEquals(expectedDate, kinematicTransform.getDate());
        Assertions.assertEquals(Vector3D.ZERO, kinematicTransform.getTranslation());
        Assertions.assertEquals(Vector3D.ZERO, kinematicTransform.getVelocity());
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, kinematicTransform.getRotation()));
        Assertions.assertEquals(expectedRotationRate, kinematicTransform.getRotationRate());
    }

    @Test
    void testOfInverse() {
        final KinematicTransform kinematicTransform = createArbitraryKineticTransform();
        // WHEN
        final KinematicTransform inverseKinematicTransform = kinematicTransform.getInverse();
        final KinematicTransform composedTransform = KinematicTransform.compose(kinematicTransform.getDate(),
                kinematicTransform, inverseKinematicTransform);
        // THEN
        Assertions.assertEquals(kinematicTransform.getDate(), composedTransform.getDate());
        Assertions.assertEquals(Vector3D.ZERO, composedTransform.getTranslation());
        Assertions.assertEquals(Vector3D.ZERO, composedTransform.getVelocity());
        Assertions.assertEquals(0., Rotation.distance(Rotation.IDENTITY, composedTransform.getRotation()));
        Assertions.assertEquals(Vector3D.ZERO, composedTransform.getRotationRate());
    }

    private KinematicTransform createArbitraryKineticTransform() {
        return KinematicTransform.of(AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates(Vector3D.MINUS_I, Vector3D.PLUS_J),
                new Rotation(Vector3D.MINUS_K, Vector3D.PLUS_I), Vector3D.MINUS_J);
    }

}
