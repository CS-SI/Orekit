/* Copyright 2002-2025 CS GROUP
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
package org.orekit.files.rinex.clock;

/** Container for a receiver or a satellite with its position in the considered frame.
 * @since 14.0
 */
public class Receiver {

    /** Designator. */
    private final String designator;

    /** Receiver identifier. */
    private final String receiverIdentifier;

    /** X coordinates in file considered Earth centered frame (in meters). */
    private final double x;

    /** Y coordinates in file considered Earth centered frame (in meters). */
    private final double y;

    /** Z coordinates in file considered Earth centered frame (in meters). */
    private final double z;

    /** Constructor.
     * @param designator         the designator
     * @param receiverIdentifier the receiver identifier
     * @param x                  the X coordinate in meters in considered Earth centered frame
     * @param y                  the Y coordinate in meters in considered Earth centered frame
     * @param z                  the Z coordinate in meters in considered Earth centered frame
     */
    public Receiver(final String designator, final String receiverIdentifier,
                    final double x, final double y, final double z) {
        this.designator = designator;
        this.receiverIdentifier = receiverIdentifier;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** Getter for the designator.
     * @return the designator
     */
    public String getDesignator() {
        return designator;
    }

    /** Getter for the receiver identifier.
     * @return the receiver identifier
     */
    public String getReceiverIdentifier() {
        return receiverIdentifier;
    }

    /** Getter for the X coordinate in meters in considered Earth centered frame.
     * @return the X coordinate in meters in considered Earth centered frame
     */
    public double getX() {
        return x;
    }

    /** Getter for the Y coordinate in meters in considered Earth centered frame.
     * @return the Y coordinate in meters in considered Earth centered frame
     */
    public double getY() {
        return y;
    }

    /** Getter for the Z coordinate in meters in considered Earth centered frame.
     * @return the Z coordinate in meters in considered Earth centered frame
     */
    public double getZ() {
        return z;
    }

}
