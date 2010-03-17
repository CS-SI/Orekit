/* Copyright 2002-2010 CS Communication & Systèmes
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

import org.orekit.errors.FrameAncestorException;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;


/** Tridimensional references frames class.
 *
 * <h5> Frame Presentation </h5>
 * <p>This class is the base class for all frames in OREKIT. The frames are
 * linked together in a tree with some specific frame chosen as the root of the tree.
 * Each frame is defined by {@link Transform transforms} combining any number
 * of translations and rotations from a reference frame which is its
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
 * @author Guylaine Prat
 * @author Luc Maisonobe
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class Frame implements Serializable {

    /** Serialiazable UID. */
    private static final long serialVersionUID = -6981146543760234087L;

    /** Parent frame (only the root frame doesn't have a parent). */
    private final Frame parent;

    /** Transform from parent frame to instance. */
    private Transform transform;

    /** Map of deepest frames commons with other frames. */
    private final HashMap<Frame, Frame> commons;

    /** Instance name. */
    private final String name;

    /** Indicator for quasi-inertial frames. */
    private final boolean quasiInertial;

    /** Private constructor used only for the root frame.
     * @param name name of the frame
     * @param quasiInertial true if frame is considered quasi-inertial
     * (i.e. suitable for propagating orbit)
     */
    private Frame(final String name, final boolean quasiInertial) {
        parent    = null;
        transform = Transform.IDENTITY;
        commons   = new HashMap<Frame, Frame>();
        this.name = name;
        this.quasiInertial = quasiInertial;
    }

    /** Build a non-inertial frame from its transform with respect to its parent.
     * <p>calling this constructor is equivalent to call
     * <code>{link {@link #Frame(Frame, Transform, String, boolean)
     * Frame(parent, transform, name, false)}</code>.</p>
     * @param parent parent frame (must be non-null)
     * @param transform transform from parent frame to instance
     * @param name name of the frame
     * @exception IllegalArgumentException if the parent frame is null
     */
    public Frame(final Frame parent, final Transform transform, final String name)
        throws IllegalArgumentException {
        this(parent, transform, name, false);
    }

    /** Build a frame from its transform with respect to its parent.
     * <p>The convention for the transform is that it is from parent
     * frame to instance. This means that the two following frames
     * are similar:</p>
     * <pre>
     * Frame frame1 = new Frame(FramesFactory.getGCRF(), new Transform(t1, t2));
     * Frame frame2 = new Frame(new Frame(FramesFactory.getGCRF(), t1), t2);
     * </pre>
     * @param parent parent frame (must be non-null)
     * @param transform transform from parent frame to instance
     * @param name name of the frame
     * @param quasiInertial true if frame is considered quasi-inertial
     * (i.e. suitable for propagating orbit)
     * @exception IllegalArgumentException if the parent frame is null
     */
    public Frame(final Frame parent, final Transform transform, final String name,
                 final boolean quasiInertial)
        throws IllegalArgumentException {

        if (parent == null) {
            throw OrekitException.createIllegalArgumentException("null parent for frame {0}",
                                                                 name);
        }
        this.name          = name;
        this.quasiInertial = quasiInertial;
        this.parent        = parent;
        this.transform     = transform;
        commons            = new HashMap<Frame, Frame>();

    }

    /** Get the name.
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /** Check if the frame is quasi-inertial.
     * <p>Quasi-inertial frames are frames that do have a linear motion and
     * either do not rotate or rotate at a very low rate resulting in
     * neglectible inertial forces. This means they are suitable for orbit
     * definition and propagation. Frames that are <em>not</em>
     * quasi-inertial are <em>not</em> suitable for orbit definition and
     * propagation.</p>
     * @return true if frame is quasi-inertial
     */
    public boolean isQuasiInertial() {
        return quasiInertial;
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
     *              GCRF
     *                |
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
                                                 fA.getName(), fB.getName(), getName());
            }

            // swap f1 and f2 to make sure the child is f2
            final Frame tmp = fA;
            fA = fB;
            fB = tmp;
            fAtoB = fAtoB.getInverse();

        } else  if (!(fB.isChildOf(this) || (fB == this))) {
            throw new FrameAncestorException("neither frames {0} nor {1} have {2} as ancestor",
                                             fA.getName(), fB.getName(), getName());
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

    /** Get the unique root frame.
     * @return the unique instance of the root frame
     */
    protected static Frame getRoot() {
        return LazyRootHolder.INSTANCE;
    }

    /** Get the unique J2000 frame.
     * <p>The J2000 frame is also called the EME2000 frame.
     * The latter denomination is preferred in Orekit.</p>
     * @return the unique instance of the J2000 frame
     * @deprecated as of 4.0, replaced by {@link FramesFactory#getEME2000()}
     * @see FramesFactory#getEME2000()
     */
    @Deprecated
    public static Frame getJ2000() {
        return FramesFactory.getEME2000();
    }

    /** Get the unique EME2000 frame.
     * <p>The EME2000 frame is also called the J2000 frame.
     * The former denomination is preferred in Orekit.</p>
     * @return the unique instance of the EME2000 frame
     * @deprecated as of 4.1, replaced by {@link FramesFactory#getEME2000()}
     */
    @Deprecated
    public static Frame getEME2000() {
        return FramesFactory.getEME2000();
    }

    /** Get the ITRF2005 reference frame.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     * @deprecated as of 4.1, replaced by {@link FramesFactory#getITRF2005()}
     */
    @Deprecated
    public static Frame getITRF2005()
        throws OrekitException {
        return FramesFactory.getITRF2005();
    }

    /** Get the TIRF2000 reference frame.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     * @deprecated as of 4.1, replaced by {@link FramesFactory#getTIRF2000()}
     */
    @Deprecated
    public static Frame getTIRF2000()
        throws OrekitException {
        return FramesFactory.getTIRF2000();
    }

    /** Get the CIRF2000 reference frame.
     * @return the selected reference frame singleton.
     * @exception OrekitException if the precession-nutation model data embedded in the
     * library cannot be read.
     * @deprecated as of 4.1, replaced by {@link FramesFactory#getCIRF2000()}
     */
    @Deprecated
    public static Frame getCIRF2000()
        throws OrekitException {
        return FramesFactory.getCIRF2000();
    }

    /** Get the VEIS 1950 reference frame.
     * @return the selected reference frame singleton.
     * @exception OrekitException if data data embedded in the
     * library cannot be read.
     * @deprecated as of 4.1, replaced by {@link FramesFactory#getVeis1950()}
     */
    @Deprecated
    public static Frame getVeis1950() throws OrekitException {
        return FramesFactory.getVeis1950();
    }

    // We use the Initialization on demand holder idiom to store
    // the singletons, as it is both thread-safe, efficient (no
    // synchronization) and works with all versions of java.

    /** Holder for the root frame singleton. */
    private static class LazyRootHolder {

        /** Unique instance. */
        private static final Frame INSTANCE = new Frame("GCRF", true);

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyRootHolder() {
        }

    }

}
