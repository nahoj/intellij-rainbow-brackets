package com.github.izhangzhihao.rainbow.brackets.lite

import com.github.izhangzhihao.rainbow.brackets.lite.settings.RainbowSettings
import com.github.izhangzhihao.rainbow.brackets.lite.util.RainbowLogger
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesScheme
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.TestOnly
import java.awt.Color
import java.awt.Font

object RainbowHighlighter {

    const val NAME_ROUND_BRACKETS = "Round Brackets"
    const val NAME_SQUARE_BRACKETS = "Square Brackets"
    const val NAME_SQUIGGLY_BRACKETS = "Squiggly Brackets"
    const val NAME_ANGLE_BRACKETS = "Angle Brackets"

    const val KEY_ROUND_BRACKETS = "ROUND_BRACKETS_RAINBOW_COLOR"
    const val KEY_SQUARE_BRACKETS = "SQUARE_BRACKETS_RAINBOW_COLOR"
    const val KEY_SQUIGGLY_BRACKETS = "SQUIGGLY_BRACKETS_RAINBOW_COLOR"
    const val KEY_ANGLE_BRACKETS = "ANGLE_BRACKETS_RAINBOW_COLOR"

    fun getBracketKeyFromName(rainbowName: String): String {
        return when (rainbowName) {
            NAME_ROUND_BRACKETS -> KEY_ROUND_BRACKETS
            NAME_SQUARE_BRACKETS -> KEY_SQUARE_BRACKETS
            NAME_SQUIGGLY_BRACKETS -> KEY_SQUIGGLY_BRACKETS
            NAME_ANGLE_BRACKETS -> KEY_ANGLE_BRACKETS
            else -> throw IllegalArgumentException("Unknown bracket type: $rainbowName")
        }
    }

    private val roundBrackets: CharArray = charArrayOf('(', ')')
    private val squareBrackets: CharArray = charArrayOf('[', ']')
    private val squigglyBrackets: CharArray = charArrayOf('{', '}')
    private val angleBrackets: CharArray = charArrayOf('<', '>')

    private val settings by lazy { RainbowSettings.instance }
    
    // Cache for color keys, will be lazily created when needed
    private var roundBracketsRainbowColorKeys: Array<TextAttributesKey>? = null
    private var squareBracketsRainbowColorKeys: Array<TextAttributesKey>? = null
    private var squigglyBracketsRainbowColorKeys: Array<TextAttributesKey>? = null
    private var angleBracketsRainbowColorKeys: Array<TextAttributesKey>? = null
    
    // Keep track of the number of colors when keys were created
    private var lastColorCount: Int = 0

    private val rainbowElement: HighlightInfoType = HighlightInfoType
            .HighlightInfoTypeImpl(HighlightSeverity.INFORMATION, DefaultLanguageHighlighterColors.CONSTANT)

    private val PsiElement.isRoundBracket get() = roundBrackets.any { textContains(it) }
    private val PsiElement.isSquareBracket get() = squareBrackets.any { textContains(it) }
    private val PsiElement.isSquigglyBracket get() = squigglyBrackets.any { textContains(it) }
    private val PsiElement.isAngleBracket get() = angleBrackets.any { textContains(it) }

    private fun createRainbowAttributesKeys(keyName: String, size: Int): Array<TextAttributesKey> {
        return generateSequence(0) { it + 1 }
                .map { TextAttributesKey.createTextAttributesKey("$keyName$it") }
                .take(size)
                .toList()
                .toTypedArray()
    }
    
    // Initialize or update color keys as needed
    private fun ensureColorKeysCreated() {
        val currentColorCount = getActualColorCount()
        
        // If color count changed or keys not yet initialized, create them
        if (lastColorCount != currentColorCount || roundBracketsRainbowColorKeys == null) {
            RainbowLogger.info(this) { "Initializing color keys with $currentColorCount colors" }
            roundBracketsRainbowColorKeys = createRainbowAttributesKeys(KEY_ROUND_BRACKETS, currentColorCount)
            squareBracketsRainbowColorKeys = createRainbowAttributesKeys(KEY_SQUARE_BRACKETS, currentColorCount)
            squigglyBracketsRainbowColorKeys = createRainbowAttributesKeys(KEY_SQUIGGLY_BRACKETS, currentColorCount)
            angleBracketsRainbowColorKeys = createRainbowAttributesKeys(KEY_ANGLE_BRACKETS, currentColorCount)
            lastColorCount = currentColorCount
        }
    }

    // Determine the actual number of colors available in the current color scheme
    private fun getActualColorCount(): Int {
        return try {
            // Try to get color scheme and count actual defined colors
            val schemeManager = EditorColorsManager.getInstance()
            val currentScheme = schemeManager.globalScheme
            
            // Count how many rainbow colors are actually defined in the scheme
            var colorCount = 0
            for (i in 0 until 10) { // Check up to 10 colors
                val colorKey = TextAttributesKey.createTextAttributesKey("$KEY_ROUND_BRACKETS$i")
                val attrs = currentScheme.getAttributes(colorKey)
                if (attrs?.foregroundColor != null) {
                    colorCount = i + 1
                }
            }
            
            // Default to 7 if no colors found or fallback needed
            if (colorCount == 0) {
                RainbowLogger.warn(this) { "No rainbow colors found in scheme, defaulting to 7" }
                7
            } else {
                colorCount
            }
        } catch (e: Exception) {
            // Fallback to 7 colors if there's any issue
            RainbowLogger.warn(this, "Error determining color count, defaulting to 7", e)
            7
        }
    }

    fun getRainbowAttributesKeys(rainbowName: String): Array<TextAttributesKey> {
        ensureColorKeysCreated()
        
        return when (rainbowName) {
            NAME_ROUND_BRACKETS -> roundBracketsRainbowColorKeys!!
            NAME_SQUARE_BRACKETS -> squareBracketsRainbowColorKeys!!
            NAME_SQUIGGLY_BRACKETS -> squigglyBracketsRainbowColorKeys!!
            NAME_ANGLE_BRACKETS -> angleBracketsRainbowColorKeys!!
            else -> {
                RainbowLogger.error(this) { "Unknown rainbow name: $rainbowName" }
                throw IllegalArgumentException("Unknown rainbow name: $rainbowName")
            }
        }
    }

    // FIXME: Meta properties(SchemeMetaInfo) should be used.
    fun isRainbowEnabled(rainbowName: String): Boolean {
        return when (rainbowName) {
            NAME_ROUND_BRACKETS -> settings.isEnableRainbowRoundBrackets
            NAME_SQUARE_BRACKETS -> settings.isEnableRainbowSquareBrackets
            NAME_SQUIGGLY_BRACKETS -> settings.isEnableRainbowSquigglyBrackets
            NAME_ANGLE_BRACKETS -> settings.isEnableRainbowAngleBrackets
            else -> {
                RainbowLogger.error(this) { "Unknown rainbow name when checking enabled state: $rainbowName" }
                throw IllegalArgumentException("Unknown rainbow name: $rainbowName")
            }
        }
    }

    // FIXME: Meta properties(SchemeMetaInfo) should be used.
    fun setRainbowEnabled(rainbowName: String, enabled: Boolean) {
        RainbowLogger.debug(this) { "Setting $rainbowName enabled state to $enabled" }
        when (rainbowName) {
            NAME_ROUND_BRACKETS -> settings.isEnableRainbowRoundBrackets = enabled
            NAME_SQUARE_BRACKETS -> settings.isEnableRainbowSquareBrackets = enabled
            NAME_SQUIGGLY_BRACKETS -> settings.isEnableRainbowSquigglyBrackets = enabled
            NAME_ANGLE_BRACKETS -> settings.isEnableRainbowAngleBrackets = enabled
            else -> {
                RainbowLogger.error(this) { "Unknown rainbow name when setting enabled state: $rainbowName" }
                throw IllegalArgumentException("Unknown rainbow name: $rainbowName")
            }
        }
    }

    fun getRainbowColorByLevel(rainbowName: String, level: Int): TextAttributesKey? {
        val ind = level % getActualColorCount()
        return getRainbowAttributesKeys(rainbowName).getOrNull(ind)
    }

    @TestOnly
    fun getBrackets(): CharArray = roundBrackets + squareBrackets + squigglyBrackets + angleBrackets

    @TestOnly
    fun getRainbowColor(rainbowName: String, level: Int): Color? {
        return getRainbowColorByLevel(rainbowName, level)?.defaultAttributes?.foregroundColor
    }

    private fun getTextAttributes(
        element: PsiElement,
        level: Int
    ): TextAttributesKey? {
        if (!settings.isRainbowEnabled) {
            return null
        }

        val rainbowName = when {
            element.isRoundBracket -> if (settings.isEnableRainbowRoundBrackets) NAME_ROUND_BRACKETS else null
            element.isSquareBracket -> if (settings.isEnableRainbowSquareBrackets) NAME_SQUARE_BRACKETS else null
            element.isSquigglyBracket -> if (settings.isEnableRainbowSquigglyBrackets) NAME_SQUIGGLY_BRACKETS else null
            element.isAngleBracket -> if (settings.isEnableRainbowAngleBrackets) NAME_ANGLE_BRACKETS else null
            else -> NAME_ROUND_BRACKETS
        } ?: return null

        return getRainbowColorByLevel(rainbowName, level)
    }

    fun getHighlightInfo(colorsScheme: TextAttributesScheme, element: PsiElement, level: Int): HighlightInfo? {
        val textAttributesKey = getTextAttributes(element, level) ?: return null

        var attributes = colorsScheme.getAttributes(textAttributesKey)

        if (settings.isDisplayBracketsInBold) {
            attributes = attributes.clone()
            attributes.fontType = attributes.fontType or Font.BOLD
        }

        val info = HighlightInfo
            .newHighlightInfo(rainbowElement)
            .textAttributes(attributes)
            .range(element)
            .create()
            
        RainbowLogger.trace(this) { "Created highlight info for ${element.text} at level $level" }
        return info
    }
}
