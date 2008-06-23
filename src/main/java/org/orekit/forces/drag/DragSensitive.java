/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.forces.drag;

import org.apache.commons.math.geometry.Vector3D;

/** Interface for spacecraft that are sensitive to atmospheric drag forces.
 *
 * @see org.orekit.forces.drag.DragForce
 * @author Fabien Maussion
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public interface DragSensitive {

    /** Get the visible surface from a specific direction.
     * See {@link org.orekit.forces.drag.DragForce} for more explanations.
     * @param direction direction of the flux in the spacecraft frame
     * @return surface (m<sup>2</sup>)
     */
    double getDragCrossSection(Vector3D direction);

    /** Get the drag coefficients vector.
     * See {@link org.orekit.forces.drag.DragForce} for more explanations.
     * @param direction direction of the flux in the spacecraft frame
     * @return drag coefficients vector (defined in the spacecraft frame)
     */
    Vector3D getDragCoef(Vector3D direction);

}
