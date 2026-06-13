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
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessageFactory;
import org.orekit.utils.units.Unit;

/** Parser for Beidou legacy.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public class BeidouD1D2Parser
        extends AbstractNavigationParser<BeidouLegacyNavigationMessage, BeidouLegacyNavigationMessageFactory> {

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param factory factory for navigation message
     */
    public BeidouD1D2Parser(final ParseInfo parseInfo, final BeidouLegacyNavigationMessageFactory factory) {
        super(parseInfo, factory);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouLegacyNavigationMessageFactory factory = getFactory();
        parseSvEpochSvClockLine(parseInfo.getTimeScales().getBDT(), parseInfo, factory);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        super.parseLine01();
        final BeidouLegacyNavigationMessageFactory factory = getFactory();
        factory.setAODE(getParseInfo().parseInt1());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine06() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouLegacyNavigationMessageFactory factory = getFactory();
        factory.setSvAccuracy(parseInfo.parseDouble1(Unit.METRE));
        factory.setSatH1(parseInfo.parseInt2());
        factory.setTGD1(parseInfo.parseDouble3(Unit.SECOND));
        factory.setTGD2(parseInfo.parseDouble4(Unit.SECOND));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine07() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouLegacyNavigationMessageFactory factory = getFactory();
        factory.setTransmissionTime(parseInfo.parseDouble1(Unit.SECOND));
        factory.setAODC(parseInfo.parseInt2());
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addBeidouLegacyNavigationMessage(getMessage());
    }

}
