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

import org.orekit.errors.OrekitException;

/** Interface for Transform providers that use {@link EOPHistory Earth Orientation Parameters}.
 * @author Luc Maisonobe
 * @since 7.1
 */
public interface EOPBasedTransformProvider extends TransformProvider {

    /** Get the EOP history.
     * @return EOP history
     */
    EOPHistory getEOPHistory();

    /** Get a version of the provider that does <em>not</em> cache tidal corrections.
     * <p>
     * This method removes the performance enhancing interpolation features that are
     * used by default in EOP-based provider, in order to focus on accuracy. The
     * interpolation features are intended to save processing time by avoiding doing
     * tidal correction evaluation at each time step and caching some results. This
     * method can be used to avoid this (it is automatically called by {@link
     * FramesFactory#getNonInterpolatingTransform(Frame, Frame, AbsoluteDate)}, when
     * very high accuracy is desired, or for testing purposes. It should be used with
     * care, as doing the full computation is <em>really</em> costly.
     * </p>
     * @return version of the provider that does <em>not</em> cache tidal corrections
     * @see FramesFactory#getNonInterpolatingTransform(Frame, Frame, AbsoluteDate)
     * @exception OrekitException if EOP cannot be retrieved
     */
    EOPBasedTransformProvider getNonInterpolatingProvider()
        throws OrekitException;

}
