package io.xarate.idea.steps;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase5;
import org.jetbrains.plugins.cucumber.psi.GherkinFileType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.xarate.idea.TestUtils.createDescriptorWithKarate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class XarateCompletionTest extends LightJavaCodeInsightFixtureTestCase5 {
    XarateCompletionTest()  {
        super(createDescriptorWithKarate());
    }

    @Test
    void input() {
        JavaCodeInsightTestFixture fixture = getFixture();

        fixture.configureByText(GherkinFileType.INSTANCE,
                """
                  Feature: Test
                    Scenario: Test
                        * inpu<caret>""");
        fixture.complete(CompletionType.BASIC);
        List<String> completions = fixture.getLookupElementStrings();

        // Not sure if regexps can distinguish between input(string) and input(string[]) ... they both map to input(<string>)
        assertEquals(Arrays.asList("input(<string>, <string>)", "input(<string>, <string>)", "input(<string>, <string>, <number>)", "input(<string>, <string>, <number>)"), completions);
    }

    @Test
    void back() {
        JavaCodeInsightTestFixture fixture = getFixture();

        fixture.configureByText(GherkinFileType.INSTANCE,
                """
                  Feature: Test
                    Scenario: Test
                        * b<caret>""");

        fixture.complete(CompletionType.BASIC);
        List<String> completions = fixture.getLookupElementStrings();

        // bac<completion> gets resolved to back in the UI but for some reason not in the UT.
        // So we're simulating b<completion> and only checks back() is the first suggestion
        assertEquals("back()", completions.get(0));
    }

    @Test
    void eval() {
        JavaCodeInsightTestFixture fixture = getFixture();

        fixture.configureByText(GherkinFileType.INSTANCE,
                """
                  Feature: Test
                    Scenario: Test
                        * someCall<caret>""");

        fixture.complete(CompletionType.BASIC);
        List<String> completions = fixture.getLookupElementStrings();
        // No completion, but should still be highlighted (maps to eval)
        assertEquals(Collections.emptyList(), completions);
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testdata";
    }

}
