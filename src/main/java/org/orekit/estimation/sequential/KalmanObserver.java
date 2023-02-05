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
package org.orekit.estimation.sequential;

/** Observer for {@link KalmanEstimator Kalman filter} estimations.
 * <p>
 * This interface is intended to be implemented by users to monitor
 * the progress of the Kalman filter estimator during estimation.
 * </p>
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 9.2
 */
public interface KalmanObserver {

    /** Notification callback after each one of a Kalman filter estimation.
     * @param estimation estimation performed by Kalman estimator
     */
    void evaluationPerformed(KalmanEstimation estimation);

}
