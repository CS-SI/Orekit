/* Copyright 2002-2026 CS GROUP
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
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessageFactory;
import org.orekit.time.GNSSDate;
import org.orekit.utils.units.Unit;

/** Parser for Galileo.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public class GalileoParser
        extends AbstractNavigationParser<GalileoNavigationMessage, GalileoNavigationMessageFactory> {

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param factory factory for navigation message
     */
    public GalileoParser(final ParseInfo parseInfo, final GalileoNavigationMessageFactory factory) {
        super(parseInfo, factory);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        final ParseInfo parseInfo = getParseInfo();
        final GalileoNavigationMessageFactory factory = getFactory();
        parseSvEpochSvClockLine(parseInfo.getTimeScales().getGPS(), parseInfo, factory);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        super.parseLine01();
        final GalileoNavigationMessageFactory factory = getFactory();
        factory.setIodNav(getParseInfo().parseInt1());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine05() {
        super.parseLine05();
        final GalileoNavigationMessageFactory factory = getFactory();
        factory.setDataSource(getParseInfo().parseInt2());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine06() {
        final ParseInfo parseInfo = getParseInfo();
        final GalileoNavigationMessageFactory factory = getFactory();
        factory.setSisa(parseInfo.parseDouble1(Unit.METRE));
        factory.setSvHealth(parseInfo.parseDouble2(Unit.NONE));
        factory.setBGDE1E5a(parseInfo.parseDouble3(Unit.SECOND));
        factory.setBGDE5bE1(parseInfo.parseDouble4(Unit.SECOND));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine07() {
        final ParseInfo parseInfo = getParseInfo();
        final GalileoNavigationMessageFactory factory = getFactory();
        factory.setTransmissionTime(new GNSSDate(factory.getTimeOfEphemeris().getWeekNumber(),
                                                 parseInfo.parseDouble1(Unit.SECOND),
                                                 factory.getSystem()));
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addGalileoNavigationMessage(getMessage());
    }

}
