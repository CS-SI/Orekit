/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.files.rinex.navigation.writers;

import org.orekit.files.rinex.navigation.RinexNavigationWriter;
import org.orekit.propagation.analytical.gnss.data.NavICLegacyNavigationMessage;

import java.io.IOException;

/** Writer for NavIC legacy messages.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class NavICLegacyNavigationMessageWriter
    extends LegacyNavigationMessageWriter<NavICLegacyNavigationMessage> {

    /** URA index to URA mapping (table 23 of NavIC ICD). */
    // CHECKSTYLE: stop Indentation check
    private static final double[] NAVIC_URA = {
           2.40,    3.40,    4.85,   6.85,
           9.65,   13.65,   24.00,  48.00,
          96.00,  192.00,  384.00, 768.00,
        1536.00, 3072.00, 6144.00, Double.NaN
    };
    // CHECKSTYLE: resume Indentation check

    /** {@inheritDoc} */
    @Override
    protected void writeURA(final NavICLegacyNavigationMessage message, final RinexNavigationWriter writer)
        throws IOException {
        int index = 0;
        while (index < NAVIC_URA.length - 1 && NAVIC_URA[index] < message.getSvAccuracy()) {
            ++index;
        }
        writer.writeInt(index);
    }

}
