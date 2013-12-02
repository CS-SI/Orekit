/* Copyright 2002-2013 CS Systèmes d'Information
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

import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnDecreasing;

/** Finder for satellite apparent elevation events.
 * <p>This class finds apparent elevation events (i.e. apparent satellite raising
 *  and setting from a terrestrial viewpoint).</p>
 * <p>Apparent elevation is the sum of geometrical elevation and refraction angle,
 *  the latter is 0 at zenith, about 1 arcminute at 45°, and 34 arcminutes at the
 *  horizon for optical wavelengths.</p>
 * <p>This event only makes sense for positive apparent elevation in the Earth environment
 * and it is not suited for near zenithal detection, where the simple
 * {@link ElevationDetector} fits better.</p>
 * <p>Refraction angle is computed according to Saemundssen formula quoted by Meeus.
 *  For reference, see <b>Astronomical Algorithms</b> (1998), 2nd ed,
 *  (ISBN 0-943396-61-1), chap. 15.</p>
 * <p>This formula is about 30 arcseconds of accuracy very close to the horizon, as
 *  variable atmospheric effects become very important.</p>
 * <p>Local pressure and temperature can be set to correct refraction at the viewpoint.</p>
 * <p>The default implementation behavior is to {@link
 * EventDetector.Action#CONTINUE continue} propagation at raising and to
 * {@link EventDetector.Action#STOP stop} propagation
 * at setting. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Pascal Parraud
 * @deprecated as of 6.1 replaced by {@link ElevationDetector}
 */
@Deprecated
public class ApparentElevationDetector extends AbstractReconfigurableDetector<ApparentElevationDetector> {

    /** Default local pressure at viewpoint (Pa). */
    public static final double DEFAULT_PRESSURE = 101000.0;

    /** Default local temperature at viewpoint (K). */
    public static final double DEFAULT_TEMPERATURE = 283.0;

    /** Serializable UID. */
    private static final long serialVersionUID = 20131118L;

    /** Elevation min value to compute refraction (under the horizon). */
    private static final double MIN_ELEVATION = -2.0;

    /** Elevation max value to compute refraction (zenithal). */
    private static final double MAX_ELEVATION = 89.89;

    /** Local pressure. */
    private double pressure = DEFAULT_PRESSURE;

    /** Local temperature. */
    private double temperature = DEFAULT_TEMPERATURE;

    /** Refraction correction from local pressure and temperature. */
    private double correfrac = 1.;

    /** Threshold apparent elevation value. */
    private final double elevation;

    /** Topocentric frame in which elevation should be evaluated. */
    private final TopocentricFrame topo;

    /** Build a new apparent elevation detector.
     * <p>This simple constructor takes default values for maximal checking
     *  interval ({@link #DEFAULT_MAXCHECK}) and convergence threshold
     * ({@link #DEFAULT_THRESHOLD}).</p>
     * @param elevation threshold elevation value
     * @param topo topocentric frame in which elevation should be evaluated
     */
    public ApparentElevationDetector(final double elevation, final TopocentricFrame topo) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, elevation, topo);
    }

    /** Build a new apparent elevation detector.
     * <p>This constructor takes default value for convergence threshold
     * ({@link #DEFAULT_THRESHOLD}).</p>
     * <p>The maximal interval between elevation checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param elevation threshold elevation value (rad)
     * @param topo topocentric frame in which elevation should be evaluated
     */
    public ApparentElevationDetector(final double maxCheck,
                                     final double elevation,
                                     final TopocentricFrame topo) {
        this(maxCheck, DEFAULT_THRESHOLD, elevation, topo);
    }

    /** Build a new apparent elevation detector.
     * <p>The maximal interval between elevation checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param elevation threshold elevation value (rad)
     * @param topo topocentric frame in which elevation should be evaluated
     */
    public ApparentElevationDetector(final double maxCheck,
                                     final double threshold,
                                     final double elevation,
                                     final TopocentricFrame topo) {
        this(maxCheck, threshold, DEFAULT_MAX_ITER, new StopOnDecreasing<ApparentElevationDetector>(),
             elevation, topo);
    }

    /** Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param threshold convergence threshold (s)
     * @param handler event handler to call at event occurrences
     * @param elevation threshold elevation value (rad)
     * @param topo topocentric frame in which elevation should be evaluated
     * @since 6.1
     */
    private ApparentElevationDetector(final double maxCheck, final double threshold,
                                      final int maxIter, final EventHandler<ApparentElevationDetector> handler,
                                      final double elevation,
                                      final TopocentricFrame topo) {
        super(maxCheck, threshold, maxIter, handler);
        this.elevation = elevation;
        this.topo      = topo;
    }

    /** {@inheritDoc} */
    @Override
    protected ApparentElevationDetector create(final double newMaxCheck, final double newThreshold,
                                               final int newMaxIter, final EventHandler<ApparentElevationDetector> newHandler) {
        return new ApparentElevationDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                             elevation, topo);
    }

    /** Set the local pressure at topocentric frame origin if needed.
     * <p>Otherwise the default value for the local pressure is set to {@link #DEFAULT_PRESSURE}.</p>
     * @param pressure the pressure to set (Pa)
     */
    public void setPressure(final double pressure) {
        this.pressure = pressure;
        this.correfrac = (pressure / DEFAULT_PRESSURE) * (DEFAULT_TEMPERATURE / temperature);
    }

    /** Set the local temperature at topocentric frame origin if needed.
     * <p>Otherwise the default value for the local temperature is set to {@link #DEFAULT_TEMPERATURE}.</p>
     * @param temperature the temperature to set (K)
     */
    public void setTemperature(final double temperature) {
        this.temperature = temperature;
        this.correfrac = (pressure / DEFAULT_PRESSURE) * (DEFAULT_TEMPERATURE / temperature);
    }

    /** Get the threshold apparent elevation value.
     * @return the threshold apparent elevation value (rad)
     */
    public double getElevation() {
        return elevation;
    }

    /** Get the topocentric frame.
     * @return the topocentric frame
     */
    public TopocentricFrame getTopocentricFrame() {
        return topo;
    }

    /** Get the local pressure at topocentric frame origin.
     * @return the pressure
     */
    public double getPressure() {
        return pressure;
    }

    /** Get the local temperature at topocentric frame origin.
     * @return the temperature
     */
    public double getTemperature() {
        return temperature;
    }

    /** Compute the value of the switching function.
     * <p>This function measures the difference between the current apparent elevation
     * and the threshold apparent elevation.</p>
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {
        final double trueElevation = topo.getElevation(s.getPVCoordinates().getPosition(), s.getFrame(), s.getDate());
        return trueElevation + getRefraction(trueElevation) - elevation;
    }

    /** Compute the refraction angle from the true (geometrical) elevation.
     * @param trueElevation true elevation (rad)
     * @return refraction angle (rad)
     */
    private double getRefraction(final double trueElevation) {
        double refraction = 0.0;
        final double eld = FastMath.toDegrees(trueElevation);
        if (eld > MIN_ELEVATION && eld < MAX_ELEVATION) {
            final double tmp = eld + 10.3 / (eld + 5.11);
            final double ref = 1.02 / FastMath.tan(FastMath.toRadians(tmp)) / 60.;
            refraction = FastMath.toRadians(correfrac * ref);
        }
        return refraction;
    }

}
