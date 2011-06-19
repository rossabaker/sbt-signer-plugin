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
  }
  import Keys._

  override lazy val settings: Seq[Project.Setting[_]] = Seq(
    signatureGenerator := None
  )

  val signerSettings: Seq[Project.Setting[_]] = Seq(
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
}
