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
the webserver on [http://localhost:4567](http://localhost:4567).

To produce an optomized/minified build, use `./gradlew release`.

In order to use the test map you will need to obtain a
[mapbox api token](https://www.mapbox.com/studio/account/tokens/) and supply
it via the command  line when executing the run script (use -h, or --help
to get command line doc). You can get an API token for free which should be
good enough for development and simple tests.

Other configuration settings can be set via the command line or by supplying
a config file. See doc/examples/config.yml for a usable configuration file
which also includes descriptions of each parameter as comments.

# Install

On debian based distros, run `./gradew deployDeb` to generate a .deb package
in ./build/distributions. The deb package will handle all steps of installing
package dependencies, creating an appropriate FS heirarchy, setting up
configuration files and generating run and service scripts. Afterward,
configuration of the mongo DB connection and map API token can be done by
modifying the config file in /opt/ldprotest/etc/ldprotest/config.yml.

On other distros these steps need to be performed manually. Example
configuration files in ./doc/example can be used as a starting point. The
development tree run script generated in scripts/run-server can be used as a
starting point for creating your own run-script.

# Mongo Database

This app requires a Mongo version 4.2 database to run. The included
docker-compose.yml can be used to download and start a containerized mongo
server for testing and development using `docker-compose up`. It is up to
you to download and configure a compatible version of docker and
docker-compose. Note that the system package version of docker is usually
out of date, and you will have to download manually from the internet.

Note that the application has been tested with docker-compose version 1.25.x
and Docker version 20.10.x (other versions may or may not work).

The default username, password and database used for Mongo are `ldprotest`,
`ldprotest` and `ldprotest` respectivley. This will eventually be made
configurable.

The script ./scripts/setup-mongo will perform this setup for you if you are
using the containerized database with docker-compose.

# Contributing

See CONTRIBUTERS.md for more info.
