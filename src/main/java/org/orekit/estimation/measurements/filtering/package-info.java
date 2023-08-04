/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * This package provides measurement pre-processing filters.
 *
 * <p>
 * Measurement pre-processing filters are used to exclude
 * measurements before they are used during an orbit determination
 * process. Example of pre-processing filters are: </p>
 * <ul>
 *     <li>Minimum satellite elevation</li>
 *     <li>Minimum signal to noise ratio</li>
 *     <li>...</li>
 * </ul>
 * @author Bryan Cazabonne
 * @author David Soulard
 */
package org.orekit.estimation.measurements.filtering;
