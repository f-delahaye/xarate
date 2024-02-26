package io.xarate.idea.steps;

import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase5;
import org.jetbrains.plugins.cucumber.psi.GherkinFileType;
import org.junit.jupiter.api.Test;

import static io.xarate.idea.TestUtils.createDescriptorWithKarate;

class XarateHighlightingTest extends LightJavaCodeInsightFixtureTestCase5 {

    XarateHighlightingTest() {
        super(createDescriptorWithKarate());
    }
    @Test
    void matchedStepIsAnnotated() {
        JavaCodeInsightTestFixture fixture = getFixture();

        fixture.allowTreeAccessForAllFiles();
        fixture.configureByText(GherkinFileType.INSTANCE,
        """
          Feature: Test
            Scenario: Test
                * def <info descr="null">var</info> = <info descr="null">'foo'</info>
          """);
        //info is the annotation used by Gherkin plugin for steps that were matched and that will appear in blue in the editor.
        // note that unlike the cucumber-java (and the official Karate plugin), only var and foo will be matched, not the full line.
        // Maybe due to our implementation of XarateStepDefition.getVariableNames??
        fixture.checkHighlighting(true, true, true, true);
    }

    @Test
    void unmatchedStepIsNotAnnotated() {
        JavaCodeInsightTestFixture fixture = getFixture();

        fixture.allowTreeAccessForAllFiles();
        fixture.configureByText(GherkinFileType.INSTANCE,
                """
                  Feature: Test
                    Scenario: Test
                        * not_a_keyword var = 'foo'
                  """);
        fixture.checkHighlighting(true, true, true, true);
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testdata";
    }

}
