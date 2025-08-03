package com.github.izhangzhihao.rainbow.brackets.lite.settings.form

import com.github.izhangzhihao.rainbow.brackets.lite.RainbowHighlighter
import com.github.izhangzhihao.rainbow.brackets.lite.util.RainbowLogger
import com.intellij.application.options.colors.*
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.EventDispatcher
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.awt.Color
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel
import javax.xml.parsers.DocumentBuilderFactory

class RainbowOptionsPanel(
        private val options: ColorAndFontOptions,
        private val schemesProvider: SchemesPanel,
        private val category: String
) : OptionsPanel {

    private lateinit var rootPanel: JPanel
    private lateinit var optionsTree: Tree

    private lateinit var rainbow: JBCheckBox

    private lateinit var colorLabel1: JLabel
    private lateinit var colorLabel2: JLabel
    private lateinit var colorLabel3: JLabel
    private lateinit var colorLabel4: JLabel
    private lateinit var colorLabel5: JLabel
    private lateinit var colorLabel6: JLabel
    private lateinit var colorLabel7: JLabel
    private lateinit var colorLabel8: JLabel
    private lateinit var colorLabel9: JLabel
    private lateinit var colorLabel10: JLabel

    private val colorLabels: Array<JLabel>

    private lateinit var color1: ColorPanel
    private lateinit var color2: ColorPanel
    private lateinit var color3: ColorPanel
    private lateinit var color4: ColorPanel
    private lateinit var color5: ColorPanel
    private lateinit var color6: ColorPanel
    private lateinit var color7: ColorPanel
    private lateinit var color8: ColorPanel
    private lateinit var color9: ColorPanel
    private lateinit var color10: ColorPanel

    private val colors: Array<ColorPanel>

    private lateinit var gradientLabel: JLabel

    private lateinit var importLabel: JLabel
    private lateinit var schemeComboBox: JComboBox<String>
    private lateinit var importButton: JButton

    private lateinit var numberOfColorsLabel: JLabel
    private lateinit var numberOfColorsField: JTextField

    private val properties: PropertiesComponent = PropertiesComponent.getInstance()
    private val eventDispatcher: EventDispatcher<ColorAndFontSettingsListener> =
            EventDispatcher.create(ColorAndFontSettingsListener::class.java)
    private var listenersInitialized = false

    init {
        colors = arrayOf(color1, color2, color3, color4, color5, color6, color7, color8, color9, color10)
        colorLabels = arrayOf(colorLabel1, colorLabel2, colorLabel3, colorLabel4, colorLabel5, colorLabel6, colorLabel7, colorLabel8, colorLabel9, colorLabel10)

        // Initialize import dropdown
        schemeComboBox.model = DefaultComboBoxModel(arrayOf(
            "Bright",
            "Darcula",
            "Default",
            "Rayman"
        ))
        
        val actionListener = ActionListener {
            eventDispatcher.multicaster.settingsChanged()
            options.stateChanged()
        }
        rainbow.addActionListener(actionListener)
        for (c in colors) {
            c.addActionListener(actionListener)
        }
        
        // Add import button listener
        importButton.addActionListener {
            importColorsFromScheme(schemeComboBox.selectedItem as String)
        }


        options.addListener(object : ColorAndFontSettingsListener.Abstract() {
            override fun settingsChanged() {
                if (!schemesProvider.areSchemesLoaded()) return
                if (optionsTree.selectedValue != null) {
                    // update options after global state change
                    processListValueChanged()
                }
            }
        })

        optionsTree.apply {
            isRootVisible = false
            model = DefaultTreeModel(DefaultMutableTreeTableNode())
            selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
            addTreeSelectionListener {
                if (schemesProvider.areSchemesLoaded()) {
                    processListValueChanged()
                }
            }
        }
    }

    override fun getPanel(): JPanel {
        // Initialize numberOfColorsField listeners after UI is created (only once)
        if (::numberOfColorsField.isInitialized && !listenersInitialized) {
            val actionListener = ActionListener {
                eventDispatcher.multicaster.settingsChanged()
                options.stateChanged()
            }
            numberOfColorsField.addActionListener(actionListener)
            numberOfColorsField.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = updateColorsBasedOnField()
                override fun removeUpdate(e: DocumentEvent?) = updateColorsBasedOnField()
                override fun changedUpdate(e: DocumentEvent?) = updateColorsBasedOnField()
            })
            listenersInitialized = true
        }
        return rootPanel
    }

    override fun addListener(listener: ColorAndFontSettingsListener) {
        eventDispatcher.addListener(listener)
    }

    override fun updateOptionsList() {
        fillOptionsList()
        processListValueChanged()
    }

    private data class DescriptionsNode(val rainbowName: String, val descriptions: List<TextAttributesDescription>) {
        override fun toString(): String = rainbowName
    }

    private fun fillOptionsList() {
        val nodes = options.currentDescriptions.asSequence()
                .filter { it is TextAttributesDescription && it.group == category }
                .map {
                    val description = it as TextAttributesDescription
                    val rainbowName = description.toString().split(":")[0]
                    rainbowName to description
                }
                .groupBy { it.first }
                .map { (rainbowName, descriptions) ->
                    DefaultMutableTreeNode(DescriptionsNode(rainbowName,
                            descriptions.asSequence().map { it.second }.toList().sortedBy { it.toString() }))
                }
        val root = DefaultMutableTreeNode()
        for (node in nodes) {
            root.add(node)
        }

        (optionsTree.model as DefaultTreeModel).setRoot(root)
    }

    private fun processListValueChanged() {
        var descriptionsNode = optionsTree.selectedDescriptions
        if (descriptionsNode == null) {
            properties.getValue(SELECTED_COLOR_OPTION_PROPERTY)?.let { preselected ->
                optionsTree.selectOptionByRainbowName(preselected)
                descriptionsNode = optionsTree.selectedDescriptions
            }
        }

        descriptionsNode?.run {
            properties.setValue(SELECTED_COLOR_OPTION_PROPERTY, rainbowName)
            reset(rainbowName, getActualColorsInScheme(rainbowName), descriptions)
        } ?: resetDefault()
    }

    private fun resetDefault() {
        RainbowLogger.debug(this) { "Resetting color panel to default state (no selection)" }
        rainbow.isEnabled = false
        rainbow.isSelected = false
        gradientLabel.text = "Assign each brackets its own color from the spectrum below:"
        
        if (::numberOfColorsField.isInitialized) {
            numberOfColorsField.isEnabled = false
            numberOfColorsField.text = ""
        }

        for (i in 0 until maxColors()) {
            colors[i].isEnabled = false
            colors[i].selectedColor = null
            colorLabels[i].isEnabled = false
        }
    }

    private fun reset(rainbowName: String, numColors: Int, descriptions: List<TextAttributesDescription>) {
        val rainbowOn = RainbowHighlighter.isRainbowEnabled(rainbowName)
        RainbowLogger.debug(this) { "Resetting panel for $rainbowName with $numColors colors, rainbow enabled: $rainbowOn" }

        rainbow.isEnabled = true
        rainbow.isSelected = rainbowOn
        gradientLabel.text = "Assign each ${rainbowName.lowercase()} its own color from the spectrum below:"
        
        if (::numberOfColorsField.isInitialized) {
            numberOfColorsField.isEnabled = true
            numberOfColorsField.text = numColors.toString()
        }

        for (i in 0 until maxColors()) {
            val shouldEnable = rainbowOn && i < numColors
            colors[i].isEnabled = shouldEnable
            colorLabels[i].isEnabled = shouldEnable
            colors[i].selectedColor = if (i < descriptions.size) descriptions[i].rainbowColor else null
            if (i < descriptions.size) {
                descriptions[i].let { eventDispatcher.multicaster.selectedOptionChanged(it) }
            }
        }
        RainbowLogger.debug(this) { "Color panel UI updated for $rainbowName" }
    }
    
    private fun updateColorsBasedOnField() {
        if (!::numberOfColorsField.isInitialized) return
        try {
            val numColors = numberOfColorsField.text.toIntOrNull()
            if (numColors != null && numColors in 1..10) {
                val rainbowOn = rainbow.isSelected
                for (i in 0 until maxColors()) {
                    val shouldEnable = rainbowOn && i < numColors
                    colors[i].isEnabled = shouldEnable
                    colorLabels[i].isEnabled = shouldEnable
                }
                // Don't automatically store the value here - only store when Apply is clicked
                eventDispatcher.multicaster.settingsChanged()
            }
        } catch (e: Exception) {
            // Ignore invalid input during typing
        }
    }

    // Check how many colors are stored for the current color scheme
    private fun getActualColorsInScheme(rainbowName: String): Int {
        return try {
            RainbowLogger.debug(this) { "Getting stored number of colors for scheme" }
            val scheme = options.selectedScheme
            val numberOfColorsKey = TextAttributesKey.createTextAttributesKey(NUMBER_OF_COLORS_KEY)
            
            // Try to get stored number of colors first
            val storedAttrs = scheme.getAttributes(numberOfColorsKey)
            val storedColor = storedAttrs?.foregroundColor
            
            if (storedColor != null) {
                val storedCount = storedColor.red // Decode the number from the red component
                if (storedCount in 1..10) {
                    RainbowLogger.info(this) { "Found stored color count $storedCount" }
                    return storedCount
                }
            }
            
            // Fallback to counting actual colors in scheme
            val keyPrefix = RainbowHighlighter.getBracketKeyFromName(rainbowName)
            var colorCount = 0
            for (i in 0 until 10) {
                val colorKey = TextAttributesKey.createTextAttributesKey("$keyPrefix$i")
                val attrs = scheme.getAttributes(colorKey)
                if (attrs?.foregroundColor != null) {
                    colorCount = i + 1
                } else {
                    RainbowLogger.debug(this) { "No color found for $keyPrefix$i, stopping search" }
                    break // Stop at first missing color
                }
            }
            
            // Default to 7 if no colors found
            if (colorCount == 0) {
                RainbowLogger.info(this) { "No colors found in scheme, defaulting to 7" }
                7
            } else {
                RainbowLogger.info(this) { "Found $colorCount colors in scheme" }
                colorCount
            }
        } catch (e: Exception) {
            RainbowLogger.error(this, "Error getting colors from scheme", e)
            7 // fallback
        }
    }
    
    private fun setNumberOfColorsInScheme(numberOfColors: Int) {
        try {
            RainbowLogger.debug(this) { "Storing number of colors $numberOfColors" }
            val scheme = options.selectedScheme
            val numberOfColorsKey = TextAttributesKey.createTextAttributesKey(NUMBER_OF_COLORS_KEY)
            
            // Encode the number of colors as a special color value (RGB = numberOfColors, 0, 0)
            val attrs = scheme.getAttributes(numberOfColorsKey)?.clone() ?: TextAttributes()
            attrs.foregroundColor = Color(numberOfColors, 0, 0)
            scheme.setAttributes(numberOfColorsKey, attrs)
            
            RainbowLogger.info(this) { "Stored color count $numberOfColors" }
        } catch (e: Exception) {
            RainbowLogger.error(this, "Error storing number of colors", e)
        }
    }

    private fun importColorsFromScheme(schemeName: String) {
        try {
            RainbowLogger.info(this) { "Importing colors from scheme: $schemeName" }
            val resourcePath = when (schemeName) {
                "Bright" -> "/colorSchemes/rainbow-color-bright.xml"
                "Darcula" -> "/colorSchemes/rainbow-color-default-darcula.xml"
                "Default" -> "/colorSchemes/rainbow-color-default.xml"
                "Rayman" -> "/colorSchemes/rainbow-color-rayman.xml"
                else -> {
                    RainbowLogger.warn(this) { "Unknown scheme name: $schemeName" }
                    return
                }
            }
            
            val colorsMap = parseColorSchemeXml(resourcePath)
            RainbowLogger.debug(this) { "Parsed colors from scheme: ${colorsMap.map { (k, v) -> "$k: ${v.size} colors" }}" }
            
            // Apply to all bracket types instead of just the selected one
            val allDescriptions = options.currentDescriptions.asSequence()
                .filter { it is TextAttributesDescription && it.group == category }
                .map { it as TextAttributesDescription }
                .groupBy { desc ->
                    val rainbowName = desc.toString().split(":")[0]
                    rainbowName
                }
            
            // Apply colors to all bracket types
            for ((rainbowName, descriptions) in allDescriptions) {
                when (rainbowName) {
                    RainbowHighlighter.NAME_ROUND_BRACKETS,
                    RainbowHighlighter.NAME_ANGLE_BRACKETS,
                    RainbowHighlighter.NAME_SQUARE_BRACKETS,
                    RainbowHighlighter.NAME_SQUIGGLY_BRACKETS -> {
                        RainbowLogger.debug(this) { "Applying imported colors to $rainbowName" }
                        applyImportedColors(rainbowName, descriptions.sortedBy { it.toString() }, colorsMap)
                    }
                }
            }
            
            // Calculate minimum number of colors across all bracket types
            val minColors = if (colorsMap.isNotEmpty()) {
                val counts = colorsMap.values.map { it.size }
                counts.minOrNull() ?: 7
            } else {
                7
            }
            
            RainbowLogger.info(this) { "Calculated minimum colors across all bracket types: $minColors" }
            
            // Store the minimum number of colors for the scheme
            setNumberOfColorsInScheme(minColors)

            // Refresh the UI - if something is selected, update it
            optionsTree.selectedDescriptions?.let { (rainbowName, descriptions) ->
                RainbowLogger.debug(this) { "Refreshing UI for $rainbowName with $minColors colors" }
                reset(rainbowName, minColors, descriptions)
            }
            
            // Fire settings changed to update everything
            eventDispatcher.multicaster.settingsChanged()
            options.stateChanged()
            
            RainbowLogger.info(this) { "Successfully imported colors from $schemeName scheme" }
        } catch (e: Exception) {
            // Handle error silently or show a notification
            RainbowLogger.error(this, "Error importing colors from scheme: $schemeName", e)
            e.printStackTrace()
        }
    }
    
    private fun parseColorSchemeXml(resourcePath: String): Map<String, List<Color>> {
        RainbowLogger.debug(this) { "Parsing color scheme XML from: $resourcePath" }
        val inputStream = javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")
        
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(inputStream)
        
        val colorsMapByType = mutableMapOf<String, MutableList<Color>>()

        // Initialize empty lists for each bracket type
        colorsMapByType[RainbowHighlighter.KEY_ROUND_BRACKETS] = mutableListOf()
        colorsMapByType[RainbowHighlighter.KEY_SQUARE_BRACKETS] = mutableListOf()
        colorsMapByType[RainbowHighlighter.KEY_SQUIGGLY_BRACKETS] = mutableListOf()
        colorsMapByType[RainbowHighlighter.KEY_ANGLE_BRACKETS] = mutableListOf()

        val options: NodeList = document.getElementsByTagName("option")
        RainbowLogger.debug(this) { "Found ${options.length} option elements in XML" }
        
        for (i in 0 until options.length) {
            val option = options.item(i) as Element
            val name = option.getAttribute("name")
            
            // Extract the bracket type and index from the name
            val bracketType = when {
                name.startsWith("ROUND_BRACKETS_RAINBOW_COLOR") -> RainbowHighlighter.KEY_ROUND_BRACKETS
                name.startsWith("SQUARE_BRACKETS_RAINBOW_COLOR") -> RainbowHighlighter.KEY_SQUARE_BRACKETS
                name.startsWith("SQUIGGLY_BRACKETS_RAINBOW_COLOR") -> RainbowHighlighter.KEY_SQUIGGLY_BRACKETS
                name.startsWith("ANGLE_BRACKETS_RAINBOW_COLOR") -> RainbowHighlighter.KEY_ANGLE_BRACKETS
                else -> null
            }
            
            if (bracketType != null) {
                val valueElement = option.getElementsByTagName("value").item(0) as? Element
                val foregroundElement = valueElement?.getElementsByTagName("option")?.let { opts ->
                    for (j in 0 until opts.length) {
                        val opt = opts.item(j) as Element
                        if (opt.getAttribute("name") == "FOREGROUND") {
                            return@let opt
                        }
                    }
                    null
                }
                
                foregroundElement?.getAttribute("value")?.let { colorValue ->
                    try {
                        val color = Color(Integer.parseInt(colorValue, 16))
                        colorsMapByType[bracketType]?.add(color)
                    } catch (e: NumberFormatException) {
                        // Skip invalid color values
                    }
                }
            }
        }
        
        // Convert to immutable lists
        return colorsMapByType.mapValues { (_, colors) -> colors.toList() }
    }
    
    private fun applyImportedColors(
        rainbowName: String, 
        descriptions: List<TextAttributesDescription>,
        colorsMap: Map<String, List<Color>>
    ) {
        colorsMap[RainbowHighlighter.getBracketKeyFromName(rainbowName)]?.forEachIndexed { i, color ->
            if (i < minOf(descriptions.size, colors.size)) {
                colors[i].selectedColor = color
                descriptions[i].rainbowColor = color
            }
        }
    }

    override fun applyChangesToScheme() {
        val scheme = options.selectedScheme
        val (rainbowName, descriptions) = optionsTree.selectedDescriptions ?: return
        when (rainbowName) {
            RainbowHighlighter.NAME_ROUND_BRACKETS,
            RainbowHighlighter.NAME_ANGLE_BRACKETS,
            RainbowHighlighter.NAME_SQUARE_BRACKETS,
            RainbowHighlighter.NAME_SQUIGGLY_BRACKETS -> {
                RainbowLogger.info(this) { "Applying changes to scheme for $rainbowName, enabled: ${rainbow.isSelected}" }
                RainbowHighlighter.setRainbowEnabled(rainbowName, rainbow.isSelected)

                // Save number of colors if valid
                if (::numberOfColorsField.isInitialized) {
                    try {
                        val numberOfColors = numberOfColorsField.text.toIntOrNull()
                        if (numberOfColors != null && numberOfColors in 1..10) {
                            setNumberOfColorsInScheme(numberOfColors)
                        }
                    } catch (e: Exception) {
                        RainbowLogger.warn(this) { "Invalid number of colors input: ${numberOfColorsField.text}" }
                    }
                }

                for (i in 0 until maxColors()) {
                    colors[i].selectedColor?.let { color ->
                        if (i < descriptions.size) {
                            RainbowLogger.debug(this) { "Setting color at index $i for $rainbowName: ${color.rgb}" }
                            descriptions[i].rainbowColor = color
                            descriptions[i].apply(scheme)
                        }
                    }
                }
            }
        }
    }

    private fun maxColors() = 10

    override fun processListOptions(): MutableSet<String> = mutableSetOf(
            RainbowHighlighter.NAME_ROUND_BRACKETS,
            RainbowHighlighter.NAME_SQUARE_BRACKETS,
            RainbowHighlighter.NAME_SQUIGGLY_BRACKETS,
            RainbowHighlighter.NAME_ANGLE_BRACKETS
    )

    override fun showOption(option: String): Runnable? = Runnable {
        optionsTree.selectOptionByRainbowName(option)
    }

    override fun selectOption(typeToSelect: String) {
        optionsTree.selectOptionByType(typeToSelect)
    }

    companion object {
        private const val SELECTED_COLOR_OPTION_PROPERTY = "rainbow.lite.selected.color.option.name"
        private const val NUMBER_OF_COLORS_KEY = "RAINBOW_NUMBER_OF_COLORS"

        private var TextAttributesDescription.rainbowColor: Color?
            get() = externalForeground
            set(value) {
                externalForeground = value
            }

        private val Tree.selectedValue: Any?
            get() = (lastSelectedPathComponent as? DefaultMutableTreeNode)?.userObject

        private val Tree.selectedDescriptions: DescriptionsNode?
            get() = selectedValue as? DescriptionsNode

        private fun Tree.findOption(nodeObject: Any, matcher: (Any) -> Boolean): TreePath? {
            val model = model as DefaultTreeModel
            for (i in 0 until model.getChildCount(nodeObject)) {
                val childObject = model.getChild(nodeObject, i)
                if (childObject is DefaultMutableTreeNode) {
                    val data = childObject.userObject
                    if (matcher(data)) {
                        return TreePath(model.getPathToRoot(childObject))
                    }
                }

                val pathInChild = findOption(childObject, matcher)
                if (pathInChild != null) return pathInChild
            }

            return null
        }

        private fun Tree.selectOptionByRainbowName(rainbowName: String) {
            selectPath(findOption(model.root) { data ->
                data is DescriptionsNode
                        && rainbowName.isNotBlank()
                        && data.rainbowName.contains(rainbowName, ignoreCase = true)
            })
        }

        private fun Tree.selectOptionByType(attributeType: String) {
            selectPath(findOption(model.root) { data ->
                data is DescriptionsNode && data.descriptions.any { it.type == attributeType }
            })
        }

        private fun Tree.selectPath(path: TreePath?) {
            if (path != null) {
                selectionPath = path
                scrollPathToVisible(path)
            }
        }
    }
}

private fun <E> List<E>.indexOfOrNull(idx: Int): E? = if (idx < this.size) this[idx] else null