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
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.propagation.analytical.gnss.data.NavICL1NvNavigationMessage;
import org.orekit.utils.units.Unit;

/** Parser for NavIC L1NV.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public class NavICL1NvParser extends CivilianNavigationParser<NavICL1NvNavigationMessage> {

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public NavICL1NvParser(final ParseInfo parseInfo, final NavICL1NvNavigationMessage message) {
        super(parseInfo, message);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine05() {
        final ParseInfo parseInfo = getParseInfo();
        final NavICL1NvNavigationMessage message = getMessage();
        message.setIDot(parseInfo.parseDouble1(RinexNavigationParser.RAD_PER_S));
        message.setDeltaN0Dot(parseInfo.parseDouble2(RinexNavigationParser.RAD_PER_S2));
        message.setReferenceSignalFlag(parseInfo.parseInt4());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine06() {
        final ParseInfo parseInfo = getParseInfo();
        final NavICL1NvNavigationMessage message = getMessage();
        final int uraIndex = parseInfo.parseInt1();
        message.setSvAccuracy(NavICLnavParser.NAVIC_URA[FastMath.min(uraIndex, NavICLnavParser.NAVIC_URA.length - 1)]);
        message.setSvHealth(parseInfo.parseInt2());
        message.setTGD(parseInfo.parseDouble3(Unit.SECOND));
        message.setTGDSL5(parseInfo.parseDouble4(Unit.SECOND));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine07() {
        final ParseInfo parseInfo = getParseInfo();
        final NavICL1NvNavigationMessage message = getMessage();
        message.setIscSL1P(parseInfo.parseDouble1(Unit.SECOND));
        message.setIscL1DL1P(parseInfo.parseDouble2(Unit.SECOND));
        message.setIscL1PS(parseInfo.parseDouble3(Unit.SECOND));
        message.setIscL1DS(parseInfo.parseDouble4(Unit.SECOND));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine08() {
        final ParseInfo parseInfo = getParseInfo();
        final NavICL1NvNavigationMessage message = getMessage();
        message.setTransmissionTime(parseInfo.parseDouble1(Unit.SECOND));
        message.setWeek(parseInfo.parseInt2());
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addNavICL1NVNavigationMessage(getMessage());
    }

}
