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
package org.orekit.propagation.events;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Unit tests for {@link FieldNegateDetector}.
 *
 * @author Evan Ward
 */
public class FieldNegateDetectorTest {

    /**
     * check {@link FieldNegateDetector#init(FieldSpacecraftState, FieldAbsoluteDate)}.
     */
    @Test
    public void testInit() {
        doTestInit(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestInit(final Field<T> field) {
        //setup
        @SuppressWarnings("unchecked")
        FieldEventDetector<T> a = Mockito.mock(FieldEventDetector.class);
        Mockito.when(a.getMaxCheckInterval()).thenReturn(s -> AbstractDetector.DEFAULT_MAXCHECK);
        Mockito.when(a.getThreshold()).thenReturn(field.getZero().newInstance(AbstractDetector.DEFAULT_THRESHOLD));
        @SuppressWarnings("unchecked")
        FieldEventHandler<T> c = Mockito.mock(FieldEventHandler.class);
        FieldNegateDetector<T> detector = new FieldNegateDetector<>(a).withHandler(c);
        FieldAbsoluteDate<T> t = FieldAbsoluteDate.getGPSEpoch(field);
        @SuppressWarnings("unchecked")
        FieldSpacecraftState<T> s = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(s.getDate()).thenReturn(t.shiftedBy(60.0));

        //action
        detector.init(s, t);

        //verify
        Mockito.verify(a).init(s, t);
        Mockito.verify(c).init(s, t, detector);
    }

    /**
     * check g function is negated.
     */
    @Test
    public void testG() {
        doTestG(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestG(final Field<T> field) {
        //setup
        @SuppressWarnings("unchecked")
        FieldEventDetector<T> a = Mockito.mock(FieldEventDetector.class);
        Mockito.when(a.getMaxCheckInterval()).thenReturn(s -> AbstractDetector.DEFAULT_MAXCHECK);
        Mockito.when(a.getThreshold()).thenReturn(field.getZero().newInstance(AbstractDetector.DEFAULT_THRESHOLD));
        FieldNegateDetector<T> detector = new FieldNegateDetector<>(a);
        @SuppressWarnings("unchecked")
        FieldSpacecraftState<T> s = Mockito.mock(FieldSpacecraftState.class);

        // verify + to -
        Mockito.when(a.g(s)).thenReturn(field.getZero().newInstance(1.0));
        MatcherAssert.assertThat(detector.g(s).getReal(), CoreMatchers.is(-1.0));
        // verify - to +
        Mockito.when(a.g(s)).thenReturn(field.getZero().newInstance(-1.0));
        MatcherAssert.assertThat(detector.g(s).getReal(), CoreMatchers.is(1.0));
    }

    /** Check a with___ method. */
    @Test
    public void testCreate() {
        doTestCreate(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestCreate(final Field<T> field) {
        //setup
        @SuppressWarnings("unchecked")
        FieldEventDetector<T> a = Mockito.mock(FieldEventDetector.class);
        Mockito.when(a.getMaxCheckInterval()).thenReturn(s -> AbstractDetector.DEFAULT_MAXCHECK);
        Mockito.when(a.getThreshold()).thenReturn(field.getZero().newInstance(AbstractDetector.DEFAULT_THRESHOLD));
        FieldNegateDetector<T> detector = new FieldNegateDetector<>(a);

        // action
        FieldNegateDetector<T> actual = detector.withMaxCheck(100);

        //verify
        MatcherAssert.assertThat(actual.getMaxCheckInterval().currentInterval(null), CoreMatchers.is(100.0));
        Assertions.assertTrue(actual.getOriginal() == a);
    }
}
