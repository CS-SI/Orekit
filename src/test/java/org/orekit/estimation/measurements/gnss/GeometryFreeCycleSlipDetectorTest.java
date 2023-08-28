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
package org.orekit.estimation.measurements.gnss;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataFilter;
import org.orekit.data.DataSource;
import org.orekit.data.GzipFilter;
import org.orekit.data.UnixCompressFilter;
import org.orekit.files.rinex.HatanakaCompressFilter;
import org.orekit.files.rinex.observation.ObservationDataSet;
import org.orekit.files.rinex.observation.RinexObservationParser;
import org.orekit.gnss.Frequency;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class GeometryFreeCycleSlipDetectorTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testTheBasicData() throws URISyntaxException, IOException {

        final String inputPath = GeometryFreeCycleSlipDetectorTest.class.getClassLoader().getResource("gnss/cycleSlip/shld0440.16d.Z").toURI().getPath();
        final File input  = new File(inputPath);
        String fileName = "shld0440.16d.Z";
        DataSource nd = new DataSource(fileName,
                                     () -> new FileInputStream(new File(input.getParentFile(), fileName)));
        for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                     new UnixCompressFilter(),
                                                     new HatanakaCompressFilter())) {
            nd = filter.filter(nd);
        }
        final RinexObservationParser parser = new RinexObservationParser();
        //RinexLoader  loader = loadCompressed("cycleSlip/shld0440.16d.Z");
        final List<ObservationDataSet> obserDataSets = parser.parse(nd).getObservationDataSets();
        GeometryFreeCycleSlipDetector slipDetectors =
            new GeometryFreeCycleSlipDetector(31, 31.0, 10);
        final List<CycleSlipDetectorResults> results = slipDetectors.detect(obserDataSets);
        for(CycleSlipDetectorResults d: results) {
            switch(getPrn(d)) {

                case 1:
                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  5,  0,  0.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,8 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;
                case 5:
                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  2, 48, 30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,8 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;

                case 6:
                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016, 2, 13,  5,  0,  0.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  4, 15,  0.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;

                case 7:
                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  4, 27,  30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,8 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;

                case 9:
                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016, 2, 13,  2, 45,  30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,8 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;

                case 11:
                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  5,  0,  0.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,8 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;

                default:   break;
            }
        }
    }

    @Test
    public void testTimeCycleSlip() throws URISyntaxException, IOException {
        final String inputPath = GeometryFreeCycleSlipDetectorTest.class.getClassLoader().getResource("gnss/cycleSlip/WithCycleSlip.16o").toURI().getPath();
        final File input  = new File(inputPath);
        String fileName = "WithCycleSlip.16o";
        DataSource nd = new DataSource(fileName,
                                     () -> new FileInputStream(new File(input.getParentFile(), fileName)));
        for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                     new UnixCompressFilter(),
                                                     new HatanakaCompressFilter())) {
            nd = filter.filter(nd);
        }
        final RinexObservationParser parser = new RinexObservationParser();
        final List<ObservationDataSet>  obserDataSets = parser.parse(nd).getObservationDataSets();
        //With dt = 31 s, cycle slip should not exist, a very huge threshold is used to not detect cycle-slip
        GeometryFreeCycleSlipDetector slipDetectors =
            new GeometryFreeCycleSlipDetector(31, 31.0, 10);
        final List<CycleSlipDetectorResults> results = slipDetectors.detect(obserDataSets);
        for(CycleSlipDetectorResults d: results) {
            Assertions.assertFalse(d.getCycleSlipMap().get(Frequency.G01).isEmpty());
        }
        //With dt = 29 s, a cycle-slip should occur at each new measurement (97 times)
        GeometryFreeCycleSlipDetector slipDetectors2 =
                        new GeometryFreeCycleSlipDetector(29, 29.0, 10);
        final List<CycleSlipDetectorResults> results2 = slipDetectors2.detect(obserDataSets);
        for(CycleSlipDetectorResults d: results2) {
            Assertions.assertTrue(d.getCycleSlipMap().get(Frequency.G01).size() == 97);
        }
    }

    @Test
    public void testCycleSlip() throws URISyntaxException, IOException {
        final String inputPath = GeometryFreeCycleSlipDetectorTest.class.getClassLoader().getResource("gnss/cycleSlip/WithCycleSlip.16o").toURI().getPath();
        final File input  = new File(inputPath);
        String fileName = "WithCycleSlip.16o";
        DataSource nd = new DataSource(fileName,
                                     () -> new FileInputStream(new File(input.getParentFile(), fileName)));
        for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                     new UnixCompressFilter(),
                                                     new HatanakaCompressFilter())) {
            nd = filter.filter(nd);
        }
        final RinexObservationParser parser = new RinexObservationParser();
        final List<ObservationDataSet> obserDataSets = parser.parse(nd).getObservationDataSets();
        //With dt = 31 s, cycle slip for time gap cannot be detected (see previous test).
        //We use T0 = 60s for threshold time constant as advice from Navipedia page.
        GeometryFreeCycleSlipDetector slipDetectors =
            new GeometryFreeCycleSlipDetector(31, 31.0, 9);
        final List<CycleSlipDetectorResults> results = slipDetectors.detect(obserDataSets);
        //According to excel graph, cycle-slip occur at 1 h 59m 43s
        AbsoluteDate trueDate = new AbsoluteDate(2016, 02, 13, 1, 59, 43, TimeScalesFactory.getUTC());
        final int size = results.get(0).getCycleSlipMap().get(Frequency.G01).size();
        Assertions.assertEquals(1, size);
        final AbsoluteDate computedDate = results.get(0).getCycleSlipMap().get(Frequency.G01).get(0);
        Assertions.assertEquals(0.0, trueDate.durationFrom(computedDate),  1e-9);
   }

    private int getPrn(final CycleSlipDetectorResults d) {

        if(d.getSatelliteName().substring(6).compareTo("1")==0) {return 1;};
        if(d.getSatelliteName().substring(6).compareTo("2")==0) {return 2;};
        if(d.getSatelliteName().substring(6).compareTo("3")==0) {return 3;};
        if(d.getSatelliteName().substring(6).compareTo("4")==0) {return 4;};
        if(d.getSatelliteName().substring(6).compareTo("5")==0) {return 5;};
        if(d.getSatelliteName().substring(6).compareTo("6")==0) {return 6;};
        if(d.getSatelliteName().substring(6).compareTo("7")==0) {return 7;};
        if(d.getSatelliteName().substring(6).compareTo("8")==0) {return 8;};
        if(d.getSatelliteName().substring(6).compareTo("9")==0) {return 9;};
        if(d.getSatelliteName().substring(6).compareTo("10")==0) {return 10;};
        if(d.getSatelliteName().substring(6).compareTo("11")==0) {return 11;};
        if(d.getSatelliteName().substring(6).compareTo("12")==0) {return 12;};
        if(d.getSatelliteName().substring(6).compareTo("13")==0) {return 13;};
        if(d.getSatelliteName().substring(6).compareTo("14")==0) {return 14;};
        if(d.getSatelliteName().substring(6).compareTo("15")==0) {return 15;};
        if(d.getSatelliteName().substring(6).compareTo("16")==0) {return 16;};
        if(d.getSatelliteName().substring(6).compareTo("17")==0) {return 17;};
        if(d.getSatelliteName().substring(6).compareTo("18")==0) {return 18;};
        if(d.getSatelliteName().substring(6).compareTo("19")==0) {return 19;};
        if(d.getSatelliteName().substring(6).compareTo("20")==0) {return 20;};
        if(d.getSatelliteName().substring(6).compareTo("21")==0) {return 21;};
        if(d.getSatelliteName().substring(6).compareTo("22")==0) {return 22;};
        if(d.getSatelliteName().substring(6).compareTo("23")==0) {return 23;};
        if(d.getSatelliteName().substring(6).compareTo("24")==0) {return 24;};
        if(d.getSatelliteName().substring(6).compareTo("25")==0) {return 25;};
        if(d.getSatelliteName().substring(6).compareTo("26")==0) {return 26;};
        if(d.getSatelliteName().substring(6).compareTo("27")==0) {return 27;};
        if(d.getSatelliteName().substring(6).compareTo("28")==0) {return 28;};
        if(d.getSatelliteName().substring(6).compareTo("29")==0) {return 29;};
        if(d.getSatelliteName().substring(6).compareTo("30")==0) {return 30;};
        if(d.getSatelliteName().substring(6).compareTo("31")==0) {return 31;} else {return 32;}

    }

}
