package io.xarate.idea.run;

import com.intellij.execution.JavaRunConfigurationBase;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase5;
import org.jetbrains.plugins.cucumber.psi.GherkinFileType;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static io.xarate.idea.TestUtils.createDescriptorWithKarate;
import static org.junit.jupiter.api.Assertions.*;

class XarateRunConfigurationProducerTest extends LightJavaCodeInsightFixtureTestCase5 {

    XarateRunConfigurationProducerTest() {
        super(createDescriptorWithKarate());
    }

    @Test
    void runConfigurationOnFeature() {
        PsiFile file = getFixture().configureByText(GherkinFileType.INSTANCE, "Feature: my<caret> feature");

        ReadAction.run(() -> {
            PsiElement feature = Objects.requireNonNull(file.findElementAt(getFixture().getCaretOffset()));
            JavaRunConfigurationBase configuration = (JavaRunConfigurationBase) PlatformTestUtil.getRunConfiguration(feature, RunConfigurationProducer.getInstance(XarateRunConfigurationProducer.class));

           assertNotNull(configuration);
            assertEquals("Feature: my feature", configuration.getName());
           assertEquals("/src/aaa.feature", configuration.getProgramParameters());
           assertNull(configuration.getVMParameters());
        });
    }

    @Test
    void runConfigurationOnScenario() {
        PsiFile file = getFixture().configureByText(GherkinFileType.INSTANCE,
        """
           Feature: my feature
           Scenario: my<caret> scenario""");
        ReadAction.run(() -> {
            PsiElement scenario = Objects.requireNonNull(file.findElementAt(getFixture().getCaretOffset()));
            JavaRunConfigurationBase configuration = (JavaRunConfigurationBase) PlatformTestUtil.getRunConfiguration(scenario, RunConfigurationProducer.getInstance(XarateRunConfigurationProducer.class));

           assertNotNull(configuration);
           assertEquals("Scenario: my scenario", configuration.getName());
           assertEquals("/src/aaa.feature --name \"^my scenario$\"", configuration.getProgramParameters());
           assertNull(configuration.getVMParameters());
        });
    }

    @Test
    void runConfigurationOnNonGherkinFile() {
        PsiFile file = getFixture().configureByText(PlainTextFileType.INSTANCE, "Feature: my<caret> feature");
        ReadAction.run(() -> {
            PsiElement feature = Objects.requireNonNull(file.findElementAt(getFixture().getCaretOffset()));
            JavaRunConfigurationBase configuration = (JavaRunConfigurationBase) PlatformTestUtil.getRunConfiguration(feature, RunConfigurationProducer.getInstance(XarateRunConfigurationProducer.class));

            assertNull(configuration);
        });
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testdata";
    }

}
