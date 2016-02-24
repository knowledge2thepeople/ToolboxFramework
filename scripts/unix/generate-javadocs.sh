#!/bin/sh
echo "Generating javadocs..."
{
  rm -rf docs
  mkdir docs
  javadoc -sourcepath src -d docs org.toolboxframework org.toolboxframework.inject org.toolboxframework.inject.annotation org.toolboxframework.scan org.toolboxframework.scan.annotation org.toolboxframework.validate
} &> /dev/null
echo "Done generating javadocs. Look for docs in this directory."
