package plus.wcj.jetbrains.plugins.screenshot.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author ChangJin Wei (魏昌进)
 */
public class ScreenshotConfigurable implements SearchableConfigurable, Configurable.NoScroll {

    SettingsUI settingsUI;

    @Override
    public @NotNull @NonNls String getId() {
        return "screenshot";
    }

    @Override
    public String getDisplayName() {
        return "Screenshot";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsUI = new SettingsUI();
        return settingsUI.getPanel();
    }

    @Override
    public boolean isModified() {
        ScreenshotStateProvider service = ScreenshotStateProvider.getInstance();
        return settingsUI != null && !service.getState().equals(settingsUI.toState());
    }

    @Override
    public void apply() {
        ScreenshotStateProvider service = ScreenshotStateProvider.getInstance();
        service.loadState(settingsUI.toState());
    }

    @Override
    public void reset() {
        ScreenshotStateProvider service = ScreenshotStateProvider.getInstance();
        settingsUI.fromState(service.getState());
    }

    @Override
    public void disposeUIResources() {
        settingsUI = null;
    }
}
