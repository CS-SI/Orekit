/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.ccsds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.AttitudeEphemerisFileParser;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * A parser for the CCSDS AEM (Attitude Ephemeris Message).
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AEMParser extends ADMParser implements AttitudeEphemerisFileParser {

    /** Pattern for dash. */
    private static final Pattern DASH = Pattern.compile("-");

    /** Maximum number of elements in an attitude data line. */
    private static final int MAX_SIZE = 8;

    /** Default interpolation degree. */
    private int interpolationDegree;

    /** Local Spacecraft Body Reference Frame A. */
    private Frame localScBodyReferenceFrameA;

    /** Local Spacecraft Body Reference Frame B. */
    private Frame localScBodyReferenceFrameB;

    /**
     * Simple constructor.
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
     * The IERS conventions to use is not set here. If it is needed in order to
     * parse some reference frames or UT1 time scale, it must be initialized before
     * parsing by calling {@link #withConventions(IERSConventions)}.
     * </p>
     * <p>
     * The international designator parameters (launch year, launch number and
     * launch piece) are not set here. If they are needed, they must be initialized before
     * parsing by calling {@link #withInternationalDesignator(int, int, String)}
     * </p>
     * <p>
     * The default interpolation degree is not set here. It is set to one by default. If another value
     * is needed it must be initialized before parsing by calling {@link #withInterpolationDegree(int)}
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}. See
     * {@link #withDataContext(DataContext)}.
     */
    @DefaultDataContext
    public AEMParser() {
        this(DataContext.getDefault());
    }

    /**
     * Constructor with data context.
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
     * The IERS conventions to use is not set here. If it is needed in order to
     * parse some reference frames or UT1 time scale, it must be initialized before
     * parsing by calling {@link #withConventions(IERSConventions)}.
     * </p>
     * <p>
     * The international designator parameters (launch year, launch number and
     * launch piece) are not set here. If they are needed, they must be initialized before
     * parsing by calling {@link #withInternationalDesignator(int, int, String)}
     * </p>
     * <p>
     * The default interpolation degree is not set here. It is set to one by default. If another value
     * is needed it must be initialized before parsing by calling {@link #withInterpolationDegree(int)}
     * </p>
     *
     * @param dataContext used by the parser.
     * @see #AEMParser()
     * @see #withDataContext(DataContext)
     */
    public AEMParser(final DataContext dataContext) {
        this(AbsoluteDate.FUTURE_INFINITY, Double.NaN, null, true, 0, 0, "", 1, dataContext);
    }

    /**
     * Complete constructor.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param launchYear launch year for TLEs
     * @param launchNumber launch number for TLEs
     * @param launchPiece piece of launch (from "A" to "ZZZ") for TLEs
     * @param interpolationDegree default interpolation degree
     * @param dataContext used to retrieve frames, time scales, etc.
     */
    private AEMParser(final AbsoluteDate missionReferenceDate, final double mu,
                      final IERSConventions conventions, final boolean simpleEOP,
                      final int launchYear, final int launchNumber,
                      final String launchPiece, final int interpolationDegree,
                      final DataContext dataContext) {
        super(missionReferenceDate, mu, conventions, simpleEOP, launchYear, launchNumber,
                launchPiece, dataContext);
        this.interpolationDegree = interpolationDegree;
    }

    /** {@inheritDoc} */
    public AEMParser withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return new AEMParser(newMissionReferenceDate, getMu(), getConventions(), isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece(),
                             getInterpolationDegree(), getDataContext());
    }

    /** {@inheritDoc} */
    public AEMParser withMu(final double newMu) {
        return new AEMParser(getMissionReferenceDate(), newMu, getConventions(), isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece(),
                             getInterpolationDegree(), getDataContext());
    }

    /** {@inheritDoc} */
    public AEMParser withConventions(final IERSConventions newConventions) {
        return new AEMParser(getMissionReferenceDate(), getMu(), newConventions, isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece(),
                             getInterpolationDegree(), getDataContext());
    }

    /** {@inheritDoc} */
    public AEMParser withSimpleEOP(final boolean newSimpleEOP) {
        return new AEMParser(getMissionReferenceDate(), getMu(), getConventions(), newSimpleEOP,
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece(),
                             getInterpolationDegree(), getDataContext());
    }

    /** {@inheritDoc} */
    public AEMParser withInternationalDesignator(final int newLaunchYear,
                                                 final int newLaunchNumber,
                                                 final String newLaunchPiece) {
        return new AEMParser(getMissionReferenceDate(), getMu(), getConventions(), isSimpleEOP(),
                             newLaunchYear, newLaunchNumber, newLaunchPiece,
                             getInterpolationDegree(), getDataContext());
    }

    /** {@inheritDoc} */
    @Override
    public AEMParser withDataContext(final DataContext dataContext) {
        return new AEMParser(getMissionReferenceDate(), getMu(), getConventions(), isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece(),
                             getInterpolationDegree(), dataContext);
    }

    /** Set default interpolation degree.
     * <p>
     * This method may be used to set a default interpolation degree which will be used
     * when no interpolation degree is parsed in the meta-data of the file. Upon instantiation
     * with {@link #AEMParser(DataContext)} the default interpolation degree is one.
     * </p>
     * @param newInterpolationDegree default interpolation degree to use while parsing
     * @return a new instance, with interpolation degree data replaced
     * @see #getInterpolationDegree()
     * @since 10.3
     */
    public AEMParser withInterpolationDegree(final int newInterpolationDegree) {
        return new AEMParser(getMissionReferenceDate(), getMu(), getConventions(), isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece(),
                             newInterpolationDegree, getDataContext());
    }

    /**
     * Set the local spacecraft body reference frame A.
     * <p>
     * This frame corresponds to {@link Keyword#REF_FRAME_A} key in AEM file.
     * This method may be used to set a reference frame "A" which will be used
     * if the frame parsed in the file does not correspond to a default frame available
     * in {@link CCSDSFrame} (e.g. SC_BODY_1, ACTUATOR_1, etc.).
     * According to CCSDS ADM documentation, it is the responsibility of the end user
     * to have an understanding of the location of these frames for their particular object.
     * </p>
     * @param frame the frame to set
     */
    public void setLocalScBodyReferenceFrameA(final Frame frame) {
        this.localScBodyReferenceFrameA = frame;
    }

    /**
     * Set the local spacecraft body reference frame B.
     * <p>
     * This frame corresponds to {@link Keyword#REF_FRAME_B} key in AEM file.
     * This method may be used to set a reference frame "B" which will be used
     * if the frame parsed in the file does not correspond to a default frame available
     * in {@link CCSDSFrame} (e.g. SC_BODY_1, ACTUATOR_1, etc.).
     * According to CCSDS ADM documentation, it is the responsibility of the end user
     * to have an understanding of the location of these frames for their particular object.
     * </p>
     * @param frame the frame to set
     */
    public void setLocalScBodyReferenceFrameB(final Frame frame) {
        this.localScBodyReferenceFrameB = frame;
    }

    /** Get default interpolation degree.
     * @return interpolationDegree default interpolation degree to use while parsing
     * @see #withInterpolationDegree(int)
     * @since 10.3
     */
    public int getInterpolationDegree() {
        return interpolationDegree;
    }

    /** {@inheritDoc} */
    @Override
    public AEMFile parse(final String fileName) {
        return (AEMFile) super.parse(fileName);
    }

    /** {@inheritDoc} */
    @Override
    public AEMFile parse(final InputStream stream) {
        return (AEMFile) super.parse(stream);
    }

    /** {@inheritDoc} */
    @Override
    public AEMFile parse(final InputStream stream, final String fileName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return parse(reader, fileName);
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /** {@inheritDoc} */
    @Override
    public AEMFile parse(final BufferedReader reader, final String fileName) {

        try {

            // initialize internal data structures
            final ParseInfo pi = new ParseInfo();
            pi.fileName = fileName;
            final AEMFile file = pi.file;

            // set the additional data that has been configured prior the parsing by the user.
            pi.file.setMissionReferenceDate(getMissionReferenceDate());
            pi.file.setMu(getMu());
            pi.file.setConventions(getConventions());
            pi.file.setDataContext(getDataContext());

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
                    case CCSDS_AEM_VERS:
                        file.setFormatVersion(pi.keyValue.getDoubleValue());
                        break;

                    case META_START:
                        file.addAttitudeBlock();
                        pi.lastEphemeridesBlock = file.getAttitudeBlocks().get(file.getAttitudeBlocks().size() - 1);
                        pi.lastEphemeridesBlock.getMetaData().setLaunchYear(getLaunchYear());
                        pi.lastEphemeridesBlock.getMetaData().setLaunchNumber(getLaunchNumber());
                        pi.lastEphemeridesBlock.getMetaData().setLaunchPiece(getLaunchPiece());
                        pi.lastEphemeridesBlock.setInterpolationDegree(getInterpolationDegree());
                        break;

                    case REF_FRAME_A:
                        pi.lastEphemeridesBlock.setRefFrameAString(pi.keyValue.getValue());
                        break;

                    case REF_FRAME_B:
                        pi.lastEphemeridesBlock.setRefFrameBString(pi.keyValue.getValue());
                        break;

                    case ATTITUDE_DIR:
                        pi.lastEphemeridesBlock.setAttitudeDirection(pi.keyValue.getValue());
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

                    case ATTITUDE_TYPE:
                        pi.lastEphemeridesBlock.setAttitudeType(pi.keyValue.getValue());
                        break;

                    case QUATERNION_TYPE:
                        final boolean isFirst = (pi.keyValue.getValue().equals("FIRST")) ? true : false;
                        pi.lastEphemeridesBlock.setIsFirst(isFirst);
                        break;

                    case EULER_ROT_SEQ:
                        pi.lastEphemeridesBlock.setEulerRotSeq(pi.keyValue.getValue());
                        pi.lastEphemeridesBlock.setRotationOrder(AEMRotationOrder.getRotationOrder(pi.keyValue.getValue()));
                        break;

                    case RATE_FRAME:
                        pi.lastEphemeridesBlock.setRateFrameString(pi.keyValue.getValue());
                        break;

                    case INTERPOLATION_METHOD:
                        pi.lastEphemeridesBlock.setInterpolationMethod(pi.keyValue.getValue());
                        break;

                    case INTERPOLATION_DEGREE:
                        pi.lastEphemeridesBlock.setInterpolationDegree(Integer.parseInt(pi.keyValue.getValue()));
                        break;

                    case META_STOP:
                        // Set attitude reference frame
                        parseReferenceFrame(pi);
                        // Read attitude ephemeris data lines
                        parseEphemeridesDataLines(reader, pi);
                        break;

                    default:
                        boolean parsed = false;
                        parsed = parsed || parseComment(pi.keyValue, pi.commentTmp);
                        parsed = parsed || parseHeaderEntry(pi.keyValue, file, pi.commentTmp);
                        if (pi.lastEphemeridesBlock != null) {
                            parsed = parsed || parseMetaDataEntry(pi.keyValue,
                                                                  pi.lastEphemeridesBlock.getMetaData(), pi.commentTmp);
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
     * Parse an attitude ephemeris data line and add its content
     * to the attitude ephemerides block.
     * @param reader the reader
     * @param pi the parser info
     * @exception IOException if an error occurs while reading from the stream
     */
    private void parseEphemeridesDataLines(final BufferedReader reader,  final ParseInfo pi)
        throws IOException {

        for (String line = reader.readLine(); line != null; line = reader.readLine()) {

            ++pi.lineNumber;
            if (line.trim().length() > 0) {
                pi.keyValue = new KeyValue(line, pi.lineNumber, pi.fileName);
                if (pi.keyValue.getKeyword() == null) {
                    try (Scanner sc = new Scanner(line)) {
                        final AbsoluteDate date = parseDate(sc.next(), pi.lastEphemeridesBlock.getMetaData().getTimeSystem());
                        // Create an array with the maximum possible size
                        final double[] attitudeData = new double[MAX_SIZE];
                        int index = 0;
                        while (sc.hasNext()) {
                            attitudeData[index++] = Double.parseDouble(sc.next());
                        }
                        final AEMAttitudeType attType = AEMAttitudeType.getAttitudeType(pi.lastEphemeridesBlock.getAttitudeType());
                        final RotationOrder rotationOrder = pi.lastEphemeridesBlock.getRotationOrder();

                        final TimeStampedAngularCoordinates epDataLine = attType.getAngularCoordinates(date, attitudeData,
                                                                                                       pi.lastEphemeridesBlock.isFirst(),
                                                                                                       rotationOrder);
                        pi.lastEphemeridesBlock.getAttitudeDataLines().add(epDataLine);
                        pi.lastEphemeridesBlock.updateAngularDerivativesFilter(attType.getAngularDerivativesFilter());
                    } catch (NumberFormatException nfe) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  pi.lineNumber, pi.fileName, line);
                    }
                } else {
                    switch (pi.keyValue.getKeyword()) {

                        case DATA_START:
                            // Do nothing
                            break;

                        case DATA_STOP:
                            pi.lastEphemeridesBlock.setAttitudeDataLinesComment(pi.commentTmp);
                            pi.commentTmp.clear();
                            //pi.lineNumber--;
                            reader.reset();
                            reader.readLine();
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
     * Parse the reference attitude frame.
     * @param pi the parser info
     */
    private void parseReferenceFrame(final ParseInfo pi) {

        // Reference frame A
        final String frameAString = DASH.matcher(j2000Check(pi.lastEphemeridesBlock.getRefFrameAString())).replaceAll("");
        final Frame frameA = isDefinedFrame(frameAString) ?
                                    CCSDSFrame.valueOf(frameAString).getFrame(getConventions(), isSimpleEOP(), getDataContext()) :
                                        localScBodyReferenceFrameA;

        // Reference frame B
        final String frameBString = DASH.matcher(j2000Check(pi.lastEphemeridesBlock.getRefFrameBString())).replaceAll("");
        final Frame frameB = isDefinedFrame(frameBString) ?
                                    CCSDSFrame.valueOf(frameBString).getFrame(getConventions(), isSimpleEOP(), getDataContext()) :
                                        localScBodyReferenceFrameB;

        // Set the attitude reference frame
        final String direction = pi.lastEphemeridesBlock.getAttitudeDirection();
        pi.lastEphemeridesBlock.setReferenceFrame("A2B".equals(direction) ? frameA : frameB);

    }

    /**
     * Check if frame name is "J2000".
     * <p>
     * If yes, the name is changed to "EME2000" in order to match
     * predefined CCSDS frame names.
     * </p>
     * @param frameName frame name
     * @return the nex name
     */
    private static String j2000Check(final String frameName) {
        return "J2000".equals(frameName) ? "EME2000" : frameName;
    }

    /**
     * Verify if the given frame is defined in predefined CCSDS frames.
     * @param frameName frame name
     * @return true is the frame is known
     */
    private static boolean isDefinedFrame(final String frameName) {
        // Loop on CCSDS frames
        for (CCSDSFrame ccsdsFrame : CCSDSFrame.values()) {
            // CCSDS frame name is defined in enumerate
            if (ccsdsFrame.name().equals(frameName)) {
                return true;
            }
        }
        // No match found
        return false;
    }

    /** Private class used to stock AEM parsing info. */
    private static class ParseInfo {

        /** Ephemerides block being parsed. */
        private AEMFile.AttitudeEphemeridesBlock lastEphemeridesBlock;

        /** Name of the file. */
        private String fileName;

        /** Current line number. */
        private int lineNumber;

        /** AEM file being read. */
        private AEMFile file;

        /** Key value of the line being read. */
        private KeyValue keyValue;

        /** Stored comments. */
        private List<String> commentTmp;

        /** Create a new {@link ParseInfo} object. */
        protected ParseInfo() {
            lineNumber = 0;
            file       = new AEMFile();
            commentTmp = new ArrayList<String>();
        }
    }

    /** Util class to convert the Euler rotation sequence to {@link RotationOrder}. */
    public enum AEMRotationOrder {

        /** This ordered set of rotations is around X, then around Y, then around Z. */
        XYZ("123", RotationOrder.XYZ),

        /** This ordered set of rotations is around X, then around Z, then around Y. */
        XZY("132", RotationOrder.XZY),

        /** This ordered set of rotations is around Y, then around X, then around Z. */
        YXZ("213", RotationOrder.YXZ),

        /** This ordered set of rotations is around Y, then around Z, then around X. */
        YZX("231", RotationOrder.YZX),

        /** This ordered set of rotations is around Z, then around X, then around Y. */
        ZXY("312", RotationOrder.ZXY),

        /** This ordered set of rotations is around Z, then around Y, then around X. */
        ZYX("321", RotationOrder.ZYX),

        /** This ordered set of rotations is around X, then around Y, then around X. */
        XYX("121", RotationOrder.XYX),

        /** This ordered set of rotations is around X, then around Z, then around X. */
        XZX("131", RotationOrder.XZX),

        /** This ordered set of rotations is around Y, then around X, then around Y. */
        YXY("212", RotationOrder.YXY),

        /** This ordered set of rotations is around Y, then around Z, then around Y. */
        YZY("232", RotationOrder.YZY),

        /** This ordered set of rotations is around Z, then around X, then around Z. */
        ZXZ("313", RotationOrder.ZXZ),

        /** This ordered set of rotations is around Z, then around Y, then around Z. */
        ZYZ("323", RotationOrder.ZYZ);

        /** Codes map. */
        private static final Map<String, RotationOrder> CODES_MAP = new HashMap<String, RotationOrder>();
        static {
            for (final AEMRotationOrder type : values()) {
                CODES_MAP.put(type.getName(), type.getRotationOrder());
            }
        }

        /** Rotation order. */
        private final RotationOrder order;

        /** Name. */
        private final String name;

        /**
         * Constructor.
         * @param name name of the rotation
         * @param order rotation order
         */
        AEMRotationOrder(final String name,
                         final RotationOrder order) {
            this.name  = name;
            this.order = order;
        }

        /**
         * Get the name of the AEM rotation order.
         * @return name
         */
        private String getName() {
            return name;
        }

        /**
         * Get the rotation order.
         * @return rotation order
         */
        private RotationOrder getRotationOrder() {
            return order;
        }

        /**
         * Get the rotation order for the given name.
         * @param orderName name of the rotation order (e.g. "123")
         * @return the corresponding rotation order
         */
        public static RotationOrder getRotationOrder(final String orderName) {
            final RotationOrder type = CODES_MAP.get(orderName);
            if (type == null) {
                // Invalid rotation sequence
                throw new OrekitException(OrekitMessages.CCSDS_AEM_INVALID_ROTATION_SEQUENCE, orderName);
            }
            return type;
        }

    }

}
