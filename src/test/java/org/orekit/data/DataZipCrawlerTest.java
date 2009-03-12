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

import java.io.InputStream;
import java.net.URL;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.orekit.errors.OrekitException;

public class DataZipCrawlerTest extends TestCase {

    public void testMultiZipClasspath() throws OrekitException {
        CountingLoader crawler = new CountingLoader(".*\\.txt$");
        new DataZipCrawler("multizip.zip", DataZipCrawler.Location.CLASSPATH).feed(crawler);
        assertEquals(6, crawler.getCount());
    }

    public void testMultiZip() throws OrekitException {
        URL url =
            DataDirectoryCrawlerTest.class.getClassLoader().getResource("multizip.zip");
        CountingLoader crawler = new CountingLoader(".*\\.txt$");
        new DataZipCrawler(url.getPath(), DataZipCrawler.Location.FILE_SYSTEM).feed(crawler);
        assertEquals(6, crawler.getCount());
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

    public static Test suite() {
        return new TestSuite(DataZipCrawlerTest.class);
    }

}
