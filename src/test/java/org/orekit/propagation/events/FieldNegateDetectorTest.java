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
package org.orekit.propagation.events;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Unit tests for {@link FieldNegateDetector}.
 *
 * @author Evan Ward
 */
class FieldNegateDetectorTest {

    @Test
    void testGetDetector() {
        // GIVEN
        final FieldDateDetector<Binary64> expectedDetector = new FieldDateDetector<>(FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()));
        
        // WHEN
        final FieldNegateDetector<Binary64> negateDetector = new FieldNegateDetector<>(expectedDetector);
        
        // THEN
        Assertions.assertEquals(expectedDetector, negateDetector.getDetector());
        Assertions.assertEquals(expectedDetector, negateDetector.getOriginal());
    }

    /**
     * check g function is negated.
     */
    @Test
    void testG() {
        doTestG(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestG(final Field<T> field) {
        //setup
        @SuppressWarnings("unchecked")
        FieldEventDetector<T> a = Mockito.mock(FieldEventDetector.class);
        Mockito.when(a.getDetectionSettings()).thenReturn(new FieldEventDetectionSettings<>(field,
                EventDetectionSettings.getDefaultEventDetectionSettings()));
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
    void testCreate() {
        doTestCreate(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestCreate(final Field<T> field) {
        //setup
        @SuppressWarnings("unchecked")
        FieldEventDetector<T> a = Mockito.mock(FieldEventDetector.class);
        Mockito.when(a.getDetectionSettings()).thenReturn(new FieldEventDetectionSettings<>(field,
                EventDetectionSettings.getDefaultEventDetectionSettings()));
        FieldNegateDetector<T> detector = new FieldNegateDetector<>(a);

        // action
        FieldNegateDetector<T> actual = detector.withMaxCheck(100);

        //verify
        MatcherAssert.assertThat(actual.getMaxCheckInterval().currentInterval(null, true), CoreMatchers.is(100.0));
        Assertions.assertTrue(actual.getOriginal() == a);
    }
}
