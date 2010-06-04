/* Copyright 2002-2010 CS Communication & Systèmes
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
package org.orekit.propagation.events;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** Finder for date events triggered by a timer.
 * <p>The trigger date for the timer can be reset.</p>
 * <p>The default implementation behavior is to {@link EventDetector#STOP
 * stop} propagation when the timer sets the event off. This can be changed by
 * overriding the {@link #eventOccurred(SpacecraftState, boolean) eventOccurred}
 * method in a derived class.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class TimerDetector extends AbstractDetector implements TimeStamped {

    /** Serializable UID. */
    private static final long serialVersionUID = -334171965326514174L;

    /** Flag the current way of g with respect to time. */
    private boolean gIncrease = true;

    /** Current/previous value of g function. */
    private double gvalue = 0.0;

    /** Flag for date reset. */
    private boolean reset = true;

    /** Current trigger date. */
    private AbsoluteDate trigger;

    /** Current event target date. */
    private AbsoluteDate target;

    /** Timer duration. */
    private final double dt;

    /** Build a new instance.
     * @param triggerDate trigger date
     * @param duration timer duration from trigger date
     */
    public TimerDetector(final AbsoluteDate triggerDate, final double duration) {
        super(10.e9, 10.e-10);
        trigger = null;
        target = null;
        dt = duration;
        resetDate(triggerDate);
    }

    /** Build a new instance.
     *  <p>The trigger date is set to infinity, so this is a pending timer.</p>
     *  <p>It will be activated whith a call to {@link #resetDate(AbsoluteDate)}.</p>
     * @param duration timer duration
     */
    public TimerDetector(final double duration) {
        this(null, duration);
    }

    /** Handle a timed event and choose what to do next.
     * <p>The default implementation behavior is to {@link
     * EventDetector#STOP stop} propagation at target date occurrence.</p>
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases
     * when times increases around event.
     * @return {@link #STOP}
     * @exception OrekitException if some specific error occurs
     */
    public int eventOccurred(final SpacecraftState s, final boolean increasing)
        throws OrekitException {
        return STOP;
    }

    /** Compute the value of the switching function.
     * This function measures the difference between the current and the target date.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {
        final AbsoluteDate gDate = s.getDate();
        if (reset) {
        	if (Math.abs(gvalue) < this.getThreshold()) {
        		if (gIncrease) {
        			gIncrease = gDate.durationFrom(target) > 0 ? false : true;
        		} else {
        			gIncrease = gDate.durationFrom(target) > 0 ? true : false;
        		}
        	} else {
            	gIncrease = gvalue * gDate.durationFrom(target) < 0 ? false : true;
            }
            reset = false;
        }
        gvalue =  gIncrease ? gDate.durationFrom(target) : target.durationFrom(gDate);
        return gvalue;
    }

    /** Get the trigger date.
     * @return trigger date
     */
    public AbsoluteDate getDate() {
        return trigger;
    }

    /** Get the timer duration.
     * @return timer duration
     */
    public double getTimerDuration() {
        return dt;
    }

    /** Reset the trigger date.
     * @param triggerDate trigger date
     */
    public void resetDate(final AbsoluteDate triggerDate) {
        if (triggerDate != null) {
            trigger = triggerDate;
            target = new AbsoluteDate(triggerDate, dt);
        } else {
            trigger = AbsoluteDate.FUTURE_INFINITY;
            target = AbsoluteDate.FUTURE_INFINITY;
        }
        reset = true;
    }

}
