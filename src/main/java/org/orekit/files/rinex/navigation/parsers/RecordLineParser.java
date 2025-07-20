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
package org.orekit.files.rinex.navigation.parsers;

import org.orekit.errors.OrekitInternalError;
import org.orekit.files.rinex.navigation.RecordType;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.utils.ParsingUtils;
import org.orekit.propagation.analytical.gnss.data.AbstractNavigationMessage;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.units.Unit;

/** Base class for record parsers.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 */
public abstract class RecordLineParser {

    /** Type of the record being parsed. */
    private final RecordType type;

    /** Simple constructor.
     * @param type type of the record being parsed
     */
    protected RecordLineParser(final RecordType type) {
        this.type = type;
    }

    /** Get the type of the record being parsed.
     * @return type of the record being parsed
     */
    public RecordType getType() {
        return type;
    }

    /** Parse the SV/Epoch/Sv clock of the navigation message.
     * @param line      line to read
     * @param timeScale time scale to use
     * @param message   navigation message
     */
    protected void parseSvEpochSvClockLineRinex2(final String line, final TimeScale timeScale,
                                                 final AbstractNavigationMessage<?> message) {
        // PRN
        message.setPRN(ParsingUtils.parseInt(line, 0, 2));

        // Toc
        final int    year  = ParsingUtils.convert2DigitsYear(ParsingUtils.parseInt(line, 2, 3));
        final int    month = ParsingUtils.parseInt(line,     5, 3);
        final int    day   = ParsingUtils.parseInt(line,     8, 3);
        final int    hours = ParsingUtils.parseInt(line,    11, 3);
        final int    min   = ParsingUtils.parseInt(line,    14, 3);
        final double sec   = ParsingUtils.parseDouble(line, 17, 5);
        message.setEpochToc(new AbsoluteDate(year, month, day, hours, min, sec, timeScale));

        // clock
        message.setAf0(ParsingUtils.parseDouble(line, 22, 19));
        message.setAf1(ParsingUtils.parseDouble(line, 41, 19));
        message.setAf2(ParsingUtils.parseDouble(line, 60, 19));

    }

    /**
     * Parse the SV/Epoch/Sv clock of the navigation message.
     *
     * @param timeScale time scale to use
     * @param parseInfo container for parsing info
     * @param message   navigation message
     */
    protected void parseSvEpochSvClockLine(final TimeScale timeScale,
                                           final ParseInfo parseInfo, final AbstractNavigationMessage<?> message) {
        // PRN
        message.setPRN(ParsingUtils.parseInt(parseInfo.getLine(), 1, 2));

        // Toc
        message.setEpochToc(parseInfo.parseDate(timeScale));

        // clock
        message.setAf0(parseInfo.parseDouble2(Unit.SECOND));
        message.setAf1(parseInfo.parseDouble3(RinexNavigationParser.S_PER_S));
        message.setAf2(parseInfo.parseDouble4(RinexNavigationParser.S_PER_S2));

    }

    /** Parse line 0 of the navigation record.
     */
    public abstract void parseLine00();

    /** Parse line 1 of the navigation record.
     */
    public void parseLine01() {
        // this should never be called (except by some tests)
        throw new OrekitInternalError(null);
    }

    /** Parse line 2 of the navigation record.
     */
    public void parseLine02() {
        // this should never be called (except by some tests)
        throw new OrekitInternalError(null);
    }

    /** Parse line 3 of the navigation record.
     */
    public void parseLine03() {
        // this should never be called (except by some tests)
        throw new OrekitInternalError(null);
    }

    /** Parse line 4 of the navigation record.
     */
    public void parseLine04() {
        // this should never be called (except by some tests)
        throw new OrekitInternalError(null);
    }

    /** Parse line 5 of the navigation record.
     */
    public void parseLine05() {
        // this should never be called (except by some tests)
        throw new OrekitInternalError(null);
    }

    /** Parse line 6 of the navigation record.
     */
    public void parseLine06() {
        // this should never be called (except by some tests)
        throw new OrekitInternalError(null);
    }

    /** Parse line 7 of the navigation record.
     */
    public void parseLine07() {
        // this should never be called (except by some tests)
        throw new OrekitInternalError(null);
    }

    /** Parse line 8 of the navigation record.
     */
    public void parseLine08() {
        // this should never be called (except by some tests)
        throw new OrekitInternalError(null);
    }

    /** Parse line 9 of the navigation record.
     */
    public void parseLine09() {
        // this should never be called (except by some tests)
        throw new OrekitInternalError(null);
    }

    /** Close a record as the last line was parsed.
     * @param file navigation file where to put the completed record
     */
    public abstract void closeRecord(RinexNavigation file);

    /** Calculates the floating-point remainder of a / b.
     * <p>
     * fmod = a - x * b where x = (int) a / b
     * </p>
     *
     * @param a numerator
     * @param b denominator
     * @return the floating-point remainder of a / b
     */
    protected static double fmod(final double a, final double b) {
        final double x = (int) (a / b);
        return a - x * b;
    }

}
