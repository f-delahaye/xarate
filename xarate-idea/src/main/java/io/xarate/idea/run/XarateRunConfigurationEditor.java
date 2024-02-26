package io.xarate.idea.run;

import com.intellij.execution.application.JavaSettingsEditorBase;
import com.intellij.execution.ui.CommonParameterFragments;
import com.intellij.execution.ui.ModuleClasspathCombo;
import com.intellij.execution.ui.NestedGroupFragment;
import com.intellij.execution.ui.SettingsEditorFragment;
import com.intellij.openapi.ui.LabeledComponent;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class XarateRunConfigurationEditor extends JavaSettingsEditorBase<XarateRunConfiguration> {
    public XarateRunConfigurationEditor(XarateRunConfiguration runConfiguration) {
        super(runConfiguration);
    }

    @Override
    protected void customizeFragments(List<SettingsEditorFragment<XarateRunConfiguration, ?>> list, SettingsEditorFragment<XarateRunConfiguration, ModuleClasspathCombo> settingsEditorFragment, CommonParameterFragments<XarateRunConfiguration> commonParameterFragments) {
        list.add(new XarateSettingsFragment());
    }

    private static class XarateSettingsFragment extends NestedGroupFragment<XarateRunConfiguration> {

        private static final String GROUP = "Xarate";
        private static final String BASE_ID = "xarate";

        protected XarateSettingsFragment() {
            super(BASE_ID, GROUP, GROUP, comp -> false);
        }

        @Override
        protected List<SettingsEditorFragment<XarateRunConfiguration, ?>> createChildren() {
            return Arrays.asList(
                    createEditor("env", "Environment", XarateRunConfiguration::getEnvironment, XarateRunConfiguration::setEnvironment),
                    createEditor("tags", "Tags", XarateRunConfiguration::getTags, XarateRunConfiguration::setTags)
            );
        }

        private SettingsEditorFragment<XarateRunConfiguration, LabeledComponent<JTextField>> createEditor(String id, String name, Function<XarateRunConfiguration, String> provideUIValue, BiConsumer<XarateRunConfiguration, String> applyUIValue)  {
            return new SettingsEditorFragment<>(BASE_ID + "." + id, name, GROUP,
                    LabeledComponent.create(new JTextField(), name + ":", "West"), // the component
                    (config, comp) -> reset(config, provideUIValue, comp.getComponent()), // reset
                    (config, comp) -> apply(config, applyUIValue, comp.getComponent()),
                    config -> true); // initially visible
        }

        private void reset(XarateRunConfiguration configuration, Function<XarateRunConfiguration, String> configurationValueSupplier, JTextField component) {
            String configurationValue = configurationValueSupplier.apply(configuration);
            component.setText(configurationValue);
            System.out.println("XARATE --- reset component text "+component.getText()+" to configuration "+configuration);
        }

        private void apply(XarateRunConfiguration configuration, BiConsumer<XarateRunConfiguration, String> configurationValueSetter, JTextField component) {
            configurationValueSetter.accept(configuration, component.getText());
            System.out.println("XARATE --- applied component text "+component.getText()+" to configuration "+configuration);
        }
    }
}
