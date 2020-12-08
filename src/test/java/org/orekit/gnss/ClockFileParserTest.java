/* Copyright 2002-2020 CS GROUP
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.gnss.clock.ClockFile;
import org.orekit.gnss.clock.ClockFileParser;

/** This class aims at validating the correct IGS clock file parsing. */
public class ClockFileParserTest {
    
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

   
    @Test
    public void testDefaultLoadExple1V304() throws URISyntaxException, IOException {
        
        // Parse file
        final String ex = "/gnss/clock/Exple_analysis_1.clk";
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check number of data lines
        Assert.assertEquals(5, file.getTotalNumberOfDataLines());
    }
    
    
    @Test
    public void testDefaultLoadExple2V304() throws URISyntaxException, IOException {
        
        // Parse file
        final String ex = "/gnss/clock/Exple_analysis_2.clk";
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check number of data lines
        Assert.assertEquals(6, file.getTotalNumberOfDataLines());
    }
    
    @Test
    public void testDefaultLoadExpleCalibrationV304() throws URISyntaxException, IOException {
        
        // Parse file
        final String ex = "/gnss/clock/Exple_calibration.clk";
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check number of data lines
        Assert.assertEquals(4, file.getTotalNumberOfDataLines());
    }
}
