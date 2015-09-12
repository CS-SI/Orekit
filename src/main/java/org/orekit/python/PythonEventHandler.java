/** Copyright 2014 SSC and 2002-2014 CS Systèmes d'Information
 * Licensed to CS SystÃ¨mes d'Information (CS) under one or more
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

// this file was created by SCC and is largely a derived work from the
// original file EventHandler.java created by CS Systèmes d'Information

package org.orekit.python;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import static org.orekit.propagation.events.handlers.EventHandler.Action;

/** This interface represents space-dynamics aware events detectors.
*
* <p>It mirrors the {@link org.apache.commons.math3.ode.events.EventHandler
* EventHandler} interface from <a href="http://commons.apache.org/math/">
* Apache Commons Math</a> but provides a space-dynamics interface to the
* methods.</p>
*
* <p>Events detectors are a useful solution to meet the requirements
* of propagators concerning discrete conditions. The state of each
* event detector is queried by the integrator at each step. When the
* sign of the underlying g switching function changes, the step is rejected
* and reduced, in order to make sure the sign changes occur only at steps
* boundaries.</p>
*
* <p>When step ends exactly at a switching function sign change, the corresponding
* event is triggered, by calling the {@link #eventOccurred(SpacecraftState, boolean)}
* method. The method can do whatever it needs with the event (logging it, performing
* some processing, ignore it ...). The return value of the method will be used by
* the propagator to stop or resume propagation, possibly changing the state vector.<p>
*
* @author Luc Maisonobe
* @author V&eacute;ronique Pommier-Maurussane
*/
public class PythonEventHandler<T extends EventDetector> implements EventHandler<T>
{

	static final long serialVersionUID = 1L;

	/** Part of JCC Python interface to object */
	private long pythonObject;

	/** Part of JCC Python interface to object */
	public void pythonExtension(long pythonObject)
	{
		this.pythonObject = pythonObject;
	}

	/** Part of JCC Python interface to object */
	public long pythonExtension()
	{
		return this.pythonObject;
	}

	/** Part of JCC Python interface to object */
	public void finalize()
			throws Throwable
			{
		pythonDecRef();
			}

	/** Part of JCC Python interface to object */
	public native void pythonDecRef();
	



    /**
     * eventOccurred method mirrors the same interface method as in {@link EventDetector}
     * and its subclasses, but with an additional parameter that allows the calling
     * method to pass in an object from the detector which would have potential
     * additional data to allow the implementing class to determine the correct
     * return state.
     *
     * @param s SpaceCraft state to be used in the evaluation
     * @param detector object with appropriate type that can be used in determining correct return state
     * @param increasing with the event occured in an "increasing" or "decreasing" slope direction
     * @return the Action that the calling detector should pass back to the evaluation system
     *
     * @exception OrekitException if some specific error occurs
     */
    public native Action eventOccurred(SpacecraftState s, T detector, boolean increasing) throws OrekitException;

    /** Reset the state prior to continue propagation.
     * <p>This method is called after the step handler has returned and
     * before the next step is started, but only when {@link
     * #eventOccurred} has itself returned the {@link Action#RESET_STATE}
     * indicator. It allows the user to reset the state for the next step,
     * without perturbing the step handler of the finishing step. If the
     * {@link #eventOccurred} never returns the {@link Action#RESET_STATE}
     * indicator, this function will never be called, and it is safe to simply return null.</p>
     * @param detector object with appropriate type that can be used in determining correct return state
     * @param oldState old state
     * @return new state
     * @exception OrekitException if the state cannot be reseted
     */
    public native SpacecraftState resetState(T detector, SpacecraftState oldState) throws OrekitException;


}

