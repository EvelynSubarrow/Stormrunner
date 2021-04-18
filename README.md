# Stormrunner
![A yellow RCX robot sits atop a field of sand, beside the wreck of the CESS Decatur spaceship](https://i.imgur.com/A48SVeU.png "Screenshot")

## About Stormrunner
Stormrunner was developed by Templar Studios and released in the year 2000 to
promote Lego Mindstorms' range of RCX robots. The game was revolutionary, at the
time one of the largest Java games ever written, accessible as an applet, with a
dramatic introduction and in-game datalogs provided by Macromedia's Flash.

### links
* Templar's [archived press release](https://web.archive.org/web/20070817042000/http://www.templar.com/info/pr_may2000.html)
* [Introduction flash video](http://biomediaproject.com/bmp/files/LEGO/gms/online/Mindstorms/Stormrunner/Stormrunner/images/sr-intro.swf)

## About this project
This project uses a ShadowJar feature to combine original Stormrunner JAR files
with compiled modifications.

The objective is to maintain a version of the game which is as functional as it
can be on modern computers.

## How to use this project (without compiling)
Please ensure that you have a compatible JRE installed (see the section below for
information on that). Get the JAR file from the latest release, and put it
somewhere convenient. Stormrunner doesn't place any files in the same directory,
so the location isn't too important.

### Linux
Open a terminal, get yourself to the same directory as the stormrunner jar, and run
`java -jar stormrunner_filename.jar`, substituting the filename. If you've got
multiple versions of the jvm installed and `java` won't be a compatible one, on
Linux at least you should look in `/usr/lib/jvm` and select an appropriate
version, and use it something like this:
`/usr/lib/jvm/adoptopenjdk-8-hotspot-jre-amd64/bin/java stormrunner_filename.jar`

### Windows
Generally, you should be able to run the game by just ensuring you have
Java installed, then doubleclicking on the jarfile. You may encounter some
difficulty if there's different versions, and you'll have to select the
appropriate one using command prompt if so.

## How to compile this project
Please checkout the latest [tagged release](https://github.com/EvelynSubarrow/Stormrunner).

If you don't trust the JAR files I've provided (and you shouldn't), you can
obtain an archive of Stormrunner at the link in this section. Place
`Stormrunner/TemplarStudios-Stormrunner-Media.jar` and
`TemplarStudios-Stormrunner.jar` into `shadow/`

Whether or not you trust me, if you have Gradle installed, the single next step
is running `gradle shadowJar`. You should then have a runnable game jar in
`build/libs/`!

### A word on the Java runtime
It's highly recommended that you use a version 8 Java runtime environment.
If you don't have one installed, [AdoptOpenJDK](https://adoptopenjdk.net)
packages are easy to use, and are available for Linux, Windows, MacOS,
Solaris, and AIX.

If you are using Debian or Ubuntu in particular, the packaged versions of
OpenJDK have exciting sound bugs which cause deadlock. I've mitigated this
by disabling sound entirely in that case, but if this applies and you
want sound, use Oracle or AdoptOpenJDK JREs instead.

Version 9 of the JRE is the last version likely to work at all with this.
Java has made significant changes to backwards compatibility since then,
affecting both sound and graphics, and there's only so much that can be done
without a total rewrite which may not be practical.

### links
* http://biomediaproject.com/bmp/files/LEGO/gms/download/Mindstorms/Stormrunner.zip

## Acknowledgements
This project relies heavily on Lee Benfield's
[CFR](https://github.com/leibnitz27/cfr/), and wouldn't be possible without
bugfixes made in response to issues encountered decompiling these old
classfiles. 

## Disclaimer
This project is not affiliated with the Lego company or with Templar Studios.
