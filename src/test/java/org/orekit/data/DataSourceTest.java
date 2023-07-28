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
package org.orekit.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class DataSourceTest {

    @Test
    public void testNullStream() throws IOException {
        DataSource ds = new DataSource("non-existent", () -> (InputStream) null);
        Assertions.assertEquals("non-existent", ds.getName());
        Assertions.assertNull(ds.getOpener().openStreamOnce());
        Assertions.assertNull(ds.getOpener().openReaderOnce());
    }

    @Test
    public void testNullReader() throws IOException {
        DataSource ds = new DataSource("non-existent", () -> (Reader) null);
        Assertions.assertEquals("non-existent", ds.getName());
        Assertions.assertNull(ds.getOpener().openStreamOnce());
        Assertions.assertNull(ds.getOpener().openReaderOnce());
    }

    @Test
    public void testFileName() throws IOException, URISyntaxException {
        URL url = DirectoryCrawlerTest.class.getClassLoader().getResource("regular-data/UTC-TAI.history");
        DataSource ds = new DataSource(Paths.get(url.toURI()).toString());
        Assertions.assertTrue(ds.getName().endsWith("UTC-TAI.history"));
        Assertions.assertTrue(ds.getOpener().rawDataIsBinary());
        try (InputStream       is  = ds.getOpener().openStreamOnce();
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader    br  = new BufferedReader(isr)) {
            checkHistory(br);
        }
    }

    @Test
    public void testFile() throws IOException, URISyntaxException {
        URL url = DirectoryCrawlerTest.class.getClassLoader().getResource("regular-data/UTC-TAI.history");
        DataSource ds = new DataSource(new File(url.toURI().getPath()));
        Assertions.assertTrue(ds.getName().endsWith("UTC-TAI.history"));
        Assertions.assertTrue(ds.getOpener().rawDataIsBinary());
        try (InputStream       is  = ds.getOpener().openStreamOnce();
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader    br  = new BufferedReader(isr)) {
            checkHistory(br);
        }
    }

    @Test
    public void testUri() throws IOException, URISyntaxException {
        URL url = DirectoryCrawlerTest.class.getClassLoader().getResource("regular-data/UTC-TAI.history");
        DataSource ds = new DataSource(url.toURI());
        Assertions.assertTrue(ds.getName().endsWith("UTC-TAI.history"));
        Assertions.assertTrue(ds.getOpener().rawDataIsBinary());
        try (InputStream       is  = ds.getOpener().openStreamOnce();
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader    br  = new BufferedReader(isr)) {
            checkHistory(br);
        }
    }

    @Test
    public void testDirectInputStream() throws IOException {
        DataSource ds = new DataSource("UTC-TAI.history",
                                       () -> DataSourceTest.class.
                                             getClassLoader().
                                             getResourceAsStream("regular-data/UTC-TAI.history"));
        Assertions.assertEquals("UTC-TAI.history", ds.getName());
        Assertions.assertTrue(ds.getOpener().rawDataIsBinary());
        try (InputStream       is  = ds.getOpener().openStreamOnce();
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader    br  = new BufferedReader(isr)) {
            checkHistory(br);
        }
    }

    @Test
    public void testInputStreamToReader() throws IOException {
        DataSource ds = new DataSource("UTC-TAI.history",
                                       () -> DataSourceTest.class.
                                             getClassLoader().
                                             getResourceAsStream("regular-data/UTC-TAI.history"));
        Assertions.assertEquals("UTC-TAI.history", ds.getName());
        Assertions.assertTrue(ds.getOpener().rawDataIsBinary());
        try (Reader         r  = ds.getOpener().openReaderOnce();
             BufferedReader br = new BufferedReader(r)) {
            checkHistory(br);
        }
    }

    @Test
    public void testDirectReader() throws IOException {
        DataSource ds = new DataSource("UTC-TAI.history",
                                       () -> new InputStreamReader(DataSourceTest.class.
                                                                   getClassLoader().
                                                                   getResourceAsStream("regular-data/UTC-TAI.history"),
                                                                   StandardCharsets.UTF_8));
        Assertions.assertEquals("UTC-TAI.history", ds.getName());
        Assertions.assertFalse(ds.getOpener().rawDataIsBinary());
        try (Reader         r  = ds.getOpener().openReaderOnce();
             BufferedReader br = new BufferedReader(r)) {
            checkHistory(br);
        }
    }

    @Test
    public void testReaderToInputStream() throws IOException {
        DataSource ds = new DataSource("UTC-TAI.history",
                                       () -> new InputStreamReader(DataSourceTest.class.
                                                                   getClassLoader().
                                                                   getResourceAsStream("regular-data/UTC-TAI.history"),
                                                                   StandardCharsets.UTF_8));
        Assertions.assertEquals("UTC-TAI.history", ds.getName());
        Assertions.assertFalse(ds.getOpener().rawDataIsBinary());
        try (InputStream       is  = ds.getOpener().openStreamOnce();
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader    br  = new BufferedReader(isr)) {
            checkHistory(br);
        }
    }

    private void checkHistory(final BufferedReader br) throws IOException {
        Assertions.assertEquals("", br.readLine());
        Assertions.assertEquals(" ---------------", br.readLine());
        Assertions.assertEquals(" UTC-TAI.history", br.readLine());
        Assertions.assertEquals(" ---------------", br.readLine());
        Assertions.assertEquals(" RELATIONSHIP BETWEEN TAI AND UTC", br.readLine());
        for (int lineNumber = 6; lineNumber < 48; ++lineNumber) {
            br.readLine();
        }
        Assertions.assertEquals(" 2015  Jul   1 - 2017  Jan   1    36s", br.readLine());
        Assertions.assertEquals(" 2017  Jan   1 -                  37s", br.readLine());
        Assertions.assertEquals(" ----------------------------------------------------------------------", br.readLine());
        Assertions.assertNull(br.readLine());
    }

}
