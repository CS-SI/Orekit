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

import org.orekit.files.rinex.navigation.IonosphereAij;
import org.orekit.files.rinex.navigation.IonosphereNavICNeQuickNMessage;
import org.orekit.files.rinex.navigation.RegionalAij;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.files.rinex.navigation.parsers.RecordLineParser;
import org.orekit.utils.units.Unit;

/** Parser for NavIC NeQuick N ionosphere.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class NavICNeQuickNParser
    extends RecordLineParser {

    /** Container for parsing data. */
    private final ParseInfo parseInfo;

    /** Container for NavIC NeQuick N message. */
    private final IonosphereNavICNeQuickNMessage message;

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public NavICNeQuickNParser(final ParseInfo parseInfo, final IonosphereNavICNeQuickNMessage message) {
        this.parseInfo = parseInfo;
        this.message   = message;
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        message.setTransmitTime(parseInfo.parseDate(message.getSystem()));
        message.setIOD(parseInfo.parseDouble2(Unit.ONE));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        parseAij(message.getRegion1());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine02() {
        parseBoundaries(message.getRegion1());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine03() {
        parseAij(message.getRegion2());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine04() {
        parseBoundaries(message.getRegion2());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine05() {
        parseAij(message.getRegion3());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine06() {
        parseBoundaries(message.getRegion3());
        parseInfo.closePendingRecord();
    }

    /** Parse the aᵢⱼ coefficients for a region.
     * @param region region to parse
     */
    private void parseAij(final RegionalAij region) {
        region.setAi0(parseInfo.parseDouble1(IonosphereAij.SFU));
        region.setAi1(parseInfo.parseDouble2(IonosphereAij.SFU_PER_DEG));
        region.setAi2(parseInfo.parseDouble3(IonosphereAij.SFU_PER_DEG2));
        region.setIDF(parseInfo.parseDouble4(Unit.ONE));

    }

    /** Parse the the boundaries of a region.
     * @param region region to parse
     */
    private void parseBoundaries(final RegionalAij region) {
        region.setLonMin(parseInfo.parseDouble1(Unit.DEGREE));
        region.setLonMax(parseInfo.parseDouble2(Unit.DEGREE));
        region.setModipMin(parseInfo.parseDouble3(Unit.DEGREE));
        region.setModipMax(parseInfo.parseDouble4(Unit.DEGREE));
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addNavICNeQuickNMessage(message);
    }

}
