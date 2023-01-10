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
 * This package provides interfaces for orbit file representations and corresponding
 * parsers.
 *
 * <p> {@link org.orekit.files.general.EphemerisFile EphemerisFile}, {@link
 * org.orekit.files.general.EphemerisFileParser EphemerisFileParser} and {@link
 * org.orekit.files.general.EphemerisFileWriter EphemerisFileWriter} for orbit ephemeris and
 * {@link org.orekit.files.general.AttitudeEphemerisFile AttitudeEphemerisFile}, {@link
 * org.orekit.files.general.AttitudeEphemerisFileParser AttitudeEphemerisFileParser} and {@link
 * org.orekit.files.general.AttitudeEphemerisFileWriter AttitudeEphemerisFileWriter} for attitude
 * ephemeris provide a standardized interface for accessing and writing the data in ephemeris files.
 * Each ephemeris file can have data for one or more satellites and the ephemeris for each satellite
 * can have one or more segments. Each ephemeris segment is interpolated independently so ephemeris
 * segments are commonly used for discontinuous events, such as maneuvers. Each specific
 * implementation provides access to additional information in the file by providing specialized
 * return types with extra getters for the information unique to that file type.
 *
 * <p> For example to create a propagator from an OEM file one can use:
 *
 * <pre>
 * EphemerisFileParser parser = new OEMParser()
 *         .withConventions(IERSConventions.IERS_2010);
 * EphemerisFile file = parser.parse("my/ephemeris/file.oem");
 * BoundedPropagator propagator = file.getPropagator();
 * </pre>
 *
 * <p> The parsed ephemeris file also provides access to the individual data records in
 * the file.
 *
 * <pre>
 * // ... continued from previous example
 * // get a satellite by ID string
 * SatelliteEphemeris sat = file.getSatellites().get("satellite ID");
 * // get first ephemeris segment
 * EphemerisSegment segment = sat.getSegments().get(0)
 * // get first state vector in segment
 * TimeStampedPVCoordinate pv = segment.getCoordinates().get(0);
 * </pre>
 *
 * @author T. Neidhart
 * @author Evan Ward
 */
package org.orekit.files.general;
