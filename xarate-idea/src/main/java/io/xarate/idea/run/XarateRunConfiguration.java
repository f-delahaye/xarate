package io.xarate.idea.run;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class XarateRunConfiguration extends ApplicationConfiguration {

    private String environment = "";
    private String tags = "";
    private String scenarioName = "";
    private String file = "";

    protected XarateRunConfiguration(String name, @NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(name, project, factory);
        setProgramParameters("");
    }

    private void rebuildProgramParameter() {
        setProgramParameters(
                file +
                (scenarioName.isEmpty()?"":(" --name \""+scenarioName+"\"")) +
                (environment.isEmpty()?"":(" --env "+environment)) +
                (tags.isEmpty()?"":(" --tags "+tags))
                );
    }

    public void setEnvironment(String env) {
        this.environment = env;
        rebuildProgramParameter();
    }

    public String getEnvironment() {
        return environment;
    }

    public void setTags(String tags) {
        this.tags = tags;
        rebuildProgramParameter();
    }

    public String getTags() {
        return tags;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
        rebuildProgramParameter();
    }

    public String getScenarioName() {
        return scenarioName;

    }

    public void setFile(String file) {
        this.file = file;
        rebuildProgramParameter();
    }

    public String getFile() {
        return file;
    }

    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new XarateRunConfigurationEditor(this);
    }

    @Override
    public String toString() {
        return "XRC "+getProgramParameters();
    }
}
