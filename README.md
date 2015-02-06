# jdbc-query-tool
A simple Java Swing query tool for inspecting databases via JDBC and performing simple queries.

The build.sbt file contains an append to library dependencies so that you can set up a simple Derby database to get started.  I don't have examples of other databases because I didn't want to have to redistribute drivers.  The connections.config file contains a derby example.  Derby's kind of like hsqldb in that connecting to the DB essentially "starts" the DB, so you don't have to (separately) start Derby to connect to a Derby database from this query tool.  If you want to, however, you can use the "ij" utilty in the JDK "db/bin" directory.  I haven't verified, but it appears that it defaults to single-user-mode, so if you're currently connected to a dabase with "ij" you won't be able to simultaneously connect to the same database from this query tool.

If you don't want to use "ij" to create the database the first time, you can add the usual "create=true" at the end of the connection URL.  For example, instead of "jdbc:derby://localhost:1527/testdb", you can use "jdbc:derby://localhost:1527/testdb;create=true".

This is not a shining example of software engineering, but rather a utility that's been accumulating features for a long time.  As they say, "Do as I say; don't do as I do."  :)
