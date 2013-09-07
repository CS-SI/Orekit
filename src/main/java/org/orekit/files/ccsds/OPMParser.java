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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.OrbitFileParser;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** A parser for the CCSDS OPM (Orbit Parameter Message).
 * @author sports
 * @since 6.1
 */
public class OPMParser extends ODMParser implements OrbitFileParser {

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
    public OPMParser() {
        this(AbsoluteDate.FUTURE_INFINITY, Double.NaN, null);
    }

    /** Complete constructor.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param conventions IERS Conventions
     */
    private OPMParser(final AbsoluteDate missionReferenceDate, final double mu,
                      final IERSConventions conventions) {
        super(missionReferenceDate, mu, conventions);
    }

    /** Set initial date.
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @return a new instance, with mission reference date replaced
     * @see #getMissionReferenceDate()
     */
    public OPMParser withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return new OPMParser(newMissionReferenceDate, getMu(), getConventions());
    }

    /** Set gravitational coefficient.
     * @param newMu gravitational coefficient to use while parsing
     * @return a new instance, with gravitational coefficient date replaced
     * @see #getMu()
     */
    public OPMParser withMu(final double newMu) {
        return new OPMParser(getMissionReferenceDate(), newMu, getConventions());
    }

    /** Set IERS conventions.
     * @param newConventions IERS conventions to use while parsing
     * @return a new instance, with IERS conventions replaced
     * @see #getConventions()
     */
    public OPMParser withConventions(final IERSConventions newConventions) {
        return new OPMParser(getMissionReferenceDate(), getMu(), newConventions);
    }

    /** {@inheritDoc} */
    public OPMFile parse(final String fileName)
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
    public OPMFile parse(final InputStream stream)
        throws OrekitException {

        try {
            return parseInternal(stream);
        } catch (IOException e) {
            throw new OrekitException(e, new DummyLocalizable(e.getMessage()));
        }
    }

    /**
     * Parse the OPM file from the given {@link InputStream} and return a
     * {@link OPMFile} object.
     * @param stream the stream to be parsed
     * @return the {@link OPMFile}
     * @throws OrekitException if the file could not be parsed successfully
     * @throws IOException if an error occurs while reading from the stream
     */
    private OPMFile parseInternal(final InputStream stream)
        throws OrekitException, IOException {

        final BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        // initialize internal data structures
        final ParseInfo pi = new ParseInfo();
        final OPMFile file = pi.file;

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
            pi.keywordTmp = sc.next();
            if (pi.keywordTmp.matches("USER_DEFINED_.*")) {
                pi.userDefinedKeyword = pi.keywordTmp;
                pi.keyword = Keyword.USER_DEFINED_X;
            } else {
                pi.keyword = Keyword.valueOf(pi.keywordTmp);
            }
            if (pi.keyword != Keyword.COMMENT) {
                sc.next(); // skip "="
            }
            pi.keyValue = sc.next();

            switch (pi.keyword) {

            case CCSDS_OPM_VERS:
                file.setFormatVersion(pi.keyValue);
                break;

            case X:
                pi.x = Double.parseDouble(pi.keyValue) * 1000;
                break;

            case Y:
                pi.y = Double.parseDouble(pi.keyValue) * 1000;
                break;

            case Z:
                pi.z = Double.parseDouble(pi.keyValue) * 1000;
                break;

            case X_DOT:
                pi.x_dot = Double.parseDouble(pi.keyValue) * 1000;
                break;

            case Y_DOT:
                pi.y_dot = Double.parseDouble(pi.keyValue) * 1000;
                break;

            case Z_DOT:
                pi.z_dot = Double.parseDouble(pi.keyValue) * 1000;
                break;

            case MAN_EPOCH_IGNITION:
                if (pi.maneuver != null) {
                    file.addManeuver(pi.maneuver);
                }
                pi.maneuver = new OPMFile.Maneuver();
                pi.maneuver.setEpochIgnition(parseDate(pi.keyValue, file.getTimeSystem()));
                if (!pi.commentTmp.isEmpty()) {
                    pi.maneuver.setComment(pi.commentTmp);
                    pi.commentTmp.clear();
                }
                break;

            case MAN_DURATION:
                pi.maneuver.setDuration(Double.parseDouble(pi.keyValue));
                break;

            case MAN_DELTA_MASS:
                pi.maneuver.setDeltaMass(Double.parseDouble(pi.keyValue));
                break;

            case MAN_REF_FRAME:
                final CCSDSFrame manFrame = parseCCSDSFrame(pi.keyValue);
                if (manFrame.isLof()) {
                    pi.maneuver.setRefLofType(manFrame.getLofType());
                } else {
                    pi.maneuver.setRefFrame(manFrame.getFrame(getConventions()));
                }
                break;

            case MAN_DV_1:
                pi.maneuver.setdV(new Vector3D(Double.parseDouble(pi.keyValue) * 1000,
                                               pi.maneuver.getDV().getY(),
                                               pi.maneuver.getDV().getZ()));
                break;

            case MAN_DV_2:
                pi.maneuver.setdV(new Vector3D(pi.maneuver.getDV().getX(),
                                               Double.parseDouble(pi.keyValue) * 1000,
                                               pi.maneuver.getDV().getZ()));
                break;

            case MAN_DV_3:
                pi.maneuver.setdV(new Vector3D(pi.maneuver.getDV().getX(),
                                               pi.maneuver.getDV().getY(),
                                               Double.parseDouble(pi.keyValue) * 1000));
                break;

            default:
                boolean parsed = false;
                parsed = parsed || parseComment(line, pi.keyword, pi.commentTmp);
                parsed = parsed || parseHeaderEntry(pi.keyword, pi.keyValue, file, pi.commentTmp);
                parsed = parsed || parseMetaDataEntry(line, pi.keyword, pi.keyValue, file.getMetaData(), pi.commentTmp);
                parsed = parsed || parseGeneralStateDataEntry(line, pi.keyword, pi.keyValue, file, pi.commentTmp, pi.userDefinedKeyword);
                if (!parsed) {
                    throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, line);
                }
            }

        }

        file.setPosition(new Vector3D(pi.x, pi.y, pi.z));
        file.setVelocity(new Vector3D(pi.x_dot, pi.y_dot, pi.z_dot));
        if (pi.maneuver != null) {
            file.addManeuver(pi.maneuver);
        }
        reader.close();
        return file;
    }

    /** Private class used to stock OPM parsing info.
     * @author sports
     */
    private static class ParseInfo {

        /** OPM file being read. */
        private OPMFile file;

        /** Current line number. */
        private int lineNumber;

        /** Stored keyword. */
        private String keywordTmp;

        /** Keyword of the line being read. */
        private Keyword keyword;

        /** Key value of the line being read. */
        private String keyValue;

        /** Stored comments. */
        private List<String> commentTmp;

        /** First component of position vector. */
        private double x;

        /** Second component of position vector. */
        private double y;

        /** Third component of position vector. */
        private double z;

        /** First component of velocity vector. */
        private double x_dot;

        /** Second component of velocity vector. */
        private double y_dot;

        /** Third component of velocity vector. */
        private double z_dot;

        /** Current maneuver. */
        private OPMFile.Maneuver maneuver;

        /** Current user-defined keyword. */
        private String userDefinedKeyword;

        /** Create a new {@link ParseInfo} object. */
        protected ParseInfo() {
            file       = new OPMFile();
            lineNumber = 0;
            commentTmp = new ArrayList<String>();
            maneuver   = null;
        }
    }
}
