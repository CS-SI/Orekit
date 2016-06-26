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
import org.junit.Test;

@Deprecated
public class PropagationExceptionTest {

    @Test
    public void testMessage() {
        PropagationException e =
                        new PropagationException(OrekitMessages.NON_EXISTENT_HMS_TIME, 97, 98, 99);
        Assert.assertEquals(OrekitMessages.NON_EXISTENT_HMS_TIME, e.getSpecifier());
        Assert.assertEquals(3, e.getParts().length);
        Assert.assertEquals(97, ((Integer) e.getParts()[0]).intValue());
        Assert.assertEquals(98, ((Integer) e.getParts()[1]).intValue());
        Assert.assertEquals(99, ((Integer) e.getParts()[2]).intValue());
        Assert.assertTrue(e.getMessage().contains("98"));
        Assert.assertEquals(e.getMessage(Locale.getDefault()), e.getLocalizedMessage());
        Assert.assertEquals("heure inexistante 97:98:99", e.getMessage(Locale.FRENCH));
    }

    @Test
    public void testUnwrapOrekitExceptionNeedsCreation() {
        OrekitException base = new OrekitException(OrekitMessages.NON_EXISTENT_HMS_TIME, 97, 98, 99);
        PropagationException unwraped = PropagationException.unwrap(base);
        Assert.assertSame(base, unwraped.getCause());
    }

    @Test
    public void testUnwrapOrekitExceptionSimpleExtraction() {
        PropagationException base = new PropagationException(OrekitMessages.NON_EXISTENT_HMS_TIME, 97, 98, 99);
        OrekitException intermediate = new OrekitException(base);
        PropagationException unwraped = PropagationException.unwrap(intermediate);
        Assert.assertNull(unwraped.getCause());
        Assert.assertSame(base, unwraped);
    }

    @Test
    public void testUnwrapMathRuntimeExceptionNeedsCreation() {
        MathRuntimeException base = new MathRuntimeException(OrekitMessages.NON_EXISTENT_HMS_TIME, 97, 98, 99);
        PropagationException unwraped = PropagationException.unwrap(base);
        Assert.assertSame(base, unwraped.getCause());
    }

    @Test
    public void testUnwrapMathRuntimeExceptionSimpleExtraction() {
        PropagationException base = new PropagationException(OrekitMessages.NON_EXISTENT_HMS_TIME, 97, 98, 99);
        MathRuntimeException intermediate = new MathRuntimeException(base, base.getSpecifier(), base.getParts());
        PropagationException unwraped = PropagationException.unwrap(intermediate);
        Assert.assertNull(unwraped.getCause());
        Assert.assertSame(base, unwraped);
    }

}
