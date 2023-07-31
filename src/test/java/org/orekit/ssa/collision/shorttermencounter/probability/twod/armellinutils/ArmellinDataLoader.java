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
package org.orekit.ssa.collision.shorttermencounter.probability.twod.armellinutils;

import org.hipparchus.util.FastMath;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class used to load Roberto Armellin's data filtered data about the ESA collision avoidance challenge.
 *
 * @author Vincent CUCCHIETTI
 */
public class ArmellinDataLoader {

    private ArmellinDataLoader() {
        // Empty constructor
    }

    public static List<ArmellinDataRow> load() throws IOException {
        final ClassLoader       classloader       = Thread.currentThread().getContextClassLoader();
        final InputStream       inputStream       = classloader.getResourceAsStream("collision-resources/data.csv");
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final BufferedReader    bufferedReader    = new BufferedReader(inputStreamReader);

        final List<ArmellinDataRow> dataRows = new ArrayList<>();

        String line       = bufferedReader.readLine();
        int    lineNumber = 0;
        while (line != null) {
            final String[] metadata = line.split(",");

            if (lineNumber != 0) {
                dataRows.add(createArmellinDataRow(metadata));
            }

            line = bufferedReader.readLine();
            lineNumber++;
        }

        return dataRows;
    }

    /**
     * Parse data row from input String array.
     *
     * @param metadata Input string array.
     *
     * @return Corresponding {@link ArmellinDataRow Armellin data row} instance.
     */
    private static ArmellinDataRow createArmellinDataRow(final String[] metadata) {
        final int    id                           = Integer.parseInt(metadata[0]);
        final double combinedRadius               = Double.parseDouble(metadata[1]) * 1e3;
        final double primaryPositionX             = Double.parseDouble(metadata[2]) * 1e3;
        final double primaryPositionY             = Double.parseDouble(metadata[3]) * 1e3;
        final double primaryPositionZ             = Double.parseDouble(metadata[4]) * 1e3;
        final double primaryVelocityX             = Double.parseDouble(metadata[5]) * 1e3;
        final double primaryVelocityY             = Double.parseDouble(metadata[6]) * 1e3;
        final double primaryVelocityZ             = Double.parseDouble(metadata[7]) * 1e3;
        final double primaryCrr                   = Double.parseDouble(metadata[8]) * 1e6;
        final double primaryCtt                   = Double.parseDouble(metadata[9]) * 1e6;
        final double primaryCnn                   = Double.parseDouble(metadata[10]) * 1e6;
        final double primaryCrt                   = Double.parseDouble(metadata[11]) * 1e6;
        final double primaryCrn                   = Double.parseDouble(metadata[12]) * 1e6;
        final double primaryCtn                   = Double.parseDouble(metadata[13]) * 1e6;
        final double secondaryPositionX           = Double.parseDouble(metadata[14]) * 1e3;
        final double secondaryPositionY           = Double.parseDouble(metadata[15]) * 1e3;
        final double secondaryPositionZ           = Double.parseDouble(metadata[16]) * 1e3;
        final double secondaryVelocityX           = Double.parseDouble(metadata[17]) * 1e3;
        final double secondaryVelocityY           = Double.parseDouble(metadata[18]) * 1e3;
        final double secondaryVelocityZ           = Double.parseDouble(metadata[19]) * 1e3;
        final double secondaryCrr                 = Double.parseDouble(metadata[20]) * 1e6;
        final double secondaryCtt                 = Double.parseDouble(metadata[21]) * 1e6;
        final double secondaryCnn                 = Double.parseDouble(metadata[22]) * 1e6;
        final double secondaryCrt                 = Double.parseDouble(metadata[23]) * 1e6;
        final double secondaryCrn                 = Double.parseDouble(metadata[24]) * 1e6;
        final double secondaryCtn                 = Double.parseDouble(metadata[25]) * 1e6;
        final double probabilityOfCollisionAlfano = Double.parseDouble(metadata[26]);
        final double probabilityOfCollisionApprox = Double.parseDouble(metadata[27]);
        final double probabilityOfCollisionMax    = Double.parseDouble(metadata[28]);
        final double relativeDistance             = Double.parseDouble(metadata[29]);
        final double relativeVelocity             = Double.parseDouble(metadata[30]);
        final double mahalanobisDistance          = FastMath.sqrt(Double.parseDouble(metadata[31]));

        return new ArmellinDataRow(id, combinedRadius, primaryPositionX, primaryPositionY, primaryPositionZ,
                                   primaryVelocityX, primaryVelocityY, primaryVelocityZ, primaryCrr, primaryCtt,
                                   primaryCnn, primaryCrt, primaryCrn, primaryCtn, secondaryPositionX,
                                   secondaryPositionY, secondaryPositionZ, secondaryVelocityX, secondaryVelocityY,
                                   secondaryVelocityZ, secondaryCrr, secondaryCtt, secondaryCnn, secondaryCrt,
                                   secondaryCrn, secondaryCtn,
                                   probabilityOfCollisionAlfano,
                                   probabilityOfCollisionApprox,
                                   probabilityOfCollisionMax,
                                   relativeDistance,
                                   relativeVelocity,
                                   mahalanobisDistance);
    }
}
