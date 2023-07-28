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

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.exception.DummyLocalizable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;

public class DataProvidersManagerTest {

    @AfterEach
    public void tearDown() {
        // clear the filters so they don't change other tests
        DataContext.getDefault().getDataProvidersManager().resetFiltersToDefault();
    }

    @Test
    public void testDefaultConfiguration() {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, getPath("regular-data"));
        CountingLoader crawler = new CountingLoader(false);
        DataContext.getDefault().getDataProvidersManager().clearProviders();
        Assertions.assertFalse(DataContext.getDefault().getDataProvidersManager().isSupported(new DirectoryCrawler(new File(getPath("regular-data")))));
        Assertions.assertTrue(DataContext.getDefault().getDataProvidersManager().feed(".*", crawler));
        Assertions.assertEquals(20, crawler.getCount());
    }

    @Test
    public void testLoadMonitoring() {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, getPath("regular-data"));
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.clearProviders();
        manager.clearLoadedDataNames();
        Assertions.assertFalse(manager.isSupported(new DirectoryCrawler(new File(getPath("regular-data")))));
        Assertions.assertEquals(0, manager.getLoadedDataNames().size());
        CountingLoader tleCounter = new CountingLoader(false);
        Assertions.assertFalse(manager.feed(".*\\.tle$", tleCounter));
        Assertions.assertEquals(0, tleCounter.getCount());
        Assertions.assertEquals(0, manager.getLoadedDataNames().size());
        CountingLoader txtCounter = new CountingLoader(false);
        Assertions.assertTrue(manager.feed(".*\\.txt$", txtCounter));
        Assertions.assertEquals(6, txtCounter.getCount());
        Assertions.assertEquals(6, manager.getLoadedDataNames().size());
        CountingLoader de405Counter = new CountingLoader(false);
        Assertions.assertTrue(manager.feed(".*\\.405$", de405Counter));
        Assertions.assertEquals(4, de405Counter.getCount());
        Assertions.assertEquals(10, manager.getLoadedDataNames().size());
        manager.clearLoadedDataNames();
        Assertions.assertEquals(0, manager.getLoadedDataNames().size());
    }

    @Test
    public void testLoadFailure() {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, getPath("regular-data"));
        DataContext.getDefault().getDataProvidersManager().clearProviders();
        CountingLoader crawler = new CountingLoader(true);
        try {
            DataContext.getDefault().getDataProvidersManager().feed(".*", crawler);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
        }
        Assertions.assertEquals(20, crawler.getCount());
    }

    @Test
    public void testEmptyProperty() {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, "");
        CountingLoader crawler = new CountingLoader(false);
        DataContext.getDefault().getDataProvidersManager().clearProviders();
        DataContext.getDefault().getDataProvidersManager().feed(".*", crawler);
        Assertions.assertEquals(0, crawler.getCount());
    }

    @Test
    public void testInexistentDirectory() {
        Assertions.assertThrows(OrekitException.class, () -> {
            File inexistent = new File(getPath("regular-data"), "inexistent");
            System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, inexistent.getAbsolutePath());
            CountingLoader crawler = new CountingLoader(false);
            DataContext.getDefault().getDataProvidersManager().clearProviders();
            DataContext.getDefault().getDataProvidersManager().feed(".*", crawler);
        });
    }

    @Test
    public void testInexistentZipArchive() {
        Assertions.assertThrows(OrekitException.class, () -> {
            File inexistent = new File(getPath("regular-data"), "inexistent.zip");
            System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, inexistent.getAbsolutePath());
            CountingLoader crawler = new CountingLoader(false);
            DataContext.getDefault().getDataProvidersManager().clearProviders();
            DataContext.getDefault().getDataProvidersManager().feed(".*", crawler);
        });
    }

    @Test
    public void testNeitherDirectoryNorZip() {
        Assertions.assertThrows(OrekitException.class, () -> {
            System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, getPath("regular-data/UTC-TAI.history"));
            CountingLoader crawler = new CountingLoader(false);
            DataContext.getDefault().getDataProvidersManager().clearProviders();
            DataContext.getDefault().getDataProvidersManager().feed(".*", crawler);
        });
    }

    @Test
    public void testListModification() {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, getPath("regular-data"));
        CountingLoader crawler = new CountingLoader(false);
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.clearProviders();
        Assertions.assertFalse(manager.isSupported(new DirectoryCrawler(new File(getPath("regular-data")))));
        Assertions.assertTrue(manager.feed(".*", crawler));
        Assertions.assertTrue(crawler.getCount() > 0);
        List<DataProvider> providers = manager.getProviders();
        Assertions.assertEquals(1, providers.size());
        for (DataProvider provider : providers) {
            Assertions.assertTrue(manager.isSupported(provider));
        }
        Assertions.assertNotNull(manager.removeProvider(providers.get(0)));
        Assertions.assertEquals(0, manager.getProviders().size());
        DataProvider provider = new DataProvider() {
            public boolean feed(Pattern supported, DataLoader visitor, DataProvidersManager manager) {
                return true;
            }
        };
        manager.addProvider(provider);
        Assertions.assertEquals(1, manager.getProviders().size());
        manager.addProvider(provider);
        Assertions.assertEquals(2, manager.getProviders().size());
        Assertions.assertNotNull(manager.removeProvider(provider));
        Assertions.assertEquals(1, manager.getProviders().size());
        Assertions.assertNull(manager.removeProvider(new DataProvider() {
            public boolean feed(Pattern supported, DataLoader visitor, DataProvidersManager manager) {
                throw new OrekitException(new DummyLocalizable("oops!"));
            }
        }));
        Assertions.assertEquals(1, manager.getProviders().size());
        Assertions.assertNotNull(manager.removeProvider(manager.getProviders().get(0)));
        Assertions.assertEquals(0, manager.getProviders().size());
    }

    @Test
    public void testComplexPropertySetting() {
        String sep = System.getProperty("path.separator");
        File top = new File(getPath("regular-data"));
        File dir1 = new File(top, "de405-ephemerides");
        File dir2 = new File(new File(top, "Earth-orientation-parameters"), "monthly");
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH,
                           dir1 + sep + sep + sep + sep + dir2);
        DataContext.getDefault().getDataProvidersManager().clearProviders();

        CountingLoader crawler = new CountingLoader(false);
        Assertions.assertTrue(DataContext.getDefault().getDataProvidersManager().feed(".*\\.405$", crawler));
        Assertions.assertEquals(4, crawler.getCount());

        crawler = new CountingLoader(false);
        Assertions.assertTrue(DataContext.getDefault().getDataProvidersManager().feed(".*\\.txt$", crawler));
        Assertions.assertEquals(1, crawler.getCount());

        crawler = new CountingLoader(false);
        Assertions.assertTrue(DataContext.getDefault().getDataProvidersManager().feed("bulletinb_.*\\.txt$", crawler));
        Assertions.assertEquals(2, crawler.getCount());

    }

    @Test
    public void testMultiZip() {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, getPath("zipped-data/multizip.zip"));
        CountingLoader crawler = new CountingLoader(false);
        DataContext.getDefault().getDataProvidersManager().clearProviders();
        Assertions.assertTrue(DataContext.getDefault().getDataProvidersManager().feed(".*\\.txt$", crawler));
        Assertions.assertEquals(6, crawler.getCount());
    }

    @Test
    public void testSimpleFilter() {
        Utils.setDataRoot("regular-data");
        CountingFilter filter = new CountingFilter();
        DataContext.getDefault().getDataProvidersManager().getFiltersManager().addFilter(filter);
        CountingLoader crawler = new CountingLoader(false);
        Assertions.assertTrue(DataContext.getDefault().getDataProvidersManager().feed(".*", crawler));
        Assertions.assertEquals(20, crawler.getCount());
        Assertions.assertEquals(20, filter.getFilteredCount());
        Assertions.assertEquals(20, filter.getOpenedCount());
    }

    @Test
    public void testMultiLayerFilter() {
        Utils.setDataRoot("regular-data");
        final int layers = 10;
        MultiLayerFilter filter = new MultiLayerFilter(layers);
        DataContext.getDefault().getDataProvidersManager().getFiltersManager().addFilter(filter);
        CountingLoader crawler = new CountingLoader(false);
        Assertions.assertTrue(DataContext.getDefault().getDataProvidersManager().feed(".*", crawler));
        Assertions.assertEquals(20, crawler.getCount());
        Assertions.assertEquals(20 * layers, filter.getOpenedCount());
    }

    private static class CountingLoader implements DataLoader {
        private boolean shouldFail;
        private int count;
        public CountingLoader(boolean shouldFail) {
            this.shouldFail = shouldFail;
            count = 0;
        }
        public boolean stillAcceptsData() {
            return true;
        }
        public void loadData(InputStream input, String name)
            {
            ++count;
            if (shouldFail) {
                throw new OrekitException(new DummyLocalizable("intentional failure"));
            }
        }
        public int getCount() {
            return count;
        }
    }

    private static class CountingFilter implements DataFilter {
        private Map<DataSource, DataSource> filtered;
        private int opened;
        public CountingFilter() {
            filtered = new IdentityHashMap<>();
            opened   = 0;
        }
        public DataSource filter(DataSource original) {
            if (filtered.containsKey(original)) {
                return original;
            } else {
                DataSource f = new DataSource(original.getName(),
                                            () -> {
                                                ++opened;
                                                return original.getOpener().openStreamOnce();
                                            });
                filtered.put(f, f);
                return f;
            }
        }
        public int getFilteredCount() {
            return filtered.size();
        }
        public int getOpenedCount() {
            return opened;
        }
    }

    private static class MultiLayerFilter implements DataFilter {
        private static final String  PREFIX  = "multilayer-";
        private static final Pattern PATTERN = Pattern.compile(PREFIX + "(\\d+)-(.*)");
        private final int layers;
        private int opened;
        public MultiLayerFilter(final int layers) {
            this.layers = layers;
            this.opened = 0;
        }
        public DataSource filter(final DataSource original) {
            Matcher matcher = PATTERN.matcher(original.getName());
            int level = 0;
            String baseName = original.getName();
            if (matcher.matches()) {
                level = Integer.parseInt(matcher.group(1));
                baseName = matcher.group(2);
            }
            if (level++ < layers) {
                // add one filtering layer
                return new DataSource(PREFIX + level + "-" + baseName,
                                     () -> {
                                         ++opened;
                                         return original.getOpener().openStreamOnce();
                                     });
            } else {
                // final layer, don't filter anymore
                return original;
            }
        }
        public int getOpenedCount() {
            return opened;
        }
    }

    private String getPath(String resourceName) {
        try {
            ClassLoader loader = DirectoryCrawlerTest.class.getClassLoader();
            return loader.getResource(resourceName).toURI().getPath();
        } catch (URISyntaxException e) {
            Assertions.fail(e.getLocalizedMessage());
            return null;
        }
    }

}
