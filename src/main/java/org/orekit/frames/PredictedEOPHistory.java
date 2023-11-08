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
    private static final long serialVersionUID = 20230309L;

    /** Raw EOP history to extend. */
    private final EOPHistory rawHistory;

    /** Duration of the extension period (s). */
    private final double extensionDuration;

    /** Fitter for all Earth Orientation Parameters. */
    private final EOPFitter fitter;

    /** Simple constructor.
     * @param rawHistory raw EOP history to extend.
     * @param extensionDuration duration of the extension period (s)
     * @param fitter fitter for all Earth Orientation Parameters
     */
    public PredictedEOPHistory(final EOPHistory rawHistory, final double extensionDuration,
                               final EOPFitter fitter) {
        super(rawHistory.getConventions(), rawHistory.getInterpolationDegree(),
              extendHistory(rawHistory, extensionDuration, fitter),
              rawHistory.isSimpleEop(), rawHistory.getTimeScales());
        this.rawHistory        = rawHistory;
        this.extensionDuration = extensionDuration;
        this.fitter            = fitter;
    }

    /** Extends raw history.
     * @param rawHistory raw EOP history to extend.
     * @param extensionDuration duration of the extension period (s)
     * @param fitter fitter for all Earth Orientation Parameters
     * @return extended history
     */
    private static Collection<? extends EOPEntry> extendHistory(final EOPHistory rawHistory,
                                                                final double extensionDuration,
                                                                final EOPFitter fitter) {


        // fit model
        final EOPFittedModel model = fitter.fit(rawHistory);

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
            final double dut1   = model.getDUT1().osculatingValue(date);
            final double lod    = -Constants.JULIAN_DAY * model.getDUT1().osculatingDerivative(date);
            final double xp     = model.getXp().osculatingValue(date);
            final double yp     = model.getYp().osculatingValue(date);
            final double xpRate = model.getXp().osculatingDerivative(date);
            final double ypRate = model.getYp().osculatingDerivative(date);
            final double dx     = model.getDx().osculatingValue(date);
            final double dy     = model.getDy().osculatingValue(date);
            final double[] equinox = converter.toEquinox(date, dx, dy);
            entries.add(new EOPEntry(last.getMjd() + i + 1, dut1, lod, xp, yp, xpRate, ypRate,
                                     equinox[0], equinox[1], dx, dy,
                                     last.getITRFType(), date));
        }

        return entries;

    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    @DefaultDataContext
    private Object writeReplace() {
        return new DataTransferObject(rawHistory, extensionDuration, fitter);
    }

    /** Internal class used only for serialization. */
    @DefaultDataContext
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20230309L;

        /** Raw EOP history to extend. */
        private final EOPHistory rawHistory;

        /** Duration of the extension period (s). */
        private final double extensionDuration;

        /** Fitter for all Earth Orientation Parameters. */
        private final EOPFitter fitter;

        /** Simple constructor.
         * @param rawHistory raw EOP history to extend.
         * @param extensionDuration duration of the extension period (s)
         * @param fitter fitter for all Earth Orientation Parameters
         */
        DataTransferObject(final EOPHistory rawHistory, final double extensionDuration, final EOPFitter fitter) {
            this.rawHistory        = rawHistory;
            this.extensionDuration = extensionDuration;
            this.fitter            = fitter;
        }

        /** Replace the deserialized data transfer object with a {@link PredictedEOPHistory}.
         * @return replacement {@link PredictedEOPHistory}
         */
        private Object readResolve() {
            try {
                return new PredictedEOPHistory(rawHistory, extensionDuration, fitter);
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}
