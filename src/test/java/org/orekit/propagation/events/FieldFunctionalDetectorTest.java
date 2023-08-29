/* Contributed in the public domain.
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.propagation.events;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;

import java.util.function.Function;

/**
 * Unit tests for {@link FieldFunctionalDetector}
 *
 * @author Evan Ward
 */
public class FieldFunctionalDetectorTest {

    /**
     * Check {@link FieldFunctionalDetector}.
     */
    @Test
    public void testFunctionalDetector() {
        doTestFunctionalDetector(Binary64Field.getInstance());
    }

    public <T extends CalculusFieldElement<T>> void doTestFunctionalDetector(Field<T> field) {
        // setup
        T zero = field.getZero();
        T one = field.getOne();
        Function<FieldSpacecraftState<T>, T> g = FieldSpacecraftState::getMass;
        FieldEventHandler<T> handler = (s, detector, increasing) -> Action.STOP;

        // action
        FieldFunctionalDetector<T> detector = new FieldFunctionalDetector<>(field)
                .withMaxIter(1)
                .withThreshold(zero.add(2))
                .withMaxCheck(3)
                .withHandler(handler)
                .withFunction(g);

        // verify
        MatcherAssert.assertThat(detector.getMaxIterationCount(), CoreMatchers.is(1));
        MatcherAssert.assertThat(detector.getThreshold().getReal(), CoreMatchers.is(2.0));
        MatcherAssert.assertThat(detector.getMaxCheckInterval().currentInterval(null), CoreMatchers.is(3.0));
        MatcherAssert.assertThat(detector.getHandler(), CoreMatchers.is(handler));
        FieldSpacecraftState<T> state = new FieldSpacecraftState<>(
                new FieldCartesianOrbit<>(
                        new FieldPVCoordinates<>(
                                new FieldVector3D<>(one, new Vector3D(1, 2, 3)),
                                new FieldVector3D<>(one, new Vector3D(4, 5, 6))),
                        FramesFactory.getGCRF(),
                        new FieldAbsoluteDate<>(field, AbsoluteDate.CCSDS_EPOCH),
                        zero.add(4)),
                zero.add(5));
        MatcherAssert.assertThat(detector.g(state).getReal(), CoreMatchers.is(5.0));
        MatcherAssert.assertThat(detector.getHandler().eventOccurred(null, detector, false),
                CoreMatchers.is(Action.STOP));
        MatcherAssert.assertThat(detector.getFunction(), CoreMatchers.is(g));
    }

}
