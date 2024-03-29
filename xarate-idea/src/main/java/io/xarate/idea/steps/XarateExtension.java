package io.xarate.idea.steps;

import com.intellij.openapi.module.Module;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.BDDFrameworkType;
import org.jetbrains.plugins.cucumber.StepDefinitionCreator;
import org.jetbrains.plugins.cucumber.psi.GherkinFile;
import org.jetbrains.plugins.cucumber.psi.GherkinFileType;
import org.jetbrains.plugins.cucumber.steps.AbstractCucumberExtension;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;

import java.util.*;

public class XarateExtension extends AbstractCucumberExtension {


    @Override
    public @Nullable List<AbstractStepDefinition> loadStepsFor(@Nullable PsiFile psiFile, @NotNull Module module) {
        List<AbstractStepDefinition> steps = new ArrayList<>();

        JavaPsiFacade javaPsi = JavaPsiFacade.getInstance(module.getProject());
        GlobalSearchScope librariesScope = ProjectScope.getLibrariesScope(module.getProject());

        Set<String> autoDefMethodNames = new TreeSet<>();
        PsiClass autoDef = javaPsi.findClass("com.intuit.karate.core.AutoDef", librariesScope);
        if (autoDef != null) {
            for (PsiMethod autoDefMethod : AnnotatedElementsSearch.searchPsiMethods(autoDef, librariesScope)) {
                steps.add(new XarateAutoDefStepDefinition(autoDefMethod));
                autoDefMethodNames.add(autoDefMethod.getName());
            }
        }       
        PsiClass karateActions = javaPsi.findClass("com.intuit.karate.ScenarioActions", librariesScope);
        if (karateActions != null) {
            for (PsiMethod method : karateActions.getAllMethods()) {
                getKarateAnnotation(method).ifPresent(karateAnnotation -> steps.add(
                        method.getName().equals("eval") && method.getParameterList().getParametersCount() == 3 ?
                                new XarateEvalStepDefinition(method, karateAnnotation, autoDefMethodNames) :
                                new XarateStepDefinition(method, karateAnnotation)));
            }
        }
        return steps;
    }

    private Optional<PsiAnnotation> getKarateAnnotation(PsiMethod method) {
        return Arrays.stream(method.getAnnotations()).filter(ann-> ann.getQualifiedName().startsWith("com.intuit.karate")).findAny();
    }

    // Methods inherited from extensions which only apply for step creation and are not relevant here
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
        return new BDDFrameworkType(GherkinFileType.INSTANCE);
    }

    @Override
    public @NotNull StepDefinitionCreator getStepDefinitionCreator() {
        throw new UnsupportedOperationException("This class implements OptionalCucumberExtension and does not support step definition creation");
    }

    @Override
    public Collection<? extends PsiFile> getStepDefinitionContainers(@NotNull GherkinFile gherkinFile) {
        return null;
    }


}
