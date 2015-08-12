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

![Architecture overview] (https://github.com/zalando/corleone/blob/setup_db/corleone/Documentation/Corleone%20Architecture.jpg)

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

## Security

Corleone provides a basic implementation of the OAUTH2 [Authorization Code Grant flow]( http://tools.ietf.org/html/rfc6749#section-4.1.3).

The mechanism can be easily enabled / disabled with 

    oauth2.enabled = true / false

The mechanism can be configured as follows:
    
    # callback to which the user is redirected by the authentication server
    oauth2.callback.url = "https://localhost:9443/oauth_callback"
  
    # access token endpoint
    oauth2.access.token.url = "https://auth.server.com/oauth2/access_token"

    # authorization server endpoint
    oauth2.authorization.url = "https://auth.server.com/z/oauth2/authorize"

    # token info endpoint
    oauth2.token.info.url = "https://auth.server.com/oauth2/tokeninfo"

    # file containing the OAUTH2 credentials
    # NOTE: content must follow the format "{"client_id":"<client id>","client_secret":"<client secret>"}"
    oauth2.credentials.filePath= "/tmp/credentials/client.json"

    # timout in ms for the requests against OAUTH infrastructure
    oauth2.request.timeout = 5000

    # specifies the expiry time boundary in seconds after which the OAUTH2 mechanism should try to reresh the access token
    oauth2.token.refresh.expiry.limit = 300

    # time in seconds after which the credentials cache should expire automatically
    oauth2.credentials.cache.expiry.time = 300

    # additional server paths excluded from the OAUTH2 validation
    oauth2.excluded.paths = ["/webjars", "/assets"]

    # additional service paths for OAUTH2 validation
    oauth2.service.paths = ["/services"]


## License

Copyright [2015] Zalando SE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


