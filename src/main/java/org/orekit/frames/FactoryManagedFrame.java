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
package org.orekit.frames;


/** Base class for the predefined frames that are managed by {@link Frames}.
 * @author Luc Maisonobe
 */
public class FactoryManagedFrame extends Frame {

    /** Key of the frame within the factory. */
    private final Predefined factoryKey;

    /** Simple constructor.
     * @param parent parent frame (must be non-null)
     * @param transformProvider provider for transform from parent frame to instance
     * @param pseudoInertial true if frame is considered pseudo-inertial
     * (i.e. suitable for propagating orbit)
     * @param factoryKey key of the frame within the factory
     */
    public FactoryManagedFrame(final Frame parent, final TransformProvider transformProvider,
                               final boolean pseudoInertial, final Predefined factoryKey) {
        super(parent, transformProvider, factoryKey.getName(), pseudoInertial);
        this.factoryKey = factoryKey;
    }

    /** Get the key of the frame within the factory.
     * @return key of the frame within the factory
     */
    public Predefined getFactoryKey() {
        return factoryKey;
    }

}
