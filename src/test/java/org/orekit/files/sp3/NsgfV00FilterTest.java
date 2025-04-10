/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.files.sp3;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.data.FiltersManager;
import org.orekit.data.GzipFilter;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class NsgfV00FilterTest {

    @Test
    public void testFiltered() throws IOException {
        doTestFilter("/sp3/nsgf.orb.stella.v00.sp3.gz", "L56", 100);
    }

    @Test
    public void testNotFiltered() throws IOException {
        doTestFilter("/sp3/example-c-1.sp3", "G04", 1, 1);
    }

    private void doTestFilter(final String name, final String id, final int... nbCoords) throws IOException {
        final DataSource original   = new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Frame      frame      = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        final SP3Parser  parser     = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 2, s -> frame);

        final FiltersManager manager = new FiltersManager();
        manager.addFilter(new NsgfV00Filter());
        manager.addFilter(new GzipFilter());

        final SP3 file = parser.parse(manager.applyRelevantFilters(original));
        for (int i = 0; i < nbCoords.length; ++i) {
            Assertions.assertEquals(nbCoords[i],
                                    file.getEphemeris(id).getSegments().get(i).getCoordinates().size());
        }

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
