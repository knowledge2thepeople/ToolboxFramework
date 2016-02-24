# The Toolbox Framework

Annotation-based, on-the-fly, dependency injection for Java (http://toolboxframework.org)

### Why, you ask?
When working with dependency injection frameworks, have you ever wanted to be able to use an annotation to inject dependencies into an instance of a class that is instantiated *after context initialization has finished* - in other words, *on-the-fly?* Or have you ever wanted to be able to use annotations to inject dependencies into static fields?

### Now you can
Toolbox is an ultra light-weight library that allows Java developers to use annotations to inject dependencies in ways they never could before - including into objects instantiated on-the-fly and static fields. Not only that, Toolbox has the most intuitive API ever created for a dependency injection framework in Java.

### Features
- Licensed under the **Apache License, Version 2.0**
- Uses only **standard** language features - no special compilers required.
- Works with **Java 5** and above.
- Ultra **light-weight**.
- Annotation-based **scanning** to populate the toolbox.
- Injects dependencies into objects *at instantiation time*, wherever those objects' constructors are invoked in the code.
- Injects dependencies into **static fields**.
- Automatically converts between **primitives types** and their class wrappers.
- All operations are guaranteed **thread safe**.
- No checked exceptions.
- Descriptive exception messages.
- Logs that allow for troubleshooting without flooding your terminal with hundreds of lines of garbage.
- Implemented without the use of any 3rd party libraries in order to provide the most **efficient** and custom solution possible.
- **Integrates seamlessly** with other dependency injection frameworks (Spring, Guice, etc.) for use within existing applications.
- **Easy to learn.** No wrestling with unfamiliar terminology, struggling with configuration file settings, or spending hours deciphering obscure documentation - using this framework is dead **simple**, the documentation is **clear**, and everything in the [API](http://toolboxframework.org/api/) is named in a way that **makes intuitive sense**.

Visit the website for more information: http://toolboxframework.org

## Installation

The easiest way to start using Toolbox is to download the JAR file from the main website at http://toolboxframework.org/download/. Alternatively, you can build the project from source using the available build scripts.

## Usage

Add `-javaagent:<path to Toolbox JAR file>="<space delimited list of packages>"` as a command to `java`.
The value in between the quotes is the list of base packages in which all classes that rely on the `@UseTool` annotation are located.
If you're using Eclipse, edit your project's runtime configuration to include the above as a VM argument.

See http://toolboxframework.org/example/ for more information.

## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request :D

## History

T.C.C., a professional software engineer, created this after experiencing working with other dependency injection frameworks and realizing their limitations.

## Credits

T.C.C.

## License

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
