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
package org.orekit.forces.gravity.potential;

import org.orekit.errors.OrekitException;

/** Interface used to provide gravity field coefficients.
 * @see PotentialReaderFactory
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public interface PotentialCoefficientsProvider {

    /** Get the zonal coefficients.
     * @param normalized (true) or un-normalized (false)
     * @param n the maximal degree requested
     * @return J the zonal coefficients array.
     * @exception OrekitException if the requested maximal degree exceeds the
     * available degree
     */
    double[] getJ(boolean normalized, int n)
        throws OrekitException;

    /** Get the tesseral-sectorial and zonal coefficients.
     * @param n the degree
     * @param m the order
     * @param normalized (true) or un-normalized (false)
     * @return the cosines coefficients matrix
     * @exception OrekitException if the requested maximal degree or order exceeds the
     * available degree or order
     */
    double[][] getC(int n, int m, boolean normalized)
        throws OrekitException;

    /** Get tesseral-sectorial coefficients.
     * @param n the degree
     * @param m the order
     * @param normalized (true) or un-normalized (false)
     * @return the sines coefficients matrix
     * @exception OrekitException if the requested maximal degree or order exceeds the
     * available degree or order
     */
    double[][] getS(int n, int m, boolean normalized)
        throws OrekitException;

    /** Get the central body attraction coefficient.
     * @return mu (m<sup>3</sup>/s<sup>2</sup>)
     */
    double getMu();

    /** Get the value of the central body reference radius.
     * @return ae (m)
     */
    double getAe();

}
