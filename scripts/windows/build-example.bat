echo "Building example.jar from source..."
mkdir example-classes >nul 2>&1
javac -sourcepath example\src -cp toolboxframework-1.0.jar example\src\com\yourcompany\*.java -d example-classes >nul 2>&1
jar cvfM example.jar -C example-classes . >nul 2>&1
rmdir /S /Q example-classes >nul 2>&1
echo "Done building. Look for example.jar in this directory."