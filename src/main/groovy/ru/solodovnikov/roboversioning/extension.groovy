package ru.solodovnikov.roboversioning

import org.gradle.api.Project

class ProjectExtension {
    private final Project project

    String git = 'git'
    /**
     * params for get git tags command
     */
    String tagsParams = '--first-parent'
    /**
     * params for describe git command
     */
    String describeParams = '--first-parent'
    /**
     * Custom git implementation
     */
    Git gitImplementation

    ProjectExtension(Project project) {
        this.project = project
    }

    void gitPath(String path) {
        setGit(project.file(path))
    }

    void gitPath(File git) {
        setGit(git)
    }

    private void setGit(File git) {
        if (!git.isFile()) {
            throw new IllegalArgumentException("$git is not a file")
        }
        this.git = git.absolutePath
    }

    Git getGitImplementation() {
        if (gitImplementation != null) {
            return gitImplementation
        } else {
            return new GitImpl(git, tagsParams, describeParams)
        }
    }
}

class FlavorExtension {
    VersionCalculator versioningCalculator
}
