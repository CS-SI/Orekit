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

import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.files.rinex.navigation.parsers.RecordLineParser;
import org.orekit.propagation.analytical.gnss.data.AbstractNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.AbstractNavigationMessageFactory;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElementsFactory;
import org.orekit.time.GNSSDate;
import org.orekit.utils.units.Unit;

/** Parser for abstract navigation messages.
 * @param <T> type of the navigation message
 * @param <F> type of the navigation message factory
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class AbstractNavigationParser<T extends AbstractNavigationMessage<T>,
                                               F extends AbstractNavigationMessageFactory<T>>
    extends RecordLineParser {

    /** Container for parsing data. */
    private final ParseInfo parseInfo;

    /** Factory for navigation factory. */
    private final F factory;

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param factory factory for navigation message
     */
    protected AbstractNavigationParser(final ParseInfo parseInfo, final F factory) {
        this.parseInfo = parseInfo;
        this.factory   = factory;
    }

    /** Get the factory.
     * @return factory
     */
    protected F getFactory() {
        return factory;
    }

    /** Get the container for parsing data.
     * @return container for parsing data
     */
    public ParseInfo getParseInfo() {
        return parseInfo;
    }

    /** Get the container for the navigation factory.
     * @return container for the navigation message
     */
    public T getMessage() {
        return factory.createFromDrivers();
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        if (parseInfo.getHeader().getFormatVersion() < 3.0) {
            parseSvEpochSvClockLineRinex2(parseInfo.getLine(), parseInfo.getTimeScales().getGPS(), factory);
        } else {
            parseSvEpochSvClockLine(parseInfo.getTimeScales().getGPS(), parseInfo, factory);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        factory.getCrsDriver().setValue(parseInfo.parseDouble2(Unit.METRE));
        factory.getDeltaN0Driver().setValue(parseInfo.parseDouble3(RinexNavigationParser.RAD_PER_S));
        factory.
            getOrbitalParametersDrivers().
            findByName(GNSSOrbitalElementsFactory.MEAN_ANOMALY).
            setValue(parseInfo.parseDouble4(Unit.RADIAN));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine02() {
        factory.getCucDriver().setValue(parseInfo.parseDouble1(Unit.RADIAN));
        factory.
            getOrbitalParametersDrivers().
            findByName(GNSSOrbitalElementsFactory.ECCENTRICITY).
            setValue(parseInfo.parseDouble2(Unit.NONE));
        factory.getCusDriver().setValue(parseInfo.parseDouble3(Unit.RADIAN));
        final double sqrtA = parseInfo.parseDouble4(RinexNavigationParser.SQRT_M);
        factory.
            getOrbitalParametersDrivers().
            findByName(GNSSOrbitalElementsFactory.SEMI_MAJOR_AXIS).
            setValue(sqrtA * sqrtA);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine03() {
        factory.getTimeDriver().setValue(parseInfo.parseDouble1(Unit.SECOND));
        factory.getCicDriver().setValue(parseInfo.parseDouble2(Unit.RADIAN));
        factory.
            getOrbitalParametersDrivers().
            findByName(GNSSOrbitalElementsFactory.NODE_LONGITUDE).
            setValue(parseInfo.parseDouble3(Unit.RADIAN));
        factory.getCisDriver().setValue(parseInfo.parseDouble4(Unit.RADIAN));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine04() {
        factory.
            getOrbitalParametersDrivers().
            findByName(GNSSOrbitalElementsFactory.INCLINATION).
            setValue(parseInfo.parseDouble1(Unit.RADIAN));
        factory.getCrcDriver().setValue(parseInfo.parseDouble2(Unit.METRE));
        factory.
            getOrbitalParametersDrivers().
            findByName(GNSSOrbitalElementsFactory.ARGUMENT_OF_PERIGEE).
            setValue(parseInfo.parseDouble3(Unit.RADIAN));
        factory.getOmegaDotDriver().setValue(parseInfo.parseDouble4(RinexNavigationParser.RAD_PER_S));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine05() {
        factory.getIDotDriver().setValue(parseInfo.parseDouble1(RinexNavigationParser.RAD_PER_S));
        factory.setTimeOfEphemeris(new GNSSDate(parseInfo.parseInt3(), factory.getTimeDriver().getValue(),
                                                factory.getSystem()));
    }

}
