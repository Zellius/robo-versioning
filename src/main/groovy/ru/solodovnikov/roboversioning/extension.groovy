package ru.solodovnikov.roboversioning

import org.gradle.api.Project
import ru.solodovnikov.roboversioning.TagVersioning
import ru.solodovnikov.roboversioning.VersioningCalculator

class GlobalExtension {
    private final Project project

    File git

    GlobalExtension(Project project) {
        this.project = project
    }

    void git(String path) {
        setGit(project.file(path))
    }

    void git(File git) {
        setGit(git)
    }

    private void setGit(File git) {
        if (!git.isFile()) {
            throw new IllegalArgumentException("$git is not a file")
        }
        this.git = git
    }
}

class FlavorExtension {
    VersioningCalculator versioningCalculator
}
