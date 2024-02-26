package io.xarate.idea.run;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.SimpleConfigurationType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;

public class XarateRunConfigurationType extends SimpleConfigurationType {
    protected XarateRunConfigurationType() {
        super("XarateRunConfiguration", "Xarate", "Xarate Run Configuration", NotNullLazyValue.createValue(() -> AllIcons.Icon));
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new XarateRunConfiguration(getDisplayName(), project, this);
    }

}
