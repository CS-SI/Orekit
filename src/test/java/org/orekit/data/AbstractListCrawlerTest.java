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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.regex.Pattern;

public abstract class AbstractListCrawlerTest<T> {

    protected abstract T input(String resource);

    protected abstract AbstractListCrawler<T> build(String... inputs);

    @BeforeEach
    public void setUp() {
        // Clear any filters that another test may have left
        DataContext.getDefault().getDataProvidersManager().resetFiltersToDefault();
    }

    @Test
    public void local() {
        CountingLoader crawler = new CountingLoader();
        AbstractListCrawler<T> nc = build("regular-data/UTC-TAI.history",
                                          "regular-data/de405-ephemerides/unxp0000.405",
                                          "regular-data/de405-ephemerides/unxp0001.405",
                                          "regular-data/de406-ephemerides/unxp0000.406");
        Assertions.assertEquals(4, nc.getInputs().size());
        nc.addInput(input("regular-data/Earth-orientation-parameters/monthly/bulletinb_IAU2000-216.txt"));
        Assertions.assertEquals(5, nc.getInputs().size());
        nc.feed(Pattern.compile(".*"), crawler, DataContext.getDefault().getDataProvidersManager());
        Assertions.assertEquals(5, crawler.getCount());
    }

    @Test
    public void compressed() {
        CountingLoader crawler = new CountingLoader();
        AbstractListCrawler<T> nc = build();
        nc.addInput(input("compressed-data/UTC-TAI.history.gz"));
        nc.addInput(input("compressed-data/eopc04_08_IAU2000.00.gz"));
        nc.addInput(input("compressed-data/eopc04_08_IAU2000.02.gz"));
        nc.feed(Pattern.compile("^eopc04.*"), crawler,
                DataContext.getDefault().getDataProvidersManager());
        Assertions.assertEquals(2, crawler.getCount());
    }

    @Test
    public void multiZip() {
        CountingLoader crawler = new CountingLoader();
        build("zipped-data/multizip.zip").feed(Pattern.compile(".*\\.txt$"), crawler,
                                               DataContext.getDefault().getDataProvidersManager());
        Assertions.assertEquals(6, crawler.getCount());
    }

    @Test
    public void ioException() {
        Assertions.assertThrows(OrekitException.class, () -> {
            try {
                build("regular-data/UTC-TAI.history").feed(Pattern.compile(".*"), new IOExceptionLoader(),
                        DataContext.getDefault().getDataProvidersManager());
            } catch (OrekitException oe) {
                // expected behavior
                Assertions.assertNotNull(oe.getCause());
                Assertions.assertEquals(IOException.class, oe.getCause().getClass());
                Assertions.assertEquals("dummy error", oe.getMessage());
                throw oe;
            }
        });
    }

    @Test
    public void parseException() {
        Assertions.assertThrows(OrekitException.class, () -> {
            try {
                build("regular-data/UTC-TAI.history").feed(Pattern.compile(".*"), new ParseExceptionLoader(),
                        DataContext.getDefault().getDataProvidersManager());
            } catch (OrekitException oe) {
                // expected behavior
                Assertions.assertNotNull(oe.getCause());
                Assertions.assertEquals(ParseException.class, oe.getCause().getClass());
                Assertions.assertEquals("dummy error", oe.getMessage());
                throw oe;
            }
        });
    }

    protected static class CountingLoader implements DataLoader {
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
