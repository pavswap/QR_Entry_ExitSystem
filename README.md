# QR Entry Exit System

This is a simple QR Based Entry exit system. Scan the QR to enter, after scanning your name will be registered. Scanning again will remove your name.

To be able to run this on your system, you must have :
1. Java (JDK 17 +) on your system.
2. Zxing amd WebCam API in the libraries section of your project

I have listed all the links to download the *.jar* files for these files, you can also use Maven or Gradle for it.


*Webcam Capture API* : Official project page: https://webcam-capture.sarxos.pl/
                       Direct download: webcam-capture-0.3.12-dist.zip (contains the needed .jar + dependencies)
                       Maven Central (if you prefer to grab .jar manually): webcam-capture-0.3.12.jar — you can browse this at Maven central under com.github.sarxos:webcam-capture

*Zqing QR Barcode Library* : Official repository / home: https://github.com/zxing/zxing
                             Maven Central distribution for Java SE: javase-3.5.1.jar (latest stable) 

How to Setup :

  Download the webcam-capture-0.3.12-dist.zip, unzip it — inside you get webcam-capture-0.3.12.jar (and possibly related driver JARs). Add those to your classpath / project’s “lib” folder.
Download the javase-3.5.1.jar (or latest) from ZXing and add it too. That covers the QR-code decoding functionality used by your project.
Alternatively, if you use a build system like Maven or Gradle, use the Maven coordinates provided on their pages (e.g. com.github.sarxos:webcam-capture:0.3.12 and com.google.zxing:javase:3.5.1).
