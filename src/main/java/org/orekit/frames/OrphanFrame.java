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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;


/** Prototype frame that can be built from leaf to roots and later attached to a tree.
 *
 * <p>Regular {@link Frame} instances can be built only from a parent frame, i.e.
 * the frames tree can be built only from root to leafs. In some cases, it may
 * desirable to build a subset tree and attach it to the main tree after build
 * time, which means the tree is built from leafs to root. This class allows
 * building this subtree.</p>
 * <p>
 * During the build process, the {@link Frame} associated with each {@link OrphanFrame}
 * is not available. It becomes available only once the {@link OrphanFrame} has been
 * attached to the main tree, and at this time it can be used to compute
 * {@link Transform transforms}.
 * </p>
 *
 * @author Luc Maisonobe
 * @since 6.0
 */
public class OrphanFrame implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130409L;

    /** Instance name. */
    private final String name;

    /** Children of the frame. */
    private final List<OrphanFrame> children;

    /** Parent orphan frame. */
    private OrphanFrame orphanParent;

    /** Provider for transform from parent frame to instance. */
    private TransformProvider provider;

    /** Indicator for pseudo-inertial frames. */
    private boolean pseudoInertial;

    /** Associated frame (available only once attached to the main frames tree). */
    private Frame frame;

    /** Simple constructor.
     * @param name name of the frame
     */
    public OrphanFrame(final String name) {
        children  = new ArrayList<OrphanFrame>();
        this.name = name;
    }

    /** Add a child.
     * <p>
     * If a child is added after the instance has been attached, the child and
     * all its tree will be attached immediately too.
     * </p>
     * @param child child to add
     * @param transform transform from instance to child
     * @param isPseudoInertial true if child is considered pseudo-inertial
     * (i.e. suitable for propagating orbit)
     * @exception OrekitException if child frame has already been attached to
     * either an {@link OrphanFrame} or to a {@link Frame}
     */
    public void addChild(final OrphanFrame child, final Transform transform,
                         final boolean isPseudoInertial)
        throws OrekitException {
        addChild(child, new FixedTransformProvider(transform), isPseudoInertial);
    }

    /** Add a child.
     * <p>
     * If a child is added after the instance has been attached, the child and
     * all its tree will be attached immediately too.
     * </p>
     * @param child child to add
     * @param transformProvider provider for transform from instance to child
     * @param isPseudoInertial true if child is considered pseudo-inertial
     * (i.e. suitable for propagating orbit)
     * @exception OrekitException if child frame has already been attached to
     * either an {@link OrphanFrame} or to a {@link Frame}
     */
    public void addChild(final OrphanFrame child, final TransformProvider transformProvider,
                         final boolean isPseudoInertial)
        throws OrekitException {

        // safety check
        if (child.orphanParent != null) {
            throw new OrekitException(OrekitMessages.FRAME_ALREADY_ATTACHED,
                                      child.name, child.orphanParent.name);
        }

        children.add(child);
        child.orphanParent   = this;
        child.provider       = transformProvider;
        child.pseudoInertial = isPseudoInertial;

        if (frame != null) {
            // we are attaching a child after having attached the instance,
            // we process the tree immediately
            buildTree();
        }

    }

    /** Attach the instance (and all its children down to leafs) to the main tree.
     * @param parent parent frame to attach to
     * @param transform transform from parent frame to instance
     * @param isPseudoInertial true if frame is considered pseudo-inertial
     * (i.e. suitable for propagating orbit)
     * @exception OrekitException if child frame has already been attached to
     * either an {@link OrphanFrame} or to a {@link Frame}
     */
    public void attachTo(final Frame parent, final Transform transform,
                         final boolean isPseudoInertial)
        throws OrekitException {
        attachTo(parent, new FixedTransformProvider(transform), isPseudoInertial);
    }

    /** Attach the instance (and all its children down to leafs) to the main tree.
     * @param parent parent frame to attach to
     * @param transformProvider provider for transform from parent frame to instance
     * @param isPseudoInertial true if frame is considered pseudo-inertial
     * (i.e. suitable for propagating orbit)
     * @exception OrekitException if child frame has already been attached to
     * either an {@link OrphanFrame} or to a {@link Frame}
     */
    public void attachTo(final Frame parent, final TransformProvider transformProvider,
                         final boolean isPseudoInertial)
        throws OrekitException {

        // safety check
        if (orphanParent != null) {
            throw new OrekitException(OrekitMessages.FRAME_ALREADY_ATTACHED,
                                      name, orphanParent.name);
        }

        // set up the attach point
        final OrphanFrame op = new OrphanFrame(parent.getName());
        op.frame = parent;
        op.addChild(this, transformProvider, isPseudoInertial);

    }

    /** Get all children of the instance.
     * @return unmodifiable list of children
     */
    public List<OrphanFrame> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /** Get the associated {@link Frame frame}.
     * @return associated frame
     * @exception OrekitException if instance has not been attached to the frames tree
     */
    public Frame getFrame() throws OrekitException {

        // safety check
        if (frame == null) {
            throw new OrekitException(OrekitMessages.FRAME_NOT_ATTACHED, name);
        }

        return frame;

    }

    /** Recursively build the frames tree starting at instance, which is already associated.
     */
    private void buildTree() {
        for (final OrphanFrame child : children) {

            if (child.frame == null) {

                // associate the child with a regular frame
                child.frame = new Frame(frame, child.provider, child.name, child.pseudoInertial);

                // recursively build the rest of the tree
                child.buildTree();

            }

        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.name;
    }

}
