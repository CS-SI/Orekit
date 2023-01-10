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
package org.orekit.gnss;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.gnss.GNSSPropagatorBuilder;
import org.orekit.propagation.analytical.gnss.data.GPSAlmanac;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.ElevationMask;
import org.orekit.utils.IERSConventions;

import java.util.ArrayList;
import java.util.List;


public class DOPComputerTest {

    private OneAxisEllipsoid earth;
    private GeodeticPoint location;
    private TimeScale     utc;

    @BeforeEach
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("gnss");
        // Defines the Earth shape
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Defines the location where to compute the DOP
        location = new GeodeticPoint(FastMath.toRadians(43.6), FastMath.toRadians(1.45), 0.);
        utc = TimeScalesFactory.getUTC();
    }

    @AfterEach
    public void tearDown() {
        earth    = null;
        location = null;
        utc      = null;
    }

    @Test
    public void testBasicCompute() {

        // Creates the computer
        final DOPComputer computer = DOPComputer.create(earth, location);
        Assertions.assertEquals(DOPComputer.DOP_MIN_ELEVATION, computer.getMinElevation(), 0.);
        Assertions.assertNull(computer.getElevationMask());

        // Defines the computation date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 31, 2, 0, 0., utc);

        // Computes the DOP with all the SV from the GPS constellation
        final DOP dop = computer.compute(date, getGpsPropagators());

        // Checks: expected values come from Trimble Planning software
        Assertions.assertEquals(11, dop.getGnssNb());
        Assertions.assertEquals(location, dop.getLocation());
        Assertions.assertEquals(date, dop.getDate());
        Assertions.assertEquals(1.53, dop.getGdop(), 0.01);
        Assertions.assertEquals(0.71, dop.getTdop(), 0.01);
        Assertions.assertEquals(1.35, dop.getPdop(), 0.01);
        Assertions.assertEquals(0.84, dop.getHdop(), 0.01);
        Assertions.assertEquals(1.06, dop.getVdop(), 0.01);
    }

    @Test
    public void testComputeWithMinElevation() {

        // Creates the computer
        final DOPComputer computer = DOPComputer.create(earth, location)
                                     .withMinElevation(FastMath.toRadians(10.));
        Assertions.assertEquals(FastMath.toRadians(10.), computer.getMinElevation(), 0.);
        Assertions.assertNull(computer.getElevationMask());

        // Defines the computation date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 31, 13, 0, 0., utc);

        // Computes the DOP with all the SV from the GPS constellation
        final DOP dop = computer.compute(date, getGpsPropagators());

        // Checks: expected values come from Trimble Planning software
        Assertions.assertEquals(10, dop.getGnssNb());
        Assertions.assertEquals(location, dop.getLocation());
        Assertions.assertEquals(date, dop.getDate());
        Assertions.assertEquals(1.94, dop.getGdop(), 0.01);
        Assertions.assertEquals(0.89, dop.getTdop(), 0.01);
        Assertions.assertEquals(1.72, dop.getPdop(), 0.01);
        Assertions.assertEquals(0.82, dop.getHdop(), 0.01);
        Assertions.assertEquals(1.51, dop.getVdop(), 0.01);
    }

    @Test
    public void testComputeWithElevationMask() {

        // Creates the computer
        final DOPComputer computer = DOPComputer.create(earth, location).withElevationMask(getMask());
        Assertions.assertEquals(DOPComputer.DOP_MIN_ELEVATION, computer.getMinElevation(), 0.);
        Assertions.assertNotNull(computer.getElevationMask());

        // Defines the computation date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 31, 7, 0, 0., utc);

        // Computes the DOP with all the SV from the GPS constellation
        final DOP dop = computer.compute(date, getGpsPropagators());

        // Checks: expected values come from Trimble Planning software
        Assertions.assertEquals(6, dop.getGnssNb());
        Assertions.assertEquals(location, dop.getLocation());
        Assertions.assertEquals(date, dop.getDate());
        Assertions.assertEquals(3.26, dop.getGdop(), 0.01);
        Assertions.assertEquals(1.79, dop.getTdop(), 0.01);
        Assertions.assertEquals(2.72, dop.getPdop(), 0.01);
        Assertions.assertEquals(1.29, dop.getHdop(), 0.01);
        Assertions.assertEquals(2.40, dop.getVdop(), 0.01);
    }

    @Test
    public void testNoDOPComputed() {

        // Creates the computer
        final DOPComputer computer = DOPComputer.create(earth, location).withElevationMask(getMask());
        Assertions.assertEquals(DOPComputer.DOP_MIN_ELEVATION, computer.getMinElevation(), 0.);
        Assertions.assertNotNull(computer.getElevationMask());

        // Defines the computation date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 31, 10, 0, 0., utc);

        // Computes the DOP with all the SV from the GPS constellation
        final DOP dop = computer.compute(date, getGpsPropagators());

        // Checks: comparison is made with results from Trimble Planning software
        Assertions.assertEquals(3, dop.getGnssNb());
        Assertions.assertEquals(location, dop.getLocation());
        Assertions.assertEquals(date, dop.getDate());
        Assertions.assertTrue(Double.isNaN(dop.getGdop()));
        Assertions.assertTrue(Double.isNaN(dop.getHdop()));
        Assertions.assertTrue(Double.isNaN(dop.getPdop()));
        Assertions.assertTrue(Double.isNaN(dop.getTdop()));
        Assertions.assertTrue(Double.isNaN(dop.getVdop()));
    }

    @Test
    public void testComputeFromTLE() {

        // Creates the computer
        final DOPComputer computer = DOPComputer.create(earth, location);
        Assertions.assertEquals(DOPComputer.DOP_MIN_ELEVATION, computer.getMinElevation(), 0.);
        Assertions.assertNull(computer.getElevationMask());

        // Defines the computation date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 27, 12, 0, 0., utc);

        // Computes the DOP with all the SV from the GPS constellation
        final DOP dop = computer.compute(date, getTlePropagators());

        // Checks
        Assertions.assertEquals(11, dop.getGnssNb());
        Assertions.assertEquals(location, dop.getLocation());
        Assertions.assertEquals(date, dop.getDate());
        Assertions.assertEquals(1.40, dop.getGdop(), 0.01);
        Assertions.assertEquals(0.81, dop.getHdop(), 0.01);
        Assertions.assertEquals(1.28, dop.getPdop(), 0.01);
        Assertions.assertEquals(0.56, dop.getTdop(), 0.01);
        Assertions.assertEquals(1.00, dop.getVdop(), 0.01);
    }

    @Test
    public void testNotEnoughSV() {
        Assertions.assertThrows(OrekitException.class, () -> {

            // Get the TLEs for 3 SV from the GPS constellation ...
            List<Propagator> gps = new ArrayList<Propagator>();
            gps.add(TLEPropagator.selectExtrapolator(new TLE("1 24876U 97035A   16084.84459975 -.00000010  00000-0  00000-0 0  9993",
                    "2 24876  55.6874 244.8168 0043829 115.0986 245.3138  2.00562757137015")));
            gps.add(TLEPropagator.selectExtrapolator(new TLE("1 25933U 99055A   16085.52437157 -.00000002  00000-0  00000+0 0  9996",
                    "2 25933  51.3408  97.3364 0160004  86.4308 330.0871  2.00564826120645")));
            gps.add(TLEPropagator.selectExtrapolator(new TLE("1 26360U 00025A   16085.38640501  .00000003  00000-0  00000+0 0  9993",
                    "2 26360  53.0703 173.6078 0045232  76.6879 342.6592  2.00558759116334")));

            // Creates the computer
            final DOPComputer computer = DOPComputer.create(earth, location);

            // Defines the computation date
            final AbsoluteDate date = new AbsoluteDate(2016, 3, 27, 12, 0, 0., utc);
            // Computes the DOP with all the SV from the GPS constellation
            computer.compute(date, gps);
        });
    }

    private List<Propagator> getGpsPropagators() {
        // Gets the GPS almanacs from the Yuma file
        final YUMAParser reader = new YUMAParser(null);
        reader.loadData();
        final List<GPSAlmanac> almanacs = reader.getAlmanacs();

        // Creates the GPS propagators from the almanacs
        final List<Propagator> propagators = new ArrayList<Propagator>();
        for (GPSAlmanac almanac: almanacs) {
            propagators.add(new GNSSPropagatorBuilder(almanac).build());
        }
        return propagators;
    }

    private List<Propagator> getTlePropagators() {

        List<Propagator> propagators = new ArrayList<Propagator>();

        // the following map corresponds to the GPS constellation status in early 2016
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 37753U 11036A   16059.51505483 -.00000016  00000-0  00000+0 0  9995",
                                                                 "2 37753  55.2230 119.7200 0049958  23.9363 306.3749  2.00566105 33828")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 28474U 04045A   16059.68518942 -.00000018 +00000-0 +00000-0 0  9992",
                                                                 "2 28474 054.0048 117.2153 0156693 236.8152 092.4773 02.00556694082981")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 40294U 14068A   16059.64183862 +.00000012 +00000-0 +00000-0 0  9990",
                                                                 "2 40294 054.9632 179.3690 0003634 223.4011 136.5596 02.00553679009768")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 34661U 09014A   16059.51658765 -.00000072  00000-0  00000+0 0  9997",
                                                                 "2 34661  56.3471   0.6400 0080177  48.0430 129.2467  2.00572451 50844")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 35752U 09043A   16059.39819238  .00000011  00000-0  00000+0 0  9993",
                                                                 "2 35752  54.2243 178.7652 0044753  24.8598  29.4422  2.00555538 47893")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 39741U 14026A   16059.18044747 -.00000016  00000-0  00000+0 0  9990",
                                                                 "2 39741  55.2140 119.2493 0005660 259.2190 100.7882  2.00566982 13061")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 32711U 08012A   16059.36304856 -.00000033 +00000-0 +00000-0 0  9998",
                                                                 "2 32711 055.4269 300.7399 0091867 207.6311 151.9340 02.00564257058321")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 40730U 15033A   16059.44106931 -.00000026 +00000-0 +00000-0 0  9994",
                                                                 "2 40730 055.1388 059.0069 0020452 282.1769 077.6168 02.00566073004562")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 40105U 14045A   16059.27451329  .00000045  00000-0  00000+0 0  9996",
                                                                 "2 40105  54.7529 238.9873 0004485 121.4766 238.5557  2.00568637 11512")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 41019U 15062A   16059.49433942  .00000013  00000-0  00000+0 0  9991",
                                                                 "2 41019  54.9785 179.1399 0012328 204.9013 155.0292  2.00561967  2382")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 25933U 99055A   16059.51073770 -.00000024  00000-0  00000+0 0  9997",
                                                                 "2 25933  51.3239  98.4815 0159812  86.1576 266.7718  2.00565163120122")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 29601U 06052A   16059.62966898 -.00000070 +00000-0 +00000-0 0  9994",
                                                                 "2 29601 056.7445 002.2755 0057667 037.0706 323.3313 02.00552237067968")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 24876U 97035A   16059.41696335  .00000046  00000-0  00000+0 0  9998",
                                                                 "2 24876  55.6966 245.8203 0044339 114.8899 245.5712  2.00562657136305")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 26605U 00071A   16059.56211888  .00000047  00000-0  00000+0 0  9997",
                                                                 "2 26605  55.2663 243.7251 0085518 248.7231  95.5323  2.00557009112094")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 32260U 07047A   16059.45678257 +.00000044 +00000-0 +00000-0 0  9994",
                                                                 "2 32260 053.3641 236.0940 0079746 026.4105 333.9774 02.00547771061402")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 27663U 03005A   16059.14440417 -.00000071  00000-0  00000+0 0  9996",
                                                                 "2 27663  56.7743   3.3691 0085346  17.6322 214.4333  2.00559487 95843")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 28874U 05038A   16059.21070933 -.00000024  00000-0  00000+0 0  9997",
                                                                 "2 28874  55.8916  61.9596 0112077 248.9647 205.7384  2.00567116 76379")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 26690U 01004A   16059.51332910  .00000008  00000-0  00000+0 0  9990",
                                                                 "2 26690  52.9999 177.6630 0169501 250.8579 153.6293  2.00563995110499")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 28190U 04009A   16058.12363503 -.00000030  00000-0  00000+0 0  9999",
                                                                 "2 28190  55.7230  64.8110 0105865  40.0254 321.4519  2.00572212 87500")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 26360U 00025A   16059.44770263  .00000005  00000-0  00000+0 0  9992",
                                                                 "2 26360  53.0712 174.6895 0046205  76.0615 334.4302  2.00559931115818")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 27704U 03010A   16059.50719524 -.00000019  00000-0  00000+0 0  9998",
                                                                 "2 27704  53.6134 117.9454 0234081 255.6874 199.2128  2.00564673 94659")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 28129U 03058A   16059.06680941  .00000008  00000-0  00000+0 0  9990",
                                                                 "2 28129  52.8771 177.7253 0079127 245.1376 114.0279  2.00398763 89357")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 28361U 04023A   16059.54310021  .00000046  00000-0  00000+0 0  9995",
                                                                 "2 28361  54.2347 239.3240 0106509 211.5355  11.7648  2.00557932 85613")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 38833U 12053A   16059.04618549 -.00000032  00000-0  00000+0 0  9999",
                                                                 "2 38833  54.4591 298.1383 0042253  18.7074 341.5041  2.00568407 24895")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 36585U 10022A   16059.29300735 -.00000074  00000-0  00000+0 0  9993",
                                                                 "2 36585  56.0738 359.4320 0050768  38.3425  49.1794  2.00578535 42134")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 40534U 15013A   16059.28299301 -.00000076  00000-0  00000+0 0  9994",
                                                                 "2 40534  55.0430 359.0082 0009349 342.4081  17.5685  2.00558853  6801")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 39166U 13023A   16059.40401153 -.00000025  00000-0  00000+0 0  9990",
                                                                 "2 39166  55.6020  59.1224 0032420   7.7969 352.2759  2.00568484 20414")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 26407U 00040A   16059.80383354 -.00000069 +00000-0 +00000-0 0  9994",
                                                                 "2 26407 056.6988 003.6328 0201499 267.0948 317.6209 02.00569902114508")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 32384U 07062A   16059.44770263 -.00000021  00000-0  00000+0 0  9992",
                                                                 "2 32384  55.9456  62.5022 0011922 319.9531 172.6730  2.00571577 60128")));
        propagators.add(TLEPropagator.selectExtrapolator(new TLE("1 39533U 14008A   16059.40267873 -.00000038 +00000-0 +00000-0 0  9996",
                                                                 "2 39533 054.6126 303.3404 0017140 179.4267 180.6311 02.00568364014251")));
        propagators.add(TLEPropagator.selectExtrapolator( new TLE("1 29486U 06042A   16059.50651990 -.00000032  00000-0  00000+0 0  9992",
                                                                  "2 29486  55.7041 301.2472 0084115 334.2804 254.9897  2.00560606 69098")));
        propagators.add(TLEPropagator.selectExtrapolator( new TLE("1 41328U 16007A   16059.56873502  .00000049  00000-0  00000+0 0  9991",
                                                                  "2 41328  55.0137 239.0304 0002157 298.9074  61.0768  1.99172830   453")));

        return propagators;

    }

    private ElevationMask getMask() {
        final double [][] mask = {
            {FastMath.toRadians(0.),   FastMath.toRadians(5.00)},
            {FastMath.toRadians(45.),  FastMath.toRadians(50.00)},
            {FastMath.toRadians(90.),  FastMath.toRadians(5.00)},
            {FastMath.toRadians(135.), FastMath.toRadians(50.00)},
            {FastMath.toRadians(180.), FastMath.toRadians(5.00)},
            {FastMath.toRadians(225.), FastMath.toRadians(50.00)},
            {FastMath.toRadians(270.), FastMath.toRadians(5.00)},
            {FastMath.toRadians(315.), FastMath.toRadians(50.00)}
        };
        return new ElevationMask(mask);
    }
}
