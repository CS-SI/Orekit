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
package org.orekit.propagation.semianalytical.dsst.utilities;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.complex.Complex;

/** Compute the S<sub>j</sub>(k, h) and the C<sub>j</sub>(k, h) series
 *  and their partial derivatives with respect to k and h.
 *  <p>
 *  Those series are given in Danielson paper by expression 2.5.3-(5):
 *
 *  <p> C<sub>j</sub>(k, h) + i S<sub>j</sub>(k, h) = (k+ih)<sup>j</sup>
 *
 *  <p>
 *  The C<sub>j</sub>(k, h) and the S<sub>j</sub>(k, h) elements are store as an
 *  {@link ArrayList} of {@link Complex} number, the C<sub>j</sub>(k, h) being
 *  represented by the real and the S<sub>j</sub>(k, h) by the imaginary part.
 */
public class CjSjCoefficient {

    /** Last computed order j. */
    private int jLast;

    /** Complex base (k + ih) of the C<sub>j</sub>, S<sub>j</sub> series. */
    private final Complex kih;

    /** List of computed elements. */
    private final List<Complex> cjsj;

    /** C<sub>j</sub>(k, h) and S<sub>j</sub>(k, h) constructor.
     * @param k k value
     * @param h h value
     */
    public CjSjCoefficient(final double k, final double h) {
        kih  = new Complex(k, h);
        cjsj = new ArrayList<Complex>();
        cjsj.add(new Complex(1, 0));
        cjsj.add(kih);
        jLast = 1;
    }

    /** Get the C<sub>j</sub> coefficient.
     * @param j order
     * @return C<sub>j</sub>
     */
    public double getCj(final int j) {
        if (j > jLast) {
            // Update to order j
            updateCjSj(j);
        }
        return cjsj.get(j).getReal();
    }

    /** Get the S<sub>j</sub> coefficient.
     * @param j order
     * @return S<sub>j</sub>
     */
    public double getSj(final int j) {
        if (j > jLast) {
            // Update to order j
            updateCjSj(j);
        }
        return cjsj.get(j).getImaginary();
    }

    /** Get the dC<sub>j</sub> / dk coefficient.
     * @param j order
     * @return dC<sub>j</sub> / d<sub>k</sub>
     */
    public double getDcjDk(final int j) {
        return j == 0 ? 0 : j * getCj(j - 1);
    }

    /** Get the dS<sub>j</sub> / dk coefficient.
     * @param j order
     * @return dS<sub>j</sub> / d<sub>k</sub>
     */
    public double getDsjDk(final int j) {
        return j == 0 ? 0 : j * getSj(j - 1);
    }

    /** Get the dC<sub>j</sub> / dh coefficient.
     * @param j order
     * @return dC<sub>i</sub> / d<sub>k</sub>
     */
    public double getDcjDh(final int j) {
        return j == 0 ? 0 : -j * getSj(j - 1);
    }

    /** Get the dS<sub>j</sub> / dh coefficient.
     * @param j order
     * @return dS<sub>j</sub> / d<sub>h</sub>
     */
    public double getDsjDh(final int j) {
        return j == 0 ? 0 : j * getCj(j - 1);
    }

    /** Update the cjsj up to order j.
     * @param j order
     */
    private void updateCjSj(final int j) {
        Complex last = cjsj.get(cjsj.size() - 1);
        for (int i = jLast; i < j; i++) {
            final Complex next = last.multiply(kih);
            cjsj.add(next);
            last = next;
        }
        jLast = j;
    }
}
