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
package org.orekit.files.ccsds.utils;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Context for parsing/writing Navigation Data Message.
 * <p>
 * This class is a facade providing late binding access to data.
 * Late binding is mainly useful at parse time as it allows some data to be set up
 * during parsing itself. This is used for example to access {@link #getTimeSystem()
 * time system} that is generally parsed from metadata block, and used later on
 * within the same metadata block.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class ContextBinding {

    /** Behavior adopted for units that have been parsed from a CCSDS message. */
    private final Supplier<ParsedUnitsBehavior> behaviorSupplier;

    /** Supplier for IERS conventions to use. */
    private final Supplier<IERSConventions> conventionsSupplier;

    /** Supplier for simple or accurate EOP interpolation indicator. */
    private final  BooleanSupplier simpleEOPSupplier;

    /** Supplier for data context. */
    private final Supplier<DataContext> dataContextSupplier;

    /** Supplier for reference date for mission elapsed time (MET), mission relative time (MRT),
     * or spacecraft clock (SCLK) time systems. */
    private final Supplier<AbsoluteDate> referenceDateSupplier;

    /** Supplier for reference system for interpreting dates. */
    private final Supplier<TimeSystem> timeSystemSupplier;

    /** Supplier for clock count at reference date in spacecraft clock (SCLK) time system. */
    private final DoubleSupplier clockCountSupplier;

    /** Supplier for clock rate in spacecraft clock (SCLK) time system. */
    private final DoubleSupplier clockRateSupplier;

    /** Create a new context.
     * @param conventionsSupplier supplier for IERS conventions to use
     * @param simpleEOPSupplier supplier for simple or accurate EOP interpolation indicator
     * @param dataContextSupplier supplier for data context to use
     * @param behaviorSupplier supplier for behavior to adopt on unit
     * @param referenceDateSupplier supplier for reference date for mission elapsed time (MET),
     * mission relative time (MRT), or spacecraft clock (SCLK) time systems
     * @param timeSystemSupplier supplier for reference system for interpreting dates
     * @param clockCountSupplier supplier for clock count at reference date in spacecraft clock (SCLK) time system
     * @param clockRateSupplier supplier for clock rate in spacecraft clock (SCLK) time system
     */
    public ContextBinding(final Supplier<IERSConventions>     conventionsSupplier,
                          final BooleanSupplier               simpleEOPSupplier,
                          final Supplier<DataContext>         dataContextSupplier,
                          final Supplier<ParsedUnitsBehavior> behaviorSupplier,
                          final Supplier<AbsoluteDate>        referenceDateSupplier,
                          final Supplier<TimeSystem>          timeSystemSupplier,
                          final DoubleSupplier                clockCountSupplier,
                          final DoubleSupplier                clockRateSupplier) {
        this.behaviorSupplier      = behaviorSupplier;
        this.conventionsSupplier   = conventionsSupplier;
        this.simpleEOPSupplier     = simpleEOPSupplier;
        this.dataContextSupplier   = dataContextSupplier;
        this.referenceDateSupplier = referenceDateSupplier;
        this.timeSystemSupplier    = timeSystemSupplier;
        this.clockCountSupplier    = clockCountSupplier;
        this.clockRateSupplier     = clockRateSupplier;
    }

    /** Get the behavior to adopt for handling parsed units.
     * @return behavior to adopt for handling parsed units
     */
    public ParsedUnitsBehavior getParsedUnitsBehavior() {
        return behaviorSupplier.get();
    }

    /** Get IERS conventions.
     * @return IERS conventions to use while parsing
     */
    public IERSConventions getConventions() {
        return conventionsSupplier.get();
    }

    /** Get EOP interpolation method.
     * @return true if tidal effects are ignored when interpolating EOP
     */
    public boolean isSimpleEOP() {
        return simpleEOPSupplier.getAsBoolean();
    }

    /** Get the data context used for getting frames, time scales, and celestial bodies.
     * @return the data context.
     */
    public DataContext getDataContext() {
        return dataContextSupplier.get();
    }

    /** Get initial date.
     * @return reference date to use while parsing
     */
    public AbsoluteDate getReferenceDate() {
        return referenceDateSupplier.get();
    }

    /** Get the time system.
     * @return time system
     */
    public TimeSystem getTimeSystem() {
        return timeSystemSupplier.get();
    }

    /** Get clock count.
     * @return clock count at reference date
     */
    public double getClockCount() {
        return clockCountSupplier.getAsDouble();
    }

    /** Get clock rate.
     * @return clock rate (in clock ticks per SI second)
     */
    public double getClockRate() {
        return clockRateSupplier.getAsDouble();
    }

}
