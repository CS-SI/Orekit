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
package org.orekit.ssa.collision.shorttermencounter.probability.twod;

import org.orekit.files.ccsds.definitions.PocMethodType;

/**
 * This enum stores every probability of collision computing method using the short-term encounter model available in
 * Orekit.
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public enum ShortTermEncounter2DPOCMethodType {

    /**
     * Probability of collision computing method described in :"SERRA, Romain, ARZELIER, Denis, JOLDES, Mioara, et al. Fast
     * and accurate computation of orbital collision probability for short-term encounters. Journal of Guidance, Control, and
     * Dynamics, 2016, vol. 39, no 5, p. 1009-1021."
     */
    LAAS_2015 {
        /** {@inheritDoc} */
        @Override
        public ShortTermEncounter2DPOCMethod getMethod() {
            return new Laas2015();
        }

        /**
         * {@inheritDoc}
         * <p>
         * This method is currently under approval by the SANA registry so its return type shall be updated once approved.
         */
        @Override
        public PocMethodType getCCSDSType() {
            return null;
        }
    },

    /**
     * Probability of collision computing method described in :"S. Alfano. A numerical implementation of spherical objet
     * collision probability. Journal of Astronautical Sciences, 53(1), January-March 2005."
     */
    ALFANO_2005 {
        /** {@inheritDoc} */
        @Override
        public ShortTermEncounter2DPOCMethod getMethod() {
            return new Alfano2005();
        }

        /** {@inheritDoc} */
        @Override
        public PocMethodType getCCSDSType() {
            return PocMethodType.ALFANO_2005;
        }
    },

    /**
     * Probability of collision computing method described in :"PATERA, Russell P. Calculating collision probability for
     * arbitrary space vehicle shapes via numerical quadrature. Journal of guidance, control, and dynamics, 2005, vol. 28, no
     * 6, p. 1326-1328."
     */
    PATERA_2005 {
        /** {@inheritDoc} */
        @Override
        public ShortTermEncounter2DPOCMethod getMethod() {
            return new Patera2005();
        }

        /** {@inheritDoc} */
        @Override
        public PocMethodType getCCSDSType() {
            return PocMethodType.PATERA_2005;
        }
    },

    /**
     * Probability of collision computing method described in :"Kyle Alfriend, Maruthi Akella, Joseph Frisbee, James Foster,
     * Deok-Jin Lee, and Matthew Wilkins. Probability of ProbabilityOfCollision Error Analysis. Space Debris, 1(1):21–35,
     * 1999."
     */
    ALFRIEND_1999 {
        /** {@inheritDoc} */
        @Override
        public ShortTermEncounter2DPOCMethod getMethod() {
            return new Alfriend1999();
        }

        /** {@inheritDoc} */
        @Override
        public PocMethodType getCCSDSType() {
            return PocMethodType.ALFRIEND_1999;
        }
    },

    /**
     * Probability of collision computing method described in :"Kyle Alfriend, Maruthi Akella, Joseph Frisbee, James Foster,
     * Deok-Jin Lee, and Matthew Wilkins. Probability of ProbabilityOfCollision Error Analysis. Space Debris, 1(1):21–35,
     * 1999."
     */
    ALFRIEND_1999_MAX {
        /** {@inheritDoc} */
        @Override
        public ShortTermEncounter2DPOCMethod getMethod() {
            return new Alfriend1999Max();
        }

        /** {@inheritDoc} */
        @Override
        public PocMethodType getCCSDSType() {
            return null;
        }
    },

    /**
     * Probability of collision computing method described in : <br> "Chan,K. “Collision Probability Analyses for Earth
     * Orbiting Satellites.” In Space Cooperation into the 21st Century: 7th AAS/JRS/CSA Symposium, International Space
     * Conference of Pacific-Basin Societies (ISCOPS; formerly PISSTA) (July 15-18, 1997, Nagasaki, Japan), edited by Peter
     * M. Bainum, et al., 1033-1048. Advances in the Astronautical Sciences Series 96. San Diego, California: Univelt, 1997.
     * (Zeroth order analytical expression)".
     * <p>
     * This method is also described in depth in : <br> "CHAN, F. Kenneth, et al. Spacecraft collision probability. El
     * Segundo, CA : Aerospace Press, 2008."
     */
    CHAN_1997 {
        /** {@inheritDoc} */
        @Override
        public ShortTermEncounter2DPOCMethod getMethod() {
            return new Chan1997();
        }

        /** {@inheritDoc} */
        @Override
        public PocMethodType getCCSDSType() {
            return PocMethodType.CHAN_1997;
        }
    };

    /**
     * Get the CCSDS type if used by the SANA registry.
     * <p>
     * Note that it may return a null if the method is <b>not</b> used by the SANA registry.
     * <p>
     * The list of available methods is available on the SANA website.
     *
     * @return probability of collision method
     *
     * @see <a href="https://sanaregistry.org/r/cdm_cpm/">SANA CDM Collision Probability Methods</a>
     */
    public abstract PocMethodType getCCSDSType();

    /** Get probability of collision method.
     * @return probability of collision method
     */
    public abstract ShortTermEncounter2DPOCMethod getMethod();

}
