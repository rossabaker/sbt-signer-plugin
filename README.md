Enables [Ivy detached signature generators](http://ant.apache.org/ivy/history/latest-milestone/settings/signers.html) in sbt.  Useful to meet the [Maven Central requirement](https://docs.sonatype.org/display/Repository/Central+Sync+Requirement) that each plugin is signed with GPG.  When this plugin is enabled, all
artifacts will be published with a \*.asc signature.

## How to use

I use it as a [global plugin](https://github.com/harrah/xsbt/wiki/Plugins).
This prevents it from cluttering up the build for those who aren't publishing
to Maven Central and makes it easy to keep my secret key secret.

1. Create `~/.sbt/plugins/project/build.scala'

        import sbt._
        import sbt.Keys._

        object PluginDef extends Build {
          override val projects = Seq(root)
          lazy val root = Project("plugins", file(".")) dependsOn (signerPlugin)
          lazy val signerPlugin = uri("git://github.com/rossabaker/sbt-signer-plugin")
        }

2. Create ~/.sbt/plugins/SignerPluginConfig.scala

        import sbt._
        import sbt.Keys._
        import com.rossabaker.sbt.signer.SignerPlugin
        import SignerPlugin.Keys._

        object SignerPluginConfig extends Plugin {
          override lazy val settings = Seq(
            signatureGenerator := Some(SignerPlugin.OpenPgpSignatureGenerator(
              name = "sbt-pgp", 
              password = "****"))) ++ SignerPlugin.signerSettings
        }

3. Download the Bouncy Castle Java Cryptography Libraries and put them in `$HOME/share/java/`.

    * [bcprov-jdk16](http://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk16/1.46/bcprov-jdk16-1.46.jar) 
    * [bcpg-jdk16](http://repo1.maven.org/maven2/org/bouncycastle/bcpg-jdk16/1.46/bcpg-jdk16-1.46.jar)

4. _[non-conformists only]_ If you chose a BouncyCastle other than 1.46, or
didn't put them in `$HOME/share/java`, declare them in your build:

        override lazy val settings = Seq(
          bouncyCastleLibraries in Global := Seq(
            "my" / "bizarre" / "location" / "bcprov.jar",
            "my" / "bizarre" / "location" / "bcpg.jar",
          )
        ) ++ SignerPlugin.signerSettings

## Cruft warning

This project [dynamically augments sbt's
classpath](https://github.com/harrah/xsbt/wiki/Specialized) when the project is
loaded.  The BouncyCastle jars are copied to `project/boot/scala-*/org.scala-tools.sbt/sbt/*/extra/`.  If you choose to stop using this plugin, you will want to do a `reboot full`.

## TODO

- Resolve the BouncyCastle libraries with Ivy to obviate steps 3 and 4.
