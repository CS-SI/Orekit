/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for Attitude Parameter Message data.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class APMSpacecraftParameters {

    /** Comments. The list contains a string for each line of comment. */
    private List<String> comments;

    /** Coordinate system for the inertia tensor. */
    private String inertiaRefFrame;

    /** Moment of Inertia about the 1-axis (kg.m²). */
    private double i11;

    /** Moment of Inertia about the 2-axis (kg.m²). */
    private double i22;

    /** Moment of Inertia about the 3-axis (kg.m²). */
    private double i33;

    /** Inertia Cross Product of the 1 and 2 axes (kg.m²). */
    private double i12;

    /** Inertia Cross Product of the 1 and 3 axes (kg.m²). */
    private double i13;

    /** Inertia Cross Product of the 2 and 3 axes (kg.m²). */
    private double i23;

    /** Simple constructor.
     */
    public APMSpacecraftParameters() {
        this.comments = new ArrayList<>();
    }

    /** Get the comments.
     * @return the comments
     */
    public List<String> getComments() {
        return comments;
    }

    /** Add comment.
     * @param comment comment to add
     */
    public void addComment(final String comment) {
        comments.add(comment);
    }

    /**
     * Get the coordinate system for the inertia tensor.
     * @return the coordinate system for the inertia tensor
     */
    public String getInertiaRefFrameString() {
        return inertiaRefFrame;
    }

    /**
     * Set the coordinate system for the inertia tensor.
     * @param frame frame to be set
     */
    public void setInertiaRefFrameString(final String frame) {
        this.inertiaRefFrame = frame;
    }

    /**
     * Get the moment of Inertia about the 1-axis (N.m²).
     * @return the moment of Inertia about the 1-axis.
     */
    public double getI11() {
        return i11;
    }

    /**
     * Set the moment of Inertia about the 1-axis (N.m²).
     * @param i11 moment of Inertia about the 1-axis
     */
    public void setI11(final double i11) {
        this.i11 = i11;
    }

    /**
     * Get the moment of Inertia about the 2-axis (N.m²).
     * @return the moment of Inertia about the 2-axis.
     */
    public double getI22() {
        return i22;
    }

    /**
     * Set the moment of Inertia about the 2-axis (N.m²).
     * @param i22 moment of Inertia about the 2-axis
     */
    public void setI22(final double i22) {
        this.i22 = i22;
    }

    /**
     * Get the moment of Inertia about the 3-axis (N.m²).
     * @return the moment of Inertia about the 3-axis.
     */
    public double getI33() {
        return i33;
    }

    /**
     * Set the moment of Inertia about the 3-axis (N.m²).
     * @param i33 moment of Inertia about the 3-axis
     */
    public void setI33(final double i33) {
        this.i33 = i33;
    }

    /**
     * Get the moment of Inertia about the 1 and 2 axes (N.m²).
     * @return the moment of Inertia about the 1 and 2 axes.
     */
    public double getI12() {
        return i12;
    }

    /**
     * Set the moment of Inertia about the 1 and 2 axes (N.m²).
     * @param i12 moment of Inertia about the 1 and 2 axes
     */
    public void setI12(final double i12) {
        this.i12 = i12;
    }

    /**
     * Get the moment of Inertia about the 1 and 3 axes (N.m²).
     * @return the moment of Inertia about the 1 and 3 axes.
     */
    public double getI13() {
        return i13;
    }

    /**
     * Set the moment of Inertia about the 1 and 3 axes (N.m²).
     * @param i13 moment of Inertia about the 1 and 3 axes
     */
    public void setI13(final double i13) {
        this.i13 = i13;
    }

    /**
     * Get the moment of Inertia about the 2 and 3 axes (N.m²).
     * @return the moment of Inertia about the 2 and 3 axes.
     */
    public double getI23() {
        return i23;
    }

    /**
     * Set the moment of Inertia about the 2 and 3 axes (N.m²).
     * @param i23 moment of Inertia about the 2 and 3 axes
     */
    public void setI23(final double i23) {
        this.i23 = i23;
    }

}
