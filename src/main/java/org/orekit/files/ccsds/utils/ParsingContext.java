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
package org.orekit.files.ccsds.utils;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Context for parsing Navigation Data Message.
 * <p>
 * This class is a facade providing late binding access to data used during parsing.
 * Late binding allow some data to be set up during parsing itself. This is
 * used for example to access {@link #getTimeScale() time scale} that is generally
 * parsed from metadata block, and used later on within the same metadata block.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class ParsingContext {

    /** Supplier for IERS conventions to use. */
    private final Supplier<IERSConventions> conventionsSupplier;

    /** Supplier for simple or accurate EOP interpolation indicator. */
    private final  BooleanSupplier simpleEOPSupplier;

    /** Supplier for data context. */
    private final Supplier<DataContext> dataContextSupplier;

    /** Supplier for reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final Supplier<AbsoluteDate> missionReferenceDateSupplier;

    /** Supplier for reference system for interpreting dates. */
    private final Supplier<CcsdsTimeScale> timeScaleSupplier;

    /** Create a new context.
     * @param conventionsSupplier supplier for IERS conventions to use
     * @param simpleEOPSupplier supplier for simple or accurate EOP interpolation indicator
     * @param dataContextSupplier supplier for data context to use
     * @param missionReferenceDateSupplier supplier for reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param timeScaleSupplier supplier for reference system for interpreting dates
     */
    public ParsingContext(final Supplier<IERSConventions> conventionsSupplier,
                          final BooleanSupplier           simpleEOPSupplier,
                          final Supplier<DataContext>     dataContextSupplier,
                          final Supplier<AbsoluteDate>    missionReferenceDateSupplier,
                          final Supplier<CcsdsTimeScale>  timeScaleSupplier) {
        this.conventionsSupplier          = conventionsSupplier;
        this.simpleEOPSupplier            = simpleEOPSupplier;
        this.dataContextSupplier          = dataContextSupplier;
        this.missionReferenceDateSupplier = missionReferenceDateSupplier;
        this.timeScaleSupplier            = timeScaleSupplier;
    }

    /** Get IERS conventions.
     * @return IERS conventions to use while parsing
     * @see #withConventions(IERSConventions)
     */
    public IERSConventions getConventions() {
        return conventionsSupplier.get();
    }

    /** Get EOP interpolation method.
     * @return true if tidal effects are ignored when interpolating EOP
     * @see #withSimpleEOP(boolean)
     */
    public boolean isSimpleEOP() {
        return simpleEOPSupplier.getAsBoolean();
    }

    /**
     * Get the data context used for getting frames, time scales, and celestial bodies.
     *
     * @return the data context.
     */
    public DataContext getDataContext() {
        return dataContextSupplier.get();
    }

    /** Get initial date.
     * @return mission reference date to use while parsing
     * @see #withMissionReferenceDate(AbsoluteDate)
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDateSupplier.get();
    }

    /** Get the time scale.
     * @return time scale
     */
    public CcsdsTimeScale getTimeScale() {
        return timeScaleSupplier.get();
    }

}
