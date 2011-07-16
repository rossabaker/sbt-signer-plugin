package com.rossabaker.sbt.signer

import sbt._
import sbt.Keys._
import scala.collection.JavaConversions._
import java.{lang => jl}
import java.{util => ju}
import org.apache.ivy.plugins.resolver._
import org.apache.ivy.plugins.signer.SignatureGenerator
import org.apache.ivy.plugins.signer.bouncycastle.OpenPGPSignatureGenerator

object SignerPlugin extends Plugin {
  object Keys {
    val signatureGenerator = SettingKey[Option[SignatureGenerator]]("signature-generator")
    val bouncyCastleLibraries = SettingKey[Seq[File]]("bouncy-castle-libraries")
  }
  import Keys._

  val signerSettings: Seq[Project.Setting[_]] = Seq(
    signatureGenerator <<= signatureGenerator ?? None,
    // TODO: would be nice to fetch these with Ivy
    bouncyCastleLibraries in Global <<= (bouncyCastleLibraries in Global) ?? Seq(
      Path.userHome / "share" / "java" / "bcprov-jdk16-146.jar",
      Path.userHome / "share" / "java" / "bcpg-jdk16-146.jar"
    ),
    ivySbt <<= (ivySbt, ivyConfiguration, signatureGenerator) map { (ivySbt, ivyConf, generatorOpt) => 
      generatorOpt map { generator =>
        ivySbt.withIvy(ivyConf.log) { ivy =>
          val settings = ivy.getSettings
          settings.addSignatureGenerator(generator)
          settings.getResolvers.asInstanceOf[jl.Iterable[DependencyResolver]] foreach { 
            walkResolvers {
              case r: RepositoryResolver => 
                r.setSigner(generator.getName)
              case _ =>
            } _
          }
        }
      }
      ivySbt
    },
    // Provide default; none exists in 0.10.1
    onLoad in Global <<= (onLoad in Global) ?? identity[State],
    // Need BouncyCastle on classpath to sign PGP artifacts
    onLoad in Global <<= (bouncyCastleLibraries, onLoad in Global) { (bcLibs, onLoad) => 
      (augment(bcLibs, "extra", ConsoleLogger()) _) compose onLoad 
    }
  )

  def OpenPgpSignatureGenerator(name: String, password: String, secring: Option[String] = None, keyId: Option[String] = None): OpenPGPSignatureGenerator = {
    val gen = new OpenPGPSignatureGenerator
    gen.setName(name)
    secring foreach gen.setSecring
    keyId foreach gen.setKeyId
    gen.setPassword(password)
    gen
  }

  private def walkResolvers(f: DependencyResolver => Any)
                           (resolver: DependencyResolver) {
    resolver match {
      case chain: ChainResolver => 
        chain.getResolvers.asInstanceOf[jl.Iterable[DependencyResolver]] foreach walkResolvers(f)
      case _ =>
    }
    f(resolver)
  }

  def augment(files: Seq[File], component: String, log: Logger)(s: State): State = {
    val cs: xsbti.ComponentProvider = s.configuration.provider.components()
    val copied: Boolean = s.locked(cs.lockFile) {
      cs.addToComponent(component, files.toArray)
    }
    if (copied) {
      log.info("Copied files to %s: %s".format(component, files.mkString(",")))
      log.info("Reloading project after change to %s".format(component))
      s.reload
    }
    else {
      log.debug("Nothing to copy to %s: %s".format(component, files.mkString(",")))
      s
    }
  }
}
