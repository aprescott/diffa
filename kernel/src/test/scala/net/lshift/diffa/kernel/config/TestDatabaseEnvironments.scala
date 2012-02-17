package net.lshift.diffa.kernel.config

import net.lshift.diffa.kernel.util.DatabaseEnvironment

/**
 * These database environments are intended for use in testing.
 */
object TestDatabaseEnvironments {
  def adminEnvironment: DatabaseEnvironment = AdminEnvironment
  def hsqldbEnvironment(path: String): DatabaseEnvironment = new HsqldbEnvironment(path)
}

/**
 * This is intended for testing purposes only.
 */
object AdminEnvironment extends DatabaseEnvironment {
  override def username = System.getProperty("diffa.jdbc.sys.username", "sys")
  override def password = System.getProperty("diffa.jdbc.sys.password", "")
  override def url = {
    val _url = System.getProperty("diffa.jdbc.sys.url")
    if (_url == null)
      System.getProperty("diffa.jdbc.url", "")
    else
      _url
  }
}

/**
 * This is intended for testing purposes only.
 */
class HsqldbEnvironment(path: String) extends DatabaseEnvironment {
  override def url = substitutableURL(path, """jdbc:hsqldb:mem:%s""")
  override def dialect = "org.hibernate.dialect.HSQLDialect"
  override def driver = "org.hsqldb.jdbc.JDBCDriver"
  override def username = System.getProperty("diffa.jdbc.username", "SA")
  override def password = System.getProperty("diffa.jdbc.password", "")
}
