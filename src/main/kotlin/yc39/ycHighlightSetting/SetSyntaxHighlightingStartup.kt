package yc39.ycHighlightSetting

import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingSettingsPerFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager


class SetSyntaxHighlightingStartup : StartupActivity {

    override fun runActivity(project: Project) {
        // Handle files already open at startup
        applyHighlightingToOpenFiles(project)

        // Listen for newly opened files
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    applyHighlighting(source.project, file)
                }
            }
        )
    }

    private fun applyHighlightingToOpenFiles(project: Project) {
        val openFiles = FileEditorManager.getInstance(project).openFiles
        for (file in openFiles) {
            applyHighlighting(project, file)
        }
    }

    /**
     * setHighlightingSettingForRoot() fires a synchronous message bus event that runs a write
     * action (DaemonListeners -> dropPsiCaches). It must be called from a write-safe context,
     * otherwise TransactionGuard throws "Write-unsafe context!". Defer via invokeLater.
     */
    private fun applyHighlighting(project: Project, file: VirtualFile) {
        if (file.isDirectory || file.extension !in TARGET_EXTENSIONS) return
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@invokeLater
            val instance = HighlightingSettingsPerFile.getInstance(project)
            instance.setHighlightingSettingForRoot(psiFile, FileHighlightingSetting.SKIP_INSPECTION)
        }
    }

    companion object {
        private val TARGET_EXTENSIONS = setOf("java", "kt", "xml")
    }
}
