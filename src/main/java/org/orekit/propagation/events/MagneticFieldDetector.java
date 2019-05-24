/* Copyright 2002-2019 CS Systèmes d'Information
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

package org.orekit.propagation.events;

import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.models.earth.GeoMagneticField;
import org.orekit.models.earth.GeoMagneticFieldFactory;
import org.orekit.models.earth.GeoMagneticFieldFactory.FieldModel;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

/** Detector for South-Atlantic anomaly frontier crossing.
 * <p>
 * The detector is based on the value of the earth magnetic field at see level at the satellite latitude and longitude.
 * </p>
 * @author Romaric Her
 */
public class MagneticFieldDetector extends AbstractDetector<MagneticFieldDetector> {

    /** Fixed threshold value of Magnetic field to be crossed. */
    private final double limit;

    /** Fixed altitude of computed magnetic field value. */
    private final boolean seaLevel;

    /** earth geomagnetic field. */
    private GeoMagneticField field;

    /** year of the current state. */
    private double currentYear;

    /** the geomagnetic field model enum. */
    private final FieldModel type;

    /** the body. */
    private final OneAxisEllipsoid body;

    /** the timescale. */
    private final TimeScale timeScale;


    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAXCHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param limit the threshold value of magnetic field at see level
     * @param type the magnetic field model
     * @param body the body
     * @exception OrekitIllegalArgumentException if orbit type is {@link OrbitType#CARTESIAN}
     */
    public MagneticFieldDetector(final double limit, final FieldModel type, final OneAxisEllipsoid body)
        throws OrekitIllegalArgumentException {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, limit, type, body, false);
    }

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAXCHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param limit the threshold value of magnetic field at see level
     * @param type the magnetic field model
     * @param body the body
     * @param seaLevel true if the magnetic field intensity is computed at the sea level, false if it is computed at satellite altitude
     * @exception OrekitIllegalArgumentException if orbit type is {@link OrbitType#CARTESIAN}
     */
    public MagneticFieldDetector(final double limit, final FieldModel type, final OneAxisEllipsoid body, final boolean seaLevel)
        throws OrekitIllegalArgumentException {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, limit, type, body, seaLevel);
    }

    /** Build a detector.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param limit the threshold value of magnetic field at see level
     * @param type the magnetic field model
     * @param body the body
     * @param seaLevel true if the magnetic field intensity is computed at the sea level, false if it is computed at satellite altitude
     * @exception OrekitIllegalArgumentException if orbit type is {@link OrbitType#CARTESIAN}
     */
    public MagneticFieldDetector(final double maxCheck, final double threshold, final double limit,
                                 final FieldModel type, final OneAxisEllipsoid body, final boolean seaLevel)
        throws OrekitIllegalArgumentException {
        this(maxCheck, threshold, DEFAULT_MAX_ITER, new StopOnIncreasing<MagneticFieldDetector>(),
             limit, type, body, seaLevel);
    }

    /** Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param limit the threshold value of magnetic field at see level
     * @param type the magnetic field model
     * @param body the body
     * @param seaLevel true if the magnetic field intensity is computed at the sea level, false if it is computed at satellite altitude
     * @exception OrekitIllegalArgumentException if orbit type is {@link OrbitType#CARTESIAN}
     */
    private MagneticFieldDetector(final double maxCheck, final double threshold,
                                  final int maxIter, final EventHandler<? super MagneticFieldDetector> handler,
                                  final double limit, final FieldModel type, final OneAxisEllipsoid body, final boolean seaLevel)
        throws OrekitIllegalArgumentException {

        super(maxCheck, threshold, maxIter, handler);

        this.limit = limit;
        this.type = type;
        this.body = body;
        this.seaLevel = seaLevel;
        this.timeScale = TimeScalesFactory.getUTC();
    }

    /** {@inheritDoc} */
    @Override
    protected MagneticFieldDetector create(final double newMaxCheck, final double newThreshold,
                                           final int newMaxIter, final EventHandler<? super MagneticFieldDetector> newHandler) {
        try {
            return new MagneticFieldDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                             limit, type, body, seaLevel);
        } catch (OrekitException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        super.init(s0, t);
        this.currentYear = s0.getDate().getComponents(timeScale).getDate().getYear();
        this.field = GeoMagneticFieldFactory.getField(type, currentYear);
    }

    /** Compute the value of the detection function.
     * <p>
     * The value is the angle difference between the spacecraft and the fixed
     * angle to be crossed, with some sign tweaks to ensure continuity.
     * These tweaks imply the {@code increasing} flag in events detection becomes
     * irrelevant here! As an example, the angle always increase in a Keplerian
     * orbit, but this g function will increase and decrease so it
     * will cross the zero value once per orbit, in increasing and decreasing
     * directions on alternate orbits..
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return angle difference between the spacecraft and the fixed
     * angle, with some sign tweaks to ensure continuity
     */
    public double g(final SpacecraftState s) {
        try {
            if (s.getDate().getComponents(timeScale).getDate().getYear() != currentYear) {
                this.currentYear = s.getDate().getComponents(timeScale).getDate().getYear();
                this.field = GeoMagneticFieldFactory.getField(type, currentYear);
            }
            final GeodeticPoint geoPoint = body.transform(s.getPVCoordinates().getPosition(), s.getFrame(), s.getDate());
            final double altitude;
            if (seaLevel) {
                altitude = 0;
            }
            else {
                altitude = geoPoint.getAltitude();
            }
            final double value = field.calculateField(geoPoint.getLatitude(), geoPoint.getLongitude(), altitude).getTotalIntensity();
            return value - limit;

        } catch (OrekitException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
