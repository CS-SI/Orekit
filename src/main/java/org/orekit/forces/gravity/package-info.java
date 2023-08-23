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
/**
 *
 * This package provides all gravity-related forces.
 *
 * <p>
 * The force models include an implementation of spherical harmonics
 * attraction model: {@link org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel Holmes-Featherstone}.</p>
 * <p>
 * The force models also include {@link org.orekit.forces.gravity.ThirdBodyAttraction third body attraction},
 * both {@link org.orekit.forces.gravity.SolidTides solid tides} and {@link
 * org.orekit.forces.gravity.OceanTides ocean tides}, both with or without pole tide and
 * {@link org.orekit.forces.gravity.Relativity post-Newtonian correction force due to general relativity}.
 * </p>
 *
 * @author L. Maisonobe
 *
 */
package org.orekit.forces.gravity;
