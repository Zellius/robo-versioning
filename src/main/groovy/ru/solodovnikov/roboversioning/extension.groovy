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
    static final String TAG_DIGIT = 'tag_digit'
    static final String TAG_DIGIT_RC = 'tag_digit_rc'
    static final String TAG_DESCRIBE_DIGIT = 'tag_describe_digit'
    static final String TAG_DESCRIBE_DIGIT_RC = 'tag_describe_digit_rc'

    VersionCalculator versioningCalculator

    void setVersioningCalculator(String versionCalculatorType) {
        switch (versionCalculatorType) {
            case TAG_DIGIT:
                this.versioningCalculator = new GitTagVersioningCalculator(new ReleaseDigitTagVersioning())
                break
            case TAG_DIGIT_RC:
                this.versioningCalculator = new GitTagVersioningCalculator(new ReleaseCandidateDigitTagVersioning())
                break
            case TAG_DESCRIBE_DIGIT:
                this.versioningCalculator = new GitTagDescribeVersionCalculator(new ReleaseCandidateDigitTagVersioning())
                break
            case TAG_DESCRIBE_DIGIT_RC:
                this.versioningCalculator = new GitTagDescribeVersionCalculator(new ReleaseCandidateDigitTagVersioning())
                break
            default:
                throw new IllegalArgumentException("Unknown versionCalculatorType $versionCalculatorType")
        }
    }

    void setVersioningCalculator(Closure<Map<String, Object>> closure) {
        this.versioningCalculator = { git -> new RoboVersion(closure.call(git)) }
    }
}
