/* Contributed in the public domain.
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

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * Unit tests for {@link AngularTransformProvider}.
 *
 * @author Evan Ward
 */
public class AngularTransformProviderTest {

    /** Check overridden methods for consistency. */
    @Test
    public void testConsistency() {
        // setup
        DataContext context = Utils.newDataContext("regular-data");
        final Frame parent = context.getFrames().getGCRF();
        // frame w/ some rotation dynamics w.r.t. parent
        final FactoryManagedFrame orientation =
                context.getFrames().getITRF(IERSConventions.IERS_2010, true);
        AbsoluteDate epoch = context.getTimeScales().getCcsdsEpoch();
        final Binary64Field field = Binary64Field.getInstance();
        FieldAbsoluteDate<?> fieldEpoch = new FieldAbsoluteDate<>(field, epoch);

        // action
        final AngularTransformProvider actual =
                new AngularTransformProvider(parent, orientation);

        // verify
        final Transform expected = parent.getTransformTo(orientation, epoch);
        final Matcher<Rotation> isExpectedRotation = OrekitMatchers
                .distanceIs(expected.getRotation(), Matchers.closeTo(0.0, 0.0));
        final Matcher<Vector3D> isExpectedRotationRate =
                OrekitMatchers.vectorCloseTo(expected.getRotationRate(), 0);
        final Matcher<Vector3D> isExpectedRotationAcceleration =
                OrekitMatchers.vectorCloseTo(expected.getRotationAcceleration(), 0);
        final Matcher<Vector3D> isExpectedPosition =
                OrekitMatchers.vectorCloseTo(Vector3D.ZERO, 0);
        final Matcher<Vector3D> isExpectedVelocity =
                OrekitMatchers.vectorCloseTo(Vector3D.ZERO, 0);
        final Matcher<Vector3D> isExpectedAcceleration =
                OrekitMatchers.vectorCloseTo(Vector3D.ZERO, 0);

        StaticTransform staticTransform = actual.getStaticTransform(epoch);
        MatcherAssert.assertThat(staticTransform.getDate(), Matchers.is(epoch));
        MatcherAssert.assertThat(staticTransform.getTranslation(),
                isExpectedPosition);
        MatcherAssert.assertThat(staticTransform.getRotation(),
                isExpectedRotation);

        FieldStaticTransform<?> fieldStaticTransform = actual.getStaticTransform(fieldEpoch);
        MatcherAssert.assertThat(fieldStaticTransform.getFieldDate(),
                Matchers.is(fieldEpoch));
        MatcherAssert.assertThat(fieldStaticTransform.getTranslation().toVector3D(),
                isExpectedPosition);
        MatcherAssert.assertThat(fieldStaticTransform.getRotation().toRotation(),
                isExpectedRotation);

        KinematicTransform kinematicTransform = actual.getKinematicTransform(epoch);
        MatcherAssert.assertThat(kinematicTransform.getDate(), Matchers.is(epoch));
        MatcherAssert.assertThat(kinematicTransform.getTranslation(),
                isExpectedPosition);
        MatcherAssert.assertThat(kinematicTransform.getRotation(),
                isExpectedRotation);
        MatcherAssert.assertThat(kinematicTransform.getVelocity(),
                isExpectedVelocity);
        MatcherAssert.assertThat(kinematicTransform.getRotationRate(),
                isExpectedRotationRate);

        FieldKinematicTransform<?> fieldKinematicTransform = actual.getKinematicTransform(fieldEpoch);
        MatcherAssert.assertThat(fieldKinematicTransform.getFieldDate(),
                Matchers.is(fieldEpoch));
        MatcherAssert.assertThat(fieldKinematicTransform.getTranslation().toVector3D(),
                isExpectedPosition);
        MatcherAssert.assertThat(fieldKinematicTransform.getRotation().toRotation(),
                isExpectedRotation);
        MatcherAssert.assertThat(fieldKinematicTransform.getVelocity().toVector3D(),
                isExpectedVelocity);
        MatcherAssert.assertThat(fieldKinematicTransform.getRotationRate().toVector3D(),
                isExpectedRotationRate);

        Transform transform = actual.getTransform(epoch);
        MatcherAssert.assertThat(transform.getDate(), Matchers.is(epoch));
        MatcherAssert.assertThat(transform.getTranslation(),
                isExpectedPosition);
        MatcherAssert.assertThat(transform.getRotation(),
                isExpectedRotation);
        MatcherAssert.assertThat(transform.getVelocity(),
                isExpectedVelocity);
        MatcherAssert.assertThat(transform.getRotationRate(),
                isExpectedRotationRate);
        MatcherAssert.assertThat(transform.getAcceleration(),
                isExpectedAcceleration);
        MatcherAssert.assertThat(transform.getRotationAcceleration(),
                isExpectedRotationAcceleration);

        FieldTransform<?> fieldTransform = actual.getTransform(fieldEpoch);
        MatcherAssert.assertThat(fieldTransform.getFieldDate(),
                Matchers.is(fieldEpoch));
        MatcherAssert.assertThat(fieldTransform.getTranslation().toVector3D(),
                isExpectedPosition);
        MatcherAssert.assertThat(fieldTransform.getRotation().toRotation(),
                isExpectedRotation);
        MatcherAssert.assertThat(fieldTransform.getVelocity().toVector3D(),
                isExpectedVelocity);
        MatcherAssert.assertThat(fieldTransform.getRotationRate().toVector3D(),
                isExpectedRotationRate);
        MatcherAssert.assertThat(fieldTransform.getAcceleration().toVector3D(),
                isExpectedAcceleration);
        MatcherAssert.assertThat(fieldTransform.getRotationAcceleration().toVector3D(),
                isExpectedRotationAcceleration);

    }

}
