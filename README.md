# Corleone
*it gives you translations… and maybe someday you have opportunity to repay us the favour, capisce?*

Corleone Translation Service provides an internationalization solution for the application in Smart Logistics. It is
implemented with the current stack:
- [play-scala 2.4.0](https://www.playframework.com/).
- [Scala 2.11.6](http://www.scala-lang.org/)
- [sbt 0.1.1](http://www.scala-sbt.org/)
- [webjars-play 2.4.0-1](http://www.webjars.org/documentation)
- [Slick 3.0.0](http://slick.typesafe.com/doc/3.0.0/)
- [Hikari CP 2.3.3](https://github.com/brettwooldridge/HikariCP)

Corleone is hosted in [AWS](http://aws.amazon.com/).

## Architechture

Corleone is an application build using Play Framework Architecture. It provides a frontend as well as a [REST api]
(https://stash.zalando.net/users/bfriedrich/repos/translation-service-api/browse/translation-service-api.yml?at=refs%2Fheads%2Fapi-definition)
in which users will be able to introduce their own set of translations.

![Architecture overview] (https://drive.google.com/file/d/0ByswMdi87hHiV29oeG1QZXBwMGc/view?usp=sharing)

## Storage

To store the information Corleone will connect to an external DB cluster [Spilo](http://spilo.readthedocs.org/en/latest/)
hosted in the same Trusted Zone. For this we use Slick (Functional Relational Mapping library) in combination with
Hikari (Connection Pool Manager) to implement the communication.

## DB-diff rollout

For continuous modification of the database, the current solution uses [FlyWay](http://flywaydb.org) to run sequentially all the db_diffs.

## Testing the application

For setting up your local database you can execute setup_db.sh and setup_postgres.sh scripts located in the script folder.
This should create a db translation_service_db with the schema and all the required roles for the application.
For setting up the domain objects execute::

     $ activator
     [corleone] $ flywayMigrate

To test the application just run:

     $ activator test

To run all the test whenever the code changes run:

     $ activator
     [corleone] $ ~ test