/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// this file was created by SCC and is largely a derived work from the
// original file UnivariateFunction.java

package org.orekit.python;

/** import org.hipparchus.analysis.UnivariateFunction; **/


public class PythonUnivariateFunction implements org.hipparchus.analysis.UnivariateFunction {

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
     * Compute the value of the function.
     *
     * @param x Point at which the function value should be computed.
     * @return the value of the function.
     * @throws IllegalArgumentException when the activated method itself can
     * ascertain that a precondition, specified in the API expressed at the
     * level of the activated method, has been violated.
     * When Commons Math throws an {@code IllegalArgumentException}, it is
     * usually the consequence of checking the actual parameters passed to
     * the method.
     */
	@Override
	public native double value(double x);

}

