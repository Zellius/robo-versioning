package ru.solodovnikov.roboversioning

class Git {
    private final GitExecutor executor

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

    static class Tag {
        String name
        long date
        String hash

        boolean equals(o) {
            if (this.is(o)) return true
            if (getClass() != o.class) return false

            Tag tag = (Tag) o

            if (date != tag.date) return false
            if (hash != tag.hash) return false
            if (name != tag.name) return false

            return true
        }

        int hashCode() {
            int result
            result = (name != null ? name.hashCode() : 0)
            result = 31 * result + (int) (date ^ (date >>> 32))
            result = 31 * result + (hash != null ? hash.hashCode() : 0)
            return result
        }


        @Override
        public String toString() {
            return "Tag{" +
                    "name='" + name + '\'' +
                    ", date=" + date +
                    ", hash='" + hash + '\'' +
                    '}'
        }
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
    @Override
    Process execute(String command) {
        if (!command) {
            throw new IllegalArgumentException("Git command cannot be null or empty")
        }
        "git $command".execute()
    }
}