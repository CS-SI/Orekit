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
package org.orekit.files.ccsds.ndm.odm.opm;

import java.io.IOException;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.odm.CommonMetadata;
import org.orekit.files.ccsds.ndm.odm.CommonMetadataKey;
import org.orekit.files.ccsds.ndm.odm.OdmMetadataKey;
import org.orekit.files.ccsds.ndm.odm.StateVectorKey;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.KvnStructureKey;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.AbstractMessageWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.units.Unit;


/**
 * Writer for CCSDS Orit Parameter Message.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OpmWriter extends AbstractMessageWriter {

    /** Version number implemented. **/
    public static final double CCSDS_OPM_VERS = 3.0;

    /** Key width for aligning the '=' sign. */
    public static final int KEY_WIDTH = 18;

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param header file header (may be null)
     * @param fileName file name for error messages
     */
    public OpmWriter(final IERSConventions conventions, final DataContext dataContext,
                     final AbsoluteDate missionReferenceDate,
                     final Header header, final String fileName) {
        super(OpmFile.FORMAT_VERSION_KEY, CCSDS_OPM_VERS, header,
              new ContextBinding(
                  () -> conventions, () -> false, () -> dataContext,
                  () -> missionReferenceDate, () -> TimeSystem.UTC,
                  () -> 0.0, () -> 1.0),
              fileName);
    }

    /** Write one segment.
     * @param generator generator to use for producing output
     * @param segment segment to write
     * @throws IOException if any buffer writing operations fails
     */
    public void writeSegment(final Generator generator, final Segment<CommonMetadata, OpmData> segment)
        throws IOException {

        // write the metadata
        writeMetadata(generator, segment.getMetadata());

        // write the data
        writeData(generator, segment.getData());

    }

    /** Write one segment metadata.
     * @param generator generator to use for producing output
     * @param metadata metadata to write
     * @throws IOException if any buffer writing operations fails
     */
    private void writeMetadata(final Generator generator, final CommonMetadata metadata) throws IOException {

        // Start metadata
        generator.enterSection(generator.getFormat() == FileFormat.KVN ?
                               KvnStructureKey.META.name() :
                               XmlStructureKey.metadata.name());

        generator.writeComments(metadata);

        // object
        generator.writeEntry(OdmMetadataKey.OBJECT_NAME.name(),  metadata.getObjectName(), true);
        generator.writeEntry(CommonMetadataKey.OBJECT_ID.name(), metadata.getObjectID(),   true);

        // frames
        generator.writeEntry(CommonMetadataKey.CENTER_NAME.name(),     metadata.getCenter().getName(),               true);
        generator.writeEntry(CommonMetadataKey.REF_FRAME.name(),       metadata.getReferenceFrame().getName(),       true);
        generator.writeEntry(CommonMetadataKey.REF_FRAME_EPOCH.name(), getTimeConverter(), metadata.getFrameEpoch(), false);

        // time
        generator.writeEntry(MetadataKey.TIME_SYSTEM.name(),   metadata.getTimeSystem(), true);

        // Stop metadata
        generator.exitSection();

    }

    /** Write one segment data.
     * @param generator generator to use for producing output
     * @param data data
     * @throws IOException if any buffer writing operations fails
     */
    private void writeData(final Generator generator, final OpmData data) throws IOException {

        // Start block
        generator.enterSection(generator.getFormat() == FileFormat.KVN ?
                               KvnStructureKey.DATA.name() :
                               XmlStructureKey.data.name());

        // state vector block
        final TimeStampedPVCoordinates pv = data.getStateVectorBlock().toTimeStampedPVCoordinates();
        generator.writeComments(data.getStateVectorBlock());
        generator.writeEntry(StateVectorKey.EPOCH.name(), getTimeConverter(), pv.getDate(), true);
        generator.writeEntry(StateVectorKey.X.name(),     Unit.KILOMETRE.fromSI(pv.getPosition().getX()), true);
        generator.writeEntry(StateVectorKey.Y.name(),     Unit.KILOMETRE.fromSI(pv.getPosition().getY()), true);
        generator.writeEntry(StateVectorKey.Z.name(),     Unit.KILOMETRE.fromSI(pv.getPosition().getZ()), true);
        generator.writeEntry(StateVectorKey.X_DOT.name(), Unit.KILOMETRE.fromSI(pv.getVelocity().getX()), true);
        generator.writeEntry(StateVectorKey.Y_DOT.name(), Unit.KILOMETRE.fromSI(pv.getVelocity().getY()), true);
        generator.writeEntry(StateVectorKey.Z_DOT.name(), Unit.KILOMETRE.fromSI(pv.getVelocity().getZ()), true);
        // note that OPM format does not use X_DDOT, Y_DDOT, Z_DDOT, they are used only in OEM format

        // Stop block
        generator.exitSection();

    }

}
