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


public class PhaseMinusCodeCycleSlipDetectorTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testTheNumberOFCycleFind() throws URISyntaxException, IOException {

        final String inputPath = GeometryFreeCycleSlipDetectorTest.class.getClassLoader().getResource("gnss/cycleSlip/seat0440.16d.Z").toURI().getPath();
        final File input  = new File(inputPath);
        String fileName = "seat0440.16d.Z";
        DataSource nd = new DataSource(fileName,
                                     () -> new FileInputStream(new File(input.getParentFile(), fileName)));
        for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                     new UnixCompressFilter(),
                                                     new HatanakaCompressFilter())) {
            nd = filter.filter(nd);
        }
        final RinexObservationParser parser = new RinexObservationParser();
        final List<ObservationDataSet> obserDataSets = parser.parse(nd).getObservationDataSets();
        PhaseMinusCodeCycleSlipDetector slipDetectors =
            new PhaseMinusCodeCycleSlipDetector(90, 10, 20, 3);
        final List<CycleSlipDetectorResults> results = slipDetectors.detect(obserDataSets);
        for(CycleSlipDetectorResults d: results) {
            switch(getPrn(d)) {
                case 1:
                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  5,  0,  0.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,33 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);

                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G02).durationFrom(new AbsoluteDate(2016,  2, 13,  5,  0,  0.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G02).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,40 ,00.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;

                case 5:
                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  2, 44, 30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,31 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);

                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G02).durationFrom(new AbsoluteDate(2016,  2, 13,  2, 44, 30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G02).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,31 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;

                case 6:
                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016, 2, 13,  5,  0,  0.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  4, 28, 30.0000000, TimeScalesFactory.getTAI())),1e-9);

                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G02).durationFrom(new AbsoluteDate(2016, 2, 13,  5,  0,  0.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G02).durationFrom(new AbsoluteDate(2016,  2, 13,  4, 31, 0.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;

                case 7:
                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  4, 13,  30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,31 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);

                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G02).durationFrom(new AbsoluteDate(2016,  2, 13,  4, 11,  00.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G02).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,31 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;

                case 9:
                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016, 2, 13,  2, 32,  00.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,31 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);

                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G02).durationFrom(new AbsoluteDate(2016, 2, 13,  2, 31,  30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G02).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,31 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;

                case 11:
                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,  2, 13,  5,  0,  0.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G01).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,31 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);

                    Assertions.assertEquals(19.0, d.getEndDate(Frequency.G02).durationFrom(new AbsoluteDate(2016,  2, 13,  5,  0,  0.0000000, TimeScalesFactory.getTAI())),1e-9);
                    Assertions.assertEquals(19.0, d.getBeginDate(Frequency.G02).durationFrom(new AbsoluteDate(2016,2, 13  ,2  ,31 ,30.0000000, TimeScalesFactory.getTAI())),1e-9);
                    break;

                default:   break;
            }
        }
    }

    //Test to verify that the cycle-slips because of data gap are computed
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
        final List<ObservationDataSet> obserDataSets = parser.parse(nd).getObservationDataSets();
        PhaseMinusCodeCycleSlipDetector slipDetectors =
            new PhaseMinusCodeCycleSlipDetector(90, 1e15, 20, 3);
        final List<CycleSlipDetectorResults> results = slipDetectors.detect(obserDataSets);
        for(CycleSlipDetectorResults d: results) {
            switch(getPrn(d)) {
                case 1:
                    //The date have been created  manually within the file
                    AbsoluteDate[] dateCycleSlipL1 = new AbsoluteDate[] {
                        new AbsoluteDate(2016,02,13,04,37,43.000 , TimeScalesFactory.getUTC()),
                        new AbsoluteDate(2016,02,13,04,45,13.000 , TimeScalesFactory.getUTC()),
                        new AbsoluteDate(2016,02,13,04,54,13.000 , TimeScalesFactory.getUTC())};
                    int i1 = 0;
                    for (AbsoluteDate dateL1: d.getCycleSlipMap().get(Frequency.G01)) {
                        Assertions.assertEquals(0,  dateL1.compareTo(dateCycleSlipL1[i1]));
                        i1++;
                    }
                    //The dates have been created manually within the file
                    AbsoluteDate[] dateCycleSlipL2 = new AbsoluteDate[] {
                        new AbsoluteDate(2016,02,13,04,38,13.000 , TimeScalesFactory.getUTC()),
                        new AbsoluteDate(2016,02,13,04,41,13.000 , TimeScalesFactory.getUTC()),
                        new AbsoluteDate(2016,02,13,04,45,43.000 , TimeScalesFactory.getUTC()),
                        new AbsoluteDate(2016,02,13,04,54,13.000 , TimeScalesFactory.getUTC())};
                    int i2 = 0;
                    for(AbsoluteDate dateL2: d.getCycleSlipMap().get(Frequency.G02)) {
                        Assertions.assertEquals(0,  dateL2.compareTo(dateCycleSlipL2[i2]));
                        i2++;
                    }
                default:    break;


            }
        }
    }

    //Test to check the detectors find the cycle slip added to data on purpose
    @Test
    public void testCycleSlipDetection() throws URISyntaxException, IOException {
        final String inputPath = GeometryFreeCycleSlipDetectorTest.class.getClassLoader().getResource("gnss/cycleSlip/WithCycleSlip.16o").toURI().getPath();
        final File input  = new File(inputPath);
        String fileName = "WithoutCycleSlip.16o";
        DataSource nd = new DataSource(fileName,
                                     () -> new FileInputStream(new File(input.getParentFile(), fileName)));
        for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                     new UnixCompressFilter(),
                                                     new HatanakaCompressFilter())) {
            nd = filter.filter(nd);
        }
        final RinexObservationParser parser = new RinexObservationParser();
        final List<ObservationDataSet> obserDataSets = parser.parse(nd).getObservationDataSets();
        final double dt = 31; //great time gap threshold to don't detect cycle-slip because of time gap
        final int N = 25;
        final int m = 2;
        //Test on L2
        final double[] thresholdL2 = new double[] {
            0.4,2};
        //The date have been computed with an excel spreadsheet.
        final AbsoluteDate[] dateL2 = new AbsoluteDate[] {
          new AbsoluteDate(2016, 2, 13 ,1, 43, 13.000, TimeScalesFactory.getUTC()),
        };

        for(int i = 0; i<thresholdL2.length; i++) {
            PhaseMinusCodeCycleSlipDetector slipDetectorsL2 =
                            new PhaseMinusCodeCycleSlipDetector(dt, thresholdL2[i], N, m);
            final List<CycleSlipDetectorResults> resultsL2 = slipDetectorsL2.detect(obserDataSets);
            for(CycleSlipDetectorResults d : resultsL2) {
                if(i == 0) {
                    final List<AbsoluteDate> computedDateOnL2 = d.getCycleSlipMap().get(Frequency.G02);
                    Assertions.assertEquals(3, computedDateOnL2.size());
                    Assertions.assertEquals(0.0, computedDateOnL2.get(0).durationFrom(dateL2[i]), 0.0);
                } else {
                    final List<AbsoluteDate> computedDateOnL2 = d.getCycleSlipMap().get(Frequency.G02);
                    Assertions.assertEquals(0, computedDateOnL2.size());
                }

            }
        }
        /////////////////////////////////////////////////////////////////////////////////////
        //Test on L1
        final double[] thresholdL1 = new double[] {
            0.1,0.30,2};
        //The date have been computed with an excel spreadsheet.
        final AbsoluteDate[] dateL1 = new AbsoluteDate[] {
            new AbsoluteDate(2016, 2, 13 ,1, 43, 13.000, TimeScalesFactory.getUTC()),
            new AbsoluteDate(2016, 02, 13, 01, 55, 43.000, TimeScalesFactory.getUTC()),
            new AbsoluteDate(2016, 02, 13, 02, 8, 13.000, TimeScalesFactory.getUTC())
          };
        for (int i=0; i<thresholdL1.length; i++) {
            PhaseMinusCodeCycleSlipDetector slipDetectorsL1 =
                            new PhaseMinusCodeCycleSlipDetector(dt, thresholdL1[i], N, m+1);
            final List<CycleSlipDetectorResults> resultsL1 = slipDetectorsL1.detect(obserDataSets);
            for(CycleSlipDetectorResults d : resultsL1) {
                if(i == 0) {
                    final List<AbsoluteDate> computedDateOnL1 = d.getCycleSlipMap().get(Frequency.G01);
                    Assertions.assertEquals(3, computedDateOnL1.size());
                    int i1 = 0;
                    for(AbsoluteDate date: computedDateOnL1) {
                        Assertions.assertEquals(0.0, date.durationFrom(dateL1[i1]), 1e-9);
                        i1++;
                    }
                } else if (i == 1) {
                    final List<AbsoluteDate> computedDateOnL1 = d.getCycleSlipMap().get(Frequency.G01);
                    Assertions.assertEquals(2, computedDateOnL1.size());
                } else {
                    final List<AbsoluteDate> computedDateOnL1 = d.getCycleSlipMap().get(Frequency.G01);
                    Assertions.assertEquals(0, computedDateOnL1.size());
                }
            }
        }
    }

    /** Getter on the PRN of the satellite. */
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
