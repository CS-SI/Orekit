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
package org.orekit.files.ccsds.ndm;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.adm.acm.AcmWriter;
import org.orekit.files.ccsds.ndm.adm.aem.AemWriter;
import org.orekit.files.ccsds.ndm.adm.apm.ApmWriter;
import org.orekit.files.ccsds.ndm.cdm.CdmWriter;
import org.orekit.files.ccsds.ndm.odm.ocm.OcmWriter;
import org.orekit.files.ccsds.ndm.odm.oem.OemWriter;
import org.orekit.files.ccsds.ndm.odm.omm.OmmWriter;
import org.orekit.files.ccsds.ndm.odm.opm.OpmWriter;
import org.orekit.files.ccsds.ndm.tdm.IdentityConverter;
import org.orekit.files.ccsds.ndm.tdm.RangeUnits;
import org.orekit.files.ccsds.ndm.tdm.RangeUnitsConverter;
import org.orekit.files.ccsds.ndm.tdm.TdmWriter;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Builder for all {@link NdmConstituent CCSDS Message} files writers.
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
     *   <li>{@link #getEquatorialRadius() central body equatorial radius} set to {@code Double.NaN}</li>
     *   <li>{@link #getFlattening() central body flattening} set to {@code Double.NaN}</li>
     *   <li>{@link #getDataContext() data context} set to {@link DataContext#getDefault() default context}</li>
     *   <li>{@link #getMissionReferenceDate() mission reference date} set to {@code null}</li>
     *   <li>{@link #getRangeUnitsConverter() converter for range units} set to {@link IdentityConverter}</li>
     * </ul>
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
     *   <li>{@link #getEquatorialRadius() central body equatorial radius} set to {@code Double.NaN}</li>
     *   <li>{@link #getFlattening() central body flattening} set to {@code Double.NaN}</li>
     *   <li>{@link #getMissionReferenceDate() mission reference date} set to {@code null}</li>
     *   <li>{@link #getRangeUnitsConverter() converter for range units} set to {@link IdentityConverter}</li>
     * </ul>
     * @param dataContext data context used to retrieve frames, time scales, etc.
     */
    public WriterBuilder(final DataContext dataContext) {
        this(IERSConventions.IERS_2010, Double.NaN, Double.NaN, dataContext, null, new IdentityConverter());
    }

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param equatorialRadius central body equatorial radius
     * @param flattening central body flattening
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param rangeUnitsConverter converter for {@link RangeUnits#RU Range Units}
     */
    private WriterBuilder(final IERSConventions conventions,
                          final double equatorialRadius, final double flattening,
                          final DataContext dataContext,
                          final AbsoluteDate missionReferenceDate, final RangeUnitsConverter rangeUnitsConverter) {
        super(conventions, equatorialRadius, flattening, dataContext, missionReferenceDate, rangeUnitsConverter);
    }

    /** {@inheritDoc} */
    @Override
    protected WriterBuilder create(final IERSConventions newConventions,
                                   final double newEquatorialRadius, final double newFlattening,
                                   final DataContext newDataContext,
                                   final AbsoluteDate newMissionReferenceDate, final RangeUnitsConverter newRangeUnitsConverter) {
        return new WriterBuilder(newConventions, newEquatorialRadius, newFlattening, newDataContext,
                                 newMissionReferenceDate, newRangeUnitsConverter);
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.Ndm Navigation Data Messages}.
     * @return a new writer
     */
    public NdmWriter buildNdmWriter() {
        return new NdmWriter(this);
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.odm.opm.Opm Orbit Parameters Messages}.
     * @return a new writer
     */
    public OpmWriter buildOpmWriter() {
        return new OpmWriter(getConventions(), getDataContext(), getMissionReferenceDate());
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.odm.omm.Omm Orbit Mean elements Messages}.
     * @return a new writer
     */
    public OmmWriter buildOmmWriter() {
        return new OmmWriter(getConventions(), getDataContext(), getMissionReferenceDate());
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.odm.oem.Oem Orbit Ephemeris Messages}.
     * @return a new writer
     */
    public OemWriter buildOemWriter() {
        return new OemWriter(getConventions(), getDataContext(), getMissionReferenceDate());
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.odm.ocm.Ocm Orbit Comprehensive Messages}.
     * @return a new writer
     */
    public OcmWriter buildOcmWriter() {
        return new OcmWriter(getConventions(), getEquatorialRadius(), getFlattening(), getDataContext());
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.adm.apm.Apm Attitude Parameters Messages}.
     * @return a new writer
     */
    public ApmWriter buildApmWriter() {
        return new ApmWriter(getConventions(), getDataContext(), getMissionReferenceDate());
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.adm.aem.Aem Attitude Ephemeris Messages}.
     * @return a new writer
     */
    public AemWriter buildAemWriter() {
        return new AemWriter(getConventions(), getDataContext(), getMissionReferenceDate());
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.adm.acm.Acm Attitude Comprehensive Messages}.
     * @return a new writer
     * @since 12.0
     */
    public AcmWriter buildAcmWriter() {
        return new AcmWriter(getConventions(), getDataContext());
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.tdm.Tdm Tracking Data Messages}.
     * @return a new writer
     */
    public TdmWriter buildTdmWriter() {
        return new TdmWriter(getConventions(), getDataContext(), getRangeUnitsConverter());
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.cdm.Cdm Conjunction Data Messages}.
     * @return a new writer
     */
    public CdmWriter buildCdmWriter() {
        return new CdmWriter(getConventions(), getDataContext());
    }

}
