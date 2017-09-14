[![Release](https://jitpack.io/v/scm4j/scm4j-releaser.svg)](https://jitpack.io/#scm4j/scm4j-releaser)
[![Build Status](https://travis-ci.org/scm4j/scm4j-releaser.svg?branch=master)](https://travis-ci.org/scm4j/scm4j-releaser)
[![Coverage Status](https://coveralls.io/repos/github/scm4j/scm4j-releaser/badge.svg?branch=master)](https://coveralls.io/github/scm4j/scm4j-releaser?branch=master)

# Status

IN DEVELOPMENT

# Overview

Tool to manage projects which are represented by many components, each component has its own repository (multi-repository configuration). It  detectes changes in repositories, builds new components versions and actualizes dependency lists.

# Terms

- `component`: component has its own repository and can be built and uploaded to some repository
- `project`: has a component and  can be installed (together with all dependencies) as "a whole", from end-user POV
- `managable dependency`: dependency of any component which is "under control", i.e. can be branched, built and uploaded to a maven repository
- `mdeps`: file which lists managable dependencies, is located at the root of every repository

# Scenarious

- view project `status`: if something has been changed in `develop` and `release` branches of any component?
- `fork`: create `release` branches out of changed `develop` branches
- `build`: apply `build` command to `release` branches

E.g. we have a `product3` which depends on `component39`:0.9.5 and `component50`:0.5.0, all components and `product3` sit in their own repositories. Now we add some commits to the `develop` branch of `component50` and run the tool using `status` command. Tool analyzes repositories and suggests that new versions of `component50` and `product3` should be built. Then we can run tool using  `fork` and `build` commands. `fork` command creates new `release branches` and increase minor versions in `develop branches`, `build` does whatever configured plus increases patch version in `release branch`.

For version definitions ref. [semantic Versioning 2.0.0](http://semver.org/).

# Installation

Install groovy, download [run.grovy](https://raw.githubusercontent.com/scm4j/scm4j-releaser/release/2/run.groovy) and execute `groovy run.groovy`, it will list available commands

# Data Structure

- ref [data-structure.md](docs/data-structure.md)

# Features

- [delayed tagging](/../../issues/2)
- [exact versions in `develop`/`mdeps`](/../../issues/4)


# See also

[Historical version](https://github.com/scm4j/scm4j-releaser/blob/d540cb00674d485846117dbd68df19bdad306e56/README.md)
