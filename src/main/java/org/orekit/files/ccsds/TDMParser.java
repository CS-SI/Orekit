/* Copyright 2002-2017 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.files.ccsds;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
//import java.util.regex.Pattern;

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

/**
 * Class for CCSDS Tracking Data Message parsers.
 *
 * <p> This base class is immutable, and hence thread safe. When parts must be
 * changed, such as reference date for Mission Elapsed Time or Mission Relative
 * Time time systems, or the gravitational coefficient or the IERS conventions,
 * the various {@code withXxx} methods must be called, which create a new
 * immutable instance with the new parameters. This is a combination of the <a
 * href="https://en.wikipedia.org/wiki/Builder_pattern">builder design
 * pattern</a> and a <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
 * interface</a>.
 *
 * @author Maxime Journot
 * @since 9.0
 */
public class TDMParser {

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final AbsoluteDate missionReferenceDate;

    /** IERS Conventions. */
    private final  IERSConventions conventions;

    /** Indicator for simple or accurate EOP interpolation. */
    private final  boolean simpleEOP;

    /** Simple constructor.
     * <p>
     * This class is immutable, and hence thread safe. When parts
     * must be changed, such as reference date for Mission Elapsed Time or
     * Mission Relative Time time systems, or the IERS conventions,
     * the IERS conventions, the various {@code withXxx} methods must be called,
     * which create a new immutable instance with the new parameters. This
     * is a combination of the
     * <a href="https://en.wikipedia.org/wiki/Builder_pattern">builder design
     * pattern</a> and a
     * <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
     * interface</a>.
     * </p>
     * <p>
     * The initial date for Mission Elapsed Time and Mission Relative Time time systems is not set here.
     * If such time systems are used, it must be initialized before parsing by calling {@link
     * #withMissionReferenceDate(AbsoluteDate)}.
     * </p>
     * <p>
     * The IERS conventions to use is not set here. If it is needed in order to
     * parse some reference frames or UT1 time scale, it must be initialized before
     * parsing by calling {@link #withConventions(IERSConventions)}.
     * </p>
     */
    public TDMParser() {
        this(AbsoluteDate.FUTURE_INFINITY, null, true);
    }

    /** Complete constructor.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     */
    private TDMParser(final AbsoluteDate missionReferenceDate,
                      final IERSConventions conventions,
                      final boolean simpleEOP) {
        this.missionReferenceDate = missionReferenceDate;
        this.conventions          = conventions;
        this.simpleEOP            = simpleEOP;
    }

    /** Set initial date.
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @return a new instance, with mission reference date replaced
     * @see #getMissionReferenceDate()
     */
    public TDMParser withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return new TDMParser(newMissionReferenceDate, getConventions(), isSimpleEOP());
    }

    /** Get initial date.
     * @return mission reference date to use while parsing
     * @see #withMissionReferenceDate(AbsoluteDate)
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /** Set IERS conventions.
     * @param newConventions IERS conventions to use while parsing
     * @return a new instance, with IERS conventions replaced
     * @see #getConventions()
     */
    public TDMParser withConventions(final IERSConventions newConventions) {
        return new TDMParser(getMissionReferenceDate(), newConventions, isSimpleEOP());
    }

    /** Get IERS conventions.
     * @return IERS conventions to use while parsing
     * @see #withConventions(IERSConventions)
     */
    public IERSConventions getConventions() {
        return conventions;
    }

    /** Set EOP interpolation method.
     * @param newSimpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return a new instance, with EOP interpolation method replaced
     * @see #isSimpleEOP()
     */
    public TDMParser withSimpleEOP(final boolean newSimpleEOP) {
        return new TDMParser(getMissionReferenceDate(), getConventions(), newSimpleEOP);
    }

    /** Get EOP interpolation method.
     * @return true if tidal effects are ignored when interpolating EOP
     * @see #withSimpleEOP(boolean)
     */
    public boolean isSimpleEOP() {
        return simpleEOP;
    }

    /** Parse a CCSDS Tracking Data Message.
     * @param fileName name of the file containing the message
     * @return parsed file content in a TDMFile object
     * @exception OrekitException if Tracking Date Message cannot be parsed or if file cannot be found
     */
    public TDMFile parse(final String fileName)
        throws OrekitException {
        final InputStream stream;
        try {
            stream = new FileInputStream(fileName);
        } catch (IOException e) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, fileName);
        }
        return parse(stream, fileName);
    }

    /** Parse a CCSDS Tracking Data Message.
     * @param stream stream containing message
     * @return parsed file content in a TDMFile object
     * @exception OrekitException if Tracking Date Message cannot be parsed
     */
    public TDMFile parse(final InputStream stream)
        throws OrekitException {
        return parse(stream, "<unknown>");
    }

    /** Parse a CCSDS Tracking Data Message.
     * @param stream stream containing message
     * @param fileName name of the file containing the message (for error messages)
     * @return parsed file content in a TDMFile object
     * @exception OrekitException if Tracking Date Message cannot be parsed
     */
    public TDMFile parse(final InputStream stream, final String fileName)
        throws  OrekitException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
            return parse(reader, fileName);
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /** Parse a CCSDS Tracking Data Message.
     * @param reader buffered reader containing message
     * @param fileName name of the file containing the message (for error messages)
     * @return parsed file content in a TDMFile object
     * @exception OrekitException if Tracking Date Message cannot be parsed
     */
    public TDMFile parse(final BufferedReader reader, final String fileName)
                    throws OrekitException {

        try {
            // Initialize internal data structures
            final ParseInfo pi = new ParseInfo();
            pi.fileName = fileName;
            final TDMFile tdmFile = pi.tdmFile;

            // Read the file
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++pi.lineNumber;
                if (line.trim().length() == 0) {
                    continue;
                }
                pi.line = line;
                pi.keyValue = new KeyValue(pi.line, pi.lineNumber, pi.fileName);
                if (pi.keyValue.getKeyword() == null) {
                    throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, pi.line);
                }
                switch (pi.keyValue.getKeyword()) {

                    // Header entries
                    case CCSDS_TDM_VERS:
                        // Set CCSDS TDM version
                        tdmFile.setFormatVersion(pi.keyValue.getDoubleValue());
                        break;

                    case CREATION_DATE:
                        // Save current comment in header
                        tdmFile.setHeaderComment(pi.commentTmp);
                        pi.commentTmp.clear();
                        // Set creation date
                        tdmFile.setCreationDate(new AbsoluteDate(pi.keyValue.getValue(), TimeScalesFactory.getUTC()));
                        break;

                    case ORIGINATOR:
                        // Set originator
                        tdmFile.setOriginator(pi.keyValue.getValue());
                        break;

                    // Comments
                    case COMMENT:
                        pi.commentTmp.add(pi.keyValue.getValue());
                        break;

                    // Start/Strop keywords
                    case META_START:
                        // Add an observation block and set the last observation block to the current
                        tdmFile.addObservationsBlock();
                        pi.currentObservationsBlock = tdmFile.getObservationsBlocks().get(tdmFile.getObservationsBlocks().size() - 1);
                        break;

                    case META_STOP:
                        // Save current comment in current meta-data comment
                        pi.currentObservationsBlock.getMetaData().setComment(pi.commentTmp);
                        pi.commentTmp.clear();
                        break;

                    case DATA_START:
                        break;

                    case DATA_STOP:
                        // Save current comment in current Observation Block comment
                        pi.currentObservationsBlock.setObservationsComment(pi.commentTmp);
                        pi.commentTmp.clear();
                        break;

                    default:
                        // Parse a line that does not display the previous keywords
                        // A function is evaluated only if any of the previous functions
                        // did not end up successfully parsing the line
                        boolean parsed = false;
                        if (pi.currentObservationsBlock != null) {
                            // Parse a meta-data line
                            parsed = parsed || parseMetaDataEntry(pi);

                            // Parse an observation data line
                            parsed = parsed || parseObservationsDataLine(pi);
                        }
                        if (!parsed) {
                            throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, pi.line);
                        }
                }
            }
            tdmFile.checkTimeSystems();
            return tdmFile;
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /** Parse a meta-data key = value entry.
     * @param pi ParseInfo object
     * @return true if the keyword was a meta-data keyword and has been parsed
     * @exception OrekitException if error in parsing dates
     */
    private boolean parseMetaDataEntry(final ParseInfo pi)
        throws OrekitException {

        final KeyValue keyValue            = pi.keyValue;
        final TDMFile.TDMMetaData metaData = pi.currentObservationsBlock.getMetaData();

        try {
            switch (keyValue.getKeyword()) {
                case TIME_SYSTEM:
                    // Read the time system and ensure that it is supported by Orekit
                    if (!CcsdsTimeScale.contains(keyValue.getValue())) {
                        throw new OrekitException(
                                OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED,
                                keyValue.getValue());
                    }
                    final CcsdsTimeScale timeSystem =
                            CcsdsTimeScale.valueOf(keyValue.getValue());
                    metaData.setTimeSystem(timeSystem);

                    // Convert start/stop time to AbsoluteDate if they have been read already
                    if (metaData.getStartTimeString() != null) {
                        metaData.setStartTime(parseDate(metaData.getStartTimeString(), timeSystem));
                    }
                    if (metaData.getStopTimeString() != null) {
                        metaData.setStopTime(parseDate(metaData.getStopTimeString(), timeSystem));
                    }
                    return true;

                case START_TIME:
                    // Set the start time as a String first
                    metaData.setStartTimeString(keyValue.getValue());

                    // If time system has already been defined, convert the start time to an AbsoluteDate
                    if (metaData.getTimeSystem() != null) {
                        metaData.setStartTime(parseDate(keyValue.getValue(), metaData.getTimeSystem()));
                    }
                    return true;

                case STOP_TIME:
                    // Set the stop time as a String first
                    metaData.setStopTimeString(keyValue.getValue());

                    // If time system has already been defined, convert the start time to an AbsoluteDate
                    if (metaData.getTimeSystem() != null) {
                        metaData.setStopTime(parseDate(keyValue.getValue(), metaData.getTimeSystem()));
                    }
                    return true;

                case PARTICIPANT_1: case PARTICIPANT_2: case PARTICIPANT_3:
                case PARTICIPANT_4: case PARTICIPANT_5:
                    // Get the participant number
                    String key = keyValue.getKey();
                    int participantNumber = Integer.parseInt(key.substring(key.length() - 1));

                    // Add the tuple to the map
                    metaData.addParticipant(participantNumber, keyValue.getValue());
                    return true;

                case MODE:
                    metaData.setMode(keyValue.getValue());
                    return true;

                case PATH:
                    metaData.setPath(keyValue.getValue());
                    return true;

                case PATH_1:
                    metaData.setPath1(keyValue.getValue());
                    return true;

                case PATH_2:
                    metaData.setPath2(keyValue.getValue());
                    return true;

                case TRANSMIT_BAND:
                    metaData.setTransmitBand(keyValue.getValue());
                    return true;

                case RECEIVE_BAND:
                    metaData.setReceiveBand(keyValue.getValue());
                    return true;

                case TURNAROUND_NUMERATOR:
                    metaData.setTurnAroundNumerator(Integer.parseInt(keyValue.getValue()));
                    return true;

                case TURNAROUND_DENOMINATOR:
                    metaData.setTurnAroundDenominator(Integer.parseInt(keyValue.getValue()));
                    return true;

                case TIMETAG_REF:
                    metaData.setTimeTagRef(keyValue.getValue());
                    return true;

                case INTEGRATION_INTERVAL:
                    metaData.setIntegrationInterval(Double.parseDouble(keyValue.getValue()));
                    return true;

                case INTEGRATION_REF:
                    metaData.setIntegrationRef(keyValue.getValue());
                    return true;

                case FREQ_OFFSET:
                    metaData.setFreqOffset(Double.parseDouble(keyValue.getValue()));
                    return true;

                case RANGE_MODE:
                    metaData.setRangeMode(keyValue.getValue());
                    return true;

                case RANGE_MODULUS:
                    metaData.setRangeModulus(Double.parseDouble(keyValue.getValue()));
                    return true;

                case RANGE_UNITS:
                    metaData.setRangeUnits(keyValue.getValue());
                    return true;

                case ANGLE_TYPE:
                    metaData.setAngleType(keyValue.getValue());
                    return true;

                case REFERENCE_FRAME:
                    metaData.setReferenceFrameString(keyValue.getValue());
                    metaData.setReferenceFrame(parseCCSDSFrame(keyValue.getValue()).getFrame(getConventions(), isSimpleEOP()));
                    return true;

                case TRANSMIT_DELAY_1: case TRANSMIT_DELAY_2: case TRANSMIT_DELAY_3:
                case TRANSMIT_DELAY_4: case TRANSMIT_DELAY_5:
                    // Get the participant number
                    key = keyValue.getKey();
                    participantNumber = Integer.parseInt(key.substring(key.length() - 1));

                    // Add the tuple to the map
                    metaData.addTransmitDelay(participantNumber, Double.parseDouble(keyValue.getValue()));
                    return true;

                case RECEIVE_DELAY_1: case RECEIVE_DELAY_2: case RECEIVE_DELAY_3:
                case RECEIVE_DELAY_4: case RECEIVE_DELAY_5:
                 // Get the participant number
                    key = keyValue.getKey();
                    participantNumber = Integer.parseInt(key.substring(key.length() - 1));

                    // Add the tuple to the map
                    metaData.addReceiveDelay(participantNumber, Double.parseDouble(keyValue.getValue()));
                    return true;

                case DATA_QUALITY:
                    metaData.setDataQuality(keyValue.getValue());
                    return true;

                case CORRECTION_ANGLE_1:
                    metaData.setCorrectionAngle1(Double.parseDouble(keyValue.getValue()));
                    return true;

                case CORRECTION_ANGLE_2:
                    metaData.setCorrectionAngle2(Double.parseDouble(keyValue.getValue()));
                    return true;

                case CORRECTION_DOPPLER:
                    metaData.setCorrectionDoppler(Double.parseDouble(keyValue.getValue()));
                    return true;

                case CORRECTION_RANGE:
                    metaData.setCorrectionRange(Double.parseDouble(keyValue.getValue()));
                    return true;

                case CORRECTION_RECEIVE:
                    metaData.setCorrectionReceive(Double.parseDouble(keyValue.getValue()));
                    return true;

                case CORRECTION_TRANSMIT:
                    metaData.setCorrectionTransmit(Double.parseDouble(keyValue.getValue()));
                    return true;

                case CORRECTIONS_APPLIED:
                    metaData.setCorrectionsApplied(keyValue.getValue());
                    return true;

                default:
                    return false;
            }
        } catch (NumberFormatException nfe) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      pi.lineNumber, pi.fileName, pi.line);
        }
    }

    /**
     * Parse an observation data line and add its content to the Observations Block
     * block.
     *
     * @param pi ParseInfo object
     * @return true if no exception were thrown
     * @exception OrekitException if error in parsing dates
     */
    private boolean parseObservationsDataLine(final ParseInfo pi)
        throws OrekitException {

        // Parse an observation line
        // An observation line should consist in the string "keyword = epoch value"
        // pi.keyValue.getValue() should return the string "epoch value"
        final String[] fields = pi.keyValue.getValue().split("\\s+");

        // Check that there are 2 fields in the value of the key
        if (fields.length != 2) {
            throw new OrekitException(OrekitMessages.CCSDS_TDM_INCONSISTENT_DATA_LINE,
                                      pi.lineNumber, pi.fileName, pi.line);
        }

        // Convert the date to an AbsoluteDate object (OrekitException if it fails)
        final AbsoluteDate epoch = parseDate(fields[0], pi.currentObservationsBlock.getMetaData().getTimeSystem());
        final double measurement;
        try {
            // Convert the value to double (NumberFormatException if it fails)
            measurement = Double.parseDouble(fields[1]);
        } catch (NumberFormatException nfe) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      pi.lineNumber, pi.fileName, pi.line);
        }

        // Adds the observation to current observation block
        pi.currentObservationsBlock.addObservation(pi.keyValue.getKeyword().name(),
                                                   epoch,
                                                   measurement);
        return true;
    }

    /** Parse a CCSDS frame.
     * @param frameName name of the frame, as the value of a CCSDS key=value line
     * @return CCSDS frame corresponding to the name
     */
    private CCSDSFrame parseCCSDSFrame(final String frameName) {
        return CCSDSFrame.valueOf(frameName.replaceAll("-", ""));
    }

    /** Parse a date.
     * @param date date to parse, as the value of a CCSDS key=value line
     * @param timeSystem time system to use
     * @return parsed date
     * @exception OrekitException if some time scale cannot be retrieved
     */
    private AbsoluteDate parseDate(final String date, final CcsdsTimeScale timeSystem)
        throws OrekitException {
        return timeSystem.parseDate(date, conventions, missionReferenceDate);
    }

    /** Private class used to stock OEM parsing info.
     * @author sports
     */
    private static class ParseInfo {

        /** Current Observation Block being parsed. */
        private TDMFile.ObservationsBlock currentObservationsBlock;

        /** Name of the file. */
        private String fileName;

        /** Current line number. */
        private int lineNumber;

        /** Current parsed line. */
        private String line;

        /** TDMFile object being filled. */
        private TDMFile tdmFile;

        /** Key value of the current line being read. */
        private KeyValue keyValue;

        /** Temporary stored comments. */
        private List<String> commentTmp;

        /** Create a new {@link ParseInfo} object. */
        protected ParseInfo() {
            lineNumber = 0;
            line = "";
            tdmFile = new TDMFile();
            commentTmp = new ArrayList<>();
        }
    }

}
