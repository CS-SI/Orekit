/* Copyright 2002-2025 Airbus Defence and Space
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Airbus Defence and Space licenses this file to You under the Apache License, Version 2.0
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DataDictionaryTest {

    @Test
    public void testEmpty() {
        Assertions.assertTrue(new DataDictionary().getData().isEmpty());
    }

    @Test
    public void testPutGet() {
        DataDictionary dictionary = new DataDictionary();
        dictionary.put("a", new double[]{1.0, 2.0, 3.0});
        dictionary.put("string", "Lorem Ipsum");
        dictionary.put("int", 9);
        File file = new File(".");
        dictionary.put("object", file);

        Assertions.assertArrayEquals(new double[]{1.0, 2.0, 3.0}, ((double[]) dictionary.get("a")), 1.0e-15);
        Assertions.assertEquals("Lorem Ipsum", ((String) dictionary.get("string")));
        Assertions.assertEquals(9, dictionary.get("int"));
        Assertions.assertEquals(file, dictionary.get("object"));
    }

    @Test
    public void testGetNotExisting() {
        DataDictionary dictionary = new DataDictionary();
        Assertions.assertNull(dictionary.get("not-a-key"));
    }

    @Test
    public void testFromDictionary() {
        DataDictionary original = new DataDictionary();
        original.put("string", "Lorem Ipsum");

        DataDictionary copy = new DataDictionary(original);
        Assertions.assertEquals("Lorem Ipsum", ((String) copy.get("string")));
    }

    @Test
    public void testArraysAreCopied() {
        DataDictionary dictionary = new DataDictionary();
        double[] original = new double[]{1.0, 2.0, 3.0};
        dictionary.put("a", original);
        double[] retrieved = ((double[]) dictionary.get("a"));
        Assertions.assertArrayEquals(original, retrieved, 1.0e-15);
        Assertions.assertNotSame(original, retrieved);
    }

    @Test
    public void testIncrement() {
        DataDictionary dictionary = new DataDictionary();
        dictionary.put("a", new double[]{1.0, 2.0, 3.0});
        dictionary.getEntry("a").increment(new double[]{2.0, 4.0, 8.0});
        Assertions.assertArrayEquals(new double[]{3.0, 6.0, 11.0}, ((double[]) dictionary.get("a")), 1.0e-15);
    }

    @Test
    public void testScaledIncrement() {
        DataDictionary dictionary = new DataDictionary();
        dictionary.put("a", new double[]{1.0, 2.0, 3.0});
        DoubleArrayDictionary other = new DoubleArrayDictionary();
        other.put("aDot", new double[]{3.0, 2.0, 1.0});
        dictionary.getEntry("a").scaledIncrement(2.0, other.getEntry("aDot"));
        Assertions.assertArrayEquals(new double[]{7.0, 6.0, 5.0}, ((double[]) dictionary.get("a")), 1.0e-15);
    }

    @Test
    public void testZero() {
        DataDictionary dictionary = new DataDictionary();
        dictionary.put("a", new double[]{1.0, 2.0, 3.0});
        dictionary.getEntry("a").zero();
        Assertions.assertArrayEquals(new double[]{0.0, 0.0, 0.0}, ((double[]) dictionary.get("a")), 1.0e-15);
    }

    @Test
    public void testDataManagement() {
        DataDictionary dictionary = new DataDictionary();
        dictionary.put("a", new double[]{1.0, 2.0, 3.0});
        dictionary.put("b", new double[]{4.0});
        dictionary.put("another", new double[]{17.0});

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
    }

    @Test
    public void testReplace() {
        DataDictionary dictionary = new DataDictionary();
        dictionary.put("a", new double[]{1.0, 2.0, 3.0});
        dictionary.put("toreplace", "Lorem Ipsum");
        dictionary.put("b", 9);
        Assertions.assertEquals(3, dictionary.size());

        File file = new File(".");
        dictionary.put("toreplace", file);
        Assertions.assertEquals(3, dictionary.size());

        Assertions.assertArrayEquals(new double[]{1.0, 2.0, 3.0}, ((double[]) dictionary.get("a")), 1.0e-15);
        Assertions.assertEquals(file, dictionary.get("toreplace"));
        Assertions.assertEquals(9, dictionary.get("b"));
        Assertions.assertEquals("a", dictionary.getData().get(0).getKey());
        Assertions.assertEquals("b", dictionary.getData().get(1).getKey());
        Assertions.assertEquals("toreplace", dictionary.getData().get(2).getKey());
    }

    @Test
    public void testPutAllDoublesMap() {
        DataDictionary dictionary = new DataDictionary();
        dictionary.put("a", new double[]{1.0, 2.0, 3.0});
        dictionary.put("b", new double[]{4.0});
        dictionary.put("another", new double[]{17.0});
        Assertions.assertEquals(3, dictionary.size());

        final Map<String, double[]> map = new HashMap<>();
        map.put("f", new double[]{12.0});
        map.put("g", new double[]{-12.0});
        map.put("b", new double[]{19.0});

        dictionary.putAllDoubles(map);
        Assertions.assertEquals(5, dictionary.size());

        Assertions.assertArrayEquals(new double[]{1.0, 2.0, 3.0}, ((double[]) dictionary.get("a")), 1.0e-15);
        Assertions.assertArrayEquals(new double[]{19.0}, ((double[]) dictionary.get("b")), 1.0e-15);
        Assertions.assertArrayEquals(new double[]{17.0}, ((double[]) dictionary.get("another")), 1.0e-15);
        Assertions.assertArrayEquals(new double[]{12.0}, ((double[]) dictionary.get("f")), 1.0e-15);
        Assertions.assertArrayEquals(new double[]{-12.0}, ((double[]) dictionary.get("g")), 1.0e-15);
    }

    @Test
    public void testPutAllDictionary() {
        DataDictionary dictionary = new DataDictionary();
        dictionary.put("a", new double[]{1.0, 2.0, 3.0});
        dictionary.put("b", new double[]{19.0});
        dictionary.put("another", new double[]{17.0});
        Assertions.assertEquals(3, dictionary.size());

        DataDictionary other = new DataDictionary();
        dictionary.put("d", "Lorem Ipsum");
        dictionary.put("e", 9);
        File file = new File(".");
        dictionary.put("another", file);

        dictionary.putAll(other);
        Assertions.assertEquals(5, dictionary.size());

        Assertions.assertArrayEquals(new double[]{1.0, 2.0, 3.0}, ((double[]) dictionary.get("a")), 1.0e-15);
        Assertions.assertArrayEquals(new double[]{19.0}, ((double[]) dictionary.get("b")), 1.0e-15);
        Assertions.assertEquals(file, dictionary.get("another") );
        Assertions.assertEquals("Lorem Ipsum", dictionary.get("d"));
        Assertions.assertEquals(9, dictionary.get("e"));
    }

    @Test
    public void testToMap() {
        DataDictionary dictionary = new DataDictionary();
        dictionary.put("a", new double[]{1.0, 2.0, 3.0});
        dictionary.put("d", "Lorem Ipsum");
        dictionary.put("e", 9);
        Assertions.assertEquals(3, dictionary.size());

        Map<String, Object> map = dictionary.toMap();
        Assertions.assertEquals(3, map.size());

        Assertions.assertArrayEquals(new double[]{1.0, 2.0, 3.0}, ((double[]) map.get("a")), 1.0e-15);
        Assertions.assertEquals("Lorem Ipsum", dictionary.get("d"));
        Assertions.assertEquals(9, dictionary.get("e"));

        dictionary.clear();
        Assertions.assertEquals(0, dictionary.size());
        Assertions.assertEquals(3, map.size());
        map.put("z", new double[]{});
        Assertions.assertEquals(4, map.size());
        Assertions.assertEquals(0, dictionary.size());
    }
}
