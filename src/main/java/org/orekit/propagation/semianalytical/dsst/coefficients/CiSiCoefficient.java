/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.coefficients;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.complex.Complex;

/**
 * Compute the S<sub>i</sub>(k, h) and the C<sub>i</sub>(k, h) series and their partial derivatives
 * with respect to <i>k, h</i>. Those series are used in the DSST theory and given by expression
 * 2.5.3-(5)(6) of the Danielson paper (Semi-Analytic Satellite Theory). The expression used here is
 * equation (5):
 *
 * <pre>
 * C<sub>j</sub>(k, h) + i S<sub>j</sub>(k, h) = (k+ih)<sup>j</sup>
 * </pre>
 *
 * The C<sub>i</sub>(k, h) and the S<sub>i</sub>(k, h) elements are store as an {@link ArrayList} of
 * {@link Complex} number, the C<sub>i</sub>(k, h) being represented by its real part and the
 * S<sub>i</sub>(k, h) by the imaginary part.
 *
 * @author Romain Di Costanzo
 */
public class CiSiCoefficient {

    /** true if no previous computation has been done. */
    private boolean       isDirty;

    /** Last computation done. */
    private int           last;

    /** List of computed elements. */
    private final List<Complex> CjSj;

    /** Is the x-component of the S<sub>i</sub>, C<sub>i</sub> series. */
    private final double  x;

    /** Is the y-component of the S<sub>i</sub>, C<sub>i</sub> series. */
    private final double  y;

    /** S<sub>i</sub>(k, h) and C<sub>i</sub>(k, h) constructor.
     * @param x x value
     * @param y y value
     */
    public CiSiCoefficient(final double x, final double y) {
        this.x       = x;
        this.y       = y;
        this.isDirty = true;
        this.CjSj    = new ArrayList<Complex>();
    }

    /** Get the C<sub>i</sub> coefficient.
     * @param i order
     * @return C<sub>i</sub>
     */
    public double getCi(final int i) {
        if (isDirty) {
            // First computation
            initializeCjSi(i);
        } else if (i > last) {
            // Update needed order from previous computation
            updateCjSi(i);
        }
        return CjSj.get(i).getReal();
    }

    /** Get the S<sub>i</sub> coefficient.
     * @param i order
     * @return S<sub>i</sub>
     */
    public double getSi(final int i) {
        if (isDirty) {
            // First computation
            initializeCjSi(i);
        } else if (i > last) {
            // Update needed order from previous computation
            updateCjSi(i);
        }
        return CjSj.get(i).getImaginary();
    }

    /** Get the dC<sub>i</sub> / dk coefficient.
     * @param i order
     * @return dC<sub>i</sub> / d<sub>k</sub>
     */
    public double getDciDk(final int i) {
        return i == 0 ? 0 : i * getCi(i - 1);
    }

    /** Get the dS<sub>i</sub> / dk coefficient.
     * @param i order
     * @return dS<sub>i</sub> / d<sub>k</sub>
     */
    public double getDsiDk(final int i) {
        return i == 0 ? 0 : i * getSi(i - 1);
    }

    /** Get the dC<sub>i</sub> / dh coefficient.
     * @param i order
     * @return dC<sub>i</sub> / d<sub>k</sub>
     */
    public double getDciDh(final int i) {
        return i == 0 ? 0 : -i * getSi(i - 1);
    }

    /** Get the dS<sub>i</sub> / dh coefficient.
     * @param i order
     * @return dS<sub>i</sub> / d<sub>h</sub>
     */
    public double getDsiDh(final int i) {
        return i == 0 ? 0 : i * getCi(i - 1);
    }

    /** Update the CjSo at wanted order from previous computation.
     * @param i final order to reach
     */
    private void updateCjSi(final int i) {
        Complex previous = CjSj.get(CjSj.size() - 1);
        for (int j = last + 1; j <= i; j++) {
            final Complex next = previous.multiply(new Complex(x, y));
            CjSj.add(next);
            previous = next;
            last = j;
        }
    }

    /** Initialize the computation at wanted order i.
     * @param i wanted order
     */
    private void initializeCjSi(final int i) {
        // Initialization
        Complex previous = new Complex(1, 0);
        CjSj.add(previous);

        // First computation :
        for (int j = 1; j <= i; j++) {
            final Complex next = previous.multiply(new Complex(x, y));
            CjSj.add(next);
            previous = next;
        }
        isDirty = false;
    }

}
