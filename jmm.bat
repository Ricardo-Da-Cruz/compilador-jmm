@echo off

::set JAVA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005

call "./build/install/jmm/bin/jmm.bat" %*
