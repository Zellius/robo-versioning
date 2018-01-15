package ru.solodovnikov.roboversioning

import com.android.build.gradle.api.BaseVariant
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

import java.util.regex.Pattern

/**
 * Calculated VersionName and VersionCode for Android application
 */
@EqualsAndHashCode
@TupleConstructor
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
     * @param variant for which version will be calculated
     * @return calculated android version
     */
    RoboVersion calculate(Git git, BaseVariant variant)
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
 * Default git tag versioning calculator
 */
class GitTagVersioningCalculator implements VersionCalculator {
    protected final TagVersioning versioning
    protected final Logger logger

    /**
     *
     * @param versioning versioning used for this calculator
     */
    GitTagVersioningCalculator(TagVersioning versioning, Logger logger) {
        super()
        this.versioning = versioning
        this.logger = logger
    }

    @Override
    RoboVersion calculate(Git git, BaseVariant variant) {
        final List<Git.Tag> tags = git.tags()

        logger.log("<${variant.name}> tags ${tags?.name ?: 'empty'}")

        return (tags?.find { versioning.isTagValid(it) } ?: null).with { Git.Tag tag ->
            final RoboVersion calculatedVersion

            if (!tag) {
                logger.log("<${variant.name}> there is no valid tag for build variant")
                calculatedVersion = versioning.empty()
            } else {
                logger.log("<${variant.name}> valid tag is ${tag.name}")
                calculatedVersion = calculate(git, tag, variant)
            }

            if (variant.mergedFlavor.versionNameSuffix) {
                calculatedVersion.name = calculatedVersion.name + variant.mergedFlavor.versionNameSuffix
            }

            logger.log("<${variant.name}> tag calculated version $calculatedVersion")

            return calculatedVersion
        }
    }

    protected RoboVersion calculate(Git git, Git.Tag tag, BaseVariant variant) {
        return versioning.calculate(tag)
    }
}

/**
 * Version calculator which use git describe as version name
 */
class GitTagDescribeVersionCalculator extends GitTagVersioningCalculator {
    GitTagDescribeVersionCalculator(TagVersioning versioning, Logger logger) {
        super(versioning, logger)
    }

    @Override
    protected RoboVersion calculate(Git git, Git.Tag tag, BaseVariant variant) {
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
        if (!isTagValid(tag)) {
            throw new IllegalArgumentException("Tag $tag is not valid")
        }

        return new RoboVersion(calculateVersionCode(calculateVersionCodeParts(tag)), calculateVersionName(tag))
    }

    @Override
    String versionName(Git.Tag tag) {
        if (!isTagValid(tag)) {
            throw new IllegalArgumentException("Tag $tag is not valid")
        }

        return calculateVersionName(tag)
    }

    @Override
    int versionCode(Git.Tag tag) {
        if (!isTagValid(tag)) {
            throw new IllegalArgumentException("Tag $tag is not valid")
        }

        return calculateVersionCode(calculateVersionCodeParts(tag))
    }

    @Override
    boolean isTagValid(Git.Tag tag) {
        if (!tag || !tag.name || !tag.date || !tag.hash) {
            return false
        }

        return tag.name ==~ pattern
    }

    /**
     * Calculate version name from tag
     * @param tag valid git tag
     * @return calculated version name
     */
    protected String calculateVersionName(Git.Tag tag) {
        return tag.name
    }

    /**
     * Calculate version code from parsed digit parts
     * @param parsedDigits parsed digits from tag
     * @return calculated version code
     */
    protected int calculateVersionCode(Integer[] parsedDigits) {
        if (parsedDigits == null || parsedDigits.length == 0) {
            throw new IllegalArgumentException("Pared digits cannot be null or empty")
        }

        return parsedDigits[1..<parsedDigits.length].inject(parsedDigits[0], { code, digit ->
            code * 100 + digit
        })
    }

    /**
     * Calculate version code digit parts from git tag
     * @param tag valid git tag
     * @return calculated version code digit parts
     */
    private Integer[] calculateVersionCodeParts(Git.Tag tag) {
        return (tag.name =~ pattern).with {
            return it[0][1..it.groupCount()]*.toInteger()
                    .toArray(new Integer[it.groupCount()]) as Integer[]
        }
    }

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
    RoboVersion empty() {
        return new RoboVersion(0, '0.0.0rc0')
    }
}