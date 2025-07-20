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

import org.orekit.files.rinex.navigation.RecordType;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.parsers.RecordLineParser;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.propagation.analytical.gnss.data.QZSSCivilianNavigationMessage;
import org.orekit.utils.units.Unit;

/** Parser for QZSS civilian.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public class QzssCnavParser extends RecordLineParser {

    /** Container for parsing data. */
    private final ParseInfo parseInfo;

    /** Container for navigation message. */
    private final QZSSCivilianNavigationMessage message;

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public QzssCnavParser(final ParseInfo parseInfo, final QZSSCivilianNavigationMessage message) {
        super(RecordType.ORBIT);
        this.parseInfo = parseInfo;
        this.message   = message;
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        parseSvEpochSvClockLine(parseInfo.getTimeScales().getGPS(), parseInfo, message);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        message.setADot(parseInfo.parseDouble1(RinexNavigationParser.M_PER_S));
        message.setCrs(parseInfo.parseDouble2(Unit.METRE));
        message.setDeltaN0(parseInfo.parseDouble3(RinexNavigationParser.RAD_PER_S));
        message.setM0(parseInfo.parseDouble4(Unit.RADIAN));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine02() {
        message.setCuc(parseInfo.parseDouble1(Unit.RADIAN));
        message.setE(parseInfo.parseDouble2(Unit.NONE));
        message.setCus(parseInfo.parseDouble3(Unit.RADIAN));
        message.setSqrtA(parseInfo.parseDouble4(RinexNavigationParser.SQRT_M));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine03() {
        message.setTime(parseInfo.parseDouble1(Unit.SECOND));
        message.setCic(parseInfo.parseDouble2(Unit.RADIAN));
        message.setOmega0(parseInfo.parseDouble3(Unit.RADIAN));
        message.setCis(parseInfo.parseDouble4(Unit.RADIAN));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine04() {
        message.setI0(parseInfo.parseDouble1(Unit.RADIAN));
        message.setCrc(parseInfo.parseDouble2(Unit.METRE));
        message.setPa(parseInfo.parseDouble3(Unit.RADIAN));
        message.setOmegaDot(parseInfo.parseDouble4(RinexNavigationParser.RAD_PER_S));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine05() {
        message.setIDot(parseInfo.parseDouble1(RinexNavigationParser.RAD_PER_S));
        message.setDeltaN0Dot(parseInfo.parseDouble2(RinexNavigationParser.RAD_PER_S2));
        message.setUraiNed0(parseInfo.parseInt3());
        message.setUraiNed1(parseInfo.parseInt4());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine06() {
        message.setUraiEd(parseInfo.parseInt1());
        message.setSvHealth(parseInfo.parseInt2());
        message.setTGD(parseInfo.parseDouble3(Unit.SECOND));
        message.setUraiNed2(parseInfo.parseInt4());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine07() {
        message.setIscL1CA(parseInfo.parseDouble1(Unit.SECOND));
        message.setIscL2C(parseInfo.parseDouble2(Unit.SECOND));
        message.setIscL5I5(parseInfo.parseDouble3(Unit.SECOND));
        message.setIscL5Q5(parseInfo.parseDouble4(Unit.SECOND));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine08() {
        if (message.isCnv2()) {
            // in CNAV2 messages, there is an additional line for L1 CD and L1 CP inter signal delay
            message.setIscL1CD(parseInfo.parseDouble1(Unit.SECOND));
            message.setIscL1CP(parseInfo.parseDouble2(Unit.SECOND));
        } else {
            parseTransmissionTimeLine();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine09() {
        parseTransmissionTimeLine();
    }

    /** Parse transmission time line.
     */
    private void parseTransmissionTimeLine() {
        message.setTransmissionTime(parseInfo.parseDouble1(Unit.SECOND));
        message.setWeek(parseInfo.parseInt2());
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addQZSSCivilianNavigationMessage(message);
    }

}
