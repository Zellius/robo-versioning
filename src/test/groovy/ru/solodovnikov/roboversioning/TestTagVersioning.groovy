package ru.solodovnikov.roboversioning

import groovy.test.GroovyAssert
import org.junit.Test


class TestTagVersioning {

    @Test
    void testReleaseVersioning_Success() {
        def tag = [name: "10.1.10", date: System.currentTimeMillis(), hash: "2312"] as Git.Tag
        def result = new ReleaseDigitTagVersioning().calculate(tag)
        assert result.name == tag.name
        assert result.code == 100110
    }

    @Test
    void testReleaseVersioning_WrongTag() {
        def tag = [name: "10.1.10.3", date: System.currentTimeMillis(), hash: "2312"] as Git.Tag
        GroovyAssert.shouldFail(IllegalArgumentException, {
            new ReleaseDigitTagVersioning().calculate(tag)
        })
    }

    @Test
    void testReleaseVersioning_WrongTag2() {
        def tag = [name: "10.1.10", date: System.currentTimeMillis(), hash: ""] as Git.Tag
        GroovyAssert.shouldFail(IllegalArgumentException, {
            new ReleaseDigitTagVersioning().calculate(tag)
        })
    }

    @Test
    void testReleaseVersioning_NullTag() {
        GroovyAssert.shouldFail(IllegalArgumentException, {
            new ReleaseDigitTagVersioning().calculate(null)
        })
    }

    @Test
    void testReleaseCandidateVersioning_Success() {
        def tag = [name: "10.1.10rc6", date: System.currentTimeMillis(), hash: "2312"] as Git.Tag
        def result = new ReleaseCandidateDigitTagVersioning().calculate(tag)
        assert result.name == tag.name
        assert result.code == 10011006
    }

    @Test
    void testReleaseCandidateVersioning_WrongTag() {
        def tag = [name: "10.1.10.3", date: System.currentTimeMillis(), hash: "2312"] as Git.Tag
        GroovyAssert.shouldFail(IllegalArgumentException, {
            new ReleaseCandidateDigitTagVersioning().calculate(tag)
        })
    }

    @Test
    void testReleaseCandidateVersioning_WrongTag2() {
        def tag = [name: "10.1.10", date: System.currentTimeMillis(), hash: ""] as Git.Tag
        GroovyAssert.shouldFail(IllegalArgumentException, {
            new ReleaseCandidateDigitTagVersioning().calculate(tag)
        })
    }

    @Test
    void testReleaseCandidateVersioning_NullTag() {
        GroovyAssert.shouldFail(IllegalArgumentException, {
            new ReleaseCandidateDigitTagVersioning().calculate(null)
        })
    }
}