/* Copyright 2023 Thales Alenia Space
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth.troposphere;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;

/** The modified Saastamoinen model.
 * @author Luc Maisonobe
 * @deprecated as of 12.1, replaced by {@link ModifiedSaastamoinenModel}
 */
@Deprecated
public class SaastamoinenModel extends ModifiedSaastamoinenModel {

    /** Default file name for δR correction term table. */
    public static final String DELTA_R_FILE_NAME = ModifiedSaastamoinenModel.DELTA_R_FILE_NAME;

    /** Default lowest acceptable elevation angle [rad]. */
    public static final double DEFAULT_LOW_ELEVATION_THRESHOLD = ModifiedSaastamoinenModel.DEFAULT_LOW_ELEVATION_THRESHOLD;

    /**
     * Create a new Saastamoinen model for the troposphere using the given environmental
     * conditions and table from the reference book.
     *
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param r0 the humidity at the station [fraction] (50% -&gt; 0.5)
     * @see ModifiedSaastamoinenModel#ModifiedSaastamoinenModel(double, double, double, String, DataProvidersManager)
     * @since 10.1
     */
    public SaastamoinenModel(final double t0, final double p0, final double r0) {
        super(t0, p0, r0);
    }

    /** Create a new Saastamoinen model for the troposphere using the given
     * environmental conditions. This constructor uses the {@link DataContext#getDefault()
     * default data context} if {@code deltaRFileName != null}.
     *
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param r0 the humidity at the station [fraction] (50% -&gt; 0.5)
     * @param deltaRFileName regular expression for filename containing δR
     * correction term table (typically {@link #DELTA_R_FILE_NAME}), if null
     * default values from the reference book are used
     * @since 7.1
     * @see ModifiedSaastamoinenModel#ModifiedSaastamoinenModel(double, double, double, String, DataProvidersManager)
     */
    @DefaultDataContext
    public SaastamoinenModel(final double t0, final double p0, final double r0,
                             final String deltaRFileName) {
        super(t0, p0, r0, deltaRFileName);
    }

    /** Create a new Saastamoinen model for the troposphere using the given
     * environmental conditions. This constructor allows the user to specify the source of
     * of the δR file.
     *
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param r0 the humidity at the station [fraction] (50% -&gt; 0.5)
     * @param deltaRFileName regular expression for filename containing δR
     * correction term table (typically {@link #DELTA_R_FILE_NAME}), if null
     * default values from the reference book are used
     * @param dataProvidersManager provides access to auxiliary data.
     * @since 10.1
     */
    public SaastamoinenModel(final double t0,
                             final double p0,
                             final double r0,
                             final String deltaRFileName,
                             final DataProvidersManager dataProvidersManager) {
        super(t0, p0, r0, deltaRFileName, dataProvidersManager);
    }

    /** Create a new Saastamoinen model using a standard atmosphere model.
    *
    * <ul>
    * <li>temperature: 18 degree Celsius
    * <li>pressure: 1013.25 mbar
    * <li>humidity: 50%
    * </ul>
    *
    * @return a Saastamoinen model with standard environmental values
    */
    public static SaastamoinenModel getStandardModel() {
        return new SaastamoinenModel(273.16 + 18, 1013.25, 0.5);
    }

}

