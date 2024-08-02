/* Copyright 2002-2024 CS GROUP
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


package org.orekit.models.earth.atmosphere.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;

import java.io.BufferedReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;


class CommonLineReaderTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data:atmosphere");
    }

    @Test
    void testParsing() throws IOException, URISyntaxException {
        final String name = "DTCFILE_CommonLineReaderTest.txt";
        URL url = CommonLineReaderTest.class.getClassLoader().getResource("atmosphere/"+name);
        DataSource ds = new DataSource(Paths.get(url.toURI()).toString());

        try (InputStream       is  = ds.getOpener().openStreamOnce();
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader    br  = new BufferedReader(isr)) {

               CommonLineReader reader = new CommonLineReader(br);
               reader.readLine();
               assertTrue(reader.isEmptyLine());
               reader.readLine();
            assertEquals("DTC 2003 360   50  17  17  17  38  38  38  74  74  74  74  74  74  31  31  31  38  38  38  38  38  38  44  44", reader.getLine());
            assertEquals(2, reader.getLineNumber());
               assertFalse(reader.isEmptyLine());

           }
    }

}
