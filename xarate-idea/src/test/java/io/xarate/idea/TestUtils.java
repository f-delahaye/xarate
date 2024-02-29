package io.xarate.idea;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.MavenDependencyUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;


public class TestUtils {

    public static LightProjectDescriptor createDescriptorWithKarate() {
        return new LightProjectDescriptor() {
            @Override
            protected void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
                MavenDependencyUtil.addFromMaven(model, "io.karatelabs:karate-core:1.5.0.RC3", true, DependencyScope.TEST, Collections.emptyList());
            }
        };
    }
}
