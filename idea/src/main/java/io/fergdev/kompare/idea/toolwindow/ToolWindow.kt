package io.fergdev.kompare.idea.toolwindow

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.unit.dp
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import io.fergdev.kompare.image.DiffResult
import io.fergdev.kompare.image.diffImage
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.foundation.util.myLogger
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.typography

internal data class KompareUiModel(
    val golden: ImageBitmap? = null,
    val actual: ImageBitmap? = null,
    val message: String? = null,
    val diffResult: DiffResult? = null
)

internal object KompareState {
    // null = nothing loaded yet
    val current = mutableStateOf(KompareUiModel(null, null, null))
}

public class KompareToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.addComposeTab("Diff", isLockable = true, focusOnClickInside = true) {
            var clicks by remember { mutableStateOf(0) }
            Column(Modifier.padding(16.dp)) {
                Text("Hello from Compose inside IntelliJ!")
                DefaultButton(onClick = { clicks++ }, Modifier.padding(top = 16.dp)) {
                    Text("Clicks: $clicks")
                }
                KompareToolWindowContent(project)
            }
        }
    }
}

@Composable
private fun KompareToolWindowContent(project: Project) {
    val model by KompareState.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (model.message != null) {
            Text(
                text = model.message!!,
                style = JewelTheme.typography.h2TextStyle
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ImageCard(
                title = "Golden",
                bitmap = model.golden,
                project = project,
                modifier = Modifier.weight(1f)
            )
            ImageCard(
                title = "Actual",
                bitmap = model.actual,
                project = project,
                modifier = Modifier.weight(1f)
            )
            val dI = when (val diffResult = model.diffResult) {
                is DiffResult.PixelDiff -> {
                    diffResult.diffImage
                }

                else -> {
                    null
                }
            }
            ImageCard(
                title = "Diff",
                bitmap = dI,
                project = project,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ImageCard(
    title: String,
    bitmap: ImageBitmap?,
    project: Project,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = JewelTheme.typography.h2TextStyle)
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    DefaultButton(onClick = {
                        selectImage(project, title == "Golden")
                    }) {
                        Text(text = "Select")
                    }
                    Text("—")
                }
            }
        }
    }
}

private fun selectImage(project: Project, isGolden: Boolean) {
    val descriptor = FileChooserDescriptorFactory
        .singleFile()
        .withFileFilter { it.extension?.lowercase() in listOf("png") }
        .withTitle(if (isGolden) "Select Golden Image" else "Select Actual Image")

    val file: VirtualFile = FileChooser.chooseFile(descriptor, project, null) ?: return

    val bytes = file.inputStream.use { it.readBytes() }

    val expected = bytes.decodeToImageBitmap()

    val current = KompareState.current.value
    KompareState.current.value = if (isGolden) {
        current.copy(
            golden = expected,
        )
    } else {
        current.copy(
            actual = expected,
        )
    }
    if (current.golden != null && current.actual != null) {
        val diffResult = diffImage(current.golden, current.actual)
        project.thisLogger().myLogger().info("diffResult: $diffResult")
        current.copy(diffResult = diffResult)
    }
}