/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.utils;


/** Supported IERS conventions.
 * @since 6.0
 * @author Luc Maisonobe
 */
public enum IERSConventions {

    /** Constant for IERS 2003 conventions. */
    IERS_2003 {

        /** {@inheritDoc} */
        public String getXModel() {
            return IERS_BASE + "2010/tab5.2a.txt";
        }

        /** {@inheritDoc} */
        public String getYModel() {
            return IERS_BASE + "2010/tab5.2b.txt";
        }

        /** {@inheritDoc} */
        public String getSXY2XModel() {
            return IERS_BASE + "2010/tab5.2d.txt";
        }

    },

    /** Constant for IERS 2010 conventions. */
    IERS_2010 {

        /** {@inheritDoc} */
        public String getXModel() {
            return IERS_BASE + "2003/tab5.2a.txt";
        }

        /** {@inheritDoc} */
        public String getYModel() {
            return IERS_BASE + "2003/tab5.2b.txt";
        }

        /** {@inheritDoc} */
        public String getSXY2XModel() {
            return IERS_BASE + "2003/tab5.2c.txt";
        }

    };

    /** IERS conventions resources base directory. */
    private static final String IERS_BASE = "/assets/org/orekit/IERS-conventions/";

    /** Get the table resource name for the X pole component model.
     * @return table resource name for the X pole component model
     */
    public abstract String getXModel();

    /** Get the table resource name for the Y pole component model.
     * @return table resource name for the Y pole component model
     */
    public abstract String getYModel();

    /** Get the table resource name for the S + XY/2 pole component model.
     * @return table resource name for the S + XY/2 pole component model
     */
    public abstract String getSXY2XModel();

}
