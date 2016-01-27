Battlecode Client
=================

* NOTE: If you are a competitor, you can download pre-compiled versions of
  everything here. See: http://www.battlecode.org/contestants/releases/

Basic Guide to Building
-----------------------

Java 1.8 is required.

1. `ant retrieve` - fetches dependencies
2. `cp /path/to/battlecode-server/battlecode-server.jar lib/compile/` - add the
    battlecode-server dependency to the library directory
3. `ant` - build the project
4. `ant tests` - run (mostly nonexistant) tests
5. `java -cp 'build/classes:lib/compile/*' battlecode.client.Main` - run the
    client
