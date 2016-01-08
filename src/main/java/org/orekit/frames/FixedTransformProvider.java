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

import org.orekit.time.AbsoluteDate;

/**
** Transform provider using fixed transform.
 * @author Luc Maisonobe
 */
public class FixedTransformProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 7143912747227560905L;

    /** Fixed transform. */
    private final Transform transform;

    /** Simple constructor.
     * @param transform fixed transform
     */
    public FixedTransformProvider(final Transform transform) {
        this.transform = transform;
    }

    /** {@inheritDoc} */
    public Transform getTransform(final AbsoluteDate date) {
        return transform;
    }

}
