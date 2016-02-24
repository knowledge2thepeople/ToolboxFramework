#!/bin/sh
echo "Building example.jar from source..."
{
  mkdir example-classes
  javac -sourcepath example/src -cp toolboxframework-1.0.jar example/src/com/yourcompany/*.java -d example-classes
  jar cvfM example.jar -C example-classes .
  rm -fr example-classes
} &> /dev/null
echo "Done building. Look for example.jar in this directory."
