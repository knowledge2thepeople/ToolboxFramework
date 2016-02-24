echo "Building toolboxframework-1.0.jar from source..."
mkdir toolboxframework-classes >nul 2>&1
javac -sourcepath src src\org\toolboxframework\*.java -d toolboxframework-classes >nul 2>&1
javac -sourcepath src src\org\toolboxframework\transform\*.java -d toolboxframework-classes >nul 2>&1
javac -sourcepath src src\org\toolboxframework\inject\*.java -d toolboxframework-classes >nul 2>&1
javac -sourcepath src src\org\toolboxframework\inject\annotation\*.java -d toolboxframework-classes >nul 2>&1
javac -sourcepath src src\org\toolboxframework\scan\*.java -d toolboxframework-classes >nul 2>&1
javac -sourcepath src src\org\toolboxframework\scan\annotation\*.java -d toolboxframework-classes >nul 2>&1
javac -sourcepath src src\org\toolboxframework\validate\*.java -d toolboxframework-classes >nul 2>&1
jar cvfm toolboxframework-1.0.jar toolboxframework-manifest.txt -C toolboxframework-classes . >nul 2>&1
rmdir /S /Q toolboxframework-classes >nul 2>&1
echo "Done building. Look for toolboxframework-1.0.jar in this directory."
