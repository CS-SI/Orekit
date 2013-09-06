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
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.OEMFile;
import org.orekit.files.general.OrbitFile;
import org.orekit.files.general.OrbitFileParser;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/**
 * A parser for the CCSDS OEM (Orbit Ephemeris Message).
 * @author sports
 * @since 6.1
 */
public class OEMParser
    implements OrbitFileParser {

    /** String representing one or more blanks. */
    private static final String BLANKS = " +";

    /** Initial Date for MET or MRT time systems. Has to be configured by the user prior parsing.*/
    private AbsoluteDate initialDate;

    /** Gravitational coefficient. Has to be configured by the user prior parsing.*/
    private double mu;

    /** IERS Conventions to use. */
    private IERSConventions conventions;

    /** Simple constructor.
     * <p>
     * The initial date for Mission Elapsed Time and Mission Relative Time time systems is not set here.
     * If such time systems are used, it must be initialized before parsing by calling {@link
     * #setInitialDate(AbsoluteDate)}.
     * </p>
     * <p>
     * The gravitational coefficient is not set here. If it is needed in order
     * to parse Cartesian orbits where the value is not set in the CCSDS file, it must
     * be initialized before parsing by calling {@link #setMu(double)}.
     * </p>
     * <p>
     * The IERS conventions to use is not set here. If it is needed in order to
     * parse some reference frames or UT1 time scale, it must be initialized before
     * parsing by calling {@link #setConventions(IERSConventions)}.
     * </p>
     */
    public OEMParser() {
        initialDate = AbsoluteDate.FUTURE_INFINITY;
        mu          = Double.NaN;
        conventions = null;
    }

    /** Set gravitational coefficient.
     * @param mu gravitational coefficient to be set
     */
    public void setMu(final double mu) {
        this.mu = mu;
    }

    /** Set initial date.
     * @param initialDate date to be set
     */
    public void setInitialDate(final AbsoluteDate initialDate) {
        this.initialDate = initialDate;
    }

    /** Set IERS conventions.
     * @param conventions IERS conventions to be set
     */
    public void setConventions(final IERSConventions conventions) {
        this.conventions = conventions;
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

        final BufferedReader reader = new BufferedReader(
                                                         new InputStreamReader(
                                                                               stream,
                                                                               "UTF-8"));
        // initialize internal data structures
        final ParseInfo pi = new ParseInfo();
        final OEMFile file = pi.file;

        // set the additional data that has been configured prior the parsing by the user.
        if (!initialDate.equals(AbsoluteDate.FUTURE_INFINITY)) {
            pi.file.setInitialDate(initialDate);
        }

        if (!Double.isNaN(mu)) {
            pi.file.setMuSet(mu);
        }

        if (conventions != null) {
            pi.file.setConventions(conventions);
        }

        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            if (line.trim().length() == 0)
                continue;
            final Scanner sc = new Scanner(line);
            pi.keyword = Keyword.valueOf(sc.next());
            if (pi.keyword != Keyword.COMMENT &&
                pi.keyword != Keyword.META_START &&
                pi.keyword != Keyword.META_STOP &&
                pi.keyword != Keyword.COVARIANCE_START &&
                pi.keyword != Keyword.COVARIANCE_STOP) {
                pi.keyValue = line.split(BLANKS, 3)[2];
            } else if (pi.keyword == Keyword.COMMENT) {
                pi.keyValue = sc.next();
            }

            switch (pi.keyword) {
            case CCSDS_OEM_VERS: {
                file.setFormatVersion(pi.keyValue);
            }
                break;

            case COMMENT: {
                pi.commentTmp.add(line.split(BLANKS, 2)[1]);
            }
                break;

            case CREATION_DATE: {
                checkSetComment(pi, file, ODMBlock.HEADER);
                file.setCreationDate(new AbsoluteDate(pi.keyValue,
                                                      TimeScalesFactory
                                                          .getUTC()));
            }
                break;

            case ORIGINATOR: {
                file.setOriginator(pi.keyValue);
            }
                break;

            case META_START: {
                file.addEphemeridesBlock();
                pi.lastEphemeridesBlock = file.getEphemeridesBlocks()
                    .get(file.getEphemeridesBlocks().size() - 1);
            }
                break;

            case OBJECT_NAME: {
                if (!pi.commentTmp.isEmpty()) {
                    pi.lastEphemeridesBlock.setMetadataComment(pi.commentTmp);
                    pi.commentTmp.clear();
                }
                pi.lastEphemeridesBlock.setObjectName(pi.keyValue);
            }
                break;

            case OBJECT_ID: {
                pi.lastEphemeridesBlock.setObjectID(pi.keyValue);
            }
                break;

            case CENTER_NAME: {
                pi.lastEphemeridesBlock.setCenterName(pi.keyValue);
                if (pi.keyValue.matches("SOLAR SYSTEM BARYCENTER") ||
                    pi.keyValue.matches("SSB")) {
                    pi.keyValue = "SOLAR_SYSTEM_BARYCENTER";
                }
                if (pi.keyValue.matches("EARTH MOON BARYCENTER") ||
                    pi.keyValue.matches("EARTH-MOON BARYCENTER") ||
                    pi.keyValue.matches("EARTH BARYCENTER") ||
                    pi.keyValue.matches("EMB")) {
                    pi.keyValue = "EARTH_MOON";
                }
                for (final CenterName c : CenterName.values()) {
                    if (c.name().equals(pi.keyValue)) {
                        pi.lastEphemeridesBlock.setHasCreatableBody(true);
                        pi.lastEphemeridesBlock.setCenterBody(c.getCelestialBody());
                        file.setMuCreated(c.getCelestialBody().getGM());
                    }
                }
            }
                break;

            case REF_FRAME: {
                pi.lastEphemeridesBlock.setRefFrame(CCSDSFrame.valueOf(pi.keyValue.replaceAll("-", "")).getFrame(conventions));
            }
                break;

            case REF_FRAME_EPOCH: {
                pi.lastEphemeridesBlock.setHasRefFrameEpoch(true);
                pi.epoch2 = pi.keyValue;
            }
                break;

            case TIME_SYSTEM: {
                pi.lastEphemeridesBlock.setTimeSystem(OrbitFile.TimeSystem.valueOf(pi.keyValue));
                switch (pi.lastEphemeridesBlock.getTimeSystem()) {
                case GMST:
                    pi.lastEphemeridesBlock.setTimeScale(TimeScalesFactory.getGMST());
                    break;

                case GPS:
                    pi.lastEphemeridesBlock.setTimeScale(TimeScalesFactory.getGPS());
                    break;
                case TAI:
                    pi.lastEphemeridesBlock.setTimeScale(TimeScalesFactory.getTAI());
                    break;
                case TCB:
                    pi.lastEphemeridesBlock.setTimeScale(TimeScalesFactory.getTCB());
                    break;

                case TDB:
                    pi.lastEphemeridesBlock.setTimeScale(TimeScalesFactory.getTDB());
                    break;

                case TCG:
                    pi.lastEphemeridesBlock.setTimeScale(TimeScalesFactory.getTCG());
                    break;

                case TT:
                    pi.lastEphemeridesBlock.setTimeScale(TimeScalesFactory.getTT());
                    break;

                case UT1:
                    pi.lastEphemeridesBlock.setTimeScale(TimeScalesFactory.getUT1(conventions));
                    break;

                case UTC:
                    pi.lastEphemeridesBlock.setTimeScale(TimeScalesFactory.getUTC());
                    break;

                default:
                    break;
                }
                if (pi.lastEphemeridesBlock.getHasRefFrameEpoch()) {
                    if (pi.lastEphemeridesBlock.getTimeSystem().equals(OrbitFile.TimeSystem.MET) ||
                            pi.lastEphemeridesBlock.getTimeSystem().equals(OrbitFile.TimeSystem.MRT)) {
                        final DateTimeComponents clock = DateTimeComponents.parseDateTime(pi.epoch2);
                        final double offset = clock.getDate().getYear() * Constants.JULIAN_YEAR +
                                            clock.getDate().getDayOfYear() * Constants.JULIAN_DAY +
                                            clock.getTime().getSecondsInDay();
                        pi.lastEphemeridesBlock.setFrameEpoch(offset);
                    } else {
                        pi.lastEphemeridesBlock.setFrameEpoch(new AbsoluteDate(pi.epoch2,
                                                                               pi.lastEphemeridesBlock.getTimeScale()));
                    }
                }

            }
                break;

            case START_TIME: {
                if (pi.lastEphemeridesBlock.getTimeSystem().equals(OrbitFile.TimeSystem.MET) ||
                        pi.lastEphemeridesBlock.getTimeSystem().equals(OrbitFile.TimeSystem.MRT)) {
                    final DateTimeComponents clock = DateTimeComponents.parseDateTime(pi.keyValue);
                    final double offset = clock.getDate().getYear() * Constants.JULIAN_YEAR +
                                          clock.getDate().getDayOfYear() * Constants.JULIAN_DAY +
                                          clock.getTime().getSecondsInDay();
                    pi.lastEphemeridesBlock.setStartTime(offset);
                } else {
                    pi.lastEphemeridesBlock.setStartTime(new AbsoluteDate(pi.keyValue,
                                                                          pi.lastEphemeridesBlock.getTimeScale()));
                }
            }
                break;

            case USEABLE_START_TIME: {
                if (pi.lastEphemeridesBlock.getTimeSystem().equals(OrbitFile.TimeSystem.MET) ||
                        pi.lastEphemeridesBlock.getTimeSystem().equals(OrbitFile.TimeSystem.MRT)) {
                    final DateTimeComponents clock = DateTimeComponents.parseDateTime(pi.keyValue);
                    final double offset = clock.getDate().getYear() * Constants.JULIAN_YEAR +
                                          clock.getDate().getDayOfYear() * Constants.JULIAN_DAY +
                                          clock.getTime().getSecondsInDay();
                    pi.lastEphemeridesBlock.setUseableStartTime(offset);
                } else {
                    pi.lastEphemeridesBlock.setUseableStartTime(new AbsoluteDate(pi.keyValue,
                                                                                 pi.lastEphemeridesBlock.getTimeScale()));
                }
            }
                break;

            case USEABLE_STOP_TIME: {
                if (pi.lastEphemeridesBlock.getTimeSystem().equals(OrbitFile.TimeSystem.MET) ||
                        pi.lastEphemeridesBlock.getTimeSystem().equals(OrbitFile.TimeSystem.MRT)) {
                    final DateTimeComponents clock = DateTimeComponents.parseDateTime(pi.keyValue);
                    final double offset = clock.getDate().getYear() * Constants.JULIAN_YEAR +
                                          clock.getDate().getDayOfYear() * Constants.JULIAN_DAY +
                                          clock.getTime().getSecondsInDay();
                    pi.lastEphemeridesBlock.setUseableStopTime(offset);
                } else {
                    pi.lastEphemeridesBlock.setUseableStopTime(new AbsoluteDate(pi.keyValue,
                                                                                pi.lastEphemeridesBlock.getTimeScale()));
                }
            }
                break;

            case STOP_TIME: {
                if (pi.lastEphemeridesBlock.getTimeSystem().equals(OrbitFile.TimeSystem.MET) ||
                        pi.lastEphemeridesBlock.getTimeSystem().equals(OrbitFile.TimeSystem.MRT)) {
                    final DateTimeComponents clock = DateTimeComponents.parseDateTime(pi.keyValue);
                    final double offset = clock.getDate().getYear() * Constants.JULIAN_YEAR +
                                          clock.getDate().getDayOfYear() * Constants.JULIAN_DAY +
                                          clock.getTime().getSecondsInDay();
                    pi.lastEphemeridesBlock.setStopTime(offset);
                } else {
                    pi.lastEphemeridesBlock.setStopTime(new AbsoluteDate(pi.keyValue,
                                                                         pi.lastEphemeridesBlock.getTimeScale()));
                }
            }
                break;

            case INTERPOLATION: {
                pi.lastEphemeridesBlock.setInterpolationMethod(pi.keyValue);
            }
                break;

            case INTERPOLATION_DEGREE: {
                pi.lastEphemeridesBlock.setInterpolationDegree(Integer
                    .parseInt(pi.keyValue));
            }
                break;

            case META_STOP: {
                file.setMuUsed();
                parseEphemeridesDataLines(reader, pi);
                if (!pi.commentTmp.isEmpty()) {
                    pi.lastEphemeridesBlock
                        .setEphemeridesDataLinesComment(pi.commentTmp);
                    pi.commentTmp.clear();
                }
            }
                break;

            case COVARIANCE_START: {
                parseCovarianceDataLines(reader, pi);
            }
                break;

            default:
                break;
            }
        }
        file.setTimeSystem();
        return file;
    }

    /**
     * Parse an ephemeris data line and add its content to the ephemerides
     * block.
     *
     * @param reader the reader
     * @param pi the parser info
     * @throws IOException if an error occurs while reading from the stream
     */
    private void parseEphemeridesDataLines(final BufferedReader reader,
                                           final ParseInfo pi)
        throws IOException {

        for (String line = reader.readLine(); line != null; line = reader.readLine()) {

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
                final AbsoluteDate date;
                if (pi.lastEphemeridesBlock.getTimeSystem().equals(OrbitFile.TimeSystem.MET) ||
                    pi.lastEphemeridesBlock.getTimeSystem().equals(OrbitFile.TimeSystem.MRT)) {
                    final DateTimeComponents clock = DateTimeComponents.parseDateTime(pi.stringTmp);
                    final double offset = clock.getDate().getYear() * Constants.JULIAN_YEAR +
                                          clock.getDate().getDayOfYear() * Constants.JULIAN_DAY +
                                          clock.getTime().getSecondsInDay();
                    date = initialDate.shiftedBy(offset);
                } else {
                    date = new AbsoluteDate(pi.stringTmp,
                                            pi.lastEphemeridesBlock.getTimeScale());
                }

                final Vector3D position = new Vector3D(
                                                 Double.parseDouble(sc.next()) * 1000,
                                                 Double.parseDouble(sc.next()) * 1000,
                                                 Double.parseDouble(sc.next()) * 1000);
                final Vector3D velocity = new Vector3D(
                                                 Double.parseDouble(sc.next()) * 1000,
                                                 Double.parseDouble(sc.next()) * 1000,
                                                 Double.parseDouble(sc.next()) * 1000);
                final CartesianOrbit orbit = new CartesianOrbit(
                                                          new PVCoordinates(position,
                                                                            velocity),
                                                          pi.lastEphemeridesBlock.getFrame(),
                                                          date,
                                                          pi.file.getMuUsed());
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
                pi.lastMatrix    = new Array2DRowRealMatrix(6, 6);
                if (pi.lastEphemeridesBlock.getTimeSystem().equals(OrbitFile.TimeSystem.MET) ||
                    pi.lastEphemeridesBlock.getTimeSystem().equals(OrbitFile.TimeSystem.MRT)) {
                    final DateTimeComponents clock = DateTimeComponents.parseDateTime(sc.next());
                    final double offset = clock.getDate().getYear() * Constants.JULIAN_YEAR +
                                          clock.getDate().getDayOfYear() * Constants.JULIAN_DAY +
                                          clock.getTime().getSecondsInDay();
                    pi.epoch = initialDate.shiftedBy(offset);
                } else {
                    pi.epoch = new AbsoluteDate(sc.next(),
                                                pi.lastEphemeridesBlock.getTimeScale());
                }
            } else if (pi.stringTmp.matches("COV_REF_FRAME")) {
                sc.next();
                pi.keyValue = sc.next();
                final CCSDSFrame frame = CCSDSFrame.valueOf(pi.keyValue.replaceAll("-", ""));
                if (frame.isLof()) {
                    pi.covRefLofType = frame.getLofType();
                    pi.covRefFrame   = null;
                } else {
                    pi.covRefLofType = null;
                    pi.covRefFrame   = frame.getFrame(conventions);
                }
            } else {
                final Scanner sc2 = new Scanner(line);
                for (int j = 0; j < i + 1; j++) {
                    pi.lastMatrix.addToEntry(i, j,
                                             Double.parseDouble(sc2.next()));
                    if (j != i) {
                        pi.lastMatrix.addToEntry(j, i,
                                                 pi.lastMatrix.getEntry(i, j));
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

    /** This method is called after a potential comment parsing. If there has been a comment parsing,
     * it sets the comment to the associated ODM block.
     * @param pi the parsing info
     * @param file the OEM file to be set
     * @param block comment's block
     * @throws OrekitException if the ODM block is DATA_MANEUVER
     */
    public void checkSetComment(final ParseInfo pi, final OEMFile file, final ODMBlock block)
        throws OrekitException {
        if (!pi.commentTmp.isEmpty()) {
            file.setComment(block, pi.commentTmp);
            pi.commentTmp.clear();
        }
    }

    /** Private class used to stock OEM parsing info.     *
     * @author sports
     */
    private static class ParseInfo {

        /** Ephemerides block being parsed. */
        private OEMFile.EphemeridesBlock lastEphemeridesBlock;

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

        /** Stored epoch. */
        private String epoch2;

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
            file = new OEMFile();
            commentTmp = new ArrayList<String>();
        }
    }
}
