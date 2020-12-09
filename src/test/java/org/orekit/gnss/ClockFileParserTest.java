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

    /** First example given in the 3.04 RINEX clock file format. */
    @Test
    public void testParseExple1V304() throws URISyntaxException, IOException {
        
        // Parse file
        final String ex = "/gnss/clock/Exple_analysis_1_304.clk";
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check number of data lines
        Assert.assertEquals(5, file.getTotalNumberOfDataLines());
    }
    
    /** Second example given in the 3.04 RINEX clock file format. */
    @Test
    public void testParseExple2V304() throws URISyntaxException, IOException {
        
        // Parse file
        final String ex = "/gnss/clock/Exple_analysis_2_304.clk";
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check number of data lines
        Assert.assertEquals(6, file.getTotalNumberOfDataLines());
    }
    
    /** Third example given in the 3.04 RINEX clock file format. */
    @Test
    public void testParseExpleCalibrationV304() throws URISyntaxException, IOException {
        
        // Parse file
        final String ex = "/gnss/clock/Exple_calibration_304.clk";
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check number of data lines
        Assert.assertEquals(4, file.getTotalNumberOfDataLines());
    }

    /** An example of the 3.00 RINEX clock file format with format mistakes and missing fields. */
    @Test
    public void testParseExple1V300() throws URISyntaxException, IOException {
        
        // Parse file
        final String ex = "/gnss/clock/mit19044_truncated_300.clk";
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check number of data lines
        Assert.assertEquals(197, file.getTotalNumberOfDataLines());
    }

    /** Another example of the 3.00 RINEX clock file format with format mistakes and missing fields. */
    @Test
    public void testParseExple2V300() throws URISyntaxException, IOException {
        
        // Parse file
        final String ex = "/gnss/clock/igr21101_truncated_300.clk";
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check number of data lines
        Assert.assertEquals(94, file.getTotalNumberOfDataLines());
    }
  
    /** An example of the 2.00 RINEX clock file format. */
    @Test
    public void testParseExpleV200() throws URISyntaxException, IOException {
        
        // Parse file
        final String ex = "/gnss/clock/emr10491_truncated_200.clk";
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check number of data lines
        Assert.assertEquals(110, file.getTotalNumberOfDataLines());
    }
}
