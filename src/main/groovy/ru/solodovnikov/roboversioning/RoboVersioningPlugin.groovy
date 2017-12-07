package ru.solodovnikov.roboversioning

import org.gradle.api.Plugin
import org.gradle.api.Project

class RoboVersioningPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        println("Hello ${getClass().name}")
    }
}
