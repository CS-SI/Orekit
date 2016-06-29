/* Copyright 2002-2016 CS Systèmes d'Information
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
/**
 *
 * This package provides models that simulate certain physical phenomena
 * of Earth and the near-Earth environment.
 * <p>
 * Currently the following models are included:
 *
 * <ul>
 *   <li>Tropospheric Delay</li>
 *   <li>Geomagnetic Field</li>
 *   <li>Geoid</li>
 * </ul>
 *
 * <h2>Geoid</h2>
 *
 * <p>A Geoid is an equipotential surface of Earth's gravity field. This package
 * provides the means to compute a Geoid from the gravity field harmonic
 * coefficients, as in the following example. See the comment for {@link
 * org.orekit.models.earth.Geoid} for some important caveats.
 *
 * <pre><code class="brush: java">
 * ReferenceEllipsoid ellipsoid
 *         = new ReferenceEllilpsoid(a, f, bodyFrame, GM, spin);
 * Geoid geoid = new Geoid(
 *         GravityFieldFactory.getNormalizedProvider(degree, order),
 *         ellipsoid);
 * double undulation = geoid.getUndulation(lat, lon, date);
 * </code></pre>
 *
 * @author T. Neidhart
 * @author E. Ward
 *
 */
package org.orekit.models.earth;
