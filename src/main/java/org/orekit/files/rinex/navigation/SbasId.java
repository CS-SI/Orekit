/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.rinex.navigation;

/** Enumerate for the SBAS ids.
 *
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum SbasId {

    /** Wide Area Augmentation System. */
    WAAS,

    /** European Geostationary Navigation Overlay Service. */
    EGNOS,

    /** Multi-functional Satellite Augmentation System. */
    MSAS,

    /** GPS Aided Geo Augmented Navigation. */
    GAGAN,

    /** System for Differential Corrections and Monitoring. */
    SDCM,

    /**  BeiDou Satellite-based Augmentation System. */
    BDSBAS,

    /** Soluciόn de Aumentaciόn para Caribe, Centro y Sudamérica. */
    SACCSA,

    /** Korea Augmentation Satellite System. */
    KASS,

    /** African Satellite-Based Augmentation System (ASECNA). */
    A_SBAS,

    /** Southern Positioning Augmentation System. */
    SPAN;

}
