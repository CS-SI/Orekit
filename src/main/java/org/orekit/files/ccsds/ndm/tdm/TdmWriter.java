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
package org.orekit.files.ccsds.ndm.tdm;

import java.io.IOException;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.generation.AbstractMessageWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * Writer for CCSDS Tracking Data Message.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
public class TdmWriter extends AbstractMessageWriter {

    /** Version number implemented. **/
    public static final double CCSDS_TDM_VERS = 1.0;

    /** Key width for aligning the '=' sign. */
    public static final int KEY_WIDTH = 25;

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param header file header (may be null)
     * @param fileName file name for error messages
     */
    public TdmWriter(final IERSConventions conventions, final DataContext dataContext,
                     final Header header, final String fileName) {
        super(TdmFile.FORMAT_VERSION_KEY, CCSDS_TDM_VERS, header,
              new ContextBinding(
                  () -> conventions, () -> false, () -> dataContext,
                  () -> null, () -> TimeSystem.UTC,
                  () -> 0.0, () -> 1.0),
              fileName);
    }

    /** Write one segment.
     * @param generator generator to use for producing output
     * @param segment segment to write
     * @throws IOException if any buffer writing operations fails
     */
    public void writeSegment(final Generator generator, final Segment<TdmMetadata, ObservationsBlock> segment)
        throws IOException {

        // prepare time converter
        final IERSConventions conventions   = getContext().getConventions();
        final boolean         simpleEOP     = getContext().isSimpleEOP();
        final DataContext     dataContext   = getContext().getDataContext();
        final AbsoluteDate    referenceDate = getContext().getReferenceDate();
        final double          clockCount    = getContext().getClockCount();
        final double          clockRate     = getContext().getClockRate();
        setContext(new ContextBinding(
            () -> conventions, () -> simpleEOP, () -> dataContext,
            () -> referenceDate, segment.getMetadata()::getTimeSystem,
            () -> clockCount, () -> clockRate));

        // write the metadata
        new TdmMetadataWriter(segment.getMetadata(), getTimeConverter()).write(generator);

        // write the observations block
        new ObservationsBlockWriter(segment.getData(), getTimeConverter()).write(generator);

    }

}
