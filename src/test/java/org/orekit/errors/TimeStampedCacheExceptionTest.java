/* Copyright 2002-2024 CS GROUP
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
package org.orekit.errors;

import org.hipparchus.exception.MathRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeStampedCacheExceptionTest {

    @Test
    void testMessage() {
        TimeStampedCacheException e =
                        new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                      AbsoluteDate.MODIFIED_JULIAN_EPOCH,
                                                      AbsoluteDate.MODIFIED_JULIAN_EPOCH.shiftedBy(-1e-16),
                                                      1e-16);
        assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE, e.getSpecifier());
        assertEquals(3, e.getParts().length);
        assertEquals(0, ((AbsoluteDate) e.getParts()[0]).durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH), 1.0e-10);
        assertEquals(e.getMessage(Locale.getDefault()), e.getLocalizedMessage());
        assertEquals("impossible de générer des données avant le 1858-11-16T23:59:27.816Z, données requises pour 1858-11-16T23:59:27.816Z qui est 1,0E-16 s avant",
                            e.getMessage(Locale.FRENCH));
    }

    @Test
    void testCause() {
        TimeStampedCacheException e =
                        new TimeStampedCacheException(new ArrayIndexOutOfBoundsException(),
                                                      OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                      AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        assertTrue(e.getCause() instanceof ArrayIndexOutOfBoundsException);
        assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE, e.getSpecifier());
        assertEquals(1, e.getParts().length);
        assertEquals(0, ((AbsoluteDate) e.getParts()[0]).durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH), 1.0e-10);
        assertEquals(e.getMessage(Locale.getDefault()), e.getLocalizedMessage());
        assertEquals("impossible de générer des données avant le 1858-11-16T23:59:27.816Z, données requises pour {1} qui est {2} s avant",
                            e.getMessage(Locale.FRENCH));
    }

    @Test
    void testUnwrapOrekitExceptionNeedsCreation() {
        OrekitException base = new OrekitException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                   AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        TimeStampedCacheException unwraped = TimeStampedCacheException.unwrap(base);
        assertSame(base, unwraped.getCause());
    }

    @Test
    void testUnwrapOrekitExceptionSimpleExtraction() {
        TimeStampedCacheException base = new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                                       AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        OrekitException intermediate = new OrekitException(base);
        TimeStampedCacheException unwraped = TimeStampedCacheException.unwrap(intermediate);
        assertNull(unwraped.getCause());
        assertSame(base, unwraped);
    }

    @Test
    void testUnwrapMathRuntimeExceptionNeedsCreation() {
        MathRuntimeException base = new MathRuntimeException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                             AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        TimeStampedCacheException unwraped = TimeStampedCacheException.unwrap(base);
        assertSame(base, unwraped.getCause());
    }

    @Test
    void testUnwrapMathRuntimeExceptionSimpleExtraction() {
        TimeStampedCacheException base = new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                                       AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        MathRuntimeException intermediate = new MathRuntimeException(base, base.getSpecifier(), base.getParts());
        TimeStampedCacheException unwraped = TimeStampedCacheException.unwrap(intermediate);
        assertNull(unwraped.getCause());
        assertSame(base, unwraped);
    }

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
