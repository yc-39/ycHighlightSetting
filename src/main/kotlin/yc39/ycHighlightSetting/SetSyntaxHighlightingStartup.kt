package yc39.ycHighlightSetting

import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingSettingsPerFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean


class SetSyntaxHighlightingStartup : StartupActivity {

    private val pendingFiles = ConcurrentLinkedQueue<VirtualFile>()
    private val flushScheduled = AtomicBoolean(false)

    override fun runActivity(project: Project) {
        // Handle files already open at startup. Wait for smart mode so the batched write
        // action below does not queue behind indexing write actions on the EDT.
        DumbService.getInstance(project).runWhenSmart {
            for (file in FileEditorManager.getInstance(project).openFiles) {
                queueFile(project, file)
            }
        }

        // Listen for newly opened files
        project.messageBus.connect(project).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    queueFile(source.project, file)
                }
            }
        )
    }

    /**
     * setHighlightingSettingForRoot() fires a synchronous message bus event that runs a write
     * action (DaemonListeners -> dropPsiCaches), so it must run on the EDT in a write-safe
     * context. Calling it once per file means one write-lock acquisition per file, and each
     * acquisition can park the EDT behind background write actions (indexing, VFS refresh, ...),
     * causing an EDT freeze. Instead, queue the files and apply all of them inside a single
     * write action: nested write actions re-enter the lock without re-acquiring it, so the EDT
     * waits for the write lock at most once per batch.
     */
    private fun queueFile(project: Project, file: VirtualFile) {
        if (file.isDirectory || file.extension !in TARGET_EXTENSIONS) return
        pendingFiles.add(file)
        if (!flushScheduled.compareAndSet(false, true)) return
        ApplicationManager.getApplication().invokeLater {
            flushScheduled.set(false)
            if (project.isDisposed) return@invokeLater
            ApplicationManager.getApplication().runWriteAction {
                val psiManager = PsiManager.getInstance(project)
                val instance = HighlightingSettingsPerFile.getInstance(project)
                var current = pendingFiles.poll()
                while (current != null) {
                    if (current.isValid) {
                        val psiFile = psiManager.findFile(current)
                        if (psiFile != null &&
                            instance.getHighlightingSettingForRoot(psiFile) != FileHighlightingSetting.SKIP_INSPECTION
                        ) {
                            instance.setHighlightingSettingForRoot(psiFile, FileHighlightingSetting.SKIP_INSPECTION)
                        }
                    }
                    current = pendingFiles.poll()
                }
            }
        }
    }

    companion object {
        private val TARGET_EXTENSIONS = setOf("java", "kt", "xml")
    }
}
