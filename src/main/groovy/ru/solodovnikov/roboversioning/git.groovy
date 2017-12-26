package ru.solodovnikov.roboversioning

import groovy.transform.Immutable

class Git {
    private final GitExecutor executor

    /**
     * Git wrap
     * @param executor
     */
    Git(GitExecutor executor = new GitExecutorImpl()) {
        this.executor = executor
    }

    /**
     * Get all git tags
     * @return list of all tags
     */
    List<Tag> tags() {
        def command = "log --simplify-by-decoration --pretty=format:\"%H|%ct|%d\" --first-parent"
        def regex = /(.*?)\|(.*?)\|.*?(tag: [^)\n]+)/
        (executor.execute(command).getText() =~ regex).collect
        {
            def (hash, time, tags) = it[1..it.size() - 1]

            (tags =~ /tag: ([^\n,]+)/)
                    .collect { it[1] }
                    .flatten()
                    .collect { [name: it, date: time.toLong(), hash: hash] as Tag }
        }
        .flatten() as List<Tag>
    }

    @Immutable
    /**
     * Git tag
     */
    static class Tag {
        String name
        long date
        String hash
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