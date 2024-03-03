package io.xarate.idea.steps;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Subclass of {@link XarateStepDefinition} dedicated to the eval(name, dot_or_parenthesis, exp) action.
 * This action will capture any steps having a "(" (in addition to those having a ".") and therefore overlaps with
 * {@link XarateAutoDefStepDefinition}.
 *
 * <p>
 *     This means that by default, back(), for example, will be matched both by eval() and by the @AutoDef Driver.back().
 *     The Gherkin plugin will (seemingly randomly, maybe there's a HashMap somewhere) one or the other to highlight the step.
 * </p>
 * <p>However, for consistency, we would like to have only one match, ideally the @AutoDef one because it is more specific.
 * Therefore, we need to exclude eval for those steps which in fact are @AutoDef.
 * </p>
 * <p>
 *     Also, eval will not appear in code completion.
 * </p>
 * <p>Note that the other eval methods of ScenarioActions, triggered by the eval keyword, are not concerned by this class
 * and are handled like any other {@link XarateStepDefinition}</p>
 */
public class XarateEvalStepDefinition extends XarateStepDefinition {

    private final Set<String> autoDefMethodNames;

    public XarateEvalStepDefinition(@NotNull PsiMethod method, PsiAnnotation karateAnnotation, Set<String> autoDefMethodNames) {
        super(method, karateAnnotation);
        this.autoDefMethodNames = autoDefMethodNames;
    }

    // Method called for highlighting purposes, and it should return a proper regex that will match steps.
    // By default, getCucumberRegex delegates to getExpression which delegates to getCucumberRegexFromElement.
    // However, this class's assumption is that getExpression returns null as eval() is not eligible for code completion.
    // So we override getCucumberRegex to delegate to getCucumberRegexFromElement directly. 
    @Override
    public @Nullable String getCucumberRegex() {
       return getCucumberRegexFromElement(getElement());
    }

    @Override
    public boolean matches(@NotNull String stepName) {
	// check based on regex
	boolean matches = super.matches(stepName);
	// refine to exclude autoDef steps, that will be matched by XarateAutoDefStepDefinition
	return matches && !isAutoDef(stepName);
    }

    private boolean isAutoDef(String stepName) {
        int parenthesisIdx = stepName.indexOf("(");
        if (parenthesisIdx != -1) {
            String methodName = stepName.substring(0, parenthesisIdx);
	    return autoDefMethodNames.contains(methodName);
        }
	return false;
    }

    // Method called for completion purposes
    // eval is not electable for auto-completion, so this always returns null;
    @Override
    public @Nullable String getExpression() {
 	return null;
    }
}
