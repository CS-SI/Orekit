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
package org.orekit.files.ccsds.ndm.tdm;

import java.io.IOException;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.generation.AbstractMessageWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.IERSConventions;

/**
 * Writer for CCSDS Tracking Data Message.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
public class TdmWriter extends AbstractMessageWriter<TdmHeader, Segment<TdmMetadata, ObservationsBlock>, Tdm> {

    /** Version number implemented. **/
    public static final double CCSDS_TDM_VERS = 2.0;

    /** Padding width for aligning the '=' sign. */
    public static final int KVN_PADDING_WIDTH = 29;

    /** Converter for {@link RangeUnits#RU Range Units}. */
    private final RangeUnitsConverter converter;

    /** Complete constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.WriterBuilder#buildTdmWriter()
     * writerBuilder.buildTdmWriter()}.
     * </p>
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param converter converter for {@link RangeUnits#RU Range Units} (may be null if there
     * are no range observations in {@link RangeUnits#RU Range Units})
     */
    public TdmWriter(final IERSConventions conventions, final DataContext dataContext,
                     final RangeUnitsConverter converter) {
        super(Tdm.ROOT, Tdm.FORMAT_VERSION_KEY, CCSDS_TDM_VERS,
              new ContextBinding(
                  () -> conventions, () -> false, () -> dataContext,
                  () -> ParsedUnitsBehavior.STRICT_COMPLIANCE,
                  () -> null, () -> TimeSystem.UTC,
                  () -> 0.0, () -> 1.0));
        this.converter = converter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeSegmentContent(final Generator generator, final double formatVersion,
                                       final Segment<TdmMetadata, ObservationsBlock> segment)
        throws IOException {

        // write the metadata
        final ContextBinding oldContext = getContext();
        final TdmMetadata    metadata   = segment.getMetadata();
        setContext(new ContextBinding(oldContext::getConventions,
                                      oldContext::isSimpleEOP,
                                      oldContext::getDataContext,
                                      oldContext::getParsedUnitsBehavior,
                                      oldContext::getReferenceDate,
                                      metadata::getTimeSystem,
                                      oldContext::getClockCount,
                                      oldContext::getClockRate));
        new TdmMetadataWriter(metadata, getTimeConverter()).
        write(generator);

        // write the observations block
        new ObservationsBlockWriter(segment.getData(), getTimeConverter(), segment.getMetadata(), converter).
        write(generator);

    }

}
