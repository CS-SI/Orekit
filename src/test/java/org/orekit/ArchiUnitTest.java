package org.orekit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchUnitTest {

    private static final JavaClasses IMPORTED_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.orekit");

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

    @Test
    void testClassesEstimationPackageAccess() {

        final ArchRule myRule = classes()
                .that().resideInAPackage(ESTIMATION_NAME)
                .should().onlyBeAccessed().byAnyPackage(ESTIMATION_NAME, PROPAGATION_NAME);

        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testClassesControlPackageAccess() {

        final ArchRule myRule = classes()
                .that().resideInAPackage(CONTROL_NAME)
                .should().onlyBeAccessed().byAnyPackage(CONTROL_NAME, ESTIMATION_NAME);

        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testClassesSsaPackageAccess() {

        final ArchRule myRule = classes()
                .that().resideInAPackage(SSA_NAME)
                .should().onlyBeAccessed().byAnyPackage(SSA_NAME, FILES_NAME);

        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesFramesPackageAccess() {

        final ArchRule myRule = noClasses()
                .that().resideInAPackage(FRAMES_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME, ORBITS_NAME);

        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesBodiesPackageAccess() {

        final ArchRule myRule = noClasses()
                .that().resideInAPackage(BODIES_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME, ORBITS_NAME);

        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesAttitudesPackageAccess() {

        final ArchRule myRule = noClasses()
                .that().resideInAPackage(ATTITUDES_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME, FILES_NAME);

        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesErrorsPackageAccess() {

        final ArchRule myRule = noClasses()
                .that().resideInAPackage(ERRORS_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME, BODIES_NAME, PROPAGATION_NAME, ATTITUDES_NAME, DATA_NAME, TIME_NAME,
                        FRAMES_NAME, ORBITS_NAME, MODELS_NAME);

        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesTimePackageAccess() {

        final ArchRule myRule = noClasses()
                .that().resideInAPackage(TIME_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME, BODIES_NAME, PROPAGATION_NAME, ATTITUDES_NAME, ORBITS_NAME);

        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesOrbitsPackageAccess() {

        final ArchRule myRule = noClasses()
                .that().resideInAPackage(ORBITS_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME);

        myRule.check(IMPORTED_CLASSES);
    }


    @Test
    void testNoClassesUtilsPackageAccess() {

        final ArchRule myRule = noClasses()
                .that().resideInAPackage(UTILS_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, ESTIMATION_NAME, CONTROL_NAME,
                        FORCES_NAME);

        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesGnssPackageAccess() {

        final ArchRule myRule = noClasses()
                .that().resideInAPackage(GNSS_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, CONTROL_NAME, FORCES_NAME);

        myRule.check(IMPORTED_CLASSES);
    }

    @Test
    void testNoClassesModelsPackageAccess() {

        final ArchRule myRule = noClasses()
                .that().resideInAPackage(MODELS_NAME)
                .should().dependOnClassesThat().resideInAnyPackage(SSA_NAME, CONTROL_NAME, ESTIMATION_NAME,
                        ATTITUDES_NAME);

        myRule.check(IMPORTED_CLASSES);
    }
}
