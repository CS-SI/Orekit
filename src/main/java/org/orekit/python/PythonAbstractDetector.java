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
// original file AbstractDetector.java created by CS Systèmes d'Information

package org.orekit.python;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/** Common parts shared by several orbital events finders.
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
public class PythonAbstractDetector<T extends EventDetector> extends AbstractDetector<T> {

	/** Serializable UID. */
    private static final long serialVersionUID = -334171965326514174L;

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

	public PythonAbstractDetector(double maxCheck, double threshold,
			int maxIter, EventHandler<T> handler) {
		super(maxCheck, threshold, maxIter, handler);
	}
  	
  	
    /** {@inheritDoc} */
	@Override
    public native void init(final SpacecraftState s0, final AbsoluteDate t);

    /** {@inheritDoc} */
    @Override
    public native double g(SpacecraftState s) throws OrekitException;

    /** {@inheritDoc} */
    @Override
    public native double getMaxCheckInterval();

    /** {@inheritDoc} */
    @Override
    public native int getMaxIterationCount();

    /** {@inheritDoc} */
    @Override
    public native double getThreshold();

    /** {@inheritDoc} */
	@Override
	public native T create(double newMaxCheck, double newThreshold, int newMaxIter,
			EventHandler<? super T> newHandler);
	

}
