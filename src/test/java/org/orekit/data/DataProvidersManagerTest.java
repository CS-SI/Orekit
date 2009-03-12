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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.orekit.errors.OrekitException;

public class DataProvidersManagerTest extends TestCase {

    public void testDefaultConfiguration() throws OrekitException {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, getPath("regular-data"));
        CountingLoader crawler = new CountingLoader(".*", false);
        DataProvidersManager.getInstance().clearProviders();
        assertFalse(DataProvidersManager.getInstance().isSupported(DataDirectoryCrawler.class));
        assertTrue(DataProvidersManager.getInstance().feed(crawler));
        assertEquals(14, crawler.getCount());
    }

    public void testLoadFailure() {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, getPath("regular-data"));
        DataProvidersManager.getInstance().clearProviders();
        CountingLoader crawler = new CountingLoader(".*", true);
        try {
            DataProvidersManager.getInstance().feed(crawler);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
        }
        assertEquals(14, crawler.getCount());
    }

    public void testEmptyProperty() throws OrekitException {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, "");
        CountingLoader crawler = new CountingLoader(".*", false);
        DataProvidersManager.getInstance().clearProviders();
        DataProvidersManager.getInstance().feed(crawler);
        assertEquals(0, crawler.getCount());
    }

    public void testInexistentDirectory() throws OrekitException {
        File inexistent = new File(getPath("regular-data"), "inexistent");
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, inexistent.getAbsolutePath());
        CountingLoader crawler = new CountingLoader(".*", false);
        DataProvidersManager.getInstance().clearProviders();
        try {
            DataProvidersManager.getInstance().feed(crawler);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected behavior
        } catch (Exception e) {
            fail("wrong exception caught");
        }
    }

    public void testInexistentZipArchive() throws OrekitException {
        File inexistent = new File(getPath("regular-data"), "inexistent.zip");
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, inexistent.getAbsolutePath());
        CountingLoader crawler = new CountingLoader(".*", false);
        DataProvidersManager.getInstance().clearProviders();
        try {
            DataProvidersManager.getInstance().feed(crawler);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected behavior
        } catch (Exception e) {
            fail("wrong exception caught");
        }
    }

    public void testNeitherDirectoryNorZip() throws OrekitException {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, getPath("regular-data/UTC-TAI.history"));
        CountingLoader crawler = new CountingLoader(".*", false);
        DataProvidersManager.getInstance().clearProviders();
        try {
            DataProvidersManager.getInstance().feed(crawler);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected behavior
        } catch (Exception e) {
            fail("wrong exception caught");
        }
    }

    public void testListModification() throws OrekitException {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, getPath("regular-data"));
        CountingLoader crawler = new CountingLoader(".*", false);
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.clearProviders();
        assertFalse(manager.isSupported(DataDirectoryCrawler.class));
        assertTrue(manager.feed(crawler));
        assertTrue(crawler.getCount() > 0);
        assertTrue(manager.isSupported(DataDirectoryCrawler.class));
        assertFalse(manager.isSupported(DataClasspathCrawler.class));
        assertEquals(1, manager.getProviders().size());
        assertNotNull(manager.removeProvider(DataDirectoryCrawler.class));
        assertEquals(0, manager.getProviders().size());
        DataProvider provider = new DataProvider() {
            private static final long serialVersionUID = -5312255682914297696L;
            public boolean feed(DataFileLoader visitor) throws OrekitException {
                return true;
            }
        };
        manager.addProvider(provider);
        assertEquals(1, manager.getProviders().size());
        manager.addProvider(provider);
        assertEquals(2, manager.getProviders().size());
        assertNotNull(manager.removeProvider(provider.getClass()));
        assertEquals(1, manager.getProviders().size());
        assertNull(manager.removeProvider(new DataProvider() {
            private static final long serialVersionUID = 6368246625696570910L;
            public boolean feed(DataFileLoader visitor) throws OrekitException {
                throw new OrekitException("oops!", new Object[0]);
            }
        }.getClass()));
        assertEquals(1, manager.getProviders().size());
        assertNotNull(manager.removeProvider(provider.getClass()));
        assertEquals(0, manager.getProviders().size());
    }

    public void testComplexPropertySetting() throws OrekitException {
        String sep = System.getProperty("path.separator");
        File top = new File(getPath("regular-data"));
        File dir1 = new File(top, "de405-ephemerides");
        File dir2 = new File(new File(top, "Earth-orientation-parameters"), "monthly");
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH,
                           dir1 + sep + sep + sep + sep + dir2);
        DataProvidersManager.getInstance().clearProviders();

        CountingLoader crawler = new CountingLoader(".*\\.405$", false);
        assertTrue(DataProvidersManager.getInstance().feed(crawler));
        assertEquals(4, crawler.getCount());

        crawler = new CountingLoader(".*\\.txt$", false);
        assertTrue(DataProvidersManager.getInstance().feed(crawler));
        assertEquals(1, crawler.getCount());

        crawler = new CountingLoader("bulletinb_.*\\.txt$", false);
        assertTrue(DataProvidersManager.getInstance().feed(crawler));
        assertEquals(3, crawler.getCount());

    }

    public void testMultiZip() throws OrekitException {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, getPath("multizip.zip"));
        CountingLoader crawler = new CountingLoader(".*\\.txt$", false);
        DataProvidersManager.getInstance().clearProviders();
        assertTrue(DataProvidersManager.getInstance().feed(crawler));
        assertEquals(6, crawler.getCount());
    }

    private static class CountingLoader implements DataFileLoader {
        private Pattern pattern;
        private boolean shouldFail;
        private int count;
        public CountingLoader(String pattern, boolean shouldFail) {
            this.pattern = Pattern.compile(pattern);
            this.shouldFail = shouldFail;
            count = 0;
        }
        public void loadData(InputStream input, String name)
            throws OrekitException {
            ++count;
            if (shouldFail) {
                throw new OrekitException("intentional failure", new Object[0]);
            }
        }
        public int getCount() {
            return count;
        }
        public boolean fileIsSupported(String fileName) {
            return pattern.matcher(fileName).matches();
        }
    }

    private String getPath(String resourceName) {
        ClassLoader loader = DataDirectoryCrawlerTest.class.getClassLoader();
        URL url = loader.getResource(resourceName);
        return url.getPath();
    }

    public static Test suite() {
        return new TestSuite(DataProvidersManagerTest.class);
    }

}
