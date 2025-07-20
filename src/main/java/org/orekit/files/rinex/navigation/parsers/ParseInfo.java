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

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.navigation.EarthOrientationParameterMessage;
import org.orekit.files.rinex.navigation.IonosphereBDGIMMessage;
import org.orekit.files.rinex.navigation.IonosphereGlonassCdmsMessage;
import org.orekit.files.rinex.navigation.IonosphereKlobucharMessage;
import org.orekit.files.rinex.navigation.IonosphereNavICKlobucharMessage;
import org.orekit.files.rinex.navigation.IonosphereNavICNeQuickNMessage;
import org.orekit.files.rinex.navigation.IonosphereNequickGMessage;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.RinexNavigationHeader;
import org.orekit.files.rinex.navigation.SystemTimeOffsetMessage;
import org.orekit.files.rinex.utils.ParsingUtils;
import org.orekit.gnss.PredefinedGnssSignal;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.analytical.gnss.data.BeidouCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GLONASSFdmaNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.NavICL1NvNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.NavICLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.QZSSCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.QZSSLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.SBASNavigationMessage;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.units.Unit;

/** Container for parsing data.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public class ParseInfo {

    /** Name of the data source. */
    private final String name;

    /** Set of time scales for parsing dates. */
    private final TimeScales timeScales;

    /** The corresponding navigation messages file object. */
    private final RinexNavigation file;

    /** Number of initial spaces in messages lines. */
    private int initialSpaces;

    /** Flag indicating header has been completely parsed. */
    private boolean headerParsed;

    /** Flag indicating the distinction between "alpha" and "beta" ionospheric coefficients. */
    private boolean isIonosphereAlphaInitialized;

    /** Satellite system line parser. */
    private SatelliteSystemLineParser systemLineParser;

    /** Current line. */
    private String line;

    /** Current global line number. */
    private int lineNumber;

    /** Current line number within the navigation message. */
    private int messageLineNumber;

    /** Container for System Time Offset message. */
    private SystemTimeOffsetMessage sto;

    /** Container for Earth Orientation Parameter message. */
    private EarthOrientationParameterMessage eop;

    /** Container for ionosphere Klobuchar message. */
    private IonosphereKlobucharMessage klobuchar;

    /** Container for NacIV Klobuchar message.
     * @since 14.0
     */
    private IonosphereNavICKlobucharMessage navICKlobuchar;

    /** Container for NacIV NeQuick N message.
     * @since 14.0
     */
    private IonosphereNavICNeQuickNMessage navICNeQuickN;

    /** Container for GLONASS CDMS message.
     * @since 14.0
     */
    private IonosphereGlonassCdmsMessage glonassCdms;

    /** Container for ionosphere Nequick-G message. */
    private IonosphereNequickGMessage neQuickG;

    /** Container for ionosphere BDGIM message. */
    private IonosphereBDGIMMessage bdgim;

    /** Constructor, build the ParseInfo object.
     * @param name name of the data source
     * @param timeScales set of time scales for parsing dates
     */
    public ParseInfo(final String name, final TimeScales timeScales) {
        // Initialize default values for fields
        this.name                         = name;
        this.timeScales                   = timeScales;
        this.isIonosphereAlphaInitialized = false;
        this.file                         = new RinexNavigation();

        // reset the default values set by header constructor
        this.file.getHeader().setProgramName(null);
        this.file.getHeader().setRunByName(null);
        this.file.getHeader().setCreationDateComponents(null);

    }

    /** Parse a date.
     * @param line line to parse
     * @param system satellite system
     * @return parsed date
     * @since 14.0
     */
    public AbsoluteDate parseDate(final String line, final SatelliteSystem system) {
        return parseDate(line, system.getObservationTimeScale().getTimeScale(timeScales));
    }

    /** Parse a date.
     * @param line line to parse
     * @param timeScale time scale
     * @return parsed date
     * @since 14.0
     */
    public AbsoluteDate parseDate(final String line, final TimeScale timeScale) {
        final int year  = ParsingUtils.parseInt(line, 4, 4);
        final int month = ParsingUtils.parseInt(line, 9, 2);
        final int day   = ParsingUtils.parseInt(line, 12, 2);
        final int hours = ParsingUtils.parseInt(line, 15, 2);
        final int min   = ParsingUtils.parseInt(line, 18, 2);
        final int sec   = ParsingUtils.parseInt(line, 21, 2);
        return new AbsoluteDate(year, month, day, hours, min, sec, timeScale);
    }

    /** Parse field 1 of a message line.
     * @param unit unit to apply
     * @return parsed field
     * @since 14.0
     */
    public double parseDouble1(final Unit unit) {
        return parseDouble(unit, initialSpaces);
    }

    /** Parse field 1 of a message line.
     * @return parsed field
     * @since 14.0
     */
    public int parseInt1() {
        return parseInt(initialSpaces);
    }

    /** Parse field 2 of a message line.
     * @param unit unit to apply
     * @return parsed field
     * @since 14.0
     */
    public double parseDouble2(final Unit unit) {
        return parseDouble(unit, initialSpaces + 19);
    }

    /** Parse field 2 of a message line.
     * @return parsed field
     * @since 14.0
     */
    public int parseInt2() {
        return parseInt(initialSpaces + 19);
    }

    /** Parse field 3 of a message line.
     * @param unit unit to apply
     * @return parsed field
     * @since 14.0
     */
    public double parseDouble3(final Unit unit) {
        return parseDouble(unit, initialSpaces + 38);
    }

    /** Parse field 3 of a message line.
     * @return parsed field
     * @since 14.0
     */
    public int parseInt3() {
        return parseInt(initialSpaces + 38);
    }

    /** Parse field 4 of a message line.
     * @param unit unit to apply
     * @return parsed field
     * @since 14.0
     */
    public double parseDouble4(final Unit unit) {
        return parseDouble(unit, initialSpaces + 57);
    }

    /** Parse field 4 of a message line.
     * @return parsed field
     * @since 14.0
     */
    public int parseInt4() {
        return parseInt(initialSpaces + 57);
    }

    /** Parse field n of a message line.
     * @param unit unit to apply
     * @param index index of first field character
     * @return parsed field
     */
    private double parseDouble(final Unit unit, final int index) {
        return unit.toSI(ParsingUtils.parseDouble(line, index, 19));
    }

    /** Parse field n of a message line.
     * @param index index of first field character
     * @return parsed field
     */
    private int parseInt(final int index) {
        return (int) FastMath.rint(ParsingUtils.parseDouble(line, index, 19));
    }

    /** Parse a comment.
     */
    public void parseComment() {
        ParsingUtils.parseComment(lineNumber, line, file);
    }

    /** Ensure navigation message has been closed.
     */
    public void closePendingMessage() {
        if (systemLineParser != null) {
            systemLineParser.closeMessage(file);
            systemLineParser = null;
        }
    }

    /** Get the file name.
     * @return file name
     */
    public String getName() {
        return name;
    }

    /** Get the time scales.
     * @return time scales
     */
    public TimeScales getTimeScales() {
        return timeScales;
    }

    /** Get the completed file.
     * @return completed file
     */
    public RinexNavigation getCompletedFile() {

        // check the header has been properly parsed
        if (!headerParsed) {
            throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE, name);
        }

        // close the last message
        closePendingMessage();

        return file;

    }

    /** Get the navigation file header.
     * @return navigation file header
     */
    public RinexNavigationHeader getHeader() {
        return file.getHeader();
    }

    /** Set the header parsing indicator.
     * @param headerParsed if true, header has been parsed
     */
    public void setHeaderParsed(final boolean headerParsed) {
        this.headerParsed = headerParsed;
    }

    /** Set the number of initial spaces in messages lines.
     * @param initialSpaces number of initial spaces in messages lines
     */
    public void setInitialSpaces(final int initialSpaces) {
        this.initialSpaces = initialSpaces;
    }

    /** Get the current line.
     * @return current line
     */
    public String getLine() {
        return line;
    }

    /** set the current line.
     * @param line current line
     */
    public void setLine(final String line) {
        this.line = line;
        ++lineNumber;
    }

    /** Get the line number.
     * @return line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /** Get the line number within the navigation message.
     * @return line number within the navigation message
     */
    public int getMessageLineNumber() {
        return messageLineNumber;
    }

    /** Set the system line parser.
     * @param system satellite system
     * @param type message type
     */
    public void setSystemLineParser(final SatelliteSystem system, final String type) {

        // Set the line number to 0
        messageLineNumber = 0;

        closePendingMessage();

        switch (system) {
            case GPS:
                if (type == null || type.equals(GPSLegacyNavigationMessage.LNAV)) {
                    // in Rinex, week number is aligned to GPS week!
                    systemLineParser = new GPSLnavParser(this,
                                                         new GPSLegacyNavigationMessage(timeScales,
                                                                                        SatelliteSystem.GPS,
                                                                                        GPSLegacyNavigationMessage.LNAV));
                } else if (type.equals(GPSCivilianNavigationMessage.CNAV)) {
                    // in Rinex, week number is aligned to GPS week!
                    systemLineParser = new GPSCnavParser(this,
                                                         new GPSCivilianNavigationMessage(false,
                                                                                          timeScales,
                                                                                          SatelliteSystem.GPS,
                                                                                          GPSCivilianNavigationMessage.CNAV));
                } else if (type.equals(GPSCivilianNavigationMessage.CNV2)) {
                    // in Rinex, week number is aligned to GPS week!
                    systemLineParser = new GPSCnavParser(this,
                                                         new GPSCivilianNavigationMessage(true,
                                                                                          timeScales,
                                                                                          SatelliteSystem.GPS,
                                                                                          GPSCivilianNavigationMessage.CNV2));
                }
                break;
            case GALILEO:
                if (type == null || type.equals(GalileoNavigationMessage.INAV) || type.equals(
                    GalileoNavigationMessage.FNAV)) {
                    // in Rinex, week number is aligned to GPS week!
                    systemLineParser = new GalileoParser(this, new GalileoNavigationMessage(timeScales,
                                                                                            SatelliteSystem.GPS,
                                                                                            type));
                }
                break;
            case GLONASS:
                if (type == null || type.equals("FDMA")) {
                    systemLineParser = new GlonassFdmaParser(this, new GLONASSFdmaNavigationMessage());
                } else if (type.equals("L1OC") || type.equals("L3OC")) {
                    systemLineParser = new GlonassCdmaParser(this);
                }
                break;
            case QZSS:
                if (type == null || type.equals(QZSSLegacyNavigationMessage.LNAV)) {
                    // in Rinex, week number is aligned to GPS week!
                    systemLineParser = new QzssLnavParser(this,
                                                          new QZSSLegacyNavigationMessage(timeScales,
                                                                                          SatelliteSystem.GPS,
                                                                                          QZSSLegacyNavigationMessage.LNAV));
                } else if (type.equals(QZSSCivilianNavigationMessage.CNAV)) {
                    // in Rinex, week number is aligned to GPS week!
                    systemLineParser = new QzssCnavParser(this,
                                                          new QZSSCivilianNavigationMessage(false,
                                                                                            timeScales,
                                                                                            SatelliteSystem.GPS,
                                                                                            QZSSCivilianNavigationMessage.CNAV));
                } else if (type.equals(QZSSCivilianNavigationMessage.CNV2)) {
                    // in Rinex, week number is aligned to GPS week!
                    systemLineParser = new QzssCnavParser(this,
                                                          new QZSSCivilianNavigationMessage(true,
                                                                                            timeScales,
                                                                                            SatelliteSystem.GPS,
                                                                                            QZSSCivilianNavigationMessage.CNV2));
                }
                break;
            case BEIDOU:
                if (type == null || type.equals(BeidouLegacyNavigationMessage.D1) || type.equals(
                    BeidouLegacyNavigationMessage.D2)) {
                    // in Rinex, week number for Beidou is really aligned to Beidou week!
                    systemLineParser = new BeidouD1D2Parser(this,
                                                            new BeidouLegacyNavigationMessage(timeScales,
                                                                                              SatelliteSystem.BEIDOU,
                                                                                              type));
                } else if (type.equals(BeidouCivilianNavigationMessage.CNV1)) {
                    // in Rinex, week number for Beidou is really aligned to Beidou week!
                    systemLineParser = new BeidouCnv123Parser(this,
                                                              new BeidouCivilianNavigationMessage(PredefinedGnssSignal.B1C,
                                                                                                  timeScales,
                                                                                                  SatelliteSystem.BEIDOU,
                                                                                                  BeidouCivilianNavigationMessage.CNV1));
                } else if (type.equals(BeidouCivilianNavigationMessage.CNV2)) {
                    // in Rinex, week number for Beidou is really aligned to Beidou week!
                    systemLineParser = new BeidouCnv123Parser(this,
                                                              new BeidouCivilianNavigationMessage(PredefinedGnssSignal.B2A,
                                                                                                  timeScales,
                                                                                                  SatelliteSystem.BEIDOU,
                                                                                                  BeidouCivilianNavigationMessage.CNV2));
                } else if (type.equals(BeidouCivilianNavigationMessage.CNV3)) {
                    // in Rinex, week number for Beidou is really aligned to Beidou week!
                    systemLineParser = new BeidouCnv123Parser(this,
                                                              new BeidouCivilianNavigationMessage(PredefinedGnssSignal.B2B,
                                                                                                  timeScales,
                                                                                                  SatelliteSystem.BEIDOU,
                                                                                                  BeidouCivilianNavigationMessage.CNV3));
                }
                break;
            case NAVIC:
                if (type == null || type.equals(NavICLegacyNavigationMessage.LNAV)) {
                    // in Rinex, week number is aligned to GPS week!
                    systemLineParser = new NavICLnavParser(this,
                                                           new NavICLegacyNavigationMessage(timeScales,
                                                                                            SatelliteSystem.GPS,
                                                                                            NavICLegacyNavigationMessage.LNAV));
                } else if (type.equals(NavICL1NvNavigationMessage.L1NV)) {
                    // in Rinex, week number is aligned to GPS week!
                    systemLineParser = new NavICL1NvParser(this,
                                                           new NavICL1NvNavigationMessage(timeScales,
                                                                                          SatelliteSystem.GPS,
                                                                                          NavICL1NvNavigationMessage.L1NV));
                }
                break;
            case SBAS:
                if (type == null || type.equals("SBAS")) {
                    systemLineParser = new SbasParser(this, new SBASNavigationMessage());
                }
                break;
            default:
                // do nothing, handle error after the switch
        }

        if (systemLineParser == null) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, name, line);
        }

    }

    /** Get the system line parser.
     * @return system line parser
     */
    public SatelliteSystemLineParser getSystemLineParser() {
        return systemLineParser;
    }

    /** Parse next message line.
     */
    public void parseMessageLine() {
        switch (++messageLineNumber) {
            case 1: systemLineParser.parseFirstBroadcastOrbit();
            break;
            case 2: systemLineParser.parseSecondBroadcastOrbit();
            break;
            case 3: systemLineParser.parseThirdBroadcastOrbit();
            break;
            case 4: systemLineParser.parseFourthBroadcastOrbit();
            break;
            case 5: systemLineParser.parseFifthBroadcastOrbit();
            break;
            case 6: systemLineParser.parseSixthBroadcastOrbit();
            break;
            case 7: systemLineParser.parseSeventhBroadcastOrbit();
            break;
            case 8: systemLineParser.parseEighthBroadcastOrbit();
            break;
            case 9: systemLineParser.parseNinthBroadcastOrbit();
            break;
            default:
                // this should never happen
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
        }
    }

    /**
     * Set the "alpha" ionspheric parameters.
     * @param klobucharAlpha the "alpha" ionspheric parameters to set
     */
    public void setKlobucharAlpha(final double[] klobucharAlpha) {
        file.setKlobucharAlpha(klobucharAlpha);
        isIonosphereAlphaInitialized = true;
    }

    /** Check if ionosphere alpha coefficients have been parsed.
     * @return true if ionosphere alpha coefficients have been parsed
     */
    public boolean isIonosphereAlphaInitialized() {
        return isIonosphereAlphaInitialized;
    }

    /**
     * Set the "beta" ionospheric parameters.
     * @param klobucharBeta the "beta" ionospheric parameters to set
     */
    public void setKlobucharBeta(final double[] klobucharBeta) {
        file.setKlobucharBeta(klobucharBeta);
    }

    /**
     * Set the "alpha" ionospheric parameters.
     * @param neQuickAlpha the "alpha" ionospheric parameters to set
     */
    public void setNeQuickAlpha(final double[] neQuickAlpha) {
        file.setNeQuickAlpha(neQuickAlpha);
    }

    /** Get container for System Time Offset message.
     * @return container for System Time Offset message
     */
    public SystemTimeOffsetMessage getSto() {
        return sto;
    }

    /** Set container for System Time Offset message.
     * @param sto container for System Time Offset message
     */
    public void setSto(final SystemTimeOffsetMessage sto) {
        closePendingMessage();
        this.sto = sto;
    }

    /** Finish System Time Offset message.
     */
    public void finishSto() {
        file.addSystemTimeOffset(sto);
        this.sto = null;
    }

    /** Get container for Earth Orientation Parameter message.
     * @return container for Earth Orientation Parameter message
     */
    public EarthOrientationParameterMessage getEop() {
        return eop;
    }

    /** Set container for Earth Orientation Parameter message.
     * @param eop container for Earth Orientation Parameter message
     */
    public void setEop(final EarthOrientationParameterMessage eop) {
        closePendingMessage();
        this.eop = eop;
    }

    /** Finish Earth Orientation Parameter message.
     */
    public void finishEop() {
        file.addEarthOrientationParameter(eop);
        this.eop = null;
    }

    /** Get container for Klobuchar ionosphere message.
     * @return container for Klobuchar ionosphere message
     */
    public IonosphereKlobucharMessage getKlobuchar() {
        return klobuchar;
    }

    /** Set container for Klobuchar ionosphere message.
     * @param klobuchar container for Klobuchar ionosphere message
     */
    public void setKlobuchar(final IonosphereKlobucharMessage klobuchar) {
        closePendingMessage();
        this.klobuchar = klobuchar;
    }

    /** Finish Klobuchar ionosphere message.
     */
    public void finishKlobuchar() {
        file.addKlobucharMessage(klobuchar);
        this.klobuchar = null;
    }

    /** Get container for NavIC Klobuchar ionosphere message.
     * @return container for NavIC Klobuchar message
     */
    public IonosphereNavICKlobucharMessage getNavICKlobuchar() {
        return navICKlobuchar;
    }

    /** Set container for NavIC Klobuchar ionosphere message.
     * @param navICKlobuchar container for NavIC Klobuchar message
     */
    public void setNavICKlobuchar(final IonosphereNavICKlobucharMessage navICKlobuchar) {
        closePendingMessage();
        this.navICKlobuchar = navICKlobuchar;
    }

    /** Finish NavIC Klobuchar ionosphere message.
     */
    public void finishNavICKlobuchar() {
        file.addNavICKlobucharMessage(navICKlobuchar);
        navICKlobuchar = null;
    }

    /** Get container for NavIC NeQuick N ionosphere message.
     * @return container for NavIC NeQuick N ionosphere message
     */
    public IonosphereNavICNeQuickNMessage getNavICNeQuickN() {
        return navICNeQuickN;
    }

    /** Set container for NavIC NeQuick N ionosphere message.
     * @param navICNeQuickN container for NavIC NeQuick N ionosphere message
     */
    public void setNavICNeQuickN(final IonosphereNavICNeQuickNMessage navICNeQuickN) {
        closePendingMessage();
        this.navICNeQuickN = navICNeQuickN;
    }

    /** Finish NavIC NeQuick N ionosphere message.
     */
    public void finishNavICNeQuickN() {
        file.addNavICNeQuickNMessage(navICNeQuickN);
        navICNeQuickN = null;
    }

    /** Get container for GLONASS CDMS ionosphere message.
     * @return container for GLONASS CDMS ionosphere message
     */
    public IonosphereGlonassCdmsMessage getGlonassCdms() {
        return glonassCdms;
    }

    /** Set container for GLONASS CDMS ionosphere message.
     * @param glonassCdms container for GLONASS CDMS ionosphere message
     */
    public void setGlonassCdms(final IonosphereGlonassCdmsMessage glonassCdms) {
        closePendingMessage();
        this.glonassCdms = glonassCdms;
    }

    /** finish GLONASS CDMS ionosphere message.
     */
    public void finishGlonassCdms() {
        file.addGlonassCDMSMessage(glonassCdms);
        navICNeQuickN = null;
    }

    /** Get container for NeQuick G ionosphere message.
     * @return container for NeQuick G ionosphere message
     */
    public IonosphereNequickGMessage getNeQuickG() {
        return neQuickG;
    }

    /** Set container for NeQuick G ionosphere message.
     * @param neQuickG container for NeQuick G ionosphere message
     */
    public void setNeQuickG(final IonosphereNequickGMessage neQuickG) {
        closePendingMessage();
        this.neQuickG = neQuickG;
    }

    /** Finish NeQuick G ionosphere message.
     */
    public void finishNequickG() {
        file.addNequickGMessage(neQuickG);
        neQuickG = null;
    }

    /** Get container for BDGIM ionosphere message.
     * @return container for BDGIM ionosphere message
     */
    public IonosphereBDGIMMessage getBdgim() {
        return bdgim;
    }

    /** Set container for BDGIM ionosphere message.
     * @param bdgim container for BDGIM ionosphere message
     */
    public void setBdgim(final IonosphereBDGIMMessage bdgim) {
        closePendingMessage();
        this.bdgim = bdgim;
    }

    /** Finish BDGIM ionosphere message.
     */
    public void finishBdgim() {
        file.addBDGIMMessage(bdgim);
        bdgim = null;
    }

}
