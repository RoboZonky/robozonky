Pull requests are welcome! Before submitting your PR, please make sure your code complies with the following rules.

## IDE Config and Code Style

_RoboZonky_ has a strictly enforced code style. 
Code formatting is done by the Eclipse code formatter, using the config files found in the `ide-configuration` directory. 
When submitting a pull request the CI build will fail if running the formatter results in any code changes, so it is
recommended that you always run the formatter, by typing `mvn formatter:format`.

### Eclipse Setup

Open the *Preferences* window, and then navigate to _Java_ -> _Code Style_ -> _Formatter_. Click _Import_ and then
select the `eclipse-format.xml` file in the `ide-configuration` directory.

Next navigate to _Java_ -> _Code Style_ -> _Organize Imports_. Click _Import_ and select the `eclipse.importorder` file.

### IDEA Setup

Open the _Preferences_ window (or _Settings_ depending on your edition) , navigate to _Plugins_ and install the 
[Eclipse Code Formatter Plugin](https://plugins.jetbrains.com/plugin/6546-eclipse-code-formatter) from the Marketplace.

Restart your IDE, open the *Preferences* (or *Settings*) window again and navigate to _Other Settings_ -> 
_Eclipse Code Formatter_.

Select _Use the Eclipse Code Formatter_, then change the _Eclipse Java Formatter Config File_ to point to the
`eclipse-format.xml` file in the `ide-configuration` directory. Make sure the _Optimize Imports_ box is ticked, and
select the `eclipse.importorder` file as the import order config file.

## Copyright headers

For every file you contribute, please include the following license header:

> Copyright $YEAR The RoboZonky Project
>
> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
>
>     http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.`

Please be aware that, by committing code to _The RoboZonky Project_, you are assigning your copyright to the project. If you're not comfortable with that, do not contribute.
