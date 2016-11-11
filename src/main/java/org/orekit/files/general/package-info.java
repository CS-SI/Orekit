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
 * This package provides interfaces for orbit file representations and corresponding
 * parsers.
 *
 * <p> {@link org.orekit.files.general.EphemerisFile} and {@link
 * org.orekit.files.general.EphemerisFileParser} provide a standardized interface for
 * accessing the date in ephemeris files. Each ephemeris file can have data for one ore
 * more satellites and the ephemeris for each satellite can have one or more segments.
 * Each ephemeris segment is interpolated independently so ephemeris segements are
 * commonly used for discontinuous events, such as maneuvers. Each specific implementation
 * provides access to additional information in the file by providing specialized return
 * types with extra getters for the information unique to that file type.
 *
 * <p> For example to create a propagator from an OEM file one can use:
 *
 * <code><pre>
 * EphemerisFileParser parser = new OEMParser()
 *         .withConventions(IERSConventions.IERS_2010);
 * EphemerisFile file = parser.parse("my/ephemeris/file.oem");
 * BoundedPropagator propagator = file.getPropagator();
 * </pre></code>
 *
 * @author T. Neidhart
 */
package org.orekit.files.general;
