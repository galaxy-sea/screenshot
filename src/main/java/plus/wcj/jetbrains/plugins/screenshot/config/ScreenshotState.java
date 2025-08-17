package plus.wcj.jetbrains.plugins.screenshot.config;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author ChangJin Wei (魏昌进)
 */
public class ScreenshotState {

    public boolean includeGutter = true;

    public boolean clipboard = true;

    public boolean save = true;

    public String outputDir;


    public void loadStateInit() {
        if (StringUtils.isBlank(outputDir)) {
            outputDir = Paths.get(System.getProperty("user.home"), "Downloads", "screenshot").toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScreenshotState that = (ScreenshotState) o;
        return includeGutter == that.includeGutter && clipboard == that.clipboard && save == that.save && Objects.equals(outputDir, that.outputDir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(includeGutter, clipboard, save, outputDir);
    }
}
