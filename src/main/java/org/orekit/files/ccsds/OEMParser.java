/* Copyright 2002-2013 CS Systèmes d'Information
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.math3.exception.util.DummyLocalizable;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.OEMFile;
import org.orekit.files.general.OrbitFileParser;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/**
 * A parser for the CCSDS OEM (Orbit Ephemeris Message).
 * @author sports
 * @since 6.1
 */
public class OEMParser extends ODMParser implements OrbitFileParser {

    /** String representing one or more blanks. */
    private static final String BLANKS = " +";

    /** Simple constructor.
     * <p>
     * This class is immutable, and hence thread safe. When parts
     * must be changed, such as reference date for Mission Elapsed Time or
     * Mission Relative Time time systems, or the gravitational coefficient or
     * the IERS conventions, the various {@code withXxx} methods must be called,
     * which create a new immutable instance with the new parameters. This
     * is a combination of the <a href="">builder design pattern</a> and
     * a <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
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
        this(AbsoluteDate.FUTURE_INFINITY, Double.NaN, null);
    }

    /** Complete constructor.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param conventions IERS Conventions
     */
    private OEMParser(final AbsoluteDate missionReferenceDate, final double mu,
                      final IERSConventions conventions) {
        super(missionReferenceDate, mu, conventions);
    }

    /** Set initial date.
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @return a new instance, with mission reference date replaced
     * @see #getMissionReferenceDate()
     */
    public OEMParser withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return new OEMParser(newMissionReferenceDate, getMu(), getConventions());
    }

    /** Set gravitational coefficient.
     * @param newMu gravitational coefficient to use while parsing
     * @return a new instance, with gravitational coefficient date replaced
     * @see #getMu()
     */
    public OEMParser withMu(final double newMu) {
        return new OEMParser(getMissionReferenceDate(), newMu, getConventions());
    }

    /** Set IERS conventions.
     * @param newConventions IERS conventions to use while parsing
     * @return a new instance, with IERS conventions replaced
     * @see #getConventions()
     */
    public OEMParser withConventions(final IERSConventions newConventions) {
        return new OEMParser(getMissionReferenceDate(), getMu(), newConventions);
    }

    /** {@inheritDoc} */
    public OEMFile parse(final String fileName)
        throws OrekitException {

        InputStream stream = null;

        try {
            stream = new FileInputStream(fileName);
            return parse(stream);
        } catch (FileNotFoundException e) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE,
                                      fileName);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /** {@inheritDoc} */
    public OEMFile parse(final InputStream stream)
        throws OrekitException {

        try {
            return parseInternal(stream);
        } catch (IOException e) {
            throw new OrekitException(e, new DummyLocalizable(e.getMessage()));
        }
    }

    /**
     * Parse the OEM file from the given {@link InputStream} and return a
     * {@link OEMFile} object.
     * @param stream the stream to be parsed
     * @return the {@link OEMFile}
     * @throws OrekitException if the file could not be parsed successfully
     * @throws IOException if an error occurs while reading from the stream
     */
    private OEMFile parseInternal(final InputStream stream)
        throws OrekitException, IOException {

        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        // initialize internal data structures
        final ParseInfo pi = new ParseInfo();
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
            final Scanner sc = new Scanner(line);
            pi.keyword = Keyword.valueOf(sc.next());
            if (pi.keyword != Keyword.COMMENT &&
                pi.keyword != Keyword.META_START && pi.keyword != Keyword.META_STOP &&
                pi.keyword != Keyword.COVARIANCE_START && pi.keyword != Keyword.COVARIANCE_STOP) {
                pi.keyValue = line.split(BLANKS, 3)[2];
            } else if (pi.keyword == Keyword.COMMENT) {
                pi.keyValue = sc.next();
            }

            switch (pi.keyword) {
            case CCSDS_OEM_VERS:
                file.setFormatVersion(pi.keyValue);
                break;

            case META_START:
                file.addEphemeridesBlock();
                pi.lastEphemeridesBlock = file.getEphemeridesBlocks().get(file.getEphemeridesBlocks().size() - 1);
                break;

            case START_TIME:
                pi.lastEphemeridesBlock.setStartTime(parseDate(pi.keyValue,
                                                               pi.lastEphemeridesBlock.getMetaData().getTimeSystem()));
                break;

            case USEABLE_START_TIME:
                pi.lastEphemeridesBlock.setUseableStartTime(parseDate(pi.keyValue,
                                                                      pi.lastEphemeridesBlock.getMetaData().getTimeSystem()));
                break;

            case USEABLE_STOP_TIME:
                pi.lastEphemeridesBlock.setUseableStopTime(parseDate(pi.keyValue, pi.lastEphemeridesBlock.getMetaData().getTimeSystem()));
                break;

            case STOP_TIME:
                pi.lastEphemeridesBlock.setStopTime(parseDate(pi.keyValue, pi.lastEphemeridesBlock.getMetaData().getTimeSystem()));
                break;

            case INTERPOLATION:
                pi.lastEphemeridesBlock.setInterpolationMethod(pi.keyValue);
                break;

            case INTERPOLATION_DEGREE:
                pi.lastEphemeridesBlock.setInterpolationDegree(Integer .parseInt(pi.keyValue));
                break;

            case META_STOP:
                file.setMuUsed();
                parseEphemeridesDataLines(reader, pi);
                pi.lastEphemeridesBlock.setEphemeridesDataLinesComment(pi.commentTmp);
                pi.commentTmp.clear();
                break;

            case COVARIANCE_START:
                parseCovarianceDataLines(reader, pi);
                break;

            default:
                boolean parsed = false;
                parsed = parsed || parseComment(line, pi.keyword, pi.commentTmp);
                parsed = parsed || parseHeaderEntry(pi.keyword, pi.keyValue, file, pi.commentTmp);
                if (pi.lastEphemeridesBlock != null) {
                    parsed = parsed || parseMetaDataEntry(line, pi.keyword, pi.keyValue,
                                                          pi.lastEphemeridesBlock.getMetaData(), pi.commentTmp);
                }
                if (!parsed) {
                    throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, line);
                }
            }
        }
        file.checkTimeSystems();
        return file;
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
    private void parseEphemeridesDataLines(final BufferedReader reader,
                                           final ParseInfo pi)
        throws OrekitException, IOException {

        for (String line = reader.readLine(); line != null; line = reader.readLine()) {

            ++pi.lineNumber;
            if (line.matches("META_START") || line.matches("COVARIANCE_START")) {
                break;
            }
            if (line.trim().length() == 0) {
                continue;
            }
            final Scanner sc = new Scanner(line);
            pi.stringTmp = sc.next();
            if (pi.stringTmp.matches("COMMENT")) {
                pi.commentTmp.add(line.split(BLANKS, 2)[1]);
            } else {
                final AbsoluteDate date = parseDate(pi.stringTmp, pi.lastEphemeridesBlock.getMetaData().getTimeSystem());
                final Vector3D position = new Vector3D(Double.parseDouble(sc.next()) * 1000,
                                                       Double.parseDouble(sc.next()) * 1000,
                                                       Double.parseDouble(sc.next()) * 1000);
                final Vector3D velocity = new Vector3D(Double.parseDouble(sc.next()) * 1000,
                                                       Double.parseDouble(sc.next()) * 1000,
                                                       Double.parseDouble(sc.next()) * 1000);
                final CartesianOrbit orbit =
                        new CartesianOrbit(new PVCoordinates(position, velocity), pi.lastEphemeridesBlock.getMetaData().getFrame(),
                                           date, pi.file.getMuUsed());
                Vector3D acceleration = null;
                if (sc.hasNext()) {
                    acceleration = new Vector3D(Double.parseDouble(sc.next()) * 1000,
                                                Double.parseDouble(sc.next()) * 1000,
                                                Double.parseDouble(sc.next()) * 1000);
                }
                final OEMFile.EphemeridesDataLine epDataLine =
                        new OEMFile.EphemeridesDataLine(orbit, acceleration);
                pi.lastEphemeridesBlock.getEphemeridesDataLines().add(epDataLine);
                reader.mark(300);
            }

        }
        reader.reset();
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
            if (line.matches("COVARIANCE_STOP")) {
                break;
            }
            final Scanner sc = new Scanner(line);
            pi.stringTmp = sc.next();
            if (pi.stringTmp.matches("EPOCH")) {
                i = 0;
                sc.next();
                pi.covRefLofType = null;
                pi.covRefFrame   = null;
                pi.lastMatrix    = MatrixUtils.createRealMatrix(6, 6);
                pi.epoch         = parseDate(sc.next(), pi.lastEphemeridesBlock.getMetaData().getTimeSystem());
            } else if (pi.stringTmp.matches("COV_REF_FRAME")) {
                sc.next();
                pi.keyValue = sc.next();
                final CCSDSFrame frame = parseCCSDSFrame(pi.keyValue);
                if (frame.isLof()) {
                    pi.covRefLofType = frame.getLofType();
                    pi.covRefFrame   = null;
                } else {
                    pi.covRefLofType = null;
                    pi.covRefFrame   = frame.getFrame(getConventions());
                }
            } else {
                final Scanner sc2 = new Scanner(line);
                for (int j = 0; j < i + 1; j++) {
                    pi.lastMatrix.addToEntry(i, j, Double.parseDouble(sc2.next()));
                    if (j != i) {
                        pi.lastMatrix.addToEntry(j, i, pi.lastMatrix.getEntry(i, j));
                    }
                }
                if (i == 5) {
                    pi.lastEphemeridesBlock.getCovarianceMatrices().add(new OEMFile.CovarianceMatrix(pi.epoch,
                                                                                                     pi.covRefLofType, pi.covRefFrame,
                                                                                                     pi.lastMatrix));
                }
                i++;
            }
        }
    }

    /** Private class used to stock OEM parsing info.
     * @author sports
     */
    private static class ParseInfo {

        /** Ephemerides block being parsed. */
        private OEMFile.EphemeridesBlock lastEphemeridesBlock;

        /** Current line number. */
        private int lineNumber;

        /** OEM file being read. */
        private OEMFile file;

        /** Keyword of the line being read. */
        private Keyword keyword;

        /** Key value of the line being read. */
        private String keyValue;

        /** Stored String. */
        private String stringTmp;

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
