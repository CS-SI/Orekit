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

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

public class ZipJarCrawlerTest {

    @Test
    public void testMultiZipClasspath() {
        CountingLoader crawler = new CountingLoader();
        new ZipJarCrawler("zipped-data/multizip.zip").feed(Pattern.compile(".*\\.txt$"), crawler,
                                                           DataContext.getDefault().getDataProvidersManager());
        Assertions.assertEquals(6, crawler.getCount());
    }

    @Test
    public void testMultiZip() throws URISyntaxException {
        URL url =
            ZipJarCrawlerTest.class.getClassLoader().getResource("zipped-data/multizip.zip");
        CountingLoader crawler = new CountingLoader();
        new ZipJarCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*\\.txt$"), crawler,
                                                                DataContext.getDefault().getDataProvidersManager());
        Assertions.assertEquals(6, crawler.getCount());
    }

    private static class CountingLoader implements DataLoader {
        private int count = 0;
        public boolean stillAcceptsData() {
            return true;
        }
        public void loadData(InputStream input, String name) {
            ++count;
            MatcherAssert.assertThat(name, CoreMatchers.containsString("!/"));
        }
        public int getCount() {
            return count;
        }
    }

}
