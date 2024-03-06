package io.xarate.idea.steps;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class XarateStepDefinition extends AbstractStepDefinition {
    private final PsiAnnotation karateAnnotation;

    public XarateStepDefinition(@NotNull PsiMethod method, PsiAnnotation karateAnnotation) {
        super(method);
        this.karateAnnotation = karateAnnotation;
    }

    protected PsiMethod getMethod() {
        return (PsiMethod) getElement();
    }

    @Override
    public List<String> getVariableNames() {
        return Arrays.stream(getMethod().getParameterList().getParameters()).map(PsiParameter::getName).collect(Collectors.toList());
    }

    @Override
    protected @Nullable String getCucumberRegexFromElement(PsiElement psiElement) {
        return karateAnnotation.findAttributeValue("value")
                .getText()
                .replace("\\\\", "\\")
                // the project that inspired this plugin, cucumber-java, replaces ("\\\", "\")
                // (https://github.com/JetBrains/intellij-plugins/blob/master/cucumber-java/src/org/jetbrains/plugins/cucumber/java/steps/JavaAnnotatedStepDefinition.java)
                // but that does not work here.
                // It's a bit confusing, especially since the replace() above is similar...
                .replace("\"", "");
    }

    @Override
    public boolean supportsRename(@Nullable String newName) {
        return false;
    }
}
