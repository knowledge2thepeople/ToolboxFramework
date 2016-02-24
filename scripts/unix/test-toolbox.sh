#!/bin/sh
echo "Running toolboxframework test suite (requires junit and hamcrest)"
mkdir toolboxframework-test-classes &> /dev/null
javac -sourcepath tst -cp .:* tst/org/toolboxframework/scan/test/*.java tst/org/toolboxframework/validate/test/*.java -d toolboxframework-test-classes &> /dev/null
java -javaagent:toolboxframework-1.0.jar="org.toolboxframework.scan.test" -cp .:*:toolboxframework-test-classes org.junit.runner.JUnitCore org.toolboxframework.scan.test.Tests
java -javaagent:toolboxframework-1.0.jar="org.toolboxframework.validate.test" -cp .:*:toolboxframework-test-classes org.junit.runner.JUnitCore org.toolboxframework.validate.test.Tests
rm -rf toolboxframework-test-classes &> /dev/null
echo "Done testing"