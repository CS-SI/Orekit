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

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.orekit.errors.OrekitException;

public class DataDirectoryCrawlerTest extends TestCase {

    public void testNoDirectoryFS() {
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_FS, "inexistant-directory");
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_CP, "");
        checkFailure();
    }

    public void testNoDirectoryCP() {
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_FS, "");
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_CP, "inexistant-directory");
        checkFailure();
    }

    public void testNotADirectoryFS() {
        URL url =
            DataDirectoryCrawlerTest.class.getClassLoader().getResource("regular-data/UTC-TAI.history");
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_FS, url.getPath());
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_CP, "");
        checkFailure();
    }

    public void testNotADirectoryCP() {
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_FS, "");
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_CP, "regular-data/UTC-TAI.history");
        checkFailure();
    }

    public void testNominalFS() throws OrekitException {
        URL url =
            DataDirectoryCrawlerTest.class.getClassLoader().getResource("regular-data");
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_FS, url.getPath());
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_CP, "");
        CountingCrawler crawler = new CountingCrawler(".*");
        new DataDirectoryCrawler().crawl(crawler);
        assertTrue(crawler.getCount() > 0);
    }

    public void testNominalCP() throws OrekitException {
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_FS, "");
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_CP, "regular-data");
        CountingCrawler crawler = new CountingCrawler(".*");
        new DataDirectoryCrawler().crawl(crawler);
        assertTrue(crawler.getCount() > 0);
    }

    public void testIOException() throws OrekitException {
        URL url =
            DataDirectoryCrawlerTest.class.getClassLoader().getResource("regular-data");
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_FS, url.getPath());
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_CP, "");
        try {
            new DataDirectoryCrawler().crawl(new DataFileCrawler(".*") {
                protected void visit(BufferedReader reader) throws IOException {
                    if (getFile().getName().equals("UTC-TAI.history")) {
                        throw new IOException("dummy error");
                    }
                }
            });
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
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_FS, url.getPath());
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_CP, "");
        try {
            new DataDirectoryCrawler().crawl(new DataFileCrawler(".*") {
                protected void visit(BufferedReader reader) throws ParseException {
                    if (getFile().getName().equals("UTC-TAI.history")) {
                        throw new ParseException("dummy error", 0);
                    }
                }
            });
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

    private void checkFailure() {
        try {
            new DataDirectoryCrawler().crawl(new CountingCrawler(".*"));
            fail("an exception should have been thrown");
        } catch (OrekitException e) {
            // expected behavior
        } catch (Exception e) {
            e.printStackTrace();
            fail("wrong exception caught");
        }
    }

    private static class CountingCrawler extends DataFileCrawler {
        private int count;
        public CountingCrawler(String pattern) {
            super(pattern);
            count = 0;
        }
        protected void visit(BufferedReader reader) {
            ++count;
        }
        public int getCount() {
            return count;
        }
    }

    public static Test suite() {
        return new TestSuite(DataDirectoryCrawlerTest.class);
    }

}
