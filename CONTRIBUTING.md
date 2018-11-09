# Contributing to Eclipse Bridge.IoT

If you like to contribute to Bridge.IoT, please read this document carefully.

## Conventions

### Code Style

Java files:

  * We follow the standard Eclipse IDE (built in) code formatter with the following changes:
    * Tab policy: spaces only: 4
    * Maximum line width for comments: 120
    * Off/On Tags enabled (@formatter:off, @formatter:on)
  * You can just import the Bridge.IoT Java formatter profile (style/eclipse-java-Bridge.IoT-style.xml) to your IDE. In Eclipse this is on Preferences->Java->Code Style->Formatter page. 
  * We recommend using at least Eclipse [Neon](https://www.eclipse.org/neon/) IDE release.
  * The formatter can also be run from Gradle. Therefore we've integrated the spotless Gradle plugin that can do both: Check the Bridge.IoT code for format violations (spotlessJavaCheck task) and apply the formatting rules (spotlessJavaApply task) on unformatted Java files.
  * For code cleanup, you can use our Java cleanup profile on Preferences->Java->Code Style->Clean Up page: style/eclipse-java-Bridge.IoT-cleanup.xml

XML files:

  * We follow the standard Eclipse IDE XML formatter with the following changes:
    * Indent using spaces only: 3
  * You can just import our XML profile as Eclipse preferences: style/eclipse-xml-Bridge.IoT-style.epf

SonarLint:

  * We use SonarLint for the static analysis of our code.
  * SonarLint with its default ruleset.
  * Please install SonarLint (download from here: https://www.sonarlint.org/eclipse/ or install from Eclipse marketplace: https://marketplace.eclipse.org/content/sonarlint)
  * Please use SonarLint to analyze your code before initiating a pull request.

## Legal considerations for your contribution

The following steps are necessary to comply with the Eclipse Foundation's IP policy.

Please also read [this](http://wiki.eclipse.org/Development_Resources/Contributing_via_Git)

In order for any contributions to be accepted you MUST do the following things.

* Sign the [Eclipse Foundation Contributor License Agreement](http://www.eclipse.org/legal/CLA.php).
To sign the Eclipse CLA you need to:

  * Obtain an Eclipse Foundation userid. Anyone who currently uses Eclipse Bugzilla or Gerrit systems already has one of those.
If you don’t, you need to [register](https://dev.eclipse.org/site_login/createaccount.php).

  * Login into the [projects portal](https://projects.eclipse.org/), select “My Account”, and then the “Contributor License Agreement” tab.

* Add your github username in your Eclipse Foundation account settings. Log in it to Eclipse and go to account settings.

* "Sign-off" your commits

Every commit you make in your patch or pull request MUST be "signed off".

You do this by adding the `-s` flag when you make the commit(s), e.g.

    git commit -s -m "Bridges are always BIG"