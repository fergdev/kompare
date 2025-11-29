package io.fergdev.kompare.idea.startup

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

public class KompareProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        thisLogger().warn(
            """
            Don't forget to remove all non-needed sample code files 
            with their corresponding registration entries in `plugin.xml`.
            """".trimIndent()
        )
    }
}