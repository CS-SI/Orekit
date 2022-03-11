/* Copyright 2002-2022 CS GROUP
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


import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

public class DoubleArrayDictionaryTest {

    @Test
    public void testEmpty() {
        Assert.assertTrue(new DoubleArrayDictionary().getData().isEmpty());
    }

    @Test
    public void testPutGet() {

        DoubleArrayDictionary dictionary = new DoubleArrayDictionary();
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });

        Assert.assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"),       1.0e-15);
        Assert.assertArrayEquals(new double[] { 17.0 },          dictionary.get("another"), 1.0e-15);
        Assert.assertArrayEquals(new double[] { 4.0 },           dictionary.get("b"),       1.0e-15);

        Assert.assertNull(dictionary.get("not-a-key"));

    }

    @Test
    public void testFromDictionary() {
        DoubleArrayDictionary original = new DoubleArrayDictionary();
        original.put("a",       new double[] { 1.0, 2.0, 3.0 });
        original.put("b",       new double[] { 4.0 });
        original.put("another", new double[] { 17.0 });

        DoubleArrayDictionary copy = new DoubleArrayDictionary(original);

        Assert.assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, copy.get("a"),       1.0e-15);
        Assert.assertArrayEquals(new double[] { 17.0 },          copy.get("another"), 1.0e-15);
        Assert.assertArrayEquals(new double[] { 4.0 },           copy.get("b"),       1.0e-15);

        Assert.assertNull(copy.get("not-a-key"));

    }

    @Test
    public void testFromMap() {
        final Map<String, double[]> map = new HashMap<>();
        map.put("a",       new double[] { 1.0, 2.0, 3.0 });
        map.put("b",       new double[] { 4.0 });
        map.put("another", new double[] { 17.0 });

        DoubleArrayDictionary dictionary = new DoubleArrayDictionary(map);

        Assert.assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"),       1.0e-15);
        Assert.assertArrayEquals(new double[] { 17.0 },          dictionary.get("another"), 1.0e-15);
        Assert.assertArrayEquals(new double[] { 4.0 },           dictionary.get("b"),       1.0e-15);

        Assert.assertNull(dictionary.get("not-a-key"));

    }

    @Test
    public void testArraysAreCopied() {
        DoubleArrayDictionary dictionary = new DoubleArrayDictionary();
        double[] original = new double[] { 1.0, 2.0, 3.0 };
        dictionary.put("a", original);
        double[] retrieved = dictionary.get("a");
        Assert.assertArrayEquals(original, retrieved, 1.0e-15);
        Assert.assertNotSame(original, retrieved);
    }

    @Test
    public void testIncrement() {
        DoubleArrayDictionary dictionary = new DoubleArrayDictionary();
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.getEntry("a").increment(new double[] { 2.0, 4.0, 8.0 });
        Assert.assertArrayEquals(new double[] { 3.0, 6.0, 11.0 }, dictionary.get("a"), 1.0e-15);
    }

    @Test
    public void testScaledIncrement() {
        DoubleArrayDictionary dictionary = new DoubleArrayDictionary();
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        DoubleArrayDictionary other = new DoubleArrayDictionary();
        other.put("aDot",       new double[] { 3.0, 2.0, 1.0 });
        dictionary.getEntry("a").scaledIncrement(2.0, other.getEntry("aDot"));
        Assert.assertArrayEquals(new double[] { 7.0, 6.0, 5.0 }, dictionary.get("a"), 1.0e-15);
    }

    @Test
    public void testZero() {
        DoubleArrayDictionary dictionary = new DoubleArrayDictionary();
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.getEntry("a").zero();
        Assert.assertArrayEquals(new double[] { 0.0, 0.0, 0.0 }, dictionary.get("a"), 1.0e-15);
    }

    @Test
    public void testSize() {
        DoubleArrayDictionary dictionary = new DoubleArrayDictionary();
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
         Assert.assertEquals(3, dictionary.getEntry("a").size(), 1.0e-15);
    }

    @Test
    public void testDataManagement() {
        DoubleArrayDictionary dictionary = new DoubleArrayDictionary();
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });

        Assert.assertEquals(3, dictionary.size());
        Assert.assertEquals("{a[3], b[1], another[1]}", dictionary.toString());

        Assert.assertTrue(dictionary.remove("another"));
        Assert.assertEquals(2, dictionary.size());
        Assert.assertFalse(dictionary.remove("not-a-key"));
        Assert.assertEquals(2, dictionary.size());

        Assert.assertEquals("a", dictionary.getData().get(0).getKey());
        Assert.assertEquals("b", dictionary.getData().get(1).getKey());

        dictionary.clear();
        Assert.assertTrue(dictionary.getData().isEmpty());

    }

    @Test
    public void testReplace() {

        DoubleArrayDictionary dictionary = new DoubleArrayDictionary();
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });
        Assert.assertEquals(3, dictionary.size());

        dictionary.put("b",       new double[] { -1.0, -1.0 });
        Assert.assertEquals(3, dictionary.size());

        Assert.assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"),       1.0e-15);
        Assert.assertArrayEquals(new double[] { 17.0 },          dictionary.get("another"), 1.0e-15);
        Assert.assertArrayEquals(new double[] { -1.0, -1.0 },    dictionary.get("b"),       1.0e-15);
        Assert.assertEquals("a",       dictionary.getData().get(0).getKey());
        Assert.assertEquals("another", dictionary.getData().get(1).getKey());
        Assert.assertEquals("b",       dictionary.getData().get(2).getKey());

    }

    @Test
    public void testPutAllMap() {

        DoubleArrayDictionary dictionary = new DoubleArrayDictionary();
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });
        Assert.assertEquals(3, dictionary.size());

        final Map<String, double[]> map = new HashMap<>();
        map.put("f", new double[] {  12.0 });
        map.put("g", new double[] { -12.0 });
        map.put("b", new double[] {  19.0 });

        dictionary.putAll(map);
        Assert.assertEquals(5, dictionary.size());

        Assert.assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"),       1.0e-15);
        Assert.assertArrayEquals(new double[] {  19.0 },         dictionary.get("b"),       1.0e-15);
        Assert.assertArrayEquals(new double[] {  17.0 },         dictionary.get("another"), 1.0e-15);
        Assert.assertArrayEquals(new double[] {  12.0 },         dictionary.get("f"),       1.0e-15);
        Assert.assertArrayEquals(new double[] { -12.0 },         dictionary.get("g"),       1.0e-15);

    }

    @Test
    public void testPutAllDictionary() {

        DoubleArrayDictionary dictionary = new DoubleArrayDictionary();
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });
        Assert.assertEquals(3, dictionary.size());

        DoubleArrayDictionary other = new DoubleArrayDictionary();
        other.put("f", new double[] {  12.0 });
        other.put("g", new double[] { -12.0 });
        other.put("b", new double[] {  19.0 });

        dictionary.putAll(other);
        Assert.assertEquals(5, dictionary.size());

        Assert.assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, dictionary.get("a"),       1.0e-15);
        Assert.assertArrayEquals(new double[] {  19.0 },         dictionary.get("b"),       1.0e-15);
        Assert.assertArrayEquals(new double[] {  17.0 },         dictionary.get("another"), 1.0e-15);
        Assert.assertArrayEquals(new double[] {  12.0 },         dictionary.get("f"),       1.0e-15);
        Assert.assertArrayEquals(new double[] { -12.0 },         dictionary.get("g"),       1.0e-15);

    }

    @Test
    public void testToMap() {
        DoubleArrayDictionary dictionary = new DoubleArrayDictionary();
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });
        Assert.assertEquals(3, dictionary.size());

        Map<String, double[]> map = dictionary.toMap();
        Assert.assertEquals(3, map.size());

        Assert.assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, map.get("a"),       1.0e-15);
        Assert.assertArrayEquals(new double[] {   4.0 },         map.get("b"),       1.0e-15);
        Assert.assertArrayEquals(new double[] {  17.0 },         map.get("another"), 1.0e-15);

        dictionary.clear();
        Assert.assertEquals(0, dictionary.size());
        Assert.assertEquals(3, map.size());
        map.put("z", new double[] {});
        Assert.assertEquals(4, map.size());
        Assert.assertEquals(0, dictionary.size());
       
    }

    @Test
    public void testView() {
        DoubleArrayDictionary dictionary = new DoubleArrayDictionary();
        dictionary.put("a",       new double[] { 1.0, 2.0, 3.0 });
        dictionary.put("b",       new double[] { 4.0 });
        dictionary.put("another", new double[] { 17.0 });

        DoubleArrayDictionary view = dictionary.unmodifiableView();
        Assert.assertEquals(3, view.size());
        Assert.assertEquals(3, view.getData().size());
        Assert.assertEquals(3, view.toMap().size());
        Assert.assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, view.get("a"),       1.0e-15);
        Assert.assertArrayEquals(new double[] {   4.0 },         view.get("b"),       1.0e-15);
        Assert.assertArrayEquals(new double[] {  17.0 }, view.getEntry("another").getValue(), 1.0e-15);

        dictionary.put("z", new double[] { 25.0 });
        Assert.assertEquals(4, view.size());
        Assert.assertArrayEquals(new double[] { 25.0 },         view.get("z"), 1.0e-15);

        checkUnsupported(view, v -> v.clear());
        checkUnsupported(view, v -> v.put("t", new double[1]));
        checkUnsupported(view, v -> v.putAll(new DoubleArrayDictionary()));
        checkUnsupported(view, v -> v.putAll(new HashMap<>()));
        checkUnsupported(view, v -> v.remove("a"));

    }

    private void checkUnsupported(DoubleArrayDictionary d, Consumer<DoubleArrayDictionary> c) {
        try {
            c.accept(d);
            Assert.fail("an exception should have been thrown");
        } catch (UnsupportedOperationException uoe) {
            // expected
        }
    }

}
