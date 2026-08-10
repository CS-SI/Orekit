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
package org.orekit.forces;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ForcesArchUnitTest {

    private static final String RADIATION_NAME = "..radiation..";
    private static final String MANEUVERS_NAME = "..maneuvers..";
    private static final String DRAG_NAME = "..drag..";
    private static final String GRAVITY_NAME = "..gravity..";
    private static final String EMPIRICAL_NAME = "..empirical..";
    private static final String INERTIA_NAME = "..inertia..";

    // GIVEN
    private static final JavaClasses IMPORTED_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.orekit.forces");

    @Test
    void testNoClassesInertiaPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(INERTIA_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(MANEUVERS_NAME, DRAG_NAME, EMPIRICAL_NAME,
                        RADIATION_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesRadiationPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(RADIATION_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(MANEUVERS_NAME, DRAG_NAME, EMPIRICAL_NAME,
                        GRAVITY_NAME, INERTIA_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesDragPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(DRAG_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(MANEUVERS_NAME, EMPIRICAL_NAME, GRAVITY_NAME,
                        RADIATION_NAME, INERTIA_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesManeuversPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(MANEUVERS_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(DRAG_NAME, EMPIRICAL_NAME, GRAVITY_NAME,
                        RADIATION_NAME, INERTIA_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesGravityPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(GRAVITY_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(RADIATION_NAME, DRAG_NAME, EMPIRICAL_NAME,
                        MANEUVERS_NAME, INERTIA_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesEmpiricalPackageAccess() {
        // WHEN
        final ArchRule myRule = noClasses()
                .that().resideInAPackage(EMPIRICAL_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(RADIATION_NAME, DRAG_NAME, GRAVITY_NAME,
                        MANEUVERS_NAME, INERTIA_NAME);
        // THEN
        myRule.check(IMPORTED_CLASSES);
    }

}
