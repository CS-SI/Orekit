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
package org.orekit.files.ccsds.ndm;

import java.util.Collections;
import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.utils.IERSConventions;

/**
 * The NDMFile (Navigation Data Message) class represents the navigation
 * messages used by the CCSDS format, (i.e. the Attitude Data Message (ADM),
 * the Orbit Data Message (ODM) and the Tracking Data Message (TDM)).
 * It contains the information of the message's header and configuration data
 * (set in the parser).
 * @param <H> type of the header
 * @param <S> type of the segments
 * @author Bryan Cazabonne
 * @since 10.2
 */
public abstract class NdmFile<H extends Header, S extends Segment<?, ?>> {

    /** Header. */
    private final H header;

    /** segments list. */
    private final List<S> segments;

    /** IERS conventions used. */
    private final IERSConventions conventions;

    /** Data context. */
    private final DataContext dataContext;

    /**
     * Constructor.
     * @param header file header
     * @param segments file segments
     * @param conventions IERS conventions
     * @param dataContext used for creating frames, time scales, etc.
     */
    protected NdmFile(final H header, final List<S> segments,
                      final IERSConventions conventions, final DataContext dataContext) {
        this.header      = header;
        this.segments    = segments;
        this.conventions = conventions;
        this.dataContext = dataContext;
    }

    /**
     * Get the header.
     * @return header
     * @since 11.0
     */
    public H getHeader() {
        return header;
    }

    /**
     * Get the segments.
     * @return segments
     * @since 11.0
     */
    public List<S> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    /**
     * Get IERS conventions.
     * @return IERS conventions
     */
    public IERSConventions getConventions() {
        if (conventions != null) {
            return conventions;
        } else {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
        }
    }

    /**
     * Get the data context.
     * @return the data context used for creating frames, time scales, etc.
     */
    public DataContext getDataContext() {
        return dataContext;
    }

}
