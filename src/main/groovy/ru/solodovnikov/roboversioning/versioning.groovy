package ru.solodovnikov.roboversioning

import java.util.regex.Pattern

/**
 * Calculated VersionName and VersionCode for Android application
 */
class RoboVersion {
    /**
     * VersionCode
     */
    int code
    /**
     * VersionName
     */
    String name

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        RoboVersion that = (RoboVersion) o

        if (code != that.code) return false
        if (name != that.name) return false

        return true
    }

    int hashCode() {
        int result
        result = code
        result = 31 * result + (name != null ? name.hashCode() : 0)
        return result
    }


    @Override
    String toString() {
        return "RoboVersion{" +
                "code=" + code +
                ", name='" + name + '\'' +
                '}'
    }
}

/**
 * Base interface for git versioning
 */
interface Versioning {
    /**
     * Calculate version based on provided git tag
     * @param tag git tag. Cannot be null.
     * @return calculated version or throw IllegalArgumentException if cannot be calculated
     *
     */
    RoboVersion calculate(Git.Tag tag)

    /**
     * Check is tag valid for this Versioning
     * @param tag git tag
     * @return true if tag is valid
     */
    boolean isTagValid(Git.Tag tag)

    /**
     * Empty version for this Versioning
     * @return empty version
     */
    RoboVersion empty()
}

/**
 * Base Versioning for digits only git tags
 */
abstract class BaseDigitVersioning implements Versioning {
    protected final Pattern pattern

    BaseDigitVersioning(Pattern pattern) {
        this.pattern = pattern
    }

    BaseDigitVersioning(String pattern) {
        this(Pattern.compile(pattern))
    }

    @Override
    RoboVersion calculate(Git.Tag tag) {
        if (!isTagValid(tag)) {
            throw new IllegalArgumentException("Tag $tag is not valid")
        }

        def digits = (tag.name =~ pattern).with {
            return it[0][1..it.groupCount()]*.toInteger()
                    .toArray(new Integer[it.groupCount()])
        }

        return calculateFromParts(tag, digits)
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
     * Calculate version from parsed digit parts
     * @param tag provided git tag
     * @param parsedDigits parsed digits from tag
     * @return calculated version
     */
    protected abstract RoboVersion calculateFromParts(Git.Tag tag, Integer[] parsedDigits)
}

/**
 * Input git tag: 1.2.3
 * Output: versionName 1.2.3; versionCode 10203
 */
class ReleaseDigitVersioning extends BaseDigitVersioning {
    ReleaseDigitVersioning() {
        super(~/(\d+).(\d+).(\d+)/)
    }

    @Override
    protected RoboVersion calculateFromParts(Git.Tag tag, Integer[] parsedDigits) {
        def (major, minor, patch) = parsedDigits[0..<parsedDigits.length]
        return [code: (major * 100 + minor) * 100 + patch, name: tag.name] as RoboVersion
    }

    @Override
    RoboVersion empty() {
        return [code: 0, name: '0.0.0'] as RoboVersion
    }
}

/**
 * Input git tag: 1.2.3rc4
 * Output: versionName 1.2.3rc4; versionCode 1020304
 */
class ReleaseCandidateDigitVersioning extends BaseDigitVersioning {
    ReleaseCandidateDigitVersioning() {
        super(~/(\d+).(\d+).(\d+)rc(\d+)/)
    }

    @Override
    protected RoboVersion calculateFromParts(Git.Tag tag, Integer[] parsedDigits) {
        def (major, minor, patch, rc) = parsedDigits[0..<parsedDigits.length]
        return [code: ((major * 100 + minor) * 100 + patch)*100+rc, name: tag.name] as RoboVersion
    }

    @Override
    RoboVersion empty() {
        return [code: 0, name: '0.0.0rc0'] as RoboVersion
    }
}