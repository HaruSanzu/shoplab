package com.shoplab.tests.ui;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.JUNIT_PLATFORM_NAMING_STRATEGY_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PUBLISH_QUIET_PROPERTY_NAME;

/**
 * The entry point of the UI tests: "run all .feature files with Cucumber".
 * <p>
 * Maven (Surefire) and IntelliJ only know how to run JUnit. This class is a JUnit "suite" that says:
 * <ul>
 *   <li>{@code @IncludeEngines("cucumber")}: let the Cucumber engine run the tests,</li>
 *   <li>{@code @SelectPackages("features")}: the scenarios are in src/test/resources/features,</li>
 *   <li>glue: the step definitions and hooks are in the package com.shoplab.tests.ui (and below).</li>
 * </ul>
 * Scenario tags (@ui, @smoke ...) become JUnit tags, so "mvn test -Dgroups=smoke" works for API, DB and UI tests alike.
 */
// failIfNoTests = false: with "-Dgroups=api" all scenarios are filtered out, and that is fine, not an error
@Suite(failIfNoTests = false)
@IncludeEngines("cucumber")
@SelectPackages("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.shoplab.tests.ui")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-report.html, io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm")
// "long" = test names like "Shopping cart - Guest adds a product" in Maven and IntelliJ output
@ConfigurationParameter(key = JUNIT_PLATFORM_NAMING_STRATEGY_PROPERTY_NAME, value = "long")
// Don't print the advert for publishing reports to reports.cucumber.io
@ConfigurationParameter(key = PLUGIN_PUBLISH_QUIET_PROPERTY_NAME, value = "true")
public class RunCucumberTest {
}
