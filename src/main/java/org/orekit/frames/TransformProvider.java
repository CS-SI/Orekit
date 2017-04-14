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

package org.orekit.frames;

import java.io.Serializable;

import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Interface for Transform providers.
 * <p>The transform provider interface is mainly used to define the
 * transform between a frame and its parent frame.
 * </p>
 * @author Luc Maisonobe
 */
public interface TransformProvider extends Serializable {

    /** Get the {@link Transform} corresponding to specified date.
     * @param date current date
     * @return transform at specified date
     * @exception OrekitException if transform cannot be computed at given date
     */
    Transform getTransform(AbsoluteDate date) throws OrekitException;

    /** Get the {@link FieldTransform} corresponding to specified date.
     * @param date current date
     * @param <T> type of the field elements
     * @return transform at specified date
     * @exception OrekitException if transform cannot be computed at given date
     * @since 9.0
     */
    <T extends RealFieldElement<T>> FieldTransform<T> getTransform(FieldAbsoluteDate<T> date) throws OrekitException;

}
