package io.fergdev.kompare.idea.run

import androidx.compose.ui.graphics.ImageBitmap
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.ide.plugins.PluginManager.getLogger
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.string.print
import io.fergdev.kompare.idea.toolwindow.KompareState
import io.fergdev.kompare.idea.toolwindow.KompareUiModel
import kotlinx.io.files.Path
import org.jetbrains.kotlin.idea.base.facet.isTestModule
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal object KompareTestUtil {

    internal fun isKompareTest(function: KtNamedFunction): Boolean {
        // Only consider test modules
        val ktModule = function.module ?: return false
        if (!ktModule.isTestModule) return false

        // Has @KompareTest annotation
        val hasKompareAnnotation = function.annotationEntries.any { ann ->
            val shortName = ann.shortName?.asString()
            shortName == "KompareTest" || shortName == "io.fergdev.kompare.KompareTest"
        }

        return hasKompareAnnotation
    }
//    internal fun isKompareTest(element: PsiElement): Boolean {
//
//        val function = element as? KtNamedFunction ?: return false
//        thisLogger().warn("""isKompareTest ${function.getDebugText()}"""".trimIndent()
//        )
//
//        // Only consider test modules
//        val ktModule = function.module ?: return false
//        if (!ktModule.isTestModule) return false
//
//        // Has @KompareTest annotation
//        val hasKompareAnnotation = function.annotationEntries.any { ann ->
//            val shortName = ann.shortName?.asString()
//            shortName == "KompareTest" || shortName == "io.fergdev.kompare.KompareTest"
//        }
//
//        return hasKompareAnnotation
//    }
}

internal class KompareRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        // We only care about identifiers that are the name of a function
        val function = (element.parent as? KtNamedFunction) ?: return null
        if (function.nameIdentifier != element) return null

        if (!KompareTestUtil.isKompareTest(function)) return null

        val action = object : AnAction("Run Kompare") {
            override fun actionPerformed(e: AnActionEvent) {
                val project = e.project ?: return
                runKompareForTest(project, function)
            }
        }

        return Info(
            AllIcons.Actions.Execute,
            arrayOf(action),
        ) { "Run Kompare" }
    }

    private fun runKompareForTest(project: Project, function: KtNamedFunction) {
        val className = function.containingClassOrObject?.name ?: "Global"
        val testName = function.name ?: "Unnamed"

        val snapshotPath = findSnapshotPath(project, className, testName, "png")
        val actualPath = findActualPath(project, className, testName, "png")

//        if (snapshotPath == null || actualPath == null) {
//            KompareState.current.value = KompareUiModel(
//                status = "Could not find golden/actual images for $className.$testName"
//            )
//        } else {
//            val goldenBitmap = loadBitmap(snapshotPath)
//            val actualBitmap = loadBitmap(actualPath)
//            val result = compareImages(goldenBitmap, actualBitmap)
//
//            KompareState.current.value = KompareUiModel(
//                goldenFile = VfsUtil.findFile(snapshotPath, true),
//                actualFile = VfsUtil.findFile(actualPath, true),
//                goldenBitmap = goldenBitmap,
//                actualBitmap = actualBitmap,
//                diffBitmap = result.diffImage,
//                status = if (result.hasDiff) "Kompare: images differ" else "Kompare: images match"
//            )
//        }

        ToolWindowManager.getInstance(project)
            .getToolWindow("Kompare")
            ?.show()
    }

    private fun findSnapshotPath(
        project: Project,
        className: String,
        testName: String,
        extension: String
    ): Path? {
        // TODO: adapt to your real layout.
        // Example: <module>/src/test/resources/__kompare__/<ClassName>/<testName>_golden.png
//        val base = project.basePath ?: return null
//        return Path.of(
//            base,
//            "src",
//            "test",
//            "resources",
//            "__kompare__",
//            className,
//            "${testName}_golden.$extension"
//        )
//            .takeIf { it.toFile().exists() }
        return null
    }

    private fun findActualPath(
        project: Project,
        className: String,
        testName: String,
        extension: String
    ): Path? {
//        val base = project.basePath ?: return null
//        return Path.of(base, "build", "kompare", className, "${testName}_actual.$extension")
//            .takeIf { it.toFile().exists() }
        return null
    }

//    private fun loadBitmap(path: Path): ImageBitmap {
//
//        val bytes = path.toFile().readBytes()
//        val skiaImage = SkiaImage.makeFromEncoded(bytes)
//        return skiaImage.toComposeImageBitmap()
//    }
}
