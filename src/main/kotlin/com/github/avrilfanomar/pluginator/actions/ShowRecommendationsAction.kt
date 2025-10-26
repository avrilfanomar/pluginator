package com.github.avrilfanomar.pluginator.actions

import com.github.avrilfanomar.pluginator.ui.RecommendationDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Action that opens the Plugin Recommendations dialog.
 *
 * This action can be triggered from the Tools menu or assigned a keyboard shortcut.
 */
class ShowRecommendationsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val dialog = RecommendationDialog(project)
        dialog.show()
    }

    override fun update(e: AnActionEvent) {
        // Action is always enabled
        e.presentation.isEnabled = true
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
