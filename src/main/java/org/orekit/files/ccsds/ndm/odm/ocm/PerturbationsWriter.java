/* Copyright 2002-2021 CS GROUP
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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/** Writer for perturbations parameters data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class PerturbationsWriter extends AbstractWriter {

    /** Perturbation parameters block. */
    private final Perturbations perturbations;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param perturbations perturbations parameters to write
     * @param timeConverter converter for dates
     */
    PerturbationsWriter(final Perturbations perturbations, final TimeConverter timeConverter) {
        super(OcmDataSubStructureKey.pert.name(), OcmDataSubStructureKey.PERT.name());
        this.perturbations = perturbations;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // perturbations parameters block
        generator.writeComments(perturbations.getComments());

        // atmosphere
        generator.writeEntry(PerturbationsKey.ATMOSPHERIC_MODEL.name(), perturbations.getAtmosphericModel(), false);

        // gravity
        if (perturbations.getGravityModel() != null) {
            final String model =
                            new StringBuilder().
                            append(perturbations.getGravityModel()).
                            append(": ").
                            append(perturbations.getGravityDegree()).
                            append("D ").
                            append(perturbations.getGravityOrder()).
                            append('O').
                            toString();
            generator.writeEntry(PerturbationsKey.GRAVITY_MODEL.name(), model, false);
        }
        generator.writeEntry(PerturbationsKey.EQUATORIAL_RADIUS.name(),    Unit.KILOMETRE.fromSI(perturbations.getEquatorialRadius()), false);
        generator.writeEntry(PerturbationsKey.GM.name(),                   Units.KM3_PER_S2.fromSI(perturbations.getGm()),             false);
        if (perturbations.getNBodyPerturbations() != null && !perturbations.getNBodyPerturbations().isEmpty()) {
            final List<String> names = new ArrayList<>();
            for (BodyFacade bf : perturbations.getNBodyPerturbations()) {
                names.add(bf.getName());
            }
            generator.writeEntry(PerturbationsKey.N_BODY_PERTURBATIONS.name(), names, false);
        }
        generator.writeEntry(PerturbationsKey.CENTRAL_BODY_ROTATION.name(), Units.DEG_PER_S.fromSI(perturbations.getCentralBodyRotation()), false);
        generator.writeEntry(PerturbationsKey.OBLATE_FLATTENING.name(),     Unit.ONE.fromSI(perturbations.getOblateFlattening()),           false);
        generator.writeEntry(PerturbationsKey.OCEAN_TIDES_MODEL.name(),     perturbations.getOceanTidesModel(),                             false);
        generator.writeEntry(PerturbationsKey.SOLID_TIDES_MODEL.name(),     perturbations.getSolidTidesModel(),                             false);
        generator.writeEntry(PerturbationsKey.REDUCTION_THEORY.name(),      perturbations.getReductionTheory(),                             false);

        // radiation
        generator.writeEntry(PerturbationsKey.ALBEDO_MODEL.name(),      perturbations.getAlbedoModel(),    false);
        generator.writeEntry(PerturbationsKey.ALBEDO_GRID_SIZE.name(),  perturbations.getAlbedoGridSize(), false);
        generator.writeEntry(PerturbationsKey.SHADOW_MODEL.name(),      perturbations.getShadowModel(),    false);
        if (perturbations.getShadowBodies() != null && !perturbations.getShadowBodies().isEmpty()) {
            final List<String> names = new ArrayList<>();
            for (BodyFacade bf : perturbations.getShadowBodies()) {
                names.add(bf.getName());
            }
            generator.writeEntry(PerturbationsKey.SHADOW_BODIES.name(), names, false);
        }
        generator.writeEntry(PerturbationsKey.SRP_MODEL.name(),          perturbations.getSrpModel(), false);

        // data source
        generator.writeEntry(PerturbationsKey.SW_DATA_SOURCE.name(),    perturbations.getSpaceWeatherSource(),                           false);
        generator.writeEntry(PerturbationsKey.SW_DATA_EPOCH.name(),     timeConverter, perturbations.getSpaceWeatherEpoch(),             false);
        generator.writeEntry(PerturbationsKey.SW_INTERP_METHOD.name(),  perturbations.getInterpMethodSW(),                               false);
        generator.writeEntry(PerturbationsKey.FIXED_GEOMAG_KP.name(),   Units.NANO_TESLA.fromSI(perturbations.getFixedGeomagneticKp()),  false);
        generator.writeEntry(PerturbationsKey.FIXED_GEOMAG_AP.name(),   Units.NANO_TESLA.fromSI(perturbations.getFixedGeomagneticAp()),  false);
        generator.writeEntry(PerturbationsKey.FIXED_GEOMAG_DST.name(),  Units.NANO_TESLA.fromSI(perturbations.getFixedGeomagneticDst()), false);
        generator.writeEntry(PerturbationsKey.FIXED_F10P7.name(),       Unit.SOLAR_FLUX_UNIT.fromSI(perturbations.getFixedF10P7()),      false);
        generator.writeEntry(PerturbationsKey.FIXED_F10P7_MEAN.name(),  Unit.SOLAR_FLUX_UNIT.fromSI(perturbations.getFixedF10P7Mean()),  false);
        generator.writeEntry(PerturbationsKey.FIXED_M10P7.name(),       Unit.SOLAR_FLUX_UNIT.fromSI(perturbations.getFixedM10P7()),      false);
        generator.writeEntry(PerturbationsKey.FIXED_M10P7_MEAN.name(),  Unit.SOLAR_FLUX_UNIT.fromSI(perturbations.getFixedM10P7Mean()),  false);
        generator.writeEntry(PerturbationsKey.FIXED_S10P7.name(),       Unit.SOLAR_FLUX_UNIT.fromSI(perturbations.getFixedS10P7()),      false);
        generator.writeEntry(PerturbationsKey.FIXED_S10P7_MEAN.name(),  Unit.SOLAR_FLUX_UNIT.fromSI(perturbations.getFixedS10P7Mean()),  false);
        generator.writeEntry(PerturbationsKey.FIXED_Y10P7.name(),       Unit.SOLAR_FLUX_UNIT.fromSI(perturbations.getFixedY10P7()),      false);
        generator.writeEntry(PerturbationsKey.FIXED_Y10P7_MEAN.name(),  Unit.SOLAR_FLUX_UNIT.fromSI(perturbations.getFixedY10P7Mean()),  false);

    }

}
