/* Copyright 2002-2025 CS GROUP
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
/**
 *
 * This package provides models that simulate the impact of the troposphere.
 * <p>
 * The impact of the troposphere is quantify via the path delay
 * for the signal from a ground station to a satellite.
 *
 * Different ways are used to compute the tropospheric delay:
 *
 * <ul>
 *   <li>Classical empirical computation</li>
 *   <li>Empirical computation by spliting the delay into hydrostatic
 *       and non-hydrostatic parts</li>
 *   <li>Estimation of the total zenith delay</li>
 * </ul>
 *
 * @author Bryan Cazabonne
 *
 */
package org.orekit.models.earth.troposphere;
