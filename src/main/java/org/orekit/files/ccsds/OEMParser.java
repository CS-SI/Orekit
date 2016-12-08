/* Copyright 2002-2016 CS Systèmes d'Information
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFileParser;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * A parser for the CCSDS OEM (Orbit Ephemeris Message).
 * @author sports
 * @since 6.1
 */
public class OEMParser extends ODMParser implements EphemerisFileParser {

    /** Simple constructor.
     * <p>
     * This class is immutable, and hence thread safe. When parts
     * must be changed, such as reference date for Mission Elapsed Time or
     * Mission Relative Time time systems, or the gravitational coefficient or
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
     * The gravitational coefficient is not set here. If it is needed in order
     * to parse Cartesian orbits where the value is not set in the CCSDS file, it must
     * be initialized before parsing by calling {@link #withMu(double)}.
     * </p>
     * <p>
     * The IERS conventions to use is not set here. If it is needed in order to
     * parse some reference frames or UT1 time scale, it must be initialized before
     * parsing by calling {@link #withConventions(IERSConventions)}.
     * </p>
     */
    public OEMParser() {
        this(AbsoluteDate.FUTURE_INFINITY, Double.NaN, null, true, 0, 0, "");
    }

    /** Complete constructor.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param launchYear launch year for TLEs
     * @param launchNumber launch number for TLEs
     * @param launchPiece piece of launch (from "A" to "ZZZ") for TLEs
     */
    private OEMParser(final AbsoluteDate missionReferenceDate, final double mu,
                      final IERSConventions conventions, final boolean simpleEOP,
                      final int launchYear, final int launchNumber, final String launchPiece) {
        super(missionReferenceDate, mu, conventions, simpleEOP, launchYear, launchNumber, launchPiece);
    }

    /** {@inheritDoc} */
    public OEMParser withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return new OEMParser(newMissionReferenceDate, getMu(), getConventions(), isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OEMParser withMu(final double newMu) {
        return new OEMParser(getMissionReferenceDate(), newMu, getConventions(), isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OEMParser withConventions(final IERSConventions newConventions) {
        return new OEMParser(getMissionReferenceDate(), getMu(), newConventions, isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OEMParser withSimpleEOP(final boolean newSimpleEOP) {
        return new OEMParser(getMissionReferenceDate(), getMu(), getConventions(), newSimpleEOP,
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OEMParser withInternationalDesignator(final int newLaunchYear,
                                                 final int newLaunchNumber,
                                                 final String newLaunchPiece) {
        return new OEMParser(getMissionReferenceDate(), getMu(), getConventions(), isSimpleEOP(),
                             newLaunchYear, newLaunchNumber, newLaunchPiece);
    }

    /** {@inheritDoc} */
    @Override
    public OEMFile parse(final String fileName) throws OrekitException {
        return (OEMFile) super.parse(fileName);
    }

    /** {@inheritDoc} */
    @Override
    public OEMFile parse(final InputStream stream) throws OrekitException {
        return (OEMFile) super.parse(stream);
    }

    /** {@inheritDoc} */
    public OEMFile parse(final InputStream stream, final String fileName) throws OrekitException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
            return parse(reader, fileName);
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    @Override
    public OEMFile parse(final BufferedReader reader, final String fileName)
            throws OrekitException {

        try {

            // initialize internal data structures
            final ParseInfo pi = new ParseInfo();
            pi.fileName = fileName;
            final OEMFile file = pi.file;

            // set the additional data that has been configured prior the parsing by the user.
            pi.file.setMissionReferenceDate(getMissionReferenceDate());
            pi.file.setMuSet(getMu());
            pi.file.setConventions(getConventions());

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++pi.lineNumber;
                if (line.trim().length() == 0) {
                    continue;
                }
                pi.keyValue = new KeyValue(line, pi.lineNumber, pi.fileName);
                if (pi.keyValue.getKeyword() == null) {
                    throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, line);
                }
                switch (pi.keyValue.getKeyword()) {
                    case CCSDS_OEM_VERS:
                        file.setFormatVersion(pi.keyValue.getDoubleValue());
                        break;

                    case META_START:
                        file.addEphemeridesBlock();
                        pi.lastEphemeridesBlock = file.getEphemeridesBlocks().get(file.getEphemeridesBlocks().size() - 1);
                        pi.lastEphemeridesBlock.getMetaData().setLaunchYear(getLaunchYear());
                        pi.lastEphemeridesBlock.getMetaData().setLaunchNumber(getLaunchNumber());
                        pi.lastEphemeridesBlock.getMetaData().setLaunchPiece(getLaunchPiece());
                        break;

                    case START_TIME:
                        pi.lastEphemeridesBlock.setStartTime(parseDate(pi.keyValue.getValue(),
                                                                       pi.lastEphemeridesBlock.getMetaData().getTimeSystem()));
                        break;

                    case USEABLE_START_TIME:
                        pi.lastEphemeridesBlock.setUseableStartTime(parseDate(pi.keyValue.getValue(),
                                                                              pi.lastEphemeridesBlock.getMetaData().getTimeSystem()));
                        break;

                    case USEABLE_STOP_TIME:
                        pi.lastEphemeridesBlock.setUseableStopTime(parseDate(pi.keyValue.getValue(), pi.lastEphemeridesBlock.getMetaData().getTimeSystem()));
                        break;

                    case STOP_TIME:
                        pi.lastEphemeridesBlock.setStopTime(parseDate(pi.keyValue.getValue(), pi.lastEphemeridesBlock.getMetaData().getTimeSystem()));
                        break;

                    case INTERPOLATION:
                        pi.lastEphemeridesBlock.setInterpolationMethod(pi.keyValue.getValue());
                        break;

                    case INTERPOLATION_DEGREE:
                        pi.lastEphemeridesBlock.setInterpolationDegree(Integer .parseInt(pi.keyValue.getValue()));
                        break;

                    case META_STOP:
                        file.setMuUsed();
                        parseEphemeridesDataLines(reader, pi);
                        break;

                    case COVARIANCE_START:
                        parseCovarianceDataLines(reader, pi);
                        break;

                    default:
                        boolean parsed = false;
                        parsed = parsed || parseComment(pi.keyValue, pi.commentTmp);
                        parsed = parsed || parseHeaderEntry(pi.keyValue, file, pi.commentTmp);
                        if (pi.lastEphemeridesBlock != null) {
                            parsed = parsed || parseMetaDataEntry(pi.keyValue,
                                                                  pi.lastEphemeridesBlock.getMetaData(), pi.commentTmp);
                            if (parsed && pi.keyValue.getKeyword() == Keyword.REF_FRAME_EPOCH) {
                                pi.lastEphemeridesBlock.setHasRefFrameEpoch(true);
                            }
                        }
                        if (!parsed) {
                            throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, line);
                        }
                }
            }
            file.checkTimeSystems();
            return file;
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /**
     * Parse an ephemeris data line and add its content to the ephemerides
     * block.
     *
     * @param reader the reader
     * @param pi the parser info
     * @exception IOException if an error occurs while reading from the stream
     * @exception OrekitException if a date cannot be parsed
     */
    private void parseEphemeridesDataLines(final BufferedReader reader,  final ParseInfo pi)
        throws OrekitException, IOException {

        for (String line = reader.readLine(); line != null; line = reader.readLine()) {

            ++pi.lineNumber;
            if (line.trim().length() > 0) {
                pi.keyValue = new KeyValue(line, pi.lineNumber, pi.fileName);
                if (pi.keyValue.getKeyword() == null) {
                    Scanner sc = null;
                    try {
                        sc = new Scanner(line);
                        final AbsoluteDate date = parseDate(sc.next(), pi.lastEphemeridesBlock.getMetaData().getTimeSystem());
                        final Vector3D position = new Vector3D(Double.parseDouble(sc.next()) * 1000,
                                                               Double.parseDouble(sc.next()) * 1000,
                                                               Double.parseDouble(sc.next()) * 1000);
                        final Vector3D velocity = new Vector3D(Double.parseDouble(sc.next()) * 1000,
                                                               Double.parseDouble(sc.next()) * 1000,
                                                               Double.parseDouble(sc.next()) * 1000);
                        Vector3D acceleration = Vector3D.NaN;
                        if (sc.hasNext()) {
                            acceleration = new Vector3D(Double.parseDouble(sc.next()) * 1000,
                                                        Double.parseDouble(sc.next()) * 1000,
                                                        Double.parseDouble(sc.next()) * 1000);
                        }
                        final TimeStampedPVCoordinates epDataLine =
                                new TimeStampedPVCoordinates(date, position, velocity, acceleration);
                        pi.lastEphemeridesBlock.getEphemeridesDataLines().add(epDataLine);
                        pi.lastEphemeridesBlock.updateHasAcceleration(acceleration != Vector3D.NaN);
                    } catch (NumberFormatException nfe) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  pi.lineNumber, pi.fileName, line);
                    } finally {
                        if (sc != null) {
                            sc.close();
                        }
                    }
                } else {
                    switch (pi.keyValue.getKeyword()) {
                        case META_START:
                            pi.lastEphemeridesBlock.setEphemeridesDataLinesComment(pi.commentTmp);
                            pi.commentTmp.clear();
                            pi.lineNumber--;
                            reader.reset();
                            return;
                        case COVARIANCE_START:
                            pi.lastEphemeridesBlock.setEphemeridesDataLinesComment(pi.commentTmp);
                            pi.commentTmp.clear();
                            pi.lineNumber--;
                            reader.reset();
                            return;
                        case COMMENT:
                            pi.commentTmp.add(pi.keyValue.getValue());
                            break;
                        default :
                            throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, line);
                    }
                }
            }
            reader.mark(300);

        }
    }

    /**
     * Parse the covariance data lines, create a set of CovarianceMatrix objects
     * and add them in the covarianceMatrices list of the ephemerides block.
     *
     * @param reader the reader
     * @param pi the parser info
     * @throws IOException if an error occurs while reading from the stream
     * @throws OrekitException if the frame cannot be retrieved
     */
    private void parseCovarianceDataLines(final BufferedReader reader, final ParseInfo pi)
        throws IOException, OrekitException  {
        int i = 0;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {

            ++pi.lineNumber;
            if (line.trim().length() == 0) {
                continue;
            }
            pi.keyValue = new KeyValue(line, pi.lineNumber, pi.fileName);
            if (pi.keyValue.getKeyword() == null) {
                final Scanner sc = new Scanner(line);
                for (int j = 0; j < i + 1; j++) {
                    try {
                        pi.lastMatrix.addToEntry(i, j, Double.parseDouble(sc.next()));
                    } catch (NumberFormatException nfe) {
                        sc.close();
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  pi.lineNumber, pi.fileName, line);
                    }
                    if (j != i) {
                        pi.lastMatrix.addToEntry(j, i, pi.lastMatrix.getEntry(i, j));
                    }
                }
                if (i == 5) {
                    final OEMFile.CovarianceMatrix cm =
                            new OEMFile.CovarianceMatrix(pi.epoch, pi.covRefLofType, pi.covRefFrame, pi.lastMatrix);
                    pi.lastEphemeridesBlock.getCovarianceMatrices().add(cm);
                }
                i++;
                if (sc != null) {
                    sc.close();
                }
            } else {
                switch (pi.keyValue.getKeyword()) {
                    case EPOCH :
                        i                = 0;
                        pi.covRefLofType = null;
                        pi.covRefFrame   = null;
                        pi.lastMatrix    = MatrixUtils.createRealMatrix(6, 6);
                        pi.epoch         = parseDate(pi.keyValue.getValue(), pi.lastEphemeridesBlock.getMetaData().getTimeSystem());
                        break;
                    case COV_REF_FRAME :
                        final CCSDSFrame frame = parseCCSDSFrame(pi.keyValue.getValue());
                        if (frame.isLof()) {
                            pi.covRefLofType = frame.getLofType();
                            pi.covRefFrame   = null;
                        } else {
                            pi.covRefLofType = null;
                            pi.covRefFrame   = frame.getFrame(getConventions(), isSimpleEOP());
                        }
                        break;
                    case COVARIANCE_STOP :
                        return;
                    default :
                        throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, line);
                }
            }
        }
    }

    /** Private class used to stock OEM parsing info.
     * @author sports
     */
    private static class ParseInfo {

        /** Ephemerides block being parsed. */
        private OEMFile.EphemeridesBlock lastEphemeridesBlock;

        /** Name of the file. */
        private String fileName;

        /** Current line number. */
        private int lineNumber;

        /** OEM file being read. */
        private OEMFile file;

        /** Key value of the line being read. */
        private KeyValue keyValue;

        /** Stored epoch. */
        private AbsoluteDate epoch;

        /** Covariance reference type of Local Orbital Frame. */
        private LOFType covRefLofType;

        /** Covariance reference frame. */
        private Frame covRefFrame;
        /** Stored matrix. */
        private RealMatrix lastMatrix;

        /** Stored comments. */
        private List<String> commentTmp;

        /** Create a new {@link ParseInfo} object. */
        protected ParseInfo() {
            lineNumber = 0;
            file = new OEMFile();
            commentTmp = new ArrayList<String>();
        }
    }
}
