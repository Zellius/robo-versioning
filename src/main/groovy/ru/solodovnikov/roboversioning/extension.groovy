package ru.solodovnikov.roboversioning

import org.gradle.api.Project

class ProjectExtension {
    private final Project project

    def git

    Logger logger = new LoggerImpl()

    ProjectExtension(Project project) {
        this.project = project
    }

    void setGit(Closure closure) {
        git = project.configure(new GitDsl(), closure)
    }

    void setGit(Git git) {
        this.git = git
    }

    Git getGit() {
        if (git) {
            if (git instanceof GitDsl) {
                return calculateGit(git)
            }
            return git
        } else {
            return calculateGit()
        }
    }

    private Git calculateGit(GitDsl gitDsl = new GitDsl()) {
        return new GitImpl(gitDsl.gitPath, gitDsl.tagsParams, gitDsl.describeParams, logger)
    }

    void quiet() {
        this.logger = {}
    }

    void setLogger(Closure<Void> loggerClosure) {
        this.logger = loggerClosure
    }
}


class FlavorExtension {
    static final String TAG_DIGIT = 'tag_digit'
    static final String TAG_DIGIT_RC = 'tag_digit_rc'
    static final String TAG_DESCRIBE_DIGIT = 'tag_describe_digit'
    static final String TAG_DESCRIBE_DIGIT_RC = 'tag_describe_digit_rc'

    private final Project project
    private final ProjectExtension projectExtension

    VersionCalculator versioningCalculator

    FlavorExtension(Project project, ProjectExtension projectExtension) {
        this.project = project
        this.projectExtension = projectExtension
    }

    void setVersioningCalculator(TagVersioning tagVersioning) {
        this.versioningCalculator = new GitTagVersioningCalculator(tagVersioning, projectExtension.logger)
    }

    void setVersioningCalculator(String versionCalculatorType) {
        switch (versionCalculatorType) {
            case TAG_DIGIT:
                this.versioningCalculator = new GitTagVersioningCalculator(new ReleaseDigitTagVersioning(), projectExtension.logger)
                break
            case TAG_DIGIT_RC:
                this.versioningCalculator = new GitTagVersioningCalculator(new ReleaseCandidateDigitTagVersioning(), projectExtension.logger)
                break
            case TAG_DESCRIBE_DIGIT:
                this.versioningCalculator = new GitTagDescribeVersionCalculator(new ReleaseCandidateDigitTagVersioning(), projectExtension.logger)
                break
            case TAG_DESCRIBE_DIGIT_RC:
                this.versioningCalculator = new GitTagDescribeVersionCalculator(new ReleaseCandidateDigitTagVersioning(), projectExtension.logger)
                break
            default:
                throw new IllegalArgumentException("Unknown versionCalculatorType $versionCalculatorType")
        }
    }

    void setVersioningCalculator(Closure<Map<String, Object>> closure) {
        this.versioningCalculator = { git, variant -> new RoboVersion(closure.call(git, variant)) }
    }

    TagVersioning customDigitTag(Closure closure) {
        final CustomDigitTag customDigitTag = project.configure(new CustomDigitTag(), closure)
        return new BaseDigitTagVersioning(customDigitTag.pattern) {
            @Override
            RoboVersion empty() {
                def versionMap
                if (customDigitTag.emptyVersion instanceof Map) {
                    versionMap = customDigitTag.emptyVersion
                } else {
                    versionMap = customDigitTag.emptyVersion.call()
                }
                return new RoboVersion(versionMap)
            }

            @Override
            boolean isTagValid(Git.Tag tag) {
                final boolean isValid = super.isTagValid(tag)

                if (!isValid) {
                    return false
                }

                if (customDigitTag.valid instanceof Boolean) {
                    return customDigitTag.valid
                }

                return customDigitTag.valid.call(tag)
            }

            @Override
            protected String calculateVersionName(Git.Tag tag) {
                if (customDigitTag.versionName) {
                    if (customDigitTag.versionName instanceof String) {
                        return customDigitTag.versionName
                    }
                    return customDigitTag.versionName.call(tag)
                } else {
                    return super.calculateVersionName(tag)
                }
            }

            @Override
            protected int calculateVersionCode(Integer[] parsedDigits) {
                if (customDigitTag.versionCode) {
                    if (customDigitTag.versionCode instanceof Integer) {
                        return customDigitTag.versionCode
                    }
                    return customDigitTag.versionCode.call(parsedDigits)
                } else {
                    return super.calculateVersionCode(parsedDigits)
                }
            }
        }
    }
}
