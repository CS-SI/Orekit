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
package org.orekit.frames;

import java.io.Serializable;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;


/** Tridimensional references frames class.
 *
 * <h1> Frame Presentation </h1>
 * <p>This class is the base class for all frames in OREKIT. The frames are
 * linked together in a tree with some specific frame chosen as the root of the tree.
 * Each frame is defined by {@link Transform transforms} combining any number
 * of translations and rotations from a reference frame which is its
 * parent frame in the tree structure.</p>
 * <p>When we say a {@link Transform transform} t is <em>from frame<sub>A</sub>
 * to frame<sub>B</sub></em>, we mean that if the coordinates of some absolute
 * vector (say the direction of a distant star for example) has coordinates
 * u<sub>A</sub> in frame<sub>A</sub> and u<sub>B</sub> in frame<sub>B</sub>,
 * then u<sub>B</sub>={@link
 * Transform#transformVector(org.hipparchus.geometry.euclidean.threed.Vector3D)
 * t.transformVector(u<sub>A</sub>)}.
 * <p>The transforms may be constant or varying, depending on the implementation of
 * the {@link TransformProvider transform provider} used to define the frame. For simple
 * fixed transforms, using {@link FixedTransformProvider} is sufficient. For varying
 * transforms (time-dependent or telemetry-based for example), it may be useful to define
 * specific implementations of {@link TransformProvider transform provider}.</p>
 *
 * @author Guylaine Prat
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
public class Frame implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -6981146543760234087L;

    /** Parent frame (only the root frame doesn't have a parent). */
    private final Frame parent;

    /** Depth of the frame with respect to tree root. */
    private final int depth;

    /** Provider for transform from parent frame to instance. */
    private final TransformProvider transformProvider;

    /** Instance name. */
    private final String name;

    /** Indicator for pseudo-inertial frames. */
    private final boolean pseudoInertial;

    /** Private constructor used only for the root frame.
     * @param name name of the frame
     * @param pseudoInertial true if frame is considered pseudo-inertial
     * (i.e. suitable for propagating orbit)
     */
    private Frame(final String name, final boolean pseudoInertial) {
        parent              = null;
        depth               = 0;
        transformProvider   = new FixedTransformProvider(Transform.IDENTITY);
        this.name           = name;
        this.pseudoInertial = pseudoInertial;
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

    /** Build a non-inertial frame from its transform with respect to its parent.
     * <p>calling this constructor is equivalent to call
     * <code>{link {@link #Frame(Frame, Transform, String, boolean)
     * Frame(parent, transform, name, false)}</code>.</p>
     * @param parent parent frame (must be non-null)
     * @param transformProvider provider for transform from parent frame to instance
     * @param name name of the frame
     * @exception IllegalArgumentException if the parent frame is null
     */
    public Frame(final Frame parent, final TransformProvider transformProvider, final String name)
        throws IllegalArgumentException {
        this(parent, transformProvider, name, false);
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
     * @param pseudoInertial true if frame is considered pseudo-inertial
     * (i.e. suitable for propagating orbit)
     * @exception IllegalArgumentException if the parent frame is null
     */
    public Frame(final Frame parent, final Transform transform, final String name,
                 final boolean pseudoInertial)
        throws IllegalArgumentException {
        this(parent, new FixedTransformProvider(transform), name, pseudoInertial);
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
     * @param transformProvider provider for transform from parent frame to instance
     * @param name name of the frame
     * @param pseudoInertial true if frame is considered pseudo-inertial
     * (i.e. suitable for propagating orbit)
     * @exception IllegalArgumentException if the parent frame is null
     */
    public Frame(final Frame parent, final TransformProvider transformProvider, final String name,
                 final boolean pseudoInertial)
        throws IllegalArgumentException {

        if (parent == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NULL_PARENT_FOR_FRAME, name);
        }
        this.parent            = parent;
        this.depth             = parent.depth + 1;
        this.transformProvider = transformProvider;
        this.name              = name;
        this.pseudoInertial    = pseudoInertial;

    }

    /** Get the name.
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /** Check if the frame is pseudo-inertial.
     * <p>Pseudo-inertial frames are frames that do have a linear motion and
     * either do not rotate or rotate at a very low rate resulting in
     * neglectible inertial forces. This means they are suitable for orbit
     * definition and propagation using Newtonian mechanics. Frames that are
     * <em>not</em> pseudo-inertial are <em>not</em> suitable for orbit
     * definition and propagation.</p>
     * @return true if frame is pseudo-inertial
     */
    public boolean isPseudoInertial() {
        return pseudoInertial;
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

    /** Get the depth of the frame.
     * <p>
     * The depth of a frame is the number of parents frame between
     * it and the frames tree root. It is 0 for the root frame, and
     * the depth of a frame is the depth of its parent frame plus one.
     * </p>
     * @return depth of the frame
     */
    public int getDepth() {
        return depth;
    }

    /** Get the n<sup>th</sup> ancestor of the frame.
     * @param n index of the ancestor (0 is the instance, 1 is its parent,
     * 2 is the parent of its parent...)
     * @return n<sup>th</sup> ancestor of the frame (must be between 0
     * and the depth of the frame)
     * @exception IllegalArgumentException if n is larger than the depth
     * of the instance
     */
    public Frame getAncestor(final int n) throws IllegalArgumentException {

        // safety check
        if (n > depth) {
            throw new OrekitIllegalArgumentException(OrekitMessages.FRAME_NO_NTH_ANCESTOR,
                                                     name, depth, n);
        }

        // go upward to find ancestor
        Frame current = this;
        for (int i = 0; i < n; ++i) {
            current = current.parent;
        }

        return current;

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
            commonToInstance =
                new Transform(date, frame.transformProvider.getTransform(date), commonToInstance);
        }

        // transform from destination up to common
        Transform commonToDestination = Transform.IDENTITY;
        for (Frame frame = destination; frame != common; frame = frame.parent) {
            commonToDestination =
                new Transform(date, frame.transformProvider.getTransform(date), commonToDestination);
        }

        // transform from instance to destination via common
        return new Transform(date, commonToInstance.getInverse(), commonToDestination);

    }

    /** Get the provider for transform from parent frame to instance.
     * @return provider for transform from parent frame to instance
     */
    public TransformProvider getTransformProvider() {
        return transformProvider;
    }

    /** Find the deepest common ancestor of two frames in the frames tree.
     * @param from origin frame
     * @param to destination frame
     * @return an ancestor frame of both <code>from</code> and <code>to</code>
     */
    private static Frame findCommon(final Frame from, final Frame to) {

        // select deepest frames that could be the common ancestor
        Frame currentF = from.depth > to.depth ? from.getAncestor(from.depth - to.depth) : from;
        Frame currentT = from.depth > to.depth ? to : to.getAncestor(to.depth - from.depth);

        // go upward until we find a match
        while (currentF != currentT) {
            currentF = currentF.parent;
            currentT = currentT.parent;
        }

        return currentF;

    }

    /** Determine if a Frame is a child of another one.
     * @param potentialAncestor supposed ancestor frame
     * @return true if the potentialAncestor belongs to the
     * path from instance to the root frame, excluding itself
     */
    public boolean isChildOf(final Frame potentialAncestor) {
        if (depth <= potentialAncestor.depth) {
            return false;
        }
        return getAncestor(depth - potentialAncestor.depth) == potentialAncestor;
    }

    /** Get the unique root frame.
     * @return the unique instance of the root frame
     */
    protected static Frame getRoot() {
        return LazyRootHolder.INSTANCE;
    }

    /** Get a new version of the instance, frozen with respect to a reference frame.
     * <p>
     * Freezing a frame consist in computing its position and orientation with respect
     * to another frame at some freezing date and fixing them so they do not depend
     * on time anymore. This means the frozen frame is fixed with respect to the
     * reference frame.
     * </p>
     * <p>
     * One typical use of this method is to compute an inertial launch reference frame
     * by freezing a {@link TopocentricFrame topocentric frame} at launch date
     * with respect to an inertial frame. Another use is to freeze an equinox-related
     * celestial frame at a reference epoch date.
     * </p>
     * <p>
     * Only the frame returned by this method is frozen, the instance by itself
     * is not affected by calling this method and still moves freely.
     * </p>
     * @param reference frame with respect to which the instance will be frozen
     * @param freezingDate freezing date
     * @param frozenName name of the frozen frame
     * @return a frozen version of the instance
     * @exception OrekitException if transform between reference frame and instance
     * cannot be computed at freezing frame
     */
    public Frame getFrozenFrame(final Frame reference, final AbsoluteDate freezingDate,
                                final String frozenName) throws OrekitException {
        return new Frame(reference, reference.getTransformTo(this, freezingDate).freeze(),
                         frozenName, reference.isPseudoInertial());
    }

    // We use the Initialization on demand holder idiom to store
    // the singletons, as it is both thread-safe, efficient (no
    // synchronization) and works with all versions of java.

    /** Holder for the root frame singleton. */
    private static class LazyRootHolder {

        /** Unique instance. */
        private static final Frame INSTANCE = new Frame("GCRF", true) {

            /** Serializable UID. */
            private static final long serialVersionUID = -2654403496396721543L;

            /** Replace the instance with a data transfer object for serialization.
             * <p>
             * This intermediate class serializes nothing.
             * </p>
             * @return data transfer object that will be serialized
             */
            private Object writeReplace() {
                return new DataTransferObject();
            }

        };

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyRootHolder() {
        }

    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 4067764035816491212L;

        /** Simple constructor.
         */
        private DataTransferObject() {
        }

        /** Replace the deserialized data transfer object with a {@link FactoryManagedFrame}.
         * @return replacement {@link FactoryManagedFrame}
         */
        private Object readResolve() {
            return getRoot();
        }

    }

}
