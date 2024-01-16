/* Contributed in the public domain.
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

import org.orekit.frames.EopHistoryLoader.Parser;
import org.orekit.time.TimeScale;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.IERSConventions.NutationCorrectionConverter;

/**
 * Abstract class that holds common data used by several implementations of {@link
 * Parser}.
 *
 * @author Evan Ward
 * @since 10.1
 */
abstract class AbstractEopParser implements Parser {

    /** Converter for nutation corrections. */
    private final IERSConventions.NutationCorrectionConverter converter;
    /** Configuration for ITRF versions. */
    private final ItrfVersionProvider itrfVersionProvider;
    /** UTC time scale. */
    private final TimeScale utc;

    /**
     * Simple constructor.
     *
     * @param converter           converter to use
     * @param itrfVersionProvider to use for determining the ITRF version of the EOP.
     * @param utc                 time scale for parsing dates.
     */
    protected AbstractEopParser(final NutationCorrectionConverter converter,
                                final ItrfVersionProvider itrfVersionProvider,
                                final TimeScale utc) {
        this.converter = converter;
        this.itrfVersionProvider = itrfVersionProvider;
        this.utc = utc;
    }

    /**
     * Get the nutation converter.
     *
     * @return the nutation converter.
     */
    protected NutationCorrectionConverter getConverter() {
        return converter;
    }

    /**
     * Get the ITRF version loader.
     *
     * @return ITRF version loader.
     */
    protected ItrfVersionProvider getItrfVersionProvider() {
        return itrfVersionProvider;
    }

    /**
     * Get the UTC time scale.
     *
     * @return UTC time scale.
     */
    protected TimeScale getUtc() {
        return utc;
    }

}
