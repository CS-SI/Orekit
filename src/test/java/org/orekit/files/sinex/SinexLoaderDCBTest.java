/* Copyright 2002-2022 CS GROUP
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

package org.orekit.files.sinex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.orekit.Utils;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.utils.Constants;
import org.junit.Test;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;


public class SinexLoaderDCBTest {
    
    private TimeScale utc;
    
    @Before
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("gnss:sinex");
        utc = TimeScalesFactory.getUTC();
    }
    
    @Test
    public void testFirstLineDCB() {
        // Verify the parsing of the first line for the Sinex loader in the DCB file case.
        SinexLoader loader = new SinexLoader("DLR0MGXFIN_20212740000_03L_01D_DCB_default_description.BSX");
        AbsoluteDate creaDate = loader.getCreationDate();
        AbsoluteDate refCreaDate = new AbsoluteDate(new DateComponents(2022, 1, 1), utc).
                shiftedBy(Constants.JULIAN_DAY * (11 - 1)).
                shiftedBy(58414);
        Assert.assertEquals(creaDate, refCreaDate);
    }
    
    @Test
    public void testDCBDescriptionSat() {
        SinexLoader loader = new SinexLoader("DLR0MGXFIN_20212740000_03L_01D_DCB_trunc_sat.BSX");
        // DCB Description test
        DCBSatellite dcbSat = loader.getDCBSatellite("G01");
        DCBDescription dcbDesc = dcbSat.getDcbDescription();
        SatelliteSystem timeSystem = dcbDesc.getTimeSystem();
        String biasMode = dcbDesc.getBiasMode();
        String determinationMethod = dcbDesc.getDeterminationMethod();
        int observationSampling = dcbDesc.getObservationSampling();
        int parameterSpacing = dcbDesc.getParameterSpacing();
        Assert.assertEquals(timeSystem, SatelliteSystem.GPS);
        Assert.assertEquals(biasMode, "RELATIVE");
        Assert.assertEquals(determinationMethod, "INTER-FREQUENCY_BIAS_ESTIMATION");
        Assert.assertEquals(parameterSpacing, 86400);
        Assert.assertEquals(observationSampling, 30);
    }
    
    @Test
    public void testDCBDescriptionStation() {
        SinexLoader loader = new SinexLoader("DLR0MGXFIN_20212740000_03L_01D_DCB_trunc_sat.BSX");
        // DCB Description test
        DCBStation dcbStation = loader.getDCBStation("AGGO");
        DCBDescription dcbDesc = dcbStation.getDcbDescription();
        SatelliteSystem timeSystem = dcbDesc.getTimeSystem();
        String biasMode = dcbDesc.getBiasMode();
        String determinationMethod = dcbDesc.getDeterminationMethod();
        int observationSampling = dcbDesc.getObservationSampling();
        int parameterSpacing = dcbDesc.getParameterSpacing();
        Assert.assertEquals(timeSystem, SatelliteSystem.GPS);
        Assert.assertEquals(biasMode, "RELATIVE");
        Assert.assertEquals(determinationMethod, "INTER-FREQUENCY_BIAS_ESTIMATION");
        Assert.assertEquals(parameterSpacing, 86400);
        Assert.assertEquals(observationSampling, 30);
    }
    
    @Test
    public void testDCBfile() {
        SinexLoader loader = new SinexLoader("DLR0MGXFIN_20212740000_03L_01D_DCB_trunc_sat.BSX");
        DCBSatellite DCBSat = loader.getDCBSatellite("G01");
        DCB DCBTest = DCBSat.getDcbObject();
        

        // Observation Pair test
        HashSet< HashSet<ObservationType> > ObsPairs = DCBTest.getAvailableObservationPairs();
        
        // Defining the observation pair present in the truncated file.
        HashSet<ObservationType> OP1 = new HashSet<ObservationType>();
        HashSet<ObservationType> OP2 = new HashSet<ObservationType>();
        HashSet<ObservationType> OP3 = new HashSet<ObservationType>();
        HashSet<ObservationType> OP4 = new HashSet<ObservationType>();

        ObservationType Ob1 = ObservationType.valueOf("C1C");
        ObservationType Ob2 = ObservationType.valueOf("C1W");
        ObservationType Ob3 = ObservationType.valueOf("C2W");
        ObservationType Ob4 = ObservationType.valueOf("C5Q");
        ObservationType Ob5 = ObservationType.valueOf("C2W");
        ObservationType Ob6 = ObservationType.valueOf("C2L");
        
        OP1.add(Ob1);
        OP1.add(Ob2);
        OP2.add(Ob1);
        OP2.add(Ob3);
        OP3.add(Ob1);
        OP3.add(Ob4);
        OP4.add(Ob5);
        OP4.add(Ob6);
        
        HashSet< HashSet<ObservationType> > observationSetsRef = new HashSet< HashSet<ObservationType> >();
        observationSetsRef.add(OP1);
        observationSetsRef.add(OP2);
        observationSetsRef.add(OP3);
        observationSetsRef.add(OP4);
        
        // Check
        Assert.assertEquals(null, ObsPairs, observationSetsRef);
        
        // Defining observation codes for further checks.
        String Obs1 = "C1C";
        String Obs2 = "C1W";
        
        // Minimum Date test
        int year = 2021;
        int day = 274;
        int secInDay = 0;
        
        AbsoluteDate refFirstDate = new AbsoluteDate(new DateComponents(year, 1, 1), utc).
                shiftedBy(Constants.JULIAN_DAY * (day - 1)).
                shiftedBy(secInDay);
        AbsoluteDate firstDate =  DCBTest.getMinDateObservationPair(Obs1, Obs2);
        
        Assert.assertEquals(firstDate, refFirstDate);
        
        // Max Date Test
        year = 2021;
        day = 283;
        secInDay = 0;
        
        AbsoluteDate refLastDate = new AbsoluteDate(new DateComponents(year, 1, 1), utc).
                shiftedBy(Constants.JULIAN_DAY * (day - 1)).
                shiftedBy(secInDay);
        AbsoluteDate lastDate =  DCBTest.getMaxDateObservationPair(Obs1, Obs2);
        
        Assert.assertEquals(lastDate, refLastDate);
        
        // Value test for Satellites
        year = 2021;
        day = 280;
        secInDay = 43200;
        
        AbsoluteDate refDate = new AbsoluteDate(new DateComponents(year, 1, 1), utc).
                shiftedBy(Constants.JULIAN_DAY * (day - 1)).
                shiftedBy(secInDay);
        
        double valueDcb = DCBTest.getDCB(Obs1, Obs2, refDate);
        double valueDcbReal = -1.0697e-9;
        
        Assert.assertEquals(valueDcbReal, valueDcb, 1e-13);
        
        // Value Test for a Station
        DCBStation DCBStation = loader.getDCBStation("ALIC");
        DCB DCBTestStation = DCBStation.getDcbObject(SatelliteSystem.parseSatelliteSystem("R"));
        HashSet<ObservationType> OPStation = new HashSet<ObservationType>();
        ObservationType Ob7 = ObservationType.valueOf("C1P");
        OPStation.add(Ob1);
        OPStation.add(Ob7);
        
        year = 2021;
        day = 300;
        secInDay = 43200;
        
        AbsoluteDate refDateStation = new AbsoluteDate(new DateComponents(year, 1, 1), utc).
                shiftedBy(Constants.JULIAN_DAY * (day - 1)).
                shiftedBy(secInDay);
        
        double valueDcbStation = DCBTestStation.getDCB("C1C", "C1P", refDateStation);
        double valueDcbRealStation = -0.6458e-9;
        
        Assert.assertEquals(valueDcbRealStation, valueDcbStation, 1e-13);
        
                
        // Test getSatelliteSystem
        Assert.assertEquals(DCBSat.getSatelliteSytem(), SatelliteSystem.GPS);
        
        // Test getPRN
        Assert.assertEquals("G01", DCBSat.getPRN());
        
    }
    
    @Test
    public void testDCBFileStation() {
        SinexLoader loader = new SinexLoader("DLR0MGXFIN_20212740000_03L_01D_DCB_trunc_sat.BSX");
        String stationIdRef = "AGGO";
        String stationSystemRef = "G";
        DCBStation DCBTest = loader.getDCBStation(stationIdRef);
        

         
         // Test getStationId : Station Case
         Assert.assertEquals(stationIdRef, DCBTest.getStationId());
    }
}
