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

package org.orekit.propagation.events;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.models.earth.GeoMagneticField;
import org.orekit.models.earth.GeoMagneticFieldFactory;
import org.orekit.models.earth.GeoMagneticFieldFactory.FieldModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

/** Detector for Earth magnetic field strength.
 * <p>
 * The detector is based on the field intensity calculated at the
 * satellite's latitude and longitude, either at sea level or at
 * satellite altitude, depending on the value chosen for the
 * <code>atSeaLevel</code> indicator.<br>
 * It can detect flyovers of the South-Atlantic anomaly with
 * a classically accepted limit value of 32,000 nT at sea level.
 * </p>
 * @author Romaric Her
 */
public class MagneticFieldDetector extends AbstractDetector<MagneticFieldDetector> {

    /** Fixed threshold value of Magnetic field to be crossed, in Teslas. */
    private final double limit;

    /** Switch for calculating field strength at sea level (true) or satellite altitude (false). */
    private final boolean atSeaLevel;

    /** Earth geomagnetic field. */
    private GeoMagneticField field;

    /** year of the current state. */
    private double currentYear;

    /** Earth geomagnetic field model. */
    private final FieldModel model;

    /** Earth body shape. */
    private final OneAxisEllipsoid body;

    /** Current data context. */
    private final DataContext dataContext;


    /** Build a new detector.
     *
     * <p>This constructor uses:
     * <ul>
     * <li>the {@link DataContext#getDefault() default data context}</li>
     * <li>the {@link AbstractDetector#DEFAULT_MAXCHECK default value} for maximal checking interval</li>
     * <li>the {@link AbstractDetector#DEFAULT_THRESHOLD default value} for convergence threshold</li>
     * <li>the <code>atSeaLevel</code> switch set to false</li>
     * </ul>
     *
     * @param limit threshold value for magnetic field detection, in Teslas
     * @param model magnetic field model
     * @param body  Earth body shape
     * @see #MagneticFieldDetector(double, double, double, GeoMagneticFieldFactory.FieldModel, OneAxisEllipsoid, boolean, DataContext)
     */
    @DefaultDataContext
    public MagneticFieldDetector(final double limit, final FieldModel model, final OneAxisEllipsoid body) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, limit, model, body, false);
    }

    /** Build a new detector.
     *
     * <p>This constructor uses:
     * <ul>
     * <li>the {@link DataContext#getDefault() default data context}</li>
     * <li>the {@link AbstractDetector#DEFAULT_MAXCHECK default value} for maximal checking interval</li>
     * <li>the {@link AbstractDetector#DEFAULT_THRESHOLD default value} for convergence threshold </li>
     * </ul>
     *
     * @param limit    threshold value for magnetic field detection, in Teslas
     * @param model    magnetic field model
     * @param body     Earth body shape
     * @param atSeaLevel switch for calculating field intensity at sea level (true) or satellite altitude (false)
     * @see #MagneticFieldDetector(double, double, double, GeoMagneticFieldFactory.FieldModel, OneAxisEllipsoid, boolean, DataContext)
     */
    @DefaultDataContext
    public MagneticFieldDetector(final double limit, final FieldModel model,
                                 final OneAxisEllipsoid body, final boolean atSeaLevel) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, limit, model, body, atSeaLevel);
    }

    /** Build a detector.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.</p>
     *
     * @param maxCheck   maximal checking interval (s)
     * @param threshold  convergence threshold (s)
     * @param limit      threshold value for magnetic field detection, in Teslas
     * @param model      magnetic field model
     * @param body       Earth body shape
     * @param atSeaLevel switch for calculating field intensity at sea level (true) or satellite altitude (false)
     * @see #MagneticFieldDetector(double, double, double, GeoMagneticFieldFactory.FieldModel, OneAxisEllipsoid, boolean, DataContext)
     */
    @DefaultDataContext
    public MagneticFieldDetector(final double maxCheck, final double threshold, final double limit,
                                 final FieldModel model, final OneAxisEllipsoid body, final boolean atSeaLevel) {
        this(maxCheck, threshold, limit, model, body, atSeaLevel, DataContext.getDefault());
    }

    /**
     * Build a detector.
     *
     * @param maxCheck    maximal checking interval (s)
     * @param threshold   convergence threshold (s)
     * @param limit       threshold value for magnetic field detection, in Teslas
     * @param model       magnetic field model
     * @param body        Earth body shape
     * @param atSeaLevel  switch for calculating field intensity at sea level (true) or satellite altitude (false)
     * @param dataContext used to look up the magnetic field model.
     * @since 10.1
     */
    public MagneticFieldDetector(final double maxCheck,
                                 final double threshold,
                                 final double limit,
                                 final FieldModel model,
                                 final OneAxisEllipsoid body,
                                 final boolean atSeaLevel,
                                 final DataContext dataContext) {
        this(s -> maxCheck, threshold, DEFAULT_MAX_ITER, new StopOnIncreasing(),
             limit, model, body, atSeaLevel, dataContext);
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck    maximal checking interval
     * @param threshold   convergence threshold (s)
     * @param maxIter     maximum number of iterations in the event time search
     * @param handler     event handler to call at event occurrences
     * @param limit       threshold value for magnetic field detection, in Teslas
     * @param model       magnetic field model
     * @param body        Earth body shape
     * @param atSeaLevel  switch for calculating field intensity at sea level (true) or satellite altitude (false)
     * @param dataContext used to look up the magnetic field model.
     */
    protected MagneticFieldDetector(final AdaptableInterval maxCheck, final double threshold,
                                    final int maxIter, final EventHandler handler,
                                    final double limit, final FieldModel model, final OneAxisEllipsoid body,
                                    final boolean atSeaLevel, final DataContext dataContext) {
        super(maxCheck, threshold, maxIter, handler);
        this.limit       = limit;
        this.model       = model;
        this.body        = body;
        this.atSeaLevel  = atSeaLevel;
        this.dataContext = dataContext;
    }

    /** {@inheritDoc} */
    @Override
    protected MagneticFieldDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                           final int newMaxIter, final EventHandler newHandler) {
        return new MagneticFieldDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                         limit, model, body, atSeaLevel, dataContext);
    }

    /** {@inheritDoc} */
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        super.init(s0, t);
        final TimeScale utc = dataContext.getTimeScales().getUTC();
        this.currentYear = s0.getDate().getComponents(utc).getDate().getYear();
        this.field = dataContext.getGeoMagneticFields().getField(model, currentYear);
    }

    /** Compute the value of the detection function.
     * <p>
     * The returned value is the difference between the field intensity at spacecraft location,
     * taking <code>atSeaLevel</code> switch into account, and the fixed threshold value.
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return difference between the field intensity at spacecraft location
     *         and the fixed threshold value
     */
    public double g(final SpacecraftState s) {
        final TimeScale utc = dataContext.getTimeScales().getUTC();
        if (s.getDate().getComponents(utc).getDate().getYear() != currentYear) {
            this.currentYear = s.getDate().getComponents(utc).getDate().getYear();
            this.field = dataContext.getGeoMagneticFields().getField(model, currentYear);
        }
        final GeodeticPoint geoPoint = body.transform(s.getPosition(), s.getFrame(), s.getDate());
        final double altitude = atSeaLevel ? 0. : geoPoint.getAltitude();
        final double value = field.calculateField(geoPoint.getLatitude(), geoPoint.getLongitude(), altitude).getTotalIntensity();
        return value - limit;
    }

}
