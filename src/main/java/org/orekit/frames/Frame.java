/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.frames;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import org.apache.commons.math.geometry.Rotation;
import org.orekit.errors.FrameAncestorException;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;


/** Tridimensional references frames class.
 *
 * <p><h5> Frame Presentation </h5>
 * This class is the base class for all frames in OREKIT. The frames are
 * linked together in a tree with the <i>Geocentric Celestial Reference Frame</i>
 * (GCRF) frame as the root of the tree. Each frame is defined by {@link Transform transforms}
 * combining any number of translations and rotations from a reference frame which is its
 * parent frame in the tree structure.</p>
 * <p>When we say a {@link Transform transform} t is <em>from frame<sub>A</sub>
 * to frame<sub>B</sub></em>, we mean that if the coordinates of some absolute
 * vector (say the direction of a distant star for example) has coordinates
 * u<sub>A</sub> in frame<sub>A</sub> and u<sub>B</sub> in frame<sub>B</sub>,
 * then u<sub>B</sub>={@link Transform#transformVector(Vector3D) t.transformVector(u<sub>A</sub>)}.
 * <p>The transforms may be constant or varying. For simple fixed transforms,
 * using this base class is sufficient. For varying transforms (time-dependent
 * or telemetry-based for example), it may be useful to define specific subclasses
 * that will implement {@link #updateFrame(AbsoluteDate)} or that will
 * add some specific <code>updateFromTelemetry(telemetry)</code>
 * methods that will compute the transform and call internally
 * the {@link #setTransform(Transform)} method.</p>
 *
 * <h5> Reference Frames </h5>
 * <p>
 *  Several Reference frames are implemented in OREKIT. The user can retrieve
 *  them using various static methods({@link #getGCRF()}, {@link #getEME2000()}, {@link
 *  #getITRF2005()} (or {@link #getITRF2005IgnoringTidalEffects()}), {@link
 *  #getCIRF2000()}, {@link #getTIRF2000()} (or {@link #getTIRF2000IgnoringTidalEffects()})
 *  and {@link #getVeis1950()}).
 * <p>
 *
 * <h5> International Terrestrial Reference Frame 2005 </h5>
 * <p>
 * This frame is the current (as of 2008) reference realization of
 * the International Terrestrial Reference System produced by IERS.
 * It is described in <a
 * href="http://www.iers.org/documents/publications/tn/tn32/tn32.pdf">
 * IERS conventions (2003)</a>. It replaces the Earth Centered Earth Fixed
 * frame which is the reference frame for GPS satellites.
 * <p>This frame is used to define position on solid Earth. It rotates with
 * the Earth and includes the pole motion with respect to Earth crust as
 * provided by {@link org.orekit.data.DataDirectoryCrawler IERS data}.
 * Its pole axis is the IERS Reference Pole (IRP).
 * </p>
 * <p>
 * OREKIT proposes all the intermediate frames used to build this specific frame.
 * Here is a schematic representation of the ITRF frame tree :
 * </p>
 * <pre>
 *
 *                                                   GCRF
 *                                   (frame bias)      |
 *                              -----------------------|
 *                              |                      |
 *                          EME2000                    |
 *                                                     |
 *             Bias, Precession and Nutation effects   |
 *                                                     |
 *                                                     |
 *    (Celestial Intermediate Reference Frame)     CIRF2000
 *                                                     |
 *                              Earth natural rotation |
 *                                                     |
 *                                                     |---------------------
 *                                                     |                    |
 *   (Terrestrial Intermediate Reference Frame)     TIRF2000     TIRF2000 without tidal effects
 *                                                     |                    |
 *                                        Pole motion  |                    |
 *                                                     |                    |
 *     (International Terrestrial Reference Frame)  ITRF2005     ITRF2005 without tidal effects
 *
 * </pre>
 * <p>This implementation follows the new non-rotating origin paradigm
 * mandated by IAU 2000 resolution B1.8. It is therefore based on
 * Celestial Ephemeris Origin (CEO-based) and Earth Rotating Angle.
 * </p>
 * <p>Other implementations of the ITRF 2005 are possible by
 * ignoring the B1.8 resolution and using the classical paradigm which
 * is equinox-based and relies on a specifically tuned Greenwich Sidereal Time.
 * They are not yet available in the OREKIT library.</p>
 * </p>
 *
 * @author Guylaine Prat
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class Frame implements Serializable {

    /** Serialiazable UID. */
    private static final long serialVersionUID = -6981146543760234087L;

    /** Parent frame (only GCRF doesn't have a parent). */
    private final Frame parent;

    /** Transform from parent frame to instance. */
    private Transform transform;

    /** Map of deepest frames commons with other frames. */
    private final HashMap<Frame, Frame> commons;

    /** Instance name. */
    private final String name;

    /** Private constructor used only for the GCRF root frame.
     * @param name name of the frame
     */
    private Frame(final String name) {
        parent    = null;
        transform = Transform.IDENTITY;
        commons   = new HashMap<Frame, Frame>();
        this.name = name;
    }

    /** Build a frame from its transform with respect to its parent.
     * <p>The convention for the transform is that it is from parent
     * frame to instance. This means that the two following frames
     * are similar:</p>
     * <pre>
     * Frame frame1 = new Frame(Frame.getGCRF(), new Transform(t1, t2));
     * Frame frame2 = new Frame(new Frame(Frame.getGCRF(), t1), t2);
     * </pre>
     * @param parent parent frame (must be non-null)
     * @param transform transform from parent frame to instance
     * @param name name of the frame
     * @exception IllegalArgumentException if the parent frame is null
     */
    public Frame(final Frame parent, final Transform transform, final String name)
        throws IllegalArgumentException {

        if (parent == null) {
            throw OrekitException.createIllegalArgumentException("null parent for frame {0}",
                                                                 new Object[] {
                                                                     name
                                                                 });
        }
        this.name      = name;
        this.parent    = parent;
        this.transform = transform;
        commons        = new HashMap<Frame, Frame>();

    }

    /** Get the name.
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /** New definition of the java.util toString() method.
     * @return the name
     */
    public String toString() {
        return this.name;
    }

    /** Get the parent frame.
     * @return parent frame
     */
    public Frame getParent() {
        return parent;
    }

    /** Update the transform from the parent frame to the instance.
     * @param transform new transform from parent frame to instance
     */
    public void setTransform(final Transform transform) {
        this.transform = transform;
    }

    /** Get the transform from the instance to another frame.
     * @param destination destination frame to which we want to transform vectors
     * @param date the date (can be null if it is sure than no date dependent frame is used)
     * @return transform from the instance to the destination frame
     * @exception OrekitException if some frame specific error occurs
     */
    public Transform getTransformTo(final Frame destination, final AbsoluteDate date)
        throws OrekitException {

        if (this == destination) {
            // shortcut for special case that may be frequent
            return Transform.IDENTITY;
        }

        // common ancestor to both frames in the frames tree
        final Frame common = findCommon(this, destination);

        // transform from common to instance
        Transform commonToInstance = Transform.IDENTITY;
        for (Frame frame = this; frame != common; frame = frame.parent) {
            frame.updateFrame(date);
            commonToInstance =
                new Transform(frame.transform, commonToInstance);
        }

        // transform from destination up to common
        Transform commonToDestination = Transform.IDENTITY;
        for (Frame frame = destination; frame != common; frame = frame.parent) {
            frame.updateFrame(date);
            commonToDestination =
                new Transform(frame.transform, commonToDestination);
        }

        // transform from instance to destination via common
        return new Transform(commonToInstance.getInverse(), commonToDestination);

    }

    /** Update the frame to the given date.
     * <p>This method is called each time {@link #getTransformTo(Frame, AbsoluteDate)}
     * is called. The base implementation in the {@link Frame} class does nothing.
     * The proper way to build a date-dependent frame is to extend the {@link Frame}
     * class and implement this method which will have to call {@link
     * #setTransform(Transform)} with the new transform </p>
     * @param date new value of the  date
     * @exception OrekitException if some frame specific error occurs
     */
    protected void updateFrame(final AbsoluteDate date) throws OrekitException {
        // do nothing in the base implementation
    }

    /** Update the transform from parent frame implicitly according to two other
     * frames.

     * <p>This method allows to control the relative position of two parts
     * of the global frames tree using any two frames in each part as
     * control handles. Consider the following simplified frames tree as an
     * example:</p>
     * <pre>
     *               GCRF
     *                 |
     *  --------------------------------
     *  |             |                |
     * Sun        satellite          Earth
     *                |                |
     *        on-board antenna   ground station
     *                                 |
     *                          tracking antenna
     * </pre>
     * <p>Tracking measurements really correspond to the link between the ground
     * and on-board antennas. This is tightly linked to the transform between
     * these two frames, however neither frame is the direct parent frame of the
     * other ones: the path involves four intermediate frames. When we process a
     * measurement, what we really want to update is the transform that defines
     * the satellite frame with respect to its parent GCRF frame. This
     * is the purpose of this method. This update is done by the following call,
     * where <code>measurementTransform</code> represent the measurement as a
     * simple translation transform between the two antenna frames:</p>
     * <pre><code>
     * satellite.updateTransform(onBoardAntenna, trackingAntenna,
     *                           measurementTransform, date);
     * </code></pre>
     * <p>One way to represent the behavior of the method is to consider the
     * sub-tree rooted at the instance on one hand (satellite and on-board antenna
     * in the example above) and the tree containing all the other frames on the
     * other hand (GCRF, Sun, Earth, ground station, tracking antenna).
     * Both tree are considered as solid sets linked by a flexible spring, which is
     * the transform we want to update. The method stretches the spring to make
     * sure the transform between the two specified frames (one in each tree part)
     * matches the specified transform.</p>
     * @param f1 first control frame (may be the instance itself)
     * @param f2 second control frame (may be the instance itself)
     * @param f1Tof2 desired transform from first to second control frame
     * @param date date of the transform
     * @exception OrekitException if the path between the two control frames does
     * not cross the link between instance and its parent frame or if some
     * intermediate transform fails
     * @see #setTransform(Transform)
     */
    public void updateTransform(final Frame f1, final Frame f2, final Transform f1Tof2,
                                final AbsoluteDate date) throws OrekitException {

        Frame fA = f1;
        Frame fB = f2;
        Transform fAtoB = f1Tof2;

        // make sure f1 is not a child of the instance
        if (fA.isChildOf(this) || (fA == this)) {

            if (fB.isChildOf(this) || (fB == this)) {
                throw new FrameAncestorException("both frames {0} and {1} are child of {2}",
                                                 new Object[] {
                                                     fA.getName(), fB.getName(), getName()
                                                 });
            }

            // swap f1 and f2 to make sure the child is f2
            final Frame tmp = fA;
            fA = fB;
            fB = tmp;
            fAtoB = fAtoB.getInverse();

        } else  if (!(fB.isChildOf(this) || (fB == this))) {
            throw new FrameAncestorException("neither frames {0} nor {1} have {2} as ancestor",
                                             new Object[] {
                                                 fA.getName(), fB.getName(), getName()
                                             });
        }

        // rebuild the transform by traveling from parent to self
        // WITHOUT using the existing this.transform that will be updated
        final Transform parentTofA = parent.getTransformTo(fA, date);
        final Transform fBtoSelf   = fB.getTransformTo(this, date);
        final Transform fAtoSelf   = new Transform(fAtoB, fBtoSelf);
        setTransform(new Transform(parentTofA, fAtoSelf));

    }

    /** Find the deepest common ancestor of two frames in the frames tree.
     * @param from origin frame
     * @param to destination frame
     * @return an ancestor frame of both <code>from</code> and <code>to</code>
     */
    private static Frame findCommon(final Frame from, final Frame to) {

        // have we already computed the common frame for this pair ?
        Frame common = (Frame) from.commons.get(to);
        if (common != null) {
            return common;
        }

        // definitions of the path up to the head tree for each frame
        final LinkedList<Frame> pathFrom = from.pathToRoot();
        final LinkedList<Frame> pathTo   = to.pathToRoot();

        if (pathFrom.isEmpty() || pathTo.contains(from)) {
            // handle root case and same branch case
            common = from;
        }
        if (pathTo.isEmpty() || pathFrom.contains(to)) {
            // handle root case and same branch case
            common = to;
        }
        if (common != null) {
            from.commons.put(to, common);
            to.commons.put(from, common);
            return common;
        }

        // at this stage pathFrom contains at least one frame
        Frame lastFrom = (Frame) pathFrom.removeLast();
        common = lastFrom; // common must be one of the instance of Frame already defined

        // at the beginning of the loop pathTo contains at least one frame
        for (Frame lastTo = (Frame) pathTo.removeLast();
             (lastTo == lastFrom) && (lastTo != null) && (lastFrom != null);
             lastTo = (Frame) (pathTo.isEmpty() ? null : pathTo.removeLast())) {
            common = lastFrom;
            lastFrom = (Frame) (pathFrom.isEmpty() ? null : pathFrom.removeLast());
        }

        from.commons.put(to, common);
        to.commons.put(from, common);
        return common;

    }

    /** Determine if a Frame is a child of another one.
     * @param potentialAncestor supposed ancestor frame
     * @return true if the potentialAncestor belongs to the
     * path from instance to the root frame
     */
    public boolean isChildOf(final Frame potentialAncestor) {
        for (Frame frame = parent; frame != null; frame = frame.parent) {
            if (frame == potentialAncestor) {
                return true;
            }
        }
        return false;
    }

    /** Get the path from instance frame to the root frame.
     * @return path from instance to root, excluding instance itself
     * (empty if instance is root)
     */
    private LinkedList<Frame> pathToRoot() {
        final LinkedList<Frame> path = new LinkedList<Frame>();
        for (Frame frame = parent; frame != null; frame = frame.parent) {
            path.add(frame);
        }
        return path;
    }

    /** Get the unique GCRF frame.
     * @return the unique instance of the GCRF frame
     */
    public static Frame getGCRF() {
        return LazyGCRFHolder.INSTANCE;
    }

    /** Get the unique J2000 frame.
     * <p>The J2000 frame is also called the EME2000 frame.
     * The later denomination is preferred in Orekit.</p>
     * @return the unique instance of the J2000 frame
     * @deprecated as of 4.0, replaced by {@link #getEME2000()}
     * @see #getEME2000()
     */
    public static Frame getJ2000() {
        return LazyEME2000Holder.INSTANCE;
    }

    /** Get the unique EME2000 frame.
     * <p>The EME2000 frame is also called the J2000 frame.
     * The former denomination is preferred in Orekit.</p>
     * @return the unique instance of the EME2000 frame
     */
    public static Frame getEME2000() {
        return LazyEME2000Holder.INSTANCE;
    }

    /** Get the ITRF2005 reference frame.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getITRF2005()
        throws OrekitException {
        if (LazyITRF2005Holder.INSTANCE == null) {
            throw LazyITRF2005Holder.OREKIT_EXCEPTION;
        }
        return LazyITRF2005Holder.INSTANCE;
    }

    /** Get the ITRF2005 without tidal effects reference frame.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getITRF2005IgnoringTidalEffects()
        throws OrekitException {
        if (LazyITRF2005IgnoredTidalEffectHolder.INSTANCE == null) {
            throw LazyITRF2005IgnoredTidalEffectHolder.OREKIT_EXCEPTION;
        }
        return LazyITRF2005IgnoredTidalEffectHolder.INSTANCE;
    }

    /** Get the TIRF2000 with tidal effects reference frame.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getTIRF2000()
        throws OrekitException {
        if (LazyTIRF2000Holder.INSTANCE == null) {
            throw LazyTIRF2000Holder.OREKIT_EXCEPTION;
        }
        return LazyTIRF2000Holder.INSTANCE;
    }

    /** Get the TIRF2000 without tidal effects reference frame.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getTIRF2000IgnoringTidalEffects()
        throws OrekitException {
        if (LazyTIRF2000IgnoredTidalEffectsHolder.INSTANCE == null) {
            throw LazyTIRF2000IgnoredTidalEffectsHolder.OREKIT_EXCEPTION;
        }
        return LazyTIRF2000IgnoredTidalEffectsHolder.INSTANCE;
    }

    /** Get the CIRF2000 reference frame.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getCIRF2000()
        throws OrekitException {
        if (LazyCIRF2000Holder.INSTANCE == null) {
            throw LazyCIRF2000Holder.OREKIT_EXCEPTION;
        }
        return LazyCIRF2000Holder.INSTANCE;
    }

    /** Get the VEIS 1950 reference frame.
     * @return the selected reference frame singleton.
     */
    public static Frame getVeis1950() {
        return LazyVeis1950Holder.INSTANCE;
    }

    // We use the Initialization on demand holder idiom to store
    // the singletons, as it is both thread-safe, efficient (no
    // synchronization) and works with all versions of java.

    /** Holder for the GCRF frame singleton. */
    private static class LazyGCRFHolder {

        /** Unique instance. */
        private static final Frame INSTANCE = new Frame("GCRF");

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyGCRFHolder() {
        }

    }

    /** Holder for the EME2000 frame singleton. */
    private static class LazyEME2000Holder {

        /** Unique instance. */
        private static final Frame INSTANCE = new EME2000Frame("EME2000");

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyEME2000Holder() {
        }

    }

    /** Holder for the ITRF 2005 frame with tidal effects singleton. */
    private static class LazyITRF2005Holder {

        /** Unique instance. */
        private static final Frame INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            Frame tmpFrame = null;
            OrekitException tmpException = null;
            try {
                if (LazyTIRF2000Holder.INSTANCE == null) {
                    tmpException = LazyTIRF2000Holder.OREKIT_EXCEPTION;
                } else {
                    tmpFrame = new ITRF2005Frame(false, AbsoluteDate.J2000_EPOCH, "ITRF2005");
                }
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpFrame;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyITRF2005Holder() {
        }

    }

    /** Holder for the ITRF 2005 frame without tidal effects singleton. */
    private static class LazyITRF2005IgnoredTidalEffectHolder {

        /** Unique instance. */
        private static final Frame INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            Frame tmpFrame = null;
            OrekitException tmpException = null;
            try {
                if (LazyTIRF2000IgnoredTidalEffectsHolder.INSTANCE == null) {
                    tmpException = LazyTIRF2000IgnoredTidalEffectsHolder.OREKIT_EXCEPTION;
                } else {
                    tmpFrame = new ITRF2005Frame(true, AbsoluteDate.J2000_EPOCH, "ITRF2005");
                }
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpFrame;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyITRF2005IgnoredTidalEffectHolder() {
        }

    }

    /** Holder for the TIRF 2000 frame with tidal effects singleton. */
    private static class LazyTIRF2000Holder {

        /** Unique instance. */
        private static final Frame INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            Frame tmpFrame = null;
            OrekitException tmpException = null;
            try {
                if (LazyCIRF2000Holder.INSTANCE == null) {
                    tmpException = LazyCIRF2000Holder.OREKIT_EXCEPTION;
                } else {
                    tmpFrame = new TIRF2000Frame(false, AbsoluteDate.J2000_EPOCH, "TIRF2000");
                }
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpFrame;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyTIRF2000Holder() {
        }

    }

    /** Holder for the TIRF 2000 frame without tidal effects singleton. */
    private static class LazyTIRF2000IgnoredTidalEffectsHolder {

        /** Unique instance. */
        private static final Frame INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            Frame tmpFrame = null;
            OrekitException tmpException = null;
            try {
                if (LazyCIRF2000Holder.INSTANCE == null) {
                    tmpException = LazyCIRF2000Holder.OREKIT_EXCEPTION;
                } else {
                    tmpFrame = new TIRF2000Frame(true, AbsoluteDate.J2000_EPOCH, "TIRF2000");
                }
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpFrame;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyTIRF2000IgnoredTidalEffectsHolder() {
        }

    }

    /** Holder for the CIRF 2000 frame singleton. */
    private static class LazyCIRF2000Holder {

        /** Unique instance. */
        private static final Frame INSTANCE;

        /** Reason why the unique instance may be missing (i.e. null). */
        private static final OrekitException OREKIT_EXCEPTION;

        static {
            Frame tmpFrame = null;
            OrekitException tmpException = null;
            try {
                tmpFrame = new CIRF2000Frame(AbsoluteDate.J2000_EPOCH, "CIRF2000");
            } catch (OrekitException oe) {
                tmpException = oe;
            }
            INSTANCE = tmpFrame;
            OREKIT_EXCEPTION = tmpException;
        }

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyCIRF2000Holder() {
        }

    }

    /** Holder for the Veis 1950 frame singleton. */
    private static class LazyVeis1950Holder {

        /** Unique instance. */
        private static final Frame INSTANCE =
            new Frame(getEME2000(),
                      new Transform(new Rotation(0.99998141186121629647,
                                                 -2.01425201682020570e-5,
                                                 -2.43283773387856897e-3,
                                                 5.59078052583013584e-3,
                                                 true)),
                      "VEIS1950");


        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyVeis1950Holder() {
        }

    }

}
