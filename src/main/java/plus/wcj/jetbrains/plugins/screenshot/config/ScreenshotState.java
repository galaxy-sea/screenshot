/*
 * Copyright (C) 2025-present The original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
