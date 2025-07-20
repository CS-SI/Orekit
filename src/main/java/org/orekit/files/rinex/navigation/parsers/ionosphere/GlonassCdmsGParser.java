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

import org.orekit.files.rinex.navigation.IonosphereGlonassCdmsMessage;
import org.orekit.files.rinex.navigation.RecordType;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.files.rinex.navigation.parsers.RecordLineParser;
import org.orekit.utils.units.Unit;

/** Parser for GLONASS CDMS ionosphere.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class GlonassCdmsGParser extends RecordLineParser {

    /** Container for parsing data. */
    private final ParseInfo parseInfo;

    /** Container for GLONASS CDMS message. */
    private final IonosphereGlonassCdmsMessage message;

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public GlonassCdmsGParser(final ParseInfo parseInfo, final IonosphereGlonassCdmsMessage message) {
        super(RecordType.ION);
        this.parseInfo = parseInfo;
        this.message   = message;
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        message.setTransmitTime(parseInfo.parseDate(message.getSystem()));
        message.setCA(parseInfo.parseDouble2(Unit.ONE));
        message.setCF107(parseInfo.parseDouble3(Unit.ONE));
        message.setCAP(parseInfo.parseDouble4(  Unit.ONE));
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addGlonassCDMSMessage(message);
    }

}
