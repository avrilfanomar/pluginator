package com.github.avrilfanomar.pluginator.ui

import com.github.avrilfanomar.pluginator.api.Plugin
import com.github.avrilfanomar.pluginator.api.Recommendation
import com.github.avrilfanomar.pluginator.service.PluginatorService
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.util.*
import javax.swing.Action
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Dialog for displaying plugin recommendations to the user.
 *
 * This dialog fetches recommendations from the Pluginator service and displays them
 * in a user-friendly format. Users can accept (install) or reject each recommendation.
 */
class RecommendationDialog(private val project: Project?) : DialogWrapper(project) {
    private val logger = Logger.getInstance(RecommendationDialog::class.java)
    private val service = PluginatorService.getInstance()

    private val mainPanel = JPanel(BorderLayout())
    private val loadingLabel = JBLabel("Loading recommendations...")
    private val errorLabel = JBLabel()
    private val recommendationsPanel = JPanel()
    private val scrollPane = JBScrollPane(recommendationsPanel)

    private var currentRecommendation: Recommendation? = null
    private val userUuid = getUserUuid()

    init {
        title = "Plugin Recommendations"
        init()
        loadRecommendations()
    }

    override fun createCenterPanel(): JComponent {
        mainPanel.preferredSize = Dimension(600, 400)
        mainPanel.border = JBUI.Borders.empty(10)

        // Initial loading state
        loadingLabel.horizontalAlignment = SwingConstants.CENTER
        mainPanel.add(loadingLabel, BorderLayout.CENTER)

        // Error label styling
        errorLabel.horizontalAlignment = SwingConstants.CENTER
        errorLabel.isVisible = false

        return mainPanel
    }

    /**
     * Loads recommendations from the Pluginator service.
     */
    private fun loadRecommendations() {
        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(project, "Loading plugin recommendations", false) {
                var recommendations: Recommendation? = null

                override fun run(indicator: ProgressIndicator) {
                    indicator.text = "Fetching recommendations from Pluginator server..."

                    try {
                        recommendations = runBlocking {
                            val appInfo = ApplicationInfo.getInstance()
                            val installedPlugins = getInstalledPlugins()

                            service.getRecommendations(
                                userUuid = userUuid,
                                ideName = appInfo.fullApplicationName,
                                ideVersion = appInfo.fullVersion,
                                os = System.getProperty("os.name"),
                                installedPlugins = installedPlugins
                            )
                        }
                    } catch (e: Exception) {
                        logger.error("Error loading recommendations", e)
                    }
                }

                override fun onSuccess() {
                    if (recommendations != null && recommendations!!.plugins.isNotEmpty()) {
                        currentRecommendation = recommendations
                        displayRecommendations(recommendations!!)
                    } else {
                        showError("No recommendations available at this time.")
                    }
                }

                override fun onThrowable(error: Throwable) {
                    logger.error("Error loading recommendations", error)
                    showError("Failed to load recommendations: ${error.message}")
                }
            })
    }

    /**
     * Displays the recommendations in the UI.
     */
    private fun displayRecommendations(recommendation: Recommendation) {
        mainPanel.removeAll()

        recommendationsPanel.layout = GridBagLayout()
        recommendationsPanel.removeAll()

        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.NORTH
            insets = JBUI.insets(5)
        }

        // Header
        val headerLabel = JBLabel("We found ${recommendation.plugins.size} plugin recommendations for you:")
        headerLabel.border = JBUI.Borders.emptyBottom(10)
        headerLabel.font = headerLabel.font.deriveFont(14f)
        recommendationsPanel.add(headerLabel, gbc)

        // Add each recommendation
        recommendation.plugins.forEachIndexed { index, pluginName ->
            gbc.gridy = index + 1
            val pluginPanel = createPluginRecommendationPanel(pluginName)
            recommendationsPanel.add(pluginPanel, gbc)
        }

        // Add a filler component to push everything to the top
        gbc.gridy++
        gbc.weighty = 1.0
        recommendationsPanel.add(Box.createVerticalGlue(), gbc)

        scrollPane.border = null
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        mainPanel.revalidate()
        mainPanel.repaint()
    }

    /**
     * Creates a panel for a single plugin recommendation.
     */
    private fun createPluginRecommendationPanel(pluginName: String): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.compound(
            JBUI.Borders.empty(5),
            JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 1)
        )

        // Plugin name label
        val nameLabel = JBLabel(pluginName)
        nameLabel.border = JBUI.Borders.empty(10)
        nameLabel.font = nameLabel.font.deriveFont(13f)

        // Button panel
        val buttonPanel = JPanel()
        buttonPanel.border = JBUI.Borders.empty(5)

        val acceptButton = JButton("Install")
        acceptButton.addActionListener {
            handleAccept(pluginName, panel)
        }

        val rejectButton = JButton("Dismiss")
        rejectButton.addActionListener {
            handleReject(pluginName, panel)
        }

        buttonPanel.add(acceptButton)
        buttonPanel.add(rejectButton)

        panel.add(nameLabel, BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.EAST)

        return panel
    }

    /**
     * Handles accepting (installing) a plugin recommendation.
     */
    private fun handleAccept(pluginName: String, panel: JPanel) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // Send feedback to the server
                val success = runBlocking {
                    service.updateRecommendation(
                        plugin = pluginName,
                        userUuid = userUuid,
                        accepted = true
                    )
                }

                ApplicationManager.getApplication().invokeLater {
                    if (success) {
                        // Update UI to show accepted state
                        updatePanelState(panel, pluginName, "Accepted - Please install from Plugin Marketplace", true)

                        // Optionally, open the plugin marketplace
                        val result = JOptionPane.showConfirmDialog(
                            panel,
                            "Would you like to open the Plugin Marketplace to install \"$pluginName\"?",
                            "Install Plugin",
                            JOptionPane.YES_NO_OPTION
                        )

                        if (result == JOptionPane.YES_OPTION) {
                            openPluginMarketplace(pluginName)
                        }
                    } else {
                        JOptionPane.showMessageDialog(
                            panel,
                            "Failed to send feedback to server",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Error accepting recommendation", e)
                ApplicationManager.getApplication().invokeLater {
                    JOptionPane.showMessageDialog(
                        panel,
                        "Error: ${e.message}",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }

    /**
     * Handles rejecting a plugin recommendation.
     */
    private fun handleReject(pluginName: String, panel: JPanel) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val success = runBlocking {
                    service.updateRecommendation(
                        plugin = pluginName,
                        userUuid = userUuid,
                        accepted = false
                    )
                }

                ApplicationManager.getApplication().invokeLater {
                    if (success) {
                        updatePanelState(panel, pluginName, "Dismissed", false)
                    } else {
                        JOptionPane.showMessageDialog(
                            panel,
                            "Failed to send feedback to server",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Error rejecting recommendation", e)
                ApplicationManager.getApplication().invokeLater {
                    JOptionPane.showMessageDialog(
                        panel,
                        "Error: ${e.message}",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }

    /**
     * Updates the panel state after user action.
     */
    private fun updatePanelState(panel: JPanel, pluginName: String, message: String, accepted: Boolean) {
        panel.removeAll()

        val nameLabel = JBLabel("$pluginName - $message")
        nameLabel.border = JBUI.Borders.empty(10)
        nameLabel.font = nameLabel.font.deriveFont(13f)

        panel.add(nameLabel, BorderLayout.CENTER)
        panel.revalidate()
        panel.repaint()
    }

    /**
     * Opens the plugin marketplace for the specified plugin.
     */
    private fun openPluginMarketplace(pluginName: String) {
        try {
            val pluginIds = mutableSetOf(PluginId.getId(pluginName))
            com.intellij.ide.plugins.PluginManagerConfigurable.showPluginConfigurable(project, pluginIds)
        } catch (e: Exception) {
            logger.warn("Could not open plugin marketplace", e)
            JOptionPane.showMessageDialog(
                mainPanel,
                "Please manually search for \"$pluginName\" in Settings > Plugins",
                "Plugin Marketplace",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    /**
     * Shows an error message in the dialog.
     */
    private fun showError(message: String) {
        mainPanel.removeAll()
        errorLabel.text = message
        errorLabel.isVisible = true
        mainPanel.add(errorLabel, BorderLayout.CENTER)
        mainPanel.revalidate()
        mainPanel.repaint()
    }

    /**
     * Gets the list of currently installed plugins.
     */
    private fun getInstalledPlugins(): List<Plugin> {
        return PluginManagerCore.plugins
            .filter { it.isEnabled }
            .map { Plugin(it.pluginId.idString, it.version) }
    }

    /**
     * Gets or generates a UUID for the current user.
     */
    private fun getUserUuid(): String {
        // In a real implementation, you might want to persist this in settings
        // For now, generate a random UUID per session
        return UUID.randomUUID().toString()
    }

    override fun createActions(): Array<Action> {
        return arrayOf(cancelAction)
    }
}
