/* Copyright 2002-2017 CS Systèmes d'Information
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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnIncreasing;
import org.orekit.utils.PVCoordinatesProvider;

/** Finder for satellite eclipse related events.
 * <p>This class finds eclipse events, i.e. satellite within umbra (total
 * eclipse) or penumbra (partial eclipse).</p>
 * <p>The default implementation behavior is to {@link
 * org.orekit.propagation.events.handlers.FieldEventHandler.Action#CONTINUE continue}
 * propagation when entering the eclipse and to {@link
 * org.orekit.propagation.events.handlers.FieldEventHandler.Action#STOP stop} propagation
 * when exiting the eclipse. This can be changed by calling {@link
 * #withHandler(FieldEventHandler)} after construction.</p>
 * @see org.orekit.propagation.FieldPropagator#addEventDetector(FieldEventDetector)
 * @author Pascal Parraud
 */
public class FieldEclipseDetector<T extends RealFieldElement<T>> extends FieldAbstractDetector<FieldEclipseDetector<T>, T> {


    /** Occulting body. */
    private final PVCoordinatesProvider occulting;

    /** Occulting body radius (m). */
    private final double occultingRadius;

    /** Occulted body. */
    private final PVCoordinatesProvider occulted;

    /** Occulted body radius (m). */
    private final double occultedRadius;

    /** Umbra, if true, or penumbra, if false, detection flag. */
    private boolean totalEclipse;

    /** Build a new eclipse detector.
     * <p>The new instance is a total eclipse (umbra) detector with default
     * values for maximal checking interval ({@link #DEFAULT_MAXCHECK})
     * and convergence threshold ({@link #DEFAULT_THRESHOLD}).</p>
     * @param occulted the body to be occulted
     * @param occultedRadius the radius of the body to be occulted (m)
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius (m)
     * @param field field used by default
     */
    public FieldEclipseDetector(final PVCoordinatesProvider occulted,  final double occultedRadius,
                           final PVCoordinatesProvider occulting, final double occultingRadius, final Field<T> field) {
        this(field.getZero().add(DEFAULT_MAXCHECK), field.getZero().add(DEFAULT_THRESHOLD),
             occulted, occultedRadius, occulting, occultingRadius);
    }

    /** Build a new eclipse detector.
     * <p>The new instance is a total eclipse (umbra) detector with default
     * value for convergence threshold ({@link #DEFAULT_THRESHOLD}).</p>
     * <p>The maximal interval between eclipse checks should be smaller than
     * the half duration of the minimal pass to handle, otherwise some short
     * passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param occulted the body to be occulted
     * @param occultedRadius the radius of the body to be occulted in meters
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius in meters
     */
    public FieldEclipseDetector(final T maxCheck,
                           final PVCoordinatesProvider occulted,  final double occultedRadius,
                           final PVCoordinatesProvider occulting, final double occultingRadius) {
        this(maxCheck, maxCheck.getField().getZero().add(DEFAULT_THRESHOLD),
             occulted, occultedRadius, occulting, occultingRadius);
    }

    /** Build a new eclipse detector.
     * <p>The new instance is a total eclipse (umbra) detector.</p>
     * <p>The maximal interval between eclipse checks should be smaller than
     * the half duration of the minimal pass to handle, otherwise some short
     * passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param occulted the body to be occulted
     * @param occultedRadius the radius of the body to be occulted in meters
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius in meters
     */
    public FieldEclipseDetector(final T maxCheck, final T threshold,
                           final PVCoordinatesProvider occulted,  final double occultedRadius,
                           final PVCoordinatesProvider occulting, final double occultingRadius) {
        this(maxCheck, threshold, DEFAULT_MAX_ITER, new FieldStopOnIncreasing<FieldEclipseDetector<T>, T>(),
             occulted, occultedRadius, occulting, occultingRadius, true);
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
     * @param occulted the body to be occulted
     * @param occultedRadius the radius of the body to be occulted in meters
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius in meters
     * @param totalEclipse umbra (true) or penumbra (false) detection flag
     * @since 6.1
     */
    private FieldEclipseDetector(final T maxCheck, final T threshold,
                            final int maxIter, final FieldEventHandler<? super FieldEclipseDetector<T>, T> handler,
                            final PVCoordinatesProvider occulted,  final double occultedRadius,
                            final PVCoordinatesProvider occulting, final double occultingRadius,
                            final boolean totalEclipse) {
        super(maxCheck, threshold, maxIter, handler);
        this.occulted        = occulted;
        this.occultedRadius  = FastMath.abs(occultedRadius);
        this.occulting       = occulting;
        this.occultingRadius = FastMath.abs(occultingRadius);
        this.totalEclipse    = totalEclipse;
    }

    /** {@inheritDoc} */
    @Override
    protected FieldEclipseDetector<T> create(final T newMaxCheck, final T newThreshold,
                                     final int nawMaxIter, final FieldEventHandler<? super FieldEclipseDetector<T>, T> newHandler) {
        return new FieldEclipseDetector<>(newMaxCheck, newThreshold, nawMaxIter, newHandler,
                                          occulted, occultedRadius, occulting, occultingRadius, totalEclipse);
    }

    /**
     * Setup the detector to full umbra detection.
     * <p>
     * This will override a penumbra/umbra flag if it has been configured previously.
     * </p>
     * @return a new detector with updated configuration (the instance is not changed)
     * @see #withPenumbra()
     * @since 6.1
     */
    public FieldEclipseDetector<T> withUmbra() {
        return new FieldEclipseDetector<>(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), getHandler(),
                                          occulted, occultedRadius, occulting, occultingRadius,
                                          true);
    }

    /**
     * Setup the detector to penumbra detection.
     * <p>
     * This will override a penumbra/umbra flag if it has been configured previously.
     * </p>
     * @return a new detector with updated configuration (the instance is not changed)
     * @see #withUmbra()
     * @since 6.1
     */
    public FieldEclipseDetector<T> withPenumbra() {
        return new FieldEclipseDetector<>(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), getHandler(),
                                          occulted, occultedRadius, occulting, occultingRadius,
                                          false);
    }

    /** Get the occulting body.
     * @return the occulting body
     */
    public PVCoordinatesProvider getOcculting() {
        return occulting;
    }

    /** Get the occulting body radius (m).
     * @return the occulting body radius
     */
    public double getOccultingRadius() {
        return occultingRadius;
    }

    /** Get the occulted body.
     * @return the occulted body
     */
    public PVCoordinatesProvider getOcculted() {
        return occulted;
    }

    /** Get the occulted body radius (m).
     * @return the occulted body radius
     */
    public double getOccultedRadius() {
        return occultedRadius;
    }

    /** Get the total eclipse detection flag.
     * @return the total eclipse detection flag (true for umbra events detection,
     * false for penumbra events detection)
     */
    public boolean getTotalEclipse() {
        return totalEclipse;
    }

    /** Compute the value of the switching function.
     * This function becomes negative when entering the region of shadow
     * and positive when exiting.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public T g(final FieldSpacecraftState<T> s) throws OrekitException {
        final Vector3D pted = occulted.getPVCoordinates(s.getDate().toAbsoluteDate(), s.getFrame()).getPosition();
        final Vector3D ping = occulting.getPVCoordinates(s.getDate().toAbsoluteDate(), s.getFrame()).getPosition();
        final Vector3D psat = s.toSpacecraftState().getPVCoordinates().getPosition();
        final Vector3D ps   = pted.subtract(psat);
        final Vector3D po   = ping.subtract(psat);
        final double angle  = Vector3D.angle(ps, po);
        final double rs     = FastMath.asin(occultedRadius / ps.getNorm());
        if (Double.isNaN(rs)) {
            return s.getOrbit().getA().getField().getZero().add(FastMath.PI);
        }
        final double ro     = FastMath.asin(occultingRadius / po.getNorm());
        if (Double.isNaN(ro)) {
            return s.getOrbit().getA().getField().getZero().add(-FastMath.PI);
        }
        return totalEclipse ? (s.getOrbit().getA().getField().getZero().add(angle - ro + rs)) :
                              (s.getOrbit().getA().getField().getZero().add(angle - ro - rs));
    }

}
