/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class ZipJarCrawlerTest {

    @Test
    public void testMultiZipClasspath() {
        CountingLoader crawler = new CountingLoader();
        new ZipJarCrawler("zipped-data/multizip.zip").feed(Pattern.compile(".*\\.txt$"), crawler,
                                                           DataContext.getDefault().getDataProvidersManager());
        Assert.assertEquals(6, crawler.getCount());
    }

    @Test
    public void testMultiZip() throws URISyntaxException {
        URL url =
            ZipJarCrawlerTest.class.getClassLoader().getResource("zipped-data/multizip.zip");
        CountingLoader crawler = new CountingLoader();
        new ZipJarCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*\\.txt$"), crawler,
                                                                DataContext.getDefault().getDataProvidersManager());
        Assert.assertEquals(6, crawler.getCount());
    }

    @Deprecated
    @Test
    public void testExtraMethods() throws URISyntaxException {
        URL url =
            ZipJarCrawlerTest.class.getClassLoader().getResource("zipped-data/orekit.zip");
        new ZipJarCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*\\.txt$"),
                                                                new MarkingLoader());
    }

    private static class CountingLoader implements DataLoader {
        private int count = 0;
        public boolean stillAcceptsData() {
            return true;
        }
        public void loadData(InputStream input, String name) {
            ++count;
            Assert.assertThat(name, CoreMatchers.containsString("!/"));
        }
        public int getCount() {
            return count;
        }
    }

    private static class MarkingLoader implements DataLoader {
        public boolean stillAcceptsData() {
            return true;
        }
        public void loadData(InputStream input, String name) throws IOException {
            Assert.assertFalse(input.markSupported());
            input.mark(1); // does nothing
            try {
                input.reset();
                Assert.fail("an exception should have been thrown");
            } catch (IOException ioe) {
                // expected
            }
            input.skip(1);
            byte[] content = new byte[3];
            content[0] = 'O';
            input.read(content, 1, 2);
            Assert.assertEquals("Ore", new String(content, StandardCharsets.UTF_8));
            Assert.assertTrue(input.available() > 0);
            byte[] remaining = new byte[4];
            input.read(remaining);
            Assert.assertEquals("kit\n", new String(remaining, StandardCharsets.UTF_8));
        }
    }

}
