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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
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
public abstract class NDMFile<H extends NDMHeader, S extends NDMSegment<?, ?>> {

    /** Data context. */
    private DataContext dataContext;

    /** IERS conventions used. */
    private IERSConventions conventions;

    /** Header. */
    private H header;

    /** segments list. */
    private List<S> segments;

    /**
     * Constructor.
     * @param header file header
     */
    protected NDMFile(final H header) {
        this.header   = header;
        this.segments = new ArrayList<>();
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
     * Add a ({@link NDMMetadata metadata}, {@link NDMData data}) pair to the segments.
     * @param segment segment to add
     * @since 11.0
     */
    public void addSegment(final S segment) {
        segments.add(segment);
    }

    /**
     * Get IERS conventions.
     * @return conventions IERS conventions
     */
    public IERSConventions getConventions() {
        if (conventions != null) {
            return conventions;
        } else {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_CONVENTIONS);
        }
    }

    /**
     * Set IERS conventions.
     * @param conventions IERS conventions to be set
     */
    public void setConventions(final IERSConventions conventions) {
        this.conventions = conventions;
    }

    /**
     * Get the data context.
     * @return the data context used for creating frames, time scales, etc.
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    /**
     * Set the data context.
     * @param dataContext used for creating frames, time scales, etc.
     */
    public void setDataContext(final DataContext dataContext) {
        this.dataContext = dataContext;
    }

}
