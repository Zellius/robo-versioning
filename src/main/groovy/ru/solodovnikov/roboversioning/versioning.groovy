package ru.solodovnikov.roboversioning

import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable

import java.util.regex.Pattern

/**
 * Calculated VersionName and VersionCode for Android application
 */
@EqualsAndHashCode
@Immutable
class RoboVersion {
    /**
     * VersionName
     */
    int code
    /**
     * VersionCode
     */
    String name
}

/**
 * Base interface for version calculators
 */
interface VersionCalculator {
    /**
     * Calculate current android version
     * @param git configured git implementation
     * @return calculated android version
     */
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

/**
 * Base git version calculator
 */
abstract class BaseGitVersionCalculator implements VersionCalculator {
    private final String tag

    BaseGitVersionCalculator() {
        this.tag = getClass().simpleName
    }

    /**
     * Log event
     * @param message log message
     */
    protected void log(String message) {
        println("$tag: $message")
    }
}

/**
 * Default git tag versioning calculator
 */
class GitTagVersioningCalculator extends BaseGitVersionCalculator {
    protected final TagVersioning versioning

    /**
     *
     * @param versioning versioning used for this calculator
     */
    GitTagVersioningCalculator(TagVersioning versioning) {
        super()
        this.versioning = versioning
    }

    @Override
    RoboVersion calculate(Git git) {
        final List<Git.Tag> tags = git.tags()

        log("tags ${tags?.name ?: 'empty'}")

        return (tags?.find { versioning.isTagValid(it) } ?: null).with { Git.Tag tag ->
            final RoboVersion calculatedVersion

            if (!tag) {
                log("there is no valid tag for build variant")
                calculatedVersion = versioning.empty()
            } else {
                log("valid tag is ${tag.name}")
                calculatedVersion = calculate(tag)
            }

            log("tag calculated version $calculatedVersion")

            return calculatedVersion
        }
    }

    protected RoboVersion calculate(Git.Tag tag) {
        return versioning.calculate(tag)
    }
}

/**
 * Version calculator which use git describe as version name
 */
class GitTagDescribeVersionCalculator extends GitTagVersioningCalculator {
    GitTagDescribeVersionCalculator(TagVersioning versioning) {
        super(versioning)
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