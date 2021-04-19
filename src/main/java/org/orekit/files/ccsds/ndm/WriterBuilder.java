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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.adm.aem.AemWriter;
import org.orekit.files.ccsds.ndm.adm.apm.ApmWriter;
import org.orekit.files.ccsds.ndm.odm.ocm.OcmWriter;
import org.orekit.files.ccsds.ndm.odm.oem.OemWriter;
import org.orekit.files.ccsds.ndm.odm.omm.OmmWriter;
import org.orekit.files.ccsds.ndm.odm.opm.OpmWriter;
import org.orekit.files.ccsds.ndm.tdm.RangeUnits;
import org.orekit.files.ccsds.ndm.tdm.RangeUnitsConverter;
import org.orekit.files.ccsds.ndm.tdm.TdmWriter;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Builder for all {@link NdmFile CCSDS Message} files writers.
 * <p>
 * This builder can be used for building all CCSDS Messages writers types.
 * It is particularly useful in multi-threaded context as writers cannot
 * be shared between threads and thus several independent writers must be
 * built in this case.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class WriterBuilder extends AbstractBuilder<WriterBuilder> {

    /**
     * Simple constructor.
     * <p>
     * This constructor creates a builder with
     * <ul>
     *   <li>{@link #getConventions() IERS conventions} set to {@link IERSConventions#IERS_2010}</li>
     *   <li>{@link #getDataContext() data context} set to {@link DataContext#getDefault() default context}</li>
     *   <li>{@link #getMissionReferenceDate() mission reference date} set to {@code null}</li>
     * </ul>
     * </p>
     */
    @DefaultDataContext
    public WriterBuilder() {
        this(DataContext.getDefault());
    }

    /**
     * Simple constructor.
     * <p>
     * This constructor creates a builder with
     * <ul>
     *   <li>{@link #getConventions() IERS conventions} set to {@link IERSConventions#IERS_2010}</li>
     *   <li>{@link #getMissionReferenceDate() mission reference date} set to {@code null}</li>
     * </ul>
     * </p>
     * @param dataContext data context used to retrieve frames, time scales, etc.
     */
    public WriterBuilder(final DataContext dataContext) {
        this(IERSConventions.IERS_2010, dataContext, null, null);
    }

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param spinAxis spin axis in spacecraft body frame
     */
    private WriterBuilder(final IERSConventions conventions, final DataContext dataContext,
                          final AbsoluteDate missionReferenceDate, final Vector3D spinAxis) {
        super(conventions, dataContext, missionReferenceDate, spinAxis);
    }

    /** {@inheritDoc} */
    @Override
    protected WriterBuilder create(final IERSConventions newConventions, final DataContext newDataContext,
                                   final AbsoluteDate newMissionReferenceDate, final Vector3D newSpinAxis) {
        return new WriterBuilder(newConventions, newDataContext, newMissionReferenceDate, newSpinAxis);
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.odm.opm.OpmFile Orbit Parameters Messages}.
     * @return a new writer
     */
    public OpmWriter buildOpmWriter() {
        return new OpmWriter(getConventions(), getDataContext(), getMissionReferenceDate());
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.odm.omm.OmmFile Orbit Mean elements Messages}.
     * @return a new writer
     */
    public OmmWriter buildOmmWriter() {
        return new OmmWriter(getConventions(), getDataContext(), getMissionReferenceDate());
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.odm.oem.OemFile Orbit Ephemeris Messages}.
     * @return a new writer
     */
    public OemWriter buildOemWriter() {
        return new OemWriter(getConventions(), getDataContext(), getMissionReferenceDate());
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.odm.ocm.OcmFile Orbit Comprehensive Messages}.
     * @return a new writer
     */
    public OcmWriter buildOcmWriter() {
        return new OcmWriter(getConventions(), getDataContext());
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.adm.apm.ApmFile Attitude Parameters Messages}.
     * @return a new writer
     */
    public ApmWriter buildApmWriter() {
        return new ApmWriter(getConventions(), getDataContext(), getMissionReferenceDate(), getSpinAxis());
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.adm.aem.AemFile Attitude Ephemeris Messages}.
     * @return a new writer
     */
    public AemWriter buildAemWriter() {
        return new AemWriter(getConventions(), getDataContext(), getMissionReferenceDate(), getSpinAxis());
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.tdm.TdmFile Tracking Data Messages}.
     * @param converter converter for {@link RangeUnits#RU Range Units} (may be null if there
     * are no range observations in {@link RangeUnits#RU Range Units})
     * @return a new writer
     */
    public TdmWriter buildTdmWriter(final RangeUnitsConverter converter) {
        return new TdmWriter(getConventions(), getDataContext(), converter);
    }

}
