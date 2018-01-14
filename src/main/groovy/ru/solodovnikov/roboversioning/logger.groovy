package ru.solodovnikov.roboversioning

interface Logger {
    void log(message)
}

class LoggerImpl implements Logger {
    @Override
    void log(message) {
        println("<RoboVersioning>...$message")
    }
}