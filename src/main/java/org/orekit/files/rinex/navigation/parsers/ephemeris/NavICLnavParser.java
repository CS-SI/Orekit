/* Copyright 2002-2025 CS GROUP
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
package org.orekit.files.rinex.navigation.parsers.ephemeris;

import org.hipparchus.util.FastMath;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.propagation.analytical.gnss.data.NavICLegacyNavigationMessage;
import org.orekit.utils.units.Unit;

/** Parser for NavIC legacy.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public class NavICLnavParser extends LegacyNavigationParser<NavICLegacyNavigationMessage> {

    /** URA index to URA mapping (table 23 of NavIC ICD). */
    // CHECKSTYLE: stop Indentation check
    static final double[] NAVIC_URA = {
           2.40,    3.40,    4.85,   6.85,
           9.65,   13.65,   24.00,  48.00,
          96.00,  192.00,  384.00, 768.00,
        1536.00, 3072.00, 6144.00, Double.NaN
    };
    // CHECKSTYLE: resume Indentation check

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public NavICLnavParser(final ParseInfo parseInfo, final NavICLegacyNavigationMessage message) {
        super(parseInfo, message);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        final ParseInfo parseInfo = getParseInfo();
        parseSvEpochSvClockLine(parseInfo.getTimeScales().getNavIC(), parseInfo, getMessage());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        super.parseLine01();
        // for NavIC legacy, Issue Of Data applies to both clock and ephemeris
        final NavICLegacyNavigationMessage message = getMessage();
        message.setIODC(message.getIODE());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine06() {
        super.parseLine06();

        // for NavIC legacy, the User Range Accurary is provided as an index in a table
        // the base class implementation just parsed it as a double, we need to fix it
        final NavICLegacyNavigationMessage message = getMessage();
        final int index = (int) FastMath.rint(message.getSvAccuracy());
        message.setSvAccuracy(NAVIC_URA[FastMath.min(index, NAVIC_URA.length - 1)]);

    }

    /** {@inheritDoc} */
    @Override
    public void parseLine07() {
        final ParseInfo parseInfo = getParseInfo();
        final NavICLegacyNavigationMessage message = getMessage();
        message.setTransmissionTime(parseInfo.parseDouble1(Unit.SECOND));
        // there is no fit interval in NavIC L message
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addNavICLegacyNavigationMessage(getMessage());
    }

}
