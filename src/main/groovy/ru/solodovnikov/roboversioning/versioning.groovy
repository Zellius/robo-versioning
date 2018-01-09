package ru.solodovnikov.roboversioning

import groovy.transform.EqualsAndHashCode

import java.util.regex.Pattern

/**
 * Calculated VersionName and VersionCode for Android application
 */
@EqualsAndHashCode
class RoboVersion {
    /**
     * VersionName
     */
    final int code
    /**
     * VersionCode
     */
    final String name

    RoboVersion(int code, String name) {
        this.code = code
        this.name = name
    }
}

interface VersioningCalculator {
    RoboVersion calculate(Git git)
}

/**
 * Base interface for git versioning
 */
interface TagVersioning {
    /**
     * Calculate version based on provided git tag
     * @param tag git tag. Cannot be null.
     * @return calculated version or throw IllegalArgumentException if it cannot be calculated
     *
     */
    RoboVersion calculate(Git.Tag tag)

    /**
     * Calculate version name based on provided git tag
     * @param tag git tag. Cannot be null.
     * @return calculated version name or throw IllegalArgumentException if it cannot be calculated
     */
    String versionName(Git.Tag tag)

    /**
     * Calculate version code based on provided git tag
     * @param tag git tag. Cannot be null.
     * @return calculated version code or throw IllegalArgumentException if it cannot be calculated
     */
    int versionCode(Git.Tag tag)

    /**
     * Check is tag valid for this TagVersioning
     * @param tag git tag
     * @return true if tag is valid
     */
    boolean isTagValid(Git.Tag tag)

    /**
     * Empty version for this TagVersioning
     * @return empty version
     */
    RoboVersion empty()
}

abstract class BaseGitVersioningCalculator implements VersioningCalculator {
    protected final String logTag
    protected final Git git

    BaseGitVersioningCalculator(Git git) {
        this.git = git
        this.logTag = getClass().simpleName
    }
}

class GitTagVersioningCalculator extends BaseGitVersioningCalculator {
    protected final TagVersioning versioning

    GitTagVersioningCalculator(Git git = new GitImpl(), TagVersioning versioning) {
        super(git)
        this.versioning = versioning
    }

    @Override
    RoboVersion calculate() {
        def tags = git.tags()

        println("$logTag: tags ${tags?.name ?: 'empty'}")

        return (gitTags?.find { resultVersioning.isTagValid(it) } ?: null).with {
            final RoboVersion calculatedVersion

            if (!it) {
                println("$logTag: there is no valid tag for build variant")
                calculatedVersion = versioning.empty()
            } else {
                println("$logTag: valid tag is ${it.name}")
                calculatedVersion = calculate(it)
            }

            println("$logTag: tag calculated version $calculatedVersion")

            check(calculatedVersion)

            return calculatedVersion
        }
    }

    protected RoboVersion calculate(Git.Tag tag) {
        return versioning.calculate(tag)
    }
}

class GitTagDescribeVersioningCalculator extends GitTagVersioningCalculator {
    GitTagDescribeVersioningCalculator(TagVersioning versioning) {
        super(versioning)
    }

    GitTagDescribeVersioningCalculator(Git git, TagVersioning versioning) {
        super(git, versioning)
    }

    @Override
    protected RoboVersion calculate(Git.Tag tag) {
        return new RoboVersion(versioning.versionCode(tag), git.describe(tag.hash))
    }
}

/**
 * Base TagVersioning for digits only git tags
 */
abstract class BaseDigitTagVersioning implements TagVersioning {
    protected final Pattern pattern

    BaseDigitTagVersioning(Pattern pattern) {
        this.pattern = pattern
    }

    BaseDigitTagVersioning(String pattern) {
        this(Pattern.compile(pattern))
    }

    @Override
    RoboVersion calculate(Git.Tag tag) {
        return new RoboVersion(versionCode(tag), versionName(tag))
    }

    @Override
    String versionName(Git.Tag tag) {
        if (!isTagValid(tag)) {
            throw new IllegalArgumentException("Tag $tag is not valid")
        }

        return tag.name
    }

    @Override
    int versionCode(Git.Tag tag) {
        if (!isTagValid(tag)) {
            throw new IllegalArgumentException("Tag $tag is not valid")
        }

        def digits = (tag.name =~ pattern).with {
            return it[0][1..it.groupCount()]*.toInteger()
                    .toArray(new Integer[it.groupCount()])
        }

        return calculateCodeFromParts(digits)
    }

    @Override
    boolean isTagValid(Git.Tag tag) {
        if (tag == null) {
            return false
        }
        if (!tag.name || !tag.date || !tag.hash) {
            return false
        }

        return tag.name ==~ pattern
    }

    /**
     * Calculate version code from parsed digit parts
     * @param parsedDigits parsed digits from tag
     * @return calculated version code
     */
    protected abstract int calculateCodeFromParts(Integer[] parsedDigits)
}

/**
 * Input git tag: 1.2.3
 * Output: versionName 1.2.3; versionCode 10203
 */
class ReleaseDigitTagVersioning extends BaseDigitTagVersioning {
    ReleaseDigitTagVersioning() {
        super(~/(\d+).(\d+).(\d+)/)
    }

    @Override
    protected int calculateCodeFromParts(Integer[] parsedDigits) {
        def (major, minor, patch) = parsedDigits[0..<parsedDigits.length]
        return (major * 100 + minor) * 100 + patch
    }

    @Override
    RoboVersion empty() {
        return new RoboVersion(0, '0.0.0')
    }
}

/**
 * Input git tag: 1.2.3rc4
 * Output: versionName 1.2.3rc4; versionCode 1020304
 */
class ReleaseCandidateDigitTagVersioning extends BaseDigitTagVersioning {
    ReleaseCandidateDigitTagVersioning() {
        super(~/(\d+).(\d+).(\d+)rc(\d+)/)
    }

    @Override
    protected int calculateCodeFromParts(Integer[] parsedDigits) {
        def (major, minor, patch, rc) = parsedDigits[0..<parsedDigits.length]
        return ((major * 100 + minor) * 100 + patch) * 100 + rc
    }

    @Override
    RoboVersion empty() {
        return new RoboVersion(0, '0.0.0rc0')
    }
}