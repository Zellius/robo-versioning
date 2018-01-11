package ru.solodovnikov.roboversioning

import org.gradle.api.Project

import java.util.regex.Pattern

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

    Logger logger = new LoggerImpl()

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
            return new GitImpl(git, tagsParams, describeParams, logger)
        }
    }

    void setLogger(boolean isEnabled) {
        if (!isEnabled) {
            this.logger = {}
        }
    }
}


class FlavorExtension {
    static final String TAG_DIGIT = 'tag_digit'
    static final String TAG_DIGIT_RC = 'tag_digit_rc'
    static final String TAG_DESCRIBE_DIGIT = 'tag_describe_digit'
    static final String TAG_DESCRIBE_DIGIT_RC = 'tag_describe_digit_rc'

    VersionCalculator versioningCalculator
    Logger logger = new LoggerImpl()

    void setLogger(boolean isEnabled) {
        if (!isEnabled) {
            this.logger = {}
        }
    }

    void setVersioningCalculator(TagVersioning tagVersioning) {
        this.versioningCalculator = new GitTagVersioningCalculator(tagVersioning, logger)
    }

    void setVersioningCalculator(String versionCalculatorType) {
        switch (versionCalculatorType) {
            case TAG_DIGIT:
                this.versioningCalculator = new GitTagVersioningCalculator(new ReleaseDigitTagVersioning(), logger)
                break
            case TAG_DIGIT_RC:
                this.versioningCalculator = new GitTagVersioningCalculator(new ReleaseCandidateDigitTagVersioning(), logger)
                break
            case TAG_DESCRIBE_DIGIT:
                this.versioningCalculator = new GitTagDescribeVersionCalculator(new ReleaseCandidateDigitTagVersioning(), logger)
                break
            case TAG_DESCRIBE_DIGIT_RC:
                this.versioningCalculator = new GitTagDescribeVersionCalculator(new ReleaseCandidateDigitTagVersioning(), logger)
                break
            default:
                throw new IllegalArgumentException("Unknown versionCalculatorType $versionCalculatorType")
        }
    }

    void setVersioningCalculator(Closure<Map<String, Object>> closure) {
        this.versioningCalculator = { git -> new RoboVersion(closure.call(git)) }
    }

    TagVersioning customTagDigit(Pattern pattern,
                                     Closure<Boolean> validClosure = null,
                                     Closure<String> nameClosure = null,
                                     Closure<Integer> codeClosure = null,
                                     Closure<Map<String, Object>> emptyClosure) {
        return new BaseDigitTagVersioning(pattern) {
            @Override
            RoboVersion empty() {
                return new RoboVersion(emptyClosure.call())
            }

            @Override
            boolean isTagValid(Git.Tag tag) {
                final boolean isValid = super.isTagValid(tag)

                if (!isValid) {
                    return false
                }

                if(validClosure){
                    return validClosure.call(tag)
                }

                return true
            }

            @Override
            protected String calculateVersionName(Git.Tag tag) {
                if (nameClosure) {
                    return nameClosure.call(tag)
                } else {
                    return super.calculateVersionName(tag)
                }
            }

            @Override
            protected int calculateVersionCode(Integer[] parsedDigits) {
                if (codeClosure) {
                    return codeClosure.call(parsedDigits)
                } else {
                    return super.calculateVersionCode(parsedDigits)
                }
            }
        }
    }
}
