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

import java.util.Collection;
import java.util.HashSet;

import org.hipparchus.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.PredefinedObservationType;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class SinexBiasParserTest {

    @BeforeAll
    public static void setUpData() {
        // Sets the root of data to read
        Utils.setDataRoot("gnss:sinex");
    }

    @Test
    public void testFirstLineDsb() {
        // Verify the parsing of the first line for the Sinex parser in the DSB file case.
        SinexBias sinexBias = load("/sinex/DLR0MGXFIN_20212740000_03L_01D_DSB_default_description.BSX");
        AbsoluteDate creationDate = sinexBias.getCreationDate();
        AbsoluteDate refCreationDate = new AbsoluteDate(new DateComponents(2022, 11),
                                                        new TimeComponents(58414),
                                                        TimeScalesFactory.getGPS());
        Assertions.assertEquals(creationDate, refCreationDate);
    }

    @Test
    public void testFirstLineDsbInUtc() {
        // Verify the parsing of the first line for the Sinex parser in the DSB file case.
        SinexBias sinexBias = load("/sinex/DLR0MGXFIN_20212740000_03L_01D_DSB_UTC.BSX");
        AbsoluteDate creationDate = sinexBias.getCreationDate();
        AbsoluteDate refCreationDate = new AbsoluteDate(new DateComponents(2022, 11),
                                                        new TimeComponents(58414),
                                                        TimeScalesFactory.getUTC());
        Assertions.assertEquals(creationDate, refCreationDate);
    }

    @Test
    public void testDsbDescriptionSat() {
        SinexBias sinexBias = load("/sinex/DLR0MGXFIN_20212740000_03L_01D_DSB_trunc_sat.BSX");
        // DSB Description test
        BiasDescription dcbDesc = sinexBias.getDescription();
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
    public void testDsbDescriptionStation() {
        SinexBias sinexBias = load("/sinex/DLR0MGXFIN_20212740000_03L_01D_DSB_trunc_sat.BSX");
        // DSB Description test
        BiasDescription dcbDesc = sinexBias.getDescription();
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
    public void testDsbfile() {
        SinexBias sinexBias = load("/sinex/DLR0MGXFIN_20212740000_03L_01D_DSB_trunc_sat.BSX");
        SatelliteDifferentialSignalBias satDsb = sinexBias.getSatellitesDsb().get(new SatInSystem("G01"));
        DifferentialSignalBias dsb = satDsb.getDsb();
        Assertions.assertTrue(sinexBias.getSatellitesOsb().isEmpty());
        Assertions.assertTrue(sinexBias.getStationsOsb().isEmpty());
        
        // Observation Pair test
        HashSet<Pair<ObservationType, ObservationType>> ObsPairs = dsb.getAvailableObservationPairs();
        
        // Defining the observation pair present in the truncated file.
        Pair<ObservationType, ObservationType> OP1 = new Pair<>(PredefinedObservationType.C1C, PredefinedObservationType.C1W);
        Pair<ObservationType, ObservationType> OP2 = new Pair<>(PredefinedObservationType.C1C, PredefinedObservationType.C2W);
        Pair<ObservationType, ObservationType> OP3 = new Pair<>(PredefinedObservationType.C1C, PredefinedObservationType.C5Q);
        Pair<ObservationType, ObservationType> OP4 = new Pair<>(PredefinedObservationType.C2W, PredefinedObservationType.C2L);
        
        HashSet<Pair<ObservationType, ObservationType>> observationSetsRef = new HashSet<>();
        observationSetsRef.add(OP1);
        observationSetsRef.add(OP2);
        observationSetsRef.add(OP3);
        observationSetsRef.add(OP4);

        // Check
        Assertions.assertEquals(ObsPairs, observationSetsRef);
        
        // Defining observation codes for further checks.
        ObservationType Obs1 = PredefinedObservationType.C1C;
        ObservationType Obs2 = PredefinedObservationType.C1W;
        
        // Minimum Date test
        AbsoluteDate refFirstDate = new AbsoluteDate(new DateComponents(2021, 274),
                                                     TimeComponents.H00,
                                                     TimeScalesFactory.getGPS());
        AbsoluteDate firstDate =  dsb.getMinimumValidDateForObservationPair(Obs1, Obs2);
        
        Assertions.assertEquals(refFirstDate, firstDate);
        
        // Max Date Test
        AbsoluteDate refLastDate = new AbsoluteDate(new DateComponents(2021, 283),
                                                    TimeComponents.H00,
                                                    TimeScalesFactory.getGPS());
        AbsoluteDate lastDate =  dsb.getMaximumValidDateForObservationPair(Obs1, Obs2);
        
        Assertions.assertEquals(refLastDate, lastDate);
        
        // Value test for Satellites
        AbsoluteDate refDate = new AbsoluteDate(new DateComponents(2021, 280),
                                                new TimeComponents(43200),
                                                TimeScalesFactory.getGPS());
        
        double valueDsb = dsb.getBias(Obs1, Obs2, refDate);
        double valueDsbReal = -1.0697e-9 * Constants.SPEED_OF_LIGHT;
        
        Assertions.assertEquals(valueDsbReal, valueDsb, 1e-5);
        
        // Value Test for a Station
        StationDifferentialSignalBias StationDifferentialSignalBias = sinexBias.getStationsDsb().get("ALIC");
        DifferentialSignalBias differentialSignalBiasTestStation = StationDifferentialSignalBias.getDsb(SatelliteSystem.parseSatelliteSystem("R"));

        AbsoluteDate refDateStation = new AbsoluteDate(new DateComponents(2021, 300),
                                                       new TimeComponents(43200),
                                                       TimeScalesFactory.getGPS());
        
        double valueDsbStation = differentialSignalBiasTestStation.getBias(PredefinedObservationType.C1C,
                                                       PredefinedObservationType.C1P,
                                                       refDateStation);
        double valueDsbRealStation = -0.6458e-9 * Constants.SPEED_OF_LIGHT;
        
        Assertions.assertEquals(valueDsbRealStation, valueDsbStation, 1e-13);
        
                
        // Test getSatelliteSystem
        Assertions.assertEquals(satDsb.getSatellite().getSystem(), SatelliteSystem.GPS);
        
        // Test getPRN
        Assertions.assertEquals(1, satDsb.getSatellite().getPRN());
        
    }

    @Test
    public void testDsbFileStation() {
        SinexBias sinexBias = load("/sinex/DLR0MGXFIN_20212740000_03L_01D_DSB_trunc_sat.BSX");
        String stationIdRef = "AGGO";
        StationDifferentialSignalBias DSBTest = sinexBias.getStationsDsb().get(stationIdRef);
         
        // Test getStationId : Station Case
        Assertions.assertEquals(stationIdRef, DSBTest.getSiteCode());
         
        final Collection<SatelliteSystem> availableSystems = DSBTest.getAvailableSatelliteSystems();
        Assertions.assertEquals(2, availableSystems.size());
        Assertions.assertTrue(availableSystems.contains(SatelliteSystem.GPS));
        Assertions.assertTrue(availableSystems.contains(SatelliteSystem.GALILEO));

    }

    @Test
    public void testOsbSatellite() {
        SinexBias sinexBias = load("/sinex/code.bia");
        SatelliteObservableSpecificSignalBias satOsb = sinexBias.getSatellitesOsb().get(new SatInSystem("E08"));
        ObservableSpecificSignalBias osb = satOsb.getOsb();
        Assertions.assertTrue(sinexBias.getSatellitesDsb().isEmpty());
        Assertions.assertTrue(sinexBias.getStationsDsb().isEmpty());

        final TimeSystem ts = sinexBias.getDescription().getTimeSystem();
        Assertions.assertEquals(TimeSystem.GPS, ts);
        final TimeScale timeScale = ts.getTimeScale(TimeScalesFactory.getTimeScales());

        // Observations test
        HashSet<ObservationType> types = osb.getAvailableObservations();
        Assertions.assertEquals(4, types.size());
        Assertions.assertTrue(types.contains(PredefinedObservationType.C1C));
        Assertions.assertTrue(types.contains(PredefinedObservationType.C1X));
        Assertions.assertTrue(types.contains(PredefinedObservationType.C5Q));
        Assertions.assertTrue(types.contains(PredefinedObservationType.C5X));

        // Minimum Date test
        AbsoluteDate refFirstDate = new AbsoluteDate(new DateComponents(2024, 237),
                                                     TimeComponents.H00,
                                                     timeScale);
        AbsoluteDate firstDate =  osb.getMinimumValidDateForObservation(PredefinedObservationType.C5X);
        Assertions.assertEquals(refFirstDate, firstDate);

        // Max Date Test
        AbsoluteDate refLastDate = new AbsoluteDate(new DateComponents(2024, 267),
                                                    TimeComponents.H00,
                                                    timeScale);
        AbsoluteDate lastDate =  osb.getMaximumValidDateForObservation(PredefinedObservationType.C5X);
        Assertions.assertEquals(refLastDate, lastDate);

        double valueOsb = osb.getBias(PredefinedObservationType.C5X,
                                      new AbsoluteDate(new DateComponents(2024, 250),
                                                       TimeComponents.H00,
                                                       timeScale));
        double valueOsbReal = -6.7298e-9 * Constants.SPEED_OF_LIGHT;

        Assertions.assertEquals(valueOsbReal, valueOsb, 1e-5);

    }

    @Test
    public void testOsbStation() {
        SinexBias sinexBias = load("/sinex/station.bia");
        StationObservableSpecificSignalBias staOsb = sinexBias.getStationsOsb().get("TUKT");
        ObservableSpecificSignalBias osb = staOsb.getOsb(SatelliteSystem.GALILEO);
        Assertions.assertTrue(sinexBias.getSatellitesDsb().isEmpty());
        Assertions.assertTrue(sinexBias.getStationsDsb().isEmpty());

        final TimeSystem ts = sinexBias.getDescription().getTimeSystem();
        Assertions.assertEquals(TimeSystem.GALILEO, ts);
        final TimeScale timeScale = ts.getTimeScale(TimeScalesFactory.getTimeScales());

        Assertions.assertEquals(1, staOsb.getAvailableSatelliteSystems().size());
        Assertions.assertTrue(staOsb.getAvailableSatelliteSystems().contains(SatelliteSystem.GALILEO));
        Assertions.assertEquals("TUKT", staOsb.getSiteCode());

        // Observations test
        HashSet<ObservationType> types = osb.getAvailableObservations();
        Assertions.assertEquals(3, types.size());
        Assertions.assertTrue(types.contains(PredefinedObservationType.C1C));
        Assertions.assertTrue(types.contains(PredefinedObservationType.C1X));
        Assertions.assertTrue(types.contains(PredefinedObservationType.C6A));

        // Minimum Date test
        AbsoluteDate refFirstDate = new AbsoluteDate(new DateComponents(2024, 237),
                                                     TimeComponents.H00,
                                                     timeScale);
        AbsoluteDate firstDate =  osb.getMinimumValidDateForObservation(PredefinedObservationType.C1C);
        Assertions.assertEquals(refFirstDate, firstDate);

        // Max Date Test
        AbsoluteDate refLastDate = new AbsoluteDate(new DateComponents(2024, 267),
                                                    TimeComponents.H00,
                                                    timeScale);
        AbsoluteDate lastDate =  osb.getMaximumValidDateForObservation(PredefinedObservationType.C1X);
        Assertions.assertEquals(refLastDate, lastDate);

        double valueOsb = osb.getBias(PredefinedObservationType.C6A,
                                      new AbsoluteDate(new DateComponents(2024, 250),
                                                       TimeComponents.H00,
                                                       timeScale));
        double valueOsbReal = -18.5167e-9 * Constants.SPEED_OF_LIGHT;

        Assertions.assertEquals(valueOsbReal, valueOsb, 1e-5);

        final double phaseBias = sinexBias.
                                 getStationsOsb().
                                 get("BRUX").
                                 getOsb(SatelliteSystem.GALILEO).
                                 getBias(PredefinedObservationType.L6A,
                                         new AbsoluteDate(new DateComponents(2024, 250), TimeComponents.H00, timeScale));
        Assertions.assertEquals(1.7e-3, phaseBias, 1.0e-15);

    }

    private SinexBias load(final String name) {
        return new SinexBiasParser(TimeScalesFactory.getTimeScales(),
                                   PredefinedObservationType::valueOf).
               parse(new DataSource(name, () -> SinexParserTest.class.getResourceAsStream(name)));
    }

}
