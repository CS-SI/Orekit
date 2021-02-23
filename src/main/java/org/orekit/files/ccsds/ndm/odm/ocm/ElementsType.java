/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm.odm.ocm;

import org.orekit.files.ccsds.definitions.Unit;

/** Orbit element set type used in CCSDS {@link OcmFile Orbit Comprehensive Messages}.
 * @see <a href="https://sanaregistry.org/r/orbital_elements">SANA registry for orbital elements</a>
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum ElementsType {

    /** Spherical 6-element set (α,δ,β,A,r,v). */
    ADBARV("Spherical 6-element set (α,δ,β,A,r,v)",
           Unit.DEG, Unit.DEG, Unit.DEG,
           Unit.DEG, Unit.KM, Unit.KM),

    /** Cartesian 3-element position (X, Y, Z). */
    CARTP("Cartesian 3-element position (X, Y, Z)",
          Unit.KM, Unit.KM, Unit.KM),

    /** Cartesian 6-element position and velocity (X, Y, Z, XD, YD, ZD). */
    CARTPV(" Cartesian 6-element position and velocity (X, Y, Z, XD, YD, ZD)",
           Unit.KM, Unit.KM, Unit.KM,
           Unit.KM_S, Unit.KM_S, Unit.KM_S),

    /** Cartesian 9-element position, velocity and acceleration (X, Y, Z, XD, YD, ZD, XDD, YDD, ZDD). */
    CARTPVA("Cartesian 9-element position, velocity and acceleration (X, Y, Z, XD, YD, ZD, XDD, YDD, ZDD)",
            Unit.KM, Unit.KM, Unit.KM,
            Unit.KM_S, Unit.KM_S, Unit.KM_S,
            Unit.KM_S2, Unit.KM_S2, Unit.KM_S2),

    /** Delaunay elements (L, G, H, l, g, h). */
    DELAUNAY("Delaunay elements (L, G, H, l, g, h)",
             Unit.KM2_S, Unit.KM2_S, Unit.KM2_S,
             Unit.DEG, Unit.DEG, Unit.DEG),

    /** Modified Delaunay elements (Lm, Gm, Hm, lm, gm, hm). */
    DELAUNAYMOD("Delaunay elements (Lm, Gm, Hm, lm, gm, hm)",
                Unit.SQKM, Unit.SQKM, Unit.SQKM,
                Unit.DEG, Unit.DEG, Unit.DEG),

    /** 12 elements eigenvalue/eigenvectors (EigMaj, EigMed, EigMin, EigVecMaj, EigVecMed, EigVecMin). */
    EIGVAL3EIGVEC3("12 elements eigenvalue/eigenvectors (EigMaj, EigMed, EigMin, EigVecMaj, EigVecMed, EigVecMin)",
                   Unit.KM, Unit.KM, Unit.KM,
                   Unit.DIMENSIONLESS, Unit.DIMENSIONLESS, Unit.DIMENSIONLESS,
                   Unit.DIMENSIONLESS, Unit.DIMENSIONLESS, Unit.DIMENSIONLESS,
                   Unit.DIMENSIONLESS, Unit.DIMENSIONLESS, Unit.DIMENSIONLESS),

    /** Equinoctial elements (a, af, ag, L=M+ω+frΩ, χ, ψ, fr). */
    EQUINOCTIAL(" Equinoctial elements (a, af, ag, L=M+ω+frΩ, χ, ψ, fr)",
                Unit.KM, Unit.DIMENSIONLESS, Unit.DIMENSIONLESS,
                Unit.DEG, Unit.DIMENSIONLESS, Unit.DIMENSIONLESS,
                Unit.DIMENSIONLESS),

    /** Modified equinoctial elements (p=a(1−e²), af, ag, L'=υ+ω+frΩ, χ, ψ, fr). */
    EQUINOCTIALMOD("Modified equinoctial elements (p=a(1−e²), af, ag, L'=υ+ω+frΩ, χ, ψ, fr)",
                   Unit.KM, Unit.DIMENSIONLESS, Unit.DIMENSIONLESS,
                   Unit.DEG, Unit.DIMENSIONLESS, Unit.DIMENSIONLESS,
                   Unit.DIMENSIONLESS),

    /** Geodetic elements (λ, ΦGD, β, A, h, vre). */
    GEODETIC("Geodetic elements (λ, ΦGD, β, A, h, vre)",
             Unit.DEG, Unit.DEG, Unit.DEG,
             Unit.DEG, Unit.KM, Unit.KM_S),

    /** Keplerian 6-elemnt classical set (a, e, i, Ω, ω, ν). */
    KEPLERIAN("Keplerian 6-elemnt classical set (a, e, i, Ω, ω, ν)",
              Unit.KM, Unit.DIMENSIONLESS, Unit.DEG,
              Unit.DEG, Unit.DEG, Unit.DEG),

    /** Keplerian 6-elemnt classical set (a, e, i, Ω, ω, M). */
    KEPLERIANMEAN("Keplerian 6-elemnt classical set (a, e, i, Ω, ω, M)",
                  Unit.KM, Unit.DIMENSIONLESS, Unit.DEG,
                  Unit.DEG, Unit.DEG, Unit.DEG),

    /** Modified spherical 6-element set (λ, δ, β, A, r, v). */
    LDBARV(" Modified spherical 6-element set (λ, δ, β, A, r, v)",
           Unit.DEG, Unit.DEG, Unit.DEG,
           Unit.DEG, Unit.KM, Unit.KM_S),

    /** Geosynchronous on-station tailored set (a, ex, ey, ix, iy, λ). */
    ONSTATION("Geosynchronous on-station tailored set (a, ex, ey, ix, iy, λ)",
              Unit.KM, Unit.DIMENSIONLESS, Unit.DIMENSIONLESS,
              Unit.DIMENSIONLESS, Unit.DIMENSIONLESS, Unit.DEG),

    /** Canonical counterpart of equinoctial 6-element set (λM=M+ω+Ω, gp, hp, Lp, Gp, Hp). */
    POINCARE("Canonical counterpart of equinoctial 6-element set (λM=M+ω+Ω, gp, hp, Lp, Gp, Hp)",
             Unit.DEG, Unit.KM_SQS, Unit.KM_SQS,
             Unit.KM2_S, Unit.KM_SQS, Unit.KM_SQS);

    /** Description. */
    private final String description;

    /** Elements units. */
    private final Unit[] units;

    /** Simple constructor.
     * @param description description
     * @param units elements units
     */
    ElementsType(final String description, final Unit... units) {
        this.description = description;
        this.units  = units.clone();
    }

    /** Get the elements units.
     * @return elements units
     */
    public Unit[] getUnits() {
        return units.clone();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return description;
    }

}
