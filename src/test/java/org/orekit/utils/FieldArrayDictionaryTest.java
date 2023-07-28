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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FieldArrayDictionaryTest {

    @Test
    public void testEmpty() {
        doTestEmpty(Binary64Field.getInstance());
    }

    @Test
    public void testPutGet() {
        doTestPutGet(Binary64Field.getInstance());
    }

    @Test
    public void testFromDictionary() {
        doTestFromDictionary(Binary64Field.getInstance());
    }

    @Test
    public void testFromMap() {
        doTestFromMap(Binary64Field.getInstance());
    }

    @Test
    public void testArraysAreCopied() {
        doTestArraysAreCopied(Binary64Field.getInstance());
    }

    @Test
    public void testIncrementField() {
        doTestIncrementField(Binary64Field.getInstance());
    }

    @Test
    public void testIncrementDouble() {
        doTestIncrementDouble(Binary64Field.getInstance());
    }

    @Test
    public void testScaledIncrementField() {
        doTestScaledIncrementField(Binary64Field.getInstance());
    }

    @Test
    public void testScaledIncrementDouble() {
        doTestScaledIncrementDouble(Binary64Field.getInstance());
    }

    @Test
    public void testZero() {
        doTestZero(Binary64Field.getInstance());
    }

    @Test
    public void testSize() {
        doTestSize(Binary64Field.getInstance());
    }

    @Test
    public void testDataManagement() {
        doTestDataManagement(Binary64Field.getInstance());
    }

    @Test
    public void testReplace() {
        doTestReplace(Binary64Field.getInstance());
    }

    @Test
    public void testPutAllMap() {
        doTestPutAllMap(Binary64Field.getInstance());
    }

    @Test
    public void testPutAllDictionary() {
        doTestPutAllDictionary(Binary64Field.getInstance());
    }

    @Test
    public void testToMap() {
        doTestToMap(Binary64Field.getInstance());
    }

    @Test
    public void testView() {
        doTestView(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestEmpty(Field<T> field) {
        Assertions.assertTrue(new FieldArrayDictionary<>(field).getData().isEmpty());
    }

    private <T extends CalculusFieldElement<T>> void doTestPutGet(Field<T> field) {

        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       convert(field, new double[] { 4.0 }));
        dictionary.put("another", convert(field, new double[] { 17.0 }));

        checkArray(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"),       1.0e-15);
        checkArray(new double[] { 17.0 },          dictionary.get("another"), 1.0e-15);
        checkArray(new double[] { 4.0 },           dictionary.get("b"),       1.0e-15);

        Assertions.assertNull(dictionary.get("not-a-key"));

    }

    private <T extends CalculusFieldElement<T>> void doTestFromDictionary(Field<T> field) {
        FieldArrayDictionary<T> original = new FieldArrayDictionary<>(field);
        original.put("a",       new double[] { 1.0, 2.0, 3.0 });
        original.put("b",       new double[] { 4.0 });
        original.put("another", new double[] { 17.0 });

        FieldArrayDictionary<T> copy = new FieldArrayDictionary<>(original);

        checkArray(new double[] { 1.0, 2.0, 3.0 }, copy.get("a"),       1.0e-15);
        checkArray(new double[] { 17.0 },          copy.get("another"), 1.0e-15);
        checkArray(new double[] { 4.0 },           copy.get("b"),       1.0e-15);

        Assertions.assertNull(copy.get("not-a-key"));

    }

    private <T extends CalculusFieldElement<T>> void doTestFromMap(Field<T> field) {
        final Map<String, T[]> map = new HashMap<>();
        map.put("a",       convert(field, new double[] { 1.0, 2.0, 3.0 }));
        map.put("b",       convert(field, new double[] { 4.0 }));
        map.put("another", convert(field, new double[] { 17.0 }));

        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field, map);

        checkArray(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"),       1.0e-15);
        checkArray(new double[] { 17.0 },          dictionary.get("another"), 1.0e-15);
        checkArray(new double[] { 4.0 },           dictionary.get("b"),       1.0e-15);

        Assertions.assertNull(dictionary.get("not-a-key"));

    }

    private <T extends CalculusFieldElement<T>> void doTestArraysAreCopied(Field<T> field) {
        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        T[] original = convert(field, new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("a", original);
        T[] retrieved = dictionary.get("a");
        checkArray(new double[] { 1.0, 2.0, 3.0 }, retrieved, 1.0e-15);
        Assertions.assertNotSame(original, retrieved);
    }

    private <T extends CalculusFieldElement<T>> void doTestIncrementField(Field<T> field) {
        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        dictionary.put("a",       convert(field, new double[] { 1.0, 2.0, 3.0 }));
        dictionary.getEntry("a").increment(convert(field, new double[] { 2.0, 4.0, 8.0 }));
        checkArray(new double[] { 3.0, 6.0, 11.0 }, dictionary.get("a"), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestIncrementDouble(Field<T> field) {
        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        dictionary.put("a",       convert(field, new double[] { 1.0, 2.0, 3.0 }));
        dictionary.getEntry("a").increment(new double[] { 2.0, 4.0, 8.0 });
        checkArray(new double[] { 3.0, 6.0, 11.0 }, dictionary.get("a"), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestScaledIncrementField(Field<T> field) {
        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        dictionary.put("a",       convert(field, new double[] { 1.0, 2.0, 3.0 }));
        FieldArrayDictionary<T> other = new FieldArrayDictionary<>(field);
        other.put("aDot",       convert(field, new double[] { 3.0, 2.0, 1.0 }));
        dictionary.getEntry("a").scaledIncrement(field.getZero().newInstance(2.0), other.getEntry("aDot"));
        checkArray(new double[] { 7.0, 6.0, 5.0 }, dictionary.get("a"), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestScaledIncrementDouble(Field<T> field) {
        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        dictionary.put("a",       convert(field, new double[] { 1.0, 2.0, 3.0 }));
        FieldArrayDictionary<T> other = new FieldArrayDictionary<>(field);
        other.put("aDot",       convert(field, new double[] { 3.0, 2.0, 1.0 }));
        dictionary.getEntry("a").scaledIncrement(2.0, other.getEntry("aDot"));
        checkArray(new double[] { 7.0, 6.0, 5.0 }, dictionary.get("a"), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestZero(Field<T> field) {
        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.getEntry("a").zero();
        checkArray(new double[] { 0.0, 0.0, 0.0 }, dictionary.get("a"), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestSize(Field<T> field) {
        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
         Assertions.assertEquals(3, dictionary.getEntry("a").size(), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestDataManagement(Field<T> field) {
        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });

        Assertions.assertEquals(3, dictionary.size());
        Assertions.assertEquals("{a[3], b[1], another[1]}", dictionary.toString());

        Assertions.assertTrue(dictionary.remove("another"));
        Assertions.assertEquals(2, dictionary.size());
        Assertions.assertFalse(dictionary.remove("not-a-key"));
        Assertions.assertEquals(2, dictionary.size());

        Assertions.assertEquals("a", dictionary.getData().get(0).getKey());
        Assertions.assertEquals("b", dictionary.getData().get(1).getKey());

        dictionary.clear();
        Assertions.assertTrue(dictionary.getData().isEmpty());

        Assertions.assertSame(field, dictionary.getField());

    }

    private <T extends CalculusFieldElement<T>> void doTestReplace(Field<T> field) {

        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });
        Assertions.assertEquals(3, dictionary.size());

        dictionary.put("b",       new double[] { -1.0, -1.0 });
        Assertions.assertEquals(3, dictionary.size());

        checkArray(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"),       1.0e-15);
        checkArray(new double[] { 17.0 },          dictionary.get("another"), 1.0e-15);
        checkArray(new double[] { -1.0, -1.0 },    dictionary.get("b"),       1.0e-15);
        Assertions.assertEquals("a",       dictionary.getData().get(0).getKey());
        Assertions.assertEquals("another", dictionary.getData().get(1).getKey());
        Assertions.assertEquals("b",       dictionary.getData().get(2).getKey());

    }

    private <T extends CalculusFieldElement<T>> void doTestPutAllMap(Field<T> field) {

        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });
        Assertions.assertEquals(3, dictionary.size());

        final Map<String, T[]> map = new HashMap<>();
        map.put("f", convert(field, new double[] {  12.0 }));
        map.put("g", convert(field, new double[] { -12.0 }));
        map.put("b", convert(field, new double[] {  19.0 }));

        dictionary.putAll(map);
        Assertions.assertEquals(5, dictionary.size());

        checkArray(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"),       1.0e-15);
        checkArray(new double[] {  19.0 },         dictionary.get("b"),       1.0e-15);
        checkArray(new double[] {  17.0 },         dictionary.get("another"), 1.0e-15);
        checkArray(new double[] {  12.0 },         dictionary.get("f"),       1.0e-15);
        checkArray(new double[] { -12.0 },         dictionary.get("g"),       1.0e-15);

    }

    private <T extends CalculusFieldElement<T>> void doTestPutAllDictionary(Field<T> field) {

        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });
        Assertions.assertEquals(3, dictionary.size());

        FieldArrayDictionary<T> other = new FieldArrayDictionary<>(field);
        other.put("f", new double[] {  12.0 });
        other.put("g", new double[] { -12.0 });
        other.put("b", new double[] {  19.0 });

        dictionary.putAll(other);
        Assertions.assertEquals(5, dictionary.size());

        checkArray(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"),       1.0e-15);
        checkArray(new double[] {  19.0 },         dictionary.get("b"),       1.0e-15);
        checkArray(new double[] {  17.0 },         dictionary.get("another"), 1.0e-15);
        checkArray(new double[] {  12.0 },         dictionary.get("f"),       1.0e-15);
        checkArray(new double[] { -12.0 },         dictionary.get("g"),       1.0e-15);

    }

    private <T extends CalculusFieldElement<T>> void doTestToMap(Field<T> field) {
        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });
        Assertions.assertEquals(3, dictionary.size());

        Map<String, T[]> map = dictionary.toMap();
        Assertions.assertEquals(3, map.size());

        checkArray(new double[] { 1.0, 2.0, 3.0 }, map.get("a"),       1.0e-15);
        checkArray(new double[] {   4.0 },         map.get("b"),       1.0e-15);
        checkArray(new double[] {  17.0 },         map.get("another"), 1.0e-15);

        dictionary.clear();
        Assertions.assertEquals(0, dictionary.size());
        Assertions.assertEquals(3, map.size());
        map.put("z", MathArrays.buildArray(field, 0));
        Assertions.assertEquals(4, map.size());
        Assertions.assertEquals(0, dictionary.size());

    }

    private <T extends CalculusFieldElement<T>> void doTestView(Field<T> field) {
        FieldArrayDictionary<T> dictionary = new FieldArrayDictionary<>(field);
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });

        FieldArrayDictionary<T> view = dictionary.unmodifiableView();
        Assertions.assertEquals(3, view.size());
        Assertions.assertEquals(3, view.getData().size());
        Assertions.assertEquals(3, view.toMap().size());
        checkArray(new double[] { 1.0, 2.0, 3.0 }, view.get("a"),       1.0e-15);
        checkArray(new double[] {   4.0 },         view.get("b"),       1.0e-15);
        checkArray(new double[] {  17.0 }, view.getEntry("another").getValue(), 1.0e-15);

        dictionary.put("z", new double[] { 25.0 });
        Assertions.assertEquals(4, view.size());
        checkArray(new double[] { 25.0 },         view.get("z"), 1.0e-15);

        checkUnsupported(view, v -> v.clear());
        checkUnsupported(view, v -> v.put("tF", convert(field, new double[1])));
        checkUnsupported(view, v -> v.put("tD", new double[1]));
        checkUnsupported(view, v -> v.putAll(new FieldArrayDictionary<>(field)));
        checkUnsupported(view, v -> v.putAll(new HashMap<>()));
        checkUnsupported(view, v -> v.remove("a"));

    }

    private <T extends CalculusFieldElement<T>> void checkArray(final double[] expected, final T[] actual, final double eps) {
        Assertions.assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; ++i) {
            Assertions.assertEquals(expected[i], actual[i].getReal(), eps);
        }
    }

    private <T extends CalculusFieldElement<T>> T[] convert(final Field<T> field, final double[] a) {
        final T[] converted = MathArrays.buildArray(field, a.length);
        for (int i = 0; i < a.length; ++i) {
            converted[i] = field.getZero().newInstance(a[i]);
        }
        return converted;
    }

    private <T extends CalculusFieldElement<T>> void checkUnsupported(FieldArrayDictionary<T> d, Consumer<FieldArrayDictionary<T>> c) {
        try {
            c.accept(d);
            Assertions.fail("an exception should have been thrown");
        } catch (UnsupportedOperationException uoe) {
            // expected
        }
    }

}
