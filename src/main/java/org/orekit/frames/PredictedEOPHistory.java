/* Copyright 2023 Luc Maisonobe
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
package org.orekit.frames;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions.NutationCorrectionConverter;
import org.orekit.utils.SecularAndHarmonic;

/** This class extends an {@link EOPHistory} for some weeks using fitting.
 * <p>
 * The goal of this class is to provide a reasonable prediction of
 * Earth Orientation Parameters past the last date available in
 * regular {@link EOPHistory}, which just generated corrections set
 * to 0 when they have no data.
 * </p>
 * <p>
 * The prediction is based on fitting of last data, with both
 * {@link SecularAndHarmonic secular (polynomial) and harmonic (periodic)}
 * terms. The extended entries are generated at one point per day
 * and are continuous (i.e. no leap seconds are introduced)
 * </p>
 * <p>
 * After construction, the history contains both the initial
 * raw history and an extension part appended after it.
 * </p>
 * @see EOPFitter
 * @see SecularAndHarmonic
 * @since 12.0
 * @author Luc Maisonobe
 */
public class PredictedEOPHistory extends EOPHistory implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20230228L;

    /** Raw EOP history to extend. */
    private final EOPHistory rawHistory;

    /** Duration of the extension period (s). */
    private final double extensionDuration;

    /** Fitter for dut1 and LOD. */
    private final EOPFitter dut1Fitter;

    /** Fitter for pole x component. */
    private final EOPFitter xPFitter;

    /** Fitter for pole y component. */
    private final EOPFitter yPFitter;

    /** Fitter for nutation x component. */
    private final EOPFitter dxFitter;

    /** Fitter for nutation y component. */
    private final EOPFitter dyFitter;

    /** Simple constructor.
     * @param rawHistory raw EOP history to extend.
     * @param extensionDuration duration of the extension period (s)
     * @param dut1Fitter fitter for dut1 and LOD
     * @param xPFitter fitter for pole x component
     * @param yPFitter fitter for pole y component
     * @param dxFitter fitter for nutation x component
     * @param dyFitter fitter for nutation y component
     */
    public PredictedEOPHistory(final EOPHistory rawHistory, final double extensionDuration,
                               final EOPFitter dut1Fitter,
                               final EOPFitter xPFitter, final EOPFitter yPFitter,
                               final EOPFitter dxFitter, final EOPFitter dyFitter) {
        super(rawHistory.getConventions(),
              extendHistory(rawHistory, extensionDuration,
                            dut1Fitter, xPFitter, yPFitter, dxFitter, dyFitter),
              rawHistory.isSimpleEop(), rawHistory.getTimeScales());
        this.rawHistory        = rawHistory;
        this.extensionDuration = extensionDuration;
        this.dut1Fitter        = dut1Fitter;
        this.xPFitter          = xPFitter;
        this.yPFitter          = yPFitter;
        this.dxFitter          = dxFitter;
        this.dyFitter          = dyFitter;
    }

    /** Extends raw history.
     * @param rawHistory raw EOP history to extend.
     * @param extensionDuration duration of the extension period (s)
     * @param dut1Fitter fitter for dut1 and LOD
     * @param xPFitter fitter for pole x component
     * @param yPFitter fitter for pole y component
     * @param dxFitter fitter for nutation x component
     * @param dyFitter fitter for nutation y component
     * @return extended history
     */
    private static Collection<? extends EOPEntry> extendHistory(final EOPHistory rawHistory,
                                                                final double extensionDuration,
                                                                final EOPFitter dut1Fitter,
                                                                final EOPFitter xPFitter,
                                                                final EOPFitter yPFitter,
                                                                final EOPFitter dxFitter,
                                                                final EOPFitter dyFitter) {


        // set up fitters for individual parameters
        final SecularAndHarmonic shDut1 = dut1Fitter.fit(rawHistory, entry -> entry.getUT1MinusUTC());
        final SecularAndHarmonic shXp   = xPFitter.fit(rawHistory, entry -> entry.getX());
        final SecularAndHarmonic shYp   = yPFitter.fit(rawHistory, entry -> entry.getY());
        final SecularAndHarmonic shDx   = dxFitter.fit(rawHistory, entry -> entry.getDx());
        final SecularAndHarmonic shDy   = dyFitter.fit(rawHistory, entry -> entry.getDy());

        // create a converter for nutation corrections
        final NutationCorrectionConverter converter =
                        rawHistory.getConventions().getNutationCorrectionConverter(rawHistory.getTimeScales());

        // generate extension entries
        final List<EOPEntry> rawEntries = rawHistory.getEntries();
        final EOPEntry       last       = rawEntries.get(rawEntries.size() - 1);
        final int n = (int) FastMath.rint(extensionDuration / Constants.JULIAN_DAY);
        final List<EOPEntry> entries = new ArrayList<>(rawEntries.size() + n);
        entries.addAll(rawEntries);
        for (int i = 0; i < n; ++i) {
            final AbsoluteDate date = last.getDate().shiftedBy((i + 1) * Constants.JULIAN_DAY);
            final double dut1 = shDut1.osculatingValue(date);
            final double lod  = -Constants.JULIAN_DAY * shDut1.osculatingDerivative(date);
            final double xp   = shXp.osculatingValue(date);
            final double yp   = shYp.osculatingValue(date);
            final double dx   = shDx.osculatingValue(date);
            final double dy   = shDy.osculatingValue(date);
            final double[] equinox = converter.toEquinox(date, dx, dy);
            entries.add(new EOPEntry(last.getMjd() + i + 1, dut1, lod, xp, yp, equinox[0], equinox[1], dx, dy,
                                     last.getITRFType(), date));
        }

        return entries;

    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    @DefaultDataContext
    private Object writeReplace() {
        return new DataTransferObject(rawHistory, extensionDuration,
                                      dut1Fitter, xPFitter, yPFitter, dxFitter, dyFitter);
    }

    /** Internal class used only for serialization. */
    @DefaultDataContext
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20230228L;

        /** Raw EOP history to extend. */
        private final EOPHistory rawHistory;

        /** Duration of the extension period (s). */
        private final double extensionDuration;

        /** Fitter for dut1 and LOD. */
        private final EOPFitter dut1Fitter;

        /** Fitter for pole x component. */
        private final EOPFitter xPFitter;

        /** Fitter for pole y component. */
        private final EOPFitter yPFitter;

        /** Fitter for nutation x component. */
        private final EOPFitter dxFitter;

        /** Fitter for nutation y component. */
        private final EOPFitter dyFitter;

        /** Simple constructor.
         * @param rawHistory raw EOP history to extend.
         * @param extensionDuration duration of the extension period (s)
         * @param dut1Fitter fitter for dut1 and LOD
         * @param xPFitter fitter for pole x component
         * @param yPFitter fitter for pole y component
         * @param dxFitter fitter for nutation x component
         * @param dyFitter fitter for nutation y component
         */
        DataTransferObject(final EOPHistory rawHistory, final double extensionDuration,
                           final EOPFitter dut1Fitter,
                           final EOPFitter xPFitter, final EOPFitter yPFitter,
                           final EOPFitter dxFitter, final EOPFitter dyFitter) {
            this.rawHistory        = rawHistory;
            this.extensionDuration = extensionDuration;
            this.dut1Fitter        = dut1Fitter;
            this.xPFitter          = xPFitter;
            this.yPFitter          = yPFitter;
            this.dxFitter          = dxFitter;
            this.dyFitter          = dyFitter;
        }

        /** Replace the deserialized data transfer object with a {@link PredictedEOPHistory}.
         * @return replacement {@link PredictedEOPHistory}
         */
        private Object readResolve() {
            try {
                return new PredictedEOPHistory(rawHistory, extensionDuration,
                                               dut1Fitter, xPFitter, yPFitter, dxFitter, dyFitter);
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}
