/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm.aem;

import java.io.IOException;
import java.util.Date;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.adm.AdmMetadataKey;
import org.orekit.files.ccsds.ndm.adm.AttitudeType;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.HeaderKey;
import org.orekit.files.ccsds.section.KvnStructureKey;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.AbstractMessageWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.XmlGenerator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.units.Unit;

/**
 * A writer for Attitude Ephemeris Messsage (AEM) files.
 *
 * <h2> Metadata </h2>
 *
 * <p> The AEM header and metadata used by this writer are described in the following tables.
 * Many metadata items are optional or have default values so they do not need to be specified.
 * At a minimum the user must supply those values that are required and for which no
 * default exits: {@link AdmMetadataKey#OBJECT_NAME}, {@link AdmMetadataKey#OBJECT_ID},
 * {@link AemMetadataKey#START_TIME} and {@link AemMetadataKey#STOP_TIME}.
 * The usage column in the table indicates where the metadata item is used, either in the AEM header
 * or in the metadata section at the start of an AEM attitude segment.
 * </p>
 *
 * <p> The AEM header for the whole AEM file is set when calling {@link #writeMessageHeader(Generator, Header)},
 * the entries are defined in table 4-2 of the ADM standard.
 *
 * <table>
 * <caption>AEM metadata</caption>
 *     <thead>
 *         <tr>
 *             <th>Keyword</th>
 *             <th>Mandatory</th>
 *             <th>Default in Orekit</th>
 *         </tr>
 *    </thead>
 *    <tbody>
 *        <tr>
 *            <td>{@link AemFile#FORMAT_VERSION_KEY CCSDS_AEM_VERS}</td>
 *            <td>Yes</td>
 *            <td>{@link #CCSDS_AEM_VERS}</td>
 *        </tr>
 *        <tr>
 *            <td>{@link HeaderKey#COMMENT}</td>
 *            <td>No</td>
 *            <td>empty</td>
 *        </tr>
 *        <tr>
 *            <td>{@link HeaderKey#CREATION_DATE}</td>
 *            <td>Yes</td>
 *            <td>{@link Date#Date() Now}</td>
 *        </tr>
 *        <tr>
 *            <td>{@link HeaderKey#ORIGINATOR}</td>
 *            <td>Yes</td>
 *            <td>{@link #DEFAULT_ORIGINATOR}</td>
 *        </tr>
 *    </tbody>
 *    </table>
 * </p>
 *
 * <p> The AEM metadata for the AEM file is set when calling {@link #writeSegment(Generator, AemSegment)},
 * the entries are defined in tables 4-3, 4-4 and annex A of the ADM standard.
 *
 * <table>
 * <caption>AEM metadata</caption>
 *     <thead>
 *         <tr>
 *             <th>Keyword</th>
 *             <th>Mandatory</th>
 *             <th>Default in Orekit</th>
 *         </tr>
 *    </thead>
 *    <tbody>
 *        <tr>
 *            <td>{@link MetadataKey#COMMENT}</td>
 *            <td>No</td>
 *            <td>empty</td>
 *        </tr>
 *        <tr>
 *            <td>{@link AdmMetadataKey#OBJECT_NAME}</td>
 *            <td>Yes</td>
 *            <td></td>
 *        </tr>
 *        <tr>
 *            <td>{@link AdmMetadataKey#OBJECT_ID}</td>
 *            <td>Yes</td>
 *            <td></td>
 *        </tr>
 *        <tr>
 *            <td>{@link AdmMetadataKey#CENTER_NAME}</td>
 *            <td>No</td>
 *            <td></td>
 *        </tr>
 *        <tr>
 *            <td>{@link AemMetadataKey#REF_FRAME_A}</td>
 *            <td>Yes</td>
 *            <td></td>
 *        </tr>
 *        <tr>
 *            <td>{@link AemMetadataKey#REF_FRAME_B}</td>
 *            <td>Yes</td>
 *            <td></td>
 *        </tr>
 *        <tr>
 *            <td>{@link AemMetadataKey#ATTITUDE_DIR}</td>
 *            <td>Yes</td>
 *            <td></td>
 *        </tr>
 *        <tr>
 *            <td>{@link MetadataKey#TIME_SYSTEM}</td>
 *            <td>Yes</td>
 *            <td></td>
 *        </tr>
 *        <tr>
 *            <td>{@link AemMetadataKey#START_TIME}</td>
 *            <td>Yes</td>
 *            <td>default to propagation start time (for forward propagation)</td>
 *        </tr>
 *        <tr>
 *            <td>{@link AemMetadataKey#USEABLE_START_TIME}</td>
 *            <td>No</td>
 *            <td></td>
 *        </tr>
 *        <tr>
 *            <td>{@link AemMetadataKey#USEABLE_STOP_TIME}</td>
 *            <td>No</td>
 *            <td></td>
 *        </tr>
 *        <tr>
 *            <td>{@link AemMetadataKey#STOP_TIME}</td>
 *            <td>Yes</td>
 *            <td>default to propagation target time (for forward propagation)</td>
 *        </tr>
 *        <tr>
 *            <td>{@link AemMetadataKey#ATTITUDE_TYPE}</td>
 *            <td>Yes</td>
 *            <td>{@link AttitudeType#QUATERNION_RATE QUATERNION/RATE}</td>
 *        </tr>
 *        <tr>
 *            <td>{@link AemMetadataKey#QUATERNION_TYPE}</td>
 *            <td>No</td>
 *            <td>{@code FIRST}</td>
 *        </tr>
 *        <tr>
 *            <td>{@link AemMetadataKey#EULER_ROT_SEQ}</td>
 *            <td>No</td>
 *            <td></td>
 *        </tr>
 *        <tr>
 *            <td>{@link AemMetadataKey#RATE_FRAME}</td>
 *            <td>No</td>
 *            <td>{@code REF_FRAME_B}</td>
 *        </tr>
 *        <tr>
 *            <td>{@link AemMetadataKey#INTERPOLATION_METHOD}</td>
 *            <td>No</td>
 *            <td></td>
 *        </tr>
 *        <tr>
 *            <td>{@link AemMetadataKey#INTERPOLATION_DEGREE}</td>
 *            <td>No</td>
 *            <td>always set in {@link AemMetadata}</td>
 *        </tr>
 *    </tbody>
 *</table>
 *
 * <p> The {@link MetadataKey#TIME_SYSTEM} must be constant for the whole file and is used
 * to interpret all dates except {@link HeaderKey#CREATION_DATE} which is always in {@link
 * TimeSystem#UTC UTC}. The guessing algorithm is not guaranteed to work so it is recommended
 * to provide values for {@link AdmMetadataKey#CENTER_NAME} and {@link MetadataKey#TIME_SYSTEM}
 * to avoid any bugs associated with incorrect guesses.
 *
 * <p> Standardized values for {@link MetadataKey#TIME_SYSTEM} are GMST, GPS, MET, MRT, SCLK,
 * TAI, TCB, TDB, TT, UT1, and UTC. Standardized values for reference frames
 * are EME2000, GTOD, ICRF, ITRF2000, ITRF-93, ITRF-97, LVLH, RTN, QSW, TOD, TNW, NTW and RSW.
 * Additionally ITRF followed by a four digit year may be used.
 *
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AemWriter extends AbstractMessageWriter<Header, AemSegment, AemFile> {

    /** Version number implemented. **/
    public static final double CCSDS_AEM_VERS = 1.0;

    /** Padding width for aligning the '=' sign. */
    public static final int KVN_PADDING_WIDTH = 20;

    /** Constant for frame A to frame B attitude. */
    private static final String A_TO_B = "A2B";

    /** Constant for frame B to frame A attitude. */
    private static final String B_TO_A = "B2A";

    /** Constant for quaternions with scalar component in first position. */
    private static final String FIRST = "FIRST";

    /** Constant for quaternions with scalar component in last position. */
    private static final String LAST = "LAST";

    /** Constant for angular rates in frame A. */
    private static final String REF_FRAME_A = "REF_FRAME_A";

    /** Constant for angular rates in frame B. */
    private static final String REF_FRAME_B = "REF_FRAME_B";

    /** Prefix for Euler rotations. */
    private static final String ROTATION = "rotation";

    /** Attribute for Euler angles. */
    private static final String ANGLE_ATTRIBUTE = "angle";

    /** Suffix for Euler angles. */
    private static final String ANGLE_SUFFIX = "_ANGLE";

    /**Attribute for Euler rates. */
    private static final String RATE_ATTRIBUTE = "rate";

    /** Suffix for Euler rates. */
    private static final String RATE_SUFFIX = "_RATE";

    /**
     * Constructor used to create a new AEM writer configured with the necessary parameters
     * to successfully fill in all required fields that aren't part of a standard object.
     * <p>
     * If the mandatory header entries are not present (or if header is null),
     * built-in defaults will be used
     * </p>
     * <p>
     * The writer is built from the complete header and partial metadata. The template
     * metadata is used to initialize and independent local copy, that will be updated
     * as new segments are written (with at least the segment start and stop will change,
     * but some other parts may change too). The {@code template} argument itself is not
     * changed.
     * </>
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param spinAxis spin axis in spacecraft body frame
     * @since 11.0
     */
    public AemWriter(final IERSConventions conventions, final DataContext dataContext,
                     final AbsoluteDate missionReferenceDate, final Vector3D spinAxis) {
        super(AemFile.ROOT, AemFile.FORMAT_VERSION_KEY, CCSDS_AEM_VERS,
              new ContextBinding(
                  () -> conventions,
                  () -> true, () -> dataContext, () -> ParsedUnitsBehavior.STRICT_COMPLIANCE,
                  () -> missionReferenceDate, () -> TimeSystem.UTC,
                  () -> 0.0, () -> 1.0, () -> spinAxis));
    }

    /** {@inheritDoc} */
    @Override
    public void writeSegmentContent(final Generator generator, final AemSegment segment) throws IOException {

        final AemMetadata metadata = segment.getMetadata();
        writeMetadata(generator, metadata);

        // Loop on attitude data
        startAttitudeBlock(generator);
        generator.writeComments(((AemSegment) segment).getData().getComments());
        for (final TimeStampedAngularCoordinates coordinates : segment.getAngularCoordinates()) {
            writeAttitudeEphemerisLine(generator, metadata, coordinates);
        }
        endAttitudeBlock(generator);

    }

    /** Write an ephemeris segment metadata.
     * @param generator generator to use for producing output
     * @param metadata metadata to write
     * @throws IOException if the output stream throws one while writing.
     */
    void writeMetadata(final Generator generator, final AemMetadata metadata) throws IOException {

        final ContextBinding oldContext = getContext();
        setContext(new ContextBinding(oldContext::getConventions,
                                      oldContext::isSimpleEOP,
                                      oldContext::getDataContext,
                                      oldContext::getParsedUnitsBehavior,
                                      oldContext::getReferenceDate,
                                      metadata::getTimeSystem,
                                      oldContext::getClockCount,
                                      oldContext::getClockRate,
                                      oldContext::getSpinAxis));

        // Start metadata
        generator.enterSection(generator.getFormat() == FileFormat.KVN ?
                               KvnStructureKey.META.name() :
                               XmlStructureKey.metadata.name());

        generator.writeComments(metadata.getComments());

        // objects
        generator.writeEntry(AdmMetadataKey.OBJECT_NAME.name(), metadata.getObjectName(),       null, true);
        generator.writeEntry(AdmMetadataKey.OBJECT_ID.name(),   metadata.getObjectID(),         null, true);
        if (metadata.getCenter() != null) {
            generator.writeEntry(AdmMetadataKey.CENTER_NAME.name(), metadata.getCenter().getName(), null, false);
        }

        // frames
        generator.writeEntry(AemMetadataKey.REF_FRAME_A.name(),  metadata.getEndpoints().getFrameA().getName(),     null, true);
        generator.writeEntry(AemMetadataKey.REF_FRAME_B.name(),  metadata.getEndpoints().getFrameB().getName(),     null, true);
        generator.writeEntry(AemMetadataKey.ATTITUDE_DIR.name(), metadata.getEndpoints().isA2b() ? A_TO_B : B_TO_A, null, true);

        // time
        generator.writeEntry(MetadataKey.TIME_SYSTEM.name(), metadata.getTimeSystem(), true);
        generator.writeEntry(AemMetadataKey.START_TIME.name(), getTimeConverter(), metadata.getStartTime(), true);
        if (metadata.getUseableStartTime() != null) {
            generator.writeEntry(AemMetadataKey.USEABLE_START_TIME.name(), getTimeConverter(), metadata.getUseableStartTime(), false);
        }
        if (metadata.getUseableStopTime() != null) {
            generator.writeEntry(AemMetadataKey.USEABLE_STOP_TIME.name(), getTimeConverter(), metadata.getUseableStopTime(), false);
        }
        generator.writeEntry(AemMetadataKey.STOP_TIME.name(), getTimeConverter(), metadata.getStopTime(), true);

        // types
        final AttitudeType attitudeType = metadata.getAttitudeType();
        generator.writeEntry(AemMetadataKey.ATTITUDE_TYPE.name(), attitudeType.toString(), null, true);
        if (attitudeType == AttitudeType.QUATERNION ||
            attitudeType == AttitudeType.QUATERNION_DERIVATIVE ||
            attitudeType == AttitudeType.QUATERNION_RATE) {
            generator.writeEntry(AemMetadataKey.QUATERNION_TYPE.name(), metadata.isFirst() ? FIRST : LAST, null, false);
        }

        if (attitudeType == AttitudeType.QUATERNION_RATE ||
            attitudeType == AttitudeType.EULER_ANGLE ||
            attitudeType == AttitudeType.EULER_ANGLE_RATE) {
            if (metadata.getEulerRotSeq() == null) {
                // the keyword *will* be missing because we cannot set it
                throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD,
                                          AemMetadataKey.EULER_ROT_SEQ.name(), generator.getOutputName());
            }
            generator.writeEntry(AemMetadataKey.EULER_ROT_SEQ.name(),
                                 metadata.getEulerRotSeq().name().replace('X', '1').replace('Y', '2').replace('Z', '3'),
                                 null, false);
        }

        if (attitudeType == AttitudeType.QUATERNION_RATE ||
            attitudeType == AttitudeType.EULER_ANGLE_RATE) {
            generator.writeEntry(AemMetadataKey.RATE_FRAME.name(),
                                 metadata.rateFrameIsA() ? REF_FRAME_A : REF_FRAME_B,
                                                         null, false);
        }

        // interpolation
        generator.writeEntry(AemMetadataKey.INTERPOLATION_METHOD.name(),
                             metadata.getInterpolationMethod(),
                             null, false);
        generator.writeEntry(AemMetadataKey.INTERPOLATION_DEGREE.name(),
                             Integer.toString(metadata.getInterpolationDegree()),
                             null, false);

        // Stop metadata
        generator.exitSection();

    }

    /**
     * Write a single attitude ephemeris line according to section 4.2.4 and Table 4-4.
     * @param generator generator to use for producing output
     * @param metadata metadata to use for interpreting data
     * @param attitude the attitude information for a given date
     * @throws IOException if the output stream throws one while writing.
     */
    void writeAttitudeEphemerisLine(final Generator generator, final AemMetadata metadata,
                                    final TimeStampedAngularCoordinates attitude)
        throws IOException {

        // Attitude data in CCSDS units
        final String[] data = metadata.getAttitudeType().createDataFields(metadata.isFirst(),
                                                                          metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                                          metadata.getEulerRotSeq(),
                                                                          metadata.isSpacecraftBodyRate(),
                                                                          getContext().getSpinAxis(),
                                                                          attitude);

        if (generator.getFormat() == FileFormat.KVN) {

            // epoch
            generator.writeRawData(generator.dateToString(getTimeConverter(), attitude.getDate()));

            // data
            final int      size = data.length;
            for (int index = 0; index < size; index++) {
                generator.writeRawData(' ');
                generator.writeRawData(data[index]);
            }

            // end the line
            generator.newLine();

        } else {
            final XmlGenerator xmlGenerator = (XmlGenerator) generator;
            xmlGenerator.enterSection(XmlSubStructureKey.attitudeState.name());
            switch (metadata.getAttitudeType()) {
                case QUATERNION :
                    writeQuaternion(xmlGenerator, metadata.isFirst(), attitude.getDate(), data);
                    break;
                case QUATERNION_DERIVATIVE :
                    writeQuaternionDerivative(xmlGenerator, metadata.isFirst(), attitude.getDate(), data);
                    break;
                case QUATERNION_RATE :
                    writeQuaternionRate(xmlGenerator, metadata.isFirst(), metadata.getEulerRotSeq(), attitude.getDate(), data);
                    break;
                case EULER_ANGLE :
                    writeEulerAngle(xmlGenerator, metadata.getEulerRotSeq(), attitude.getDate(), data);
                    break;
                case EULER_ANGLE_RATE :
                    writeEulerAngleRate(xmlGenerator, metadata.getEulerRotSeq(), attitude.getDate(), data);
                    break;
                case SPIN :
                    writeSpin(xmlGenerator, attitude.getDate(), data);
                    break;
//                case SPIN_NUTATION :
//                    writeSpinNutation(xmlGenerator, attitude.getDate(), data);
//                    break;
                default :
                    // this should never happen
                    throw new OrekitInternalError(null);
            }
            generator.exitSection();
        }

    }

    /** Write a quaternion entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param first flag for scalar component to appear first
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeQuaternion(final XmlGenerator xmlGenerator, final boolean first, final AbsoluteDate epoch, final String[] data)
        throws IOException {

        // wrapping element
        xmlGenerator.enterSection(AttitudeEntryKey.quaternion.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, true);

        // quaternion part
        int i = 0;
        if (first) {
            xmlGenerator.writeEntry(AttitudeEntryKey.QC.name(), data[i++], Unit.ONE, false);
        }
        xmlGenerator.writeEntry(AttitudeEntryKey.Q1.name(), data[i++], Unit.ONE, false);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q2.name(), data[i++], Unit.ONE, false);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q3.name(), data[i++], Unit.ONE, false);
        if (!first) {
            xmlGenerator.writeEntry(AttitudeEntryKey.QC.name(), data[i++], Unit.ONE, false);
        }

        xmlGenerator.exitSection();

    }

    /** Write a quaternion/derivative entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param first flag for scalar component to appear first
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeQuaternionDerivative(final XmlGenerator xmlGenerator, final boolean first, final AbsoluteDate epoch, final String[] data)
        throws IOException {

        // wrapping element
        xmlGenerator.enterSection(AttitudeEntryKey.quaternionDerivative.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, true);
        int i = 0;

        // quaternion part
        xmlGenerator.enterSection(AttitudeEntryKey.quaternion.name());
        if (first) {
            xmlGenerator.writeEntry(AttitudeEntryKey.QC.name(), data[i++], Unit.ONE, true);
        }
        xmlGenerator.writeEntry(AttitudeEntryKey.Q1.name(), data[i++], Unit.ONE, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q2.name(), data[i++], Unit.ONE, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q3.name(), data[i++], Unit.ONE, true);
        if (!first) {
            xmlGenerator.writeEntry(AttitudeEntryKey.QC.name(), data[i++], Unit.ONE, true);
        }
        xmlGenerator.exitSection();

        // derivative part
        xmlGenerator.enterSection(AttitudeEntryKey.quaternionRate.name());
        if (first) {
            xmlGenerator.writeEntry(AttitudeEntryKey.QC_DOT.name(), data[i++], Units.ONE_PER_S, true);
        }
        xmlGenerator.writeEntry(AttitudeEntryKey.Q1_DOT.name(), data[i++], Units.ONE_PER_S, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q2_DOT.name(), data[i++], Units.ONE_PER_S, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q3_DOT.name(), data[i++], Units.ONE_PER_S, true);
        if (!first) {
            xmlGenerator.writeEntry(AttitudeEntryKey.QC_DOT.name(), data[i++], Units.ONE_PER_S, true);
        }
        xmlGenerator.exitSection();

        xmlGenerator.exitSection();

    }

    /** Write a quaternion/rate entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param first flag for scalar component to appear first
     * @param order Euler rotation order
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeQuaternionRate(final XmlGenerator xmlGenerator, final boolean first, final RotationOrder order,
                             final AbsoluteDate epoch, final String[] data)
        throws IOException {

        // wrapping element
        xmlGenerator.enterSection(AttitudeEntryKey.quaternionEulerRate.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, true);
        int i = 0;

        // quaternion part
        xmlGenerator.enterSection(AttitudeEntryKey.quaternion.name());
        if (first) {
            xmlGenerator.writeEntry(AttitudeEntryKey.QC.name(), data[i++], Unit.ONE, true);
        }
        xmlGenerator.writeEntry(AttitudeEntryKey.Q1.name(), data[i++], Unit.ONE, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q2.name(), data[i++], Unit.ONE, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q3.name(), data[i++], Unit.ONE, true);
        if (!first) {
            xmlGenerator.writeEntry(AttitudeEntryKey.QC.name(), data[i++], Unit.ONE, true);
        }
        xmlGenerator.exitSection();

        // derivative part
        xmlGenerator.enterSection(AttitudeEntryKey.rotationRates.name());
        writeEulerRate(xmlGenerator, 0, order.name(), data[i++]);
        writeEulerRate(xmlGenerator, 1, order.name(), data[i++]);
        writeEulerRate(xmlGenerator, 2, order.name(), data[i++]);
        xmlGenerator.exitSection();

        xmlGenerator.exitSection();

    }

    /** Write a Euler angles entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param order Euler rotation order
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeEulerAngle(final XmlGenerator xmlGenerator, final RotationOrder order,
                         final AbsoluteDate epoch, final String[] data)
        throws IOException {

        // wrapping element
        xmlGenerator.enterSection(AttitudeEntryKey.eulerAngle.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, true);
        int i = 0;

        // angle part
        xmlGenerator.enterSection(AttitudeEntryKey.rotationAngles.name());
        writeEulerAngle(xmlGenerator, 0, order.name(), data[i++]);
        writeEulerAngle(xmlGenerator, 1, order.name(), data[i++]);
        writeEulerAngle(xmlGenerator, 2, order.name(), data[i++]);
        xmlGenerator.exitSection();

        xmlGenerator.exitSection();

    }

    /** Write a Euler angles/rates entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param order Euler rotation order
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeEulerAngleRate(final XmlGenerator xmlGenerator, final RotationOrder order,
                             final AbsoluteDate epoch, final String[] data)
        throws IOException {

        // wrapping element
        xmlGenerator.enterSection(AttitudeEntryKey.eulerAngle.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, true);
        int i = 0;

        // angle part
        xmlGenerator.enterSection(AttitudeEntryKey.rotationAngles.name());
        writeEulerAngle(xmlGenerator, 0, order.name(), data[i++]);
        writeEulerAngle(xmlGenerator, 1, order.name(), data[i++]);
        writeEulerAngle(xmlGenerator, 2, order.name(), data[i++]);
        xmlGenerator.exitSection();

        // rates part
        xmlGenerator.enterSection(AttitudeEntryKey.rotationRates.name());
        writeEulerRate(xmlGenerator, 0, order.name(), data[i++]);
        writeEulerRate(xmlGenerator, 1, order.name(), data[i++]);
        writeEulerRate(xmlGenerator, 2, order.name(), data[i++]);
        xmlGenerator.exitSection();

        xmlGenerator.exitSection();

    }

    /** Write a spin entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeSpin(final XmlGenerator xmlGenerator, final AbsoluteDate epoch, final String[] data)
        throws IOException {

        // wrapping element
        xmlGenerator.enterSection(AttitudeEntryKey.spin.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, true);
        int i = 0;
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ALPHA.name(),     data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_DELTA.name(),     data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ANGLE.name(),     data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ANGLE_VEL.name(), data[i++], Units.DEG_PER_S, true);

        xmlGenerator.exitSection();

    }

//    /** Write a spin/nutation entry in XML.
//     * @param xmlGenerator generator to use for producing output
//     * @param epoch of the entry
//     * @param data entry data
//     * @throws IOException if the output stream throws one while writing.
//     */
//    void writeSpinNutation(final XmlGenerator xmlGenerator, final AbsoluteDate epoch, final String[] data)
//        throws IOException {
//
//        // wrapping element
//        xmlGenerator.enterSection(AttitudeEntryKey.spin.name());
//
//        // data part
//        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, true);
//        int i = 0;
//        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ALPHA.name(),     data[i++], Unit.DEGREE,     true);
//        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_DELTA.name(),     data[i++], Unit.DEGREE,     true);
//        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ANGLE.name(),     data[i++], Unit.DEGREE,     true);
//        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ANGLE_VEL.name(), data[i++], Units.DEG_PER_S, true);
//        xmlGenerator.writeEntry(AttitudeEntryKey.NUTATION.name(),       data[i++], Unit.DEGREE,     true);
//        xmlGenerator.writeEntry(AttitudeEntryKey.NUTATION_PER.name(),   data[i++], Unit.SECOND,     true);
//        xmlGenerator.writeEntry(AttitudeEntryKey.NUTATION_PHASE.name(), data[i++], Unit.DEGREE,     true);
//
//        xmlGenerator.exitSection();
//
//    }

    /** Write an angle from an Euler sequence.
     * @param xmlGenerator generator to use
     * @param index angle index
     * @param seq Euler sequence
     * @param angle angle value
     * @throws IOException if the output stream throws one while writing.
     */
    private void writeEulerAngle(final XmlGenerator xmlGenerator, final int index, final String seq, final String angle)
        throws IOException {
        if (xmlGenerator.writeUnits(Unit.DEGREE)) {
            xmlGenerator.writeTwoAttributesElement(ROTATION + (index + 1), angle,
                                                   ANGLE_ATTRIBUTE, seq.charAt(index) + ANGLE_SUFFIX,
                                                   XmlGenerator.UNITS,
                                                   xmlGenerator.siToCcsdsName(Unit.DEGREE.getName()));
        } else {
            xmlGenerator.writeOneAttributeElement(ROTATION + (index + 1), angle,
                                                  ANGLE_ATTRIBUTE, seq.charAt(index) + ANGLE_SUFFIX);
        }
    }

    /** Write a rate from an Euler sequence.
     * @param xmlGenerator generator to use
     * @param index angle index
     * @param seq Euler sequence
     * @param rate rate value
     * @throws IOException if the output stream throws one while writing.
     */
    private void writeEulerRate(final XmlGenerator xmlGenerator, final int index, final String seq, final String rate)
        throws IOException {
        if (xmlGenerator.writeUnits(Units.DEG_PER_S)) {
            xmlGenerator.writeTwoAttributesElement(ROTATION + (index + 1), rate,
                                                   RATE_ATTRIBUTE, seq.charAt(index) + RATE_SUFFIX,
                                                   XmlGenerator.UNITS,
                                                   xmlGenerator.siToCcsdsName(Units.DEG_PER_S.getName()));
        } else {
            xmlGenerator.writeOneAttributeElement(ROTATION + (index + 1), rate,
                                                  RATE_ATTRIBUTE, seq.charAt(index) + RATE_SUFFIX);
        }
    }

    /** Start of an attitude block.
     * @param generator generator to use for producing output
     * @throws IOException if the output stream throws one while writing.
     */
    void startAttitudeBlock(final Generator generator) throws IOException {
        generator.enterSection(generator.getFormat() == FileFormat.KVN ?
                               KvnStructureKey.DATA.name() :
                               XmlStructureKey.data.name());
    }

    /** End of an attitude block.
     * @param generator generator to use for producing output
     * @throws IOException if the output stream throws one while writing.
     */
    void endAttitudeBlock(final Generator generator) throws IOException {
        generator.exitSection();
    }

}
