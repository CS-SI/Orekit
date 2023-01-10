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
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

public class NetworkCrawlerTest extends AbstractListCrawlerTest<URL> {

    protected URL input(String resource) {
        return NetworkCrawlerTest.class.getClassLoader().getResource(resource);
    }

    protected NetworkCrawler build(String... inputs) {
        URL[] converted = new URL[inputs.length];
        for (int i = 0; i < inputs.length; ++i) {
            converted[i] = input(inputs[i]);
        }
        final NetworkCrawler nc = new NetworkCrawler(converted);
        nc.setTimeout(20);
        return nc;
    }

    @Test
    public void noElement() throws MalformedURLException {
        try {
            File existing   = new File(input("regular-data").getPath());
            File inexistent = new File(existing.getParent(), "inexistant-directory");
            new NetworkCrawler(inexistent.toURI().toURL()).feed(Pattern.compile(".*"), new CountingLoader(),
                                                                DataContext.getDefault().getDataProvidersManager());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertTrue(oe.getCause() instanceof FileNotFoundException);
            Assertions.assertTrue(oe.getLocalizedMessage().contains("inexistant-directory"));
        }
    }

    // WARNING!
    // the following test is commented out by default, as it does connect to the web
    // if you want to enable it, you will have uncomment it and to either set the proxy
    // settings according to your local network or remove the proxy authentication
    // settings if you have a transparent connection to internet
//    @Test
//    public void remote() throws java.net.MalformedURLException {
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
//        Assertions.assertEquals(1, loader.getCount());
//
//    }

}
