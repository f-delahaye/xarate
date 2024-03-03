package io.xarate.idea.steps;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code @AutoDef} steps are usually javascript functions which are registered in
 * Karate's JS engine to add extra functionalities.
 *<p>
 * All operations in Karate UI defined in Driver are for example @AutoDef.
 *</p>
 * <p>
 * Since they typically contain parenthesis (e.g. back() or input('p', 'foo'),
 * they are by default matched by ScenarioActions#eval.
 * </p>
 *<p>
 * AutoDef steps, on the other hand, don't have any @Given/When annotation associated, the matching regex is derived from the  method name and the parameter.
 * This allows for better highlighting and code-completion.
 *</p>
 * <p>
 * Note however that this is experimental
 * </p>
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

    // Method called for highlighting purposes, and it should return a proper regex that will match steps.
    // By default, getCucumberRegex delegates to getExpression which delegates to getCucumberRegexFromElement.
    // However, this class's assumption is that getExpression returns a human readable expression e.g. for code completion purposes.
    // So we override getCucumberRegex to delegate to getCucumberRegexFromElement directly. 
    @Override
    public @Nullable String getCucumberRegex() {
       return getCucumberRegexFromElement(getElement());
    }
	
    protected @Nullable String getCucumberRegexFromElement(PsiElement element) {
        if (element instanceof PsiMethod method) {
            return getRegex(method, false);
        }
        return null;
    }

        // Method called for completion purposes, and it should return a regex human-readable.
    // So, for example, parameters are separated by proper whitespaces, not \\h+
    // Note that CucumberCompletionContributor will automatically replace (.+) with <string>, \d+ with <number>, ...
    @Override
    public @Nullable String getExpression() {
        return getRegex(getMethod(), true);
    }

    private String getRegex(PsiMethod method, boolean humanReadable) {
        String whitespace = humanReadable?" ":"\\h*";
        // CucumberCompletionContributor removes the first ( but not the second one, hence ((
        String openingParenthesis = humanReadable?"((":"\\(";
        String closingParenthesis = humanReadable?"))":"\\)";
        return Arrays.stream(method.getParameterList().getParameters())
                .map(this::asRegexParameter)
                .collect(Collectors.joining(","+whitespace, "^"+method.getName() + openingParenthesis, closingParenthesis+"$"));
    }

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
