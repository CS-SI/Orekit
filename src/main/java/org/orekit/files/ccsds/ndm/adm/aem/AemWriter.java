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
import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.TimeConverter;
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
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.files.general.AttitudeEphemerisFile;
import org.orekit.files.general.AttitudeEphemerisFile.SatelliteAttitudeEphemeris;
import org.orekit.files.general.AttitudeEphemerisFileWriter;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

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
 * <p> The AEM header for the whole AEM file is set when calling {@link #writeHeader(Header)},
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
 * <p> The AEM metadata for the whole AEM file is set when calling {@link #newSegment(AemMetadata)},
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
 * TimeConverter#UTC UTC}. The guessing algorithm is not guaranteed to work so it is recommended
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
public class AemWriter extends AbstractMessageWriter implements AttitudeEphemerisFileWriter {

    /** Version number implemented. **/
    public static final double CCSDS_AEM_VERS = 1.0;

    /**
     * Default format used for attitude ephemeris data output: 9 digits
     * after the decimal point and leading space for positive values.
     */
    public static final String DEFAULT_ATTITUDE_FORMAT = "% .9f";

    /** Key width for aligning the '=' sign. */
    public static final int KEY_WIDTH = 20;

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

    /** Current metadata. */
    private final AemMetadata metadata;

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
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.WriterBuilder#buildAemWriter(AemMetadata, String)
     * writerBuilder.buildAemWriter(template, fileName)}.
     * </p>
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param header file header (may be null)
     * @param template template for metadata
     * @param fileName file name for error messages
     * @since 11.0
     */
    public AemWriter(final IERSConventions conventions, final DataContext dataContext,
                     final Header header, final AemMetadata template,
                     final String fileName) {
        super(AemFile.FORMAT_VERSION_KEY, CCSDS_AEM_VERS,
              header,
              new ContextBinding(
                  () -> conventions, () -> true, () -> dataContext,
                  () -> null, template::getTimeSystem,
                  () -> 0.0, () -> 1.0),
              fileName);
        this.metadata = copy(template);
    }

    /** Get current metadata.
     * @return current metadata
     */
    AemMetadata getMetadata() {
        return metadata;
    }

    /** {@inheritDoc}
     * <p>
     * As {@link AttitudeEphemerisFile.SatelliteAttitudeEphemeris} does not have all the entries
     * from {@link AemMetadata}, the only values that will be extracted from the
     * {@code ephemerisFile} will be the start time, stop time, reference frame, interpolation
     * method and interpolation degree. The missing values (like object name, local spacecraft
     * body frame, attitude type...) will be inherited from the template  metadata set at writer
     * {@link #AEMWriter(IERSConventions, DataContext, Header, AemMetadata, String, String) construction}.
     * </p>
     */
    @Override
    public <C extends TimeStampedAngularCoordinates, S extends AttitudeEphemerisFile.AttitudeEphemerisSegment<C>>
        void write(final Appendable appendable, final AttitudeEphemerisFile<C, S> ephemerisFile)
        throws IOException {

        if (appendable == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "writer");
        }

        if (ephemerisFile == null) {
            return;
        }

        final SatelliteAttitudeEphemeris<C, S> satEphem =
                        ephemerisFile.getSatellites().get(metadata.getObjectID());
        if (satEphem == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.VALUE_NOT_FOUND,
                                                     metadata.getObjectID(), "ephemerisFile");
        }

        // Get attitude ephemeris segments to output.
        final List<S> segments = satEphem.getSegments();
        if (segments.isEmpty()) {
            // No data -> No output
            return;
        }

        try (Generator generator = new KvnGenerator(appendable, KEY_WIDTH, getFileName())) {
            writeHeader(generator);

            // Loop on segments
            for (final S segment : segments) {

                // override template metadata with segment values
                metadata.setStartTime(segment.getStart());
                metadata.setStopTime(segment.getStop());
                if (metadata.getEndpoints().getFrameA() == null ||
                    metadata.getEndpoints().getFrameA().asSpacecraftBodyFrame() == null) {
                    // the external frame must be frame A
                    metadata.getEndpoints().setFrameA(FrameFacade.map(segment.getReferenceFrame()));
                } else {
                    // the external frame must be frame B
                    metadata.getEndpoints().setFrameB(FrameFacade.map(segment.getReferenceFrame()));
                }
                metadata.setInterpolationMethod(segment.getInterpolationMethod());
                metadata.setInterpolationDegree(segment.getInterpolationSamples() - 1);
                writeMetadata(generator);

                // Loop on attitude data
                startAttitudeBlock(generator);
                if (segment instanceof AemSegment) {
                    generator.writeComments(((AemSegment) segment).getData());
                }
                for (final TimeStampedAngularCoordinates coordinates : segment.getAngularCoordinates()) {
                    writeAttitudeEphemerisLine(generator, coordinates);
                }
                endAttitudeBlock(generator);
            }
        }

    }

    /** Write an ephemeris segment metadata.
     * @param generator generator to use for producing output
     * @throws IOException if the output stream throws one while writing.
     */
    public void writeMetadata(final Generator generator) throws IOException {

        // Start metadata
        generator.enterSection(generator.getFormat() == FileFormat.KVN ?
                               KvnStructureKey.META.name() :
                               XmlStructureKey.metadata.name());

        generator.writeComments(metadata);

        // objects
        generator.writeEntry(AdmMetadataKey.OBJECT_NAME.name(), metadata.getObjectName(),       true);
        generator.writeEntry(AdmMetadataKey.OBJECT_ID.name(),   metadata.getObjectID(),         true);
        if (metadata.getCenter() != null) {
            generator.writeEntry(AdmMetadataKey.CENTER_NAME.name(), metadata.getCenter().getName(), false);
        }

        // frames
        generator.writeEntry(AemMetadataKey.REF_FRAME_A.name(),  metadata.getEndpoints().getFrameA().getName(),     true);
        generator.writeEntry(AemMetadataKey.REF_FRAME_B.name(),  metadata.getEndpoints().getFrameB().getName(),     true);
        generator.writeEntry(AemMetadataKey.ATTITUDE_DIR.name(), metadata.getEndpoints().isA2b() ? A_TO_B : B_TO_A, true);

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
        generator.writeEntry(AemMetadataKey.ATTITUDE_TYPE.name(), attitudeType.toString(), true);
        if (attitudeType == AttitudeType.QUATERNION ||
            attitudeType == AttitudeType.QUATERNION_DERIVATIVE ||
            attitudeType == AttitudeType.QUATERNION_RATE) {
            generator.writeEntry(AemMetadataKey.QUATERNION_TYPE.name(), metadata.isFirst() ? FIRST : LAST, false);
        }

        if (attitudeType == AttitudeType.EULER_ANGLE ||
            attitudeType == AttitudeType.EULER_ANGLE_RATE) {
            if (metadata.getEulerRotSeq() == null) {
                // the keyword *will* be missing because we cannot set it
                throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD,
                                          AemMetadataKey.EULER_ROT_SEQ.name(), getFileName());
            }
            generator.writeEntry(AemMetadataKey.EULER_ROT_SEQ.name(),
                                 metadata.getEulerRotSeq().name().replace('X', '1').replace('Y', '2').replace('Z', '3'),
                                 false);
        }

        if (attitudeType == AttitudeType.QUATERNION_RATE ||
            attitudeType == AttitudeType.EULER_ANGLE_RATE) {
            generator.writeEntry(AemMetadataKey.RATE_FRAME.name(),
                                 metadata.rateFrameIsA() ? REF_FRAME_A : REF_FRAME_B,
                                 false);
        }

        // interpolation
        generator.writeEntry(AemMetadataKey.INTERPOLATION_METHOD.name(),
                             metadata.getInterpolationMethod(),
                             false);
        generator.writeEntry(AemMetadataKey.INTERPOLATION_DEGREE.name(),
                             Integer.toString(metadata.getInterpolationDegree()),
                             false);

        // Stop metadata
        generator.exitSection();

    }

    /**
     * Write a single attitude ephemeris line according to section 4.2.4 and Table 4-4.
     * @param generator generator to use for producing output
     * @param attitude the attitude information for a given date.
     * @throws IOException if the output stream throws one while writing.
     */
    public void writeAttitudeEphemerisLine(final Generator generator, final TimeStampedAngularCoordinates attitude)
        throws IOException {

        // Epoch
        generator.writeRawData(generator.dateToString(getTimeConverter(), attitude.getDate()));

        // Attitude data in CCSDS units
        final String[] data = metadata.getAttitudeType().createDataFields(metadata.isFirst(),
                                                                          metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                                          metadata.getEulerRotSeq(),
                                                                          metadata.isSpacecraftBodyRate(),
                                                                          attitude);
        final int      size = data.length;
        for (int index = 0; index < size; index++) {
            generator.writeRawData(' ');
            generator.writeRawData(data[index]);
        }

        // end the line
        generator.newLine();

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

    /** Copy a metadata object, making sure mandatory fields have been initialized.
     * @param original original object
     * @return a new copy
     */
    private AemMetadata copy(final AemMetadata original) {

        original.checkMandatoryEntriesExceptDatesAndExternalFrame();

        // allocate new instance
        final AemMetadata copy = new AemMetadata(original.getInterpolationDegree());

        // copy comments
        for (String comment : original.getComments()) {
            copy.addComment(comment);
        }

        // copy object
        copy.setObjectName(original.getObjectName());
        copy.setObjectID(original.getObjectID());
        if (original.getCenter() != null) {
            copy.setCenter(original.getCenter());
        }

        // copy frames (we may copy null references here)
        copy.getEndpoints().setFrameA(original.getEndpoints().getFrameA());
        copy.getEndpoints().setFrameB(original.getEndpoints().getFrameB());
        copy.getEndpoints().setA2b(original.getEndpoints().isA2b());
        copy.setRateFrameIsA(original.rateFrameIsA());

        // copy time system only (ignore times themselves)
        copy.setTimeSystem(original.getTimeSystem());

        // copy attitude definitions
        copy.setAttitudeType(original.getAttitudeType());
        if (original.isFirst() != null) {
            copy.setIsFirst(original.isFirst());
        }
        if (original.getEulerRotSeq() != null) {
            copy.setEulerRotSeq(original.getEulerRotSeq());
        }

        // copy interpolation (degree has already been set up at construction)
        if (original.getInterpolationMethod() != null) {
            copy.setInterpolationMethod(original.getInterpolationMethod());
        }

        return copy;

    }

}
