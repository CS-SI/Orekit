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
import org.orekit.files.rinex.navigation.RecordType;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.RinexNavigationHeader;
import org.orekit.files.rinex.navigation.SystemTimeOffsetMessage;
import org.orekit.files.rinex.navigation.parsers.ephemeris.BeidouCnv123Parser;
import org.orekit.files.rinex.navigation.parsers.ephemeris.BeidouD1D2Parser;
import org.orekit.files.rinex.navigation.parsers.ephemeris.GPSCnavParser;
import org.orekit.files.rinex.navigation.parsers.ephemeris.GPSLnavParser;
import org.orekit.files.rinex.navigation.parsers.ephemeris.GalileoParser;
import org.orekit.files.rinex.navigation.parsers.ephemeris.GlonassCdmaParser;
import org.orekit.files.rinex.navigation.parsers.ephemeris.GlonassFdmaParser;
import org.orekit.files.rinex.navigation.parsers.ephemeris.NavICL1NvParser;
import org.orekit.files.rinex.navigation.parsers.ephemeris.NavICLnavParser;
import org.orekit.files.rinex.navigation.parsers.ephemeris.QzssCnavParser;
import org.orekit.files.rinex.navigation.parsers.ephemeris.QzssLnavParser;
import org.orekit.files.rinex.navigation.parsers.ephemeris.SbasParser;
import org.orekit.files.rinex.navigation.parsers.ionosphere.BdgimParser;
import org.orekit.files.rinex.navigation.parsers.ionosphere.GlonassCdmsGParser;
import org.orekit.files.rinex.navigation.parsers.ionosphere.KlobucharParser;
import org.orekit.files.rinex.navigation.parsers.ionosphere.NavICKlobucharParser;
import org.orekit.files.rinex.navigation.parsers.ionosphere.NavICNeQuickNParser;
import org.orekit.files.rinex.navigation.parsers.ionosphere.NeQuickGParser;
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

    /** Record line parser. */
    private RecordLineParser recordLineParser;

    /** Current line. */
    private String line;

    /** Current global line number. */
    private int lineNumber;

    /** Current line number within the navigation message. */
    private int recordLineNumber;

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
     * @param system satellite system
     * @return parsed date
     * @since 14.0
     */
    public AbsoluteDate parseDate(final SatelliteSystem system) {
        return parseDate(system.getObservationTimeScale().getTimeScale(timeScales));
    }

    /** Parse a date.
     * @param timeScale time scale
     * @return parsed date
     * @since 14.0
     */
    public AbsoluteDate parseDate(final TimeScale timeScale) {
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

    /** Ensure navigation record has been closed.
     */
    public void closePendingRecord() {
        if (recordLineParser != null) {
            recordLineParser.closeRecord(file);
            recordLineParser = null;
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
        closePendingRecord();

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

    /** Get the line number within the navigation record.
     * @return line number within the navigation record
     */
    public int getRecordLineNumber() {
        return recordLineNumber;
    }

    /** Set the record line parser.
     * @param recordType retord type
     */
    public void setRecordLineParser(final RecordType recordType) {
        final SatelliteSystem system  = SatelliteSystem.parseSatelliteSystem(ParsingUtils.parseString(line, 6, 1));
        final int             prn     = ParsingUtils.parseInt(line, 7, 2);
        final String          type    = ParsingUtils.parseString(line, 10, 4);
        final String          subtype = ParsingUtils.parseString(line, 15, 4);
        setRecordLineParser(recordType, system, prn, type, subtype);
    }

    /** Set the record line parser.
     * @param recordType retord type
     * @param system satellite system
     * @param prn satellite number
     * @param messageType message type
     * @param subType subtype
     */
    public void setRecordLineParser(final RecordType recordType,
                                    final SatelliteSystem system, final int prn,
                                    final String messageType, final String subType) {

        // Set the line number to 0
        recordLineNumber = 0;

        closePendingRecord();

        recordLineParser = null;
        switch (recordType) {
            case STO:
                recordLineParser = buildStoRecordLineParser(system, prn, messageType, subType);
                break;
            case EOP:
                recordLineParser = buildEopRecordLineParser(system, prn, messageType, subType);
                break;
            case ION:
                recordLineParser = buildIonRecordLineParser(system, prn, messageType, subType);
                break;
            case ORBIT :
                recordLineParser = buildEphRecordLineParser(system, messageType);
                break;
            default :
                // do nothing, handle error after the switch
        }

        if (recordLineParser == null) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, name, line);
        }

    }

    /** Build a line parser for system time offset record.
     * @param system satellite system
     * @param prn satellite number
     * @param messageType message type
     * @param subType subtype
     * @return built record container
     */
    private RecordLineParser buildStoRecordLineParser(final SatelliteSystem system, final int prn,
                                                      final String messageType, final String subType) {
        return new SystemTimeOffsetParser(this, new SystemTimeOffsetMessage(system, prn,
                                                                            messageType, subType));
    }

    /** Build a line parser for Earth Orientation Parameter record.
     * @param system satellite system
     * @param prn satellite number
     * @param messageType message type
     * @param subType subtype
     * @return built record container
     */
    private RecordLineParser buildEopRecordLineParser(final SatelliteSystem system, final int prn,
                                                      final String messageType, final String subType) {
        return new EarthOrientationParameterParser(this, new EarthOrientationParameterMessage(system, prn,
                                                                                              messageType, subType));
    }

    /** Build a line parser for ionosphere record.
     * @param system satellite system
     * @param prn satellite number
     * @param messageType message type
     * @param subType subtype
     * @return built record container
     */
    private RecordLineParser buildIonRecordLineParser(final SatelliteSystem system, final int prn,
                                                      final String messageType, final String subType) {
        if (system == SatelliteSystem.GALILEO) {
            return new NeQuickGParser(this, new IonosphereNequickGMessage(system, prn, messageType, subType));
        } else if (system == SatelliteSystem.BEIDOU && "CNVX".equals(messageType)) {
            // in Rinex 4.00, tables A32 and A34 (A35 and A37 in Rinex 4.02) are ambiguous
            // as both seem to apply to Beidou CNVX messages; we consider BDGIM is the
            // proper model in this case
            return new BdgimParser(this, new IonosphereBDGIMMessage(system, prn, messageType, subType));
        } else if (system == SatelliteSystem.NAVIC &&
                   NavICL1NvNavigationMessage.L1NV.equals(messageType) &&
                   "KLOB".equals(subType)) {
            return new NavICKlobucharParser(this, new IonosphereNavICKlobucharMessage(system, prn, messageType, subType));
        } else if (system == SatelliteSystem.NAVIC &&
                   NavICL1NvNavigationMessage.L1NV.equals(messageType) &&
                  "NEQN".equals(subType)) {
            return new NavICNeQuickNParser(this, new IonosphereNavICNeQuickNMessage(system, prn, messageType, subType));
        } else if (system == SatelliteSystem.GLONASS) {
            return new GlonassCdmsGParser(this, new IonosphereGlonassCdmsMessage(system, prn, messageType, subType));
        } else  {
            return new KlobucharParser(this, new IonosphereKlobucharMessage(system, prn, messageType, subType));
        }
    }

    /** Build a line parser for ephemeris records.
     * @param system satellite system
     * @param messageType message type
     * @return record parser for ephemeris message
     */
    private RecordLineParser buildEphRecordLineParser(final SatelliteSystem system, final String messageType) {
        switch (system) {
            case GPS:
                if (messageType == null || messageType.equals(GPSLegacyNavigationMessage.LNAV)) {
                    // in Rinex, week number is aligned to GPS week!
                    return new GPSLnavParser(this,
                                             new GPSLegacyNavigationMessage(timeScales,
                                                                            SatelliteSystem.GPS,
                                                                            GPSLegacyNavigationMessage.LNAV));
                } else if (messageType.equals(GPSCivilianNavigationMessage.CNAV)) {
                    // in Rinex, week number is aligned to GPS week!
                    return new GPSCnavParser(this,
                                             new GPSCivilianNavigationMessage(false,
                                                                              timeScales,
                                                                              SatelliteSystem.GPS,
                                                                              GPSCivilianNavigationMessage.CNAV));
                } else if (messageType.equals(GPSCivilianNavigationMessage.CNV2)) {
                    // in Rinex, week number is aligned to GPS week!
                    return new GPSCnavParser(this,
                                             new GPSCivilianNavigationMessage(true,
                                                                              timeScales,
                                                                              SatelliteSystem.GPS,
                                                                              GPSCivilianNavigationMessage.CNV2));
                }
                break;
            case GALILEO:
                if (messageType == null || messageType.equals(GalileoNavigationMessage.INAV) || messageType.equals(
                    GalileoNavigationMessage.FNAV)) {
                    // in Rinex, week number is aligned to GPS week!
                    return new GalileoParser(this, new GalileoNavigationMessage(timeScales,
                                                                                SatelliteSystem.GPS,
                                                                                messageType));
                }
                break;
            case GLONASS:
                if (messageType == null || messageType.equals("FDMA")) {
                    return new GlonassFdmaParser(this, new GLONASSFdmaNavigationMessage());
                } else if (messageType.equals("L1OC") || messageType.equals("L3OC")) {
                    return new GlonassCdmaParser(this);
                }
                break;
            case QZSS:
                if (messageType == null || messageType.equals(QZSSLegacyNavigationMessage.LNAV)) {
                    // in Rinex, week number is aligned to GPS week!
                    return new QzssLnavParser(this,
                                              new QZSSLegacyNavigationMessage(timeScales,
                                                                              SatelliteSystem.GPS,
                                                                              QZSSLegacyNavigationMessage.LNAV));
                } else if (messageType.equals(QZSSCivilianNavigationMessage.CNAV)) {
                    // in Rinex, week number is aligned to GPS week!
                    return new QzssCnavParser(this,
                                              new QZSSCivilianNavigationMessage(false,
                                                                                timeScales,
                                                                                SatelliteSystem.GPS,
                                                                                QZSSCivilianNavigationMessage.CNAV));
                } else if (messageType.equals(QZSSCivilianNavigationMessage.CNV2)) {
                    // in Rinex, week number is aligned to GPS week!
                    return new QzssCnavParser(this,
                                              new QZSSCivilianNavigationMessage(true,
                                                                                timeScales,
                                                                                SatelliteSystem.GPS,
                                                                                QZSSCivilianNavigationMessage.CNV2));
                }
                break;
            case BEIDOU:
                if (messageType == null || messageType.equals(BeidouLegacyNavigationMessage.D1) || messageType.equals(
                    BeidouLegacyNavigationMessage.D2)) {
                    // in Rinex, week number for Beidou is really aligned to Beidou week!
                    return new BeidouD1D2Parser(this,
                                                new BeidouLegacyNavigationMessage(timeScales,
                                                                                  SatelliteSystem.BEIDOU,
                                                                                  messageType));
                } else if (messageType.equals(BeidouCivilianNavigationMessage.CNV1)) {
                    // in Rinex, week number for Beidou is really aligned to Beidou week!
                    return new BeidouCnv123Parser(this,
                                                  new BeidouCivilianNavigationMessage(PredefinedGnssSignal.B1C,
                                                                                      timeScales,
                                                                                      SatelliteSystem.BEIDOU,
                                                                                      BeidouCivilianNavigationMessage.CNV1));
                } else if (messageType.equals(BeidouCivilianNavigationMessage.CNV2)) {
                    // in Rinex, week number for Beidou is really aligned to Beidou week!
                    return new BeidouCnv123Parser(this,
                                                  new BeidouCivilianNavigationMessage(PredefinedGnssSignal.B2A,
                                                                                      timeScales,
                                                                                      SatelliteSystem.BEIDOU,
                                                                                      BeidouCivilianNavigationMessage.CNV2));
                } else if (messageType.equals(BeidouCivilianNavigationMessage.CNV3)) {
                    // in Rinex, week number for Beidou is really aligned to Beidou week!
                    return new BeidouCnv123Parser(this,
                                                  new BeidouCivilianNavigationMessage(PredefinedGnssSignal.B2B,
                                                                                      timeScales,
                                                                                      SatelliteSystem.BEIDOU,
                                                                                      BeidouCivilianNavigationMessage.CNV3));
                }
                break;
            case NAVIC:
                if (messageType == null || messageType.equals(NavICLegacyNavigationMessage.LNAV)) {
                    // in Rinex, week number is aligned to GPS week!
                    return new NavICLnavParser(this,
                                               new NavICLegacyNavigationMessage(timeScales,
                                                                                SatelliteSystem.GPS,
                                                                                NavICLegacyNavigationMessage.LNAV));
                } else if (messageType.equals(NavICL1NvNavigationMessage.L1NV)) {
                    // in Rinex, week number is aligned to GPS week!
                    return new NavICL1NvParser(this,
                                               new NavICL1NvNavigationMessage(timeScales,
                                                                              SatelliteSystem.GPS,
                                                                              NavICL1NvNavigationMessage.L1NV));
                }
                break;
            case SBAS:
                if (messageType == null || messageType.equals("SBAS")) {
                    return new SbasParser(this, new SBASNavigationMessage());
                }
                break;
            default:
                // do nothing, handle error after the switch
        }

        // no parse could be set up
        return null;

    }

    /** Get the message line parser.
     * @return message line parser
     */
    public RecordLineParser getRecordLineParser() {
        return recordLineParser;
    }

    /** Parse next record line.
     */
    public void parseRecordLine() {
        switch (++recordLineNumber) {
            case 1: recordLineParser.parseLine01();
            break;
            case 2: recordLineParser.parseLine02();
            break;
            case 3: recordLineParser.parseLine03();
            break;
            case 4: recordLineParser.parseLine04();
            break;
            case 5: recordLineParser.parseLine05();
            break;
            case 6: recordLineParser.parseLine06();
            break;
            case 7: recordLineParser.parseLine07();
            break;
            case 8: recordLineParser.parseLine08();
            break;
            case 9: recordLineParser.parseLine09();
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

}
