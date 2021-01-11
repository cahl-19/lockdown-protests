# About this Project

This project is a simple tool for users to plan adhoc protests.

# Depedencies

A Java 11 jdk, installation of nodejs, and Mongo 4.2 server (or docker) are
required to build and run this project. All other dependencies are downloaded
by the build script. This app was designed to run on modern Linux. It may build
and run on Windows, but this is not intentional and Windows support may
eventually break.

# Building/Running

Build this project by executing `./gradlew build`. On completion, a script
will be generated and written to ./scripts which you may execute to launch
the webserver.

# Mongo Database

This app requires a Mongo version 4.2 database to run. The included
docker-compose.yml can be used to download and start a containerized mongo
server for testing and development using `docker-compose up`. It is up to
you to download and configure a compatible version of docker and
docker-compose.

The default username, password and database used for Mongo are `ldprotest`,
`ldprotest` and `ldprotest` respectivley. This will eventually be made
configurable.

At the moment, the ldprotest user must be added as a root user (this is not
intentional and will eventually be fixed).

# Contributing

See CONTRIBUTERS.md for more info.
