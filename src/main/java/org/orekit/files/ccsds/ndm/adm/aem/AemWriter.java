/* Copyright 2002-2023 CS GROUP
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
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitInternalError;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.adm.AdmCommonMetadataKey;
import org.orekit.files.ccsds.ndm.adm.AdmHeader;
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
 * default exits: {@link AdmMetadataKey#OBJECT_NAME}, {@link AdmCommonMetadataKey#OBJECT_ID},
 * {@link AemMetadataKey#START_TIME} and {@link AemMetadataKey#STOP_TIME}.
 * The usage column in the table indicates where the metadata item is used, either in the AEM header
 * or in the metadata section at the start of an AEM attitude segment.
 * </p>
 *
 * <p> The AEM header for the whole AEM file is set when calling {@link #writeHeader(Generator, Header)},
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
 *            <td>{@link Aem#FORMAT_VERSION_KEY CCSDS_AEM_VERS}</td>
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
 *
 * <p> The AEM metadata for the AEM file is set when calling {@link #writeSegmentContent(Generator, double, AemSegment)},
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
 *            <td>{@link AdmCommonMetadataKey#OBJECT_ID}</td>
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
 *            <td>{@link AttitudeType#QUATERNION_DERIVATIVE QUATERNION/DERIVATIVE}</td>
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
public class AemWriter extends AbstractMessageWriter<AdmHeader, AemSegment, Aem> {

    /** Version number implemented. **/
    public static final double CCSDS_AEM_VERS = 2.0;

    /** Padding width for aligning the '=' sign. */
    public static final int KVN_PADDING_WIDTH = 20;

    /** Constant for frame A to frame B attitude. */
    private static final String A_TO_B = "A2B";

    /** Constant for frame B to frame A attitude. */
    private static final String B_TO_A = "B2A";

    /** Constant for quaternions with scalar component in  position. */
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
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @since 11.0
     */
    public AemWriter(final IERSConventions conventions, final DataContext dataContext,
                     final AbsoluteDate missionReferenceDate) {
        super(Aem.ROOT, Aem.FORMAT_VERSION_KEY, CCSDS_AEM_VERS,
              new ContextBinding(
                  () -> conventions,
                  () -> true, () -> dataContext, () -> ParsedUnitsBehavior.STRICT_COMPLIANCE,
                  () -> missionReferenceDate, () -> TimeSystem.UTC,
                  () -> 0.0, () -> 1.0));
    }

    /** {@inheritDoc} */
    @Override
    protected void writeSegmentContent(final Generator generator, final double formatVersion,
                                       final AemSegment segment)
        throws IOException {

        final AemMetadata metadata = segment.getMetadata();
        writeMetadata(generator, formatVersion, metadata);

        // Loop on attitude data
        startAttitudeBlock(generator);
        generator.writeComments(((AemSegment) segment).getData().getComments());
        for (final TimeStampedAngularCoordinates coordinates : segment.getAngularCoordinates()) {
            writeAttitudeEphemerisLine(generator, formatVersion, metadata, coordinates);
        }
        endAttitudeBlock(generator);

    }

    /** Write an ephemeris segment metadata.
     * @param generator generator to use for producing output
     * @param formatVersion format version
     * @param metadata metadata to write
     * @throws IOException if the output stream throws one while writing.
     */
    void writeMetadata(final Generator generator, final double formatVersion, final AemMetadata metadata)
        throws IOException {

        final ContextBinding oldContext = getContext();
        setContext(new ContextBinding(oldContext::getConventions,
                                      oldContext::isSimpleEOP,
                                      oldContext::getDataContext,
                                      oldContext::getParsedUnitsBehavior,
                                      oldContext::getReferenceDate,
                                      metadata::getTimeSystem,
                                      oldContext::getClockCount,
                                      oldContext::getClockRate));

        // Start metadata
        generator.enterSection(generator.getFormat() == FileFormat.KVN ?
                               KvnStructureKey.META.name() :
                               XmlStructureKey.metadata.name());

        generator.writeComments(metadata.getComments());

        // objects
        generator.writeEntry(AdmMetadataKey.OBJECT_NAME.name(),     metadata.getObjectName(), null, true);
        generator.writeEntry(AdmCommonMetadataKey.OBJECT_ID.name(), metadata.getObjectID(),   null, true);
        if (metadata.getCenter() != null) {
            generator.writeEntry(AdmMetadataKey.CENTER_NAME.name(), metadata.getCenter().getName(), null, false);
        }

        // frames
        generator.writeEntry(AemMetadataKey.REF_FRAME_A.name(),  metadata.getEndpoints().getFrameA().getName(),     null, true);
        generator.writeEntry(AemMetadataKey.REF_FRAME_B.name(),  metadata.getEndpoints().getFrameB().getName(),     null, true);
        if (formatVersion < 2.0) {
            generator.writeEntry(AemMetadataKey.ATTITUDE_DIR.name(), metadata.getEndpoints().isA2b() ? A_TO_B : B_TO_A, null, true);
        }

        // time
        generator.writeEntry(MetadataKey.TIME_SYSTEM.name(), metadata.getTimeSystem(), true);
        generator.writeEntry(AemMetadataKey.START_TIME.name(), getTimeConverter(), metadata.getStartTime(), false, true);
        if (metadata.getUseableStartTime() != null) {
            generator.writeEntry(AemMetadataKey.USEABLE_START_TIME.name(), getTimeConverter(), metadata.getUseableStartTime(), false, false);
        }
        if (metadata.getUseableStopTime() != null) {
            generator.writeEntry(AemMetadataKey.USEABLE_STOP_TIME.name(), getTimeConverter(), metadata.getUseableStopTime(), false, false);
        }
        generator.writeEntry(AemMetadataKey.STOP_TIME.name(), getTimeConverter(), metadata.getStopTime(), false, true);

        // types
        final AttitudeType attitudeType = metadata.getAttitudeType();
        generator.writeEntry(AemMetadataKey.ATTITUDE_TYPE.name(), attitudeType.getName(formatVersion), null, true);
        if (formatVersion < 2.0) {
            if (attitudeType == AttitudeType.QUATERNION ||
                attitudeType == AttitudeType.QUATERNION_DERIVATIVE ||
                attitudeType == AttitudeType.QUATERNION_ANGVEL) {
                generator.writeEntry(AemMetadataKey.QUATERNION_TYPE.name(), metadata.isFirst() ? FIRST : LAST, null, false);
            }
        }

        if (attitudeType == AttitudeType.QUATERNION_EULER_RATES ||
            attitudeType == AttitudeType.EULER_ANGLE            ||
            attitudeType == AttitudeType.EULER_ANGLE_DERIVATIVE ||
            attitudeType == AttitudeType.EULER_ANGLE_ANGVEL) {
            if (formatVersion < 2.0) {
                generator.writeEntry(AemMetadataKey.EULER_ROT_SEQ.name(),
                                     metadata.getEulerRotSeq().name().replace('X', '1').replace('Y', '2').replace('Z', '3'),
                                     null, false);
            } else {
                generator.writeEntry(AemMetadataKey.EULER_ROT_SEQ.name(),
                                     metadata.getEulerRotSeq().name(),
                                     null, false);
            }
        }

        if (formatVersion < 2 && attitudeType == AttitudeType.EULER_ANGLE_DERIVATIVE) {
            generator.writeEntry(AemMetadataKey.RATE_FRAME.name(),
                                 metadata.rateFrameIsA() ? REF_FRAME_A : REF_FRAME_B,
                                 null, false);
        }

        if (attitudeType == AttitudeType.QUATERNION_ANGVEL ||
            attitudeType == AttitudeType.EULER_ANGLE_ANGVEL) {
            generator.writeEntry(AemMetadataKey.ANGVEL_FRAME.name(),
                                 metadata.getFrameAngvelFrame().getName(),
                                 null, true);
        }

        // interpolation
        if (metadata.getInterpolationMethod() != null) {
            generator.writeEntry(AemMetadataKey.INTERPOLATION_METHOD.name(),
                                 metadata.getInterpolationMethod(),
                                 null, true);
            generator.writeEntry(AemMetadataKey.INTERPOLATION_DEGREE.name(),
                                 Integer.toString(metadata.getInterpolationDegree()),
                                 null, true);
        }

        // Stop metadata
        generator.exitSection();

    }

    /**
     * Write a single attitude ephemeris line according to section 4.2.4 and Table 4-4.
     * @param generator generator to use for producing output
     * @param formatVersion format version to use
     * @param metadata metadata to use for interpreting data
     * @param attitude the attitude information for a given date
     * @throws IOException if the output stream throws one while writing.
     */
    void writeAttitudeEphemerisLine(final Generator generator, final double formatVersion,
                                    final AemMetadata metadata,
                                    final TimeStampedAngularCoordinates attitude)
        throws IOException {

        // Attitude data in CCSDS units
        final String[] data = metadata.getAttitudeType().createDataFields(metadata.isFirst(),
                                                                          metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                                          metadata.getEulerRotSeq(),
                                                                          metadata.isSpacecraftBodyRate(),
                                                                          attitude);

        if (generator.getFormat() == FileFormat.KVN) {

            // epoch
            generator.writeRawData(generator.dateToString(getTimeConverter(), attitude.getDate()));

            // data
            for (int index = 0; index < data.length; index++) {
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
                    writeQuaternion(xmlGenerator, formatVersion, metadata.isFirst(), attitude.getDate(), data);
                    break;
                case QUATERNION_DERIVATIVE :
                    writeQuaternionDerivative(xmlGenerator, formatVersion, metadata.isFirst(), attitude.getDate(), data);
                    break;
                case QUATERNION_EULER_RATES :
                    writeQuaternionEulerRates(xmlGenerator, metadata.isFirst(), metadata.getEulerRotSeq(), attitude.getDate(), data);
                    break;
                case QUATERNION_ANGVEL :
                    writeQuaternionAngularVelocity(xmlGenerator, attitude.getDate(), data);
                    break;
                case EULER_ANGLE :
                    writeEulerAngle(xmlGenerator, formatVersion, metadata.getEulerRotSeq(), attitude.getDate(), data);
                    break;
                case EULER_ANGLE_DERIVATIVE :
                    writeEulerAngleDerivative(xmlGenerator, formatVersion, metadata.getEulerRotSeq(), attitude.getDate(), data);
                    break;
                case EULER_ANGLE_ANGVEL :
                    writeEulerAngleAngularVelocity(xmlGenerator, formatVersion, metadata.getEulerRotSeq(), attitude.getDate(), data);
                    break;
                case SPIN :
                    writeSpin(xmlGenerator, attitude.getDate(), data);
                    break;
                case SPIN_NUTATION :
                    writeSpinNutation(xmlGenerator, attitude.getDate(), data);
                    break;
                case SPIN_NUTATION_MOMENTUM :
                    writeSpinNutationMomentum(xmlGenerator, attitude.getDate(), data);
                    break;
                default :
                    // this should never happen
                    throw new OrekitInternalError(null);
            }
            generator.exitSection();
        }

    }

    /** Write a quaternion entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param formatVersion format version to use
     * @param first flag for scalar component to appear first (only relevant in ADM V1)
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeQuaternion(final XmlGenerator xmlGenerator, final double formatVersion,
                         final boolean first, final AbsoluteDate epoch, final String[] data)
        throws IOException {

        xmlGenerator.enterSection(formatVersion < 2.0 ?
                                  AttitudeEntryKey.quaternionState.name() :
                                  AttitudeEntryKey.quaternionEphemeris.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, false, true);

        // wrapping element
        xmlGenerator.enterSection(AttitudeEntryKey.quaternion.name());

        // quaternion part
        int i = 0;
        if (formatVersion < 2.0 && first) {
            xmlGenerator.writeEntry(AttitudeEntryKey.QC.name(), data[i++], Unit.ONE, false);
        }
        xmlGenerator.writeEntry(AttitudeEntryKey.Q1.name(), data[i++], Unit.ONE, false);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q2.name(), data[i++], Unit.ONE, false);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q3.name(), data[i++], Unit.ONE, false);
        if (!(formatVersion < 2.0 && first)) {
            xmlGenerator.writeEntry(AttitudeEntryKey.QC.name(), data[i++], Unit.ONE, false);
        }

        xmlGenerator.exitSection();
        xmlGenerator.exitSection();

    }

    /** Write a quaternion/derivative entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param formatVersion format version to use
     * @param first flag for scalar component to appear first (only relevant in ADM V1)
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeQuaternionDerivative(final XmlGenerator xmlGenerator, final double formatVersion,
                                   final boolean first, final AbsoluteDate epoch, final String[] data)
        throws IOException {

        // wrapping element
        xmlGenerator.enterSection(AttitudeEntryKey.quaternionDerivative.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, false, true);
        int i = 0;

        // quaternion part
        xmlGenerator.enterSection(AttitudeEntryKey.quaternion.name());
        if (formatVersion < 2.0 && first) {
            xmlGenerator.writeEntry(AttitudeEntryKey.QC.name(), data[i++], Unit.ONE, true);
        }
        xmlGenerator.writeEntry(AttitudeEntryKey.Q1.name(), data[i++], Unit.ONE, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q2.name(), data[i++], Unit.ONE, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q3.name(), data[i++], Unit.ONE, true);
        if (!(formatVersion < 2.0 && first)) {
            xmlGenerator.writeEntry(AttitudeEntryKey.QC.name(), data[i++], Unit.ONE, true);
        }
        xmlGenerator.exitSection();

        // derivative part
        xmlGenerator.enterSection(formatVersion < 2.0 ?
                                  AttitudeEntryKey.quaternionRate.name() :
                                  AttitudeEntryKey.quaternionDot.name());
        if (formatVersion < 2.0 && first) {
            xmlGenerator.writeEntry(AttitudeEntryKey.QC_DOT.name(), data[i++], Units.ONE_PER_S, true);
        }
        xmlGenerator.writeEntry(AttitudeEntryKey.Q1_DOT.name(), data[i++], Units.ONE_PER_S, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q2_DOT.name(), data[i++], Units.ONE_PER_S, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q3_DOT.name(), data[i++], Units.ONE_PER_S, true);
        if (!(formatVersion < 2.0 && first)) {
            xmlGenerator.writeEntry(AttitudeEntryKey.QC_DOT.name(), data[i++], Units.ONE_PER_S, true);
        }
        xmlGenerator.exitSection();

        xmlGenerator.exitSection();

    }

    /** Write a quaternion/Euler rates entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param first flag for scalar component to appear first (only relevant in ADM V1)
     * @param order Euler rotation order
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeQuaternionEulerRates(final XmlGenerator xmlGenerator, final boolean first, final RotationOrder order,
                                   final AbsoluteDate epoch, final String[] data)
        throws IOException {

        // wrapping element
        xmlGenerator.enterSection(AttitudeEntryKey.quaternionEulerRate.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, false, true);
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
        writeLabeledEulerRate(xmlGenerator, 0, order.name(), data[i++]);
        writeLabeledEulerRate(xmlGenerator, 1, order.name(), data[i++]);
        writeLabeledEulerRate(xmlGenerator, 2, order.name(), data[i++]);
        xmlGenerator.exitSection();

        xmlGenerator.exitSection();

    }

    /** Write a quaternion/rate entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeQuaternionAngularVelocity(final XmlGenerator xmlGenerator,
                                        final AbsoluteDate epoch, final String[] data)
        throws IOException {

        // wrapping element
        xmlGenerator.enterSection(AttitudeEntryKey.quaternionAngVel.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, false, true);
        int i = 0;

        // quaternion part
        xmlGenerator.enterSection(AttitudeEntryKey.quaternion.name());
        xmlGenerator.writeEntry(AttitudeEntryKey.Q1.name(), data[i++], Unit.ONE, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q2.name(), data[i++], Unit.ONE, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.Q3.name(), data[i++], Unit.ONE, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.QC.name(), data[i++], Unit.ONE, true);
        xmlGenerator.exitSection();

        // angular velocity part
        xmlGenerator.enterSection(AttitudeEntryKey.angVel.name());
        xmlGenerator.writeEntry(AttitudeEntryKey.ANGVEL_X.name(), data[i++], Units.DEG_PER_S, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.ANGVEL_Y.name(), data[i++], Units.DEG_PER_S, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.ANGVEL_Z.name(), data[i++], Units.DEG_PER_S, true);
        xmlGenerator.exitSection();

        xmlGenerator.exitSection();

    }

    /** Write a Euler angles entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param formatVersion format version to use
     * @param order Euler rotation order
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeEulerAngle(final XmlGenerator xmlGenerator, final double formatVersion,
                         final RotationOrder order, final AbsoluteDate epoch, final String[] data)
        throws IOException {

        // wrapping element
        xmlGenerator.enterSection(AttitudeEntryKey.eulerAngle.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, false, true);
        int i = 0;

        // angle part
        if (formatVersion < 2.0) {
            xmlGenerator.enterSection(AttitudeEntryKey.rotationAngles.name());
            writeLabeledEulerAngle(xmlGenerator, 0, order.name(), data[i++]);
            writeLabeledEulerAngle(xmlGenerator, 1, order.name(), data[i++]);
            writeLabeledEulerAngle(xmlGenerator, 2, order.name(), data[i++]);
            xmlGenerator.exitSection();
        } else {
            xmlGenerator.writeEntry(AttitudeEntryKey.ANGLE_1.name(), data[i++], Unit.DEGREE, true);
            xmlGenerator.writeEntry(AttitudeEntryKey.ANGLE_2.name(), data[i++], Unit.DEGREE, true);
            xmlGenerator.writeEntry(AttitudeEntryKey.ANGLE_3.name(), data[i++], Unit.DEGREE, true);
        }

        xmlGenerator.exitSection();

    }

    /** Write a Euler angles entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param formatVersion format version to use
     * @param order Euler rotation order
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeEulerAngleDerivative(final XmlGenerator xmlGenerator, final double formatVersion,
                                   final RotationOrder order, final AbsoluteDate epoch, final String[] data)
        throws IOException {

        // wrapping element
        xmlGenerator.enterSection(formatVersion < 2.0 ?
                                  AttitudeEntryKey.eulerAngleRate.name() :
                                  AttitudeEntryKey.eulerAngleDerivative.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, false, true);
        int i = 0;

        // angle part
        if (formatVersion < 2.0) {
            xmlGenerator.enterSection(AttitudeEntryKey.rotationAngles.name());
            writeLabeledEulerAngle(xmlGenerator, 0, order.name(), data[i++]);
            writeLabeledEulerAngle(xmlGenerator, 1, order.name(), data[i++]);
            writeLabeledEulerAngle(xmlGenerator, 2, order.name(), data[i++]);
            xmlGenerator.exitSection();
            xmlGenerator.enterSection(AttitudeEntryKey.rotationRates.name());
            writeLabeledEulerRate(xmlGenerator, 0, order.name(), data[i++]);
            writeLabeledEulerRate(xmlGenerator, 1, order.name(), data[i++]);
            writeLabeledEulerRate(xmlGenerator, 2, order.name(), data[i++]);
            xmlGenerator.exitSection();
        } else {
            xmlGenerator.writeEntry(AttitudeEntryKey.ANGLE_1.name(),     data[i++], Unit.DEGREE,     true);
            xmlGenerator.writeEntry(AttitudeEntryKey.ANGLE_2.name(),     data[i++], Unit.DEGREE,     true);
            xmlGenerator.writeEntry(AttitudeEntryKey.ANGLE_3.name(),     data[i++], Unit.DEGREE,     true);
            xmlGenerator.writeEntry(AttitudeEntryKey.ANGLE_1_DOT.name(), data[i++], Units.DEG_PER_S, true);
            xmlGenerator.writeEntry(AttitudeEntryKey.ANGLE_2_DOT.name(), data[i++], Units.DEG_PER_S, true);
            xmlGenerator.writeEntry(AttitudeEntryKey.ANGLE_3_DOT.name(), data[i++], Units.DEG_PER_S, true);
        }

        xmlGenerator.exitSection();

    }

    /** Write a Euler angles/angular velocity entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param formatVersion format version to use
     * @param order Euler rotation order
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeEulerAngleAngularVelocity(final XmlGenerator xmlGenerator, final double formatVersion, final RotationOrder order,
                                        final AbsoluteDate epoch, final String[] data)
        throws IOException {

        // wrapping element
        xmlGenerator.enterSection(AttitudeEntryKey.eulerAngleAngVel.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, false, true);
        int i = 0;

        // angle part
        xmlGenerator.writeEntry(AttitudeEntryKey.ANGLE_1.name(), data[i++], Unit.DEGREE, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.ANGLE_2.name(), data[i++], Unit.DEGREE, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.ANGLE_3.name(), data[i++], Unit.DEGREE, true);

        // angular velocity part
        xmlGenerator.writeEntry(AttitudeEntryKey.ANGVEL_X.name(), data[i++], Units.DEG_PER_S, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.ANGVEL_Y.name(), data[i++], Units.DEG_PER_S, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.ANGVEL_Z.name(), data[i++], Units.DEG_PER_S, true);

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
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, false, true);
        int i = 0;
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ALPHA.name(),     data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_DELTA.name(),     data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ANGLE.name(),     data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ANGLE_VEL.name(), data[i++], Units.DEG_PER_S, true);

        xmlGenerator.exitSection();

    }

    /** Write a spin/nutation entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeSpinNutation(final XmlGenerator xmlGenerator, final AbsoluteDate epoch, final String[] data)
        throws IOException {

        // wrapping element
        xmlGenerator.enterSection(AttitudeEntryKey.spinNutation.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, false, true);
        int i = 0;
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ALPHA.name(),     data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_DELTA.name(),     data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ANGLE.name(),     data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ANGLE_VEL.name(), data[i++], Units.DEG_PER_S, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.NUTATION.name(),       data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.NUTATION_PER.name(),   data[i++], Unit.SECOND,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.NUTATION_PHASE.name(), data[i++], Unit.DEGREE,     true);

        xmlGenerator.exitSection();

    }

    /** Write a spin/nutation/momentum entry in XML.
     * @param xmlGenerator generator to use for producing output
     * @param epoch of the entry
     * @param data entry data
     * @throws IOException if the output stream throws one while writing.
     */
    void writeSpinNutationMomentum(final XmlGenerator xmlGenerator, final AbsoluteDate epoch, final String[] data)
        throws IOException {

        // wrapping element
        xmlGenerator.enterSection(AttitudeEntryKey.spinNutationMom.name());

        // data part
        xmlGenerator.writeEntry(AttitudeEntryKey.EPOCH.name(), getTimeConverter(), epoch, false, true);
        int i = 0;
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ALPHA.name(),     data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_DELTA.name(),     data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ANGLE.name(),     data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.SPIN_ANGLE_VEL.name(), data[i++], Units.DEG_PER_S, true);
        xmlGenerator.writeEntry(AttitudeEntryKey.MOMENTUM_ALPHA.name(), data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.MOMENTUM_DELTA.name(), data[i++], Unit.DEGREE,     true);
        xmlGenerator.writeEntry(AttitudeEntryKey.NUTATION_VEL.name(),   data[i++], Units.DEG_PER_S, true);

        xmlGenerator.exitSection();

    }

    /** Write an angle from an Euler sequence.
     * @param xmlGenerator generator to use
     * @param index angle index
     * @param seq Euler sequence
     * @param angle angle value
     * @throws IOException if the output stream throws one while writing.
     */
    private void writeLabeledEulerAngle(final XmlGenerator xmlGenerator, final int index,
                                        final String seq, final String angle)
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
    private void writeLabeledEulerRate(final XmlGenerator xmlGenerator, final int index, final String seq, final String rate)
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
