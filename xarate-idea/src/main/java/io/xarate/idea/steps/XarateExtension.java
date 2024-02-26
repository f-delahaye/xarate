package io.xarate.idea.steps;

import com.intellij.openapi.module.Module;
import com.intellij.psi.*;
import com.intellij.psi.search.ProjectScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.BDDFrameworkType;
import org.jetbrains.plugins.cucumber.StepDefinitionCreator;
import org.jetbrains.plugins.cucumber.psi.GherkinFile;
import org.jetbrains.plugins.cucumber.steps.AbstractCucumberExtension;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;

import java.util.*;

public class XarateExtension extends AbstractCucumberExtension {

    @Override
    public boolean isStepLikeFile(@NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
        return false;
    }

    @Override
    public boolean isWritableStepLikeFile(@NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
        return false;
    }

    @Override
    public @NotNull BDDFrameworkType getStepFileType() {
        return null;
    }

    @Override
    public @NotNull StepDefinitionCreator getStepDefinitionCreator() {
        throw new UnsupportedOperationException("This class implements OptionalCucumberExtension and does not support step definition creation");
    }

    @Override
    public @Nullable List<AbstractStepDefinition> loadStepsFor(@Nullable PsiFile psiFile, @NotNull Module module) {
        PsiClass karateActions = JavaPsiFacade.getInstance(module.getProject()).findClass("com.intuit.karate.ScenarioActions", ProjectScope.getLibrariesScope(module.getProject()));
        if (karateActions == null) {
            return null;
        }
        List<AbstractStepDefinition> steps = new ArrayList<>();
        for (PsiMethod method: karateActions.getAllMethods()) {
            getKarateAnnotation(method).ifPresent(karateAnnotation -> steps.add(new XarateStepDefinition(method, karateAnnotation)));
        }
        return steps;
    }

    private Optional<PsiAnnotation> getKarateAnnotation(PsiMethod method) {
        return Arrays.stream(method.getAnnotations()).filter(ann-> ann.getQualifiedName().startsWith("com.intuit.karate")).findAny();
    }

    @Override
    public Collection<? extends PsiFile> getStepDefinitionContainers(@NotNull GherkinFile gherkinFile) {
        return null;
    }
}
