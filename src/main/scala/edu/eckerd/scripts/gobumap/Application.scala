package edu.eckerd.scripts.gobumap

import language.higherKinds
import doobie.imports._
import cats._, cats.data._, cats.implicits._

/**
  * Created by davenpcm on 11/15/16.
  */
object Application extends App {


  final case class Identity(
                             id: Int,
                             ldapUID: String
                           )

  final case class GobumapRecord(
                                  udc_id: String,
                                  pidm: Int,
                                  createDate: java.sql.Date,
                                  activityDate: java.sql.Date,
                                  user_id: String,
                                  data_origin: Option[String]
                                )

  final case class GobumapActivityRecord(
                                          udc_id: String,
                                          activityDate: java.sql.Date,
                                          pidm: Int
                                        )



  val xa = DriverManagerTransactor[IOLite](
    "oracle.jdbc.OracleDriver",
    "jdbc:oracle:thin:@//bannertest.eckerd.edu:2322/TEST",
    "USERNAME",
    "PASSWORD"
  )

  def generateGOBUMAP(identity: Identity): GobumapRecord = {
    val d = new java.util.Date()
    val sqlDate = new java.sql.Date(d.getTime)
    GobumapRecord(identity.ldapUID, identity.id, sqlDate, sqlDate, "davenpcm", None)
  }

  def generateGOBUMAPActivity(identity: Identity): GobumapActivityRecord = {
    val d = new java.util.Date()
    val sqlDate = new java.sql.Date(d.getTime)
    GobumapActivityRecord(identity.ldapUID, sqlDate, identity.id)
  }


  def dbProcess(): ConnectionIO[Int] =
    for {
      l1 <- sql"SELECT GOBTPAC_PIDM, GOBTPAC_LDAP_USER FROM GOBTPAC LEFT OUTER JOIN GOBUMAP ON GOBUMAP_PIDM = GOBTPAC_PIDM WHERE GOBTPAC_LDAP_USER is not null and GOBUMAP_PIDM is null".query[Identity].list
      i1 <- Update[GobumapRecord]("insert into GOBUMAP (GOBUMAP_UDC_ID, GOBUMAP_PIDM, GOBUMAP_CREATE_DATE, GOBUMAP_ACTIVITY_DATE, GOBUMAP_USER_ID, GOBUMAP_DATA_ORIGIN) VALUES (?, ?, ?, ?, ?, ?)").updateMany(l1.map(generateGOBUMAP))
      l2 <- sql"SELECT GOBTPAC_PIDM, GOBTPAC_LDAP_USER FROM GOBTPAC INNER JOIN GOBUMAP ON GOBUMAP_PIDM = GOBTPAC_PIDM WHERE GOBTPAC_LDAP_USER is not null and GOBUMAP_UDC_ID <> GOBTPAC_LDAP_USER".query[Identity].list
      i2 <- Update[GobumapActivityRecord]("update GOBUMAP set GOBUMAP_UDC_ID = ? , GOBUMAP_ACTIVITY_DATE = ? where GOBUMAP_PIDM = ?").updateMany(l2.map(generateGOBUMAPActivity))
      l3 <- sql"SELECT GOBTPAC_PIDM FROM GOBTPAC INNER JOIN GOBUMAP ON GOBUMAP_PIDM = GOBTPAC_PIDM WHERE GOBTPAC_LDAP_USER is null".query[Int].list
      i3 <- Update[Int]("DELETE FROM GOBUMAP WHERE GOBUMAP_PIDM = ?").updateMany(l3)
    } yield i1 + i2 + i3

  val a = dbProcess().transact(xa).unsafePerformIO

  println(s"Records Changed - $a")

}
