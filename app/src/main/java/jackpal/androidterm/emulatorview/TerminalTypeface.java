package jackpal.androidterm.emulatorview;

import android.graphics.Typeface;

/**
 * Same-package access to the typeface of the patched emulatorview renderer (vendor/emulatorview, patch 1.0.42-p8.1,
 * roadmap P8). Renderers created after {@link #set} use the new typeface; a view rebuilds its renderer when its text
 * size is set again.
 * zh-CN: 以同包方式访问补丁后 emulatorview 渲染器的字体 (P8); {@link #set} 之后创建的渲染器使用新字体, 视图再次设置字号时重建渲染器.
 */
public final class TerminalTypeface {

    private TerminalTypeface() {
    }

    public static void set(Typeface typeface) {
        PaintRenderer.setDefaultTypeface(typeface);
    }

    public static Typeface get() {
        return PaintRenderer.getDefaultTypeface();
    }

}
