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

import org.orekit.files.rinex.navigation.IonosphereBDGIMMessage;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.parsers.RecordLineParser;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;

/** Parser for BDGIM ionosphere.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class BdgimParser extends RecordLineParser {

    /** Container for parsing data. */
    private final ParseInfo parseInfo;

    /** Container for BDGIM message. */
    private final IonosphereBDGIMMessage message;

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public BdgimParser(final ParseInfo parseInfo, final IonosphereBDGIMMessage message) {
        this.parseInfo = parseInfo;
        this.message   = message;
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        message.setTransmitTime(parseInfo.parseDate(message.getSystem()));
        message.setAlphaI(0, parseInfo.parseDouble2(RinexNavigationParser.TEC));
        message.setAlphaI(1, parseInfo.parseDouble3(RinexNavigationParser.TEC));
        message.setAlphaI(2, parseInfo.parseDouble4(RinexNavigationParser.TEC));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        message.setAlphaI(3, parseInfo.parseDouble1(RinexNavigationParser.TEC));
        message.setAlphaI(4, parseInfo.parseDouble2(RinexNavigationParser.TEC));
        message.setAlphaI(5, parseInfo.parseDouble3(RinexNavigationParser.TEC));
        message.setAlphaI(6, parseInfo.parseDouble4(RinexNavigationParser.TEC));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine02() {
        message.setAlphaI(7, parseInfo.parseDouble1(RinexNavigationParser.TEC));
        message.setAlphaI(8, parseInfo.parseDouble2(RinexNavigationParser.TEC));
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addBDGIMMessage(message);
    }

}
