package io.xarate.idea.run;

import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.junit.JavaRunConfigurationProducerBase;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.cucumber.psi.GherkinFeature;
import org.jetbrains.plugins.cucumber.psi.GherkinScenario;
import org.jetbrains.plugins.cucumber.psi.GherkinScenarioOutline;
import org.jetbrains.plugins.cucumber.psi.GherkinStepsHolder;

public class XarateRunConfigurationProducer extends JavaRunConfigurationProducerBase<XarateRunConfiguration> {

    private final static Logger LOG = Logger.getInstance(XarateRunConfigurationProducer.class);

    private static final String SCENARIO_OUTLINE_PARAMETER_REGEXP = "\\\\<.*?\\\\>";
    private static final String ANY_STRING_REGEXP = ".*";
    private static final String NAME_FILTER_TEMPLATE = "^%s$";

    private static final XarateRunConfigurationType FACTORY = new XarateRunConfigurationType();

    @Override
    protected boolean setupConfigurationFromContext(@NotNull XarateRunConfiguration configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> ref) {

        Location<?> location = context.getLocation();
        if (location == null) {
            return false;
        }

        String karateMainClassName = "com.intuit.karate.Main";
        final Project project = configuration.getProject();
        if (JavaPsiFacade.getInstance(project).findClass(karateMainClassName, GlobalSearchScope.allScope(project)) == null) {
            LOG.debug("Failed to find main class: ", configuration.getMainClassName());
            return false;
        }

        configuration.setMainClassName(karateMainClassName);
        PsiElement element = context.getPsiLocation();
        GherkinStepsHolder scenario = PsiTreeUtil.getParentOfType(element, GherkinScenario.class, GherkinScenarioOutline.class);
        GherkinFeature feature = PsiTreeUtil.getParentOfType(element, GherkinFeature.class);

        if (scenario == null && feature == null) {
            // All features in directories and other exotic cases are not supported.
            return false;
        }

        configuration.setFile(element.getContainingFile().getVirtualFile().getCanonicalPath());
        configuration.setScenarioName(getScenarioNameParameter(scenario));

        configuration.setModule(context.getModule());

        configuration.setName(scenario != null ? ("Scenario: "+scenario.getScenarioName()) : ("Feature: "+feature.getFeatureName()));

        JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, location);
        return true;
    }

    protected String getScenarioNameParameter(GherkinStepsHolder scenario) {
        if (scenario != null) {
            String scenarioName = String.format(NAME_FILTER_TEMPLATE, StringUtil.escapeToRegexp(scenario.getScenarioName()));
            if (scenario instanceof GherkinScenarioOutline) {
                scenarioName = scenarioName.replaceAll(SCENARIO_OUTLINE_PARAMETER_REGEXP, ANY_STRING_REGEXP);
            }
            return scenarioName;
        }
        return "";
    }


    @Override
    public boolean isConfigurationFromContext(@NotNull XarateRunConfiguration configuration, @NotNull ConfigurationContext context) {
        Location<?> location = context.getLocation();
        if (location == null) {
            return false;
        }
        final Location<?> classLocation = JavaExecutionUtil.stepIntoSingleClass(location);
        if (classLocation == null) {
            return false;
        }

        PsiElement element = context.getPsiLocation();
        if (element == null) {
            return false;
        }

        final VirtualFile fileToRun = element.getContainingFile().getVirtualFile();
        if (fileToRun == null) {
            return false;
        }

        if (!configuration.getFile().equals(fileToRun.getCanonicalPath())) {
            return false;
        }

        GherkinStepsHolder scenario = PsiTreeUtil.getParentOfType(element, GherkinScenario.class, GherkinScenarioOutline.class);
        if (!Comparing.strEqual(getScenarioNameParameter(scenario), configuration.getScenarioName())) {
            return false;
        }

        final Module configurationModule = configuration.getConfigurationModule().getModule();
        return Comparing.equal(classLocation.getModule(), configurationModule);
    }

    @Override
    public @NotNull ConfigurationFactory getConfigurationFactory() {
        return FACTORY;
    }
}
