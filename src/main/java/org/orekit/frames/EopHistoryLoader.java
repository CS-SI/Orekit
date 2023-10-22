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
package org.orekit.frames;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.SortedSet;

import org.orekit.time.TimeScales;
import org.orekit.utils.IERSConventions;

/** Interface for loading Earth Orientation Parameters history.
 * @author Luc Maisonobe
 * @since 6.1
 */
public interface EopHistoryLoader {

    /** Load celestial body.
     * @param converter converter to use for nutation corrections
     * @param history history to fill up
     */
    void fillHistory(IERSConventions.NutationCorrectionConverter converter,
                     SortedSet<EOPEntry> history);

    /**
     * Interface for parsing EOP data files.
     *
     * @author Evan Ward
     * @since 10.1
     */
    interface Parser {

        /**
         * Parse EOP from the given input stream.
         *
         * @param input stream to parse.
         * @param name  of the stream for error messages.
         * @return parsed EOP entries.
         * @throws IOException if {@code input} throws one during parsing.
         */
        Collection<EOPEntry> parse(InputStream input, String name) throws IOException;

        /**
         * Create a new parser for EOP data in the rapid and predicted XML format.
         *
         * <p>The XML EOP files are recognized thanks to their base names, which
         * match one of the the patterns <code>finals.2000A.*.xml</code> or
         * <code>finals.*.xml</code> where * stands for a word like "all", "daily", or
         * "data".
         *
         * @param conventions         used to convert between equinox-based and
         *                            non-rotating-origin-based paradigms.
         * @param itrfVersionProvider used to determine the ITRF version of parsed EOP.
         * @param timeScales          used to parse the EOP data.
         * @return a new parser.
         */
        static Parser newFinalsXmlParser(
                final IERSConventions conventions,
                final ItrfVersionProvider itrfVersionProvider,
                final TimeScales timeScales) {
            return new EopXmlLoader.Parser(
                    conventions.getNutationCorrectionConverter(timeScales),
                    itrfVersionProvider,
                    timeScales.getUTC());
        }

        /**
         * Create a new parser for EOP data in the rapid and predicted columnar format.
         *
         * <p>The rapid data and prediction file is recognized thanks to its base name,
         * which match one of the the patterns <code>finals.*</code> or
         * <code>finals2000A.*</code> where * stands for a word like "all", "daily", or
         * "data". The file with 2000A in their name correspond to the IAU-2000
         * precession-nutation model whereas the files without any identifier correspond
         * to the IAU-1980 precession-nutation model. The files with the all suffix start
         * from 1973-01-01, and the files with the data suffix start from 1992-01-01.
         *
         * @param conventions         used to convert between equinox-based and
         *                            non-rotating-origin-based paradigms.
         * @param itrfVersionProvider used to determine the ITRF version of parsed EOP.
         * @param timeScales          used to parse the EOP data.
         * @param isNonRotatingOrigin if true the supported files <em>must</em> contain
         *                            δX/δY nutation corrections, otherwise they
         *                            <em>must</em> contain δΔψ/δΔε nutation
         *                            corrections
         * @return a new parser.
         */
        static Parser newFinalsColumnsParser(
                final IERSConventions conventions,
                final ItrfVersionProvider itrfVersionProvider,
                final TimeScales timeScales,
                final boolean isNonRotatingOrigin) {
            return new RapidDataAndPredictionColumnsLoader.Parser(
                    conventions.getNutationCorrectionConverter(timeScales),
                    itrfVersionProvider,
                    timeScales.getUTC(),
                    isNonRotatingOrigin);
        }

        /**
         * Create a new parser for EOP data in the EOP C04 format.
         *
         * <p>The EOP xx C04 files are recognized thanks to their base names, which
         * match one of the patterns {@code eopc04_##_IAU2000.##} or {@code eopc04_##.##}
         * where # stands for a digit character.
         *
         * @param conventions         used to convert between equinox-based and
         *                            non-rotating-origin-based paradigms.
         * @param itrfVersionProvider used to determine the ITRF version of parsed EOP.
         * @param timeScales          used to parse the EOP data.
         * @return a new parser.
         */
        static Parser newEopC04Parser(
                final IERSConventions conventions,
                final ItrfVersionProvider itrfVersionProvider,
                final TimeScales timeScales) {
            return new EopC04FilesLoader.Parser(conventions.getNutationCorrectionConverter(timeScales),
                                                timeScales.getUTC());
        }

        /**
         * Create a new parser for EOP data in the Bulletin B format.
         *
         * @param conventions         used to convert between equinox-based and
         *                            non-rotating-origin-based paradigms.
         * @param itrfVersionProvider used to determine the ITRF version of parsed EOP.
         * @param timeScales          used to parse the EOP data.
         * @return a new parser.
         */
        static Parser newBulletinBParser(
                final IERSConventions conventions,
                final ItrfVersionProvider itrfVersionProvider,
                final TimeScales timeScales) {
            return new BulletinBFilesLoader.Parser(
                    conventions.getNutationCorrectionConverter(timeScales),
                    itrfVersionProvider,
                    timeScales.getUTC());
        }


    }

}
