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
 * This package provides interfaces and classes dealing with events occurring during propagation.
 * It is used when {@link org.orekit.propagation.events.EventDetector
 * EventDetector} instances are registered to any {@link org.orekit.propagation.Propagator
 * Propagator}. When the event associated with the detector occurs, the propagator interrupts
 * the propagation and calls the {@link
 * org.orekit.propagation.events.EventDetector#eventOccurred(SpacecraftState, boolean) eventOccurred}
 * method of the event detector, which can do whatever the user want and either stop or resume
 * propagation, optionally resetting the state.
 *
 * <p>
 * If the registered events detectors are configured to stop propagation when triggered, the propagator
 * can be run with an arbitrary large target date, relying on the events occurrence to stop propagation
 * exactly at the right time.
 * </p>
 *
 * <p>
 * The package provides some predefined events:
 * </p>
 * <ul>
 *  <li>{@link org.orekit.propagation.events.AlignmentDetector AlignmentDetector}
 *  detects satellite/body alignment (and by default stop when reaching alignment)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.AngularSeparationDetector AngularSeparationDetector}
 *  detects angular separation thresholds crossing between spacecraft and a beacon
 *  (typically the Sun) as seen from an observer (typically a ground station)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.AltitudeDetector AltitudeDetector}
 *  detects altitude crossing (and by default stop at descending)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.ApsideDetector ApsideDetector}
 *  detects apside crossing (and by default stop at perigee)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.CircularFieldOfViewDetector CircularFieldOfViewDetector}
 *  detects moving target entering/exiting satellite sensor Field Of View with a
 *  circular shaped boundary (and by default continue on entry and stop on exit)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.DateDetector DateDetector}
 *  detects occurrence of a predefine instant (and by default stop there)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.EclipseDetector EclipseDetector}
 *  detects satellite entering/exiting an eclipse (and by default stop on exit)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.ElevationDetector ElevationDetector}
 *  detects satellite raising/setting (and by default stop at setting)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.ElevationExtremumDetector ElevationExtremumDetector}
 *  detects satellite maximum/minimum elevation (and by default stop at minimum)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.FieldOfViewDetector FieldOfViewDetector}
 *  detects moving target entering/exiting satellite sensor Field Of View with an arbitrarily
 *  shaped boundary (and by default continue on entry and stop on exit)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.FootprintOverlapDetector FootprintOverlapDetector}
 *  detects geographic zone entering or exiting Field Of View of a spacecraft sensor (and by default stop at zone exit)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.GeographicZoneDetector GeographicZoneDetector}
 *  detects geographic boundaries crossings (and by default stop at zone exit)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.GroundFieldOfViewDetector GroundFieldOfViewDetector}
 *  detects spacecraft entering/exiting ground based Field Of View with an arbitrarily
 *  shaped boundary (and by default continue on entry and stop on exit)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.LatitudeCrossingDetector LatitudeCrossingDetector}
 *  detects satellite crossing a parallel (and by default stop at northward crossing)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.LatitudeExtremumDetector LatitudeExtremumDetector}
 *  detects satellite maximum/minimum latitude (and by default stop at minimum)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.LongitudeCrossingDetector LongitudeCrossingDetector}
 *  detects satellite crossing a meridian (the increasing/decreasing flag is irrelevant for this detector)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.LongitudeExtremumDetector LongitudeExtremumDetector}
 *  detects satellite maximum/minimum longitude (and by default stop at minimum)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.NodeDetector NodeDetector}
 *  detects node crossing (and by default stop at ascending node)
 *  </li>
 *  <li>{@link org.orekit.propagation.events.PositionAngleDetector PositionAngleDetector}
 *  detects anomaly, latitude argument or longitude argument crossings, with either true
 *  eccentric or mean angles (the increasing/decreasing flag is irrelevant for this detector)
 *  </li>
 * </ul>
 *
 * <p>
 * In addition to raw events, the class also provides {@link org.orekit.propagation.events.EventsLogger
 * EventsLogger} to gather all events that occurred during a propagation, {@link
 * org.orekit.propagation.events.EventShifter EventShifter} which allows to slightly shift an
 * event in time  (for example to trigger something say 5 minutes before eclipse entry), {@link
 * org.orekit.propagation.events.EventSlopeFilter EventSlopeFilter} and {@link
 * org.orekit.propagation.events.EventEnablingPredicateFilter EventEnablingPredicateFilter} to trigger
 * only specific types of events, without losing computation time by locating events user is not
 * interested in.
 * </p>
 *
 * <p>
 * The low level interfaces and classes are heavily based on similar classes
 * from the ode.events package from the <a
 * href="https://hipparchus.org/">Hipparchus</a> library. The changes are mainly
 * adaptations of the signatures to space dynamics.
 * </p>
 * <ul>
 *  <li>the type of dependent variable t has been changed from <code>double</code>
 *      to {@link org.orekit.time.AbsoluteDate AbsoluteDate}</li>
 *  <li>the type of state vector y has been changed from <code>double[]</code>
 *      to {@link org.orekit.propagation.SpacecraftState SpacecraftState}</li>
 * </ul>
 *
 * @author Luc Maisonobe
 * @author Pascal Parraud
 *
 */
package org.orekit.propagation.events;
