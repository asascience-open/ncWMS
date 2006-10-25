@echo off

java -Dpython.home=c:\jython-2.1 -cp c:\jython-2.1\jython.jar;c:\java_libs\NetCDF2.2\toolsUI-2.2.16.jar;..\..\..\build\web\WEB-INF\classes org.python.util.jython test.py
