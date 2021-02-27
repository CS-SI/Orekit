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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Orbit element set type used in CCSDS {@link OcmFile Orbit Comprehensive Messages}.
 * @see <a href="https://sanaregistry.org/r/orbital_elements">SANA registry for orbital elements</a>
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum ElementsType {

    /** Spherical 6-element set (α,δ,β,A,r,v). */
    ADBARV("Spherical 6-element set (α,δ,β,A,r,v)",
           ElementsUnit.DEG, ElementsUnit.DEG, ElementsUnit.DEG,
           ElementsUnit.DEG, ElementsUnit.KM, ElementsUnit.KM),

    /** Cartesian 3-element position (X, Y, Z). */
    CARTP("Cartesian 3-element position (X, Y, Z)",
          ElementsUnit.KM, ElementsUnit.KM, ElementsUnit.KM) {
        /** {@inheritDoc} */
        @Override
        public TimeStampedPVCoordinates toCartesian(final AbsoluteDate date, final double[] elements, final double mu) {
            return new TimeStampedPVCoordinates(date,
                                                new Vector3D(elements[0], elements[1], elements[2]),
                                                Vector3D.ZERO,
                                                Vector3D.ZERO);
        }
    },

    /** Cartesian 6-element position and velocity (X, Y, Z, XD, YD, ZD). */
    CARTPV(" Cartesian 6-element position and velocity (X, Y, Z, XD, YD, ZD)",
           ElementsUnit.KM, ElementsUnit.KM, ElementsUnit.KM,
           ElementsUnit.KM_S, ElementsUnit.KM_S, ElementsUnit.KM_S) {
        /** {@inheritDoc} */
        @Override
        public TimeStampedPVCoordinates toCartesian(final AbsoluteDate date, final double[] elements, final double mu) {
            return new TimeStampedPVCoordinates(date,
                                                new Vector3D(elements[0], elements[1], elements[2]),
                                                new Vector3D(elements[3], elements[4], elements[5]),
                                                Vector3D.ZERO);
        }
    },

    /** Cartesian 9-element position, velocity and acceleration (X, Y, Z, XD, YD, ZD, XDD, YDD, ZDD). */
    CARTPVA("Cartesian 9-element position, velocity and acceleration (X, Y, Z, XD, YD, ZD, XDD, YDD, ZDD)",
            ElementsUnit.KM, ElementsUnit.KM, ElementsUnit.KM,
            ElementsUnit.KM_S, ElementsUnit.KM_S, ElementsUnit.KM_S,
            ElementsUnit.KM_S2, ElementsUnit.KM_S2, ElementsUnit.KM_S2) {
        /** {@inheritDoc} */
        @Override
        public TimeStampedPVCoordinates toCartesian(final AbsoluteDate date, final double[] elements, final double mu) {
            return new TimeStampedPVCoordinates(date,
                                                new Vector3D(elements[0], elements[1], elements[2]),
                                                new Vector3D(elements[3], elements[4], elements[5]),
                                                new Vector3D(elements[6], elements[7], elements[8]));
        }
    },

    /** Delaunay elements (L, G, H, l, g, h). */
    DELAUNAY("Delaunay elements (L, G, H, l, g, h)",
             ElementsUnit.KM2_S, ElementsUnit.KM2_S, ElementsUnit.KM2_S,
             ElementsUnit.DEG, ElementsUnit.DEG, ElementsUnit.DEG),

    /** Modified Delaunay elements (Lm, Gm, Hm, lm, gm, hm). */
    DELAUNAYMOD("Delaunay elements (Lm, Gm, Hm, lm, gm, hm)",
                ElementsUnit.SQKM, ElementsUnit.SQKM, ElementsUnit.SQKM,
                ElementsUnit.DEG, ElementsUnit.DEG, ElementsUnit.DEG),

    /** 12 elements eigenvalue/eigenvectors (EigMaj, EigMed, EigMin, EigVecMaj, EigVecMed, EigVecMin). */
    EIGVAL3EIGVEC3("12 elements eigenvalue/eigenvectors (EigMaj, EigMed, EigMin, EigVecMaj, EigVecMed, EigVecMin)",
                   ElementsUnit.KM, ElementsUnit.KM, ElementsUnit.KM,
                   ElementsUnit.ND, ElementsUnit.ND, ElementsUnit.ND,
                   ElementsUnit.ND, ElementsUnit.ND, ElementsUnit.ND,
                   ElementsUnit.ND, ElementsUnit.ND, ElementsUnit.ND),

    /** Equinoctial elements (a, af, ag, L=M+ω+frΩ, χ, ψ, fr). */
    EQUINOCTIAL(" Equinoctial elements (a, af, ag, L=M+ω+frΩ, χ, ψ, fr)",
                ElementsUnit.KM, ElementsUnit.ND, ElementsUnit.ND,
                ElementsUnit.DEG, ElementsUnit.ND, ElementsUnit.ND,
                ElementsUnit.ND),

    /** Modified equinoctial elements (p=a(1−e²), af, ag, L'=υ+ω+frΩ, χ, ψ, fr). */
    EQUINOCTIALMOD("Modified equinoctial elements (p=a(1−e²), af, ag, L'=υ+ω+frΩ, χ, ψ, fr)",
                   ElementsUnit.KM, ElementsUnit.ND, ElementsUnit.ND,
                   ElementsUnit.DEG, ElementsUnit.ND, ElementsUnit.ND,
                   ElementsUnit.ND),

    /** Geodetic elements (λ, ΦGD, β, A, h, vre). */
    GEODETIC("Geodetic elements (λ, ΦGD, β, A, h, vre)",
             ElementsUnit.DEG, ElementsUnit.DEG, ElementsUnit.DEG,
             ElementsUnit.DEG, ElementsUnit.KM, ElementsUnit.KM_S),

    /** Keplerian 6-elemnt classical set (a, e, i, Ω, ω, ν). */
    KEPLERIAN("Keplerian 6-elemnt classical set (a, e, i, Ω, ω, ν)",
              ElementsUnit.KM, ElementsUnit.ND, ElementsUnit.DEG,
              ElementsUnit.DEG, ElementsUnit.DEG, ElementsUnit.DEG),

    /** Keplerian 6-elemnt classical set (a, e, i, Ω, ω, M). */
    KEPLERIANMEAN("Keplerian 6-elemnt classical set (a, e, i, Ω, ω, M)",
                  ElementsUnit.KM, ElementsUnit.ND, ElementsUnit.DEG,
                  ElementsUnit.DEG, ElementsUnit.DEG, ElementsUnit.DEG),

    /** Modified spherical 6-element set (λ, δ, β, A, r, v). */
    LDBARV(" Modified spherical 6-element set (λ, δ, β, A, r, v)",
           ElementsUnit.DEG, ElementsUnit.DEG, ElementsUnit.DEG,
           ElementsUnit.DEG, ElementsUnit.KM, ElementsUnit.KM_S),

    /** Geosynchronous on-station tailored set (a, ex, ey, ix, iy, λ). */
    ONSTATION("Geosynchronous on-station tailored set (a, ex, ey, ix, iy, λ)",
              ElementsUnit.KM, ElementsUnit.ND, ElementsUnit.ND,
              ElementsUnit.ND, ElementsUnit.ND, ElementsUnit.DEG),

    /** Canonical counterpart of equinoctial 6-element set (λM=M+ω+Ω, gp, hp, Lp, Gp, Hp). */
    POINCARE("Canonical counterpart of equinoctial 6-element set (λM=M+ω+Ω, gp, hp, Lp, Gp, Hp)",
             ElementsUnit.DEG, ElementsUnit.KM_SQS, ElementsUnit.KM_SQS,
             ElementsUnit.KM2_S, ElementsUnit.KM_SQS, ElementsUnit.KM_SQS);

    /** Description. */
    private final String description;

    /** Elements units. */
    private final ElementsUnit[] units;

    /** Simple constructor.
     * @param description description
     * @param units elements units
     */
    ElementsType(final String description, final ElementsUnit... units) {
        this.description = description;
        this.units  = units.clone();
    }

    /** Get the reference units.
     * @return reference units
     */
    public ElementsUnit[] getUnits() {
        return units.clone();
    }

    /** Convert to Cartesian coordinates.
     * @param date elements date
     * @param elements elements values
     * @param mu gravitational parameter in m³/s²
     * @return Cartesian coordinates
     */
    public TimeStampedPVCoordinates toCartesian(final AbsoluteDate date, final double[] elements, final double mu) {
        throw new OrekitException(OrekitMessages.UNSUPPORTED_ELEMENT_TYPE, name());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return description;
    }

}
