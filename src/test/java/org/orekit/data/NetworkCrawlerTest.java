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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;

public class NetworkCrawlerTest {

    @Test(expected=OrekitException.class)
    public void noElement() throws OrekitException, MalformedURLException {
        File existing   = new File(url("regular-data").getPath());
        File inexistent = new File(existing.getParent(), "inexistant-directory");
        new NetworkCrawler(inexistent.toURI().toURL()).feed(Pattern.compile(".*"), new CountingLoader());
    }

    // WARNING!
    // the following test is commented out by default, as it does connect to the web
    // if you want to enable it, you will have uncomment it and to either set the proxy
    // settings according to your local network or remove the proxy authentication
    // settings if you have a transparent connection to internet
//    @Test
//    public void remote() throws java.net.MalformedURLException, OrekitException, URISyntaxException {
//
//        System.setProperty("http.proxyHost",     "proxy.your.domain.com");
//        System.setProperty("http.proxyPort",     "8080");
//        System.setProperty("http.nonProxyHosts", "localhost|*.your.domain.com");
//        java.net.Authenticator.setDefault(new AuthenticatorDialog());
//        CountingLoader loader = new CountingLoader();
//        NetworkCrawler crawler =
//            new NetworkCrawler(new URL("http://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history"));
//        crawler.setTimeout(1000);
//        crawler.feed(Pattern.compile(".*\\.history"), loader);
//        Assert.assertEquals(1, loader.getCount());
//
//    }

    @Test
    public void local() throws OrekitException {
        CountingLoader crawler = new CountingLoader();
        NetworkCrawler nc = new NetworkCrawler(url("regular-data/UTC-TAI.history"),
                           url("regular-data/de405-ephemerides/unxp0000.405"),
                           url("regular-data/de405-ephemerides/unxp0001.405"),
                           url("regular-data/de406-ephemerides/unxp0000.406"),
                           url("regular-data/Earth-orientation-parameters/monthly/bulletinb_IAU2000-216.txt"),
                           url("no-data"));
        nc.setTimeout(20);
        nc.feed(Pattern.compile(".*"), crawler);
        Assert.assertEquals(6, crawler.getCount());
    }

    @Test
    public void compressed() throws OrekitException {
        CountingLoader crawler = new CountingLoader();
        new NetworkCrawler(url("compressed-data/UTC-TAI.history.gz"),
                           url("compressed-data/eopc04_08_IAU2000.00.gz"),
                           url("compressed-data/eopc04_08_IAU2000.02.gz")).feed(Pattern.compile("^eopc04.*"), crawler);
        Assert.assertEquals(2, crawler.getCount());
    }

    @Test
    public void multiZip() throws OrekitException {
        CountingLoader crawler = new CountingLoader();
        new NetworkCrawler(url("zipped-data/multizip.zip")).feed(Pattern.compile(".*\\.txt$"), crawler);
        Assert.assertEquals(6, crawler.getCount());
    }

    @Test(expected=OrekitException.class)
    public void ioException() throws OrekitException {
        try {
            new NetworkCrawler(url("regular-data/UTC-TAI.history")).feed(Pattern.compile(".*"), new IOExceptionLoader());
        } catch (OrekitException oe) {
            // expected behavior
            Assert.assertNotNull(oe.getCause());
            Assert.assertEquals(IOException.class, oe.getCause().getClass());
            Assert.assertEquals("dummy error", oe.getMessage());
            throw oe;
        }
    }

    @Test(expected=OrekitException.class)
    public void parseException() throws OrekitException {
        try {
            new NetworkCrawler(url("regular-data/UTC-TAI.history")).feed(Pattern.compile(".*"), new ParseExceptionLoader());
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

    private URL url(String resource) {
        return DirectoryCrawlerTest.class.getClassLoader().getResource(resource);
    }

}
