# Stormrunner
## About Stormrunner
Stormrunner was developed by Templar Studios and released in the year 2000 to
promote Lego Mindstorms' range of RCX robots. The game was revolutionary, at the
time one of the largest Java games ever written, with a dramatic introduction
and in-game datalogs provided by Macromedia's Flash. 

### links
* Templar's [archived press release](https://web.archive.org/web/20070817042000/http://www.templar.com/info/pr_may2000.html)
* [Introduction flash video](http://biomediaproject.com/bmp/files/LEGO/gms/online/Mindstorms/Stormrunner/Stormrunner/images/sr-intro.swf)

## About this project
This project uses a much-maligned feature of ShadowJar to combine original
Stormrunner JAR files with compiled modifications into a single JAR. At present,
the modifications just allow this game to be run as a standalone java
application, but I plan to make some minor changes to make the game more
playable on modern systems.

## How to use this project
If you don't trust the JAR files I've provided (and you shouldn't), you can
obtain an archive of Stormrunner at the link in this section. Place
`Stormrunner/TemplarStudios-Stormrunner-Media.jar` and
`TemplarStudios-Stormrunner.jar` into `libs/`

Whether or not you trust me, if you have Gradle installed, the single next step
is running `gradle shadowJar`. You should then have a runnable game jar in
`build/libs/`!

### links
* http://biomediaproject.com/bmp/files/LEGO/gms/download/Mindstorms/Stormrunner.zip

