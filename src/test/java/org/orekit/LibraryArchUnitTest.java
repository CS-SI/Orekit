/* Copyright 2022-2026 Romain Serra
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
package org.orekit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class LibraryArchUnitTest {

    // packages
    private static final String SSA_NAME = "..ssa..";
    private static final String FILES_NAME = "..files..";
    private static final String ESTIMATION_NAME = "..estimation..";
    private static final String PROPAGATION_NAME = "..propagation..";
    private static final String FORCES_NAME = "..forces..";
    private static final String ATTITUDES_NAME = "..attitudes..";
    private static final String DATA_NAME = "..data..";
    private static final String ORBITS_NAME = "..orbits..";
    private static final String TIME_NAME = "..time..";
    private static final String BODIES_NAME = "..bodies..";
    private static final String UTILS_NAME = "..utils..";
    private static final String CONTROL_NAME = "..control..";
    private static final String FRAMES_NAME = "..frames..";
    private static final String ERRORS_NAME = "..errors..";
    private static final String MODELS_NAME = "..models..";
    private static final String GNSS_NAME = "..gnss..";

    // sub-packages
    private static final String CR3BP_NAME = "..cr3bp..";
    private static final String INDIRECT_NAME = "..indirect..";

    // GIVEN
    private static final JavaClasses IMPORTED_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.orekit");

    @Test
    void testClassesEstimationPackageAccess() {
        // WHEN
        final ArchRule myRule = classes()
                .that().resideInAPackage(ESTIMATION_NAME)
                .should().onlyBeAccessed().byAnyPackage(ESTIMATION_NAME, PROPAGATION_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testClassesControlExceptIndirectPackageAccess() {
        // WHEN
        final ArchRule myRule = classes()
                .that().resideInAPackage(CONTROL_NAME).and().resideOutsideOfPackage(INDIRECT_NAME)
                .should().onlyBeAccessed().byAnyPackage(CONTROL_NAME, ESTIMATION_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesControlIndirectPackageAccess() {
        // WHEN
        final ArchRule myRule = classes()
                .that().resideInAPackage(CONTROL_NAME).and().resideInAPackage(INDIRECT_NAME)
                .should().onlyBeAccessed().byAnyPackage(CONTROL_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testClassesSsaPackageAccess() {
        // WHEN
        final ArchRule myRule = classes()
                .that().resideInAPackage(SSA_NAME)
                .should().onlyBeAccessed().byAnyPackage(SSA_NAME, FILES_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesPropagationPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(PROPAGATION_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, FILES_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesFramesPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(FRAMES_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME, ORBITS_NAME, GNSS_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesBodiesPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(BODIES_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME, ORBITS_NAME, GNSS_NAME, FILES_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesAttitudesPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(ATTITUDES_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME, FILES_NAME, ORBITS_NAME, GNSS_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesErrorsPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(ERRORS_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME, BODIES_NAME, PROPAGATION_NAME, ATTITUDES_NAME, DATA_NAME, TIME_NAME,
                        FRAMES_NAME, ORBITS_NAME, MODELS_NAME, GNSS_NAME, FILES_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesTimePackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(TIME_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME, BODIES_NAME, PROPAGATION_NAME, ATTITUDES_NAME, ORBITS_NAME, FILES_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesOrbitsExceptCr3bpPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(ORBITS_NAME).and().resideOutsideOfPackage(CR3BP_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME, PROPAGATION_NAME, CR3BP_NAME, GNSS_NAME, FILES_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesOrbitsCr3bpPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(ORBITS_NAME).and().resideInAPackage(CR3BP_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME, GNSS_NAME, FILES_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesUtilsPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(UTILS_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesGnssPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(GNSS_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, CONTROL_NAME, FORCES_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesModelsPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(MODELS_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, CONTROL_NAME, ESTIMATION_NAME,
                        ATTITUDES_NAME, FILES_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }
}
