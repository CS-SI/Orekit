/* Copyright 2002-2009 CS Communication & Systèmes
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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.orekit.errors.OrekitException;

public class DataClasspathCrawlerTest extends TestCase {

    public void testNoElement() {
        checkFailure("inexistant-element");
    }

    public void testNominal() throws OrekitException {
        CountingLoader crawler = new CountingLoader(".*");
        new DataClasspathCrawler("regular-data/UTC-TAI.history",
                                 "regular-data/de405-ephemerides/unxp0000.405",
                                 "regular-data/de405-ephemerides/unxp0001.405",
                                 "regular-data/de406-ephemerides/unxp0000.406",
                                 "regular-data/Earth-orientation-parameters/monthly/bulletinb_IAU2000-216.txt",
                                 "no-data").feed(crawler);
        assertEquals(6, crawler.getCount());
    }

    public void testCompressed() throws OrekitException {
        CountingLoader crawler = new CountingLoader(".*/eopc04.*");
        new DataClasspathCrawler("compressed-data/UTC-TAI.history.gz",
                                 "compressed-data/eopc04_IAU2000.00.gz",
                                 "compressed-data/eopc04_IAU2000.02.gz").feed(crawler);
        assertEquals(2, crawler.getCount());
    }

    public void testMultiZip() throws OrekitException {
        CountingLoader crawler = new CountingLoader(".*\\.txt$");
        new DataClasspathCrawler("multizip.zip").feed(crawler);
        assertEquals(6, crawler.getCount());
    }

    public void testIOException() throws OrekitException {
        try {
            new DataClasspathCrawler("regular-data/UTC-TAI.history").feed(new IOExceptionLoader(".*"));
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
        try {
            new DataClasspathCrawler("regular-data/UTC-TAI.history").feed(new ParseExceptionLoader(".*"));
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

    private void checkFailure(String ...list) {
        try {
            new DataClasspathCrawler(list).feed(new CountingLoader(".*"));
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
            if (name.endsWith("UTC-TAI.history")) {
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
            if (name.endsWith("UTC-TAI.history")) {
                throw new ParseException("dummy error", 0);
            }
        }
        public boolean fileIsSupported(String fileName) {
            return namePattern.matcher(fileName).matches();
        }
    }

    public static Test suite() {
        return new TestSuite(DataClasspathCrawlerTest.class);
    }

}
