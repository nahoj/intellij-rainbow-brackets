package com.github.izhangzhihao.rainbow.brackets.lite

import com.github.izhangzhihao.rainbow.brackets.lite.settings.RainbowSettings
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesScheme
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.TestOnly
import java.awt.Color
import java.awt.Font

object RainbowHighlighter {

    const val NAME_ROUND_BRACKETS = "Round Brackets"
    const val NAME_SQUARE_BRACKETS = "Square Brackets"
    const val NAME_SQUIGGLY_BRACKETS = "Squiggly Brackets"
    const val NAME_ANGLE_BRACKETS = "Angle Brackets"

    private const val KEY_ROUND_BRACKETS = "ROUND_BRACKETS_RAINBOW_COLOR"
    private const val KEY_SQUARE_BRACKETS = "SQUARE_BRACKETS_RAINBOW_COLOR"
    private const val KEY_SQUIGGLY_BRACKETS = "SQUIGGLY_BRACKETS_RAINBOW_COLOR"
    private const val KEY_ANGLE_BRACKETS = "ANGLE_BRACKETS_RAINBOW_COLOR"

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
        val currentColorCount = settings.numberOfColors
        
        // If color count changed or keys not yet initialized, create them
        if (lastColorCount != currentColorCount || roundBracketsRainbowColorKeys == null) {
            roundBracketsRainbowColorKeys = createRainbowAttributesKeys(KEY_ROUND_BRACKETS, currentColorCount)
            squareBracketsRainbowColorKeys = createRainbowAttributesKeys(KEY_SQUARE_BRACKETS, currentColorCount)
            squigglyBracketsRainbowColorKeys = createRainbowAttributesKeys(KEY_SQUIGGLY_BRACKETS, currentColorCount)
            angleBracketsRainbowColorKeys = createRainbowAttributesKeys(KEY_ANGLE_BRACKETS, currentColorCount)
            lastColorCount = currentColorCount
        }
    }

    fun getRainbowAttributesKeys(rainbowName: String): Array<TextAttributesKey> {
        ensureColorKeysCreated()
        
        return when (rainbowName) {
            NAME_ROUND_BRACKETS -> roundBracketsRainbowColorKeys!!
            NAME_SQUARE_BRACKETS -> squareBracketsRainbowColorKeys!!
            NAME_SQUIGGLY_BRACKETS -> squigglyBracketsRainbowColorKeys!!
            NAME_ANGLE_BRACKETS -> angleBracketsRainbowColorKeys!!
            else -> throw IllegalArgumentException("Unknown rainbow name: $rainbowName")
        }
    }

    // FIXME: Meta properties(SchemeMetaInfo) should be used.
    fun isRainbowEnabled(rainbowName: String): Boolean {
        return when (rainbowName) {
            NAME_ROUND_BRACKETS -> settings.isEnableRainbowRoundBrackets
            NAME_SQUARE_BRACKETS -> settings.isEnableRainbowSquareBrackets
            NAME_SQUIGGLY_BRACKETS -> settings.isEnableRainbowSquigglyBrackets
            NAME_ANGLE_BRACKETS -> settings.isEnableRainbowAngleBrackets
            else -> throw IllegalArgumentException("Unknown rainbow name: $rainbowName")
        }
    }

    // FIXME: Meta properties(SchemeMetaInfo) should be used.
    fun setRainbowEnabled(rainbowName: String, enabled: Boolean) {
        when (rainbowName) {
            NAME_ROUND_BRACKETS -> settings.isEnableRainbowRoundBrackets = enabled
            NAME_SQUARE_BRACKETS -> settings.isEnableRainbowSquareBrackets = enabled
            NAME_SQUIGGLY_BRACKETS -> settings.isEnableRainbowSquigglyBrackets = enabled
            NAME_ANGLE_BRACKETS -> settings.isEnableRainbowAngleBrackets = enabled
            else -> throw IllegalArgumentException("Unknown rainbow name: $rainbowName")
        }
    }

    fun getRainbowColorByLevel(rainbowName: String, level: Int): TextAttributesKey? {
        val ind = level % settings.numberOfColors
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

        return HighlightInfo
            .newHighlightInfo(rainbowElement)
            .textAttributes(attributes)
            .range(element)
            .create()
    }
}
