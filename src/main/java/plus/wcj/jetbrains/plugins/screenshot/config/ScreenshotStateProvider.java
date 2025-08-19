package plus.wcj.jetbrains.plugins.screenshot.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.SettingsCategory;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author ChangJin Wei (魏昌进)
 */
@State(name = "ScreenshotPro", storages = {@Storage(value = "screenshotPro.xml")}, category = SettingsCategory.PLUGINS)
public class ScreenshotStateProvider implements PersistentStateComponent<ScreenshotState> {

    ScreenshotState config = new ScreenshotState();

    public static ScreenshotStateProvider getInstance() {
        return ApplicationManager.getApplication().getService(ScreenshotStateProvider.class);
    }

    @Override
    @NotNull
    public ScreenshotState getState() {
        this.config.loadStateInit();
        return this.config;
    }

    @Override
    public void loadState(@NotNull ScreenshotState state) {
        XmlSerializerUtil.copyBean(state, this.config);
        this.config.loadStateInit();
    }


}
