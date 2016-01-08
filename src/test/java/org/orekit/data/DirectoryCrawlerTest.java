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
package org.orekit.data;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;

public class DirectoryCrawlerTest {

    @Test(expected=OrekitException.class)
    public void testNoDirectory() throws OrekitException, URISyntaxException {
        File existing = new File(getClass().getClassLoader().getResource("regular-data").toURI().getPath());
        File inexistent = new File(existing.getParent(), "inexistant-directory");
        new DirectoryCrawler(inexistent).feed(Pattern.compile(".*"), new CountingLoader());
   }

    @Test(expected=OrekitException.class)
    public void testNotADirectory() throws OrekitException, URISyntaxException {
        URL url =
            DirectoryCrawlerTest.class.getClassLoader().getResource("regular-data/UTC-TAI.history");
        new DirectoryCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*"), new CountingLoader());
    }

    @Test
    public void testNominal() throws OrekitException, URISyntaxException {
        URL url =
            DirectoryCrawlerTest.class.getClassLoader().getResource("regular-data");
        CountingLoader crawler = new CountingLoader();
        new DirectoryCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*"), crawler);
        Assert.assertTrue(crawler.getCount() > 0);
    }

    @Test
    public void testCompressed() throws OrekitException, URISyntaxException {
        URL url =
            DirectoryCrawlerTest.class.getClassLoader().getResource("compressed-data");
        CountingLoader crawler = new CountingLoader();
        new DirectoryCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*"), crawler);
        Assert.assertTrue(crawler.getCount() > 0);
    }

    @Test
    public void testMultiZipClasspath() throws OrekitException, URISyntaxException {
        URL url =
            DirectoryCrawlerTest.class.getClassLoader().getResource("zipped-data/multizip.zip");
        File parent = new File(url.toURI().getPath()).getParentFile();
        CountingLoader crawler = new CountingLoader();
        new DirectoryCrawler(parent).feed(Pattern.compile(".*\\.txt$"), crawler);
        Assert.assertEquals(6, crawler.getCount());
    }

    @Test(expected=OrekitException.class)
    public void testIOException() throws OrekitException, URISyntaxException {
        URL url =
            DirectoryCrawlerTest.class.getClassLoader().getResource("regular-data");
        try {
            new DirectoryCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*"), new IOExceptionLoader());
        } catch (OrekitException oe) {
            // expected behavior
            Assert.assertNotNull(oe.getCause());
            Assert.assertEquals(IOException.class, oe.getCause().getClass());
            Assert.assertEquals("dummy error", oe.getMessage());
            throw oe;
        }
    }

    @Test(expected=OrekitException.class)
    public void testParseException() throws OrekitException, URISyntaxException {
        URL url =
            DirectoryCrawlerTest.class.getClassLoader().getResource("regular-data");
        try {
            new DirectoryCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*"), new ParseExceptionLoader());
        } catch (OrekitException oe) {
            // expected behavior
            Assert.assertNotNull(oe.getCause());
            Assert.assertEquals(ParseException.class, oe.getCause().getClass());
            Assert.assertEquals("dummy error", oe.getMessage());
            throw oe;
        }
    }

    private static class CountingLoader implements DataLoader {
        private int count = 0;
        public boolean stillAcceptsData() {
            return true;
        }
        public void loadData(InputStream input, String name) {
            ++count;
        }
        public int getCount() {
            return count;
        }
    }

    private static class IOExceptionLoader implements DataLoader {
        public boolean stillAcceptsData() {
            return true;
        }
        public void loadData(InputStream input, String name) throws IOException {
            if (name.endsWith("UTC-TAI.history")) {
                throw new IOException("dummy error");
            }
        }
    }

    private static class ParseExceptionLoader implements DataLoader {
        public boolean stillAcceptsData() {
            return true;
        }
        public void loadData(InputStream input, String name) throws ParseException {
            if (name.endsWith("UTC-TAI.history")) {
                throw new ParseException("dummy error", 0);
            }
        }
    }

}
