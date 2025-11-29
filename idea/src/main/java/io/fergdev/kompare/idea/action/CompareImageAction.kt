package io.fergdev.kompare.idea.action

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.vfs.VirtualFile
import io.fergdev.kompare.idea.toolwindow.KompareState
import io.fergdev.kompare.idea.toolwindow.KompareUiModel
import io.fergdev.kompare.image.DiffResult
import io.fergdev.kompare.image.diffImage
import org.jetbrains.skia.Image as SkiaImage

public class CompareImageAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.isImage() == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val actualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val goldenFile = chooseGolden(project) ?: return

        val (golden, actual, result) = compareFilesWithKompare(actualFile, goldenFile)

        val hasDiff = when (result) {
            DiffResult.NoDiff, is DiffResult.Error -> false
            is DiffResult.PixelDiff, is DiffResult.SizeDiff -> true
        }
//        val diffImage = when (result) {
//            DiffResult.NoDiff, is DiffResult.Error -> null
//            is DiffResult.PixelDiff -> result.diffImage
//            is DiffResult.SizeDiff -> result.diffImage
//        }
        // Push result into state
        KompareState.current.value = KompareUiModel(
            golden = golden,
            actual = actual,
            diffResult = result,
            message = if (hasDiff) "Images differ" else "Images match"
        )

        ToolWindowManager.getInstance(project)
            .getToolWindow("Kompare")
            ?.show()
    }

    private fun chooseGolden(project: Project): VirtualFile? {
        val descriptor = FileChooserDescriptorFactory
            .createSingleFileDescriptor()
            .withTitle("Select Golden Image")
            .withDescription("Choose the golden snapshot image to compare against.")
        return FileChooser.chooseFile(descriptor, project, null)
    }
}

private fun VirtualFile.isImage(): Boolean =
    extension?.lowercase() in listOf("png", "jpg", "jpeg", "webp")

private data class CompareInputs(
    val golden: ImageBitmap,
    val actual: ImageBitmap,
    val result: DiffResult,
)

private fun compareFilesWithKompare(
    actualFile: VirtualFile,
    goldenFile: VirtualFile
): CompareInputs {
    val actual = actualFile.toImageBitmap()
    val golden = goldenFile.toImageBitmap()
    val result = diffImage(golden, actual)
    return CompareInputs(golden, actual, result)
}

private fun VirtualFile.toImageBitmap(): ImageBitmap {
    val bytes = inputStream.use { it.readBytes() }
    val skiaImage = SkiaImage.makeFromEncoded(bytes)
    return skiaImage.toComposeImageBitmap()
}