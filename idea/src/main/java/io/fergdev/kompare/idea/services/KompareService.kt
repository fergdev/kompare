package io.fergdev.kompare.idea.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import io.fergdev.kompare.idea.KompareBundle

@Service(Service.Level.PROJECT)
public class KompareService(project: Project) {

    init {
        thisLogger().info(KompareBundle.message("projectService", project.name))
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    public fun getRandomNumber(): Int = (1..100).random()
}