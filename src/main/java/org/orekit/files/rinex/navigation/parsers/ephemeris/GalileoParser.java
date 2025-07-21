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

import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.utils.units.Unit;

/** Parser for Galileo.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public class GalileoParser extends AbstractNavigationParser<GalileoNavigationMessage> {

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public GalileoParser(final ParseInfo parseInfo, final GalileoNavigationMessage message) {
        super(parseInfo, message);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        final ParseInfo parseInfo = getParseInfo();
        final GalileoNavigationMessage message = getMessage();
        parseSvEpochSvClockLine(parseInfo.getTimeScales().getGPS(), parseInfo, message);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        super.parseLine01();
        getMessage().setIODNav(getParseInfo().parseInt1());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine05() {
        super.parseLine05();
        getMessage().setDataSource(getParseInfo().parseInt2());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine06() {
        final ParseInfo parseInfo = getParseInfo();
        final GalileoNavigationMessage message = getMessage();
        message.setSisa(parseInfo.parseDouble1(Unit.METRE));
        message.setSvHealth(parseInfo.parseDouble2(Unit.NONE));
        message.setBGDE1E5a(parseInfo.parseDouble3(Unit.SECOND));
        message.setBGDE5bE1(parseInfo.parseDouble4(Unit.SECOND));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine07() {
        final ParseInfo parseInfo = getParseInfo();
        final GalileoNavigationMessage message = getMessage();
        message.setTransmissionTime(parseInfo.parseDouble1(Unit.SECOND));
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addGalileoNavigationMessage(getMessage());
    }

}
