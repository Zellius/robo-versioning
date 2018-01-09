package ru.solodovnikov.roboversioning

import groovy.transform.Immutable

interface Git {
    /**
     * Get all git tags
     * @return list of all tags
     */
    List<Tag> tags()

    String describe(String params)

    /**
     * Git tag
     */
    @Immutable
    static class Tag {
        String name
        long date
        String hash
    }
}

class GitImpl implements Git {
    private final GitExecutor executor

    /**
     * Git wrap
     * @param executor
     */
    GitImpl(GitExecutor executor = new GitExecutorImpl()) {
        this.executor = executor
    }

    /**
     * Get all git tags
     * @return list of all tags
     */
    @Override
    List<Git.Tag> tags() {
        def command = "log --simplify-by-decoration --pretty=format:\"%H|%ct|%d\" --first-parent"
        def regex = /(.*?)\|(.*?)\|.*?(tag: [^)\n]+)/
        (executor.execute(command).getText() =~ regex).collect
        {
            def (hash, time, tags) = it[1..it.size() - 1]

            (tags =~ /tag: ([^\n,]+)/)
                    .collect { it[1] }
                    .flatten()
                    .collect { [name: it, date: time.toLong(), hash: hash] as Git.Tag }
        }
        .flatten() as List<Git.Tag>
    }

    @Override
    String describe(String params = null) {
        def command = "describe --tags ${params ?: ""}"
        return executor.execute(command).getText()
    }
}

interface GitExecutor {
    /**
     * Execute Git command
     * @param command git command. Cannot be null.
     * @return a process for the command
     */
    Process execute(String command)
}

class GitExecutorImpl implements GitExecutor {
    private final String gitPath

    GitExecutorImpl(String gitPath = 'git') {
        this.gitPath = gitPath
    }

    @Override
    Process execute(String command) {
        if (!command) {
            throw new IllegalArgumentException("Git command cannot be null or empty")
        }
        "$gitPath $command".execute()
    }
}