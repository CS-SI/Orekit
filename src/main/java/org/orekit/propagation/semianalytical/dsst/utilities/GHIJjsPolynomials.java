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


/** Compute the G<sub>js</sub>, H<sub>js</sub>, I<sub>js</sub> and J<sub>js</sub>
 *  polynomials in the equinoctial elements h, k and the direction cosines α and β
 *  and their partial derivatives with respect to k, h, α and β.
 *  <p>
 *  The expressions used are equations 4.1-(10) from the Danielson paper.
 *  </p>
 *  @author Lucian Barbulescu
 */
public class GHIJjsPolynomials {

    /** C<sub>j</sub>(k, h), S<sub>j</sub>(k, h) coefficient.
     * (k, h) are the (x, y) component of the eccentricity vector in equinoctial elements
     */
    private final CjSjCoefficient cjsjKH;

    /** C<sub>j</sub>(α, β), S<sub>j</sub>(α, β) coefficient.
     * (α, β) are the direction cosines
     */
    private final CjSjCoefficient cjsjAB;

    /** Create a set of G<sub>js</sub>, H<sub>js</sub>, I<sub>js</sub> and J<sub>js</sub> polynomials.
     *  @param k X component of the eccentricity vector
     *  @param h Y component of the eccentricity vector
     *  @param alpha direction cosine α
     *  @param beta direction cosine β
     **/
    public GHIJjsPolynomials(final double k, final double h,
                            final double alpha, final double beta) {
        this.cjsjKH = new CjSjCoefficient(k, h);
        this.cjsjAB = new CjSjCoefficient(alpha, beta);
    }

    /** Get the G<sub>js</sub> coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the G<sub>js</sub>
     */
    public double getGjs(final int j, final int s) {
        return cjsjKH.getCj(j) * cjsjAB.getCj(s) + cjsjKH.getSj(j) * cjsjAB.getSj(s);
    }

    /** Get the dG<sub>js</sub> / dk coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the dG<sub>js</sub> / dk
     */
    public double getdGjsdk(final int j, final int s) {
        return cjsjKH.getDcjDk(j) * cjsjAB.getCj(s) + cjsjKH.getDsjDk(j) * cjsjAB.getSj(s);
    }

    /** Get the dG<sub>js</sub> / dh coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the dG<sub>js</sub> / dh
     */
    public double getdGjsdh(final int j, final int s) {
        return cjsjKH.getDcjDh(j) * cjsjAB.getCj(s) + cjsjKH.getDsjDh(j) * cjsjAB.getSj(s);
    }

    /** Get the dG<sub>js</sub> / dα coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the dG<sub>js</sub> / dα
     */
    public double getdGjsdAlpha(final int j, final int s) {
        return cjsjKH.getCj(j) * cjsjAB.getDcjDk(s) + cjsjKH.getSj(j) * cjsjAB.getDsjDk(s);
    }

    /** Get the dG<sub>js</sub> / dβ coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the dG<sub>js</sub> / dβ
     */
    public double getdGjsdBeta(final int j, final int s) {
        return cjsjKH.getCj(j) * cjsjAB.getDcjDh(s) + cjsjKH.getSj(j) * cjsjAB.getDsjDh(s);
    }

    /** Get the H<sub>js</sub> coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the H<sub>js</sub>
     */
    public double getHjs(final int j, final int s) {
        return cjsjKH.getCj(j) * cjsjAB.getSj(s) - cjsjKH.getSj(j) * cjsjAB.getCj(s);
    }

    /** Get the dH<sub>js</sub> / dk coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the H<sub>js</sub> / dk
     */
    public double getdHjsdk(final int j, final int s) {
        return cjsjKH.getDcjDk(j) * cjsjAB.getSj(s) - cjsjKH.getDsjDk(j) * cjsjAB.getCj(s);
    }

    /** Get the dH<sub>js</sub> / dh coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the H<sub>js</sub> / dh
     */
    public double getdHjsdh(final int j, final int s) {
        return cjsjKH.getDcjDh(j) * cjsjAB.getSj(s) - cjsjKH.getDsjDh(j) * cjsjAB.getCj(s);
    }

    /** Get the dH<sub>js</sub> / dα coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the H<sub>js</sub> / dα
     */
    public double getdHjsdAlpha(final int j, final int s) {
        return cjsjKH.getCj(j) * cjsjAB.getDsjDk(s) - cjsjKH.getSj(j) * cjsjAB.getDcjDk(s);
    }

    /** Get the dH<sub>js</sub> / dβ coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the H<sub>js</sub> / dβ
     */
    public double getdHjsdBeta(final int j, final int s) {
        return cjsjKH.getCj(j) * cjsjAB.getDsjDh(s) - cjsjKH.getSj(j) * cjsjAB.getDcjDh(s);
    }

    /** Get the I<sub>js</sub> coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the I<sub>js</sub>
     */
    public double getIjs(final int j, final int s) {
        return cjsjKH.getCj(j) * cjsjAB.getSj(s) + cjsjKH.getSj(j) * cjsjAB.getCj(s);
    }

    /** Get the dI<sub>js</sub> / dk coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the I<sub>js</sub> / dk
     */
    public double getdIjsdk(final int j, final int s) {
        return cjsjKH.getDcjDk(j) * cjsjAB.getSj(s) + cjsjKH.getDsjDk(j) * cjsjAB.getCj(s);
    }

    /** Get the dI<sub>js</sub> / dh coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the I<sub>js</sub> / dh
     */
    public double getdIjsdh(final int j, final int s) {
        return cjsjKH.getDcjDh(j) * cjsjAB.getSj(s) + cjsjKH.getDsjDh(j) * cjsjAB.getCj(s);
    }

    /** Get the dI<sub>js</sub> / dα coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the I<sub>js</sub> / dα
     */
    public double getdIjsdAlpha(final int j, final int s) {
        return cjsjKH.getCj(j) * cjsjAB.getDsjDk(s) + cjsjKH.getSj(j) * cjsjAB.getDcjDk(s);
    }

    /** Get the dI<sub>js</sub> / dβ coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the I<sub>js</sub> / dβ
     */
    public double getdIjsdBeta(final int j, final int s) {
        return cjsjKH.getCj(j) * cjsjAB.getDsjDh(s) + cjsjKH.getSj(j) * cjsjAB.getDcjDh(s);
    }

    /** Get the J<sub>js</sub> coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the J<sub>js</sub>
     */
    public double getJjs(final int j, final int s) {
        return cjsjKH.getCj(j) * cjsjAB.getCj(s) - cjsjKH.getSj(j) * cjsjAB.getSj(s);
    }

    /** Get the dJ<sub>js</sub> / dk coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the J<sub>js</sub> / dk
     */
    public double getdJjsdk(final int j, final int s) {
        return cjsjKH.getDcjDk(j) * cjsjAB.getCj(s) - cjsjKH.getDsjDk(j) * cjsjAB.getSj(s);
    }
    /** Get the dJ<sub>js</sub> / dh coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the J<sub>js</sub> / dh
     */
    public double getdJjsdh(final int j, final int s) {
        return cjsjKH.getDcjDh(j) * cjsjAB.getCj(s) - cjsjKH.getDsjDh(j) * cjsjAB.getSj(s);
    }
    /** Get the dJ<sub>js</sub> / dα coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the J<sub>js</sub> / dα
     */
    public double getdJjsdAlpha(final int j, final int s) {
        return cjsjKH.getCj(j) * cjsjAB.getDcjDk(s) - cjsjKH.getSj(j) * cjsjAB.getDsjDk(s);
    }
    /** Get the dJ<sub>js</sub> / dβ coefficient.
     * @param j j subscript
     * @param s s subscript
     * @return the J<sub>js</sub> / dβ
     */
    public double getdJjsdBeta(final int j, final int s) {
        return cjsjKH.getCj(j) * cjsjAB.getDcjDh(s) - cjsjKH.getSj(j) * cjsjAB.getDsjDh(s);
    }
}
