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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.propagation.analytical.gnss.data.BeidouCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.BeidouCivilianNavigationMessageFactory;
import org.orekit.propagation.analytical.gnss.data.BeidouCivilianType;
import org.orekit.propagation.analytical.gnss.data.BeidouSatelliteType;
import org.orekit.time.GNSSDate;
import org.orekit.utils.units.Unit;

/** Parser for Beidou-3 CNAV.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public class BeidouCnv123Parser
        extends AbstractNavigationParser<BeidouCivilianNavigationMessage, BeidouCivilianNavigationMessageFactory> {

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param factory factory for navigation message
     */
    public BeidouCnv123Parser(final ParseInfo parseInfo, final BeidouCivilianNavigationMessageFactory factory) {
        super(parseInfo, factory);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouCivilianNavigationMessageFactory factory = getFactory();
        parseSvEpochSvClockLine(parseInfo.getTimeScales().getBDT(), parseInfo, factory);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        super.parseLine01();
        final BeidouCivilianNavigationMessageFactory factory = getFactory();
        factory.getADotDriver().setValue(getParseInfo().parseDouble1(RinexNavigationParser.M_PER_S));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine05() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouCivilianNavigationMessageFactory factory = getFactory();
        factory.getIDotDriver().setValue(parseInfo.parseDouble1(RinexNavigationParser.RAD_PER_S));
        factory.getDeltaN0DotDriver().setValue(parseInfo.parseDouble2(RinexNavigationParser.RAD_PER_S2));
        try {
            factory.setSatelliteType(BeidouSatelliteType.parseSatelliteType(parseInfo.parseInt3()));
        } catch (IllegalArgumentException iae) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      parseInfo.getLineNumber(), parseInfo.getName(),
                                      parseInfo.getLine());
        }
        factory.getTimeDriver().setValue(parseInfo.parseDouble4(Unit.SECOND));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine06() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouCivilianNavigationMessageFactory factory = getFactory();
        factory.setSisaiOe(parseInfo.parseInt1());
        factory.setSisaiOcb(parseInfo.parseInt2());
        factory.setSisaiOc1(parseInfo.parseInt3());
        factory.setSisaiOc2(parseInfo.parseInt4());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine07() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouCivilianNavigationMessageFactory factory = getFactory();
        if (factory.getBeidouType() == BeidouCivilianType.CNV1) {
            factory.setIscB1CD(parseInfo.parseDouble1(Unit.SECOND));
            // field 2 is spare
            factory.setTgdB1Cp(parseInfo.parseDouble3(Unit.SECOND));
            factory.setTgdB2ap(parseInfo.parseDouble4(Unit.SECOND));
        } else if (factory.getBeidouType() == BeidouCivilianType.CNV2) {
            // field 1 is spare
            factory.setIscB2AD(parseInfo.parseDouble2(Unit.SECOND));
            factory.setTgdB1Cp(parseInfo.parseDouble3(Unit.SECOND));
            factory.setTgdB2ap(parseInfo.parseDouble4(Unit.SECOND));
        } else {
            parseSismaiHealthIntegrity();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine08() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouCivilianNavigationMessageFactory factory = getFactory();
        if (factory.getBeidouType() == BeidouCivilianType.CNV3) {
            factory.setTransmissionTime(new GNSSDate(factory.getTimeOfEphemeris().getWeekNumber(),
                                                     parseInfo.parseDouble1(Unit.SECOND),
                                                     factory.getSystem()));
            parseInfo.closePendingRecord();
        } else {
            parseSismaiHealthIntegrity();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine09() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouCivilianNavigationMessageFactory factory = getFactory();
        factory.setTransmissionTime(new GNSSDate(factory.getTimeOfEphemeris().getWeekNumber(),
                                                 parseInfo.parseDouble1(Unit.SECOND),
                                                 factory.getSystem()));
        // field 2 is spare
        // field 3 is spare
        factory.setIode(parseInfo.parseInt4());
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addBeidouCivilianNavigationMessage(getMessage());
    }

    /**
     * Parse the SISMAI/Health/integrity line.
     */
    private void parseSismaiHealthIntegrity() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouCivilianNavigationMessageFactory factory = getFactory();
        factory.setSismai(parseInfo.parseInt1());
        factory.setHealth(parseInfo.parseInt2());
        factory.setIntegrityFlags(parseInfo.parseInt3());
        factory.setIodc(parseInfo.parseInt4());
    }

}
