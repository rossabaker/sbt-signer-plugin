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

3. Download the Bouncy Castle Java Cryptography Libraries and put them somewhere.

    * [bcprov-jdk16](http://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk16/1.46/bcprov-jdk16-1.46.jar) 
    * [bcpg-jdk16](http://repo1.maven.org/maven2/org/bouncycastle/bcpg-jdk16/1.46/bcpg-jdk16-1.46.jar)

4. Create an ~/sbt.boot.properties to add the Bouncy Castle libraries to the classpath.  Custome the resources line for the proper path:

        [scala]
          version: 2.8.1

        [app]
          org: org.scala-tools.sbt
          name: sbt
          version: read(sbt.version)[0.10.0]
          class: ${sbt.main.class-sbt.xMain}
          components: xsbti
          cross-versioned: true
          resources: /home/ross/.sbt/lib/bcpg-jdk16-1.46.jar,/home/ross/.sbt/lib/bcprov-jdk16-1.46.jar

        [repositories]
          local
          maven-local
          typesafe-ivy-releases: http://repo.typesafe.com/typesafe/ivy-releases/, [organization]/[module]/[revision]/[type]s/[artifact](-[classifier]).[ext]
          maven-central
          scala-tools-releases
          scala-tools-snapshots

        [boot]
         directory: ${sbt.boot.directory-project/boot/}

        [ivy]
          ivy-home: ${sbt.ivy.home-${user.home}/.ivy2/}

5. Reference the boot properties in your sbt script:

    java -Dsbt.boot.properties=sbt.boot.properties ... sbt-launch.7.7.jar "$@"

## TODO

Steps 3-5 are necessary because the PGP libraries need to be on the boot
classpath to be seen by Ivy.  Simply Declaring the Bouncy Castle Dependencies
as plugin dependencies results in a class loader error.  Is there a cleaner
solution?
