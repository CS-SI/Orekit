/* Copyright 2002-2017 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.gnss.attitude;

import java.util.ArrayList;
import java.util.List;

import org.orekit.gnss.antenna.AntexLoader;
import org.orekit.utils.TimeSpanMap;

/**
 * Factory for the attitude providers for different navigation satellites.
 *
 * @author Luc Maisonobe
 * @since 9.2
 * @see <a href="http://acc.igs.org/orbits/eclips_May2017.tar">Kouba model for eclipse/noon turn of GNSS satellites</a>
 * @see <a href="https://www.sciencedirect.com/science/article/pii/S0273117715004378">GNSS satellite geometry and attitude models</a>
 *
 */
public class GNSSAttitudeProviderFactory {

    /** Maximum PRN index. */
    private static final int MAX_PRN = 136;

    /** Full list of providers. */
    private final List<TimeSpanMap<GNSSAttitudeProvider>> providers;

    /** Simple constructor.
     * @param loader loader for ANTEX file
     */
    public GNSSAttitudeProviderFactory(final AntexLoader loader) {
        providers = new ArrayList<TimeSpanMap<GNSSAttitudeProvider>>(MAX_PRN + 1);
    }

}
