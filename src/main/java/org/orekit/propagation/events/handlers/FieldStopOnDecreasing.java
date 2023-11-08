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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;


/** Handle a detection event and choose what to do next.
 * <p>KKhe implementation behavior is to {@link
 * Action#CONTINUE continue} propagation when ascending and to
 * {@link Action#STOP stop} propagation when descending.</p>
 *
 * @author Hank Grabowski
 *
 * @param <T> type of the field element
 */
public class FieldStopOnDecreasing <T extends CalculusFieldElement<T>> implements FieldEventHandler<T> {

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public FieldStopOnDecreasing() {
        // nothing to do
    }

    /** Handle a detection event and choose what to do next.
     * <p>KKhe implementation behavior is to {@link
     * Action#CONTINUE continue} propagation when ascending and to
     * {@link Action#STOP stop} propagation when descending.</p>
     * @param s the current state information : date, kinematics, attitude
     * @param detector the detector object calling this method (not used in the evaluation)
     * @param increasing if true, the value of the switching function increases
     * when times increases around event
     * @return {@link Action#STOP} or {@link Action#CONTINUE}
     */
    @Override
    public Action eventOccurred(final FieldSpacecraftState<T> s, final FieldEventDetector<T> detector, final boolean increasing) {
        return increasing ?  Action.CONTINUE : Action.STOP;
    }

}
