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

import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.files.rinex.navigation.parsers.RecordLineParser;
import org.orekit.propagation.analytical.gnss.data.AbstractNavigationMessage;
import org.orekit.utils.units.Unit;

/** Parser for abstract navigation messages.
 * @param <T> type of the navigation message
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class AbstractNavigationParser<T extends AbstractNavigationMessage<T>> extends RecordLineParser {

    /** Container for parsing data. */
    private final ParseInfo parseInfo;

    /** Container for navigation message. */
    private final T message;

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    protected AbstractNavigationParser(final ParseInfo parseInfo, final T message) {
        this.parseInfo = parseInfo;
        this.message   = message;
    }

    /** Get the container for parsing data.
     * @return container for parsing data
     */
    public ParseInfo getParseInfo() {
        return parseInfo;
    }

    /** Get the container for the navigation message.
     * @return container for the navigation message
     */
    public T getMessage() {
        return message;
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        if (parseInfo.getHeader().getFormatVersion() < 3.0) {
            parseSvEpochSvClockLineRinex2(parseInfo.getLine(), parseInfo.getTimeScales().getGPS(), message);
        } else {
            parseSvEpochSvClockLine(parseInfo.getTimeScales().getGPS(), parseInfo, message);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
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
        message.setWeek(parseInfo.parseInt3());
    }

}
