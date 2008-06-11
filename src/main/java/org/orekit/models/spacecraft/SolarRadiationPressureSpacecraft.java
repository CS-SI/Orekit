/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.models.spacecraft;

import java.io.Serializable;

import org.apache.commons.math.geometry.Vector3D;

/** Adapted container for the SolarRadiationPressure force model.
 *
 * @author Fabien Maussion
 * @version $Revision$ $Date$
 */
public interface SolarRadiationPressureSpacecraft extends Serializable {

    /** Get the surface.
     * @param direction direction of the light flux in the spacecraft frame
     * @return surface (m<sup>2</sup>)
     */
    double getSurface(Vector3D direction);

    /** Get the absorption coefficients vector.
     * @param direction direction of the light flux in the spacecraft frame
     * @return absorption coefficients vector in the spacecraft frame
     */
    Vector3D getAbsCoef(Vector3D direction);

    /** Get the specular reflection coefficients vector.
     * @param direction direction of the light flux in the spacecraft frame
     * @return specular reflection coefficients vector in the spacecraft frame
     */
    Vector3D getReflectionCoef(Vector3D direction);

}
