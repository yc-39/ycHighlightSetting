package yc39.ycHighlightSetting

import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingSettingsPerFile
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.startup.StartupActivity
import com.intellij.psi.PsiManager


class SetSyntaxHighlightingStartup : StartupActivity {

    override fun runActivity(project: Project) {
        // Handle files already open at startup
        applyHighlightingToOpenFiles(project)

        // Listen for newly opened files
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: com.intellij.openapi.vfs.VirtualFile) {
                    if (!file.isDirectory && file.extension in setOf("java", "kt", "xml")) {
                        println("fileOpened: $file")
                        val psiFile = PsiManager.getInstance(project).findFile(file)
                        if (psiFile != null) {
                            val instance = HighlightingSettingsPerFile.getInstance(project)
                            instance.setHighlightingSettingForRoot(psiFile, FileHighlightingSetting.SKIP_INSPECTION)
                        }
                    }
                }
            }
        )
    }

    private fun applyHighlightingToOpenFiles(project: Project) {
        val instance = HighlightingSettingsPerFile.getInstance(project)
        val openFiles = FileEditorManager.getInstance(project).openFiles

        for (file in openFiles) {
            if (!file.isDirectory && file.extension in setOf("java", "kt", "xml")) {
                println("fileOpened: $file")
                val psiFile = PsiManager.getInstance(project).findFile(file)
                if (psiFile != null) {
                    instance.setHighlightingSettingForRoot(psiFile, FileHighlightingSetting.SKIP_INSPECTION)
                }
            }
        }
    }

//    override fun runActivity(project: Project) {
//        val instance = HighlightingSettingsPerFile.getInstance(project)
//        ProjectRootManager.getInstance(project).fileIndex.iterateContent { file ->
//            if (!file.isDirectory && file.extension in setOf("java", "kt", "xml")) {
//                val psiFile = PsiManager.getInstance(project).findFile(file)
//                if (psiFile != null) {
//                    instance.setHighlightingSettingForRoot(psiFile, FileHighlightingSetting.SKIP_INSPECTION)
//                }
//            }
//            true
//        }
//    }


}
