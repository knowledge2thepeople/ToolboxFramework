#!/bin/sh
echo "Building toolboxframework-1.0.zip..."
{
  rm toolboxframework-1.0.zip
  rm -fr toolboxframework-1.0

  mkdir toolboxframework-1.0

  cp -r docs toolboxframework-1.0
  cp -r example toolboxframework-1.0
  cp README.md toolboxframework-1.0
  cp LICENSE toolboxframework-1.0
  cp toolboxframework-1.0.jar toolboxframework-1.0

  jar cvfM toolboxframework-1.0.zip -C toolboxframework-1.0 .

  rm -fr toolboxframework-1.0
} &> /dev/null
echo "Done building. Look for toolboxframework-1.0.zip in this directory."