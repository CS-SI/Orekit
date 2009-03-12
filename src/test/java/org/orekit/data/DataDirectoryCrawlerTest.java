/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.orekit.errors.OrekitException;

public class DataDirectoryCrawlerTest extends TestCase {

    public void testNoDirectory() {
        File existing = new File(getClass().getClassLoader().getResource("regular-data").getPath());
        File inexistent = new File(existing.getParent(), "inexistant-directory");
        checkFailure(inexistent);
    }

    public void testNotADirectory() {
        URL url =
            DataDirectoryCrawlerTest.class.getClassLoader().getResource("regular-data/UTC-TAI.history");
        checkFailure(new File(url.getPath()));
    }

    public void testNominal() throws OrekitException {
        URL url =
            DataDirectoryCrawlerTest.class.getClassLoader().getResource("regular-data");
        CountingLoader crawler = new CountingLoader(".*");
        new DataDirectoryCrawler(new File(url.getPath())).feed(crawler);
        assertTrue(crawler.getCount() > 0);
    }

    public void testCompressed() throws OrekitException {
        URL url =
            DataDirectoryCrawlerTest.class.getClassLoader().getResource("compressed-data");
        CountingLoader crawler = new CountingLoader(".*");
        new DataDirectoryCrawler(new File(url.getPath())).feed(crawler);
        assertTrue(crawler.getCount() > 0);
    }

    public void testMultiZipClasspath() throws OrekitException {
        URL url =
            DataDirectoryCrawlerTest.class.getClassLoader().getResource("multizip.zip");
        File parent = new File(url.getPath()).getParentFile();
        CountingLoader crawler = new CountingLoader(".*\\.txt$");
        new DataDirectoryCrawler(parent).feed(crawler);
        assertEquals(23, crawler.getCount());
    }

    public void testIOException() throws OrekitException {
        URL url =
            DataDirectoryCrawlerTest.class.getClassLoader().getResource("regular-data");
        try {
            new DataDirectoryCrawler(new File(url.getPath())).feed(new IOExceptionLoader(".*"));
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected behavior
            assertNotNull(oe.getCause());
            assertEquals(IOException.class, oe.getCause().getClass());
            assertEquals("dummy error", oe.getMessage());
        } catch (Exception e) {
            fail("wrong exception caught");
        }
    }

    public void testParseException() throws OrekitException {
        URL url =
            DataDirectoryCrawlerTest.class.getClassLoader().getResource("regular-data");
        try {
            new DataDirectoryCrawler(new File(url.getPath())).feed(new ParseExceptionLoader(".*"));
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected behavior
            assertNotNull(oe.getCause());
            assertEquals(ParseException.class, oe.getCause().getClass());
            assertEquals("dummy error", oe.getMessage());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail("wrong exception caught");
        }
    }

    private void checkFailure(File root) {
        try {
            new DataDirectoryCrawler(root).feed(new CountingLoader(".*"));
            fail("an exception should have been thrown");
        } catch (OrekitException e) {
            // expected behavior
        } catch (Exception e) {
            e.printStackTrace();
            fail("wrong exception caught");
        }
    }

    private static class CountingLoader implements DataFileLoader {
        private Pattern namePattern;
        private int count;
        public CountingLoader(String pattern) {
            namePattern = Pattern.compile(pattern);
            count = 0;
        }
        public void loadData(InputStream input, String name) {
            ++count;
        }
        public int getCount() {
            return count;
        }
        public boolean fileIsSupported(String fileName) {
            return namePattern.matcher(fileName).matches();
        }
    }

    private static class IOExceptionLoader implements DataFileLoader {
        private Pattern namePattern;
        public IOExceptionLoader(String pattern) {
            namePattern = Pattern.compile(pattern);
        }
        public void loadData(InputStream input, String name) throws IOException {
            if (name.equals("UTC-TAI.history")) {
                throw new IOException("dummy error");
            }
        }
        public boolean fileIsSupported(String fileName) {
            return namePattern.matcher(fileName).matches();
        }
    }

    private static class ParseExceptionLoader implements DataFileLoader {
        private Pattern namePattern;
        public ParseExceptionLoader(String pattern) {
            namePattern = Pattern.compile(pattern);
        }
        public void loadData(InputStream input, String name) throws ParseException {
            if (name.equals("UTC-TAI.history")) {
                throw new ParseException("dummy error", 0);
            }
        }
        public boolean fileIsSupported(String fileName) {
            return namePattern.matcher(fileName).matches();
        }
    }

    public static Test suite() {
        return new TestSuite(DataDirectoryCrawlerTest.class);
    }

}
