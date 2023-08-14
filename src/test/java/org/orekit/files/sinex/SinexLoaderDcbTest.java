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

import java.util.HashSet;
import java.util.Set;

import org.hipparchus.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;


public class SinexLoaderDcbTest {

    @BeforeAll
    public static void setUpData() {
        // Sets the root of data to read
        Utils.setDataRoot("gnss:sinex");
    }

    @Test
    public void testFirstLineDCB() {
        // Verify the parsing of the first line for the Sinex loader in the DCB file case.
        SinexLoader loader = new SinexLoader("DLR0MGXFIN_20212740000_03L_01D_DCB_default_description.BSX");
        AbsoluteDate creationDate = loader.getCreationDate();
        AbsoluteDate refCreationDate = new AbsoluteDate(new DateComponents(2022, 1, 1), TimeScalesFactory.getGPS()).
                shiftedBy(Constants.JULIAN_DAY * (11 - 1)).
                shiftedBy(58414);
        Assertions.assertEquals(creationDate, refCreationDate);
    }

    @Test
    public void testFirstLineDCBInUtc() {
        // Verify the parsing of the first line for the Sinex loader in the DCB file case.
        SinexLoader loader = new SinexLoader("DLR0MGXFIN_20212740000_03L_01D_DCB_UTC.BSX");
        AbsoluteDate creationDate = loader.getCreationDate();
        AbsoluteDate refCreationDate = new AbsoluteDate(new DateComponents(2022, 1, 1), TimeScalesFactory.getUTC()).
                shiftedBy(Constants.JULIAN_DAY * (11 - 1)).
                shiftedBy(58414);
        Assertions.assertEquals(creationDate, refCreationDate);
    }

    @Test
    public void testDCBDescriptionSat() {
        SinexLoader loader = new SinexLoader("DLR0MGXFIN_20212740000_03L_01D_DCB_trunc_sat.BSX");
        // DCB Description test
        DcbSatellite dcbSat = loader.getDcbSatellite("G01");
        DcbDescription dcbDesc = dcbSat.getDescription();
        TimeSystem timeSystem = dcbDesc.getTimeSystem();
        String biasMode = dcbDesc.getBiasMode();
        String determinationMethod = dcbDesc.getDeterminationMethod();
        int observationSampling = dcbDesc.getObservationSampling();
        int parameterSpacing = dcbDesc.getParameterSpacing();
        Assertions.assertEquals(timeSystem, TimeSystem.GPS);
        Assertions.assertEquals(biasMode, "RELATIVE");
        Assertions.assertEquals(determinationMethod, "INTER-FREQUENCY_BIAS_ESTIMATION");
        Assertions.assertEquals(parameterSpacing, 86400);
        Assertions.assertEquals(observationSampling, 30);
    }
    
    @Test
    public void testDCBDescriptionStation() {
        SinexLoader loader = new SinexLoader("DLR0MGXFIN_20212740000_03L_01D_DCB_trunc_sat.BSX");
        // DCB Description test
        DcbStation dcbStation = loader.getDcbStation("AGGO");
        DcbDescription dcbDesc = dcbStation.getDescription();
        TimeSystem timeSystem = dcbDesc.getTimeSystem();
        String biasMode = dcbDesc.getBiasMode();
        String determinationMethod = dcbDesc.getDeterminationMethod();
        int observationSampling = dcbDesc.getObservationSampling();
        int parameterSpacing = dcbDesc.getParameterSpacing();
        Assertions.assertEquals(timeSystem, TimeSystem.GPS);
        Assertions.assertEquals(biasMode, "RELATIVE");
        Assertions.assertEquals(determinationMethod, "INTER-FREQUENCY_BIAS_ESTIMATION");
        Assertions.assertEquals(parameterSpacing, 86400);
        Assertions.assertEquals(observationSampling, 30);
    }
    
    @Test
    public void testDCBfile() {
        SinexLoader loader = new SinexLoader("DLR0MGXFIN_20212740000_03L_01D_DCB_trunc_sat.BSX");
        DcbSatellite DCBSat = loader.getDcbSatellite("G01");
        Dcb DCBTest = DCBSat.getDcbData();
        
        // Observation Pair test
        HashSet<Pair<ObservationType, ObservationType>> ObsPairs = DCBTest.getAvailableObservationPairs();
        
        // Defining the observation pair present in the truncated file.
        Pair<ObservationType, ObservationType> OP1 = new Pair<>(ObservationType.valueOf("C1C"), ObservationType.valueOf("C1W"));
        Pair<ObservationType, ObservationType> OP2 = new Pair<>(ObservationType.valueOf("C1C"), ObservationType.valueOf("C2W"));
        Pair<ObservationType, ObservationType> OP3 = new Pair<>(ObservationType.valueOf("C1C"), ObservationType.valueOf("C5Q"));
        Pair<ObservationType, ObservationType> OP4 = new Pair<>(ObservationType.valueOf("C2W"), ObservationType.valueOf("C2L"));
        
        HashSet<Pair<ObservationType, ObservationType>> observationSetsRef = new HashSet<>();
        observationSetsRef.add(OP1);
        observationSetsRef.add(OP2);
        observationSetsRef.add(OP3);
        observationSetsRef.add(OP4);

        // Check
        Assertions.assertEquals(ObsPairs, observationSetsRef);
        
        // Defining observation codes for further checks.
        String Obs1 = "C1C";
        String Obs2 = "C1W";
        
        // Minimum Date test
        int year = 2021;
        int day = 274;
        int secInDay = 0;
        
        AbsoluteDate refFirstDate = new AbsoluteDate(new DateComponents(year, 1, 1), TimeScalesFactory.getGPS()).
                shiftedBy(Constants.JULIAN_DAY * (day - 1)).
                shiftedBy(secInDay);
        AbsoluteDate firstDate =  DCBTest.getMinimumValidDateForObservationPair(Obs1, Obs2);
        
        Assertions.assertEquals(firstDate, refFirstDate);
        
        // Max Date Test
        year = 2021;
        day = 283;
        secInDay = 0;
        
        AbsoluteDate refLastDate = new AbsoluteDate(new DateComponents(year, 1, 1), TimeScalesFactory.getGPS()).
                shiftedBy(Constants.JULIAN_DAY * (day - 1)).
                shiftedBy(secInDay);
        AbsoluteDate lastDate =  DCBTest.getMaximumValidDateForObservationPair(Obs1, Obs2);
        
        Assertions.assertEquals(lastDate, refLastDate);
        
        // Value test for Satellites
        year = 2021;
        day = 280;
        secInDay = 43200;
        
        AbsoluteDate refDate = new AbsoluteDate(new DateComponents(year, 1, 1), TimeScalesFactory.getGPS()).
                shiftedBy(Constants.JULIAN_DAY * (day - 1)).
                shiftedBy(secInDay);
        
        double valueDcb = DCBTest.getDcb(Obs1, Obs2, refDate);
        double valueDcbReal = -1.0697e-9;
        
        Assertions.assertEquals(valueDcbReal, valueDcb, 1e-13);
        
        // Value Test for a Station
        DcbStation DCBStation = loader.getDcbStation("ALIC");
        Dcb DCBTestStation = DCBStation.getDcbData(SatelliteSystem.parseSatelliteSystem("R"));

        year = 2021;
        day = 300;
        secInDay = 43200;
        
        AbsoluteDate refDateStation = new AbsoluteDate(new DateComponents(year, 1, 1), TimeScalesFactory.getGPS()).
                shiftedBy(Constants.JULIAN_DAY * (day - 1)).
                shiftedBy(secInDay);
        
        double valueDcbStation = DCBTestStation.getDcb("C1C", "C1P", refDateStation);
        double valueDcbRealStation = -0.6458e-9;
        
        Assertions.assertEquals(valueDcbRealStation, valueDcbStation, 1e-13);
        
                
        // Test getSatelliteSystem
        Assertions.assertEquals(DCBSat.getSatelliteSytem(), SatelliteSystem.GPS);
        
        // Test getPRN
        Assertions.assertEquals("G01", DCBSat.getPRN());
        
    }
    
    @Test
    public void testDCBFileStation() {
        SinexLoader loader = new SinexLoader("DLR0MGXFIN_20212740000_03L_01D_DCB_trunc_sat.BSX");
        String stationIdRef = "AGGO";
        DcbStation DCBTest = loader.getDcbStation(stationIdRef);
         
        // Test getStationId : Station Case
        Assertions.assertEquals(stationIdRef, DCBTest.getSiteCode());
         
        //Test getAvailableSystems
        final SatelliteSystem sat1 = SatelliteSystem.parseSatelliteSystem("G");
        final SatelliteSystem sat2 = SatelliteSystem.parseSatelliteSystem("E");
        final Set<SatelliteSystem> setSystemRef = new HashSet<>();
        setSystemRef.add(sat1);
        setSystemRef.add(sat2);
        
        final Iterable<SatelliteSystem> setSystem = DCBTest.getAvailableSatelliteSystems();
        Assertions.assertEquals(setSystemRef, setSystem);
    }
}
