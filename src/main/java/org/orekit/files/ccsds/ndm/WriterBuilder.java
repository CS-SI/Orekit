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

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.adm.aem.AemMetadata;
import org.orekit.files.ccsds.ndm.adm.aem.AemWriter;
import org.orekit.files.ccsds.ndm.adm.apm.ApmWriter;
import org.orekit.files.ccsds.ndm.odm.opm.OpmWriter;
import org.orekit.files.ccsds.ndm.tdm.RangeUnits;
import org.orekit.files.ccsds.ndm.tdm.RangeUnitsConverter;
import org.orekit.files.ccsds.ndm.tdm.TdmWriter;
import org.orekit.files.ccsds.section.Header;
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
        this(IERSConventions.IERS_2010, dataContext, null);
    }

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     */
    private WriterBuilder(final IERSConventions conventions, final DataContext dataContext,
                          final AbsoluteDate missionReferenceDate) {
        super(conventions, dataContext, missionReferenceDate);
    }

    /** {@inheritDoc} */
    @Override
    protected WriterBuilder create(final IERSConventions newConventions, final DataContext newDataContext,
                                   final AbsoluteDate newMissionReferenceDate) {
        return new WriterBuilder(newConventions, newDataContext, newMissionReferenceDate);
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.odm.opm.OpmFile Orbit Parameters Messages}.
     * @param header file header to used
     * @param fileName file name for error messages
     * @return a new writer
     */
    public OpmWriter buildOpmWriter(final Header header, final String fileName) {
        return new OpmWriter(getConventions(), getDataContext(), getMissionReferenceDate(), header, fileName);
    }

//    /** Build a writer for {@link org.orekit.files.ccsds.ndm.odm.opm.OmmFile Orbit Mean elements Messages}.
//     * @param header file header to used
//     * @param fileName file name for error messages
//     * @return a new writer
//     */
//    public OmmWriter buildOmmWriter(final Header header, final String fileName) {
//        return new OmmWriter(getConventions(), getDataContext(), getMissionReferenceDate(), fileName);
//    }
//
//    /** Build a writer for {@link org.orekit.files.ccsds.ndm.odm.oem.OemFile Orbit Ephemeris Messages}.
//     * @param header file header to used
//     * @param template template for metadata
//     * @param fileName file name for error messages
//     * @return a new writer
//     */
//    public OemWriter buildOemWriter(final Header header, final OemMetadata template, final String fileName) {
//        return new OemWriter(getConventions(), getDataContext(), header, template, fileName);
//    }
//
//    /** Build a writer for {@link org.orekit.files.ccsds.ndm.odm.ocm.OcmFile Orbit Comprehensive Messages}.
//     * @param header file header to used
//     * @param fileName file name for error messages
//     * @return a new writer
//     */
//    public OcmWriter buildOcmWriter(final Header header, final String fileName) {
//        return new OcmWriter(getConventions(), getDataContext(), fileName);
//    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.adm.apm.ApmFile Attitude Parameters Messages}.
     * @param header file header to used
     * @param fileName file name for error messages
     * @return a new writer
     */
    public ApmWriter buildApmWriter(final Header header, final String fileName) {
        return new ApmWriter(getConventions(), getDataContext(), getMissionReferenceDate(), header, fileName);
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.adm.aem.AemFile Attitude Ephemeris Messages}.
     * @param header file header to used
     * @param template template for metadata
     * @param fileName file name for error messages
     * @return a new writer
     */
    public AemWriter buildAemWriter(final Header header, final AemMetadata template, final String fileName) {
        return new AemWriter(getConventions(), getDataContext(), header, template, fileName);
    }

    /** Build a writer for {@link org.orekit.files.ccsds.ndm.tdm.TdmFile Tracking Data Messages}.
     * @param header file header to used
     * @param fileName file name for error messages
     * @param converter converter for {@link RangeUnits#RU Range Units} (may be null if there
     * are no range observations in {@link RangeUnits#RU Range Units})
     * @return a new writer
     */
    public TdmWriter buildTdmWriter(final Header header, final String fileName, final RangeUnitsConverter converter) {
        return new TdmWriter(getConventions(), getDataContext(), converter, header, fileName);
    }

}
