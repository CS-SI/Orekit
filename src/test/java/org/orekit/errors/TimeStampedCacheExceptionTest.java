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
package org.orekit.errors;


import java.util.Locale;

import org.hipparchus.exception.MathRuntimeException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;

public class TimeStampedCacheExceptionTest {

    @Test
    public void testMessage() {
        TimeStampedCacheException e =
                        new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                      AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        Assert.assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE, e.getSpecifier());
        Assert.assertEquals(1, e.getParts().length);
        Assert.assertEquals(0, ((AbsoluteDate) e.getParts()[0]).durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH), 1.0e-10);
        Assert.assertEquals(e.getMessage(Locale.getDefault()), e.getLocalizedMessage());
        Assert.assertEquals("impossible de générer des données avant le 1858-11-16T23:59:27.816",
                            e.getMessage(Locale.FRENCH));
    }

    @Test
    public void testCause() {
        TimeStampedCacheException e =
                        new TimeStampedCacheException(new ArrayIndexOutOfBoundsException(),
                                                      OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                      AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        Assert.assertTrue(e.getCause() instanceof ArrayIndexOutOfBoundsException);
        Assert.assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE, e.getSpecifier());
        Assert.assertEquals(1, e.getParts().length);
        Assert.assertEquals(0, ((AbsoluteDate) e.getParts()[0]).durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH), 1.0e-10);
        Assert.assertEquals(e.getMessage(Locale.getDefault()), e.getLocalizedMessage());
        Assert.assertEquals("impossible de générer des données avant le 1858-11-16T23:59:27.816",
                            e.getMessage(Locale.FRENCH));
    }

    @Test
    public void testUnwrapOrekitExceptionNeedsCreation() {
        OrekitException base = new OrekitException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                   AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        TimeStampedCacheException unwraped = TimeStampedCacheException.unwrap(base);
        Assert.assertSame(base, unwraped.getCause());
    }

    @Test
    public void testUnwrapOrekitExceptionSimpleExtraction() {
        TimeStampedCacheException base = new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                                       AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        OrekitException intermediate = new OrekitException(base);
        TimeStampedCacheException unwraped = TimeStampedCacheException.unwrap(intermediate);
        Assert.assertNull(unwraped.getCause());
        Assert.assertSame(base, unwraped);
    }

    @Test
    public void testUnwrapMathRuntimeExceptionNeedsCreation() {
        MathRuntimeException base = new MathRuntimeException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                             AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        TimeStampedCacheException unwraped = TimeStampedCacheException.unwrap(base);
        Assert.assertSame(base, unwraped.getCause());
    }

    @Test
    public void testUnwrapMathRuntimeExceptionSimpleExtraction() {
        TimeStampedCacheException base = new TimeStampedCacheException(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                                                                       AbsoluteDate.MODIFIED_JULIAN_EPOCH);
        MathRuntimeException intermediate = new MathRuntimeException(base, base.getSpecifier(), base.getParts());
        TimeStampedCacheException unwraped = TimeStampedCacheException.unwrap(intermediate);
        Assert.assertNull(unwraped.getCause());
        Assert.assertSame(base, unwraped);
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
    }

}
