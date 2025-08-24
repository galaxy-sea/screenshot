package plus.wcj.jetbrains.plugins.screenshot;

import com.intellij.diff.tools.simple.SimpleDiffChange;
import com.intellij.diff.tools.simple.SimpleDiffModel;
import com.intellij.diff.tools.simple.SimpleDiffViewer;
import com.intellij.diff.util.DiffDividerDrawUtil;
import com.intellij.diff.util.Side;
import com.intellij.diff.util.TextDiffType;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * @author ChangJin Wei (魏昌进)
 * @since 2025/8/23
 */
public final class ScreenshotUtil {

    private ScreenshotUtil() {
    }

    public static <T> T getField(Object obj, String fieldName) {
        return ReflectionUtil.getField(obj.getClass(), obj, null, fieldName);
    }

    public static boolean myViewer_needAlignChanges(SimpleDiffModel simpleDiffModel) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        SimpleDiffViewer myViewer = getField(simpleDiffModel, "myViewer");
        Method needAlignChanges = myViewer.getClass().getSuperclass().getDeclaredMethod("needAlignChanges");
        needAlignChanges.setAccessible(true);                                         // 非 public 时需要
        return (boolean) needAlignChanges.invoke(myViewer);
    }

    public static EditorEx getEditor(@NotNull EditorEx editor, int maxHeight, int maxWidth) {
        Object o = Proxy.newProxyInstance(
                EditorEx.class.getClassLoader(),
                new Class[]{EditorEx.class},
                (p, method, args) -> {
                    if (method.getName().equals("getScrollingModel")) {
                        Object invoke = method.invoke(editor, args);
                        return getVisibleArea(invoke, maxHeight, maxWidth);
                    } else {
                        return method.invoke(editor, args);
                    }
                });
        return (EditorEx) o;
    }

    public static Object getVisibleArea(Object invoke, int maxHeight, int maxWidth) {
        return Proxy.newProxyInstance(
                ScrollingModel.class.getClassLoader(),
                new Class[]{ScrollingModel.class},
                (p, method, args) -> {
                    if (method.getName().equals("getVisibleArea")) {
                        return new Rectangle(0, 0, maxWidth, maxHeight);
                    } else if (method.getName().equals("getVerticalScrollOffset")) {
                        return 0;
                    } else {
                        return method.invoke(invoke, args);
                    }
                });
    }


    /** {@link SimpleDiffModel.MyPaintable} */
    public static class ScreenshotPaintable implements DiffDividerDrawUtil.DividerPaintable {

        private final List<SimpleDiffChange> myChanges;

        private final boolean myNeedAlignChanges;


        public ScreenshotPaintable(@NotNull List<SimpleDiffChange> changes, boolean alignChanges) {
            myChanges = changes;
            myNeedAlignChanges = alignChanges;
        }

        @Override
        public void process(@NotNull DiffDividerDrawUtil.DividerPaintable.Handler handler) {
            for (SimpleDiffChange diffChange : myChanges) {
                int startLine1 = diffChange.getStartLine(Side.LEFT);
                int endLine1 = diffChange.getEndLine(Side.LEFT);
                int startLine2 = diffChange.getStartLine(Side.RIGHT);
                int endLine2 = diffChange.getEndLine(Side.RIGHT);
                TextDiffType type = diffChange.getDiffType();

                if (myNeedAlignChanges) {
                    if (!handler.processAligned(startLine1, endLine1, startLine2, endLine2, type)) {
                        return;
                    }
                } else if (!handler.processExcludable(startLine1, endLine1,
                                                      startLine2, endLine2,
                                                      type, diffChange.isExcluded(), diffChange.isSkipped())) {
                    return;
                }
            }
        }
    }
}
