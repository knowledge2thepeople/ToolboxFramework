echo "Running toolboxframework test suite (requires junit and hamcrest)"
mkdir toolboxframework-test-classes >nul 2>&1
javac -sourcepath tst -cp .;* tst\org\toolboxframework\scan\test\*.java -d toolboxframework-test-classes >nul 2>&1
java -javaagent:toolboxframework-1.0.jar="org.toolboxframework.scan.test" -cp .;*;toolboxframework-test-classes org.junit.runner.JUnitCore org.toolboxframework.scan.test.Tests
java -javaagent:toolboxframework-1.0.jar="org.toolboxframework.validate.test" -cp .;*;toolboxframework-test-classes org.junit.runner.JUnitCore org.toolboxframework.validate.test.Tests
rmdir /S /Q toolboxframework-test-classes >nul 2>&1
echo "Done testing"