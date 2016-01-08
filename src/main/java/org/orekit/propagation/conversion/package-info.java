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
 * This package provides tools to convert a given propagator or a set of
 * {@link org.orekit.propagation.SpacecraftState} into another propagator.
 * The conversion principle is to minimize the mean square error for positions
 * and velocities over a given time span.
 * <p>
 * The conversion from osculating to mean elements appears as a side effect of
 * propagation models conversion.
 * </p>
 * <p>
 * These package extends an original contribution from Telespazio for TLE
 * (Orbit Converter for Two-Lines Elements) to all propagators.
 * </p>
 *
 * @author Pascal Parraud
 */
package org.orekit.propagation.conversion;

