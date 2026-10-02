package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.widget.TextView

/**
 * Cursor and selection handle tinting for API 24 to 28, where `TextView` has no public setters:
 * the editor loads these drawables lazily through `TextView.context.getDrawable()`, so a context
 * whose resources tint exactly those ids is enough. No private fields, framework resource names or
 * global theme / resource mutations are involved.
 * zh-CN: API 24 至 28 的光标与选择手柄着色: `TextView` 没有公开 setter, 但编辑器会经 `context.getDrawable()`
 * 惰性加载这些 drawable, 用一个只对这几个 id 着色的资源上下文即可; 不涉及私有字段, 框架资源名或全局主题修改.
 */
@Suppress("DEPRECATION") // Resources constructor and the legacy overloads remain public on these API levels.
internal class LegacyInputTintContext(base: Context, color: Int) : ContextWrapper(base) {

    private val tintedResources = InputResources(base.resources, color)

    override fun getResources(): Resources = tintedResources

    /** Call after Material has wrapped the input's context and before its first draw. */
    fun register(input: TextView) {
        val ids = mutableSetOf<Int>()
        // AppCompat is the real default of TextInputEditText; the platform style covers OEM widgets.
        for (style in intArrayOf(androidx.appcompat.R.attr.editTextStyle, android.R.attr.editTextStyle)) {
            val values = input.context.obtainStyledAttributes(null, DRAWABLE_ATTRIBUTES, style, 0)
            try {
                for (index in DRAWABLE_ATTRIBUTES.indices) {
                    values.getResourceId(index, 0).takeIf { it != 0 }?.let(ids::add)
                }
            } finally {
                values.recycle()
            }
        }
        tintedResources.drawableIds = ids
    }

    private class InputResources(private val source: Resources, private val color: Int) :
        Resources(source.assets, source.displayMetrics, source.configuration) {

        var drawableIds: Set<Int> = emptySet()

        private fun tint(id: Int, drawable: Drawable): Drawable =
            if (id in drawableIds) drawable.mutate().apply { setTint(color) } else drawable

        @Deprecated("Deprecated in Java")
        override fun getDrawable(id: Int): Drawable = tint(id, source.getDrawable(id))
        override fun getDrawable(id: Int, theme: Theme?): Drawable = tint(id, source.getDrawable(id, theme))
        @Deprecated("Deprecated in Java")
        override fun getDrawableForDensity(id: Int, density: Int): Drawable? = source.getDrawableForDensity(id, density)?.let { tint(id, it) }
        override fun getDrawableForDensity(id: Int, density: Int, theme: Theme?): Drawable? =
            source.getDrawableForDensity(id, density, theme)?.let { tint(id, it) }

    }

    private companion object {
        val DRAWABLE_ATTRIBUTES = intArrayOf(
            android.R.attr.textCursorDrawable,
            android.R.attr.textSelectHandle,
            android.R.attr.textSelectHandleLeft,
            android.R.attr.textSelectHandleRight,
        )
    }

}
