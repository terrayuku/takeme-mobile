#!/usr/bin/env bash
./gradlew -Dtest.single=me.toptas.rssreader.UnitTestSuite clean test
./gradlew assembleDebug