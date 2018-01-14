package ru.solodovnikov.roboversioning

import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable

/**
 * Git interface
 */
interface Git {
    /**
     * Get git tags
     * @return list of git tags
     */
    List<Tag> tags()

    /**
     * Git describe
     * @return
     */
    String describe(String hash)

    /**
     * Git tag
     */
    @Immutable
    @EqualsAndHashCode
    static class Tag {
        String name
        long date
        String hash
    }
}

/**
 * Default git implementation
 */
class GitImpl implements Git {
    private final String git
    private final String tagsCommandParams
    private final String describeCommandParams
    private final Logger logger

    GitImpl(String git,
            String tagsCommandParams,
            String describeCommandParams,
            Logger logger) {
        this.git = git
        this.tagsCommandParams = tagsCommandParams
        this.describeCommandParams = describeCommandParams
        this.logger = logger
    }

    @Override
    List<Git.Tag> tags() {
        def command = "log --simplify-by-decoration --pretty=format:\"%H|%ct|%d\" ${tagsCommandParams ?: ''}"
        def regex = /(.*?)\|(.*?)\|.*?(tag: [^)\n]+)/
        (execute(command).getText() =~ regex).collect
        {
            def (hash, time, tags) = it[1..it.size() - 1]

            (tags =~ /tag: ([^\n,]+)/)
                    .collect { it[1] }
                    .flatten()
                    .collect { new Git.Tag(it, time.toLong(), hash) }
        }
        .flatten() as List<Git.Tag>
    }

    @Override
    String describe(String hash) {
        def command = "describe --tags ${describeCommandParams ?: ''} ${hash ?: ''}"
        return execute(command).getText()
    }

    /**
     * Execute git command
     * @param command git command
     * @return
     */
    Process execute(String command) {
        logger.log("git command executed: $command")

        if (!command) {
            throw new IllegalArgumentException("Git command cannot be null or empty")
        }

        return "$git $command".execute()
    }
}