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
package org.orekit.data;


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

import org.junit.Assert;
import org.junit.Test;

public class DataSourceTest {

    @Test
    public void testNullStream() throws IOException {
        DataSource ds = new DataSource("non-existent", () -> (InputStream) null);
        Assert.assertEquals("non-existent", ds.getName());
        Assert.assertNull(ds.getOpener().openStreamOnce());
        Assert.assertNull(ds.getOpener().openReaderOnce());
    }

    @Test
    public void testNullReader() throws IOException {
        DataSource ds = new DataSource("non-existent", () -> (Reader) null);
        Assert.assertEquals("non-existent", ds.getName());
        Assert.assertNull(ds.getOpener().openStreamOnce());
        Assert.assertNull(ds.getOpener().openReaderOnce());
    }

    @Test
    public void testFileName() throws IOException, URISyntaxException {
        URL url = DirectoryCrawlerTest.class.getClassLoader().getResource("regular-data/UTC-TAI.history");
        DataSource ds = new DataSource(Paths.get(url.toURI()).toString());
        Assert.assertTrue(ds.getName().endsWith("UTC-TAI.history"));
        Assert.assertTrue(ds.getOpener().rawDataIsBinary());
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
        Assert.assertTrue(ds.getName().endsWith("UTC-TAI.history"));
        Assert.assertTrue(ds.getOpener().rawDataIsBinary());
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
        Assert.assertTrue(ds.getName().endsWith("UTC-TAI.history"));
        Assert.assertTrue(ds.getOpener().rawDataIsBinary());
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
        Assert.assertEquals("UTC-TAI.history", ds.getName());
        Assert.assertTrue(ds.getOpener().rawDataIsBinary());
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
        Assert.assertEquals("UTC-TAI.history", ds.getName());
        Assert.assertTrue(ds.getOpener().rawDataIsBinary());
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
        Assert.assertEquals("UTC-TAI.history", ds.getName());
        Assert.assertFalse(ds.getOpener().rawDataIsBinary());
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
        Assert.assertEquals("UTC-TAI.history", ds.getName());
        Assert.assertFalse(ds.getOpener().rawDataIsBinary());
        try (InputStream       is  = ds.getOpener().openStreamOnce();
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader    br  = new BufferedReader(isr)) {
            checkHistory(br);
        }
    }

    private void checkHistory(final BufferedReader br) throws IOException {
        Assert.assertEquals("", br.readLine());
        Assert.assertEquals(" ---------------", br.readLine());
        Assert.assertEquals(" UTC-TAI.history", br.readLine());
        Assert.assertEquals(" ---------------", br.readLine());
        Assert.assertEquals(" RELATIONSHIP BETWEEN TAI AND UTC", br.readLine());
        for (int lineNumber = 6; lineNumber < 47; ++lineNumber) {
            br.readLine();
        }
        Assert.assertEquals(" 2012  Jul   1 - 2015  Jul   1    35s", br.readLine());
        Assert.assertEquals(" 2015  Jul   1 -                  36s", br.readLine());
        Assert.assertEquals(" ----------------------------------------------------------------------", br.readLine());
        Assert.assertNull(br.readLine());
    }

}
