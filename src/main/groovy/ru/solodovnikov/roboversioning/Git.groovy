package ru.solodovnikov.roboversioning

class Git {
    /**
     * Get all git tags
     * @return list of all tags
     */
    List<Tag> tags() {
        def regex = /(.*?)\|(.*?)\|.*?(tag: [^)\n]+)/
        ("git log --simplify-by-decoration --pretty=format:\"%H|%ct|%d\" --first-parent" =~ regex).collect
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
        final String name
        final long date
        final String hash
    }
}
