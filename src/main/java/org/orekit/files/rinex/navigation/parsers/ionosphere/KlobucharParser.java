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
package org.orekit.files.rinex.navigation.parsers.ionosphere;

import org.orekit.files.rinex.navigation.IonosphereBaseMessage;
import org.orekit.files.rinex.navigation.IonosphereKlobucharMessage;
import org.orekit.files.rinex.navigation.RecordType;
import org.orekit.files.rinex.navigation.RegionCode;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.files.rinex.navigation.parsers.RecordLineParser;
import org.orekit.utils.units.Unit;

/** Parser for Klobuchar ionosphere.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class KlobucharParser extends RecordLineParser {

    /** Container for parsing data. */
    private final ParseInfo parseInfo;

    /** Container for Klobuchar message. */
    private final IonosphereKlobucharMessage message;

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public KlobucharParser(final ParseInfo parseInfo, final IonosphereKlobucharMessage message) {
        super(RecordType.ION);
        this.parseInfo = parseInfo;
        this.message   = message;
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        message.setTransmitTime(parseInfo.parseDate(message.getSystem()));
        message.setAlphaI(0, parseInfo.parseDouble2(IonosphereBaseMessage.S_PER_SC_N0));
        message.setAlphaI(1, parseInfo.parseDouble3(IonosphereBaseMessage.S_PER_SC_N1));
        message.setAlphaI(2, parseInfo.parseDouble4(IonosphereBaseMessage.S_PER_SC_N2));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        message.setAlphaI(3, parseInfo.parseDouble1(IonosphereBaseMessage.S_PER_SC_N3));
        message.setBetaI(0, parseInfo.parseDouble2(IonosphereBaseMessage.S_PER_SC_N0));
        message.setBetaI(1, parseInfo.parseDouble3(IonosphereBaseMessage.S_PER_SC_N1));
        message.setBetaI(2, parseInfo.parseDouble4(IonosphereBaseMessage.S_PER_SC_N2));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine02() {
        message.setBetaI(3, parseInfo.parseDouble1(IonosphereBaseMessage.S_PER_SC_N3));
        message.setRegionCode(parseInfo.parseDouble2(Unit.ONE) < 0.5 ?
                              RegionCode.WIDE_AREA : RegionCode.JAPAN);
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addKlobucharMessage(message);
    }

}
