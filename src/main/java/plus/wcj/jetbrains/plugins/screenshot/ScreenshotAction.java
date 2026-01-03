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

package plus.wcj.jetbrains.plugins.screenshot;

import com.intellij.diff.FrameDiffTool;
import com.intellij.diff.tools.simple.SimpleDiffModel;
import com.intellij.diff.tools.simple.SimpleDiffViewer;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.diff.tools.util.FoldingModelSupport;
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer;
import com.intellij.diff.util.DiffDividerDrawUtil;
import com.intellij.diff.util.DiffDrawUtil;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import plus.wcj.jetbrains.plugins.screenshot.config.ScreenshotState;
import plus.wcj.jetbrains.plugins.screenshot.config.ScreenshotStateProvider;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author ChangJin Wei (魏昌进)
 */
public class ScreenshotAction extends DumbAwareAction {

    public static final String ID = "Screenshot Pro";

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        ScreenshotState state = ScreenshotStateProvider.getInstance().getState();

        Presentation presentation = e.getPresentation();

        if (!(state.clipboard || state.save)) {
            presentation.setEnabled(false);
            Project project = e.getProject();
            NotificationGroupManager.getInstance()
                                    .getNotificationGroup(ID)
                                    .createNotification(ID, "Please enable Clipboard or set an Output directory in Settings.", NotificationType.INFORMATION)
                                    .addAction(NotificationAction.createSimpleExpiring("Open in Settings", () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, ID)))
                                    .notify(project);
            return;
        }
        presentation.setEnabled(true);
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            if ("Android Studio".equals(ApplicationNamesInfo.getInstance().getFullProductName())) {
                ToolWindow tw = e.getData(PlatformDataKeys.TOOL_WINDOW);
                if (tw != null && "Running Devices".equalsIgnoreCase(tw.getId())) {
                    presentation.setEnabled(false);
                }
            }
            return;
        }
        if (editor.getEditorKind() == EditorKind.DIFF) {
            FrameDiffTool.DiffViewer viewer = e.getData(DiffDataKeys.DIFF_VIEWER);
            if (viewer instanceof TwosideTextDiffViewer) {
                e.getPresentation().setText(editor.getSelectionModel().hasSelection() ? "Screenshot Selected Code" : "Screenshot Diff Code(Left + Right)");
                return;
            }
        }
        e.getPresentation().setText(editor.getSelectionModel().hasSelection() ? "Screenshot Selected Code" : "Screenshot All Code");
    }


    @Override
    public void actionPerformed(AnActionEvent e) {
        ScreenshotState state = ScreenshotStateProvider.getInstance().getState();

        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            notifyError(project, "Screenshotting code is only available in an editor");
            return;
        }
        if (editor.getEditorKind() == EditorKind.DIFF && !editor.getSelectionModel().hasSelection()) {
            FrameDiffTool.DiffViewer viewer = e.getData(DiffDataKeys.DIFF_VIEWER);
            if (viewer instanceof TwosideTextDiffViewer twosideTextDiffViewer) {
                screenshot(twosideTextDiffViewer, editor, state, project);
                return;
            }
        }
        screenshot(editor, state, project);
    }

    private void screenshot(TwosideTextDiffViewer twosideTextDiffViewer, Editor editor, ScreenshotState state, Project project) {
        try {
            Color background = editor.getContentComponent().getBackground();

            BufferedImage leftEditor = toBufferedImage(twosideTextDiffViewer.getEditor1(), state, project);
            BufferedImage rightEditor = toBufferedImage(twosideTextDiffViewer.getEditor2(), state, project);
            BufferedImage splitter = toBufferedImage(twosideTextDiffViewer, leftEditor, rightEditor);
            BufferedImage image = imageMerge(background, leftEditor, splitter, rightEditor);

            copyToClipboard(image, state);
            File file = saveImageToDisk(image, state, editor);
            notifyInfo(project, state, file);
        } catch (Exception e) {
            Messages.showErrorDialog("Failed to capture screenshot: " + e.getMessage(), "Error");
        }
    }

    private void screenshot(Editor editor, ScreenshotState state, Project project) {
        try {
            BufferedImage image = toBufferedImage(editor, state, project);
            copyToClipboard(image, state);
            File file = saveImageToDisk(image, state, editor);
            notifyInfo(project, state, file);
        } catch (Exception e) {
            Messages.showErrorDialog("Failed to capture screenshot: " + e.getMessage(), "Error");
        }
    }

    private BufferedImage toBufferedImage(TwosideTextDiffViewer twosideTextDiffViewer, BufferedImage leftEditor, BufferedImage rightEditor) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InterruptedException {
        Splitter splitter = UIUtil.findComponentOfType(twosideTextDiffViewer.getComponent(), Splitter.class);

        int maxHeight = Math.max(leftEditor.getHeight(), rightEditor.getHeight());
        int maxWidth = Math.max(leftEditor.getWidth(), rightEditor.getWidth());

        /** {@link SimpleDiffViewer.MyDividerPainter#paint(Graphics, JComponent)} */
        BufferedImage image = ImageUtil.createImage(splitter.getDividerWidth(), maxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = image.createGraphics();
        gg.setClip(new Rectangle(0, 0, splitter.getDividerWidth(), maxHeight));
        gg.setColor(DiffDrawUtil.getDividerColor(twosideTextDiffViewer.getEditor1()));
        gg.fill(new Rectangle(0, 0, splitter.getDividerWidth(), maxHeight));

        /** {@link SimpleDiffModel#paintPolygons(Graphics2D, JComponent)} */
        SimpleDiffModel myModel = ScreenshotUtil.getField(twosideTextDiffViewer, "myModel");
        ScreenshotUtil.ScreenshotPaintable paintable = new ScreenshotUtil.ScreenshotPaintable(myModel.getChanges(), ScreenshotUtil.myViewer_needAlignChanges(myModel));
        EditorEx editor1 = ScreenshotUtil.getEditor(twosideTextDiffViewer.getEditor1(), maxHeight, maxWidth);
        EditorEx editor2 = ScreenshotUtil.getEditor(twosideTextDiffViewer.getEditor2(), maxHeight, maxWidth);
        DiffDividerDrawUtil.paintPolygons(gg, splitter.getDividerWidth(), editor1, editor2, paintable);

        /** {@link SimpleDiffViewer.MyFoldingModel#paintOnDivider(Graphics2D, Component)} */
        /** {@link FoldingModelSupport.MyPaintable#paintOnDivider(Graphics2D, Component)} */
        FoldingModelSupport myFoldingModel = ScreenshotUtil.getField(twosideTextDiffViewer, "myFoldingModel");
        DiffDividerDrawUtil.DividerSeparatorPaintable myPaintable = ScreenshotUtil.getField(myFoldingModel, "myPaintable");
        DiffDividerDrawUtil.paintSeparators(gg, splitter.getDividerWidth(), editor1, editor2, myPaintable);

        gg.dispose();
        return image;
    }

    private BufferedImage toBufferedImage(Editor editor, ScreenshotState state, Project project) {
        try (CaretVisibilityGuard ignored = new CaretVisibilityGuard(editor)) {
            JComponent contentComponent = editor.getContentComponent();
            EditorGutterComponentEx gutterComponent = (EditorGutterComponentEx) editor.getGutter();

            ComponentInfo contentInfo = new ComponentInfo(editor, contentComponent, state, project);
            ComponentInfo gutterInfo = new ComponentInfo(gutterComponent, contentInfo, state);

            contentInfo.translateXY(contentComponent, contentInfo, gutterInfo);
            gutterInfo.translateXY(gutterComponent, contentInfo, gutterInfo);

            int imageWidth = gutterInfo.width + contentInfo.width;
            int imageHeight = Math.max(gutterInfo.height, contentInfo.height);

            BufferedImage image = ImageUtil.createImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                contentInfo.paint(graphics);
                gutterInfo.paint(graphics);
            } finally {
                if (contentInfo.hasSelection) {
                    editor.getSelectionModel().setSelection(contentInfo.selectionStart, contentInfo.selectionEnd);
                }
                graphics.dispose();
            }
            return image;
        }
    }

    private BufferedImage imageMerge(Color background, BufferedImage... images) {
        int imageWidth = 0;
        int imageHeight = 0;
        for (BufferedImage image : images) {
            imageWidth += image.getWidth();
            imageHeight = Math.max(imageHeight, image.getHeight());
        }
        BufferedImage imageMerge = ImageUtil.createImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = imageMerge.createGraphics();
        try {
            graphics.setColor(background);
            graphics.fillRect(0, 0, imageWidth, imageHeight);

            int x = 0;
            for (BufferedImage image : images) {
                graphics.drawImage(image, x, 0, null);
                x += image.getWidth();
            }
        } finally {
            graphics.dispose();
        }
        return imageMerge;
    }


    private File saveImageToDisk(BufferedImage image, ScreenshotState state, Editor editor) throws IOException {
        if (!state.save) {
            return null;
        }
        VirtualFile virtualFile = ((EditorImpl) editor).getVirtualFile();
        String fileName = virtualFile != null ? virtualFile.getName() : "screenshot";

        Path dir = Paths.get(state.outputDir);
        Files.createDirectories(dir);
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        Path out = dir.resolve(String.format("%s_%s.png", fileName, ts));
        File file = out.toFile();
        ImageIO.write(image, "PNG", file);
        return file;
    }

    private void copyToClipboard(BufferedImage image, ScreenshotState state) {
        if (!state.clipboard) {
            return;
        }
        Toolkit.getDefaultToolkit()
               .getSystemClipboard()
               .setContents(new Transferable() {
                   @Override
                   public DataFlavor[] getTransferDataFlavors() {
                       return new DataFlavor[]{DataFlavor.imageFlavor};
                   }

                   @Override
                   public boolean isDataFlavorSupported(DataFlavor flavor) {
                       return flavor == DataFlavor.imageFlavor;
                   }

                   @NotNull
                   @Override
                   public Object getTransferData(DataFlavor flavor) {
                       return image;
                   }
               }, null);
    }


    private void notifyInfo(Project project, ScreenshotState state, File file) {
        String content = "Please enable Clipboard or set an Output directory in Settings.";
        Notification n = NotificationGroupManager.getInstance()
                                                 .getNotificationGroup(ID)
                                                 .createNotification(ID, content, NotificationType.INFORMATION);

        if (state.save && file != null && file.exists()) {
            content = "Saved to: \n" + file.getName();
            n.addAction(NotificationAction.createSimpleExpiring("Open in Folder", () -> RevealFileAction.openFile(file)));
            n.addAction(NotificationAction.createSimpleExpiring("Open in Editor", () -> openImageInEditor(project, file)));
        }

        if (state.clipboard && state.save) {
            content = "Copied to clipboard and saved to:";
        } else if (state.clipboard) {
            content = "Copied to clipboard.";
        }

        n.setContent(content);
        n.notify(project);
    }

    private static void openImageInEditor(Project project, File file) {
        if (project == null || file == null) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile vf = VfsUtil.findFileByIoFile(file, true);
            if (vf != null) {
                FileEditorManager.getInstance(project).openFile(vf, true);
            } else {
                NotificationGroupManager.getInstance()
                                        .getNotificationGroup(ID)
                                        .createNotification(ID, "Failed to open image in editor.", NotificationType.WARNING)
                                        .notify(project);
            }
        });
    }

    private static void notifyError(Project project, String content) {
        NotificationGroupManager.getInstance()
                                .getNotificationGroup(ID)
                                .createNotification(ID, content, NotificationType.ERROR)
                                .notify(project);
    }

    /**  Ensures the caret is hidden during painting to avoid capturing it in the screenshot. */
    private static final class CaretVisibilityGuard implements AutoCloseable {

        private final Object target;
        private final Method setter;
        private final Boolean previous;

        CaretVisibilityGuard(Editor editor) {
            Object possibleEditor = editor instanceof EditorEx ? editor : null;
            Object caretModel = editor.getCaretModel();

            Object target = findTargetWithSetter(possibleEditor);
            if (target == null) {
                target = findTargetWithSetter(caretModel);
            }

            this.target = target;
            this.setter = target == null ? null : findSetter(target);
            this.previous = target == null ? null : readCurrentState(target);

            if (this.target != null && this.setter != null) {
                setCaretState(false);
            }
        }

        private static Object findTargetWithSetter(Object candidate) {
            if (candidate == null) {
                return null;
            }
            return findSetter(candidate) != null ? candidate : null;
        }

        private static Method findSetter(Object target) {
            for (String name : new String[]{"setCaretEnabled", "setCaretsEnabled", "setCaretVisible"}) {
                try {
                    return target.getClass().getMethod(name, boolean.class);
                } catch (NoSuchMethodException ignored) {
                }
            }
            return null;
        }

        private static Boolean readCurrentState(Object target) {
            for (String getter : new String[]{"isCaretEnabled", "isCaretsEnabled", "isCaretVisible"}) {
                try {
                    Method method = target.getClass().getMethod(getter);
                    Object result = method.invoke(target);
                    if (result instanceof Boolean b) {
                        return b;
                    }
                } catch (Exception ignored) {
                }
            }
            return null;
        }

        private void setCaretState(boolean enabled) {
            try {
                setter.invoke(target, enabled);
            } catch (Exception ignored) {
            }
        }

        @Override
        public void close() {
            if (target != null && setter != null) {
                setCaretState(previous != null ? previous : true);
            }
        }
    }

}
