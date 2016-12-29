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

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** A parser for the CCSDS OPM (Orbit Parameter Message).
 * @author sports
 * @since 6.1
 */
public class OPMParser extends ODMParser {

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
    public OPMParser() {
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
    private OPMParser(final AbsoluteDate missionReferenceDate, final double mu,
                      final IERSConventions conventions, final boolean simpleEOP,
                      final int launchYear, final int launchNumber, final String launchPiece) {
        super(missionReferenceDate, mu, conventions, simpleEOP, launchYear, launchNumber, launchPiece);
    }

    /** {@inheritDoc} */
    public OPMParser withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return new OPMParser(newMissionReferenceDate, getMu(), getConventions(), isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OPMParser withMu(final double newMu) {
        return new OPMParser(getMissionReferenceDate(), newMu, getConventions(), isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OPMParser withConventions(final IERSConventions newConventions) {
        return new OPMParser(getMissionReferenceDate(), getMu(), newConventions, isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OPMParser withSimpleEOP(final boolean newSimpleEOP) {
        return new OPMParser(getMissionReferenceDate(), getMu(), getConventions(), newSimpleEOP,
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OPMParser withInternationalDesignator(final int newLaunchYear,
                                                 final int newLaunchNumber,
                                                 final String newLaunchPiece) {
        return new OPMParser(getMissionReferenceDate(), getMu(), getConventions(), isSimpleEOP(),
                             newLaunchYear, newLaunchNumber, newLaunchPiece);
    }

    /** {@inheritDoc} */
    @Override
    public OPMFile parse(final String fileName) throws OrekitException {
        return (OPMFile) super.parse(fileName);
    }

    /** {@inheritDoc} */
    @Override
    public OPMFile parse(final InputStream stream) throws OrekitException {
        return (OPMFile) super.parse(stream);
    }

    /** {@inheritDoc} */
    public OPMFile parse(final InputStream stream, final String fileName) throws OrekitException {

        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            // initialize internal data structures
            final ParseInfo pi = new ParseInfo();
            pi.fileName = fileName;
            final OPMFile file = pi.file;

            // set the additional data that has been configured prior the parsing by the user.
            pi.file.setMissionReferenceDate(getMissionReferenceDate());
            pi.file.setMuSet(getMu());
            pi.file.setConventions(getConventions());
            pi.file.getMetaData().setLaunchYear(getLaunchYear());
            pi.file.getMetaData().setLaunchNumber(getLaunchNumber());
            pi.file.getMetaData().setLaunchPiece(getLaunchPiece());

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

                    case CCSDS_OPM_VERS:
                        file.setFormatVersion(pi.keyValue.getDoubleValue());
                        break;

                    case X:
                        pi.x = pi.keyValue.getDoubleValue() * 1000;
                        break;

                    case Y:
                        pi.y = pi.keyValue.getDoubleValue() * 1000;
                        break;

                    case Z:
                        pi.z = pi.keyValue.getDoubleValue() * 1000;
                        break;

                    case X_DOT:
                        pi.x_dot = pi.keyValue.getDoubleValue() * 1000;
                        break;

                    case Y_DOT:
                        pi.y_dot = pi.keyValue.getDoubleValue() * 1000;
                        break;

                    case Z_DOT:
                        pi.z_dot = pi.keyValue.getDoubleValue() * 1000;
                        break;

                    case MAN_EPOCH_IGNITION:
                        if (pi.maneuver != null) {
                            file.addManeuver(pi.maneuver);
                        }
                        pi.maneuver = new OPMFile.Maneuver();
                        pi.maneuver.setEpochIgnition(parseDate(pi.keyValue.getValue(), file.getMetaData().getTimeSystem()));
                        if (!pi.commentTmp.isEmpty()) {
                            pi.maneuver.setComment(pi.commentTmp);
                            pi.commentTmp.clear();
                        }
                        break;

                    case MAN_DURATION:
                        pi.maneuver.setDuration(pi.keyValue.getDoubleValue());
                        break;

                    case MAN_DELTA_MASS:
                        pi.maneuver.setDeltaMass(pi.keyValue.getDoubleValue());
                        break;

                    case MAN_REF_FRAME:
                        final CCSDSFrame manFrame = parseCCSDSFrame(pi.keyValue.getValue());
                        if (manFrame.isLof()) {
                            pi.maneuver.setRefLofType(manFrame.getLofType());
                        } else {
                            pi.maneuver.setRefFrame(manFrame.getFrame(getConventions(), isSimpleEOP()));
                        }
                        break;

                    case MAN_DV_1:
                        pi.maneuver.setdV(new Vector3D(pi.keyValue.getDoubleValue() * 1000,
                                                       pi.maneuver.getDV().getY(),
                                                       pi.maneuver.getDV().getZ()));
                        break;

                    case MAN_DV_2:
                        pi.maneuver.setdV(new Vector3D(pi.maneuver.getDV().getX(),
                                                       pi.keyValue.getDoubleValue() * 1000,
                                                       pi.maneuver.getDV().getZ()));
                        break;

                    case MAN_DV_3:
                        pi.maneuver.setdV(new Vector3D(pi.maneuver.getDV().getX(),
                                                       pi.maneuver.getDV().getY(),
                                                       pi.keyValue.getDoubleValue() * 1000));
                        break;

                    default:
                        boolean parsed = false;
                        parsed = parsed || parseComment(pi.keyValue, pi.commentTmp);
                        parsed = parsed || parseHeaderEntry(pi.keyValue, file, pi.commentTmp);
                        parsed = parsed || parseMetaDataEntry(pi.keyValue, file.getMetaData(), pi.commentTmp);
                        parsed = parsed || parseGeneralStateDataEntry(pi.keyValue, file, pi.commentTmp);
                        if (!parsed) {
                            throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, line);
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
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /** Private class used to stock OPM parsing info.
     * @author sports
     */
    private static class ParseInfo {

        /** OPM file being read. */
        private OPMFile file;

        /** Name of the file. */
        private String fileName;

        /** Current line number. */
        private int lineNumber;

        /** Key value of the line being read. */
        private KeyValue keyValue;

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

        /** Create a new {@link ParseInfo} object. */
        protected ParseInfo() {
            file       = new OPMFile();
            lineNumber = 0;
            commentTmp = new ArrayList<String>();
            maneuver   = null;
        }
    }
}
