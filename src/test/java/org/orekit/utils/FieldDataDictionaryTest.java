/* Copyright 2002-2025 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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


class FieldDataDictionaryTest {


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
    public void testScaledIncrementField() {
        doTestScaledIncrementField(Binary64Field.getInstance());
    }

    @Test
    public void testScaledIncrementDouble() {
        doTestScaledIncrementDouble(Binary64Field.getInstance());
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

    private <T extends CalculusFieldElement<T>> void doTestEmpty(Field<T> field) {
        Assertions.assertTrue(new FieldDataDictionary<>(field).getData().isEmpty());
    }

    private <T extends CalculusFieldElement<T>> void doTestPutGet(Field<T> field) {

        FieldDataDictionary<T> dictionary = new FieldDataDictionary<>(field);
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       convertToObject(field, new double[] { 4.0 }));
        dictionary.put("another", convertToObject(field, new double[] { 17.0 }));

        checkArray(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"));
        checkArray(new double[] { 17.0 },          dictionary.get("another"));
        checkArray(new double[] { 4.0 },           dictionary.get("b"));

        Assertions.assertNull(dictionary.get("not-a-key"));

    }

    private <T extends CalculusFieldElement<T>> void doTestFromDictionary(Field<T> field) {
        FieldDataDictionary<T> original = new FieldDataDictionary<>(field);
        original.put("a",       new double[] { 1.0, 2.0, 3.0 });
        original.put("b",       new double[] { 4.0 });
        original.put("another", new double[] { 17.0 });

        FieldDataDictionary<T> copy = new FieldDataDictionary<>(original);

        checkArray(new double[] { 1.0, 2.0, 3.0 }, copy.get("a"));
        checkArray(new double[] { 17.0 },          copy.get("another"));
        checkArray(new double[] { 4.0 },           copy.get("b"));

        Assertions.assertNull(copy.get("not-a-key"));

    }

    private <T extends CalculusFieldElement<T>> void doTestFromMap(Field<T> field) {
        final Map<String, Object> map = new HashMap<>();
        map.put("a",       convertToObject(field, new double[] { 1.0, 2.0, 3.0 }));
        map.put("b",       convertToObject(field, new double[] { 4.0 }));
        map.put("another", convertToObject(field, new double[] { 17.0 }));

        FieldDataDictionary<T> dictionary = new FieldDataDictionary<>(field, map);

        checkArray(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"));
        checkArray(new double[] { 17.0 },          dictionary.get("another"));
        checkArray(new double[] { 4.0 },           dictionary.get("b"));

        Assertions.assertNull(dictionary.get("not-a-key"));

    }

    private <T extends CalculusFieldElement<T>> void doTestArraysAreCopied(Field<T> field) {
        FieldDataDictionary<T> dictionary = new FieldDataDictionary<>(field);
        Object original = convertToObject(field, new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("a", original);
        Object retrieved = dictionary.get("a");
        checkArray(new double[] { 1.0, 2.0, 3.0 }, retrieved);
        Assertions.assertNotSame(original, retrieved);
    }

    private <T extends CalculusFieldElement<T>> void doTestScaledIncrementField(Field<T> field) {
        FieldDataDictionary<T> dictionary = new FieldDataDictionary<>(field);
        dictionary.put("a",       convertToObject(field, new double[] { 1.0, 2.0, 3.0 }));
        FieldArrayDictionary<T> other = new FieldArrayDictionary<>(field);
        other.put("aDot",       convertToFieldArray(field, new double[] { 3.0, 2.0, 1.0 }));
        dictionary.getEntry("a").scaledIncrement(field.getZero().newInstance(2.0), other.getEntry("aDot"));
        checkArray(new double[] { 7.0, 6.0, 5.0 }, dictionary.get("a"));

        FieldDataDictionary<T> dictionaryString = new FieldDataDictionary<>(field);
        dictionaryString.put("b", "hello");
        dictionaryString.getEntry("b").scaledIncrement(2.0, other.getEntry("aDot"));
        Assertions.assertEquals("hello", dictionaryString.get("b"));
    }

    private <T extends CalculusFieldElement<T>> void doTestScaledIncrementDouble(Field<T> field) {
        FieldDataDictionary<T> dictionary = new FieldDataDictionary<>(field);
        dictionary.put("a",       convertToObject(field, new double[] { 1.0, 2.0, 3.0 }));
        FieldArrayDictionary<T> other = new FieldArrayDictionary<>(field);
        other.put("aDot",       convertToFieldArray(field, new double[] { 3.0, 2.0, 1.0 }));
        dictionary.getEntry("a").scaledIncrement(2.0, other.getEntry("aDot"));
        checkArray(new double[] { 7.0, 6.0, 5.0 }, dictionary.get("a"));

        FieldDataDictionary<T> dictionaryString = new FieldDataDictionary<>(field);
        dictionaryString.put("b", "hello");
        dictionaryString.getEntry("b").scaledIncrement(2.0, other.getEntry("aDot"));
        Assertions.assertEquals("hello", dictionaryString.get("b"));
    }

    @Test
    void testToFieldArrayDictionary() {
        doTestToFieldArrayDictionary(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestToFieldArrayDictionary(Field<T> field) {
        FieldDataDictionary<T> dictionary = new FieldDataDictionary<>(field);
        dictionary.put("a", convertToObject(field, new double[] { 1.0, 2.0, 3.0 }));
        FieldArrayDictionary<T> fieldArrayDictionary1 = dictionary.toFieldArrayDictionary();
        checkArray(new double[] { 1.0, 2.0, 3.0 }, fieldArrayDictionary1.get("a"));

        FieldDataDictionary<T> dictionaryString = new FieldDataDictionary<>(field);
        dictionaryString.put("b", "hello");
        FieldArrayDictionary<T> fieldArrayDictionary2 = dictionaryString.toFieldArrayDictionary();
        Assertions.assertEquals(0, fieldArrayDictionary2.getData().size());
    }

    private <T extends CalculusFieldElement<T>> void doTestDataManagement(Field<T> field) {
        FieldDataDictionary<T> dictionary = new FieldDataDictionary<>(field);
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

        FieldDataDictionary<T> dictionary = new FieldDataDictionary<>(field);
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });
        Assertions.assertEquals(3, dictionary.size());

        dictionary.put("b",       new double[] { -1.0, -1.0 });
        Assertions.assertEquals(3, dictionary.size());

        checkArray(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"));
        checkArray(new double[] { 17.0 },          dictionary.get("another"));
        checkArray(new double[] { -1.0, -1.0 },    dictionary.get("b"));
        Assertions.assertEquals("a",       dictionary.getData().get(0).getKey());
        Assertions.assertEquals("another", dictionary.getData().get(1).getKey());
        Assertions.assertEquals("b",       dictionary.getData().get(2).getKey());

    }

    private <T extends CalculusFieldElement<T>> void doTestPutAllMap(Field<T> field) {

        FieldDataDictionary<T> dictionary = new FieldDataDictionary<>(field);
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });
        Assertions.assertEquals(3, dictionary.size());

        final Map<String, Object> map = new HashMap<>();
        map.put("f", convertToObject(field, new double[] {  12.0 }));
        map.put("g", convertToObject(field, new double[] { -12.0 }));
        map.put("b", convertToObject(field, new double[] {  19.0 }));

        dictionary.putAll(map);
        Assertions.assertEquals(5, dictionary.size());

        checkArray(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"));
        checkArray(new double[] {  19.0 },         dictionary.get("b"));
        checkArray(new double[] {  17.0 },         dictionary.get("another"));
        checkArray(new double[] {  12.0 },         dictionary.get("f"));
        checkArray(new double[] { -12.0 },         dictionary.get("g"));

    }

    private <T extends CalculusFieldElement<T>> void doTestPutAllDictionary(Field<T> field) {

        FieldDataDictionary<T> dictionary = new FieldDataDictionary<>(field);
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });
        Assertions.assertEquals(3, dictionary.size());

        FieldDataDictionary<T> other = new FieldDataDictionary<>(field);
        other.put("f", new double[] {  12.0 });
        other.put("g", new double[] { -12.0 });
        other.put("b", new double[] {  19.0 });

        dictionary.putAll(other);
        Assertions.assertEquals(5, dictionary.size());

        checkArray(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"));
        checkArray(new double[] {  19.0 },         dictionary.get("b"));
        checkArray(new double[] {  17.0 },         dictionary.get("another"));
        checkArray(new double[] {  12.0 },         dictionary.get("f"));
        checkArray(new double[] { -12.0 },         dictionary.get("g"));

    }

    private <T extends CalculusFieldElement<T>> void doTestToMap(Field<T> field) {
        FieldDataDictionary<T> dictionary = new FieldDataDictionary<>(field);
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });
        Assertions.assertEquals(3, dictionary.size());

        Map<String, Object> map = dictionary.toMap();
        Assertions.assertEquals(3, map.size());

        checkArray(new double[] { 1.0, 2.0, 3.0 }, map.get("a"));
        checkArray(new double[] {   4.0 },         map.get("b"));
        checkArray(new double[] {  17.0 },         map.get("another"));

        dictionary.clear();
        Assertions.assertEquals(0, dictionary.size());
        Assertions.assertEquals(3, map.size());
        map.put("z", MathArrays.buildArray(field, 0));
        Assertions.assertEquals(4, map.size());
        Assertions.assertEquals(0, dictionary.size());

    }

    @SuppressWarnings("rawtypes")
    private <T extends CalculusFieldElement<T>> void checkArray(final double[] expected, final Object actual) {
        if (actual instanceof double[]) {
            double[] actualField = (double[]) actual;
            Assertions.assertEquals(expected.length, actualField.length);
            for (int i = 0; i < expected.length; ++i) {
                Assertions.assertEquals(expected[i], actualField[i], 1.0E-15);
            }
        } else {
            CalculusFieldElement[] actualField = (CalculusFieldElement[]) actual;
            Assertions.assertEquals(expected.length, actualField.length);
            for (int i = 0; i < expected.length; ++i) {
                Assertions.assertEquals(expected[i], actualField[i].getReal(), 1.0E-15);
            }
        }
    }

    private <T extends CalculusFieldElement<T>> Object convertToObject(final Field<T> field, final double[] a) {
        final T[] converted = MathArrays.buildArray(field, a.length);
        for (int i = 0; i < a.length; ++i) {
            converted[i] = field.getZero().newInstance(a[i]);
        }
        return converted;
    }

    private <T extends CalculusFieldElement<T>> T[] convertToFieldArray(final Field<T> field, final double[] a) {
        final T[] converted = MathArrays.buildArray(field, a.length);
        for (int i = 0; i < a.length; ++i) {
            converted[i] = field.getZero().newInstance(a[i]);
        }
        return converted;
    }

}