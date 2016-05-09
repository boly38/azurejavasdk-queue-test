export AC_QUEUENAME="myqueue"
export AC_BUSDOMAIN="myazbus"
export AC_POLICYNAME="mySASPolicy"
export AC_POLICYKEY="mySecretHere"
export AC_NBMSG=10
java -jar target/azt-withdeps.jar
#
# if proxy needed : 
# java -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128 -jar target/azt-withdeps.jar
