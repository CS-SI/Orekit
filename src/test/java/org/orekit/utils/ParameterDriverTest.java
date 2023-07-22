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
package org.orekit.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TimeSpanMap.Span;

public class ParameterDriverTest {

	@Test
    public void testPDriverConstruction(){
        ParameterDriver p1 = new ParameterDriver("p1", 0.0, 1.0, -10.0, +10.0);
        AbsoluteDate date = new AbsoluteDate(2010, 11, 02, 03, 0, 0, TimeScalesFactory.getUTC());

        p1.addSpanAtDate(date);
        p1.setValue(3.0, date.shiftedBy(10));
        Assertions.assertEquals(3.0, p1.getValue(date.shiftedBy(10)), 1e-10);
        Assertions.assertEquals(0.0, p1.getValue(date.shiftedBy(-10)), 1e-10);
        Assertions.assertEquals("Span" + p1.getName() + Integer.toString(0), p1.getNameSpan(date.shiftedBy(-10)));
        Assertions.assertEquals("Span" + p1.getName() + Integer.toString(1), p1.getNameSpan(date.shiftedBy(10)));

        p1.addSpanAtDate(date.shiftedBy(2 * 24 * 3600));
        p1.setValue(6.0, date.shiftedBy(2 * 24 * 3600));
        Assertions.assertEquals(p1.getValue(date.shiftedBy(2 * 24 * 3600 + 10)), 6.0, 1e-10);
        int nb = 0;
        for (Span<String> span = p1.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
        	Assertions.assertEquals(span.getData(),"Span" + p1.getName() + Integer.toString(nb++));
        }
        
        p1.setName("p1_new");
        nb = 0;
        for (Span<String> span = p1.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
        	Assertions.assertEquals(span.getData(),"Span" + p1.getName() + Integer.toString(nb++));
        }
        
        
	}

	@Test
    public void testExceptionSetPeriod(){
        ParameterDriver p1 = new ParameterDriver("p1", 0.0, 1.0, -1.0, +1.0);
        AbsoluteDate date = new AbsoluteDate(2010, 11, 02, 03, 0, 0, TimeScalesFactory.getUTC());
        p1.addSpans(date, date.shiftedBy(15 * 3600), 3 * 3600);
        int nb = 0;
        for (Span<String> span = p1.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
        	Assertions.assertEquals(span.getData(),"Span" + p1.getName() + Integer.toString(nb++));
        }
        try {
            p1.addSpans(date, date.shiftedBy(15 * 3600), 5*3600);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalStateException oe) {
        	Assertions.assertEquals(OrekitMessages.PARAMETER_PERIODS_HAS_ALREADY_BEEN_SET, oe.getSpecifier());
        	Assertions.assertEquals(p1.getName(), oe.getParts()[0]);
        }
        
	}

	@Test
    public void testExceptiongetValue(){
        ParameterDriver p1 = new ParameterDriver("p1", 0.0, 1.0, -1.0, +1.0);
        AbsoluteDate date = new AbsoluteDate(2010, 11, 02, 03, 0, 0, TimeScalesFactory.getUTC());
        p1.addSpans(date, date.shiftedBy(15 * 3600), 3 * 3600);
        try {
            p1.getNormalizedValue();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalStateException oe) {
        	Assertions.assertEquals(OrekitMessages.PARAMETER_WITH_SEVERAL_ESTIMATED_VALUES, oe.getSpecifier());
        	Assertions.assertEquals(p1.getName(), oe.getParts()[0]);
        	Assertions.assertEquals("getValue(date)", oe.getParts()[1]);
        }
        
    }

	@Test
    public void testExceptionsetValue(){
        ParameterDriver p1 = new ParameterDriver("p1", 0.0, 1.0, -1.0, +1.0);
        AbsoluteDate date = new AbsoluteDate(2010, 11, 02, 03, 0, 0, TimeScalesFactory.getUTC());
        p1.addSpans(date, date.shiftedBy(15 * 3600), 3 * 3600);
        p1.setValue(30., date.shiftedBy(-100));
        Assertions.assertEquals(1.0, p1.getValue(date.shiftedBy(-500)), 0);
        p1.setValue(0.8, date.shiftedBy(-100));
        Assertions.assertEquals(0.8, p1.getValue(date.shiftedBy(-500)), 0);
        try {
            p1.setNormalizedValue(2.0);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalStateException oe) {
        	Assertions.assertEquals(OrekitMessages.PARAMETER_WITH_SEVERAL_ESTIMATED_VALUES, oe.getSpecifier());
        	Assertions.assertEquals(p1.getName(), oe.getParts()[0]);
        	Assertions.assertEquals("setValue(date)", oe.getParts()[1]);
        }
        
    }
	
    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
    
}
