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

import org.hipparchus.linear.RealMatrix;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.odm.CartesianCovariance;
import org.orekit.files.ccsds.ndm.odm.CartesianCovarianceKey;
import org.orekit.files.ccsds.ndm.odm.CommonMetadataKey;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.ndm.odm.OdmMetadataKey;
import org.orekit.files.ccsds.ndm.odm.StateVectorKey;
import org.orekit.files.ccsds.section.HeaderKey;
import org.orekit.files.ccsds.section.KvnStructureKey;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.AbstractMessageWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.units.Unit;

/**
 * A writer for Orbit Ephemeris Message (OEM) files.
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
 *            <td>{@link Oem#FORMAT_VERSION_KEY}</td>
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
 *            <td></td>
 *            <td>Table 5-3</td>
 *        <tr>
 *            <td>{@link CommonMetadataKey#REF_FRAME}</td>
 *            <td>Segment</td>
 *            <td>Yes</td>
 *            <td></td>
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
 * TimeSystem#UTC UTC}. The guessing algorithm is not guaranteed to work so it is recommended
 * to provide values for {@link CommonMetadataKey#CENTER_NAME} and {@link MetadataKey#TIME_SYSTEM}
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
public class OemWriter extends AbstractMessageWriter<OdmHeader, OemSegment, Oem> {

    /** Version number implemented. **/
    public static final double CCSDS_OEM_VERS = 3.0;

    /** Default file name for error messages. */
    public static final String DEFAULT_FILE_NAME = "<OEM output>";

    /** Padding width for aligning the '=' sign. */
    public static final int KVN_PADDING_WIDTH = 20;

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
     * </p>
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.WriterBuilder#buildOemWriter()
     * writerBuilder.buildOemWriter()}.
     * </p>
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @since 11.0
     * @see #DEFAULT_FILE_NAME
     */
    public OemWriter(final IERSConventions conventions, final DataContext dataContext,
                     final AbsoluteDate missionReferenceDate) {
        super(Oem.ROOT, Oem.FORMAT_VERSION_KEY, CCSDS_OEM_VERS,
              new ContextBinding(
                  () -> conventions, () -> true, () -> dataContext,
                  () -> ParsedUnitsBehavior.STRICT_COMPLIANCE,
                  () -> missionReferenceDate, () -> TimeSystem.UTC, () -> 0.0, () -> 1.0));
    }

    /** {@inheritDoc} */
    @Override
    protected void writeSegmentContent(final Generator generator, final double formatVersion,
                                       final OemSegment segment)
        throws IOException {

        final OemMetadata metadata = segment.getMetadata();
        writeMetadata(generator, metadata);

        startData(generator);

        // write data comments
        generator.writeComments(segment.getData().getComments());

        // Loop on orbit data
        final CartesianDerivativesFilter filter = segment.getAvailableDerivatives();
        if (filter == CartesianDerivativesFilter.USE_P) {
            throw new OrekitException(OrekitMessages.MISSING_VELOCITY);
        }
        final boolean useAcceleration = filter.equals(CartesianDerivativesFilter.USE_PVA);
        for (final TimeStampedPVCoordinates coordinates : segment.getCoordinates()) {
            writeOrbitEphemerisLine(generator, metadata, coordinates, useAcceleration);
        }

        // output covariance data
        writeCovariances(generator, segment.getMetadata(), segment.getData().getCovarianceMatrices());

        endData(generator);

    }

    /** Write an ephemeris segment metadata.
     * @param generator generator to use for producing output
     * @param metadata metadata to write
     * @throws IOException if the output stream throws one while writing.
     */
    void writeMetadata(final Generator generator, final OemMetadata metadata)
        throws IOException {

        // add an empty line for presentation
        generator.newLine();

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
        generator.writeEntry(OdmMetadataKey.OBJECT_NAME.name(),    metadata.getObjectName(),       null, true);
        generator.writeEntry(CommonMetadataKey.OBJECT_ID.name(),   metadata.getObjectID(),         null, true);
        generator.writeEntry(CommonMetadataKey.CENTER_NAME.name(), metadata.getCenter().getName(), null, false);

        // frames
        generator.writeEntry(CommonMetadataKey.REF_FRAME.name(), metadata.getReferenceFrame().getName(), null, true);
        if (metadata.getFrameEpoch() != null) {
            generator.writeEntry(CommonMetadataKey.REF_FRAME_EPOCH.name(),
                                 getTimeConverter(), metadata.getFrameEpoch(),
                                 true, false);
        }

        // time
        generator.writeEntry(MetadataKey.TIME_SYSTEM.name(), metadata.getTimeSystem(), true);
        generator.writeEntry(OemMetadataKey.START_TIME.name(), getTimeConverter(), metadata.getStartTime(), false, true);
        if (metadata.getUseableStartTime() != null) {
            generator.writeEntry(OemMetadataKey.USEABLE_START_TIME.name(), getTimeConverter(), metadata.getUseableStartTime(), false, false);
        }
        if (metadata.getUseableStopTime() != null) {
            generator.writeEntry(OemMetadataKey.USEABLE_STOP_TIME.name(), getTimeConverter(), metadata.getUseableStopTime(), false, false);
        }
        generator.writeEntry(OemMetadataKey.STOP_TIME.name(), getTimeConverter(), metadata.getStopTime(), false, true);

        // interpolation
        generator.writeEntry(OemMetadataKey.INTERPOLATION.name(), metadata.getInterpolationMethod(), false);
        // treat degree < 0 as equivalent to null
        if (metadata.getInterpolationDegree() >= 0) {
            generator.writeEntry(OemMetadataKey.INTERPOLATION_DEGREE.name(),
                    Integer.toString(metadata.getInterpolationDegree()),
                    null, false);
        }

        // Stop metadata
        generator.exitSection();

        // add an empty line for presentation
        generator.newLine();

    }

    /**
     * Write a single orbit ephemeris line .
     * @param generator generator to use for producing output
     * @param metadata metadata to use for interpreting data
     * @param coordinates orbit information for a given date
     * @param useAcceleration is true, the acceleration data must be used
     * @throws IOException if the output stream throws one while writing.
     */
    void writeOrbitEphemerisLine(final Generator generator, final OemMetadata metadata,
                                 final TimeStampedPVCoordinates coordinates,
                                 final boolean useAcceleration)
        throws IOException {

        if (generator.getFormat() == FileFormat.KVN) {

            // Epoch
            generator.writeRawData(generator.dateToString(getTimeConverter(), coordinates.getDate()));

            // Position data in km
            generator.writeRawData(' ');
            generator.writeRawData(String.format(AccurateFormatter.format(Unit.KILOMETRE.fromSI(coordinates.getPosition().getX()))));
            generator.writeRawData(' ');
            generator.writeRawData(String.format(AccurateFormatter.format(Unit.KILOMETRE.fromSI(coordinates.getPosition().getY()))));
            generator.writeRawData(' ');
            generator.writeRawData(String.format(AccurateFormatter.format(Unit.KILOMETRE.fromSI(coordinates.getPosition().getZ()))));

            // Velocity data in km/s
            generator.writeRawData(' ');
            generator.writeRawData(String.format(AccurateFormatter.format(Units.KM_PER_S.fromSI(coordinates.getVelocity().getX()))));
            generator.writeRawData(' ');
            generator.writeRawData(String.format(AccurateFormatter.format(Units.KM_PER_S.fromSI(coordinates.getVelocity().getY()))));
            generator.writeRawData(' ');
            generator.writeRawData(String.format(AccurateFormatter.format(Units.KM_PER_S.fromSI(coordinates.getVelocity().getZ()))));

            // Acceleration data in km/s²
            if (useAcceleration) {
                generator.writeRawData(' ');
                generator.writeRawData(String.format(AccurateFormatter.format(Units.KM_PER_S2.fromSI(coordinates.getAcceleration().getX()))));
                generator.writeRawData(' ');
                generator.writeRawData(String.format(AccurateFormatter.format(Units.KM_PER_S2.fromSI(coordinates.getAcceleration().getY()))));
                generator.writeRawData(' ');
                generator.writeRawData(String.format(AccurateFormatter.format(Units.KM_PER_S2.fromSI(coordinates.getAcceleration().getZ()))));
            }

            // end the line
            generator.newLine();
        } else {
            generator.enterSection(OemDataSubStructureKey.stateVector.name());

            // Epoch
            generator.writeEntry(StateVectorKey.EPOCH.name(), getTimeConverter(), coordinates.getDate(), false, true);

            // Position data in km
            generator.writeEntry(StateVectorKey.X.name(), coordinates.getPosition().getX(), Unit.KILOMETRE, true);
            generator.writeEntry(StateVectorKey.Y.name(), coordinates.getPosition().getY(), Unit.KILOMETRE, true);
            generator.writeEntry(StateVectorKey.Z.name(), coordinates.getPosition().getZ(), Unit.KILOMETRE, true);

            // Velocity data in km/s
            generator.writeEntry(StateVectorKey.X_DOT.name(), coordinates.getVelocity().getX(), Units.KM_PER_S, true);
            generator.writeEntry(StateVectorKey.Y_DOT.name(), coordinates.getVelocity().getY(), Units.KM_PER_S, true);
            generator.writeEntry(StateVectorKey.Z_DOT.name(), coordinates.getVelocity().getZ(), Units.KM_PER_S, true);

            // Acceleration data in km/s²
            if (useAcceleration) {
                generator.writeEntry(StateVectorKey.X_DDOT.name(), coordinates.getAcceleration().getX(), Units.KM_PER_S2, true);
                generator.writeEntry(StateVectorKey.Y_DDOT.name(), coordinates.getAcceleration().getY(), Units.KM_PER_S2, true);
                generator.writeEntry(StateVectorKey.Z_DDOT.name(), coordinates.getAcceleration().getZ(), Units.KM_PER_S2, true);
            }

            generator.exitSection();

        }
    }

    /**
     * Write a covariance matrices.
     * @param generator generator to use for producing output
     * @param metadata metadata to use for interpreting data
     * @param covariances covariances to write
     * @throws IOException if the output stream throws one while writing.
     */
    void writeCovariances(final Generator generator, final OemMetadata metadata,
                          final List<CartesianCovariance> covariances)
        throws IOException {
        if (covariances != null && !covariances.isEmpty()) {

            // enter the global covariance section in KVN
            if (generator.getFormat() == FileFormat.KVN) {
                generator.enterSection(OemDataSubStructureKey.COVARIANCE.name());
            }

            for (final CartesianCovariance covariance : covariances) {
                writeCovariance(generator, metadata, covariance);
            }

            // exit the global covariance section in KVN
            if (generator.getFormat() == FileFormat.KVN) {
                generator.exitSection();
            }

        }
    }

    /**
     * Write a single covariance matrix.
     * @param generator generator to use for producing output
     * @param metadata metadata to use for interpreting data
     * @param covariance covariance to write
     * @throws IOException if the output stream throws one while writing.
     */
    private void writeCovariance(final Generator generator, final OemMetadata metadata,
                                 final CartesianCovariance covariance)
        throws IOException {

        // wrapper for a single matrix in XML
        if (generator.getFormat() == FileFormat.XML) {
            generator.enterSection(OemDataSubStructureKey.covarianceMatrix.name());
        }

        // epoch
        generator.writeEntry(CartesianCovarianceKey.EPOCH.name(), getTimeConverter(), covariance.getEpoch(), false, true);

        // reference frame
        if (covariance.getReferenceFrame() != metadata.getReferenceFrame()) {
            generator.writeEntry(CartesianCovarianceKey.COV_REF_FRAME.name(), covariance.getReferenceFrame().getName(), null, false);
        }

        // matrix data
        final RealMatrix m = covariance.getCovarianceMatrix();
        if (generator.getFormat() == FileFormat.KVN) {
            for (int i = 0; i < m.getRowDimension(); ++i) {

                // write triangular matrix entries
                for (int j = 0; j <= i; ++j) {
                    if (j > 0) {
                        generator.writeRawData(' ');
                    }
                    generator.writeRawData(AccurateFormatter.format(Units.KM2.fromSI(m.getEntry(i, j))));
                }

                // end the line
                generator.newLine();

            }
        } else {
            generator.writeEntry(CartesianCovarianceKey.CX_X.name(),         m.getEntry(0, 0), Units.KM2,        true);
            generator.writeEntry(CartesianCovarianceKey.CY_X.name(),         m.getEntry(1, 0), Units.KM2,        true);
            generator.writeEntry(CartesianCovarianceKey.CY_Y.name(),         m.getEntry(1, 1), Units.KM2,        true);
            generator.writeEntry(CartesianCovarianceKey.CZ_X.name(),         m.getEntry(2, 0), Units.KM2,        true);
            generator.writeEntry(CartesianCovarianceKey.CZ_Y.name(),         m.getEntry(2, 1), Units.KM2,        true);
            generator.writeEntry(CartesianCovarianceKey.CZ_Z.name(),         m.getEntry(2, 2), Units.KM2,        true);
            generator.writeEntry(CartesianCovarianceKey.CX_DOT_X.name(),     m.getEntry(3, 0), Units.KM2_PER_S,  true);
            generator.writeEntry(CartesianCovarianceKey.CX_DOT_Y.name(),     m.getEntry(3, 1), Units.KM2_PER_S,  true);
            generator.writeEntry(CartesianCovarianceKey.CX_DOT_Z.name(),     m.getEntry(3, 2), Units.KM2_PER_S,  true);
            generator.writeEntry(CartesianCovarianceKey.CX_DOT_X_DOT.name(), m.getEntry(3, 3), Units.KM2_PER_S2, true);
            generator.writeEntry(CartesianCovarianceKey.CY_DOT_X.name(),     m.getEntry(4, 0), Units.KM2_PER_S,  true);
            generator.writeEntry(CartesianCovarianceKey.CY_DOT_Y.name(),     m.getEntry(4, 1), Units.KM2_PER_S,  true);
            generator.writeEntry(CartesianCovarianceKey.CY_DOT_Z.name(),     m.getEntry(4, 2), Units.KM2_PER_S,  true);
            generator.writeEntry(CartesianCovarianceKey.CY_DOT_X_DOT.name(), m.getEntry(4, 3), Units.KM2_PER_S2, true);
            generator.writeEntry(CartesianCovarianceKey.CY_DOT_Y_DOT.name(), m.getEntry(4, 4), Units.KM2_PER_S2, true);
            generator.writeEntry(CartesianCovarianceKey.CZ_DOT_X.name(),     m.getEntry(5, 0), Units.KM2_PER_S,  true);
            generator.writeEntry(CartesianCovarianceKey.CZ_DOT_Y.name(),     m.getEntry(5, 1), Units.KM2_PER_S,  true);
            generator.writeEntry(CartesianCovarianceKey.CZ_DOT_Z.name(),     m.getEntry(5, 2), Units.KM2_PER_S,  true);
            generator.writeEntry(CartesianCovarianceKey.CZ_DOT_X_DOT.name(), m.getEntry(5, 3), Units.KM2_PER_S2, true);
            generator.writeEntry(CartesianCovarianceKey.CZ_DOT_Y_DOT.name(), m.getEntry(5, 4), Units.KM2_PER_S2, true);
            generator.writeEntry(CartesianCovarianceKey.CZ_DOT_Z_DOT.name(), m.getEntry(5, 5), Units.KM2_PER_S2, true);
        }

        // wrapper for a single matrix in XML
        if (generator.getFormat() == FileFormat.XML) {
            generator.exitSection();
        }

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

    /** End of a data block.
     * @param generator generator to use for producing output
     * @throws IOException if the output stream throws one while writing.
     */
    void endData(final Generator generator) throws IOException {
        if (generator.getFormat() == FileFormat.XML) {
            generator.exitSection();
        }
    }

}
