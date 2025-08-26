package plus.wcj.jetbrains.plugins.screenshot;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.InlayModel;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import plus.wcj.jetbrains.plugins.screenshot.config.ScreenshotState;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ChangJin Wei (魏昌进)
 * @since 2025/8/21
 */
class ComponentInfo {

    public int x, y;

    public int width, height;

    public final JComponent component;

    public final boolean show;

    public int translateX, translateY;

    //
    public boolean hasSelection;

    public int selectionStart, selectionEnd;

    public final boolean diffLeft;

    public int miniLineIndent;

    public ComponentInfo(Editor editor, JComponent contentComponent, ScreenshotState config, Project project) {
        Document document = editor.getDocument();
        this.hasSelection = editor.getSelectionModel().hasSelection();
        if (this.hasSelection) {
            SelectionModel selectionModel = editor.getSelectionModel();
            this.selectionStart = selectionModel.getSelectionStart();
            this.selectionEnd = selectionModel.getSelectionEnd();

            Point start = editor.offsetToXY(selectionStart);
            Point end = editor.offsetToXY(selectionEnd);

            this.height = end.y - start.y + editor.getLineHeight();
            getMaxSelectedLineWidth(editor, selectionStart, selectionEnd, project);
            this.translateY = -start.y;
            selectionModel.removeSelection();
        } else {
            int lineCount = document.getLineCount();
            int lastLineOffset = document.getLineEndOffset(lineCount - 1);
            int lineNumber = document.getLineNumber(lastLineOffset);
            int lastLineStartOffset = document.getLineStartOffset(lineNumber);

            Point lastLinePosition = editor.logicalPositionToXY(editor.offsetToLogicalPosition(lastLineStartOffset));
            int lineHeight = editor.getLineHeight();
            this.height = lastLinePosition.y + lineHeight;
            this.width = contentComponent.getPreferredSize().width;
        }
        this.component = contentComponent;
        this.show = true;
        this.diffLeft = false;
    }

    public ComponentInfo(EditorGutterComponentEx gutterComponent, ComponentInfo contentInfo, ScreenshotState config) {
        this.component = gutterComponent;
        if (this.show = config.includeGutter) {
            Dimension preferredSize = gutterComponent.getPreferredSize();
            this.width = preferredSize.width;
            this.height = contentInfo.height;
        } else {
            this.width = 0;
            this.height = 0;
        }
        this.diffLeft = gutterComponent.getLocationOnScreen().getX() > contentInfo.component.getLocationOnScreen().getX();
    }

    public void translateXY(JComponent contentComponent, ComponentInfo contentInfo, ComponentInfo gutterInfo) {
        if (gutterInfo.show) {
            if (gutterInfo.diffLeft) {
                this.x = 0;
                this.translateX = 0;
            } else {
                this.x = gutterInfo.width;
                this.translateX = gutterInfo.width;
            }
        }
        this.translateX -= this.miniLineIndent;
        this.width -= this.miniLineIndent;
    }

    public void translateXY(EditorGutterComponentEx contentComponent, ComponentInfo contentInfo, ComponentInfo gutterInfo) {
        this.translateY = contentInfo.translateY;
        if (gutterInfo.diffLeft) {
            this.x = contentInfo.width;
            this.translateX = contentInfo.width;
        } else {
            this.translateX = 0;
        }
    }


    public void paint(Graphics2D graphics) {
        if (show) {
            graphics.setClip(this.x, this.y, this.width, this.height);
            AffineTransform affineTransform = new AffineTransform();
            affineTransform.translate(this.translateX, this.translateY);
            graphics.setTransform(affineTransform);
            component.paint(graphics);
            graphics.setTransform(new AffineTransform());
        }
    }

    private void getMaxSelectedLineWidth(Editor editor, int selectionStart, int selectionEnd, Project project) {
        int maxWidth = 0;
        int minIndentsY = Integer.MAX_VALUE;
        int tabSize = tabSize(editor, project);
        List<Integer> LineIndentList = new ArrayList<>();

        InlayModel inlayModel = editor.getInlayModel();

        Document document = editor.getDocument();
        int startLine = document.getLineNumber(selectionStart);
        int endLine = document.getLineNumber(selectionEnd);
        for (int line = startLine; line <= endLine; line++) {
            int lineStart = document.getLineStartOffset(line);
            int lineEnd = document.getLineEndOffset(line);

            Point pEnd = editor.visualPositionToXY(editor.offsetToVisualPosition(lineEnd));
            minIndentsY = getMinIndentsY(pEnd, minIndentsY, document, lineStart, lineEnd, tabSize, LineIndentList);

            int inlayWidth = inlayModel.getAfterLineEndElementsForLogicalLine(line)
                                       .stream()
                                       .mapToInt(inlay -> inlay.getRenderer().calcWidthInPixels(inlay))
                                       .max()
                                       .orElse(0);

            maxWidth = Math.max(maxWidth, pEnd.x + inlayWidth);

        }

        Font font = editor.getColorsScheme().getFont(EditorFontType.PLAIN);
        Graphics2D graphics = (Graphics2D) editor.getComponent().getGraphics();
        FontMetrics fontMetrics = graphics.getFontMetrics(font);

        int miniLineIndent = fontMetrics
                .stringWidth(" ".repeat(LineIndentList.stream()
                                                      .mapToInt(value -> value)
                                                      .min()
                                                      .orElse(0)));

        this.width = maxWidth + 24;
        this.miniLineIndent = miniLineIndent;
    }

    private static int getMinIndentsY(Point pEnd, int minIndentsY, Document document, int lineStart, int lineEnd, int tabSize, List<Integer> LineIndentList) {
        if (pEnd.y != minIndentsY) {
            minIndentsY = pEnd.y;
            if (lineStart == lineEnd) {
                return minIndentsY;
            }
            String text = document.getText(new TextRange(lineStart, lineEnd));
            int indent = 0;
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (' ' == ch) {
                    indent++;
                    continue;
                } else if (Character.isWhitespace(ch)) {
                    indent += tabSize;
                    continue;
                }
                break;
            }
            LineIndentList.add(indent);
        }
        return minIndentsY;
    }


    private int tabSize(Editor editor, Project project) {
        try {
            return editor.getSettings().getTabSize(project);
        } catch (Throwable ignore) {
            return 4;
        }
    }
}
