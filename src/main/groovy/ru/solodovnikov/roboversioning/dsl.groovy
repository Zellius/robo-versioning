package ru.solodovnikov.roboversioning

import java.util.regex.Pattern

class CustomDigitTag {
    def pattern
    def valid = true
    def versionCode
    def versionName
    def emptyVersion

    def setPattern(String pattern) {
        this.pattern = pattern
    }

    def setPattern(Pattern pattern) {
        this.pattern = pattern
    }

    def getPattern() {
        if (!pattern) {
            throw new IllegalArgumentException("Pattern cannot be null or empty")
        }
        return pattern
    }

    def setValid(boolean valid) {
        this.valid = valid
    }

    def setValid(Closure<Boolean> valid) {
        this.valid = valid
    }

    def setVersionCode(Integer versionCode) {
        this.versionCode = versionCode
    }

    def setVersionCode(Closure<Integer> versionCode) {
        this.versionCode = versionCode
    }

    def setVersionName(String versionName) {
        this.versionName = versionName
    }

    def setVersionName(Closure<String> versionName) {
        this.versionName = versionName
    }

    def setEmptyVersion(Map<String, Object> emptyVersion) {
        this.emptyVersion = emptyVersion
    }

    def setEmptyVersion(Closure<Map<String, Object>> emptyVersion) {
        this.emptyVersion = emptyVersion
    }

    def getEmptyVersion() {
        if (!emptyVersion) {
            throw new IllegalArgumentException("EmptyVersion cannot be null or empty")
        }
        return emptyVersion
    }
}

class GitDsl {
    /**
     * Path to the Git executable
     */
    def gitPath = 'git'
    /**
     * params for get git tags command
     */
    String tagsParams = '--first-parent'
    /**
     * params for describe git command
     */
    String describeParams = '--first-parent'

    void setGitPath(String path) {
        setGitPath(new File(path))
    }

    void setGitPath(File git) {
        if (!git.isFile()) {
            throw new IllegalArgumentException("$git is not a file")
        }
        this.gitPath = git.absolutePath
    }
}