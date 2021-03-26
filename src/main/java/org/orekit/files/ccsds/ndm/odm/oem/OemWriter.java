/* Copyright 2016 Applied Defense Solutions (ADS)
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * ADS licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.files.ccsds.ndm.odm.oem;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hipparchus.linear.RealMatrix;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.ndm.adm.AdmMetadataKey;
import org.orekit.files.ccsds.ndm.odm.CartesianCovariance;
import org.orekit.files.ccsds.ndm.odm.CartesianCovarianceKey;
import org.orekit.files.ccsds.ndm.odm.CommonMetadataKey;
import org.orekit.files.ccsds.ndm.odm.OdmMetadataKey;
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
import org.orekit.files.general.EphemerisFile;
import org.orekit.files.general.EphemerisFile.SatelliteEphemeris;
import org.orekit.files.general.EphemerisFileWriter;
import org.orekit.frames.Frame;
import org.orekit.time.TimeScale;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.units.Unit;

/**
 * An OEM Writer class that can take in a general {@link EphemerisFile} object
 * and export it as a valid OEM file.
 *
 * <h2> Metadata </h2>
 *
 * <p> The OEM metadata used by this writer is described in the following table. Many
 * metadata items are optional or have default values so they do not need to be specified.
 * At a minimum the user must supply those values that are required and for which no
 * default exits: {@link OdmMetadataKey#OBJECT_NAME}, and {@link CommonMetadataKey#OBJECT_ID}. The usage
 * column in the table indicates where the metadata item is used, either in the OEM header
 * or in the metadata section at the start of an OEM ephemeris segment.
 *
 * <p> The OEM metadata for the whole OEM file is set in the {@link
 * #StreamingOemWriter(Appendable, TimeScale, Map) constructor}. Any of the metadata may
 * be overridden for a particular segment using the {@code metadata} argument to {@link
 * #newSegment(Frame, Map)}.
 *
 * <table>
 * <caption>OEM metadata</caption>
 *     <thead>
 *         <tr>
 *             <th>Keyword</th>
 *             <th>Usage</th>
 *             <th>Obligatory</th>
 *             <th>Default</th>
 *             <th>Reference</th>
 *    </thead>
 *    <tbody>
 *        <tr>
 *            <td>{@code CCSDS_OEM_VERS}</td>
 *            <td>Header</td>
 *            <td>Yes</td>
 *            <td>{@link OemFile#FORMAT_VERSION_KEY}</td>
 *            <td>Table 5-2</td>
 *        <tr>
 *            <td>{@code COMMENT}</td>
 *            <td>Header</td>
 *            <td>No</td>
 *            <td></td>
 *            <td>Table 5-2</td>
 *        <tr>
 *            <td>{@link HeaderKey#CREATION_DATE}</td>
 *            <td>Header</td>
 *            <td>Yes</td>
 *            <td>{@link Date#Date() Now}</td>
 *            <td>Table 5.2, 6.5.9</td>
 *        <tr>
 *            <td>{@link HeaderKey#ORIGINATOR}</td>
 *            <td>Header</td>
 *            <td>Yes</td>
 *            <td>{@link #DEFAULT_ORIGINATOR}</td>
 *            <td>Table 5-2</td>
 *        <tr>
 *            <td>{@link OdmMetadataKey#OBJECT_NAME}</td>
 *            <td>Segment</td>
 *            <td>Yes</td>
 *            <td></td>
 *            <td>Table 5-3</td>
 *        <tr>
 *            <td>{@link CommonMetadataKey#OBJECT_ID}</td>
 *            <td>Segment</td>
 *            <td>Yes</td>
 *            <td></td>
 *            <td>Table 5-3</td>
 *        <tr>
 *            <td>{@link CommonMetadataKey#CENTER_NAME}</td>
 *            <td>Segment</td>
 *            <td>Yes</td>
 *            <td>Guessed from the {@link #newSegment(Frame, Map) segment}'s {@code frame}</td>
 *            <td>Table 5-3</td>
 *        <tr>
 *            <td>{@link CommonMetadataKey#REF_FRAME}</td>
 *            <td>Segment</td>
 *            <td>Yes</td>
 *            <td>Guessed from the {@link #newSegment(Frame, Map) segment}'s {@code frame}</td>
 *            <td>Table 5-3, Annex A</td>
 *        <tr>
 *            <td>{@link CommonMetadataKey#REF_FRAME_EPOCH}</td>
 *            <td>Segment</td>
 *            <td>No</td>
 *            <td></td>
 *            <td>Table 5-3, 6.5.9</td>
 *        <tr>
 *            <td>{@link MetadataKey#TIME_SYSTEM}</td>
 *            <td>Segment</td>
 *            <td>Yes</td>
 *            <td></td>
 *        <tr>
 *            <td>{@link OemMetadataKey#START_TIME}</td>
 *            <td>Segment</td>
 *            <td>Yes</td>
 *            <td></td>
 *            <td>Table 5-3, 6.5.9</td>
 *        <tr>
 *            <td>{@link OemMetadataKey#USEABLE_START_TIME}</td>
 *            <td>Segment</td>
 *            <td>No</td>
 *            <td></td>
 *            <td>Table 5-3, 6.5.9</td>
 *        <tr>
 *            <td>{@link OemMetadataKey#STOP_TIME}</td>
 *            <td>Segment</td>
 *            <td>Yes</td>
 *            <td></td>
 *            <td>Table 5-3, 6.5.9</td>
 *        <tr>
 *            <td>{@link OemMetadataKey#USEABLE_STOP_TIME}</td>
 *            <td>Segment</td>
 *            <td>No</td>
 *            <td></td>
 *            <td>Table 5-3, 6.5.9</td>
 *        <tr>
 *            <td>{@link OemMetadataKey#INTERPOLATION}</td>
 *            <td>Segment</td>
 *            <td>No</td>
 *            <td></td>
 *            <td>Table 5-3</td>
 *        <tr>
 *            <td>{@link OemMetadataKey#INTERPOLATION_DEGREE}</td>
 *            <td>Segment</td>
 *            <td>No</td>
 *            <td></td>
 *            <td>Table 5-3</td>
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
 * @author Hank Grabowski
 * @author Evan Ward
 * @since 9.0
 * @see <a href="https://public.ccsds.org/Pubs/502x0b2c1.pdf">CCSDS 502.0-B-2 Orbit Data
 *      Messages</a>
 * @see <a href="https://public.ccsds.org/Pubs/500x0g4.pdf">CCSDS 500.0-G-4 Navigation
 *      Data Definitions and Conventions</a>
 * @see StreamingOemWriter
 */
public class OemWriter extends AbstractMessageWriter implements EphemerisFileWriter {

    /** Version number implemented. **/
    public static final double CCSDS_OEM_VERS = 3.0;

    /** Default file name for error messages. */
    public static final String DEFAULT_FILE_NAME = "<OEM output>";

    /**
     * Default format used for position data output: 6 digits
     * after the decimal point and leading space for positive values.
     */
    public static final String DEFAULT_POSITION_FORMAT = "% .6f";

    /**
     * Default format used for velocity data output: 9 digits
     * after the decimal point and leading space for positive values.
     */
    public static final String DEFAULT_VELOCITY_FORMAT = "% .9f";

    /**
     * Default format used for acceleration data output: 12 significant digits
     * and leading space for positive values.
     */
    public static final String DEFAULT_ACCELERATION_FORMAT = "% .12e";

    /**
     * Default format used for covariance data output: 7 significant digits
     * and leading space for positive values.
     */
    public static final String DEFAULT_COVARIANCE_FORMAT = "% .7e";

    /** Key width for aligning the '=' sign. */
    public static final int KEY_WIDTH = 20;

    /** Current metadata. */
    private final OemMetadata metadata;

    /** Format for position ephemeris data output. */
    private final String positionFormat;

    /** Format for velocity ephemeris data output. */
    private final String velocityFormat;

    /** Format for acceleration ephemeris data output. */
    private final String accelerationFormat;

    /** Format for acovariance data output. */
    private final String covarianceFormat;

    /**
     * Standard default constructor that creates a writer with default
     * configurations.
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param header file header (may be null)
     * @param template template for metadata
     * @since 11.0
     */
    public OemWriter(final IERSConventions conventions, final DataContext dataContext,
                     final Header header, final OemMetadata template) {
        this(conventions, dataContext, header, template, DEFAULT_FILE_NAME,
             DEFAULT_POSITION_FORMAT, DEFAULT_VELOCITY_FORMAT,
             DEFAULT_ACCELERATION_FORMAT, DEFAULT_COVARIANCE_FORMAT);
    }

    /**
     * Constructor used to create a new OEM writer configured with the necessary parameters
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
     * @param header file header (may be null)
     * @param template template for metadata
     * @param fileName file name for error messages
     * @param positionFormat {@link java.util.Formatter format parameters} for
     *                       position ephemeris data output
     * @param velocityFormat {@link java.util.Formatter format parameters} for
     *                       velocity ephemeris data output
     * @param accelerationFormat {@link java.util.Formatter format parameters} for
     *                       acceleration ephemeris data output
     * @param covarianceFormat {@link java.util.Formatter format parameters} for
     *                       covariance data output
     * @since 11.0
     */
    public OemWriter(final IERSConventions conventions, final DataContext dataContext,
                     final Header header, final OemMetadata template,
                     final String fileName, final String positionFormat,
                     final String velocityFormat, final String accelerationFormat,
                     final String covarianceFormat) {
        super(OemFile.FORMAT_VERSION_KEY, CCSDS_OEM_VERS,
              header,
              new ContextBinding(
                  () -> conventions, () -> true, () -> dataContext,
                  () -> null, template::getTimeSystem, () -> 0.0, () -> 1.0),
              fileName);
        this.metadata           = copy(template);
        this.positionFormat     = positionFormat;
        this.velocityFormat     = velocityFormat;
        this.accelerationFormat = accelerationFormat;
        this.covarianceFormat   = covarianceFormat;
    }

    /** Get current metadata.
     * @return current metadata
     */
    OemMetadata getMetadata() {
        return metadata;
    }

    /** {@inheritDoc}
     * <p>
     * As {@link EphemerisFile.SatelliteEphemeris} does not have all the entries
     * from {@link OemMetadata}, the only values that will be extracted from the
     * {@code ephemerisFile} will be the start time, stop time, reference frame, interpolation
     * method and interpolation degree. The missing values (like object name, local spacecraft
     * body frame...) will be inherited from the template  metadata set at writer
     * {@link #OEMWriter(IERSConventions, DataContext, Header, OemMetadata, String, String) construction}.
     * </p>
     */
    @Override
    public <C extends TimeStampedPVCoordinates, S extends EphemerisFile.EphemerisSegment<C>>
        void write(final Appendable appendable, final EphemerisFile<C, S> ephemerisFile)
        throws IOException {

        if (appendable == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_ARGUMENT, "writer");
        }

        if (ephemerisFile == null) {
            return;
        }

        final SatelliteEphemeris<C, S> satEphem = ephemerisFile.getSatellites().get(metadata.getObjectID());
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
                if (segment instanceof OemSegment) {
                    final OemSegment oemSegment = (OemSegment) segment;
                    metadata.setReferenceFrame(oemSegment.getMetadata().getReferenceFrame());
                } else {
                    metadata.setReferenceFrame(FrameFacade.map(segment.getFrame()));
                }
                metadata.setStartTime(segment.getStart());
                metadata.setStopTime(segment.getStop());
                metadata.setInterpolationDegree(segment.getInterpolationSamples() - 1);
                writeMetadata(generator);

                if (segment instanceof OemSegment) {
                    // write data comments
                    generator.writeComments(((OemSegment) segment).getData().getComments());
                }

                // Loop on orbit data
                final CartesianDerivativesFilter filter = segment.getAvailableDerivatives();
                if (filter == CartesianDerivativesFilter.USE_P) {
                    throw new OrekitException(OrekitMessages.MISSING_VELOCITY);
                }
                final boolean useAcceleration = filter.equals(CartesianDerivativesFilter.USE_PVA);
                for (final TimeStampedPVCoordinates coordinates : segment.getCoordinates()) {
                    writeOrbitEphemerisLine(generator, coordinates, useAcceleration);
                }

                if (segment instanceof OemSegment) {
                    // output covariance data
                    final List<CartesianCovariance> covariances = ((OemSegment) segment).getCovarianceMatrices();
                    boolean continuation = false;
                    if (!covariances.isEmpty()) {
                        if (generator.getFormat() == FileFormat.KVN) {
                            generator.enterSection(OemFile.COVARIANCE_KVN);
                            for (final CartesianCovariance covariance : covariances) {
                                if (continuation) {
                                    generator.newLine();
                                }
                                generator.writeEntry(CartesianCovarianceKey.EPOCH.name(),
                                                     getTimeConverter(), covariance.getEpoch(),
                                                     true);
                                if (covariance.getReferenceFrame() != metadata.getReferenceFrame()) {
                                    generator.writeEntry(CartesianCovarianceKey.COV_REF_FRAME.name(),
                                                         covariance.getReferenceFrame().getName(),
                                                         false);
                                }
                                final RealMatrix m = covariance.getCovarianceMatrix();
                                for (int i = 0; i < m.getRowDimension(); ++i) {

                                    // write triangular matrix entries
                                    for (int j = 0; j <= i; ++j) {
                                        if (j > 0) {
                                            generator.writeRawData(' ');
                                        }
                                        generator.writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE,
                                                                             covarianceFormat,
                                                                             Units.KM2.fromSI(m.getEntry(i, j))));
                                    }

                                    // end the line
                                    generator.newLine();

                                }
                                continuation = true;
                            }
                            generator.exitSection();
                        } else {
                            // TODO: write covariance in OEM XML files
                            throw new OrekitInternalError(null);
                        }
                    }
                }

            }
        }

    }

    /** Write an ephemeris segment metadata.
     * @param generator generator to use for producing output
     * @throws IOException if the output stream throws one while writing.
     */
    void writeMetadata(final Generator generator)
        throws IOException {

        // add an empty line for presentation
        generator.newLine();

        // Start metadata
        generator.enterSection(generator.getFormat() == FileFormat.KVN ?
                               KvnStructureKey.META.name() :
                               XmlStructureKey.metadata.name());

        generator.writeComments(metadata.getComments());

        // objects
        generator.writeEntry(OdmMetadataKey.OBJECT_NAME.name(),    metadata.getObjectName(), true);
        generator.writeEntry(CommonMetadataKey.OBJECT_ID.name(),   metadata.getObjectID(),   true);
        generator.writeEntry(CommonMetadataKey.CENTER_NAME.name(), metadata.getCenter().getName(), false);

        // frames
        generator.writeEntry(CommonMetadataKey.REF_FRAME.name(), metadata.getReferenceFrame().getName(), true);
        if (metadata.getFrameEpoch() != null) {
            generator.writeEntry(CommonMetadataKey.REF_FRAME_EPOCH.name(),
                                 getTimeConverter(), metadata.getFrameEpoch(),
                                 false);
        }

        // time
        generator.writeEntry(MetadataKey.TIME_SYSTEM.name(), metadata.getTimeSystem(), true);
        generator.writeEntry(OemMetadataKey.START_TIME.name(), getTimeConverter(), metadata.getStartTime(), true);
        if (metadata.getUseableStartTime() != null) {
            generator.writeEntry(OemMetadataKey.USEABLE_START_TIME.name(), getTimeConverter(), metadata.getUseableStartTime(), false);
        }
        if (metadata.getUseableStopTime() != null) {
            generator.writeEntry(OemMetadataKey.USEABLE_STOP_TIME.name(), getTimeConverter(), metadata.getUseableStopTime(), false);
        }
        generator.writeEntry(OemMetadataKey.STOP_TIME.name(), getTimeConverter(), metadata.getStopTime(), true);

        // interpolation
        generator.writeEntry(OemMetadataKey.INTERPOLATION.name(), metadata.getInterpolationMethod(), false);
        generator.writeEntry(OemMetadataKey.INTERPOLATION_DEGREE.name(),
                             Integer.toString(metadata.getInterpolationDegree()),
                             false);

        // Stop metadata
        generator.exitSection();

        // add an empty line for presentation
        generator.newLine();

    }

    /**
     * Write a single orbit ephemeris line .
     * @param generator generator to use for producing output
     * @param coordinates orbit information for a given date
     * @param useAcceleration is true, the acceleration data must be used
     * @throws IOException if the output stream throws one while writing.
     */
    void writeOrbitEphemerisLine(final Generator generator,
                                 final TimeStampedPVCoordinates coordinates,
                                 final boolean useAcceleration)
        throws IOException {

        // Epoch
        generator.writeRawData(generator.dateToString(getTimeConverter(), coordinates.getDate()));

        // Position data in km
        generator.writeRawData(' ');
        generator.writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, positionFormat,
                                             Unit.KILOMETRE.fromSI(coordinates.getPosition().getX())));
        generator.writeRawData(' ');
        generator.writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, positionFormat,
                                             Unit.KILOMETRE.fromSI(coordinates.getPosition().getY())));
        generator.writeRawData(' ');
        generator.writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, positionFormat,
                                             Unit.KILOMETRE.fromSI(coordinates.getPosition().getZ())));

        // Velocity data in km/s
        generator.writeRawData(' ');
        generator.writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, velocityFormat,
                                             Units.KM_PER_S.fromSI(coordinates.getVelocity().getX())));
        generator.writeRawData(' ');
        generator.writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, velocityFormat,
                                             Units.KM_PER_S.fromSI(coordinates.getVelocity().getY())));
        generator.writeRawData(' ');
        generator.writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, velocityFormat,
                                             Units.KM_PER_S.fromSI(coordinates.getVelocity().getZ())));

        // Acceleration data in km/s²
        if (useAcceleration) {
            generator.writeRawData(' ');
            generator.writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, accelerationFormat,
                                                 Units.KM_PER_S2.fromSI(coordinates.getAcceleration().getX())));
            generator.writeRawData(' ');
            generator.writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, accelerationFormat,
                                                 Units.KM_PER_S2.fromSI(coordinates.getAcceleration().getY())));
            generator.writeRawData(' ');
            generator.writeRawData(String.format(AccurateFormatter.STANDARDIZED_LOCALE, accelerationFormat,
                                                 Units.KM_PER_S2.fromSI(coordinates.getAcceleration().getZ())));
        }

        // end the line
        generator.newLine();

    }

    /** Start of a data block.
     * @param generator generator to use for producing output
     * @throws IOException if the output stream throws one while writing.
     */
    void startData(final Generator generator) throws IOException {
        if (generator.getFormat() == FileFormat.XML) {
            generator.enterSection(XmlStructureKey.data.name());
        }
    }

    /** End of an attitude block.
     * @param generator generator to use for producing output
     * @throws IOException if the output stream throws one while writing.
     */
    void endData(final Generator generator) throws IOException {
        if (generator.getFormat() == FileFormat.XML) {
            generator.exitSection();
        }
    }

    /** Copy a metadata object, making sure mandatory fields have been initialized.
     * @param original original object
     * @return a new copy
     */
    private OemMetadata copy(final OemMetadata original) {

        original.checkMandatoryEntriesExceptDates();

        // allocate new instance
        final OemMetadata copy = new OemMetadata(original.getInterpolationDegree());

        // copy comments
        for (String comment : original.getComments()) {
            copy.addComment(comment);
        }

        // copy object
        copy.setObjectName(original.getObjectName());
        copy.setObjectID(original.getObjectID());
        if (original.getCenter().getName() != null) {
            copy.setCenter(original.getCenter());
        }

        // copy frames
        copy.setFrameEpoch(original.getFrameEpoch());
        copy.setReferenceFrame(original.getReferenceFrame());

        // copy time system only (ignore times themselves)
        copy.setTimeSystem(original.getTimeSystem());

        // copy interpolation (degree has already been set up at construction)
        if (original.getInterpolationMethod() != null) {
            copy.setInterpolationMethod(original.getInterpolationMethod());
        }

        return copy;

    }

}
