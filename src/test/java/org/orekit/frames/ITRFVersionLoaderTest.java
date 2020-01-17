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
package org.orekit.frames;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Unit tests for {@link ITRFVersionLoader}.
 *
 * @author Evan Ward
 */
public class ITRFVersionLoaderTest {

    /** Check loading regular file. */
    @Test
    public void testVersion() {
        // setup
        Utils.setDataRoot("regular-data");
        ItrfVersionProvider loader =
                new ITRFVersionLoader(ITRFVersionLoader.SUPPORTED_NAMES);

        // action + verify
        Assert.assertThat(loader.getConfiguration("eopc04_05.00", 0).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2005));
        Assert.assertThat(loader.getConfiguration("eopc04_05_IAU2000.00", 0).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2005));
        Assert.assertThat(loader.getConfiguration("eopc04_08.00", 0).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2008));
        Assert.assertThat(loader.getConfiguration("eopc04_08_IAU2000.00", 0).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2008));
        Assert.assertThat(loader.getConfiguration("eopc04_14.00", 0).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2014));
        Assert.assertThat(loader.getConfiguration("eopc04_14_IAU2000.00", 0).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2014));

        Assert.assertThat(
                loader.getConfiguration("bulletina-xi-001.txt", 54000).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2000));
        Assert.assertThat(
                loader.getConfiguration("bulletina-xxi-001.txt", 55555).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2005));
        Assert.assertThat(
                loader.getConfiguration("bulletina-xxvi-001.txt", 57777).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2008));
        Assert.assertThat(
                loader.getConfiguration("bulletina-xxxi-001.txt", 58484).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2014));

        Assert.assertThat(
                loader.getConfiguration("bulletinb_IAU1980-123.txt", 0).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2005));
        Assert.assertThat(
                loader.getConfiguration("bulletinb_IAU2000-123.txt", 0).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2005));

        Assert.assertThat(
                loader.getConfiguration("bulletinb-123.txt", 55555).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2005));
        Assert.assertThat(
                loader.getConfiguration("bulletinb.123", 55555).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2005));
        Assert.assertThat(
                loader.getConfiguration("bulletinb-123.txt", 57777).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2008));
        Assert.assertThat(
                loader.getConfiguration("bulletinb.123", 57777).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2008));
        Assert.assertThat(
                loader.getConfiguration("bulletinb-123.txt", 58484).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2014));
        Assert.assertThat(
                loader.getConfiguration("bulletinb.123", 58484).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2014));

        Assert.assertThat(loader.getConfiguration("finals.all", 0).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2000));
        Assert.assertThat(loader.getConfiguration("finals2000A.all", 0).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2000));
        Assert.assertThat(loader.getConfiguration("finals.all", 55555).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2005));
        Assert.assertThat(loader.getConfiguration("finals2000A.all", 55555).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2005));
        Assert.assertThat(loader.getConfiguration("finals.all", 57777).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2008));
        Assert.assertThat(loader.getConfiguration("finals2000A.all", 57777).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2008));
        Assert.assertThat(loader.getConfiguration("finals.all", 58484).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2014));
        Assert.assertThat(loader.getConfiguration("finals2000A.all", 58484).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2014));
    }

    /** Check using file names generated by other loaders. */
    @Test
    public void testLoaders() {
        // setup
        Utils.setDataRoot("regular-data");
        ItrfVersionProvider loader =
                new ITRFVersionLoader(ITRFVersionLoader.SUPPORTED_NAMES);

        Assert.assertThat(loader.getConfiguration("/finals.all", 55555).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2005));
        Assert.assertThat(loader.getConfiguration("\\finals.all", 55555).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2005));
        Assert.assertThat(
                loader .getConfiguration(
                                "https://user@example.com:port/path/finals.all?a=b#c",
                                55555)
                        .getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2005));
        Assert.assertThat(
                loader.getConfiguration("a.zip!/finals.all", 55555).getVersion(),
                CoreMatchers.is(ITRFVersion.ITRF_2005));
    }

    /** Check that using the old format throws a helpful exception. */
    @Test
    public void testOldFormat() {
        Utils.setDataRoot("obsolete-data");
        try {
            new ITRFVersionLoader(ITRFVersionLoader.SUPPORTED_NAMES);
            Assert.fail("Expected Exception");
        } catch (OrekitException e) {
            Assert.assertEquals(e.getSpecifier(), OrekitMessages.ITRF_VERSIONS_PREFIX_ONLY);
        }
    }

}
