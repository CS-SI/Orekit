/* Copyright 2002-2026 CS GROUP
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

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterval;
import org.orekit.time.TimeOffset;

import java.util.concurrent.atomic.AtomicBoolean;

public class ParameterDriverTest {

	@Test
    public void testPDriverConstruction(){
        ParameterDriver p1 =
            new ParameterDriver("p1", 7.0, 1.0, -10.0, +10.0,
                                TimeInterval.of(AbsoluteDate.ARBITRARY_EPOCH,
                                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(TimeOffset.DAY),
                                                false));
        Assertions.assertEquals(7.0, p1.getValue(), 1e-10);
        p1.setValue(3.0);
        Assertions.assertEquals(3.0, p1.getValue(), 1e-10);

	}

    @Test
    public void testNonChronologicalInterval() {
        ParameterDriver p = new ParameterDriver("p", 0.0, 1.0, -1.0, +1.0, TimeInterval.UNLIMITED);

        try {
            p.setValidity(TimeInterval.of(AbsoluteDate.ARBITRARY_EPOCH,
                                          AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(TimeOffset.MILLISECOND.negate()),
                                          false));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_CHRONOLOGICALLY_SORTED_ENTRIES, oe.getSpecifier());
            Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH, oe.getParts()[0]);
            Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(TimeOffset.MILLISECOND.negate()),
                                    oe.getParts()[1]);
            Assertions.assertEquals(TimeOffset.MILLISECOND.toDouble(), oe.getParts()[2]);
        }

    }

    @Test
    public void testObserver() {
        ParameterDriver p = new ParameterDriver("p", 7.0, FastMath.scalb(1.0, -4), -10.0, +10.0, TimeInterval.UNLIMITED);

        final AtomicBoolean valueFlag          = new AtomicBoolean(Boolean.FALSE);
        final AtomicBoolean referenceDateFlag  = new AtomicBoolean(Boolean.FALSE);
        final AtomicBoolean referenceValueFlag = new AtomicBoolean(Boolean.FALSE);
        final AtomicBoolean nameFlag           = new AtomicBoolean(Boolean.FALSE);
        final AtomicBoolean selectionFlag      = new AtomicBoolean(Boolean.FALSE);
        final AtomicBoolean minFlag            = new AtomicBoolean(Boolean.FALSE);
        final AtomicBoolean maxFlag            = new AtomicBoolean(Boolean.FALSE);
        final AtomicBoolean scaleFlag          = new AtomicBoolean(Boolean.FALSE);
        final AtomicBoolean validityFlag       = new AtomicBoolean(Boolean.FALSE);

        p.addObserver(new ParameterObserver() {

            /** {@inheritDoc} */
            @Override
            public void valueChanged(final double previousValue, final ParameterDriver driver) {
                // we just chek the first call
                // because there will be additional ones with difference values
                if (!valueFlag.get()) {
                    Assertions.assertEquals( 7.0, previousValue, 1e-10);
                    Assertions.assertEquals(-7.0, driver.getValue(), 1e-10);
                    valueFlag.set(true);
                }
            }

            /** {@inheritDoc} */
            @Override
            public void referenceDateChanged(final AbsoluteDate previousReferenceDate, final ParameterDriver driver) {
                Assertions.assertNull(previousReferenceDate);
                Assertions.assertEquals(AbsoluteDate.CCSDS_EPOCH, driver.getReferenceDate());
                referenceDateFlag.set(true);
            }

            /** {@inheritDoc} */
            @Override
            public void referenceValueChanged(final double previousReferenceValue, final ParameterDriver driver) {
                Assertions.assertEquals(7.0, previousReferenceValue, 1e-10);
                Assertions.assertEquals(8.0, driver.getReferenceValue(), 1e-10);
                referenceValueFlag.set(true);
            }

            /** {@inheritDoc} */
            @Override
            public void nameChanged(final String previousName, final ParameterDriver driver) {
                Assertions.assertEquals("p", previousName);
                Assertions.assertEquals("q", driver.getName());
                nameFlag.set(true);
            }

            /** {@inheritDoc} */
            @Override
            public void selectionChanged(final boolean previousSelection, final ParameterDriver driver) {
                Assertions.assertFalse(previousSelection);
                Assertions.assertTrue(driver.isSelected());
                selectionFlag.set(true);
            }

            /** {@inheritDoc} */
            @Override
            public void minValueChanged(final double previousMinValue, final ParameterDriver driver) {
                Assertions.assertEquals(-10.0, previousMinValue, 1e-10);
                Assertions.assertEquals(-2.0, driver.getMinValue(), 1e-10);
                Assertions.assertTrue(driver.getValue() >= driver.getMinValue());
                minFlag.set(true);
            }

            /** {@inheritDoc} */
            @Override
            public void maxValueChanged(final double previousMaxValue, final ParameterDriver driver) {
                Assertions.assertEquals(10.0, previousMaxValue, 1e-10);
                Assertions.assertEquals(2.0, driver.getMaxValue(), 1e-10);
                Assertions.assertTrue(driver.getValue() <= driver.getMaxValue());
                maxFlag.set(true);
            }

            /** {@inheritDoc} */
            @Override
            public void scaleChanged(final double previousScale, final ParameterDriver driver) {
                Assertions.assertEquals(FastMath.scalb(1.0, -4), previousScale, 1e-10);
                Assertions.assertEquals(FastMath.scalb(1.0, -3), driver.getScale(), 1e-10);
                scaleFlag.set(true);
            }

            /** {@inheritDoc} */
            @Override
            public void validityChanged(final TimeInterval previousValidity,
                                        final ParameterDriver driver) {
                Assertions.assertEquals(AbsoluteDate.PAST_INFINITY,   previousValidity.getStartDate());
                Assertions.assertEquals(AbsoluteDate.FUTURE_INFINITY, previousValidity.getEndDate());
                Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH, driver.getValidity().getStartDate());
                Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH, driver.getValidity().getEndDate());
                validityFlag.set(true);
            }

        });

        Assertions.assertFalse(valueFlag.get());
        p.setValue(-7.0);
        Assertions.assertTrue(valueFlag.get());

        Assertions.assertFalse(referenceDateFlag.get());
        p.setReferenceDate(AbsoluteDate.CCSDS_EPOCH);
        Assertions.assertTrue(referenceDateFlag.get());

        Assertions.assertFalse(referenceValueFlag.get());
        p.setReferenceValue(8.0);
        Assertions.assertTrue(referenceValueFlag.get());

        Assertions.assertFalse(nameFlag.get());
        p.setName("q");
        Assertions.assertTrue(nameFlag.get());

        Assertions.assertFalse(selectionFlag.get());
        p.setSelected(true);
        Assertions.assertTrue(selectionFlag.get());

        Assertions.assertFalse(minFlag.get());
        try {
            p.setMinValue(300);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.NUMBER_TOO_LARGE, oe.getSpecifier());
            Assertions.assertEquals(300.0, (Double) oe.getParts()[0], 1.0e-9);
            Assertions.assertEquals( 10.0, (Double) oe.getParts()[1], 1.0e-9);
        }
        Assertions.assertEquals(-7.0, p.getValue(), 1.0e-9);
        p.setMinValue(-2.0);
        Assertions.assertEquals(-2.0, p.getValue(), 1.0e-9);
        Assertions.assertTrue(minFlag.get());
        Assertions.assertFalse(maxFlag.get());
        try {
            p.setMaxValue(-300);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.NUMBER_TOO_SMALL, oe.getSpecifier());
            Assertions.assertEquals(-300.0, (Double) oe.getParts()[0], 1.0e-9);
            Assertions.assertEquals( -2.0, (Double) oe.getParts()[1], 1.0e-9);
        }
        p.setValue(7.0);
        Assertions.assertEquals(7.0, p.getValue(), 1.0e-9);
        p.setMaxValue(2.0);
        Assertions.assertEquals(2.0, p.getValue(), 1.0e-9);
        Assertions.assertTrue(maxFlag.get());

        Assertions.assertFalse(scaleFlag.get());
        p.setScale(FastMath.scalb(1.0, -3));
        Assertions.assertTrue(scaleFlag.get());

        Assertions.assertFalse(validityFlag.get());
        p.setValidity(TimeInterval.of(AbsoluteDate.ARBITRARY_EPOCH, AbsoluteDate.ARBITRARY_EPOCH, true));
        Assertions.assertTrue(validityFlag.get());

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
    
}
