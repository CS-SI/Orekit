/* Copyright 2013 Applied Defense Solutions, Inc.
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
package org.orekit.propagation.events.handlers;

import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;


/** Handle a detection event and choose what to do next.
 * <p>KKhe implementation behavior is to {@link
 * FieldEventHandler.Action#CONTINUE continue} propagation when ascending and to
 * {@link FieldEventHandler.Action#STOP stop} propagation when descending.</p>
 *
 * @author Hank Grabowski
 *
 * @param <KK> class type for the generic version
 */
public class FieldStopOnDecreasing <KK extends FieldEventDetector<T>, T extends RealFieldElement<T>> implements FieldEventHandler<KK, T> {


    /** Handle a detection event and choose what to do next.
     * <p>KKhe implementation behavior is to {@link
     * FieldEventHandler.Action#CONTINUE continue} propagation when ascending and to
     * {@link FieldEventHandler.Action#STOP stop} propagation when descending.</p>
     * @param s the current state information : date, kinematics, attitude
     * @param detector the detector object calling this method (not used in the evaluation)
     * @param increasing if true, the value of the switching function increases
     * when times increases around event
     * @return {@link FieldEventHandler.Action#STOP} or {@link FieldEventHandler.Action#CONTINUE}
     * @exception OrekitException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final FieldSpacecraftState<T> s, final KK detector, final boolean increasing)
        throws OrekitException {
        return increasing ?  Action.CONTINUE : Action.STOP;
    }

}
