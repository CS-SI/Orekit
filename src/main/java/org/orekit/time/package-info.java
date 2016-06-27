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
 * This independent package provides classes to handle epochs, time scales,
 * and to compare instants together.
 *
 * <p>
 * The principal class is {@link org.orekit.time.AbsoluteDate}
 * which represents a unique instant in time, with no ambiguity. For that
 * purpose, the ways to define this object are quite strict.
 * </p>
 *
 * <p>The easiest and most evident way is to define an instant with an offset from another
 * one. Orekit defines 9 reference epochs. The first 6 are commonly used in the space
 * community, the seventh one is commonly used in the computer science field and the
 * last two are convenient for initialization in min/max research loops:
 * <ul>
 *   <li>{@link org.orekit.time.AbsoluteDate#JULIAN_EPOCH Julian Epoch}: -4712-01-01 at 12:00:00, TTScale</li>
 *   <li>{@link org.orekit.time.AbsoluteDate#MODIFIED_JULIAN_EPOCH Modified Julian Epoch}: 1858-11-17 at 00:00:00, TTScale</li>
 *   <li>{@link org.orekit.time.AbsoluteDate#FIFTIES_EPOCH Fifties Epoch}: 1950-01-01 at 00:00:00,  TTScale</li>
 *   <li>{@link org.orekit.time.AbsoluteDate#CCSDS_EPOCH CCSDS Epoch}: 1958-01-01 at 00:00:00,  TAIScale</li>
 *   <li>{@link org.orekit.time.AbsoluteDate#GPS_EPOCH GPS Epoch}: 1980-01-06 at 00:00:00,  UTCScale</li>
 *   <li>{@link org.orekit.time.AbsoluteDate#J2000_EPOCH J2000 Epoch}: 2000-01-01 at 12:00:00, TTScale</li>
 *   <li>{@link org.orekit.time.AbsoluteDate#JAVA_EPOCH Java Epoch}: 1970-01-01 at 00:00:00, TTScale</li>
 *   <li>{@link org.orekit.time.AbsoluteDate#PAST_INFINITY Past infinity}: at infinity in the past,</li>
 *   <li>{@link org.orekit.time.AbsoluteDate#FUTURE_INFINITY Future infinity}: at infinity in the future.</li>
 * </ul>
 *
 * <p>
 * The second definition, which could be the source of some confusion if not used with care,
 * is by giving a location (a date) in a specific <em>time scale</em>. It is of prime importance
 * to understand the various available time scales definitions to avoid mistakes. Orekit provides
 * 9 of the most important ones:
 * <ul>
 *   <li>{@link org.orekit.time.TAIScale}: International Atomic Time,</li>
 *   <li>{@link org.orekit.time.TTScale}: Terrestrial Time as defined by IAU(1991)
 *       recommendation IV.
 *       Coordinate time at the surface of the Earth. It is the successor of
 *       Ephemeris Time TE. By convention, TT = TAI + 32.184 s,</li>
 *   <li>{@link org.orekit.time.UTCScale}: Coordinated Universal Time.
 *       UTC is related to TAI using step adjustments from time to time according
 *       to IERS (International Earth Rotation Service) rules. These adjustments
 *       require introduction of leap seconds.
 *       Some leaps are already known and predefined in the library (at least
 *       from 1972-01-01 to 2009-01-01) and other ones can be supported by
 *       providing UTC-TAI.history files using the data loading mechanism
 *       provided by {@link org.orekit.data.DataProvidersManager},</li>
 *   <li>{@link org.orekit.time.UT1Scale}: Universal Time 1.
 *       UT1 is a time scale directly linked to the actual rotation of the Earth.
 *       It is an irregular scale, reflecting Earth irregular rotation rate.
 *       The offset between UT1 and {@link org.orekit.time.UTCScale} is found in
 *       the Earth Orientation Parameters published by IERS,</li>
 *   <li>{@link org.orekit.time.TCGScale}: Geocentric Coordinate Time.
 *       Coordinate time at the center of mass of the Earth.
 *       This time scale depends linearly on TTScale,</li>
 *   <li>{@link org.orekit.time.TDBScale}: Barycentric Dynamic Time.
 *       Time used to compute ephemerides in the solar system.
 *       This time is offset with respect to TT by small relativistic corrections
 *       due to Earth motion,</li>
 *   <li>{@link org.orekit.time.TCBScale}: Barycentric Coordinate Time.
 *       Coordinate time used for computations in the solar system.
 *       This time scale depends linearly on TDBScale,</li>
 *   <li>{@link org.orekit.time.GPSScale}: Global Positioning System reference scale.
 *       This scale was equal to UTC at start of the {@link
 *       org.orekit.time.AbsoluteDate#GPS_EPOCH GPS Epoch} when it was 19 seconds
 *       behind TAI, and remained parallel to TAI since then (i.e. UTC is now
 *       offset from GPS due to leap seconds). TGPS = TAI - 19 s,</li>
 *   <li>{@link org.orekit.time.GMSTScale}: Greenwich Mean Sidereal Time scale.
 *       The Greenwich Mean Sidereal Time is the hour angle between the meridian
 *       of Greenwich and mean equinox of date at 0h UT1.</li>
 * </ul>
 *
 * <p>
 * Once it is built, an {@link org.orekit.time.AbsoluteDate} can be compared to
 * other ones, and expressed in other time scales. It is used to define states,
 * orbits, frames... Classes that include a date implement the {@link
 * org.orekit.time.TimeStamped} interface.
 * The {@link org.orekit.time.ChronologicalComparator} singleton can sort objects
 * implementing this interface chronologically.
 * </p>
 *
 * @author L. Maisonobe
 *
 */
package org.orekit.time;
