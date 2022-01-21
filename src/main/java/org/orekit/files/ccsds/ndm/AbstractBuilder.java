/* Copyright 2002-2022 CS GROUP
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

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.adm.aem.AemParser;
import org.orekit.files.ccsds.ndm.adm.apm.ApmParser;
import org.orekit.files.ccsds.ndm.odm.oem.OemParser;
import org.orekit.files.ccsds.ndm.odm.omm.OmmParser;
import org.orekit.files.ccsds.ndm.odm.opm.OpmParser;
import org.orekit.files.ccsds.ndm.tdm.RangeUnits;
import org.orekit.files.ccsds.ndm.tdm.RangeUnitsConverter;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Abstract builder for all {@link NdmConstituent CCSDS Message} files parsers/writers.
 * @param <T> type of the builder
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class AbstractBuilder<T extends AbstractBuilder<T>> {

    /** IERS conventions used. */
    private final IERSConventions conventions;

    /** Data context. */
    private final DataContext dataContext;

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final AbsoluteDate missionReferenceDate;

    /** Converter for {@link RangeUnits#RU Range Units}. */
    private final RangeUnitsConverter rangeUnitsConverter;

    /**
     * Complete constructor.
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param rangeUnitsConverter converter for {@link RangeUnits#RU Range Units}
     */
    protected AbstractBuilder(final IERSConventions conventions, final DataContext dataContext,
                              final AbsoluteDate missionReferenceDate,
                              final RangeUnitsConverter rangeUnitsConverter) {
        this.conventions          = conventions;
        this.dataContext          = dataContext;
        this.missionReferenceDate = missionReferenceDate;
        this.rangeUnitsConverter  = rangeUnitsConverter;
    }

    /** Build an instance.
     * @param newConventions IERS Conventions
     * @param newDataContext used to retrieve frames, time scales, etc.
     * @param newMissionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param newRangeUnitsConverter converter for {@link RangeUnits#RU Range Units}
     * @return new instance
     */
    protected abstract T create(IERSConventions newConventions, DataContext newDataContext,
                                AbsoluteDate newMissionReferenceDate, RangeUnitsConverter newRangeUnitsConverter);

    /** Set up IERS conventions.
     * @param newConventions IERS Conventions
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public T withConventions(final IERSConventions newConventions) {
        return create(newConventions, getDataContext(), getMissionReferenceDate(), getRangeUnitsConverter());
    }

    /** Get the IERS conventions.
     * @return IERS conventions
     */
    public IERSConventions getConventions() {
        return conventions;
    }

    /** Set up data context used to retrieve frames, time scales, etc..
     * @param newDataContext data context used to retrieve frames, time scales, etc.
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public T withDataContext(final DataContext newDataContext) {
        return create(getConventions(), newDataContext, getMissionReferenceDate(), getRangeUnitsConverter());
    }

    /** Get the data context.
     * @return data context used to retrieve frames, time scales, etc.
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    /** Set up mission reference date or Mission Elapsed Time or Mission Relative Time time systems.
     * <p>
     * The mission reference date is used only by {@link AemParser} and {@link ApmParser},
     * and by {@link OpmParser}, {@link OmmParser} and {@link OemParser} up to version 2.0
     * of ODM (starting with version 3.0 of ODM, both MET and MRT time system have been
     * withdrawn from the standard).
     * </p>
     * @param newMissionReferenceDate mission reference date or Mission Elapsed Time or Mission Relative Time time systems
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public T withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return create(getConventions(), getDataContext(), newMissionReferenceDate, getRangeUnitsConverter());
    }

    /** Get the mission reference date or Mission Elapsed Time or Mission Relative Time time systems.
     * @return mission reference date
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /** Set up the converter for {@link RangeUnits#RU Range Units}.
     * @param newRangeUnitsConverter converter for {@link RangeUnits#RU Range Units}
     * @return a new builder with updated configuration (the instance is not changed)
     */
    public T withRangeUnitsConverter(final RangeUnitsConverter newRangeUnitsConverter) {
        return create(getConventions(), getDataContext(), getMissionReferenceDate(), newRangeUnitsConverter);
    }

    /** Get the converter for {@link RangeUnits#RU Range Units}.
     * @return converter for {@link RangeUnits#RU Range Units}
     */
    public RangeUnitsConverter getRangeUnitsConverter() {
        return rangeUnitsConverter;
    }

}
