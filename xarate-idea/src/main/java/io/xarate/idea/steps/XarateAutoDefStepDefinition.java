package io.xarate.idea.steps;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @AutoDef steps are usually javascript functions which are registered in
 * Karate's JS engine to add extra functionalities.
 *
 * All operations in Karate UI defined in Driver are for example @AutoDef.
 *
 * Since they typically contain parenthesis (e.g. back() or input('p', 'foo'),
 * they are by default matched by ScenarioActions#eval.
 *
 * AuotDef steps, on the other hand, don't have any @Given/When annotation associated, the matching regex is derived from the  method name and the parameter.
 * This allows for better highlighting and code-completion.
 *
 * Note however that this is experimental
 */
public class XarateAutoDefStepDefinition extends AbstractStepDefinition {

    public XarateAutoDefStepDefinition(@NotNull PsiMethod method) {
        super(method);
    }

    private PsiMethod getMethod() {
        return (PsiMethod) getElement();
    }


    @Override
    public List<String> getVariableNames() {
        return Arrays.stream(getMethod().getParameterList().getParameters()).map(PsiParameter::getName).collect(Collectors.toList());
    }

    // Method called for completion purposes, and it should return a regex human-readable.
    // note that CucumberCompletionContributor will automatically replace (.+) with <string>, \d+ with <number>, ...
    @Override
    protected @Nullable String getCucumberRegexFromElement(PsiElement element) {
        if (element instanceof PsiMethod method) {
            return Arrays.stream(method.getParameterList().getParameters())
                    .map(this::asRegexParameter)
                    // CucumberCompletionContributor removes the first ( but not the second one, hence ((
                    .collect(Collectors.joining(", ", method.getName() + "((", "))"));
        }
        return null;
    }

//    @Override
//    public boolean matches(@NotNull String stepName) {
//        if (stepName.startsWith(AUTO_DEF_PREFIX)) {
//            return super.matches(stepName.substring(AUTO_DEF_PREFIX.length()));
//        }
//        return false;
//    }

    @Override
    public boolean supportsRename(@Nullable String newName) {
        return false;
    }

    // The returned String must match those defined in CucumberCompletionContributor
    private String asRegexParameter(PsiParameter parameter) {
        PsiType type = parameter.getType();
        if (PsiTypes.intType().equals(type) || PsiTypes.doubleType().equals(type)) {
            return "(-?\\d+)";
        }
        if (PsiTypes.doubleType().equals(type)) {
            return "(-?\\d*[.,]?\\d+)";
        }
        return "(.+)";
    }

}
