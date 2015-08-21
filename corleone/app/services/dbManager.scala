package services
import com.zaxxer.hikari.{ HikariDataSource, HikariConfig }
/**
 * @author ychahbi
 */
object dbManager {
  def getDataSource(): HikariDataSource = {
    val dsConfig = new HikariConfig()

    //For config options see https://github.com/brettwooldridge/HikariCP
    dsConfig.addDataSourceProperty("url", "jdbc:postgresql://localhost:5432/translation_service_db")
    dsConfig.addDataSourceProperty("user", "postgres")
    dsConfig.addDataSourceProperty("password", "postgres")
    dsConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
    dsConfig.setConnectionTimeout(30000);
    dsConfig.setIdleTimeout(60000);
    dsConfig.setMaxLifetime(1800000);
    dsConfig.setLeakDetectionThreshold(15000);
    dsConfig.setMaximumPoolSize(20);
    dsConfig.setPoolName("test_DBPool");
    dsConfig.setRegisterMbeans(true);
    dsConfig.setIsolateInternalQueries(true)

    //    zalos.sproc.datasource.idleConnectionTestPeriodInMinutes = 4
    //zalos.sproc.datasource.idleMaxAgeInMinutes = 5
    //zalos.sproc.datasource.maxConnectionsPerPartition = 8
    //zalos.sproc.datasource.minConnectionsPerPartition = 1
    //zalos.sproc.datasource.poolAvailabilityThreshold = 40
    //zalos.sproc.datasource.partitionCount = 2
    //zalos.sproc.datasource.acquireIncrement = 1
    //zalos.sproc.datasource.acquireRetryAttempts = 3
    //zalos.sproc.datasource.acquireRetryDelayInMs = 500
    //zalos.sproc.datasource.releaseHelperThreads = 2
    //zalos.sproc.datasource.statisticsEnabled = 0
    //zalos.sproc.datasource.disableJMX = 1
    //zalos.sproc.datasource.connectionTimeoutInMs = 5000
    //zalos.sproc.datasource.disableConnectionTracking = 1
    //zalos.sproc.datasource.maxConnectionAgeInSeconds = 7200

    val datasource = new HikariDataSource(dsConfig)
    datasource
  }
}