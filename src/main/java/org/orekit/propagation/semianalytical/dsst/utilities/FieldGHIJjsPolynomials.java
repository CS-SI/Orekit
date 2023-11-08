/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;

/** Compute the G<sub>js</sub>, H<sub>js</sub>, I<sub>js</sub> and J<sub>js</sub>
 *  polynomials in the equinoctial elements h, k and the direction cosines α and β
 *  and their partial derivatives with respect to k, h, α and β.
 *  <p>
 *  The expressions used are equations 4.1-(10) from the Danielson paper.
 *  </p>
 *  @author Lucian Barbulescu
 *  @author Bryan Cazabonne (field translation)
 * @param <T> type of the field elements
 */
public class FieldGHIJjsPolynomials<T extends CalculusFieldElement<T>> {

    /** C<sub>j</sub>(k, h), S<sub>j</sub>(k, h) coefficient.
     * (k, h) are the (x, y) component of the eccentricity vector in equinoctial elements
     */
    private final FieldCjSjCoefficient<T> cjsjKH;

    /** C<sub>j</sub>(α, β), S<sub>j</sub>(α, β) coefficient.
     * (α, β) are the direction cosines
     */
    private final FieldCjSjCoefficient<T> cjsjAB;

    /** Create a set of G<sub>js</sub>, H<sub>js</sub>, I<sub>js</sub> and J<sub>js</sub> polynomials.
     *  @param k X component of the eccentricity vector
     *  @param h Y component of the eccentricity vector
     *  @param alpha direction cosine α
     *  @param beta direction cosine β
     **/
    public FieldGHIJjsPolynomials(final T k, final T h,
                                  final T alpha, final T beta) {
        final Field<T> field = k.getField();
        this.cjsjKH = new FieldCjSjCoefficient<>(k, h, field);
        this.cjsjAB = new FieldCjSjCoefficient<>(alpha, beta, field);
    }

    /** Get the G<sub>js</sub> coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the G<sub>js</sub>
     */
    public T getGjs(final int j, final int s) {
        return cjsjKH.getCj(j).multiply(cjsjAB.getCj(s)).add(cjsjKH.getSj(j).multiply(cjsjAB.getSj(s)));
    }

    /** Get the dG<sub>js</sub> / dk coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the dG<sub>js</sub> / dk
     */
    public T getdGjsdk(final int j, final int s) {
        return cjsjKH.getDcjDk(j).multiply(cjsjAB.getCj(s)).add(cjsjKH.getDsjDk(j).multiply(cjsjAB.getSj(s)));
    }

    /** Get the dG<sub>js</sub> / dh coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the dG<sub>js</sub> / dh
     */
    public T getdGjsdh(final int j, final int s) {
        return cjsjKH.getDcjDh(j).multiply(cjsjAB.getCj(s)).add(cjsjKH.getDsjDh(j).multiply(cjsjAB.getSj(s)));
    }

    /** Get the dG<sub>js</sub> / dα coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the dG<sub>js</sub> / dα
     */
    public T getdGjsdAlpha(final int j, final int s) {
        return cjsjKH.getCj(j).multiply(cjsjAB.getDcjDk(s)).add(cjsjKH.getSj(j).multiply(cjsjAB.getDsjDk(s)));
    }

    /** Get the dG<sub>js</sub> / dβ coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the dG<sub>js</sub> / dβ
     */
    public T getdGjsdBeta(final int j, final int s) {
        return cjsjKH.getCj(j).multiply(cjsjAB.getDcjDh(s)).add(cjsjKH.getSj(j).multiply(cjsjAB.getDsjDh(s)));
    }

    /** Get the H<sub>js</sub> coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the H<sub>js</sub>
     */
    public T getHjs(final int j, final int s) {
        return cjsjKH.getCj(j).multiply(cjsjAB.getSj(s)).subtract(cjsjKH.getSj(j).multiply(cjsjAB.getCj(s)));
    }

    /** Get the dH<sub>js</sub> / dk coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the H<sub>js</sub> / dk
     */
    public T getdHjsdk(final int j, final int s) {
        return cjsjKH.getDcjDk(j).multiply(cjsjAB.getSj(s)).subtract(cjsjKH.getDsjDk(j).multiply(cjsjAB.getCj(s)));
    }

    /** Get the dH<sub>js</sub> / dh coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the H<sub>js</sub> / dh
     */
    public T getdHjsdh(final int j, final int s) {
        return cjsjKH.getDcjDh(j).multiply(cjsjAB.getSj(s)).subtract(cjsjKH.getDsjDh(j).multiply(cjsjAB.getCj(s)));
    }

    /** Get the dH<sub>js</sub> / dα coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the H<sub>js</sub> / dα
     */
    public T getdHjsdAlpha(final int j, final int s) {
        return cjsjKH.getCj(j).multiply(cjsjAB.getDsjDk(s)).subtract(cjsjKH.getSj(j).multiply(cjsjAB.getDcjDk(s)));
    }

    /** Get the dH<sub>js</sub> / dβ coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the H<sub>js</sub> / dβ
     */
    public T getdHjsdBeta(final int j, final int s) {
        return cjsjKH.getCj(j).multiply(cjsjAB.getDsjDh(s)).subtract(cjsjKH.getSj(j).multiply(cjsjAB.getDcjDh(s)));
    }

    /** Get the I<sub>js</sub> coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the I<sub>js</sub>
     */
    public T getIjs(final int j, final int s) {
        return cjsjKH.getCj(j).multiply(cjsjAB.getSj(s)).add(cjsjKH.getSj(j).multiply(cjsjAB.getCj(s)));
    }

    /** Get the dI<sub>js</sub> / dk coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the I<sub>js</sub> / dk
     */
    public T getdIjsdk(final int j, final int s) {
        return cjsjKH.getDcjDk(j).multiply(cjsjAB.getSj(s)).add(cjsjKH.getDsjDk(j).multiply(cjsjAB.getCj(s)));
    }

    /** Get the dI<sub>js</sub> / dh coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the I<sub>js</sub> / dh
     */
    public T getdIjsdh(final int j, final int s) {
        return cjsjKH.getDcjDh(j).multiply(cjsjAB.getSj(s)).add(cjsjKH.getDsjDh(j).multiply(cjsjAB.getCj(s)));
    }

    /** Get the dI<sub>js</sub> / dα coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the I<sub>js</sub> / dα
     */
    public T getdIjsdAlpha(final int j, final int s) {
        return cjsjKH.getCj(j).multiply(cjsjAB.getDsjDk(s)).add(cjsjKH.getSj(j).multiply(cjsjAB.getDcjDk(s)));
    }

    /** Get the dI<sub>js</sub> / dβ coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the I<sub>js</sub> / dβ
     */
    public T getdIjsdBeta(final int j, final int s) {
        return cjsjKH.getCj(j).multiply(cjsjAB.getDsjDh(s)).add(cjsjKH.getSj(j).multiply(cjsjAB.getDcjDh(s)));
    }

    /** Get the J<sub>js</sub> coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the J<sub>js</sub>
     */
    public T getJjs(final int j, final int s) {
        return cjsjKH.getCj(j).multiply(cjsjAB.getCj(s)).subtract(cjsjKH.getSj(j).multiply(cjsjAB.getSj(s)));
    }

    /** Get the dJ<sub>js</sub> / dk coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the J<sub>js</sub> / dk
     */
    public T getdJjsdk(final int j, final int s) {
        return cjsjKH.getDcjDk(j).multiply(cjsjAB.getCj(s)).subtract(cjsjKH.getDsjDk(j).multiply(cjsjAB.getSj(s)));
    }
    /** Get the dJ<sub>js</sub> / dh coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the J<sub>js</sub> / dh
     */
    public T getdJjsdh(final int j, final int s) {
        return cjsjKH.getDcjDh(j).multiply(cjsjAB.getCj(s)).subtract(cjsjKH.getDsjDh(j).multiply(cjsjAB.getSj(s)));
    }
    /** Get the dJ<sub>js</sub> / dα coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the J<sub>js</sub> / dα
     */
    public T getdJjsdAlpha(final int j, final int s) {
        return cjsjKH.getCj(j).multiply(cjsjAB.getDcjDk(s)).subtract(cjsjKH.getSj(j).multiply(cjsjAB.getDsjDk(s)));
    }
    /** Get the dJ<sub>js</sub> / dβ coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the J<sub>js</sub> / dβ
     */
    public T getdJjsdBeta(final int j, final int s) {
        return cjsjKH.getCj(j).multiply(cjsjAB.getDcjDh(s)).subtract(cjsjKH.getSj(j).multiply(cjsjAB.getDsjDh(s)));
    }

}
